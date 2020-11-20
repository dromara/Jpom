import axios from './config';

// ssh 列表
export function getSshList() {
  return axios({
    url: '/node/ssh/list_data.json',
    method: 'post'
  })
}

/**
 * 编辑 SSH
 * @param {*} params
 * params.type = {'add': 表示新增, 'edit': 表示修改} 
 */
export function editSsh(params) {
  const data = {
    type: params.type,
    id: params.id,
    name: params.name,
    host: params.host,
    port: params.port,
    user: params.user,
    password: params.password,
    connectType: params.connectType,
    privateKey: params.privateKey,
    charset: params.charset,
    fileDirs: params.fileDirs
  }
  return axios({
    url: '/node/ssh/save.json',
    method: 'post',
    data
  })
}

// 删除 SSH
export function deleteSsh(id) {
  return axios({
    url: '/node/ssh/del.json',
    method: 'post',
    data: {id}
  })
}

// 上传文件
export function installAgentNode(formData) {
  return axios({
    url: '/node/ssh/installAgentSubmit.json',
    headers: {
      'Content-Type': 'multipart/form-data;charset=UTF-8'
    },
    method: 'post',
    data: formData
  })
}