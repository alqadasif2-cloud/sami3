import React, { useState } from 'react';
import { X, ZoomIn, ZoomOut, RotateCcw } from 'lucide-react';
import { CloudImage } from './CloudImage';

interface ImageViewerModalProps {
  imageUrl: string | null;
  title?: string;
  onClose: () => void;
}

export const ImageViewerModal: React.FC<ImageViewerModalProps> = ({
  imageUrl,
  title = 'عرض الصورة المرفقة',
  onClose,
}) => {
  const [scale, setScale] = useState(1);

  if (!imageUrl) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/95 backdrop-blur-md p-4 animate-in fade-in duration-200">
      {/* Top Header */}
      <div className="absolute top-4 left-4 right-4 flex items-center justify-between text-white z-10">
        <h3 className="text-sm font-semibold text-slate-200">{title}</h3>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setScale((s) => Math.min(s + 0.3, 3))}
            className="p-2 bg-slate-800/80 hover:bg-slate-700 rounded-full transition-colors"
            title="تكبير"
          >
            <ZoomIn className="w-5 h-5" />
          </button>
          <button
            onClick={() => setScale((s) => Math.max(s - 0.3, 0.6))}
            className="p-2 bg-slate-800/80 hover:bg-slate-700 rounded-full transition-colors"
            title="تصغير"
          >
            <ZoomOut className="w-5 h-5" />
          </button>
          <button
            onClick={() => setScale(1)}
            className="p-2 bg-slate-800/80 hover:bg-slate-700 rounded-full transition-colors"
            title="إعادة التعيين"
          >
            <RotateCcw className="w-4 h-4" />
          </button>
          <button
            onClick={onClose}
            className="p-2 bg-red-600/80 hover:bg-red-700 rounded-full transition-colors text-white"
            title="إغلاق"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Image container */}
      <div className="max-w-full max-h-[85vh] overflow-auto flex items-center justify-center p-4">
        <CloudImage
          src={imageUrl}
          alt={title}
          style={{ transform: `scale(${scale})`, transition: 'transform 0.15s ease-out' }}
          className="max-w-full max-h-[75vh] object-contain rounded-lg shadow-2xl origin-center"
        />
      </div>
    </div>
  );
};
