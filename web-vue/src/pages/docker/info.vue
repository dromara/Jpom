<template>
  <div class="full-content">
    <a-collapse :activeKey="['1', '2']">
      <a-collapse-panel key="1" header="基础信息">
        <a-timeline>
          <a-timeline-item>
            <span class="layui-elem-quote">
              基础信息：{{ temp.name }} - {{ temp.osType }} - {{ temp.operatingSystem }} - <a-tag>{{ temp.architecture }}</a-tag>
            </span>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote">版本：{{ temp.serverVersion }} </span>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote"
              >资源： <a-tag>cpu:{{ temp.nCPU || temp.NCPU }}</a-tag> <a-tag>内存:{{ renderSize(temp.memTotal) }}</a-tag>

              <a-tag>容器数：{{ temp.containers }}</a-tag>
              <a-tag>镜像数：{{ temp.images }}</a-tag>
            </span>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote">系统时间：{{ temp.systemTime }} </span>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote">运行目录：{{ temp.dockerRootDir }} </span>
          </a-timeline-item>
          <template v-if="temp.swarm">
            <a-timeline-item>
              <div class="layui-elem-quote">
                集群信息：
                <div style="padding-left: 10px">
                  <a-space direction="vertical">
                    <div>
                      本地状态：<a-tag v-if="temp.swarm.nodeAddr">{{ temp.swarm.nodeAddr }}</a-tag> <a-tag>{{ temp.swarm.localNodeState }}</a-tag>
                    </div>
                    <div v-if="temp.swarm.remoteManagers">
                      管理列表： <a-tag v-for="(item, index) in temp.swarm.remoteManagers" :key="index">{{ item.addr }}</a-tag>
                    </div>
                    <div>管理节点：{{ temp.swarm.controlAvailable ? "是" : "否" }}</div>
                  </a-space>
                </div>
              </div>
            </a-timeline-item>
          </template>
          <a-timeline-item v-if="temp.plugins">
            <div class="layui-elem-quote">
              插件：

              <a-list item-layout="horizontal" :data-source="Object.keys(temp.plugins)" size="small">
                <a-list-item slot="renderItem" slot-scope="item">
                  {{ item }} <a-tag v-for="(item1, index) in temp.plugins[item]" :key="index">{{ item1 }}</a-tag>
                </a-list-item>
              </a-list>
            </div>
          </a-timeline-item>
          <a-timeline-item v-if="temp.registryConfig">
            <div class="layui-elem-quote">
              仓库：
              <a-list item-layout="horizontal" :data-source="Object.keys(temp.registryConfig.indexConfigs)" size="small">
                <a-list-item slot="renderItem" slot-scope="item">
                  {{ item }} <a-tag color="green" v-if="temp.registryConfig.indexConfigs[item].official">官方</a-tag
                  ><a-tag color="green" v-if="temp.registryConfig.indexConfigs[item].secure">安全</a-tag>
                  <a-tag v-for="(item1, index) in temp.registryConfig.indexConfigs[item].mirrors" :key="index">{{ item1 }}</a-tag>
                </a-list-item>
              </a-list>
            </div>
          </a-timeline-item>
        </a-timeline>
      </a-collapse-panel>
      <a-collapse-panel key="2" header="修剪">
        <a-row>
          <a-col :span="10">
            <a-form-model ref="ruleForm" :model="pruneForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }" :rules="rules">
              <a-form-model-item :wrapper-col="{ span: 14, offset: 4 }">
                <a-alert message="修剪操作会删除相关数据，请谨慎操作。请您再确认本操作后果后再使用" banner />
              </a-form-model-item>
              <a-form-model-item label="修剪类型" :help="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].help">
                <a-select v-model="pruneForm.pruneType" placeholder="请选择修剪类型">
                  <a-select-option :key="key" v-for="(item, key) in pruneTypes"> {{ item.name }} </a-select-option>
                </a-select>
              </a-form-model-item>
              <a-form-model-item label="悬空类型" v-if="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].filters.includes('dangling')">
                <a-switch v-model="pruneForm.dangling" checked-children="悬空" un-checked-children="非悬空" />
              </a-form-model-item>
              <a-form-model-item label="限定时间" v-if="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].filters.includes('until')">
                <template #help><a-tag color="#f50"> 建议添加指定时间范围,否则将删除满足条件的所有数据</a-tag> </template>
                <a-tooltip title="可以是 Unix 时间戳、日期格式的时间戳或 Go 持续时间字符串（例如 10m、1h30m），相对于守护进程机器的时间计算。">
                  <a-input v-model="pruneForm.until" :placeholder="`修剪在此时间戳之前创建的对象 例如：24h`" />
                </a-tooltip>
              </a-form-model-item>

              <a-form-model-item label="指定标签" v-if="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].filters.includes('labels')">
                <a-tooltip title="示例：key,key1 或者 key=value,key1=value1">
                  <a-input v-model="pruneForm.labels" placeholder="修剪具有指定标签的对象,多个使用逗号分隔" />
                </a-tooltip>
              </a-form-model-item>
              <a-form-model-item :wrapper-col="{ span: 14, offset: 4 }">
                自动执行：docker {{ pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].command }} prune xxxxx
              </a-form-model-item>
              <a-form-model-item :wrapper-col="{ span: 14, offset: 4 }">
                <a-button type="primary" @click="onPruneSubmit"> 确定 </a-button>
              </a-form-model-item>
            </a-form-model>
          </a-col>
        </a-row>
      </a-collapse-panel>
    </a-collapse>
  </div>
