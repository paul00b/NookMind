import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { APP_STORE_URL, PLAY_STORE_URL } from '../lib/stores';

type Lang = 'fr' | 'en';

const copy = {
  fr: {
    signIn: 'Se connecter',
    title: 'Tes livres, films et séries au même endroit.',
    lead: "Suis ce que tu lis et regardes, partage ta bibliothèque avec tes amis, et trouvez enfin quoi regarder ce soir.",
    play: 'Disponible sur Google Play',
    appStore: "Télécharger sur l'App Store",
    appStoreSoon: "Bientôt sur l'App Store",
    tonightKicker: 'On regarde quoi ?',
    tonightTitle: 'Fini les 45 minutes à choisir un film.',
    tonightBody: "Chacun swipe sur son téléphone : oui, non, coup de cœur, déjà vu. Le film que vous aimez tous les deux sort à la fin. Ça marche aussi avec les séries.",
    featuresTitle: 'Tout ce que tu lis et regardes, enfin rangé',
    features: [
      { img: '/landing/books.webp', title: 'Tes lectures', body: 'Livres lus, en cours ou à lire. Ta page actuelle, ta note, tes impressions et tes collections.' },
      { img: '/landing/movies.webp', title: 'Tes films', body: 'Ta liste à voir, tes films vus, où les regarder en streaming, et les sorties à ne pas rater.' },
      { img: '/landing/series.webp', title: 'Tes séries', body: 'Épisode par épisode, avec le compte à rebours jusqu’au prochain et une alerte à chaque nouvelle saison.' },
    ],
    friendsTitle: 'Avec tes amis',
    friendsBody: "Ajoute un ami avec son code ou un QR code, vois ce qu'il lit et regarde cette semaine, réagis à ses titres et ajoute les siens en un geste.",
    help: 'Aide',
    privacy: 'Confidentialité',
    terms: 'Conditions',
    tmdb: 'Données films et séries fournies par TMDB.',
  },
  en: {
    signIn: 'Sign in',
    title: 'Your books, movies and series in one place.',
    lead: 'Track what you read and watch, share your library with your friends, and finally find what to watch tonight.',
    play: 'Get it on Google Play',
    appStore: 'Download on the App Store',
    appStoreSoon: 'Coming soon to the App Store',
    tonightKicker: 'What are we watching?',
    tonightTitle: 'No more 45 minutes picking a movie.',
    tonightBody: 'Each of you swipes on your own phone: yes, no, love it, seen it. The movie you both like comes out at the end. It works with series too.',
    featuresTitle: 'Everything you read and watch, finally in order',
    features: [
      { img: '/landing/books.webp', title: 'Your books', body: 'Read, reading or to read. Your current page, your rating, your thoughts and your collections.' },
      { img: '/landing/movies.webp', title: 'Your movies', body: 'Your watchlist, the movies you have seen, where to stream them, and the releases not to miss.' },
      { img: '/landing/series.webp', title: 'Your series', body: 'Episode by episode, with a countdown to the next one and an alert for every new season.' },
    ],
    friendsTitle: 'With your friends',
    friendsBody: 'Add a friend with their code or a QR code, see what they read and watch this week, react to their titles and add theirs in one tap.',
    help: 'Help',
    privacy: 'Privacy',
    terms: 'Terms',
    tmdb: 'Movie and series data provided by TMDB.',
  },
} satisfies Record<Lang, unknown>;

function Phone({ src, alt, className = '' }: { src: string; alt: string; className?: string }) {
  return (
    <div className={`rounded-[2.2rem] bg-[#0b0d12] p-2 ring-1 ring-white/15 shadow-2xl shadow-black/50 ${className}`}>
      <img src={src} alt={alt} loading="lazy" className="w-full aspect-[390/844] object-cover object-top rounded-[1.7rem] block" />
    </div>
  );
}

function StoreButtons({ t }: { t: (typeof copy)[Lang] }) {
  const base = 'inline-flex items-center justify-center gap-2 rounded-full px-5 py-3 text-sm font-semibold transition-colors';
  return (
    <div className="flex flex-wrap gap-3">
      <a href={PLAY_STORE_URL} target="_blank" rel="noopener noreferrer" className={`${base} bg-amber-500 hover:bg-amber-400 text-gray-900`}>
        {t.play}
      </a>
      {APP_STORE_URL ? (
        <a href={APP_STORE_URL} target="_blank" rel="noopener noreferrer" className={`${base} bg-white hover:bg-gray-100 text-gray-900`}>
          {t.appStore}
        </a>
      ) : (
        <span className={`${base} border border-white/20 text-gray-400`}>{t.appStoreSoon}</span>
      )}
    </div>
  );
}

