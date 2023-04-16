import { createVNode } from 'vue'
import * as Icons from '@ant-design/icons-vue'

const Icon = (props: { type: string }) => {
  const { type } = props
  // @ts-ignore
  return createVNode(Icons[type])
}

export default Icon
