const isBlob = (data: any) => {
  return data instanceof Blob
}

const isArrayBuffer = (data: any) => {
  return data instanceof ArrayBuffer
}

const isFile = (mimeType: any) => {
  return data instanceof File
}

const isImageType = (mimeType: any) => {
  return mimeType.startsWith('image/')
}