function Section({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`max-w-5xl mx-auto px-5 ${className}`}>{children}</section>;
}

/** Public home page for visitors who are not signed in: what the app does and where to get it. */
export default function Landing() {
  const { i18n } = useTranslation();
  const [lang, setLang] = useState<Lang>(i18n.language?.startsWith('fr') ? 'fr' : 'en');
  const t = copy[lang];

  return (
    <div className="min-h-screen bg-[#1d2130] text-gray-100 overflow-x-hidden">
      <header className="max-w-5xl mx-auto px-5 py-5 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <img src="/logo.png" alt="" className="w-8 h-8" />
          <span className="font-serif text-xl font-bold">NookMind</span>
        </div>
        <div className="flex items-center gap-4 text-sm">
          <button type="button" onClick={() => setLang(lang === 'fr' ? 'en' : 'fr')} className="text-gray-400 hover:text-gray-200">
            {lang === 'fr' ? 'EN' : 'FR'}
          </button>
          <a href="/login" className="text-amber-400 hover:underline">{t.signIn}</a>
        </div>
      </header>

      <Section className="pt-8 pb-16 md:pt-16 md:pb-24 grid md:grid-cols-[1.2fr_1fr] gap-12 items-center">
        <div>
          <h1 className="font-serif text-4xl md:text-6xl font-bold leading-[1.08]">{t.title}</h1>
          <p className="mt-5 text-lg text-gray-400 max-w-xl">{t.lead}</p>
          <div className="mt-8"><StoreButtons t={t} /></div>
        </div>
        <Phone src="/landing/swipe.webp" alt={t.tonightKicker} className="w-64 md:w-72 mx-auto md:rotate-2" />
      </Section>

      <div className="bg-gradient-to-b from-[#241d3d] to-[#1d2130]">
        <Section className="py-16 md:py-24 grid md:grid-cols-2 gap-10 items-center">
          <div className="md:order-2">
            <p className="text-sm font-semibold uppercase tracking-wider text-violet-300">{t.tonightKicker}</p>
            <h2 className="mt-3 font-serif text-3xl md:text-4xl font-bold leading-tight">{t.tonightTitle}</h2>
            <p className="mt-4 text-gray-400 text-lg">{t.tonightBody}</p>
          </div>
          <div className="flex gap-4 justify-center md:order-1">
            <Phone src="/landing/match.webp" alt={t.tonightKicker} className="w-48 md:w-56 -rotate-3" />
          </div>
        </Section>
      </div>

      <Section className="py-16 md:py-24">
        <h2 className="font-serif text-3xl md:text-4xl font-bold text-center">{t.featuresTitle}</h2>
        <div className="mt-12 grid sm:grid-cols-3 gap-10">
          {t.features.map((f) => (
            <div key={f.title} className="text-center">
              <Phone src={f.img} alt={f.title} className="w-44 mx-auto" />
              <h3 className="mt-6 font-semibold text-lg">{f.title}</h3>
              <p className="mt-2 text-sm text-gray-400 leading-relaxed">{f.body}</p>
            </div>
          ))}
        </div>
      </Section>

      <Section className="pb-20 text-center">
        <h2 className="font-serif text-3xl md:text-4xl font-bold">{t.friendsTitle}</h2>
        <p className="mt-4 text-gray-400 text-lg max-w-2xl mx-auto">{t.friendsBody}</p>
        <div className="mt-8 flex justify-center"><StoreButtons t={t} /></div>
      </Section>

      <footer className="border-t border-white/10">
        <div className="max-w-5xl mx-auto px-5 py-8 flex flex-col sm:flex-row gap-4 justify-between text-sm text-gray-400">
          <div className="flex gap-5">
            <a href="/support" className="hover:text-gray-200">{t.help}</a>
            <a href="/privacy" className="hover:text-gray-200">{t.privacy}</a>
            <a href="/terms" className="hover:text-gray-200">{t.terms}</a>
          </div>
          <p>{t.tmdb}</p>
        </div>
      </footer>
    </div>
  );
}
