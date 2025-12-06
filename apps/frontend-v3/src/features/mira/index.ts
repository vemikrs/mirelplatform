/**
 * Mira Feature Index
 */

// Components
export * from './components';

// Re-export hooks for convenience
export { useMira, useMiraChat, useMiraPanel, useMiraConversation } from '@/hooks/useMira';

// Re-export store types
export type { MiraMessage, MiraConversation } from '@/stores/miraStore';
