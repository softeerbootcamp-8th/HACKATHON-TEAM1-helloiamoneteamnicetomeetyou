import { motion } from 'motion/react'
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { RejectDialog } from '@/components/domain/ConfirmDialogs'
import { useCatalog } from '@/features/catalog/useCatalog'
import type { ReceivedPoke } from '@/features/poke/api'
import { usePoke } from '@/features/poke/usePoke'
import { EmptyState } from '@/components/domain/EmptyState'
import { GoodsCard, ItemCard } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { staggerChild, staggerParent } from '@/lib/motion'
import { itemById, type Item } from '@/mocks/data'
import { useLastDefined } from '@/lib/useLastDefined'
import { useStore } from '@/store/useStore'

/**
 * 받은 교환 요청. 상대의 묶음에서 한 장을 고른다.
 *
 * 서버에 실제로 온 찔러보기가 있으면 그것을 쓰고, 없으면 목업 흐름으로 떨어진다.
 * 매칭 알고리즘(#20)이 들어와 목업이 걷히면 아래쪽 분기가 사라진다.
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
  const requested = mockItemOf?.(poke.requestedItem.id)
  const chosen = picked ?? poke.offeredItems[0]?.id ?? null

  const submitAccept = async () => {
    if (chosen === null || submitting) return
    setSubmitting(true)
    try {
      await onAccept(poke.pokeId, chosen)
      navigate('/match')
    } catch {
      // 사유는 PokeProvider 가 들고 있고 홈 화면이 띄운다. 여기서는 화면을 붙잡아 둔다.
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
      <div className="flex-1 overflow-y-auto px-6 pt-6 no-scrollbar">
        <h1 className="text-[24px] font-extrabold tracking-[-0.02em] text-ink">
          상대가 교환을 요청했어요
        </h1>
        <p className="mt-2 text-[13px] text-neutral-400">
          상대의 카드 묶음에서 원하는 1장을 선택할 수 있어요
        </p>

        <section className="mt-7">
          <h2 className="text-[13px] font-bold text-ink">상대가 원하는 카드</h2>
          {requested ? (
            <ItemCard item={requested} size="md" className="mt-3 w-[118px]" />
          ) : (
            <p className="mt-3 w-[118px] rounded-2xl bg-white py-8 text-center text-[12px] font-bold text-ink ring-1 ring-line">
              {poke.requestedItem.name}
            </p>
          )}
        </section>

        <section className="mt-8">
          <h2 className="text-[13px] font-bold text-ink">상대의 카드 묶음 (1장 선택)</h2>
          <motion.div
            variants={staggerParent}
            initial="hidden"
            animate="show"
            className="mt-3 grid grid-cols-3 gap-3"
          >
            {poke.offeredItems.map((server) => {
              const item: Item | undefined = mockItemOf?.(server.id)
              return (
                <motion.div key={server.id} variants={staggerChild}>
                  {item ? (
                    <GoodsCard
                      item={item}
                      selected={chosen === server.id}
                      onClick={() => setPicked(server.id)}
                    />
                  ) : (
                    // 목업에 짝이 없는 카드다. 조용히 버리면 상대가 3장 냈는데 2장만 보인다.
                    <button
                      type="button"
                      onClick={() => setPicked(server.id)}
                      aria-pressed={chosen === server.id}
                      className={`w-full rounded-2xl bg-white p-2.5 text-center ${
                        chosen === server.id ? 'ring-2 ring-ink' : 'ring-1 ring-line'
                      }`}
                    >
                      <span className="flex h-[74px] items-center justify-center rounded-xl bg-tile text-[12px] font-bold text-ink">
                        {server.name}
                      </span>
                      <p className="mt-2 text-[12px] font-bold text-ink">{server.name}</p>
                    </button>
                  )}
                </motion.div>
              )
            })}
          </motion.div>
        </section>
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button disabled={chosen === null || submitting} onClick={submitAccept}>
          {submitting ? '보내는 중...' : '이 카드로 진행하기'}
        </Button>
        <TextButton onClick={() => setRejectOpen(true)}>거절하기</TextButton>
      </div>

      <RejectDialog open={rejectOpen} onKeep={() => setRejectOpen(false)} onReject={submitReject} />
    </div>
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
  useEffect(() => {
    if (demo && !incoming) dispatch({ type: 'seed-demo', kind: 'incoming' })
  }, [demo, incoming, dispatch])

  // 아직 아무것도 안 골랐으면 첫 장을 고른 것으로 본다. effect 로 setState 하지 않고
  // 렌더할 때 계산하면 상태가 하나 줄어든다.
  const chosen = picked ?? incoming?.offeredItemIds[0] ?? null

  if (!incoming) {
    return (
      <EmptyState
        title="받은 교환 요청이 없어요"
        description={'요청이 오면 알림으로\n바로 알려 드릴게요.'}
        onAction={() => navigate('/home')}
      />
    )
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <div className="flex-1 overflow-y-auto px-6 pt-6 no-scrollbar">
        <h1 className="text-[24px] font-extrabold tracking-[-0.02em] text-ink">
          상대가 교환을 요청했어요
        </h1>
        <p className="mt-2 text-[13px] text-neutral-400">
          상대의 카드 묶음에서 원하는 1장을 선택할 수 있어요
        </p>

        <section className="mt-7">
          <h2 className="text-[13px] font-bold text-ink">상대가 원하는 카드</h2>
          <ItemCard item={itemById(incoming.wantItemId)} size="md" className="mt-3 w-[118px]" />
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

      <RejectDialog
        open={rejectOpen}
        onKeep={() => setRejectOpen(false)}
        onReject={() => {
          setRejectOpen(false)
          dispatch({ type: 'reject-incoming' })
          navigate('/home')
        }}
      />
    </div>
  )
}
