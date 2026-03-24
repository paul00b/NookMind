import { Compass } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export default function Discover() {
  const { t } = useTranslation();

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 pb-36 md:pb-8">
      <div className="flex items-center gap-3 mb-8">
        <div className="w-10 h-10 bg-amber-500/10 rounded-xl flex items-center justify-center">
          <Compass size={20} className="text-amber-500" />
        </div>
        <div>
          <h1 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100">{t('discover.title')}</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">{t('discover.subtitle')}</p>
        </div>
      </div>

      <div className="flex flex-col items-center justify-center py-24 gap-4 text-center">
        <div className="w-16 h-16 bg-amber-500/10 rounded-2xl flex items-center justify-center">
          <Compass size={32} className="text-amber-500" />
        </div>
        <h2 className="font-serif text-xl font-bold text-gray-900 dark:text-gray-100">Coming Soon</h2>
        <p className="text-sm text-gray-500 dark:text-gray-400 max-w-xs">
          The Discovery section is under construction. Check back soon!
        </p>
      </div>
    </div>
  );
}
