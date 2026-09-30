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
            'with open(os.environ["NETWORK_LOG"], "a") as stream:\n'
            '    stream.write(json.dumps({"tool": "curl", "args": sys.argv[1:]}) + "\\n")\n'
            'sys.exit(22)\n',
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

    def results(self, path):
        return dict(line.split("=", 1) for line in path.read_text().splitlines())

    def test_java_only_change_is_skipped(self):
        self.commit("java/hct/Hct.java", "Java update")
        result = self.run_monitor()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("no Kotlin or license changes", result.stdout)
        self.assertIn("1 commits, 0 had Kotlin or license changes", result.stdout)

    def test_unrelated_change_is_skipped(self):
        self.commit("typescript/hct/hct.ts", "TypeScript update")
        result = self.run_monitor()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("0 had Kotlin or license changes", result.stdout)

    def test_no_new_commits(self):
        output = self.root / "github-output"
        result = self.run_monitor("--github-output", str(output))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("No new commits found", result.stdout)
        values = self.results(output)
        self.assertEqual(self.base, values["current"])
        self.assertEqual(self.base, values["target"])
        self.assertEqual("0", values["relevant"])

    def test_uninitialised_submodule_fails_loudly(self):
        # An uninitialised submodule directory still lets `cd` succeed, so without
        # an explicit guard every later git command would resolve against the
        # parent repository and the monitor would report success forever.
        shutil.rmtree(self.repo / ".git")
        result = self.run_monitor(prepare_refs=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("not initialised", result.stderr)

    def test_kotlin_and_license_changes_are_relevant_and_java_is_not(self):
        kotlin = self.commit("kotlin/hct/Hct.kt", "Kotlin update")
        self.commit("LICENSE", "License update")
        self.commit("docs/NOTICE.txt", "Notice update")
        self.commit("java/hct/Hct.java", "Java update")
        head = self.commit("README.md", "Unrelated documentation")
        output = self.root / "github-output"
        summary = self.root / "summary.md"
        result = self.run_monitor("--github-output", str(output), "--summary", str(summary))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("5 commits, 3 had Kotlin or license changes", result.stdout)
        values = self.results(output)
        self.assertEqual(self.base, values["current"])
        # The pin moves to upstream's head even when the newest commits are irrelevant.
        self.assertEqual(head, values["target"])
        self.assertEqual("3", values["relevant"])
        self.assertEqual("0", values["scaffold"])
        text = summary.read_text()
        self.assertIn(f"material-color-utilities/commit/{kotlin})", text)
        for expected in ["Kotlin update", "`kotlin/hct/Hct.kt`", "`LICENSE`", "`docs/NOTICE.txt`"]:
            self.assertIn(expected, text)
        for unexpected in ["Java update", "Unrelated documentation", "kotlin-build-scaffold"]:
            self.assertNotIn(unexpected, text)

    def test_kotlin_build_scaffold_is_reported_as_its_own_category(self):
        # Build files appearing under upstream's kotlin/ tree are the first visible sign that
        # upstream may publish its own Kotlin Multiplatform artifacts, so they are flagged
        # separately from ordinary source churn.
        self.commit("kotlin/build.gradle.kts", "Add Kotlin Multiplatform build")
        self.commit("kotlin/gradle/libs.versions.toml", "Add a version catalog")
        output = self.root / "github-output"
        summary = self.root / "summary.md"
        result = self.run_monitor("--github-output", str(output), "--summary", str(summary))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("Category: kotlin-build-scaffold", result.stdout)
        self.assertIn("2 of those changed kotlin/ build scaffolding", result.stdout)
        self.assertEqual("2", self.results(output)["scaffold"])
        text = summary.read_text()
        self.assertEqual(2, text.count("(**kotlin-build-scaffold**)"))
        self.assertIn("Kotlin Multiplatform publication may be starting", text)
        self.assertIn("material-color-utilities/pull/76", text)

    def test_ordinary_kotlin_source_change_is_not_scaffold(self):
        self.commit("kotlin/hct/Hct.kt", "Kotlin update")
        self.commit("kotlin/dynamiccolor/MaterialDynamicColors.kt", "Another Kotlin update")
        summary = self.root / "summary.md"
        result = self.run_monitor("--summary", str(summary))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("Category: upstream-source", result.stdout)
        self.assertNotIn("kotlin-build-scaffold", result.stdout)
        self.assertNotIn("kotlin-build-scaffold", summary.read_text())

    def test_quote_bearing_commit_title_is_kept_verbatim(self):
        message = 'Fix "quoted" Kotlin title with \u00e9'
        self.commit("kotlin/hct/Hct.kt", message)
        summary = self.root / "summary.md"
        result = self.run_monitor("--summary", str(summary))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn(message, summary.read_text())

    def test_only_git_fetch_touches_the_network(self):
        self.commit("kotlin/hct/Hct.kt", "Kotlin update")
        result = self.run_monitor("--summary", str(self.root / "summary.md"))
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(["git"], [call["tool"] for call in self.requests()])

    def test_option_without_a_value_is_rejected(self):
        for option in ["--github-output", "--summary"]:
            result = self.run_monitor(option)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("Usage:", result.stderr)
            result = self.run_monitor(option, "")
            self.assertNotEqual(0, result.returncode)
            self.assertIn("Usage:", result.stderr)

    def test_removed_token_option_is_rejected(self):
        result = self.run_monitor("--token", "fixture-token")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Unknown option", result.stderr)

    def test_no_output_is_quiet(self):
        self.commit("kotlin/hct/Hct.kt", "Kotlin update")
        result = self.run_monitor("--no-output")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("", result.stdout)


if __name__ == "__main__":
    unittest.main()
