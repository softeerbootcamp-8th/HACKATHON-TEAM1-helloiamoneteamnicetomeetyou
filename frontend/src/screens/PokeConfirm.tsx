import { motion } from 'motion/react'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { CardStack } from '@/components/domain/CardStack'
import { GoodsFace } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import { useCatalog } from '@/features/catalog/useCatalog'
import type { BoothHaveItem } from '@/features/poke/api'
import { usePoke } from '@/features/poke/usePoke'
import { springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { ALL_WAITING, itemById, type Item } from '@/mocks/data'
import { useStore } from '@/store/useStore'

/**
 * 찔러보기를 보내기 전 확인 화면.
 *
 * 주소의 `to` 로 두 갈래가 갈린다. 서버 사용자(UUID)면 실제로 보내고, 목업 사용자(`u3`)면
 * 목업 흐름으로 돈다. 레이더가 아직 목업 사용자를 세우고 있어서 둘 다 필요하다.
 */
export function PokeConfirm() {
  const [params] = useSearchParams()
  const { waiting, ready } = usePoke()
  const to = params.get('to') ?? ''

  // 나가는 중에는 주소가 이미 다음 화면 것이라, 처음 잡은 상대를 계속 들고 있는다.
  const target = useLastDefined(waiting.find((row) => String(row.haveItemId) === to))

  if (ready && target) {
    return <ServerPokeConfirm target={target} />
  }

  return <MockPokeConfirm />
}

/** 서버 사용자에게 실제로 보낸다. */
function ServerPokeConfirm({ target }: { target: BoothHaveItem }) {
  const navigate = useNavigate()
  const { state } = useStore()
  const { send } = usePoke()
  const { state: catalog } = useCatalog()
  const [submitting, setSubmitting] = useState(false)

  const mockItemOf = catalog.status === 'ready' ? catalog.mockItemOf : undefined
  const targetItem = mockItemOf?.(target.item.id)

  const topItemId = state.have[0]?.itemId ?? 'sf'
  const haveCount = state.have.reduce((sum, s) => sum + s.qty, 0)

  const submit = async () => {
    if (submitting) return
    setSubmitting(true)
    try {
      await send(target.ownerId, target.item.id)
      navigate('/home')
    } catch {
      // 사유는 PokeProvider 가 들고 있고 홈 화면이 띄운다. 여기서는 되돌아가지 않는다.
      setSubmitting(false)
    }
  }

  return (
    <Layout
      footer={
        <>
          <Button disabled={submitting} onClick={submit}>
            {submitting ? '보내는 중...' : '찔러보기 보내기'}
          </Button>
          <TextButton onClick={() => navigate('/home')}>취소</TextButton>
        </>
      }
    >
      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-ink">
        교환 요청을 보내시겠어요?
      </h1>
      <p className="mt-2 text-[13px] text-neutral-400">상대에게 교환 요청을 보냅니다.</p>

      <Exchange
        topItemId={topItemId}
        haveCount={haveCount}
        targetItem={targetItem}
        targetName={target.item.name}
      />

      <Notice />
    </Layout>
  )
}

/** 목업 흐름. 레이더에 뜬 가짜 사용자에게 보낸 것처럼 보여 준다. */
function MockPokeConfirm() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { state, dispatch } = useStore()

  const target = useLastDefined(ALL_WAITING.find((u) => u.id === (params.get('to') ?? '')))
  const topItemId = state.have[0]?.itemId ?? 'sf'
  const haveCount = state.have.reduce((sum, s) => sum + s.qty, 0)

  if (!target) {
    return (
      <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-8">
          <p className="text-[15px] text-neutral-500">상대를 찾을 수 없어요.</p>
          <Button onClick={() => navigate('/home')}>홈으로</Button>
        </div>
      </div>
    )
  }

  const send = () => {
    dispatch({ type: 'send-poke', targetUserId: target.id })
    navigate('/home')
  }

  return (
    <Layout
      footer={
        <>
          <Button onClick={send}>찔러보기 보내기</Button>
          <TextButton onClick={() => navigate('/home')}>취소</TextButton>
        </>
      }
    >
      <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-ink">
        교환 요청을 보내시겠어요?
      </h1>
      <p className="mt-2 text-[13px] text-neutral-400">상대에게 교환 요청을 보냅니다.</p>

      <Exchange
        topItemId={topItemId}
        haveCount={haveCount}
        targetItem={itemById(target.itemId)}
        targetName={itemById(target.itemId).name}
      />

      <Notice />
    </Layout>
  )
}

/**
 * 본문은 스크롤되고 버튼은 아래에 고정된다.
 *
 * 버튼을 스크롤 영역 안에 두면 내용이 길어질 때 화면 밖으로 밀려서 보낼 수가 없다.
 */
function Layout({ children, footer }: { children: React.ReactNode; footer: React.ReactNode }) {
  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <div className="no-scrollbar flex-1 overflow-y-auto px-6 pt-6">{children}</div>
      <div className="shrink-0 px-6 pt-4 pb-8">{footer}</div>
    </div>
  )
}

/** 내 묶음과 상대 카드를 화살표로 잇는다. 시안 225:23450 그대로다. */
function Exchange({
  topItemId,
  haveCount,
  targetItem,
  targetName,
}: {
  topItemId: string
  haveCount: number
  targetItem: Item | undefined
  targetName: string
}) {
  return (
    <div className="mt-10 flex items-center justify-center gap-5">
      <div className="text-center">
        <p className="mb-3 text-[12px] font-semibold text-neutral-400">내 카드 묶음</p>
        <CardStack topItemId={topItemId} count={haveCount} />
        {haveCount > 1 && <p className="mt-3 text-[12px] text-neutral-400">외 {haveCount - 1}장</p>}
      </div>

      <span className="anim-nudge-x text-[22px] text-neutral-300">→</span>

      <div className="text-center">
        <p className="mb-3 text-[12px] font-semibold text-neutral-400">상대 카드</p>
        <motion.div
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={springSnap}
          className="w-[112px] rounded-2xl bg-white p-2.5 shadow-[0_6px_20px_rgba(0,0,0,0.10)]"
        >
          {targetItem ? (
            <>
              <GoodsFace item={targetItem} size="md" />
              <p className="mt-2 text-center text-[12px] font-bold text-ink">{targetItem.name}</p>
              <p className="text-center text-[11px] text-neutral-400">{targetItem.nameKo}</p>
            </>
          ) : (
            // 서버에만 있는 카드다. 그림은 못 그려도 무엇을 요청하는지는 보여야 한다.
            <p className="py-8 text-center text-[12px] font-bold text-ink">{targetName}</p>
          )}
        </motion.div>
      </div>
    </div>
  )
}

function Notice() {
  return (
    <div className="mt-12 flex items-start gap-2.5 rounded-2xl bg-neutral-50 p-4">
      <span className="text-[14px] text-neutral-400">ⓘ</span>
      <p className="text-[13px] leading-[1.55] text-neutral-500">
        상대는 내 카드 묶음 중 한 장을 선택해 교환을 수락할 수 있어요.
      </p>
    </div>
  )
}
