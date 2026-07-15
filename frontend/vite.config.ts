import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg', 'icons/*.png'],
      manifest: {
        name: 'AisleGo',
        short_name: 'AisleGo',
        description:
          'Shop your local supermarkets online — browse the catalogue, pay, and track delivery from stores near you.',
        theme_color: '#0e7490',
        background_color: '#ffffff',
        display: 'standalone',
        orientation: 'portrait',
        lang: 'en-IN',
        id: '/',
        start_url: '/',
        scope: '/',
        categories: ['shopping', 'food', 'lifestyle'],
        shortcuts: [
          {
            name: 'Find supermarkets',
            short_name: 'Find stores',
            description: 'Discover supermarkets near your location',
            url: '/',
            icons: [{ src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' }],
          },
          {
            name: 'Open cart',
            short_name: 'Cart',
            description: 'Review your AisleGo cart',
            url: '/cart',
            icons: [{ src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' }],
          },
        ],
        icons: [
          { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          {
            src: '/icons/icon-maskable-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,ico,webmanifest}'],
        navigateFallbackDenylist: [/^\/api\//],
      },
      devOptions: {
        enabled: false,
      },
    }),
  ],
  test: {
    include: ['src/test/**/*.test.{ts,tsx}'],
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: true,
  },
})
