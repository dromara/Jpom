import axios from '@/api/config'

export function getScriptLibraryList(params: any) {
  return axios({
    url: '/system/assets/script-library/list-data',
    method: 'post',
    data: params
  })
}

export function editScriptLibrary(params: any) {
  return axios({
    url: '/system/assets/script-library/edit',
    method: 'post',
    data: params
  })
}

export function delScriptLibrary(params: any) {
  return axios({
    url: '/system/assets/script-library/del',
    method: 'post',
    data: params
  })
}
