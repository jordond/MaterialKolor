import type { Page } from '@playwright/test';

// Files a page makes for itself and the drops and pastes that carry them, copied from
// `browser-apis.spec.ts` so the image seeding specs stand on their own.

/**
 * Hang `__makeFiles` and `__makeTransfer` on the page. `quadrants.png` is 200 by 100 with a red,
 * green, blue and amber quadrant, and `notes.txt` is text. The transfer is a real DataTransfer where
 * the browser lets a page fill one, else a stand-in with the same shape. Install it with
 * `context.addInitScript(installFileMakers)`.
 */
export function installFileMakers(): void {
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
export async function dispatchDrag(page: Page, type: string, names: string[]): Promise<boolean> {
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

/** Paste [files] on the page while nothing editable has focus. True when the page took it. */
export async function dispatchPaste(page: Page, files: string[]): Promise<boolean> {
  return page.evaluate(async (names) => {
    const transfer = (window as any).__makeTransfer(await (window as any).__makeFiles(names), names.join(' '));
    const event = new Event('paste', { bubbles: true, cancelable: true, composed: true });
    Object.defineProperty(event, 'clipboardData', { value: transfer });
    document.body.dispatchEvent(event);
    return event.defaultPrevented;
  }, files);
}
