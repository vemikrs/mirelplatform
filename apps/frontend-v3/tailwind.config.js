/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
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
        // Tailwind default colors (preserve white, black, gray for utility classes)
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
        // Custom semantic colors
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        outline: "hsl(var(--outline))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        surface: {
          DEFAULT: "hsl(var(--surface))",
          subtle: "hsl(var(--surface-subtle))",
          raised: "hsl(var(--surface-raised))",
          overlay: "hsl(var(--surface-overlay))",
        },
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        info: {
          DEFAULT: "hsl(var(--info))",
          foreground: "hsl(var(--info-foreground))",
        },
        success: {
          DEFAULT: "hsl(var(--success))",
          foreground: "hsl(var(--success-foreground))",
        },
        warning: {
          DEFAULT: "hsl(var(--warning))",
          foreground: "hsl(var(--warning-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
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
        '110': '110',  // Dialog Overlay用
        '120': '120',  // Dialog Content用
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
