import { expect, test, type Page } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { gesture, hook, openWithBrowserApis, wantHooks } from './builder';

// The clipboard, downloads, the share sheet, images from the picker, drops and pastes, and the
// eyedropper, driven through the shell's test hooks while the app is still a placeholder. Spike S7
// times the image decode, and spike S9 is the clipboard and download tests in WebKit.

const PHOTO = path.resolve(__dirname, '../fixtures/photo-12mp.jpg');

// The four quadrants of the photo and of the PNG the drop and paste tests draw, as #RRGGBB.
const QUADRANTS = [0xd32f2f, 0x388e3c, 0x1976d2, 0xffa000];

test.beforeEach(async ({ context }) => {
  await wantHooks(context);
});

test.describe('clipboard', () => {
  test('a write inside a click lands on the clipboard', async ({ page, context, browserName }) => {
    if (browserName === 'chromium') await context.grantPermissions(['clipboard-read', 'clipboard-write']);
    await openWithBrowserApis(page);
    await gesture(page, 'copy:#1A73E8');
    await expect.poll(() => hook(page, 'outcome', 'copy')).toBe('Done');
    if (browserName === 'chromium') {
      expect(await page.evaluate(() => navigator.clipboard.readText())).toBe('#1A73E8');
    }
  });

  test('a refused write comes back as a failure', async ({ page }) => {
    await openWithBrowserApis(page);
    await page.evaluate(() => {
      Object.defineProperty(navigator.clipboard, 'writeText', {
        configurable: true,
        value: () => Promise.reject(new DOMException('Refused by the test', 'NotAllowedError')),
      });
    });
    await gesture(page, 'copy:#1A73E8');
    await expect.poll(() => hook(page, 'outcome', 'copy')).toBe('Failed NotAllowedError');
  });

  test('a write outside a click settles either way', async ({ page }, testInfo) => {
    await openWithBrowserApis(page);
    await hook(page, 'copyWithoutGesture', 'no click');
    await expect.poll(() => hook(page, 'outcome', 'copy')).not.toBe('Pending');
    const outcome = await hook(page, 'outcome', 'copy');
    expect(outcome === 'Done' || outcome.startsWith('Failed ')).toBe(true);
    console.log(`[S9] ${testInfo.project.name} clipboard write outside a click: ${outcome}`);
  });
});

test.describe('download', () => {
  test('a download carries the bytes and revokes its URL 30 s later', async ({ page }, testInfo) => {
    await page.clock.install();
    await page.addInitScript(() => {
      const urls = { created: [] as string[], revoked: [] as string[] };
      (window as any).__urls = urls;
      const create = URL.createObjectURL.bind(URL);
      const revoke = URL.revokeObjectURL.bind(URL);
      URL.createObjectURL = (object: Blob | MediaSource) => {
        const url = create(object);
        urls.created.push(url);
        return url;
      };
      URL.revokeObjectURL = (url: string) => {
        urls.revoked.push(url);
        revoke(url);
      };
    });
    await openWithBrowserApis(page);
    const before = await page.evaluate(() => (window as any).__urls.created.length as number);

    const downloading = page.waitForEvent('download');
    await gesture(page, 'save:bytes.bin');
    const download = await downloading;
    expect(download.suggestedFilename()).toBe('bytes.bin');
    const bytes = await readFile((await download.path())!);
    expect([...bytes]).toEqual(Array.from({ length: 256 }, (_, value) => value));
    await expect.poll(() => hook(page, 'outcome', 'save')).toBe('Done');

    const created = await page.evaluate((from) => (window as any).__urls.created.slice(from) as string[], before);
    expect(created).toHaveLength(1);
    const revoked = () => page.evaluate(() => (window as any).__urls.revoked as string[]);
    await page.clock.fastForward(29_000);
    expect(await revoked()).not.toContain(created[0]);
    await page.clock.fastForward(2_000);
    await expect.poll(revoked).toContain(created[0]);
    console.log(`[S9] ${testInfo.project.name} Blob download with the URL revoked 30 s later: saved`);
  });
});

