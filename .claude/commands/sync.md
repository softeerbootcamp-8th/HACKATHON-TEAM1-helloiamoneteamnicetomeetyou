---
description: dev 최신 내용을 현재 기능 브랜치로 가져온다
argument-hint: (없음)
allowed-tools: Bash(git fetch:*), Bash(git status:*), Bash(git log:*), Bash(git diff:*), Bash(git rebase:*), Bash(git stash:*), Bash(git branch:*), Bash(git rev-parse:*), Bash(git rev-list:*), Read, Grep, Glob
---

# dev 최신 받기

해커톤 기간에는 `dev` 가 하루에도 여러 번 바뀐다. 오래된 `dev` 위에서 계속
작업하면 PR 을 열 때 충돌이 한꺼번에 터진다. 자주 당겨온다.

## 1. 현재 상태 확인

    git rev-parse --abbrev-ref HEAD
    git status --short

- 현재 브랜치가 `dev` 나 `main` 이면 **중단한다.** 이 커맨드는 기능 브랜치용이다.
  `dev` 에 있으면 `git pull` 로 충분하다고 알린다.
- 커밋하지 않은 변경이 있으면 **먼저 사용자에게 알린다.** 임의로 stash 하지 않는다.
  이어서 진행하겠다고 하면 `git stash push -u` 로 넣어두고, 끝난 뒤 `git stash pop`
  으로 되돌린다. **pop 을 빠뜨리지 않는다.**

## 2. 최신 dev 를 받는다

    git fetch origin dev

얼마나 뒤처져 있는지 보여준다.

    git log --oneline HEAD..origin/dev
    git rev-list --count HEAD..origin/dev

**0이면 여기서 끝낸다.** 이미 최신이라고 알리고 rebase 하지 않는다.

## 3. rebase

    git rebase origin/dev

**merge 가 아니라 rebase 를 쓴다.** 기능 브랜치의 커밋을 최신 `dev` 위로 다시 얹어야
PR diff 에 남의 커밋이 섞이지 않는다.

이미 원격에 push 한 브랜치를 rebase 하면 이력이 갈리므로, 다음 push 는
`--force-with-lease` 가 필요하다. 그 사실을 사용자에게 알린다.
**`--force` 는 쓰지 않는다.** 금지 명령어 차단 훅이 막는다.

## 4. 충돌이 났을 때

**자동으로 한쪽을 골라 해결하지 않는다.** 충돌 파일과 각 쪽이 무엇을 바꿨는지
보여주고 어떻게 할지 물어본다.

    git status --short
    git diff

`.pnpm-lock.yaml` 이나 자동 생성 파일이 충돌하면 손으로 고치지 말고
재생성하는 쪽이 맞는지 확인한다.

되돌리려면 아래로 원래 상태로 돌아간다. 이건 안전하다.

    git rebase --abort

## 5. 확인

rebase 가 끝나면 코드가 실제로 도는지 본다. 남의 변경이 들어왔기 때문에
어제까지 되던 것이 깨져 있을 수 있다.

    /verify

## 6. 보고

- 가져온 커밋 수와 주요 변경 내용
- 충돌이 있었으면 어느 파일이었고 어떻게 처리했는지
- stash 를 썼으면 되돌렸는지
- **다음 push 에 `--force-with-lease` 가 필요한지**
- `/verify` 결과
