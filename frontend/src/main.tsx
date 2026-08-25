import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router'

import { router } from '@/app/router'
import { getDeviceId } from '@/store/identity'
import { StoreProvider } from '@/store/StoreProvider'

import './index.css'

// 로그인 없이 기기를 식별한다. 백엔드 인증 방식이 정해지면 이 자리만 바꾼다.
getDeviceId()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <StoreProvider>
      <RouterProvider router={router} />
    </StoreProvider>
  </StrictMode>,
)
