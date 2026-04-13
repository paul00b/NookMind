import { useRef, useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import { Star, Bell, Drama, Satellite } from 'lucide-react';
import OnboardingSlide from '../components/OnboardingSlide';

const ONBOARDING_KEY = 'nookmind_onboarding_completed';

const SLIDES = [
  { accentColor: '#f59e0b', gradientColor: '#f59e0b' },
  { accentColor: '#6366f1', gradientColor: '#6366f1' },
  { accentColor: '#14b8a6', gradientColor: '#14b8a6' },
];

/* ── Illustrations ── */

function Slide1Illustration() {
  return (
    <div className="relative w-40 h-40 flex items-center justify-center">
      {/* Floating background icons */}
      <span className="absolute top-0 left-0 text-3xl opacity-15 animate-[float1_6s_ease-in-out_infinite]">🎬</span>
      <span className="absolute top-4 right-0 text-2xl opacity-12 animate-[float2_7s_ease-in-out_infinite]">📺</span>
      <span className="absolute bottom-2 left-4 text-2xl opacity-10 animate-[float3_8s_ease-in-out_infinite]">📖</span>
      {/* Logo */}
      <img src="/logo.svg" alt="NookMind" className="w-20 h-20 drop-shadow-[0_8px_32px_rgba(245,158,11,0.27)]" />
    </div>
  );
}

function Slide2Illustration() {
  const icons = [
    { Icon: Star, delay: '0s' },
    { Icon: Bell, delay: '1.5s' },
    { Icon: Drama, delay: '3s' },
    { Icon: Satellite, delay: '4.5s' },
  ];
  return (
    <div className="flex gap-5 flex-wrap justify-center">
      {icons.map(({ Icon, delay }, i) => (
        <div
          key={i}
          className="w-14 h-14 rounded-[14px] bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center"
          style={{ animation: `float${(i % 3) + 1} ${6 + i}s ease-in-out ${delay} infinite` }}
        >
          <Icon size={24} className="text-indigo-400" />
        </div>
      ))}
    </div>
  );
}

function Slide3Illustration() {
  return (
    <div className="flex gap-3 items-end">
      <div className="w-12 h-[68px] rounded-lg bg-gradient-to-br from-teal-500/20 to-teal-500/5 border border-teal-500/20" />
      <div className="w-12 h-[68px] rounded-lg bg-gradient-to-br from-teal-500/30 to-teal-500/10 border border-teal-500/30 scale-110" />
      <div className="w-12 h-[68px] rounded-lg bg-gradient-to-br from-teal-500/20 to-teal-500/5 border border-teal-500/20" />
    </div>
  );
}

/* ── Page ── */

export default function Onboarding() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { user } = useAuth();
  const scrollRef = useRef<HTMLDivElement>(null);
  const [activeIndex, setActiveIndex] = useState(0);

  const finish = useCallback(() => {
    localStorage.setItem(ONBOARDING_KEY, 'true');
    navigate(user ? '/' : '/login', { replace: true });
  }, [navigate, user]);

  // Track active slide via IntersectionObserver
  useEffect(() => {
    const container = scrollRef.current;
    if (!container) return;

    const slides = container.children;
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const index = Array.from(slides).indexOf(entry.target as HTMLElement);
            if (index >= 0) setActiveIndex(index);
          }
        });
      },
      { root: container, threshold: 0.5 }
    );

    Array.from(slides).forEach((slide) => observer.observe(slide));
    return () => observer.disconnect();
  }, []);

  const illustrations = [<Slide1Illustration />, <Slide2Illustration />, <Slide3Illustration />];
  const titleKeys = ['onboarding.slide1Title', 'onboarding.slide2Title', 'onboarding.slide3Title'] as const;
  const bodyKeys = ['onboarding.slide1Body', 'onboarding.slide2Body', 'onboarding.slide3Body'] as const;

  return (
    <div className="relative w-screen h-[100svh] overflow-hidden bg-[#0f1117]">
      {/* Skip button */}
      <button
        onClick={finish}
        className="absolute top-4 right-4 z-20 text-sm text-gray-500 hover:text-gray-300 transition-colors px-3 py-1.5"
        style={{ paddingTop: 'calc(0.375rem + env(safe-area-inset-top))' }}
      >
        {t('onboarding.skip')}
      </button>

      {/* Carousel */}
      <div
        ref={scrollRef}
        className="flex w-full h-full overflow-x-auto snap-x snap-mandatory scrollbar-none"
        style={{ scrollbarWidth: 'none' }}
      >
        {SLIDES.map((slide, i) => (
          <OnboardingSlide
            key={i}
            title={t(titleKeys[i])}
            body={t(bodyKeys[i])}
            accentColor={slide.accentColor}
            gradientColor={slide.gradientColor}
            illustration={illustrations[i]}
          />
        ))}
      </div>

      {/* Bottom area: progress dots + Get Started */}
      <div className="absolute bottom-0 left-0 right-0 z-20 flex flex-col items-center gap-5 pb-12">
        {/* Get Started button — only on last slide */}
        {activeIndex === SLIDES.length - 1 && (
          <button
            onClick={finish}
            className="px-8 py-3 rounded-full font-semibold text-white text-sm shadow-[0_4px_20px_rgba(20,184,166,0.27)]"
            style={{ background: 'linear-gradient(135deg, #14b8a6, #0d9488)' }}
          >
            {t('onboarding.getStarted')}
          </button>
        )}

        {/* Progress dots */}
        <div className="flex gap-1.5">
          {SLIDES.map((slide, i) => (
            <div
              key={i}
              className="h-1.5 rounded-full transition-all duration-300"
              style={{
                width: activeIndex === i ? 20 : 6,
                backgroundColor: activeIndex === i ? slide.accentColor : '#4b5563',
              }}
            />
          ))}
        </div>
      </div>

      {/* Floating icon keyframes */}
      <style>{`
        @keyframes float1 {
          0%, 100% { transform: translate(0, 0); }
          50% { transform: translate(4px, -6px); }
        }
        @keyframes float2 {
          0%, 100% { transform: translate(0, 0); }
          50% { transform: translate(-5px, 5px); }
        }
        @keyframes float3 {
          0%, 100% { transform: translate(0, 0); }
          50% { transform: translate(3px, 4px); }
        }
      `}</style>
    </div>
  );
}
