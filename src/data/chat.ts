// Single Cloud Group Chat: "💬 دفعة الرحلة إلى 3000$"
// Powered by Firebase Firestore & Firebase Storage.
// Messages and images older than 48 hours are automatically deleted.

import {
  collection,
  doc,
  setDoc,
  deleteDoc,
  onSnapshot,
  query,
  orderBy,
} from 'firebase/firestore';
import { db, uploadImageToCloud, deleteCloudImage } from '../services/firebase';

export interface ChatMessage {
  id: string;
  senderId: number;
  senderName: string;
  text?: string;
  imageUrl?: string;
  storagePath?: string;
  timestamp: number;
}

const CHAT_COLLECTION = 'chat_messages';
const FORTY_EIGHT_HOURS_MS = 48 * 60 * 60 * 1000;

// Clean up any legacy localStorage chat keys so no chat data or images remain in localStorage
try {
  if (typeof window !== 'undefined' && window.localStorage) {
    window.localStorage.removeItem('sami_chat_messages_v2');
    window.localStorage.removeItem('sami_chat_messages');
  }
} catch {
  // Ignore
}

/**
 * Filter out messages older than 48 hours.
 */
export function filterExpiredMessages(messages: ChatMessage[]): ChatMessage[] {
  const now = Date.now();
  return messages.filter((m) => now - m.timestamp < FORTY_EIGHT_HOURS_MS);
}

/**
 * Purges expired messages (> 48 hours) from Firestore and Firebase Storage.
 */
export async function purgeExpiredFirestoreMessages(expiredMessages: ChatMessage[]): Promise<void> {
  for (const msg of expiredMessages) {
    try {
      await deleteDoc(doc(db, CHAT_COLLECTION, msg.id));
      if (msg.imageUrl || msg.storagePath) {
        await deleteCloudImage(msg.imageUrl, msg.storagePath);
      }
    } catch (err) {
      console.warn('Failed to purge expired chat message:', err);
    }
  }
}

/**
 * Send a new message:
 * - Uploads any attached image to Firebase Storage (saving only its URL in Firestore).
 * - Stores the message document in Firebase Firestore `chat_messages`.
 */
export async function sendChatMessage(
  senderId: number,
  senderName: string,
  content: { text?: string; imageUrl?: string; imageFile?: File }
): Promise<ChatMessage> {
  const now = Date.now();
  const msgId = `msg_${now}_${senderId}_${Math.random().toString(36).substring(2, 8)}`;

  let cloudImageUrl: string | undefined;
  let storagePath: string | undefined;

  if (content.imageFile) {
    const uploaded = await uploadImageToCloud(content.imageFile, 'chat_images', senderId);
    cloudImageUrl = uploaded.imageUrl;
    storagePath = uploaded.storagePath;
  } else if (content.imageUrl) {
    if (content.imageUrl.startsWith('data:')) {
      const uploaded = await uploadImageToCloud(content.imageUrl, 'chat_images', senderId);
      cloudImageUrl = uploaded.imageUrl;
      storagePath = uploaded.storagePath;
    } else {
      cloudImageUrl = content.imageUrl;
    }
  }

  const trimmedText = content.text?.trim();

  const firestorePayload: Record<string, unknown> = {
    id: msgId,
    senderId,
    senderName,
    timestamp: now,
  };

  if (trimmedText) {
    firestorePayload.text = trimmedText;
  }
  if (cloudImageUrl) {
    firestorePayload.imageUrl = cloudImageUrl;
  }
  if (storagePath) {
    firestorePayload.storagePath = storagePath;
  }

  await setDoc(doc(db, CHAT_COLLECTION, msgId), firestorePayload);

  return {
    id: msgId,
    senderId,
    senderName,
    text: trimmedText || undefined,
    imageUrl: cloudImageUrl,
    storagePath,
    timestamp: now,
  };
}

/**
 * Subscribe to real-time messages from Firebase Firestore across all devices.
 * Automatically purges messages older than 48 hours.
 */
export function subscribeToChat(
  onMessagesChange: (messages: ChatMessage[]) => void,
  onError?: (errorMsg: string) => void
): () => void {
  const q = query(collection(db, CHAT_COLLECTION), orderBy('timestamp', 'asc'));

  const unsubscribe = onSnapshot(
    q,
    (snapshot) => {
      const now = Date.now();
      const validMessages: ChatMessage[] = [];
      const expiredMessages: ChatMessage[] = [];

      snapshot.forEach((docSnap) => {
        const data = docSnap.data();
        const msg: ChatMessage = {
          id: String(data.id || docSnap.id),
          senderId: Number(data.senderId || 0),
          senderName: String(data.senderName || 'متداول'),
          text: typeof data.text === 'string' ? data.text : undefined,
          imageUrl: typeof data.imageUrl === 'string' ? data.imageUrl : undefined,
          storagePath: typeof data.storagePath === 'string' ? data.storagePath : undefined,
          timestamp: Number(data.timestamp || now),
        };

        if (now - msg.timestamp < FORTY_EIGHT_HOURS_MS) {
          validMessages.push(msg);
        } else {
          expiredMessages.push(msg);
        }
      });

      if (expiredMessages.length > 0) {
        purgeExpiredFirestoreMessages(expiredMessages);
      }

      validMessages.sort((a, b) => a.timestamp - b.timestamp);
      onMessagesChange(validMessages);
    },
    (error) => {
      if (error?.code !== 'unavailable' && onError) {
        onError('تعذر الاتصال بخدمة الدردشة السحابية.');
      }
    }
  );

  return unsubscribe;
}
