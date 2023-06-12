<template>
  <div>
    <template v-if="type === 'container'">
      <a-table :data-source="list" size="middle" :columns="columns" :pagination="false" bordered rowKey="id">
        <template #title>
          <a-space>
            <a-input v-model="listQuery['name']" @pressEnter="loadData" placeholder="名称" class="search-input-item" />
            <a-input
              v-model="listQuery['containerId']"
              @pressEnter="loadData"
              placeholder="容器id"
              class="search-input-item"
            />
            <a-input
              v-model="listQuery['imageId']"
              @keyup.enter="loadData"
              placeholder="镜像id"
              class="search-input-item"
            />
            <div>
              查看
              <a-switch checked-children="所有" un-checked-children="运行中" v-model="listQuery['showAll']" />
            </div>
            <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
            <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="autoUpdate" />
          </a-space>
        </template>

        <a-popover :title="`容器名称：${(text || []).join(',')}`" #names slot-scope="text, record">
          <template #content>
            <p>容器Id: {{ record.id }}</p>
            <p>镜像：{{ record.image }}</p>
            <p>镜像Id: {{ record.imageId }}</p>
          </template>

          <span>{{ (text || []).join(',') }}</span>
        </a-popover>

        <a-popover :title="`容器名标签`" #labels slot-scope="text, record">
          <template #content>
            <template v-if="record.labels">
              <p v-for="(value, key) in record.labels" :key="key">{{ key }}<a-icon type="arrow-right" />{{ value }}</p>
            </template>
          </template>
          <template v-if="record.labels && Object.keys(record.labels).length">
            <span>{{ (record.labels && Object.keys(record.labels).length) || 0 }} <a-icon type="tags" /></span>
          </template>
          <template v-else>-</template>
        </a-popover>
        <a-popover :title="`挂载`" #mounts slot-scope="text, record">
          <template #content>
            <template v-if="record.mounts">
              <div v-for="(item, index) in record.mounts" :key="index">
                <p>
                  名称：{{ item.name }} <a-tag>{{ item.rw ? '读写' : '读' }}</a-tag>
                </p>
                <p>路径：{{ item.source }}(宿主机) => {{ item.destination }}(容器)</p>
                <a-divider></a-divider>
              </div>
            </template>
          </template>
          <template v-if="record.mounts && Object.keys(record.mounts).length">
            <span>{{ (record.mounts && Object.keys(record.mounts).length) || 0 }} <a-icon type="api" /></span>
          </template>
          <template v-else>-</template>
        </a-popover>

        <a-tooltip #tooltip slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>

        <a-tooltip #showid slot-scope="text" placement="topLeft" :title="text">
          <span style="display: none"> {{ (array = text.split(':')) }}</span>
          <span>{{ array[array.length - 1].slice(0, 12) }}</span>
        </a-tooltip>

        <a-popover #ports slot-scope="text, record" placement="topLeft">
          <template #title>
            网络端口
            <ul>
              <li v-for="(item, index) in text || []" :key="index">
                {{ item.type + ' ' + (item.ip || '') + ':' + (item.publicPort || '') + ':' + item.privatePort }}
              </li>
            </ul>
          </template>
          <template #content>
            <template v-if="record.networkSettings">
              <template v-if="record.networkSettings.networks">
                <template v-if="record.networkSettings.networks.bridge">
                  桥接模式：
                  <p v-if="record.networkSettings.networks.bridge.ipAddress">
                    IP: <a-tag>{{ record.networkSettings.networks.bridge.ipAddress }}</a-tag>
                  </p>
                  <p v-if="record.networkSettings.networks.bridge.macAddress">
                    MAC: <a-tag>{{ record.networkSettings.networks.bridge.macAddress }}</a-tag>
                  </p>
                  <p v-if="record.networkSettings.networks.bridge.gateway">
                    网关: <a-tag>{{ record.networkSettings.networks.bridge.gateway }}</a-tag>
                  </p>
                  <p v-if="record.networkSettings.networks.bridge.networkID">
                    networkID: <a-tag>{{ record.networkSettings.networks.bridge.networkID }}</a-tag>
                  </p>
                  <p v-if="record.networkSettings.networks.bridge.endpointId">
                    endpointId: <a-tag>{{ record.networkSettings.networks.bridge.endpointId }}</a-tag>
                  </p>
                </template>
                <template v-if="record.networkSettings.networks.ingress">
                  <p v-if="record.networkSettings.networks.ingress.ipAddress">
                    IP: <a-tag>{{ record.networkSettings.networks.ingress.ipAddress }}</a-tag>
                  </p>
                  <p v-if="record.networkSettings.networks.ingress.macAddress">
                    MAC: <a-tag>{{ record.networkSettings.networks.ingress.macAddress }}</a-tag>
                  </p>
                  <p v-if="record.networkSettings.networks.ingress.gateway">
                    网关: <a-tag>{{ record.networkSettings.networks.ingress.gateway }}</a-tag>
                  </p>
                  <p v-if="record.networkSettings.networks.ingress.networkID">
                    networkID: <a-tag>{{ record.networkSettings.networks.ingress.networkID }}</a-tag>
                  </p>
                  <p v-if="record.networkSettings.networks.ingress.endpointId">
                    endpointId: <a-tag>{{ record.networkSettings.networks.ingress.endpointId }}</a-tag>
                  </p>
                </template>
              </template>
            </template>
          </template>
          <span>{{
            (text || [])
              .map((item) => {
                return item.type + ' ' + (item.publicPort || '') + ':' + item.privatePort
              })
              .join('/')
          }}</span>
        </a-popover>

        <template #state slot-scope="text, record">
          <a-tooltip :title="(record.status || '') + ' 点击查看日志'" @click="viewLog(record)">
            <a-switch :checked="text === 'running'" :disabled="true">
              <a-icon #checkedChildren type="check-circle" />
              <a-icon #unCheckedChildren type="warning" />
            </a-switch>
          </a-tooltip>
        </template>
        <template #operation slot-scope="text, record">
          <a-space>
            <template v-if="record.state === 'running'">
              <a-tooltip title="容器是运行中可以进入终端">
                <a-button
                  size="small"
                  type="link"
                  :disabled="record.state !== 'running'"
                  @click="handleTerminal(record)"
                  ><a-icon type="code"
                /></a-button>
              </a-tooltip>
              <a-tooltip title="停止">
                <a-button size="small" type="link" @click="doAction(record, 'stop')"><a-icon type="stop" /></a-button>
              </a-tooltip>
              <a-tooltip title="重启">
                <a-button size="small" type="link" @click="doAction(record, 'restart')"
                  ><a-icon type="reload"
                /></a-button>
              </a-tooltip>
            </template>
            <template v-else>
              <a-tooltip title="启动">
                <a-button size="small" type="link" @click="doAction(record, 'start')">
                  <a-icon type="play-circle"
                /></a-button>
              </a-tooltip>
              <a-tooltip title="停止">
                <a-button size="small" type="link" :disabled="true"><a-icon type="stop" /></a-button>
              </a-tooltip>
              <a-tooltip title="重启">
                <a-button size="small" type="link" :disabled="true"><a-icon type="reload" /></a-button>
              </a-tooltip>
            </template>

            <a-dropdown>
              <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
                <a-icon type="more" />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <atooltip title="编辑容器的一些基础参数">
                      <a-button
                        size="small"
                        type="link"
                        icon="edit"
                        :disabled="record.state !== 'running'"
                        @click="editContainer(record)"
                        >编辑</a-button
                      >
                    </atooltip>
                  </a-menu-item>
                  <a-menu-item>
                    <a-tooltip title="点击查看日志">
                      <a-button size="small" type="link" icon="message" @click="viewLog(record)">日志</a-button>
                    </a-tooltip>
                  </a-menu-item>
                  <a-menu-item>
                    <a-tooltip title="强制删除">
                      <a-button size="small" type="link" icon="delete" @click="doAction(record, 'remove')"
                        >删除</a-button
                      >
                    </a-tooltip>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </a-table>
    </template>
    <template v-else-if="type === 'compose'">
      <a-table
        :data-source="list"
        size="middle"
        :columns="parentColumns"
        :pagination="false"
        bordered
        :rowKey="
          (item, index) => {
            return index
          }
        "
      >
        <template #title>
          <a-space>
            <a-input v-model="listQuery['name']" @pressEnter="loadData" placeholder="名称" class="search-input-item" />
            <a-input
              v-model="listQuery['containerId']"
              @pressEnter="loadData"
              placeholder="容器id"
              class="search-input-item"
            />
            <a-input
              v-model="listQuery['imageId']"
              @keyup.enter="loadData"
              placeholder="镜像id"
              class="search-input-item"
            />
            <div>
              查看
              <a-switch checked-children="所有" un-checked-children="运行中" v-model="listQuery['showAll']" />
            </div>
            <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
            <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="autoUpdate" />
          </a-space>
        </template>
        <a-tooltip #tooltip slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <template #state slot-scope="text, record">
          <span style="display: none">
            {{
              (array = (record.child || []).map((item) => {
                return item.state
              }))
            }}
            {{
              (runningCount = array
                .map((item) => {
                  return item === 'running' ? 1 : 0
                })
                .reduce((prev, curr) => {
                  return prev + curr
                }, 0))
            }}</span
          >
          <span v-if="runningCount">Running({{ runningCount }}/{{ array.length }})</span>
          <span v-else>Exited</span>
        </template>

        <a-table
          #expandedRowRender
          slot-scope="record"
          :data-source="record.child"
          size="middle"
          :columns="columns"
          :pagination="false"
          bordered
          rowKey="id"
        >
          <a-popover :title="`容器名称：${(text || []).join(',')}`" #names slot-scope="text, record">
            <template #content>
              <p>容器Id: {{ record.id }}</p>
              <p>镜像：{{ record.image }}</p>
              <p>镜像Id: {{ record.imageId }}</p>
            </template>

            <span>{{ (text || []).join(',') }}</span>
          </a-popover>

          <a-popover :title="`容器名标签`" #labels slot-scope="text, record">
            <template #content>
              <template v-if="record.labels">
                <p v-for="(value, key) in record.labels" :key="key">
                  {{ key }}<a-icon type="arrow-right" />{{ value }}
                </p>
              </template>
            </template>
            <template v-if="record.labels && Object.keys(record.labels).length">
              <span>{{ (record.labels && Object.keys(record.labels).length) || 0 }} <a-icon type="tags" /></span>
            </template>
            <template v-else>-</template>
          </a-popover>
          <a-popover :title="`挂载`" #mounts slot-scope="text, record">
            <template #content>
              <template v-if="record.mounts">
                <div v-for="(item, index) in record.mounts" :key="index">
                  <p>
                    名称：{{ item.name }} <a-tag>{{ item.rw ? '读写' : '读' }}</a-tag>
                  </p>
                  <p>路径：{{ item.source }}(宿主机) => {{ item.destination }}(容器)</p>
                  <a-divider></a-divider>
                </div>
              </template>
            </template>
            <template v-if="record.mounts && Object.keys(record.mounts).length">
              <span>{{ (record.mounts && Object.keys(record.mounts).length) || 0 }} <a-icon type="api" /></span>
            </template>
            <template v-else>-</template>
          </a-popover>

          <a-tooltip #tooltip slot-scope="text" placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>

          <a-tooltip #showid slot-scope="text" placement="topLeft" :title="text">
            <span style="display: none"> {{ (array = text.split(':')) }}</span>
            <span>{{ array[array.length - 1].slice(0, 12) }}</span>
          </a-tooltip>

          <a-popover #ports slot-scope="text, record" placement="topLeft">
            <template #title>
              网络端口
              <ul>
                <li v-for="(item, index) in text || []" :key="index">
                  {{ item.type + ' ' + (item.ip || '') + ':' + (item.publicPort || '') + ':' + item.privatePort }}
                </li>
              </ul>
            </template>
            <template #content>
              <template v-if="record.networkSettings">
                <template v-if="record.networkSettings.networks">
                  <template v-if="record.networkSettings.networks.bridge">
                    桥接模式：
                    <p v-if="record.networkSettings.networks.bridge.ipAddress">
                      IP: <a-tag>{{ record.networkSettings.networks.bridge.ipAddress }}</a-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.bridge.macAddress">
                      MAC: <a-tag>{{ record.networkSettings.networks.bridge.macAddress }}</a-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.bridge.gateway">
                      网关: <a-tag>{{ record.networkSettings.networks.bridge.gateway }}</a-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.bridge.networkID">
                      networkID: <a-tag>{{ record.networkSettings.networks.bridge.networkID }}</a-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.bridge.endpointId">
                      endpointId: <a-tag>{{ record.networkSettings.networks.bridge.endpointId }}</a-tag>
                    </p>
                  </template>
                  <template v-if="record.networkSettings.networks.ingress">
                    <p v-if="record.networkSettings.networks.ingress.ipAddress">
                      IP: <a-tag>{{ record.networkSettings.networks.ingress.ipAddress }}</a-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.ingress.macAddress">
                      MAC: <a-tag>{{ record.networkSettings.networks.ingress.macAddress }}</a-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.ingress.gateway">
                      网关: <a-tag>{{ record.networkSettings.networks.ingress.gateway }}</a-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.ingress.networkID">
                      networkID: <a-tag>{{ record.networkSettings.networks.ingress.networkID }}</a-tag>
                    </p>
                    <p v-if="record.networkSettings.networks.ingress.endpointId">
                      endpointId: <a-tag>{{ record.networkSettings.networks.ingress.endpointId }}</a-tag>
                    </p>
                  </template>
                </template>
              </template>
            </template>
            <span>{{
              (text || [])
                .map((item) => {
                  return item.type + ' ' + (item.publicPort || '') + ':' + item.privatePort
                })
                .join('/')
            }}</span>
          </a-popover>

          <template #state slot-scope="text, record">
            <a-tooltip :title="(record.status || '') + ' 点击查看日志'" @click="viewLog(record)">
              <a-switch :checked="text === 'running'" :disabled="true">
                <a-icon #checkedChildren type="check-circle" />
                <a-icon #unCheckedChildren type="warning" />
              </a-switch>
            </a-tooltip>
          </template>
          <template #operation slot-scope="text, record">
            <a-space>
              <template v-if="record.state === 'running'">
                <a-tooltip title="容器是运行中可以进入终端">
                  <a-button size="small" type="link" @click="handleTerminal(record)"><a-icon type="code" /></a-button>
                </a-tooltip>
                <a-tooltip title="停止">
                  <a-button size="small" type="link" @click="doAction(record, 'stop')"><a-icon type="stop" /></a-button>
                </a-tooltip>
                <a-tooltip title="重启">
                  <a-button size="small" type="link" @click="doAction(record, 'restart')"
                    ><a-icon type="reload"
                  /></a-button>
                </a-tooltip>
              </template>
              <template v-else>
                <a-tooltip title="启动">
                  <a-button size="small" type="link" @click="doAction(record, 'start')">
                    <a-icon type="play-circle"
                  /></a-button>
                </a-tooltip>
                <a-tooltip title="停止">
                  <a-button size="small" type="link" :disabled="true"><a-icon type="stop" /></a-button>
                </a-tooltip>
                <a-tooltip title="重启">
                  <a-button size="small" type="link" :disabled="true"><a-icon type="reload" /></a-button>
                </a-tooltip>
              </template>

              <a-dropdown>
                <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
                  <a-icon type="more" />
                </a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item>
                      <atooltip title="编辑容器的一些基础参数">
                        <a-button
                          size="small"
                          type="link"
                          icon="edit"
                          :disabled="record.state !== 'running'"
                          @click="editContainer(record)"
                          >编辑</a-button
                        >
                      </atooltip>
                    </a-menu-item>
                    <a-menu-item>
                      <a-tooltip title="点击查看日志">
                        <a-button size="small" type="link" icon="message" @click="viewLog(record)">日志</a-button>
                      </a-tooltip>
                    </a-menu-item>
                    <a-menu-item>
                      <a-tooltip title="强制删除">
                        <a-button size="small" type="link" icon="delete" @click="doAction(record, 'remove')"
                          >删除</a-button
                        >
                      </a-tooltip>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </a-table>
      </a-table>
    </template>
    <!-- 日志 -->
    <a-modal
      destroyOnClose
      :width="'80vw'"
      v-model:visible="logVisible"
      title="执行日志"
      :footer="null"
      :maskClosable="false"
    >
      <log-view v-if="logVisible" :id="id" :machineDockerId="machineDockerId" :containerId="temp.id" />
    </a-modal>
    <!-- Terminal -->
    <a-modal
      v-model="terminalVisible"
      width="80vw"
      :bodyStyle="{
        padding: '0 10px',
        paddingTop: '10px',
        marginRight: '10px',
        height: `70vh`
      }"
      :title="`docker cli ${(temp.names || []).join(',')}`"
      :footer="null"
      :maskClosable="false"
    >
      <terminal v-if="terminalVisible" :id="id" :machineDockerId="machineDockerId" :containerId="temp.id" />
    </a-modal>
    <!-- 编辑容器配置 -->
    <a-modal
      destroyOnClose
      v-model="editVisible"
      width="60vw"
      title="配置容器"
      @ok="
        () => {
          this.$refs.editContainer.handleEditOk()
          this.editVisible = false
          this.loadData()
        }
      "
      :maskClosable="false"
    >
      <editContainer
        ref="editContainer"
        :id="id"
        :machineDockerId="machineDockerId"
        :urlPrefix="urlPrefix"
        :containerId="temp.id"
      ></editContainer>
    </a-modal>
  </div>
