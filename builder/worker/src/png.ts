// A palette PNG writer, just enough for the theme card. Deflate is the runtime's CompressionStream,
// whose zlib output is exactly what an IDAT chunk holds.

const SIGNATURE = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
const BIT_DEPTH = 8;
const COLOR_TYPE_PALETTE = 3;

const CRC_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    table[n] = c >>> 0;
  }
  return table;
})();

function crc32(bytes: Uint8Array, start: number, end: number): number {
  let crc = 0xffffffff;
  for (let index = start; index < end; index++) crc = CRC_TABLE[(crc ^ bytes[index]!) & 0xff]! ^ (crc >>> 8);
  return (crc ^ 0xffffffff) >>> 0;
}

/**
 * The PNG for an 8-bit palette image. [scanlines] holds every row with its filter byte in front,
 * and [palette] three bytes a color.
 */
export async function encodePng(
  width: number,
  height: number,
  palette: Uint8Array,
  scanlines: Uint8Array,
): Promise<Uint8Array> {
  const stream = new Response(scanlines).body!.pipeThrough(new CompressionStream('deflate'));
  const idat = new Uint8Array(await new Response(stream).arrayBuffer());
  const header = new Uint8Array(13);
  const view = new DataView(header.buffer);
  view.setUint32(0, width);
  view.setUint32(4, height);
  header.set([BIT_DEPTH, COLOR_TYPE_PALETTE, 0, 0, 0], 8);

  const chunks: [string, Uint8Array][] = [
    ['IHDR', header],
    ['PLTE', palette],
    ['IDAT', idat],
    ['IEND', new Uint8Array(0)],
  ];
  const size = SIGNATURE.length + chunks.reduce((sum, [, data]) => sum + 12 + data.length, 0);
  const png = new Uint8Array(size);
  const out = new DataView(png.buffer);
  png.set(SIGNATURE, 0);
  let position = SIGNATURE.length;
  for (const [type, data] of chunks) {
    out.setUint32(position, data.length);
    for (let index = 0; index < 4; index++) png[position + 4 + index] = type.charCodeAt(index);
    png.set(data, position + 8);
    out.setUint32(position + 8 + data.length, crc32(png, position + 4, position + 8 + data.length));
    position += 12 + data.length;
  }
  return png;
}
