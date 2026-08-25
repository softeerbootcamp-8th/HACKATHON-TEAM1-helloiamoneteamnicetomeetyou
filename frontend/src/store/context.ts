import { createContext } from 'react'

import type { Action } from './reducer'
import type { State } from './types'

export type Store = { state: State; dispatch: (action: Action) => void }

/**
 * 컴포넌트 파일에서 훅과 컨텍스트를 같이 내보내면 Fast Refresh 가 깨진다.
 * 그래서 컨텍스트는 이 파일에, 훅은 useStore.ts 에 따로 둔다.
 */
export const StoreContext = createContext<Store | null>(null)
