# HACKATHON-TEAM1-helloiamoneteamnicetomeetyou

안녕하세요 원팀입니다 잘부탁드립니다

프론트엔드와 백엔드를 한 저장소에서 관리합니다. 두 디렉터리는 서로 의존하지 않고, 각자
자기 디렉터리 안에서만 빌드하고 테스트합니다.

```
.
├── backend/    Spring Boot 4 · Java 21 · Gradle
├── frontend/   React 19 · TypeScript · Vite · Tailwind CSS
└── .github/workflows/
    ├── deploy-backend.yml   main push 시 backend/ 가 바뀌었으면 EC2 로 배포
    └── ci-frontend.yml      PR 에서 frontend/ 포맷·린트·빌드 확인
```

<br>

## 백엔드 실행하기

Java 21 이 필요하고, Gradle 은 wrapper 가 알아서 받습니다.

```bash
cd backend
./gradlew bootRun          # http://localhost:8080
./gradlew test
```

도커로 띄워서 확인하려면 아래를 씁니다. 배포와 같은 이미지를 그대로 씁니다.

```bash
cd backend
docker compose up --build -d
docker compose down
```

<br>

## 프론트엔드 실행하기

Node 22 이상과 pnpm 이 필요합니다. pnpm 이 없으면 `corepack enable` 로 켭니다.

```bash
cd frontend
pnpm install
pnpm dev                   # http://localhost:5173
```

첫 화면에 백엔드 연결 상태가 나옵니다. 백엔드를 같이 띄워 두면 `연결됨` 으로 바뀝니다.

| 명령 | 하는 일 |
|---|---|
| `pnpm dev` | 개발 서버. `/api` 와 `/health` 는 8080 으로 프록시됩니다 |
| `pnpm build` | 타입 검사 후 `dist/` 로 빌드 |
| `pnpm lint` | ESLint (`--fix` 는 `pnpm lint:fix`) |
| `pnpm format` | Prettier 로 전체 정리 |

`import { api } from '@/lib/api'` 처럼 `@` 로 `src` 를 가리킵니다. 이 별칭은
`vite.config.ts` 와 `tsconfig.app.json` 두 곳에 적혀 있으니 한쪽만 고치지 않습니다.

<br>

## 배포

백엔드만 자동 배포합니다. main 에 `backend/` 변경이 들어오면 테스트를 돌리고, arm64 이미지를
GHCR 에 올린 뒤 EC2(t4g.micro)에서 컨테이너를 갈아 끼웁니다. 결과는 슬랙으로 옵니다.

프론트엔드는 아직 배포 파이프라인이 없습니다. PR 에서 포맷과 린트, 빌드만 확인합니다.
