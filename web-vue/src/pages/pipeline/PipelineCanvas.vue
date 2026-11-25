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
  <div class="pipeline-canvas">
    <VueFlow
      :id="flowId"
      v-model:nodes="nodes"
      v-model:edges="edges"
      :default-viewport="{ x: 24, y: 24, zoom: 1 }"
      :min-zoom="0.3"
      :max-zoom="1.6"
      :nodes-draggable="true"
      :pan-on-drag="false"
      :zoom-on-scroll="false"
      :node-drag-threshold="1"
      :nodes-connectable="false"
      :elements-selectable="true"
      @init="onInit"
      @nodes-initialized="fitView"
      @node-click="onNodeClick"
      @node-drag-stop="onDragStop"
    >
      <template #node-stage="props">
        <div class="flow-node" :class="{ selected: props.data.index === selected, off: !props.data.enabled }">
          <div class="flow-node-head">
            <span class="no">{{ props.data.index + 1 }}</span>
            <b>{{ props.data.name }}</b>
            <span class="tag">{{ props.data.typeText }}</span>
            <span v-if="props.data.parallel" class="tag tag-parallel">{{ $t('i18n_pipeline_release_parallel_label') }}</span>
          </div>
          <div class="flow-node-body">{{ props.data.summary }}</div>
          <div class="flow-node-foot" :class="{ handled: props.data.handled }">
            <span v-if="props.data.handled">{{ $t('i18n_pipeline_stage_configured') }}</span>
            <span v-else>{{ $t('i18n_pipeline_stage_unconfigured') }}</span>
          </div>
        </div>
      </template>
      <!-- 网格点颜色由 CSS 变量控制（见下方 :deep(.vue-flow__background circle)），不在模板里写死色值 -->
      <Background :gap="16" />
      <Controls />
    </VueFlow>
  </div>
</template>

<script>
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'

const X_GAP = 260

export default {
  name: 'PipelineCanvas',
  components: { VueFlow, Background, Controls },
  props: {
    stages: { type: Array, default: () => [] },
    selected: { type: Number, default: -1 }
  },
  emits: ['select', 'reorder'],
  data() {
    return {
      nodes: [],
      edges: [],
      flowId: 'pipeline-flow-' + Math.random().toString(36).slice(2, 8),
      vueFlow: null,
      lastCount: -1
    }
  },
  watch: {
    stages: {
      handler() {
        this.render()
      },
      deep: true
    },
    selected() {
      this.render()
    }
  },
  mounted() {
    this.render()
    // 面板折叠、拖拽分割线、改窗口都会让画布尺寸突变：尺寸稳定后重新 fitView，
    // 否则节点会停在旧视口位置（甚至跑到可视区外）。
    if (typeof ResizeObserver !== 'undefined') {
      let timer = null
      let lastWidth = 0
      let lastHeight = 0
      this.resizeObserver = new ResizeObserver((entries) => {
        const rect = entries[0] && entries[0].contentRect
        if (!rect) {
          return
        }
        if (Math.abs(rect.width - lastWidth) < 24 && Math.abs(rect.height - lastHeight) < 24) {
          return
        }
        lastWidth = rect.width
        lastHeight = rect.height
        clearTimeout(timer)
        timer = setTimeout(() => this.fitView(), 180)
      })
      this.resizeObserver.observe(this.$el)
    }
  },
  beforeUnmount() {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect()
      this.resizeObserver = null
    }
  },
  methods: {
    typeText(type) {
      const map = {
        BUILD: this.$t('i18n_pipeline_stage_type_build'),
        COMMAND: this.$t('i18n_pipeline_stage_type_command'),
        RELEASE: this.$t('i18n_pipeline_stage_type_release'),
        NOTIFY: this.$t('i18n_pipeline_stage_type_notify')
      }
      return map[type] || type
    },
    summary(stage) {
      if (stage.type === 'BUILD' || stage.type === 'COMMAND') {
        const where =
          stage.executor === 'container'
            ? this.$t('i18n_pipeline_executor_container')
            : stage.executor === 'node'
              ? this.$t('i18n_pipeline_executor_node')
              : this.$t('i18n_pipeline_executor_local')
        const command = stage.run ? String(stage.run).split('\n')[0].slice(0, 26) : this.$t('i18n_pipeline_stage_no_command')
        return where + ' · ' + command
      }
      if (stage.type === 'RELEASE') {
        const mode = stage.parallel ? this.$t('i18n_pipeline_release_parallel_label') : this.$t('i18n_pipeline_release_seq')
        return mode + ' · ' + (stage.targets ? stage.targets.length : 0) + ' ' + this.$t('i18n_pipeline_target_unit')
      }
      if (stage.type === 'NOTIFY') {
        return (stage.scripts ? stage.scripts.length : 0) + ' ' + this.$t('i18n_pipeline_notify_script_unit')
      }
      return ''
    },
    render() {
      const nodes = this.stages.map((stage, index) => ({
        id: stage.id ? 'stage-' + stage.id + '-' + index : 'stage-' + index,
        type: 'stage',
        position: { x: index * X_GAP, y: 24 },
        data: {
          index,
          name: stage.name || this.$t('i18n_pipeline_stage_unnamed'),
          typeText: this.typeText(stage.type),
          summary: this.summary(stage),
          parallel: stage.type === 'RELEASE' && stage.parallel === true,
          enabled: stage.enabled !== false,
          handled: this.stageConfigured(stage)
        }
      }))
      const edges = []
      for (let i = 0; i < nodes.length - 1; i++) {
        edges.push({ id: 'e' + i, source: nodes[i].id, target: nodes[i + 1].id, animated: false, type: 'smoothstep' })
      }
      this.nodes = nodes
      this.edges = edges
      // 节点数量变化时重新适配视口（拖拽排序时不重置，避免和用户抢位置）
      if (this.stages.length !== this.lastCount) {
        this.lastCount = this.stages.length
        // 等节点真正完成测量后再适配视口（直接 nextTick 时节点尺寸还是 0，fitView 不会缩放）
        this.$nextTick(() => setTimeout(() => this.fitView(), 80))
      }
    },
    stageConfigured(stage) {
      if (stage.type === 'BUILD' || stage.type === 'COMMAND') return !!stage.run
      if (stage.type === 'RELEASE') return !!(stage.targets && stage.targets.length)
      if (stage.type === 'NOTIFY') return !!(stage.scripts && stage.scripts.length)
      return false
    },
    fitView() {
      if (this.vueFlow && typeof this.vueFlow.fitView === 'function') {
        this.vueFlow.fitView({ padding: 0.18, maxZoom: 1 })
      }
    },
    onInit(instance) {
      this.vueFlow = instance
      this.$nextTick(() => this.fitView())
    },
    onNodeClick(event) {
      const index = event.node && event.node.data ? event.node.data.index : -1
      if (index >= 0) this.$emit('select', index)
    },
    onDragStop(event) {
      const node = event.node
      if (!node || !node.data) return
      const from = node.data.index
      // 按横坐标重新排序：拖到第几位就是第几位
      const order = this.nodes
        .map((n, i) => ({ i, x: n.id === node.id ? node.position.x : n.position.x }))
        .sort((a, b) => a.x - b.x)
        .map((item) => item.i)
      const to = order.indexOf(from)
      if (to >= 0 && to !== from) {
        this.$emit('reorder', { from, to })
      }
      this.render()
    }
  }
}
</script>

