import { useNavigate } from 'react-router'

import { useStore } from '@/store/useStore'

import { SelectScreen } from './SelectScreen'

export function NeedsSelect() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()

  return (
    <SelectScreen
      title="내가 찾는 굿즈"
      heading="찾는 굿즈를 선택하세요"
      ctaLabel="교환하러 가기"
      allowEmpty
      selections={state.needs}
      onBack={() => navigate('/have')}
      onToggle={(itemId) => dispatch({ type: 'toggle-need', itemId })}
      onChangeQty={(itemId, qty) => dispatch({ type: 'set-need-qty', itemId, qty })}
      onClear={(itemId) => dispatch({ type: 'clear-need', itemId })}
      onSubmit={() => navigate('/home')}
    />
  )
}
