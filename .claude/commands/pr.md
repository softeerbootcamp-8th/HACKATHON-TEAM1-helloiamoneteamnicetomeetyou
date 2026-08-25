---
description: 지정한 커밋부터 현재까지의 변경사항으로 팀 컨벤션에 맞는 PR 을 연다
argument-hint: <시작 커밋 SHA> [assignee 지정 시 함께 언급]
allowed-tools: Bash(gh pr:*), Bash(gh label:*), Bash(gh auth:*), Bash(git log:*), Bash(git diff:*), Bash(git show:*), Bash(git status:*), Bash(git branch:*), Bash(git rev-parse:*), Bash(git rev-list:*), Bash(git push:*), Read, Write, Grep, Glob
---

# PR 생성

시작 커밋: **$1**

`$1` 커밋을 **포함해서** 현재 HEAD 까지의 변경사항으로 PR 을 만든다.
`.github/PULL_REQUEST_TEMPLATE.md` 형식을 따른다.

## 1. 사전 확인

`gh auth status` 로 인증을 확인한다. 실패하면 `gh auth login` 을 안내하고 중단한다.

인자가 없으면 최근 커밋 목록(`git log --oneline -15`)을 보여주고 어느 커밋부터
묶을지 물어본다. 임의로 정하지 않는다.

`git rev-parse --verify $1^{commit}` 으로 SHA 가 유효한지 확인한다.

## 2. 범위

`$1` 을 포함해야 하므로 범위는 `$1^..HEAD` 다.

    git log --oneline $1^..HEAD

`$1` 이 최초 커밋이면 `$1^` 가 없어 실패한다. 이때는 `git log --oneline --root HEAD` 를 쓴다.

**범위에 잡힌 커밋 목록을 먼저 보여주고 확인받은 뒤 진행한다.**
범위를 잘못 잡으면 남의 커밋까지 PR 에 들어간다.

## 3. 변경사항 파악

    git diff --stat $1^..HEAD
    git diff $1^..HEAD

커밋 메시지만 요약하지 말고 실제 diff 를 읽는다. 커밋 메시지에 없는 판단이
본문의 "주요 고민 및 해결 과정" 에 들어가야 한다.

## 4. 이슈 번호

순서대로 시도한다.

1. 현재 브랜치명에서 뽑는다 : `be/feat/10-order-create` → `10`
2. 범위 안 커밋 메시지의 `#N` 참조
3. 못 찾으면 사용자에게 묻는다. 이슈 없이 진행하면 `Closes #` 줄을 비워 둔다

## 5. 제목

커밋 메시지와 같은 형식이다. `타입(범위): 요약`

- 범위는 `be` / `fe` / `cd`
- 범위 안 커밋이 여러 영역에 걸치면 **범위를 생략한다**
- 예: `feat(be): 주문 생성 API 구현`, `chore: 개발 컨벤션 문서 추가`

## 6. 본문

템플릿 네 섹션을 채운다. 인용문(`>`) 안내 문구와 예시 항목은 지운다.
스크린샷 섹션은 화면이 바뀌었을 때만 남기고, 없으면 통째로 지운다.

```markdown
## 📌 관련 이슈

- Closes #{번호}

---

## ✨ 작업 개요

- {무엇을 했는지 항목별로}

---

## 🤔 주요 고민 및 해결 과정

- {고민한 지점}
- {고른 방법과 그 이유}

---

## 🙏 리뷰 요청 및 전달사항

- {중점적으로 봐줬으면 하는 부분}
- {확인하지 못한 것, 남은 위험}
```

**작업 개요**는 커밋 제목 나열이 아니라 무엇이 달라졌는지를 쓴다.
첫 줄은 이 PR 을 모르는 사람이 읽어도 알 수 있게 쓴다.

**주요 고민**은 diff 에서 판단이 들어간 지점을 찾아 쓴다. 다르게 골랐으면 코드
구조가 달라졌을 것만 남기고, 도구 선택이나 사소한 판단은 뺀다.
고민할 지점이 없던 단순 작업이면 억지로 만들지 말고 그렇게 쓴다.

**리뷰 요청**에는 확인하지 못한 부분과 남은 위험을 반드시 넣는다.

**글쓰기 톤**: 팀원이 읽는 글이므로 존댓말로 통일한다. 한 불릿은 1~2줄이고,
굵은 글씨로 핵심을 앞에 둔다. 쉬운 말로 짧게 쓴다.

## 7. assignee 와 리뷰어, label

**assignee**: 지정하지 않으면 본인으로 한다. `--assignee @me`

**리뷰어**: **자동으로 지정하지 않는다.** 이 저장소는 부트캠프 조직에 속해 있어
collaborator 목록에 팀원이 아닌 사람까지 들어 있다. 사용자가 명시할 때만 지정한다.

**label**: 실행할 때 실제 목록을 확인하고 맞는 것을 고른다.

    gh label list --limit 100 --json name,description

마땅한 게 없으면 label 없이 진행하고 알린다. **label 을 새로 만들지 않는다.**

## 8. 푸시와 생성

브랜치가 원격에 없으면 먼저 푸시한다.

    git push -u origin HEAD

본문은 임시 파일에 쓰고 `--body-file` 로 넘긴다.

    gh pr create --base dev --title "{제목}" --body-file {임시파일} \
      --assignee @me --label "{label}"

**base 는 `dev`** 다 (`main ← dev ← 기능 브랜치`). `main` 으로 열지 않는다.
현재 브랜치가 `dev` 나 `main` 이면 PR 을 만들 수 없으므로 중단하고 알린다.

label 지정이 실패해도 **PR 본체는 살린다.** PR 을 먼저 만든 뒤 `gh pr edit` 으로
다시 시도하고, 그래도 안 되면 사실대로 알린다.

## 9. 보고

PR URL 과 지정된 assignee, label 을 알린다. label 을 못 붙였으면 이유도 알린다.

**리뷰어를 지정하지 않았음을 알리고 직접 지정하도록 안내한다.**

    gh pr edit {번호} --add-reviewer {아이디}
