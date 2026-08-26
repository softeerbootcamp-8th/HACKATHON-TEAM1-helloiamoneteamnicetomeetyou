import { motion } from 'motion/react'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { BoothPicker } from '@/components/domain/BoothPicker'
import { CardStack } from '@/components/domain/CardStack'
import { RadarRings } from '@/components/domain/Radar'
import { Button } from '@/components/ui/Button'
import { useCatalog } from '@/features/catalog/useCatalog'
import { springPage, springSnap } from '@/lib/motion'
import { ALL_WAITING } from '@/mocks/data'
import { useStore } from '@/store/useStore'

/**
 * 서버에서 부스 이름을 받기 전까지 세워 둘 이름.
 *
 * 로딩 중이나 서버가 끊겼을 때 이 자리를 비우면 첫 화면 맨 위가 잠깐 무너진다. 이 앱은
 * 서버가 없어도 목업으로 도는 것이 전제라, 대표 부스 이름을 그대로 세워 둔다.
 */
const FALLBACK_BOOTH_NAME = '현대자동차 팝업'
const FALLBACK_BOOTH_DESCRIPTION = '자동차 포토카드 교환'

export function Onboarding() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const { booths, booth, selectBooth } = useCatalog()
  const [pickerOpen, setPickerOpen] = useState(false)

  const boothName = booth?.name ?? FALLBACK_BOOTH_NAME

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
          className="mt-8 text-[30px] leading-[1.32] font-extrabold tracking-[-0.02em] text-ink"
        >
          내 굿즈를 올리면
          <br />
          교환 상대를 찾아드려요
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springPage, delay: 0.19 }}
          className="mt-4 text-[13px] leading-[1.6] text-neutral-400"
        >
          현장에서 쉽고 빠르게
          <br />
          {boothName} 굿즈를 교환하세요
        </motion.p>

        <div className="relative mt-6 flex h-[280px] items-center justify-center">
          <RadarRings />
          <motion.div
            initial={{ opacity: 0, scale: 0.8, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ ...springSnap, delay: 0.24 }}
          >
            <div className="anim-float">
              <CardStack topItemId="avn" count={3} className="w-[168px]" />
            </div>
          </motion.div>
        </div>

        <motion.p
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springPage, delay: 0.34 }}
          className="mx-auto w-fit rounded-full bg-neutral-100 px-4 py-2 text-[12px] font-medium text-neutral-500"
        >
          지금 {ALL_WAITING.length + 6}명이 교환 중이에요
        </motion.p>
      </div>

      <div className="shrink-0 px-6 pt-4 pb-8">
        <Button variant="brand" onClick={start}>
          교환하러 가기
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
