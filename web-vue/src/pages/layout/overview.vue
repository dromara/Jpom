<template>
  <div>
    <a-page-header :backIcon="false">
      <template #title> 欢迎【{{ getUserInfo.name }}】您使用本系统</template>
      <!-- <template #subTitle>当前您在【】工作空间中 </template> -->
      <template v-slot:tags>
        <a-tag color="blue">
          <template v-if="getUserInfo.demoUser">演示账号</template>
          <template v-else-if="getUserInfo.superSystemUser">超级管理员</template>
          <template v-else-if="getUserInfo.systemUser">管理员</template>
          <template v-else>普通用户</template>
        </a-tag>
      </template>
      <template v-slot:extra>
        <a-button @click="init">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </template>
    </a-page-header>
    <a-divider dashed />
    <a-row>
      <a-col :span="4">
        <!-- <a-list bordered :data-source="myWorkspaceList">
            <a-list-item slot="renderItem" slot-scope="item"> <a-tag color="blue" v-if="getWorkspaceId() === item.id">当前</a-tag>{{ item.name }} </a-list-item>
            <div slot="header">
              <b>您的工作空间</b>
            </div>
          </a-list> -->
        <!-- <a-card size="small" title="您的工作空间"> -->
        <!-- <a slot="extra" href="#">more</a> -->

        <!-- <p v-for="(item, index) in myWorkspaceList" :key="index">{{ item.name }}</p> -->
        <!-- </a-card> -->
      </a-col>
    </a-row>
    <a-row :gutter="[16, 16]">
      <a-col :span="6">
        <a-card size="small">
          <template #title>
            数据统计
            <a-tooltip title="当前工作空间关联数据统计"><QuestionCircleOutlined /></a-tooltip>
          </template>
          <a-list :data-source="statNames">
            <template #renderItem="{ item }">
              <a-list-item> {{ item.name }}：{{ statData[item.field] || '-' }} </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <template #title>
            构建日志
            <a-tooltip title="当前工作空间您触发的构建记录"><QuestionCircleOutlined /></a-tooltip>
          </template>
          <a-timeline v-if="buildLog && buildLog.length">
            <a-timeline-item v-for="item in buildLog" :key="item.id" :color="statusColor[item.status]">
              <a-space direction="vertical" :size="1">
                <div>
                  {{ parseTime(item.startTime) }} ~
                  {{ parseTime(item.endTime) }}
                </div>
                <a-space>
                  <span :style="`color: ${statusColor[item.status]};`">#{{ item.buildNumberId }} </span>
                  <span v-if="item.buildName">{{ item.buildName }}</span>
                  <a-tooltip :title="item.statusMsg || statusMap[item.status] || '未知'">
                    <a-tag :color="statusColor[item.status]">{{ statusMap[item.status] || '未知' }}</a-tag>
                  </a-tooltip>
                </a-space>
              </a-space>
            </a-timeline-item>
          </a-timeline>
          <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" description="您还未构建" v-else />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <template #title> 登录日志 </template>
          <template v-slot:extra>
            <a href="#" @click="handleUserlog(2)">more</a>
          </template>
          <a-timeline v-if="loginLog && loginLog.length">
            <a-timeline-item :color="item.success ? 'green' : 'red'" v-for="(item, index) in loginLog" :key="index">
              <a-space direction="vertical" :size="1">
                <div>{{ parseTime(item.createTimeMillis) }}</div>
                <a-space>
                  <a-tag> {{ operateCodeMap[item.operateCode] || '未知' }}</a-tag>
                  <span> IP:{{ item.ip }}</span>
                </a-space>
              </a-space>
            </a-timeline-item>
          </a-timeline>
          <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" description="您还未登录过" v-else />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <template #title>
            操作日志
            <a-tooltip title="系统中您所有操作日志"><QuestionCircleOutlined /></a-tooltip>
          </template>
          <template v-slot:extra>
            <a href="#" @click="handleUserlog(1)">more</a>
          </template>
          <a-timeline v-if="operateLog && operateLog.length">
            <a-timeline-item
              :color="item.optStatus === 200 ? 'green' : 'red'"
              v-for="(item, index) in operateLog"
              :key="index"
            >
              <a-space direction="vertical" :size="1">
                <div>{{ parseTime(item.createTimeMillis) }}</div>
                <a-space>
                  <a-tag>{{ classFeatureMap[item.classFeature] }}</a-tag>
                  <a-tag>{{ methodFeatureMap[item.methodFeature] }}</a-tag>
                </a-space>
              </a-space>
            </a-timeline-item>
          </a-timeline>
          <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" description="您还未执行操作" v-else />
        </a-card>
      </a-col>
    </a-row>
    <!-- 查看操作日志 -->
    <a-modal
      v-if="viewLogVisible > 0"
      @cancel="
        () => {
          viewLogVisible = 0
        }
      "
      destroyOnClose
      :open="viewLogVisible > 0"
      :width="'90vw'"
      title="操作日志"
      :footer="null"
      :maskClosable="false"
    >
      <user-log v-if="viewLogVisible > 0" :openTab="viewLogVisible"></user-log>
    </a-modal>
  </div>
