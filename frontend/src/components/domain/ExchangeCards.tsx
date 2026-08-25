import { motion } from 'motion/react'
import type { ReactNode } from 'react'

import { ItemCard } from '@/components/domain/GoodsCard'
import { cn } from '@/lib/cn'
import { springSnap } from '@/lib/motion'
import { itemById, MY_IDENTITY } from '@/mocks/data'
import type { ExchangePair } from '@/store/matching'

/**
 * 교환 결과를 보여주는 카드. 매칭 결과 화면과 약속 완료 화면이 같은 모양을 쓴다.
 * 카드 안에는 굵은 아이템 이름만 두고, 이동 상태 같은 부가 표시는 카드 아래 badge 로 붙인다.
 */
export function ExchangeCard({
  itemId,
  label,
  badge,
  compact = false,
}: {
  itemId: string
  label?: string
  badge?: ReactNode
  /** 카드가 여러 장 쌓이는 자리. 화면을 넘기지 않게 줄인다. */
  compact?: boolean
}) {
  const item = itemById(itemId)
  return (
    <div className="text-center">
      {label && <p className="mb-3 text-[12px] font-bold text-ink">{label}</p>}
      <motion.div
        initial={{ opacity: 0, y: 16, scale: 0.94 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={springSnap}
        className={cn(compact ? 'w-[124px] md:w-[112px]' : 'w-[124px] md:w-[140px]')}
      >
        <ItemCard
          item={item}
          size={compact ? 'md' : 'lg'}
          className="shadow-[0_6px_22px_rgba(0,0,0,0.10)]"
        />
      </motion.div>
      {badge && <div className="mt-2.5">{badge}</div>}
    </div>
  )
}

/** 이동 상태 알약. 도착한 사람만 브랜드색으로 칠해서 멀리서도 갈린다. */
export function MoveStatusBadge({ arrived }: { arrived: boolean }) {
  return (
    <span
      className={cn(
        'inline-block rounded-full px-3 py-1 text-[11px] font-bold text-white',
        arrived ? 'bg-brand' : 'bg-neutral-400',
      )}
    >
      {arrived ? '도착' : '이동중..'}
    </span>
  )
}

/**
 * 1:1 교환. 한 쌍이면 좌우로 나란히 두고, 여러 장을 한 번에 바꾸면 시안의
 * `8_v1. 여러개 거래시` 처럼 두 줄로 세워서 목록이 길어지면 굴러가게 한다.
 */
export function OneToOneView({
  pairs,
  giveBadge,
  receiveBadge,
}: {
  pairs: ExchangePair[]
  giveBadge?: ReactNode
  receiveBadge?: ReactNode
}) {
  if (pairs.length <= 1) {
    return (
      <div className="mt-10 flex items-start justify-center gap-3 md:mt-0">
        <ExchangeCard itemId={pairs[0].giveItemId} label="내가 주는 카드" badge={giveBadge} />
        <span className="anim-breathe mt-[86px] text-[20px] text-brand">⇄</span>
        <ExchangeCard itemId={pairs[0].receiveItemId} label="내가 받는 카드" badge={receiveBadge} />
      </div>
    )
  }

  return (
    <div className="mt-6 md:mt-0">
      <div className="flex justify-center gap-3">
        <p className="w-[124px] text-center text-[12px] font-bold text-ink">내가 주는 카드</p>
        <span className="w-[20px]" />
        <p className="w-[124px] text-center text-[12px] font-bold text-ink">내가 받는 카드</p>
      </div>

      {/* 목록이 길어지면 이 칸만 굴러간다. 화면 전체가 늘어나면 CTA 가 밀려 내려간다. */}
      <div className="mt-3 max-h-[46vh] overflow-y-auto pb-2 no-scrollbar">
        {pairs.map((pair, i) => (
          <div
            key={`${pair.giveItemId}-${pair.receiveItemId}-${i}`}
            className="flex justify-center gap-3 pt-3 first:pt-0"
          >
            <ExchangeCard itemId={pair.giveItemId} badge={i === 0 ? giveBadge : undefined} />
            <span className="flex w-[20px] items-center justify-center text-[18px] text-brand">
              {i === 0 ? <span className="anim-breathe">⇄</span> : null}
            </span>
            <ExchangeCard itemId={pair.receiveItemId} badge={i === 0 ? receiveBadge : undefined} />
          </div>
        ))}
      </div>
    </div>
  )
}

export function ThreeWayView({
  myItemId,
  giverNickname,
  giverItemId,
  receiverNickname,
  receiverItemId,
  myBadge,
  giverBadge,
  receiverBadge,
}: {
  myItemId: string
  giverNickname: string
  giverItemId: string
  receiverNickname: string
  receiverItemId: string
  myBadge?: ReactNode
  giverBadge?: ReactNode
  receiverBadge?: ReactNode
}) {
  return (
    <div className="mt-8 md:mt-0">
      <div className="flex justify-center">
        <ExchangeCard
          compact
          itemId={myItemId}
          label={`나 (${MY_IDENTITY.fruit} ${MY_IDENTITY.number})`}
          badge={myBadge}
        />
      </div>

      <div className="mt-3 flex items-center justify-center gap-24 text-[18px] text-brand md:mt-1">
        <span className="anim-float-sm">↗</span>
        <span className="anim-float-sm" style={{ animationDelay: '0.8s' }}>
          ↘
        </span>
      </div>

      <div className="mt-3 flex items-start justify-center gap-3 md:mt-1">
        <ExchangeCard compact itemId={giverItemId} label={giverNickname} badge={giverBadge} />
        <span className="anim-nudge-x-back mt-16 text-[18px] text-brand">←</span>
        <ExchangeCard
          compact
          itemId={receiverItemId}
          label={receiverNickname}
          badge={receiverBadge}
        />
      </div>
    </div>
  )
}