<style scoped>
.pipeline-canvas {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 320px;
  border: 1px solid var(--jp-color-border-secondary);
  border-radius: var(--jp-radius-lg);
  background: var(--jp-color-fill-alter);
  overflow: hidden;
}
.pipeline-canvas :deep(.vue-flow) {
  width: 100%;
  height: 100%;
}
.flow-node {
  pointer-events: none;
  width: 200px;
  color: var(--jp-color-text);
  border: 1px solid var(--jp-color-border);
  border-radius: 10px;
  background: var(--jp-color-bg-container);
  padding: 8px 10px;
  box-shadow: var(--jp-shadow-tertiary);
}
.flow-node.selected {
  border-color: var(--jp-color-primary);
  box-shadow: 0 0 0 3px var(--jp-color-primary-bg);
}
.flow-node.off {
  opacity: 0.55;
}
.flow-node-head {
  display: flex;
  align-items: center;
  gap: 6px;
}
.flow-node-head .no {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--jp-color-fill-tertiary);
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.flow-node-head b {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.flow-node-head .tag {
  font-size: 11px;
  color: var(--jp-color-primary);
  background: var(--jp-color-primary-bg);
  border-radius: 10px;
  padding: 0 6px;
}
.flow-node-head .tag-parallel {
  color: var(--jp-color-warning-text);
  background: var(--jp-color-warning-bg);
}
.flow-node-body {
  font-size: 12px;
  color: var(--jp-color-text-secondary);
  margin-top: 4px;
  min-height: 16px;
}
.flow-node-foot {
  font-size: 11px;
  color: var(--jp-color-warning-text);
  margin-top: 4px;
}
.flow-node-foot.handled {
  color: var(--jp-color-success-text);
}
/* vue-flow 自带主题（@vue-flow/core/dist/theme-default.css、@vue-flow/controls/dist/style.css）
   里把网格点、连线、缩放控件按钮写成了浅色固定值，暗色主题下会变成白块/看不清，
   这里统一改由 --jp-* 变量驱动；选择器用 :deep() 穿透 scoped，优先级高于主题包里的同名规则。 */
.pipeline-canvas :deep(.vue-flow__background circle) {
  fill: var(--jp-color-border);
}
.pipeline-canvas :deep(.vue-flow__edge-path) {
  stroke: var(--jp-color-text-quaternary);
}
.pipeline-canvas :deep(.vue-flow__edge-textbg) {
  fill: var(--jp-color-bg-container);
}
.pipeline-canvas :deep(.vue-flow__controls) {
  box-shadow: var(--jp-shadow-tertiary);
}
.pipeline-canvas :deep(.vue-flow__controls-button) {
  background: var(--jp-color-bg-elevated);
  border-bottom: 1px solid var(--jp-color-border-secondary);
}
.pipeline-canvas :deep(.vue-flow__controls-button:hover) {
  background: var(--jp-color-fill-tertiary);
}
.pipeline-canvas :deep(.vue-flow__controls-button svg) {
  fill: var(--jp-color-text-secondary);
}
</style>
