///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

// Jpom 为开源软件，请基于开源协议用于商业用途

// 开源不等同于免费，如果您基于 Jpom 二次开发修改了 logo、名称、版权等，请联系我们授权，否则会有法律风险。
//   我们有权利追诉破坏开源并因此获利的团队个人的全部违法所得，也欢迎给我们提供侵权线索。

// 二次修改不可删除或者修改版权，否则可能承担法律责任

// 擅自修改或者删除版权信息有法律风险，请尊重开源协议，不要擅自修改版本信息，否则可能承担法律责任。

import axios from './config'

export function getLicense() {
  return axios({
    url: '/about/license',
    method: 'get'
  })
}

export function getThankDependency() {
  return axios({
    url: '/about/thank-dependency',
    method: 'get'
  })
}
