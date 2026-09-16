#!/usr/bin/env node
// Converts the web app's i18n dictionaries (src/i18n/locales/{en,fr}.ts) into Compose Multiplatform
// string resources so both apps share one source of truth for copy.
//
//   node native/tools/gen-strings.mjs
//
// Output:
//   native/composeApp/src/commonMain/composeResources/values/strings.xml      (English, default)
//   native/composeApp/src/commonMain/composeResources/values-fr/strings.xml   (French)
//
// Rules:
//   - `home.title`               -> <string name="home_title">
//   - `library.booksCount_one/_other` -> <plurals name="library_booksCount"> (in every locale)
//   - `{{count}}` in a plural   -> %1$d ; every other `{{param}}` -> positional %N$s, indexed in the
//     order of first appearance in the English string so both locales share the same argument order.
//   - `{{param}}` in a plain string -> positional %N$s as well.
//   - Extra copy that was hard-coded in the React components lives in `EXTRA` below.

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(here, '..', '..');

async function loadLocale(name) {
  // pathToFileURL is required here: dynamic import() rejects bare Windows absolute
  // paths (e.g. "C:\\...") with ERR_UNSUPPORTED_ESM_URL_SCHEME, since it parses them
  // as URLs whose scheme is the drive letter. POSIX absolute paths aren't affected,
  // which is presumably why this went unnoticed until run on native Windows.
  const mod = await import(pathToFileURL(join(repoRoot, 'src', 'i18n', 'locales', `${name}.ts`)).href);
  return mod.default;
}

function flatten(obj, prefix = '', out = {}) {
  for (const [k, v] of Object.entries(obj)) {
    const key = prefix ? `${prefix}.${k}` : k;
    if (v && typeof v === 'object') flatten(v, key, out);
    else out[key] = String(v);
  }
  return out;
}

