<template>
  <div>
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      bordered
      :pagination="false"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            v-model:value="listQuery['serviceId']"
            @pressEnter="loadData"
            placeholder="id"
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['serviceName']"
            @pressEnter="loadData"
            placeholder="名称"
            class="search-input-item"
          />

          <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          <a-button type="primary" @click="handleAdd">创建</a-button>
          <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="loadData" />
        </a-space>
      </template>

      <template #bodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'id'">
          <a-tooltip placement="topLeft" :title="text" @click="handleLog(record)">
            <span>{{ text }}</span>
            <EyeOutlined />
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip
            placement="topLeft"
            :title="`节点状态：${text} 节点可用性：${record.spec ? record.spec.availability || '' : ''}`"
          >
            <a-tag :color="(record.spec && record.spec.availability) === 'ACTIVE' ? 'green' : 'red'">
              {{ text }}
              <template v-if="record.spec">{{ record.spec.availability }}</template>
            </a-tag>
          </a-tooltip>
        </template>
        <!-- 角色显示 -->
        <template v-else-if="column.dataIndex === 'role'">
          <a-tooltip
            placement="topLeft"
            :title="`角色：${text} ${
              record.managerStatus && record.managerStatus.reachability === 'REACHABLE'
                ? '管理状态：' + record.managerStatus.reachability
                : ''
            }`"
          >
            <a-tag
              :color="`${record.managerStatus && record.managerStatus.reachability === 'REACHABLE' ? 'green' : ''}`"
            >
              {{ text }}
            </a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'address'">
          <a-tooltip placement="topLeft" :title="text">
            <CloudServerOutlined v-if="item.managerStatus && item.managerStatus.leader" />

            {{ text }}
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'os'">
          <a-tooltip placement="topLeft" :title="text">
            <span>
              <a-tag
                >{{ text }}-{{
                  item.description && item.description.platform && item.description.platform.architecture
                }}</a-tag
              >
            </span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'updatedAt'">
          <a-tooltip placement="topLeft" :title="`修改时间：${text} 创建时间：${record.createdAt}`">
            {{ text }}
          </a-tooltip>
        </template>

        <template v-else-if="column.replicas">
          <a-tooltip placement="topLeft" :title="`点击数字查看运行中的任务,点击图标查看关联的所有任务`">
            <a-tag @click="handleTask(record, 'RUNNING')">{{ text }}</a-tag>

            <ReadOutlined @click="handleTask(record)" />
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">修改</a-button>
            <a-button size="small" type="primary" danger @click="handleDel(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑节点 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="editVisible"
      title="编辑服务"
      width="70vw"
      @ok="handleEditOk"
      :maskClosable="false"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="服务名称" name="name">
          <a-input v-model:value="temp.name" :disabled="temp.serviceId ? true : false" placeholder="服务名称" />
        </a-form-item>
        <a-form-item label="运行模式" name="mode">
          <a-radio-group name="mode" v-model:value="temp.mode" :disabled="temp.serviceId ? true : false">
            <a-radio value="REPLICATED">副本</a-radio>
            <a-radio value="GLOBAL">独立 </a-radio>
          </a-radio-group>
          <a-form-item-rest>
            <template v-if="temp.mode === 'REPLICATED'">
              副本数:
              <a-input-number v-model:value="temp.replicas" :min="1" />
            </template>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item label="镜像名称" name="image">
          <a-input v-model:value="temp.image" placeholder="镜像名称" />
        </a-form-item>
        <a-form-item label="hostname" name="hostname">
          <a-input v-model:value="temp.hostname" placeholder="主机名 hostname" />
        </a-form-item>
        <a-form-item label="更多配置" name="">
          <a-form-item-rest>
            <a-tabs>
              <a-tab-pane key="port" tab="端口">
                <a-form-item label="解析模式" name="endpointResolutionMode">
                  <a-radio-group
                    name="endpointResolutionMode"
                    v-model:value="temp.endpointResolutionMode"
                    @change="
                      () => {
                        temp.exposedPorts = temp.exposedPorts.map((item) => {
                          if (temp.endpointResolutionMode === 'DNSRR') {
                            item.publishMode = 'host'
                          }
                          return item
                        })
                      }
                    "
                  >
                    <a-radio value="VIP">VIP</a-radio>
                    <a-radio value="DNSRR">DNSRR </a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item>
                  <a-row v-for="(item, index) in temp.exposedPorts" :key="index">
                    <a-col :span="21">
                      <a-input-group>
                        <a-row>
                          <a-col :span="7">
                            <a-radio-group name="publishMode" v-model:value="item.publishMode">
                              <a-radio value="ingress" :disabled="temp.endpointResolutionMode === 'DNSRR'"
                                >路由</a-radio
                              >
                              <a-radio value="host">主机</a-radio>
                            </a-radio-group>
                          </a-col>
                          <a-col :span="7">
                            <a-input addon-before="端口" placeholder="端口" v-model:value="item.publishedPort">
                            </a-input>
                          </a-col>
                          <a-col :span="8" :offset="1">
                            <a-input addon-before="容器" v-model:value="item.targetPort" placeholder="容器端口">
                              <template v-slot:addonAfter>
                                <a-select v-model:value="item.protocol" placeholder="端口协议">
                                  <a-select-option value="TCP">TCP</a-select-option>
                                  <a-select-option value="UDP">UDP</a-select-option>
                                  <a-select-option value="SCTP">SCTP</a-select-option>
                                </a-select>
                              </template>
                            </a-input>
                          </a-col>
                        </a-row>
                      </a-input-group>
                    </a-col>
                    <a-col :span="2" :offset="1">
                      <a-space>
                        <MinusCircleOutlined
                          v-if="temp.exposedPorts && temp.exposedPorts.length > 1"
                          @click="
                            () => {
                              temp.exposedPorts.splice(index, 1)
                            }
                          "
                        />

                        <PlusSquareOutlined
                          @click="
                            () => {
                              temp.exposedPorts.push({
                                protocol: 'TCP',
                                publishMode: 'host'
                              })
                            }
                          "
                        />
                      </a-space>
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-tab-pane>

              <a-tab-pane key="volumes" tab="挂载卷">
                <a-form-item>
                  <a-row v-for="(item, index) in temp.volumes" :key="index">
                    <a-col :span="21">
                      <a-input-group>
                        <a-row>
                          <a-col :span="7">
                            <a-radio-group name="publishMode" v-model:value="item.type">
                              <a-radio value="VOLUME">VOLUME</a-radio>
                              <a-radio value="BIND">BIND</a-radio>
                            </a-radio-group>
                          </a-col>
                          <a-col :span="7">
                            <a-input addon-before="宿主" v-model:value="item.source" placeholder="宿主机目录" />
                          </a-col>
                          <a-col :span="8" :offset="1">
                            <a-input addon-before="容器" v-model:value="item.target" placeholder="容器目录" />
                          </a-col>
                        </a-row>
                      </a-input-group>
                    </a-col>
                    <a-col :span="2" :offset="1">
                      <a-space>
                        <MinusCircleOutlined
                          v-if="temp.volumes && temp.volumes.length > 1"
                          @click="
                            () => {
                              temp.volumes.splice(index, 1)
                            }
                          "
                        />

                        <PlusSquareOutlined
                          @click="
                            () => {
                              temp.volumes.push({})
                            }
                          "
                        />
                      </a-space>
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-tab-pane>

              <a-tab-pane key="args" tab="参数">
                <a-form-item>
                  <a-row v-for="(item, index) in temp.args" :key="index">
                    <a-col :span="20">
                      <a-input addon-before="参数值" v-model:value="item.value" placeholder="填写运行参数" />
                    </a-col>

                    <a-col :span="2" :offset="1">
                      <a-space>
                        <MinusCircleOutlined
                          v-if="temp.args && temp.args.length > 1"
                          @click="
                            () => {
                              temp.args.splice(index, 1)
                            }
                          "
                        />

                        <PlusSquareOutlined
                          @click="
                            () => {
                              temp.args.push({})
                            }
                          "
                        />
                      </a-space>
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-tab-pane>
              <a-tab-pane key="command" tab="命令">
                <a-form-item>
                  <a-row v-for="(item, index) in temp.commands" :key="index">
                    <a-col :span="20">
                      <a-input addon-before="命令值" v-model:value="item.value" placeholder="填写运行命令" />
                    </a-col>

                    <a-col :span="2" :offset="1">
                      <a-space>
                        <MinusCircleOutlined
                          v-if="temp.commands && temp.commands.length > 1"
                          @click="
                            () => {
                              temp.commands.splice(index, 1)
                            }
                          "
                        />
                        <PlusSquareOutlined
                          @click="
                            () => {
                              temp.commands.push({})
                            }
                          "
                        />
                      </a-space>
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-tab-pane>
              <a-tab-pane key="env" tab="环境变量">
                <a-form-item>
                  <a-row v-for="(item, index) in temp.envs" :key="index">
                    <a-col :span="10">
                      <a-input addon-before="名称" v-model:value="item.name" placeholder="变量名称" />
                    </a-col>
                    <a-col :span="10" :offset="1">
                      <a-input addon-before="变量值" v-model:value="item.value" placeholder="变量值" />
                    </a-col>
                    <a-col :span="2" :offset="1">
                      <a-space>
                        <MinusCircleOutlined
                          v-if="temp.envs && temp.envs.length > 1"
                          @click="
                            () => {
                              temp.envs.splice(index, 1)
                            }
                          "
                        />

                        <PlusSquareOutlined
                          @click="
                            () => {
                              temp.envs.push({})
                            }
                          "
                        />
                      </a-space>
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-tab-pane>
              <a-tab-pane tab="升级策略" v-if="temp.update">
                <a-form-item label="并行度" name="parallelism">
                  <a-input-number
                    style="width: 80%"
                    :min="0"
                    v-model:value="temp.update.parallelism"
                    placeholder="并行度,同一时间升级的容器数量"
                  />
                </a-form-item>
                <a-form-item label="延迟" name="delay">
                  <template #help>
                    <span style="padding-left: 20%">单位 ns 秒</span>
                  </template>
                  <a-input-number
                    style="width: 80%"
                    :min="1"
                    v-model:value="temp.update.delay"
                    placeholder="延迟,容器升级间隔时间"
                  />
                </a-form-item>
                <a-form-item label="失败率" name="maxFailureRatio">
                  <a-input-number
                    style="width: 80%"
                    :min="0"
                    v-model:value="temp.update.maxFailureRatio"
                    placeholder="失败率,更新期间允许的失败率"
                  />
                </a-form-item>
                <a-form-item label="失败策略" name="failureAction">
                  <a-radio-group name="failureAction" v-model:value="temp.update.failureAction">
                    <a-radio value="PAUSE">暂停</a-radio>
                    <a-radio value="CONTINUE">继续</a-radio>
                    <a-radio value="ROLLBACK">回滚</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item label="执行顺序" name="order">
                  <a-radio-group name="order" v-model:value="temp.update.order">
                    <a-radio value="STOP_FIRST">先停止</a-radio>
                    <a-radio value="START_FIRST">先启动</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item label="监控" name="monitor">
                  <a-input-number
                    style="width: 80%"
                    :min="1"
                    v-model:value="temp.update.monitor"
                    placeholder="更新完成后确实成功的时间"
                  />
                </a-form-item>
              </a-tab-pane>
              <a-tab-pane tab="回滚策略" v-if="temp.rollback">
                <a-form-item label="并行度" name="parallelism">
                  <a-input-number
                    style="width: 80%"
                    :min="0"
                    v-model:value="temp.rollback.parallelism"
                    placeholder="并行度,同一时间升级的容器数量"
                  />
                </a-form-item>
                <a-form-item label="延迟" name="delay">
                  <template #help>
                    <span style="padding-left: 20%">单位 ns 秒</span>
                  </template>
                  <a-input-number
                    style="width: 80%"
                    :min="1"
                    v-model:value="temp.rollback.delay"
                    placeholder="延迟,容器回滚间隔时间"
                  />
                </a-form-item>
                <a-form-item label="失败率" name="maxFailureRatio">
                  <a-input-number
                    style="width: 80%"
                    :min="0"
                    v-model:value="temp.rollback.maxFailureRatio"
                    placeholder="失败率,更新期间允许的失败率"
                  />
                </a-form-item>
                <a-form-item label="失败策略" name="failureAction">
                  <a-radio-group name="failureAction" v-model:value="temp.rollback.failureAction">
                    <a-radio value="PAUSE">暂停</a-radio>
                    <a-radio value="CONTINUE">继续</a-radio>
                    <a-radio value="ROLLBACK">回滚</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item label="执行顺序" name="order">
                  <a-radio-group name="order" v-model:value="temp.rollback.order">
                    <a-radio value="STOP_FIRST">先停止</a-radio>
                    <a-radio value="START_FIRST">先启动</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item label="监控" name="monitor">
                  <a-input-number
                    style="width: 80%"
                    :min="1"
                    v-model:value="temp.rollback.monitor"
                    placeholder="更新完成后确实成功的时间"
                  />
                </a-form-item>
              </a-tab-pane>
              <a-tab-pane tab="资源" v-if="temp.resources">
                <a-form-item label="预占资源">
                  <a-row>
                    <a-col :span="8">
                      <a-input
                        addon-before="CPUs"
                        v-model:value="temp.resources.reservations.nanoCPUs"
                        placeholder="nanoCPUs 最小 1000000"
                      />
                    </a-col>
                    <a-col :span="8" :offset="1">
                      <a-input
                        addon-before="memory"
                        v-model:value="temp.resources.reservations.memoryBytes"
                        placeholder="memory 最小 4M"
                      />
                    </a-col>
                  </a-row>
                </a-form-item>
                <a-form-item label="限制资源">
                  <a-row>
                    <a-col :span="8">
                      <a-input
                        addon-before="CPUs"
                        v-model:value="temp.resources.limits.nanoCPUs"
                        placeholder="nanoCPUs 最小 1000000"
                      />
                    </a-col>
                    <a-col :span="8" :offset="1">
                      <a-input
                        addon-before="memory"
                        v-model:value="temp.resources.limits.memoryBytes"
                        placeholder="memory 最小 4M"
                      />
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-tab-pane>
            </a-tabs>
          </a-form-item-rest>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 查看任务 -->
    <a-modal
      destroyOnClose
      v-model:open="taskVisible"
      title="查看任务"
      width="80vw"
      :footer="null"
      :maskClosable="false"
    >
      <swarm-task
        v-if="taskVisible"
        :visible="taskVisible"
        :taskState="this.temp.state"
        :id="this.id"
        :serviceId="this.temp.id"
        :urlPrefix="this.urlPrefix"
      />
    </a-modal>
    <!-- 查看日志 -->

    <pull-log
      v-if="logVisible > 0"
      :visible="logVisible != 0"
      @close="
        () => {
          logVisible = 0
        }
      "
      :id="this.id"
      :dataId="this.temp.id"
      type="service"
      :urlPrefix="this.urlPrefix"
    />
  </div>
</template>

<script>
import { dockerSwarmServicesDel, dockerSwarmServicesEdit, dockerSwarmServicesList } from '@/api/docker-swarm'
import SwarmTask from './task'
import PullLog from './pull-log'
import { renderSize } from '@/utils/const'

export default {
  components: { SwarmTask, PullLog },
  props: {
    id: {
      type: String
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String
    }
  },
  data() {
    return {
      loading: false,
      listQuery: {},
      list: [],
      temp: {
        update: {},
        rollback: {}
      },
      editVisible: false,
      initSwarmVisible: false,
      taskVisible: false,
      logVisible: 0,
      confirmLoading: false,
      rules: {
        name: [{ required: true, message: '服务名称必填', trigger: 'blur' }],
        mode: [{ required: true, message: '运行模式必填', trigger: 'blur' }],
        image: [{ required: true, message: '镜像名称必填', trigger: 'blur' }]
      },
      columns: [
        {
          title: '序号',
          width: 80,
          ellipsis: true,
          align: 'center',
          customRender: ({ text, record, index }) => `${index + 1}`
        },
        {
          title: '服务Id',
          dataIndex: 'id',
          ellipsis: true
        },
        {
          title: '名称',
          dataIndex: ['spec', 'name'],
          ellipsis: true,

          tooltip: true
        },
        {
          title: '运行模式',
          dataIndex: ['spec', 'mode', 'mode'],
          ellipsis: true,
          width: 120,
          tooltip: true
        },
        // { title: "网络模式", dataIndex: "spec.endpointSpec.mode", ellipsis: true, width: 120, },
        {
          title: '副本数',
          dataIndex: ['spec', 'mode', 'replicated', 'replicas'],
          align: 'center',
          width: 90,
          ellipsis: true,
          replicas: true
        },
        {
          title: '解析模式',
          dataIndex: ['spec', 'endpointSpec', 'mode'],
          ellipsis: true,
          width: 100,
          tooltip: true
        },

        {
          title: '修改时间',
          dataIndex: 'updatedAt',

          ellipsis: true,

          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',
          width: '120px'
        }
      ],
      countdownTime: Date.now()
    }
  },

  beforeUnmount() {},
  computed: {},
  mounted() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData() {
      if (!this.visible) {
        return
      }
      this.loading = true
      this.listQuery.id = this.id
      dockerSwarmServicesList(this.urlPrefix, this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data
        }
        this.loading = false
        this.countdownTime = Date.now() + 5 * 1000
      })
    },
    //  任务
    handleTask(record, state) {
      this.taskVisible = true
      this.temp = record
      this.temp = { ...this.temp, state: state || '' }
    },
    // 日志
    handleLog(record) {
      this.logVisible = new Date() * Math.random()
      this.temp = record
    },
    //  创建服务
    handleAdd() {
      this.editVisible = true
      this.temp = {
        mode: 'REPLICATED',
        replicas: 1,
        endpointResolutionMode: 'VIP',
        exposedPorts: [
          {
            publishMode: 'host',
            protocol: 'TCP'
          }
        ],
        volumes: [
          {
            type: 'VOLUME'
          }
        ],
        args: [{}],
        commands: [{}],
        envs: [{}],
        update: {},
        rollback: {},
        resources: {
          limits: {},
          reservations: {}
        }
      }
    },
    // 编辑
    handleEdit(record) {
      const spec = record.spec
      if (!spec) {
        $notification.error({
          message: '信息不完整不能编辑'
        })
        return
      }
      this.editVisible = true
      let image = spec.taskTemplate?.containerSpec?.image

      if (image && image.includes('@')) {
        image = image.split('@')[0]
      }
      this.temp = {
        serviceId: record.id,
        name: spec.name,
        hostname: spec.taskTemplate?.containerSpec?.hostname,
        mode: spec.mode?.mode,
        replicas: spec.mode?.replicated?.replicas,
        image: image,
        version: record.version?.index,
        endpointResolutionMode: spec.endpointSpec?.mode,
        exposedPorts: [
          {
            publishMode: 'host',
            protocol: 'TCP'
          }
        ],
        volumes: [{ type: 'VOLUME' }],
        args: [{}],
        commands: [{}],
        envs: [{}],
        update: {},
        rollback: {},
        resources: {}
      }

      const args = spec.taskTemplate?.containerSpec?.args
      const mounts = spec.taskTemplate?.containerSpec?.mounts
      const command = spec.taskTemplate?.containerSpec?.command
      const env = spec.taskTemplate?.containerSpec?.env
      const limits = spec.taskTemplate?.resources?.limits
      const reservations = spec.taskTemplate?.resources?.reservations
      const ports = spec.endpointSpec?.ports
      const updateConfig = spec.updateConfig
      const rollbackConfig = spec.rollbackConfig
      if (args) {
        this.temp = {
          ...this.temp,
          args: args.map((item) => {
            return {
              value: item
            }
          })
        }
      }
      if (command) {
        this.temp = {
          ...this.temp,
          commands: command.map((item) => {
            return {
              value: item
            }
          })
        }
      }
      if (env) {
        this.temp = {
          ...this.temp,
          envs: env.map((item) => {
            return {
              name: item.split('=')[0],
              value: item.split('=')[1]
            }
          })
        }
      }
      if (ports) {
        this.temp = { ...this.temp, exposedPorts: ports }
      }
      if (mounts) {
        this.temp = { ...this.temp, volumes: mounts }
      }
      if (updateConfig) {
        this.temp = { ...this.temp, update: updateConfig }
      }
      if (rollbackConfig) {
        this.temp = { ...this.temp, rollback: rollbackConfig }
      }
      let resources = { limits: {}, reservations: {} }
      if (limits) {
        limits.memoryBytes = renderSize(limits.memoryBytes)
        resources = { ...resources, limits: limits }
      }
      if (reservations) {
        reservations.memoryBytes = renderSize(reservations.memoryBytes)
        resources = { ...resources, reservations: reservations }
      }
      this.temp = { ...this.temp, resources: resources }
    },
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        this.temp.id = this.id
        const temp = Object.assign({}, this.temp)
        temp.volumes = (this.temp.volumes || []).filter((item) => {
          return item.source && item.target
        })
        // 处理端口
        temp.exposedPorts = (this.temp.exposedPorts || []).filter((item) => {
          return item.publishedPort && item.targetPort
        })
        this.confirmLoading = true
        dockerSwarmServicesEdit(this.urlPrefix, temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.editVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除
    handleDel(record) {
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除此服务么？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 组装参数
            const params = {
              serviceId: record.id,
              id: that.id
            }
            dockerSwarmServicesDel(that.urlPrefix, params)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadData()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    }
  }
}
</script>

<style scoped>
:deep(.ant-statistic div) {
  display: inline-block;
}
:deep(.ant-statistic-content-value, .ant-statistic-content) {
  font-size: 16px;
}
</style>
