import type { VercelRequest, VercelResponse } from '@vercel/node';
import { ANDROID_PACKAGE } from './_lib/shareItem.js';

// /.well-known/assetlinks.json (rewritten here by vercel.json): lets Android open the shared links
// straight in the app. The SHA-256 fingerprints of the app's signing certificates (Play Console >
// App integrity, plus the upload or debug key if wanted) go in ANDROID_SHA256_CERT_FINGERPRINTS,
// comma-separated. Without them the links still work, through the page and its button.

export default function handler(_req: VercelRequest, res: VercelResponse) {
  const fingerprints = (process.env.ANDROID_SHA256_CERT_FINGERPRINTS ?? '')
    .split(',')
    .map((value) => value.trim().toUpperCase())
    .filter((value) => /^([0-9A-F]{2}:){31}[0-9A-F]{2}$/.test(value));
  const statements = fingerprints.length
    ? [{
      relation: ['delegate_permission/common.handle_all_urls'],
      target: { namespace: 'android_app', package_name: ANDROID_PACKAGE, sha256_cert_fingerprints: fingerprints },
    }]
    : [];
  res.setHeader('Content-Type', 'application/json');
  res.setHeader('Cache-Control', 'public, max-age=3600');
  return res.status(200).send(JSON.stringify(statements));
}