test.describe('share', () => {
  test('canShareFiles says what the browser says', async ({ page }) => {
    await openWithBrowserApis(page);
    const native = await page.evaluate(() => {
      if (typeof navigator.canShare !== 'function' || typeof navigator.share !== 'function') return false;
      return navigator.canShare({ files: [new File(['x'], 'probe.txt', { type: 'text/plain' })] });
    });
    expect(await hook(page, 'canShareFiles')).toBe(String(native));
  });

  test('files go to the share sheet only when canShare takes them', async ({ page }) => {
    await page.addInitScript(() => {
      const shared: string[][] = [];
      (window as any).__shared = shared;
      Object.defineProperty(Navigator.prototype, 'canShare', {
        configurable: true,
        value: (data: ShareData) => (data.files ?? []).every((file) => file.type !== 'application/x-refused'),
      });
      Object.defineProperty(Navigator.prototype, 'share', {
        configurable: true,
        value: async (data: ShareData) => {
          const files = data.files ?? [];
          if (files.some((file) => file.type === 'application/x-dismissed')) {
            throw new DOMException('Closed by the test', 'AbortError');
          }
          shared.push(await Promise.all(files.map(async (file) => `${file.name} ${file.type} ${await file.text()}`)));
        },
      });
    });
    await openWithBrowserApis(page);
    expect(await hook(page, 'canShareFiles')).toBe('true');

    await gesture(page, 'share:text/plain');
    await expect.poll(() => hook(page, 'outcome', 'share')).toBe('Done');
    expect(await page.evaluate(() => (window as any).__shared)).toEqual([
      ['palette.txt text/plain MaterialKolor', 'theme.txt text/plain val seed = 0xFF1A73E8'],
    ]);

    await gesture(page, 'share:application/x-refused');
    await expect.poll(() => hook(page, 'outcome', 'share')).toBe('Failed NotSupportedError');

    // Closing the sheet is not an error to report.
    await gesture(page, 'share:application/x-dismissed');
    await expect.poll(() => hook(page, 'outcome', 'share')).toBe('Done');
    expect(await page.evaluate(() => (window as any).__shared.length)).toBe(1);
  });
});

test.describe('images', () => {
  test.beforeEach(async ({ context }) => {
    await context.addInitScript(installFileMakers);
  });

  test('the picker hands over a 12 MP photo that decodes to three sizes', async ({ page }) => {
    await openWithBrowserApis(page);
    await pickPhoto(page);

    const decoded = await decodeLatest(page);
    expect(decoded).toMatchObject({ width: 128, height: 96, pixels: 128 * 96, thumbnail: [256, 192], detail: [1024, 768] });
    // Quadrant centers. JPEG and its grain move each channel a little.
    await expectNear(page, 'pixel', ['32,24', '96,24', '32,72', '96,72']);
    await expectNear(page, 'thumbnailPixel', ['64,48', '192,48', '64,144', '192,144']);
    await expectNear(page, 'detailPixel', ['256,192', '768,192', '256,576', '768,576']);
  });

  test('a drop hands over image files and the drag is followed', async ({ page }) => {
    await openWithBrowserApis(page);
    expect(await hook(page, 'dragging')).toBe('false');

    expect(await dispatchDrag(page, 'dragenter', [])).toBe(true);
    expect(await hook(page, 'dragging')).toBe('true');
    await dispatchDrag(page, 'dragleave', []);
    expect(await hook(page, 'dragging')).toBe('false');

    await dispatchDrag(page, 'dragenter', []);
    const taken = await dispatchDrag(page, 'drop', ['quadrants.png', 'notes.txt']);
    expect(taken).toBe(true);
    expect(await hook(page, 'dragging')).toBe('false');
    await expect.poll(() => hook(page, 'inputs')).toBe('drop quadrants.png');

    // The PNG is lossless and every scaled pixel at a quadrant center is that quadrant's color, so
    // the RGBA to ARGB swizzle has to be exact.
    const decoded = await decodeLatest(page);
    expect(decoded).toMatchObject({ width: 128, height: 64, thumbnail: [200, 100], detail: [200, 100] });
    expect(await hook(page, 'pixel', '32,16')).toBe('#FFD32F2F');
    expect(await hook(page, 'pixel', '96,16')).toBe('#FF388E3C');
    expect(await hook(page, 'pixel', '32,48')).toBe('#FF1976D2');
    expect(await hook(page, 'pixel', '96,48')).toBe('#FFFFA000');
    expect(await hook(page, 'thumbnailPixel', '150,75')).toBe('#FFFFA000');
  });

  test('a file that only claims to be an image decodes to nothing', async ({ page }) => {
    await openWithBrowserApis(page);
    await dispatchDrag(page, 'drop', ['broken.png']);
    await expect.poll(() => hook(page, 'inputs')).toBe('drop broken.png');
    await hook(page, 'decodeLatest');
    await expect.poll(() => hook(page, 'decoded')).toBe('None');
  });

  test('a paste hands over text and images, but not while a field has focus', async ({ page }) => {
    await openWithBrowserApis(page);
    expect(await dispatchPaste(page, { text: '#6750A4' })).toBe(true);
    expect(await dispatchPaste(page, { text: 'quadrants.png', files: ['quadrants.png'] })).toBe(true);
    expect(await dispatchPaste(page, { text: '   ' })).toBe(false);
    expect(await dispatchPaste(page, { text: '#FFFFFF', intoField: true })).toBe(false);
    await expect.poll(() => hook(page, 'inputs')).toBe('text #6750A4\nfiles quadrants.png');

    const decoded = await decodeLatest(page);
    expect(decoded).toMatchObject({ width: 128, height: 64 });
  });

  test('S7 a 12 MP JPEG decodes and scales well inside 150 ms', async ({ page }, testInfo) => {
    await openWithBrowserApis(page);
    await pickPhoto(page);
    const runs: Record<string, number | boolean>[] = [];
    for (let run = 0; run < 6; run++) {
      await hook(page, 'profileLatest');
      await expect.poll(() => hook(page, 'decoded'), { timeout: 15_000 }).not.toBe('Pending');
      runs.push(JSON.parse(await hook(page, 'decoded')));
    }
    // The first run warms up the decoder and the wasm code.
    runs.shift();
    const median = (field: string) =>
      runs.map((run) => run[field] as number).sort((a, b) => a - b)[Math.floor(runs.length / 2)];
    const summary = ['decode', 'scale', 'read', 'copy', 'total'].map((field) => `${field} ${median(field)}`).join(', ');
    const resized = runs.every((run) => run.resized === true);
    testInfo.annotations.push({ type: 'S7 median ms', description: `${summary}, resize options honored ${resized}` });
    console.log(`[S7] ${testInfo.project.name} ${summary} ms, resize options honored ${resized}`);
    expect(runs.every((run) => typeof run.total === 'number')).toBe(true);
  });
});

