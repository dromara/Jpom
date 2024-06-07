<template>
  <div>
    <a-form :model="temp" :label-col="{ span: 2 }" :wrapper-col="{ span: 20 }">
      <a-form-item :label="$t('pages.file-manager.release-task.details.e62a5bf3')" name="name">
        <a-input
          :placeholder="$t('pages.file-manager.release-task.details.2ab36118')"
          :disabled="true"
          :value="temp.taskData && temp.taskData.name"
        />
      </a-form-item>

      <a-form-item :label="$t('pages.file-manager.release-task.details.907f888f')" name="taskType">
        <a-radio-group :value="temp.taskData && temp.taskData.taskType" :disabled="true">
          <a-radio :value="0"> SSH </a-radio>
          <a-radio :value="1"> {{ $t('pages.file-manager.release-task.details.602a0a5e') }} </a-radio>
        </a-radio-group>
      </a-form-item>

      <a-form-item name="releasePath" :label="$t('pages.file-manager.release-task.details.b6c9f9c')">
        <a-input
          :placeholder="$t('pages.file-manager.release-task.details.2ab36118')"
          :disabled="true"
          :value="temp.taskData && temp.taskData.releasePath"
        />
      </a-form-item>
      <a-form-item
        name="releasePath"
        :label="$t('pages.file-manager.release-task.details.9c32c887')"
        :help="temp.taskData && temp.taskData.statusMsg"
      >
        {{ statusMap[temp.taskData && temp.taskData.status] || $t('pages.file-manager.release-task.details.ca1cdfa6') }}
      </a-form-item>

      <a-form-item :label="$t('pages.file-manager.release-task.details.8fb8f5f9')">
        <a-tabs :active-key="activeKey" @change="tabCallback">
          <a-tab-pane v-for="item in temp.taskList" :key="item.id">
            <template #tab>
              <LoadingOutlined v-if="!logMap[item.id] || logMap[item.id].run" type="loading" />
              <template v-if="temp.taskData && temp.taskData.taskType === 0">
                {{
                  sshList.filter((item2) => {
                    return item2.id === item.taskDataId
                  })[0] &&
                  sshList.filter((item2) => {
                    return item2.id === item.taskDataId
                  })[0].name
                }}
              </template>
              <template v-else-if="temp.taskData && temp.taskData.taskType === 1">
                {{
                  nodeList.filter((item2) => {
                    return item2.id === item.taskDataId
                  })[0] &&
                  nodeList.filter((item2) => {
                    return item2.id === item.taskDataId
                  })[0].name
                }}
              </template>
              <a-tooltip v-if="item.statusMsg" :title="item.statusMsg"><InfoCircleOutlined /></a-tooltip>
            </template>
            <log-view1 :ref="`logView-${item.id}`" height="60vh" />
          </a-tab-pane>
        </a-tabs>
      </a-form-item>
      <a-form-item :label="$t('pages.file-manager.release-task.details.86c3791a')" name="releaseBeforeCommand">
        <a-tabs tab-position="right">
          <a-tab-pane key="before" :tab="$t('pages.file-manager.release-task.details.5eadc84d')">
            <code-editor
              height="40vh"
              :content="temp.taskData && temp.taskData.beforeScript"
              :options="{
                mode: 'shell',
                readOnly: true
              }"
            ></code-editor>
          </a-tab-pane>
          <a-tab-pane key="after" :tab="$t('pages.file-manager.release-task.details.e643b0a1')">
            <code-editor
              height="40vh"
              :content="temp.taskData && temp.taskData.afterScript"
              :options="{
                mode: 'shell',
                readOnly: true
              }"
            ></code-editor>
          </a-tab-pane>
        </a-tabs>
      </a-form-item>
    </a-form>
  </div>
</template>

<script>
import { taskDetails, statusMap, taskLogInfoList } from '@/api/file-manager/release-task-log'
import LogView1 from '@/components/logView/index2'
import codeEditor from '@/components/codeEditor'
import { getSshListAll } from '@/api/ssh'
import { getNodeListAll } from '@/api/node'

export default {
  components: {
    LogView1,
    codeEditor
  },
  props: {
    taskId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      statusMap,
      logList: [],
      activeKey: '',
      logTimerMap: {},
      logMap: {},
      temp: {},
      sshList: [],
      nodeList: []
    }
  },
  beforeUnmount() {
    if (this.logTimerMap) {
      this.temp.taskList?.forEach((item) => {
        clearInterval(this.logTimerMap[item.id])
      })
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.fileManager.releaseTask.details.${key}`, ...args)
    },
    // 加载日志内容
    loadData() {
      this.activeKey = this.temp.id || ''
      taskDetails({
        id: this.taskId
      }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data
          if (this.temp.taskData?.taskType === 0) {
            this.loadSshList()
          } else if (this.temp.taskData?.taskType === 1) {
            this.loadNodeList()
          }

          if (!this.activeKey) {
            this.activeKey = this.temp.taskList && this.temp.taskList[0].id
          }
          this.tabCallback(this.activeKey)
        }
      })
    },
    // 加载 SSH 列表
    loadSshList() {
      return new Promise((resolve) => {
        this.sshList = []
        getSshListAll().then((res) => {
          if (res.code === 200) {
            this.sshList = res.data
            resolve()
          }
        })
      })
    },
    // 加载节点
    loadNodeList() {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data
        }
      })
    },
    initItemTimer(item) {
      if (!item) {
        return
      }
      // 加载构建日志
      this.logMap[item.id] = {
        line: 1,
        run: true
      }
      this.pullLog(item)
      this.logTimerMap[item.id] = setInterval(() => {
        this.pullLog(item)
      }, 2000)
    },
    pullLog(item) {
      const params = {
        id: item.id,
        line: this.logMap[item.id].line,
        tryCount: 0
      }

      taskLogInfoList(params).then((res) => {
        if (res.code === 200) {
          if (!res.data) {
            $notification.warning({
              message: res.msg
            })
            if (res.data.status !== 0) {
              // 还未开始的不计算次数
              this.logMap[item.id].tryCount = this.logMap[item.id].tryCount + 1
              if (this.logMap[item.id].tryCount > 10) {
                clearInterval(this.logTimerMap[item.id])
              }
            }
            return false
          }
          // 停止请求
          if (!res.data.run) {
            clearInterval(this.logTimerMap[item.id])
          }
          this.logMap[item.id].run = res.data.run
          // 更新日志

          this.$refs[`logView-${item.id}`][0]?.appendLine(res.data.dataLines)

          this.logMap[item.id].line = res.data.line

          this.logMap = { ...this.logMap }
        }
      })
    },
    tabCallback(key) {
      if (!key) {
        return
      }
      this.activeKey = key
      // console.log(this.$refs);
      if (this.logTimerMap[key]) {
        return
      }
      this.$nextTick(() => {
        const data = this.temp.taskList?.filter((item1) => {
          return item1.id === key
        })[0]
        this.initItemTimer(data)
      })
    }
  }
}
</script>
