"""Exercise the CI lane selection against a disposable Git repository."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "scripts/detect-engine-changes"
REAL_GIT = shutil.which("git")


class DetectEngineChangesTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.repo = Path(self.temporary.name) / "repo"
        self.repo.mkdir()
        self.git("init", "--quiet", "-b", "next")
        self.commit("README.md")
        self.base = self.git("rev-parse", "HEAD")

    def git(self, *args):
        return subprocess.run(
            [REAL_GIT, *args], cwd=self.repo, text=True, capture_output=True, check=True
        ).stdout.strip()

    def commit(self, *paths):
        for path in paths:
            file = self.repo / path
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text(path + "\n")
        self.git("add", "-A")
        self.git("-c", "user.email=fixture@example.invalid", "-c", "user.name=Fixture", "commit",
                 "--quiet", "--allow-empty", "-m", "change")

    def detect(self, event="pull_request", base=None):
        output = self.repo.parent / "github-output"
        output.write_text("")
        environment = {**os.environ, "EVENT_NAME": event, "BASE_SHA": base or self.base}
        result = subprocess.run(
            ["bash", str(SCRIPT), "--github-output", str(output)], cwd=self.repo, env=environment,
            text=True, capture_output=True,
        )
        self.assertEqual(0, result.returncode, result.stderr)
        return dict(line.split("=", 1) for line in output.read_text().splitlines())

    def assertLanes(self, engine, apple, *paths):
        self.commit(*paths)
        self.assertEqual({"engine": engine, "apple": apple}, self.detect())

    def test_other_events_run_the_full_matrix(self):
        for event in ["push", "workflow_dispatch"]:
            self.assertEqual({"engine": "true", "apple": "true"}, self.detect(event=event))

    def test_documentation_skips_both_lanes(self):
        self.assertLanes("false", "false", "docs/guide.md", "builder/src/App.kt")

    def test_build_machinery_runs_publication_only(self):
        self.assertLanes("true", "false", "build-logic/convention/Plugin.kt")

    def test_new_library_module_runs_publication_only(self):
        self.assertLanes("true", "false", "material-kolor-unstyled/src/commonMain/Theme.kt")

    def test_upstream_scripts_run_publication_only(self):
        self.assertLanes("true", "false", ".github/scripts/plan-upstream")

    def test_engine_runs_both_lanes(self):
        self.assertLanes("true", "true", "material-color-utilities/src/commonMain/Hct.kt")

    def test_upstream_pin_runs_both_lanes(self):
        self.assertLanes("true", "true", "gradle/mcu-upstream.lock.json")

    def test_this_script_runs_both_lanes(self):
        self.assertLanes("true", "true", ".github/scripts/detect-engine-changes")

    def test_only_changes_since_the_base_count(self):
        self.commit("tools/mcu-source-transformer/Rule.kt")
        moved_base = self.git("rev-parse", "HEAD")
        self.commit("docs/guide.md")
        self.assertEqual({"engine": "false", "apple": "false"}, self.detect(base=moved_base))

    def test_pull_request_without_a_base_is_rejected(self):
        output = self.repo.parent / "github-output"
        result = subprocess.run(
            ["bash", str(SCRIPT), "--github-output", str(output)], cwd=self.repo,
            env={**os.environ, "EVENT_NAME": "pull_request", "BASE_SHA": ""}, text=True,
            capture_output=True,
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Usage:", result.stderr)


if __name__ == "__main__":
    unittest.main()
