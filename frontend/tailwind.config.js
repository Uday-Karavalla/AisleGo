/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f0fdf6',
          100: '#dcfced',
          200: '#b8f5d9',
          300: '#84e8bc',
          400: '#4bd39a',
          500: '#22b67d',
          600: '#149566',
          700: '#107753',
          800: '#105f44',
          900: '#0e4e3a',
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
