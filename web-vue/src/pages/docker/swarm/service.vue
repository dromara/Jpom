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
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['serviceId']"
            placeholder="id"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['serviceName']"
            :placeholder="$tl('c.name')"
            class="search-input-item"
            @press-enter="loadData"
          />

          <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          <a-button type="primary" @click="handleAdd">{{ $tl('p.create') }}</a-button>
          <a-statistic-countdown
            format="s"
            :title="$tl('p.refreshCountdown')"
            :value="countdownTime"
            @finish="loadData"
          >
            <template #suffix>
              <div style="font-size: 12px">{{ $tl('p.seconds') }}</div>
            </template>
          </a-statistic-countdown>
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
            :title="`${$tl('p.nodeStatus')}${text} ${$tl('p.nodeAvailability')}${record.spec ? record.spec.availability || '' : ''}`"
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
            :title="`${$tl('p.role')}${text} ${record.managerStatus && record.managerStatus.reachability === 'REACHABLE' ? $tl('p.managementStatus') + record.managerStatus.reachability : ''}`"
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
          <a-tooltip
            placement="topLeft"
            :title="`${$tl('p.modifiedTime')}${text} ${$tl('p.creationTime')}${record.createdAt}`"
          >
            {{ text }}
          </a-tooltip>
        </template>

        <template v-else-if="column.replicas">
          <a-tooltip placement="topLeft" :title="`${$tl('p.viewRunningTasks')},${$tl('p.viewAllTasks')}`">
            <a-tag @click="handleTask(record, 'RUNNING')">{{ text }}</a-tag>

            <ReadOutlined @click="handleTask(record)" />
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('p.modify') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDel(record)">{{ $tl('p.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑节点 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$tl('p.editService')"
      width="70vw"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$tl('c.serviceName')" name="name">
          <template #help
            ><span v-if="!temp.serviceId">{{ $tl('c.immutableAfterCreated') }}</span></template
          >
          <a-input
            v-model:value="temp.name"
            :disabled="temp.serviceId ? true : false"
            :placeholder="$tl('c.serviceName')"
          />
        </a-form-item>
        <a-form-item :label="$tl('c.runMode')" name="mode">
          <template #help
            ><span v-if="!temp.serviceId">{{ $tl('c.immutableAfterCreated') }}</span></template
          >
          <a-radio-group v-model:value="temp.mode" name="mode" :disabled="temp.serviceId ? true : false">
            <a-radio value="REPLICATED">{{ $tl('p.replicas') }}</a-radio>
            <a-radio value="GLOBAL">{{ $tl('p.standalone') }} </a-radio>
          </a-radio-group>
          <a-form-item-rest>
            <template v-if="temp.mode === 'REPLICATED'">
              {{ $tl('c.replicaCount') }}:
              <a-input-number v-model:value="temp.replicas" :min="1" />
            </template>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item :label="$tl('c.imageName')" name="image">
          <a-input v-model:value="temp.image" :placeholder="$tl('c.imageName')" />
        </a-form-item>
        <a-form-item label="hostname" name="hostname">
          <a-input v-model:value="temp.hostname" :placeholder="$tl('p.hostname')" />
        </a-form-item>
        <a-form-item :label="$tl('p.moreConfigurations')" name="">
          <a-form-item-rest>
            <a-tabs>
              <a-tab-pane key="port" :tab="$tl('c.port')">
                <a-form-item :label="$tl('c.resolutionMode')" name="endpointResolutionMode">
                  <a-radio-group
                    v-model:value="temp.endpointResolutionMode"
                    name="endpointResolutionMode"
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
                            <a-radio-group v-model:value="item.publishMode" name="publishMode">
                              <a-radio value="ingress" :disabled="temp.endpointResolutionMode === 'DNSRR'">{{
                                $tl('p.routing')
                              }}</a-radio>
                              <a-radio value="host">{{ $tl('p.host') }}</a-radio>
                            </a-radio-group>
                          </a-col>
                          <a-col :span="7">
                            <a-input
                              v-model:value="item.publishedPort"
                              :addon-before="$tl('c.port')"
                              :placeholder="$tl('c.port')"
                            >
                            </a-input>
                          </a-col>
                          <a-col :span="8" :offset="1">
                            <a-input
                              v-model:value="item.targetPort"
                              :addon-before="$tl('c.container')"
                              placeholder="{{$tl('c.container')}}端口"
                            >
                              <template #addonAfter>
                                <a-select v-model:value="item.protocol" :placeholder="$tl('p.portProtocol')">
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

              <a-tab-pane key="volumes" :tab="$tl('p.mountedVolumes')">
                <a-form-item>
                  <a-row v-for="(item, index) in temp.volumes" :key="index">
                    <a-col :span="21">
                      <a-input-group>
                        <a-row>
                          <a-col :span="7">
                            <a-radio-group v-model:value="item.type" name="publishMode">
                              <a-radio value="VOLUME">VOLUME</a-radio>
                              <a-radio value="BIND">BIND</a-radio>
                            </a-radio-group>
                          </a-col>
                          <a-col :span="7">
                            <a-input
                              v-model:value="item.source"
                              :addon-before="$tl('p.hostMachine')"
                              placeholder="{{$tl('p.hostMachine')}}机目录"
                            />
                          </a-col>
                          <a-col :span="8" :offset="1">
                            <a-input
                              v-model:value="item.target"
                              :addon-before="$tl('c.container')"
                              placeholder="{{$tl('c.container')}}目录"
                            />
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

              <a-tab-pane key="args" :tab="$tl('p.parameters')">
                <a-form-item>
                  <a-row v-for="(item, index) in temp.args" :key="index">
                    <a-col :span="20">
                      <a-input
                        v-model:value="item.value"
                        :addon-before="$tl('p.parameterValue')"
                        :placeholder="$tl('p.fillRunningParameters')"
                      />
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
              <a-tab-pane key="command" :tab="$tl('p.command')">
                <a-form-item>
                  <a-row v-for="(item, index) in temp.commands" :key="index">
                    <a-col :span="20">
                      <a-input
                        v-model:value="item.value"
                        :addon-before="$tl('p.commandValue')"
                        :placeholder="$tl('p.fillRunningCommand')"
                      />
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
              <a-tab-pane key="env" :tab="$tl('p.environmentVariables')">
                <a-form-item>
                  <a-row v-for="(item, index) in temp.envs" :key="index">
                    <a-col :span="10">
                      <a-input
                        v-model:value="item.name"
                        :addon-before="$tl('c.name')"
                        placeholder="变量{{$tl('c.name')}}"
                      />
                    </a-col>
                    <a-col :span="10" :offset="1">
                      <a-input
                        v-model:value="item.value"
                        :addon-before="$tl('c.variableValue')"
                        :placeholder="$tl('c.variableValue')"
                      />
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
              <a-tab-pane v-if="temp.update" :tab="$tl('p.upgradeStrategy')">
                <a-form-item :label="$tl('c.parallelism')" name="parallelism">
                  <a-input-number
                    v-model:value="temp.update.parallelism"
                    style="width: 80%"
                    :min="0"
                    :placeholder="$tl('c.parallelismDetail')"
                  />
                </a-form-item>
                <a-form-item :label="$tl('c.delay')" name="delay">
                  <template #help>
                    <span style="padding-left: 20%">{{ $tl('c.delayUnit') }}</span>
                  </template>
                  <a-input-number
                    v-model:value="temp.update.delay"
                    style="width: 80%"
                    :min="1"
                    :placeholder="$tl('p.upgradeInterval')"
                  />
                </a-form-item>
                <a-form-item :label="$tl('c.failureRate')" name="maxFailureRatio">
                  <a-input-number
                    v-model:value="temp.update.maxFailureRatio"
                    style="width: 80%"
                    :min="0"
                    placeholder="{{$tl('c.failureRate')}}{{$tl('c.failureRateDetail')}}"
                  />
                </a-form-item>
                <a-form-item :label="$tl('c.failureStrategy')" name="failureAction">
                  <a-radio-group v-model:value="temp.update.failureAction" name="failureAction">
                    <a-radio value="PAUSE">{{ $tl('c.pause') }}</a-radio>
                    <a-radio value="CONTINUE">{{ $tl('c.resume') }}</a-radio>
                    <a-radio value="ROLLBACK">{{ $tl('c.rollback') }}</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item :label="$tl('c.executionOrder')" name="order">
                  <a-radio-group v-model:value="temp.update.order" name="order">
                    <a-radio value="STOP_FIRST">{{ $tl('c.stopFirst') }}</a-radio>
                    <a-radio value="START_FIRST">{{ $tl('c.startFirst') }}</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item :label="$tl('c.monitoring')" name="monitor">
                  <a-input-number
                    v-model:value="temp.update.monitor"
                    style="width: 80%"
                    :min="1"
                    :placeholder="$tl('c.successTimeAfterUpdated')"
                  />
                </a-form-item>
              </a-tab-pane>
              <a-tab-pane v-if="temp.rollback" :tab="$tl('p.rollbackStrategy')">
                <a-form-item :label="$tl('c.parallelism')" name="parallelism">
                  <a-input-number
                    v-model:value="temp.rollback.parallelism"
                    style="width: 80%"
                    :min="0"
                    :placeholder="$tl('c.parallelismDetail')"
                  />
                </a-form-item>
                <a-form-item :label="$tl('c.delay')" name="delay">
                  <template #help>
                    <span style="padding-left: 20%">{{ $tl('c.delayUnit') }}</span>
                  </template>
                  <a-input-number
                    v-model:value="temp.rollback.delay"
                    style="width: 80%"
                    :min="1"
                    :placeholder="$tl('p.rollbackInterval')"
                  />
                </a-form-item>
                <a-form-item :label="$tl('c.failureRate')" name="maxFailureRatio">
                  <a-input-number
                    v-model:value="temp.rollback.maxFailureRatio"
                    style="width: 80%"
                    :min="0"
                    placeholder="{{$tl('c.failureRate')}}{{$tl('c.failureRateDetail')}}"
                  />
                </a-form-item>
                <a-form-item :label="$tl('c.failureStrategy')" name="failureAction">
                  <a-radio-group v-model:value="temp.rollback.failureAction" name="failureAction">
                    <a-radio value="PAUSE">{{ $tl('c.pause') }}</a-radio>
                    <a-radio value="CONTINUE">{{ $tl('c.resume') }}</a-radio>
                    <a-radio value="ROLLBACK">{{ $tl('c.rollback') }}</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item :label="$tl('c.executionOrder')" name="order">
                  <a-radio-group v-model:value="temp.rollback.order" name="order">
                    <a-radio value="STOP_FIRST">{{ $tl('c.stopFirst') }}</a-radio>
                    <a-radio value="START_FIRST">{{ $tl('c.startFirst') }}</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item :label="$tl('c.monitoring')" name="monitor">
                  <a-input-number
                    v-model:value="temp.rollback.monitor"
                    style="width: 80%"
                    :min="1"
                    :placeholder="$tl('c.successTimeAfterUpdated')"
                  />
                </a-form-item>
              </a-tab-pane>
              <a-tab-pane v-if="temp.resources" :tab="$tl('p.resources')">
                <a-form-item :label="$tl('p.preemptionResources')">
                  <a-row>
                    <a-col :span="8">
                      <a-input
                        v-model:value="temp.resources.reservations.nanoCPUs"
                        addon-before="CPUs"
                        :placeholder="$tl('c.nanoCPUs')"
                      />
                    </a-col>
                    <a-col :span="8" :offset="1">
                      <a-input
                        v-model:value="temp.resources.reservations.memoryBytes"
                        addon-before="memory"
                        :placeholder="$tl('c.memory')"
                      />
                    </a-col>
                  </a-row>
                </a-form-item>
                <a-form-item :label="$tl('p.limitResources')">
                  <a-row>
                    <a-col :span="8">
                      <a-input
                        v-model:value="temp.resources.limits.nanoCPUs"
                        addon-before="CPUs"
                        :placeholder="$tl('c.nanoCPUs')"
                      />
                    </a-col>
                    <a-col :span="8" :offset="1">
                      <a-input
                        v-model:value="temp.resources.limits.memoryBytes"
                        addon-before="memory"
                        :placeholder="$tl('c.memory')"
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
      v-model:open="taskVisible"
      destroy-on-close
      :title="$tl('p.viewTasks')"
      width="80vw"
      :footer="null"
      :mask-closable="false"
    >
      <swarm-task
        v-if="taskVisible"
        :id="id"
        :visible="taskVisible"
        :task-state="temp.state"
        :service-id="temp.id"
        :url-prefix="urlPrefix"
      />
    </a-modal>
    <!-- 查看日志 -->

    <pull-log
      v-if="logVisible > 0"
      :id="id"
      :visible="logVisible != 0"
      :data-id="temp.id"
      type="service"
      :url-prefix="urlPrefix"
      @close="
        () => {
          logVisible = 0
        }
      "
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
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String,
      default: ''
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
        name: [{ required: true, message: this.$tl('p.serviceNameRequired'), trigger: 'blur' }],
        mode: [{ required: true, message: this.$tl('p.operationModeRequired'), trigger: 'blur' }],
        image: [{ required: true, message: this.$tl('p.imageNameRequired'), trigger: 'blur' }]
      },
      columns: [
        {
          title: this.$tl('p.serialNumber'),
          width: 80,
          ellipsis: true,
          align: 'center',
          customRender: ({ index }) => `${index + 1}`
        },
        {
          title: this.$tl('p.serviceId'),
          dataIndex: 'id',
          ellipsis: true
        },
        {
          title: this.$tl('c.name'),
          dataIndex: ['spec', 'name'],
          ellipsis: true,

          tooltip: true
        },
        {
          title: this.$tl('c.runMode'),
          dataIndex: ['spec', 'mode', 'mode'],
          ellipsis: true,
          width: 120,
          tooltip: true
        },
        // { title: "网络模式", dataIndex: "spec.endpointSpec.mode", ellipsis: true, width: 120, },
        {
          title: this.$tl('c.replicaCount'),
          dataIndex: ['spec', 'mode', 'replicated', 'replicas'],
          align: 'center',
          width: 90,
          ellipsis: true,
          replicas: true
        },
        {
          title: this.$tl('c.resolutionMode'),
          dataIndex: ['spec', 'endpointSpec', 'mode'],
          ellipsis: true,
          width: 100,
          tooltip: true
        },

        {
          title: this.$tl('p.modificationTime'),
          dataIndex: 'updatedAt',

          ellipsis: true,

          width: '170px'
        },
        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',
          width: '120px'
        }
      ],
      countdownTime: Date.now()
    }
  },
  computed: {},

  beforeUnmount() {},
  mounted() {
    this.loadData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.docker.swarm.service.${key}`, ...args)
    },
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
          message: this.$tl('p.incompleteInformationCannotEdit')
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
      $confirm({
        title: this.$tl('p.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.reallyDeleteThisService'),
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
        onOk: () => {
          return dockerSwarmServicesDel(this.urlPrefix, {
            serviceId: record.id,
            id: this.id
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
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
