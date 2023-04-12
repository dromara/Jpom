import axios from './config'

/**
 *
 * @param data
 */
export function cronTools(params: any) {
  return axios({
    url: '/tools/cron',
    method: 'get',
    params: data
  })
}
