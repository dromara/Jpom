<template>
  <div>
    <a-table size="middle" :data-source="list" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <div>
            显示所有
            <a-switch checked-children="是" un-checked-children="否" v-model="listQuery['showAll']" />
          </div>
          <div>
            悬空
            <a-switch checked-children="是" un-checked-children="否" v-model="listQuery['dangling']" />
          </div>
          <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
        </a-space>
        |

        <a-input-search v-model="pullImageName" @search="pullImage" style="width: 260px" placeholder="要拉取的镜像名称" class="search-input-item">
          <a-button slot="enterButton"> <a-icon type="cloud-download" /> </a-button>
        </a-input-search>
        <!-- <a-button type="primary" @click="pullImage">拉取</a-button> -->
      </template>

      <a-tooltip slot="repoTags" slot-scope="text" placement="topLeft" :title="(text || []).join(',')">
        <span>{{ (text || []).join(",") }}</span>
      </a-tooltip>
      <a-tooltip slot="size" slot-scope="text, record" placement="topLeft" :title="renderSize(text) + ' ' + renderSize(record.virtualSize)">
        <span>{{ renderSize(text) }}</span>
      </a-tooltip>

      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <a-tooltip slot="id" slot-scope="text" :title="text">
        <span> {{ text && text.split(":")[1].slice(0, 12) }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <!-- <a-tooltip title="停止" v-if="record.state === 'running'">
          <a-button size="small" type="link" @click="doAction(record, 'stop')"><a-icon type="stop" /></a-button>
        </a-tooltip>
        <a-tooltip title="启动" v-else>
          <a-button size="small" type="link" @click="doAction(record, 'start')"> <a-icon type="play-circle" /></a-button>
        </a-tooltip>
        -->
          <a-tooltip title="使用当前镜像创建一个容器">
            <a-button size="small" type="link" @click="createContainer(record)"><a-icon type="select" /></a-button>
          </a-tooltip>
          <a-tooltip title="更新镜像">
            <a-button size="small" type="link" :disabled="!record.repoTags" @click="tryPull(record)"><a-icon type="cloud-download" /></a-button>
          </a-tooltip>

          <a-tooltip title="删除镜像">
            <a-button size="small" type="link" @click="doAction(record, 'remove')"><a-icon type="delete" /></a-button>
          </a-tooltip>
        </a-space>
      </template>
    </a-table>
    <a-modal v-model="buildVisible" width="60vw" title="构建容器" @ok="handleBuildOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item label="基础镜像" prop="image">
          <a-input v-model="temp.image" disabled placeholder="" />
        </a-form-model-item>
        <a-form-model-item label="容器名称" prop="name">
          <a-input v-model="temp.name" placeholder="容器名称" />
        </a-form-model-item>
        <a-form-model-item label="端口">
          <a-row v-for="(item, index) in temp.exposedPorts" :key="index">
            <a-col :span="21">
              <a-space>
                <a-input-group>
                  <a-row>
                    <a-col :span="8">
                      <a-input addon-before="IP" placeholder="宿主机ip" v-model="item.ip"> </a-input>
                    </a-col>
                    <a-col :span="6" :offset="1">
                      <a-input addon-before="端口" placeholder="端口" v-model="item.publicPort"> </a-input>
                    </a-col>
                    <a-col :span="8" :offset="1">
                      <a-input addon-before="容器" :disabled="item.disabled" v-model="item.port" placeholder="容器端口">
                        <a-select
                          :getPopupContainer="
                            (triggerNode) => {
                              return triggerNode.parentNode || document.body;
                            }
                          "
                          slot="addonAfter"
                          :disabled="item.disabled"
                          v-model="item.scheme"
                          placeholder="端口协议"
                        >
                          <a-select-option value="tcp">tcp</a-select-option>
                          <a-select-option value="udp">udp</a-select-option>
                          <a-select-option value="sctp">sctp</a-select-option>
                        </a-select>
                      </a-input>
                    </a-col>
                  </a-row>
                </a-input-group>
              </a-space>
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
                        scheme: 'tcp',
                        ip: '0.0.0.0',
                      });
                    }
                  "
                />
              </a-space>
            </a-col>
          </a-row>
        </a-form-model-item>

        <a-form-model-item label="挂载卷">
          <a-row v-for="(item, index) in temp.volumes" :key="index">
            <a-col :span="10">
              <a-input addon-before="宿主" v-model="item.host" placeholder="宿主机目录" />
            </a-col>
            <a-col :span="10" :offset="1">
              <a-input addon-before="容器" :disabled="item.disabled" v-model="item.container" placeholder="容器目录" />
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
        <a-form-model-item label="环境变量">
          <a-row v-for="(item, index) in temp.env" :key="index">
            <a-col :span="10">
              <a-input v-model="item.key" placeholder="变量名" />
            </a-col>
            <a-col :span="10" :offset="1">
              <a-input v-model="item.value" placeholder="变量值" />
            </a-col>
            <a-col :span="2" :offset="1">
              <a-space>
                <a-icon
                  type="minus-circle"
                  v-if="temp.env && temp.env.length > 1"
                  @click="
                    () => {
                      temp.env.splice(index, 1);
                    }
                  "
                />

                <a-icon
                  type="plus-square"
                  @click="
                    () => {
                      temp.env.push({});
                    }
                  "
                />
              </a-space>
            </a-col>
          </a-row>
        </a-form-model-item>
        <a-form-model-item label="命令">
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
        <a-form-model-item label="自动启动">
          <a-switch v-model="temp.autorun" checked-children="启动" un-checked-children="不启动" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 日志 -->
    <a-modal :width="'80vw'" v-model="logVisible" title="pull日志" :footer="null" :maskClosable="false">
      <pull-image-Log v-if="logVisible" :id="temp.id" />
    </a-modal>
  </div>
