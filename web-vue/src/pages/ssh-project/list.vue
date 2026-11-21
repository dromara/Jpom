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
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      table-name="ssh-project-list"
      :active-page="activePage"
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery.name"
            :placeholder="$t('i18n_47d26a4994')"
            class="search-input-item"
            allow-clear
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery.group"
            :placeholder="$t('i18n_0a52738bb5')"
            class="search-input-item"
            allow-clear
            @press-enter="loadData"
          />
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('i18n_66ab5e9f24') }}</a-button>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'group'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'sshId'">
          <a-tooltip placement="topLeft" :title="sshTooltip(record)">
            <span>{{ record.sshName || text || '-' }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="statusTooltip(record)">
            <a-tag :color="isRunning(record) ? 'green' : 'default'">
              {{ isRunning(record) ? $t('i18n_d679aea3aa') : $t('i18n_607ce7db25') }}
            </a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'path'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleConsole(record)">{{ $t('i18n_b5c3770699') }}</a-button>
            <a-tooltip :title="isRunning(record) ? $t('i18n_55ec2b5767') : ''">
              <span>
                <a-button
                  size="small"
                  :disabled="isRunning(record)"
                  :loading="optLoading[record.id] === 'start'"
                  @click="handleOperate(record, 'start')"
                  >{{ $t('i18n_8e54ddfe24') }}</a-button
                >
              </span>
            </a-tooltip>
            <a-tooltip :title="!isRunning(record) ? $t('i18n_cccefb2c86') : ''">
              <span>
                <a-button
                  size="small"
                  :disabled="!isRunning(record)"
                  :loading="optLoading[record.id] === 'stop'"
                  @click="handleOperate(record, 'stop')"
                  >{{ $t('i18n_095e938e2a') }}</a-button
                >
              </span>
            </a-tooltip>
            <a-button
              size="small"
              :loading="optLoading[record.id] === 'restart'"
              @click="handleOperate(record, 'restart')"
              >{{ $t('i18n_01b4e06f39') }}</a-button
            >
            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $t('i18n_0ec9eaf9c3') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button type="primary" size="small" @click="handleFile(record)">{{
                      $t('i18n_2a0c4740f1')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button type="primary" size="small" @click="handleVersion(record)">{{
                      $t('i18n_fe2df04a16')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button type="primary" size="small" @click="handleTrigger(record)">{{
                      $t('i18n_4696724ed3')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button type="primary" size="small" @click="handleEdit(record)">{{
                      $t('i18n_95b351c862')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
                      $t('i18n_2f4aaddde3')
                    }}</a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 文件管理 -->
    <CustomDrawer
      v-if="fileVisible"
      destroy-on-close
      :open="fileVisible"
      :title="`${temp.name} ${$t('i18n_8780e6b3d1')}`"
      placement="right"
      width="90vw"
      @close="
        () => {
          fileVisible = false
        }
      "
    >
      <ssh-file v-if="fileVisible" :data-id="temp.id" api-base="/node/ssh-project/file/" auto-select-first />
    </CustomDrawer>
    <!-- 版本快照（构建发布自动登记 + 人工快照，支持快速回滚） -->
    <CustomDrawer
      v-if="versionVisible"
      destroy-on-close
      :open="versionVisible"
      :title="`${temp.name} ${$t('i18n_fe2df04a16')}`"
      placement="right"
      width="80vw"
      @close="
        () => {
          versionVisible = false
        }
      "
    >
      <ssh-project-version v-if="versionVisible" :project-id="temp.id" />
    </CustomDrawer>
    <!-- 控制台（与项目控制台一致：抽屉内嵌，不再新标签打开） -->
    <CustomDrawer
      v-if="consoleVisible"
      destroy-on-close
      :open="consoleVisible"
      :title="`${temp.name} ${$t('i18n_b5c3770699')}`"
      placement="right"
      width="85vw"
      @close="
        () => {
          consoleVisible = false
        }
      "
    >
      <ssh-project-console v-if="consoleVisible" :project-id="temp.id" />
    </CustomDrawer>
    <!-- 触发器（与项目/脚本触发器一致：地址 + 批量地址 + 重置 token） -->
    <CustomModal
      v-if="triggerVisible"
      v-model:open="triggerVisible"
      destroy-on-close
      :title="$t('i18n_4696724ed3')"
      width="50%"
      :footer="null"
      :mask-closable="false"
    >
      <a-tabs default-active-key="1">
        <template #rightExtra>
          <a-tooltip :title="$t('i18n_01ad26f4a9')">
            <a-button type="primary" size="small" @click="resetTrigger">{{ $t('i18n_4b9c3271dc') }}</a-button>
          </a-tooltip>
        </template>
        <a-tab-pane key="1" :tab="$t('i18n_1a6aa24e76')">
          <a-space direction="vertical" style="width: 100%">
            <a-alert :message="$t('i18n_947d983961')" type="warning">
              <template #description>
                <ul>
                  <li>{{ $t('i18n_9a00e13160') }}</li>
                  <li>{{ $t('i18n_632a907224') }}</li>
                  <li>{{ $t('i18n_cb9b3ec760') }}</li>
                </ul>
              </template>
            </a-alert>
            <a-alert
              v-for="item in triggerUses"
              :key="item.value"
              type="info"
              :message="`${item.desc}${$t('i18n_b9a4098131')}(${$t('i18n_00a070c696')})`"
            >
              <template #description>
                <a-typography-paragraph :copyable="{ tooltip: false, text: `${temp.triggerUrl}?action=${item.value}` }">
                  <a-tag>GET</a-tag>
                  <span>{{ `${temp.triggerUrl}?action=${item.value}` }} </span>
                </a-typography-paragraph>
              </template>
            </a-alert>
            <a-alert type="info" :message="`${$t('i18n_8d202b890c')}(${$t('i18n_00a070c696')})`">
              <template #description>
                <a-typography-paragraph :copyable="{ tooltip: false, text: temp.batchTriggerUrl }">
                  <a-tag>POST</a-tag> <span>{{ temp.batchTriggerUrl }} </span>
                </a-typography-paragraph>
              </template>
            </a-alert>
          </a-space>
        </a-tab-pane>
      </a-tabs>
    </CustomModal>
    <!-- 编辑区 -->
    <edit-ssh-project v-if="editVisible" :data="temp" @cancel="handleEditCancel"></edit-ssh-project>
  </div>
</template>
<script>
import {
  deleteSshProject,
  getSshProjectList,
  getSshProjectStatus,
  getSshProjectTriggerUrl,
  operateSshProject
} from '@/api/ssh-project'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import SshFile from '@/pages/ssh/ssh-file'
import editSshProject from './edit.vue'
import SshProjectConsole from './console.vue'
import SshProjectVersion from './version.vue'
export default {
  components: {
    editSshProject,
    SshFile,
    SshProjectConsole,
    SshProjectVersion
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY, { name: '', group: '' }),
      list: [],
      /**
       * 实时状态：key 为项目 id，value 为 status 接口返回的状态对象
       */
      statusMap: {},
      /**
       * 行内操作 loading：key 为项目 id，value 为正在执行的操作
       */
      optLoading: {},
      temp: {},
      editVisible: false,
      fileVisible: false,
      consoleVisible: false,
      versionVisible: false,
      triggerVisible: false,
      /**
       * 触发器可执行动作（与生命周期脚本一致）
       */
      triggerUses: [
        { desc: this.$t('i18n_0e16902c1e'), value: 'status' },
        { desc: this.$t('i18n_6b29a6e523'), value: 'start' },
        { desc: this.$t('i18n_d75c02d050'), value: 'stop' },
        { desc: this.$t('i18n_f3e93355ee'), value: 'restart' },
        { desc: this.$t('i18n_aaeb54633e'), value: 'reload' }
      ],
      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          ellipsis: true
        },
        {
          title: this.$t('i18n_829abe5a8d'),
          dataIndex: 'group',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_1030348573'),
          dataIndex: 'sshId',
          ellipsis: true,
          width: 220
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: 'status',
          align: 'center',
          width: 110
        },
        {
          title: this.$t('i18n_4f35e80da6'),
          dataIndex: 'path',
          ellipsis: true
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTime',
          customRender: ({ text }) => {
            if (!text) {
              return ''
            }
            return parseTime(text)
          },
          width: 180
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          fixed: 'right',
          width: '1%'
        }
      ]
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  created() {
    // 批量状态请求可能较慢，用非响应式属性做并发保护（避免自动刷新叠加请求）
    this.statusPending = null
    this.loadData()
  },
  methods: {
    // 加载数据（列表 + 实时状态）
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getSshProjectList({
        page: this.listQuery.page,
        limit: this.listQuery.limit,
        name: this.listQuery.name,
        group: this.listQuery.group
      })
        .then((res) => {
          if (res.code === 200 && res.data) {
            this.list = res.data.result || []
            this.listQuery.total = res.data.total
            return this.loadStatus()
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    // 批量刷新当前页的实时状态
    loadStatus() {
      if (this.statusPending) {
        return this.statusPending
      }
      const ids = this.list.map((item) => item.id).filter((id) => !!id)
      if (!ids.length) {
        this.statusMap = {}
        return Promise.resolve()
      }
      this.statusPending = getSshProjectStatus({ ids })
        .then((res) => {
          if (res.code === 200 && res.data) {
            this.statusMap = res.data
          }
        })
        .catch(() => {
          $notification.warn({ message: this.$t('i18n_6a8bc730ef') })
        })
        .finally(() => {
          this.statusPending = null
        })
      return this.statusPending
    },
    // 状态来源：优先实时接口，其次列表里的 lastStatus
    statusInfo(record) {
      const item = this.statusMap[record.id]
      if (item) {
        return item
      }
      return {
        status: record.lastStatus,
        pid: record.lastPid,
        port: record.lastPort,
        message: record.lastStatusMsg
      }
    },
    // 机器 SSH 展示（名称优先，tooltip 补充 id 便于排查）
    sshTooltip(record) {
      const name = record.sshName || record.sshId || record.machineSshId || '-'
      const ids = [
        record.sshId ? 'sshId=' + record.sshId : '',
        record.machineSshId ? 'machineSshId=' + record.machineSshId : ''
      ]
        .filter(Boolean)
        .join(' / ')
      return ids ? name + ' (' + ids + ')' : name
    },
    // 状态占位：running 表示运行中，其他均为已停止
    isRunning(record) {
      const info = this.statusInfo(record)
      return typeof info.status === 'string' && info.status.toLowerCase() === 'running'
    },
    statusTooltip(record) {
      const info = this.statusInfo(record)
      const messages = []
      if (info.pid) {
        messages.push(`PID: ${info.pid}`)
      }
      if (info.port && info.port !== '-') {
        messages.push(`${this.$t('i18n_c76cfefe72')}: ${info.port}`)
      }
      if (info.message) {
        messages.push(info.message)
      }
      return messages.join(' | ')
    },
    // 启动/停止/重启
    handleOperate(record, opt) {
      if (opt === 'stop') {
        $confirm({
          title: this.$t('i18n_c4535759ee'),
          zIndex: 1009,
          content: this.$t('i18n_010865ca50'),
          okText: this.$t('i18n_e83a256e4f'),
          cancelText: this.$t('i18n_625fb26b4b'),
          onOk: () => this.doOperate(record, opt)
        })
        return
      }
      if (opt === 'restart') {
        $confirm({
          title: this.$t('i18n_c4535759ee'),
          zIndex: 1009,
          content: this.$t('i18n_989f1f2b61'),
          okText: this.$t('i18n_e83a256e4f'),
          cancelText: this.$t('i18n_625fb26b4b'),
          onOk: () => this.doOperate(record, opt)
        })
        return
      }
      this.doOperate(record, opt)
    },
    doOperate(record, opt) {
      this.optLoading[record.id] = opt
      return operateSshProject({ id: record.id, opt })
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
          }
          // 失败由 request 封装统一提示，这里只刷新当前行状态
          return this.refreshRowStatus(record.id)
        })
        .finally(() => {
          delete this.optLoading[record.id]
        })
    },
    // 刷新单行状态（操作完成后只刷该行，避免整页重新执行 status 脚本）
    refreshRowStatus(id) {
      return getSshProjectStatus({ id })
        .then((res) => {
          if (res.code === 200 && res.data) {
            this.statusMap = { ...this.statusMap, ...res.data }
          }
        })
        .catch(() => {
          $notification.warn({ message: this.$t('i18n_6a8bc730ef') })
        })
    },
    // 新增
    handleAdd() {
      this.temp = {}
      this.editVisible = true
    },
    // 文件管理（复用 SSH 资产的文件管理组件，接口前缀换成 SSH 项目）
    handleFile(record) {
      this.temp = Object.assign({}, record)
      this.fileVisible = true
    },
    // 版本快照（构建发布自动登记、人工创建，支持快速回滚）
    handleVersion(record) {
      this.temp = Object.assign({}, record)
      this.versionVisible = true
    },
    // 控制台（抽屉内嵌，交互与普通项目控制台保持一致）
    handleConsole(record) {
      this.temp = Object.assign({}, record)
      this.consoleVisible = true
    },
    // 触发器（首次打开自动生成 token，重置后旧地址失效）
    handleTrigger(record) {
      this.temp = Object.assign({}, record)
      getSshProjectTriggerUrl({ id: record.id }).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res)
          this.triggerVisible = true
        }
      })
    },
    // 重置触发器 token
    resetTrigger() {
      getSshProjectTriggerUrl({ id: this.temp.id, rest: 'rest' }).then((res) => {
        if (res.code === 200) {
          $notification.success({ message: res.msg })
          this.fillTriggerResult(res)
        }
      })
    },
    fillTriggerResult(res) {
      this.temp.triggerUrl = `${location.protocol}//${location.host}${res.data.triggerUrl}`
      this.temp.batchTriggerUrl = `${location.protocol}//${location.host}${res.data.batchTriggerUrl}`
      this.temp = { ...this.temp }
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      this.editVisible = true
    },
    handleEditCancel() {
      this.editVisible = false
      this.loadData()
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_8026b4ff6c'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteSshProject(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }
  }
}
</script>
