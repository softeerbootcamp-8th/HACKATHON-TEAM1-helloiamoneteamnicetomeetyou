---
description: 팀 컨벤션에 맞춰 GitHub 이슈를 만들고 브랜치까지 딴다
argument-hint: <작업 내용 설명> [assignee 지정 시 함께 언급]
allowed-tools: Bash(gh issue:*), Bash(gh label:*), Bash(gh auth:*), Bash(git branch:*), Bash(git switch:*), Bash(git fetch:*), Bash(git log:*), Bash(git status:*), Read, Write, Grep, Glob
---

# 이슈 생성

사용자 요청: **$ARGUMENTS**

`.github/ISSUE_TEMPLATE/issue_template.md` 형식과 팀 컨벤션에 맞춰 이슈를 만든다.

## 사전 확인

`gh auth status` 로 인증을 확인한다. 실패하면 아래를 안내하고 중단한다.

    brew install gh && gh auth login

## 1. 제목

형식은 `타입(범위): 이슈 이름` 이다. 커밋, PR 제목과 같은 형식을 쓴다.

- 타입: `feat` / `fix` / `refactor` / `chore` / `test` / `docs`
- 범위: `be` / `fe` / `cd` (배포와 CI). **저장소 전체에 걸치면 범위를 생략한다**

요청 내용만으로 타입이나 범위가 애매하면 추측하지 말고 사용자에게 묻는다.
이름은 무엇을 하는지 드러나게 쓴다. "API 작업" 대신 "주문 생성 API 구현".

예: `feat(be): 주문 생성 API 구현`, `chore: 개발 컨벤션 문서 추가`

## 2. 본문

템플릿 두 섹션을 채운다. 인용문(`>`) 안내 문구는 지우고 실제 내용을 넣는다.

```markdown
## 🌿 Branch Name

`{브랜치명}`

---

## 📄 상세 내용

- 작업 내용 1
- 작업 내용 2
```

**상세 내용**은 무엇을 만드는지와 작업 범위를 항목으로 쓴다. 요청이 짧으면
관련 코드와 `.claude/skills/oneteam-development/references/` 를 읽고 구체화한다.
추측으로 범위를 부풀리지 않는다.

**브랜치명** 형식은 `{영역}/{타입}/{이슈번호}-{기능}` 이다.

- 영역은 소문자 `be` / `fe` 다. 양쪽에 걸치면 영역을 생략한다
- 기능 부분은 영문 kebab-case
- 예: `be/feat/10-order-create`, `chore/14-claude-agent-setup`

이슈 번호는 만들기 전에는 모르므로, 브랜치명 자리를 `TBD` 로 두고 이슈를 만든 뒤
실제 번호로 채워 넣는다.

**글쓰기 톤**: 팀원이 읽는 글이므로 존댓말로 쓰고, 쉬운 말로 짧게 쓴다.
한 번 읽고 바로 핵심을 파악할 수 있게 쓴다.

## 3. assignee 와 label

**assignee**: 사용자가 지정하지 않으면 본인으로 한다. `--assignee @me`

**label**: 저장소에 있는 것 중에서 고른다. **실행할 때 실제 목록을 확인한다.**

    gh label list --limit 100 --json name,description

작성 시점의 기본 label 기준 대응은 아래와 같다.

| 타입 | label |
|---|---|
| feat | `enhancement` |
| fix | `bug` |
| docs | `documentation` |
| refactor / chore / test | 마땅한 것 없음 |

마땅한 게 없으면 label 없이 진행하고 그 사실을 알린다.
**label 을 새로 만들지 않는다.** 저장소 전체에 영향을 주므로 제안만 한다.

## 4. 생성

본문은 임시 파일에 쓰고 `--body-file` 로 넘긴다. 인라인 `--body` 는 따옴표 처리가
깨지기 쉽고 금지 명령어 차단 훅의 오탐도 유발한다.

    gh issue create --title "{제목}" --body-file {임시파일} \
      --assignee @me --label "{label}"

없는 label 을 넘기면 명령 전체가 실패한다. 위에서 조회한 이름만 쓴다.

만들어진 번호 `N` 으로 본문의 `TBD` 를 실제 브랜치명으로 바꾼다.

    gh issue edit N --body-file {수정한 임시파일}

## 5. 브랜치

이슈 번호, URL, 확정된 브랜치명, assignee 와 label 을 알린다.
label 을 못 붙였으면 이유도 같이 알린다.

그리고 브랜치를 지금 만들지 물어본다. 만든다고 하면 `dev` 최신을 받고 딴다.

    git fetch origin dev
    git switch -c {브랜치명} origin/dev

현재 브랜치가 `dev` 가 아니어도 기능 브랜치는 항상 `dev` 에서 딴다
(`main ← dev ← 기능 브랜치`).
