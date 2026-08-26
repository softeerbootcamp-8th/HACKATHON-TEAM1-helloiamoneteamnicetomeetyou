import { MotionConfig } from 'motion/react'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router'

import { router } from '@/app/router'
import { CatalogProvider } from '@/features/catalog/CatalogProvider'
import { PokeProvider } from '@/features/poke/PokeProvider'
import { StoreProvider } from '@/store/StoreProvider'

import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {/* index.css 의 media query 는 CSS 전환만 끈다. motion 의 애니메이션은 JS 가
        인라인 스타일로 그리기 때문에 여기서 따로 꺼 줘야 실제로 멈춘다. */}
    <MotionConfig reducedMotion="user">
      {/* 기기 식별자를 서버에 등록하고 부스 카드 목록을 받아 둔다. 카드 등록 화면이 쓴다. */}
      <CatalogProvider>
        {/* 서버에 오간 찔러보기를 들고 실시간 알림을 구독한다. 부스를 알아야 구독할 수
            있어서 CatalogProvider 안쪽이다. */}
        <PokeProvider>
          <StoreProvider>
            <RouterProvider router={router} />
          </StoreProvider>
        </PokeProvider>
      </CatalogProvider>
    </MotionConfig>
  </StrictMode>,
)
