import { useContext } from 'react'

import { StoreContext, type Store } from './context'

export function useStore(): Store {
  const store = useContext(StoreContext)
  if (!store) throw new Error('StoreProvider 안에서만 쓸 수 있습니다.')
  return store
}
