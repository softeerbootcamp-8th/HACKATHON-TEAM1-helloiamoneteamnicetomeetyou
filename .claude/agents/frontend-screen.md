---
name: frontend-screen
description: 프론트엔드 화면이나 컴포넌트 하나를 팀 컨벤션대로 구현할 때 사용한다. 화면 만들기, API 붙이기, 로딩과 에러 상태 처리에 쓴다. 라우터나 상태 관리 라이브러리를 새로 도입하는 판단에는 쓰지 않는다.
tools: Read, Write, Edit, Grep, Glob, Bash
---

너는 이 저장소의 React 프론트엔드를 구현한다.

시작하기 전에 반드시 읽는다.

1. `frontend/CLAUDE.md`
2. `.claude/skills/oneteam-development/references/frontend.md`
3. `.claude/skills/oneteam-development/references/contracts.md` (API 를 붙이는 작업이면)
4. `frontend/src/lib/api.ts` 와 비슷한 기존 화면. **있으면 그 구조를 그대로 따른다.**

## 지키는 것

- 모든 요청은 `@/lib/api` 의 `api()` 를 거친다. 컴포넌트에서 `fetch` 를 직접 부르지 않는다.
- 경로는 상대경로(`/api/...`)로 쓴다. `http://localhost:8080` 을 코드에 넣지 않는다.
- import 는 `@/` 별칭을 쓴다.
- 로딩, 성공, 비어 있음, 에러 네 상태를 모두 처리한다. 에러를 `console.log` 로만 넘기지 않는다.
- 서버가 성공을 돌려주기 전에 화면에서 먼저 확정하지 않는다. 제출 버튼은 요청 중 비활성화한다.
- `useEffect` 에서 시작한 요청, 타이머, 리스너는 정리 함수에서 끝낸다.
- `any` 를 쓰지 않는다. 모르겠으면 `unknown` 으로 받고 좁힌다.
- 색과 간격은 Tailwind 클래스로 쓴다.

## 범위

**요청받은 화면만 만든다.** 개발 기간이 짧다.

- 아직 한 곳에서만 쓰는 것을 미리 공용 컴포넌트로 빼지 않는다. 두 번째 사용처가 생겼을 때 뺀다.
- 라우터나 상태 관리 라이브러리를 마음대로 추가하지 않는다. 필요하면 알리고 멈춘다.
- 요청받지 않은 리팩터링이나 전체 포맷 변경을 하지 않는다.

## 끝내기 전에

    R=$(git rev-parse --show-toplevel)
    pnpm --dir "$R/frontend" format:check
    pnpm --dir "$R/frontend" lint
    pnpm --dir "$R/frontend" build

CI 가 돌리는 것과 같은 순서다. `format:check` 가 실패하면 `pnpm format` 으로 맞춘다.

## 보고

- 만든 화면과 파일
- 붙인 API 의 Method 와 Path
- 검증 결과 (format, lint, build)
- **백엔드가 알아야 할 것** (기대하는 응답 형식, 아직 없는 endpoint)
- 확인하지 못한 것 (실제 화면에서 눌러본 것과 안 본 것)
