import path from 'node:path'

import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
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
