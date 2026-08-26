import { motion } from 'motion/react'

import { CARD_SHELL, ItemCardBody } from '@/components/domain/GoodsCard'
import { cn } from '@/lib/cn'
import { springSnap } from '@/lib/motion'
import type { Item } from '@/features/catalog/api'

type Props = {
  /**
   * 끌어다 놓기의 표적이다. `hitTest` 가 `data-radar-user` 로 이 값을 읽어 누구 위에 있는지
   * 판단하고, 그대로 찔러보기 대상이 된다.
   *
   * 보유 등록 줄 id(`haveItemId`)를 문자열로 넣는다. DOM 데이터 속성으로 오가는 값이라
   * 이 컴포넌트는 문자열로만 다룬다.
   */
  targetId: string
  /** 이 사람이 내놓은 카드. 부스 목록에 없는 카드면 없다. */
  item: Item | undefined
  /** 읽어 주는 이름. 사람 이름이 아직 없어 카드 이름을 쓴다. */
  label: string
  /** 지금 내 카드 묶음이 이 사람 위에 올라와 있는지 */
  hovered: boolean
  /** 이미 찔러보기를 보내고 답을 기다리는 중인지 */
  pending: boolean
  /** 방금 이 사람에게 카드를 놓았는지. 고리가 한 번 터진다. */
  burst?: boolean
  /** 카드를 눌렀을 때. 끌어놓기 말고 그냥 눌러서도 찔러볼 수 있어야 한다. */
  onSelect?: () => void
  index: number
}

/** 레이더 위에 서 있는 상대 한 명. */
export function RadarUser({
  targetId,
  item,
  label,
  hovered,
  pending,
  burst = false,
  onSelect,
  index,
}: Props) {
  return (
    <motion.div
      data-radar-user={targetId}
      initial={{ opacity: 0, scale: 0.6 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ ...springSnap, delay: 0.08 * index }}
      className="relative w-[90px] md:w-[104px]"
    >
      {burst && (
        <span aria-hidden className="pointer-events-none absolute inset-0 z-20">
          {/* 고리 두 개를 살짝 어긋나게 띄워서 물결처럼 퍼지게 한다. */}
          {[0, 0.28].map((delay) => (
            <span
              key={delay}
              className="anim-ripple absolute top-1/2 left-1/2 aspect-square w-full rounded-full border-[3px] border-brand"
              style={{ animationDelay: `${delay}s` }}
            />
          ))}
        </span>
      )}

      {hovered && (
        <motion.span
          initial={{ opacity: 0, y: 6, scale: 0.8 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          transition={springSnap}
          className="absolute -top-8 left-1/2 z-10 -translate-x-1/2 rounded-lg bg-ink px-2.5 py-1 text-[11px] font-bold whitespace-nowrap text-white md:text-[12px]"
        >
          찔러보기
        </motion.span>
      )}

      {/* 떠다니는 것은 CSS 가 맡고, 손가락이 올라왔을 때의 확대만 motion 이 맡는다.
          둘을 한 요소에 겹치면 transform 이 서로를 덮어써서 하나가 죽는다. */}
      <motion.button
        type="button"
        onClick={onSelect}
        disabled={pending || !onSelect}
        aria-label={`${label}에게 찔러보기`}
        whileTap={pending ? undefined : { scale: 0.94 }}
        animate={{ scale: hovered ? 1.08 : 1 }}
        transition={springSnap}
        style={{ animationDelay: `${index * 0.4}s` }}
        className={cn(
          CARD_SHELL,
          'block w-full p-2 shadow-[0_4px_16px_rgba(0,0,0,0.10)]',
          !hovered && !pending && 'anim-float-sm',
          hovered && 'ring-2 ring-ink',
          pending && 'opacity-45 grayscale',
        )}
      >
        {item ? (
          <ItemCardBody item={item} size="sm" />
        ) : (
          /*
            카드 그림을 못 그린다고 통째로 빼면 레이더가 비어 보인다. 전체리스트가 이름만
            적힌 타일로 자리를 지키는 것과 같은 처리다. 높이는 `ItemCardBody` 와 맞춰 둔다.
          */
          <>
            <span
              aria-hidden
              className="flex h-[70px] w-full items-center justify-center rounded-xl bg-tile ring-1 ring-line md:h-[78px]"
            />
            <p className="mt-2 flex h-[26px] items-center justify-center text-center text-[10px] leading-tight font-bold text-ink">
              <span className="line-clamp-2">{label}</span>
            </p>
          </>
        )}

        {pending && (
          <span className="absolute inset-0 flex items-center justify-center text-[15px] font-bold text-neutral-500">
            ···
          </span>
        )}
        {hovered && (
          <span className="absolute -top-1.5 -right-1.5 flex size-5 items-center justify-center rounded-full bg-ink text-[10px] text-white">
            ✓
          </span>
        )}
      </motion.button>
    </motion.div>
  )
}
