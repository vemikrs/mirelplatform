/**
 * マルチタブ認証同期サービス
 * 
 * BroadcastChannelを使用して複数タブ間で認証状態を同期する。
 * - ログアウト: 全タブで同時にログアウト
 * - トークン更新: 全タブで新しいトークンを共有
 */

type AuthBroadcastMessage =
  | { type: 'LOGOUT' }
  | { type: 'TOKEN_UPDATED'; tokens: { accessToken: string; refreshToken: string } }
  | { type: 'SESSION_EXPIRED' };

let channel: BroadcastChannel | null = null;
let messageHandler: ((event: MessageEvent<AuthBroadcastMessage>) => void) | null = null;

/**
 * マルチタブ同期を初期化
 * @param onLogout ログアウト時のコールバック
 * @param onTokenUpdated トークン更新時のコールバック
 */
export function initAuthBroadcast(
  onLogout: () => void,
  onTokenUpdated: (tokens: { accessToken: string; refreshToken: string }) => void
) {
  if (typeof window === 'undefined' || !('BroadcastChannel' in window)) {
    console.log('[AuthBroadcast] BroadcastChannel not supported');
    return;
  }

  // 既存のチャンネルをクリーンアップ
  cleanupAuthBroadcast();

  channel = new BroadcastChannel('mirel-auth-sync');
  
  messageHandler = (event: MessageEvent<AuthBroadcastMessage>) => {
    console.log('[AuthBroadcast] Received message:', event.data.type);

    switch (event.data.type) {
      case 'LOGOUT':
        console.log('[AuthBroadcast] Logout received, clearing local auth');
        onLogout();
        break;
      
      case 'TOKEN_UPDATED':
        console.log('[AuthBroadcast] Token update received');
        onTokenUpdated(event.data.tokens);
        break;

      case 'SESSION_EXPIRED':
        console.log('[AuthBroadcast] Session expired, redirecting to login');
        window.location.href = '/login?error=session_expired';
        break;
    }
  };

  channel.addEventListener('message', messageHandler);
  console.log('[AuthBroadcast] Initialized');
}

/**
 * ログアウトを全タブに通知
 */
export function broadcastLogout() {
  if (channel) {
    console.log('[AuthBroadcast] Broadcasting logout');
    channel.postMessage({ type: 'LOGOUT' } as AuthBroadcastMessage);
  }
}

/**
 * トークン更新を全タブに通知
 */
export function broadcastTokenUpdated(tokens: { accessToken: string; refreshToken: string }) {
  if (channel) {
    console.log('[AuthBroadcast] Broadcasting token update');
    channel.postMessage({ type: 'TOKEN_UPDATED', tokens } as AuthBroadcastMessage);
  }
}

/**
 * セッション期限切れを全タブに通知
 */
export function broadcastSessionExpired() {
  if (channel) {
    console.log('[AuthBroadcast] Broadcasting session expired');
    channel.postMessage({ type: 'SESSION_EXPIRED' } as AuthBroadcastMessage);
  }
}

/**
 * クリーンアップ
 */
export function cleanupAuthBroadcast() {
  if (channel) {
    if (messageHandler) {
      channel.removeEventListener('message', messageHandler);
      messageHandler = null;
    }
    channel.close();
    channel = null;
    console.log('[AuthBroadcast] Cleaned up');
  }
}