// Copy hard-coded in components (kept bilingual there) — now regular resources.
const EXTRA = {
  en: {
    'common.appName': 'NookMind',
    'common.defaultDisplayName': 'Reader',
    'common.close': 'Close',
    'common.cancel': 'Cancel',
    'common.later': 'Maybe later',
    'common.ok': 'OK',
    'common.stats': 'Stats',
    'common.episodeNumber': 'Episode {{number}}',
    'common.seasonEpisodeShort': 'S{{season}}E{{episode}}',
    'common.pageOf': 'Page {{current}} / {{total}}',
    'common.pageOnly': 'Page {{current}}',
    'common.minutesShort': '{{count}} min',
    'common.itemsCountBooks_one': '{{count}} book',
    'common.itemsCountBooks_other': '{{count}} books',
    'common.itemsCountMovies_one': '{{count}} movie',
    'common.itemsCountMovies_other': '{{count}} movies',
    'common.itemsCountSeries_one': '{{count}} series',
    'common.itemsCountSeries_other': '{{count}} series',
    'common.collectionDeleted': '"{{name}}" deleted',
    'common.loading': 'Loading…',
    'common.retry': 'Retry',
    'common.back': 'Back',
    'common.exitApp': 'Press back again to exit',
    'notifPrompt.title': 'Stay up to date',
    'notifPrompt.subtitle': 'NookMind notifications',
    'notifPrompt.body': 'Enable notifications to be notified on the release day of your favourite episodes and movies — without opening the app.',
    'notifPrompt.enable': 'Enable',
    'notifPrompt.enabling': 'Enabling…',
    'notifPrompt.enabled': 'Notifications enabled!',
    'notifPrompt.tokenError': 'Could not get push token. Check your Firebase setup.',
    'notifPrompt.enableFailed': 'Failed to enable notifications.',
    'seriesStats.title': 'My stats',
    'seriesStats.subtitle': 'Watched & watching',
    'seriesStats.totalWatchTime': 'Total watch time',
    'seriesStats.series': 'Series',
    'seriesStats.episodes': 'Episodes',
    'seriesStats.seasons': 'Seasons',
    'seriesStats.avgRating': 'Avg rating',
    'seriesStats.topGenre': 'Top genre',
    'seriesStats.topCreator': 'Top creator',
    'seriesStats.minutes_one': '{{count}} minute',
    'seriesStats.minutes_other': '{{count}} minutes',
    'seriesStats.hours_one': '{{count}} hour',
    'seriesStats.hours_other': '{{count}} hours',
    'seriesStats.days_one': '{{count}} day',
    'seriesStats.days_other': '{{count}} days',
    'settings.testNotifications': 'Test notifications',
    'settings.testNotificationsRunning': 'Testing…',
    'settings.testNotificationsSent': 'Test sent',
    'settings.testNotificationsRetry': 'Test notifications again',
    'settings.testNotificationsSending': 'Sending the test…',
    'settings.testNotificationsFailed': 'The test failed.',
    'settings.testNotificationsNoToken': 'Could not retrieve the device token.',
    'settings.notifUnsupportedDevice': 'Notifications are not supported on this device.',
    'settings.notifTestSent': 'Test notification sent.',
    'settings.notifTestRejected': 'The test notification was rejected.',
    'settings.notifTestNobody': 'No subscribed device accepted the notification.',
    'settings.notifTestUnavailable': 'Could not send the test notification.',
    'settings.googleSignInUnavailable': 'Google sign-in is not available on this platform.',
    'settings.appleSignInIosOnly': 'Apple sign-in is only available on iOS.',
    'settings.haptics': 'Vibrations',
    'settings.hapticsHelp': 'Short vibrations on ratings, switches and saved changes.',
    'legal.privacyTitle': 'Privacy policy',
    'legal.termsTitle': 'Terms of use',
    'legal.lastUpdated': 'Last updated: March 2025',
    'legal.back': '← Back',
    'auth.notSignedIn': 'Not signed in',
    'auth.googleNoToken': 'Google sign-in returned no ID token',
  },
  fr: {
    'common.appName': 'NookMind',
    'common.defaultDisplayName': 'Lecteur',
    'common.close': 'Fermer',
    'common.cancel': 'Annuler',
    'common.later': 'Plus tard',
    'common.ok': 'OK',
    'common.stats': 'Stats',
    'common.episodeNumber': 'Épisode {{number}}',
    'common.seasonEpisodeShort': 'S{{season}}E{{episode}}',
    'common.pageOf': 'Page {{current}} / {{total}}',
    'common.pageOnly': 'Page {{current}}',
    'common.minutesShort': '{{count}} min',
    'common.itemsCountBooks_one': '{{count}} livre',
    'common.itemsCountBooks_other': '{{count}} livres',
    'common.itemsCountMovies_one': '{{count}} film',
    'common.itemsCountMovies_other': '{{count}} films',
    'common.itemsCountSeries_one': '{{count}} série',
    'common.itemsCountSeries_other': '{{count}} séries',
    'common.collectionDeleted': '"{{name}}" supprimée',
    'common.loading': 'Chargement…',
    'common.retry': 'Réessayer',
    'common.back': 'Retour',
    'common.exitApp': 'Appuyez de nouveau pour quitter',
    'notifPrompt.title': 'Restez informé',
    'notifPrompt.subtitle': 'Notifications NookMind',
    'notifPrompt.body': "Activez les notifications pour être prévenu le jour de la sortie de vos épisodes et films préférés — sans avoir à ouvrir l'app.",
    'notifPrompt.enable': 'Activer',
    'notifPrompt.enabling': 'Activation…',
    'notifPrompt.enabled': 'Notifications activées !',
    'notifPrompt.tokenError': "Impossible d'obtenir le token push. Vérifie la configuration Firebase.",
    'notifPrompt.enableFailed': "Erreur lors de l'activation.",
    'seriesStats.title': 'Mes stats',
    'seriesStats.subtitle': 'Séries vues & en cours',
    'seriesStats.totalWatchTime': 'Temps total regardé',
    'seriesStats.series': 'Séries',
    'seriesStats.episodes': 'Épisodes',
    'seriesStats.seasons': 'Saisons',
    'seriesStats.avgRating': 'Note moyenne',
    'seriesStats.topGenre': 'Genre favori',
    'seriesStats.topCreator': 'Créateur favori',
    'seriesStats.minutes_one': '{{count}} minute',
    'seriesStats.minutes_other': '{{count}} minutes',
    'seriesStats.hours_one': '{{count}} heure',
    'seriesStats.hours_other': '{{count}} heures',
    'seriesStats.days_one': '{{count}} jour',
    'seriesStats.days_other': '{{count}} jours',
    'settings.testNotifications': 'Tester les notifications',
    'settings.testNotificationsRunning': 'Test en cours...',
    'settings.testNotificationsSent': 'Test envoyé',
    'settings.testNotificationsRetry': 'Retester les notifications',
    'settings.testNotificationsSending': 'Envoi du test en cours...',
    'settings.testNotificationsFailed': 'Le test a échoué.',
    'settings.testNotificationsNoToken': "Impossible de récupérer le jeton de l'appareil.",
    'settings.notifUnsupportedDevice': 'Notifications non supportées sur cet appareil.',
    'settings.notifTestSent': 'Notification de test envoyée.',
    'settings.notifTestRejected': 'La notification de test a été rejetée.',
    'settings.notifTestNobody': "Aucun appareil abonné n'a accepté la notification.",
    'settings.notifTestUnavailable': "Impossible d'envoyer la notification de test.",
    'settings.googleSignInUnavailable': "La connexion Google n'est pas disponible sur cette plateforme.",
    'settings.appleSignInIosOnly': "La connexion Apple n'est disponible que sur iOS.",
    'settings.haptics': 'Vibrations',
    'settings.hapticsHelp': 'De courtes vibrations sur les notes, les interrupteurs et les modifications enregistrées.',
    'legal.privacyTitle': 'Politique de confidentialité',
    'legal.termsTitle': "Conditions d'utilisation",
    'legal.lastUpdated': 'Dernière mise à jour : mars 2025',
    'legal.back': '← Retour',
    'auth.notSignedIn': 'Non connecté',
    'auth.googleNoToken': "La connexion Google n'a renvoyé aucun jeton",
  },
};

