import { useEffect, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

const CONTACT_EMAIL = 'broussolle.paul@gmail.com';

type Lang = 'fr' | 'en';

const link = 'text-amber-600 hover:underline';
const mail = <a href={`mailto:${CONTACT_EMAIL}`} className={link}>{CONTACT_EMAIL}</a>;

function DeleteRequestButton({ label, subject, body }: { label: string; subject: string; body: string }) {
  const href = `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
  return (
    <a
      href={href}
      className="inline-block mt-3 px-4 py-2 rounded-full bg-red-600 hover:bg-red-700 text-white text-sm font-semibold"
    >
      {label}
    </a>
  );
}

function Section({ id, title, children }: { id?: string; title: string; children: ReactNode }) {
  return (
    <section id={id} className="scroll-mt-8">
      <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">{title}</h2>
      {children}
    </section>
  );
}

function List({ items }: { items: ReactNode[] }) {
  return (
    <ul className="list-disc pl-5 mt-2 space-y-1">
      {items.map((item, i) => <li key={i}>{item}</li>)}
    </ul>
  );
}

function French() {
  return (
    <>
      <Section title="1. Données collectées">
        <p>NookMind collecte uniquement les données nécessaires au fonctionnement de l'application :</p>
        <List items={[
          "Adresse e-mail et identifiant de compte (via Google, Apple ou inscription par e-mail)",
          "Nom d'affichage : celui fourni par Google ou Apple à la connexion, ou celui que vous choisissez dans les paramètres",
          "Données de bibliothèque : livres, films et séries ajoutés, collections, notes personnelles, évaluations, avancement de lecture ou de visionnage",
          "Données sociales, si vous les utilisez : votre code ami, vos amis, les réactions que vous envoyez et vos parties « Ce soir ? » à distance (films proposés et votes)",
          "Jeton de notification (Firebase Cloud Messaging), uniquement si vous activez les notifications",
        ]} />
        <p className="mt-2">Sur le site web, nous mesurons la fréquentation avec Vercel Analytics, sans cookie et sans identifier les visiteurs. L'application mobile ne contient ni publicité ni outil de suivi.</p>
      </Section>

      <Section title="2. Utilisation des données">
        <p>Vos données sont utilisées exclusivement pour :</p>
        <List items={[
          'Vous authentifier et sécuriser votre compte',
          'Stocker et synchroniser votre bibliothèque personnelle entre vos appareils',
          'Faire fonctionner les fonctions sociales que vous utilisez : amis, réactions, « Ce soir ? » à distance',
          "Vous envoyer les notifications que vous avez activées (nouvelles saisons, sorties de films, demandes d'amis et invitations)",
        ]} />
        <p className="mt-2">Nous ne vendons pas et ne monétisons pas vos données personnelles. Nous ne les partageons avec personne, en dehors des amis que vous ajoutez (section 3) et des services de la section 4.</p>
      </Section>

      <Section title="3. Ce que voient vos amis">
        <p>Les fonctions sociales sont facultatives. Un ami s'ajoute avec son code ami, et il doit accepter la demande. Une fois que vous êtes amis :</p>
        <List items={[
          "Il voit votre nom d'affichage et les films, séries et livres de votre bibliothèque, avec leur statut. Dans l'onglet Social, vous choisissez ce que vous partagez : films, séries, livres, évaluations",
          'Vos notes personnelles ne sont jamais partagées',
          'Il voit les réactions que vous laissez sur ses titres',
          'Pendant une partie « Ce soir ? » à distance, il voit les films proposés, puis vos votes à la fin de la partie',
        ]} />
        <p className="mt-2">Personne d'autre que vos amis n'a accès à ces informations, et vous pouvez retirer un ami à tout moment. Quand vous partagez un titre par lien, ce lien contient votre prénom, pour que la personne qui le reçoit sache qui le lui recommande.</p>
      </Section>

      <Section title="4. Hébergement et services tiers">
        <p>NookMind s'appuie sur les services suivants, qui traitent les données pour notre compte :</p>
        <List items={[
          <><a href="https://supabase.com" className={link} target="_blank" rel="noopener noreferrer">Supabase</a> : hébergement de la base de données et authentification</>,
          'Vercel : hébergement du site et des fonctions serveur',
          'Google Firebase Cloud Messaging : envoi des notifications',
          "Google et Apple : connexion à votre compte (OAuth 2.0), si vous choisissez ce mode",
        ]} />
        <p className="mt-2">Pour afficher les fiches des livres, films et séries, l'application interroge TMDB, Google Books et IMDb. Ces services reçoivent vos termes de recherche et les titres consultés, mais jamais votre adresse e-mail ni votre identifiant de compte.</p>
      </Section>

      <Section title="5. Conservation">
        <p>Vos données sont conservées tant que votre compte existe. Elles sont supprimées définitivement lorsque vous supprimez votre compte.</p>
      </Section>

      <Section id="delete-account" title="6. Supprimer votre compte">
        <p><strong>Depuis l'application NookMind :</strong> ouvrez les Paramètres, touchez « Supprimer le compte », puis confirmez avec « Oui, supprimer mon compte ». La suppression est immédiate.</p>
        <p className="mt-2"><strong>Sans l'application :</strong> écrivez à {mail} depuis l'adresse e-mail de votre compte, avec pour objet « Suppression de compte NookMind ». Nous supprimons le compte sous 30 jours au plus et vous le confirmons par e-mail.</p>
        <DeleteRequestButton
          label="Demander la suppression de mon compte"
          subject="Suppression de compte NookMind"
          body={"Bonjour,\n\nJe souhaite la suppression de mon compte NookMind et de toutes les données associées.\n\nAdresse e-mail du compte : \n\nMerci."}
        />
        <p className="mt-2">Dans les deux cas, sont supprimés : votre compte, vos livres, films, séries, collections, notes, évaluations, votre profil, vos amis, vos réactions, vos parties « Ce soir ? » et vos abonnements aux notifications. Aucune donnée n'est conservée après la suppression.</p>
      </Section>

      <Section title="7. Vos droits">
        <p>Conformément au RGPD, vous pouvez accéder à vos données, les rectifier, en demander la portabilité ou l'effacement, en écrivant à {mail}. Vous pouvez aussi adresser une réclamation à la <a href="https://www.cnil.fr" className={link} target="_blank" rel="noopener noreferrer">CNIL</a>.</p>
      </Section>

      <Section title="8. Contact">
        <p>Pour toute question relative à vos données personnelles : {mail}.</p>
      </Section>
    </>
  );
}

function English() {
  return (
    <>
      <Section title="1. Data we collect">
        <p>NookMind only collects the data it needs to work:</p>
        <List items={[
          'Email address and account ID (via Google, Apple or email sign-up)',
          'Display name: the one provided by Google or Apple when you sign in, or the one you choose in the settings',
          'Library data: books, movies and series you add, collections, personal notes, ratings, reading or watching progress',
          'Social data, if you use it: your friend code, your friends, the reactions you send and your remote “Tonight?” games (suggested movies and votes)',
          'Notification token (Firebase Cloud Messaging), only if you turn notifications on',
        ]} />
        <p className="mt-2">On the website, we measure traffic with Vercel Analytics, without cookies and without identifying visitors. The mobile app contains no ads and no tracking tools.</p>
      </Section>

      <Section title="2. How we use it">
        <p>Your data is used only to:</p>
        <List items={[
          'Authenticate you and keep your account secure',
          'Store and sync your personal library across your devices',
          'Run the social features you use: friends, reactions, remote “Tonight?” games',
          'Send you the notifications you turned on (new seasons, movie releases, friend requests and invitations)',
        ]} />
        <p className="mt-2">We do not sell or monetize your personal data. We share it with no one, apart from the friends you add (section 3) and the services in section 4.</p>
      </Section>

      <Section title="3. What your friends see">
        <p>The social features are optional. You add a friend with their friend code, and they have to accept the request. Once you are friends:</p>
        <List items={[
          'They see your display name and the movies, series and books in your library, with their status. In the Social tab, you choose what you share: movies, series, books, ratings',
          'Your personal notes are never shared',
          'They see the reactions you leave on their titles',
          'During a remote “Tonight?” game, they see the suggested movies, then your votes at the end of the game',
        ]} />
        <p className="mt-2">No one but your friends can see this, and you can remove a friend at any time. When you share a title by link, the link includes your first name, so the person who gets it knows who recommends it.</p>
      </Section>

      <Section title="4. Hosting and third-party services">
        <p>NookMind relies on the following services, which process data on our behalf:</p>
        <List items={[
          <><a href="https://supabase.com" className={link} target="_blank" rel="noopener noreferrer">Supabase</a>: database hosting and authentication</>,
          'Vercel: website and server functions hosting',
          'Google Firebase Cloud Messaging: notification delivery',
          'Google and Apple: signing in to your account (OAuth 2.0), if you choose to',
        ]} />
        <p className="mt-2">To show book, movie and series details, the app queries TMDB, Google Books and IMDb. These services receive your search terms and the titles you view, but never your email address or account ID.</p>
      </Section>

      <Section title="5. Retention">
        <p>Your data is kept for as long as your account exists. It is permanently deleted when you delete your account.</p>
      </Section>

      <Section id="delete-account" title="6. Delete your account">
        <p><strong>From the NookMind app:</strong> open Settings, tap “Delete account”, then confirm with “Yes, delete my account”. Deletion is immediate.</p>
        <p className="mt-2"><strong>Without the app:</strong> email {mail} from your account's email address, with the subject “NookMind account deletion”. We delete the account within 30 days at most and confirm by email.</p>
        <DeleteRequestButton
          label="Request account deletion"
          subject="NookMind account deletion"
          body={"Hello,\n\nI would like my NookMind account and all associated data to be deleted.\n\nAccount email address: \n\nThank you."}
        />
        <p className="mt-2">Either way, we delete your account, books, movies, series, collections, notes, ratings, profile, friends, reactions, “Tonight?” games and notification subscriptions. No data is kept after deletion.</p>
      </Section>

      <Section title="7. Your rights">
        <p>Under the GDPR, you can access, correct, export or erase your data by emailing {mail}. You can also lodge a complaint with the <a href="https://www.cnil.fr/en" className={link} target="_blank" rel="noopener noreferrer">CNIL</a>, the French data protection authority.</p>
      </Section>

      <Section title="8. Contact">
        <p>For any question about your personal data: {mail}.</p>
      </Section>
    </>
  );
}

/** `section` scrolls to that section on load, for URLs like /delete-account that store listings link to. */
export default function Privacy({ section }: { section?: string }) {
  const { i18n } = useTranslation();
  const [lang, setLang] = useState<Lang>(i18n.language?.startsWith('fr') ? 'fr' : 'en');
  const fr = lang === 'fr';

  // The page renders client-side, so the browser cannot jump to #delete-account on load by itself.
  useEffect(() => {
    const id = section ?? window.location.hash.slice(1);
    if (id) document.getElementById(id)?.scrollIntoView();
  }, [section]);

  return (
    <div className="min-h-screen bg-[#f8f6f1] dark:bg-[#0f1117] py-12 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="flex items-start justify-between gap-4 mb-2">
          <h1 className="font-serif text-3xl font-bold text-gray-900 dark:text-gray-100">
            {fr ? 'Politique de confidentialité' : 'Privacy policy'}
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
          {fr ? 'Dernière mise à jour : octobre 2026' : 'Last updated: October 2026'}
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
