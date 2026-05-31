import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const pdf = path.join(path.dirname(fileURLToPath(import.meta.url)), 'ENTREGA_FINAL_LEONARDO_ARAYA.pdf');
const buf = fs.readFileSync(pdf);
const text = buf.toString('latin1');
const typePage = (text.match(/\/Type\s*\/Page\b/g) || []).length;
const countMatch = text.match(/\/Count\s+(\d+)/g);
const maxCount = countMatch
  ? Math.max(...countMatch.map((m) => parseInt(m.replace(/\D/g, ''), 10)))
  : 0;
console.log(JSON.stringify({ bytes: buf.length, typePageObjects: typePage, maxCountField: maxCount }));
