// Runs in the head, before the body is parsed and before anything paints. It checks the browser can
// run the builder, colors the splash and loads the app. It reads mk:splash and the address and never
// writes storage. Kept to plain ES2015 so an old browser still reaches the unsupported page. It adds
// no preloads. WebKit fetches an as=fetch preload with an Origin header and the real request without
// one, so each file would download twice there, and the glue's tag goes in during this same task.
(() => {
  // What the default document writes to mk:splash, and its seed, for a first visit. The shell spec
  // holds the two colors to what the app writes.
  const DEFAULT_LIGHT = 0xfff8f6;
  const DEFAULT_DARK = 0x1a110f;
  const DEFAULT_SEED = 0xd9653b;

  // The smallest module that uses what Kotlin/Wasm needs. The struct type needs WasmGC, and the
  // function body is a try with catch_all, the legacy exception handling Kotlin/Wasm emits.
  const GATE_MODULE = new Uint8Array([
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // magic and version 1
    0x01, 0x08, 0x02, 0x5f, 0x01, 0x78, 0x00, 0x60, 0x00, 0x00, // types, a struct of one i8 and a func with no params
    0x03, 0x02, 0x01, 0x01, // one function of the func type
    0x0a, 0x08, 0x01, 0x06, 0x00, 0x06, 0x40, 0x19, 0x0b, 0x0b, // its body, try catch_all end
  ]);

  const root = document.documentElement;
  const linkSeed = readLinkSeed();

  if (!supported()) {
    root.classList.add('mk-unsupported');
    whenParsed(() => {
      if (linkSeed === null) return;
      document.querySelector('#unsupported-seed .mk-hex').textContent = hex(linkSeed);
      document.getElementById('unsupported-seed').hidden = false;
    });
    return;
  }

  const assets = JSON.parse(document.getElementById('mk-assets').textContent);
  catchErrors(assets.glue.split('/').pop());
  paintSplash();

  const glue = document.createElement('script');
  glue.src = assets.glue;
  document.head.appendChild(glue);

  /** Whether this browser has WasmGC, legacy exception handling and WebGL 2. */
  function supported() {
    try {
      if (typeof WebAssembly !== 'object' || !WebAssembly.validate(GATE_MODULE)) return false;
      const gl = document.createElement('canvas').getContext('webgl2');
      if (!gl) return false;
      const context = gl.getExtension('WEBGL_lose_context');
      if (context) context.loseContext();
      return true;
    } catch (error) {
      return false;
    }
  }

  /**
   * Colors the splash. The chrome comes from mk:splash, one color per scheme that the stylesheet
   * picks between. The poster takes the seed of a theme link, else the seed mk:splash keeps, else
   * stays neutral. With no mk:splash at all the default document's colors stand in.
   */
  function paintSplash() {
    const stored = readSplash();
    const light = stored ? color(stored.light) : null;
    const dark = stored ? color(stored.dark) : null;
    const storedSeed = stored ? color(stored.seed) : DEFAULT_SEED;
    const seed = linkSeed !== null ? linkSeed : storedSeed;
    root.style.setProperty('--mk-light', hex(light !== null ? light : DEFAULT_LIGHT));
    root.style.setProperty('--mk-dark', hex(dark !== null ? dark : DEFAULT_DARK));
    if (seed === null) return;
    root.style.setProperty('--mk-seed', hex(seed));
    root.style.setProperty('--mk-seed-ink', luminance(seed) > 0.179 ? '#000' : '#fff');
    root.style.setProperty('--mk-hex', JSON.stringify(hex(seed)));
  }

  /** mk:splash as an object, an empty one when it holds something else, or null when it is not there. */
  function readSplash() {
    let text = null;
    try {
      text = localStorage.getItem('mk:splash');
    } catch (error) {
      return null;
    }
    if (text === null) return null;
    try {
      const value = JSON.parse(text);
      return value !== null && typeof value === 'object' ? value : {};
    } catch (error) {
      return {};
    }
  }

  /** The seed of a theme link, `/t/<code>`. The code is base64url and bytes 1 to 3 are the seed. */
  function readLinkSeed() {
    const match = /^\/t\/([A-Za-z0-9_-]{6,})/.exec(location.pathname);
    if (!match) return null;
    try {
      const bytes = atob(match[1].slice(0, 8).replace(/-/g, '+').replace(/_/g, '/'));
      return (bytes.charCodeAt(1) << 16) | (bytes.charCodeAt(2) << 8) | bytes.charCodeAt(3);
    } catch (error) {
      return null;
    }
  }

  /** The RGB of a signed 32 bit ARGB integer, or null for anything else. */
  function color(value) {
    return Number.isInteger(value) ? value & 0xffffff : null;
  }

  function hex(rgb) {
    return '#' + ('00000' + rgb.toString(16).toUpperCase()).slice(-6);
  }

  /** WCAG relative luminance, so the hex on the poster is black or white, whichever reads better. */
  function luminance(rgb) {
    const channel = (shift) => {
      const value = ((rgb >> shift) & 0xff) / 255;
      return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    };
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0);
  }

  function whenParsed(run) {
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', run);
    else run();
  }

  /**
   * Shows the error overlay on the first uncaught error or rejection (F-38). The details say what
   * failed, where, in which browser and which build. They leave out the address, since a share code
   * can carry a project name.
   */
  function catchErrors(build) {
    let shown = false;
    const show = (error, fallback) => {
      if (shown) return;
      shown = true;
      const details = describe(error, fallback, build);
      whenParsed(() => showOverlay(details));
    };
    window.addEventListener('error', (event) => {
      // Chrome and Firefox report a ResizeObserver that settles over two frames. It is harmless.
      if (/ResizeObserver loop/.test(event.message)) return;
      // A script from another origin without CORS reports this and nothing else.
      if (event.message === 'Script error.' && !event.error) return;
      show(event.error, event.message);
    });
    window.addEventListener('unhandledrejection', (event) => show(event.reason, 'Unhandled rejection'));
  }

  function describe(error, fallback, build) {
    let message = fallback;
    let stack = '';
    try {
      if (error instanceof Error) message = error.name + ': ' + error.message;
      else if (error !== undefined && error !== null) message = String(error);
      if (error && typeof error.stack === 'string') stack = error.stack;
    } catch (ignored) {
      // An object whose toString throws still gets the fallback.
    }
    return [
      'MaterialKolor Builder error',
      'Build: ' + build,
      'User agent: ' + navigator.userAgent,
      'Message: ' + message,
      'Stack:',
      stack,
    ].join('\n');
  }

  function showOverlay(details) {
    const overlay = document.getElementById('error-overlay');
    const reload = document.getElementById('error-reload');
    const status = document.getElementById('error-status');
    const app = document.getElementById('app');
    if (app) app.inert = true;
    reload.addEventListener('click', () => location.reload());
    document.getElementById('error-copy').addEventListener('click', () => {
      const showDetails = () => {
        const area = document.getElementById('error-details');
        area.value = details;
        area.hidden = false;
        area.select();
        status.textContent = 'Copy the details below.';
      };
      if (!navigator.clipboard) return showDetails();
      navigator.clipboard.writeText(details).then(() => {
        status.textContent = 'Details copied.';
      }, showDetails);
    });
    overlay.hidden = false;
    reload.focus();
  }
})();
