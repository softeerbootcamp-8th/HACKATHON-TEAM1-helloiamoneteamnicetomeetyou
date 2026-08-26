import type { Exchange } from '@/lib/exchange'

import { toAppointment } from './appointment'
import type { ExchangePair, MatchPartner, MatchResult } from './matching'
import { getPersistedSetupDone } from './setup-status'
import type { ActiveMatch, Appointment, Selection, State } from './types'

/**
 * 알림 문구는 시안의 `교환 대기장소 알림 정리` 를 그대로 옮긴 것이다.
 * 알림은 메인 텍스트만 바뀌고 보조 문구는 전부 "탭하여 확인" 이다.
 */
const NOTICE_BODY = '탭하여 확인'

export const initialState: State = {
  onboarded: false,
  boothId: null,
  // 기기가 전에 홈까지 가 본 적이 있으면 온보딩을 다시 보여주지 않는다.
  setupDone: getPersistedSetupDone(),
  have: [],
  needs: [],
  autoMatching: false,
  match: null,
  appointments: [],
  activeAppointmentId: null,
  notifications: [],
  collection: [],
  toast: null,
}

export type Action =
  | { type: 'onboarded' }
  | { type: 'toggle-have'; itemId: number }
  | { type: 'set-have-qty'; itemId: number; qty: number }
  | { type: 'toggle-need'; itemId: number }
  | { type: 'set-need-qty'; itemId: number; qty: number }
  | { type: 'clear-have'; itemId: number }
  | { type: 'clear-need'; itemId: number }
  | {
      /**
       * 서버에 이미 등록해 둔 카드를 화면으로 되살린다.
       *
       * 화면 상태는 새로고침에 통째로 사라지는데 서버 등록은 남아 있다. 되살리지 않으면
       * 등록 화면이 아무것도 안 고른 것처럼 뜨고, 그대로 "교환하러 가기" 를 누르면
       * 등록 동기화가 서버에 있던 카드를 전부 해제해 버린다.
       */
      type: 'registrations-loaded'
      have: Selection[]
      needs: Selection[]
    }
  | { type: 'enter-home' }
  | { type: 'server-match-arrived'; match: ActiveMatch }
  | { type: 'server-match-rejected'; exchangeId: number }
  | {
      /**
       * 서버에서 찔러보기가 성사됐다. 시안 `7. 찔러보기 성사` 를 세우는 자리다.
       */
      type: 'server-poke-matched'
      exchangeId: number
      /** 내가 상대에게 주는 카드 */
      giveItemId: number
      /** 내가 상대에게 받는 카드 */
      receiveItemId: number
      partnerUserId: string
      partnerName?: string
    }
  | { type: 'open-match' }
  | { type: 'decline-match' }
  | { type: 'booth-loaded'; boothId: number }
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

function bump(list: State['have'], itemId: number, delta: number): State['have'] {
  const found = list.find((s) => s.itemId === itemId)
  if (!found) return delta > 0 ? [...list, { itemId, qty: 1 }] : list
  const qty = found.qty + delta
  if (qty <= 0) return list.filter((s) => s.itemId !== itemId)
  return list.map((s) => (s.itemId === itemId ? { ...s, qty } : s))
}

function setQty(list: State['have'], itemId: number, qty: number): State['have'] {
  if (qty <= 0) return list.filter((s) => s.itemId !== itemId)
  if (!list.some((s) => s.itemId === itemId)) return [...list, { itemId, qty }]
  return list.map((s) => (s.itemId === itemId ? { ...s, qty } : s))
}

/** 서버에서 온 상대. 이름을 안 보낸 사용자는 "상대" 로 들어간다. */
function partnerOf(userId: string, name: string | undefined): MatchPartner {
  return { id: userId, nickname: name ?? '상대' }
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

    /**
     * 온보딩을 건너뛰고 곧바로 홈으로 온 사람의 카드를 서버에서 받아 채운다.
     *
     * `/have`, `/needs` 를 거쳐야만 `state.have`, `state.needs` 가 채워지는데, 이미 등록을
     * 마친 기기는 그 화면을 안 거치고 홈으로 바로 온다. 그대로 두면 서버에는 카드가 있는데
     * 화면에는 하나도 없는 것처럼 보인다.
     *
     * 이번 방문에 이미 고른 것이 있으면 건드리지 않는다. 서버 응답이 늦게 도착했을 때
     * 방금 고른 카드를 옛 등록으로 덮으면 사용자가 한 일이 사라진다.
     */
    case 'registrations-loaded': {
      if (state.have.length > 0 || state.needs.length > 0) return state
      return { ...state, have: action.have, needs: action.needs }
    }

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
        notifications: notify(state, 'match', '딱 맞는 교환 상대를 찾았어요! 🎯', NOTICE_BODY),
      }
    }

    /**
     * 서버 찔러보기 성사. 수락한 쪽은 응답을 받자마자, 보낸 쪽은 보낸 목록이 `ACCEPTED` 로
     * 바뀐 것을 보고 부른다.
     *
     * <b>`exchangeId` 로 `교환 장소 확인하기` 가 서버 교환을 수락하러 간다.</b>
     */
    case 'server-poke-matched': {
      // 같은 교환을 이미 세워 뒀으면 그대로 둔다. 목록을 다시 읽을 때마다 새 객체를
      // 만들면 화면이 매번 처음부터 다시 그려진다.
      if (state.match?.exchangeId === action.exchangeId) return state

      const match: ActiveMatch = {
        kind: 'ONE_TO_ONE',
        partner: partnerOf(action.partnerUserId, action.partnerName),
        pairs: [{ giveItemId: action.giveItemId, receiveItemId: action.receiveItemId }],
        giveItemId: action.giveItemId,
        receiveItemId: action.receiveItemId,
        origin: 'poke',
        exchangeId: action.exchangeId,
      }
      return { ...state, match, autoMatching: false }
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
        notifications: notify(state, 'match-rejected', '상대가 이번엔 패스했어요', NOTICE_BODY),
        toast: '상대가 패스했어요. 새 상대를 찾아드릴게요',
      }
    }

    case 'open-match':
      return state

    case 'decline-match': {
      if (!state.match) return state
      return {
        ...state,
        match: null,
        autoMatching: state.appointments.length === 0,
        toast: '이번 교환은 패스했어요. 새 상대를 찾아드릴게요',
      }
    }

    case 'booth-loaded':
      return { ...state, boothId: action.boothId }

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
          ? notify(state, 'time-matched', '만날 시간이 정해졌어요! ⏰', NOTICE_BODY)
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
        notifications: notify(state, 'time-request', '혹시… 다른 시간도 될까요?', NOTICE_BODY),
        toast: '상대에게 다른 시간을 물어봤어요',
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
        autoMatching: appointments.length === 0,
        toast: '약속을 취소했어요',
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
