/**
 * 조건부 className 을 이어 붙인다. 이것 하나 때문에 clsx 를 넣지 않는다.
 */
export function cn(...parts: Array<string | false | null | undefined>): string {
  return parts.filter(Boolean).join(' ')
}
