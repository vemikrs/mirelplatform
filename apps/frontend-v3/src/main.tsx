import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { setTokenProvider } from './lib/api/client'
import { useAuthStore } from './stores/authStore'

// Initialize API client with token provider from auth store
setTokenProvider(() => useAuthStore.getState().tokens?.accessToken);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
