import { inflateRawSync } from 'node:zlib';

// Just enough of a zip reader for the export spec: the central directory's entries, stored or
// deflated, read back to their bytes.

/** Every file in the zip [bytes], by its path, in the order the central directory lists them. */
export function readZip(bytes: Buffer): Map<string, Buffer> {
  const end = bytes.lastIndexOf(Buffer.from([0x50, 0x4b, 0x05, 0x06]));
  if (end < 0) throw new Error('Not a zip, no end of central directory');
  const count = bytes.readUInt16LE(end + 10);
  let entry = bytes.readUInt32LE(end + 16);
  const files = new Map<string, Buffer>();
  for (let index = 0; index < count; index++) {
    if (bytes.readUInt32LE(entry) !== 0x02014b50) throw new Error(`No central directory entry at ${entry}`);
    const method = bytes.readUInt16LE(entry + 10);
    const compressed = bytes.readUInt32LE(entry + 20);
    const nameLength = bytes.readUInt16LE(entry + 28);
    const extraLength = bytes.readUInt16LE(entry + 30);
    const commentLength = bytes.readUInt16LE(entry + 32);
    const local = bytes.readUInt32LE(entry + 42);
    const name = bytes.subarray(entry + 46, entry + 46 + nameLength).toString('utf8');
    const start = local + 30 + bytes.readUInt16LE(local + 26) + bytes.readUInt16LE(local + 28);
    const data = bytes.subarray(start, start + compressed);
    if (method !== 0 && method !== 8) throw new Error(`${name} uses compression method ${method}`);
    files.set(name, method === 8 ? inflateRawSync(data) : Buffer.from(data));
    entry += 46 + nameLength + extraLength + commentLength;
  }
  return files;
}
