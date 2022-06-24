<template>
  <div>
    <a-table :data-source="list" size="middle" :columns="columns" bordered :pagination="false" :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['serviceId']" @pressEnter="loadData" placeholder="id" class="search-input-item" />
          <a-input v-model="listQuery['serviceName']" @pressEnter="loadData" placeholder="名称" class="search-input-item" />

          <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          <a-button type="primary" @click="handleAdd">创建</a-button>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="id" slot-scope="text, item" placement="topLeft" :title="text" @click="handleLog(item)">
        <span>{{ text }}</span>
      </a-tooltip>

      <a-tooltip slot="status" slot-scope="text, item" placement="topLeft" :title="`节点状态：${text} 节点可用性：${item.spec ? item.spec.availability || '' : ''}`">
        <a-tag :color="(item.spec && item.spec.availability) === 'ACTIVE' ? 'green' : 'red'">
          {{ text }}
          <template v-if="item.spec">{{ item.spec.availability }}</template>
        </a-tag>
      </a-tooltip>
      <!-- 角色显示 -->
      <a-tooltip
        slot="role"
        slot-scope="text, item"
        placement="topLeft"
        :title="`角色：${text} ${item.managerStatus && item.managerStatus.reachability === 'REACHABLE' ? '管理状态：' + item.managerStatus.reachability : ''}`"
      >
        <a-tag :color="`${item.managerStatus && item.managerStatus.reachability === 'REACHABLE' ? 'green' : ''}`">
          {{ text }}
        </a-tag>
      </a-tooltip>
      <a-tooltip slot="address" slot-scope="text, item" placement="topLeft" :title="text">
        <a-icon v-if="item.managerStatus && item.managerStatus.leader" type="cloud-server" />
        {{ text }}
      </a-tooltip>

      <a-tooltip slot="os" slot-scope="text, item" placement="topLeft" :title="text">
        <span>
          <a-tag>{{ text }}-{{ item.description && item.description.platform && item.description.platform.architecture }}</a-tag>
        </span>
      </a-tooltip>
      <a-tooltip slot="updatedAt" slot-scope="text, item" placement="topLeft" :title="`修改时间：${text} 创建时间：${item.createdAt}`">
        <span>
          <a-tag>{{ text }}</a-tag>
        </span>
      </a-tooltip>

      <a-tooltip slot="replicas" slot-scope="text, record" placement="topLeft" :title="`点击数字查看运行中的任务,点击图标查看关联的所有任务`">
        <a-tag @click="handleTask(record, 'RUNNING')">{{ text }}</a-tag>

        <a-icon type="read" @click="handleTask(record)" />
      </a-tooltip>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">修改</a-button>
          <a-button size="small" type="danger" @click="handleDel(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑节点 -->
    <a-modal v-model="editVisible" title="编辑服务" width="70vw" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="服务名称" prop="name">
          <a-input v-model="temp.name" :disabled="temp.serviceId ? true : false" placeholder="服务名称" />
        </a-form-model-item>
        <a-form-model-item label="运行模式" prop="mode">
          <a-radio-group name="mode" v-model="temp.mode" :disabled="temp.serviceId ? true : false">
            <a-radio value="REPLICATED">副本</a-radio>
            <a-radio value="GLOBAL">独立 </a-radio>
          </a-radio-group>
          <template v-if="temp.mode === 'REPLICATED'">
            副本数:
            <a-input-number v-model="temp.replicas" :min="1" />
          </template>
        </a-form-model-item>
        <a-form-model-item label="镜像名称" prop="image">
          <a-input v-model="temp.image" placeholder="镜像名称" />
        </a-form-model-item>
        <a-form-model-item label="更多配置" prop="">
          <a-tabs>
            <a-tab-pane key="port" tab="端口">
              <a-form-model-item label="解析模式" prop="endpointResolutionMode">
                <a-radio-group
                  name="endpointResolutionMode"
                  v-model="temp.endpointResolutionMode"
                  @change="
                    () => {
                      temp.exposedPorts = temp.exposedPorts.map((item) => {
                        if (temp.endpointResolutionMode === 'DNSRR') {
                          item.publishMode = 'host';
                        }
                        return item;
                      });
                    }
                  "
                >
                  <a-radio value="VIP">VIP</a-radio>
                  <a-radio value="DNSRR">DNSRR </a-radio>
                </a-radio-group>
              </a-form-model-item>
              <a-form-model-item>
                <a-row v-for="(item, index) in temp.exposedPorts" :key="index">
                  <a-col :span="21">
                    <a-input-group>
                      <a-row>
                        <a-col :span="7">
                          <a-radio-group name="publishMode" v-model="item.publishMode">
                            <a-radio value="ingress" :disabled="temp.endpointResolutionMode === 'DNSRR'">路由</a-radio>
                            <a-radio value="host">主机</a-radio>
                          </a-radio-group>
                        </a-col>
                        <a-col :span="7">
                          <a-input addon-before="端口" placeholder="端口" v-model="item.publishedPort"> </a-input>
                        </a-col>
                        <a-col :span="8" :offset="1">
                          <a-input addon-before="容器" v-model="item.targetPort" placeholder="容器端口">
                            <a-select
                              :getPopupContainer="
                                (triggerNode) => {
                                  return triggerNode.parentNode || document.body;
                                }
                              "
                              slot="addonAfter"
                              v-model="item.protocol"
                              placeholder="端口协议"
                            >
                              <a-select-option value="TCP">TCP</a-select-option>
                              <a-select-option value="UDP">UDP</a-select-option>
                              <a-select-option value="SCTP">SCTP</a-select-option>
                            </a-select>
                          </a-input>
                        </a-col>
                      </a-row>
                    </a-input-group>
                  </a-col>
                  <a-col :span="2" :offset="1">
                    <a-space>
                      <a-icon
                        type="minus-circle"
                        v-if="temp.exposedPorts && temp.exposedPorts.length > 1"
                        @click="
                          () => {
                            temp.exposedPorts.splice(index, 1);
                          }
                        "
                      />

                      <a-icon
                        type="plus-square"
                        @click="
                          () => {
                            temp.exposedPorts.push({
                              protocol: 'TCP',
                              publishMode: 'host',
                            });
                          }
                        "
                      />
                    </a-space>
                  </a-col>
                </a-row>
              </a-form-model-item>
            </a-tab-pane>

            <a-tab-pane key="volumes" tab="挂载卷">
              <a-form-model-item>
                <a-row v-for="(item, index) in temp.volumes" :key="index">
                  <a-col :span="21">
                    <a-input-group>
                      <a-row>
                        <a-col :span="7">
                          <a-radio-group name="publishMode" v-model="item.type">
                            <a-radio value="VOLUME">VOLUME</a-radio>
                            <a-radio value="BIND">BIND</a-radio>
                          </a-radio-group>
                        </a-col>
                        <a-col :span="7">
                          <a-input addon-before="宿主" v-model="item.source" placeholder="宿主机目录" />
                        </a-col>
                        <a-col :span="8" :offset="1">
                          <a-input addon-before="容器" v-model="item.target" placeholder="容器目录" />
                        </a-col>
                      </a-row>
                    </a-input-group>
                  </a-col>
                  <a-col :span="2" :offset="1">
                    <a-space>
                      <a-icon
                        type="minus-circle"
                        v-if="temp.volumes && temp.volumes.length > 1"
                        @click="
                          () => {
                            temp.volumes.splice(index, 1);
                          }
                        "
                      />

                      <a-icon
                        type="plus-square"
                        @click="
                          () => {
                            temp.volumes.push({});
                          }
                        "
                      />
                    </a-space>
                  </a-col>
                </a-row>
              </a-form-model-item>
            </a-tab-pane>

            <a-tab-pane key="args" tab="参数">
              <a-form-model-item>
                <a-row v-for="(item, index) in temp.args" :key="index">
                  <a-col :span="20">
                    <a-input addon-before="参数值" v-model="item.value" placeholder="填写运行参数" />
                  </a-col>

                  <a-col :span="2" :offset="1">
                    <a-space>
                      <a-icon
                        type="minus-circle"
                        v-if="temp.args && temp.args.length > 1"
                        @click="
                          () => {
                            temp.args.splice(index, 1);
                          }
                        "
                      />

                      <a-icon
                        type="plus-square"
                        @click="
                          () => {
                            temp.args.push({});
                          }
                        "
                      />
                    </a-space>
                  </a-col>
                </a-row>
              </a-form-model-item>
            </a-tab-pane>
            <a-tab-pane key="command" tab="命令">
              <a-form-model-item>
                <a-row v-for="(item, index) in temp.commands" :key="index">
                  <a-col :span="20">
                    <a-input addon-before="命令值" v-model="item.value" placeholder="填写运行命令" />
                  </a-col>

                  <a-col :span="2" :offset="1">
                    <a-space>
                      <a-icon
                        type="minus-circle"
                        v-if="temp.commands && temp.commands.length > 1"
                        @click="
                          () => {
                            temp.commands.splice(index, 1);
                          }
                        "
                      />
                      <a-icon
                        type="plus-square"
                        @click="
                          () => {
                            temp.commands.push({});
                          }
                        "
                      />
                    </a-space>
                  </a-col>
                </a-row>
              </a-form-model-item>
            </a-tab-pane>
            <a-tab-pane key="env" tab="环境变量">
              <a-form-model-item>
                <a-row v-for="(item, index) in temp.envs" :key="index">
                  <a-col :span="10">
                    <a-input addon-before="名称" v-model="item.name" placeholder="变量名称" />
                  </a-col>
                  <a-col :span="10" :offset="1">
                    <a-input addon-before="变量值" v-model="item.value" placeholder="变量值" />
                  </a-col>
                  <a-col :span="2" :offset="1">
                    <a-space>
                      <a-icon
                        type="minus-circle"
                        v-if="temp.envs && temp.envs.length > 1"
                        @click="
                          () => {
                            temp.envs.splice(index, 1);
                          }
                        "
                      />

                      <a-icon
                        type="plus-square"
                        @click="
                          () => {
                            temp.envs.push({});
                          }
                        "
                      />
                    </a-space>
                  </a-col>
                </a-row>
              </a-form-model-item>
            </a-tab-pane>
            <a-tab-pane key="update" tab="升级策略" v-if="temp.update">
              <a-form-model-item label="并行度" prop="parallelism">
                <a-input-number style="width: 80%" :min="0" v-model="temp.update.parallelism" placeholder="并行度,同一时间升级的容器数量" />
              </a-form-model-item>
              <a-form-model-item label="延迟" prop="delay">
                <a-input-number style="width: 80%" :min="1" v-model="temp.update.delay" placeholder="延迟,容器升级间隔时间" />
              </a-form-model-item>
              <a-form-model-item label="失败率" prop="maxFailureRatio">
                <a-input-number style="width: 80%" :min="0" v-model="temp.update.maxFailureRatio" placeholder="失败率,更新期间允许的失败率" />
              </a-form-model-item>
              <a-form-model-item label="失败策略" prop="failureAction">
                <a-radio-group name="failureAction" v-model="temp.update.failureAction">
                  <a-radio value="PAUSE">暂停</a-radio>
                  <a-radio value="CONTINUE">继续</a-radio>
                  <a-radio value="ROLLBACK">回滚</a-radio>
                </a-radio-group>
              </a-form-model-item>
              <a-form-model-item label="执行顺序" prop="order">
                <a-radio-group name="order" v-model="temp.update.order">
                  <a-radio value="STOP_FIRST">先停止</a-radio>
                  <a-radio value="START_FIRST">先启动</a-radio>
                </a-radio-group>
              </a-form-model-item>
              <a-form-model-item label="监控" prop="monitor">
                <a-input-number style="width: 80%" :min="1" v-model="temp.update.monitor" placeholder="更新完成后确实成功的时间" />
              </a-form-model-item>
            </a-tab-pane>
            <a-tab-pane key="rollback" tab="回滚策略" v-if="temp.rollback">
              <a-form-model-item label="并行度" prop="parallelism">
                <a-input-number style="width: 80%" :min="0" v-model="temp.rollback.parallelism" placeholder="并行度,同一时间升级的容器数量" />
              </a-form-model-item>
              <a-form-model-item label="延迟" prop="delay">
                <a-input-number style="width: 80%" :min="1" v-model="temp.rollback.delay" placeholder="延迟" />
              </a-form-model-item>
              <a-form-model-item label="失败率" prop="maxFailureRatio">
                <a-input-number style="width: 80%" :min="0" v-model="temp.rollback.maxFailureRatio" placeholder="失败率,更新期间允许的失败率" />
              </a-form-model-item>
              <a-form-model-item label="失败策略" prop="failureAction">
                <a-radio-group name="failureAction" v-model="temp.rollback.failureAction">
                  <a-radio value="PAUSE">暂停</a-radio>
                  <a-radio value="CONTINUE">继续</a-radio>
                  <a-radio value="ROLLBACK">回滚</a-radio>
                </a-radio-group>
              </a-form-model-item>
              <a-form-model-item label="执行顺序" prop="order">
                <a-radio-group name="order" v-model="temp.rollback.order">
                  <a-radio value="STOP_FIRST">先停止</a-radio>
                  <a-radio value="START_FIRST">先启动</a-radio>
                </a-radio-group>
              </a-form-model-item>
              <a-form-model-item label="监控" prop="monitor">
                <a-input-number style="width: 80%" :min="1" v-model="temp.rollback.monitor" placeholder="更新完成后确实成功的时间" />
              </a-form-model-item>
            </a-tab-pane>
            <a-tab-pane key="resources" tab="资源" v-if="temp.resources">
              <a-form-model-item label="预占资源">
                <a-row>
                  <a-col :span="8">
                    <a-input addon-before="CPUs" v-model="temp.resources.reservations.nanoCPUs" placeholder="nanoCPUs 最小 1000000" />
                  </a-col>
                  <a-col :span="8" :offset="1">
                    <a-input addon-before="memory" v-model="temp.resources.reservations.memoryBytes" placeholder="memory 最小 4M" />
                  </a-col>
                </a-row>
              </a-form-model-item>
              <a-form-model-item label="限制资源">
                <a-row>
                  <a-col :span="8">
                    <a-input addon-before="CPUs" v-model="temp.resources.limits.nanoCPUs" placeholder="nanoCPUs 最小 1000000" />
                  </a-col>
                  <a-col :span="8" :offset="1">
                    <a-input addon-before="memory" v-model="temp.resources.limits.memoryBytes" placeholder="memory 最小 4M" />
                  </a-col>
                </a-row>
              </a-form-model-item>
            </a-tab-pane>
          </a-tabs>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 查看任务 -->
    <a-modal v-model="taskVisible" title="查看任务" width="80vw" :footer="null" :maskClosable="false">
      <swarm-task v-if="taskVisible" :visible="taskVisible" :taskState="this.temp.state" :id="this.id" :serviceId="this.temp.id" />
    </a-modal>
    <!-- 查看日志 -->
    <a-modal v-model="logVisible" title="查看日志" width="80vw" :footer="null" :maskClosable="false">
      <pull-log v-if="logVisible" :id="this.id" :dataId="this.temp.id" type="service" />
    </a-modal>
  </div>
