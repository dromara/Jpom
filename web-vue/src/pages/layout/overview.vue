<template>
  <div>
    <a-page-header :back-icon="false">
      <template #title>
        {{ $t('pages.layout.overview.50f119d2') }}{{ getUserInfo.name
        }}{{ $t('pages.layout.overview.72c43195') }}</template
      >
      <template #subTitle>
        {{ $t('pages.layout.overview.306f3e5e', { count: (myWorkspaceList && myWorkspaceList.length) || 0 }) }}
      </template>
      <template #tags>
        <a-tag color="blue">
          <template v-if="getUserInfo.demoUser">{{ $t('pages.layout.overview.18437ccc') }}</template>
          <template v-else-if="getUserInfo.superSystemUser">{{ $t('pages.layout.overview.723b44a0') }}</template>
          <template v-else-if="getUserInfo.systemUser">{{ $t('pages.layout.overview.28e9178') }}</template>
          <template v-else>{{ $t('pages.layout.overview.501ac2ec') }}</template>
        </a-tag>
      </template>
      <template #extra>
        <a-tooltip :title="$t('pages.layout.overview.c5f3dbc5')">
          <a-button @click="init">
            <template #icon><ReloadOutlined /></template>
          </a-button>
        </a-tooltip>
        <!-- // 擅自修改或者删除版权信息有法律风险，请尊重开源协议，不要擅自修改版本信息，否则可能承担法律责任。 -->
        <a-tooltip
          v-if="getUserInfo && (getUserInfo.systemUser || getUserInfo.demoUser)"
          :title="$t('pages.layout.overview.8397a674')"
        >
          <a-button @click="showAbout">
            <template #icon><ExclamationCircleOutlined /></template>
          </a-button>
        </a-tooltip>
      </template>
    </a-page-header>
    <a-divider dashed />

    <a-row :gutter="[16, 16]">
      <a-col :span="6">
        <a-card size="small">
          <template #title>
            {{ $t('pages.layout.overview.e86dceb9') }}
            <a-tooltip :title="$t('pages.layout.overview.930f07d8')"><QuestionCircleOutlined /></a-tooltip>
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
            {{ $t('pages.layout.overview.fc20bf28') }}
            <a-tooltip :title="$t('pages.layout.overview.3318ecb')"><QuestionCircleOutlined /></a-tooltip>
          </template>
          <a-timeline v-if="buildLog && buildLog.length">
            <a-timeline-item v-for="item in buildLog" :key="item.id" :color="statusColor[item.status]">
              <a-space direction="vertical" :size="1">
                <div>
                  {{ parseTime(item.startTime) }} ~
                  {{ parseTime(item.endTime) }}
                </div>

                <a-row :gutter="16">
                  <a-col>
                    <span :style="`color: ${statusColor[item.status]};`" @click="handleBuildLog(item)">
                      #{{ item.buildNumberId }}
                    </span>
                  </a-col>
                  <a-col>
                    <span>{{ item.buildName || '-' }}</span>
                  </a-col>
                  <a-col>
                    <a-tooltip
                      :title="item.statusMsg || statusMap[item.status] || $t('pages.layout.overview.5f51a112')"
                    >
                      <a-tag :color="statusColor[item.status]" @click="handleBuildLog(item)">
                        {{ statusMap[item.status] || $t('pages.layout.overview.5f51a112') }}
                      </a-tag>
                    </a-tooltip>
                  </a-col>
                </a-row>
              </a-space>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" :description="$t('pages.layout.overview.6f21bc24')" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <template #title> {{ $t('pages.layout.overview.b75360fe') }} </template>
          <template #extra>
            <a href="#" @click="handleUserlog(2)">more</a>
          </template>
          <a-timeline v-if="loginLog && loginLog.length">
            <a-timeline-item v-for="(item, index) in loginLog" :key="index" :color="item.success ? 'green' : 'red'">
              <a-space direction="vertical" :size="1">
                <div>{{ parseTime(item.createTimeMillis) }}</div>
                <a-space>
                  <a-tag> {{ operateCodeMap[item.operateCode] || $t('pages.layout.overview.5f51a112') }}</a-tag>
                  <span> IP:{{ item.ip }}</span>
                </a-space>
              </a-space>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" :description="$t('pages.layout.overview.7328ac6d')" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <template #title>
            {{ $t('pages.layout.overview.86d58c89') }}
            <a-tooltip :title="$t('pages.layout.overview.a53992b3')"><QuestionCircleOutlined /></a-tooltip>
          </template>
          <template #extra>
            <a href="#" @click="handleUserlog(1)">more</a>
          </template>
          <a-timeline v-if="operateLog && operateLog.length">
            <a-timeline-item
              v-for="(item, index) in operateLog"
              :key="index"
              :color="item.optStatus === 200 ? 'green' : 'red'"
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
          <a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" :description="$t('pages.layout.overview.6b286e33')" />
        </a-card>
      </a-col>
    </a-row>
    <!-- 查看操作日志 -->
    <CustomModal
      v-if="viewLogVisible > 0"
      destroy-on-close
      :open="viewLogVisible > 0"
      :width="'90vw'"
      :title="$t('pages.layout.overview.86d58c89')"
      :footer="null"
      :mask-closable="false"
      @cancel="viewLogVisible = 0"
    >
      <div>
        <user-log v-if="viewLogVisible > 0" :open-tab="viewLogVisible"></user-log>
      </div>
    </CustomModal>
    <!-- 关于系统 -->
    <CustomModal
      v-if="aboutVisible > 0"
      destroy-on-close
      :open="aboutVisible > 0"
      :width="'90vw'"
      :title="$t('pages.layout.overview.5f76d6ae')"
      :footer="null"
      :mask-closable="false"
      @cancel="aboutVisible = 0"
    >
      <AboutPage></AboutPage>
    </CustomModal>
    <!-- 构建日志 -->
    <build-log v-if="buildLogVisible > 0" :temp="temp" :visible="buildLogVisible != 0" @close="buildLogVisible = 0" />
  </div>
