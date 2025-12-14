import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@mirel/ui': path.resolve(__dirname, '../../packages/ui/src'),
    },
  },
  optimizeDeps: {
    exclude: ['@mirel/ui'],
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: (id) => {
          // node_modules のベンダーライブラリを分割
          if (id.includes('node_modules')) {
            // React コアライブラリ
            if (id.includes('react') || id.includes('react-dom')) {
              return 'vendor-react';
            }
            // ルーティング関連
            if (id.includes('react-router')) {
              return 'vendor-router';
            }
            // TanStack Query
            if (id.includes('@tanstack/react-query')) {
              return 'vendor-query';
            }
            // Radix UI コンポーネント
            if (id.includes('@radix-ui')) {
              return 'vendor-radix';
            }
            // アイコンライブラリ
            if (id.includes('lucide-react')) {
              return 'vendor-icons';
            }
            // Monaco Editor (大きいので単独分割)
            if (id.includes('monaco-editor') || id.includes('@monaco-editor')) {
              return 'vendor-monaco';
            }
            // State management
            if (id.includes('zustand')) {
              return 'vendor-state';
            }
            // HTTP client
            if (id.includes('axios')) {
              return 'vendor-http';
            }
            // Code editor
            if (id.includes('codemirror') || id.includes('@codemirror') || id.includes('@uiw/react-codemirror')) {
              return 'vendor-codemirror';
            }
            // Form libraries
            if (id.includes('react-hook-form') || id.includes('zod')) {
              return 'vendor-form';
            }
            // その他のベンダーライブラリ
            return 'vendor-misc';
          }
          
          // 機能モジュールごとに分割
          if (id.includes('/src/features/')) {
            if (id.includes('/features/promarker/')) {
              return 'feature-promarker';
            }
            if (id.includes('/features/mira/')) {
              return 'feature-mira';
            }
            if (id.includes('/features/studio/')) {
              return 'feature-studio';
            }
            if (id.includes('/features/admin/')) {
              return 'feature-admin';
            }
            if (id.includes('/features/auth/')) {
              return 'feature-auth';
            }
            if (id.includes('/features/stencil-editor/')) {
              return 'feature-stencil-editor';
            }
          }
        },
      },
    },
    chunkSizeWarningLimit: 1500, // React本体は1.4MBだがgzip後は432KB。実用上問題ないため閾値を調整
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: createProxyConfig(),
  },
  preview: {
    host: '0.0.0.0',
    port: 5173,
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
