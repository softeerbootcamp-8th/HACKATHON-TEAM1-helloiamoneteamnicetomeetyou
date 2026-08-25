import { motion } from 'motion/react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { GoodsCard, GoodsFace } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { useCatalog } from '@/features/catalog/useCatalog'
import type { ReceivedPoke, ServerItemRef } from '@/features/poke/api'
import { usePoke } from '@/features/poke/usePoke'
import { staggerChild, staggerParent } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { itemById, type Item } from '@/mocks/data'
import { useStore } from '@/store/useStore'

/**
 * 받은 교환 요청. 상대의 묶음에서 한 장을 고른다.
 *
 * 서버에 실제로 온 찔러보기가 있으면 그걸 쓰고, 없으면 목업 흐름으로 떨어진다. 매칭
 * 알고리즘(#20)이 들어와 목업이 걷히면 아래쪽 분기가 사라진다.
 */
export function PokeReceived() {
  const { received, accept, reject, ready } = usePoke()

  // 나가는 중에는 목록이 이미 비어 있어서, 처음 잡은 건을 계속 들고 있는다.
  const serverPoke = useLastDefined(received[0])

  if (ready && serverPoke) {
    return <ServerPokeReceived poke={serverPoke} onAccept={accept} onReject={reject} />
  }

  return <MockPokeReceived />
}

/** 서버에서 받은 찔러보기. 카드 그림은 목업에서 이름으로 찾아 온다. */
function ServerPokeReceived({
  poke,
  onAccept,
  onReject,
}: {
  poke: ReceivedPoke
  onAccept: (pokeId: number, chosenItemId: number) => Promise<void>
  onReject: (pokeId: number) => Promise<void>
}) {
  const navigate = useNavigate()
  const { state: catalog } = useCatalog()
  const [picked, setPicked] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [rejectOpen, setRejectOpen] = useState(false)

  const mockItemOf = catalog.status === 'ready' ? catalog.mockItemOf : undefined

  // 서버 카드를 화면이 그릴 수 있는 형태로 바꾼다. 이름으로 짝을 못 찾은 카드는 이름만
  // 남기고 그림 없이 보여 준다. 조용히 버리면 상대가 3장을 냈는데 2장만 보이게 된다.
  const offered = useMemo(
    () => poke.offeredItems.map((server) => ({ server, mock: mockItemOf?.(server.id) })),
    [poke.offeredItems, mockItemOf],
  )
  const requested = mockItemOf?.(poke.requestedItem.id)

  // 아직 아무것도 안 골랐으면 첫 장을 고른 것으로 본다. 렌더할 때 계산하면 상태가 하나 줄어든다.
  const chosen = picked ?? poke.offeredItems[0]?.id ?? null

  const submitAccept = async () => {
    if (chosen === null || submitting) return
    setSubmitting(true)
    try {
      await onAccept(poke.pokeId, chosen)
      navigate('/match')
    } catch {
      // 사유는 PokeProvider 가 잡아 두고 홈 화면이 띄운다. 여기서는 화면을 붙잡아 둔다.
      setSubmitting(false)
    }
  }

  const submitReject = async () => {
    setRejectOpen(false)
    setSubmitting(true)
    try {
      await onReject(poke.pokeId)
      navigate('/home')
    } catch {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <div className="no-scrollbar flex-1 overflow-y-auto px-6 pt-6">
        <h1 className="text-[24px] font-extrabold tracking-[-0.02em] text-ink">
          상대가 교환을 요청했어요
        </h1>
        <p className="mt-2 text-[13px] text-neutral-400">
          상대의 카드 묶음에서 원하는 1장을 선택할 수 있어요
        </p>

        <section className="mt-7">
          <h2 className="text-[13px] font-bold text-ink">상대가 원하는 카드</h2>
          <div className="mt-3 w-[118px] rounded-2xl bg-white p-2.5 shadow-[0_4px_16px_rgba(0,0,0,0.08)]">
            {requested ? (
              <>
                <GoodsFace item={requested} size="md" />
                <p className="mt-2 text-center text-[12px] font-bold text-ink">{requested.name}</p>
                <p className="text-center text-[11px] text-neutral-400">{requested.nameKo}</p>
              </>
            ) : (
              <p className="py-6 text-center text-[12px] font-bold text-ink">
                {poke.requestedItem.name}
              </p>
            )}
          </div>
        </section>

        <section className="mt-8">
          <h2 className="text-[13px] font-bold text-ink">상대의 카드 묶음 (1장 선택)</h2>
          <motion.div
            variants={staggerParent}
            initial="hidden"
            animate="show"
            className="mt-3 grid grid-cols-3 gap-3"
          >
            {offered.map(({ server, mock }) => (
              <motion.div key={server.id} variants={staggerChild}>
                <OfferedCard
                  server={server}
                  mock={mock}
                  selected={chosen === server.id}
                  onClick={() => setPicked(server.id)}
                />
              </motion.div>
            ))}
          </motion.div>
        </section>
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button disabled={chosen === null || submitting} onClick={submitAccept}>
          {submitting ? '보내는 중...' : '이 카드로 진행하기'}
        </Button>
        <TextButton onClick={() => setRejectOpen(true)}>거절하기</TextButton>
      </div>

      {/* 시안 desc 204:5630. 거절은 되돌릴 수 없어서 한 번 더 묻는다. */}
      <Dialog
        open={rejectOpen}
        title="교환 요청을 거절할까요?"
        description="거절하면 상대에게 알림이 가고 이 요청은 사라져요."
        cancelLabel="아니요"
        confirmLabel="거절할게요"
        onCancel={() => setRejectOpen(false)}
        onConfirm={submitReject}
      />
    </div>
  )
}

/** 목업 카드에 짝이 있으면 그림째로, 없으면 서버가 준 이름만 보여 준다. */
function OfferedCard({
  server,
  mock,
  selected,
  onClick,
}: {
  server: ServerItemRef
  mock: Item | undefined
  selected: boolean
  onClick: () => void
}) {
  if (mock) {
    return <GoodsCard item={mock} selected={selected} onClick={onClick} />
  }

  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={selected}
      className={`w-full rounded-2xl bg-white p-2.5 text-center shadow-[0_2px_10px_rgba(0,0,0,0.06)] ${
        selected ? 'ring-2 ring-ink' : 'ring-1 ring-neutral-100'
      }`}
    >
      <span className="flex h-[74px] items-center justify-center rounded-xl bg-tile text-[13px] font-bold text-ink">
        {server.name}
      </span>
      <p className="mt-2 text-[12px] font-bold text-ink">{server.name}</p>
    </button>
  )
}

