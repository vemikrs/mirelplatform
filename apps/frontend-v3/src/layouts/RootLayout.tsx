import { Outlet } from 'react-router-dom';

/**
 * Root layout component
 * Provides common layout structure for all pages
 */
export function RootLayout() {
  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <h1 className="text-2xl font-bold text-foreground">
                ProMarker Platform
              </h1>
              <nav className="flex space-x-4">
                <a 
                  href="/" 
                  className="text-sm text-muted-foreground hover:text-foreground transition-colors"
                >
                  Home
                </a>
                <a 
                  href="/promarker" 
                  className="text-sm text-muted-foreground hover:text-foreground transition-colors"
                >
                  ProMarker
                </a>
              </nav>
            </div>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="container mx-auto px-4 py-8">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="border-t mt-auto">
        <div className="container mx-auto px-4 py-6">
          <p className="text-sm text-muted-foreground text-center">
            © 2025 ProMarker Platform. Powered by mirelplatform framework.
          </p>
        </div>
      </footer>
    </div>
  );
}