const en = { ...flatten(await loadLocale('en')), ...EXTRA.en };
const fr = { ...flatten(await loadLocale('fr')), ...EXTRA.fr };

const PLURAL_RE = /_(one|other)$/;

// Collect base keys and whether they are plurals (in any locale).
const baseKeys = new Map(); // base -> { plural: boolean }
for (const dict of [en, fr]) {
  for (const key of Object.keys(dict)) {
    const m = key.match(PLURAL_RE);
    const base = m ? key.replace(PLURAL_RE, '') : key;
    const entry = baseKeys.get(base) ?? { plural: false };
    if (m) entry.plural = true;
    baseKeys.set(base, entry);
  }
}

const resName = (key) => key.replace(/\./g, '_');

// Compose Multiplatform resources read the XML text as-is (no aapt-style unescaping of \' and \"),
// so only the XML entities are escaped.
function escapeXml(s) {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

// Build the parameter order per base key from the English text (fallback: French).
const paramOrder = new Map();
function paramsIn(text) {
  const params = [];
  for (const m of text.matchAll(/\{\{(\w+)\}\}/g)) if (!params.includes(m[1])) params.push(m[1]);
  return params;
}
for (const [base, { plural }] of baseKeys) {
  const samples = plural
    ? [en[`${base}_other`], en[`${base}_one`], en[base], fr[`${base}_other`], fr[`${base}_one`], fr[base]]
    : [en[base], fr[base]];
  const order = [];
  for (const s of samples) if (s) for (const p of paramsIn(s)) if (!order.includes(p)) order.push(p);
  paramOrder.set(base, order);
}

function format(text, base, plural) {
  const order = paramOrder.get(base);
  let out = text.replace(/%/g, '%%');
  out = out.replace(/\{\{(\w+)\}\}/g, (_, p) => {
    const idx = order.indexOf(p) + 1;
    const isInt = p === 'count' || p === 'days' || p === 'season' || p === 'episode' || p === 'number' || p === 'current' || p === 'total' || p === 'watched';
    return `%${idx}$${isInt ? 'd' : 's'}`;
  });
  return escapeXml(out);
}

function pick(dict, base, quantity) {
  return dict[`${base}_${quantity}`] ?? dict[base] ?? dict[`${base}_other`] ?? dict[`${base}_one`];
}

function render(dict, fallback) {
  const lines = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>'];
  const keys = [...baseKeys.keys()].sort();
  for (const base of keys) {
    const { plural } = baseKeys.get(base);
    const name = resName(base);
    if (plural) {
      const one = pick(dict, base, 'one') ?? pick(fallback, base, 'one');
      const other = pick(dict, base, 'other') ?? pick(fallback, base, 'other');
      lines.push(`    <plurals name="${name}">`);
      lines.push(`        <item quantity="one">${format(one, base, true)}</item>`);
      lines.push(`        <item quantity="other">${format(other, base, true)}</item>`);
      lines.push(`    </plurals>`);
    } else {
      const text = dict[base] ?? fallback[base];
      lines.push(`    <string name="${name}">${format(text, base, false)}</string>`);
    }
  }
  lines.push('</resources>', '');
  return lines.join('\n');
}

const resDir = join(here, '..', 'composeApp', 'src', 'commonMain', 'composeResources');
mkdirSync(join(resDir, 'values'), { recursive: true });
mkdirSync(join(resDir, 'values-fr'), { recursive: true });
writeFileSync(join(resDir, 'values', 'strings.xml'), render(en, fr));
writeFileSync(join(resDir, 'values-fr', 'strings.xml'), render(fr, en));

// Report keys whose parameter sets differ between locales (would be a copy bug).
for (const [base] of baseKeys) {
  const a = new Set(paramsIn(pick(en, base, 'other') ?? ''));
  const b = new Set(paramsIn(pick(fr, base, 'other') ?? ''));
  const diff = [...a].filter((p) => !b.has(p)).concat([...b].filter((p) => !a.has(p)));
  if (diff.length) console.warn(`param mismatch for ${base}: en=[${[...a]}] fr=[${[...b]}]`);
}
console.log(`Wrote ${baseKeys.size} resources (en + fr).`);
