import { motion } from 'motion/react'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { BoothPicker } from '@/components/domain/BoothPicker'
import { CardStack } from '@/components/domain/CardStack'
import { RadarRings } from '@/components/domain/Radar'
import { Button } from '@/components/ui/Button'
import { useCatalog } from '@/features/catalog/useCatalog'
import { springPage, springSnap } from '@/lib/motion'
import { useStore } from '@/store/useStore'

/**
 * 서버에서 부스 이름을 받기 전까지 세워 둘 이름.
 *
 * 로딩 중이나 서버가 끊겼을 때 이 자리를 비우면 첫 화면 맨 위가 잠깐 무너진다.
 */
const FALLBACK_BOOTH_NAME = '현대자동차 팝업'

export function Onboarding() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const { state: catalog, booths, booth, selectBooth } = useCatalog()
  const [pickerOpen, setPickerOpen] = useState(false)

  const boothName = booth?.name ?? FALLBACK_BOOTH_NAME

  /*
    첫 화면의 장식용 카드다. 부스에 실제로 있는 카드 중 첫 장을 세운다. 목록을 아직 못 받았으면
    빈 묶음이 뜨는데, 아무 카드나 세워 두면 그 부스에 없는 카드를 보여주게 된다.
  */
  const cardCount = catalog.status === 'ready' ? catalog.items.length : 0
  const topItemId = catalog.status === 'ready' ? (catalog.items[0]?.id ?? null) : null

  /**
   * 부스를 바꿀 수 있는 것은 여기, 그리고 카드를 등록하기 전까지다.
   *
   * `/` 는 주소로 바로 들어올 수 있어서 카드를 등록한 뒤에도 돌아올 길이 있다. 그때 부스를
   * 바꾸면 화면에는 등록한 카드가 남아 있는데 서버의 새 부스에는 아무것도 없는 상태가 된다.
   * 부스가 하나뿐이면 고를 것이 없으니 이름만 보여준다. 시연 중에 헛되이 열리지 않게.
   */
  const canSwitch = booths.length > 1 && !state.setupDone

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
          <p className="mt-0.5 pl-4 text-[11px] text-neutral-400">자동차 포토카드 교환</p>
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
              <CardStack topItemId={topItemId} count={3} className="w-[168px]" />
            </div>
          </motion.div>
        </div>

        <motion.p
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ ...springPage, delay: 0.34 }}
          className="mx-auto w-fit rounded-full bg-neutral-100 px-4 py-2 text-[12px] font-medium text-neutral-500"
        >
          지금 {cardCount}종의 카드를 교환하고 있어요
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
        onSelect={(id) => {
          selectBooth(id)
          setPickerOpen(false)
        }}
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
