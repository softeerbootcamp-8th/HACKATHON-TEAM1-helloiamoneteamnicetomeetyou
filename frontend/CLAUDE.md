# 프론트엔드

프론트엔드에서 항상 지키는 규칙만 적는다. 루트 `CLAUDE.md` 를 먼저 읽고,
상세한 내용은 `.claude/skills/oneteam-development/references/frontend.md` 를 본다.

## 현재 스택

- Vite 8 + React 19 + TypeScript (strict)
- Tailwind CSS v4 (`@tailwindcss/vite` 플러그인, CSS-first)
- ESLint + Prettier (포맷 규칙은 전부 Prettier 가 맡는다)
- 패키지 매니저는 **pnpm 고정**이다. Node 22 이상이 필요하다.
- **라우터와 서버 상태 라이브러리는 아직 없다.** 필요해지면 팀에 알리고 넣는다.

## 명령어

| 명령                | 하는 일                                                       |
| ------------------- | ------------------------------------------------------------- |
| `pnpm dev`          | 개발 서버 (5173). `/api` 와 `/health` 는 8080 으로 프록시된다 |
| `pnpm build`        | `tsc -b` 타입 검사 후 `dist/` 로 빌드                         |
| `pnpm lint`         | ESLint (`pnpm lint:fix` 로 자동 수정)                         |
| `pnpm format`       | Prettier 로 정리                                              |
| `pnpm format:check` | Prettier 검사만. **CI 가 이걸 돌린다**                        |

### push 전에 돌릴 것

`.github/workflows/ci-frontend.yml` 이 돌리는 것과 같다.

```bash
pnpm format:check
pnpm lint
pnpm build
```

`format:check` 를 빠뜨려서 CI 가 깨지는 일이 잦다. 특히 편집 도구가 아니라
`sed` 같은 스크립트로 파일을 고쳤으면 포맷이 거의 확실히 어긋나므로
`pnpm format` 을 먼저 돌린다.

**`frontend/` 아래의 `.md` 파일도 Prettier 검사 대상이다.** 이 문서를 포함해서
표 정렬이 어긋나면 CI 가 빨개진다. `.prettierignore` 에 있는 `dist`,
`node_modules`, `pnpm-lock.yaml` 만 예외다.

## 폴더와 import

```
src/
├── lib/        api 클라이언트 등 공용 유틸
├── components/ 재사용 UI
├── features/   도메인별 화면 조각
└── main.tsx    진입점
```

- import 는 항상 `@/` 별칭을 쓴다 (`@/lib/api`).
- 이 별칭은 `vite.config.ts` 와 `tsconfig.app.json` 두 곳에 적혀 있다.
  **한쪽만 고치면 빌드나 에디터 중 하나가 깨진다.**

## API 호출

- **모든 요청은 `@/lib/api` 의 `api()` 를 거친다.** 컴포넌트에서 `fetch` 를 직접
  부르지 않는다. baseURL 과 에러 처리가 거기 한 군데 모여 있다.
- 경로는 항상 상대경로(`/api/...`)로 쓴다. dev 서버가 프록시해 주기 때문에
  `http://localhost:8080` 을 코드에 넣을 이유가 없다.
- URL, 토큰, 환경값을 하드코딩하지 않는다. 환경값은 `import.meta.env.VITE_*` 로 읽는다.
- 백엔드 응답 형식은
  `.claude/skills/oneteam-development/references/contracts.md` 를 따른다.

## 화면 규칙

- 로딩, 성공, 비어 있음, 에러 네 가지 상태를 처리한다. 에러를 `console.log` 나
  빈 화면으로만 넘기지 않는다.
- 서버가 성공을 돌려주기 전에 화면에서 먼저 성공으로 확정하지 않는다.
- 제출 버튼은 요청 중에 다시 눌리지 않게 막는다.
- `useEffect` 안에서 시작한 요청, 타이머, 이벤트 리스너는 정리 함수에서 끝낸다.
- 색과 간격은 Tailwind 클래스로 쓰고, 같은 것을 여러 곳에서 쓰게 되면 컴포넌트로 뺀다.

## 네이밍

| 대상                    | 규칙                | 예시                         |
| ----------------------- | ------------------- | ---------------------------- |
| 컴포넌트 파일, 컴포넌트 | PascalCase          | `OrderCard.tsx`              |
| 그 외 파일              | kebab-case          | `use-countdown.ts`, `api.ts` |
| 변수, 함수              | camelCase           | `formatPrice()`              |
| 커스텀 훅               | `use` 접두어        | `useCountdown()`             |
| 타입, 인터페이스        | PascalCase          | `OrderSummary`               |
| boolean                 | `is` / `has` 접두어 | `isLoading`, `hasNext`       |

## 금지

- `any` 사용. 타입을 모르겠으면 `unknown` 으로 받고 좁혀 쓴다
- 컴포넌트 안에서 `fetch` 직접 호출
- npm 이나 yarn 으로 설치 (pnpm 고정)
- `dist/` 나 자동 생성 파일 직접 수정
- 요청받지 않은 전체 포맷 변경
