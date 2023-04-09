export function getHashQuery() {
  const querys: Record<string, any> = {}
  location.hash.replace(/[?&]+([^=&]+)=([^&]*)/gi, (_m: string, key: string, value: any) => {
    querys[key] = value
  })
  return querys
}
