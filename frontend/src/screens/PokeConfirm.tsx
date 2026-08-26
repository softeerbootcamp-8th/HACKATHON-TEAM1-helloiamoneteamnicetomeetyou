import { motion } from 'motion/react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { CardStack } from '@/components/domain/CardStack'
import { EmptyState } from '@/components/domain/EmptyState'
import { ItemCard } from '@/components/domain/GoodsCard'
import { Button, TextButton } from '@/components/ui/Button'
import type { Item } from '@/features/catalog/api'
import { unknownItem } from '@/features/catalog/useItem'
import { useCatalog } from '@/features/catalog/useCatalog'
import type { BoothHaveItem } from '@/features/poke/api'
import { usePoke } from '@/features/poke/usePoke'
import { messageOf } from '@/lib/api'
import { springSnap } from '@/lib/motion'
import { useLastDefined } from '@/lib/useLastDefined'
import { byPresence } from '@/store/top-card'
import { useStore } from '@/store/useStore'

/**
 * 찔러보기를 보내기 전 확인 화면.
 *
 * 주소의 `to` 는 부스 목록의 보유 등록 줄 id 다. 그 줄을 못 찾으면 목록이 갱신되면서
 * 사라진 것이라 고르는 자리로 돌려보낸다.
 */
export function PokeConfirm() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { waiting } = usePoke()
  const to = params.get('to') ?? ''

  // 나가는 중에는 목록이 이미 바뀌어 있어서, 처음 잡은 상대를 계속 들고 있는다.
  const target = useLastDefined(waiting.find((row) => String(row.haveItemId) === to))

  if (!target) {
    return (
      <EmptyState
        title="상대를 찾을 수 없어요"
        description={'교환 대기존에서\n다시 골라 주세요.'}
        onAction={() => navigate('/home')}
      />
    )
  }

  return <ConfirmView target={target} />
}

/** 내가 내주는 카드 한 종류. 서버 등록 한 줄에 그릴 카드를 붙인 것이다. */
type OfferedCard = { item: Item; qty: number }

