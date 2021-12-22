import axios from './config';

// 命令列表
export function getCommandList(params) {
    return axios({
        url: "/node/command/list",
        method: "post",
        data: params,
    });
}

// 编辑命令
export function editCommand(params) {
    return axios({
        url: "/node/command/edit",
        method: "post",
        headers: {
            "Content-Type": "application/json",
        },
        data: params,
    });
}

// 删除命令
export function deleteCommand(id) {
    return axios({
        url: "/node/command/del",
        method: "post",
        data: {id},
    });
}

// 删除命令
export function executeBatch(param) {
    return axios({
        url: "/node/command/batch",
        method: "post",
        data: param,
    });
}

