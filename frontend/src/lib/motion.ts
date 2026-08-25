import type { Transition, Variants } from 'motion/react'

/**
 * 모션 프리셋을 여기 한 곳에 모아 둔다. 화면마다 stiffness 를 다르게 적으면
 * 같은 앱인데 화면마다 손맛이 달라진다.
 *
 * duration 기반 이징 대신 spring 을 쓴다. 손가락을 떼는 속도가 그대로 이어져야
 * 끊긴 느낌이 안 난다.
 */

/** 화면 전환처럼 큰 덩어리가 움직일 때. */
export const springPage: Transition = {
  type: 'spring',
  stiffness: 320,
  damping: 34,
  mass: 0.9,
}

/** 버튼, 카드처럼 손가락 밑에서 바로 반응해야 하는 것. */
export const springSnap: Transition = {
  type: 'spring',
  stiffness: 520,
  damping: 32,
  mass: 0.6,
}

/** 바텀시트나 다이얼로그처럼 무게감이 있어야 하는 것. */
export const springSheet: Transition = {
  type: 'spring',
  stiffness: 300,
  damping: 34,
  mass: 0.8,
}

/** 사라지는 것은 빠르게. 기다리게 하면 답답하다. */
export const easeOut: Transition = { duration: 0.18, ease: [0.22, 1, 0.36, 1] }

/** 화면 전환: 앞으로 갈 때는 오른쪽에서, 뒤로 갈 때는 왼쪽에서 들어온다. */
export const pageVariants: Variants = {
  enter: (back: boolean) => ({ x: back ? -28 : 28, opacity: 0 }),
  center: { x: 0, opacity: 1 },
  exit: (back: boolean) => ({ x: back ? 28 : -28, opacity: 0 }),
}

/** 리스트가 순서대로 뜨게 한다. 한꺼번에 나타나면 싸구려로 보인다. */
export const staggerParent: Variants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.045, delayChildren: 0.04 } },
}

export const staggerChild: Variants = {
  hidden: { opacity: 0, y: 12, scale: 0.97 },
  show: { opacity: 1, y: 0, scale: 1, transition: springSnap },
}

/** 누를 때의 촉감. 모바일에는 hover 가 없으니 press 가 유일한 피드백이다. */
export const pressable = {
  whileTap: { scale: 0.96 },
  transition: springSnap,
} as const
