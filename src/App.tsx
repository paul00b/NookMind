import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { BooksProvider } from './context/BooksContext';
import ProtectedRoute from './router/ProtectedRoute';
import AppLayout from './components/AppLayout';
import BottomNav from './components/BottomNav';
import Login from './pages/Login';
import Home from './pages/Home';
import Library from './pages/Library';
import Discover from './pages/Discover';

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <BooksProvider>
                    <AppLayout />
                  </BooksProvider>
                </ProtectedRoute>
              }
            >
              <Route index element={<Home />} />
              <Route path="library" element={<Library />} />
              <Route path="discover" element={<Discover />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Routes>

          {/* Always-visible bottom nav on mobile */}
          <BottomNav />

          <Toaster
            position="bottom-center"
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
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}
