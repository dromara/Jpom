<template>
  <div class="node-full-content">
    <a-form-model ref="editForm" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
      <a-form-model-item class="node-content-config">
        <code-editor v-model="temp.content" :options="{ mode: 'yaml', tabSize: 2 }"></code-editor>
        <!-- <a-input v-model="temp.content" type="textarea" :rows="25" style="resize: none" class="node-content-config" placeholder="请输入配置内容，参考项目的配置文件"/> -->
      </a-form-model-item>
      <a-form-model-item :wrapper-col="{ span: 14, offset: 2 }">
        <a-button type="primary" class="btn" :disabled="submitAble" @click="onSubmit(false)">保存</a-button>
        <a-button type="primary" class="btn" :disabled="submitAble" @click="onSubmit(true)">保存并重启</a-button>
      </a-form-model-item>
    </a-form-model>
  </div>
</template>
<script>
import { getConfigData, editConfig } from "../../../../api/system";
import codeEditor from "@/components/codeEditor";

export default {
  components: {
    codeEditor,
  },
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      temp: {
        content: "",
      },
      submitAble: false,
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // load data
    loadData() {
      getConfigData(this.node.id).then((res) => {
        if (res.code === 200) {
          this.temp.content = res.data;
        }
      });
    },
    // submit
    onSubmit(restart) {
      // disabled submit button
      this.submitAble = true;
      this.temp.nodeId = this.node.id;
      this.temp.restart = restart;
      editConfig(this.temp).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
            
          });
        }
        // button recover
        this.submitAble = false;
      });
    },
  },
};
</script>
<style scoped>
.node-content-config {
  height: calc(100vh - 200px);
  overflow-y: scroll;
}
.btn {
  margin-left: 20px;
}
</style>
