#!/usr/bin/env node
// Generates Kotlin ImageVector definitions from Lucide SVG icons (ISC license).
//
// Usage:
//   npm pack lucide-static            # or point LUCIDE_DIR at an extracted package
//   node native/tools/gen-icons.mjs <lucide-icons-dir>
//
// Output: native/composeApp/src/commonMain/kotlin/fr/paulbr/nookmind/core/designsystem/icons/LucideIcons.kt
//
// The web app uses lucide-react; keeping the exact same glyphs (24x24 viewport, 2px round stroke)
// is what makes the native app look identical.

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const iconsDir = process.argv[2];
if (!iconsDir) {
  console.error('usage: gen-icons.mjs <lucide-static/icons dir>');
  process.exit(1);
}

// Kotlin name -> lucide file name
const ICONS = {
  Search: 'search',
  X: 'x',
  BookOpen: 'book-open',
  CheckCircle2: 'circle-check',
  Film: 'film',
  Bookmark: 'bookmark',
  CheckCheck: 'check-check',
  Tv: 'tv',
  Eye: 'eye',
  EyeOff: 'eye-off',
  Play: 'play',
  Clock: 'clock',
  Clock3: 'clock-3',
  CalendarDays: 'calendar-days',
  Check: 'check',
  Star: 'star',
  ChevronDown: 'chevron-down',
  ChevronUp: 'chevron-up',
  ChevronLeft: 'chevron-left',
  ChevronRight: 'chevron-right',
  LayoutGrid: 'layout-grid',
  List: 'list',
  Plus: 'plus',
  Trash2: 'trash-2',
  FolderOpen: 'folder-open',
  BarChart2: 'chart-column',
  Pencil: 'pencil',
  FolderPlus: 'folder-plus',
  FolderMinus: 'folder-minus',
  ArrowLeftRight: 'arrow-left-right',
  ArrowLeft: 'arrow-left',
  Loader2: 'loader-circle',
  User: 'user',
  Sun: 'sun',
  Moon: 'moon',
  Monitor: 'monitor',
  RefreshCw: 'refresh-cw',
  RotateCcw: 'rotate-ccw',
  Bell: 'bell',
  BellOff: 'bell-off',
  Clapperboard: 'clapperboard',
  Send: 'send',
  GripVertical: 'grip-vertical',
  Library: 'library',
  Compass: 'compass',
  Settings: 'settings',
  Flame: 'flame',
  AlertTriangle: 'triangle-alert',
  Download: 'download',
  Share: 'share',
  Tag: 'tag',
  Drama: 'drama',
  Satellite: 'satellite',
  ExternalLink: 'external-link',
};

function num(v) {
  const n = parseFloat(v);
  return Number.isFinite(n) ? n : 0;
}

