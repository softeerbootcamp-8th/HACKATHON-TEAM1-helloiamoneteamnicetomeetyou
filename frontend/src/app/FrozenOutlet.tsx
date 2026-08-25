import { useState } from 'react'
import { useOutlet } from 'react-router'

/**
 * 마운트될 때의 화면을 붙들어 둔다.
 *
 * `<Outlet />` 을 그대로 쓰면 화면이 빠져나가는 동안에도 새 라우트를 다시 그린다.
 * 그래서 전환 중에 같은 화면이 두 장 겹쳐 보이고, 그게 잔상으로 보인다.
 * 판마다 이 컴포넌트를 새로 마운트해서 각자 자기 화면만 그리게 한다.
 */
export function FrozenOutlet() {
  const [frozen] = useState(useOutlet())
  return frozen
}
