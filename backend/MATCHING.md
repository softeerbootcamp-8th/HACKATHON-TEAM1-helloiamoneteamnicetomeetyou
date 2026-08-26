# 매칭 서비스 흐름 정리

## 전체 흐름

```
사용자가 보유/희망 아이템 등록
        ↓
runMatching() 비동기 호출 (@Async)
        ↓
  ┌─ 쿼리 A: 내 아이템을 원하는 후보 조회 → toThem
  └─ 쿼리 B: 내가 원하는 아이템을 가진 후보 조회 → toMe
        ↓
  tryOneToOne()
    → toThem ∩ toMe = 양방향 교환이 가능한 후보
    → 후보 있으면: score 기반으로 최적 상대 선정 → Exchange 생성
    → 후보 없으면: Optional.empty()
        ↓ (1대1 실패 시 폴백)
  tryThreeWay()
    ┌─ 쿼리 C: B → C 방향 교환 가능 여부 조회
    └─ A→B→C→A 사이클 존재하면 Exchange 생성
        ↓
   SSE로 매칭 결과 전송 (미구현)
```

---

## 쿼리 A — `findToThemData`

> **"내 보유 아이템을 원하는 사람이 누구고, 얼마나 줄 수 있나?"**

```sql
SELECT uwi.user_id,                                        -- 후보 ID (A가 줄 수 있는 상대)
       uwi.item_id,                                        -- 교환할 아이템
       LEAST(my_uhi.quantity_left, uwi.quantity) AS qty,   -- 실제 교환 가능 수량
       uwi.id AS want_id                                   -- 등록 순서 tiebreaker용
FROM user_want_items uwi          -- 다른 사람들의 희망 목록
JOIN user_have_items my_uhi       -- 나의 보유 목록
    ON my_uhi.item_id = uwi.item_id   -- 같은 아이템일 때만 JOIN
   AND my_uhi.user_id = :myUserId
   AND my_uhi.status = 'LEFT'
   AND my_uhi.quantity_left > 0
WHERE uwi.user_id != :myUserId    -- 나 자신 제외
```

### 예시

내가 `피카츄 5개`, `파이리 2개` 보유. B가 `피카츄 3개`, `파이리 1개` 원함. C가 `피카츄 10개` 원함.

JOIN은 `"내 보유 목록에 있는 아이템을 원하는 사람"` 조건이므로:

| uwi.user_id | uwi.item_id | my_uhi.quantity_left | uwi.quantity | LEAST = qty | want_id |
|---|---|---|---|---|---|
| B | 피카츄 | 5 | 3 | **3** | 101 |
| B | 파이리 | 2 | 1 | **1** | 102 |
| C | 피카츄 | 5 | 10 | **5** | 103 |

**LEAST의 역할**: 내가 5개 있어도 상대가 3개만 원하면 3개. 상대가 10개 원해도 내가 5개밖에 없으면 5개. 실제 교환 가능한 수량을 SQL에서 바로 계산한다.

**Java 조립 결과 (buildToThem)**

```java
// result: candidateId → { itemId → qty }
toThem = {
  B: { 피카츄: 3, 파이리: 1 },
  C: { 피카츄: 5 }
}

// earliestReg: 후보별 가장 작은 want_id (먼저 등록한 순서)
earliestReg = { B: 101, C: 103 }
```

```java
private Map<Long, Map<Long, Integer>> buildToThem(Long myUserId, Map<Long, Long> earliestReg) {
    Map<Long, Map<Long, Integer>> result = new HashMap<>();
    for (Object[] row : userWantItemRepository.findToThemData(myUserId)) {
        Long candidateId = toLong(row[0]);  // uwi.user_id
        Long itemId      = toLong(row[1]);  // uwi.item_id
        int  qty         = toInt(row[2]);   // LEAST(...)
        Long wantId      = toLong(row[3]);  // uwi.id
        result.computeIfAbsent(candidateId, k -> new HashMap<>()).put(itemId, qty);
        earliestReg.merge(candidateId, wantId, Math::min); // 후보별 최솟값 유지
    }
    return result;
}
```

---

## 쿼리 B — `findToMeData`

> **"내가 원하는 아이템을 가진 사람이 누구고, 얼마나 받을 수 있나?"**

```sql
SELECT uhi.user_id,                                        -- 후보 ID (나에게 줄 수 있는 상대)
       uhi.item_id,                                        -- 교환할 아이템
       LEAST(uhi.quantity_left, my_uwi.quantity) AS qty    -- 실제 교환 가능 수량
FROM user_have_items uhi          -- 다른 사람들의 보유 목록
JOIN user_want_items my_uwi       -- 나의 희망 목록
    ON my_uwi.item_id = uhi.item_id   -- 같은 아이템일 때만 JOIN
   AND my_uwi.user_id = :myUserId
WHERE uhi.user_id != :myUserId    -- 나 자신 제외
  AND uhi.status = 'LEFT'
  AND uhi.quantity_left > 0
```

