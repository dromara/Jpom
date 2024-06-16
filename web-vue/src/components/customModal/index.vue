<template>
  <a-modal v-bind="props" :body-style="bodyStyle" :z-index="zIndex">
    <slot name="default"></slot>
    <template v-if="slots.footer" #footer>
      <slot name="footer"></slot>
    </template>
  </a-modal>
</template>
<script lang="ts">
import { modalProps } from 'ant-design-vue/es/modal/Modal'
import { initDefaultProps } from 'ant-design-vue/es/_util/props-util'
import { CustomSlotsType } from 'ant-design-vue/es/_util/type'
import { CSSProperties, defineComponent } from 'vue'
import { increaseZIndex } from '@/utils/utils'

export default defineComponent({
  name: 'CustomModal',
  props: initDefaultProps(modalProps(), {
    width: 520,
    confirmLoading: false,
    okType: 'primary'
  }),
  slots: Object as CustomSlotsType<{ default: any; footer?: any }>,
  setup(props, { emit, slots }) {
    const bodyStyle: CSSProperties = {
      maxHeight: 'calc(100vh - 196px )',
      overflowY: 'auto',
      ...props.bodyStyle
    }

    return {
      props,
      zIndex: increaseZIndex(),
      bodyStyle,
      emit,
      slots
    }
  }
})
</script>
