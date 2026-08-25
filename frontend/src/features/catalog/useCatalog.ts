import { useContext } from 'react'

import { CatalogContext, type CatalogValue } from './CatalogContext'

export function useCatalog(): CatalogValue {
  const value = useContext(CatalogContext)
  if (!value) throw new Error('useCatalog 는 CatalogProvider 안에서만 쓸 수 있습니다')
  return value
}
