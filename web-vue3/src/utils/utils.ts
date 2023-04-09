export function getHashQuery() {
  const querys: Record<string, string> = {}
  location.hash.replace(/[?&]+([^=&]+)=([^&]*)/gi, (_match: string, key: string, value: string) => {
    querys[key] = value
    return ''
  })
  return querys
}