/** 상대에게 실제로 보낸다. */
function ConfirmView({ target }: { target: BoothHaveItem }) {
  const navigate = useNavigate()
  const { dispatch } = useStore()
  const { send, myOfferable, loaded, refresh, clearError } = usePoke()
  const { state: catalog } = useCatalog()
  const [submitting, setSubmitting] = useState(false)
  /*
    보내지 못한 이유. 홈 화면의 토스트에 맡기면 여기 남아 있는 동안에는 아무것도 안 뜬다.
    실패하면 되돌아가지 않는 화면이라 그 자리가 영영 오지 않는다.
  */
  const [failed, setFailed] = useState<string | null>(null)

  /*
    들어올 때 묶음을 다시 읽는다.

    이 화면이 보여주는 것은 "내가 고른 카드" 가 아니라 <b>상대가 실제로 고르게 될 카드</b>다.
    교환 대기장에 머무는 동안 그 묶음은 얼마든지 달라진다 — 다른 교환이 잡혀 예약되거나, 다른
    기기에서 등록을 고쳤거나. 여기서 한 번 맞춰 두고, 머무는 동안의 변화는 `PokeProvider` 가
    실시간 알림을 받아 갱신한다.
  */
  useEffect(() => {
    refresh()
  }, [refresh])

  const targetItem = catalog.status === 'ready' ? catalog.itemById(target.item.id) : undefined

  /*
    부스를 가리지 않는다. 서버가 묶음을 고를 때 부스를 안 보기 때문에, 여기서 지금 부스 카드만
    남기면 상대는 화면에 없던 카드까지 고를 수 있게 된다. 이 부스 목록에 없는 카드는 그림을 못
    그릴 뿐이라 자리표시자로 세우고, 있다는 사실 자체는 감추지 않는다.
  */
  const offered: OfferedCard[] = useMemo(() => {
    const itemById = catalog.status === 'ready' ? catalog.itemById : () => undefined
    return myOfferable
      .map((row) => ({ item: itemById(row.itemId) ?? unknownItem(row.itemId), qty: row.quantity }))
      .sort((a, b) =>
        byPresence({ itemId: a.item.id, qty: a.qty }, { itemId: b.item.id, qty: b.qty }),
      )
  }, [myOfferable, catalog])

  const kinds = offered.length
  const total = offered.reduce((sum, card) => sum + card.qty, 0)
  // 묶음이 비면 서버가 4014 로 막는다. 눌러서 실패하게 두지 않고 미리 가른다.
  const canSend = loaded && kinds > 0

  /**
   * 보내고 교환 대기장으로 돌아간다.
   *
   * 돌아간 자리에서 토스트로 무엇을 보냈는지 알린다 (시안 `토스트 정리` 204:5148 의
   * "찔러보기 제안한 경우"). 화면이 바뀌면서 요청이 나갔다는 흔적이 사라지기 때문에,
   * 이 한 줄이 없으면 아무 일도 없던 것처럼 보인다.
   */
  const submit = async () => {
    if (submitting || !canSend) return
    setSubmitting(true)
    setFailed(null)
    try {
      await send(target.ownerId, target.item.id)
      dispatch({
        type: 'toast',
        message: `${target.item.name} 찔러보기를 보냈어요\n답변 기다리는 중..`,
      })
      navigate('/home')
    } catch (err) {
      // 여기서 띄우므로 provider 가 들고 있는 사유는 지운다. 실패한 김에 묶음도 다시
      // 읽히므로(`run` 의 catch), 화면은 갱신된 값으로 다시 그려진다.
      setFailed(messageOf(err))
      clearError()
      setSubmitting(false)
    }
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <div className="flex-1 overflow-y-auto px-6 pt-6 no-scrollbar">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-ink">
          이 상대에게 찔러볼까요?
        </h1>

        <div className="mt-12 flex items-start justify-center gap-5">
          <div className="text-center">
            <p className="mb-3 text-[12px] font-semibold text-neutral-400">내 카드</p>
            <CardStack topItemId={offered[0]?.item.id ?? null} count={total} />
            {kinds > 1 && <p className="mt-3 text-[12px] text-neutral-400">외 {kinds - 1}종</p>}
          </div>

          <span className="anim-nudge-x mt-[100px] text-[22px] text-brand">→</span>

          <div className="text-center">
            <p className="mb-3 text-[12px] font-semibold text-neutral-400">상대 카드</p>
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={springSnap}
              className="w-[112px]"
            >
              {targetItem ? (
                <ItemCard
                  item={targetItem}
                  size="md"
                  className="shadow-[0_6px_20px_rgba(0,0,0,0.10)]"
                />
              ) : (
                // 목록을 아직 못 받았거나 이 부스에 없는 카드다. 그림은 못 그려도 무엇을
                // 요청하는지는 보여야 한다.
                <p className="rounded-2xl bg-white py-8 text-center text-[12px] font-bold text-ink ring-1 ring-line">
                  {target.item.name}
                </p>
              )}
            </motion.div>
          </div>
        </div>

        <MyBundleNotice loaded={loaded} offered={offered} />
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        {failed && (
          <p className="mb-2 text-center text-[12px] text-rose-500" role="alert">
            {failed}
          </p>
        )}
        {loaded && kinds === 0 ? (
          // 죽은 버튼을 남기지 않는다. 보낼 수 없는 이유가 "내놓은 카드가 없다" 하나뿐이라,
          // 그 자리를 고치러 가는 길을 버튼에 그대로 둔다.
          <Button onClick={() => navigate('/have')}>내놓을 카드 고르러 가기</Button>
        ) : (
          <Button disabled={submitting || !canSend} onClick={submit}>
            {submitting ? '보내는 중...' : '한 번 찔러보기'}
          </Button>
        )}
        <TextButton onClick={() => navigate('/home')}>취소</TextButton>
      </div>
    </div>
  )
}

/**
 * 상대가 무엇 중에서 고르는지 그대로 적는다.
 *
 * 카드 묶음은 맨 위 한 장만 보이기 때문에, 이름을 적어 주지 않으면 나머지가 무엇인지 알 길이
 * 없다. 편집 화면에서 고른 것과 여기 적힌 것이 다르면 등록이 아직 안 끝난 것이고, 그것이
 * 보이는 편이 낫다.
 */
function MyBundleNotice({ loaded, offered }: { loaded: boolean; offered: OfferedCard[] }) {
  const message = !loaded
    ? '내 카드를 확인하는 중이에요'
    : offered.length === 0
      ? '지금 내놓은 카드가 없어서 찔러볼 수 없어요'
      : '상대가 이 중 한 장을 골라 수락해요'

  return (
    <div className="mt-12 flex items-start gap-2.5 rounded-2xl bg-neutral-50 p-4">
      <span className="text-[14px] text-neutral-400">ⓘ</span>
      <div>
        <p className="text-[13px] leading-[1.55] text-neutral-500">{message}</p>
        {offered.length > 0 && (
          <p className="mt-1.5 text-[13px] leading-[1.55] font-bold text-ink">
            {offered
              .map((card) => (card.qty > 1 ? `${card.item.name} ${card.qty}장` : card.item.name))
              .join(' · ')}
          </p>
        )}
      </div>
    </div>
  )
}
