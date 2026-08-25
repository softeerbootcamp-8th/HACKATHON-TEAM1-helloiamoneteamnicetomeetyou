import { useCallback, useEffect, useState } from 'react'

import { api, ApiError, type CommonResponse } from '@/lib/api'

type PingData = {
  message: string
  serverTime: string
}

type PingState =
  | { status: 'loading' }
  | { status: 'ok'; data: PingData }
  | { status: 'empty' }
  | { status: 'error'; reason: string }

const DOT: Record<PingState['status'], string> = {
  loading: 'bg-neutral-400',
  ok: 'bg-emerald-500',
  empty: 'bg-amber-500',
  error: 'bg-rose-500',
}

function formatServerTime(serverTime: string): string {
  const parsed = new Date(serverTime)
  if (Number.isNaN(parsed.getTime())) return serverTime
  return parsed.toLocaleString('ko-KR')
}

/**
 * 첫 화면은 프론트가 백엔드까지 실제로 닿는지 눈으로 확인하는 용도다.
 * `/api/ping` 을 부르는 이유는 프록시뿐 아니라 `/api` prefix 와 공통 응답 형식 파싱까지
 * 한 번에 걸리기 때문이다. 실제 화면을 붙이기 시작하면 이 컴포넌트는 통째로 갈아엎으면 된다.
 */
function App() {
  const [ping, setPing] = useState<PingState>({ status: 'loading' })

  // 상태를 여기서 'loading' 으로 먼저 바꾸지 않는다. effect 안에서 동기로 setState 하면
  // react-hooks/set-state-in-effect 에 걸리고, 첫 렌더의 상태가 이미 'loading' 이라 필요도 없다.
  const requestPing = useCallback((signal?: AbortSignal) => {
    api<CommonResponse<PingData>>('/api/ping', { signal })
      .then((res) => {
        if (!res.success || !res.data) {
          setPing({ status: 'empty' })
          return
        }
        setPing({ status: 'ok', data: res.data })
      })
      .catch((error: unknown) => {
        if (signal?.aborted) return
        const reason =
          error instanceof ApiError
            ? `서버가 ${error.status} 로 응답했습니다.`
            : '서버에 닿지 못했습니다.'
        setPing({ status: 'error', reason })
      })
  }, [])

  useEffect(() => {
    // StrictMode 가 개발 모드에서 effect 를 두 번 돌리므로, 먼저 뜬 요청은 정리 단계에서 끊는다.
    const controller = new AbortController()
    requestPing(controller.signal)
    return () => controller.abort()
  }, [requestPing])

  const handleRetry = () => {
    setPing({ status: 'loading' })
    requestPing()
  }

  return (
    <main className="mx-auto flex min-h-dvh max-w-2xl flex-col justify-center gap-8 px-6">
      <header className="space-y-2">
        <p className="text-sm font-medium text-neutral-500">HACKATHON TEAM 1</p>
        <h1 className="text-3xl font-bold tracking-tight">안녕하세요 원팀입니다 잘부탁드립니다</h1>
      </header>

      <section className="rounded-xl border border-neutral-200 p-5 dark:border-neutral-800">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-2.5">
            <span className={`size-2.5 rounded-full ${DOT[ping.status]}`} aria-hidden />
            <span className="text-sm">
              백엔드 <code className="text-neutral-500">GET /api/ping</code>
            </span>
          </div>

          <button
            type="button"
            onClick={handleRetry}
            disabled={ping.status === 'loading'}
            className="rounded-lg border border-neutral-200 px-3 py-1.5 text-sm font-medium hover:bg-neutral-50 disabled:opacity-50 dark:border-neutral-800 dark:hover:bg-neutral-900"
          >
            다시 보내기
          </button>
        </div>

        <div aria-live="polite" className="mt-4 text-sm">
          {ping.status === 'loading' && <p className="text-neutral-500">요청을 보내는 중입니다.</p>}

          {ping.status === 'ok' && (
            <dl className="space-y-1">
              <div className="flex gap-2">
                <dt className="text-neutral-500">응답</dt>
                <dd>
                  <code>{ping.data.message}</code>
                </dd>
              </div>
              <div className="flex gap-2">
                <dt className="text-neutral-500">서버 시각</dt>
                <dd>{formatServerTime(ping.data.serverTime)}</dd>
              </div>
            </dl>
          )}

          {ping.status === 'empty' && (
            <p className="text-neutral-500">
              응답은 왔는데 <code>data</code> 가 비어 있습니다. 백엔드 응답 형식을 확인해 주세요.
            </p>
          )}

          {ping.status === 'error' && (
            <div className="space-y-2">
              <p className="text-rose-600 dark:text-rose-400">{ping.reason}</p>
              <p className="text-neutral-500">
                <code>cd backend &amp;&amp; ./gradlew bootRun</code> 으로 서버를 띄우면 연결됩니다.
              </p>
            </div>
          )}
        </div>
      </section>

      <p className="text-sm text-neutral-500">
        <code>src/App.tsx</code> 를 수정해서 시작하세요.
      </p>
    </main>
  )
}

export default App
