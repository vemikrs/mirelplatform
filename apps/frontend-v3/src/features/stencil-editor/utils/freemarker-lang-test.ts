/**
 * FreeMarker highlighting test
 * ブラウザのコンソールで動作確認用
 */
import { freemarker } from './freemarker-lang';

// テスト用のFTLコード
const testCode = `
<#-- コメント -->
<#if user??>
  Hello \${user.name}!
  <#list items as item>
    \${item?html}
  </#list>
</#if>
`;

console.log('FreeMarker language mode:', freemarker);
console.log('Test code:', testCode);
