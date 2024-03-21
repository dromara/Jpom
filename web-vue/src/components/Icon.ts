///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import { createVNode } from 'vue'
import {
  FileOutlined,
  SettingOutlined,
  FileTextOutlined,
  CloudServerOutlined,
  UserOutlined,
  DesktopOutlined,
  ToolOutlined,
  MonitorOutlined,
  CodeOutlined,
  BuildOutlined,
  SaveOutlined,
  HddOutlined,
  ApartmentOutlined,
  DashboardOutlined,
  ProjectOutlined,
  GatewayOutlined,
  LaptopOutlined
} from '@ant-design/icons-vue'

const iconObj = {
  file: FileOutlined,
  desktop: DesktopOutlined,
  setting: SettingOutlined,
  hdd: HddOutlined,
  save: SaveOutlined,
  user: UserOutlined,
  apartment: ApartmentOutlined,
  build: BuildOutlined,
  code: CodeOutlined,
  'file-text': FileTextOutlined,
  'cloud-server': CloudServerOutlined,
  monitor: MonitorOutlined,
  tool: ToolOutlined,
  dashboard: DashboardOutlined,
  project: ProjectOutlined,
  gateway: GatewayOutlined,
  laptop: LaptopOutlined
}

const Icon = (props: { type: string }) => {
  const { type } = props
  // @ts-ignore
  return createVNode(iconObj[type])
}

export default Icon
