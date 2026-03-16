import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { supabase } from '../lib/supabase';
import { BookOpen } from 'lucide-react';

export default function AuthCallback() {
  const navigate = useNavigate();

  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      navigate(session ? '/' : '/login', { replace: true });
    });
  }, [navigate]);

  return (
    <div className="min-h-screen bg-[#f8f6f1] dark:bg-[#0f1117] flex items-center justify-center">
      <div className="flex flex-col items-center gap-4">
        <div className="w-14 h-14 bg-amber-500 rounded-2xl flex items-center justify-center shadow-lg">
          <BookOpen size={28} className="text-white" />
        </div>
        <span className="w-6 h-6 border-2 border-amber-500/30 border-t-amber-500 rounded-full animate-spin" />
      </div>
    </div>
  );
}
