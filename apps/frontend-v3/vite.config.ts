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
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/mapi': {
        target: 'http://localhost:3000/mipla2',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/mapi/, ''),
        // Cookie のドメインとパスを書き換える
        cookieDomainRewrite: 'localhost',
        cookiePathRewrite: '/',
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            // Log only that Set-Cookie header exists to avoid leaking sensitive values
            const setCookie = proxyRes.headers['set-cookie'];
            if (setCookie) {
              console.log('[Vite Proxy] Set-Cookie received (masked). Count:', Array.isArray(setCookie) ? setCookie.length : 1);
            }
          });
        },
      },
    },
  },
})
