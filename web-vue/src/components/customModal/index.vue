<template>
  <a-modal v-bind="props" :body-style="bodyStyle" :z-index="zIndex">
    <slot name="default"></slot>
    <template v-if="slots.footer" #footer>
      <slot name="footer"></slot>
    </template>
    <template v-if="slots.title" #title>
      <slot name="title"></slot>
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
    okType: 'primary',
    wrapClassName: 'diy-custom-modal'
  }),
  slots: Object as CustomSlotsType<{ default: any; footer?: any; title?: any }>,
  setup(props, { emit, slots }) {
    const diyBodyStyle: CSSProperties = { maxHeight: 'calc(100vh - 240px )', overflowY: 'auto', padding: '8px 0px' }
    if (props?.bodyStyle?.height) {
      // 如果指定了高，内部不做限制（外部可能要求全屏）
      delete diyBodyStyle.maxHeight
    }
    const bodyStyle: CSSProperties = {
      ...diyBodyStyle,
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
<style lang="less">
.diy-custom-modal {
  .ant-modal-header {
    box-shadow: 0 1px 0 rgb(152 152 152 / 8%);
    padding-bottom: 8px;
    margin-bottom: 0;
  }
  .ant-modal-footer {
    box-shadow: 0 -1px 0 rgba(152 152 152 / 8%);
    margin-top: 0;
    padding-top: 8px;
  }
}
</style>
