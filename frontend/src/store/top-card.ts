/** 카드 한 종류와 그 수량. 화면의 선택 목록도, 서버 등록 목록도 이 모양으로 맞춰서 넘긴다. */
export type CardCount = { itemId: number; qty: number }

/**
 * 카드 묶음을 늘어놓는 순서. 많이 들고 있는 카드가 앞이고, 같으면 카드 id 가 작은 쪽이 앞이다.
 *
 * <b>기준이 무엇인지보다 흔들리지 않는다는 것이 중요하다.</b> 같은 목록을 받은 두 화면은
 * 반드시 같은 카드를 앞에 세워야 하고, 목록이 바뀌어도 건드리지 않은 카드의 자리는 그대로여야
 * 한다. 수량이 같을 때 id 로 한 번 더 가르는 것이 그래서다. 배열에 들어온 순서에 맡기면
 * 서버가 목록 순서를 바꾸는 것만으로 대표 카드가 달라진다.
 */
export function byPresence(a: CardCount, b: CardCount): number {
  return b.qty - a.qty || a.itemId - b.itemId
}

/**
 * 묶음 맨 위에 보이는 카드. 가진 카드가 없으면 `null` 이고, 가지고 있지도 않은 카드를 세우지
 * 않는다.
 *
 * <b>난수로 고르지 않는다.</b> 전에는 앱이 뜰 때 뽑아 둔 난수로 배열 인덱스를 골랐는데,
 * 인덱스가 목록 길이에 걸려 있어서 카드를 하나 넣거나 빼면 건드리지도 않은 다른 카드가 대표로
 * 올라왔다. 편집 화면에서 고른 카드와 찔러보기 화면에 뜨는 카드가 다르던 이유가 이것이다.
 */
export function topItemIdOf(cards: CardCount[]): number | null {
  return cards.length === 0 ? null : [...cards].sort(byPresence)[0].itemId
}
