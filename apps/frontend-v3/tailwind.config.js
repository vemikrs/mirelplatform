/**
 * Tailwind CSS v4 Configuration
 * 
 * ============================================================================
 * ⚠️ 開発者向け重要ガイド（Tailwind v4 @theme統合）
 * ============================================================================
 * 
 * ■ セマンティックカラー（bg-background, text-primary等）について：
 *   - packages/ui/src/theme/index.css の @theme ブロックで定義されています
 *   - このファイル(tailwind.config.js)のcolorsセクションには追加しないでください
 *   - @themeにより、全てのVariants（hover:, data-[state=active]:等）が自動生成されます
 * 
 * ■ 新しいセマンティックカラーを追加する場合：
 *   1. packages/ui/src/theme/index.css の @layer base 内で CSS変数を定義
 *   2. 同ファイルの @theme ブロックに --color-[名前]: hsl(var(--[名前])) を追加
 * 
 * ■ このファイルのcolorsセクションについて：
 *   - Tailwindのデフォルト色（white, black, gray）のみ定義
 *   - CSS変数参照（hsl(var(--xxx))）は @theme 経由で処理されるため不要
 * 
 * 参考: https://tailwindcss.com/docs/v4-beta
 * ============================================================================
 * 
 * @type {import('tailwindcss').Config}
 */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    "../../packages/ui/src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    container: {
      center: true,
      padding: {
        DEFAULT: "1rem",      // 16px - スマートフォン
        sm: "1.5rem",         // 24px - 大スマートフォン
        md: "2rem",           // 32px - タブレット
        lg: "3rem",           // 48px - デスクトップ
        xl: "4rem",           // 64px - 大画面
        "2xl": "6rem",        // 96px - ワイドモニター
      },
      screens: {
        sm: "640px",
        md: "768px",
        lg: "1024px",
        xl: "1280px",
        "2xl": "1536px",
      },
    },
    extend: {
      screens: {
        xs: "475px",          // 追加ブレークポイント（小スマートフォン対応）
      },
      colors: {
        // Tailwind v4: セマンティックカラーは packages/ui/src/theme/index.css の
        // @theme ブロックで定義されているため、ここでは標準色のみ保持
        
        // Tailwind default colors
        white: '#ffffff',
        black: '#000000',
        gray: {
          50: '#f9fafb',
          100: '#f3f4f6',
          200: '#e5e7eb',
          300: '#d1d5db',
          400: '#9ca3af',
          500: '#6b7280',
          600: '#4b5563',
          700: '#374151',
          800: '#1f2937',
          900: '#111827',
          950: '#030712',
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
        // Liquid Design radius tokens
        'liquid-xs': 'var(--liquid-radius-xs)',
        'liquid-sm': 'var(--liquid-radius-sm)',
        'liquid-md': 'var(--liquid-radius-md)',
        'liquid-lg': 'var(--liquid-radius-lg)',
        'liquid-xl': 'var(--liquid-radius-xl)',
        'liquid-2xl': 'var(--liquid-radius-2xl)',
      },
      fontFamily: {
        sans: "var(--font-sans)",
        mono: "var(--font-mono)",
      },
      boxShadow: {
        sm: "var(--shadow-sm)",
        md: "var(--shadow-md)",
        lg: "var(--shadow-lg)",
        xl: "var(--shadow-xl)",
        // Liquid Design elevation tokens
        'liquid-floating': 'var(--liquid-elevation-floating)',
        'liquid-raised': 'var(--liquid-elevation-raised)',
        'liquid-overlay': 'var(--liquid-elevation-overlay)',
        'liquid-modal': 'var(--liquid-elevation-modal)',
      },
      spacing: {
        // Liquid Design spacing tokens
        'liquid-xs': 'var(--liquid-space-xs)',
        'liquid-sm': 'var(--liquid-space-sm)',
        'liquid-md': 'var(--liquid-space-md)',
        'liquid-lg': 'var(--liquid-space-lg)',
        'liquid-xl': 'var(--liquid-space-xl)',
        'liquid-2xl': 'var(--liquid-space-2xl)',
        'liquid-3xl': 'var(--liquid-space-3xl)',
      },
      backdropBlur: {
        'liquid-sm': 'var(--liquid-glass-blur-sm)',
        'liquid-md': 'var(--liquid-glass-blur-md)',
        'liquid-lg': 'var(--liquid-glass-blur-lg)',
        'liquid-xl': 'var(--liquid-glass-blur-xl)',
      },
      transitionTimingFunction: {
        'liquid-fluid': 'var(--liquid-ease-fluid)',
        'liquid-bounce': 'var(--liquid-ease-bounce)',
        'liquid-smooth': 'var(--liquid-ease-smooth)',
      },
      transitionDuration: {
        'liquid-instant': 'var(--liquid-duration-instant)',
        'liquid-fast': 'var(--liquid-duration-fast)',
        'liquid-normal': 'var(--liquid-duration-normal)',
        'liquid-slow': 'var(--liquid-duration-slow)',
      },
      zIndex: {
        '60': '60',   // Sheet Overlay用
        '110': '110', // Dialog Overlay用
        '120': '120', // Dialog Content用
      },
      keyframes: {
        'toast-in': {
          '0%': { opacity: 0, transform: 'translateY(12px) scale(0.95)' },
          '100%': { opacity: 1, transform: 'translateY(0) scale(1)' },
        },
        'toast-out': {
          '0%': { opacity: 1, transform: 'translateY(0) scale(1)' },
          '100%': { opacity: 0, transform: 'translateY(8px) scale(0.97)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-100% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        'liquid-float': {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-4px)' },
        },
        'liquid-glow': {
          '0%, 100%': { opacity: '0.5' },
          '50%': { opacity: '1' },
        },
      },
      animation: {
        'toast-in': 'toast-in 180ms cubic-bezier(0.22, 1, 0.36, 1)',
        'toast-out': 'toast-out 160ms cubic-bezier(0.22, 1, 0.36, 1)',
        'shimmer': 'shimmer 1.4s ease-in-out infinite',
        'liquid-float': 'liquid-float 3s ease-in-out infinite',
        'liquid-glow': 'liquid-glow 2s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
