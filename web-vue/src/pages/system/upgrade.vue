<template>
  <div>
    <a-tabs type="card" default-active-key="1">
      <a-tab-pane key="1" tab="服务端"> <upgrade></upgrade></a-tab-pane>
      <a-tab-pane key="2" tab="所有节点(插件端)">
        <a-table
          :columns="columns"
          :data-source="list"
          bordered
          size="middle"
          rowKey="id"
          :pagination="pagination"
          @change="changePage"
          :row-selection="rowSelection"
          :scroll="{
            x: 'max-content'
          }"
        >
          <template #title>
            <a-row v-if="percentage">
              <a-col span="24"><a-progress v-if="percentage" :percent="percentage" /> </a-col>
            </a-row>
            <a-space>
              <a-input
                class="search-input-item"
                @pressEnter="getNodeList"
                v-model:value="listQuery['%name%']"
                placeholder="节点名称"
              />
              <a-input
                class="search-input-item"
                @pressEnter="getNodeList"
                v-model:value="listQuery['%jpomUrl%']"
                placeholder="节点地址"
              />
              <a-input
                class="search-input-item"
                @pressEnter="getNodeList"
                v-model:value="listQuery['%jpomVersion%']"
                placeholder="插件版本"
              />
              <a-select
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
                v-model:value="listQuery.groupName"
                allowClear
                placeholder="分组"
                class="search-input-item"
              >
                <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
              </a-select>
              <a-button :loading="loading" type="primary" @click="getNodeList">搜索</a-button>

              <a-select v-model:value="temp.protocol" placeholder="升级协议" class="search-input-item">
                <a-select-option value="WebSocket">WebSocket</a-select-option>
                <a-select-option value="Http">Http</a-select-option>
              </a-select>
              <a-button type="primary" @click="batchUpdate">批量更新</a-button>
              |
              <a-upload
                name="file"
                accept=".jar,.zip"
                action=""
                :disabled="!!percentage"
                :showUploadList="false"
                :multiple="false"
                :before-upload="beforeUpload"
              >
                <LoadingOutlined v-if="percentage" />
                <a-button type="primary" v-else> <UploadOutlined />上传包 </a-button>
              </a-upload>
              <a-tooltip :title="`打包时间：${agentTimeStamp || '未知'}`">
                Agent版本：{{ version_filter(agentVersion) }}
                <a-tag v-if="temp.upgrade" color="pink" @click="downloadRemoteEvent">
                  新版本：{{ temp.newVersion }} <DownloadOutlined />
                </a-tag>
                <!-- </div> -->
              </a-tooltip>

              <!-- 打包时间：{{ agentTimeStamp | version }}</div> -->
            </a-space>
          </template>
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.tooltip">
              <a-tooltip :title="text">
                <span>{{ text }}</span>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'jpomVersion'">
              {{ version_filter(text) }}
            </template>
            <template v-else-if="column.dataIndex === 'upTime'">
              <a-tooltip placement="topLeft" :title="formatDuration(text)">
                <span>{{ formatDuration(text, '', 2) }}</span>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <a-tag :color="text === 1 ? 'green' : 'pink'" style="margin-right: 0">
                {{ statusMap[text] || '未知' }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'updateStatus'">
              <div v-if="text && text.type === 'restarting'">
                <a-tooltip :title="text.data"> {{ text.data }} </a-tooltip>
              </div>
              <div v-if="text && text.type === 'error'">
                <a-tooltip :title="text.data"> {{ text.data }} </a-tooltip>
              </div>
              <div v-if="text && text.type === 'uploading'">
                <div class="text">
                  {{ text.percent === 100 ? '上传成功' : '正在上传文件' }}
                </div>
                <a-progress :percent="text.percent" />
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'operation'">
              <a-button type="primary" size="small" @click="updateNodeHandler(record)">更新</a-button>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import upgrade from '@/components/upgrade'
import { checkVersion, downloadRemote, uploadAgentFile, uploadAgentFileMerge } from '@/api/node'
import { machineListData, machineListGroup, statusMap } from '@/api/system/assets-machine'
import { mapState } from 'pinia'
import { useUserStore } from '@/stores/user'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, formatDuration } from '@/utils/const'
import { getWebSocketUrl } from '@/api/config'
import { uploadPieces } from '@/utils/upload-pieces'

