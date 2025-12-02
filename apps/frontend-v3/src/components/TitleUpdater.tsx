import { useEffect } from 'react';
import { useMatches } from 'react-router-dom';

/**
 * Route handle type definition for title
 */
export interface TitleHandle {
  title?: string;
}

/**
 * TitleUpdater component
 * Updates document.title based on the current route's handle.title property.
 * Falls back to 'mirelplatform' if no title is defined.
 */
export function TitleUpdater() {
  const matches = useMatches();

  useEffect(() => {
    // Find the last match that has a title in its handle
    // Note: findLast is ES2023, so we use reverse().find() for better compatibility
    const titleMatch = [...matches].reverse().find((match) => {
      const handle = match.handle as TitleHandle | undefined;
      return handle?.title;
    });

    const handle = titleMatch?.handle as TitleHandle | undefined;
    const pageTitle = handle?.title;

    if (pageTitle) {
      document.title = `${pageTitle} - mirelplatform`;
    } else {
      document.title = 'mirelplatform';
    }
  }, [matches]);

  return null;
}