</template>
<script>
import { parseTime } from '@/utils/const'
import {
  dockerContainerList,
  dockerContainerRemove,
  dockerContainerRestart,
  dockerContainerStart,
  dockerContainerStop,
  dockerContainerListCompose
} from '@/api/docker-api'
import LogView from '@/pages/docker/log-view'
import Terminal from '@/pages/docker/terminal'
import editContainer from './editContainer.vue'

export default {
  name: 'container',
  components: {
    LogView,
    Terminal,
    editContainer
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String
    },
    machineDockerId: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      // container  or compose
      default: 'container'
    }
  },
  data() {
    return {
      list: [],
      loading: false,
      listQuery: {
        showAll: true
      },
      terminalVisible: false,
      logVisible: false,
      temp: {},

      columns: [
        {
          title: '序号',
          width: '80px',
          ellipsis: true,
          align: 'center',
          customRender: (text, record, index) => `${index + 1}`
        },
        { title: '名称', dataIndex: 'names', ellipsis: true, width: 150, scopedSlots: { customRender: 'names' } },
        { title: '容器ID', dataIndex: 'id', ellipsis: true, width: '130px', scopedSlots: { customRender: 'showid' } },
        {
          title: '镜像ID',
          dataIndex: 'imageId',
          ellipsis: true,
          width: '130px',
          scopedSlots: { customRender: 'showid' }
        },
        {
          title: '状态',
          dataIndex: 'state',
          ellipsis: true,
          align: 'center',
          width: '80px',
          scopedSlots: { customRender: 'state' }
        },
        {
          title: '状态描述',
          dataIndex: 'status',
          ellipsis: true,
          align: 'center',
          width: 100,
          scopedSlots: { customRender: 'tooltip' }
        },
        { title: '端口', dataIndex: 'ports', ellipsis: true, width: 150, scopedSlots: { customRender: 'ports' } },
        { title: '命令', dataIndex: 'command', ellipsis: true, width: 150, scopedSlots: { customRender: 'tooltip' } },
        { title: '标签', dataIndex: 'labels', ellipsis: true, width: '50px', scopedSlots: { customRender: 'labels' } },
        { title: '挂载', dataIndex: 'mounts', ellipsis: true, width: '50px', scopedSlots: { customRender: 'mounts' } },

        {
          title: '创建时间',
          dataIndex: 'created',

          ellipsis: true,
          sorter: (a, b) => Number(a.created) - new Number(b.created),
          sortDirections: ['descend', 'ascend'],
          defaultSortOrder: 'descend',
          customRender: (text) => parseTime(text),
          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          fixed: 'right',
          scopedSlots: { customRender: 'operation' },
          width: '160px'
        }
      ],
      parentColumns: [
        {
          title: '序号',
          width: '80px',
          ellipsis: true,
          align: 'center',
          customRender: (text, record, index) => `${index + 1}`
        },
        { title: '名称', width: 200, dataIndex: 'name', ellipsis: true, scopedSlots: { customRender: 'tooltip' } },
        { title: '状态', dataIndex: 'state', width: '150px', ellipsis: true, scopedSlots: { customRender: 'state' } },
        { title: '操作', width: '80px', ellipsis: true, scopedSlots: { customRender: 'pids' } }
      ],
      action: {
        remove: {
          msg: '您确定要删除当前容器吗？',
          api: dockerContainerRemove
        },
        stop: {
          msg: '您确定要停止当前容器吗？',
          api: dockerContainerStop
        },
        restart: {
          msg: '您确定要重启当前容器吗？',
          api: dockerContainerRestart
        },
        start: {
          msg: '您确定要启动当前容器吗？',
          api: dockerContainerStart
        }
      },
      editVisible: false,

      countdownTime: Date.now()
    }
  },
  beforeDestroy() {},
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId
    }
  },
  mounted() {
    this.autoUpdate()
  },
  methods: {
    autoUpdate() {
      this.loadData()
    },
    // 加载数据
    loadData() {
      if (!this.visible) {
        return
      }
      this.loading = true
      //this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.id = this.reqDataId
      ;(this.type === 'container'
        ? dockerContainerList(this.urlPrefix, this.listQuery)
        : dockerContainerListCompose(this.urlPrefix, this.listQuery)
      ).then((res) => {
        if (res.code === 200) {
          this.list = this.sortPort(res.data || []).map((item) => {
            let child = item.child
            if (child) {
              child = this.sortPort(child)
            }
            return { ...item, child: child }
          })
        }
        this.loading = false
        this.countdownTime = Date.now() + 5 * 1000
      })
    },
    sortPort(list) {
      return list.map((item) => {
        let ports = item.ports
        if (ports) {
          try {
            ports = ports.sort(
              (a, b) =>
                a.privatePort - b.privatePort ||
                (a.type || '').toLowerCase().localeCompare((b.type || '').toLowerCase())
            )
          } catch (e) {
            console.error(e)
          }
        }

        return { ...item, ports: ports }
      })
    },
    doAction(record, actionKey) {
      const action = this.action[actionKey]
      if (!action) {
        return
      }
      $confirm({
        title: '系统提示',
        content: action.msg,
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 组装参数
          const params = {
            id: this.reqDataId,
            containerId: record.id
          }
          action.api(this.urlPrefix, params).then((res) => {
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
    viewLog(record) {
      this.logVisible = true
      this.temp = record
    },
    // 进入终端
    handleTerminal(record) {
      this.temp = Object.assign({}, record)
      this.terminalVisible = true
    },
    editContainer(record) {
      this.temp = Object.assign({}, record)
      this.editVisible = true
      // console.log(this.temp);
    }
  }
}
</script>
<style scoped>
/deep/ .ant-statistic div {
  display: inline-block;
}

/deep/ .ant-statistic-content-value,
/deep/ .ant-statistic-content {
  font-size: 16px;
}
</style>
