import { MotionConfig } from 'motion/react'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router'

import { router } from '@/app/router'
import { CatalogProvider } from '@/features/catalog/CatalogProvider'
import { StoreProvider } from '@/store/StoreProvider'

import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {/* index.css 의 media query 는 CSS 전환만 끈다. motion 의 애니메이션은 JS 가
        인라인 스타일로 그리기 때문에 여기서 따로 꺼 줘야 실제로 멈춘다. */}
    <MotionConfig reducedMotion="user">
      {/* 기기 식별자를 서버에 등록하고 부스 카드 목록을 받아 둔다. 카드 등록 화면이 쓴다. */}
      <CatalogProvider>
        <StoreProvider>
          <RouterProvider router={router} />
        </StoreProvider>
      </CatalogProvider>
    </MotionConfig>
  </StrictMode>,
)
