import React, { useRef, useEffect } from 'react';

interface BackgroundVideoProps {
  overlayOpacity?: string; // 60-70% black transparent overlay, e.g. "bg-black/65"
  className?: string;
}

export const BackgroundVideo: React.FC<BackgroundVideoProps> = ({
  overlayOpacity = 'bg-black/65',
  className = '',
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    video.muted = true;
    video.defaultMuted = true;
    video.playsInline = true;
    video.loop = true;

    const playPromise = video.play();
    if (playPromise !== undefined) {
      playPromise.catch(() => {
        // Retry play on first user interaction if browser blocked initial autoplay
        const resumePlay = () => {
          if (videoRef.current) {
            videoRef.current.muted = true;
            videoRef.current.play().catch(() => {});
          }
          window.removeEventListener('pointerdown', resumePlay);
          window.removeEventListener('keydown', resumePlay);
        };
        window.addEventListener('pointerdown', resumePlay, { once: true });
        window.addEventListener('keydown', resumePlay, { once: true });
      });
    }

    return () => {
      if (video) {
        video.pause();
      }
    };
  }, []);

  return (
    <div
      className={`fixed inset-0 w-full h-full overflow-hidden pointer-events-none z-0 ${className}`}
      aria-hidden="true"
    >
      <video
        ref={videoRef}
        src="/background_video.mp4"
        autoPlay
        loop
        muted
        playsInline
        preload="auto"
        controls={false}
        disablePictureInPicture
        className="w-full h-full object-cover object-center select-none pointer-events-none"
      />
      {/* 60-70% Black Transparent Overlay for clear text legibility */}
      <div className={`absolute inset-0 ${overlayOpacity}`} />
    </div>
  );
};
