"""Exercise the bump pull request planner against disposable Git repositories and a stubbed gh."""

import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "scripts/plan-upstream"
REAL_GIT = shutil.which("git")
BOT_EMAIL = "41898282+github-actions[bot]@users.noreply.github.com"
OLD_PIN = "a" * 40
NEW_PIN = "b" * 40
NEWER_PIN = "c" * 40


class PlanUpstreamTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.origin = self.root / "origin.git"
        self.repo = self.root / "work"
        subprocess.run([REAL_GIT, "init", "--quiet", "--bare", "-b", "next", str(self.origin)], check=True)
        subprocess.run([REAL_GIT, "init", "--quiet", "-b", "next", str(self.repo)], check=True)
        self.git("remote", "add", "origin", str(self.origin))
        self.write_lock(OLD_PIN)
        self.commit("chore: base", email="owner@example.invalid")
        self.git("push", "--quiet", "origin", "next")
        self.bin = self.root / "bin"
        self.bin.mkdir()
        self.gh_log = self.root / "gh.jsonl"
        stub = self.bin / "gh"
        stub.write_text(
            '#!/usr/bin/env python3\n'
            'import json, os, sys\n'
            'import subprocess\n'
            'args = sys.argv[1:]\n'
            'with open(os.environ["GH_LOG"], "a") as stream:\n'
            '    stream.write(json.dumps(args) + "\\n")\n'
            'data = os.environ.get("PULL_REQUESTS", "[]")\n'
            'if "--jq" in args:\n'
            '    data = subprocess.run(["jq", "-c", args[args.index("--jq") + 1]], input=data,\n'
            '                          text=True, capture_output=True, check=True).stdout\n'
            'print(data)\n'
        )
        stub.chmod(0o755)

    def git(self, *args):
        return subprocess.run(
            [REAL_GIT, *args], cwd=self.repo, text=True, capture_output=True, check=True
        ).stdout.strip()

    def write_lock(self, revision):
        lock = self.repo / "gradle/mcu-upstream.lock.json"
        lock.parent.mkdir(parents=True, exist_ok=True)
        lock.write_text(json.dumps({"upstreamRevision": revision}, indent=2) + "\n")

    def commit(self, message, email=BOT_EMAIL, path=None):
        if path:
            file = self.repo / path
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text(message + "\n")
        self.git("add", "-A")
        self.git("-c", f"user.email={email}", "-c", "user.name=Fixture", "commit", "--quiet",
                 "--allow-empty", "-m", message)

    def bump_branch(self, revision=NEW_PIN):
        self.git("switch", "--quiet", "-C", "upstream/mcu", "origin/next")
        self.write_lock(revision)
        self.commit(f"fix(mcu): update upstream pin to {revision[:7]}")

    def publish(self):
        self.git("push", "--quiet", "--force", "origin", "upstream/mcu")
        self.git("switch", "--quiet", "next")
        self.git("fetch", "--quiet", "origin")

    def move_next_pin(self, revision):
        self.git("switch", "--quiet", "next")
        self.write_lock(revision)
        self.commit("fix(mcu): manual bump", email="owner@example.invalid")
        self.git("push", "--quiet", "origin", "next")
        self.git("fetch", "--quiet", "origin")

    def plan(self, current=OLD_PIN, target=NEW_PIN, relevant=1, pull_requests=(), **overrides):
        output = self.root / "github-output"
        output.write_text("")
        environment = {
            **os.environ,
            "PATH": str(self.bin) + os.pathsep + os.environ["PATH"],
            "GH_LOG": str(self.gh_log),
            "PULL_REQUESTS": json.dumps(list(pull_requests)),
            "BASE_BRANCH": "next",
            "BUMP_BRANCH": "upstream/mcu",
            "BOT_EMAIL": BOT_EMAIL,
            "CURRENT": current,
            "TARGET": target,
            "TARGET_SHORT": target[:7],
            "RELEVANT": str(relevant),
            **overrides,
        }
        result = subprocess.run(
            ["bash", str(SCRIPT), "--github-output", str(output)], cwd=self.repo, env=environment,
            text=True, capture_output=True,
        )
        values = dict(line.split("=", 1) for line in output.read_text().splitlines())
        return result, values

    def assertPlan(self, expected_action, expected_delete="false", **kwargs):
        result, values = self.plan(**kwargs)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(expected_action, values["action"], result.stdout)
        self.assertEqual(expected_delete, values["delete_branch"], result.stdout)
        return values

    def open_pr(self, number=7, target=NEW_PIN, cross_repository=False):
        return {"number": number, "state": "OPEN", "isCrossRepository": cross_repository,
                "title": f"fix(mcu): update upstream pin to {target[:7]}"}

    def test_nothing_relevant_and_no_pull_request_does_nothing(self):
        self.assertPlan("none", relevant=0)

    def test_first_relevant_change_opens_a_pull_request(self):
        values = self.assertPlan("update")
        self.assertEqual("", values["number"])
        # No branch yet, so the push must find it absent.
        self.assertEqual("", values["expected"])
        self.assertEqual(f"fix(mcu): update upstream pin to {NEW_PIN[:7]}", values["title"])

    def test_gh_is_asked_for_every_pull_request_on_the_bump_branch(self):
        self.plan()
        call = json.loads(self.gh_log.read_text().splitlines()[0])
        self.assertEqual(["pr", "list"], call[:2])
        for flag, value in [("--base", "next"), ("--head", "upstream/mcu"), ("--state", "all"),
                            ("--limit", "100")]:
            self.assertEqual(value, call[call.index(flag) + 1])
        fields = call[call.index("--json") + 1].split(",")
        self.assertEqual({"number", "state", "title", "isCrossRepository"}, set(fields))

    def test_caught_up_pull_request_is_closed_and_its_branch_deleted(self):
        self.bump_branch()
        self.publish()
        values = self.assertPlan("close", "true", relevant=0, current=NEW_PIN, pull_requests=[self.open_pr()])
        self.assertEqual("7", values["number"])

    def test_caught_up_pull_request_keeps_a_branch_with_maintainer_commits(self):
        self.bump_branch()
        self.commit("fix(mcu): adapt a rule", email="owner@example.invalid", path="tools/rule.kt")
        self.publish()
        self.assertPlan("close", "false", relevant=0, current=NEW_PIN, pull_requests=[self.open_pr()])

    def test_up_to_date_pull_request_is_left_alone(self):
        self.bump_branch()
        self.publish()
        self.assertPlan("none", pull_requests=[self.open_pr()])

    def test_upstream_moving_again_rewrites_a_bot_only_branch(self):
        self.bump_branch()
        self.publish()
        self.assertPlan("update", target=NEWER_PIN, pull_requests=[self.open_pr()])

    def test_abi_dump_commits_do_not_stop_the_rewrite(self):
        self.bump_branch()
        self.commit("chore: update ABI dumps", email="owner@example.invalid",
                    path="material-color-utilities/api/material-color-utilities.klib.api")
        self.publish()
        self.assertPlan("update", target=NEWER_PIN, pull_requests=[self.open_pr()])

    def test_update_branch_merges_do_not_stop_the_rewrite(self):
        self.bump_branch()
        self.git("switch", "--quiet", "next")
        self.commit("feat: unrelated work on next", email="owner@example.invalid", path="README.md")
        self.git("push", "--quiet", "origin", "next")
        self.git("switch", "--quiet", "upstream/mcu")
        self.git("-c", "user.email=owner@example.invalid", "-c", "user.name=Owner", "merge", "--quiet",
                 "--no-edit", "next")
        self.publish()
        self.assertPlan("update", target=NEWER_PIN, pull_requests=[self.open_pr()])

    def test_maintainer_source_commits_stop_the_rewrite(self):
        self.bump_branch()
        self.commit("fix(mcu): adapt a rule", email="owner@example.invalid", path="tools/rule.kt")
        self.publish()
        self.assertPlan("notify", target=NEWER_PIN, pull_requests=[self.open_pr()])

    def test_manual_pin_change_on_next_refreshes_the_pull_request(self):
        self.bump_branch(NEWER_PIN)
        self.publish()
        self.move_next_pin(NEW_PIN)
        self.assertPlan("update", current=NEW_PIN, target=NEWER_PIN, pull_requests=[self.open_pr(target=NEWER_PIN)])

    def test_pull_request_closed_by_the_maintainer_stays_closed_for_that_target(self):
        closed = {**self.open_pr(), "state": "CLOSED"}
        self.assertPlan("none", pull_requests=[closed])

    def test_closed_pull_request_for_an_older_target_does_not_snooze(self):
        closed = {**self.open_pr(target=OLD_PIN), "state": "CLOSED"}
        self.assertPlan("update", pull_requests=[closed])

    def test_merged_pull_request_does_not_snooze(self):
        merged = {**self.open_pr(), "state": "MERGED"}
        self.assertPlan("update", pull_requests=[merged])

    def test_fork_pull_request_with_the_same_branch_name_is_ignored(self):
        values = self.assertPlan("update", pull_requests=[self.open_pr(number=9, cross_repository=True)])
        self.assertEqual("", values["number"])

    def test_source_under_an_api_directory_counts_as_maintainer_work(self):
        self.bump_branch()
        self.commit("feat: api helper", email="owner@example.invalid",
                    path="material-color-utilities/src/commonMain/kotlin/api/Helper.kt")
        self.publish()
        self.assertPlan("notify", target=NEWER_PIN, pull_requests=[self.open_pr()])

    def test_abi_dumps_in_nested_directories_do_not_count(self):
        self.bump_branch()
        self.commit("chore: update ABI dumps", email="owner@example.invalid",
                    path="material-kolor-core/api/android/material-kolor-core.api")
        self.publish()
        self.assertPlan("update", target=NEWER_PIN, pull_requests=[self.open_pr()])

    def test_orphaned_branch_with_maintainer_commits_is_left_alone(self):
        self.bump_branch()
        self.commit("fix(mcu): adapt a rule", email="owner@example.invalid", path="tools/rule.kt")
        self.publish()
        result, values = self.plan(target=NEWER_PIN)
        self.assertEqual("none", values["action"])
        self.assertIn("Delete it to resume", result.stdout)

    def test_orphaned_bot_branch_is_rewritten_under_a_lease(self):
        self.bump_branch()
        self.publish()
        values = self.assertPlan("update", target=NEWER_PIN)
        self.assertEqual(self.git("rev-parse", "origin/upstream/mcu"), values["expected"])

    def test_open_pull_request_without_its_branch_fails(self):
        result, _ = self.plan(pull_requests=[self.open_pr()])
        self.assertNotEqual(0, result.returncode)
        self.assertIn("is gone", result.stderr)

    def test_base_is_the_checked_out_commit(self):
        values = self.assertPlan("update")
        self.assertEqual(self.git("rev-parse", "HEAD"), values["base"])

    def test_failed_branch_lookup_is_not_read_as_absent(self):
        self.git("remote", "set-url", "origin", str(self.root / "missing.git"))
        result, values = self.plan()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Could not look up upstream/mcu", result.stderr)
        self.assertNotIn("action", values)

    def test_missing_input_is_rejected(self):
        result, _ = self.plan(TARGET_SHORT="")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Missing TARGET_SHORT", result.stderr)


if __name__ == "__main__":
    unittest.main()
