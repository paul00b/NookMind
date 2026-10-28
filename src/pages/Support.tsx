import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { CONTACT_EMAIL } from '../lib/contact';

type Lang = 'fr' | 'en';

const link = 'text-amber-600 hover:underline';
const mail = <a href={`mailto:${CONTACT_EMAIL}`} className={link}>{CONTACT_EMAIL}</a>;

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section>
      <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">{title}</h2>
      {children}
    </section>
  );
}

function ContactCard({ title, body, cta }: { title: string; body: ReactNode; cta: string }) {
  const href = `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent('NookMind : support')}`;
  return (
    <div className="rounded-2xl bg-white dark:bg-[#1a1f2e] border border-gray-200 dark:border-gray-800 p-5">
      <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-1">{title}</h2>
      <p>{body}</p>
      <a
        href={href}
        className="inline-block mt-4 px-4 py-2 rounded-full bg-amber-500 hover:bg-amber-600 text-gray-900 text-sm font-semibold"
      >
        {cta}
      </a>
    </div>
  );
}

function French() {
  return (
    <>
      <ContactCard
        title="Une question, un bug, une idée ?"
        body={<>Écrivez-nous à {mail}. Chaque message est lu, et nous répondons dès que possible.</>}
        cta="Écrire au support"
      />

      <Section title="Se connecter">
        <p>Connectez-vous avec Google, Apple ou une adresse e-mail. Utilisez le même moyen de connexion sur tous vos appareils pour y retrouver votre bibliothèque.</p>
      </Section>

      <Section title="Ajouter un ami">
        <p>Dans l'onglet Social, partagez votre code ami, votre lien ou votre QR code. Une fois la demande acceptée, chacun voit la bibliothèque de l'autre. Vous choisissez ce que vous partagez dans ce même onglet.</p>
      </Section>

      <Section title="Un livre, un film ou une série introuvable">
        <p>Les livres viennent de Google Books, les films et les séries de TMDB. Essayez le titre exact ou le nom de l'auteur. Si un message d'erreur s'affiche, réessayez quelques secondes plus tard.</p>
      </Section>

      <Section title="Signaler un problème">
        <p>Dites-nous ce que vous faisiez, sur quel téléphone, et la version de l'app (Paramètres, puis À propos). Une capture d'écran aide beaucoup.</p>
      </Section>

      <Section title="Supprimer votre compte">
        <p>Dans l'app : Paramètres, puis Supprimer le compte. La suppression est définitive. Vous pouvez aussi la demander par e-mail, comme expliqué sur <a href="/delete-account" className={link}>cette page</a>.</p>
      </Section>

      <Section title="Confidentialité et conditions">
        <p>
          <a href="/privacy" className={link}>Politique de confidentialité</a>
          {' · '}
          <a href="/terms" className={link}>Conditions d'utilisation</a>
        </p>
      </Section>
    </>
  );
}

function English() {
  return (
    <>
      <ContactCard
        title="A question, a bug, an idea?"
        body={<>Write to us at {mail}. Every message is read, and we answer as soon as we can.</>}
        cta="Contact support"
      />

      <Section title="Signing in">
        <p>Sign in with Google, Apple or an email address. Use the same sign-in method on all your devices to find your library there.</p>
      </Section>

      <Section title="Adding a friend">
        <p>In the Social tab, share your friend code, your link or your QR code. Once the request is accepted, each of you sees the other's library. You choose what you share in that same tab.</p>
      </Section>

      <Section title="A book, movie or series you cannot find">
        <p>Books come from Google Books, movies and series from TMDB. Try the exact title or the author's name. If an error message shows, try again a few seconds later.</p>
      </Section>

      <Section title="Reporting a problem">
        <p>Tell us what you were doing, on which phone, and the app version (Settings, then About). A screenshot helps a lot.</p>
      </Section>

      <Section title="Deleting your account">
        <p>In the app: Settings, then Delete account. Deletion is permanent. You can also ask for it by email, as explained on <a href="/delete-account" className={link}>this page</a>.</p>
      </Section>

      <Section title="Privacy and terms">
        <p>
          <a href="/privacy" className={link}>Privacy policy</a>
          {' · '}
          <a href="/terms" className={link}>Terms of use</a>
        </p>
      </Section>
    </>
  );
}

/** Public help page: the Support URL of the store listings. */
export default function Support() {
  const { i18n } = useTranslation();
  const [lang, setLang] = useState<Lang>(i18n.language?.startsWith('fr') ? 'fr' : 'en');
  const fr = lang === 'fr';

  return (
    <div className="min-h-screen bg-[#f8f6f1] dark:bg-[#0f1117] py-12 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="flex items-start justify-between gap-4 mb-2">
          <h1 className="font-serif text-3xl font-bold text-gray-900 dark:text-gray-100">
            {fr ? 'Aide et contact' : 'Help and contact'}
          </h1>
          <button
            type="button"
            onClick={() => setLang(fr ? 'en' : 'fr')}
            className="shrink-0 mt-2 text-sm text-amber-600 hover:underline"
          >
            {fr ? 'English' : 'Français'}
          </button>
        </div>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-8">
          {fr
            ? 'NookMind, pour suivre vos livres, films et séries, et les partager avec vos amis.'
            : 'NookMind, to keep track of your books, movies and series, and share them with your friends.'}
        </p>

        <div className="space-y-6 text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
          {fr ? <French /> : <English />}
        </div>

        <a href="/login" className="inline-block mt-10 text-sm text-amber-600 hover:underline">
          {fr ? '← Retour' : '← Back'}
        </a>
      </div>
    </div>
  );
}