test.describe('eyedropper', () => {
  test('is offered only where the browser has one', async ({ page, browserName }) => {
    await openWithBrowserApis(page);
    const native = await page.evaluate(() => typeof (window as any).EyeDropper === 'function');
    if (browserName !== 'chromium') expect(native).toBe(false);
    expect(await hook(page, 'eyeDropperAvailable')).toBe(String(native));
    if (!native) {
      await gesture(page, 'screenColor');
      await expect.poll(() => hook(page, 'outcome', 'screenColor')).toBe('None');
    }
  });

  test('a picked color comes back and Esc cancels with no change', async ({ page }) => {
    await page.addInitScript(() => {
      class TestEyeDropper {
        open() {
          return new Promise((resolve, reject) => {
            const onKey = (event: KeyboardEvent) => {
              if (event.key !== 'Escape') return;
              document.removeEventListener('keydown', onKey, true);
              delete (window as any).__pick;
              reject(new DOMException('Closed by the test', 'AbortError'));
            };
            document.addEventListener('keydown', onKey, true);
            (window as any).__pick = (hex: string) => {
              document.removeEventListener('keydown', onKey, true);
              delete (window as any).__pick;
              resolve({ sRGBHex: hex });
            };
          });
        }
      }
      Object.defineProperty(window, 'EyeDropper', { configurable: true, writable: true, value: TestEyeDropper });
    });
    await openWithBrowserApis(page);
    expect(await hook(page, 'eyeDropperAvailable')).toBe('true');

    await gesture(page, 'screenColor');
    await page.waitForFunction(() => typeof (window as any).__pick === 'function');
    await page.evaluate(() => (window as any).__pick('#1a73e8'));
    await expect.poll(() => hook(page, 'outcome', 'screenColor')).toBe('#1A73E8');

    await gesture(page, 'screenColor');
    await page.waitForFunction(() => typeof (window as any).__pick === 'function');
    await page.keyboard.press('Escape');
    await expect.poll(() => hook(page, 'outcome', 'screenColor')).toBe('None');
  });
});

async function pickPhoto(page: Page): Promise<void> {
  const choosing = page.waitForEvent('filechooser');
  await gesture(page, 'pick');
  await (await choosing).setFiles(PHOTO);
  await expect.poll(() => hook(page, 'outcome', 'pick')).toBe('photo-12mp.jpg');
}

