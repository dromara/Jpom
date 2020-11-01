import axios from './config';

export function login(params) {
  return axios({
    url: '/userLogin',
    method: 'post',
    data: params
  })
}
