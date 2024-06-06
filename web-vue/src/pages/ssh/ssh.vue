<template>
  <div>
    <template v-if="useSuggestions">
      <a-result :title="$t('pages.ssh.ssh.37cc0d7c')" :sub-title="$t('pages.ssh.ssh.bea5f075')">
        <template #extra>
          <router-link to="/system/assets/ssh-list">
            <a-button key="console" type="primary">{{ $t('pages.ssh.ssh.b533d598') }}</a-button></router-link
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
      :empty-description="$t('pages.ssh.ssh.17ccc167')"
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
            :placeholder="$t('pages.ssh.ssh.d0781803')"
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
            :placeholder="$t('pages.ssh.ssh.e740d8cb')"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('pages.ssh.ssh.3b2221af')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('pages.ssh.ssh.53c2763c') }}</a-button>
          </a-tooltip>

          <a-button
            type="primary"
            :disabled="!tableSelections || !tableSelections.length"
            @click="syncToWorkspaceShow"
            >{{ $t('pages.ssh.ssh.ff284043') }}</a-button
          >
          <a-button type="primary" @click="toSshTabs">{{ $t('pages.ssh.ssh.e794eafb') }}</a-button>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>
              <ul>
                <li>{{ $t('pages.ssh.ssh.1415ce09') }}</li>
                <li>{{ $t('pages.ssh.ssh.c41a515a') }}</li>
                <li>{{ $t('pages.ssh.ssh.2ce9c46a') }}</li>
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
                $t('pages.ssh.ssh.5f51a112')
              }}
            </a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('osName')">
          <a-popover :title="$t('pages.ssh.ssh.8397a674')">
            <template #content>
              <p>{{ $t('pages.ssh.ssh.163de925') }}{{ record.machineSsh && record.machineSsh.osName }}</p>
              <p>{{ $t('pages.ssh.ssh.41180a8c') }}{{ record.machineSsh && record.machineSsh.osVersion }}</p>
              <p>
                CPU{{ $t('pages.ssh.ssh.5d15eed7') }}{{ record.machineSsh && record.machineSsh.osCpuIdentifierName }}
              </p>
              <p>{{ $t('pages.ssh.ssh.7fb10499') }}{{ record.machineSsh && record.machineSsh.hostName }}</p>
              <p>
                {{ $t('pages.ssh.ssh.abd23cf2')
                }}{{ formatDuration(record.machineSsh && record.machineSsh.osSystemUptime) }}
              </p>
            </template>
            {{ text || $t('pages.ssh.ssh.5f51a112') }}
          </a-popover>
        </template>
        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('osOccupyMemory')">
          <a-tooltip
            placement="topLeft"
            :title="`${$t('pages.ssh.ssh.cc3be96d')}${formatPercent(
              record.machineSsh && record.machineSsh.osOccupyMemory
            )},${$t('pages.ssh.ssh.8b8cc8a1')}${renderSize(record.machineSsh && record.machineSsh.osMoneyTotal)}`"
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
            :title="`CPU${$t('pages.ssh.ssh.6ba20f00')}${formatPercent2Number(
              record.machineSsh && record.machineSsh.osOccupyCpu
            )}%,CPU${$t('pages.ssh.ssh.f59d86c')}${record.machineSsh && record.machineSsh.osCpuCores}`"
          >
            <span
              >{{ (formatPercent2Number(record.machineSsh && record.machineSsh.osOccupyCpu) || '-') + '%' }} /
              {{ record.machineSsh && record.machineSsh.osCpuCores }}</span
            >
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('osMaxOccupyDisk')">
          <a-popover :title="$t('pages.ssh.ssh.a429e6a3')">
            <template #content>
              <p>
                {{ $t('pages.ssh.ssh.a430103d')
                }}{{ renderSize(record.machineSsh && record.machineSsh.osFileStoreTotal) }}
              </p>
              <p>
                {{ $t('pages.ssh.ssh.dd561a8b')
                }}{{ formatPercent(record.machineSsh && record.machineSsh.osMaxOccupyDisk) }}
              </p>
              <p>{{ $t('pages.ssh.ssh.7a0b637a') }}{{ record.machineSsh && record.machineSsh.osMaxOccupyDiskName }}</p>
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
            <a-tooltip placement="topLeft" :title="`${$t('pages.ssh.ssh.fa8d810f')}${record.linkNode.name}`">
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
                >{{ $t('pages.ssh.ssh.b5a97ef7') }}<DownOutlined
              /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="1">
                    <a-button size="small" type="primary" @click="handleTerminal(record, true)"
                      ><FullscreenOutlined />{{ $t('pages.ssh.ssh.230a04ee') }}</a-button
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
                        <FullscreenOutlined />{{ $t('pages.ssh.ssh.c5dbc58c') }}</a-button
                      >
                    </router-link>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <template v-if="record.fileDirs">
              <a-button size="small" type="primary" @click="handleFile(record)">{{
                $t('pages.ssh.ssh.b88f3e29')
              }}</a-button>
            </template>
            <template v-else>
              <a-tooltip placement="topLeft" :title="$t('pages.ssh.ssh.247ebe39')">
                <a-button size="small" type="primary" :disabled="true">{{ $t('pages.ssh.ssh.b88f3e29') }}</a-button>
              </a-tooltip>
            </template>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $t('pages.ssh.ssh.6e071067') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="handleEdit(record)">{{
                      $t('pages.ssh.ssh.64603c01')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                      $t('pages.ssh.ssh.dd20d11c')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="handleViewLog(record)">{{
                      $t('pages.ssh.ssh.194f04f1')
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
      :title="$t('pages.ssh.ssh.e943f850')"
      :mask-closable="false"
      :confirm-loading="confirmLoading"
      @ok="handleEditSshOk"
    >
      <a-form ref="editSshForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <template v-if="getUserInfo && getUserInfo.systemUser">
          <a-alert type="info" show-icon style="width: 100%; margin-bottom: 10px">
            <template #message>
              <ul>
                <li>{{ $t('pages.ssh.ssh.19e3f5b9') }}</li>
                <li>{{ $t('pages.ssh.ssh.72ca2f8d') }}</li>
                <li>{{ $t('pages.ssh.ssh.50663c25') }}</li>
              </ul>
            </template>
          </a-alert>
        </template>
        <a-form-item :label="$t('pages.ssh.ssh.493a5eda')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('pages.ssh.ssh.493a5eda')" />
        </a-form-item>
        <a-form-item :label="$t('pages.ssh.ssh.12d0e469')" name="group">
          <custom-select
            v-model:value="temp.group"
            :data="groupList"
            :input-placeholder="$t('pages.ssh.ssh.39d3d62f')"
            :select-placeholder="$t('pages.ssh.ssh.b474200b')"
          >
          </custom-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 文件管理 -->
    <a-drawer
      destroy-on-close
      :open="drawerVisible"
      :title="`${temp.name} ${$t('pages.ssh.ssh.502f94')}`"
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
      :title="$t('pages.ssh.ssh.9c06953e')"
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
      :title="$t('pages.ssh.ssh.d3b55aa0')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-alert :message="$t('pages.ssh.ssh.d5e02e8a')" type="warning" show-icon>
        <template #description>
          <ul>
            <li>{{ $t('pages.ssh.ssh.b74cd503') }}</li>
            <li>{{ $t('pages.ssh.ssh.bd652644') }}</li>
            <li>{{ $t('pages.ssh.ssh.9fefa492') }}</li>
          </ul>
        </template>
      </a-alert>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$t('pages.ssh.ssh.b86b400d')" name="workspaceId">
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
            :placeholder="$t('pages.ssh.ssh.3a321a02')"
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
          title: this.$t('pages.ssh.ssh.bb769c1d'),
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
          title: this.$t('pages.ssh.ssh.c28c6dc1'),
          dataIndex: ['machineSsh', 'user'],
          width: '100px',
          ellipsis: true
        },

        {
          title: this.$t('pages.ssh.ssh.1c01cf58'),
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
          title: this.$t('pages.ssh.ssh.d5f99ae'),
          dataIndex: ['machineSsh', 'osOccupyMemory'],
          width: 80,
          ellipsis: true
        },
        {
          title: this.$t('pages.ssh.ssh.d66598e7'),
          dataIndex: ['machineSsh', 'osMaxOccupyDisk'],
          width: 80,
          ellipsis: true
        },
        // { title: "编码格式", dataIndex: "charset", sorter: true, width: 120, ellipsis: true,  },
        {
          title: this.$t('pages.ssh.ssh.1d119f3f'),
          dataIndex: ['machineSsh', 'status'],
          ellipsis: true,
          align: 'center',
          width: '90px'
        },
        // { title: "编码格式", dataIndex: "machineSsh.charset", sorter: true, width: 120, ellipsis: true, },
        {
          title: this.$t('pages.ssh.ssh.7d267f5b'),
          dataIndex: 'nodeId',

          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('pages.ssh.ssh.f06e8846'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.ssh.ssh.61164914'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.ssh.ssh.3bb962bf'),
          dataIndex: 'operation',

          width: '200px',
          align: 'center',
          // ellipsis: true,
          fixed: 'right'
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$t('pages.ssh.ssh.e3ea8f07'), trigger: 'blur' }]
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
        title: this.$t('pages.ssh.ssh.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.ssh.ssh.e51646aa'),
        okText: this.$t('pages.ssh.ssh.e8e9db25'),
        cancelText: this.$t('pages.ssh.ssh.b12468e9'),
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
          message: this.$t('pages.ssh.ssh.3a321a02')
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
