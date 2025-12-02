import * as React from 'react';
import { cn } from '../lib/utils';

export interface AvatarProps extends React.HTMLAttributes<HTMLDivElement> {
  /**
   * アバター画像URL
   */
  src?: string | null;
  
  /**
   * 代替テキスト（画像が読み込めない場合のフォールバック）
   */
  alt?: string;
  
  /**
   * アバターサイズ
   * @default 'md'
   */
  size?: 'sm' | 'md' | 'lg' | 'xl';
  
  /**
   * フォールバック文字（画像がない場合に表示）
   */
  fallback?: string;
}

const sizeClasses = {
  sm: 'h-8 w-8 text-xs',
  md: 'h-10 w-10 text-sm',
  lg: 'h-12 w-12 text-base',
  xl: 'h-16 w-16 text-lg',
};

/**
 * アバターコンポーネント
 * 
 * ユーザーのプロフィール画像を表示します。
 * 画像がない場合はフォールバック文字（イニシャル等）を表示します。
 */
export const Avatar = React.forwardRef<HTMLDivElement, AvatarProps>(
  ({ src, alt, size = 'md', fallback, className, ...props }, ref) => {
    const [imageError, setImageError] = React.useState(false);
    const [imageLoaded, setImageLoaded] = React.useState(false);

    // src変更時にエラー状態をリセット
    React.useEffect(() => {
      setImageError(false);
      setImageLoaded(false);
    }, [src]);

    const showImage = src && !imageError;
    const showFallback = !src || imageError || !imageLoaded;

    return (
      <div
        ref={ref}
        className={cn(
          'relative inline-flex items-center justify-center overflow-hidden rounded-full bg-muted',
          sizeClasses[size],
          className
        )}
        {...props}
      >
        {showImage && (
          <img
            src={src}
            alt={alt || 'Avatar'}
            className={cn(
              'h-full w-full object-cover',
              imageLoaded ? 'opacity-100' : 'opacity-0'
            )}
            onLoad={() => setImageLoaded(true)}
            onError={() => setImageError(true)}
          />
        )}
        
        {showFallback && fallback && (
          <span className="font-medium text-muted-foreground uppercase">
            {fallback}
          </span>
        )}
        
        {showFallback && !fallback && (
          <svg
            className="h-full w-full text-muted-foreground"
            fill="currentColor"
            viewBox="0 0 24 24"
          >
            <path d="M24 20.993V24H0v-2.996A14.977 14.977 0 0112.004 15c4.904 0 9.26 2.354 11.996 5.993zM16.002 8.999a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
        )}
      </div>
    );
  }
);

Avatar.displayName = 'Avatar';
