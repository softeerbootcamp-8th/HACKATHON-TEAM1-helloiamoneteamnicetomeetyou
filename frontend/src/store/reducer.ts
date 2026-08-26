import type { Exchange, Zone } from '@/lib/exchange'
import { ALL_WAITING, itemById } from '@/mocks/data'

import { toAppointment } from './appointment'
import type { ExchangePair, MatchResult } from './matching'
import type { ActiveMatch, Appointment, IncomingPoke, State } from './types'

/**
 * 알림 문구는 시안의 `교환 대기장소 알림 정리` 를 그대로 옮긴 것이다.
 * 알림은 메인 텍스트만 바뀌고 보조 문구는 전부 "탭하여 확인" 이다.
 */
const NOTICE_BODY = '탭하여 확인'

export const initialState: State = {
  onboarded: false,
  boothId: null,
  zones: [],
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
  | { type: 'poke-answered'; accepted: boolean; chosenItemId?: string }
  | { type: 'receive-poke'; poke: IncomingPoke }
  | { type: 'release-held-poke' }
  | { type: 'accept-incoming'; chosenItemId: string }
  | { type: 'reject-incoming' }
  | { type: 'booth-loaded'; boothId: number; zones: Zone[] }
  | {
      type: 'exchange-synced'
      exchange: Exchange
      myUserId: string
      /** 매칭 결과를 아는 자리에서만 넘긴다. 새로고침으로 들어온 경우에는 없다. */
      match?: ActiveMatch | null
      /** 이 약속을 지금 보고 있는 것으로 삼을지. 만들자마자 들어갈 때만 참이다. */
      activate?: boolean
    }
  | { type: 'select-appointment'; id: number }
  | { type: 'my-slots-picked'; slots: number[] }
  | { type: 'request-time-again' }
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

export function activeAppointment(state: State): Appointment | null {
  return state.appointments.find((a) => a.exchangeId === state.activeAppointmentId) ?? null
}

/** 지금 다루고 있는 약속 하나만 고친다. 나머지 약속은 그대로 둔다. */
function patchActive(state: State, patch: (appt: Appointment) => Appointment): State {
  if (state.activeAppointmentId === null) return state
  return {
    ...state,
    appointments: state.appointments.map((a) =>
      a.exchangeId === state.activeAppointmentId ? patch(a) : a,
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
      /*
        시안 desc 165:3500 1번 — 태그를 가르는 것은 <b>진행 중인 약속이 있는지 하나</b>다.
        찾는 카드를 아직 등록하지 않은 사용자도 이 화면의 정상 상태라서(desc 165:3500 2번의
        "Wanted 등록 안 한 사용자" 경로) 그 사람에게도 태그가 떠야 한다.

        `!state.match` 는 남긴다. 매칭이 잡히면 자동 매칭은 실제로 멈춘 것이고, 그 자리는
        매칭 배너가 대신 쓴다.
      */
      const canMatch = state.appointments.length === 0 && !state.match
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
        autoMatching: state.appointments.length === 0,
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
        autoMatching: state.appointments.length === 0,
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

      // 상대가 내 묶음에서 무엇을 골랐는지는 고른 쪽만 안다.
      //
      // 전에는 wantedFromMe 로 짐작했는데, 찔러보기는 정의상 "상대 희망 ∩ 내 보유" 가
      // 비어 있어서 그 계산이 늘 빈 배열이었다. 그래서 항상 have[0] 으로 떨어져 보낸 사람
      // 화면에 엉뚱한 카드가 떴다. 고른 카드를 받아 쓰고, 없을 때만 첫 장으로 떨어진다.
      const giveItemId = action.chosenItemId ?? state.have[0]?.itemId ?? target.needsItemIds[0]

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

    case 'booth-loaded':
      return { ...state, boothId: action.boothId, zones: action.zones }

    /**
     * 서버에서 읽어 온 교환으로 약속을 갈아끼운다. 만들었을 때, 실시간 알림을 받았을 때,
     * 앱을 다시 열었을 때 모두 이 하나를 거친다.
     *
     * 이미 들고 있던 같은 약속이 있으면 그 자리에 덮어쓴다. 무엇을 주고받는지는 서버가 모르는
     * 값이라 이전 것에서 이어받는다.
     */
    case 'exchange-synced': {
      const { exchange, myUserId } = action
      const previous = state.appointments.find((a) => a.exchangeId === exchange.exchangeId)

      // 끝났거나 취소된 약속은 목록에서 뺀다. 그대로 두면 오지 않을 약속을 계속 보여주게 된다.
      if (exchange.status === 'CANCELLED') {
        const appointments = state.appointments.filter((a) => a.exchangeId !== exchange.exchangeId)
        return {
          ...state,
          appointments,
          activeAppointmentId:
            state.activeAppointmentId === exchange.exchangeId ? null : state.activeAppointmentId,
          autoMatching: appointments.length === 0,
          toast: previous ? '교환이 취소됐어요' : state.toast,
        }
      }

      const next = toAppointment(exchange, myUserId, previous, action.match)
      const appointments = previous
        ? state.appointments.map((a) => (a.exchangeId === next.exchangeId ? next : a))
        : [...state.appointments, next]

      // 겹치는 시간이 막 생긴 순간에만 알린다. 이미 알린 약속에 또 알리지 않는다.
      const overlapAppeared = previous?.overlapSlot === null && next.overlapSlot !== null

      return {
        ...state,
        match: action.match ? null : state.match,
        appointments,
        activeAppointmentId: action.activate ? next.exchangeId : state.activeAppointmentId,
        autoMatching: false,
        notifications: overlapAppeared
          ? notify(state, 'time-matched', '시간 매칭이 완료되었어요!', NOTICE_BODY)
          : state.notifications,
      }
    }

    case 'select-appointment':
      return { ...state, activeAppointmentId: action.id }

    /**
     * 칸을 눌렀을 때 화면을 먼저 바꾼다. 서버 응답을 기다렸다 칠하면 손가락을 뗀 뒤에야
     * 칸이 차서 눌린 느낌이 사라진다. 저장이 실패하면 그때 다시 읽어 되돌린다.
     */
    case 'my-slots-picked':
      return patchActive(state, (appt) => ({ ...appt, mySlots: action.slots }))

    case 'request-time-again':
      return {
        ...state,
        notifications: notify(
          state,
          'time-request',
          '혹시... 다른 시간도 가능하세요?',
          NOTICE_BODY,
        ),
        toast: '상대에게 시간 조율을 요청했어요',
      }

    case 'complete': {
      const active = activeAppointment(state)
      if (!active) return state
      // 매칭 결과를 모르면 무엇을 주고받았는지도 모른다. 그때는 카드를 건드리지 않는다.
      const next = active.match ? consume(state, pairsOf(active.match)) : {}
      const appointments = state.appointments.filter((a) => a.exchangeId !== active.exchangeId)
      return {
        ...state,
        ...next,
        appointments,
        activeAppointmentId: null,
        outgoingPoke: null,
        declined: [],
        // 성사 이후 다른 약속이 없으면 자동 매칭을 다시 돌린다.
        autoMatching: appointments.length === 0,
      }
    }

    case 'cancel-appointment': {
      const appointments = state.appointments.filter(
        (a) => a.exchangeId !== state.activeAppointmentId,
      )
      return {
        ...state,
        appointments,
        activeAppointmentId: null,
        match: null,
        outgoingPoke: null,
        autoMatching: appointments.length === 0,
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
