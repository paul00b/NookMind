import { Hammer } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export default function Discover() {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] px-4 text-center">
      <div className="w-16 h-16 bg-amber-500/10 rounded-2xl flex items-center justify-center mb-5">
        <Hammer size={30} className="text-amber-500" />
      </div>
      <h1 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100 mb-2">
        {t('discover.title')}
      </h1>
      <p className="text-gray-500 dark:text-gray-400 text-sm max-w-xs">
        {t('discover.description')}
      </p>
    </div>
  );
}
