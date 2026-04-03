import { Bell, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import SheetModal, { SheetCloseButton } from './SheetModal';
import { useNotificationSubscription } from '../hooks/useNotificationSubscription';
import toast from 'react-hot-toast';

interface Props {
  onDismiss: () => void;
}

export default function NotificationPromptSheet({ onDismiss }: Props) {
  const { i18n } = useTranslation();
  const isFr = i18n.language.startsWith('fr');
  const { subscribe, loading, permission } = useNotificationSubscription();

  const handleEnable = async () => {
    const ok = await subscribe();
    if (ok) {
      toast.success(isFr ? 'Notifications activées !' : 'Notifications enabled!');
      onDismiss();
    } else if (permission === 'denied') {
      onDismiss();
    }
  };

  return (
    <SheetModal
      onClose={onDismiss}
      panelClassName="bg-[#f8f6f1] dark:bg-[#1a1f2e] rounded-t-3xl md:rounded-3xl shadow-2xl animate-slide-up px-6 pt-5 pb-8 md:max-w-sm"
    >
      <SheetCloseButton className="absolute top-4 right-4 p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors">
        <X size={18} />
      </SheetCloseButton>

      {/* Icon */}
      <div className="flex items-center gap-4 mb-4">
        <div className="w-14 h-14 rounded-2xl bg-teal-500/10 flex items-center justify-center shrink-0">
          <Bell size={26} className="text-teal-500" />
        </div>
        <div>
          <h2 className="font-serif text-lg font-bold text-gray-900 dark:text-gray-100">
            {isFr ? 'Restez informé' : 'Stay up to date'}
          </h2>
          <p className="text-xs text-teal-600 dark:text-teal-400 font-medium mt-0.5">
            {isFr ? 'Notifications NookMind' : 'NookMind notifications'}
          </p>
        </div>
      </div>

      <p className="text-sm text-gray-600 dark:text-gray-400 leading-relaxed mb-6">
        {isFr
          ? 'Activez les notifications pour être prévenu le jour de la sortie de vos épisodes et films préférés — sans avoir à ouvrir l\'app.'
          : 'Enable notifications to be notified on the release day of your favourite episodes and movies — without opening the app.'}
      </p>

      <div className="flex gap-3">
        <button onClick={onDismiss} className="btn-ghost flex-1 text-sm">
          {isFr ? 'Plus tard' : 'Maybe later'}
        </button>
        <button
          onClick={handleEnable}
          disabled={loading}
          className="flex-1 flex items-center justify-center gap-2 bg-teal-500 hover:bg-teal-600 text-white font-medium rounded-full px-5 py-2.5 transition-all duration-150 active:scale-95 disabled:opacity-60 text-sm"
        >
          <Bell size={14} />
          {loading
            ? (isFr ? 'Activation…' : 'Enabling…')
            : (isFr ? 'Activer' : 'Enable')}
        </button>
      </div>
    </SheetModal>
  );
}