export default {
  inject: ['globalLoading'],
  components: {
    upgrade
  },
  data() {
    return {
      agentVersion: '',
      agentTimeStamp: '',
      websocket: null,

      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      groupList: [],
      columns: [
        {
          title: '机器名称',
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '节点地址',
          dataIndex: 'jpomUrl',
          sorter: true,
          width: '150px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '分组名',
          dataIndex: 'groupName',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: '机器状态(缓存)',
          dataIndex: 'status',
          width: '130px',
          ellipsis: true
        },
        {
          title: '缓存版本号',
          dataIndex: 'jpomVersion',
          width: '100px',
          ellipsis: true
        },
        {
          title: '实时版本号',
          dataIndex: 'version',
          width: '100px',
          ellipsis: true,
          scopedSlots: { customRender: 'version' }
        },
        {
          title: '打包时间',
          dataIndex: 'timeStamp',
          width: '170px',
          ellipsis: true,
          scopedSlots: { customRender: 'timeStamp' }
        },
        {
          title: '运行时间',
          dataIndex: 'upTime',
          width: '110px',
          ellipsis: true,
          scopedSlots: { customRender: 'upTime' }
        },

        {
          title: '更新状态',
          dataIndex: 'updateStatus',
          ellipsis: true
        },
        // {title: '自动更新', dataIndex: 'autoUpdate', ellipsis: true, scopedSlots: {customRender: 'autoUpdate'}},
        {
          title: '操作',
          dataIndex: 'operation',
          width: '80px',

          align: 'center',
          fixed: 'right'
        }
      ],
      nodeVersion: {},
      nodeStatus: {},
      tableSelections: [],
      temp: {},
      percentage: 0,
      statusMap
    }
  },
  computed: {
    ...mapState(useUserStore, ['getLongTermToken']),

    rowSelection() {
      return {
        onChange: this.tableSelectionChange,
        selectedRowKeys: this.tableSelections
      }
    },
    socketUrl() {
      return getWebSocketUrl('/socket/node_update', `userId=${this.getLongTermToken}&nodeId=system&type=nodeUpdate`)
    },
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  mounted() {
    this.getNodeList()
    this.loadGroupList()
    this.checkAgentFileVersion()
    // 监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
    window.onbeforeunload = () => {
      this.close()
    }
  },
  beforeUnmount() {
    this.close()
  },
  methods: {
    status_filter(value) {
      return (value && value) || '未知'
    },
    version_filter(value) {
      return value || '---'
    },
    close() {
      this.socket?.close()
      clearInterval(this.heart)
      this.socket = null
    },
    formatDuration,
    updateList() {
      this.list = this.list.map((item) => {
        let itemData = this.nodeVersion[item.id]
        if (itemData) {
          try {
            itemData = JSON.parse(itemData)
            item.version = itemData.version
            item.timeStamp = itemData.timeStamp
            item.upTime = itemData.upTime
          } catch (e) {
            item.version = itemData
          }
        }
        // item.nextTIme = Date.now();
        item.updateStatus = this.nodeStatus[item.id]
        // data.push(item);
        return { ...item }
      })
    },
    // 获取所有的分组
    loadGroupList() {
      machineListGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    initWebsocket(ids) {
      if (this.socket) {
        this.initHeart(ids)
        return
      }
      // if (!this.socket || this.socket.readyState !== this.socket.OPEN || this.socket.readyState !== this.socket.CONNECTING) {
      this.socket = new WebSocket(this.socketUrl)
      // }

      this.socket.onopen = () => {
        this.initHeart(ids)
      }
      this.socket.onmessage = ({ data: socketData }) => {
        let msgObj
        try {
          msgObj = JSON.parse(socketData)
        } catch (e) {
          console.error(e)
        }
        if (msgObj) {
          const { command, data, nodeId } = msgObj
          this[`${command}Result`] && this[`${command}Result`](data, nodeId)
        }
      }
      this.socket.onerror = (err) => {
        console.error(err)
        $notification.error({
          message: 'web socket 错误,请检查是否开启 ws 代理'
        })
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err)
        this.$message.warning('会话已经关闭[upgrade]')
        clearInterval(this.heart)
      }
    },
    checkAgentFileVersion() {
      // 获取是否有新版本
      checkVersion().then((res) => {
        if (res.code === 200) {
          let upgrade = res.data?.upgrade
          if (upgrade) {
            //
            this.temp = {
              ...this.temp,
              upgrade: upgrade,
              newVersion: res.data.tagName
            }
            // this.temp.upgrade = upgrade;
            // this.temp.newVersion = ;
          } else {
            this.temp = { ...this.temp, upgrade: false }
          }
        }
      })
    },
    initHeart(ids) {
      this.sendMsg('getNodeList:' + ids.join(','))
      this.getAgentVersion()
      // 创建心跳，防止掉线
      this.heart && clearInterval(this.heart)
      this.heart = setInterval(() => {
        this.sendMsg('heart', {})
      }, 2000)
    },
    // refresh() {
    //   // if (this.socket) {
    //   //   this.socket.close();
    //   // }
    //   //this.nodeStatus = {};
    //   //this.nodeVersion = {};
    //   this.getNodeList();
    // },
    batchUpdate() {
      if (this.tableSelections.length === 0) {
        $notification.warning({
          message: '请选择要升级的节点'
        })
        return
      }
      this.updateNode()
    },
    updateNodeHandler(record) {
      this.tableSelections = [record.id]
      this.updateNode()
    },
    tableSelectionChange(selectedRowKeys) {
      this.tableSelections = selectedRowKeys
    },
    sendMsg(command, params) {
      this.socket.send(
        JSON.stringify({
          command,
          params
        })
      )
    },
    getNodeList() {
      this.loading = true
      machineListData(this.listQuery)
        .then((res) => {
          if (res.code === 200) {
            this.list = res.data.result
            this.listQuery.total = res.data.total
            let ids = this.list.map((item) => {
              return item.id
            })
            if (ids.length > 0) {
              this.initWebsocket(ids)
            }
          }

          this.updateList()
        })
        .finally(() => {
          this.loading = false
        })
    },
    // getNodeListResult(data) {
    //   this.list = data;
    //   this.updateList();
    // },
    getAgentVersion() {
      this.sendMsg('getAgentVersion')
    },
    getAgentVersionResult(data) {
      try {
        let newData = JSON.parse(data)
        this.agentVersion = newData.version
        this.agentTimeStamp = newData.timeStamp
      } catch (e) {
        this.agentVersion = data || ''
      }
      this.checkAgentFileVersion()
      this.updateList()
    },
    getVersionResult(data, nodeId) {
      this.nodeVersion = Object.assign({}, this.nodeVersion, {
        [nodeId]: data
      })
      this.updateList()
    },
    onErrorResult(data, nodeId) {
      if (nodeId) {
        //
        this.nodeStatus = Object.assign({}, this.nodeStatus, {
          [nodeId]: {
            type: 'error',
            data: data
          }
        })
      } else {
        $notification.warning({
          message: data
        })
      }
      this.updateList()
    },
    updateNodeResult(data, nodeId) {
      const { completeSize, size } = data
      this.nodeStatus = Object.assign({}, this.nodeStatus, {
        [nodeId]: {
          ...data,
          type: 'uploading',
          percent: Math.floor((completeSize / size) * 100)
        }
      })
      this.updateList()
    },
    restartResult(data, nodeId) {
      this.nodeStatus = Object.assign({}, this.nodeStatus, {
        [nodeId]: {
          type: 'restarting',
          data: data
        }
      })
      this.updateList()
    },
    updateNode() {
      if (!this.agentVersion) {
        $notification.error({
          message: '请先上传或者下载新版本'
        })
        return
      }
      const len = this.tableSelections.length
      const html =
        "确认要将选中的  <b style='color:red;font-size: 20px;'>" +
        len +
        "</b> 个节点升级到 <b style='color:red;font-size: 20px;'>" +
        (this.agentVersion || '--') +
        "</b> 吗？<ul style='color:red;'>" +
        '<li>升级前请阅读更新日志里面的说明和注意事项并且<b>请注意备份数据防止数据丢失！！</b></li>' +
        '<li>如果升级失败需要手动恢复奥</li>' +
        '<li>一般情况下不建议降级操作</li>' +
        ' </ul>'

      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          this.sendMsg('updateNode', {
            ids: this.tableSelections,
            protocol: this.temp.protocol
          })
          this.tableSelections = []
        }
      })
    },
    beforeUpload(file) {
      const html =
        "确认要上传最新的插件包吗？<ul style='color:red;'>" +
        '<li>上传前请阅读更新日志里面的说明和注意事项并且更新前<b>请注意备份数据防止数据丢失！！</b></li>' +
        '<li>上传前请检查包是否完整,否则可能出现更新后无法正常启动的情况！！</li>' +
        ' </ul>'

      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          this.percentage = 0
          uploadPieces({
            file,
            resolveFileProcess: (msg) => {
              this.globalLoading({
                spinning: true,
                tip: msg
              })
            },
            resolveFileEnd: () => {
              this.globalLoading(false)
            },
            process: (process) => {
              this.percentage = Math.max(this.percentage, process)
            },
            success: (uploadData) => {
              // 准备合并
              uploadAgentFileMerge({
                ...uploadData[0]
              }).then((res) => {
                if (res.code === 200) {
                  this.getNodeList()
                }
                setTimeout(() => {
                  this.percentage = 0
                }, 2000)
              })
            },
            error: (msg) => {
              $notification.error({
                message: msg
              })
            },
            uploadCallback: (formData) => {
              return new Promise((resolve, reject) => {
                // 上传文件
                uploadAgentFile(formData)
                  .then((res) => {
                    if (res.code === 200) {
                      resolve()
                    } else {
                      reject()
                    }
                  })
                  .catch(() => {
                    reject()
                  })
              })
            }
          })
        }
      })

      return false
    },
    // 下载远程最新文件
    downloadRemoteEvent() {
      const html =
        "确认要下载最新版本吗？<ul style='color:red;'>" +
        '<li>下载速度根据网速来确定,如果网络不佳下载会较慢</li>' +
        '<li>下载前请阅读更新日志里面的说明和注意事项并且更新前<b>请注意备份数据防止数据丢失！！</b></li>' +
        '<li>下载完成后需要手动选择更新到节点才能完成节点更新奥</li>' +
        '<li>如果升级失败需要手动恢复奥</li>' +
        ' </ul>'
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            downloadRemote()
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({ message: res.msg })
                  that.getNodeList()
                } else {
                  //$notification.error({ message: res.msg });
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.getNodeList()
    }
  }
}
</script>
