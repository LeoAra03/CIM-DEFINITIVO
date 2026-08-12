// ArUco Generator - Port de OpenCV para chev.me/arucogen/ style
// Usa diccionarios generados desde OpenCV 5.0 con Python
// Cada marker: markerSize x markerSize bits (0=white,1=black) + border black

class ArucoGenerator {
  constructor() {
    this.dicts = null;
    this.loaded = false;
  }

  async load() {
    if (this.loaded) return;
    try {
      const res = await fetch('./assets/dict/aruco_dicts.json');
      this.dicts = await res.json();
      this.loaded = true;
      console.log('[OK] Aruco dicts loaded', Object.keys(this.dicts).length);
    } catch (e) {
      console.error('Error loading dicts', e);
      // fallback small dict
      this.dicts = {
        'DICT_4X4_50': {
          markerSize: 4,
          count: 50,
          markers: [[[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]]
        }
      };
    }
  }

  getDictNames() {
    if (!this.dicts) return [];
    return Object.keys(this.dicts).map(k => {
      const d = this.dicts[k];
      const label = k.replace('DICT_', '').replace('_', 'x') + ` (${d.count})`;
      return { key: k, label, markerSize: d.markerSize, count: d.count };
    });
  }

  generateCanvas(dictKey, markerId, sizePx = 300, options = {}) {
    const {
      borderBits = 1,
      margin = 10,
      background = '#FFFFFF',
      invert = false
    } = options;

    if (!this.dicts || !this.dicts[dictKey]) {
      console.error('Dict not found', dictKey);
      return null;
    }
    const dict = this.dicts[dictKey];
    if (markerId < 0 || markerId >= dict.count) {
      console.error('Invalid id', markerId, 'max', dict.count);
      return null;
    }
    const bits = dict.markers[markerId];
    const mSize = dict.markerSize;

    const totalCells = mSize + 2 * borderBits;
    const canvasSize = sizePx + 2 * margin;
    const canvas = document.createElement('canvas');
    canvas.width = canvasSize;
    canvas.height = canvasSize;
    const ctx = canvas.getContext('2d');

    // background
    ctx.fillStyle = background;
    ctx.fillRect(0, 0, canvasSize, canvasSize);

    const cellSize = sizePx / totalCells;
    const offset = margin;

    // Outer border black
    ctx.fillStyle = invert ? '#FFFFFF' : '#000000';
    ctx.fillRect(offset, offset, sizePx, sizePx);

    // Inner white area (where bits + inner border if any? Actually border is black, inner is bits)
    // Draw white background inside border
    ctx.fillStyle = invert ? '#000000' : '#FFFFFF';
    ctx.fillRect(
      offset + borderBits * cellSize,
      offset + borderBits * cellSize,
      mSize * cellSize,
      mSize * cellSize
    );

    // Draw bits
    for (let r = 0; r < mSize; r++) {
      for (let c = 0; c < mSize; c++) {
        const bit = bits[r][c];
        // bit 1 = black, 0 = white (or inverted)
        const isBlack = invert ? bit === 0 : bit === 1;
        ctx.fillStyle = isBlack ? '#000000' : '#FFFFFF';
        if (isBlack) {
          ctx.fillRect(
            offset + (borderBits + c) * cellSize,
            offset + (borderBits + r) * cellSize,
            cellSize,
            cellSize
          );
        }
      }
    }

    // Add label text bottom?
    return canvas;
  }

  generateDataURL(dictKey, markerId, sizePx, options) {
    const canvas = this.generateCanvas(dictKey, markerId, sizePx, options);
    if (!canvas) return null;
    return canvas.toDataURL('image/png');
  }

  // Convert canvas to G-code for laser (raster)
  canvasToGcode(canvas, options = {}) {
    const {
      power = 80,
      speed = 1200,
      threshold = 128,
      pixelSizeMm = 0.1, // each pixel = 0.1mm
    } = options;

    // Get image data
    const ctx = canvas.getContext('2d');
    const w = canvas.width;
    const h = canvas.height;
    const imgData = ctx.getImageData(0, 0, w, h);
    const data = imgData.data;

    let gcode = [];
    gcode.push(`; ArUco Laser G-code generated ${new Date().toISOString()}`);
    gcode.push(`; Power ${power}% Speed ${speed}mm/min`);
    gcode.push(`; Size ${w}x${h} pixels @ ${pixelSizeMm}mm/px`);
    gcode.push('G21 ; mm');
    gcode.push('G90 ; absolute');
    gcode.push('G28 ; home');
    gcode.push(`M3 S0 ; laser off`);
    gcode.push('');

    // Simple raster: for each row, scan
    // We'll generate G-code that moves and fires laser where black pixel
    // This is simplified - real laser might need PWM

    let isLaserOn = false;
    for (let y = 0; y < h; y += 2) { // skip every 2px for speed
      let rowHasBlack = false;
      // Check if row has any black
      for (let x = 0; x < w; x++) {
        const idx = (y * w + x) * 4;
        const gray = (data[idx] + data[idx + 1] + data[idx + 2]) / 3;
        if (gray < threshold) { rowHasBlack = true; break; }
      }
      if (!rowHasBlack) continue;

      const yMm = y * pixelSizeMm;
      gcode.push(`G0 Y${yMm.toFixed(3)}`);

      let xStart = -1;
      for (let x = 0; x < w; x++) {
        const idx = (y * w + x) * 4;
        const gray = (data[idx] + data[idx + 1] + data[idx + 2]) / 3;
        const isBlack = gray < threshold;

        if (isBlack && xStart === -1) {
          xStart = x;
        }
        if ((!isBlack || x === w - 1) && xStart !== -1) {
          let xEnd = isBlack ? x : x - 1;
          const xStartMm = xStart * pixelSizeMm;
          const xEndMm = xEnd * pixelSizeMm;
          if (xEndMm - xStartMm > 0.01) {
            gcode.push(`G0 X${xStartMm.toFixed(3)}`);
            gcode.push(`M3 S${Math.round(power * 10)} ; on`);
            gcode.push(`G1 X${xEndMm.toFixed(3)} F${speed}`);
            gcode.push(`M3 S0 ; off`);
          }
          xStart = -1;
        }
      }
    }

    gcode.push('');
    gcode.push('M5 ; laser off');
    gcode.push('G0 X0 Y0 ; return home');
    gcode.push('; END');

    return gcode.join('\n');
  }

  // Image (uploaded) to G-code
  imageToGcode(imgElement, options) {
    const canvas = document.createElement('canvas');
    const maxW = options.maxWidth || 400;
    let w = imgElement.width;
    let h = imgElement.height;
    if (w > maxW) {
      h = Math.round((maxW / w) * h);
      w = maxW;
    }
    canvas.width = w;
    canvas.height = h;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(imgElement, 0, 0, w, h);
    return {
      canvas,
      gcode: this.canvasToGcode(canvas, options)
    };
  }
}

// Singleton
window.ArucoGen = new ArucoGenerator();