</template>

<script>
import { myWorkspace, statWorkspace, recentLogData } from '@/api/user/user'

import { parseTime } from '@/utils/const'
import { operateCodeMap } from '@/api/user/user-login-log'
import { getMonitorOperateTypeList } from '@/api/monitor'
import UserLog from './user-log.vue'
import { useUserStore } from '@/stores/user'
import { mapState } from 'pinia'
import { statusMap, statusColor, triggerBuildTypeMap } from '@/api/build-info'
import { Empty } from 'ant-design-vue'
export default {
  components: {
    UserLog
  },
  computed: {},
  data() {
    return {
      Empty,
      triggerBuildTypeMap,
      statusMap,
      statusColor,
      myWorkspaceList: [],
      loginLog: [],
      operateLog: [],
      buildLog: [],
      operateCodeMap,
      methodFeatureMap: {},
      classFeatureMap: {},
      viewLogVisible: 0,
      // "逻辑节点", "节点项目", "节点脚本", "项目分发", "SSH终端", "SSH命令", "本地脚本", "Docker节点", "动态文件", "静态文件"
      statNames: [
        { name: '逻辑节点', field: 'nodeCount' },
        { name: '节点项目', field: 'projectCount' },
        { name: '节点脚本', field: 'nodeScriptCount' },
        { name: '项目分发', field: 'outGivingCount' },
        { name: 'SSH终端', field: 'sshCount' },
        { name: 'SSH脚本', field: 'sshCommandCount' },
        { name: '本地脚本', field: 'scriptCount' },
        { name: 'Docker节点', field: 'dockerCount' },
        { name: 'Docker集群', field: 'dockerSwarmCount' },
        { name: '动态文件', field: 'fileCount' }
        // { name: "静态文件", field: "staticFileCount" },
      ],
      statData: {}
    }
  },
  computed: {
    ...mapState(useUserStore, ['getUserInfo'])
  },
  created() {
    this.init()
  },
  methods: {
    parseTime,
    init() {
      // 工作空间
      myWorkspace().then((res) => {
        if (res.code == 200 && res.data) {
          this.myWorkspaceList = res.data
        }
      })
      // 近期操作记录
      recentLogData().then((res) => {
        if (res.code == 200 && res.data) {
          this.operateLog = res.data.operateLog || []
          this.loginLog = res.data.loginLog || []
          this.buildLog = res.data.buildLog || []
        }
      })
      // 操作方法
      getMonitorOperateTypeList().then((res) => {
        this.methodFeature = res.data.methodFeature
        this.classFeature = res.data.classFeature
        res.data.methodFeature.forEach((item) => {
          this.methodFeatureMap[item.value] = item.title
        })
        res.data.classFeature.forEach((item) => {
          this.classFeatureMap[item.value] = item.title
        })
      })
      // 数据统计
      statWorkspace().then((res) => {
        if (res.code === 200 && res.data) {
          this.statData = res.data || {}
        }
      })
    },
    handleUserlog(val) {
      this.viewLogVisible = val
    }
  }
}
</script>

<style scoped>
:deep(.ant-divider-horizontal) {
  margin: 5px 0;
}
</style>
