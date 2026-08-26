import type { Item } from '@/mocks/data'

import type { ServerItem } from './api'

/**
 * 목업 카드와 서버 카드를 **이름으로** 잇는다.
 *
 * 왜 이름인가: 목업은 카드 id 가 문자열(`'sf'`)이고 서버는 서버가 매긴 숫자다. 둘을 잇는
 * 공통 열쇠가 이름밖에 없다. 목업 id 를 서버 id 로 갈아 끼우면 저장되는 값이 바뀌면서
 * 대기자·매칭·레이더·찔러보기가 전부 목업 id 를 쓰는 지금 상태에서 다 같이 깨진다. 그래서
 * 화면과 저장소는 목업 id 로 두고, **서버로 나갈 때만** 여기서 바꿔 준다.
 *
 * 매칭 알고리즘(#20)과 목록 조회(#34)가 들어와 목업을 걷어낼 때 이 파일도 같이 사라진다.
 */

/** 영문 이름과 한글 이름 중 어느 쪽으로 넣었든 걸리게 한다. 공백과 대소문자는 무시한다. */
function normalize(name: string): string {
  return name.replace(/\s+/g, '').toLowerCase()
}

export type NameMatch = {
  /** 목업 카드 id → 서버 카드 id */
  serverIdOf: (mockItemId: string) => number | undefined
  /** 서버 카드 id → 목업 카드 id. 매칭 알림처럼 서버 id 로 오는 걸 화면에 그릴 때 쓴다. */
  mockIdOf: (serverItemId: number) => string | undefined
  /**
   * 서버 카드 id → 목업 카드. 서버에서 받은 것을 화면에 그릴 때 쓴다.
   *
   * 찔러보기는 상대의 카드와 내 묶음을 서버에서 받아 오는데, 카드 그림과 약칭·한글 이름은
   * 목업 쪽에만 있다. 짝을 못 찾으면 `undefined` 라서 부르는 쪽이 그 줄을 건너뛰거나
   * 이름만이라도 보여 줄 수 있다.
   */
  mockItemOf: (serverItemId: number) => Item | undefined
  /** 서버에서 짝을 찾지 못한 목업 카드들. 화면이 이유를 설명하는 데 쓴다. */
  unmatched: Item[]
}

/**
 * 목업 카드 전체를 서버 카드 목록에 대조한다.
 *
 * 짝이 없는 카드를 조용히 버리지 않고 `unmatched` 로 돌려준다. 어드민에 카드를 아직 안
 * 넣었거나 이름을 다르게 넣은 경우인데, 말해 주지 않으면 "등록했다고 나오는데 서버에는 없다"
 * 가 된다.
 */
export function matchByName(mockItems: Item[], serverItems: ServerItem[]): NameMatch {
  const byName = new Map<string, number>()
  for (const server of serverItems) {
    byName.set(normalize(server.name), server.id)
  }

  const resolved = new Map<string, number>()
  const reversed = new Map<number, Item>()
  const unmatched: Item[] = []

  for (const mock of mockItems) {
    const found = byName.get(normalize(mock.name)) ?? byName.get(normalize(mock.nameKo))
    if (found === undefined) {
      unmatched.push(mock)
      continue
    }
    resolved.set(mock.id, found)
    reversed.set(found, mock)
  }

  const byServerId = new Map<number, string>()
  for (const [mockId, serverId] of resolved) byServerId.set(serverId, mockId)

  return {
    serverIdOf: (mockItemId) => resolved.get(mockItemId),
    mockIdOf: (serverItemId) => byServerId.get(serverItemId),
    mockItemOf: (serverItemId) => reversed.get(serverItemId),
    unmatched,
  }
}
