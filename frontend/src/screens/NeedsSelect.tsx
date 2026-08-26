import { useNavigate } from 'react-router'

import { catalogNotice, useRegisterSelections } from '@/features/catalog/use-register-selections'
import { useCatalog } from '@/features/catalog/useCatalog'
import { useStore } from '@/store/useStore'

import { SelectScreen } from './SelectScreen'

export function NeedsSelect() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const { state: catalog } = useCatalog()
  const { submit, submitting, error } = useRegisterSelections()

  return (
    <SelectScreen
      title="내가 찾는 굿즈"
      heading="찾는 굿즈를 선택하세요"
      ctaLabel="교환하러 가기"
      allowEmpty
      disabledItemIds={state.have.map((s) => s.itemId)}
      selections={state.needs}
      submitting={submitting}
      submitError={error}
      notice={catalogNotice(catalog)}
      onBack={() => navigate('/have')}
      onToggle={(itemId) => dispatch({ type: 'toggle-need', itemId })}
      onChangeQty={(itemId, qty) => dispatch({ type: 'set-need-qty', itemId, qty })}
      onClear={(itemId) => dispatch({ type: 'clear-need', itemId })}
      // 두 화면에서 고른 것을 여기서 한 번에 보낸다. 등록이 끝나야 서버 매칭이 돈다.
      onSubmit={() => void submit(state.have, state.needs, () => navigate('/home'))}
    />
  )
}