// Convert each SVG primitive to path data (SVG path mini-language), so everything goes through
// PathParser on the Compose side.
function elementToPath(tag, attrs) {
  switch (tag) {
    case 'path':
      return attrs.d;
    case 'circle': {
      const cx = num(attrs.cx), cy = num(attrs.cy), r = num(attrs.r);
      return `M ${cx - r} ${cy} A ${r} ${r} 0 1 0 ${cx + r} ${cy} A ${r} ${r} 0 1 0 ${cx - r} ${cy} Z`;
    }
    case 'ellipse': {
      const cx = num(attrs.cx), cy = num(attrs.cy), rx = num(attrs.rx), ry = num(attrs.ry);
      return `M ${cx - rx} ${cy} A ${rx} ${ry} 0 1 0 ${cx + rx} ${cy} A ${rx} ${ry} 0 1 0 ${cx - rx} ${cy} Z`;
    }
    case 'rect': {
      const x = num(attrs.x), y = num(attrs.y), w = num(attrs.width), h = num(attrs.height);
      const rx = attrs.rx != null ? num(attrs.rx) : (attrs.ry != null ? num(attrs.ry) : 0);
      const ry = attrs.ry != null ? num(attrs.ry) : rx;
      if (!rx && !ry) return `M ${x} ${y} H ${x + w} V ${y + h} H ${x} Z`;
      return (
        `M ${x + rx} ${y} H ${x + w - rx} A ${rx} ${ry} 0 0 1 ${x + w} ${y + ry} V ${y + h - ry} ` +
        `A ${rx} ${ry} 0 0 1 ${x + w - rx} ${y + h} H ${x + rx} A ${rx} ${ry} 0 0 1 ${x} ${y + h - ry} ` +
        `V ${y + ry} A ${rx} ${ry} 0 0 1 ${x + rx} ${y} Z`
      );
    }
    case 'line':
      return `M ${num(attrs.x1)} ${num(attrs.y1)} L ${num(attrs.x2)} ${num(attrs.y2)}`;
    case 'polyline': {
      const pts = attrs.points.trim().split(/[\s,]+/).map(num);
      let d = `M ${pts[0]} ${pts[1]}`;
      for (let i = 2; i < pts.length; i += 2) d += ` L ${pts[i]} ${pts[i + 1]}`;
      return d;
    }
    case 'polygon': {
      const pts = attrs.points.trim().split(/[\s,]+/).map(num);
      let d = `M ${pts[0]} ${pts[1]}`;
      for (let i = 2; i < pts.length; i += 2) d += ` L ${pts[i]} ${pts[i + 1]}`;
      return d + ' Z';
    }
    default:
      return null;
  }
}

// Compose's `addPathNodes` reads path data with a greedy number tokenizer, so the compact SVG arc
// notation, where the two flags are packed against the coordinate that follows ("A2 2 0 0022 17"),
// parses as one number instead of three and the icon comes out mangled. Lucide ships paths in that
// form. Re-spacing every arc argument is valid SVG and makes both parsers agree.
//
// An arc takes seven arguments (rx ry rot large-arc sweep x y) and a single command letter may be
// followed by several such groups, so the flags have to be read positionally rather than by regex.
function normalizeArcArgs(args) {
  const groups = [];
  let i = 0;
  const skip = () => { while (i < args.length && /[\s,]/.test(args[i])) i++; };
  const readNumber = () => {
    skip();
    const m = /^-?\d*\.?\d+(?:[eE][-+]?\d+)?/.exec(args.slice(i));
    if (!m) return null;
    i += m[0].length;
    return m[0];
  };
  // A flag is exactly one character, 0 or 1, whatever follows it.
  const readFlag = () => {
    skip();
    if (i >= args.length || (args[i] !== '0' && args[i] !== '1')) return null;
    return args[i++];
  };
  while (i < args.length) {
    const group = [readNumber(), readNumber(), readNumber(), readFlag(), readFlag(), readNumber(), readNumber()];
    if (group.some((v) => v === null)) return null;
    groups.push(group.join(' '));
    skip();
  }
  return groups.join(' ');
}

function normalizePathData(d) {
  return d
    .replace(/([Aa])([^A-Za-z]*)/g, (whole, cmd, args) => {
      const normalized = normalizeArcArgs(args);
      return normalized === null ? whole : `${cmd} ${normalized} `;
    })
    .replace(/\s+/g, ' ')
    .trim();
}

// Guard against the bug this normaliser exists for: re-read every emitted path the way Compose
// does, with a greedy number tokenizer, and refuse to write a file whose arcs would not survive it.
function assertArcsSurviveGreedyTokenizer(name, d) {
  for (const m of d.matchAll(/([Aa])([^A-Za-z]*)/g)) {
    const nums = m[2].match(/-?\d*\.?\d+(?:[eE][-+]?\d+)?/g) || [];
    if (nums.length === 0 || nums.length % 7 !== 0) {
      throw new Error(
        `${name}: an arc command reads as ${nums.length} numbers, which is not a multiple of 7. ` +
        `The flags are probably packed against the next coordinate. Path: ${d}`,
      );
    }
    for (let i = 0; i < nums.length; i += 7) {
      for (const flag of [nums[i + 3], nums[i + 4]]) {
        if (flag !== '0' && flag !== '1') {
          throw new Error(`${name}: arc flag "${flag}" is not 0 or 1. Path: ${d}`);
        }
      }
    }
  }
}

