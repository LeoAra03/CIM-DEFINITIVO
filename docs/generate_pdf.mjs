/**
 * generate_pdf.mjs — Exporta ENTREGA_FINAL_LEONARDO_ARAYA.md a PDF A4 industrial
 * Uso: npm install && npm run pdf
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { marked } from 'marked';
import puppeteer from 'puppeteer-core';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const mdFile = path.join(__dirname, 'ENTREGA_FINAL_LEONARDO_ARAYA.md');
const cssFile = path.join(__dirname, 'styles', 'industrial_pdf.css');
const pdfFile = path.join(__dirname, 'ENTREGA_FINAL_LEONARDO_ARAYA.pdf');

const chromePaths = [
  process.env.CHROME_PATH,
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
].filter(Boolean);

const executablePath = chromePaths.find((p) => fs.existsSync(p));
if (!executablePath) {
  console.error('No se encontró Chrome/Edge. Instale Chrome o defina CHROME_PATH.');
  process.exit(1);
}

const md = fs.readFileSync(mdFile, 'utf8');
const css = fs.readFileSync(cssFile, 'utf8');

// Quitar front matter YAML
const bodyMd = md.replace(/^---[\s\S]*?---\s*/, '');

marked.setOptions({ gfm: true, breaks: false });
const bodyHtml = marked.parse(bodyMd);

const html = `<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8"/>
  <title>Entrega Final CIM v6.0 — Leonardo Araya Labarca</title>
  <style>${css}</style>
  <style>
    pre code { white-space: pre-wrap; word-break: break-word; }
    body img { max-height: 220mm; max-width: 100%; object-fit: contain; }
  </style>
</head>
<body>${bodyHtml}</body>
</html>`;

const htmlFile = path.join(__dirname, '_preview_entrega.html');
fs.writeFileSync(htmlFile, html, 'utf8');
console.log('HTML preview:', htmlFile);

const browser = await puppeteer.launch({
  executablePath,
  headless: true,
  args: ['--no-sandbox', '--disable-setuid-sandbox'],
});

const page = await browser.newPage();
await page.goto(`file:///${htmlFile.replace(/\\/g, '/')}`, { waitUntil: 'networkidle0', timeout: 120000 });
await page.pdf({
  path: pdfFile,
  format: 'A4',
  printBackground: true,
  margin: { top: '25mm', bottom: '28mm', left: '20mm', right: '20mm' },
});

await browser.close();
const stats = fs.statSync(pdfFile);
console.log(`PDF generado: ${pdfFile} (${(stats.size / 1024 / 1024).toFixed(2)} MB)`);
