import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@mireljs/ui-core': path.resolve(__dirname, '../../packages/ui/src'),
      '@mirel/ui': path.resolve(__dirname, '../../packages/ui/src'), // TODO: Remove this alias
    },
    // React重複インスタンスを防ぐため、単一インスタンスに統一
    // モノレポ構成でのReact解決の一貫性を保証
    dedupe: ['react', 'react-dom', 'react/jsx-runtime'],
  },
  optimizeDeps: {
    // Reactを事前バンドルに含めることで、@mirel/uiとの整合性を保つ
    // force: trueでnode_modules配下のReactを統一
    force: true,
    include: ['react', 'react-dom', 'react/jsx-runtime'],
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: (id) => {
          // node_modules のベンダーライブラリを分割
          if (id.includes('node_modules')) {
            // アイコンライブラリ
            if (id.includes('lucide-react')) {
              return 'vendor-icons';
            }
            // Monaco Editor (大きいので単独分割)
            if (id.includes('monaco-editor') || id.includes('@monaco-editor')) {
              return 'vendor-monaco';
            }
            // Code editor
            if (id.includes('codemirror') || id.includes('@codemirror') || id.includes('@uiw/react-codemirror')) {
              return 'vendor-codemirror';
            }
            // その他は Rollup に任せる（循環依存を作りやすい細分化を避ける）
            return undefined;
          }
          
          // アプリ側の分割も Rollup に任せる（循環依存を避ける）
          return undefined;
        },
      },
    },
    chunkSizeWarningLimit: 1500,
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: createProxyConfig(),
  },
  preview: {
    host: '0.0.0.0',
    port: 4173,
    proxy: createProxyConfig(),
    allowedHosts: getAllowedHosts(),
  },
})

// Get allowed hosts from environment variable
// Example: VITE_ALLOWED_HOSTS=mirel.vemi.jp,example.com
function getAllowedHosts(): string[] | undefined {
  const allowedHosts = process.env.VITE_ALLOWED_HOSTS;
  if (!allowedHosts) {
    return undefined; // Allow all hosts by default
  }
  
  // Parse comma-separated hosts
  const hosts = allowedHosts.split(',').map(host => host.trim()).filter(Boolean);
  
  // Always include localhost variants for local development and Docker internal access
  const localhostVariants = ['localhost', '127.0.0.1', '::1'];
  
  // Combine custom hosts with localhost, removing duplicates
  return Array.from(new Set([...hosts, ...localhostVariants]));
}

// Environment-aware proxy configuration
// Supports both local development and Docker container environments
function createProxyConfig() {
  // Get backend URL from environment variable, fallback to localhost
  const backendUrl = process.env.VITE_BACKEND_URL || 'http://localhost:3000';
  
  return {
    // API endpoints proxy - rewrites /mapi to /mipla2
    '/mapi': {
      target: `${backendUrl}/mipla2`,
      changeOrigin: true,
      rewrite: (path: string) => path.replace(/^\/mapi/, ''),
      // Cookie のドメインとパスを書き換える
      cookieDomainRewrite: 'localhost',
      cookiePathRewrite: '/',
      configure: (proxy: any) => {
        proxy.on('proxyRes', (proxyRes: any) => {
          // Log only that Set-Cookie header exists to avoid leaking sensitive values
          const setCookie = proxyRes.headers['set-cookie'];
          if (setCookie) {
            console.log('[Vite Proxy] Set-Cookie received (masked). Count:', Array.isArray(setCookie) ? setCookie.length : 1);
          }
        });
      },
    },
    // Static files proxy (avatars, uploaded files, etc.) - direct passthrough
    // This is needed for <img> and other static resource requests that bypass API client
    '/mipla2': {
      target: backendUrl,
      changeOrigin: true,
    },
  };
}
