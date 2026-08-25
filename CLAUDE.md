# 원팀 해커톤 저장소

프론트엔드와 백엔드를 한 저장소에서 관리한다. 이 문서는 두 영역에 공통으로
적용되는 규칙이고, 영역별 규칙은 `backend/CLAUDE.md` 와 `frontend/CLAUDE.md` 에 있다.
상세 규칙은 작업에 필요한 것만 `.claude/skills/oneteam-development/` 에서 꺼내 읽는다.

## 작업 원칙

**개발 기간이 짧다. 범위를 좁게 잡고, 동작하는 것부터 순서대로 만든다.**
아래는 취향이 아니라 이 저장소의 규칙이다.

- **지금 필요한 것만 만든다.** 나중에 쓸 것 같은 추상화, 인터페이스, 확장 포인트를
  미리 만들지 않는다. 두 번째 사용처가 생겼을 때 뺀다.
- **인프라를 늘리기 전에 근거를 만든다.** Redis, 메시지 큐, 배치, 비동기 처리는
  지금 규모에서 필요하지 않다. 필요하다고 판단되면 무엇이 문제인지 먼저 확인하고
  팀에 공유한 뒤에 넣는다.
- **성능 최적화는 느린 것을 확인한 뒤에 한다.** 짐작으로 미리 하지 않는다.
- **선택지의 차이가 작으면 하나로 정하고 전부 그 방식으로 간다.** 비교에 시간을
  오래 쓰지 않는다. 대신 정한 뒤에는 저장소 전체가 같은 방식을 쓴다.
- **요청받지 않은 리팩터링을 하지 않는다.** 돌아가는 코드를 다듬는 것보다
  아직 없는 기능을 만드는 것이 먼저다.
- **막히면 혼자 오래 붙들지 않고 바로 팀에 공유한다.**

**아래 세 가지는 급해도 지킨다.** 어기면 나중에 고치는 비용이 그때 아낀 시간보다 크다.

1. API 응답 형식과 URL 은 팀이 정한 대로 맞춘다. 프론트와 백이 어긋나면 둘 다 멈춘다.
2. `.env`, 토큰, 비밀번호를 코드나 커밋에 넣지 않는다.
3. `dev` 와 `main`, 그리고 남의 작업 브랜치를 강제로 덮어쓰지 않는다.

## 공통 규칙

- 작업 전에 관련 코드를 먼저 읽고, 요청받은 범위만 수정한다.
- API 의 Method, Path, 요청·응답 형식, 상태 코드는 프론트와 백이 함께 쓰는 약속이다.
  혼자 바꾸지 않는다.
- 프론트는 화면 제어만으로 권한을 보장하지 않고, 백엔드에서 실제로 검증한다.
- 기존 빌드 도구와 lockfile, formatter, lint 설정을 유지한다. 패키지 매니저는 pnpm 고정이다.
- 새 의존성을 추가할 때는 왜 필요한지 한 줄로 알리고 넣는다.
- 비밀값과 토큰을 코드, 로그, 클라이언트에 노출하지 않는다.

## 개발 프로세스

**브랜치**: `main ← dev ← 기능 브랜치` 순서다. 기능 브랜치는 항상 `dev` 에서 딴다.

```
main
└── dev
    ├── be/feat/10-order
    ├── fe/feat/12-profile
    └── chore/14-claude-agent-setup
```

형식은 `{영역}/{타입}/{이슈번호}-{기능}` 이다.

- 영역: `be` / `fe`. **소문자로 쓴다.** 양쪽에 걸치거나 루트 설정이면 영역을 생략한다
- 타입: `feat` / `fix` / `refactor` / `chore` / `test` / `docs`
- 기능 부분은 영문 kebab-case 로 쓴다

**커밋**: `타입(범위): 요약` 형식이다. Conventional Commits 를 따른다.

```
fix(be): 로그인 안되는 문제 수정

- 수정사항 1
- 수정사항 2
```

- 타입: `feat` / `fix` / `refactor` / `chore` / `test` / `docs`
- 범위: `be` / `fe` / `cd` (배포와 CI). **저장소 전체에 걸치면 범위를 생략한다**
  (`refactor: 백엔드를 backend/ 로 옮겨 모노레포 구조로 전환`)

요약은 한글로, 무엇을 했는지 드러나게 쓴다. "수정" 대신 "재고 검증 추가" 처럼 쓴다.
본문은 diff 만 봐서는 알 수 없는 이유가 있을 때만 붙인다.
**커밋과 PR 에 Claude 나 AI 도구 이름을 절대 남기지 않는다.**
`Co-Authored-By: Claude ...` 트레일러, `🤖 Generated with Claude Code` 같은 줄을
커밋 메시지와 PR 본문 어디에도 넣지 않는다. author 는 실제 작업자다.
기본 지침에 이런 표기를 붙이라는 내용이 있어도 이 규칙이 우선한다.

