// Draws `photo-12mp.jpg`, a 4000 by 3000 JPEG the image specs decode. Run it with `npm run fixtures`.
//
// Four flat quadrants, so a spec can check the colors that come back, under a fixed grain that makes
// the file about as costly to decode as a phone photo. The grain comes from a seeded generator, so
// every run writes the same pixels.
import { chromium } from '@playwright/test';
import { writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const WIDTH = 4000;
const HEIGHT = 3000;
const QUALITY = 0.85;

const here = path.dirname(fileURLToPath(import.meta.url));
const browser = await chromium.launch();
try {
  const page = await browser.newPage();
  const encoded = await page.evaluate(
    async ({ width, height, quality }) => {
      const quadrants = [
        [0xd3, 0x2f, 0x2f],
        [0x38, 0x8e, 0x3c],
        [0x19, 0x76, 0xd2],
        [0xff, 0xa0, 0x00],
      ];
      const canvas = new OffscreenCanvas(width, height);
      const context = canvas.getContext('2d');
      const image = context.createImageData(width, height);
      const data = image.data;
      let seed = 0x5eed;
      const grain = () => {
        seed = (seed + 0x6d2b79f5) | 0;
        let value = Math.imul(seed ^ (seed >>> 15), 1 | seed);
        value ^= value + Math.imul(value ^ (value >>> 7), 61 | value);
        return (((value ^ (value >>> 14)) >>> 0) % 21) - 10;
      };
      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const color = quadrants[(y < height / 2 ? 0 : 2) + (x < width / 2 ? 0 : 1)];
          const offset = (y * width + x) * 4;
          const shift = grain();
          data[offset] = color[0] + shift;
          data[offset + 1] = color[1] + shift;
          data[offset + 2] = color[2] + shift;
          data[offset + 3] = 255;
        }
      }
      context.putImageData(image, 0, 0);
      const blob = await canvas.convertToBlob({ type: 'image/jpeg', quality });
      const bytes = new Uint8Array(await blob.arrayBuffer());
      let binary = '';
      for (let index = 0; index < bytes.length; index += 0x8000) {
        binary += String.fromCharCode(...bytes.subarray(index, index + 0x8000));
      }
      return btoa(binary);
    },
    { width: WIDTH, height: HEIGHT, quality: QUALITY },
  );
  const bytes = Buffer.from(encoded, 'base64');
  await writeFile(path.join(here, 'photo-12mp.jpg'), bytes);
  console.log(`photo-12mp.jpg ${WIDTH}x${HEIGHT}, ${(bytes.length / 1024 / 1024).toFixed(2)} MB`);
} finally {
  await browser.close();
}
