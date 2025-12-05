import React, { useState } from 'react';
import { Resizable, ResizeCallbackData } from 'react-resizable';
import 'react-resizable/css/styles.css';

// Simple utility for class names if @mirel/ui version is not available yet
// const cn = (...classes: (string | undefined | null | false)[]) => classes.filter(Boolean).join(' ');

interface StudioLayoutProps {
  children: React.ReactNode;
  /**
   * Navigation content (Left pane)
   */
  navigation?: React.ReactNode;
  /**
   * Explorer content (Left pane, unified with navigation or additional)
   */
  explorer?: React.ReactNode;
  /**
   * Properties content (Right pane)
   */
  properties?: React.ReactNode;
  /**
   * Whether to hide the navigation pane
   */
  hideNavigation?: boolean;
  /**
   * Whether to hide the properties pane
   */
  hideProperties?: boolean;
}

/**
 * StudioLayout
 * 
 * A 3-pane layout component for Mirel Studio.
 * - Left: Navigation / Explorer
 * - Center: Main Content (children)
 * - Right: Properties
 */
export const StudioLayout: React.FC<StudioLayoutProps> = ({
  children,
  navigation,
  explorer,
  properties,
  hideNavigation = false,
  hideProperties = false,
}) => {
  const [leftWidth, setLeftWidth] = useState(250);
  const [rightWidth, setRightWidth] = useState(300);

  const onResizeLeft = (_event: React.SyntheticEvent, { size }: ResizeCallbackData) => {
    setLeftWidth(size.width);
  };

  const onResizeRight = (_event: React.SyntheticEvent, { size }: ResizeCallbackData) => {
    setRightWidth(size.width);
  };

  // Combine navigation and explorer
  const showLeft = !hideNavigation && (navigation || explorer);
  const showRight = !hideProperties && properties;

  return (
    <div className="flex h-screen w-full overflow-hidden bg-background text-foreground">
      {/* Left Pane */}
      {showLeft && (
        <Resizable
          width={leftWidth}
          height={Infinity}
          resizeHandles={['e']}
          onResize={onResizeLeft}
          handle={
            <div
              className="absolute right-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-black/10 dark:hover:bg-white/10 z-10 transition-colors"
              style={{ right: -2 }}
            />
          }
        >
          <aside
            style={{ width: leftWidth }}
            className="flex h-full flex-col border-r bg-muted/5 relative shrink-0"
          >
            {navigation}
            {explorer}
          </aside>
        </Resizable>
      )}

      {/* Main Content */}
      <main className="flex-1 overflow-hidden relative min-w-0 flex flex-col">
        {children}
      </main>

      {/* Right Pane */}
      {showRight && (
        <Resizable
          width={rightWidth}
          height={Infinity}
          resizeHandles={['w']}
          onResize={onResizeRight}
          handle={
            <div
              className="absolute left-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-black/10 dark:hover:bg-white/10 z-10 transition-colors"
              style={{ left: -2 }}
            />
          }
        >
          <aside
            style={{ width: rightWidth }}
            className="flex h-full flex-col border-l bg-background relative shrink-0"
          >
            {properties}
          </aside>
        </Resizable>
      )}
    </div>
  );
};
