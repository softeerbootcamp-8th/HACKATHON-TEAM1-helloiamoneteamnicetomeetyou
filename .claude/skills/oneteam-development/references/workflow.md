# 협업과 안전

## Git

흐름은 `main ← dev ← 기능 브랜치` 다. 기능 브랜치는 항상 `dev` 에서 딴다.

| 항목 | 형식 | 예시 |
|---|---|---|
| 브랜치 | `{영역}/{타입}/{이슈번호}-{기능}` | `be/feat/10-order-create` |
| 커밋 | `타입(범위): 요약` | `feat(be): 주문 생성 API 구현` |
| 이슈 제목 | 커밋과 같은 형식 | `feat(be): 백엔드 초기 세팅` |
| PR 제목 | 커밋과 같은 형식 | `fix(fe): 로그인 후 리다이렉트 오류 수정` |

**커밋, 이슈, PR 제목이 모두 같은 형식이다.** Conventional Commits 를 따른다.
저장소에 올라온 이력이 그렇게 되어 있다.

- 브랜치의 영역은 소문자 `be` / `fe` 다. 양쪽에 걸치면 영역을 생략한다
  (`chore/14-claude-agent-setup`).
- 커밋의 범위는 `be` / `fe` / `cd` 다. 저장소 전체에 걸치면 범위를 생략한다
  (`refactor: 백엔드를 backend/ 로 옮겨 모노레포 구조로 전환`).
- 타입은 `feat` / `fix` / `refactor` / `chore` / `test` / `docs`
- 이슈를 먼저 만들고 그 번호로 브랜치를 딴다. PR 본문에서 `Closes #번호` 로 닫는다.
- PR base 는 `dev` 다. `main` 으로 직접 열지 않는다.
- CI 가 통과하고 팀원 1명 이상이 승인하면 머지한다.

**커밋과 PR 에 Claude 나 AI 도구 이름을 절대 남기지 않는다.**
`Co-Authored-By: Claude ...` 트레일러와 `🤖 Generated with Claude Code` 같은 줄을
커밋 메시지, PR 본문, 이슈 어디에도 넣지 않는다. author 는 실제 작업자다.

## 환경 변수

- 실제 값은 `.env` 에 두고 커밋하지 않는다. 팀 공유는 노션으로 한다.
- `.env.example` 에는 key 만 적는다. 값을 넣지 않는다.
- 프론트에서 읽는 값은 `VITE_` 로 시작해야 하고, 빌드 결과물에 그대로 들어간다.
  **백엔드 비밀값을 `VITE_` 로 넘기지 않는다.**
- 백엔드는 `application.yml` 안에서 프로필로 나눈다 (`e2e`, `prod`).
- 환경 변수를 새로 추가하면 `.env.example` 과 배포 설정(GitHub Actions secrets)을
  같이 확인한다.

## 로컬 실행

```bash
cd backend  && ./gradlew bootRun     # http://localhost:8080
cd frontend && pnpm dev              # http://localhost:5173
```

프론트 dev 서버가 `/api` 와 `/health` 를 8080 으로 프록시한다. CORS 설정을
따로 넣을 필요가 없다.

도커로 백엔드를 확인할 때는 `backend/docker-compose.yml` 하나만 쓴다.

```bash
cd backend && docker compose up --build -d
cd backend && docker compose down
```

`down` 에 `-v` 를 붙이지 않는다. 볼륨이 지워진다.

## CI/CD

- `.github/workflows/ci-frontend.yml`: PR 에서 `frontend/` 가 바뀌면
  `format:check`, `lint`, `build` 를 돌린다.
- `.github/workflows/deploy-backend.yml`: `main` 에 `backend/` 변경이 들어오면
  테스트를 돌리고 이미지를 만들어 EC2 에 배포한다.

**CI 실패를 우회하지 않는다.** 테스트를 지우거나 lint 규칙을 끄지 않는다.
실패하면 출력을 그대로 보고 원인을 고친다.

## 팀에 먼저 알릴 것

- 인증 방식과 토큰 저장 위치
- API 의 Method, Path, 응답 필드
- DB 스키마
- 새 의존성
- 환경 변수와 배포 설정

**확인을 기다리느라 멈추지 않는다.** 가정을 적어두고 나머지를 진행한다.

## 금지

- 비밀값, 토큰, 개인정보를 출력하거나 커밋하기
- `DROP`, `TRUNCATE`, 조건 없는 `DELETE` 와 `UPDATE`
- `docker compose down -v`
- `git push --force`, `git reset --hard`, `git clean`
- 테스트를 지우거나 검증을 약하게 해서 CI 통과시키기
- 요청받지 않은 리팩터링이나 전체 포맷 변경

위 셋째, 넷째 줄은 `.claude/hooks/block-forbidden-commands.sh` 가 실행 전에 막는다.

## 끝내고 보고할 때

1. 바꾼 파일과 무엇이 달라졌는지
2. API 형식이 바뀌었으면 그 사실
3. 돌린 검증과 결과
4. 확인하지 못한 것과 남은 위험

**4번을 비워두지 않는다.** 하지 않은 검증을 한 것처럼 쓰지 않는다.
