import { existsSync } from 'node:fs';
import path from 'node:path';
import { serveSite } from './serve';

/**
 * Serve the built site for the whole run and hand its address to the specs as `MK_E2E_BASE_URL`.
 *
 * `MK_E2E_SITE` points at another build, relative to this folder or absolute.
 */
export default async function globalSetup(): Promise<() => Promise<void>> {
  // b-213
  // The assembled site, laid out the way the host serves it.
  const root = path.resolve(__dirname, process.env.MK_E2E_SITE ?? '../web/build/site');
  if (!existsSync(path.join(root, 'index.html'))) {
    throw new Error(`No built site at ${root}. Run ./gradlew :builder:web:assembleSite first.`);
  }
  const site = await serveSite(root);
  process.env.MK_E2E_BASE_URL = site.url;
  return site.close;
}
