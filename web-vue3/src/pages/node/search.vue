<template>
  <div class="full-content">
    <template v-if="useSuggestions">
      <a-result title="当前工作空间还没有项目" sub-title="可以创建节点分发或者到节点管理创建项目"> </a-result>
    </template>
    <a-table
      v-else
      :data-source="projList"
      :columns="columns"
      size="middle"
      bordered
      :pagination="pagination"
      @change="changePage"
      :row-selection="rowSelection"
      :rowKey="(record, index) => index"
    >
      <template #title>
        <a-space>
          <a-select v-model="listQuery.nodeId" allowClear placeholder="请选择节点" class="search-input-item">
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-select
            v-model="listQuery.group"
            allowClear
            placeholder="请选择分组"
            class="search-input-item"
            @change="getNodeProjectData"
          >
            <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
          </a-select>
          <a-input
            v-model="listQuery['%name%']"
            @pressEnter="getNodeProjectData"
            placeholder="搜索项目"
            class="search-input-item"
          />

          <a-select v-model="listQuery.runMode" allowClear placeholder="项目类型" class="search-input-item">
            <a-select-option v-for="item in runModeList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="getNodeProjectData">搜索</a-button>
          </a-tooltip>

          <a-dropdown v-if="selectedRowKeys && this.selectedRowKeys.length">
            <a-button type="primary"> 批量操作 <down-outlined /> </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item>
                  <a-button type="primary" @click="batchStart">批量启动</a-button>
                </a-menu-item>
                <a-menu-item>
                  <a-button type="primary" @click="batchRestart">批量重启</a-button>
                </a-menu-item>
                <a-menu-item>
                  <a-button type="danger" @click="batchStop">批量关闭</a-button>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <a-button v-else type="primary" :disabled="true"> 批量操作 <down-outlined /> </a-button>

          <a-tooltip
            placement="topLeft"
            title="清除服务端缓存节点所有的项目信息, 需要重新同步：可以通过节点列表逐个同步"
          >
            <a-button type="danger" @click="delAll()" icon="delete"> 删除缓存 </a-button>
          </a-tooltip>

          <a-tooltip>
            <template #title>
              <div>
                <ul>
                  <li>项目是存储在节点中的、创建需要到节点管理里面去操作</li>
                  <li>状态数据是异步获取有一定时间延迟</li>
                  <li>在单页列表里面 file 类型项目将自动排序到最后</li>
                </ul>
              </div>
            </template>
            <question-circle-filled />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip #name slot-scope="text, record" placement="topLeft" :title="text">
        <a-icon v-if="record.outGivingProject" type="apartment" />
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip #nodeId slot-scope="text" placement="topLeft" :title="text">
        <a-button type="link" style="padding: 0" size="small" @click="toNode(text)">
          <span>{{ nodeMap[text] }}</span>
          <a-icon type="fullscreen" />
        </a-button>
      </a-tooltip>
      <a-tooltip #path slot-scope="text, item" placement="topLeft" :title="item.whitelistDirectory + item.lib">
        <span>{{ item.whitelistDirectory + item.lib }}</span>
      </a-tooltip>

      <a-tooltip #tooltip slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <template #status slot-scope="text, record">
        <template v-if="record.error">
          <a-tooltip :title="record.error">
            <a-icon type="warning" />
          </a-tooltip>
        </template>
        <template v-else>
          <a-tooltip v-if="noFileModes.includes(record.runMode)" title="状态操作请到控制台中控制">
            <a-switch :checked="text" disabled checked-children="开" un-checked-children="关" />
          </a-tooltip>
          <span v-else>-</span>
        </template>
      </template>

      <a-tooltip
        #port
        slot-scope="text, record"
        placement="topLeft"
        :title="`进程号：${(record.pids || [record.pid || '-']).join(',')} / 端口号：${record.port}`"
      >
        <span>{{ record.port || '-' }}/{{ (record.pids || [record.pid || '-']).join(',') }}</span>
      </a-tooltip>
      <template #operation slot-scope="text, record, index">
        <a-space>
          <a-button size="small" type="primary" @click="handleFile(record)">文件</a-button>
          <template v-if="noFileModes.includes(record.runMode)">
            <a-button size="small" type="primary" @click="handleConsole(record)">控制台</a-button>
          </template>
          <template v-else>
            <a-tooltip title="文件类型没有控制台功能">
              <a-button size="small" type="primary" :disabled="true">控制台</a-button></a-tooltip
            >
          </template>

          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
              更多
              <down-outlined />
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item>
                  <template v-if="noFileModes.includes(record.runMode)">
                    <a-button size="small" type="primary" @click="handleTrigger(record)">触发器</a-button>
                  </template>
                  <template v-else>
                    <a-tooltip title="文件类型没有触发器功能">
                      <a-button size="small" type="primary" :disabled="true">触发器</a-button></a-tooltip
                    >
                  </template>
                </a-menu-item>
                <a-menu-item>
                  <a-button
                    size="small"
                    type="primary"
                    :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                    @click="sortItemHander(record, index, 'top')"
                    >置顶</a-button
                  >
                </a-menu-item>
                <a-menu-item>
                  <a-button
                    size="small"
                    type="primary"
                    :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                    @click="sortItemHander(record, index, 'up')"
                    >上移</a-button
                  >
                </a-menu-item>
                <a-menu-item>
                  <a-button
                    size="small"
                    type="primary"
                    :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total"
                    @click="sortItemHander(record, index, 'down')"
                  >
                    下移
                  </a-button>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </a-table>
    <!-- 项目文件组件 -->
    <a-drawer
      destroyOnClose
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :visible="drawerFileVisible"
      @close="onFileClose"
    >
      <file
        v-if="drawerFileVisible"
        :nodeId="temp.nodeId"
        :projectId="temp.projectId"
        @goConsole="goConsole"
        @goReadFile="goReadFile"
      />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer
      destroyOnClose
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :visible="drawerConsoleVisible"
      @close="onConsoleClose"
    >
      <console
        v-if="drawerConsoleVisible"
        :id="temp.id"
        :nodeId="temp.nodeId"
        :projectId="temp.projectId"
        @goFile="goFile"
      />
    </a-drawer>
    <!-- 项目跟踪文件组件 -->
    <a-drawer
      destroyOnClose
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :visible="drawerReadFileVisible"
      @close="onReadFileClose"
    >
      <file-read
        v-if="drawerReadFileVisible"
        :nodeId="temp.nodeId"
        :readFilePath="temp.readFilePath"
        :id="temp.id"
        :projectId="temp.projectId"
        @goFile="goFile"
      />
    </a-drawer>
    <!-- 批量操作状态 -->
    <a-modal destroyOnClose v-model:visible="batchVisible" :title="batchTitle" :footer="null" @cancel="batchClose">
      <a-list bordered :data-source="selectedRowKeys">
        <a-list-item #renderItem slot-scope="item">
          <a-list-item-meta :description="item.email">
            <template #title>
              <a> {{ projList[item].name }}</a>
            </template>
          </a-list-item-meta>
          <div>
            <a-tooltip :title="`${projList[item].cause || '未开始'}`"
              >{{ projList[item].cause || '未开始' }}
            </a-tooltip>
          </div>
        </a-list-item>
      </a-list>
    </a-modal>
    <!-- 触发器 -->
    <a-modal
      destroyOnClose
      v-model:visible="triggerVisible"
      title="触发器"
      width="50%"
      :footer="null"
      :maskClosable="false"
    >
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #tabBarExtraContent>
            <a-tooltip title="重置触发器 token 信息,重置后之前的触发器 token 将失效">
              <a-button type="primary" size="small" @click="resetTrigger">重置</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" tab="执行">
            <a-space style="display: block" direction="vertical" align="baseline">
              <a-alert message="温馨提示" type="warning">
                <template #description>
                  <ul>
                    <li>单个触发器地址中：第一个随机字符串为项目ID(服务端)，第二个随机字符串为 token</li>
                    <li>
                      重置为重新生成触发地址,重置成功后之前的触发器地址将失效,触发器绑定到生成触发器到操作人上,如果将对应的账号删除触发器将失效
                    </li>
                    <li>批量触发参数 BODY json： [ { "id":"1", "token":"a","action":"status" } ]</li>
                  </ul>
                </template>
              </a-alert>

              <a-alert
                :key="item.value"
                v-for="item in triggerUses"
                v-clipboard:copy="`${temp.triggerUrl}?action=${item.value}`"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' })
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' })
                  }
                "
                type="info"
                :message="`${item.desc}触发器地址(点击可以复制)`"
              >
                <template #description>
                  <a-tag>GET</a-tag> <span>{{ `${temp.triggerUrl}?action=${item.value}` }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>

              <a-alert
                v-clipboard:copy="temp.batchTriggerUrl"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' })
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' })
                  }
                "
                type="info"
                :message="`批量触发器地址(点击可以复制)`"
              >
                <template #description>
                  <a-tag>POST</a-tag> <span>{{ temp.batchTriggerUrl }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
        </a-tabs>
      </a-form>
    </a-modal>
  </div>
</template>
<script>
import { delAllProjectCache, getNodeListAll, getProjectList, sortItemProject } from '@/api/node'
import {
  getRuningProjectInfo,
  noFileModes,
  restartProject,
  runModeList,
  startProject,
  stopProject,
  getProjectTriggerUrl,
  getProjectGroupAll
} from '@/api/node-project'
import File from '@/pages/node/node-layout/project/project-file'
import Console from '../node/node-layout/project/project-console'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  concurrentExecution,
  itemGroupBy,
  parseTime
} from '@/utils/const'
import FileRead from '@/pages/node/node-layout/project/project-file-read'
// import Vue from 'vue'
import { mapState } from 'pinia'

