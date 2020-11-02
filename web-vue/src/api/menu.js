import axios from './config';

export function getSystemMenu() {
  return axios({
    url: 'menus_data.json',
    method: 'post'
  })
}
