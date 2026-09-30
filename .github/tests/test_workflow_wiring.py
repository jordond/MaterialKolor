"""Check that the workflows and the scripts they call agree on output and environment names."""

from pathlib import Path
import re
import unittest


GITHUB = Path(__file__).resolve().parents[1]
WORKFLOWS = [GITHUB / "workflows/upstream.yml", GITHUB / "workflows/ci.yml"]


def steps(workflow):
    """Yields each step as its id, the script its run line calls and the env names it sets."""
    step = None
    in_env = False
    for line in workflow.read_text().splitlines():
        if re.match(r"^      - (name|uses):", line):
            if step:
                yield step
            step = {"id": None, "script": None, "env": set()}
            in_env = False
            continue
        if step is None:
            continue
        if re.match(r"^\S|^  \S|^    \S", line):
            yield step
            step = None
            continue
        if match := re.match(r"^        id: (\S+)", line):
            step["id"] = match.group(1)
        if match := re.search(r"\./\.github/scripts/([\w-]+)", line):
            step["script"] = match.group(1)
        if re.match(r"^        env:", line):
            in_env = True
            continue
        if in_env:
            if match := re.match(r"^          (\w+):", line):
                step["env"].add(match.group(1))
            elif re.match(r"^        \S", line):
                in_env = False
    if step:
        yield step


def workflow_env(workflow):
    names = set()
    in_env = False
    for line in workflow.read_text().splitlines():
        if line == "env:":
            in_env = True
        elif in_env and (match := re.match(r"^  (\w+):", line)):
            names.add(match.group(1))
        elif in_env and re.match(r"^\S", line):
            in_env = False
    return names


def script(name):
    return (GITHUB / "scripts" / name).read_text()


def required_env(source):
    names = set()
    for match in re.finditer(r"for name in ([A-Z_ ]+); do|require ([A-Z_ ]+)$", source, re.MULTILINE):
        names.update((match.group(1) or match.group(2)).split())
    return names


class WorkflowWiringTest(unittest.TestCase):
    def test_step_outputs_are_written_by_their_scripts(self):
        for workflow in WORKFLOWS:
            text = workflow.read_text()
            scripts = {step["id"]: step["script"] for step in steps(workflow) if step["id"]}
            for step_id, key in set(re.findall(r"steps\.(\w+)\.outputs\.(\w+)", text)):
                with self.subTest(workflow=workflow.name, step=step_id, key=key):
                    self.assertIn(step_id, scripts)
                    self.assertIsNotNone(scripts[step_id])
                    # A key follows a word boundary or a printf `\n` escape.
                    self.assertRegex(script(scripts[step_id]), rf"(?:(?<!\w)|(?<=\\n)){key}=")

    def test_prepare_outputs_used_by_publish_are_declared(self):
        text = (GITHUB / "workflows/upstream.yml").read_text()
        block = re.search(r"^    outputs:\n((?:      .*\n)+)", text, re.MULTILINE).group(1)
        declared = set(re.findall(r"^      (\w+):", block, re.MULTILINE))
        used = set(re.findall(r"needs\.prepare\.outputs\.(\w+)", text))
        self.assertLessEqual(used, declared)

    def test_scripts_receive_the_environment_they_require(self):
        for workflow in WORKFLOWS:
            shared = workflow_env(workflow)
            for step in steps(workflow):
                if not step["script"]:
                    continue
                with self.subTest(workflow=workflow.name, script=step["script"]):
                    required = required_env(script(step["script"]))
                    self.assertLessEqual(required, step["env"] | shared)

    def test_expressions_do_not_select_a_falsy_value(self):
        # `cond && 0 || 1` always yields 1, since 0, false and '' are falsy in expressions.
        for workflow in WORKFLOWS:
            with self.subTest(workflow=workflow.name):
                self.assertNotRegex(workflow.read_text(), r"&&\s*(0|false|''|\"\")\s*\|\|")


if __name__ == "__main__":
    unittest.main()
