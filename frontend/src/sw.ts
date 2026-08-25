/// <reference lib="webworker" />
import { clientsClaim } from 'workbox-core'
import {
  cleanupOutdatedCaches,
  createHandlerBoundToURL,
  precacheAndRoute,
  type PrecacheEntry,
} from 'workbox-precaching'
import { NavigationRoute, registerRoute } from 'workbox-routing'

/**
 * 손으로 관리하는 서비스 워커다. `vite.config.ts` 의 `injectManifest` 가 아래
 * `self.__WB_MANIFEST` 자리에 프리캐시 목록을 채워 넣는다.
 *
 * **`pnpm dev` 에서는 돌지 않는다.** 확인하려면 `pnpm build && pnpm preview` 를 쓴다.
 */
declare let self: ServiceWorkerGlobalScope & {
  __WB_MANIFEST: (string | PrecacheEntry)[]
}

// registerType: 'autoUpdate' 의 동작이다. generateSW 일 때는 플러그인이 넣어 줬지만
// injectManifest 에서는 우리가 직접 써야 새 배포가 다음 방문에 갈아끼워진다.
self.skipWaiting()
clientsClaim()

cleanupOutdatedCaches()
precacheAndRoute(self.__WB_MANIFEST)

// 예전 workbox 설정에 있던 navigateFallback 과 denylist 를 그대로 옮긴 것이다.
// injectManifest 에는 그 옵션이 없어서, 이게 없으면 오프라인에서 앱 껍데기가 뜨지 않는다.
// /api 로 가는 네비게이션까지 index.html 로 되돌려 보내면 안 되므로 denylist 를 함께 둔다.
registerRoute(
  new NavigationRoute(createHandlerBoundToURL('index.html'), {
    denylist: [/^\/api\//, /^\/health$/],
  }),
)

type PushPayload = {
  title?: string
  body?: string
  url?: string
}

self.addEventListener('push', (event) => {
  // iOS 는 푸시를 받고 알림을 띄우지 않으면 그것을 silent push 로 보고 구독을 해지한다.
  // 몇 번 반복되면 조용히 알림이 끊기고, 증상이 "처음엔 되다가 안 됨" 이라 원인을 찾기 어렵다.
  // 그래서 파싱이 실패하든 내용이 비었든 무조건 showNotification 을 부른다.
  //
  // event.waitUntil 로 감싸는 것도 같은 이유다. 이게 없으면 showNotification 의 Promise 가
  // 이벤트가 끝난 뒤에 resolve 돼서 사파리가 역시 silent 로 판정한다.
  let payload: PushPayload

  try {
    payload = (event.data?.json() ?? {}) as PushPayload
  } catch {
    payload = { body: event.data?.text() }
  }

  event.waitUntil(
    self.registration.showNotification(payload.title ?? 'NearLy', {
      body: payload.body ?? '새로운 소식이 있어요.',
      // iOS 는 actions 와 image 를 지원하지 않는다. 아이콘과 뱃지만 쓴다.
      icon: '/pwa-192x192.png',
      badge: '/pwa-64x64.png',
      data: { url: payload.url ?? '/home' },
    }),
  )
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()

  const target = (event.notification.data as { url?: string } | undefined)?.url ?? '/home'

  event.waitUntil(
    (async () => {
      const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })

      // 이미 앱이 떠 있으면 창을 새로 열지 않고 그쪽으로 옮긴다. 홈 화면 앱에서 창이 여러 개
      // 생기면 사용자가 어느 것이 진짜인지 알 수 없다.
      const existing = windows.find((client) => client.url.startsWith(self.registration.scope))

      if (existing) {
        await existing.focus()
        // 라우팅은 앱이 한다. 서비스 워커가 주소를 바꾸면 진행 중이던 화면 상태가 날아간다.
        existing.postMessage({ type: 'notification-click', url: target })
        return
      }

      await self.clients.openWindow(target)
    })(),
  )
})
