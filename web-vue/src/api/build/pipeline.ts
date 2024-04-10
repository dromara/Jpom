import axios from '../config'

export function buildPipelineList(params) {
  return axios({
    url: '/build/pipeline/list',
    method: 'post',
    data: params
  })
}

export function editBuildPipeline(params) {
  return axios({
    url: '/build/pipeline/edit',
    method: 'post',
    headers: {
      'Content-Type': 'application/json'
    },
    data: params
  })
}

export function getBuildPipelineItem(params) {
  return axios({
    url: '/build/pipeline/get',
    method: 'get',
    params
  })
}

export function deleteBuildPipelineItem(params) {
  return axios({
    url: '/build/pipeline/delete',
    method: 'get',
    params
  })
}
