export default function Terms() {
  return (
    <div className="min-h-screen bg-[#f8f6f1] dark:bg-[#0f1117] py-12 px-4">
      <div className="max-w-2xl mx-auto">
        <h1 className="font-serif text-3xl font-bold text-gray-900 dark:text-gray-100 mb-2">Conditions d'utilisation</h1>
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-8">Dernière mise à jour : mars 2025</p>

        <div className="space-y-6 text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
          <section>
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">1. Acceptation des conditions</h2>
            <p>En utilisant NookMind, vous acceptez les présentes conditions d'utilisation. Si vous n'acceptez pas ces conditions, veuillez ne pas utiliser l'application.</p>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">2. Description du service</h2>
            <p>NookMind est une application personnelle de gestion de bibliothèque permettant de suivre des livres, films et séries. Le service est fourni tel quel, sans garantie de disponibilité continue.</p>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">3. Utilisation acceptable</h2>
            <p>Vous vous engagez à utiliser NookMind uniquement à des fins personnelles et légales. Il est interdit :</p>
            <ul className="list-disc pl-5 mt-2 space-y-1">
              <li>D'utiliser le service à des fins commerciales</li>
              <li>De tenter d'accéder aux données d'autres utilisateurs</li>
              <li>D'automatiser des requêtes de manière abusive</li>
            </ul>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">4. Compte utilisateur</h2>
            <p>Vous êtes responsable de la sécurité de votre compte. En cas d'utilisation non autorisée de votre compte, contactez-nous immédiatement.</p>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">5. Limitation de responsabilité</h2>
            <p>NookMind ne peut être tenu responsable de la perte de données ou de toute interruption de service. Nous recommandons de ne pas stocker d'informations critiques uniquement dans l'application.</p>
          </section>

          <section>
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">6. Modifications</h2>
            <p>Nous nous réservons le droit de modifier ces conditions à tout moment. Les modifications seront notifiées via l'application.</p>
          </section>
        </div>

        <a href="/login" className="inline-block mt-10 text-sm text-amber-600 hover:underline">← Retour</a>
      </div>
    </div>
  );
}
