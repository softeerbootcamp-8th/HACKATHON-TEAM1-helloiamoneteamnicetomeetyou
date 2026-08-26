import { useNavigate } from 'react-router'

import { catalogNotice } from '@/features/catalog/use-register-selections'
import { useCatalog } from '@/features/catalog/useCatalog'
import { useStore } from '@/store/useStore'

import { SelectScreen } from './SelectScreen'

export function HaveSelect() {
  const navigate = useNavigate()
  const { state, dispatch } = useStore()
  const { state: catalog } = useCatalog()

  return (
    <SelectScreen
      title="내가 내놓을 굿즈"
      heading="교환할 굿즈를 선택하세요"
      ctaLabel="다음"
      allowEmpty={false}
      selections={state.have}
      notice={catalogNotice(catalog)}
      // 처음 등록 중이면 온보딩으로, 대기장소를 이미 본 뒤에 고치는 중이면 대기장소로.
      // Needs 는 비워도 되기 때문에 개수로 판단하면 안 된다.
      onBack={() => navigate(state.setupDone ? '/home' : '/')}
      onToggle={(itemId) => dispatch({ type: 'toggle-have', itemId })}
      onChangeQty={(itemId, qty) => dispatch({ type: 'set-have-qty', itemId, qty })}
      onClear={(itemId) => dispatch({ type: 'clear-have', itemId })}
      // 여기서는 서버로 보내지 않는다. 찾는 굿즈까지 고르고 "교환하러 가기" 를 눌렀을 때
      // 내놓을 카드와 함께 한 번에 등록한다. 뒤로 가서 고치는 동안 서버에 반쪽짜리 등록이
      // 남지 않게 하려는 것이다.
      onSubmit={() => navigate('/needs')}
    />
  )
}
