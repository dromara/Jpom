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
  GatewayOutlined
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
  gateway: GatewayOutlined
}

const Icon = (props: { type: string }) => {
  const { type } = props
  // @ts-ignore
  return createVNode(iconObj[type])
}

export default Icon