/** 목업 흐름. 서버에 받은 찔러보기가 없을 때 쓴다. */
function MockPokeReceived() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { state, dispatch } = useStore()
  const incoming = useLastDefined(state.incomingPoke)
  const [picked, setPicked] = useState<string | null>(null)
  const [rejectOpen, setRejectOpen] = useState(false)
  const demo = params.get('demo')

  // 주소로 바로 열었을 때 화면을 볼 수 있게 상태를 심어 준다.
  // 렌더 중에 dispatch 하면 React 가 같은 렌더에서 상태를 바꾼다고 경고한다.
  useEffect(() => {
    if (demo && !incoming) dispatch({ type: 'seed-demo', kind: 'incoming' })
  }, [demo, incoming, dispatch])

  const chosen = picked ?? incoming?.offeredItemIds[0] ?? null

  if (!incoming) {
    return (
      <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-8 text-center">
          <p className="text-[15px] text-neutral-500">받은 교환 요청이 없어요.</p>
          <Button onClick={() => navigate('/home')}>홈으로</Button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <div className="no-scrollbar flex-1 overflow-y-auto px-6 pt-6">
        <h1 className="text-[24px] font-extrabold tracking-[-0.02em] text-ink">
          상대가 교환을 요청했어요
        </h1>
        <p className="mt-2 text-[13px] text-neutral-400">
          상대의 카드 묶음에서 원하는 1장을 선택할 수 있어요
        </p>

        <section className="mt-7">
          <h2 className="text-[13px] font-bold text-ink">상대가 원하는 카드</h2>
          <div className="mt-3 w-[118px] rounded-2xl bg-white p-2.5 shadow-[0_4px_16px_rgba(0,0,0,0.08)]">
            <GoodsFace item={itemById(incoming.wantItemId)} size="md" />
            <p className="mt-2 text-center text-[12px] font-bold text-ink">
              {itemById(incoming.wantItemId).name}
            </p>
            <p className="text-center text-[11px] text-neutral-400">
              {itemById(incoming.wantItemId).nameKo}
            </p>
          </div>
        </section>

        <section className="mt-8">
          <h2 className="text-[13px] font-bold text-ink">상대의 카드 묶음 (1장 선택)</h2>
          <motion.div
            variants={staggerParent}
            initial="hidden"
            animate="show"
            className="mt-3 grid grid-cols-3 gap-3"
          >
            {incoming.offeredItemIds.map((id) => (
              <motion.div key={id} variants={staggerChild}>
                <GoodsCard
                  item={itemById(id)}
                  selected={chosen === id}
                  onClick={() => setPicked(id)}
                />
              </motion.div>
            ))}
          </motion.div>
        </section>
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button
          disabled={!chosen}
          onClick={() => {
            if (!chosen) return
            dispatch({ type: 'accept-incoming', chosenItemId: chosen })
            navigate('/match')
          }}
        >
          이 카드로 진행하기
        </Button>
        <TextButton onClick={() => setRejectOpen(true)}>거절하기</TextButton>
      </div>

      <Dialog
        open={rejectOpen}
        title="교환 요청을 거절할까요?"
        description="거절하면 상대에게 알림이 가고 이 요청은 사라져요."
        cancelLabel="아니요"
        confirmLabel="거절할게요"
        onCancel={() => setRejectOpen(false)}
        onConfirm={() => {
          setRejectOpen(false)
          dispatch({ type: 'reject-incoming' })
          navigate('/home')
        }}
      />
    </div>
  )
}
