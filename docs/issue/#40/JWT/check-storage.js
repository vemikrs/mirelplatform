/**
 * sessionStorage 直接確認スクリプト
 * ブラウザコンソールで実行
 */

console.log('=== sessionStorage Contents ===');
console.log('Total items:', sessionStorage.length);

for (let i = 0; i < sessionStorage.length; i++) {
  const key = sessionStorage.key(i);
  const value = sessionStorage.getItem(key);
  console.log(`[${i}] ${key}:`, value);
  
  // auth-storage があれば詳細表示
  if (key === 'auth-storage') {
    try {
      const parsed = JSON.parse(value);
      console.log('  Parsed:', JSON.stringify(parsed, null, 2));
    } catch (e) {
      console.error('  Parse error:', e);
    }
  }
}

console.log('=== Direct check ===');
const authStorage = sessionStorage.getItem('auth-storage');
console.log('auth-storage:', authStorage);

console.log('=== Zustand Store State (in-memory) ===');
// Reactの内部にアクセスする必要があるため、グローバル変数経由でチェック
// __REACT_DEVTOOLS_GLOBAL_HOOK__ を使う方法もあるが、直接は難しい
console.log('Note: Zustand store state は React DevTools の Components タブで確認してください');
