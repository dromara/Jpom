<template>
  <div>
    <a-modal
      destroyOnClose
      :visible="true"
      title="编辑 Script"
      @ok="handleEditScriptOk"
      :maskClosable="false"
      width="80vw"
      @cancel="
        () => {
          $emit('close');
        }
      "
    >
      <a-form-model ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-alert v-if="this.temp.scriptType === 'server-sync'" message="服务端同步的脚本不能在此修改" banner />
        <a-form-model-item label="选择节点">
          <a-select v-model="temp.nodeId" :disabled="!!temp.nodeId" allowClear placeholder="请选择节点">
            <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <template v-if="temp.nodeId">
          <a-form-model-item label="Script 名称" prop="name">
            <a-input v-model="temp.name" placeholder="名称" />
          </a-form-model-item>
          <a-form-model-item label="Script 内容" prop="context">
            <div style="height: 40vh; overflow-y: scroll">
              <code-editor v-model="temp.context" :options="{ mode: 'shell', tabSize: 2, theme: 'abcdef' }"></code-editor>
            </div>
          </a-form-model-item>
          <!-- <a-form-model-item label="默认参数" prop="defArgs">
          <a-input v-model="temp.defArgs" placeholder="默认参数" />
        </a-form-model-item> -->
          <a-form-model-item label="默认参数">
            <div v-for="(item, index) in commandParams" :key="item.key">
              <a-row type="flex" justify="center" align="middle">
                <a-col :span="22">
                  <a-input :addon-before="`参数${index + 1}描述`" v-model="item.desc" placeholder="参数描述,参数描述没有实际作用,仅是用于提示参数的含义" />
                  <a-input :addon-before="`参数${index + 1}值`" v-model="item.value" placeholder="参数值,添加默认参数后在手动执行脚本时需要填写参数值" />
                </a-col>
                <a-col :span="2">
                  <a-row type="flex" justify="center" align="middle">
                    <a-col>
                      <a-icon @click="() => commandParams.splice(index, 1)" type="minus-circle" style="color: #ff0000" />
                    </a-col>
                  </a-row>
                </a-col>
              </a-row>
              <a-divider style="margin: 5px 0" />
            </div>

            <a-button type="primary" @click="() => commandParams.push({})">添加参数</a-button>
          </a-form-model-item>
          <a-form-model-item label="共享" prop="global">
            <a-radio-group v-model="temp.global">
              <a-radio :value="true"> 全局</a-radio>
              <a-radio :value="false"> 当前工作空间</a-radio>
            </a-radio-group>
          </a-form-model-item>
          <a-form-model-item label="定时执行" prop="autoExecCron">
            <a-auto-complete
              v-model="temp.autoExecCron"
              placeholder="如果需要定时自动执行则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）"
              option-label-prop="value"
            >
              <template slot="dataSource">
                <a-select-opt-group v-for="group in cronDataSource" :key="group.title">
                  <span slot="label">
                    {{ group.title }}
                  </span>
                  <a-select-option v-for="opt in group.children" :key="opt.title" :value="opt.value"> {{ opt.title }} {{ opt.value }} </a-select-option>
                </a-select-opt-group>
              </template>
            </a-auto-complete>
          </a-form-model-item>
          <a-form-model-item label="描述" prop="description">
            <a-input v-model="temp.description" type="textarea" :rows="3" style="resize: none" placeholder="详细描述" />
          </a-form-model-item>
        </template>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import codeEditor from "@/components/codeEditor";
import { editScript, itemScript } from "@/api/node-other";
import { CRON_DATA_SOURCE } from "@/utils/const";
import { getNodeListAll } from "@/api/node";
export default {
  components: {
    codeEditor,
  },
  props: {
    nodeId: {
      type: String,
      default: undefined,
    },
    scriptId: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      temp: {},
      cronDataSource: CRON_DATA_SOURCE,
      commandParams: [],
      nodeList: [],
      rules: {
        name: [{ required: true, message: "请输入脚本名称", trigger: "blur" }],
        context: [{ required: true, message: "请输入脚本内容", trigger: "blur" }],
      },
    };
  },
  mounted() {
    // 查询节点
    getNodeListAll().then((res) => {
      if (res.code === 200 && res.data) {
        this.nodeList = res.data;
        // res.data.forEach((item) => {
        //   this.nodeMap[item.id] = item.name;
        // });
      }
      this.handleEdit();
    });
  },
  methods: {
    // 修改
    handleEdit() {
      this.$refs["editScriptForm"]?.resetFields();
      if (this.scriptId && this.nodeId) {
        itemScript({
          id: this.scriptId,
          nodeId: this.nodeId,
        }).then((res) => {
          if (res.code === 200) {
            this.temp = Object.assign({}, res.data, { global: res.data.workspaceId === "GLOBAL", workspaceId: "" });
            this.temp.nodeId = this.nodeId;
            this.commandParams = this.temp.defArgs ? JSON.parse(this.temp.defArgs) : [];
            //
          }
        });
      } else {
        this.temp = { global: false, type: "add", nodeId: this.nodeId };
      }
    },
    // 提交 Script 数据
    handleEditScriptOk() {
      if (this.temp.scriptType === "server-sync") {
        this.$notification.warning({
          message: "服务端同步的脚本不能在此修改",
        });
        return;
      }
      if (!this.temp.nodeId) {
        this.$notification.warning({
          message: "没有选择节点不能保存脚本",
        });
        return;
      }
      // 检验表单
      this.$refs["editScriptForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              this.$notification.error({
                message: "请填写第" + (i + 1) + "个参数的描述",
              });
              return false;
            }
          }
          this.temp.defArgs = JSON.stringify(this.commandParams);
        } else {
          this.temp.defArgs = "";
        }
        // 提交数据
        editScript(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });

            this.$emit("close");
          }
        });
      });
    },
  },
};
</script>
