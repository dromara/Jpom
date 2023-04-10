export interface IResponse<T> {
  data?: T
  code: number
  msg: string
}
