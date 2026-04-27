import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const isNativeBuild = env.VITE_NATIVE_BUILD === '1';

  return {
    plugins: [
      react(),
      ...(isNativeBuild
        ? []
        : [
            VitePWA({
              registerType: 'autoUpdate',
              strategies: 'injectManifest',
              srcDir: 'src',
              filename: 'sw.ts',
              includeAssets: ['favicon.svg', 'favicon.ico', 'robots.txt', 'apple-touch-icon.png'],
              manifest: {
                name: 'NookMind',
                short_name: 'NookMind',
                description: 'Your personal reading tracker',
                theme_color: '#f8f6f1',
                background_color: '#0f1117',
                display: 'standalone',
                icons: [
                  { src: '/pwa-icon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' },
                  { src: '/pwa-icon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'maskable' },
                ],
              },
              injectManifest: {
                globPatterns: ['**/*.{js,css,html,ico,png,svg}'],
              },
              devOptions: {
                enabled: true,
                type: 'module',
              },
            }),
          ]),
    ],
  };
});
