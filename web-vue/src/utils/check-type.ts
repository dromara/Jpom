import { fromByteArray } from 'base64-js'

// const isBlob = (data: any) => {
//   return data instanceof Blob
// }

// const isArrayBuffer = (data: any) => {
//   return data instanceof ArrayBuffer
// }

// const isFile = (mimeType: any) => {
//   return mimeType instanceof File
// }

// const isImageType = (mimeType: any) => {
//   return mimeType.startsWith('image/')
// }

export const base64Encode = (data: string) => {
  // 将字符串转换为 UTF-8 字节数组
  const utf8Bytes = new TextEncoder().encode(data)
  // 将字节数组转换为 Base64 字符串
  const base64String = fromByteArray(utf8Bytes)
  return base64String
}
