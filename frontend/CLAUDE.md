# 프론트엔드

프론트엔드에서 항상 지키는 규칙만 적는다. 루트 `CLAUDE.md` 를 먼저 읽고,
상세한 내용은 `.claude/skills/oneteam-development/references/frontend.md` 를 본다.

## 현재 스택

- Vite 8 + React 19 + TypeScript (strict)
- Tailwind CSS v4 (`@tailwindcss/vite` 플러그인, CSS-first)
- PWA (`vite-plugin-pwa`). 홈 화면 설치와 오프라인 앱 껍데기까지만 한다
- ESLint + Prettier (포맷 규칙은 전부 Prettier 가 맡는다)
- 패키지 매니저는 **pnpm 고정**이다. Node 22 이상이 필요하다.
- 라우팅은 `react-router`, 모션은 `motion` 을 쓴다.
- **서버 상태 라이브러리는 아직 없다.** 필요해지면 팀에 알리고 넣는다.

## 명령어

| 명령                | 하는 일                                                       |
| ------------------- | ------------------------------------------------------------- |
| `pnpm dev`          | 개발 서버 (5173). `/api` 와 `/health` 는 8080 으로 프록시된다 |
| `pnpm build`        | `tsc -b` 타입 검사 후 `dist/` 로 빌드                         |
| `pnpm lint`         | ESLint (`pnpm lint:fix` 로 자동 수정)                         |
| `pnpm format`       | Prettier 로 정리                                              |
| `pnpm format:check` | Prettier 검사만. **CI 가 이걸 돌린다**                        |
| `pnpm preview`      | 빌드 결과를 띄운다. **서비스 워커는 여기서만 돈다**           |

## PWA

`vite.config.ts` 의 `VitePWA` 설정 한 군데에 모여 있다.

- **서비스 워커는 `pnpm dev` 에서 돌지 않는다.** 설치나 오프라인을 확인하려면
  `pnpm build && pnpm preview` 로 봐야 한다.
- **`/api` 응답은 캐시하지 않는다.** 프리캐시 대상은 빌드 산출물뿐이고 `runtimeCaching`
  을 비워 뒀다. 오래된 데이터가 화면에 남으면 디버깅이 어려워진다. 오프라인에서 API 를
  쓰고 싶어지면 그때 팀에 알리고 넣는다.
- 새 배포가 올라오면 다음 방문에 서비스 워커가 알아서 갈아끼운다(`autoUpdate`).

### 아이콘 바꾸기

**`public/logo.svg` 하나만 갈아끼우고 아래를 돌린다.** 512x512 정사각형이고,
안드로이드가 아이콘을 원형으로 깎기 때문에 가장자리에 여백이 있어야 한다.

```bash
pnpm generate-pwa-assets
```

`public/` 의 `favicon.ico`, `apple-touch-icon-180x180.png`, `pwa-*.png`,
`maskable-icon-512x512.png` 가 다시 만들어진다. **결과물은 커밋한다.** 빌드할 때
만들지 않는 이유는 CI 와 Vercel 이 `sharp` 를 설치하지 않아도 되게 하려는 것이다.

- 브라우저 탭 아이콘인 `public/favicon.svg` 는 이 생성기가 건드리지 않는다.
  배경이 투명한 별도 파일이라 같이 바꿔야 한다.
- 이름과 테마색은 `vite.config.ts` 의 `manifest` 에 있다. `theme_color` 를 바꾸면
  `index.html` 의 `<meta name="theme-color">` 도 같이 고친다.

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

Claude Code 로 작업할 때는 `frontend/` 아래 파일을 고치는 즉시
`.claude/hooks/format-frontend.sh` 가 Prettier 를 돌린다. 손으로 고쳤거나
스크립트로 바꿨을 때는 이 훅이 돌지 않으므로 위 명령을 직접 돌린다.

## 폴더와 import

```
src/
├── app/          라우터와 화면 껍데기(AppShell)
├── lib/          api 클라이언트, 모션 프리셋 등 공용 유틸
├── components/
│   ├── ui/       버튼, 다이얼로그처럼 도메인을 모르는 것
│   └── domain/   카드, 레이더처럼 이 서비스에만 있는 것
├── screens/      화면 하나당 파일 하나
├── store/        전역 상태와 매칭 규칙
├── mocks/        API 가 붙기 전까지 쓰는 고정 데이터
└── main.tsx      진입점
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
- **API 오리진은 `VITE_API_BASE_URL` 하나로 갈린다.** 로컬은 값이 없어서 dev 서버 proxy 를
  타고, 배포는 Vercel 환경변수가 채운다. 저절로 갈리므로 `import.meta.env.DEV` 로 호출
  주소를 분기하지 않는다. 자세한 것은 루트 `CLAUDE.md` 의 「배포」 절에 있다.
- 백엔드 공통 응답은 `CommonResponse<T>` 로 감싸여 온다. `api()` 는 이걸 벗겨주지 않으므로
  호출하는 쪽에서 `res.data` 를 꺼낸다.
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

## 모션

**모션 값을 화면마다 따로 적지 않는다.** `@/lib/motion` 의 프리셋만 쓴다.
화면마다 stiffness 를 다르게 두면 같은 앱인데 화면마다 손맛이 달라진다.

- 화면 전환은 `springPage`, 버튼과 카드는 `springSnap`, 시트와 다이얼로그는 `springSheet` 다.
- **duration 기반 이징으로 때우지 않는다.** 손가락을 떼는 속도가 이어져야 끊긴 느낌이 안 난다.
- 누를 수 있는 것에는 반드시 `whileTap` 축소를 준다. 모바일에는 hover 가 없어서
  이것이 사용자가 받는 유일한 피드백이다.
- 리스트는 `staggerParent` / `staggerChild` 로 순서대로 띄운다.
- `prefers-reduced-motion` 은 `index.css` 에서 한 번에 처리한다.

## 화면을 만들 때

- **죽은 버튼을 남기지 않는다.** 누를 것이 있으면 반드시 무언가 일어난다.
- 다이얼로그와 바텀시트는 바깥 탭, Esc, 버튼 셋 다로 닫힌다. 하나라도 막히면 사용자가 갇힌다.
- 모바일이 기준이다. 데스크톱은 `AppShell` 이 가운데 판으로 감싸고, 넓게 쓰는 화면만
  `app/routes.ts` 의 `WIDE_ROUTES` 에 넣는다.
- 아이콘은 이모지 대신 `components/ui/icons.tsx` 의 선 아이콘을 쓴다. 이모지는 기기마다
  모양과 색이 달라서 시안대로 맞출 수가 없다.
- 색과 그라데이션은 `index.css` 의 `@theme` 토큰만 쓴다. 시안에서 뽑은 값이라 임의로 바꾸지 않는다.
