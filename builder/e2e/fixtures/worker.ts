import { spawn, type ChildProcess } from 'node:child_process';
import { existsSync } from 'node:fs';
import { createServer } from 'node:net';
import path from 'node:path';

// The Worker in front of the site, run by `wrangler dev` for the specs that need what only it
// serves, the per-theme link meta of `/t/<code>`. Everything else runs on the global setup's server.
// It serves the site `wrangler.jsonc` names, `builder/apps/web/build/site`.

const WORKER = path.resolve(__dirname, '../../worker');

const WRANGLER = path.join(WORKER, 'node_modules/.bin/wrangler');

/** Why the Worker cannot run here, or null when it can. */
export function workerMissing(): string | null {
  return existsSync(WRANGLER) ? null : `wrangler is not installed, run npm --prefix builder/worker ci`;
}

export interface Worker {
  url: string;
  stop: () => Promise<void>;
}

/** Starts `wrangler dev` on free ports and waits until it serves the site's root. */
export async function startWorker(): Promise<Worker> {
  const port = await freePort();
  const inspector = await freePort();
  const child = spawn(
    WRANGLER,
    ['dev', '--ip', '127.0.0.1', '--port', String(port), '--inspector-port', String(inspector), '--log-level', 'warn'],
    {
      cwd: WORKER,
      env: { ...process.env, WRANGLER_SEND_METRICS: 'false', CI: 'true' },
      stdio: ['ignore', 'pipe', 'pipe'],
    },
  );
  let output = '';
  child.stdout?.on('data', (chunk) => (output += chunk));
  child.stderr?.on('data', (chunk) => (output += chunk));
  const url = `http://127.0.0.1:${port}`;
  const deadline = Date.now() + 60_000;
  while (!(await answers(`${url}/robots.txt`))) {
    if (child.exitCode !== null || Date.now() > deadline) {
      await stop(child);
      throw new Error(`wrangler dev did not come up:\n${output}`);
    }
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  return { url, stop: () => stop(child) };
}

async function answers(url: string): Promise<boolean> {
  try {
    return (await fetch(url)).ok;
  } catch {
    return false;
  }
}

function stop(child: ChildProcess): Promise<void> {
  if (child.exitCode !== null) return Promise.resolve();
  return new Promise((resolve) => {
    child.once('exit', () => resolve());
    child.kill('SIGTERM');
  });
}

function freePort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const server = createServer();
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const address = server.address();
      server.close(() => (typeof address === 'object' && address ? resolve(address.port) : reject(new Error('No port'))));
    });
  });
}
