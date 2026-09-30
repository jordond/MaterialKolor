import { appendFileSync, existsSync, readFileSync } from 'node:fs';

// Lists the tests in results.json, the JSON reporter's output, that failed and then passed on their
// retry. A flaky test passes the run, so the CI job's summary is where it shows.

const results = 'results.json';
if (!existsSync(results)) process.exit(0);

const flaky = [];
const walk = (suite) => {
  for (const spec of suite.specs ?? []) {
    for (const test of spec.tests ?? []) {
      if (test.status === 'flaky') flaky.push(`${spec.file}:${spec.line} ${spec.title} (${test.projectName})`);
    }
  }
  (suite.suites ?? []).forEach(walk);
};
(JSON.parse(readFileSync(results, 'utf8')).suites ?? []).forEach(walk);

const lines = flaky.length === 0 ? ['None.'] : flaky.map((test) => `- ${test}`);
const text = `### Flaky browser tests\n\n${lines.join('\n')}\n`;
if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, text);
else console.log(text);
