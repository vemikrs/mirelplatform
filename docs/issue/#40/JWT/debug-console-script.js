/**
 * 401エラーハンドリング デバッグスクリプト
 * 
 * 使い方:
 * 1. ブラウザの開発者ツール（F12）を開く
 * 2. Consoleタブを選択
 * 3. このスクリプト全体をコピー&ペースト
 * 4. 問題の操作（ログアウト、ProMarkerアクセス等）を実行
 * 5. コンソールに出力されるログを確認
 */

(function() {
  console.log('=== 401 Debug Script Started ===');
  
  // 1. Zustand Storeの状態を監視（メモリ内のみ、persistなし）
  function checkAuthStore() {
    console.log('[AuthStore State] JWT認証はメモリ内のみで管理（sessionStorage/localStorage使用なし）');
    console.log('[AuthStore State] ページリロード時は未認証 → authLoaderで/users/me再認証');
  }
  
  // 2. React Router Loaderの実行を追跡
  const originalFetch = window.fetch;
  window.fetch = function(...args) {
    const url = args[0];
    if (typeof url === 'string' && url.includes('/users/me')) {
      console.log('[API Call] /users/me requested', {
        url,
        timestamp: new Date().toISOString(),
        stackTrace: new Error().stack
      });
    }
    return originalFetch.apply(this, args).then(response => {
      if (typeof url === 'string' && url.includes('/users/me')) {
        console.log('[API Call] /users/me response:', {
          status: response.status,
          ok: response.ok,
          timestamp: new Date().toISOString()
        });
      }
      return response;
    });
  };
  
  // 3. Navigate/Redirectの追跡
  const originalPushState = history.pushState;
  const originalReplaceState = history.replaceState;
  
  history.pushState = function(...args) {
    console.log('[Navigation] pushState:', {
      url: args[2],
      timestamp: new Date().toISOString(),
      stackTrace: new Error().stack?.split('\n').slice(0, 5).join('\n')
    });
    return originalPushState.apply(this, args);
  };
  
  history.replaceState = function(...args) {
    console.log('[Navigation] replaceState:', {
      url: args[2],
      timestamp: new Date().toISOString(),
      stackTrace: new Error().stack?.split('\n').slice(0, 5).join('\n')
    });
    return originalReplaceState.apply(this, args);
  };
  
  // 4. window.location.replace()の呼び出しを追跡
  const originalLocationReplace = window.location.replace.bind(window.location);
  window.location.replace = function(url) {
    console.log('[Navigation] window.location.replace called:', {
      from: window.location.href,
      to: url,
      timestamp: new Date().toISOString(),
      stackTrace: new Error().stack?.split('\n').slice(0, 8).join('\n')
    });
    return originalLocationReplace(url);
  };
  
  // 5. ProtectedRouteのNavigateを追跡
  window.addEventListener('beforeunload', (event) => {
    console.log('[Page] beforeunload event fired');
  });
  
  // 6. React Routerのlocation変更を追跡
  const observeLocationChanges = setInterval(() => {
    const currentPath = window.location.pathname;
    const currentSearch = window.location.search;
    const key = currentPath + currentSearch;
    
    if (window._lastRouterLocation !== key) {
      console.log('[Router] Location changed:', {
        from: window._lastRouterLocation,
        to: key,
        timestamp: new Date().toISOString()
      });
      window._lastRouterLocation = key;
      
      // location.state を確認（React Router Navigateで渡されたstate）
      try {
        // React Routerの内部stateは直接取得できないため、DOMから推測
        const toastElements = document.querySelectorAll('[role="status"], [role="alert"]');
        if (toastElements.length > 0) {
          console.log('[Router] Toast/Alert found:', Array.from(toastElements).map(el => el.textContent));
        }
      } catch (error) {
        console.error('[Router] Error checking toast:', error);
      }
    }
  }, 100);
  
  // 7. 初回状態チェック
  console.log('[Initial State Check]');
  checkAuthStore();
  console.log('[Current Location]', {
    pathname: window.location.pathname,
    search: window.location.search,
    hash: window.location.hash
  });
  
  // 8. 定期的な状態チェック
  const stateCheckInterval = setInterval(() => {
    checkAuthStore();
  }, 2000);
  
  // 9. クリーンアップ関数
  window.stopDebug = function() {
    clearInterval(observeLocationChanges);
    clearInterval(stateCheckInterval);
    window.fetch = originalFetch;
    history.pushState = originalPushState;
    history.replaceState = originalReplaceState;
    window.location.replace = originalLocationReplace;
    console.log('=== 401 Debug Script Stopped ===');
  };
  
  console.log('=== Debug Script Ready ===');
  console.log('実行中: ログアウト操作やページ遷移を行ってください');
  console.log('停止するには: stopDebug() を実行');
})();
