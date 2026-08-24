import { useEffect, useState } from 'react'

import { api } from '@/lib/api'

type HealthState = 'checking' | 'ok' | 'down'

const LABEL: Record<HealthState, string> = {
  checking: '확인 중',
  ok: '연결됨',
  down: '응답 없음',
}

const DOT: Record<HealthState, string> = {
  checking: 'bg-neutral-400',
  ok: 'bg-emerald-500',
  down: 'bg-rose-500',
}

/**
 * 첫 화면은 프론트 dev 서버가 백엔드까지 제대로 프록시하는지 눈으로 확인하는 용도다.
 * 실제 화면을 붙이기 시작하면 이 컴포넌트는 통째로 갈아엎으면 된다.
 */
function App() {
  const [health, setHealth] = useState<HealthState>('checking')

  useEffect(() => {
    // StrictMode 가 개발 모드에서 effect 를 두 번 돌리므로, 먼저 뜬 요청은 정리 단계에서 끊는다.
    const controller = new AbortController()

    api<unknown>('/health', { signal: controller.signal })
      .then(() => setHealth('ok'))
      .catch(() => {
        if (!controller.signal.aborted) setHealth('down')
      })

    return () => controller.abort()
  }, [])

  return (
    <main className="mx-auto flex min-h-dvh max-w-2xl flex-col justify-center gap-8 px-6">
      <header className="space-y-2">
        <p className="text-sm font-medium text-neutral-500">HACKATHON TEAM 1</p>
        <h1 className="text-3xl font-bold tracking-tight">안녕하세요 원팀입니다 잘부탁드립니다</h1>
      </header>

      <div className="rounded-xl border border-neutral-200 p-5 dark:border-neutral-800">
        <div className="flex items-center gap-2.5">
          <span className={`size-2.5 rounded-full ${DOT[health]}`} aria-hidden />
          <span className="text-sm">
            백엔드 <code className="text-neutral-500">GET /health</code> — {LABEL[health]}
          </span>
        </div>
        {health === 'down' && (
          <p className="mt-3 text-sm text-neutral-500">
            <code>cd backend &amp;&amp; ./gradlew bootRun</code> 으로 서버를 띄우면 연결됩니다.
          </p>
        )}
      </div>

      <p className="text-sm text-neutral-500">
        <code>src/App.tsx</code> 를 수정해서 시작하세요.
      </p>
    </main>
  )
}

export default App
