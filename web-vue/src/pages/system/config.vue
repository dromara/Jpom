<template>
  <a-tabs default-active-key="1">
    <a-tab-pane key="1" tab="系统配置">
      <a-form-model ref="editForm" :model="temp" :label-col="{ span: 2 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item class="config-editor">
          <code-editor v-model="temp.content" :options="{ mode: 'yaml', tabSize: 2 }"></code-editor>
          <!-- <a-input v-model="temp.content" type="textarea" :rows="25" class="content-config" placeholder="请输入配置内容，参考项目的配置文件" /> -->
        </a-form-model-item>
        <a-form-model-item :wrapper-col="{ span: 14, offset: 2 }">
          <a-button type="primary" class="btn" :disabled="submitAble" @click="onSubmit(false)">保存</a-button>
          <a-button type="primary" class="btn" :disabled="submitAble" @click="onSubmit(true)">保存并重启</a-button>
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
    <a-tab-pane key="2" tab="IP白名单配置" class="ip-config-panel">
      <a-alert :message="`当前访问IP：${ipTemp.ip}`" type="success" />
      <a-alert message="请仔细确认后配置，ip配置后立即生效。配置时需要保证当前ip能访问！127.0.0.1 该IP不受访问限制" style="margin-top: 10px" banner />
      <a-alert message="如果配置错误需要重新服务端并添加命令行参数 --rest:ip_config 将恢复默认配置" style="margin-top: 10px" banner />
      <a-form-model style="margin-top: 10px" ref="editForm" :model="temp" :label-col="{ span: 2 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item label="IP白名单" prop="content">
          <a-input v-model="ipTemp.allowed" type="textarea" :rows="10" class="ip-list-config" placeholder="请输入IP白名单,多个使用换行,0.0.0.0 是开发所有IP,支持配置IP段 192.168.1.1/192.168.1.254" />
        </a-form-model-item>
        <a-form-model-item label="IP黑名单" prop="content">
          <a-input v-model="ipTemp.prohibited" type="textarea" :rows="10" class="ip-list-config" placeholder="请输入IP黑名单,多个使用换行,支持配置IP段 192.168.1.1/192.168.1.254" />
        </a-form-model-item>
        <a-form-model-item :wrapper-col="{ span: 14, offset: 2 }" class="ip-config-button">
          <a-button type="primary" class="btn" :disabled="submitIpAble" @click="onSubmitIp()">保存</a-button>
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>
<script>
import { getConfigData, editConfig, getIpConfigData, editIpConfig, systemInfo } from "@/api/system";
import codeEditor from "@/components/codeEditor";
import { RESTART_UPGRADE_WAIT_TIME_COUNT } from "@/utils/const";
import Vue from "vue";

export default {
  components: {
    codeEditor,
  },
  data() {
    return {
      temp: {
        content: "",
      },
      ipTemp: {
        allowed: "",
        prohibited: "",
      },
      submitAble: false,
      submitIpAble: false,
      $global_loading: null,
      checkCount: 0,
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // load data
    loadData() {
      getConfigData().then((res) => {
        if (res.code === 200) {
          this.temp.content = res.data;
        }
      });
      getIpConfigData().then((res) => {
        if (res.code === 200) {
          if (res.data) {
            this.ipTemp = res.data;
          }
        }
      });
    },
    // submit
    onSubmit(restart) {
      // disabled submit button
      this.submitAble = true;
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
          //
        }
        // button recover
        this.submitAble = false;
      });
    },
    startCheckRestartStatus(msg) {
      this.checkCount = 0;
      this.$global_loading = Vue.prototype.$loading.service({
        lock: true,
        text: (msg || "重启中，请稍候...") + ",请耐心等待暂时不用刷新页面,重启成功后会自动刷新",
        spinner: "el-icon-loading",
        background: "rgba(0, 0, 0, 0.7)",
      });
      //
      this.timer = setInterval(() => {
        systemInfo()
          .then((res) => {
            if (res.code === 200) {
              clearInterval(this.timer);
              this.$global_loading && this.$global_loading.close();
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
              }
            }
          })
          .catch((error) => {
            console.log(error);
            if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
              this.$global_loading && this.$global_loading.close();
              this.$notification.error({
                message: "重启超时,请去服务器查看控制台日志排查问题",
              });
            }
          });
        this.checkCount = this.checkCount + 1;
      }, 2000);
    },
    // submit ip config
    onSubmitIp() {
      // disabled submit button
      this.submitIpAble = true;
      editIpConfig(this.ipTemp).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
        }
        // button recover
        this.submitIpAble = false;
      });
    },
  },
};
</script>
<style scoped>
.ant-tabs-tabpane-active {
  height: calc(100vh - 150px);
  overflow-y: scroll;
}
.ip-list-config {
  max-height: calc((100vh - 320px) / 2);
}
textarea {
  resize: none;
}
.btn {
  margin-left: 20px;
}
.config-editor {
  height: calc(100vh - 240px);
  overflow-y: scroll;
}
.ip-config-panel {
  height: calc(100vh - 240px);
  overflow-y: scroll;
}
.ip-config-button {
  position: fixed;
  bottom: 10px;
}
</style>
