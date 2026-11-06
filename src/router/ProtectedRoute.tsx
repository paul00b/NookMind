import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { isWeb } from '../lib/platform';
import Landing from '../pages/Landing';

interface Props {
  children: React.ReactNode;
}

export default function ProtectedRoute({ children }: Props) {
  const { user, loading } = useAuth();
  const { pathname } = useLocation();

  if (loading) {
    return (
      <div className="min-h-screen bg-[#f8f6f1] dark:bg-[#0f1117] flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <img src="/logo.png" alt="NookMind" className="w-12 h-12 animate-pulse" />
          <div className="w-5 h-5 border-2 border-amber-500/30 border-t-amber-500 rounded-full animate-spin" />
        </div>
      </div>
    );
  }

  // Visitors of the site who are not signed in land on the presentation of the app, the page
  // the store listings and shared links point to; the sign-in is one tap from there.
  if (!user) return isWeb() && pathname === '/' ? <Landing /> : <Navigate to="/login" replace />;

  return <>{children}</>;
}