export default {
  components: {
    File,
    Console,
    FileRead
  },
  data() {
    return {
      projList: [],
      groupList: [],
      runModeList: runModeList,
      selectedRowKeys: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      noFileModes: noFileModes,
      nodeMap: {},
      drawerTitle: '',
      loading: true,
      temp: {},
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      drawerReadFileVisible: false,
      batchVisible: false,
      batchTitle: '',
      columns: [
        { title: '项目名称', dataIndex: 'name', width: 200, ellipsis: true, scopedSlots: { customRender: 'name' } },
        {
          title: '项目分组',
          dataIndex: 'group',
          sorter: true,
          width: '100px',
          ellipsis: true,
          scopedSlots: { customRender: 'group' }
        },
        { title: '节点名称', dataIndex: 'nodeId', width: 90, ellipsis: true, scopedSlots: { customRender: 'nodeId' } },
        {
          title: '项目路径',
          dataIndex: 'path',
          ellipsis: true,
          width: 120,
          scopedSlots: { customRender: 'path' }
        },
        {
          title: '运行状态',
          dataIndex: 'status',
          align: 'center',
          width: 100,
          ellipsis: true,
          scopedSlots: { customRender: 'status' }
        },
        { title: '端口/PID', dataIndex: 'port', width: 100, ellipsis: true, scopedSlots: { customRender: 'port' } },

        {
          title: '运行方式',
          dataIndex: 'runMode',
          width: 90,
          ellipsis: true,
          scopedSlots: { customRender: 'runMode' }
        },
        {
          title: 'webhook',
          dataIndex: 'token',
          width: 120,
          ellipsis: true,
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: '170px'
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: (text) => parseTime(text),
          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          align: 'center',
          fixed: 'right',
          scopedSlots: { customRender: 'operation' },
          width: '190px'
        }
      ],
      triggerVisible: false,
      triggerUses: [
        { desc: '查看状态', value: 'status' },
        { desc: '启动项目', value: 'start' },
        { desc: '停止项目', value: 'stop' },
        { desc: '重启项目', value: 'restart' }
      ]
    }
  },
  computed: {
    ...mapGetters(['getUserInfo']),
    filePath() {
      return (this.temp.whitelistDirectory || '') + (this.temp.lib || '')
    },
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        onChange: this.onSelectChange,
        columnWidth: '40px',
        getCheckboxProps: this.getCheckboxProps
      }
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
  mounted() {
    this.getNodeProjectData()
    getNodeListAll().then((res) => {
      if (res.code === 200) {
        res.data.forEach((item) => {
          this.nodeMap = { ...this.nodeMap, [item.id]: item.name }
        })
      }
    })
  },
  methods: {
    getNodeProjectData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getProjectList(this.listQuery).then((res) => {
        if (res.code === 200) {
          let resultList = res.data.result

          let tempList = resultList.filter((item) => item.runMode !== 'File')
          let fileList = resultList.filter((item) => item.runMode === 'File')
          this.projList = tempList.concat(fileList)

          this.listQuery.total = res.data.total

          let nodeProjects = itemGroupBy(this.projList, 'nodeId')
          this.getRuningProjectInfo(nodeProjects)
          //
          this.loadGroupList()
        }
        this.loading = false
      })
    },
    loadGroupList() {
      getProjectGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    getRuningProjectInfo(nodeProjects) {
      if (nodeProjects.length <= 0) {
        return
      }
      concurrentExecution(
        nodeProjects.map((item, index) => {
          return index
        }),
        3,
        (citem) => {
          // console.log(i);
          const data = nodeProjects[citem]
          return new Promise((resolve, reject) => {
            const ids = data.data.map((item) => {
              return item.projectId
            })
            if (ids.length <= 0) {
              return
            }
            const tempParams = {
              nodeId: data.type,
              ids: JSON.stringify(ids)
            }
            getRuningProjectInfo(tempParams, 'noTip')
              .then((res2) => {
                if (res2.code === 200) {
                  this.projList = this.projList.map((element) => {
                    if (res2.data[element.projectId] && element.nodeId === data.type) {
                      element.port = res2.data[element.projectId].port
                      element.pid = res2.data[element.projectId].pid
                      element.pids = res2.data[element.projectId].pids
                      element.status = element.pid > 0
                      element.error = res2.data[element.projectId].error
                    }
                    return element
                  })
                  resolve()
                } else {
                  this.projList = this.projList.map((element) => {
                    if (element.nodeId === data.type) {
                      element.port = 0
                      element.pid = 0
                      element.status = false
                      element.error = res2.msg
                    }
                    return element
                  })
                  reject()
                }
                // this.getRuningProjectInfo(nodeProjects, i + 1);
              })
              .catch(() => {
                this.projList = this.projList.map((element) => {
                  if (element.nodeId === data.type) {
                    element.port = 0
                    element.pid = 0
                    element.status = false
                    element.error = '网络异常'
                  }
                  return element
                })
                reject()
              })
          })
        }
      )
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `文件管理(${this.temp.name})`
      this.drawerFileVisible = true
    },
    // 关闭文件管理对话框
    onFileClose() {
      this.drawerFileVisible = false
      this.getNodeProjectData()
    },
    // 控制台
    handleConsole(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `控制台(${this.temp.name})`
      this.drawerConsoleVisible = true
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false
      this.getNodeProjectData()
    },
    //前往文件
    goFile() {
      // 关闭控制台
      this.onConsoleClose()
      this.onReadFileClose()
      this.handleFile(this.temp)
    },
    //前往控制台
    goConsole() {
      //关闭文件
      this.onFileClose()
      this.handleConsole(this.temp)
    },
    // 跟踪文件
    goReadFile(path, filename) {
      this.onFileClose()
      this.drawerReadFileVisible = true
      this.temp.readFilePath = (path + '/' + filename).replace(new RegExp('//', 'gm'), '/')
      this.drawerTitle = `跟踪文件(${filename})`
    },
    onReadFileClose() {
      this.drawerReadFileVisible = false
    },
    //选中项目
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys
    },
    batchClose() {
      this.batchVisible = false
      this.getNodeProjectData()
      this.selectedRowKeys = []
    },
    //批量开始
    batchStart() {
      if (this.selectedRowKeys.length <= 0) {
        $notification.warning({
          message: '请选中要启动的项目'
        })
        return
      }
      this.batchVisible = true
      this.batchTitle = '批量启动'
      this.projList = this.projList.map((item) => {
        delete item.cause
        return item
      })
      this.batchStartInfo(1)
    },
    //批量启动详情
    batchStartInfo(count) {
      if (count > this.selectedRowKeys.length) {
        return
      }
      let value = this.selectedRowKeys[count - 1]
      this.projList[value].cause = '启动中'
      count++
      if (this.projList[value].runMode !== 'File') {
        const params = {
          nodeId: this.projList[value].nodeId,
          id: this.projList[value].projectId
        }
        startProject(params)
          .then((data) => {
            this.projList[value].cause = data.msg
            this.selectedRowKeys = [...this.selectedRowKeys]
            this.batchStartInfo(count)
          })
          .catch(() => {
            this.projList[value].cause = '启动失败'
            this.selectedRowKeys = [...this.selectedRowKeys]
            this.batchStartInfo(count)
          })
      } else {
        this.projList[value].cause = '跳过'
        this.selectedRowKeys = [...this.selectedRowKeys]
        this.batchStartInfo(count)
      }
    },
    //批量重启
    batchRestart() {
      if (this.selectedRowKeys.length <= 0) {
        $notification.warning({
          message: '请选中要重启的项目'
        })
        return
      }
      this.batchVisible = true
      this.batchTitle = '批量重新启动'
      this.projList = this.projList.map((item) => {
        delete item.cause
        return item
      })
      this.batchRestartInfo(1)
    },
    //批量重启详情
    batchRestartInfo(count) {
      if (count > this.selectedRowKeys.length) {
        return
      }
      let value = this.selectedRowKeys[count - 1]
      this.projList[value].cause = '重新启动中'
      count++
      if (this.projList[value].runMode !== 'File') {
        const params = {
          nodeId: this.projList[value].nodeId,
          id: this.projList[value].projectId
        }
        restartProject(params)
          .then((data) => {
            this.projList[value].cause = data.msg
            this.selectedRowKeys = [...this.selectedRowKeys]
            this.batchRestartInfo(count)
          })
          .catch(() => {
            this.projList[value].cause = '重新启动失败'
            this.selectedRowKeys = [...this.selectedRowKeys]
            this.batchRestartInfo(count)
          })
      } else {
        this.projList[value].cause = '跳过'
        this.selectedRowKeys = [...this.selectedRowKeys]
        this.batchRestartInfo(count)
      }
    },
    //批量关闭
    batchStop() {
      if (this.selectedRowKeys.length <= 0) {
        $notification.warning({
          message: '请选中要关闭的项目'
        })
      }
      this.batchVisible = true
      this.batchTitle = '批量关闭启动'
      this.projList = this.projList.map((item) => {
        delete item.cause
        return item
      })
      this.batchStopInfo(1)
    },
    //批量关闭详情
    batchStopInfo(count) {
      if (count > this.selectedRowKeys.length) {
        return
      }
      let value = this.selectedRowKeys[count - 1]
      this.projList[value].cause = '关闭中'
      count++
      if (this.projList[value].runMode !== 'File') {
        const params = {
          nodeId: this.projList[value].nodeId,
          id: this.projList[value].projectId
        }
        stopProject(params)
          .then((data) => {
            this.projList[value].cause = data.msg
            this.selectedRowKeys = [...this.selectedRowKeys]
            this.batchStopInfo(count)
          })
          .catch(() => {
            this.projList[value].cause = '关闭失败'
            this.selectedRowKeys = [...this.selectedRowKeys]
            this.batchStopInfo(count)
          })
      } else {
        this.projList[value].cause = '跳过'
        this.selectedRowKeys = [...this.selectedRowKeys]
        this.batchStopInfo(count)
      }
    },
    // 获取复选框属性 判断是否可以勾选
    getCheckboxProps(record) {
      return {
        props: {
          disabled: record.runMode === 'File',
          name: record.name
        }
      }
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.getNodeProjectData()
    },
    delAll() {
      $confirm({
        title: '系统提示',
        content: '确定要清除服务端所有的项目缓存信息吗？清除后需要重新同步节点项目才能正常使用项目相关功能',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          delAllProjectCache().then((res) => {
            if (res.code == 200) {
              $notification.success({
                message: res.msg
              })
              this.getNodeProjectData()
            }
          })
        }
      })
    },
    // 排序
    sortItemHander(record, index, method) {
      const msgData = {
        top: '确定要将此数据置顶吗？',
        up: '确定要将此数上移吗？',
        down: '确定要将此数据下移吗？下移操作可能因为列表后续数据没有排序值操作无效！'
      }
      let msg = msgData[method] || '确定要操作吗？'
      if (!record.sortValue) {
        msg += ' 当前数据为默认状态,操后上移或者下移可能不会达到预期排序,还需要对相关数据都操作后才能达到预期排序'
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.projList[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      $confirm({
        title: '系统提示',
        content: msg,
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 解锁
          sortItemProject({
            id: record.id,
            method: method,
            compareId: compareId
          }).then((res) => {
            if (res.code == 200) {
              $notification.success({
                message: res.msg
              })

              this.getNodeProjectData()
              return false
            }
          })
        }
      })
      // console.log(record, index, method);
    },
    // 触发器
    handleTrigger(record) {
      this.temp = Object.assign({}, record)
      this.tempVue = Vue
      getProjectTriggerUrl({
        id: record.id
      }).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res)
          this.triggerVisible = true
        }
      })
    },
    // 重置触发器
    resetTrigger() {
      getProjectTriggerUrl({
        id: this.temp.id,
        rest: 'rest'
      }).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.fillTriggerResult(res)
        }
      })
    },
    fillTriggerResult(res) {
      this.temp.triggerUrl = `${location.protocol}//${location.host}${res.data.triggerUrl}`
      this.temp.batchTriggerUrl = `${location.protocol}//${location.host}${res.data.batchTriggerUrl}`

      this.temp = { ...this.temp }
    },
    toNode(nodeId) {
      const newpage = this.$router.resolve({
        name: 'node_' + nodeId,
        path: '/node/list',
        query: {
          ...this.$route.query,
          nodeId: nodeId,
          pId: 'manage',
          id: 'manageList'
        }
      })
      window.open(newpage.href, '_blank')
    }
  }
}
</script>
<style scoped></style>