### 예시

내가 `뮤츠 2개`, `잠만보 3개` 원함. B가 `뮤츠 1개` 보유. C가 `뮤츠 5개`, `잠만보 2개` 보유.

JOIN은 `"내 희망 목록에 있는 아이템을 보유한 사람"` 조건이므로:

| uhi.user_id | uhi.item_id | uhi.quantity_left | my_uwi.quantity | LEAST = qty |
|---|---|---|---|---|
| B | 뮤츠 | 1 | 2 | **1** |
| C | 뮤츠 | 5 | 2 | **2** |
| C | 잠만보 | 2 | 3 | **2** |

**Java 조립 결과 (buildToMe)**

```java
// result: candidateId → { itemId → qty }
toMe = {
  B: { 뮤츠: 1 },
  C: { 뮤츠: 2, 잠만보: 2 }
}
```

---

## 두 쿼리 결과로 교집합 → 후보 확정

```java
// toThem: 내 아이템을 원하는 사람들
// toMe:   내가 원하는 아이템을 가진 사람들
// 교집합: 두 조건을 동시에 만족하는 사람 = 실제 교환 가능 후보

Set<Long> candidates = toThem.keySet().stream()
        .filter(toMe::containsKey)
        .collect(Collectors.toSet());
```

```
toThem = { B, C }
toMe   = { B, C }
교집합  = { B, C }  ← 양쪽 모두에 있는 사람만
```

---

## 최적 상대 선정 (selectBest)

```java
score(후보) = min(내가 줄 총량, 내가 받을 총량)
```

| 후보 | toThem 합계 | toMe 합계 | score |
|---|---|---|---|
| B | 3+1 = 4 | 1 | **1** |
| C | 5 | 2+2 = 4 | **4** |

→ **C 선정**

동점이면 `earliestReg` 작은 쪽 (want를 먼저 등록한 사람) 우선.

---

## 교환 생성 (createExchange)

```java
// 양쪽 총합을 맞춰 대칭 교환 보장
exchangeQty = min(toThem[C] 합계, toMe[C] 합계) = min(5, 4) = 4

actualGive    = capTo({피카츄: 5}, 4)            → {피카츄: 4}
actualReceive = capTo({뮤츠: 2, 잠만보: 2}, 4)   → {뮤츠: 2, 잠만보: 2}
```

```
Exchange 생성 (ONE_TO_ONE)
ExchangeParticipant: A, C

ExchangeItem:
  A → C: 피카츄 4개
  C → A: 뮤츠 2개
  C → A: 잠만보 2개

quantityLeft 감소 (dirty checking으로 자동 반영):
  A의 피카츄: 5 → 1
  C의 뮤츠:   5 → 3
  C의 잠만보: 2 → 0 (status = OUT)
```

---

## 3인 교환 (tryThreeWay)

1대1 교집합이 없을 때만 실행. **쿼리 A, B 결과를 그대로 재사용**한다.

```java
// 쿼리 C: B(∈ toThem) → C(∈ toMe) 방향으로 줄 수 있는 아이템 탐색
Map<Long, Map<Long, Map<Long, Integer>>> bToC = buildBToC(toThem.keySet(), toMe.keySet());

// B 를 want 등록이 빠른 순으로 훑으면서, 사이클이 실제로 성립하는 첫 (B, C) 를 쓴다.
// C 가 나에게 줄 카드가 내가 B 에게 주는 카드뿐이면(한 바퀴 돌아 도로 받는 꼴) 그 조합만
// 건너뛰고 다음 조합을 본다. 예전에는 첫 조합 하나만 보고 부스를 통째로 포기했다.
Long bId = ...;
Long cId = ...;

// 각 방향에서 아이템 1개씩
ExchangeItem A → B: 아이템 1개
ExchangeItem B → C: 아이템 1개
ExchangeItem C → A: 아이템 1개
```

---

## 쿼리 횟수 요약

| 단계 | 쿼리 | 실행 조건 |
|---|---|---|
| 쿼리 A `findToThemData` | 항상 | |
| 쿼리 B `findToMeData` | 항상 | |
| 쿼리 C `findBToCData` | 3인 폴백 시에만 | |
| 쿼리 D `findByUserIdAndItemIds` | 교환 생성 시 상대 엔티티 로드 | |
| 쿼리 E `findById` (User) | 교환 생성 시 상대 User 로드 | |

- **1대1 성공**: A + B + D + E = 4쿼리
- **3인 폴백**: A + B + C + D×2 + E×2 = 7쿼리
