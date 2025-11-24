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
      },
    },
  },
})
