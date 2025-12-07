import { useEffect, useRef, type RefObject } from 'react';
import { type MiraChatInputHandle } from '../components/MiraChatInput';

// Simple message interface for keyboard shortcuts
interface MiraMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
}

interface UseMiraShortcutsProps {
  isSidebarOpen: boolean;
  setIsSidebarOpen: (value: boolean | ((prev: boolean) => boolean)) => void;
  handleNewConversation: () => void;
  messages: MiraMessage[];
  selectedMessageIndex: number;
  setSelectedMessageIndex: (index: number) => void;
  scrollToMessage: (index: number) => void;
  handleEditMessage: (messageId: string) => void;
  setShowKeyboardShortcuts: (value: boolean) => void;
  chatInputRef: RefObject<MiraChatInputHandle | null>;
  searchInputRef: RefObject<HTMLInputElement | null>;
}

export function useMiraShortcuts({
  isSidebarOpen,
  setIsSidebarOpen,
  handleNewConversation,
  messages,
  selectedMessageIndex,
  setSelectedMessageIndex,
  scrollToMessage,
  handleEditMessage,
  setShowKeyboardShortcuts,
  chatInputRef,
  searchInputRef,
}: UseMiraShortcutsProps) {
  // 連続キー入力の追跡（gg, ge用）
  const lastKeyRef = useRef<{ key: string; time: number }>({ key: '', time: 0 });

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      const isInput = target.tagName === 'TEXTAREA' || target.tagName === 'INPUT';
      
      const now = Date.now();
      const lowerKey = e.key.toLowerCase();
      
      // ⌘/Ctrl + Shift + H でサイドバーを切り替え (入力中でも有効)
      if (lowerKey === 'h' && (e.ctrlKey || e.metaKey) && e.shiftKey) {
        e.preventDefault();
        e.stopPropagation();
        setIsSidebarOpen((prev) => !prev);
        return;
      }
      
      // ⌘/Ctrl + K で会話検索 (入力中でも有効)
      if (lowerKey === 'k' && (e.ctrlKey || e.metaKey)) {
        e.preventDefault();
        e.stopPropagation();
        
        // サイドバーが閉じていたら開く
        if (!isSidebarOpen) {
          setIsSidebarOpen(true);
          // サイドバーが開くアニメーション/レンダリングを待つために少し遅延させる
          setTimeout(() => searchInputRef.current?.focus(), 100);
        } else {
          searchInputRef.current?.focus();
        }
        return;
      }
      
      // ⌘/Ctrl + Shift + O で新規会話 (入力中でも有効)
      if (lowerKey === 'o' && (e.ctrlKey || e.metaKey) && e.shiftKey) {
        e.preventDefault();
        e.stopPropagation();
        handleNewConversation();
        return;
      }

      // 以下は入力エリアにフォーカスがある場合は無視
      if (isInput) {
        return;
      }
      
      // ? でショートカット一覧を表示
      if (e.key === '?' && !e.ctrlKey && !e.metaKey) {
        e.preventDefault();
        // ヘルプ表示は伝播しても問題ないことが多いが念のため
        e.stopPropagation();
        setShowKeyboardShortcuts(true);
        return;
      }
      
      // n で入力欄にフォーカス（Ctrl/Cmdなし）
      if (lowerKey === 'n' && !e.ctrlKey && !e.metaKey && !e.altKey) {
        e.preventDefault();
        e.stopPropagation();
        // MiraChatInput内のtextareaにフォーカス (ref経由)
        chatInputRef.current?.focus();
        return;
      }
      
      // j で次のメッセージへ
      if (lowerKey === 'j' && !e.ctrlKey && !e.metaKey) {
        e.preventDefault();
        if (messages.length > 0) {
          const newIndex = Math.min(selectedMessageIndex + 1, messages.length - 1);
          setSelectedMessageIndex(newIndex);
          scrollToMessage(newIndex);
        }
        lastKeyRef.current = { key: 'j', time: now };
        return;
      }
      
      // k で前のメッセージへ
      if (lowerKey === 'k' && !e.ctrlKey && !e.metaKey) {
        e.preventDefault();
        if (messages.length > 0) {
          const newIndex = Math.max(selectedMessageIndex - 1, 0);
          setSelectedMessageIndex(newIndex);
          scrollToMessage(newIndex);
        }
        lastKeyRef.current = { key: 'k', time: now };
        return;
      }
      
      // g + g で最初のメッセージへ（500ms以内の連続押下）
      if (lowerKey === 'g' && !e.ctrlKey && !e.metaKey) {
        if (lastKeyRef.current.key === 'g' && now - lastKeyRef.current.time < 500) {
          e.preventDefault();
          setSelectedMessageIndex(0);
          scrollToMessage(0);
          lastKeyRef.current = { key: '', time: 0 };
        } else {
          lastKeyRef.current = { key: 'g', time: now };
        }
        return;
      }
      
      // g + e で最後のメッセージへ（500ms以内）
      if (lowerKey === 'e' && !e.ctrlKey && !e.metaKey) {
        if (lastKeyRef.current.key === 'g' && now - lastKeyRef.current.time < 500) {
          e.preventDefault();
          const lastIndex = messages.length - 1;
          setSelectedMessageIndex(lastIndex);
          scrollToMessage(lastIndex);
          lastKeyRef.current = { key: '', time: 0 };
        } else {
          // 単独の e キーでメッセージ編集（選択中のメッセージがユーザーメッセージの場合）
          if (selectedMessageIndex >= 0 && messages[selectedMessageIndex]?.role === 'user') {
            e.preventDefault();
            handleEditMessage(messages[selectedMessageIndex].id);
          }
          lastKeyRef.current = { key: 'e', time: now };
        }
        return;
      }
      
      // c で選択中メッセージをコピー
      if (lowerKey === 'c' && !e.ctrlKey && !e.metaKey && selectedMessageIndex >= 0) {
        e.preventDefault();
        const msg = messages[selectedMessageIndex];
        if (msg) {
          navigator.clipboard.writeText(msg.content);
        }
        return;
      }
      
      // Escape でサイドバーを閉じる / メッセージ選択解除
      if (e.key === 'Escape') {
        if (selectedMessageIndex >= 0) {
          setSelectedMessageIndex(-1);
          e.preventDefault();
          e.stopPropagation();
        }
        return;
      }
    };
    
    // キャプチャフェーズでイベントを捕捉してブラウザのショートカットより先に処理する
    document.addEventListener('keydown', handleKeyDown, true);
    return () => document.removeEventListener('keydown', handleKeyDown, true);
  }, [
    isSidebarOpen, 
    setIsSidebarOpen, 
    handleNewConversation, 
    messages, 
    selectedMessageIndex, 
    setSelectedMessageIndex, 
    scrollToMessage, 
    handleEditMessage, 
    setShowKeyboardShortcuts, 
    chatInputRef, 
    searchInputRef
  ]);
}