function parseSvg(svg) {
  const body = svg.replace(/<!--[\s\S]*?-->/g, '');
  const elements = [];
  const re = /<(path|circle|ellipse|rect|line|polyline|polygon)\b([^>]*?)\/?>/g;
  let m;
  while ((m = re.exec(body)) !== null) {
    const attrs = {};
    const attrRe = /([a-zA-Z-]+)="([^"]*)"/g;
    let a;
    while ((a = attrRe.exec(m[2])) !== null) attrs[a[1]] = a[2];
    const d = elementToPath(m[1], attrs);
    if (d) elements.push(normalizePathData(d));
  }
  return elements;
}

const out = [];
out.push(`// GENERATED FILE — do not edit by hand. Run: node native/tools/gen-icons.mjs <lucide icons dir>`);
out.push(`// Icons: Lucide (https://lucide.dev) — ISC License.`);
out.push(`@file:Suppress("unused", "ObjectPropertyName", "MaxLineLength")`);
out.push(``);
out.push(`package fr.paulbr.nookmind.core.designsystem.icons`);
out.push(``);
out.push(`import androidx.compose.ui.graphics.Color`);
out.push(`import androidx.compose.ui.graphics.PathFillType`);
out.push(`import androidx.compose.ui.graphics.SolidColor`);
out.push(`import androidx.compose.ui.graphics.StrokeCap`);
out.push(`import androidx.compose.ui.graphics.StrokeJoin`);
out.push(`import androidx.compose.ui.graphics.vector.ImageVector`);
out.push(`import androidx.compose.ui.graphics.vector.addPathNodes`);
out.push(`import androidx.compose.ui.unit.dp`);
out.push(``);
out.push(`/** Lucide icon set used by the web app, as Compose ImageVectors (24x24, 2px round stroke). */`);
out.push(`object LucideIcons {`);
out.push(``);
out.push(`    private fun lucide(name: String, paths: List<String>): ImageVector {`);
out.push(`        val builder = ImageVector.Builder(`);
out.push(`            name = "Lucide.$name",`);
out.push(`            defaultWidth = 24.dp,`);
out.push(`            defaultHeight = 24.dp,`);
out.push(`            viewportWidth = 24f,`);
out.push(`            viewportHeight = 24f,`);
out.push(`        )`);
out.push(`        paths.forEach { d ->`);
out.push(`            builder.addPath(`);
out.push(`                pathData = addPathNodes(d),`);
out.push(`                pathFillType = PathFillType.NonZero,`);
out.push(`                fill = null,`);
out.push(`                stroke = SolidColor(Color.Black),`);
out.push(`                strokeLineWidth = 2f,`);
out.push(`                strokeLineCap = StrokeCap.Round,`);
out.push(`                strokeLineJoin = StrokeJoin.Round,`);
out.push(`            )`);
out.push(`        }`);
out.push(`        return builder.build()`);
out.push(`    }`);
out.push(``);

for (const [kotlinName, file] of Object.entries(ICONS)) {
  const svg = readFileSync(join(iconsDir, `${file}.svg`), 'utf8');
  const paths = parseSvg(svg);
  if (paths.length === 0) throw new Error(`No drawable elements in ${file}.svg`);
  paths.forEach((d) => assertArcsSurviveGreedyTokenizer(kotlinName, d));
  const list = paths.map((d) => `"${d.replace(/"/g, '\\"')}"`).join(', ');
  out.push(`    val ${kotlinName}: ImageVector by lazy { lucide("${kotlinName}", listOf(${list})) }`);
}
out.push(`}`);
out.push(``);

const target = join(
  here,
  '..',
  'composeApp/src/commonMain/kotlin/fr/paulbr/nookmind/core/designsystem/icons/LucideIcons.kt',
);
mkdirSync(dirname(target), { recursive: true });
writeFileSync(target, out.join('\n'));
console.log(`Wrote ${Object.keys(ICONS).length} icons to ${target}`);
