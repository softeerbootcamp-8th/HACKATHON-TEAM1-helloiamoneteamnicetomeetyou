# 백엔드

Spring 백엔드에서 항상 지키는 규칙만 적는다. 루트 `CLAUDE.md` 를 먼저 읽고,
상세한 내용은 `.claude/skills/oneteam-development/references/backend.md` 를 본다.

## 현재 스택

- Java 21, Spring Boot 4.0.7, Gradle Wrapper
- 의존성은 `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`,
  `mysql-connector-j` 다
- DB 는 MySQL 이다. 배포 환경은 RDS, 로컬은 `docker compose up -d mysql` 로 띄운 컨테이너다.
  스키마는 `ddl-auto: update` 로 반영하고 마이그레이션 도구는 쓰지 않는다
- **Bean Validation 은 아직 없다.** 필요해지면 `build.gradle` 에 추가하고
  아래 검증 규칙을 그때부터 적용한다
- Redis 는 쓰지 않는다

## 구조

- 패키지는 도메인별로 나눈다.

```
com.helloiamoneteamnicetomeetyou.hackathon
├── global
│   ├── config
│   ├── common
│   ├── exception
│   └── util
└── domain
    └── member
        ├── controller
        ├── service
        ├── repository
        ├── dto
        └── entity
```

- `Controller → Service → Repository` 로 흐른다.
- Controller 는 요청을 받고 `@Valid` 를 붙이고 응답을 내보내는 것만 한다.
- Service 가 비즈니스 검증과 상태 변경, 트랜잭션을 담당한다.
- 트랜잭션은 Service 에서만 연다.
- 의존성은 생성자 주입으로 받는다.

## DTO

- Entity 를 요청이나 응답으로 그대로 쓰지 않는다.
- `record` 로 만들고 Request 와 Response 를 분리한다.
- 내용이 같아도 재사용하지 않고 따로 만든다.
- 이름은 `{도메인}{동작}RequestDto`, `{도메인}{동작}ResponseDto` 로 짓는다.
  예: `OrderCreateRequestDto`, `OrderCreateResponseDto`, `OrderDetailResponseDto`
- Entity 에서 DTO 로 바꾸는 것은 정적 팩토리 메서드 `from`, `of` 로 한다.
- Request DTO 에 `@NotNull`, `@Positive`, `@Size` 같은 검증 어노테이션을 붙인다.
  형식 검증은 DTO 에서, 비즈니스 규칙 검증은 Service 에서 한다.

## 예외와 응답

- 상위 예외 클래스 `ApplicationException` 하나를 두고 전부 그걸로 감싼다.
- 도메인별 `ErrorType` enum 에 status, code, message 를 모아둔다.
- `GlobalExceptionHandler` 에서 전역으로 응답 형식을 맞춘다.
- 로그는 slf4j 를 쓰고 예외는 ERROR 레벨로 남긴다.

응답 형식은 `.claude/skills/oneteam-development/references/contracts.md` 에 있다.

## 네이밍

자바 컨벤션을 따른다.

| 대상 | 규칙 | 예시 |
|---|---|---|
| 클래스, 인터페이스 | PascalCase | `OrderService` |
| 변수, 메서드 | camelCase | `addBookmark()` |
| 상수 | 대문자 스네이크 | `MAX_ORDER_COUNT` |
| boolean | `is` / `has` 접두어 | `isClosed`, `hasWinner()` |
| 컬렉션 | 복수형 | `orders`, `items` |
| REST URL | kebab-case, 복수 명사 | `/api/orders`, `/api/order-items` |
| DB 테이블·컬럼 | snake_case, 테이블은 복수 | `order_items` |

## 테스트

- JUnit 5 로 작성하고, 필요하면 Mockito 를 쓴다.
- 테스트 메서드명은 한글로 쓴다.
- **커버리지 숫자를 목표로 삼지 않는다.** 대신 금액 계산, 권한, 상태 전이처럼
  틀리면 바로 문제가 되는 곳은 반드시 테스트를 남긴다.
- 버그를 고칠 때는 그 버그를 재현하는 테스트를 먼저 하나 추가한다.

## 금지

- Controller 안의 비즈니스 로직과 트랜잭션
- `@Autowired` 필드 주입
- `System.out.println()`, `e.printStackTrace()`
- Entity 를 그대로 반환하기
- 예외를 잡아놓고 아무것도 안 하기
- 팀 확인 없는 API 형식, DB 스키마, 인증 방식 변경
- 요청받지 않은 대규모 리팩터링

## 검증

    ./gradlew test
    ./gradlew build

Gradle Wrapper 를 쓴다. 전역 `gradle` 명령을 쓰지 않는다.
로컬에서 컨테이너로 확인하려면 `docker compose up --build -d` 를 쓰고,
내릴 때는 `-v` 없이 `docker compose down` 만 한다.