</template>
<script>
import { dockerInfo, dockerPrune } from "@/api/docker-api";
import { renderSize } from "@/utils/const";
export default {
  props: {
    id: {
      type: String,
      default: "",
    },
    urlPrefix: {
      type: String,
    },
    machineDockerId: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      temp: {},
      pruneForm: {
        pruneType: "IMAGES",
        dangling: true,
      },
      pruneTypes: {
        IMAGES: {
          value: "IMAGES",
          name: "镜像",
          command: "image",
          help: "仅修剪未使用和未标记的镜像",
          filters: ["until", "dangling"],
        },
        CONTAINERS: {
          value: "CONTAINERS",
          name: "容器",
          command: "container",
          filters: ["until", "labels"],
        },
        NETWORKS: {
          value: "NETWORKS",
          name: "网络",
          command: "network",
          filters: ["until"],
        },
        VOLUMES: {
          value: "VOLUMES",
          name: "卷",
          command: "volume",
          filters: ["labels"],
        },
        BUILD: {
          value: "BUILD",
          name: "构建",
          command: "builder",
          filters: ["until"],
        },
      },
      rules: {},
    };
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId;
    },
  },
  mounted() {
    this.loadData();
    // console.log(Comparator);
  },
  methods: {
    renderSize,
    // load data
    loadData() {
      dockerInfo(this.urlPrefix, {
        id: this.reqDataId,
      }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data;
        }
      });
    },
    onPruneSubmit() {
      this.$refs["ruleForm"].validate((valid) => {
        if (!valid) {
          return;
        }
        //
        this.$confirm({
          title: "系统提示",
          content: "确定要修剪对应的信息吗？修剪会自动清理对应的数据",
          okText: "确认",
          cancelText: "取消",
          onOk: () => {
            // 组装参数
            const params = { id: this.reqDataId, pruneType: this.pruneForm.pruneType };

            this.pruneTypes[params.pruneType] &&
              this.pruneTypes[params.pruneType].filters.forEach((element) => {
                params[element] = this.pruneForm[element];
              });

            dockerPrune(this.urlPrefix, params).then((res) => {
              if (res.code === 200) {
                this.$notification.success({
                  message: res.msg,
                });
              }
            });
          },
        });
      });
    },
  },
};
</script>
