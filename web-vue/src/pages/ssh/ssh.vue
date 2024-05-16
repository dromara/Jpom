<template>
  <div>
    <template v-if="useSuggestions">
      <a-result :title="$tl('p.noSshInWorkspace')" :sub-title="$tl('p.addSshInSystemManagement')">
        <template #extra>
          <router-link to="/system/assets/ssh-list">
            <a-button key="console" type="primary">{{ $tl('p.goToNow') }}</a-button></router-link
          >
        </template>
      </a-result>
    </template>
    <!-- 数据表格 -->
    <CustomTable
      v-else
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      table-name="ssh-list"
      :empty-description="$tl('p.noSshAvailable')"
      :active-page="activePage"
      :data-source="list"
      :columns="columns"
      size="middle"
      :pagination="pagination"
      bordered
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            class="search-input-item"
            :placeholder="$tl('p.sshGroupName')"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.group"
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
            allow-clear
            :placeholder="$tl('p.group')"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip :title="$tl('p.quickReturnFirstPage')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>

          <a-button
            type="primary"
            :disabled="!tableSelections || !tableSelections.length"
            @click="syncToWorkspaceShow"
            >{{ $tl('p.workspaceSync') }}</a-button
          >
          <a-button type="primary" @click="toSshTabs">{{ $tl('p.managementPanel') }}</a-button>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>
              <ul>
                <li>{{ $tl('p.associationNodeDataAsync') }}</li>
                <li>{{ $tl('p.javaEnvironmentDetection') }}</li>
                <li>{{ $tl('p.javaEnvironmentPluginRunning') }}</li>
              </ul>
            </div>
          </template>
          <QuestionCircleOutlined />
        </a-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip :title="text"> {{ text }}</a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'host'">
          <a-tooltip
            :title="`${record.machineSsh && record.machineSsh.host}:${record.machineSsh && record.machineSsh.port}`"
          >
            {{ record.machineSsh && record.machineSsh.host }}:{{ record.machineSsh && record.machineSsh.port }}
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('status')">
          <a-tooltip :title="record.machineSsh && record.machineSsh.statusMsg">
            <a-tag
              :color="
                statusMap[record.machineSsh && record.machineSsh.status] &&
                statusMap[record.machineSsh && record.machineSsh.status].color
              "
            >
              {{
                (statusMap[record.machineSsh && record.machineSsh.status] &&
                  statusMap[record.machineSsh && record.machineSsh.status].desc) ||
                $tl('c.unknown')
              }}
            </a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('osName')">
          <a-popover :title="$tl('p.systemInfo')">
            <template #content>
              <p>{{ $tl('p.systemName') }}{{ record.machineSsh && record.machineSsh.osName }}</p>
              <p>{{ $tl('p.systemVersion') }}{{ record.machineSsh && record.machineSsh.osVersion }}</p>
              <p>CPU{{ $tl('p.model') }}{{ record.machineSsh && record.machineSsh.osCpuIdentifierName }}</p>
              <p>{{ $tl('p.hostName') }}{{ record.machineSsh && record.machineSsh.hostName }}</p>
              <p>{{ $tl('p.bootTime') }}{{ formatDuration(record.machineSsh && record.machineSsh.osSystemUptime) }}</p>
            </template>
            {{ text || $tl('c.unknown') }}
          </a-popover>
        </template>
        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('osOccupyMemory')">
          <a-tooltip
            placement="topLeft"
            :title="`${$tl('p.memoryUsage')}${formatPercent(record.machineSsh && record.machineSsh.osOccupyMemory)},${$tl('p.totalMemory')}${renderSize(record.machineSsh && record.machineSsh.osMoneyTotal)}`"
          >
            <span
              >{{ formatPercent(record.machineSsh && record.machineSsh.osOccupyMemory) }}/{{
                renderSize(record.machineSsh && record.machineSsh.osMoneyTotal)
              }}</span
            >
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('osOccupyCpu')">
          <a-tooltip
            placement="topLeft"
            :title="`CPU${$tl('p.usageRate')}${formatPercent2Number(record.machineSsh && record.machineSsh.osOccupyCpu)}%,CPU${$tl('p.count')}${record.machineSsh && record.machineSsh.osCpuCores}`"
          >
            <span
              >{{ (formatPercent2Number(record.machineSsh && record.machineSsh.osOccupyCpu) || '-') + '%' }} /
              {{ record.machineSsh && record.machineSsh.osCpuCores }}</span
            >
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('osMaxOccupyDisk')">
          <a-popover :title="$tl('p.hardDiskInfo')">
            <template #content>
              <p>
                {{ $tl('p.totalHardDiskSpace')
                }}{{ renderSize(record.machineSsh && record.machineSsh.osFileStoreTotal) }}
              </p>
              <p>
                {{ $tl('p.maxHardDiskUsage')
                }}{{ formatPercent(record.machineSsh && record.machineSsh.osMaxOccupyDisk) }}
              </p>
              <p>{{ $tl('p.maxUsagePartition') }}{{ record.machineSsh && record.machineSsh.osMaxOccupyDiskName }}</p>
            </template>
            <span
              >{{ formatPercent(record.machineSsh && record.machineSsh.osMaxOccupyDisk) }}
              /
              {{ renderSize(record.machineSsh && record.machineSsh.osFileStoreTotal) }}</span
            >
          </a-popover>
        </template>

        <template v-else-if="column.dataIndex === 'nodeId'">
          <template v-if="record.linkNode">
            <a-tooltip placement="topLeft" :title="`${$tl('p.nodeName')}${record.linkNode.name}`">
              <a-button
                size="small"
                style="width: 90px; padding: 0 10px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis"
                type="link"
                @click="toNode(record.linkNode)"
              >
                {{ record.linkNode.name }}
              </a-button>
            </a-tooltip>
          </template>
          <template v-else>-</template>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-dropdown>
              <a-button size="small" type="primary" @click="handleTerminal(record, false)"
                >{{ $tl('p.terminal') }}<DownOutlined
              /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="1">
                    <a-button size="small" type="primary" @click="handleTerminal(record, true)"
                      ><FullscreenOutlined />{{ $tl('p.fullScreenTerminal') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item key="2">
                    <router-link
                      target="_blank"
                      :to="{
                        path: '/full-terminal',
                        query: { id: record.id, wid: getWorkspaceId() }
                      }"
                    >
                      <a-button size="small" type="primary">
                        <FullscreenOutlined />{{ $tl('p.newTabTerminal') }}</a-button
                      >
                    </router-link>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <template v-if="record.fileDirs">
              <a-button size="small" type="primary" @click="handleFile(record)">{{ $tl('c.fileType') }}</a-button>
            </template>
            <template v-else>
              <a-tooltip placement="topLeft" :title="$tl('p.authorizeFolder')">
                <a-button size="small" type="primary" :disabled="true">{{ $tl('c.fileType') }}</a-button>
              </a-tooltip>
            </template>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $tl('p.more') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('p.edit') }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                      $tl('p.delete')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="handleViewLog(record)">{{
                      $tl('p.terminalLog')
                    }}</a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <a-modal
      v-model:open="editSshVisible"
      destroy-on-close
      width="600px"
      :title="$tl('p.editSsh')"
      :mask-closable="false"
      :confirm-loading="confirmLoading"
      @ok="handleEditSshOk"
    >
      <a-form ref="editSshForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <template v-if="getUserInfo && getUserInfo.systemUser">
          <a-alert type="info" show-icon style="width: 100%; margin-bottom: 10px">
            <template #message>
              <ul>
                <li>{{ $tl('p.editSshInfo') }}</li>
                <li>{{ $tl('p.configureSsh') }}</li>
                <li>{{ $tl('p.sshAuthConfig') }}</li>
              </ul>
            </template>
          </a-alert>
        </template>
        <a-form-item :label="$tl('c.sshName')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$tl('c.sshName')" />
        </a-form-item>
        <a-form-item :label="$tl('p.groupName')" name="group">
          <custom-select
            v-model:value="temp.group"
            :data="groupList"
            :input-placeholder="$tl('p.groupAddition')"
            :select-placeholder="$tl('p.groupSelection')"
          >
          </custom-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 文件管理 -->
    <a-drawer
      destroy-on-close
      :open="drawerVisible"
      :title="`${temp.name} ${$tl('p.fileManagement')}`"
      placement="right"
      width="90vw"
      @close="
        () => {
          drawerVisible = false
        }
      "
    >
      <ssh-file v-if="drawerVisible" :ssh-id="temp.id" />
    </a-drawer>
    <!-- Terminal -->
    <a-modal
      v-model:open="terminalVisible"
      destroy-on-close
      :style="{
        maxWidth: '100vw',
        top: terminalFullscreen ? 0 : false,
        paddingBottom: 0
      }"
      :width="terminalFullscreen ? '100vw' : '80vw'"
      :body-style="{
        padding: '0 5px',
        paddingTop: '10px',
        marginRight: '10px',
        height: `${terminalFullscreen ? 'calc(100vh - 80px)' : '70vh'}`
      }"
      :title="temp.name"
      :footer="null"
      :mask-closable="false"
    >
      <!-- <div :style="`height: ${this.terminalFullscreen ? 'calc(100vh - 70px - 20px)' : 'calc(70vh - 20px)'}`"> -->
      <terminal1 v-if="terminalVisible" :ssh-id="temp.id" />
      <!-- </div> -->
    </a-modal>
    <!-- 操作日志 -->
    <a-modal
      v-model:open="viewOperationLog"
      destroy-on-close
      :title="$tl('p.operationLog')"
      width="80vw"
      :footer="null"
      :mask-closable="false"
    >
      <OperationLog v-if="viewOperationLog" :ssh-id="temp.id"></OperationLog>
    </a-modal>
    <!-- 同步到其他工作空间 -->
    <a-modal
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :title="$tl('p.syncToOtherWorkspaces')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-alert :message="$tl('p.warmTip')" type="warning" show-icon>
        <template #description>
          <ul>
            <li>{{ $tl('p.syncMechanism') }}</li>
            <li>{{ $tl('p.createNewSsh') }}</li>
            <li>{{ $tl('p.syncSshInfo') }}</li>
          </ul>
        </template>
      </a-alert>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$tl('p.workspaceSelection')" name="workspaceId">
          <a-select
            v-model:value="temp.workspaceId"
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
            :placeholder="$tl('c.selectWorkspace')"
          >
            <a-select-option v-for="item in workspaceList" :key="item.id" :disabled="getWorkspaceId() === item.id">{{
              item.name
            }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { deleteSsh, editSsh, getSshList, syncToWorkspace, getSshGroupAll } from '@/api/ssh'
import { statusMap } from '@/api/system/assets-ssh'
import SshFile from '@/pages/ssh/ssh-file'
import Terminal1 from '@/pages/ssh/terminal'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime,
  formatPercent2Number,
  renderSize,
  formatDuration,
  formatPercent
} from '@/utils/const'
import { getWorkSpaceListAll } from '@/api/workspace'

