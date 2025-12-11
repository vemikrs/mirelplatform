/**
 * ブラウザ開発者ツールで実行する余白検証スクリプト
 * 
 * 使い方：
 * 1. ブラウザでF12キーを押して開発者ツールを開く
 * 2. Consoleタブを選択
 * 3. このスクリプト全体をコピー＆ペーストしてEnterキーを押す
 * 4. 出力されたレポートで余白の原因を特定
 */

(function debugMarginPadding() {
  console.clear();
  console.log('=== 余白デバッグレポート ===\n');

  // 1. html要素のチェック
  const html = document.documentElement;
  const htmlStyles = window.getComputedStyle(html);
  console.log('📄 <html> 要素:');
  console.log('  margin:', htmlStyles.margin);
  console.log('  padding:', htmlStyles.padding);
  console.log('  box-sizing:', htmlStyles.boxSizing);
  console.log('');

  // 2. body要素のチェック
  const body = document.body;
  const bodyStyles = window.getComputedStyle(body);
  console.log('📄 <body> 要素:');
  console.log('  margin:', bodyStyles.margin);
  console.log('  padding:', bodyStyles.padding);
  console.log('  box-sizing:', bodyStyles.boxSizing);
  console.log('  width:', bodyStyles.width);
  console.log('  height:', bodyStyles.height);
  console.log('');

  // 3. ルート要素（div#root）のチェック
  const root = document.getElementById('root');
  if (root) {
    const rootStyles = window.getComputedStyle(root);
    console.log('📦 #root 要素:');
    console.log('  margin:', rootStyles.margin);
    console.log('  padding:', rootStyles.padding);
    console.log('  width:', rootStyles.width);
    console.log('  height:', rootStyles.height);
    console.log('');
  }

  // 4. メインレイアウト要素（flex min-h-screen）のチェック
  const mainLayout = document.querySelector('.flex.min-h-screen.flex-col');
  if (mainLayout) {
    const layoutStyles = window.getComputedStyle(mainLayout);
    console.log('📐 メインレイアウト (.flex.min-h-screen.flex-col):');
    console.log('  margin:', layoutStyles.margin);
    console.log('  padding:', layoutStyles.padding);
    console.log('  width:', layoutStyles.width);
    console.log('  height:', layoutStyles.height);
    console.log('');
  }

  // 5. header要素のチェック
  const header = document.querySelector('header');
  if (header) {
    const headerStyles = window.getComputedStyle(header);
    console.log('📋 <header> 要素:');
    console.log('  margin:', headerStyles.margin);
    console.log('  padding:', headerStyles.padding);
    console.log('  height:', headerStyles.height);
    console.log('');
  }

  // 6. main要素のチェック
  const main = document.querySelector('main');
  if (main) {
    const mainStyles = window.getComputedStyle(main);
    console.log('📝 <main> 要素:');
    console.log('  margin:', mainStyles.margin);
    console.log('  padding:', mainStyles.padding);
    console.log('  width:', mainStyles.width);
    console.log('');
  }

  // 7. サイドバーのチェック
  const sidebar = document.querySelector('aside, nav');
  if (sidebar) {
    const sidebarStyles = window.getComputedStyle(sidebar);
    console.log('🔲 サイドバー (aside/nav):');
    console.log('  margin:', sidebarStyles.margin);
    console.log('  padding:', sidebarStyles.padding);
    console.log('  width:', sidebarStyles.width);
    console.log('');
  }

  // 8. スクロールバーの確認
  const hasVerticalScroll = document.documentElement.scrollHeight > window.innerHeight;
  const hasHorizontalScroll = document.documentElement.scrollWidth > window.innerWidth;
  console.log('📏 スクロールバー:');
  console.log('  縦スクロールバー:', hasVerticalScroll ? 'あり' : 'なし');
  console.log('  横スクロールバー:', hasHorizontalScroll ? 'あり ⚠️' : 'なし');
  console.log('  viewport width:', window.innerWidth);
  console.log('  document width:', document.documentElement.scrollWidth);
  console.log('  viewport height:', window.innerHeight);
  console.log('  document height:', document.documentElement.scrollHeight);
  console.log('');

  // 9. ビューポートと実際のサイズの差分
  const widthDiff = document.documentElement.scrollWidth - window.innerWidth;
  const heightDiff = document.documentElement.scrollHeight - window.innerHeight;
  console.log('📊 サイズ差分:');
  console.log('  幅の差:', widthDiff, 'px', widthDiff > 0 ? '⚠️ はみ出ている' : '✅');
  console.log('  高さの差:', heightDiff, 'px');
  console.log('');

  // 10. px-4, py-6 などのTailwindクラスを持つ要素
  console.log('🎨 padding/margin を持つ要素 (上位5件):');
  const allElements = document.querySelectorAll('*');
  const elementsWithSpacing = [];
  
  allElements.forEach(el => {
    const styles = window.getComputedStyle(el);
    const margin = styles.margin;
    const padding = styles.padding;
    const hasSpacing = margin !== '0px' || padding !== '0px';
    
    if (hasSpacing) {
      elementsWithSpacing.push({
        element: el,
        tag: el.tagName.toLowerCase(),
        classes: el.className,
        margin: margin,
        padding: padding,
      });
    }
  });

  // 上位5件を表示
  elementsWithSpacing.slice(0, 5).forEach((item, index) => {
    console.log(`  ${index + 1}. <${item.tag}> ${item.classes ? `class="${item.classes}"` : ''}`);
    console.log(`     margin: ${item.margin}, padding: ${item.padding}`);
  });
  console.log('');

  // 11. 最も外側の要素のボックスモデルを可視化
  console.log('💡 推奨アクション:');
  if (widthDiff > 0) {
    console.log('  ⚠️ 横スクロールバーが発生しています');
    console.log('  → 以下の要素を確認してください:');
    const wideElements = Array.from(allElements).filter(el => {
      return el.scrollWidth > window.innerWidth;
    });
    wideElements.slice(0, 3).forEach(el => {
      console.log(`     - <${el.tagName.toLowerCase()}> class="${el.className}"`);
    });
  }
  
  if (bodyStyles.margin !== '0px') {
    console.log('  ⚠️ <body> に margin があります');
    console.log('  → CSS で body { margin: 0; } を設定してください');
  }
  
  if (bodyStyles.padding !== '0px') {
    console.log('  ⚠️ <body> に padding があります');
    console.log('  → CSS で body { padding: 0; } を設定してください');
  }

  console.log('\n=== レポート終了 ===');
  console.log('💡 特定の要素を詳しく調べるには、Elements タブで選択してください');
})();
