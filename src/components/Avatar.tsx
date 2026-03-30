interface AvatarProps {
    name: string;
    imageUrl?: string | null;
    size?: 'sm' | 'md' | 'lg';
    className?: string;
}

const SIZES = { sm: 'w-8 h-8 text-xs', md: 'w-10 h-10 text-sm', lg: 'w-12 h-12 text-base' };

const COLORS = [
    'bg-violet-500', 'bg-blue-500', 'bg-emerald-500',
    'bg-rose-500', 'bg-amber-500', 'bg-indigo-500',
];

function getColor(name: string) {
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
    return COLORS[Math.abs(hash) % COLORS.length];
}

function getInitials(name: string) {
    return name.split(/\s+/).slice(0, 2).map(w => w[0]?.toUpperCase() ?? '').join('');
}

export default function Avatar({ name, imageUrl, size = 'md', className = '' }: AvatarProps) {
    const baseClasses = `${SIZES[size]} rounded-full flex-shrink-0 ${className}`;

    // Si une image est fournie, on l'affiche directement
    if (imageUrl) {
        return (
            <img
                src={imageUrl}
                alt={name}
                className={`${baseClasses} object-cover`}
            />
        );
    }

    // Sinon, on affiche les initiales avec la couleur générée
    return (
        <div className={`${baseClasses} ${getColor(name)} flex items-center justify-center text-white font-semibold`}>
            {getInitials(name || '?')}
        </div>
    );
}