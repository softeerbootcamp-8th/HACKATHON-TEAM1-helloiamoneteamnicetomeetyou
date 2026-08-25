---
description: 바뀐 영역의 테스트, lint, 타입 검사, 빌드를 돌리고 결과를 정리한다
argument-hint: [be | fe | all (선택, 기본은 자동 감지)]
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git rev-parse:*), Bash(./gradlew:*), Bash(pnpm:*), Bash(cd *), Read, Grep, Glob
---

# 검증

대상: **$ARGUMENTS** (비어 있으면 자동 감지)

## 1. 대상 결정

인자가 있으면 그대로 따른다 (`be` / `fe` / `all`). 없으면 바뀐 경로로 판단한다.

    git status --short
    git diff --name-only dev...HEAD

`backend/` 가 바뀌었으면 백엔드, `frontend/` 면 프론트, 양쪽이면 둘 다.
문서와 설정만 바뀌었으면 돌릴 검증이 없다고 알리고 끝낸다.

## 2. 저장소 루트 고정

**모든 경로를 저장소 루트 기준으로 만든다.** 상대경로를 쓰면 `backend/` 안에서
실행할 때 `backend/backend` 를 찾아 실패한다.

    R=$(git rev-parse --show-toplevel)

`git` 자체는 어느 하위 폴더에서 실행해도 저장소 전체를 대상으로 하므로
따로 처리할 필요가 없다.

## 3. 백엔드

    ( cd "$R/backend" && ./gradlew test )

Gradle Wrapper 를 쓴다. 전역 `gradle` 을 쓰지 않는다.

**`cd` 는 반드시 서브셸 `( ... )` 안에서 한다.** 셸 작업 디렉터리는 호출 사이에
유지되므로, 서브셸 없이 `cd` 하면 이후 명령이 엉뚱한 위치에서 돈다.

## 4. 프론트엔드

    pnpm --dir "$R/frontend" format:check
    pnpm --dir "$R/frontend" lint
    pnpm --dir "$R/frontend" build

`.github/workflows/ci-frontend.yml` 이 돌리는 것과 같은 순서다.
`build` 가 `tsc -b && vite build` 라서 타입 검사가 빌드에 포함된다.

`format` (자동 수정) 이 아니라 `format:check` 를 쓴다. 검증 단계에서 파일을
임의로 고치지 않는다. 다만 `format:check` 가 실패하면 `pnpm format` 으로 고칠지
사용자에게 물어본다.

패키지 매니저는 **pnpm 고정**이다. npm 이나 yarn 을 섞지 않는다.

## 5. 실패 처리

**실패를 숨기거나 우회하지 않는다.** 다음은 금지다.

- 실패하는 테스트 삭제나 비활성화
- 검증 완화 (assertion 약화, lint 규칙 끄기)
- 실패를 "일단 통과" 로 보고

실패하면 출력을 그대로 보여주고 원인을 분석한다. 고칠지는 사용자가 정한다.
한쪽이 실패해도 나머지 검증은 마저 돌린다. 부분 결과가 더 쓸모 있다.

## 6. 보고

```markdown
## 검증 결과

| 항목 | 결과 |
|---|---|
| backend test | ✅ 통과 (N개) / ❌ 실패 (N개) |
| frontend format:check | ... |
| frontend lint | ... |
| frontend build | ... |

## 바꾼 영역
- {건드린 영역과 파일}

## API 영향
- {API 형식이 바뀌었는지. 없으면 "없음"}

## 확인하지 못한 것
- {돌리지 않은 검증과 그 이유}
- {남은 위험}
```

**"확인하지 못한 것" 을 비워두지 않는다.** 프론트와 백을 붙여서 도는지,
실제 화면에서 동작하는지는 자동 검증으로 확인되지 않는다. API 형식이 바뀌었으면
양쪽을 함께 띄워 확인해야 한다는 점을 알린다.
