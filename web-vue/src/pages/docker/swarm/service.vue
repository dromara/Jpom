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
            :placeholder="$t('i18n_d7ec2d3fea')"
            class="search-input-item"
            @press-enter="loadData"
          />

          <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          <a-button type="primary" @click="handleAdd">{{ $t('i18n_d9ac9228e8') }}</a-button>
          <a-statistic-countdown format="s" :title="$t('i18n_0f8403d07e')" :value="countdownTime" @finish="loadData">
            <template #suffix>
              <div style="font-size: 12px">{{ $t('i18n_ee6ce96abb') }}</div>
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
            :title="`${$t('i18n_9b3e947cc9')}${text} ${$t('i18n_fb91527ce5')}${
              record.spec ? record.spec.availability || '' : ''
            }`"
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
            :title="`${$t('i18n_20f32e1979')}${text} ${
              record.managerStatus && record.managerStatus.reachability === 'REACHABLE'
                ? $t('i18n_88c5680d0d') + record.managerStatus.reachability
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
          <a-tooltip
            placement="topLeft"
            :title="`${$t('i18n_bf94b97d1a')}${text} ${$t('i18n_312f45014a')}${record.createdAt}`"
          >
            {{ text }}
          </a-tooltip>
        </template>

        <template v-else-if="column.replicas">
          <a-tooltip placement="topLeft" :title="`${$t('i18n_ce07501354')},${$t('i18n_c0e498a259')}`">
            <a-tag @click="handleTask(record, 'RUNNING')">{{ text }}</a-tag>

            <ReadOutlined @click="handleTask(record)" />
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_8347a927c0') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDel(record)">{{
              $t('i18n_2f4aaddde3')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑节点 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_cc51f34aa4')"
      width="70vw"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('i18n_8f3747c057')" name="name">
          <template #help
            ><span v-if="!temp.serviceId">{{ $t('i18n_9ee9d48699') }}</span></template
          >
          <a-input
            v-model:value="temp.name"
            :disabled="temp.serviceId ? true : false"
            :placeholder="$t('i18n_8f3747c057')"
          />
        </a-form-item>
        <a-form-item :label="$t('i18n_44c4aaa1d9')" name="mode">
          <template #help
            ><span v-if="!temp.serviceId">{{ $t('i18n_9ee9d48699') }}</span></template
          >
          <a-radio-group v-model:value="temp.mode" name="mode" :disabled="temp.serviceId ? true : false">
            <a-radio value="REPLICATED">{{ $t('i18n_0428b36ab1') }}</a-radio>
            <a-radio value="GLOBAL">{{ $t('i18n_0c1de8295a') }} </a-radio>
          </a-radio-group>
          <a-form-item-rest>
            <template v-if="temp.mode === 'REPLICATED'">
              {{ $t('i18n_532495b65b') }}:
              <a-input-number v-model:value="temp.replicas" :min="1" />
            </template>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item :label="$t('i18n_bbf2775521')" name="image">
          <a-input v-model:value="temp.image" :placeholder="$t('i18n_bbf2775521')" />
        </a-form-item>
        <a-form-item label="hostname" name="hostname">
          <a-input v-model:value="temp.hostname" :placeholder="$t('i18n_f9361945f3')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_38aa9dc2a0')" name="">
          <a-form-item-rest>
            <a-tabs>
              <a-tab-pane key="port" :tab="$t('i18n_c76cfefe72')">
                <a-form-item :label="$t('i18n_d9435aa802')" name="endpointResolutionMode">
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
                                $t('i18n_75fc7de737')
                              }}</a-radio>
                              <a-radio value="host">{{ $t('i18n_652273694e') }}</a-radio>
                            </a-radio-group>
                          </a-col>
                          <a-col :span="7">
                            <a-input
                              v-model:value="item.publishedPort"
                              :addon-before="$t('i18n_c76cfefe72')"
                              :placeholder="$t('i18n_c76cfefe72')"
                            >
                            </a-input>
                          </a-col>
                          <a-col :span="8" :offset="1">
                            <a-input
                              v-model:value="item.targetPort"
                              :addon-before="$t('i18n_22c799040a')"
                              :placeholder="$t('i18n_31691a647c', { slot1: $t('i18n_22c799040a') })"
                            >
                              <template #addonAfter>
                                <a-select v-model:value="item.protocol" :placeholder="$t('i18n_0739b9551d')">
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

              <a-tab-pane key="volumes" :tab="$t('i18n_640374b7ae')">
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
                              :addon-before="$t('i18n_ad4b4a5b3b')"
                              :placeholder="$t('i18n_ec537c957a', { slot1: $t('i18n_ad4b4a5b3b') })"
                            />
                          </a-col>
                          <a-col :span="8" :offset="1">
                            <a-input
                              v-model:value="item.target"
                              :addon-before="$t('i18n_22c799040a')"
                              :placeholder="$t('i18n_368ffad051', { slot1: $t('i18n_22c799040a') })"
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

              <a-tab-pane key="args" :tab="$t('i18n_3d0a2df9ec')">
                <a-form-item>
                  <a-row v-for="(item, index) in temp.args" :key="index">
                    <a-col :span="20">
                      <a-input
                        v-model:value="item.value"
                        :addon-before="$t('i18n_bfed4943c5')"
                        :placeholder="$t('i18n_d65d977f1d')"
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
              <a-tab-pane key="command" :tab="$t('i18n_ddf7d2a5ce')">
                <a-form-item>
                  <a-row v-for="(item, index) in temp.commands" :key="index">
                    <a-col :span="20">
                      <a-input
                        v-model:value="item.value"
                        :addon-before="$t('i18n_579a6d0d92')"
                        :placeholder="$t('i18n_2a6a516f9d')"
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
              <a-tab-pane key="env" :tab="$t('i18n_3867e350eb')">
                <a-form-item>
                  <a-row v-for="(item, index) in temp.envs" :key="index">
                    <a-col :span="10">
                      <a-input
                        v-model:value="item.name"
                        :addon-before="$t('i18n_d7ec2d3fea')"
                        :placeholder="$t('i18n_7cb8d163bb')"
                      />
                    </a-col>
                    <a-col :span="10" :offset="1">
                      <a-input
                        v-model:value="item.value"
                        :addon-before="$t('i18n_9a2ee7044f')"
                        :placeholder="$t('i18n_9a2ee7044f')"
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
              <a-tab-pane v-if="temp.update" :tab="$t('i18n_a84a45b352')">
                <a-form-item :label="$t('i18n_07a828310b')" name="parallelism">
                  <a-input-number
                    v-model:value="temp.update.parallelism"
                    style="width: 80%"
                    :min="0"
                    :placeholder="$t('i18n_31eb055c9c')"
                  />
                </a-form-item>
                <a-form-item :label="$t('i18n_db732ecb48')" name="delay">
                  <template #help>
                    <span style="padding-left: 20%">{{ $t('i18n_e2adcc679a') }}</span>
                  </template>
                  <a-input-number
                    v-model:value="temp.update.delay"
                    style="width: 80%"
                    :min="1"
                    :placeholder="$t('i18n_85ec12ccd3')"
                  />
                </a-form-item>
                <a-form-item :label="$t('i18n_b3fe677b5f')" name="maxFailureRatio">
                  <a-input-number
                    v-model:value="temp.update.maxFailureRatio"
                    style="width: 80%"
                    :min="0"
                    :placeholder="`${$t('i18n_b3fe677b5f')}${$t('i18n_c7c4e4632f')}`"
                  />
                </a-form-item>
                <a-form-item :label="$t('i18n_fa2f7a8927')" name="failureAction">
                  <a-radio-group v-model:value="temp.update.failureAction" name="failureAction">
                    <a-radio value="PAUSE">{{ $t('i18n_8d63ef388e') }}</a-radio>
                    <a-radio value="CONTINUE">{{ $t('i18n_27ca568be2') }}</a-radio>
                    <a-radio value="ROLLBACK">{{ $t('i18n_d00b485b26') }}</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item :label="$t('i18n_d5c68a926e')" name="order">
                  <a-radio-group v-model:value="temp.update.order" name="order">
                    <a-radio value="STOP_FIRST">{{ $t('i18n_0647b5fc26') }}</a-radio>
                    <a-radio value="START_FIRST">{{ $t('i18n_42fd64c157') }}</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item :label="$t('i18n_9aff624153')" name="monitor">
                  <a-input-number
                    v-model:value="temp.update.monitor"
                    style="width: 80%"
                    :min="1"
                    :placeholder="$t('i18n_f6d6ab219d')"
                  />
                </a-form-item>
              </a-tab-pane>
              <a-tab-pane v-if="temp.rollback" :tab="$t('i18n_ad780debbc')">
                <a-form-item :label="$t('i18n_07a828310b')" name="parallelism">
                  <a-input-number
                    v-model:value="temp.rollback.parallelism"
                    style="width: 80%"
                    :min="0"
                    :placeholder="$t('i18n_31eb055c9c')"
                  />
                </a-form-item>
                <a-form-item :label="$t('i18n_db732ecb48')" name="delay">
                  <template #help>
                    <span style="padding-left: 20%">{{ $t('i18n_e2adcc679a') }}</span>
                  </template>
                  <a-input-number
                    v-model:value="temp.rollback.delay"
                    style="width: 80%"
                    :min="1"
                    :placeholder="$t('i18n_6a66d4cdf3')"
                  />
                </a-form-item>
                <a-form-item :label="$t('i18n_b3fe677b5f')" name="maxFailureRatio">
                  <a-input-number
                    v-model:value="temp.rollback.maxFailureRatio"
                    style="width: 80%"
                    :min="0"
                    :placeholder="`${$t('i18n_b3fe677b5f')}${$t('i18n_c7c4e4632f')}`"
                  />
                </a-form-item>
                <a-form-item :label="$t('i18n_fa2f7a8927')" name="failureAction">
                  <a-radio-group v-model:value="temp.rollback.failureAction" name="failureAction">
                    <a-radio value="PAUSE">{{ $t('i18n_8d63ef388e') }}</a-radio>
                    <a-radio value="CONTINUE">{{ $t('i18n_27ca568be2') }}</a-radio>
                    <a-radio value="ROLLBACK">{{ $t('i18n_d00b485b26') }}</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item :label="$t('i18n_d5c68a926e')" name="order">
                  <a-radio-group v-model:value="temp.rollback.order" name="order">
                    <a-radio value="STOP_FIRST">{{ $t('i18n_0647b5fc26') }}</a-radio>
                    <a-radio value="START_FIRST">{{ $t('i18n_42fd64c157') }}</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item :label="$t('i18n_9aff624153')" name="monitor">
                  <a-input-number
                    v-model:value="temp.rollback.monitor"
                    style="width: 80%"
                    :min="1"
                    :placeholder="$t('i18n_f6d6ab219d')"
                  />
                </a-form-item>
              </a-tab-pane>
              <a-tab-pane v-if="temp.resources" :tab="$t('i18n_eee83a9211')">
                <a-form-item :label="$t('i18n_3711cbf638')">
                  <a-row>
                    <a-col :span="8">
                      <a-input
                        v-model:value="temp.resources.reservations.nanoCPUs"
                        addon-before="CPUs"
                        :placeholder="$t('i18n_9e6b699597')"
                      />
                    </a-col>
                    <a-col :span="8" :offset="1">
                      <a-input
                        v-model:value="temp.resources.reservations.memoryBytes"
                        addon-before="memory"
                        :placeholder="$t('i18n_18eb76c8a0')"
                      />
                    </a-col>
                  </a-row>
                </a-form-item>
                <a-form-item :label="$t('i18n_87db69bd44')">
                  <a-row>
                    <a-col :span="8">
                      <a-input
                        v-model:value="temp.resources.limits.nanoCPUs"
                        addon-before="CPUs"
                        :placeholder="$t('i18n_9e6b699597')"
                      />
                    </a-col>
                    <a-col :span="8" :offset="1">
                      <a-input
                        v-model:value="temp.resources.limits.memoryBytes"
                        addon-before="memory"
                        :placeholder="$t('i18n_18eb76c8a0')"
                      />
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-tab-pane>
            </a-tabs>
          </a-form-item-rest>
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 查看任务 -->
    <CustomModal
      v-if="taskVisible"
      v-model:open="taskVisible"
      destroy-on-close
      :title="$t('i18n_13f931c5d9')"
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
    </CustomModal>
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
        name: [{ required: true, message: this.$t('i18n_4e7e04b15d'), trigger: 'blur' }],
        mode: [{ required: true, message: this.$t('i18n_922b76febd'), trigger: 'blur' }],
        image: [{ required: true, message: this.$t('i18n_b9af769752'), trigger: 'blur' }]
      },
      columns: [
        {
          title: this.$t('i18n_faaadc447b'),
          width: 80,
          ellipsis: true,
          align: 'center',
          customRender: ({ index }) => `${index + 1}`
        },
        {
          title: this.$t('i18n_a75b96584d'),
          dataIndex: 'id',
          ellipsis: true
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: ['spec', 'name'],
          ellipsis: true,

          tooltip: true
        },
        {
          title: this.$t('i18n_44c4aaa1d9'),
          dataIndex: ['spec', 'mode', 'mode'],
          ellipsis: true,
          width: 120,
          tooltip: true
        },
        // { title: "网络模式", dataIndex: "spec.endpointSpec.mode", ellipsis: true, width: 120, },
        {
          title: this.$t('i18n_532495b65b'),
          dataIndex: ['spec', 'mode', 'replicated', 'replicas'],
          align: 'center',
          width: 90,
          ellipsis: true,
          replicas: true
        },
        {
          title: this.$t('i18n_d9435aa802'),
          dataIndex: ['spec', 'endpointSpec', 'mode'],
          ellipsis: true,
          width: 100,
          tooltip: true
        },

        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'updatedAt',

          ellipsis: true,

          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
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
          message: this.$t('i18n_534115e981')
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_a4266aea79'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
