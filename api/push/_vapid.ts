import webpush from 'web-push';

function cleanEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing environment variable: ${name}`);
  }

  return value.trim();
}

export function getVapidConfig() {
  return {
    subject: cleanEnv('VAPID_CONTACT'),
    publicKey: cleanEnv('VITE_VAPID_PUBLIC_KEY'),
    privateKey: cleanEnv('VAPID_PRIVATE_KEY'),
  };
}

export function configureWebPush() {
  const vapid = getVapidConfig();
  webpush.setVapidDetails(vapid.subject, vapid.publicKey, vapid.privateKey);
  return vapid;
}
