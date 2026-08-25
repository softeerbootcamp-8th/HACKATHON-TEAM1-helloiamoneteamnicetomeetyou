import { useState } from 'react'

/**
 * 값이 사라져도 마지막으로 있던 값을 계속 돌려준다.
 *
 * 화면이 빠져나가는 동안에도 컴포넌트는 살아 있어서, 그 사이에 상태가 비면
 * "진행 중인 매칭이 없어요" 같은 빈 화면이 잠깐 깜빡인다. 나가는 중에는 마지막 모습을
 * 그대로 붙들고 있는 편이 자연스럽다.
 */
export function useLastDefined<T>(value: T | null | undefined): T | null {
  const [last, setLast] = useState<T | null>(value ?? null)
  if (value != null && value !== last) setLast(value)
  return value ?? last
}
