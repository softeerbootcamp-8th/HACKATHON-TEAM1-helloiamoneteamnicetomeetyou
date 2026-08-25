import { MotionConfig } from 'motion/react'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router'

import { router } from '@/app/router'
import { getDeviceId, registerDevice } from '@/store/identity'
import { StoreProvider } from '@/store/StoreProvider'

import './index.css'

// 로그인 없이 기기를 식별한다. 백엔드 인증 방식이 정해지면 이 자리만 바꾼다.
// 서버 등록은 실패해도 화면을 막지 않는다. 백엔드가 안 떠 있어도 목업 흐름은 돌아야 한다.
void registerDevice(getDeviceId()).catch(() => {})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {/* index.css 의 media query 는 CSS 전환만 끈다. motion 의 애니메이션은 JS 가
        인라인 스타일로 그리기 때문에 여기서 따로 꺼 줘야 실제로 멈춘다. */}
    <MotionConfig reducedMotion="user">
      <StoreProvider>
        <RouterProvider router={router} />
      </StoreProvider>
    </MotionConfig>
  </StrictMode>,
)
