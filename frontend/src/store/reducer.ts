import { ALL_WAITING, FIXED_ZONE, itemById } from '@/mocks/data'

import { findMatch, type MatchResult } from './matching'
import type { ActiveMatch, IncomingPoke, State } from './types'

export const initialState: State = {
  onboarded: false,
  setupDone: false,
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
  | { type: 'poke-answered'; accepted: boolean; chosenItemId?: string }
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
  | { type: 'seed-demo'; kind: 'three-way' | 'incoming' }

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

    // 내놓기로 한 굿즈는 찾는 굿즈에서 빠진다. 뒤로 가서 Have 를 고쳤을 때
    // Needs 에 같은 것이 남아 있으면 화면에는 못 고르는데 개수에는 잡힌다.
    case 'toggle-have': {
      const have = bump(state.have, action.itemId, 1)
      return { ...state, have, needs: state.needs.filter((n) => n.itemId !== action.itemId) }
    }
    case 'set-have-qty': {
      const have = setQty(state.have, action.itemId, action.qty)
      const stillHave = have.some((h) => h.itemId === action.itemId)
      return {
        ...state,
        have,
        needs: stillHave ? state.needs.filter((n) => n.itemId !== action.itemId) : state.needs,
      }
    }
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
      return { ...state, autoMatching: canMatch, setupDone: true }
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

      // 상대가 내 묶음에서 무엇을 골랐는지는 고른 쪽만 안다.
      //
      // 전에는 wantedFromMe 로 짐작했는데, 찔러보기는 정의상 "상대 희망 ∩ 내 보유" 가
      // 비어 있어서 그 계산이 늘 빈 배열이었다. 그래서 항상 have[0] 으로 떨어져 A 화면에
      // 엉뚱한 카드가 떴다. 고른 카드를 받아 쓰고, 없으면 첫 장으로만 떨어진다.
      const giveItemId = action.chosenItemId ?? state.have[0]?.itemId ?? target.needsItemIds[0]

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
        toast: `🎉 ${target.nickname}님이 교환을 수락했어요!`,
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
      // 요청받은 카드의 잔여 수량이 없으면 나에게는 알리지 않는다. 고를 수 있는 것이
      // 없는 화면을 띄우는 것보다 조용히 지나가는 편이 낫다.
      //
      // 다만 상대는 답을 기다리고 있으므로 거절이 전달돼야 한다 (시안 desc 204:5194).
      // 목업에서는 보낼 상대가 가짜라 토스트로 그 사실만 드러낸다. 서버 경로에서는
      // 수락을 시도할 때 POKE_ITEM_SOLD_OUT 으로 갈린다.
      const left = state.have.find((s) => s.itemId === held.wantItemId)?.qty ?? 0
      if (left <= 0) {
        return {
          ...state,
          heldIncoming: null,
          toast: '받은 교환 요청의 카드가 이미 나갔어요. 상대에게 알렸어요.',
        }
      }
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

    case 'seed-demo': {
      // 특정 화면을 바로 열어 볼 수 있게 상태를 심어 준다. 주소로만 들어올 수 있고
      // 실제 흐름에서는 쓰이지 않는다.
      const have = state.have.length > 0 ? state.have : [{ itemId: 'nv74', qty: 2 }]

      if (action.kind === 'three-way') {
        const giver = ALL_WAITING.find((u) => u.id === 'u3')
        const receiver = ALL_WAITING.find((u) => u.id === 'u6')
        if (!giver || !receiver) return state
        return {
          ...state,
          onboarded: true,
          setupDone: true,
          have,
          needs: state.needs.length > 0 ? state.needs : [{ itemId: 'i5n', qty: 1 }],
          autoMatching: false,
          appointment: null,
          match: {
            kind: 'THREE_WAY',
            giver,
            receiver,
            giveItemId: 'nv74',
            receiveItemId: 'i5n',
            middleItemId: 'pony',
            origin: 'auto',
          },
          notifications: notify(state, 'match', '서로의 니즈가 매칭됐어요!', '탭하여 확인'),
        }
      }

      const from = ALL_WAITING.find((u) => u.id === 'u1')
      if (!from) return state
      return {
        ...state,
        onboarded: true,
        setupDone: true,
        have,
        autoMatching: false,
        match: null,
        incomingPoke: {
          fromUserId: from.id,
          wantItemId: have[0].itemId,
          offeredItemIds: ['i5n', 'sf', 'cas'],
        },
        notifications: notify(
          state,
          'poke-received',
          '상대가 교환을 요청했어요',
          `${from.nickname}님의 찔러보기`,
        ),
      }
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
