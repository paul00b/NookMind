import type { ReactNode } from 'react';

interface Props {
  title: string;
  body: string;
  accentColor: string;       // e.g. '#f59e0b'
  gradientColor: string;     // e.g. '#f59e0b22' — used in radial gradient
  illustration: ReactNode;
}

export default function OnboardingSlide({ title, body, gradientColor, illustration }: Props) {
  return (
    <div
      className="w-screen h-[100svh] flex-shrink-0 snap-center snap-always flex flex-col items-center justify-center px-8 relative overflow-hidden"
      style={{
        backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/feTurbulence%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E"), radial-gradient(ellipse 80% 40% at 50% -10%, ${gradientColor}, transparent)`,
        backgroundSize: '256px 256px, 100% 100%',
        backgroundRepeat: 'repeat, no-repeat',
        backgroundColor: '#0f1117',
      }}
    >
      {/* Illustration area */}
      <div className="mb-8">
        {illustration}
      </div>

      {/* Text */}
      <h2 className="text-2xl font-bold text-gray-100 text-center mb-3 max-w-xs">
        {title}
      </h2>
      <p className="text-base text-gray-400 text-center leading-relaxed max-w-[280px]">
        {body}
      </p>
    </div>
  );
}
