import { api, type CommonResponse } from '@/lib/api'

/**
 * 웹푸시 구독을 만들고 서버에 등록한다.
 *
 * 앱이 열려 있는 동안의 알림은 SSE 가 맡는다. 여기서 하는 것은 **앱을 닫아 둔 사이에도**
 * 소식이 닿게 하는 것이고, 서버가 SSE 연결이 없는 사람에게만 보낸다. 그래서 화면에서
 * "지금 앱이 열려 있으니 무시" 같은 처리를 하지 않아도 같은 알림이 두 번 오지 않는다.
 */
type VapidPublicKeyResponseDto = { publicKey: string }

export type PushSupport =
  /** 켤 수 있다 */
  | 'available'
  /** iOS 사파리 탭이다. 홈 화면에 추가해야 켤 수 있다 */
  | 'needs-install'
  /** 이 브라우저로는 안 된다 */
  | 'unsupported'

/**
 * iOS 는 홈 화면에 추가한 웹앱에서만 `PushManager` 를 노출한다. 사파리 탭에서는 아예 없다.
 * 그래서 이 객체의 존재 여부가 곧 "설치했는가" 의 판정이 된다.
 */
export function detectPushSupport(): PushSupport {
  if (!('serviceWorker' in navigator)) return 'unsupported'
  if ('PushManager' in window && 'Notification' in window) return 'available'

  return isIos() ? 'needs-install' : 'unsupported'
}

function isIos(): boolean {
  // iPadOS 13+ 는 자기를 Mac 이라고 말한다. 터치 지원 여부를 같이 본다.
  return (
    /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.userAgent.includes('Mac') && navigator.maxTouchPoints > 1)
  )
}

export function getPermission(): NotificationPermission | null {
  return 'Notification' in window ? Notification.permission : null
}

/**
 * 서버에서 VAPID 공개키를 받는다.
 *
 * 환경변수가 아니라 API 인 이유는, 값을 바꿀 때 프론트를 다시 배포하지 않아도 되게 하기
 * 위해서다. 비밀이 아니라 그대로 내려받아도 된다.
 */
export async function fetchVapidPublicKey(): Promise<string> {
  const res = await api<CommonResponse<VapidPublicKeyResponseDto>>('/api/push/vapid-public-key')

  if (!res.data?.publicKey) throw new Error('VAPID 공개키를 받지 못했습니다.')

  return res.data.publicKey
}

function toApplicationServerKey(base64Url: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64Url.length % 4)) % 4)
  const base64 = (base64Url + padding).replace(/-/g, '+').replace(/_/g, '/')
  const raw = atob(base64)

  // ArrayBuffer 를 직접 만들어 넘긴다. new Uint8Array(길이) 는 SharedArrayBuffer 도 될 수 있는
  // 타입이라 applicationServerKey 가 받지 않는다.
  const bytes = new Uint8Array(new ArrayBuffer(raw.length))

  for (let i = 0; i < raw.length; i += 1) bytes[i] = raw.charCodeAt(i)

  return bytes
}

async function saveOnServer(userId: string, subscription: PushSubscription): Promise<void> {
  // toJSON() 의 p256dh 와 auth 는 스펙상 이미 base64url(패딩 없음)이다.
  // getKey() 로 ArrayBuffer 를 받아 직접 인코딩하면 표준 base64 가 나와서 서버가 못 읽는다.
  const json = subscription.toJSON()

  await api<CommonResponse<void>>('/api/push/subscriptions', {
    method: 'POST',
    body: JSON.stringify({
      userId,
      endpoint: json.endpoint,
      p256dh: json.keys?.p256dh,
      auth: json.keys?.auth,
    }),
  })
}

/**
 * 알림을 켠다. **반드시 클릭 핸들러 안에서 부른다.**
 *
 * iOS 는 사용자가 직접 누른 흐름 밖에서의 권한 요청을 거부한다. `vapidPublicKey` 를 인자로
 * 받는 것도 같은 이유다. 여기서 네트워크를 기다리면 그 사이에 제스처 컨텍스트가 끊긴다.
 */
export async function enablePush(userId: string, vapidPublicKey: string): Promise<void> {
  const permission = await Notification.requestPermission()

  if (permission !== 'granted') {
    throw new Error('알림 권한이 허용되지 않았습니다.')
  }

  const registration = await navigator.serviceWorker.ready
  const existing = await registration.pushManager.getSubscription()

  const subscription =
    existing ??
    (await registration.pushManager.subscribe({
      // iOS 는 알림을 띄우지 않는 푸시를 허용하지 않는다. 항상 true 다.
      userVisibleOnly: true,
      applicationServerKey: toApplicationServerKey(vapidPublicKey),
    }))

  await saveOnServer(userId, subscription)
}

/**
 * 이미 켜 둔 구독을 서버에 다시 등록한다. 앱을 열 때마다 부른다.
 *
 * `getSubscription()` 은 읽기라 제스처가 필요 없다. **`unsubscribe()` 는 부르지 않는다** —
 * 사파리는 한 번 해지하면 다시 켤 때 또 사용자 제스처를 요구한다. 그리고 사파리가 지원하지
 * 않는 `pushsubscriptionchange` 대신, 이 재등록이 갱신된 구독을 서버에 맞춰 주는 역할을 한다.
 */
export async function syncPush(userId: string): Promise<boolean> {
  if (detectPushSupport() !== 'available') return false
  if (getPermission() !== 'granted') return false

  const registration = await navigator.serviceWorker.ready
  const subscription = await registration.pushManager.getSubscription()

  if (!subscription) return false

  await saveOnServer(userId, subscription)

  return true
}

/** 켜자마자 실제로 오는지 눈으로 확인시켜 준다. */
export async function sendTestPush(userId: string): Promise<void> {
  await api<CommonResponse<void>>('/api/push/test', {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
}
