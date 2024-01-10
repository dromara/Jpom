<template>
  <div class="">
    <a-tabs default-active-key="1" tab-position="left">
      <a-tab-pane key="1" tab="缓存信息">
        <a-alert
          message="请勿手动删除数据目录下面文件,如果需要删除需要提前备份或者已经确定对应文件弃用后才能删除"
          style="margin-top: 10px; margin-bottom: 40px"
          banner
        />
        <a-timeline>
          <a-timeline-item v-if="temp.dateTime">
            <span class="layui-elem-quote">
              插件端时间：{{ temp.dateTime }}
              <a-tag>{{ temp.timeZoneId }}</a-tag>
            </span>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote">数据目录占用空间：{{ renderSize(temp.dataSize) }}</span>
          </a-timeline-item>
          <a-timeline-item v-if="temp.fileSize">
            <a-space>
              <span class="layui-elem-quote">临时文件占用空间：{{ renderSize(temp.fileSize) }}</span>
              <a-button size="small" type="primary" class="btn" @click="clear('fileSize')">清空</a-button>
            </a-space>
          </a-timeline-item>
          <a-timeline-item v-if="temp.oldJarsSize">
            <a-space>
              <span class="layui-elem-quote">旧版程序包占有空间：{{ renderSize(temp.oldJarsSize) }}</span>
              <a-button size="small" type="primary" class="btn" @click="clear('oldJarsSize')">清空</a-button>
            </a-space>
          </a-timeline-item>

          <a-timeline-item>
            <a-space>
              <span class="layui-elem-quote">进程端口缓存：{{ temp.pidPort }}</span>
              <a-button size="small" v-if="temp.pidPort" type="primary" class="btn" @click="clear('pidPort')"
                >清空</a-button
              >
            </a-space>
          </a-timeline-item>
          <!-- <a-timeline-item>
          <span class="layui-elem-quote">错误进程缓存：{{temp.pidError}}</span>
          <a-button type="primary" class="btn" @click="clear('pidError')">清空</a-button>
        </a-timeline-item> -->
          <a-timeline-item>
            <span class="layui-elem-quote">在读取的日志文件数：{{ temp.readFileOnLineCount }}</span>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote">插件数：{{ temp.pluginSize || 0 }}</span>
          </a-timeline-item>
          <a-timeline-item>
            <div class="layui-elem-quote">
              环境变量：
              <a-tag v-for="(item, index) in temp.envVarKeys" :key="index">
                <a-tooltip :title="`环境变量的key:${item}`">
                  {{ item }}
                </a-tooltip>
              </a-tag>
            </div>
          </a-timeline-item>
        </a-timeline>
      </a-tab-pane>
      <a-tab-pane key="2" tab="定时任务"> <task-stat :taskList="taskList" @refresh="loadData" /></a-tab-pane>
      <a-tab-pane key="3" tab="孤独数据">
        <a-space direction="vertical" style="width: 100%">
          <a-alert message="何为孤独数据" type="warning" show-icon>
            <template #description>
              <ul>
                <li>
                  孤独数据是值机器几点里面存在数据，但是无非和当前系统绑定上关系（关闭绑定节点ID+工作空间ID对应才行），一般情况下不会出现这样的数据
                </li>
                <li>通常情况为项目迁移工作空间、迁移物理机器等一些操作可能产生孤独数据</li>
                <li>如果孤独数据被工作空间下的其他功能关联，修正后关联的数据将失效对应功能无非查询到关联数据</li>
                <li>低版本项目数据未存储节点ID，对应项目数据也将出来在孤独数据中（此类数据不影响使用）</li>
              </ul>
            </template>
          </a-alert>
          <a-list size="small" bordered :data-source="machineLonelyData.projects">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-space>
                  <span>项目名称：{{ item.name }}</span>
                  <span>项目ID：{{ item.id }}</span>
                  <span>工作空间ID：{{ item.workspaceId }}</span>
                  <span>节点ID：{{ item.nodeId }}</span>
                  <a-button type="primary" size="small" danger @click="openCorrectLonely(item, 'project')"
                    >修正</a-button
                  >
                </a-space>
              </a-list-item>
            </template>
            <template #header>
              <div>项目孤独数据</div>
            </template>
          </a-list>
          <a-list size="small" bordered :data-source="machineLonelyData.scripts">
            <template #renderItem="{ item }">
              <a-list-item
                ><a-space>
                  <span>脚本名称：{{ item.name }}</span>
                  <span>脚本ID：{{ item.id }}</span>
                  <span>工作空间ID：{{ item.workspaceId }}</span>
                  <span>节点ID：{{ item.nodeId }}</span>
                  <a-button type="primary" size="small" danger @click="openCorrectLonely(item, 'script')"
                    >修正</a-button
                  >
                </a-space>
              </a-list-item>
            </template>
            <template #header>
              <div>脚本孤独数据</div>
            </template>
          </a-list></a-space
        >
      </a-tab-pane>
    </a-tabs>
    <!-- 分配到其他工作空间 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="correctLonelyOpen"
      title="修正孤独数据"
      @ok="handleCorrectLonely"
      :maskClosable="false"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert message="温馨提示" type="warning">
          <template #description>
            <ul>
              <li>修改后如果有原始关联数据将失效，需要重新配置关联</li>
              <li>如果节点选项是禁用，则表示对应数据有推荐关联节点（低版本项目数据可能出现此情况）</li>
            </ul>
          </template>
        </a-alert>
        <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
          <a-form-item label="选择节点" name="nodeId">
            <a-select
              show-search
              option-filter-prop="children"
              :disabled="temp.toNodeId && temp.recommend"
              v-model:value="temp.toNodeId"
              placeholder="请选择节点"
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
      type: String
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
      nodeList: [],
      temp: {}
    }
  },
  mounted() {
    this.loadData()
    this.listMachineLonelyData()
  },
  methods: {
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
          message: '请选择节点'
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
