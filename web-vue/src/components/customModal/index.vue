<template>
  <a-modal v-bind="props" :body-style="bodyStyle">
    <slot name="default"></slot>
  </a-modal>
</template>

<script lang="ts">
import { modalProps } from 'ant-design-vue/es/modal/Modal'
import { initDefaultProps } from 'ant-design-vue/es/_util/props-util'
import { CustomSlotsType } from 'ant-design-vue/es/_util/type'
import { CSSProperties, defineComponent } from 'vue'

export default defineComponent({
  name: 'CustomTable',
  props: initDefaultProps(modalProps(), {
    width: 520,
    confirmLoading: false,
    okType: 'primary'
  }),
  slots: Object as CustomSlotsType<{ default: any }>,
  setup(props, { slots }) {
    const bodyStyle: CSSProperties = {
      maxHeight: 'calc(100vh - 196px )',
      overflowY: 'auto'
    }
    console.log(slots, props)
    return {
      props,
      slots,
      bodyStyle
    }
  }
})
</script>
