import { motion } from 'motion/react'
import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router'

import { BoothPicker } from '@/components/domain/BoothPicker'
import { CARD_SHELL } from '@/components/domain/GoodsCard'
import { RadarRings } from '@/components/domain/Radar'
import { Button } from '@/components/ui/Button'
import { useCatalog } from '@/features/catalog/useCatalog'
import { cn } from '@/lib/cn'
import { springPage, springSnap } from '@/lib/motion'
import { useStore } from '@/store/useStore'

/**
 * 서버에서 부스 이름을 받기 전까지 세워 둘 이름.
 *
 * 로딩 중이나 서버가 끊겼을 때 이 자리를 비우면 첫 화면 맨 위가 잠깐 무너진다.
 */
const FALLBACK_BOOTH_NAME = '현대자동차 팝업'
const FALLBACK_BOOTH_DESCRIPTION = '포토카드 교환존'

export function Onboarding() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const { state: catalog, booths, booth, selectBooth } = useCatalog()
  const [pickerOpen, setPickerOpen] = useState(false)

  const boothName = booth?.name ?? FALLBACK_BOOTH_NAME

  const cardCount = catalog.status === 'ready' ? catalog.items.length : 0

  /**
   * 이 기기가 전에 홈까지 가 본 적이 있으면 온보딩을 다시 보여주지 않는다.
   *
   * 홈에서도 카드는 얼마든지 고칠 수 있어서, 매번 부스 고르기부터 다시 시키는 것은 방문객을
   * 세워 두고 필요 없는 화면을 하나 더 거치게 하는 것이다. `/` 로 주소를 바로 쳐도 마찬가지다.
   */
  if (state.setupDone) {
    return <Navigate to="/home" replace />
  }

  /**
   * 부스를 바꾸는 곳은 여기 하나뿐이다.
   *
   * 예전에는 `booths.length > 1 && !state.setupDone` 이었는데, 운영 DB 에 부스가 하나라
   * 이름만 나오고 누를 곳이 없었다. 어드민에서 부스를 새로 만들어도 앱에서는 그 사실을
   * 알 방법이 없어서, 부스가 하나여도 열리게 둔다. 목록을 아직 못 받았을 때만 글자로 둔다.
   */
  const canSwitch = booths.length > 0

  /**
   * 다른 부스로 옮기면 등록해 둔 것을 비운다.
   *
   * `/` 는 주소로 바로 들어올 수 있어서 카드를 등록한 뒤에도 돌아올 길이 있다. 그대로 부스만
   * 바꾸면 화면에는 앞 부스에서 고른 카드가 남아 있는데 서버의 새 부스에는 아무것도 없어서,
   * 매칭이 영영 안 되는 상태로 시연을 하게 된다. 같은 부스를 다시 고른 것은 그냥 닫는다.
   */
  const pickBooth = (boothId: number) => {
    if (boothId !== booth?.id && state.setupDone) dispatch({ type: 'reset' })
    selectBooth(boothId)
    setPickerOpen(false)
  }

  const start = () => {
    dispatch({ type: 'onboarded' })
    navigate('/have')
  }

  return (
    <div className="flex h-full flex-col md:mx-auto md:w-full md:max-w-[900px] md:px-10">
      <div className="flex-1 overflow-y-auto px-6 pt-4 no-scrollbar">
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springPage, delay: 0.05 }}
        >
          {canSwitch ? (
            <motion.button
              type="button"
              onClick={() => setPickerOpen(true)}
              whileTap={{ scale: 0.97 }}
              transition={springSnap}
              className="-mx-1 flex max-w-full items-center gap-2 rounded-lg px-1 text-[15px] font-bold text-ink"
            >
              <span aria-hidden className="size-2 shrink-0 rounded-full bg-brand" />
              <span className="truncate">{boothName}</span>
              <ChevronDown />
              <span className="sr-only">부스 바꾸기</span>
            </motion.button>
          ) : (
            <p className="flex max-w-full items-center gap-2 text-[15px] font-bold text-ink">
              <span aria-hidden className="size-2 shrink-0 rounded-full bg-brand" />
              <span className="truncate">{boothName}</span>
            </p>
          )}
          {/* 부스를 바꾸면 이 줄도 같이 바뀌어야 한다. 이름만 바뀌고 설명이 남으면 안 옮겨진 것처럼 보인다. */}
          <p className="mt-0.5 pl-4 text-[11px] text-neutral-400">
            {booth?.description ?? FALLBACK_BOOTH_DESCRIPTION}
          </p>
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springPage, delay: 0.12 }}
          className="mt-8 text-[clamp(26px,7.4vw,30px)] leading-[1.32] font-extrabold tracking-[-0.02em] text-ink"
        >
          원하는 굿즈를 알려주세요,
          <br />
          상대는 NearLy가 찾아줄게요
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springPage, delay: 0.19 }}
          className="mt-4 text-[13px] leading-[1.6] text-neutral-400"
        >
          떠나기 전에, 원하는
          <br />
          {boothName} 굿즈로 바꿔가요
        </motion.p>

        <div className="relative mt-6 flex h-[280px] items-center justify-center">
          <RadarRings />
          <motion.div
            initial={{ opacity: 0, scale: 0.8, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ ...springSnap, delay: 0.24 }}
          >
            <div className="anim-float">
              <BrandCard />
            </div>
          </motion.div>
        </div>

        <motion.p
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springPage, delay: 0.34 }}
          className="mx-auto w-fit rounded-full bg-neutral-100 px-4 py-2 text-[12px] font-medium text-neutral-500"
        >
          지금 {cardCount}종의 카드를 교환 중! 🎪
        </motion.p>
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button variant="brand" onClick={start}>
          교환하러 출발!
        </Button>
      </div>

      <BoothPicker
        open={pickerOpen}
        booths={booths}
        selectedId={booth?.id ?? null}
        onSelect={pickBooth}
        onDismiss={() => setPickerOpen(false)}
      />
    </div>
  )
}

