import { useEffect, useMemo, useReducer, type ReactNode } from 'react'

import { ALL_WAITING } from '@/mocks/data'

import { StoreContext } from './context'
import { initialState, reducer } from './reducer'

export function StoreProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, initialState)

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
      // 상대가 내 묶음에서 원하는 카드를 찾을 수 있으면 수락한다.
      const target = ALL_WAITING.find((u) => u.id === state.outgoingPoke?.targetUserId)
      const accepted = Boolean(
        target && state.have.some((s) => target.needsItemIds.includes(s.itemId)),
      )
      dispatch({ type: 'poke-answered', accepted })
    }, 3200)
    return () => window.clearTimeout(timer)
  }, [state.outgoingPoke, state.have])

  // 남이 나에게 보내는 찔러보기. 내놓은 카드가 있어야 들어온다.
  useEffect(() => {
    if (state.have.length === 0) return
    if (state.incomingPoke || state.heldIncoming || state.match || state.appointment) return
    const timer = window.setTimeout(() => {
      const mine = state.have[0].itemId
      const from = ALL_WAITING.find(
        (u) => u.needsItemIds.includes(mine) && u.id !== state.outgoingPoke?.targetUserId,
      )
      if (!from) return
      const offered = ALL_WAITING.filter((u) => u.id !== from.id)
        .map((u) => u.itemId)
        .filter((id, i, arr) => arr.indexOf(id) === i)
        .slice(0, 3)
      dispatch({
        type: 'receive-poke',
        poke: {
          fromUserId: from.id,
          wantItemId: mine,
          offeredItemIds: [from.itemId, ...offered].slice(0, 3),
        },
      })
    }, 9000)
    return () => window.clearTimeout(timer)
  }, [
    state.have,
    state.incomingPoke,
    state.heldIncoming,
    state.match,
    state.appointment,
    state.outgoingPoke,
  ])

  // 시간 선택 후 상대들의 응답.
  useEffect(() => {
    const appt = state.appointment
    if (!appt || appt.stage !== 'time-waiting') return
    if (appt.mySlots.length === 0) return
    if (Object.keys(appt.partnerSlots).length > 0) return

    const timer = window.setTimeout(() => {
      const partners =
        state.match?.kind === 'ONE_TO_ONE'
          ? [state.match.partner.id]
          : state.match
            ? [state.match.giver.id, state.match.receiver.id]
            : []

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
  }, [state.appointment, state.match])

  // 토스트는 스스로 사라진다.
  useEffect(() => {
    if (!state.toast) return
    const timer = window.setTimeout(() => dispatch({ type: 'toast', message: null }), 2400)
    return () => window.clearTimeout(timer)
  }, [state.toast])

  const value = useMemo(() => ({ state, dispatch }), [state])
  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>
}
