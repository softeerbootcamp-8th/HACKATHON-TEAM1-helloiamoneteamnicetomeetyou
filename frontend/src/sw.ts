/// <reference lib="webworker" />
import { clientsClaim } from 'workbox-core'
import {
  cleanupOutdatedCaches,
  createHandlerBoundToURL,
  precacheAndRoute,
  type PrecacheEntry,
} from 'workbox-precaching'
import { CacheableResponsePlugin } from 'workbox-cacheable-response'
import { ExpirationPlugin } from 'workbox-expiration'
import { NavigationRoute, registerRoute } from 'workbox-routing'
import { CacheFirst } from 'workbox-strategies'

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

/**
 * 굿즈 카드와 식별 표시 그림은 Supabase 스토리지에서 온다. 한 번 받은 것은 캐시에서 준다.
 *
 * **스토리지가 `cache-control: no-cache` 로 준다.** 그래서 브라우저 캐시만 믿으면 쓸 때마다
 * 서버에 "안 바뀌었지" 를 다시 물어본다. 바뀐 게 없어도 왕복은 왕복이라, 네트워크가 막히면
 * 그림이 안 뜬다. 하필 식별 화면은 사람이 몰린 곳에서 쓰고 그림이 화면 내용의 전부다.
 * CacheFirst 는 그 왕복을 건너뛴다.
 *
 * **주소가 곧 내용이어야 한다.** 그림을 같은 이름으로 갈아 끼우면 여기 걸린 사람은 30일 동안
 * 옛 그림을 본다. 식별 표시는 `store/identity-mark.ts` 의 `IMAGE_VERSION` 을 올려서 주소를
 * 바꾼다.
 */
registerRoute(
  ({ url }) => url.origin === 'https://sdumqvkniemiowanvsef.supabase.co',
  new CacheFirst({
    cacheName: 'supabase-images',
    plugins: [
      // **0(opaque)을 넣지 않는다.** no-cors 로 나간 요청은 성공이든 404 든 status 가 0 이라,
      // 0 을 캐시하면 실패한 응답이 성공으로 굳어서 그 기기에서는 그림이 영영 안 뜬다.
      // 그림을 부르는 쪽에 crossOrigin 을 붙여 두었으니 실제 status 가 그대로 온다.
      new CacheableResponsePlugin({ statuses: [200] }),
      new ExpirationPlugin({ maxEntries: 60, maxAgeSeconds: 30 * 24 * 60 * 60 }),
    ],
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
