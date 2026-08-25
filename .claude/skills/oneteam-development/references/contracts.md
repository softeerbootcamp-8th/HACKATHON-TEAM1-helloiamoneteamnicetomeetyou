# API 규칙

프론트와 백이 함께 쓰는 약속이다. 여기 있는 것을 혼자 바꾸면 반대편이 멈춘다.

## URL

- prefix 는 `/api` 다. 버전은 붙이지 않는다.
- kebab-case 에 복수 명사를 쓴다. `/api/orders`, `/api/order-items`
- 리소스 하나는 `/api/orders/{orderId}` 처럼 경로 변수로 받는다.

프론트는 항상 상대경로로 호출한다. dev 서버(`vite.config.ts`)가 `/api` 와
`/health` 를 8080 으로 프록시하므로 오리진을 코드에 넣을 일이 없다.

## 응답 형식

모든 응답은 `success` 로 시작한다.

데이터가 있는 성공 응답.

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "홍길동"
  }
}
```

데이터가 없는 성공 응답(생성, 삭제 등)은 `data` 를 `{}` 로 두거나 빼도 된다.
`@JsonInclude(NON_NULL)` 을 붙여 두면 null 필드가 응답에서 빠진다.

실패 응답.

```json
{
  "success": false,
  "code": 4001,
  "message": "주문을 찾을 수 없습니다."
}
```

- `code` 는 HTTP status 와 별개인 팀 내부 코드다. 도메인별로 앞자리를 나눠 쓴다.
- `message` 는 사용자에게 그대로 보여줘도 되는 한글 문장으로 쓴다.
- HTTP status 는 실제 의미에 맞춘다. 없으면 404, 권한 없으면 403, 잘못된 요청은 400.

Validation 실패는 어느 필드가 틀렸는지 같이 내려준다.

```json
{
  "success": false,
  "code": 4000,
  "message": "잘못된 요청입니다.",
  "errors": [
    { "field": "name", "message": "이름은 필수입니다." }
  ]
}
```

## 목록 응답

```json
{
  "success": true,
  "data": {
    "content": [],
    "nextCursor": null,
    "hasNext": false,
    "size": 11
  }
}
```

- 조회 파라미터는 offset 방식이다. `page` 기본 0, `size` 기본 20.
- `size` 는 요청한 크기가 아니라 실제로 담긴 `content` 의 개수다.
- `hasNext` 로 다음 페이지가 있는지 알린다.
- `nextCursor` 는 커서 방식으로 바꿀 때를 위해 자리만 잡아둔 것이라, offset 으로
  쓰는 동안에는 `null` 로 둔다. 무한 스크롤이 필요해지면 그때 팀에서 정한다.

## 에러 코드

`ErrorType` enum 하나에 도메인별로 모아둔다. 셋을 함께 들고 있는다.

| 항목 | 예시 |
|---|---|
| status | 404 |
| code | 4001 |
| message | "주문을 찾을 수 없습니다." |

새 에러를 만들 때는 이미 있는 것을 먼저 찾아본다. 비슷한 게 있으면 그걸 쓴다.

## 바꿔야 할 때

1. 프론트와 백 중 어느 쪽 코드가 영향을 받는지 찾는다.
2. 반대편 담당자에게 먼저 알린다. 해커톤이라 배포 순서까지 맞출 필요는 없지만,
   상대가 모르는 채로 바뀌면 원인을 찾느라 시간이 두 배로 든다.
3. 서버와 클라이언트를 같이 고친다.
