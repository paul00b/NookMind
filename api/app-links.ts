import type { VercelRequest, VercelResponse } from '@vercel/node';
import { ANDROID_PACKAGE, APPLE_TEAM_ID, IOS_BUNDLE_ID } from './_lib/shareItem.js';

// The two files that let a phone open the shared links (/s/...) straight in the app, rewritten here
// by vercel.json. One function for both: the Hobby plan stops at 12 functions per deployment.
//
// - Android, /.well-known/assetlinks.json: the SHA-256 fingerprints of the app's signing certificates
//   (Play Console > App integrity, plus the upload or debug key if wanted) in
//   ANDROID_SHA256_CERT_FINGERPRINTS, comma-separated.
// - iOS, /.well-known/apple-app-site-association: the app of the Apple developer team (APPLE_TEAM_ID
//   overrides the one in shareItem.ts). The app declares the domain on its side (associated domains,
//   ios-release.yml).
//
// Without them the links still work, through the page and its button.

function android() {
  const fingerprints = (process.env.ANDROID_SHA256_CERT_FINGERPRINTS ?? '')
    .split(',')
    .map((value) => value.trim().toUpperCase())
    .filter((value) => /^([0-9A-F]{2}:){31}[0-9A-F]{2}$/.test(value));
  return fingerprints.length
    ? [{
      relation: ['delegate_permission/common.handle_all_urls'],
      target: { namespace: 'android_app', package_name: ANDROID_PACKAGE, sha256_cert_fingerprints: fingerprints },
    }]
    : [];
}

function ios() {
  const team = (process.env.APPLE_TEAM_ID || APPLE_TEAM_ID).trim().toUpperCase();
  const details = /^[A-Z0-9]{10}$/.test(team)
    ? [{ appIDs: [`${team}.${IOS_BUNDLE_ID}`], components: [
      { '/': '/s/*', comment: 'A shared movie, series or book' },
      { '/': '/f/*', comment: 'A friend\'s invitation' },
    ] }]
    : [];
  return { applinks: { details } };
}

export default function handler(req: VercelRequest, res: VercelResponse) {
  res.setHeader('Content-Type', 'application/json');
  res.setHeader('Cache-Control', 'public, max-age=3600');
  return res.status(200).send(JSON.stringify(req.query.os === 'ios' ? ios() : android()));
}
