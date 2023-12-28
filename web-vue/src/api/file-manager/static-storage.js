import axios from "@/api/config";

export function staticFileStorageList(params) {
  return axios({
    url: "/file-storage/static/list",
    method: "post",
    data: params,
  });
}

export function delFile(params) {
  return axios({
    url: "/file-storage/static/del",
    method: "get",
    params: params,
  });
}

// 下载 url
export function triggerUrl(params) {
  return axios({
    url: "/file-storage/static/trigger-url",
    method: "get",
    params: params,
  });
}

// 修改文件
export function fileEdit(params) {
  return axios({
    url: "/file-storage/static/edit",
    method: "post",
    data: params,
  });
}

export function hasStaticFile(params) {
  return axios({
    url: "/file-storage/static/has-file",
    method: "get",
    params: params,
  });
}
