import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Loader2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

interface Props {
  open: boolean;
  onClose: () => void;
}

export function DeleteAccountDialog({ open, onClose }: Props) {
  const { t } = useTranslation();
  const { deleteAccount } = useAuth();
  const [busy, setBusy] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!open) return null;

  const handleConfirm = async () => {
    setBusy(true);
    setErrorMessage(null);
    const { error } = await deleteAccount();
    if (error) {
      setErrorMessage(error.message || t('settings.deleteAccountFailed'));
      setBusy(false);
      return;
    }
    onClose();
  };

  const handleCancel = () => {
    if (busy) return;
    setErrorMessage(null);
    onClose();
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-[60] flex items-center justify-center px-4"
    >
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm animate-fade-in"
        onClick={handleCancel}
      />
      <div className="relative z-10 w-full max-w-sm card p-6 animate-fade-in bg-[#f8f6f1] dark:bg-[#1a1f2e]">
        <h2 className="font-serif text-lg font-bold text-gray-900 dark:text-gray-100">
          {t('settings.deleteAccountConfirmTitle')}
        </h2>
        <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
          {t('settings.deleteAccountConfirmBody')}
        </p>
        {errorMessage && (
          <p className="mt-3 text-sm text-red-600 dark:text-red-400">
            {errorMessage}
          </p>
        )}
        <div className="mt-6 flex gap-3">
          <button
            type="button"
            onClick={handleCancel}
            disabled={busy}
            className="btn-ghost flex-1 text-sm disabled:opacity-60"
          >
            {t('settings.deleteAccountCancel')}
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={busy}
            className="flex-1 inline-flex items-center justify-center gap-2 rounded-xl px-3 py-2 text-sm font-semibold bg-red-600 text-white hover:bg-red-700 disabled:opacity-60 transition-colors"
          >
            {busy && <Loader2 size={14} className="animate-spin" />}
            {t('settings.deleteAccountConfirmCta')}
          </button>
        </div>
      </div>
    </div>
  );
}
