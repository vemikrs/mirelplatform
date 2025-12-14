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
    '/mipla2': {
      target: `${backendUrl}/mipla2`,
      changeOrigin: true,
      rewrite: (path: string) => path.replace(/^\/mipla2/, ''),
    },
  };
}