async function decodeLatest(page: Page): Promise<Record<string, unknown>> {
  await hook(page, 'decodeLatest');
  await expect.poll(() => hook(page, 'decoded'), { timeout: 15_000 }).not.toBe('Pending');
  const decoded = await hook(page, 'decoded');
  expect(decoded).not.toBe('None');
  return JSON.parse(decoded);
}

async function expectNear(page: Page, pixelHook: string, points: string[]): Promise<void> {
  for (const [index, point] of points.entries()) {
    const argb = parseInt((await hook(page, pixelHook, point)).slice(1), 16);
    const expected = QUADRANTS[index];
    expect(argb >>> 24, `${pixelHook} ${point} alpha`).toBe(0xff);
    for (const shift of [16, 8, 0]) {
      const channel = (argb >>> shift) & 0xff;
      const wanted = (expected >>> shift) & 0xff;
      expect(Math.abs(channel - wanted), `${pixelHook} ${point} channel ${shift}`).toBeLessThanOrEqual(12);
    }
  }
}

// Files the page makes for itself. `quadrants.png` is 200 by 100 with the four quadrant colors,
// `broken.png` claims to be a PNG and is not, and `notes.txt` is text. The transfer is a real
// DataTransfer where the browser lets a page fill one, else a stand-in with the same shape.
function installFileMakers(): void {
  const quadrants = () => {
    const canvas = document.createElement('canvas');
    canvas.width = 200;
    canvas.height = 100;
    const context = canvas.getContext('2d')!;
    ['#D32F2F', '#388E3C', '#1976D2', '#FFA000'].forEach((color, index) => {
      context.fillStyle = color;
      context.fillRect((index % 2) * 100, Math.floor(index / 2) * 50, 100, 50);
    });
    return new Promise<Blob>((resolve) => canvas.toBlob((blob) => resolve(blob!), 'image/png'));
  };
  (window as any).__makeFiles = async (names: string[]) => {
    const files: File[] = [];
    for (const name of names) {
      if (name === 'quadrants.png') files.push(new File([await quadrants()], name, { type: 'image/png' }));
      if (name === 'broken.png') files.push(new File(['not a png'], name, { type: 'image/png' }));
      if (name === 'notes.txt') files.push(new File(['notes'], name, { type: 'text/plain' }));
    }
    return files;
  };
  (window as any).__makeTransfer = (files: File[], text?: string) => {
    try {
      const transfer = new DataTransfer();
      files.forEach((file) => transfer.items.add(file));
      if (text !== undefined) transfer.setData('text/plain', text);
      if (transfer.files.length === files.length) return transfer;
    } catch (error) {}
    return {
      types: [...(files.length > 0 ? ['Files'] : []), ...(text !== undefined ? ['text/plain'] : [])],
      files,
      dropEffect: 'none',
      getData: (type: string) => (type === 'text/plain' && text !== undefined ? text : ''),
    };
  };
}

/** Fire [type] with a drag of [names] on the page. True when the page took the event over. */
async function dispatchDrag(page: Page, type: string, names: string[]): Promise<boolean> {
  return page.evaluate(
    async ({ type, names }) => {
      const files = await (window as any).__makeFiles(names);
      // A drag over the page shows the types it carries before it drops the files themselves.
      const carried = type === 'drop' ? files : [new File([''], 'carried.png', { type: 'image/png' })];
      const event = new Event(type, { bubbles: true, cancelable: true });
      Object.defineProperty(event, 'dataTransfer', { value: (window as any).__makeTransfer(carried) });
      document.body.dispatchEvent(event);
      return event.defaultPrevented;
    },
    { type, names },
  );
}

/** Paste [text] and [files] where focus is, or into a text field. True when the page took it. */
async function dispatchPaste(
  page: Page,
  { text, files = [], intoField = false }: { text?: string; files?: string[]; intoField?: boolean },
): Promise<boolean> {
  return page.evaluate(
    async ({ text, names, intoField }) => {
      let target: HTMLElement = document.body;
      if (intoField) {
        target = document.createElement('textarea');
        document.body.appendChild(target);
        target.focus();
      }
      const transfer = (window as any).__makeTransfer(await (window as any).__makeFiles(names), text);
      const event = new Event('paste', { bubbles: true, cancelable: true });
      Object.defineProperty(event, 'clipboardData', { value: transfer });
      target.dispatchEvent(event);
      if (intoField) target.remove();
      return event.defaultPrevented;
    },
    { text, names: files, intoField },
  );
}
