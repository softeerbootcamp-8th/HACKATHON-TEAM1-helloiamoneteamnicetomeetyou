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

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
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
    throw new ApiError(res.status, `${init?.method ?? 'GET'} ${path} → ${res.status}`)
  }

  // 204 No Content 처럼 본문이 없는 응답에 res.json() 을 부르면 파싱 에러가 난다.
  if (res.status === 204) return undefined as T

  // /health 처럼 아직 JSON 이 아닌 엔드포인트도 있어서 Content-Type 을 보고 갈라 준다.
  if (!res.headers.get('Content-Type')?.includes('application/json')) {
    return (await res.text()) as T
  }

  return (await res.json()) as T
}
