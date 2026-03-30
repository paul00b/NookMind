export default function Privacy() {
  return (
    <div className="min-h-screen bg-[#f8f6f1] dark:bg-[#0f1117] py-12 px-4">
      <div className="max-w-2xl mx-auto">
        <h1 className="font-serif text-3xl font-bold text-gray-900 dark:text-gray-100 mb-2">Politique de confidentialité</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-8">Dernière mise à jour : mars 2025</p>

        <div className="space-y-6 text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
          <section>
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">1. Données collectées</h2>
            <p>NookMind collecte uniquement les données nécessaires au fonctionnement de l'application :</p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>Adresse e-mail et identifiant de compte (via Google OAuth ou inscription directe)</li>
              <li>Données de bibliothèque : livres, films et séries ajoutés, notes personnelles, statuts de lecture/visionnage</li>
            </ul>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">2. Utilisation des données</h2>
            <p>Vos données sont utilisées exclusivement pour :</p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>Vous authentifier et sécuriser votre compte</li>
              <li>Stocker et synchroniser votre bibliothèque personnelle</li>
            </ul>
            <p className="mt-2">Nous ne vendons pas, ne partageons pas et ne monétisons pas vos données personnelles.</p>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">3. Hébergement et sécurité</h2>
            <p>Les données sont stockées sur <a href="https://supabase.com" className="text-amber-600 hover:underline" target="_blank" rel="noopener noreferrer">Supabase</a>, un service sécurisé conforme aux standards RGPD. L'authentification Google est gérée via OAuth 2.0.</p>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">4. Suppression des données</h2>
            <p>Vous pouvez demander la suppression de votre compte et de toutes vos données à tout moment en nous contactant.</p>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">5. Contact</h2>
            <p>Pour toute question relative à vos données personnelles, contactez-nous à l'adresse associée au projet.</p>
          </section>
        </div>

        <a href="/login" className="inline-block mt-10 text-sm text-amber-600 hover:underline">← Retour</a>
      </div>
    </div>
  );
}
