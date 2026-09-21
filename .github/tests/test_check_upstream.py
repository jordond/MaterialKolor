"""Exercise the upstream monitor with disposable Git repos and stubbed network tools."""

import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "check-upstream"
REAL_GIT = shutil.which("git")


class UpstreamMonitorTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.repo = self.root / "tools/mcu-upstream/src/main"
        self.repo.mkdir(parents=True)
        self.script = self.root / ".github/check-upstream"
        self.script.parent.mkdir()
        shutil.copyfile(SCRIPT, self.script)
        self.git("init", "--quiet", "-b", "main")
        self.git("config", "user.email", "fixture@example.invalid")
        self.git("config", "user.name", "Monitor fixture")
        self.git("commit", "--quiet", "--allow-empty", "-m", "Initial fixture")
        self.base = self.git("rev-parse", "HEAD").strip()
        self.network_log = self.root / "network.jsonl"
        self.bin = self.root / "bin"
        self.bin.mkdir()
        self.stub(
            "git",
            '#!/usr/bin/env python3\n'
            'import json, os, sys\n'
            'if sys.argv[1:2] == ["fetch"]:\n'
            '    with open(os.environ["NETWORK_LOG"], "a") as stream:\n'
            '        stream.write(json.dumps({"tool": "git", "args": sys.argv[1:]}) + "\\n")\n'
            '    sys.exit(0)\n'
            'os.execv(os.environ["REAL_GIT"], [os.environ["REAL_GIT"], *sys.argv[1:]])\n',
        )
        self.stub(
            "curl",
            '#!/usr/bin/env python3\n'
            'import json, os, sys\n'
            'args = sys.argv[1:]\n'
            'request = {"tool": "curl", "args": args}\n'
            'if "-d" in args:\n'
            '    request["body"] = json.loads(args[args.index("-d") + 1])\n'
            'with open(os.environ["NETWORK_LOG"], "a") as stream:\n'
            '    stream.write(json.dumps(request) + "\\n")\n'
            'if os.environ.get("CURL_FAIL"):\n'
            '    print("stubbed HTTP failure", file=sys.stderr)\n'
            '    sys.exit(22)\n'
            'if "body" not in request:\n'
            '    print(os.environ.get("EXISTING_ISSUES", "[]"))\n'
            '    sys.exit(0)\n'
            'print(os.environ.get("POST_RESPONSE", json.dumps({"number": 123})))\n',
        )

    def stub(self, name, source):
        path = self.bin / name
        path.write_text(source)
        path.chmod(0o755)

    def git(self, *args):
        return subprocess.run(
            [REAL_GIT, *args], cwd=self.repo, text=True, capture_output=True, check=True
        ).stdout

    def commit(self, path, message):
        file = self.repo / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(message + "\n")
        self.git("add", "--", path)
        self.git("commit", "--quiet", "-m", message)
        return self.git("rev-parse", "HEAD").strip()

    def run_monitor(self, *args, prepare_refs=True, **env):
        # `prepare_refs=False` leaves the fixture repository untouched, for the cases that need the
        # monitor to meet a repository it cannot read.
        if prepare_refs:
            self.git("update-ref", "refs/remotes/origin/main", "HEAD")
            self.git("checkout", "--quiet", "--detach", self.base)
        environment = {
            **os.environ,
            "PATH": str(self.bin) + os.pathsep + os.environ["PATH"],
            "REAL_GIT": REAL_GIT,
            "NETWORK_LOG": str(self.network_log),
            **env,
        }
        return subprocess.run(
            ["bash", str(self.script), *args], cwd=self.root, env=environment,
            text=True, capture_output=True,
        )

    def requests(self):
        if not self.network_log.exists():
            return []
        return [json.loads(line) for line in self.network_log.read_text().splitlines()]

    def test_java_change_is_reported_without_sending_an_issue(self):
        self.commit("java/hct/Hct.java", "Java update")
        result = self.run_monitor()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("java/hct/Hct.java", result.stdout)
        self.assertFalse(any(call["tool"] == "curl" for call in self.requests()))

    def test_unrelated_change_does_not_send_an_issue(self):
        self.commit("typescript/hct/hct.ts", "TypeScript update")
        result = self.run_monitor("--token", "fixture-token")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertFalse(any(call["tool"] == "curl" for call in self.requests()))

    def test_no_new_commits(self):
        result = self.run_monitor("--token", "fixture-token")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("No new commits found", result.stdout)
        self.assertFalse(any(call["tool"] == "curl" for call in self.requests()))

    def test_uninitialised_submodule_fails_loudly(self):
        # An uninitialised submodule directory still lets `cd` succeed, so without
        # an explicit guard every later git command would resolve against the
        # parent repository and the monitor would report success forever.
        shutil.rmtree(self.repo / ".git")
        result = self.run_monitor(prepare_refs=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("not initialised", result.stderr)
        self.assertFalse(any(call["tool"] == "curl" for call in self.requests()))

    def test_kotlin_and_license_changes_are_reported_alongside_java(self):
        self.commit("kotlin/hct/Hct.kt", "Kotlin update")
        self.commit("LICENSE", "License update")
        self.commit("docs/NOTICE.txt", "Notice update")
        self.commit("java/hct/Hct.java", "Java update")
        self.commit("README.md", "Unrelated documentation")
        result = self.run_monitor("--token", "fixture-token")
        self.assertEqual(0, result.returncode, result.stderr)
        posts = [call["body"] for call in self.requests() if "body" in call]
        self.assertEqual(4, len(posts))
        self.assertIn("5 commits, 4 had Java, Kotlin, or license changes", result.stdout)
        self.assertIn("kotlin/hct/Hct.kt", posts[0]["body"])
        self.assertIn("LICENSE", posts[1]["body"])
        self.assertIn("docs/NOTICE.txt", posts[2]["body"])
        self.assertIn("java/hct/Hct.java", posts[3]["body"])

    def test_kotlin_build_scaffold_is_reported_as_its_own_category(self):
        # Build files appearing under upstream's kotlin/ tree are the first visible sign that
        # upstream may publish its own Kotlin Multiplatform artifacts, so they are flagged
        # separately from ordinary source churn.
        self.commit("kotlin/build.gradle.kts", "Add Kotlin Multiplatform build")
        self.commit("kotlin/gradle/libs.versions.toml", "Add a version catalog")
        result = self.run_monitor("--token", "fixture-token")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("Category: kotlin-build-scaffold", result.stdout)
        self.assertIn("2 of those changed kotlin/ build scaffolding", result.stdout)
        posts = [call["body"] for call in self.requests() if "body" in call]
        self.assertEqual(2, len(posts))
        for post in posts:
            self.assertIn("kotlin-build-scaffold", post["labels"])
            self.assertIn("kotlin-build-scaffold", post["body"])
            self.assertIn("Kotlin Multiplatform publication may be starting", post["body"])
            self.assertIn("material-color-utilities/pull/76", post["body"])

    def test_ordinary_kotlin_source_change_is_not_scaffold(self):
        self.commit("kotlin/hct/Hct.kt", "Kotlin update")
        self.commit("kotlin/dynamiccolor/MaterialDynamicColors.kt", "Another Kotlin update")
        result = self.run_monitor("--token", "fixture-token")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("Category: upstream-source", result.stdout)
        self.assertNotIn("kotlin-build-scaffold", result.stdout)
        posts = [call["body"] for call in self.requests() if "body" in call]
        self.assertEqual(2, len(posts))
        for post in posts:
            self.assertEqual(["upstream"], post["labels"])
            self.assertNotIn("kotlin-build-scaffold", post["body"])

    def test_quote_bearing_commit_message_produces_valid_json(self):
        message = 'Fix "quoted" Java title with \u00e9'
        self.commit("java/hct/Hct.java", message)
        result = self.run_monitor("--token", "fixture-token")
        self.assertEqual(0, result.returncode, result.stderr)
        posts = [call["body"] for call in self.requests() if "body" in call]
        self.assertEqual(1, len(posts))
        self.assertTrue(posts[0]["title"].endswith(message))

    def test_existing_issue_is_not_recreated(self):
        commit = self.commit("kotlin/hct/Hct.kt", "Kotlin update")
        short_sha = self.git("rev-parse", "--short", commit).strip()
        existing = [{"title": f"[Upstream:{short_sha}] Kotlin update", "number": 77}]
        result = self.run_monitor("--token", "fixture-token", EXISTING_ISSUES=json.dumps(existing))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("Issue #77", result.stdout)
        self.assertFalse(any("body" in call for call in self.requests()))

    def test_network_failure_fails_the_monitor(self):
        self.commit("kotlin/hct/Hct.kt", "Kotlin update")
        result = self.run_monitor("--token", "fixture-token", CURL_FAIL="1")
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(any("body" in call for call in self.requests()))

    def test_failed_issue_creation_continues_with_the_next_commit(self):
        # A single commit GitHub refuses must not cost the run every commit after it.
        self.commit("kotlin/hct/Hct.kt", "Kotlin update")
        self.commit("java/hct/Hct.java", "Java update")
        result = self.run_monitor(
            "--token", "fixture-token", POST_RESPONSE=json.dumps({"message": "Validation Failed"})
        )
        self.assertEqual(0, result.returncode, result.stderr)
        posts = [call["body"] for call in self.requests() if "body" in call]
        self.assertEqual(2, len(posts))
        self.assertEqual(2, result.stderr.count("returned no issue number"))
        self.assertIn("2 commits, 2 had Java, Kotlin, or license changes", result.stdout)

    def test_token_without_a_value_is_rejected(self):
        result = self.run_monitor("--token")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Usage:", result.stderr)
        self.assertFalse(any(call["tool"] == "curl" for call in self.requests()))

    def test_empty_token_is_rejected(self):
        result = self.run_monitor("--token", "")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Usage:", result.stderr)
        self.assertFalse(any(call["tool"] == "curl" for call in self.requests()))

    def test_no_output_is_quiet(self):
        self.commit("java/hct/Hct.java", "Java update")
        result = self.run_monitor("--no-output")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("", result.stdout)


if __name__ == "__main__":
    unittest.main()
