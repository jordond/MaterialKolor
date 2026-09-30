"""Exercise the bump publisher against a local origin, a real bundle and a stubbed gh."""

import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "scripts/publish-upstream"
REAL_GIT = shutil.which("git")
TOKEN = "fixture-secret-token"
TARGET = "b" * 40
CURRENT = "a" * 40
SUBMODULE = "tools/mcu-upstream/src/main"
FIXTURE = "material-color-utilities/src/commonTest/kotlin/com/materialkolor/conformance/UpstreamRoleGoldenData.kt"


class PublishUpstreamTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.origin = self.root / "origin.git"
        self.builder = self.root / "builder"
        self.checkout = self.root / "checkout"
        subprocess.run([REAL_GIT, "init", "--quiet", "--bare", "-b", "next", str(self.origin)], check=True)
        subprocess.run([REAL_GIT, "clone", "--quiet", str(self.origin), str(self.builder)],
                       check=True, capture_output=True)
        self.set_pin(self.builder, CURRENT)
        self.commit(self.builder, "README.md", "chore: base")
        self.git(self.builder, "push", "--quiet", "origin", "HEAD:next")
        self.base = self.git(self.builder, "rev-parse", "HEAD")

        # The bump is built in one clone and published from another, as the two jobs do.
        self.git(self.builder, "switch", "--quiet", "-c", "upstream/mcu")
        self.set_pin(self.builder, TARGET)
        self.commit(self.builder, "gradle/mcu-upstream.lock.json", "fix(mcu): update upstream pin to bbbbbbb")
        self.commit(self.builder, FIXTURE, "test(mcu): regenerate golden fixtures for upstream bbbbbbb")
        self.bump_head = self.git(self.builder, "rev-parse", "HEAD")
        self.bundle = self.root / "bump/bump.bundle"
        self.bundle.parent.mkdir()
        self.make_bundle()
        subprocess.run([REAL_GIT, "clone", "--quiet", str(self.origin), str(self.checkout)],
                       check=True, capture_output=True)

        self.summary = self.root / "bump/upstream-commits.md"
        self.summary.write_text("- [`bbbbbbb`](https://example.invalid) Kotlin update\n")
        self.bin = self.root / "bin"
        self.bin.mkdir()
        self.log = self.root / "calls.jsonl"
        self.stub("gh", (
            '#!/usr/bin/env python3\n'
            'import json, os, sys\n'
            'args = sys.argv[1:]\n'
            'call = {"tool": "gh", "args": args}\n'
            'if "--body-file" in args:\n'
            '    call["body"] = open(args[args.index("--body-file") + 1]).read()\n'
            'with open(os.environ["CALL_LOG"], "a") as stream:\n'
            '    stream.write(json.dumps(call) + "\\n")\n'
            'if args[:2] == ["pr", "view"]:\n'
            '    print(os.environ.get("COMMENTS", ""))\n'
        ))
        # Records every git command line, to prove the token never appears on one.
        self.stub("git", (
            '#!/usr/bin/env python3\n'
            'import json, os, sys\n'
            'with open(os.environ["CALL_LOG"], "a") as stream:\n'
            '    stream.write(json.dumps({"tool": "git", "args": sys.argv[1:],\n'
            '        "header": os.environ.get("GIT_CONFIG_VALUE_0", "")}) + "\\n")\n'
            'os.execv(os.environ["REAL_GIT"], [os.environ["REAL_GIT"], *sys.argv[1:]])\n'
        ))

    def set_pin(self, cwd, revision):
        self.git(cwd, "update-index", "--add", "--cacheinfo", f"160000,{revision},{SUBMODULE}")

    def make_bundle(self):
        self.git(self.builder, "bundle", "create", "--quiet", str(self.bundle), "upstream/mcu", f"^{self.base}")

    def stub(self, name, source):
        path = self.bin / name
        path.write_text(source)
        path.chmod(0o755)

    def git(self, cwd, *args):
        return subprocess.run(
            [REAL_GIT, *args], cwd=cwd, text=True, capture_output=True, check=True
        ).stdout.strip()

    def commit(self, cwd, path, message):
        file = cwd / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(message + "\n")
        # Only the named file, since `add -A` would drop the Gitlink, which has no checkout.
        self.git(cwd, "add", "--", path)
        self.git(cwd, "-c", "user.email=bot@example.invalid", "-c", "user.name=Bot", "commit",
                 "--quiet", "-m", message)

    def publish(self, action, **overrides):
        environment = {
            **os.environ,
            "PATH": str(self.bin) + os.pathsep + os.environ["PATH"],
            "REAL_GIT": REAL_GIT,
            "CALL_LOG": str(self.log),
            "GH_TOKEN": TOKEN,
            "ACTION": action,
            "BASE_BRANCH": "next",
            "BUMP_BRANCH": "upstream/mcu",
            "NUMBER": "",
            "DELETE_BRANCH": "true",
            "TITLE": "fix(mcu): update upstream pin to bbbbbbb",
            "EXPECTED": "",
            "CURRENT": CURRENT,
            "CURRENT_SHORT": CURRENT[:7],
            "TARGET": TARGET,
            "TARGET_SHORT": TARGET[:7],
            "TRANSFORM": "success",
            "GOLDENS": "success",
            "RUN_URL": "https://example.invalid/run",
            **overrides,
        }
        return subprocess.run(
            ["bash", str(SCRIPT), "--bundle", str(self.bundle), "--summary", str(self.summary)],
            cwd=self.checkout, env=environment, text=True, capture_output=True,
        )

    def calls(self, tool):
        if not self.log.exists():
            return []
        return [call for call in map(json.loads, self.log.read_text().splitlines()) if call["tool"] == tool]

    def origin_head(self):
        return subprocess.run([REAL_GIT, "rev-parse", "-q", "--verify", "refs/heads/upstream/mcu"],
                              cwd=self.origin, text=True, capture_output=True).stdout.strip()

    def test_update_pushes_the_bundle_and_opens_a_pull_request(self):
        result = self.publish("update")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(self.bump_head, self.origin_head())
        (create,) = self.calls("gh")
        self.assertEqual(["pr", "create"], create["args"][:2])
        for flag, value in [("--base", "next"), ("--head", "upstream/mcu"), ("--label", "upstream"),
                            ("--title", "fix(mcu): update upstream pin to bbbbbbb")]:
            self.assertEqual(value, create["args"][create["args"].index(flag) + 1])
        body = create["body"]
        self.assertIn("from `aaaaaaa` to `bbbbbbb`", body)
        self.assertIn("Kotlin update", body)
        self.assertIn("The transformer adapted the new sources.", body)
        self.assertIn("Golden fixtures were regenerated", body)

    def test_token_travels_only_as_a_header(self):
        self.publish("update")
        for call in self.calls("git"):
            self.assertFalse(any(TOKEN in arg for arg in call["args"]), call)
        (push,) = [call for call in self.calls("git") if call["args"][:1] == ["push"]]
        self.assertTrue(push["header"].startswith("AUTHORIZATION: basic "))

    def test_update_edits_an_open_pull_request(self):
        result = self.publish("update", NUMBER="7")
        self.assertEqual(0, result.returncode, result.stderr)
        (edit,) = self.calls("gh")
        self.assertEqual(["pr", "edit", "7"], edit["args"][:3])

    def push_old_bot_branch(self):
        """Leaves origin with a bump branch that diverges from the bundle, as a rewrite finds it."""
        self.git(self.builder, "switch", "--quiet", "-c", "old-bump", self.base)
        self.commit(self.builder, "gradle/mcu-upstream.lock.json", "fix(mcu): update upstream pin to older")
        self.git(self.builder, "push", "--quiet", "origin", "old-bump:upstream/mcu")
        return self.git(self.builder, "rev-parse", "HEAD")

    def test_matching_lease_rewrites_a_diverged_branch(self):
        old = self.push_old_bot_branch()
        result = self.publish("update", EXPECTED=old, NUMBER="7")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(self.bump_head, self.origin_head())

    def test_stale_lease_refuses_to_overwrite_a_diverged_branch(self):
        old = self.push_old_bot_branch()
        result = self.publish("update", EXPECTED=self.base, NUMBER="7")
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(old, self.origin_head())
        self.assertEqual([], self.calls("gh"))

    def test_absent_lease_refuses_to_overwrite_an_existing_branch(self):
        old = self.push_old_bot_branch()
        result = self.publish("update", EXPECTED="")
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(old, self.origin_head())
        self.assertEqual([], self.calls("gh"))

    def assertBumpRejected(self, message, **overrides):
        self.make_bundle()
        result = self.publish("update", **overrides)
        self.assertNotEqual(0, result.returncode)
        self.assertIn(message, result.stderr)
        self.assertEqual("", self.origin_head())
        self.assertEqual([], self.calls("gh"))

    def test_bump_touching_other_files_is_rejected(self):
        self.commit(self.builder, ".github/scripts/publish-upstream", "chore: doctored script")
        self.assertBumpRejected(".github/scripts/publish-upstream")

    def test_bump_pinning_another_revision_is_rejected(self):
        self.assertBumpRejected("instead of " + "c" * 40, TARGET="c" * 40)

    def test_base_off_the_base_branch_is_rejected(self):
        self.git(self.checkout, "-c", "user.email=x@example.invalid", "-c", "user.name=X", "commit",
                 "--quiet", "--allow-empty", "-m", "detached work")
        self.assertBumpRejected("is not on next")

    def test_encoded_token_is_masked(self):
        result = self.publish("update")
        self.assertEqual(0, result.returncode, result.stderr)
        encoded = __import__("base64").b64encode(f"x-access-token:{TOKEN}".encode()).decode()
        self.assertIn(f"::add-mask::{encoded}", result.stdout)

    def test_status_lines_follow_the_bump_outcome(self):
        self.publish("update", TRANSFORM="failure", GOLDENS="skipped")
        body = self.calls("gh")[0]["body"]
        self.assertIn("**The transformer failed on the new sources.**", body)
        self.assertIn("not regenerated because the transform failed", body)
        self.assertIn("https://example.invalid/run", body)

    def test_close_deletes_a_bot_branch(self):
        result = self.publish("close", NUMBER="7", DELETE_BRANCH="true")
        self.assertEqual(0, result.returncode, result.stderr)
        (close,) = self.calls("gh")
        self.assertEqual(["pr", "close", "7", "--delete-branch"], close["args"][:4])

    def test_close_keeps_a_maintainer_branch(self):
        result = self.publish("close", NUMBER="7", DELETE_BRANCH="false")
        self.assertEqual(0, result.returncode, result.stderr)
        (close,) = self.calls("gh")
        self.assertNotIn("--delete-branch", close["args"])
        self.assertIn("so it was kept", close["args"][-1])

    def test_notify_comments_once_per_target_and_base(self):
        result = self.publish("notify", NUMBER="7")
        self.assertEqual(0, result.returncode, result.stderr)
        view, comment = self.calls("gh")
        self.assertEqual(["pr", "view", "7"], view["args"][:3])
        self.assertEqual(["pr", "comment", "7"], comment["args"][:3])
        marker = f"<!-- upstream-target:{TARGET} base:{CURRENT} -->"
        self.assertIn(marker, comment["args"][-1])

        self.log.unlink()
        result = self.publish("notify", NUMBER="7", COMMENTS=f"Earlier note\n\n{marker}")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual([["pr", "view"]], [call["args"][:2] for call in self.calls("gh")])

    def test_missing_token_fails_with_the_secret_name(self):
        result = self.publish("update", GH_TOKEN="")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("UPSTREAM_BOT_TOKEN", result.stderr)
        self.assertEqual([], self.calls("gh"))

    def test_unknown_action_is_rejected(self):
        result = self.publish("merge")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Unknown action", result.stderr)


if __name__ == "__main__":
    unittest.main()
