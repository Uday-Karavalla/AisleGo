// One-off placeholder icon generator for the PWA manifest.
// Draws a simple shopping-bag glyph on a brand-green square using raw pixel
// math + Node's built-in zlib (no image-library dependency).
// TODO: replace the output PNGs with real branded artwork before launch.
import { deflateSync } from 'node:zlib'
import { writeFileSync, mkdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const outDir = path.join(__dirname, '..', 'public', 'icons')
mkdirSync(outDir, { recursive: true })

const BRAND = [0x14, 0x95, 0x66] // #149566
const WHITE = [0xff, 0xff, 0xff]

function crc32(buf) {
  let c
  const table = crc32.table || (crc32.table = (() => {
    const t = new Uint32Array(256)
    for (let n = 0; n < 256; n++) {
      c = n
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1
      t[n] = c >>> 0
    }
    return t
  })())
  let crc = 0xffffffff
  for (let i = 0; i < buf.length; i++) crc = table[(crc ^ buf[i]) & 0xff] ^ (crc >>> 8)
  return (crc ^ 0xffffffff) >>> 0
}

function chunk(type, data) {
  const len = Buffer.alloc(4)
  len.writeUInt32BE(data.length, 0)
  const typeBuf = Buffer.from(type, 'ascii')
  const crcBuf = Buffer.alloc(4)
  crcBuf.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0)
  return Buffer.concat([len, typeBuf, data, crcBuf])
}

function buildPng(size, pixelFn) {
  const raw = Buffer.alloc(size * (1 + size * 4))
  for (let y = 0; y < size; y++) {
    const rowStart = y * (1 + size * 4)
    raw[rowStart] = 0 // no filter
    for (let x = 0; x < size; x++) {
      const [r, g, b, a] = pixelFn(x, y)
      const off = rowStart + 1 + x * 4
      raw[off] = r
      raw[off + 1] = g
      raw[off + 2] = b
      raw[off + 3] = a
    }
  }
  const ihdr = Buffer.alloc(13)
  ihdr.writeUInt32BE(size, 0)
  ihdr.writeUInt32BE(size, 4)
  ihdr[8] = 8 // bit depth
  ihdr[9] = 6 // color type RGBA
  ihdr[10] = 0
  ihdr[11] = 0
  ihdr[12] = 0

  const idat = deflateSync(raw)
  const signature = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])
  return Buffer.concat([
    signature,
    chunk('IHDR', ihdr),
    chunk('IDAT', idat),
    chunk('IEND', Buffer.alloc(0)),
  ])
}

// Draws a rounded square background + simple shopping-bag glyph.
function makeIcon(size, { bleed = 0 } = {}) {
  const pad = size * bleed
  const radius = size * 0.22
  return buildPng(size, (x, y) => {
    const inRoundedSquare = (() => {
      const rx = Math.min(x, size - 1 - x)
      const ry = Math.min(y, size - 1 - y)
      if (rx >= radius || ry >= radius) return true
      const dx = radius - rx
      const dy = radius - ry
      return dx * dx + dy * dy <= radius * radius
    })()

    if (bleed === 0 && !inRoundedSquare) return [0, 0, 0, 0]

    const cx = size / 2
    const cy = size / 2 + size * 0.03
    const bagHalfW = size * 0.2
    const bagTop = cy - size * 0.14
    const bagBottom = cy + size * 0.2

    // Bag body: trapezoid, narrower at top.
    if (y >= bagTop && y <= bagBottom) {
      const t = (y - bagTop) / (bagBottom - bagTop)
      const halfW = bagHalfW * (0.78 + 0.22 * t)
      if (Math.abs(x - cx) <= halfW) {
        return [...WHITE, 255]
      }
    }

    // Bag handle: two arcs.
    const handleY = bagTop
    const handleR = size * 0.09
    const leftHandleCx = cx - bagHalfW * 0.55
    const rightHandleCx = cx + bagHalfW * 0.55
    const handleThickness = size * 0.028
    for (const hcx of [leftHandleCx, rightHandleCx]) {
      const dx = x - hcx
      const dy = y - handleY
      if (dy <= 0) {
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist <= handleR && dist >= handleR - handleThickness) {
          return [...WHITE, 255]
        }
      }
    }

    if (pad > 0 && (x < pad || y < pad || x > size - pad || y > size - pad)) {
      return [...BRAND, 255]
    }

    return [...BRAND, 255]
  })
}

writeFileSync(path.join(outDir, 'icon-192.png'), makeIcon(192))
writeFileSync(path.join(outDir, 'icon-512.png'), makeIcon(512))
// Maskable icons need extra safe-area padding since OSes crop to a circle/shape.
writeFileSync(path.join(outDir, 'icon-maskable-512.png'), makeIcon(512, { bleed: 0.1 }))

console.log('Generated placeholder PWA icons in', outDir)