</template>

<script>
import {dockerSwarmServicesDel, dockerSwarmServicesEdit, dockerSwarmServicesList} from "@/api/docker-swarm";
import SwarmTask from "./task";
import PullLog from "./pull-log";
import {renderSize} from "@/utils/time";

export default {
  components: { SwarmTask, PullLog },
  props: {
    id: {
      type: String,
    },
    visible: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      loading: false,
      listQuery: {},
      list: [],
      temp: {
        update: {},
        rollback: {},
      },
      editVisible: false,
      initSwarmVisible: false,
      taskVisible: false,
      logVisible: false,
      autoUpdateTime: null,
      rules: {
        name: [{ required: true, message: "服务名称必填", trigger: "blur" }],
        mode: [{ required: true, message: "运行模式必填", trigger: "blur" }],
        image: [{ required: true, message: "镜像名称必填", trigger: "blur" }],
      },
      columns: [
        { title: "序号", width: 80, ellipsis: true, align: "center", customRender: (text, record, index) => `${index + 1}` },
        { title: "服务Id", dataIndex: "id", ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: "名称", dataIndex: "spec.name", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "模式", dataIndex: "spec.mode.mode", ellipsis: true, width: 120, scopedSlots: { customRender: "tooltip" } },
        { title: "副本数", dataIndex: "spec.mode.replicated.replicas", align: "center", width: 90, ellipsis: true, scopedSlots: { customRender: "replicas" } },
        { title: "解析模式", dataIndex: "spec.endpointSpec.mode", ellipsis: true, width: 100, scopedSlots: { customRender: "tooltip" } },

        {
          title: "修改时间",
          dataIndex: "updatedAt",

          ellipsis: true,
          scopedSlots: { customRender: "updatedAt" },
          width: 170,
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, align: "center", width: 120 },
      ],
    };
  },
  computed: {},
  beforeDestroy() {
    this.autoUpdateTime && clearTimeout(this.autoUpdateTime);
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      if (!this.visible) {
        return;
      }
      this.loading = true;
      this.listQuery.id = this.id;
      dockerSwarmServicesList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
        clearTimeout(this.autoUpdateTime);
        this.autoUpdateTime = setTimeout(() => {
          this.loadData();
        }, 3000);
      });
    },
    //  任务
    handleTask(record, state) {
      this.taskVisible = true;
      this.temp = record;
      this.temp = { ...this.temp, state: state || "" };
    },
    // 日志
    handleLog(record) {
      this.logVisible = true;
      this.temp = record;
    },
    //  创建服务
    handleAdd() {
      this.editVisible = true;
      this.temp = {
        mode: "REPLICATED",
        replicas: 1,
        endpointResolutionMode: "VIP",
        exposedPorts: [
          {
            publishMode: "host",
            protocol: "TCP",
          },
        ],
        volumes: [
          {
            type: "VOLUME",
          },
        ],
        args: [{}],
        commands: [{}],
        envs: [{}],
        update: {},
        rollback: {},
        resources: {
          limits: {},
          reservations: {},
        },
      };
    },
    // 编辑
    handleEdit(record) {
      const spec = record.spec;
      if (!spec) {
        this.$notification.error({
          message: "信息不完整不能编辑",
        });
        return;
      }
      this.editVisible = true;
      let image = spec.taskTemplate?.containerSpec?.image;

      if (image && image.includes("@")) {
        image = image.split("@")[0];
      }
      this.temp = {
        serviceId: record.id,
        name: spec.name,
        mode: spec.mode?.mode,
        replicas: spec.mode?.replicated?.replicas,
        image: image,
        version: record.version?.index,
        endpointResolutionMode: spec.endpointSpec?.mode,
        exposedPorts: [
          {
            publishMode: "host",
            protocol: "TCP",
          },
        ],
        volumes: [{ type: "VOLUME" }],
        args: [{}],
        commands: [{}],
        envs: [{}],
        update: {},
        rollback: {},
        resources: {},
      };

      const args = spec.taskTemplate?.containerSpec?.args;
      const mounts = spec.taskTemplate?.containerSpec?.mounts;
      const command = spec.taskTemplate?.containerSpec?.command;
      const env = spec.taskTemplate?.containerSpec?.env;
      const limits = spec.taskTemplate?.resources?.limits;
      const reservations = spec.taskTemplate?.resources?.reservations;
      const ports = spec.endpointSpec?.ports;
      const updateConfig = spec.updateConfig;
      const rollbackConfig = spec.rollbackConfig;
      if (args) {
        this.temp = {
          ...this.temp,
          args: args.map((item) => {
            return {
              value: item,
            };
          }),
        };
      }
      if (command) {
        this.temp = {
          ...this.temp,
          commands: command.map((item) => {
            return {
              value: item,
            };
          }),
        };
      }
      if (env) {
        this.temp = {
          ...this.temp,
          envs: env.map((item) => {
            return {
              name: item.split("=")[0],
              value: item.split("=")[1],
            };
          }),
        };
      }
      if (ports) {
        this.temp = { ...this.temp, exposedPorts: ports };
      }
      if (mounts) {
        this.temp = { ...this.temp, volumes: mounts };
      }
      if (updateConfig) {
        this.temp = { ...this.temp, update: updateConfig };
      }
      if (rollbackConfig) {
        this.temp = { ...this.temp, rollback: rollbackConfig };
      }
      let resources = { limits: {}, reservations: {} };
      if (limits) {
        limits.memoryBytes = renderSize(limits.memoryBytes);
        resources = { ...resources, limits: limits };
      }
      if (reservations) {
        reservations.memoryBytes = renderSize(reservations.memoryBytes);
        resources = { ...resources, reservations: reservations };
      }
      this.temp = { ...this.temp, resources: resources };
    },
    handleEditOk() {
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.temp.id = this.id;
        const temp = Object.assign({}, this.temp);
        temp.volumes = (this.temp.volumes || []).filter((item) => {
          return item.source && item.target;
        });
        // 处理端口
        temp.exposedPorts = (this.temp.exposedPorts || []).filter((item) => {
          return item.publishedPort && item.targetPort;
        });
        dockerSwarmServicesEdit(temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.editVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 删除
    handleDel(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除此服务么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            serviceId: record.id,
            id: this.id,
          };
          dockerSwarmServicesDel(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
  },
};
</script>
