import { useState } from 'react';

export interface VideoPlayerProps {
    url: string;
    title?: string;
}

export function VideoPlayer({ url, title }: VideoPlayerProps) {
    const [error, setError] = useState<string | null>(null);

    // Extract YouTube video ID from URL
    const getYouTubeId = (url: string): string | null => {
        const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|&v=)([^#&?]*).*/;
        const match = url.match(regExp);
        return (match && match[2].length === 11) ? match[2] : null;
    };

    const videoId = getYouTubeId(url);

    if (!videoId) {
        return (
            <div className="relative bg-black rounded-lg overflow-hidden w-full aspect-video shadow-lg flex items-center justify-center">
                <p className="text-red-500 font-bold">Невірний формат URL відео</p>
            </div>
        );
    }

    // YouTube embed URL with maximum privacy and restriction parameters
    const embedUrl = `https://www.youtube-nocookie.com/embed/${videoId}?` + new URLSearchParams({
        rel: '0',                    // Don't show related videos
        modestbranding: '1',         // Minimal YouTube branding
        controls: '1',               // Show player controls
        showinfo: '0',               // Hide video title
        fs: '1',                     // Allow fullscreen
        iv_load_policy: '3',         // Hide video annotations
        disablekb: '0',              // Enable keyboard controls
        playsinline: '1',            // Play inline on iOS
        origin: window.location.origin, // Restrict to current domain
    }).toString();

    return (
        <div
            className="relative bg-black rounded-lg overflow-hidden w-full aspect-video shadow-lg"
            onClick={(e) => e.stopPropagation()}
        >
            <iframe
                src={embedUrl}
                title={title || "Video Player"}
                className="absolute top-0 left-0 w-full h-full"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                onError={() => setError("Помилка завантаження відео")}
                sandbox="allow-scripts allow-same-origin allow-presentation"
            />

            {error && (
                <div className="absolute inset-0 flex items-center justify-center bg-black/80 z-40 text-white flex-col">
                    <p className="text-red-500 font-bold mb-2">{error}</p>
                    <p className="text-xs text-gray-400">URL: {url}</p>
                </div>
            )}

            {/* Overlay to block YouTube logo and "Copy link" button */}
            <div
                className="absolute top-0 right-0 w-32 h-16 z-10 cursor-default"
                onClick={(e) => {
                    e.stopPropagation();
                    e.preventDefault();
                }}
                onMouseDown={(e) => {
                    e.stopPropagation();
                    e.preventDefault();
                }}
                onContextMenu={(e) => {
                    e.preventDefault();
                }}
                title="Відео доступне тільки на цій платформі"
            />

            {/* Overlay to block bottom-right controls area (settings, YouTube logo) */}
            <div
                className="absolute bottom-0 right-0 w-24 h-12 z-10 cursor-default"
                onClick={(e) => {
                    e.stopPropagation();
                    e.preventDefault();
                }}
                onMouseDown={(e) => {
                    e.stopPropagation();
                    e.preventDefault();
                }}
                onContextMenu={(e) => {
                    e.preventDefault();
                }}
            />
        </div>
    );
}
