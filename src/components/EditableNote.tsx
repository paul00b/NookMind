import { useState } from 'react';
import { Pencil, Check } from 'lucide-react';

interface Props {
  note: string | null | undefined;
  labelText: string;
  placeholderText: string;
  saveText: string;
  cancelText: string;
  noNotesText: string;
  onSave: (note: string) => void;
}

export default function EditableNote({
  note,
  labelText,
  placeholderText,
  saveText,
  cancelText,
  noNotesText,
  onSave,
}: Props) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState('');

  const handleStartEdit = () => {
    setDraft(note || '');
    setEditing(true);
  };

  const handleSave = () => {
    onSave(draft);
    setEditing(false);
  };

  const handleCancel = () => {
    setEditing(false);
  };

  return (
    <div>
      <div className="flex items-center gap-2 mb-1">
        <p className="text-sm text-gray-500 dark:text-gray-400">{labelText}</p>
        {!editing && (
          <button onClick={handleStartEdit} className="text-gray-400 hover:text-amber-500 transition-colors">
            <Pencil size={13} />
          </button>
        )}
      </div>
      {editing ? (
        <div className="space-y-2">
          <textarea
            value={draft}
            onChange={e => setDraft(e.target.value)}
            className="input text-sm h-24 resize-none"
            placeholder={placeholderText}
            autoFocus
          />
          <div className="flex gap-2">
            <button onClick={handleSave} className="btn-primary text-sm py-1.5 flex items-center gap-1">
              <Check size={14} /> {saveText}
            </button>
            <button onClick={handleCancel} className="btn-ghost text-sm py-1.5">
              {cancelText}
            </button>
          </div>
        </div>
      ) : (
        <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
          {note || <span className="text-gray-400 italic">{noNotesText}</span>}
        </p>
      )}
    </div>
  );
}
