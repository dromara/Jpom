import axios from './config';

// ssh 列表
export function getSshList() {
	return axios({
		url: '/node/ssh/list_data.json',
		method: 'post'
	})
}

// 根据 nodeId 查询列表
export function getSshListByNodeId(nodeId) {
	return axios({
		url: '/node/ssh/list_by_node_id',
		method: 'post',
		data: {nodeId}
	})
}

// ssh 操作日志列表
export function getSshOperationLogList(params) {
	return axios({
		url: '/node/ssh/log_list_data.json',
		method: 'post',
		data: params
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
		fileDirs: params.fileDirs,
		notAllowedCommand: params.notAllowedCommand
	}
	return axios({
		url: '/node/ssh/save.json',
		method: 'post',
		// 0 表示无超时时间
		timeout: 0,
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

/**
 * 上传安装文件
 * @param {
 *  file: 文件 multipart/form-data,
 *  id: ssh ID,
 *  nodeData: 节点数据 json 字符串 `{"url":"121.42.160.109:2123","protocol":"http","id":"test","name":"tesst","path":"/seestech"}`,
 *  path: 文件保存的路径
 * } formData
 */
export function installAgentNode(formData) {
	return axios({
		url: '/node/ssh/installAgentSubmit.json',
		headers: {
			'Content-Type': 'multipart/form-data;charset=UTF-8'
		},
		method: 'post',
		// 0 表示无超时时间
		timeout: 0,
		data: formData
	})
}

/**
 * 上传文件到 SSH 节点
 * @param {
 *  file: 文件 multipart/form-data,
 *  id: ssh ID,
 *  name: 当前目录,
 *  path: 父级目录
 * } formData
 */
export function uploadFile(formData) {
	return axios({
		url: '/node/ssh/upload',
		headers: {
			'Content-Type': 'multipart/form-data;charset=UTF-8'
		},
		method: 'post',
		// 0 表示无超时时间
		timeout: 0,
		data: formData
	})
}

/**
 * 授权目录列表
 * @param {String} id
 */
export function getRootFileList(id) {
	return axios({
		url: '/node/ssh/root_file_data.json',
		method: 'post',
		data: {id}
	})
}

/**
 * 文件列表
 * @param {id, path, children} params
 */
export function getFileList(params) {
	return axios({
		url: '/node/ssh/list_file_data.json',
		method: 'post',
		data: params
	})
}

/**
 * 下载文件
 * 下载文件的返回是 blob 类型，把 blob 用浏览器下载下来
 * @param {id, path, name} params
 */
export function downloadFile(params) {
	return axios({
		url: '/node/ssh/download.html',
		method: 'get',
		responseType: 'blob',
		timeout: 0,
		params
	})
}

/**
 * 删除文件
 * @param {id, path, name} params x
 */
export function deleteFile(params) {
	return axios({
		url: '/node/ssh/delete.json',
		method: 'post',
		data: params
	})
}
