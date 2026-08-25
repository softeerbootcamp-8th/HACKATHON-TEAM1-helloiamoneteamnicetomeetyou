import { defineConfig, minimal2023Preset } from '@vite-pwa/assets-generator/config'

// 아이콘을 바꿀 때는 public/logo.svg 하나만 갈아끼우고 `pnpm generate-pwa-assets` 를 돌린다.
// 같은 폴더의 favicon.ico, apple-touch-icon, pwa-*.png 가 전부 다시 만들어진다.
// 결과물은 커밋한다. 빌드할 때 만들지 않는 이유는 CI 와 Vercel 이 sharp 를 설치하지 않아도
// 되게 하려는 것이다.
export default defineConfig({
  preset: minimal2023Preset,
  images: ['public/logo.svg'],
})
