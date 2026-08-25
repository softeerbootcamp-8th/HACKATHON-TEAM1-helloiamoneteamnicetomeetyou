import { ALL_WAITING, FIXED_ZONE, itemById } from '@/mocks/data'

import { findMatch, type MatchResult } from './matching'
import type { ActiveMatch, IncomingPoke, State } from './types'

export const initialState: State = {
  onboarded: false,
  have: [],
  needs: [],
  autoMatching: false,
  match: null,
  declined: [],
  outgoingPoke: null,
  incomingPoke: null,
  heldIncoming: null,
  appointment: null,
  notifications: [],
  collection: [],
  toast: null,
}

export type Action =
  | { type: 'onboarded' }
  | { type: 'toggle-have'; itemId: string }
  | { type: 'set-have-qty'; itemId: string; qty: number }
  | { type: 'toggle-need'; itemId: string }
  | { type: 'set-need-qty'; itemId: string; qty: number }
  | { type: 'clear-have'; itemId: string }
  | { type: 'clear-need'; itemId: string }
  | { type: 'enter-home' }
  | { type: 'auto-match-tick' }
  | { type: 'open-match' }
  | { type: 'decline-match' }
  | { type: 'send-poke'; targetUserId: string }
  | { type: 'poke-answered'; accepted: boolean }
  | { type: 'receive-poke'; poke: IncomingPoke }
  | { type: 'release-held-poke' }
  | { type: 'accept-incoming'; chosenItemId: string }
  | { type: 'reject-incoming' }
  | { type: 'start-appointment' }
  | { type: 'set-my-slots'; slots: number[] }
  | { type: 'partner-slots-arrived'; slots: Record<string, number[]> }
  | { type: 'confirm-time'; slot: number; label: string }
  | { type: 'request-time-again' }
  | { type: 'arrive' }
  | { type: 'complete' }
  | { type: 'cancel-appointment' }
  | { type: 'reset' }
  | { type: 'toast'; message: string | null }
  | { type: 'read-notification'; id: string }

let notifSeq = 0
function notify(
  state: State,
  kind: State['notifications'][number]['kind'],
  title: string,
  body: string,
) {
  notifSeq += 1
  return [{ id: `n${notifSeq}`, kind, title, body }, ...state.notifications].slice(0, 12)
}

function bump(list: State['have'], itemId: string, delta: number): State['have'] {
  const found = list.find((s) => s.itemId === itemId)
  if (!found) return delta > 0 ? [...list, { itemId, qty: 1 }] : list
  const qty = found.qty + delta
  if (qty <= 0) return list.filter((s) => s.itemId !== itemId)
  return list.map((s) => (s.itemId === itemId ? { ...s, qty } : s))
}

function setQty(list: State['have'], itemId: string, qty: number): State['have'] {
  if (qty <= 0) return list.filter((s) => s.itemId !== itemId)
  if (!list.some((s) => s.itemId === itemId)) return [...list, { itemId, qty }]
  return list.map((s) => (s.itemId === itemId ? { ...s, qty } : s))
}

function partnersOf(match: MatchResult): string[] {
  return match.kind === 'ONE_TO_ONE' ? [match.partner.id] : [match.giver.id, match.receiver.id]
}

/** 교환이 끝난 카드는 다음 매칭에서 빠진다. */
function consume(state: State, giveItemId: string, receiveItemId: string): Partial<State> {
  return {
    have: bump(state.have, giveItemId, -1),
    needs: bump(state.needs, receiveItemId, -1),
    collection: [...state.collection, receiveItemId],
  }
}

