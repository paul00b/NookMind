import { X, Download, Share } from 'lucide-react';
import { useInstallPrompt } from '../hooks/useInstallPrompt';
import { useTranslation } from 'react-i18next';
import SheetModal, { SheetCloseButton } from './SheetModal';

interface Props {
  onDismiss: () => void;
}

const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) ||
  (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

export default function InstallPromptSheet({ onDismiss }: Props) {
  const { canInstall, triggerInstall } = useInstallPrompt();
  const { i18n } = useTranslation();
  const isFr = i18n.language.startsWith('fr');

  const handleInstall = async () => {
    const accepted = await triggerInstall();
    if (accepted) onDismiss();
  };

  return (
    <SheetModal
      onClose={onDismiss}
      rootClassName="z-50 md:hidden"
      overlayClassName="bg-black/40 backdrop-blur-sm animate-fade-in"
      panelClassName="bg-[#f8f6f1] dark:bg-[#1a1f2e] rounded-t-3xl shadow-2xl animate-slide-up px-6 pt-5 pb-8"
    >
        {/* Dismiss */}
        <SheetCloseButton
          className="absolute top-4 right-4 p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
        >
          <X size={18} />
        </SheetCloseButton>

        {/* Header */}
        <div className="flex items-center gap-4 mb-4">
          <img src="/logo.svg" className="w-14 h-14" alt="" />
          <div>
            <h2 className="font-serif text-xl font-bold text-gray-900 dark:text-gray-100">
              {isFr ? 'Installez NookMind' : 'Install NookMind'}
            </h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
              {isFr ? "Accès rapide depuis votre écran d'accueil" : 'Quick access from your home screen'}
            </p>
          </div>
        </div>

        {/* Instructions */}
        {isIOS ? (
          // iOS: manual instructions
          <div className="bg-white dark:bg-[#0f1117] rounded-2xl p-4 mb-5 space-y-3">
            <Step number={1}>
              <span className="text-sm text-gray-600 dark:text-gray-400">
                {isFr ? 'Appuyez sur ' : 'Tap '}
                <span className="inline-flex items-center gap-1 font-medium text-gray-800 dark:text-gray-200">
                  <Share size={13} className="text-blue-500" />
                  {isFr ? 'Partager' : 'Share'}
                </span>
                {isFr ? ' dans Safari' : ' in Safari'}
              </span>
            </Step>
            <Step number={2}>
              <span className="text-sm text-gray-600 dark:text-gray-400">
                {isFr
                  ? "Sélectionnez « Sur l'écran d'accueil »"
                  : 'Select "Add to Home Screen"'}
              </span>
            </Step>
            <Step number={3}>
              <span className="text-sm text-gray-600 dark:text-gray-400">
                {isFr ? 'Appuyez sur « Ajouter »' : 'Tap "Add"'}
              </span>
            </Step>
          </div>
        ) : canInstall ? (
          // Android/Chrome: one-tap install
          <p className="text-sm text-gray-600 dark:text-gray-400 mb-5">
            {isFr
              ? "Ajoutez NookMind à votre écran d'accueil en un tap pour un accès rapide, même hors connexion."
              : 'Add NookMind to your home screen for quick access, even offline.'}
          </p>
        ) : (
          // Other mobile browser: manual
          <div className="bg-white dark:bg-[#0f1117] rounded-2xl p-4 mb-5">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              {isFr
                ? "Ouvrez le menu de votre navigateur et sélectionnez « Ajouter à l'écran d'accueil »."
                : 'Open your browser menu and select "Add to Home Screen".'}
            </p>
          </div>
        )}

        {/* Actions */}
        <div className="flex gap-3">
          {canInstall && !isIOS ? (
            <>
              <button onClick={onDismiss} className="btn-ghost flex-1 text-sm">
                {isFr ? 'Plus tard' : 'Maybe later'}
              </button>
              <button onClick={handleInstall} className="btn-primary flex-1 flex items-center justify-center gap-2 text-sm">
                <Download size={15} />
                {isFr ? 'Installer' : 'Install'}
              </button>
            </>
          ) : (
            <button onClick={onDismiss} className="btn-primary w-full text-sm">
              {isFr ? 'OK, compris !' : 'Got it!'}
            </button>
          )}
        </div>
    </SheetModal>
  );
}

function Step({ number, children }: { number: number; children: React.ReactNode }) {
  return (
    <div className="flex items-start gap-3">
      <span className="w-5 h-5 rounded-full bg-amber-500 text-white text-xs font-bold flex items-center justify-center flex-shrink-0 mt-0.5">
        {number}
      </span>
      <div>{children}</div>
    </div>
  );
}
