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
    <a-tabs type="card" default-active-key="1">
      <a-tab-pane key="1" :tab="$t('i18n_55abea2d61')"> <upgrade></upgrade></a-tab-pane>
      <a-tab-pane key="2" :tab="$t('i18n_78dccb6e97')">
        <CustomTable
          is-show-tools
          default-auto-refresh
          :auto-refresh-time="30"
          table-name="upgrade-node-list"
          :empty-description="$t('i18n_0fca8940a8')"
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
            <a-alert
              v-if="clusterAlert"
              type="warning"
              show-icon
              :message="clusterAlert"
              style="margin-bottom: 10px"
            />
            <a-row v-if="percentage">
              <a-col span="24"><a-progress v-if="percentage" :percent="percentage" /> </a-col>
            </a-row>
            <a-space>
              <a-input
                v-model:value="listQuery['%name%']"
                class="search-input-item"
                :placeholder="$t('i18n_b1785ef01e')"
                @press-enter="getNodeList"
              />
              <a-input
                v-model:value="listQuery['%jpomUrl%']"
                class="search-input-item"
                :placeholder="$t('i18n_c1786d9e11')"
                @press-enter="getNodeList"
              />
              <a-input
                v-model:value="listQuery['%jpomVersion%']"
                class="search-input-item"
                :placeholder="$t('i18n_a912a83e6f')"
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
                :placeholder="$t('i18n_829abe5a8d')"
                class="search-input-item"
              >
                <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
              </a-select>
              <a-button :loading="loading" type="primary" @click="getNodeList">{{ $t('i18n_e5f71fc31e') }}</a-button>

              <a-select v-model:value="temp.protocol" :placeholder="$t('i18n_0836332bf6')" class="search-input-item">
                <a-select-option value="WebSocket">WebSocket</a-select-option>
                <a-select-option value="Http">Http</a-select-option>
              </a-select>
              <a-button type="primary" @click="batchUpdate">{{ $t('i18n_463e2bed82') }}</a-button>
              |
              <a-upload
                name="file"
                accept=".jar,.zip,.tar.gz"
                action=""
                :disabled="!!percentage"
                :show-upload-list="false"
                :multiple="false"
                :before-upload="beforeUpload"
              >
                <LoadingOutlined v-if="percentage" />
                <a-button v-else type="primary"> <UploadOutlined />{{ $t('i18n_080b914139') }} </a-button>
              </a-upload>

              <!-- 打包时间：{{ agentTimeStamp | version }}</div> -->
            </a-space>
          </template>
          <template #toolPrefix>
            <a-space size="small" wrap>
              <a-tooltip :title="`${$t('i18n_3f78f88499')}${agentTimeStamp || $t('i18n_1622dc9b6b')}`">
                Agent{{ $t('i18n_2684c4634d') }}{{ version_filter(agentVersion) }}
                <span v-if="packageTypeLabel"> · {{ $t('i18n_52d9520ee1') }}: {{ packageTypeLabel }}</span>
              </a-tooltip>
              <!-- 按集群实际存在的插件端类型逐条展示升级包状态：混合部署时 Java 与 Go 各自一份 -->
              <template v-for="row in packageRows" :key="row.key">
                <a-tag :color="row.color">{{ row.label }}</a-tag>
                <a-button v-if="row.needDownload" type="link" size="small" @click="downloadPackageEvent(row)">
                  <DownloadOutlined />{{ row.actionText }}
                </a-button>
              </template>
              <!-- 旧服务端（无 packages 字段）保持原有单包展示 -->
              <a-tag v-if="!packageRows.length && temp.upgrade" color="pink" @click="downloadRemoteEvent">
                {{ $t('i18n_ac2f4259f1') }}{{ temp.newVersion }} <DownloadOutlined />
              </a-tag>
            </a-space>
          </template>
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
                {{ statusMap[text] || $t('i18n_1622dc9b6b') }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'engine'">
              <a-tag v-if="text === 'go'" color="green">{{ $t('i18n_11b8cdd4a6') }}</a-tag>
              <a-tag v-else-if="text === 'java'" color="blue">{{ $t('i18n_ac0399d13b') }}</a-tag>
              <span v-else>{{ $t('i18n_1622dc9b6b') }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'platform'">
              {{ text || $t('i18n_7daad00d6a') }}
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
                  {{ text.percent === 100 ? $t('i18n_a7699ba731') : $t('i18n_c7099dabf6') }}
                </div>
                <a-progress :percent="text.percent" />
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'operation'">
              <a-tooltip :title="nodeUpgradeTip(record)">
                <a-button type="primary" size="small" :disabled="!!nodeUpgradeTip(record)" @click="updateNodeHandler(record)">
                  {{ $t('i18n_32ac152be1') }}
                </a-button>
              </a-tooltip>
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
          title: this.$t('i18n_e4013f8b81'),
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_c1786d9e11'),
          dataIndex: 'jpomUrl',
          sorter: true,
          width: '150px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_b37b786351'),
          dataIndex: 'groupName',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: this.$t('i18n_2fc0d53656'),
          dataIndex: 'status',
          width: '130px',
          ellipsis: true
        },
        {
          // 与机器管理同一概念同一叫法：引擎（agent engine）统一展示为「插件端类型」
          title: this.$t('i18n_084fca46ee'),
          dataIndex: 'engine',
          width: '80px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_4981287fe5'),
          dataIndex: 'platform',
          width: '120px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_3b885fca15'),
          dataIndex: 'jpomVersion',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_9829e60a29'),
          dataIndex: 'version',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_2c014aeeee'),
          dataIndex: 'timeStamp',
          width: '170px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_9f70e40e04'),
          dataIndex: 'upTime',
          width: '110px',
          ellipsis: true
        },

        {
          title: this.$t('i18n_597b1a5130'),
          dataIndex: 'updateStatus',
          ellipsis: true
        },
        // {title: '自动更新', dataIndex: 'autoUpdate', ellipsis: true,},
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          width: '1%',

          align: 'center',
          fixed: 'right'
        }
      ],

      nodeVersion: {},
      nodeStatus: {},
      tableSelections: [],
      // 升级包总览（服务端聚合：集群里有哪些插件端类型、每类包是否已上传/是否最新）
      overview: {
        engines: { java: 0, go: 0, unknown: 0 },
        packages: [],
        latestVersion: ''
      },
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
      return getWebSocketUrl('/socket/node_update', `nodeId=system&type=nodeUpdate`)
    },
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    /**
     * 当前本地升级包的引擎/平台展示（Java 升级包 / Go 升级容器 + 平台）
     */
    packageTypeLabel() {
      const engine = this.temp.packageEngine
      if (!engine) {
        return ''
      }
      const type = engine === 'go' ? this.$t('i18n_baee195ca8') : this.$t('i18n_6fb5d67e27')
      const platform = engine === 'go' && this.temp.packagePlatform ? ` (${this.temp.packagePlatform})` : ''
      return `${type}${platform}`
    },
    /**
     * 集群插件端类型：mixed（Java 与 Go 并存）/ java / go（服务端按资产表统计，不受列表分页影响）
     */
    clusterType() {
      const engines = this.overview.engines || {}
      const java = engines.java || 0
      const go = engines.go || 0
      if (java > 0 && go > 0) {
        return 'mixed'
      }
      return go > 0 ? 'go' : 'java'
    },
    /**
     * 混合部署提示：需要分别准备两类升级包，缺哪类哪类升不了
     */
    clusterAlert() {
      if (this.clusterType !== 'mixed') {
        return ''
      }
      const engines = this.overview.engines || {}
      return this.$t('i18n_b879f13b73', { java: engines.java || 0, go: engines.go || 0 })
    },
    /**
     * 按引擎/平台展示的升级包状态行（混合时 Java 与 Go 各一行）
     */
    packageRows() {
      const latest = this.overview.latestVersion
      return (this.overview.packages || []).map((item) => {
        const typeText = item.engine === 'go' ? this.$t('i18n_baee195ca8') : this.$t('i18n_6fb5d67e27')
        const platformText = item.engine === 'go' && item.platform ? ` (${item.platform})` : ''
        const actionText = item.engine === 'go' ? this.$t('i18n_74f40f3cea') : this.$t('i18n_df3f233fb3')
        let label
        let color
        let needDownload = false
        if (item.missing) {
          label = `${typeText}${platformText}: ${this.$t('i18n_7c166a0415')}`
          color = 'pink'
          needDownload = true
        } else if (item.latestKnown && !item.upToDate) {
          label = `${typeText}${platformText}: ${item.version} → ${latest}`
          color = 'orange'
          needDownload = true
        } else {
          label = `${typeText}${platformText}: ${item.version || '---'}${item.upToDate ? ' ' + this.$t('i18n_72ac9a729b') : ''}`
          color = 'green'
        }
        return {
          key: `${item.engine}-${item.platform || ''}`,
          engine: item.engine,
          platform: item.platform,
          label,
          color,
          needDownload,
          actionText
        }
      })
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
      return (value && value) || this.$t('i18n_1622dc9b6b')
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
        item.engine = ''
        item.platform = ''
        if (itemData) {
          try {
            itemData = JSON.parse(itemData)
            item.version = itemData.version
            item.timeStamp = itemData.timeStamp
            item.upTime = itemData.upTime
            // 引擎：优先 manifest.engine；旧版 Go 插件端没有 engine 时用 jarFile 兜底（Java 运行文件一定是 .jar）
            if (itemData.engine) {
              item.engine = itemData.engine === 'go' ? 'go' : 'java'
            } else if (itemData.jarFile) {
              item.engine = /\.jar$/i.test(itemData.jarFile) ? 'java' : 'go'
            }
            // 平台：优先 manifest.platform（Java/Go 插件端都上报）；旧版插件端只有 arch + osName 时按 Server 同口径映射
            item.platform = itemData.platform || ''
            if (!item.platform && itemData.arch) {
              const osMap = { linux: 'linux', windows: 'windows', 'mac os x': 'darwin' }
              const os = osMap[(itemData.osName || '').toLowerCase()]
              item.platform = os ? `${os}/${itemData.arch}` : ''
            }
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
      if (
        this.socket &&
        (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)
      ) {
        this.initHeart(ids)
        return
      }
      this.socket = null
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
          message: `web socket ${this.$t('i18n_7030ff6470')},${this.$t('i18n_226a6f9cdd')}`
        })
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        // 离开页面/切换页签时关闭属于正常事件，不作为 console error 噪音输出
        console.debug(err)
        clearInterval(this.heart)
        $message.warning(this.$t('i18n_23b38c8dad'))
      }
    },
    checkAgentFileVersion() {
      // 获取是否有新版本
      checkVersion().then((res) => {
        if (res.code === 200) {
          let upgrade = res.data?.upgrade
          // 集群需要的升级包（旧服务端没有该字段时保持原有单包展示）
          if (res.data.packages) {
            this.overview = {
              engines: res.data.engines || { java: 0, go: 0, unknown: 0 },
              packages: res.data.packages || [],
              latestVersion: res.data.tagName || ''
            }
          }
          this.temp = {
            ...this.temp,
            // 本地升级包的引擎/平台（Go 集群按平台下载/升级）
            packageEngine: res.data.engine || '',
            packagePlatform: res.data.platform || ''
          }
          if (upgrade) {
            //
            this.temp = {
              ...this.temp,
              upgrade: upgrade,
              newVersion: res.data.tagName,
              downloadSource: res.data.downloadSource
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
      this.sendMsg('getNodeList:' + ids.join(','), undefined, true)
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
          message: this.$t('i18n_27f105b0c3')
        })
        return
      }
      const rows = this.tableSelections
        .map((id) => this.list.find((item) => item.id === id))
        .filter((item) => !!item)
      if (this.overview.packages.length) {
        // 升级包未就绪的节点先拦下：混合部署时不静默失败（跳过并说明原因）
        const allowed = rows.filter((row) => !this.nodeUpgradeTip(row)).map((row) => row.id)
        const skipped = rows.length - allowed.length
        if (allowed.length === 0) {
          $notification.warning({
            message: this.$t('i18n_9a4aac7429', { count: skipped })
          })
          return
        }
        if (skipped > 0) {
          $notification.warning({
            message: this.$t('i18n_9a4aac7429', { count: skipped })
          })
        }
        this.updateNode(allowed)
        return
      }
      this.updateNode()
    },
    /**
     * 比较两段版本号（容忍 v 前缀与 -beta 之类后缀）
     *
     * @return 1 / 0 / -1
     */
    compareVersion(left, right) {
      const parse = (value) =>
        String(value || '')
          .replace(/^v/i, '')
          .split('.')
          .map((part) => parseInt(part, 10) || 0)
      const a = parse(left)
      const b = parse(right)
      const len = Math.max(a.length, b.length)
      for (let i = 0; i < len; i++) {
        const l = a[i] || 0
        const r = b[i] || 0
        if (l > r) {
          return 1
        }
        if (l < r) {
          return -1
        }
      }
      return 0
    },
    /**
     * 按引擎查找升级包状态（Go 只有一份包，平台在行内比对）
     */
    findPackage(engine) {
      const packages = this.overview.packages || []
      return packages.find((item) => item.engine === engine) || null
    },
    /**
     * 行内「更新」不可用原因（空字符串表示可升级）
     *
     * <p>不可用即禁用（不做「点完再看插件端报错」）：缺少该类包、Go 平台不匹配、
     * 包与运行版本一致（插件端只会回「新包和正在运行的包一致」）、不能降级。</p>
     */
    nodeUpgradeTip(record) {
      if (!this.overview.packages.length) {
        // 旧服务端没有 packages：保持原有行为（由服务端探测引擎）
        return ''
      }
      if (!record.engine) {
        // 类型还没识别出来（节点未上报 / 离线）：不可用即禁用，不做「点完等报错」
        return this.$t('i18n_bcfec05b8f')
      }
      const pkg = this.findPackage(record.engine)
      const platformSuffix = record.engine === 'go' && record.platform ? ` (${record.platform})` : ''
      if (!pkg || pkg.missing) {
        return this.$t('i18n_c369b37de5') + platformSuffix
      }
      if (record.engine === 'go' && pkg.platform && record.platform && pkg.platform !== record.platform) {
        return this.$t('i18n_c369b37de5') + platformSuffix
      }
      // 以节点「实时运行版本」为准（缓存版本是上次推过去的包，不能代表运行中的包）
      const runningVersion = record.jpomVersion || record.version
      if (!runningVersion || !pkg.version) {
        return ''
      }
      if (this.compareVersion(pkg.version, runningVersion) < 0) {
        return this.$t('i18n_7f3130948c')
      }
      // 版本相同不在这里置灰：是否「完全一致的包」要同时比较打包时间，只有服务端能拿到运行包的打包时间，
      // 由服务端预检返回精确提示（避免打包时间不同却被前端误拦）
      return ''
    },
    /**
     * 下载某一类升级包（Java 升级包 / 指定平台的 Go 升级容器）
     */
    downloadPackageEvent(row) {
      this.downloadRemoteEvent(row)
    },
    updateNodeHandler(record) {
      this.tableSelections = [record.id]
      this.updateNode()
    },
    tableSelectionChange(selectedRowKeys) {
      this.tableSelections = selectedRowKeys
    },
    sendMsg(command, params, silent = false) {
      const socket = this.socket
      if (!socket || socket.readyState !== WebSocket.OPEN) {
        // 连接未建立/已断开时直接 send 会抛异常，用户点「更新」后没有任何反馈，这里显式提示并尝试重连
        if (!silent) {
          $notification.error({
            message: `${this.$t('i18n_7030ff6470')}: ${this.$t('i18n_226a6f9cdd')}`
          })
        }
        if (!socket || socket.readyState !== WebSocket.CONNECTING) {
          this.socket = null
          this.initWebsocket(this.list.map((item) => item.id))
        }
        return false
      }
      socket.send(
        JSON.stringify({
          command,
          params
        })
      )
      return true
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
      this.sendMsg('getAgentVersion', undefined, true)
    },
    getAgentVersionResult(data) {
      try {
        let newData = JSON.parse(data)
        this.agentVersion = newData.version
        this.agentTimeStamp = newData.timeStamp
        // 展示本地升级包的引擎/平台/包类型
        if (newData.engine) {
          this.temp = { ...this.temp, packageEngine: newData.engine, packagePlatform: newData.platform || '' }
        }
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
    updateNode(ids) {
      if (!this.agentVersion) {
        $notification.error({
          message: this.$t('i18n_41fdb0c862')
        })
        return
      }
      const targetIds = ids && ids.length ? ids : this.tableSelections
      if (!targetIds.length) {
        $notification.warning({
          message: this.$t('i18n_27f105b0c3')
        })
        return
      }
      const len = targetIds.length
      const html = `
        ${this.$t('i18n_4c28044efc')}
        <b style='color:red;font-size: 20px;'>
          ${len}
        </b>
        ${this.$t('i18n_667fa07b52')}
        <b style='color:red;font-size: 20px;'>
          ${this.agentVersion || '--'}
        </b>
        ${this.$t('i18n_16f7fa08db')}
        <ul style='color:red;'>
          <li>${this.$t('i18n_52d9520ee1')}: <b>${this.packageTypeLabel || '--'}</b></li>
          <li>${this.$t('i18n_e8505e27f4')}<b>${this.$t('i18n_ddf0c97bce')}</b></li>
          <li>${this.$t('i18n_a52a10123f')}</li>
          <li>${this.$t('i18n_1c040e6b87')}</li>
        </ul>
      `
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          if (
            this.sendMsg('updateNode', {
              ids: targetIds,
              protocol: this.temp.protocol
            })
          ) {
            this.tableSelections = []
          }
        }
      })
    },
    beforeUpload(file) {
      const html = `
        ${this.$t('i18n_835050418f')}
        <ul style='color:red;'>
          <li>${this.$t('i18n_527f7e18f1')}<b>${this.$t('i18n_ddf0c97bce')}</b></li>
          <li>${this.$t('i18n_a5daa9be44')}</li>
        </ul>
      `
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
                  $notification.success({
                    message: res.msg || this.$t('i18n_a7699ba731')
                  })
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
    // 下载远程最新文件（带 row 时下载指定类型的包；不带时兼容旧的单入口行为）
    downloadRemoteEvent(row) {
      // 混合部署时按「这一行缺哪类包」下载，不再由「列表里有没有 Go 节点」决定唯一一份包的类型
      let engine = 'java'
      let platform = ''
      if (row && row.engine) {
        engine = row.engine
        platform = row.platform || ''
      } else if (this.temp.packageEngine) {
        engine = this.temp.packageEngine
        platform = this.temp.packagePlatform || ''
      }
      if (engine === 'go' && !platform) {
        // Go 容器按平台区分：没有记录平台时使用第一个已上报平台的 Go 节点
        const goNode = (this.list || []).find((item) => item.engine === 'go' && item.platform)
        platform = (goNode && goNode.platform) || 'linux/amd64'
      }
      const engineText = engine === 'go' ? this.$t('i18n_11b8cdd4a6') : this.$t('i18n_ac0399d13b')
      const platformText = engine === 'go' ? platform || this.$t('i18n_2a20de5e5a') : '---'
      const packageTypeText = engine === 'go' ? this.$t('i18n_baee195ca8') : this.$t('i18n_6fb5d67e27')
      const downloadSource = this.$t('i18n_917381e4a5')
      const html = `
        ${this.$t('i18n_35b89dbc59')}
        <ul style='color:red;'>
          <li>${this.$t('i18n_6c1b19dde4')}: <b>${engineText}</b></li>
          <li>${this.$t('i18n_4981287fe5')}: <b>${platformText}</b></li>
          <li>${this.$t('i18n_52d9520ee1')}: <b>${packageTypeText}</b></li>
          ${engine === 'go' ? `<li>${this.$t('i18n_f24307b683')}</li>` : ''}
          <li style="display: ${this.temp.downloadSource ? 'revert' : 'none'};">${downloadSource}<b>${
            this.temp.downloadSource
          }</b></li>
          <li>${this.$t('i18n_bb316d9acd')}</li>
          <li>${this.$t('i18n_56bb769354')}<b>${this.$t('i18n_ddf0c97bce')}</b></li>
          <li>${this.$t('i18n_2c74d8485f')}</li>
          <li>${this.$t('i18n_a52a10123f')}</li>
        </ul>
      `

      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return downloadRemote({ engine, platform }).then((res) => {
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