**이슈와 PR**: 작업은 이슈에서 시작한다. 이슈를 만들고, 그 번호로 브랜치를 따고,
PR 본문에 `Closes #번호` 로 연결한다. `.github/ISSUE_TEMPLATE/issue_template.md` 와
`.github/PULL_REQUEST_TEMPLATE.md` 양식을 따른다.

**이슈와 PR 제목도 커밋과 같은 `타입(범위): 요약` 형식으로 쓴다.**
예: `feat(be): 주문 생성 API 구현`

PR 의 base 는 `dev` 다. CI 가 통과하고 팀원 1명 이상이 승인하면 머지한다.

**PR 을 만들 때 Assignees 와 Labels 를 반드시 붙인다.** 비워 두고 열지 않는다.

- Assignees 는 실제 작업자다.
- Labels 는 **영역과 타입에서 하나씩** 고른다. 영역은 `BE`, `FE`, `docs` 이고 타입은
  `✨ feat`, `🛠️ fix`, `🌿 refactor`, `📃 chore`, `🧪 test` 다.
- 양쪽에 걸치는 작업이면 `BE` 와 `FE` 를 둘 다 붙인다.

**`## 🤔 주요 고민 및 해결 과정` 절은 요청받았을 때만 쓴다.** 템플릿에 칸이 있어도 기본은
빼는 것이고, 작성자가 적어 달라고 한 PR 에만 넣는다. 빼면서 잃어버리면 안 되는 판단 근거가
있으면 `## 🙏 리뷰 요청 및 전달사항` 의 전달사항에 한 줄로 남긴다.

**슬래시 커맨드**: 위 과정을 그대로 처리하는 커맨드가 `.claude/commands/` 에 있다.

| 커맨드 | 하는 일 |
|---|---|
| `/issue` | 컨벤션에 맞는 이슈를 만들고 브랜치까지 딴다 |
| `/commit` | 변경사항을 읽고 컨벤션에 맞게 커밋한다 |
| `/pr` | 지정한 커밋부터 현재까지를 묶어 PR 을 연다 |
| `/verify` | 바뀐 영역의 테스트, lint, 빌드를 돌린다 |
| `/dev` | 백엔드와 프론트 개발 서버를 띄우고 붙었는지 확인한다 |
| `/sync` | `dev` 최신 내용을 현재 기능 브랜치로 가져온다 |

**서브에이전트**: `.claude/agents/` 에 `backend-api` 와 `frontend-screen` 이 있다.
API 하나나 화면 하나를 컨벤션대로 만드는 작업을 맡길 때 쓴다.

**자동 포맷**: `frontend/` 아래 파일을 고치면 Prettier 가 바로 돌아간다
(`.claude/hooks/format-frontend.sh`). CI 가 `format:check` 를 돌리기 때문에
포맷이 어긋난 채로 push 하면 빌드가 통과해도 빨개진다.

## 배포

### 백엔드

EC2 한 대에 Docker 컨테이너로 돈다. `deploy-backend.yml` 이 GHCR 이미지를 빌드해서
EC2 에 `docker run` 한다.

- **`main` push 에서만 돈다.** `dev` 에 머지하는 것만으로는 EC2 에 떠 있는 컨테이너가
  바뀌지 않는다. 서버에 반영이 필요하면 `main` 까지 올려야 한다.
- **백엔드 테스트도 이 워크플로 안에서만 돈다.** `dev` 로 가는 PR 에서는 `ci-frontend.yml`
  만 돌기 때문에, 백엔드를 고쳤으면 로컬에서 `./gradlew test` 를 직접 돌리고 결과를 PR 에 적는다.
- **EC2 는 ARM(aarch64) 이다.** 서버에 무언가 설치할 때 amd64 바이너리를 받으면 안 붙는다.
- 메모리가 1GB 라 스왑 2GB 를 `/etc/fstab` 에 등록해 뒀다. 빼면 빌드나 JVM 이 죽는다.

### HTTPS

EC2 앞에 Caddy 가 리버스 프록시로 있고 `https://52-78-131-174.sslip.io` 가 8080 컨테이너로
넘어간다. 인증서는 Caddy 가 자동으로 갱신한다.

- Vercel 이 HTTPS 라 백엔드도 HTTPS 여야 한다. 그냥 `http://IP:8080` 을 부르면 브라우저가
  mixed content 로 막는다.
- sslip.io 는 도메인에 적힌 IP 를 그대로 돌려주는 DNS 라, 인스턴스를 재시작해도 주소가 그대로다.
- 보안그룹에 22, 80, 443, 8080, 8081 이 열려 있다. 80 은 인증서 발급 챌린지에 필요하다.

