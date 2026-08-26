import { ALL_WAITING, FIXED_ZONE, itemById } from '@/mocks/data'

import { wantedFromMe, type ExchangePair, type MatchResult } from './matching'
import { earliestOverlap } from './time'
import type { ActiveMatch, Appointment, IncomingPoke, State } from './types'

/**
 * 알림 문구는 시안의 `교환 대기장소 알림 정리` 를 그대로 옮긴 것이다.
 * 알림은 메인 텍스트만 바뀌고 보조 문구는 전부 "탭하여 확인" 이다.
 */
const NOTICE_BODY = '탭하여 확인'

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
  appointments: [],
  activeAppointmentId: null,
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
  | { type: 'server-match-arrived'; match: ActiveMatch }
  | { type: 'server-match-rejected'; exchangeId: number }
  | { type: 'open-match' }
  | { type: 'decline-match' }
  | { type: 'send-poke'; targetUserId: string }
  | { type: 'poke-answered'; accepted: boolean }
  | { type: 'receive-poke'; poke: IncomingPoke }
  | { type: 'release-held-poke' }
  | { type: 'accept-incoming'; chosenItemId: string }
  | { type: 'reject-incoming' }
  | { type: 'start-appointment' }
  | { type: 'select-appointment'; id: string }
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

/** 이 교환에서 실제로 주고받는 카드 쌍. 삼자 교환은 언제나 한 쌍이다. */
function pairsOf(match: MatchResult): ExchangePair[] {
  if (match.kind === 'ONE_TO_ONE') return match.pairs
  return [{ giveItemId: match.giveItemId, receiveItemId: match.receiveItemId }]
}

/** 교환이 끝난 카드는 다음 매칭에서 빠진다. 여러 장을 한 번에 바꿨으면 전부 덜어낸다. */
function consume(state: State, pairs: ExchangePair[]): Partial<State> {
  let have = state.have
  let needs = state.needs
  const collection = [...state.collection]

  for (const pair of pairs) {
    have = bump(have, pair.giveItemId, -1)
    needs = bump(needs, pair.receiveItemId, -1)
    collection.push(pair.receiveItemId)
  }

  return { have, needs, collection }
}

let apptSeq = 0

export function activeAppointment(state: State): Appointment | null {
  return state.appointments.find((a) => a.id === state.activeAppointmentId) ?? null
}

