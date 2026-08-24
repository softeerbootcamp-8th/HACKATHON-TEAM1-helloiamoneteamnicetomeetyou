import js from '@eslint/js'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import globals from 'globals'
import tseslint from 'typescript-eslint'
// 포매팅에 관한 ESLint 규칙을 전부 끈다. 그 영역은 Prettier 가 맡는다.
// 반드시 배열의 마지막에 와야 앞선 설정의 규칙을 덮어쓸 수 있다.
import prettier from 'eslint-config-prettier'

export default tseslint.config([
  { ignores: ['dist'] },
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      ...tseslint.configs.recommended,
      reactHooks.configs.flat['recommended-latest'],
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2023,
      globals: globals.browser,
    },
  },
  prettier,
])
