import { useEffect, useRef, useState } from 'react';
import { BrowserMultiFormatReader } from '@zxing/browser';
import type { IScannerControls } from '@zxing/browser';
import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface Props {
  onDetected: (isbn: string) => void;
  onClose: () => void;
}

export default function BarcodeScannerModal({ onDetected, onClose }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const controlsRef = useRef<IScannerControls | null>(null);
  const detectedRef = useRef(false);
  const [error, setError] = useState('');
  const { i18n } = useTranslation();
  const isFr = i18n.language.startsWith('fr');

  useEffect(() => {
    if (!videoRef.current) return;

    const reader = new BrowserMultiFormatReader();

    reader
      .decodeFromConstraints(
        { video: { facingMode: 'environment' } },
        videoRef.current,
        (result) => {
          if (!result || detectedRef.current) return;
          const text = result.getText();
          const isIsbn13 = /^97[89]\d{10}$/.test(text);
          const isIsbn10 = /^\d{10}$/.test(text);
          if (isIsbn13 || isIsbn10) {
            detectedRef.current = true;
            controlsRef.current?.stop();
            onDetected(text);
          }
        }
      )
      .then((controls) => {
        controlsRef.current = controls;
      })
      .catch(() => {
        setError(
          isFr
            ? "Impossible d'accéder à la caméra. Vérifiez les permissions."
            : "Cannot access camera. Check your permissions."
        );
      });

    return () => {
      controlsRef.current?.stop();
    };
  }, []);

  return (
    <div className="fixed inset-0 z-50 bg-black flex flex-col animate-fade-in">
      {/* Top bar */}
      <div className="flex items-center justify-between px-4 py-3 bg-black/60">
        <p className="text-white text-sm font-medium">
          {isFr ? 'Scannez le code-barre du livre' : 'Scan the book barcode'}
        </p>
        <button
          onClick={onClose}
          className="text-white p-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <X size={22} />
        </button>
      </div>

      {/* Camera */}
      <div className="flex-1 relative overflow-hidden">
        <video ref={videoRef} className="w-full h-full object-cover" />

        {/* Dark overlay with transparent window */}
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
          <div className="w-full h-full absolute bg-black/50" />

          {/* Viewfinder */}
          <div className="relative z-10 w-72 h-36">
            {/* Clear window */}
            <div className="absolute inset-0 bg-transparent" style={{ boxShadow: '0 0 0 9999px rgba(0,0,0,0.5)' }} />

            {/* Animated scan line */}
            <div className="absolute inset-x-0 top-1/2 h-0.5 bg-amber-400/70 animate-pulse" />

            {/* Corner brackets */}
            <span className="absolute top-0 left-0 w-6 h-6 border-t-2 border-l-2 border-amber-400 rounded-tl-sm" />
            <span className="absolute top-0 right-0 w-6 h-6 border-t-2 border-r-2 border-amber-400 rounded-tr-sm" />
            <span className="absolute bottom-0 left-0 w-6 h-6 border-b-2 border-l-2 border-amber-400 rounded-bl-sm" />
            <span className="absolute bottom-0 right-0 w-6 h-6 border-b-2 border-r-2 border-amber-400 rounded-br-sm" />
          </div>
        </div>
      </div>

      {/* Bottom hint */}
      <div className="px-4 py-5 text-center bg-black/60">
        {error ? (
          <p className="text-red-400 text-sm">{error}</p>
        ) : (
          <p className="text-gray-400 text-sm">
            {isFr
              ? 'Pointez la caméra vers le code-barre au dos du livre'
              : 'Point the camera at the barcode on the back of the book'}
          </p>
        )}
      </div>
    </div>
  );
}
