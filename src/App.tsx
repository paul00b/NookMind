import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { Analytics } from '@vercel/analytics/react';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { MediaModeProvider } from './context/MediaModeContext';
import { BooksProvider } from './context/BooksContext';
import { CategoriesProvider } from './context/CategoriesContext';
import { MoviesProvider } from './context/MoviesContext';
import { MovieCategoriesProvider } from './context/MovieCategoriesContext';
import { SeriesProvider } from './context/SeriesContext';
import { SeriesCategoriesProvider } from './context/SeriesCategoriesContext';
import { useMediaMode } from './context/MediaModeContext';
import ProtectedRoute from './router/ProtectedRoute';
import AppLayout from './components/AppLayout';
import BottomNav from './components/BottomNav';
import Login from './pages/Login';
import AuthCallback from './pages/AuthCallback';
import Home from './pages/Home';
import Library from './pages/Library';
import Discover from './pages/Discover';
import MovieHome from './pages/MovieHome';
import MovieLibrary from './pages/MovieLibrary';
import SeriesHome from './pages/SeriesHome';
import SeriesLibrary from './pages/SeriesLibrary';

function HomeSwitch() {
  const { mode } = useMediaMode();
  if (mode === 'movies') return <MovieHome />;
  if (mode === 'series') return <SeriesHome />;
  return <Home />;
}

function LibrarySwitch() {
  const { mode } = useMediaMode();
  if (mode === 'movies') return <MovieLibrary />;
  if (mode === 'series') return <SeriesLibrary />;
  return <Library />;
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <MediaModeProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="/auth/callback" element={<AuthCallback />} />
              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <BooksProvider>
                      <CategoriesProvider>
                        <MoviesProvider>
                          <MovieCategoriesProvider>
                            <SeriesProvider>
                              <SeriesCategoriesProvider>
                                <AppLayout />
                              </SeriesCategoriesProvider>
                            </SeriesProvider>
                          </MovieCategoriesProvider>
                        </MoviesProvider>
                      </CategoriesProvider>
                    </BooksProvider>
                  </ProtectedRoute>
                }
              >
                <Route index element={<HomeSwitch />} />
                <Route path="library" element={<LibrarySwitch />} />
                <Route path="discover" element={<Discover />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Route>
            </Routes>

            {/* Always-visible bottom nav on mobile */}
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
        </MediaModeProvider>
      </AuthProvider>
    </ThemeProvider>
  );
}
