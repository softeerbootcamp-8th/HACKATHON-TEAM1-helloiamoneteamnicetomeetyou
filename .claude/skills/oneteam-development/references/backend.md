# Spring 백엔드

`backend/CLAUDE.md` 에 있는 상시 규칙 위에, 실제로 코드를 쓸 때 판단이 필요한
것들만 적는다.

## 지금 들어와 있는 것

`build.gradle` 에 `spring-boot-starter-webmvc` 하나만 있다. JPA, MySQL,
Bean Validation, Lombok 모두 없다. 아래 규칙 중 JPA 나 검증 어노테이션이
나오는 부분은 그 의존성을 추가한 시점부터 적용한다.

의존성을 추가할 때는 `build.gradle` 에 넣고 왜 필요한지 한 줄로 알린다.
**Redis, 메시지 큐, 배치 스케줄러는 지금 규모에서 필요하지 않다.** 넣어야 한다고
판단되면 무엇이 문제인지 먼저 확인하고 팀에 공유한다.

## 레이어

```
Controller → Service → Repository
```

- Controller: 요청 매핑, `@Valid`, 응답 반환. 여기에 if 문으로 비즈니스 규칙을
  넣기 시작하면 테스트가 불가능해진다.
- Service: 권한 확인, 상태 전이, 비즈니스 검증, `@Transactional`
- Repository: 영속성 접근만

트랜잭션은 Service 에서만 연다. Controller 나 Repository 에 `@Transactional` 을
붙이지 않는다.

## DTO

- `record` 로 만든다.
- Request 와 Response 를 분리한다. 필드가 같아도 합치지 않는다. 한쪽이 바뀔 때
  다른 쪽이 같이 끌려가면 더 느려진다.
- 이름은 `{도메인}{동작}RequestDto` / `{도메인}{동작}ResponseDto`.
- Entity → DTO 변환은 정적 팩토리 `from`, `of` 로 한다.

```java
public record OrderDetailResponseDto(Long id, String name, int price) {
    public static OrderDetailResponseDto from(Order order) {
        return new OrderDetailResponseDto(order.getId(), order.getName(), order.getPrice());
    }
}
```

- Entity 를 요청이나 응답으로 그대로 쓰지 않는다. 필드가 늘어날 때마다
  API 응답이 조용히 바뀐다.

## 검증

- 형식 검증(null, 길이, 범위, 이메일 형식)은 Request DTO 에 어노테이션으로 붙인다.
- 비즈니스 규칙(재고가 남았는가, 내 주문이 맞는가)은 Service 에서 확인한다.
- Controller 파라미터에 `@Valid` 를 빠뜨리면 어노테이션이 아무 일도 하지 않는다.

## 예외

- 던지는 예외는 전부 `ApplicationException` 을 통한다.
- `ErrorType` enum 에 status, code, message 를 담는다.
- `GlobalExceptionHandler` 에서 `@RestControllerAdvice` 로 받아 응답 형식을 맞춘다.
- 잡아놓고 아무것도 안 하는 catch 블록을 만들지 않는다.
- 로그는 slf4j 로 남긴다. 예외는 ERROR 레벨이다.

## JPA (도입한 뒤)

- 연관관계는 기본 LAZY 로 둔다. N+1 이 보이면 EAGER 로 덮지 말고 fetch join 이나
  전용 조회 DTO 로 푼다.
- 양방향 연관관계와 `CascadeType.ALL` 은 꼭 필요할 때만 쓴다.
- `spring.jpa.open-in-view=false` 로 두고, Lazy 로딩을 Controller 까지 넘기지 않는다.
- 해커톤 기간에는 `ddl-auto: update` 로 두고 마이그레이션 도구를 넣지 않는다.
  대신 스키마를 바꾸면 팀에 알린다.

## 테스트

- JUnit 5, 필요하면 Mockito.
- 테스트 메서드명은 한글로 쓴다.

```java
@Test
void 재고가_없으면_주문에_실패한다() { ... }
```

- **커버리지 숫자를 목표로 삼지 않는다.** 대신 금액 계산, 권한, 상태 전이처럼
  틀리면 바로 문제가 되는 곳은 반드시 테스트를 남긴다.
- 버그를 고칠 때는 그 버그를 재현하는 테스트를 먼저 하나 추가한다.
- DB 를 띄우는 통합 테스트는 이번 기간의 범위 밖이다. Service 를 단위 테스트로
  검증하고, 실제 연동은 로컬에서 직접 실행해 확인한다.

## 하지 말 것

- `@Autowired` 필드 주입 (생성자 주입을 쓴다)
- `System.out.println()`, `e.printStackTrace()`
- Controller 에 들어간 비즈니스 로직
- Entity 직접 반환
- 근거 없는 캐시, 비동기, 스레드 풀 추가
