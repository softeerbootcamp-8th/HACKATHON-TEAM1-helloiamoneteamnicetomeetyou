/**
 * API 호출의 단일 진입점.
 *
 * 개발 중에는 base 가 빈 문자열이고 vite.config.ts 의 proxy 가 8080 으로 넘겨준다.
 * 배포 환경에서 API 오리진이 프론트와 다르면 VITE_API_BASE_URL 만 채우면 되고,
 * 호출하는 쪽 코드는 그대로 둔다.
 */
export const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * 백엔드가 모든 응답을 감싸는 공통 형식.
 * 형식 자체는 `.claude/skills/oneteam-development/references/contracts.md` 가 기준이다.
 */
export type CommonResponse<T> = {
  success: boolean
  data?: T
  code?: number
  message?: string
  errors?: { field: string; message: string }[]
}

export class ApiError extends Error {
  readonly status: number
  /** 팀 내부 에러 코드. 화면이 상황을 갈라야 할 때 본다 (예: 2000 이면 사용자 재등록). */
  readonly code?: number

  constructor(status: number, message: string, code?: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

/**
 * 실패 응답에서 사용자에게 보여줄 문장을 꺼낸다.
 *
 * 서버의 `message` 는 그대로 화면에 띄워도 되는 한글 문장이라는 것이 팀 약속이라, 있으면
 * 그것을 쓴다. 본문이 없거나 JSON 이 아닌 경우(프록시 오류 등)에만 기본 문장으로 떨어진다.
 */
async function toApiError(res: Response, path: string, method: string): Promise<ApiError> {
  try {
    const body = (await res.json()) as CommonResponse<never>
    if (body.message) return new ApiError(res.status, body.message, body.code)
  } catch {
    // 본문이 비었거나 JSON 이 아니다. 아래 기본 문장으로 간다.
  }

  // 서버가 팀 형식으로 답하지 못한 경우다. 프록시나 게이트웨이가 대신 뱉은 응답이라
  // 사용자에게 보여줄 문장이 없다. 기술적인 내용은 화면에 올리지 않고 개발 모드 콘솔로만 넘긴다.
  if (import.meta.env.DEV) {
    console.warn(`[api] ${method} ${path} → ${res.status} (팀 응답 형식이 아님)`)
  }

  return new ApiError(res.status, '서버에 닿지 못했어요. 잠시 뒤에 다시 시도해 주세요')
}

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })

  if (!res.ok) {
    throw await toApiError(res, path, init?.method ?? 'GET')
  }

  // 204 No Content 처럼 본문이 없는 응답에 res.json() 을 부르면 파싱 에러가 난다.
  if (res.status === 204) return undefined as T

  // /health 처럼 아직 JSON 이 아닌 엔드포인트도 있어서 Content-Type 을 보고 갈라 준다.
  if (!res.headers.get('Content-Type')?.includes('application/json')) {
    return (await res.text()) as T
  }

  return (await res.json()) as T
}

/**
 * 공통 응답 껍데기를 벗겨 `data` 만 돌려준다.
 *
 * 실패는 `api()` 가 이미 던진다. 여기서 `success` 를 다시 보는 것은 200 인데
 * `success: false` 인 경우를 잡기 위해서다. 그런 응답이 조용히 `undefined` 로 흘러가면
 * 화면이 빈 채로 성공한 것처럼 보인다.
 */
export async function apiData<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await api<CommonResponse<T>>(path, init)

  if (!res.success || res.data === undefined) {
    // 여기까지 왔다는 것은 HTTP 는 2xx 였다는 뜻이라 상태 코드는 200 으로 둔다.
    throw new ApiError(200, res.message ?? '서버 응답이 예상과 달라요', res.code)
  }

  return res.data
}

/** 본문 없이 성공만 확인하면 되는 요청(등록 등). 실패는 `api()` 가 던진다. */
export async function apiVoid(path: string, init?: RequestInit): Promise<void> {
  await api<CommonResponse<never>>(path, init)
}

/** 어떤 예외든 화면에 그대로 띄울 수 있는 한글 한 문장으로 바꾼다. */
export function messageOf(error: unknown): string {
  if (error instanceof ApiError) return error.message
  return '서버에 닿지 못했어요. 잠시 뒤에 다시 시도해 주세요'
}