export function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'onboarded':
      return { ...state, onboarded: true }

    case 'toggle-have':
      return { ...state, have: bump(state.have, action.itemId, 1) }
    case 'set-have-qty':
      return { ...state, have: setQty(state.have, action.itemId, action.qty) }
    case 'clear-have':
      return { ...state, have: state.have.filter((s) => s.itemId !== action.itemId) }

    case 'toggle-need':
      return { ...state, needs: bump(state.needs, action.itemId, 1) }
    case 'set-need-qty':
      return { ...state, needs: setQty(state.needs, action.itemId, action.qty) }
    case 'clear-need':
      return { ...state, needs: state.needs.filter((s) => s.itemId !== action.itemId) }

    case 'enter-home': {
      // 약속이 있으면 자동 매칭을 돌리지 않는다.
      const canMatch = !state.appointment && !state.match && state.needs.length > 0
      return { ...state, autoMatching: canMatch }
    }

    case 'auto-match-tick': {
      if (!state.autoMatching || state.match || state.appointment) return state
      const result = findMatch(
        state.have.map((s) => s.itemId),
        state.needs.map((s) => s.itemId),
      )
      if (!result) return state
      if (partnersOf(result).some((id) => state.declined.includes(id))) return state

      const match: ActiveMatch = { ...result, origin: 'auto' }
      return {
        ...state,
        match,
        autoMatching: false,
        notifications: notify(state, 'match', '서로의 니즈가 매칭됐어요!', '탭하여 확인'),
      }
    }

    case 'open-match':
      return state

    case 'decline-match': {
      if (!state.match) return state
      const declined = [...state.declined, ...partnersOf(state.match)]
      return {
        ...state,
        match: null,
        declined,
        appointment: null,
        autoMatching: state.needs.length > 0,
        toast: '교환을 거절했어요. 다시 상대를 찾을게요.',
      }
    }

    case 'send-poke': {
      const target = ALL_WAITING.find((u) => u.id === action.targetUserId)
      if (!target) return state
      return {
        ...state,
        outgoingPoke: { targetUserId: target.id, wantItemId: target.itemId, status: 'pending' },
        toast: null,
      }
    }

    case 'poke-answered': {
      const poke = state.outgoingPoke
      if (!poke || poke.status !== 'pending') return state
      const target = ALL_WAITING.find((u) => u.id === poke.targetUserId)
      if (!target) return state

      if (!action.accepted) {
        return {
          ...state,
          outgoingPoke: null,
          notifications: notify(
            state,
            'poke-rejected',
            `${target.nickname}님이 교환을 거절했어요`,
            '다른 상대를 찾아보세요',
          ),
          toast: `${target.nickname}님이 거절했어요`,
        }
      }

      // 상대가 내 묶음 중 자기가 원하는 카드를 골랐다고 본다.
      const giveItemId =
        state.have.find((s) => s.itemId === target.needsItemId)?.itemId ??
        state.have[0]?.itemId ??
        target.needsItemId

      const match: ActiveMatch = {
        kind: 'ONE_TO_ONE',
        partner: target,
        giveItemId,
        receiveItemId: target.itemId,
        origin: 'poke',
      }
      return {
        ...state,
        outgoingPoke: { ...poke, status: 'accepted' },
        match,
        autoMatching: false,
        notifications: notify(
          state,
          'poke-accepted',
          '찔러보기가 성사됐어요!',
          `${target.nickname}님이 수락했어요`,
        ),
      }
    }

    case 'receive-poke': {
      // 매칭이 도는 중에는 알리지 않고 붙잡아 둔다.
      if (state.autoMatching) return { ...state, heldIncoming: action.poke }
      const from = ALL_WAITING.find((u) => u.id === action.poke.fromUserId)
      return {
        ...state,
        incomingPoke: action.poke,
        notifications: notify(
          state,
          'poke-received',
          '상대가 교환을 요청했어요',
          `${from?.nickname ?? '상대'}님의 찔러보기`,
        ),
      }
    }

    case 'release-held-poke': {
      const held = state.heldIncoming
      if (!held) return state
      // 요청받은 카드의 잔여 수량이 없으면 알리지 않고 조용히 거절 처리한다.
      const left = state.have.find((s) => s.itemId === held.wantItemId)?.qty ?? 0
      if (left <= 0) return { ...state, heldIncoming: null }
      const from = ALL_WAITING.find((u) => u.id === held.fromUserId)
      return {
        ...state,
        heldIncoming: null,
        incomingPoke: held,
        notifications: notify(
          state,
          'poke-received',
          '상대가 교환을 요청했어요',
          `${from?.nickname ?? '상대'}님의 찔러보기`,
        ),
      }
    }

    case 'accept-incoming': {
      const incoming = state.incomingPoke
      if (!incoming) return state
      const from = ALL_WAITING.find((u) => u.id === incoming.fromUserId)
      if (!from) return state
      const match: ActiveMatch = {
        kind: 'ONE_TO_ONE',
        partner: from,
        giveItemId: incoming.wantItemId,
        receiveItemId: action.chosenItemId,
        origin: 'poke',
      }
      return { ...state, incomingPoke: null, match, autoMatching: false }
    }

    case 'reject-incoming':
      return {
        ...state,
        incomingPoke: null,
        toast: '교환 요청을 거절했어요',
      }

    case 'start-appointment':
      return {
        ...state,
        appointment: {
          stage: 'place',
          zoneId: FIXED_ZONE.id,
          mySlots: [],
          partnerSlots: {},
          confirmedSlot: null,
          confirmedLabel: null,
        },
        autoMatching: false,
      }

    case 'set-my-slots': {
      if (!state.appointment) return state
      return {
        ...state,
        appointment: { ...state.appointment, mySlots: action.slots, stage: 'time-waiting' },
      }
    }

    case 'partner-slots-arrived': {
      if (!state.appointment) return state
      return { ...state, appointment: { ...state.appointment, partnerSlots: action.slots } }
    }

    case 'confirm-time': {
      if (!state.appointment) return state
      return {
        ...state,
        appointment: {
          ...state.appointment,
          stage: 'confirmed',
          confirmedSlot: action.slot,
          confirmedLabel: action.label,
        },
      }
    }

    case 'request-time-again': {
      if (!state.appointment) return state
      return {
        ...state,
        appointment: { ...state.appointment, stage: 'time-conflict', partnerSlots: {} },
        notifications: notify(
          state,
          'time-request',
          '혹시.. 다른 시간도 되시나요?',
          `${itemById(state.match?.giveItemId ?? 'nv74').name} 거래`,
        ),
        toast: '상대에게 시간 조율을 요청했어요',
      }
    }

    case 'arrive': {
      if (!state.appointment) return state
      return { ...state, appointment: { ...state.appointment, stage: 'arrived' } }
    }

    case 'complete': {
      if (!state.match) return state
      const { giveItemId, receiveItemId } = state.match
      const next = consume(state, giveItemId, receiveItemId)
      return {
        ...state,
        ...next,
        match: null,
        appointment: null,
        outgoingPoke: null,
        declined: [],
        // 성사 이후 Needs 가 남아 있으면 자동 매칭을 다시 돌린다.
        autoMatching: (next.needs ?? state.needs).length > 0,
      }
    }

    case 'cancel-appointment':
      return {
        ...state,
        appointment: null,
        match: null,
        outgoingPoke: null,
        autoMatching: state.needs.length > 0,
        toast: '거래를 취소했어요',
      }

    case 'read-notification':
      return { ...state, notifications: state.notifications.filter((n) => n.id !== action.id) }

    case 'toast':
      return { ...state, toast: action.message }

    case 'reset':
      return { ...initialState }

    default:
      return state
  }
}
