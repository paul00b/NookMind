// Fixes Windows backslash path separators in the SPM Package.swift that
// Capacitor's CLI regenerates on every `cap sync`. SPM on macOS requires
// forward slashes. No-op on macOS (file already has forward slashes).
const fs = require('fs');
const path = require('path');

const target = path.join(__dirname, '..', 'ios', 'App', 'CapApp-SPM', 'Package.swift');

if (!fs.existsSync(target)) {
  // iOS platform may not have been added (CI / fresh clone before cap add)
  process.exit(0);
}

const original = fs.readFileSync(target, 'utf8');
const fixed = original.replace(/path:\s*"([^"]+)"/g, (_, p) =>
  `path: "${p.replace(/\\/g, '/')}"`
);

if (fixed !== original) {
  fs.writeFileSync(target, fixed);
  console.log('[fix-spm-paths] Restored forward slashes in Package.swift');
}
