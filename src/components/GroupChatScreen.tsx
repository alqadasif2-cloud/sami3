import React, { useState, useEffect, useRef } from 'react';
import {
  Send,
  Image as ImageIcon,
  Camera,
  WifiOff,
  Clock,
  User,
  X,
  Maximize2,
  Users,
  AlertCircle,
} from 'lucide-react';
import {
  ChatMessage,
  sendChatMessage,
  subscribeToChat,
  filterExpiredMessages,
} from '../data/chat';
import { CurrentUser } from '../data/auth';
import { processImageFile } from '../utils/image';
import { CloudImage } from './CloudImage';

interface GroupChatScreenProps {
  currentUser: CurrentUser;
}

export const GroupChatScreen: React.FC<GroupChatScreenProps> = ({ currentUser }) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState('');
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewModalImage, setPreviewModalImage] = useState<string | null>(null);
  const [isOnline, setIsOnline] = useState<boolean>(
    typeof navigator !== 'undefined' ? navigator.onLine : true
  );
  const [isSending, setIsSending] = useState(false);
  const [alertBanner, setAlertBanner] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  const showBannerError = (msg: string) => {
    setAlertBanner(msg);
    setTimeout(() => {
      setAlertBanner((prev) => (prev === msg ? null : prev));
    }, 4500);
  };

  // Monitor online / offline status
  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Subscribe to real-time Firestore messages across all devices
  useEffect(() => {
    const unsubscribe = subscribeToChat(
      (liveMessages) => {
        setMessages(liveMessages);
      },
      (errMsg) => {
        showBannerError(errMsg);
      }
    );

    // Periodic check every minute to auto-filter messages older than 48 hours
    const interval = setInterval(() => {
      setMessages((prev) => filterExpiredMessages(prev));
    }, 60000);

    return () => {
      clearInterval(interval);
      unsubscribe();
    };
  }, []);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Handle image file selection (Gallery or Camera)
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;

    if (file.size > 15 * 1024 * 1024) {
      showBannerError('حجم الصورة كبير جداً، يرجى اختيار صورة أقل من 15 ميغابايت.');
      return;
    }

    try {
      const compressedDataUrl = await processImageFile(file, 1280, 0.82);
      setSelectedImage(compressedDataUrl);
      setSelectedFile(file);
      setAlertBanner(null);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'تعذر معالجة الصورة المختارة.';
      showBannerError(msg);
    }
  };

  // Send message to Firebase Firestore & Firebase Storage
  const handleSendMessage = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!isOnline) {
      showBannerError('لا يوجد اتصال بالإنترنت.');
      return;
    }

    const trimmed = inputText.trim();
    if (!trimmed && !selectedImage) return;

    setIsSending(true);
    setAlertBanner(null);
    try {
      await sendChatMessage(currentUser.id, currentUser.displayName, {
        text: trimmed || undefined,
        imageUrl: selectedImage || undefined,
        imageFile: selectedFile || undefined,
      });

      setInputText('');
      setSelectedImage(null);
      setSelectedFile(null);
    } catch {
      showBannerError('تعذر إرسال الرسالة، يرجى التحقق من اتصال الإنترنت والمحاولة مرة أخرى.');
    } finally {
      setIsSending(false);
    }
  };

  const formatMessageTime = (timestamp: number) => {
    const d = new Date(timestamp);
    return d.toLocaleTimeString('ar-SA', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
  };

  return (
    <div className="flex flex-col h-[calc(100vh-140px)] sm:h-[calc(100vh-170px)] bg-[#0b101a]/95 border border-slate-800/80 rounded-3xl overflow-hidden shadow-2xl relative font-['Cairo'] pb-1">
      {/* Hidden file inputs for Gallery and Camera */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept="image/*"
        className="hidden"
      />
      <input
        type="file"
        ref={cameraInputRef}
        onChange={handleFileChange}
        accept="image/*"
        capture="environment"
        className="hidden"
      />

      {/* Header */}
      <div
        dir="rtl"
        className="px-4 py-3 bg-[#0e1626] border-b border-slate-800/90 flex items-center justify-between shadow-md shrink-0"
      >
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center text-slate-950 font-black shadow-md shadow-amber-500/20">
            💬
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-black text-white tracking-wide">
                دفعة الرحلة إلى 3000$
              </h2>
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            </div>
            <p className="text-[11px] text-slate-400 flex items-center gap-1">
              <Users className="w-3 h-3 text-amber-400" />
              <span>20 عضواً معتمداً • حذف تلقائي بعد 48 ساعة</span>
            </p>
          </div>
        </div>

        {/* Current user badge */}
        <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-800/80 border border-slate-700/60 text-[11px] text-amber-300 font-bold">
          <User className="w-3 h-3" />
          <span>{currentUser.displayName}</span>
        </div>
      </div>

      {/* Offline Alert Banner */}
      {!isOnline && (
        <div
          dir="rtl"
          className="bg-rose-500/20 border-b border-rose-500/40 px-4 py-2 flex items-center justify-center gap-2 text-rose-300 text-xs font-bold animate-in slide-in-from-top duration-200 shrink-0"
        >
          <WifiOff className="w-4 h-4 text-rose-400 shrink-0" />
          <span>لا يوجد اتصال بالإنترنت.</span>
        </div>
      )}

      {/* In-App Notification / Error Banner (replaces browser alert) */}
      {alertBanner && (
        <div
          dir="rtl"
          className="bg-rose-500/20 border-b border-rose-500/40 px-4 py-2 flex items-center justify-between gap-2 text-rose-200 text-xs font-bold animate-in slide-in-from-top duration-200 shrink-0"
        >
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
            <span>{alertBanner}</span>
          </div>
          <button
            type="button"
            onClick={() => setAlertBanner(null)}
            className="p-0.5 text-rose-300 hover:text-white"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Messages Scroll Area */}
      <div
        dir="rtl"
        className="flex-1 overflow-y-auto p-4 space-y-3.5 scrollbar-thin scrollbar-thumb-slate-800"
      >
        {messages.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center text-slate-500 p-6 space-y-2">
            <div className="w-12 h-12 rounded-full bg-slate-800/50 flex items-center justify-center text-amber-400 text-xl">
              💬
            </div>
            <p className="text-sm font-bold text-slate-400">لا توجد رسائل بعد</p>
            <p className="text-xs text-slate-500 max-w-xs">
              كن أول من يبدأ المحادثة في مجموعة دفعة الرحلة إلى 3000$!
            </p>
          </div>
        ) : (
          messages.map((msg) => {
            const isMe = msg.senderId === currentUser.id;
            return (
              <div
                key={msg.id}
                className={`flex flex-col ${isMe ? 'items-start' : 'items-end'} animate-in fade-in duration-200`}
              >
                {/* Bubble Container */}
                <div
                  className={`max-w-[85%] sm:max-w-[70%] rounded-2xl p-3 shadow-md relative ${
                    isMe
                      ? 'bg-gradient-to-br from-amber-500/25 to-amber-600/15 border border-amber-500/40 text-slate-100 rounded-tr-none'
                      : 'bg-[#151f33] border border-slate-800/90 text-slate-200 rounded-tl-none'
                  }`}
                >
                  {/* Sender Name with Badge */}
                  <div className="flex items-center justify-between gap-3 mb-1">
                    <span
                      className={`text-[11px] font-black tracking-tight ${
                        isMe ? 'text-amber-400 font-extrabold' : 'text-sky-300'
                      }`}
                    >
                      {msg.senderName} {isMe && '(أنت)'}
                    </span>
                    <span className="text-[10px] text-slate-400 flex items-center gap-0.5">
                      <Clock className="w-2.5 h-2.5" />
                      {formatMessageTime(msg.timestamp)}
                    </span>
                  </div>

                  {/* Attached Cloud Image if any */}
                  {msg.imageUrl && (
                    <div className="relative my-1.5 rounded-xl overflow-hidden group cursor-pointer border border-slate-700/60 bg-black/40">
                      <CloudImage
                        src={msg.imageUrl}
                        alt="مرفق الدردشة"
                        className="max-h-60 w-auto rounded-xl object-contain mx-auto transition-transform duration-200 group-hover:scale-[1.02]"
                        onClick={() => setPreviewModalImage(msg.imageUrl || null)}
                      />
                      <button
                        type="button"
                        onClick={() => setPreviewModalImage(msg.imageUrl || null)}
                        className="absolute bottom-2 left-2 p-1.5 rounded-lg bg-black/60 text-white hover:bg-black/80 backdrop-blur-sm opacity-90 transition"
                      >
                        <Maximize2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  )}

                  {/* Text Message */}
                  {msg.text && (
                    <p className="text-xs sm:text-sm font-medium leading-relaxed whitespace-pre-wrap select-text">
                      {msg.text}
                    </p>
                  )}
                </div>
              </div>
            );
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Selected Image Pending Preview Banner */}
      {selectedImage && (
        <div
          dir="rtl"
          className="px-4 py-2 bg-[#121c2e] border-t border-slate-800 flex items-center justify-between shrink-0"
        >
          <div className="flex items-center gap-2">
            <img
              src={selectedImage}
              alt="صورة قيد الإرسال"
              className="w-10 h-10 rounded-lg object-cover border border-amber-500/50"
            />
            <span className="text-xs text-amber-300 font-bold">صورة جاهزة للإرسال</span>
          </div>
          <button
            type="button"
            onClick={() => {
              setSelectedImage(null);
              setSelectedFile(null);
            }}
            className="p-1 rounded-full text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Input Form */}
      <form
        onSubmit={handleSendMessage}
        dir="rtl"
        className="p-3 bg-[#0d1524] border-t border-slate-800/90 flex items-center gap-2 shrink-0"
      >
        {/* Gallery Image Button */}
        <button
          type="button"
          disabled={!isOnline || isSending}
          onClick={() => fileInputRef.current?.click()}
          title="إرسال صورة من المعرض"
          className="p-2.5 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 text-amber-400 hover:text-amber-300 border border-slate-700/60 transition disabled:opacity-40 cursor-pointer"
        >
          <ImageIcon className="w-5 h-5" />
        </button>

        {/* Camera Button */}
        <button
          type="button"
          disabled={!isOnline || isSending}
          onClick={() => cameraInputRef.current?.click()}
          title="التقاط صورة بالكاميرا"
          className="p-2.5 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 text-amber-400 hover:text-amber-300 border border-slate-700/60 transition disabled:opacity-40 cursor-pointer"
        >
          <Camera className="w-5 h-5" />
        </button>

        {/* Text Input */}
        <input
          type="text"
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder={isOnline ? 'اكتب رسالتك للمجموعة...' : 'لا يوجد اتصال بالإنترنت'}
          disabled={!isOnline || isSending}
          className="flex-1 bg-[#131d30] border border-slate-700/80 focus:border-amber-400 focus:ring-1 focus:ring-amber-400 rounded-xl px-3.5 py-2.5 text-xs sm:text-sm text-white placeholder-slate-500 outline-none transition disabled:opacity-50"
        />

        {/* Send Button */}
        <button
          type="submit"
          disabled={!isOnline || (!inputText.trim() && !selectedImage) || isSending}
          className="p-2.5 sm:px-4 rounded-xl font-bold text-xs bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 shadow-md shadow-amber-500/20 flex items-center gap-1.5 disabled:opacity-40 transition active:scale-95 cursor-pointer"
        >
          {isSending ? (
            <span className="w-5 h-5 border-2 border-slate-950 border-t-transparent rounded-full animate-spin" />
          ) : (
            <>
              <span className="hidden sm:inline">إرسال</span>
              <Send className="w-4 h-4 rotate-180" />
            </>
          )}
        </button>
      </form>

      {/* Image Preview Modal */}
      {previewModalImage && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 backdrop-blur-md p-4 animate-in fade-in"
          onClick={() => setPreviewModalImage(null)}
        >
          <button
            type="button"
            onClick={() => setPreviewModalImage(null)}
            className="absolute top-4 left-4 p-2 rounded-full bg-slate-800 text-white hover:bg-slate-700 transition"
          >
            <X className="w-6 h-6" />
          </button>
          <CloudImage
            src={previewModalImage}
            alt="صورة كاملة"
            className="max-h-[90vh] max-w-[95vw] rounded-2xl object-contain shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}
    </div>
  );
};
