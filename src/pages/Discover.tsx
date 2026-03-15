import { Bell, Sparkles } from 'lucide-react';
import toast from 'react-hot-toast';

// TODO: Call AI recommendations endpoint
// Suggested prompt: "Here are the books I've read: {list of titles + authors + genres + ratings}.
// Based on my taste, suggest 5 authors I haven't read yet with a short explanation for each.
// Return JSON: [{ author, reason, example_book }]"

export default function Discover() {
  return (
    <div className="p-4 md:p-8 max-w-3xl mx-auto">
      {/* Header */}
      <div className="mb-10">
        <h1 className="font-serif text-3xl font-bold text-gray-900 dark:text-gray-100 mb-2">Discover</h1>
        <p className="text-gray-500 dark:text-gray-400">Personalized recommendations based on your reading taste</p>
      </div>

      {/* Coming soon card */}
      <div className="card p-8 text-center mb-8">
        <div className="w-16 h-16 bg-amber-500/10 rounded-2xl flex items-center justify-center mx-auto mb-5">
          <Sparkles size={32} className="text-amber-500" />
        </div>
        <h2 className="font-serif text-2xl font-semibold text-gray-900 dark:text-gray-100 mb-3">
          AI Recommendations Coming Soon
        </h2>
        <p className="text-gray-500 dark:text-gray-400 max-w-sm mx-auto mb-6 leading-relaxed">
          BookMind will analyze your reading history and suggest authors and books perfectly matched to your taste — powered by AI.
        </p>
        <button
          onClick={() => toast.success('You\'ll be notified when Discover launches!')}
          className="btn-primary inline-flex items-center gap-2"
        >
          <Bell size={16} /> Notify me
        </button>
      </div>

      {/* Preview skeleton cards */}
      <div>
        <p className="text-sm text-gray-400 dark:text-gray-500 mb-4 text-center italic">
          Based on your reading taste...
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="card p-4 flex gap-3 opacity-50">
              <div className="w-12 h-16 bg-gray-200 dark:bg-gray-700 rounded-lg flex-shrink-0" />
              <div className="flex-1 space-y-2.5 py-1">
                <div className="h-3.5 bg-gray-200 dark:bg-gray-700 rounded-full w-3/4" />
                <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full w-1/2" />
                <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full w-5/6" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
