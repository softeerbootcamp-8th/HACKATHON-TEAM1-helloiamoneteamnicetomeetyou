---
description: 백엔드와 프론트엔드 개발 서버를 띄우고 붙어 있는지 확인한다
argument-hint: [be | fe | all (선택, 기본은 all)]
allowed-tools: Bash(./gradlew:*), Bash(pnpm:*), Bash(curl:*), Bash(lsof:*), Bash(git rev-parse:*), Bash(cd *), Read
---

# 개발 서버 기동

대상: **$ARGUMENTS** (비어 있으면 둘 다)

## 1. 저장소 루트 고정

    R=$(git rev-parse --show-toplevel)

`cd` 는 반드시 서브셸 `( ... )` 안에서 한다. 셸 작업 디렉터리는 호출 사이에
유지되므로, 서브셸 없이 `cd` 하면 이후 명령이 엉뚱한 위치에서 돈다.

## 2. 이미 떠 있는지 먼저 본다

    lsof -ti tcp:8080
    lsof -ti tcp:5173

**이미 떠 있으면 다시 띄우지 않는다.** 포트가 물려 있으면 새로 뜬 서버가 조용히
실패하고, 사용자는 옛날 코드를 보면서 왜 안 바뀌는지 헤매게 된다.
떠 있으면 그 사실을 알리고 3번으로 넘어간다.

내려야 한다면 PID 를 보여주고 **사용자에게 물어본 뒤** 종료한다. 임의로 죽이지 않는다.

## 3. 기동

두 서버 모두 `run_in_background` 로 띄운다. 포그라운드로 돌리면 세션이 묶인다.

백엔드.

    ( cd "$R/backend" && ./gradlew bootRun )

프론트엔드.

    pnpm --dir "$R/frontend" dev

`frontend/node_modules` 가 없으면 `pnpm --dir "$R/frontend" install` 을 먼저 돌린다.

## 4. 붙었는지 확인

백엔드가 뜨는 데 시간이 걸리므로 바로 찌르면 실패한다. 몇 초 간격으로 재시도한다.

    curl -fsS http://localhost:8080/health
    curl -fsS http://localhost:5173/health

두 번째가 되면 **프론트 dev 서버의 프록시까지 살아 있다는 뜻**이다
(`vite.config.ts` 가 `/api` 와 `/health` 를 8080 으로 넘긴다).
프론트에서 API 가 404 나 CORS 로 실패하면 여기부터 확인한다.

30초 안에 안 뜨면 백그라운드 출력을 읽어 원인을 보여준다. 계속 재시도하지 않는다.

## 5. 보고

| 항목 | 상태 |
|---|---|
| backend (8080) | 기동됨 / 이미 떠 있었음 / 실패 |
| frontend (5173) | 기동됨 / 이미 떠 있었음 / 실패 |
| 프록시 (5173 → 8080) | 확인됨 / 실패 |

접속 주소(`http://localhost:5173`)를 알리고, 백그라운드로 돌고 있으니
로그를 볼 수 있다는 점을 함께 알린다.
