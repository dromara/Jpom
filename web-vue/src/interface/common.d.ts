///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

export interface IPageQuery {
  page: number
  limit: number
  total: number
  [key: string]: any
}
export interface SystemType {
  disabledCaptcha: bollean
  disabledGuide: bollean
  inDocker: bollean
  loginTitle: string
  name: string
  notificationPlacement: string
  routerBase: string
  subTitle: string
}

export interface GlobalWindow {
  routerBase: string
  apiTimeout: string
  uploadFileSliceSize: string
  uploadFileConcurrent: string
  oauth2Provide: string
  transportEncryption: string
}
