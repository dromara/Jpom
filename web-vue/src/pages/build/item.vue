<template>
  <div>
    <!-- 编辑区 -->
    <a-drawer
      destroyOnClose
      :open="true"
      @close="
        () => {
          $emit('close')
        }
      "
      :bodyStyle="{
        padding: '0'
      }"
      :headerStyle="{
        padding: '0 10px'
      }"
      width="70vw"
      :maskClosable="false"
      :footer-style="{ textAlign: 'right' }"
    >
      <template #title>
        <template v-if="id">
          <a-menu mode="horizontal" class="menu" v-model:selectedKeys="menuKey" @click="menuClick">
            <a-menu-item key="info">
              <span><InfoOutlined /> 构建信息</span>
            </a-menu-item>
            <a-menu-item key="edit">
              <span> <EditOutlined /> 编辑构建</span>
            </a-menu-item>
            <a-menu-item key="trigger">
              <span><ApiOutlined /> 触发器</span>
            </a-menu-item>
          </a-menu>
        </template>
        <template v-else>
          <a-menu mode="horizontal" class="menu" v-model:selectedKeys="menuKey" @click="menuClick">
            <a-menu-item key="edit">
              <span> <EditOutlined /> 新增构建</span>
            </a-menu-item>
          </a-menu>
        </template>
      </template>

      <div class="layout-content">
        <detailsPage v-if="id" :id="id" v-show="menuKey.includes('info')" />
        <editBuildPage
          v-show="menuKey.includes('edit')"
          ref="editBuild"
          :id="id"
          :data="data"
          v-model:editSteps="stepsCurrent"
          @confirm="
            (build, buildId) => {
              $emit('build', build, buildId)
            }
          "
          @changeEditSteps="
            (current) => {
              this.stepsCurrent = current
            }
          "
        ></editBuildPage>
        <triggerPage v-if="id" :id="id" v-show="menuKey.includes('trigger')" />
      </div>
      <!-- <template> </template> -->

      <template #footer v-if="menuKey.includes('edit')">
        <a-space>
          <a-button
            @click="
              () => {
                $emit('close')
              }
            "
          >
            取消
          </a-button>
          <a-tooltip
            v-if="id"
            title="如果当前构建信息已经在其他页面更新过，需要点击刷新按钮来获取最新的信息，点击刷新后未保存的数据也将丢失"
          >
            <a-button @click="$refs.editBuild.refresh()"> 刷新</a-button>
          </a-tooltip>
          <a-divider type="vertical" />
          <a-button
            type="primary"
            :disabled="stepsCurrent === 0"
            @click="
              () => {
                stepsCurrent = stepsCurrent - 1
              }
            "
            >上一步</a-button
          >
          <a-button
            type="primary"
            :disabled="stepsCurrent === 4"
            @click="
              () => {
                stepsCurrent = stepsCurrent + 1
              }
            "
            >下一步</a-button
          >
          <a-divider type="vertical" />

          <a-button type="primary" @click="$refs.editBuild.handleEditBuildOk(false)"> 保存 </a-button>
          <a-button type="primary" @click="$refs.editBuild.handleEditBuildOk(true)"> 保存并构建 </a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>
<script>
import detailsPage from './details.vue'
import editBuildPage from './edit.vue'
import triggerPage from './trigger.vue'
export default {
  components: {
    detailsPage,
    triggerPage,
    editBuildPage
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    visibleType: {
      type: Number
    },
    data: {
      type: Object,
      default: null
    },
    editSteps: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      menuKey: ['info'],
      stepsCurrent: this.editSteps
    }
  },
  created() {
    const array = ['info', 'edit', 'trigger']
    if (this.id) {
      this.menuKey = [array[this.visibleType - 1]]
    } else {
      this.menuKey = [array[1]]
    }
  },
  methods: {
    menuClick(item) {
      this.menuKey = item.key
    },
    onClose() {
      this.$emit('close')
    }
  },
  emits: ['close', 'build']
}
</script>
<style scoped>
.menu {
  border-bottom: 0;
}

.layout-content {
  padding: 0;
  margin: 15px;
}
</style>
