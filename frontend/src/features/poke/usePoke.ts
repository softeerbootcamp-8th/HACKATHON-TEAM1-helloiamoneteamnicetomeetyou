import { useContext } from 'react'

import { PokeContext, type PokeValue } from './PokeContext'

export function usePoke(): PokeValue {
  const value = useContext(PokeContext)
  if (!value) throw new Error('usePoke 는 PokeProvider 안에서만 쓸 수 있습니다')
  return value
}
