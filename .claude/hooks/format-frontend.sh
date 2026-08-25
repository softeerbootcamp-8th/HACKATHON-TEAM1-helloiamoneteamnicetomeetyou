#!/usr/bin/env bash
# frontend/ 아래 파일을 고친 직후 Prettier 를 돌린다.
#
# CI(`.github/workflows/ci-frontend.yml`)가 `pnpm format:check` 를 돌리기 때문에
# 포맷이 어긋나면 빌드가 통과해도 빨개진다. 고칠 때마다 자동으로 맞춰두면
# push 전에 format:check 를 빠뜨려서 수정 커밋을 하나 더 붙이는 일이 없어진다.
#
# 실패해도 작업을 막지 않는다 (exit 0). 포맷은 CI 가 어차피 한 번 더 본다.
set -uo pipefail

command -v jq >/dev/null 2>&1 || exit 0

f="$(jq -r '.tool_input.file_path // empty' 2>/dev/null)"
[ -z "$f" ] && exit 0
[ -f "$f" ] || exit 0

# frontend/ 밖의 파일은 대상이 아니다. 루트 CLAUDE.md 나 backend/ 는 Prettier 를
# 쓰지 않으므로 건드리면 안 된다.
case "$f" in
  */frontend/*) ;;
  *) exit 0 ;;
esac

# .prettierignore 대상은 애초에 넘기지 않는다.
case "$f" in
  */node_modules/*|*/dist/*|*pnpm-lock.yaml) exit 0 ;;
esac

# Prettier 가 아는 확장자만 넘긴다. 모르는 확장자를 주면 에러를 뱉는다.
case "$f" in
  *.ts|*.tsx|*.js|*.jsx|*.mjs|*.cjs|*.css|*.json|*.md|*.html|*.yaml|*.yml) ;;
  *) exit 0 ;;
esac

root="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null)}"
[ -n "$root" ] || exit 0
[ -d "$root/frontend/node_modules" ] || exit 0

pnpm --dir "$root/frontend" exec prettier --write "$f" >/dev/null 2>&1 || exit 0
exit 0
