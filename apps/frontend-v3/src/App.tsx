import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { TooltipProvider } from '@mirel/ui'
import { AppRouter } from './app/routes'
import './App.css'

/**
 * Main App component
 * Provides React Router v7 + TanStack Query integration
 * 
 * Phase 1 - Step 4: TanStack Query setup
 */

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <AppRouter />
      </TooltipProvider>
    </QueryClientProvider>
  )
}

export default App