</template>
<script>
import {parseTime, renderSize} from "@/utils/time";
import {dockerImageCreateContainer, dockerImageInspect, dockerImagePullImage, dockerImageRemove, dockerImagesList} from "@/api/docker-api";
import PullImageLog from "@/pages/docker/pull-image-log";

export default {
  components: {
    PullImageLog,
  },
  props: {
    id: {
      type: String,
    },
  },
  data() {
    return {
      list: [],
      loading: false,
      listQuery: {
        showAll: false,
      },
      logVisible: false,
      pullImageName: "",
      renderSize,
      temp: {},
      rules: {
        name: [
          { required: true, message: "容器名称必填", trigger: "blur" },
          { pattern: /[a-zA-Z0-9][a-zA-Z0-9_.-]$/, message: "容器名称数字字母,且长度大于1", trigger: "blur" },
        ],
      },
      columns: [
        { title: "序号", width: 80, ellipsis: true, align: "center", customRender: (text, record, index) => `${index + 1}` },
        { title: "名称", dataIndex: "repoTags", ellipsis: true, scopedSlots: { customRender: "repoTags" } },
        { title: "镜像ID", dataIndex: "id", ellipsis: true, width: 140, align: "center", scopedSlots: { customRender: "id" } },
        { title: "父级ID", dataIndex: "parentId", ellipsis: true, width: 140, align: "center", scopedSlots: { customRender: "id" } },
        { title: "占用空间", dataIndex: "size", ellipsis: true, width: 120, scopedSlots: { customRender: "size" } },
        {
          title: "创建时间",
          dataIndex: "created",
          sorter: (a, b) => new Number(a.created) - new Number(b.created),
          sortDirections: ["descend", "ascend"],
          defaultSortOrder: "descend",
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 180,
        },

        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 120 },
      ],
      action: {
        remove: {
          msg: "您确定要删除当前镜像吗？",
          api: dockerImageRemove,
        },
      },
      buildVisible: false,
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      //this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.id = this.id;
      dockerImagesList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      });
    },
    doAction(record, actionKey) {
      const action = this.action[actionKey];
      if (!action) {
        return;
      }
      this.$confirm({
        title: "系统提示",
        content: action.msg,
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: this.id,
            imageId: record.id,
          };
          action.api(params).then((res) => {
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
    tryPull(record) {
      const repoTags = record?.repoTags[0];
      if (!repoTags) {
        this.$notification.error({
          message: "镜像名称不正确 不能更新",
        });
        return;
      }
      this.pullImageName = repoTags;
      this.pullImage();
    },
    // 构建镜像
    createContainer(record) {
      dockerImageInspect({
        id: this.id,
        imageId: record.id,
      }).then((res) => {
        this.buildVisible = true;
        // const volumesObj = {}; // res.data?.config?.volumes || {};
        // const keys = Object.keys(volumesObj);

        this.temp = {
          volumes: [{}],
          exposedPorts: (res.data?.config?.exposedPorts || [{}]).map((item) => {
            item.disabled = item.port !== null;
            item.ip = "0.0.0.0";
            item.scheme = item.scheme || "tcp";
            return item;
          }),
          image: (record.repoTags || []).join(","),
          autorun: true,
          imageId: record.id,
          env: [{}],
          commands: [{}],
        };
      });
    },
    // 创建容器
    handleBuildOk() {
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const temp = {
          id: this.id,
          autorun: this.temp.autorun,
          imageId: this.temp.imageId,
          name: this.temp.name,
          env: {},
          commands: [],
        };
        temp.volumes = (this.temp.volumes || [])
          .filter((item) => {
            return item.host;
          })
          .map((item) => {
            return item.host + ":" + item.container;
          })
          .join(",");
        // 处理端口
        temp.exposedPorts = (this.temp.exposedPorts || [])
          .filter((item) => {
            return item.publicPort && item.ip;
          })
          .map((item) => {
            return item.ip + ":" + item.publicPort + ":" + item.port;
          })
          .join(",");
        // 环境变量
        this.temp.env.forEach((item) => {
          if (item.key && item.key) {
            temp.env[item.key] = item.value;
          }
        });
        //
        temp.commands = (this.temp.commands || []).map((item) => {
          return item.value || "";
        });
        dockerImageCreateContainer(temp).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            this.buildVisible = false;
          }
        });
      });
    },
    // 拉取镜像
    pullImage() {
      if (!this.pullImageName) {
        this.$notification.warn({
          message: "请填写要拉取的镜像名称",
        });
        return;
      }
      dockerImagePullImage({
        id: this.id,
        repository: this.pullImageName,
      }).then((res) => {
        if (res.code === 200) {
          this.logVisible = true;
          this.temp = {
            id: res.data,
          };
        }
      });
    },
  },
};
</script>
