import { useNavigate } from 'react-router'

import { catalogNotice, useRegisterSelections } from '@/features/catalog/use-register-selections'
import { useCatalog } from '@/features/catalog/useCatalog'
import { useStore } from '@/store/useStore'

import { SelectScreen } from './SelectScreen'

export function HaveSelect() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const { state: catalog } = useCatalog()
  const { submit, submitting, error } = useRegisterSelections('have')

  return (
    <SelectScreen
      title="내가 내놓을 굿즈"
      heading="교환할 굿즈를 선택하세요"
      ctaLabel="다음"
      allowEmpty={false}
      selections={state.have}
      submitting={submitting}
      submitError={error}
      notice={catalogNotice(catalog)}
      // 처음 등록 중이면 온보딩으로, 대기장소를 이미 본 뒤에 고치는 중이면 대기장소로.
      // Needs 는 비워도 되기 때문에 개수로 판단하면 안 된다.
      onBack={() => navigate(state.setupDone ? '/home' : '/')}
      onToggle={(itemId) => dispatch({ type: 'toggle-have', itemId })}
      onChangeQty={(itemId, qty) => dispatch({ type: 'set-have-qty', itemId, qty })}
      onClear={(itemId) => dispatch({ type: 'clear-have', itemId })}
      onSubmit={() => void submit(state.have, () => navigate('/needs'))}
    />
  )
}