import { mapState } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import OperationLog from '@/pages/system/assets/ssh/operation-log'
import CustomSelect from '@/components/customSelect'

export default {
  components: {
    SshFile,
    Terminal1,
    OperationLog,
    CustomSelect
  },
  data() {
    return {
      loading: true,
      list: [],
      temp: {},
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editSshVisible: false,

      syncToWorkspaceVisible: false,
      tableSelections: [],
      workspaceList: [],
      tempNode: {},
      // fileList: [],
      statusMap,

      drawerVisible: false,
      terminalVisible: false,
      terminalFullscreen: false,
      viewOperationLog: false,

      columns: [
        {
          title: this.$tl('p.name'),
          dataIndex: 'name',
          sorter: true,
          width: 100,
          ellipsis: true,
          tooltip: true
        },

        {
          title: 'Host',
          dataIndex: ['machineSsh', 'host'],
          width: 100,
          ellipsis: true
        },
        // { title: "Port", dataIndex: "machineSsh.port", sorter: true, width: 80, ellipsis: true, },
        {
          title: this.$tl('p.username'),
          dataIndex: ['machineSsh', 'user'],
          width: '100px',
          ellipsis: true
        },

        {
          title: this.$tl('p.systemName_1'),
          dataIndex: ['machineSsh', 'osName'],
          width: 80,
          ellipsis: true
        },
        // { title: "系统版本", dataIndex: "machineSsh.osVersion", sorter: true, ellipsis: true,},
        {
          title: 'CPU',
          dataIndex: ['machineSsh', 'osOccupyCpu'],
          width: 80,
          ellipsis: true
        },
        {
          title: this.$tl('p.memory'),
          dataIndex: ['machineSsh', 'osOccupyMemory'],
          width: 80,
          ellipsis: true
        },
        {
          title: this.$tl('p.hardDisk'),
          dataIndex: ['machineSsh', 'osMaxOccupyDisk'],
          width: 80,
          ellipsis: true
        },
        // { title: "编码格式", dataIndex: "charset", sorter: true, width: 120, ellipsis: true,  },
        {
          title: this.$tl('p.connectionStatus'),
          dataIndex: ['machineSsh', 'status'],
          ellipsis: true,
          align: 'center',
          width: '90px'
        },
        // { title: "编码格式", dataIndex: "machineSsh.charset", sorter: true, width: 120, ellipsis: true, },
        {
          title: this.$tl('p.associatedNodes'),
          dataIndex: 'nodeId',

          width: '100px',
          ellipsis: true
        },
        {
          title: this.$tl('p.creationTime'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.modificationTime'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',

          width: '200px',
          align: 'center',
          // ellipsis: true,
          fixed: 'right'
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$tl('p.sshNameInput'), trigger: 'blur' }]
      },

      groupList: [],
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useUserStore, ['getUserInfo']),
    ...mapState(useAppStore, ['getWorkspaceId']),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections
      }
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    useSuggestions() {
      if (this.loading) {
        // 加载中不提示
        return false
      }
      if (!this.getUserInfo || !this.getUserInfo.systemUser) {
        // 没有登录或者不是超级管理员
        return false
      }
      if (this.listQuery.page !== 1 || this.listQuery.total > 0) {
        // 不是第一页 或者总记录数大于 0
        return false
      }
      // 判断是否存在搜索条件
      const nowKeys = Object.keys(this.listQuery)
      const defaultKeys = Object.keys(PAGE_DEFAULT_LIST_QUERY)
      const dictOrigin = nowKeys.filter((item) => !defaultKeys.includes(item))
      return dictOrigin.length === 0
    }
  },
  created() {
    this.loadData()
    this.loadGroupList()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.ssh.ssh.${key}`, ...args)
    },
    formatPercent2Number,
    renderSize,
    formatPercent,
    formatDuration,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getSshList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
          //
        }
        this.loading = false
      })
    },
    // 获取所有的分组
    loadGroupList() {
      getSshGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, { id: record.id, group: record.group, name: record.name })

      this.editSshVisible = true
      // @author jzy 08-04
      this.$refs['editSshForm'] && this.$refs['editSshForm'].resetFields()
    },
    // 提交 SSH 数据
    handleEditSshOk() {
      // 检验表单
      this.$refs['editSshForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        editSsh(this.temp)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              //this.$refs['editSshForm'].resetFields();
              // this.fileList = [];
              this.editSshVisible = false
              this.loadData()
              this.loadGroupList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 进入终端
    handleTerminal(record, terminalFullscreen) {
      this.temp = Object.assign({}, record)
      this.terminalVisible = true
      this.terminalFullscreen = terminalFullscreen
    },
    // 操作日志
    handleViewLog(record) {
      this.temp = Object.assign({}, record)
      this.viewOperationLog = true
    },

    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, { id: record.id, group: record.group, name: record.name })

      this.drawerVisible = true
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$tl('p.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmDeletion'),
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
        onOk: () => {
          return deleteSsh(record.id).then((res) => {
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
    // 前往节点
    toNode(node) {
      const newpage = this.$router.resolve({
        path: '/node/list',
        query: {
          ...this.$route.query,
          nodeId: node.id,
          pId: 'manage',
          id: 'manageList',
          wid: node.workspaceId,
          searchNodeName: node.name
        }
      })
      window.open(newpage.href, '_blank')

      // this.$router.push({
      //   path: "/node/list",
      //   query: {
      //     ...this.$route.query,
      //     tipNodeId: nodeId,
      //   },
      // });
    },
    toSshTabs() {
      const newpage = this.$router.resolve({
        path: '/ssh-tabs',
        query: {
          wid: this.getWorkspaceId()
        }
      })
      window.open(newpage.href, '_blank')
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },

    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data
        }
      })
    },
    // 同步到其他工作情况
    syncToWorkspaceShow() {
      this.syncToWorkspaceVisible = true
      this.loadWorkSpaceListAll()
      this.temp = {
        workspaceId: undefined
      }
    },
    //
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: this.$tl('c.selectWorkspace')
        })
        return false
      }
      // 同步
      this.confirmLoading = true
      syncToWorkspace({
        ids: this.tableSelections.join(','),
        toWorkspaceId: this.temp.workspaceId
      })
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.tableSelections = []
            this.syncToWorkspaceVisible = false
            return false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    }
  }
}
</script>
