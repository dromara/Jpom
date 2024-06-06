<template>
  <div class="">
    <a-tabs default-active-key="1" tab-position="left">
      <a-tab-pane key="1" :tab="$t('pages.node.node-layout.system.cache.f84aaf3f')">
        <a-alert
          :message="$t('pages.node.node-layout.system.cache.74dc9002')"
          style="margin-top: 10px; margin-bottom: 40px"
          banner
        />
        <a-timeline>
          <a-timeline-item v-if="temp.dateTime">
            <span class="layui-elem-quote">
              {{ $t('pages.node.node-layout.system.cache.8ad18624') }}{{ temp.dateTime }}
              <a-tag>{{ temp.timeZoneId }}</a-tag>
            </span>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote"
              >{{ $t('pages.node.node-layout.system.cache.45c2d461') }}{{ renderSize(temp.dataSize) }}</span
            >
          </a-timeline-item>
          <a-timeline-item v-if="temp.fileSize">
            <a-space>
              <span class="layui-elem-quote"
                >{{ $t('pages.node.node-layout.system.cache.2857c7f8') }}{{ renderSize(temp.fileSize) }}</span
              >
              <a-button size="small" type="primary" class="btn" @click="clear('fileSize')">{{
                $t('pages.node.node-layout.system.cache.3907eb5b')
              }}</a-button>
            </a-space>
          </a-timeline-item>
          <a-timeline-item v-if="temp.oldJarsSize">
            <a-space>
              <span class="layui-elem-quote"
                >{{ $t('pages.node.node-layout.system.cache.890d7cd0') }}{{ renderSize(temp.oldJarsSize) }}</span
              >
              <a-button size="small" type="primary" class="btn" @click="clear('oldJarsSize')">{{
                $t('pages.node.node-layout.system.cache.3907eb5b')
              }}</a-button>
            </a-space>
          </a-timeline-item>

          <a-timeline-item>
            <a-space>
              <span class="layui-elem-quote"
                >{{ $t('pages.node.node-layout.system.cache.c50e1b39') }}{{ temp.pidPort }}</span
              >
              <a-button v-if="temp.pidPort" size="small" type="primary" class="btn" @click="clear('pidPort')">{{
                $t('pages.node.node-layout.system.cache.3907eb5b')
              }}</a-button>
            </a-space>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote"
              >{{ $t('pages.node.node-layout.system.cache.5fb51609') }}{{ temp.scriptExecLogSize }}</span
            >
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote"
              >{{ $t('pages.node.node-layout.system.cache.4d78c1a4') }}{{ temp.readFileOnLineCount }}</span
            >
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote"
              >{{ $t('pages.node.node-layout.system.cache.bf8950b2') }}{{ temp.pluginSize || 0 }}</span
            >
          </a-timeline-item>
          <a-timeline-item>
            <div class="layui-elem-quote">
              {{ $t('pages.node.node-layout.system.cache.c317de67') }}
              <a-tag v-for="(item, index) in temp.envVarKeys" :key="index">
                <a-tooltip :title="`${$t('pages.node.node-layout.system.cache.632d5d79')}:${item}`">
                  {{ item }}
                </a-tooltip>
              </a-tag>
            </div>
          </a-timeline-item>
        </a-timeline>
      </a-tab-pane>
      <a-tab-pane key="2" :tab="$t('pages.node.node-layout.system.cache.1cf16f46')">
        <task-stat :task-list="taskList" @refresh="loadData"
      /></a-tab-pane>
      <a-tab-pane key="3" :tab="$t('pages.node.node-layout.system.cache.78ea56d4')">
        <a-space direction="vertical" style="width: 100%">
          <a-alert :message="$t('pages.node.node-layout.system.cache.90cf9dce')" type="warning" show-icon>
            <template #description>
              <ul>
                <li>{{ $t('pages.node.node-layout.system.cache.f4768643') }}</li>
                <li>{{ $t('pages.node.node-layout.system.cache.e09b7fd4') }}</li>
                <li>{{ $t('pages.node.node-layout.system.cache.6c106822') }}</li>
                <li>{{ $t('pages.node.node-layout.system.cache.857b8a0d') }}</li>
                <li>{{ $t('pages.node.node-layout.system.cache.714a17c') }}</li>
              </ul>
            </template>
          </a-alert>
          <a-list size="small" bordered :data-source="machineLonelyData.projects">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-space>
                  <span>{{ $t('pages.node.node-layout.system.cache.e06912d') }}{{ item.name }}</span>
                  <span>{{ $t('pages.node.node-layout.system.cache.4eaba425') }}{{ item.id }}</span>
                  <span>{{ $t('pages.node.node-layout.system.cache.97772e66') }}{{ item.workspaceId }}</span>
                  <span>{{ $t('pages.node.node-layout.system.cache.1c648cd4') }}{{ item.nodeId }}</span>
                  <a-button type="primary" size="small" danger @click="openCorrectLonely(item, 'project')">{{
                    $t('pages.node.node-layout.system.cache.b1e41588')
                  }}</a-button>
                </a-space>
              </a-list-item>
            </template>
            <template #header>
              <div>{{ $t('pages.node.node-layout.system.cache.2caa09af') }}</div>
            </template>
          </a-list>
          <a-list size="small" bordered :data-source="machineLonelyData.scripts">
            <template #renderItem="{ item }">
              <a-list-item
                ><a-space>
                  <span>{{ $t('pages.node.node-layout.system.cache.db9bba81') }}{{ item.name }}</span>
                  <span>{{ $t('pages.node.node-layout.system.cache.461afc3f') }}{{ item.id }}</span>
                  <span>{{ $t('pages.node.node-layout.system.cache.97772e66') }}{{ item.workspaceId }}</span>
                  <span>{{ $t('pages.node.node-layout.system.cache.1c648cd4') }}{{ item.nodeId }}</span>
                  <a-button type="primary" size="small" danger @click="openCorrectLonely(item, 'script')">{{
                    $t('pages.node.node-layout.system.cache.b1e41588')
                  }}</a-button>
                </a-space>
              </a-list-item>
            </template>
            <template #header>
              <div>{{ $t('pages.node.node-layout.system.cache.300d088d') }}</div>
            </template>
          </a-list></a-space
        >
      </a-tab-pane>
    </a-tabs>
    <!-- 分配到其他工作空间 -->
    <a-modal
      v-model:open="correctLonelyOpen"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('pages.node.node-layout.system.cache.6a8b7888')"
      :mask-closable="false"
      @ok="handleCorrectLonely"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert :message="$t('pages.node.node-layout.system.cache.2774e4a7')" type="warning">
          <template #description>
            <ul>
              <li>{{ $t('pages.node.node-layout.system.cache.b28bd9b1') }}</li>
              <li>{{ $t('pages.node.node-layout.system.cache.478331d8') }}</li>
            </ul>
          </template>
        </a-alert>
        <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
          <a-form-item :label="$t('pages.node.node-layout.system.cache.580e6c10')" name="nodeId">
            <a-select
              v-model:value="temp.toNodeId"
              show-search
              :filter-option="
                (input, option) => {
                  const children = option.children && option.children()
                  return (
                    children &&
                    children[0].children &&
                    children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  )
                }
              "
              :disabled="temp.toNodeId && temp.recommend"
              :placeholder="$t('pages.node.node-layout.system.cache.2388531c')"
            >
              <a-select-option v-for="item in nodeList" :key="item.id">
                【{{ item.workspace && item.workspace.name }}】{{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-space>
    </a-modal>
  </div>
</template>

<script>
import { getNodeCache, clearCache } from '@/api/system'
import TaskStat from '@/pages/system/taskStat'
import { renderSize } from '@/utils/const'
import { machineLonelyData, machineCorrectLonelyData, machineListNode } from '@/api/system/assets-machine'
export default {
  components: {
    TaskStat
  },
  props: {
    machineId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      temp: {},
      taskList: [],
      machineLonelyDataLoading: true,
      machineLonelyData: {},
      correctLonelyOpen: false,
      confirmLoading: false,
      nodeList: []
    }
  },
  mounted() {
    this.loadData()
    this.listMachineLonelyData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.node.nodeLayout.system.cache.${key}`, ...args)
    },
    // parseTime,
    renderSize,
    // load data
    loadData() {
      getNodeCache({
        machineId: this.machineId
      }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data
          this.taskList = res.data?.taskList
        }
      })
    },
    // clear
    clear(type) {
      const params = {
        type: type,
        machineId: this.machineId
      }
      clearCache(params).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    },
    // 查询孤独数据
    listMachineLonelyData() {
      this.machineLonelyDataLoading = true
      machineLonelyData({
        id: this.machineId
      })
        .then((res) => {
          if (res.code === 200 && res.data) {
            this.machineLonelyData = res.data
          }
        })
        .finally(() => {
          this.machineLonelyDataLoading = false
        })
    },
    // 查询机器关联的节点
    listMachineNode(item) {
      machineListNode({
        id: this.machineId
      }).then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data || []
          if (item) {
            const find = this.nodeList.filter((node) => {
              return node.id === item.nodeId && node.workspaceId === item.workspaceId
            })
            if (find && find.length === 1) {
              this.temp = { ...this.temp, toNodeId: find[0].id, recommend: true }
            }
          }
        }
      })
    },
    // 打开修正窗口
    openCorrectLonely(item, type) {
      this.temp = { type: type, id: this.machineId, dataId: item.id }
      this.correctLonelyOpen = true
      this.listMachineNode(item)
    },
    //确认修正
    handleCorrectLonely() {
      if (!this.temp.toNodeId) {
        $notification.warn({
          message: this.$t('pages.node.node-layout.system.cache.2388531c')
        })
        return false
      }
      this.confirmLoading = true
      machineCorrectLonelyData(this.temp)
        .then((res) => {
          if (res.code == 200) {
            $notification.success({
              message: res.msg
            })
            this.correctLonelyOpen = false
            this.listMachineLonelyData()
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    }
  }
}
</script>
