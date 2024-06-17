<template>
  <a-drawer v-bind="props" :root-style="rootStyle">
    <slot name="default"></slot>
    <template v-if="slots.footer" #footer>
      <slot name="footer"></slot>
    </template>
    <template v-if="slots.extra" #extra>
      <slot name="extra"></slot>
    </template>
    <template v-if="slots.title" #title>
      <slot name="title"></slot>
    </template>
  </a-drawer>
</template>
<script lang="ts">
import { drawerProps } from 'ant-design-vue/es/drawer'
import { initDefaultProps } from 'ant-design-vue/es/_util/props-util'
import { CustomSlotsType } from 'ant-design-vue/es/_util/type'
import { CSSProperties, defineComponent } from 'vue'
import { increaseZIndex } from '@/utils/utils'

export default defineComponent({
  name: 'CustomDrawer',
  props: initDefaultProps(drawerProps(), {}),
  slots: Object as CustomSlotsType<{ default: any; footer?: any; extra?: any; title?: any }>,
  setup(props, { emit, slots }) {
    const rootStyle: CSSProperties = {
      zIndex: increaseZIndex(),
      ...props.rootStyle
    }
    return {
      props,
      rootStyle,
      emit,
      slots
    }
  }
})
</script>