</template>

<script>
import { myWorkspace, statWorkspace, recentLogData } from '@/api/user/user'
import BuildLog from '@/pages/build/log'
import { parseTime } from '@/utils/const'
import { operateCodeMap } from '@/api/user/user-login-log'
import { getMonitorOperateTypeList } from '@/api/monitor'
import UserLog from './user-log.vue'
import AboutPage from '@/pages/layout/about'
import { useUserStore } from '@/stores/user'
import { mapState } from 'pinia'
import { statusMap, statusColor, triggerBuildTypeMap } from '@/api/build-info'
import { Empty } from 'ant-design-vue'
export default {
  components: {
    UserLog,
    BuildLog,
    AboutPage
  },
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
        { name: this.$t('pages.layout.overview.ccdca9d8'), field: 'nodeCount' },
        { name: this.$t('pages.layout.overview.82289858'), field: 'projectCount' },
        { name: this.$t('pages.layout.overview.e0417750'), field: 'nodeScriptCount' },
        { name: this.$t('pages.layout.overview.9acec431'), field: 'outGivingCount' },
        { name: `SSH${this.$t('pages.layout.overview.b5a97ef7')}`, field: 'sshCount' },
        { name: `SSH${this.$t('pages.layout.overview.fbb32aa1')}`, field: 'sshCommandCount' },
        { name: this.$t('pages.layout.overview.a1f47198'), field: 'scriptCount' },
        { name: `Docker${this.$t('pages.layout.overview.602a0a5e')}`, field: 'dockerCount' },
        { name: `Docker${this.$t('pages.layout.overview.82ff11f5')}`, field: 'dockerSwarmCount' },
        { name: this.$t('pages.layout.overview.30d0bb03'), field: 'fileCount' }
        // { name: "静态文件", field: "staticFileCount" },
      ],
      statData: {},
      temp: {},
      buildLogVisible: 0,
      aboutVisible: 0
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
    },
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.buildDataId,
        buildId: record.buildNumberId
      }
      this.buildLogVisible = new Date() * Math.random()
    },
    // 关于系统
    showAbout() {
      this.aboutVisible = 1
    }
  }
}
</script>

<style scoped>
:deep(.ant-divider-horizontal) {
  margin: 5px 0;
}
</style>
