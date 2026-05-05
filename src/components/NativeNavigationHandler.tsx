import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function NativeNavigationHandler() {
  const navigate = useNavigate();

  useEffect(() => {
    const handler = (e: Event) => {
      const route = (e as CustomEvent<{ route: string }>).detail?.route;
      if (route) navigate(route);
    };
    window.addEventListener('nookmind:navigate', handler);
    return () => window.removeEventListener('nookmind:navigate', handler);
  }, [navigate]);

  return null;
}
