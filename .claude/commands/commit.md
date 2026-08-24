---
description: 팀 컨벤션에 맞춰 변경사항을 커밋한다
argument-hint: [커밋할 내용 힌트 (선택)]
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git add:*), Bash(git commit:*), Bash(git log:*), Bash(git restore:*), Bash(git rev-parse:*), Read, Write, Grep, Glob
---

# 커밋

추가 지시: **$ARGUMENTS**

## 1. 변경사항 파악

저장소 루트를 먼저 고정한다. `backend/` 나 `frontend/` 안에서 실행하면
`git status` 가 `../.claude/...` 같은 상대경로를 출력해 스테이징할 때 헷갈린다.

    R=$(git rev-parse --show-toplevel)
    git -C "$R" status --short
    git -C "$R" diff
    git -C "$R" diff --staged

이후 모든 git 명령에 `-C "$R"` 를 붙이고 경로는 루트 기준으로 쓴다.

**diff 를 실제로 읽는다.** 파일명만 보고 요약하지 않는다.
`git log --oneline -5` 로 현재 커밋 스타일도 확인한다.

## 2. 커밋 분리

성격이 다른 변경이 섞여 있으면 나눠서 커밋한다. 기능 구현과 무관한 포맷 변경,
서로 다른 도메인의 수정은 분리한다. 되돌릴 단위가 다르면 커밋도 다르다.
판단이 서지 않으면 사용자에게 묻는다.

## 3. 메시지

형식은 `[영역] 타입: 요약` 이다. 콜론 앞에 공백을 넣지 않는다.

**영역**: 바뀐 경로로 판단한다.

| 경로 | 영역 |
|---|---|
| `backend/` 만 | `BE` |
| `frontend/` 만 | `FE` |
| 양쪽 또는 루트(`.github/`, `.claude/`, `CLAUDE.md`, `.gitignore`, `README.md`) | `ALL` |

**타입**: `feat` / `fix` / `refactor` / `chore` / `test` / `docs`

**요약**: 한글, 명사형으로 끝낸다. "수정" 같은 모호한 표현 대신
"재고 검증 추가" 처럼 무엇을 했는지 드러나게 쓴다.

본문은 diff 만 봐서는 알 수 없는 이유가 있을 때만 빈 줄 뒤에 붙인다.
무엇을 했는지를 본문에 나열하지 않는다. diff 에 이미 있다.

커밋 author 는 실제 작업자다. `Co-Authored-By` 같은 도구 이름을 이력에 넣지 않는다.

## 4. 커밋

메시지는 **반드시 파일로 써서 `-F` 로 넘긴다.**

    git commit -F {임시파일}

`-m` 과 heredoc 을 쓰지 않는다. 메시지에 `git reset --hard` 나
`docker compose down -v` 같은 문자열이 들어가면 금지 명령어 차단 훅이
실행이 아닌 텍스트를 오탐해 커밋이 막힌다.

## 5. 커밋 전 확인

- `.env`, 토큰, 개인정보가 들어가지 않았는지 본다
- 현재 브랜치가 `main` 이나 `dev` 면 **중단하고 알린다.** 기능 브랜치에서 작업한다
- `git add .` 대신 의도한 경로만 명시해 스테이징한다

## 6. 보고

커밋 해시와 제목을 보여준다. 여러 개면 전부 나열한다.
푸시는 **하지 않는다.** 필요하면 사용자가 지시한다.
