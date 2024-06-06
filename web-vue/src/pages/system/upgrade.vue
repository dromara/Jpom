<template>
  <div>
    <a-tabs type="card" default-active-key="1">
      <a-tab-pane key="1" :tab="$t('pages.system.upgrade.bd5f786d')"> <upgrade></upgrade></a-tab-pane>
      <a-tab-pane key="2" :tab="$t('pages.system.upgrade.6a4f20af')">
        <CustomTable
          is-show-tools
          default-auto-refresh
          :auto-refresh-time="30"
          table-name="upgrade-node-list"
          :empty-description="$t('pages.system.upgrade.c2a3f0cd')"
          :active-page="activePage"
          :columns="columns"
          :data-source="list"
          bordered
          size="middle"
          row-key="id"
          :pagination="pagination"
          :row-selection="rowSelection"
          :scroll="{
            x: 'max-content'
          }"
          @change="changePage"
          @refresh="getNodeList"
        >
          <template #title>
            <a-row v-if="percentage">
              <a-col span="24"><a-progress v-if="percentage" :percent="percentage" /> </a-col>
            </a-row>
            <a-space>
              <a-input
                v-model:value="listQuery['%name%']"
                class="search-input-item"
                :placeholder="$t('pages.system.upgrade.fa8d810f')"
                @press-enter="getNodeList"
              />
              <a-input
                v-model:value="listQuery['%jpomUrl%']"
                class="search-input-item"
                :placeholder="$t('pages.system.upgrade.a918ed23')"
                @press-enter="getNodeList"
              />
              <a-input
                v-model:value="listQuery['%jpomVersion%']"
                class="search-input-item"
                :placeholder="$t('pages.system.upgrade.24fef8f1')"
                @press-enter="getNodeList"
              />
              <a-select
                v-model:value="listQuery.groupName"
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
                :placeholder="$t('pages.system.upgrade.e740d8cb')"
                class="search-input-item"
              >
                <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
              </a-select>
              <a-button :loading="loading" type="primary" @click="getNodeList">{{
                $t('pages.system.upgrade.53c2763c')
              }}</a-button>

              <a-select
                v-model:value="temp.protocol"
                :placeholder="$t('pages.system.upgrade.f5958e7d')"
                class="search-input-item"
              >
                <a-select-option value="WebSocket">WebSocket</a-select-option>
                <a-select-option value="Http">Http</a-select-option>
              </a-select>
              <a-button type="primary" @click="batchUpdate">{{ $t('pages.system.upgrade.1724d4b9') }}</a-button>
              |
              <a-upload
                name="file"
                accept=".jar,.zip"
                action=""
                :disabled="!!percentage"
                :show-upload-list="false"
                :multiple="false"
                :before-upload="beforeUpload"
              >
                <LoadingOutlined v-if="percentage" />
                <a-button v-else type="primary"> <UploadOutlined />{{ $t('pages.system.upgrade.bdc35ba3') }} </a-button>
              </a-upload>

              <!-- 打包时间：{{ agentTimeStamp | version }}</div> -->
            </a-space>
          </template>
          <template #toolPrefix>
            <a-tooltip
              :title="`${$t('pages.system.upgrade.85c82af7')}${agentTimeStamp || $t('pages.system.upgrade.5f51a112')}`"
            >
              Agent{{ $t('pages.system.upgrade.d826aba2') }}{{ version_filter(agentVersion) }}
              <a-tag v-if="temp.upgrade" color="pink" @click="downloadRemoteEvent">
                {{ $t('pages.system.upgrade.19f7beca') }}{{ temp.newVersion }} <DownloadOutlined />
              </a-tag>
              <!-- </div> -->
            </a-tooltip></template
          >
          <template #tableBodyCell="{ column, text, record }">
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
                {{ statusMap[text] || $t('pages.system.upgrade.5f51a112') }}
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
                  {{ text.percent === 100 ? $t('pages.system.upgrade.cabe384') : $t('pages.system.upgrade.3b5150a7') }}
                </div>
                <a-progress :percent="text.percent" />
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'operation'">
              <a-button type="primary" size="small" @click="updateNodeHandler(record)">{{
                $t('pages.system.upgrade.7f1798e3')
              }}</a-button>
            </template>
          </template>
        </CustomTable>
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
  components: {
    upgrade
  },
  inject: ['globalLoading'],
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
          title: this.$t('pages.system.upgrade.255211fb'),
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.system.upgrade.a918ed23'),
          dataIndex: 'jpomUrl',
          sorter: true,
          width: '150px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.system.upgrade.12d0e469'),
          dataIndex: 'groupName',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: this.$t('pages.system.upgrade.71ffd54'),
          dataIndex: 'status',
          width: '130px',
          ellipsis: true
        },
        {
          title: this.$t('pages.system.upgrade.14777df6'),
          dataIndex: 'jpomVersion',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('pages.system.upgrade.ff006622'),
          dataIndex: 'version',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('pages.system.upgrade.14b1dca5'),
          dataIndex: 'timeStamp',
          width: '170px',
          ellipsis: true
        },
        {
          title: this.$t('pages.system.upgrade.4a4e5b97'),
          dataIndex: 'upTime',
          width: '110px',
          ellipsis: true
        },

        {
          title: this.$t('pages.system.upgrade.9f69259c'),
          dataIndex: 'updateStatus',
          ellipsis: true
        },
        // {title: '自动更新', dataIndex: 'autoUpdate', ellipsis: true,},
        {
          title: this.$t('pages.system.upgrade.3bb962bf'),
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
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    socketUrl() {
      return getWebSocketUrl('/socket/node_update', `userId=${this.getLongTermToken()}&nodeId=system&type=nodeUpdate`)
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
    $tl(key, ...args) {
      return this.$t(`pages.system.upgrade.${key}`, ...args)
    },
    status_filter(value) {
      return (value && value) || this.$t('pages.system.upgrade.5f51a112')
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
          message: `web socket ${this.$t('pages.system.upgrade.d75d207f')},${this.$t('pages.system.upgrade.763330b')}`
        })
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err)
        clearInterval(this.heart)
        $message.warning(this.$t('pages.system.upgrade.8a2aae09'))
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
          message: this.$t('pages.system.upgrade.a57f20c2')
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
          message: this.$t('pages.system.upgrade.861928db')
        })
        return
      }
      const len = this.tableSelections.length
      const html = `
        ${this.$t('pages.system.upgrade.beb005a')}
        <b style='color:red;font-size: 20px;'>
          ${len}
        </b>
        ${this.$t('pages.system.upgrade.7c75700')}
        <b style='color:red;font-size: 20px;'>
          ${this.agentVersion || '--'}
        </b>
        ${this.$t('pages.system.upgrade.b8e15906')}
        <ul style='color:red;'>
          <li>${this.$t('pages.system.upgrade.94225c9f')}<b>${this.$t('pages.system.upgrade.3794dd57')}</b></li>
          <li>${this.$t('pages.system.upgrade.d4b5dacd')}</li>
          <li>${this.$t('pages.system.upgrade.81be8808')}</li>
        </ul>
      `
      $confirm({
        title: this.$t('pages.system.upgrade.a8fe4c17'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('pages.system.upgrade.7da4a591'),
        cancelText: this.$t('pages.system.upgrade.43105e21'),
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
      const html = `
        ${this.$t('pages.system.upgrade.add6ee25')}
        <ul style='color:red;'>
          <li>${this.$t('pages.system.upgrade.8d7b0c1f')}<b>${this.$t('pages.system.upgrade.3794dd57')}</b></li>
          <li>${this.$t('pages.system.upgrade.ec360a25')}</li>
        </ul>
      `
      $confirm({
        title: this.$t('pages.system.upgrade.a8fe4c17'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('pages.system.upgrade.7da4a591'),
        cancelText: this.$t('pages.system.upgrade.43105e21'),
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
      const html = `
        ${this.$t('pages.system.upgrade.106295d')}
        <ul style='color:red;'>
          <li>${this.$t('pages.system.upgrade.5e362844')}</li>
          <li>${this.$t('pages.system.upgrade.3e3c1748')}<b>${this.$t('pages.system.upgrade.3794dd57')}</b></li>
          <li>${this.$t('pages.system.upgrade.8b2df049')}</li>
          <li>${this.$t('pages.system.upgrade.d4b5dacd')}</li>
        </ul>
      `
      $confirm({
        title: this.$t('pages.system.upgrade.a8fe4c17'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('pages.system.upgrade.7da4a591'),
        cancelText: this.$t('pages.system.upgrade.43105e21'),
        onOk: () => {
          return downloadRemote().then((res) => {
            if (res.code === 200) {
              $notification.success({ message: res.msg })
              this.getNodeList()
            } else {
              //$notification.error({ message: res.msg });
            }
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
