/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#ecfeff',
          100: '#cffafe',
          200: '#a5f3fc',
          300: '#67e8f9',
          400: '#22d3ee',
          500: '#0aa2c0',
          600: '#0e7490',
          700: '#155e75',
          800: '#164e63',
          900: '#123c4d',
        },
        surface: {
          DEFAULT: '#ffffff',
          muted: '#f5f7f6',
        },
        ink: {
          DEFAULT: '#10241c',
          muted: '#5c6b64',
          faint: '#95a29c',
        },
        danger: {
          50: '#fef2f2',
          500: '#dc2626',
          600: '#b91c1c',
        },
        warning: {
          50: '#fffbeb',
          500: '#d97706',
        },
      },
      borderRadius: {
        xl: '0.875rem',
        '2xl': '1.25rem',
        '3xl': '1.75rem',
      },
      boxShadow: {
        card: '0 1px 2px rgba(16, 36, 28, 0.06), 0 6px 20px rgba(16, 36, 28, 0.08)',
        pop: '0 8px 30px rgba(16, 36, 28, 0.16)',
      },
      spacing: {
        18: '4.5rem',
        22: '5.5rem',
      },
      maxWidth: {
        app: '480px',
      },
    },
  },
  plugins: [],
}