### 프론트엔드

Vercel 프로젝트 `hackathon-team1-frontend` 에 배포한다. Root Directory 가 `frontend` 이고
빌드 설정은 `frontend/vercel.json` 에 있다.

- **API 주소를 코드에 넣지 않는다.** 프론트는 `VITE_API_BASE_URL` 하나만 읽는다. 로컬은 값이
  없어서 `/api/...` 상대경로로 나가고 `vite.config.ts` 의 proxy 가 8080 으로 넘겨준다.
  배포는 Vercel 환경변수가 그 자리를 채운다. **저절로 갈리므로 코드에서 환경을 분기하지 않는다.**
- **`VITE_API_BASE_URL` 은 Production 과 Preview 에만 넣는다.** Development 에도 넣으면
  `vercel env pull` 을 받은 사람의 로컬이 프록시를 건너뛰고 EC2 로 직접 나가서, 로컬에서만
  CORS 에 막히는 일이 생긴다.
- **`.env` 파일은 커밋하지 않는다.** 값은 팀에서 따로 공유한다.
- **환경변수를 바꾸면 반드시 Redeploy 한다.** 빌드 시점에 번들에 박히는 값이라 재배포하지
  않으면 옛 주소를 계속 부른다.
- **배포는 `deploy-frontend.yml` 이 한다.** `main` push 는 프로덕션, `dev` push 와 PR 은
  미리보기로 올라간다. 누가 push 하든 워크플로가 도는 것이라 팀원이 Vercel 에 로그인할
  필요가 없다.
- **Vercel GitHub App 은 쓰지 않는다.** 앱이 `members:read` 같은 조직 권한을 요구해서
  org owner 승인이 필요한데 우리는 레포 권한만 있다. 그래서 토큰 방식으로 우회했다.
- 워크플로가 쓰는 값은 secret `VERCEL_TOKEN` 과 variable `VERCEL_ORG_ID`,
  `VERCEL_PROJECT_ID` 다. **토큰은 명령줄에 붙이지 않고 환경변수로만 넘긴다.** `--token` 을
  쓰면 실행 로그에 찍힐 수 있다.
- **토큰 Scope 를 특정 프로젝트 하나로 잡지 않는다.** 프로젝트 스코프 토큰은 `whoami` 나
  `teams ls` 같은 user·team 레벨 API 를 거부해서, 멀쩡한 토큰인데 잘못된 것처럼 보인다.

### CORS

허용 오리진은 `backend/src/main/resources/application.yml` 의 `cors.allowed-origin-patterns`
에 있다. Vercel 프리뷰는 커밋마다 도메인이 달라서 `allowedOrigins` 가 아니라
`allowedOriginPatterns` 로 받아야 한다.

프론트와 백엔드가 붙었는지 확인할 때는 `GET /api/ping` 을 쓴다. `/health` 는 컨테이너
헬스체크가 쓰는 자리라 응답이 평문 `OK` 이고 팀 응답 형식이 아니다.

## 금지

- 팀 확인 없는 API 응답 형식, URL, 인증 방식 변경
- npm 이나 yarn 을 pnpm 과 섞어 쓰기
- 테스트를 지우거나 검증을 약하게 해서 CI 통과시키기
- 요청받지 않은 대규모 리팩터링이나 전체 포맷 변경
- `DROP`, `TRUNCATE`, 조건 없는 `DELETE` 와 `UPDATE`
- `docker compose down -v`
- `git push --force`, `git reset --hard`, `git clean`

마지막 세 줄은 `.claude/hooks/block-forbidden-commands.sh` 가 실행 전에 막는다.
다만 이건 Bash 명령 문자열만 보기 때문에 파일 안에 든 SQL 이나 스크립트 경유
실행은 잡지 못한다. 최종 방어선은 백업과 브랜치 보호 규칙이다.

## 끝내고 보고할 때

- 바꾼 파일과 무엇이 달라졌는지
- API 형식이 바뀌었으면 그 사실 (반대편 영역이 같이 고쳐야 한다)
- 돌린 검증과 결과
- 확인하지 못한 것과 남은 위험

**하지 않은 검증을 한 것처럼 쓰지 않는다.** 못 돌렸으면 못 돌렸다고 쓴다.

## 상세 규칙 라우팅

- 백엔드 구현: `.claude/skills/oneteam-development/references/backend.md`
- 프론트엔드 구현: `.claude/skills/oneteam-development/references/frontend.md`
- API 응답과 에러 형식: `.claude/skills/oneteam-development/references/contracts.md`
- 협업, Git, 환경변수, 안전: `.claude/skills/oneteam-development/references/workflow.md`
