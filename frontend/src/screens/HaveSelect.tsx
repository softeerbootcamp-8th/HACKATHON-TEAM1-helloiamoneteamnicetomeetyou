import { useNavigate } from 'react-router'

import { useStore } from '@/store/useStore'

import { SelectScreen } from './SelectScreen'

export function HaveSelect() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()

  return (
    <SelectScreen
      title="내가 내놓을 굿즈"
      heading="교환할 굿즈를 선택하세요"
      ctaLabel="다음"
      allowEmpty={false}
      selections={state.have}
      onBack={() => navigate(state.onboarded && state.needs.length > 0 ? '/home' : '/')}
      onToggle={(itemId) => dispatch({ type: 'toggle-have', itemId })}
      onChangeQty={(itemId, qty) => dispatch({ type: 'set-have-qty', itemId, qty })}
      onClear={(itemId) => dispatch({ type: 'clear-have', itemId })}
      onSubmit={() => navigate('/needs')}
    />
  )
}
