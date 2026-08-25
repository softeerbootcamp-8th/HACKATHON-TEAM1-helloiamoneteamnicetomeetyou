import path from 'node:path'

import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      // 새 배포가 올라오면 다음 방문에 알아서 갈아끼우고 새로고침한다. 사용자에게
      // "업데이트 하시겠습니까" 를 묻는 화면은 아직 필요 없다.
      registerType: 'autoUpdate',
      // 서비스 워커 등록 스크립트는 플러그인이 index.html 에 넣어 준다.
      injectRegister: 'auto',
      // 아이콘은 pwa-assets.config.ts 가 public/logo.svg 로 미리 만들어 둔 것을 쓴다.
      manifest: {
        name: 'NearLy',
        short_name: 'NearLy',
        description: '현장에서 쉽고 빠르게 현대자동차 팝업 굿즈를 교환하세요',
        lang: 'ko',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        background_color: '#ffffff',
        theme_color: '#2ced90',
        icons: [
          { src: 'pwa-64x64.png', sizes: '64x64', type: 'image/png' },
          { src: 'pwa-192x192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512x512.png', sizes: '512x512', type: 'image/png' },
          {
            src: 'maskable-icon-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
      workbox: {
        // 빌드 산출물만 프리캐시한다. 오프라인에서 앱 껍데기가 뜨는 데까지가 목표다.
        globPatterns: ['**/*.{js,css,html,svg,png,ico,woff2}'],
        // API 응답은 캐시하지 않는다. 오래된 데이터가 화면에 남으면 디버깅이 어려워진다.
        // 여기에 runtimeCaching 을 넣지 않는 것으로 충분하고, 아래 denylist 는 /api 로 가는
        // 네비게이션 요청까지 index.html 로 되돌려 보내지 않게 막는 것이다.
        navigateFallbackDenylist: [/^\/api\//, /^\/health$/],
      },
    }),
  ],
  resolve: {
    // '@/components/Button' 처럼 src 기준 절대경로로 import 한다.
    // tsconfig.app.json 의 paths 와 짝을 맞춰야 하니 한쪽만 고치지 않는다.
    alias: {
      '@': path.resolve(import.meta.dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    // 개발 중에는 프론트(5173)와 백엔드(8080)의 오리진이 달라 그냥 부르면 CORS 에 막힌다.
    // 서버 쪽에 CORS 설정을 넣는 대신 dev 서버가 같은 오리진인 척 프록시해 준다.
    // 덕분에 코드에서는 배포 환경과 똑같이 상대경로('/api/...')로만 호출하면 된다.
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/health': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
