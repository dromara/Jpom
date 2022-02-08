<template>
  <div class="node-full-content">
    <a-form-model ref="editForm" :model="temp">
      <a-alert v-if="temp.file" :message="`配置文件路径:${temp.file}`" style="margin-top: 10px; margin-bottom: 20px" banner />
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
import { getConfigData, editConfig, systemInfo } from "@/api/system";
import codeEditor from "@/components/codeEditor";
import { RESTART_UPGRADE_WAIT_TIME_COUNT } from "@/utils/const";
import Vue from "vue";

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

      checkCount: 0,
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
          this.temp.content = res.data.content;
          this.temp.file = res.data.file;
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
          if (this.temp.restart) {
            this.startCheckRestartStatus(res.msg);
          }
        }
        // button recover
        this.submitAble = false;
      });
    },
    startCheckRestartStatus(msg) {
      this.checkCount = 0;
      Vue.prototype.$setLoading({
        spinning: true,
        tip: (msg || "重启中，请稍候...") + ",请耐心等待暂时不用刷新页面,重启成功后会自动刷新",
      });
      setTimeout(() => {
        //
        this.timer = setInterval(() => {
          systemInfo(this.node.id)
            .then((res) => {
              if (res.code === 200) {
                clearInterval(this.timer);
                Vue.prototype.$setLoading(false);
                this.$notification.success({
                  message: "重启成功",
                });

                setTimeout(() => {
                  location.reload();
                }, 1000);
              } else {
                if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                  this.$notification.warning({
                    message: "未重启成功：" + (res.msg || ""),
                  });
                  Vue.prototype.$setLoading(false);
                  clearInterval(this.timer);
                }
              }
            })
            .catch((error) => {
              console.error(error);
              if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                Vue.prototype.$setLoading(false);
                this.$notification.error({
                  message: "重启超时,请去服务器查看控制台日志排查问题",
                });
                clearInterval(this.timer);
              }
            });
          this.checkCount = this.checkCount + 1;
        }, 2000);
      }, 6000);
    },
  },
};
</script>
<style scoped>
.node-content-config {
  height: calc(100vh - 260px);
  overflow-y: scroll;
}
.btn {
  margin-left: 20px;
}
</style>