/** 지금 다루고 있는 약속 하나만 고친다. 나머지 약속은 그대로 둔다. */
function patchActive(state: State, patch: (appt: Appointment) => Appointment): State {
  if (!state.activeAppointmentId) return state
  return {
    ...state,
    appointments: state.appointments.map((a) =>
      a.id === state.activeAppointmentId ? patch(a) : a,
    ),
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
      const canMatch = state.appointments.length === 0 && !state.match && state.needs.length > 0
      return { ...state, autoMatching: canMatch, setupDone: true }
    }

    /**
     * 서버가 SSE 로 실제 매칭을 알려온 것. 이미 화면에 매칭이나 약속이 떠 있으면 덮어쓰지 않는다.
     */
    case 'server-match-arrived': {
      if (state.match || state.appointments.length > 0) return state
      return {
        ...state,
        match: action.match,
        autoMatching: false,
        notifications: notify(state, 'match', '내가 원하는 굿즈로 교환할 수 있어요!', NOTICE_BODY),
      }
    }

    /**
     * 상대가 이 매칭을 거절했다는 서버 알림. 내가 거절한 게 아니라 상대 쪽에서 온 거라
     * `decline-match` 와는 다른 자리다. `exchangeId` 가 지금 뜬 매칭과 다르면(이미 다른
     * 매칭으로 넘어갔거나 약속을 잡은 뒤) 조용히 무시한다 — 뒤늦게 도착한 알림이 엉뚱한
     * 화면을 지우면 안 된다.
     */
    case 'server-match-rejected': {
      if (state.match?.exchangeId !== action.exchangeId) return state
      return {
        ...state,
        match: null,
        autoMatching: state.appointments.length === 0 && state.needs.length > 0,
        notifications: notify(state, 'match-rejected', '상대가 교환을 거절했어요', NOTICE_BODY),
        toast: '상대가 거절해서 다시 상대를 찾을게요.',
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
        autoMatching: state.appointments.length === 0 && state.needs.length > 0,
        toast: '교환을 거절했어요. 다시 상대를 찾을게요.',
      }
    }

    case 'send-poke': {
      const target = ALL_WAITING.find((u) => u.id === action.targetUserId)
      if (!target) return state
      return {
        ...state,
        outgoingPoke: { targetUserId: target.id, wantItemId: target.itemId, status: 'pending' },
        toast: `${itemById(target.itemId).name} 교환을 제안했어요\n답변 기다리는 중`,
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
            `${itemById(target.itemId).name} 교환이 거절되었어요`,
            NOTICE_BODY,
          ),
          toast: `${itemById(target.itemId).name} 교환이 거절되었어요`,
        }
      }

      // 상대가 내 묶음 중 자기가 원하는 카드를 골랐다고 본다.
      const wanted = wantedFromMe(
        target,
        state.have.map((s) => s.itemId),
      )
      const giveItemId = wanted[0] ?? state.have[0]?.itemId ?? target.needsItemIds[0]

      const match: ActiveMatch = {
        kind: 'ONE_TO_ONE',
        partner: target,
        pairs: [{ giveItemId, receiveItemId: target.itemId }],
        giveItemId,
        receiveItemId: target.itemId,
        origin: 'poke',
        exchangeId: null,
      }
      return {
        ...state,
        outgoingPoke: { ...poke, status: 'accepted' },
        match,
        autoMatching: false,
        notifications: notify(
          state,
          'poke-accepted',
          '상대방이 내 신청을 받아들였어요!',
          NOTICE_BODY,
        ),
        toast: null,
      }
    }

    case 'receive-poke': {
      // 매칭이 도는 중에는 알리지 않고 붙잡아 둔다.
      if (state.autoMatching) return { ...state, heldIncoming: action.poke }
      return {
        ...state,
        incomingPoke: action.poke,
        notifications: notify(state, 'poke-received', '교환 신청이 왔어요~', NOTICE_BODY),
      }
    }

    case 'release-held-poke': {
      const held = state.heldIncoming
      if (!held) return state
      // 요청받은 카드의 잔여 수량이 없으면 알리지 않고 조용히 거절 처리한다.
      const left = state.have.find((s) => s.itemId === held.wantItemId)?.qty ?? 0
      if (left <= 0) return { ...state, heldIncoming: null }
      return {
        ...state,
        heldIncoming: null,
        incomingPoke: held,
        notifications: notify(state, 'poke-received', '교환 신청이 왔어요~', NOTICE_BODY),
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
        pairs: [{ giveItemId: incoming.wantItemId, receiveItemId: action.chosenItemId }],
        giveItemId: incoming.wantItemId,
        receiveItemId: action.chosenItemId,
        origin: 'poke',
        exchangeId: null,
      }
      return { ...state, incomingPoke: null, match, autoMatching: false }
    }

    case 'reject-incoming':
      return {
        ...state,
        incomingPoke: null,
        toast: '교환 요청을 거절했어요',
      }

    case 'start-appointment': {
      if (!state.match) return state
      apptSeq += 1
      const appointment: Appointment = {
        id: `appt${apptSeq}`,
        match: state.match,
        stage: 'place',
        zoneId: FIXED_ZONE.id,
        mySlots: [],
        partnerSlots: {},
        confirmedSlot: null,
        confirmedLabel: null,
      }
      // 약속이 들고 갈 교환이라 화면 전역의 match 는 여기서 비운다.
      return {
        ...state,
        match: null,
        appointments: [...state.appointments, appointment],
        activeAppointmentId: appointment.id,
        autoMatching: false,
      }
    }

    case 'select-appointment':
      return { ...state, activeAppointmentId: action.id }

    case 'set-my-slots':
      return patchActive(state, (appt) => ({
        ...appt,
        mySlots: action.slots,
        stage: 'time-waiting',
      }))

    case 'partner-slots-arrived': {
      const active = activeAppointment(state)
      if (!active) return state
      // 겹치는 시간이 생긴 순간에만 알린다. 없으면 조율 중인 채로 둔다.
      const overlap = earliestOverlap([active.mySlots, ...Object.values(action.slots)])
      const next = patchActive(state, (appt) => ({ ...appt, partnerSlots: action.slots }))
      return {
        ...next,
        notifications:
          overlap === -1
            ? state.notifications
            : notify(state, 'time-matched', '시간 매칭이 완료되었어요!', NOTICE_BODY),
      }
    }

    case 'confirm-time':
      return patchActive(state, (appt) => ({
        ...appt,
        stage: 'confirmed',
        confirmedSlot: action.slot,
        confirmedLabel: action.label,
      }))

    case 'request-time-again': {
      const next = patchActive(state, (appt) => ({
        ...appt,
        stage: 'time-conflict',
        partnerSlots: {},
      }))
      return {
        ...next,
        notifications: notify(
          state,
          'time-request',
          '혹시... 다른 시간도 가능하세요?',
          NOTICE_BODY,
        ),
        toast: '상대에게 시간 조율을 요청했어요',
      }
    }

    case 'arrive':
      return patchActive(state, (appt) => ({ ...appt, stage: 'arrived' }))

    case 'complete': {
      const active = activeAppointment(state)
      if (!active) return state
      const next = consume(state, pairsOf(active.match))
      const appointments = state.appointments.filter((a) => a.id !== active.id)
      return {
        ...state,
        ...next,
        appointments,
        activeAppointmentId: null,
        outgoingPoke: null,
        declined: [],
        // 성사 이후 Needs 가 남아 있고 다른 약속이 없으면 자동 매칭을 다시 돌린다.
        autoMatching: appointments.length === 0 && (next.needs ?? state.needs).length > 0,
      }
    }

    case 'cancel-appointment': {
      const appointments = state.appointments.filter((a) => a.id !== state.activeAppointmentId)
      return {
        ...state,
        appointments,
        activeAppointmentId: null,
        match: null,
        outgoingPoke: null,
        autoMatching: appointments.length === 0 && state.needs.length > 0,
        toast: '약속을 취소했어요',
      }
    }

    case 'seed-demo': {
      // 특정 화면을 바로 열어 볼 수 있게 상태를 심어 준다. 주소로만 들어올 수 있고
      // 실제 흐름에서는 쓰이지 않는다.
      const have = state.have.length > 0 ? state.have : [{ itemId: 'avn', qty: 2 }]

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
          appointments: [],
          activeAppointmentId: null,
          match: {
            kind: 'THREE_WAY',
            giver,
            receiver,
            giveItemId: 'avn',
            receiveItemId: 'i5n',
            middleItemId: 'i30f',
            origin: 'auto',
            exchangeId: null,
          },
          notifications: notify(
            state,
            'match',
            '내가 원하는 굿즈로 교환할 수 있어요!',
            NOTICE_BODY,
          ),
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
          offeredItemIds: ['i5n', 'i30f', 'kona'],
        },
        notifications: notify(state, 'poke-received', '교환 신청이 왔어요~', NOTICE_BODY),
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
