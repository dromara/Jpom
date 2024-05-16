<template>
  <div class="">
    <a-tabs default-active-key="1" tab-position="left">
      <a-tab-pane key="1" :tab="$tl('p.cacheInfo')">
        <a-alert :message="$tl('p.dataDirectoryWarning')" style="margin-top: 10px; margin-bottom: 40px" banner />
        <a-timeline>
          <a-timeline-item v-if="temp.dateTime">
            <span class="layui-elem-quote">
              {{ $tl('p.pluginTime') }}{{ temp.dateTime }}
              <a-tag>{{ temp.timeZoneId }}</a-tag>
            </span>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote">{{ $tl('p.dataDirectorySpace') }}{{ renderSize(temp.dataSize) }}</span>
          </a-timeline-item>
          <a-timeline-item v-if="temp.fileSize">
            <a-space>
              <span class="layui-elem-quote">{{ $tl('p.tempFileSpace') }}{{ renderSize(temp.fileSize) }}</span>
              <a-button size="small" type="primary" class="btn" @click="clear('fileSize')">{{
                $tl('c.clear')
              }}</a-button>
            </a-space>
          </a-timeline-item>
          <a-timeline-item v-if="temp.oldJarsSize">
            <a-space>
              <span class="layui-elem-quote">{{ $tl('p.oldPackageSpace') }}{{ renderSize(temp.oldJarsSize) }}</span>
              <a-button size="small" type="primary" class="btn" @click="clear('oldJarsSize')">{{
                $tl('c.clear')
              }}</a-button>
            </a-space>
          </a-timeline-item>

          <a-timeline-item>
            <a-space>
              <span class="layui-elem-quote">{{ $tl('p.processPortCache') }}{{ temp.pidPort }}</span>
              <a-button v-if="temp.pidPort" size="small" type="primary" class="btn" @click="clear('pidPort')">{{
                $tl('c.clear')
              }}</a-button>
            </a-space>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote">{{ $tl('p.scriptLogCount') }}{{ temp.scriptExecLogSize }}</span>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote">{{ $tl('p.readingLogFileCount') }}{{ temp.readFileOnLineCount }}</span>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote">{{ $tl('p.pluginCount') }}{{ temp.pluginSize || 0 }}</span>
          </a-timeline-item>
          <a-timeline-item>
            <div class="layui-elem-quote">
              {{ $tl('p.environmentVariable') }}
              <a-tag v-for="(item, index) in temp.envVarKeys" :key="index">
                <a-tooltip :title="`${$tl('p.environmentVariableKey')}:${item}`">
                  {{ item }}
                </a-tooltip>
              </a-tag>
            </div>
          </a-timeline-item>
        </a-timeline>
      </a-tab-pane>
      <a-tab-pane key="2" :tab="$tl('p.scheduledTask')">
        <task-stat :task-list="taskList" @refresh="loadData"
      /></a-tab-pane>
      <a-tab-pane key="3" :tab="$tl('p.lonelyData')">
        <a-space direction="vertical" style="width: 100%">
          <a-alert :message="$tl('p.lonelyDataDesc')" type="warning" show-icon>
            <template #description>
              <ul>
                <li>{{ $tl('p.lonelyDataDetail') }}</li>
                <li>{{ $tl('p.lonelyDataCause') }}</li>
                <li>{{ $tl('p.lonelyDataEffect') }}</li>
                <li>{{ $tl('p.oldVersionData') }}</li>
                <li>{{ $tl('p.multipleBinding') }}</li>
              </ul>
            </template>
          </a-alert>
          <a-list size="small" bordered :data-source="machineLonelyData.projects">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-space>
                  <span>{{ $tl('p.projectName') }}{{ item.name }}</span>
                  <span>{{ $tl('p.projectId') }}{{ item.id }}</span>
                  <span>{{ $tl('c.workspaceId') }}{{ item.workspaceId }}</span>
                  <span>{{ $tl('c.nodeId') }}{{ item.nodeId }}</span>
                  <a-button type="primary" size="small" danger @click="openCorrectLonely(item, 'project')">{{
                    $tl('c.correction')
                  }}</a-button>
                </a-space>
              </a-list-item>
            </template>
            <template #header>
              <div>{{ $tl('p.projectLonelyData') }}</div>
            </template>
          </a-list>
          <a-list size="small" bordered :data-source="machineLonelyData.scripts">
            <template #renderItem="{ item }">
              <a-list-item
                ><a-space>
                  <span>{{ $tl('p.scriptName') }}{{ item.name }}</span>
                  <span>{{ $tl('p.scriptId') }}{{ item.id }}</span>
                  <span>{{ $tl('c.workspaceId') }}{{ item.workspaceId }}</span>
                  <span>{{ $tl('c.nodeId') }}{{ item.nodeId }}</span>
                  <a-button type="primary" size="small" danger @click="openCorrectLonely(item, 'script')">{{
                    $tl('c.correction')
                  }}</a-button>
                </a-space>
              </a-list-item>
            </template>
            <template #header>
              <div>{{ $tl('p.scriptLonelyData') }}</div>
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
      :title="$tl('p.correctLonelyData')"
      :mask-closable="false"
      @ok="handleCorrectLonely"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert :message="$tl('p.warning')" type="warning">
          <template #description>
            <ul>
              <li>{{ $tl('p.correctionEffect') }}</li>
              <li>{{ $tl('p.disabledNodeTip') }}</li>
            </ul>
          </template>
        </a-alert>
        <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
          <a-form-item :label="$tl('p.selectNode')" name="nodeId">
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
              :placeholder="$tl('c.selectNode')"
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
          message: this.$tl('c.selectNode')
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
