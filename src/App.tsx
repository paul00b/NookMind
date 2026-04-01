import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { Analytics } from '@vercel/analytics/react';
import { useMediaMode } from './context/MediaModeContext';
import ProtectedRoute from './router/ProtectedRoute';
import AppLayout from './components/AppLayout';
import BottomNav from './components/BottomNav';
import AppProviders from './app/AppProviders';
import { renderByMediaMode } from './app/mediaMode';
import Login from './pages/Login';
import AuthCallback from './pages/AuthCallback';
import Home from './pages/Home';
import Library from './pages/Library';
import NextUpSeries from './pages/NextUpSeries';
import NextUpBooks from './pages/NextUpBooks';
import NextUpMovies from './pages/NextUpMovies';
import MovieHome from './pages/MovieHome';
import MovieLibrary from './pages/MovieLibrary';
import SeriesHome from './pages/SeriesHome';
import SeriesLibrary from './pages/SeriesLibrary';
import Privacy from './pages/Privacy';
import Terms from './pages/Terms';

function HomeSwitch() {
  const { mode } = useMediaMode();
  return renderByMediaMode(mode, {
    books: <Home />,
    movies: <MovieHome />,
    series: <SeriesHome />,
  });
}

function LibrarySwitch() {
  const { mode } = useMediaMode();
  return renderByMediaMode(mode, {
    books: <Library />,
    movies: <MovieLibrary />,
    series: <SeriesLibrary />,
  });
}

function NextUpSwitch() {
  const { mode } = useMediaMode();
  return renderByMediaMode(mode, {
    books: <NextUpBooks />,
    movies: <NextUpMovies />,
    series: <NextUpSeries />,
  });
}

export default function App() {
  return (
    <AppProviders>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/auth/callback" element={<AuthCallback />} />
          <Route path="/privacy" element={<Privacy />} />
          <Route path="/terms" element={<Terms />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<HomeSwitch />} />
            <Route path="library" element={<LibrarySwitch />} />
            <Route path="discover" element={<NextUpSwitch />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>

        <BottomNav />

        <Toaster
          position="bottom-center"
          containerStyle={{ bottom: 96 }}
          toastOptions={{
            duration: 3000,
            style: {
              background: 'var(--toast-bg, #1a1f2e)',
              color: 'var(--toast-color, #f3f4f6)',
              borderRadius: '12px',
              fontSize: '14px',
              fontFamily: 'Inter, sans-serif',
              padding: '12px 16px',
              boxShadow: '0 4px 24px rgba(0,0,0,0.3)',
            },
            success: {
              iconTheme: { primary: '#f59e0b', secondary: '#fff' },
            },
          }}
        />
        <Analytics />
      </BrowserRouter>
    </AppProviders>
  );
}
