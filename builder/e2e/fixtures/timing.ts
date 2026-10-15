// The only places a spec may wait on the clock. Each is part of what the test does, how long a
// finger stays down or how long the network has to stay quiet, never a wait for the page to catch
// up. lint.mjs allows `setTimeout` sleeps in these functions by name and nowhere else.

/** How long a finger rests between two touch events, the pace of a thumb that drags. */
const BETWEEN_TOUCHES_MS = 50;

/** How often the network is looked at while it has to stay quiet. */
const QUIET_POLL_MS = 250;

/** How often a local server that is starting is asked whether it is up. */
const SERVER_POLL_MS = 250;

/** Keeps a finger down for [ms], how long the gesture lasts. */
export function holdFingerDown(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** Leaves the gap a moving finger leaves between two touch events. */
export function holdBetweenTouches(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, BETWEEN_TOUCHES_MS));
}

/**
 * Waits until [seen], a list the page's requests or responses are pushed onto, has not grown for
 * [quietMs], or [capMs] have passed.
 */
export async function networkQuietFor(seen: unknown[], quietMs = 2_000, capMs = 15_000): Promise<void> {
  const deadline = Date.now() + capMs;
  let count = seen.length;
  let quietSince = Date.now();
  while (Date.now() < deadline && Date.now() - quietSince < quietMs) {
    await new Promise((resolve) => setTimeout(resolve, QUIET_POLL_MS));
    if (seen.length !== count) {
      count = seen.length;
      quietSince = Date.now();
    }
  }
}

/** Leaves a gap before a starting server is asked again whether it is up. */
export function pauseBeforeServerCheck(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, SERVER_POLL_MS));
}