/**
 * 첫 화면 한가운데 뜨는 NearLy 카드 (시안 159:2024~159:2032).
 *
 * <b>부스의 실제 굿즈를 세우지 않는다.</b> 전에는 목록의 첫 카드를 세웠는데, 아직 아무것도
 * 고르지 않은 사람에게 특정 굿즈를 들이미는 그림이었고 부스마다 첫 화면이 달라 보였다.
 * 시안은 이 자리를 서비스 카드 한 장으로 두고 있다.
 *
 * 뒤에 흰 카드 한 장이 어긋나게 겹치고, 앞 카드 안에 그라데이션 썸네일이 살짝 기울어 앉는다.
 * 카드 겉모양은 다른 화면과 같은 `CARD_SHELL` 을 쓴다.
 */
function BrandCard() {
  return (
    <div className="relative w-[150px]">
      <div
        aria-hidden
        className={cn(CARD_SHELL, 'absolute inset-0 rotate-5 shadow-[0_8px_20px_rgba(0,0,0,0.08)]')}
      />

      <div
        className={cn(
          CARD_SHELL,
          'relative px-5 pt-[22px] pb-3 shadow-[0_8px_20px_rgba(0,0,0,0.08)]',
        )}
      >
        {/* 썸네일이 기울어 앉는 자리. 밑에 깔린 회색 타일이 모서리로 비어져 나온다. */}
        <div className="relative flex h-[135px] items-center justify-center rounded-[14px] bg-tile">
          <span
            aria-hidden
            className="absolute size-[64%] rounded-full blur-[11px]"
            style={{ background: 'radial-gradient(circle, #2cb3edd9 0%, #2cb3ed00 74%)' }}
          />
          <div className="card-face relative flex size-full -rotate-5 items-center justify-center rounded-[14px]">
            {/*
              심볼은 시안에서 크게 기울어 있다. 원본 svg 가 preserveAspectRatio="none" 이라
              폭과 높이를 둘 다 못 박아야 모양이 눌리지 않는다.
            */}
            <img
              src="/nearly-mark.svg"
              alt=""
              aria-hidden
              draggable={false}
              className="h-[38px] w-[46px] rotate-[80deg] select-none"
            />
          </div>
        </div>

        <p className="mt-2.5 -rotate-5 text-center text-[16px] font-bold text-[#4e5146]">NearLy</p>
      </div>
    </div>
  )
}

/** 누를 수 있는 줄이라는 표시. 이 화면에서만 써서 여기 둔다. */
function ChevronDown() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="size-3.5 shrink-0 text-neutral-400" aria-hidden>
      <path
        d="m6 9 6 6 6-6"
        stroke="currentColor"
        strokeWidth="2.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}
