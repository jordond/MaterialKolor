"""Exercise the upstream bump against a disposable superproject, submodule and fake Gradle."""

import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "scripts/bump-upstream"
REAL_GIT = shutil.which("git")
FIXTURES = "material-color-utilities/src/commonTest/kotlin/com/materialkolor/conformance"
SUBMODULE = "tools/mcu-upstream/src/main"

# Stands in for the Gradle tasks the bump calls, logging each one and failing the one named by
# FAIL_TASK.
FAKE_GRADLEW = r'''#!/usr/bin/env python3
import json, os, subprocess, sys
task = sys.argv[-1]
with open(os.environ["GRADLE_LOG"], "a") as stream:
    stream.write(task + "\n")
if os.environ.get("FAIL_TASK") == task:
    sys.exit(1)
revision = subprocess.run(["git", "-C", "tools/mcu-upstream/src/main", "rev-parse", "HEAD"],
                          text=True, capture_output=True, check=True).stdout.strip()
if task == "candidateMcuUpstreamLock":
    os.makedirs("build", exist_ok=True)
    with open("build/mcu-upstream.lock.candidate.json", "w") as stream:
        json.dump({"upstreamRevision": revision}, stream, indent=4)
elif task.startswith(":mcu-upstream:printMcu"):
    print(f"package com.materialkolor.conformance\n// {task} {revision}")
'''


class BumpUpstreamTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.upstream = self.root / "upstream"
        self.repo = self.root / "work"
        self.gradle_log = self.root / "gradle.log"

        self.upstream.mkdir()
        self.run_git(self.upstream, "init", "--quiet", "-b", "main")
        (self.upstream / "kotlin").mkdir()
        (self.upstream / "kotlin/Hct.kt").write_text("one\n")
        self.commit_all(self.upstream, "Upstream one")
        self.old_pin = self.run_git(self.upstream, "rev-parse", "HEAD")
        (self.upstream / "kotlin/Hct.kt").write_text("two\n")
        self.commit_all(self.upstream, "Upstream two")
        self.new_pin = self.run_git(self.upstream, "rev-parse", "HEAD")

        self.repo.mkdir()
        self.run_git(self.repo, "init", "--quiet", "-b", "next")
        (self.repo / ".github/scripts").mkdir(parents=True)
        shutil.copyfile(SCRIPT, self.repo / ".github/scripts/bump-upstream")
        gradlew = self.repo / "gradlew"
        gradlew.write_text(FAKE_GRADLEW)
        gradlew.chmod(0o755)
        (self.repo / ".gitignore").write_text("build/\n")
        subprocess.run([REAL_GIT, "clone", "--quiet", str(self.upstream), str(self.repo / SUBMODULE)],
                       check=True)
        self.run_git(self.repo / SUBMODULE, "checkout", "--quiet", "--detach", self.old_pin)
        (self.repo / "gradle").mkdir()
        (self.repo / "gradle/mcu-upstream.lock.json").write_text(
            json.dumps({"upstreamRevision": self.old_pin}, indent=2) + "\n")
        fixtures = self.repo / FIXTURES
        fixtures.mkdir(parents=True)
        for name in ["UpstreamRoleGoldenData.kt", "UpstreamQuantizerGoldenData.kt"]:
            (fixtures / name).write_text("original\n")
        self.commit_all(self.repo, "chore: base")
        self.base = self.run_git(self.repo, "rev-parse", "HEAD")

    def run_git(self, cwd, *args):
        return subprocess.run(
            [REAL_GIT, "-c", "advice.addEmbeddedRepo=false", *args], cwd=cwd, text=True,
            capture_output=True, check=True,
        ).stdout.strip()

    def commit_all(self, cwd, message):
        self.run_git(cwd, "add", "-A")
        self.run_git(cwd, "-c", "user.email=fixture@example.invalid", "-c", "user.name=Fixture",
                     "commit", "--quiet", "-m", message)

    def bump(self, **overrides):
        output = self.root / "github-output"
        output.write_text("")
        bundle = self.root / "out/bump.bundle"
        environment = {
            **os.environ,
            "GRADLE_LOG": str(self.gradle_log),
            "BUMP_BRANCH": "upstream/mcu",
            "BOT_NAME": "github-actions[bot]",
            "BOT_EMAIL": "bot@example.invalid",
            "TARGET": self.new_pin,
            "TARGET_SHORT": self.new_pin[:7],
            "TITLE": f"fix(mcu): update upstream pin to {self.new_pin[:7]}",
            **overrides,
        }
        result = subprocess.run(
            ["bash", str(self.repo / ".github/scripts/bump-upstream"), "--github-output", str(output),
             "--bundle", str(bundle)],
            cwd=self.root, env=environment, text=True, capture_output=True,
        )
        values = dict(line.split("=", 1) for line in output.read_text().splitlines())
        return result, values, bundle

    def tasks(self):
        return self.gradle_log.read_text().splitlines() if self.gradle_log.exists() else []

    def bump_commits(self):
        return self.run_git(self.repo, "log", "--format=%s", f"{self.base}..upstream/mcu").splitlines()

    def test_clean_bump_commits_the_pin_then_the_fixtures(self):
        result, values, bundle = self.bump()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual({"base": self.base, "transform": "success", "goldens": "success"}, values)
        self.assertEqual([
            f"test(mcu): regenerate golden fixtures for upstream {self.new_pin[:7]}",
            f"fix(mcu): update upstream pin to {self.new_pin[:7]}",
        ], self.bump_commits())
        pin_commit = self.run_git(self.repo, "rev-parse", "upstream/mcu~1")
        gitlink = self.run_git(self.repo, "ls-tree", pin_commit, SUBMODULE)
        self.assertIn(f"commit {self.new_pin}", gitlink)
        lock = self.run_git(self.repo, "show", f"{pin_commit}:gradle/mcu-upstream.lock.json")
        self.assertEqual(json.dumps({"upstreamRevision": self.new_pin}, indent=2), lock)
        role = (self.repo / FIXTURES / "UpstreamRoleGoldenData.kt").read_text()
        self.assertIn(f"printMcuRoleGoldens {self.new_pin}", role)
        # The build is warmed before the quiet prints, so no build output reaches a fixture.
        tasks = self.tasks()
        self.assertLess(tasks.index(":mcu-upstream:testClasses"), tasks.index(":mcu-upstream:printMcuRoleGoldens"))
        self.assertLess(tasks.index(":material-color-utilities:generateMcuSources"), tasks.index(":mcu-upstream:testClasses"))
        heads = self.run_git(self.repo, "bundle", "list-heads", str(bundle))
        self.assertIn("refs/heads/upstream/mcu", heads)
        self.assertIn(self.run_git(self.repo, "rev-parse", "upstream/mcu"), heads)

    def test_failed_transform_skips_the_fixtures(self):
        result, values, _ = self.bump(FAIL_TASK=":material-color-utilities:generateMcuSources")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("failure", values["transform"])
        self.assertEqual("skipped", values["goldens"])
        self.assertEqual(1, len(self.bump_commits()))
        self.assertNotIn(":mcu-upstream:testClasses", self.tasks())

    def test_failed_fixture_regeneration_leaves_the_tree_clean(self):
        # Spotless runs after the new fixtures are moved into place, the worst point to fail.
        result, values, _ = self.bump(FAIL_TASK=":material-color-utilities:spotlessApply")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("success", values["transform"])
        self.assertEqual("failure", values["goldens"])
        self.assertEqual(1, len(self.bump_commits()))
        self.assertEqual("original\n", (self.repo / FIXTURES / "UpstreamRoleGoldenData.kt").read_text())
        self.assertEqual("", self.run_git(self.repo, "status", "--porcelain"))

    def test_failed_lock_stops_the_bump(self):
        result, _, bundle = self.bump(FAIL_TASK="candidateMcuUpstreamLock")
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(bundle.exists())

    def test_missing_input_is_rejected(self):
        result, _, _ = self.bump(TITLE="")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Missing TITLE", result.stderr)


if __name__ == "__main__":
    unittest.main()
