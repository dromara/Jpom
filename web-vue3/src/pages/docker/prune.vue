<template>
  <div class="full-content">
    <a-form-model ref="ruleForm" :model="pruneForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }" :rules="rules">
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
      <a-form-model-item :wrapper-col="{ span: 14, offset: 4 }"> 自动执行：docker {{ pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].command }} prune xxxxx </a-form-model-item>
      <a-form-model-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" @click="onPruneSubmit"> 确定 </a-button>
      </a-form-model-item>
    </a-form-model>
  </div>
</template>
<script>
import { dockerPrune } from "@/api/docker-api";
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
