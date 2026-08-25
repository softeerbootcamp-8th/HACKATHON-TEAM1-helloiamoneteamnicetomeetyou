import { useEffect, useMemo, useReducer, type ReactNode } from 'react'

import { ALL_WAITING } from '@/mocks/data'

import { StoreContext } from './context'
import { activeAppointment, initialState, reducer } from './reducer'

export function StoreProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, initialState)
  // 지금 다루고 있는 약속. 아래 효과들이 이것만 보고 돌면 되므로 밖에서 한 번 꺼내 둔다.
  const appointment = activeAppointment(state)

  // 자동 매칭. 켜져 있는 동안 짧은 간격으로 상대를 찾는다.
  useEffect(() => {
    if (!state.autoMatching) return
    const timer = window.setTimeout(() => dispatch({ type: 'auto-match-tick' }), 2600)
    return () => window.clearTimeout(timer)
  }, [state.autoMatching, state.have, state.needs])

  // 매칭이 끝나면 붙잡아 둔 찔러보기를 그때 알린다.
  useEffect(() => {
    if (state.autoMatching || !state.heldIncoming) return
    const timer = window.setTimeout(() => dispatch({ type: 'release-held-poke' }), 700)
    return () => window.clearTimeout(timer)
  }, [state.autoMatching, state.heldIncoming])

  // 보낸 찔러보기에 대한 상대의 응답.
  useEffect(() => {
    if (state.outgoingPoke?.status !== 'pending') return
    const timer = window.setTimeout(() => {
      const target = ALL_WAITING.find((u) => u.id === state.outgoingPoke?.targetUserId)

      // 상대는 자기가 원하는 카드가 내 묶음에 있으면 그걸 고르고, 없으면 아무 한 장을
      // 고른다. 찔러보기는 원래 원하는 것이 없는 상대에게 보내는 것이라 뒤쪽이 보통이다.
      // 서버 연동에서는 이 자리에 실제로 고른 카드가 들어온다.
      const wanted = state.have.find((s) => target?.needsItemIds.includes(s.itemId))
      const chosen = wanted ?? state.have[0]

      dispatch({
        type: 'poke-answered',
        accepted: Boolean(target && chosen),
        chosenItemId: chosen?.itemId,
      })
    }, 3200)
    return () => window.clearTimeout(timer)
  }, [state.outgoingPoke, state.have])

  // 시간 선택 후 상대들의 응답.
  useEffect(() => {
    const appt = appointment
    if (!appt || appt.stage !== 'time-waiting') return
    if (appt.mySlots.length === 0) return
    if (Object.keys(appt.partnerSlots).length > 0) return

    const timer = window.setTimeout(() => {
      const partners =
        appt.match.kind === 'ONE_TO_ONE'
          ? [appt.match.partner.id]
          : [appt.match.giver.id, appt.match.receiver.id]

      // 상대도 자기가 되는 칸을 고른다. 내가 고른 칸이 하나라도 들어 있으면 약속이 잡히고,
      // 늦은 시간만 되는 상대를 만나면 겹치는 칸이 없어서 조율 화면으로 넘어간다.
      const first = appt.mySlots[0]
      const slots: Record<string, number[]> = {}
      partners.forEach((id, i) => {
        const prefersLate = (id.charCodeAt(id.length - 1) + i) % 3 === 2
        slots[id] = prefersLate ? [5, 6, 7] : [first, Math.min(first + 1, 7), 4]
      })
      dispatch({ type: 'partner-slots-arrived', slots })
    }, 1800)
    return () => window.clearTimeout(timer)
  }, [appointment])

  // 토스트는 스스로 사라진다.
  useEffect(() => {
    if (!state.toast) return
    const timer = window.setTimeout(() => dispatch({ type: 'toast', message: null }), 2400)
    return () => window.clearTimeout(timer)
  }, [state.toast])

  const value = useMemo(() => ({ state, dispatch }), [state])
  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>
}
