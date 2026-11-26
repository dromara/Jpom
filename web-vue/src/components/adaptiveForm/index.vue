<!--

    Copyright (c) 2019 Of Him Code Technology Studio
    Jpom is licensed under Mulan PSL v2.
    You can use this software according to the terms and conditions of the Mulan PSL v2.
    You may obtain a copy of Mulan PSL v2 at:
    			http://license.coscl.org.cn/MulanPSL2
    THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
    See the Mulan PSL v2 for more details.

-->
<template>
  <div ref="wrap" class="adaptive-form-wrap">
    <a-form
      v-bind="$attrs"
      :class="['adaptive-form', 'adaptive-form--' + effectiveLayout]"
      :layout="effectiveLayout"
      :style="formStyle"
    >
      <slot />
    </a-form>
  </div>
</template>

<script>
/**
 * AdaptiveForm —— 可变宽容器里的表单（默认一行一个字段）标签宽度自适应组件。
 *
 * <p>背景（2026-11-25 用户反馈）：流水线编辑的「阶段配置」面板宽度可以拖拽（26%~62%），
 * 也可以把画布折叠后占满整行，但表单用的是百分比分栏（labelCol span 7 / wrapperCol span 17）。
 * 百分比分栏有两个必然结果：标签文本宽度不会跟着容器变 → 面板越宽标签列留白越大（左侧一大片空白），
 * 面板越窄控件可用宽度越小。这不是某一个页面的问题，而是「可变宽容器 + 百分比 labelCol」这一组合的必然结果。</p>
 *
 * <p>契约（三条，组件自身保证）：</p>
 * <ul>
 *   <li>标签列宽按「像素」给，且只在两档之间切换（宽档 / 窄档），不随容器线性膨胀；</li>
 *   <li>容器窄到放不下横向标签时，整表切竖排（标签在控件上方），把横向空间全部让给控件；</li>
 *   <li>判定依据是组件自身宽度（ResizeObserver），不是视口断点：拖拽分割线、折叠侧栏、窗口缩放都会重新判定。</li>
 * </ul>
 *
 * <p>用法：把 <code>&lt;a-form layout="horizontal" :label-col :wrapper-col&gt;</code> 直接替换成
 * <code>&lt;adaptive-form&gt;</code>，其余属性（model / rules / 事件）原样透传；容器是不可变宽的固定弹窗时，
 * 继续用 <code>label-col</code> 也可以，但必须写像素（<code>{ flex: '112px' }</code>），禁止百分比分栏。</p>
 */
export default {
  name: 'AdaptiveForm',
  inheritAttrs: false,
  props: {
    /** auto：按自身宽度自动判定；horizontal / vertical：强制布局 */
    layoutMode: { type: String, default: 'auto' },
    /** 容器宽度小于该值时切竖排（标签在控件上方，不再横向分栏） */
    inlineMinWidth: { type: Number, default: 420 },
    /** 容器宽度小于该值时用窄档标签宽度 */
    tightWidth: { type: Number, default: 620 },
    /** 宽档标签列宽度（px） */
    labelWidth: { type: Number, default: 112 },
    /** 窄档标签列宽度（px） */
    labelWidthTight: { type: Number, default: 88 }
  },
  data() {
    return {
      // -1 表示还没量到（首帧渲染按横向宽档，避免闪一下竖排）
      containerWidth: -1
    }
  },
  computed: {
    effectiveLayout() {
      if (this.layoutMode !== 'auto') {
        return this.layoutMode
      }
      if (this.containerWidth <= 0) {
        return 'horizontal'
      }
      return this.containerWidth < this.inlineMinWidth ? 'vertical' : 'horizontal'
    },
    effectiveLabelWidth() {
      if (this.containerWidth > 0 && this.containerWidth < this.tightWidth) {
        return this.labelWidthTight
      }
      return this.labelWidth
    },
    formStyle() {
      return { '--af-label-width': this.effectiveLabelWidth + 'px' }
    }
  },
  mounted() {
    this.observer = null
    if (typeof ResizeObserver !== 'undefined' && this.$refs.wrap) {
      this.observer = new ResizeObserver((entries) => {
        const entry = entries && entries[0]
        const width = entry ? (entry.contentRect ? entry.contentRect.width : entry.target.clientWidth) : 0
        this.setWidth(width)
      })
      this.observer.observe(this.$refs.wrap)
    }
    this.setWidth(this.$refs.wrap ? this.$refs.wrap.clientWidth : 0)
    window.addEventListener('resize', this.onWindowResize)
  },
  beforeUnmount() {
    if (this.observer) {
      this.observer.disconnect()
      this.observer = null
    }
    window.removeEventListener('resize', this.onWindowResize)
  },
  methods: {
    setWidth(width) {
      const next = Math.round(Number(width) || 0)
      // display:none（v-show 折叠 / 步骤未激活）时宽度为 0：保留上一次判定，折叠再展开不会闪竖排
      if (next > 0 && next !== this.containerWidth) {
        this.containerWidth = next
      }
    },
    onWindowResize() {
      if (this.$refs.wrap) {
        this.setWidth(this.$refs.wrap.clientWidth)
      }
    }
  }
}
</script>

<style scoped>
.adaptive-form-wrap {
  width: 100%;
  min-width: 0;
}
/* 横向：标签列只占固定像素（宽档 112 / 窄档 88），剩余宽度全部给控件 */
.adaptive-form--horizontal :deep(.ant-form-item-row) {
  flex-wrap: nowrap;
  align-items: flex-start;
}
.adaptive-form--horizontal :deep(.ant-form-item-label) {
  flex: 0 0 var(--af-label-width, 112px);
  max-width: var(--af-label-width, 112px);
  text-align: right;
  padding-right: 8px;
}
.adaptive-form--horizontal :deep(.ant-form-item-label > label) {
  justify-content: flex-end;
  white-space: normal;
  line-height: 1.4;
}
.adaptive-form--horizontal :deep(.ant-form-item-control) {
  flex: 1 1 0;
  min-width: 0;
}
/* 竖排：标签在控件上方，横向空间 100% 给控件 */
.adaptive-form--vertical :deep(.ant-form-item-label) {
  padding-bottom: 2px;
}
.adaptive-form--vertical :deep(.ant-form-item-control) {
  min-width: 0;
}
</style>
