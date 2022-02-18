<template>
  <div class="main">
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote">当前程序打包时间：{{ temp.timeStamp }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">当前前端打包时间：{{ temp.vueTimeStamp }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">当前版本号：{{ temp.version }} </span>
        <template v-if="temp.upgrade !== undefined">
          <a-tag v-if="temp.upgrade" color="pink" @click="upgrageVerion">新版本：{{ temp.newVersion }} </a-tag>
          <a-tag v-else color="orange" @click="checkVersion">
            <a-icon type="rocket" />
          </a-tag>
        </template>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">已经运行时间：{{ temp.upTimeStr }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote"
          >端口号：<a-tag>{{ temp.port }}</a-tag></span
        >
        <span class="layui-elem-quote">&nbsp;&nbsp;</span>
        <span class="layui-elem-quote"
          >进程号：<a-tag>{{ temp.pid }}</a-tag></span
        >
      </a-timeline-item>
      <a-timeline-item>
        <a-alert message="请勿手动删除数据目录下面文件,如果需要删除需要提前备份或者已经确定对应文件弃用后才能删除" type="warning" />
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">
          数据存储目录：
          <a-tag>{{ temp.dataPath }}</a-tag>
        </span>
        <span class="layui-elem-quote" v-if="temp.jarFile">
          运行的Jar包：
          <a-tag>{{ temp.jarFile }}</a-tag>
        </span>
      </a-timeline-item>
    </a-timeline>

    <a-upload :file-list="fileList" :remove="handleRemove" :before-upload="beforeUpload" accept=".jar,.zip">
      <a-button><a-icon type="upload" />选择升级文件</a-button>
    </a-upload>
    <a-button type="primary" class="upload-btn" :disabled="fileList.length === 0" @click="startUpload">上传升级文件</a-button>

    <a-divider dashed />
    <markdown-it-vue class="md-body" :content="changelog" :options="markdownOptions" />
  </div>
</template>
<script>
import { systemInfo, uploadUpgradeFile, changelog, checkVersion, remoteUpgrade } from "@/api/system";
import Vue from "vue";
import { parseTime } from "@/utils/time";
import MarkdownItVue from "markdown-it-vue";
import "markdown-it-vue/dist/markdown-it-vue.css";
import { RESTART_UPGRADE_WAIT_TIME_COUNT } from "@/utils/const";
export default {
  name: "Upgrade",
  components: {
    MarkdownItVue,
  },
  props: {
    nodeId: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      temp: {},

      checkCount: 0,
      fileList: [],
      timer: null,
      changelog: "",
      markdownOptions: {
        markdownIt: {
          linkify: true,
        },
        linkAttributes: {
          attrs: {
            target: "_blank",
            rel: "noopener",
          },
        },
      },
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      systemInfo(this.nodeId).then((res) => {
        this.temp = res.data?.manifest;
        //
        // vueTimeStamp
        this.temp = { ...this.temp, vueTimeStamp: parseTime(this.getMeta("build-time")) };
        //
        changelog(this.nodeId).then((resLog) => {
          this.changelog = resLog.data;
          //
          // res.data.
          this.showVersion(false, res.data?.remoteVersion);
        });
      });
    },
    getMeta(metaName) {
      const metas = document.getElementsByTagName("meta");
      for (let i = 0; i < metas.length; i++) {
        try {
          if (metas[i].getAttribute("name") === metaName) {
            return metas[i].getAttribute("content");
          }
        } catch (e) {
          console.error(e);
        }
      }
      return "";
    },
    // 处理文件移除
    handleRemove(file) {
      const index = this.fileList.indexOf(file);
      const newFileList = this.fileList.slice();
      newFileList.splice(index, 1);
      this.fileList = newFileList;
    },
    // 准备上传文件
    beforeUpload(file) {
      // 只允许上传单个文件
      this.fileList = [file];
      return false;
    },
    // 开始上传文件
    startUpload() {
      const html =
        "确认要上传文件更新到最新版本吗？<ul style='color:red;'>" +
        "<li>上传更新前请阅读更新日志里面的说明和注意事项并且<b>请注意备份数据防止数据丢失！！</b></li>" +
        "<li>上传前请检查包是否完整,否则可能出现更新后无法正常启动的情况！！</li>" +
        "<li>如果升级失败需要手动恢复奥</li>" +
        " </ul>";
      const h = this.$createElement;
      this.$confirm({
        title: "系统提示",
        content: h("div", null, [h("p", { domProps: { innerHTML: html } }, null)]),
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          const formData = new FormData();
          formData.append("file", this.fileList[0]);
          formData.append("nodeId", this.nodeId);
          // 上传文件
          uploadUpgradeFile(formData).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });

              this.startCheckUpgradeStatus(res.msg);
            }
          });
          this.fileList = [];
        },
      });
    },
    startCheckUpgradeStatus(msg) {
      this.checkCount = 0;
      Vue.prototype.$setLoading({
        spinning: true,
        tip: (msg || "升级中，请稍候...") + ",请耐心等待暂时不用刷新页面,升级成功后会自动刷新",
      });
      //
      this.timer = setInterval(() => {
        systemInfo(this.nodeId)
          .then((res) => {
            let manifest = res.data?.manifest;
            if (res.code === 200 && manifest?.timeStamp !== this.temp.timeStamp) {
              clearInterval(this.timer);
              Vue.prototype.$setLoading(false);
              this.$notification.success({
                message: "升级成功",
              });
              this.temp = manifest;
              setTimeout(() => {
                location.reload();
              }, 1000);
            } else {
              if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                this.$notification.warning({
                  message: "未升级成功：" + (res.msg || ""),
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
                message: "升级超时,请去服务器查看控制台日志排查问题",
              });
              clearInterval(this.timer);
            }
          });
        this.checkCount = this.checkCount + 1;
      }, 2000);
    },
    // 检查新版本
    checkVersion() {
      checkVersion(this.nodeId).then((res) => {
        if (res.code === 200) {
          this.showVersion(true, res.data);
        }
      });
    },
    showVersion(tip, data) {
      if (!data) {
        this.temp.upgrade = false;
        if (tip) {
          this.$notification.success({
            message: "没有检查到最新版",
          });
        }
        return;
      }
      this.temp.upgrade = data.upgrade;
      this.temp.newVersion = data.tagName;
      if (this.temp.upgrade && data.changelog) {
        this.changelog = data.changelog;
      }
      if (tip) {
        this.$notification.success({
          message: this.temp.upgrade ? "检测到新版本 " + data.tagName : "没有检查到最新版",
        });
      }
    },
    // 升级
    upgrageVerion() {
      // "确认要升级到最新版本吗？,升级前请阅读更新日志里面的说明和注意事项并且请注意备份数据防止数据丢失！！"
      const html =
        "确认要下载更新最新版本吗？<ul style='color:red;'>" +
        "<li>下载速度根据网速来确定,如果网络不佳下载会较慢</li>" +
        "<li>下载前请阅读更新日志里面的说明和注意事项并且<b>请注意备份数据防止数据丢失！！</b></li>" +
        "<li>如果升级失败需要手动恢复奥</li>" +
        " </ul>";
      const h = this.$createElement;
      this.$confirm({
        title: "系统提示",
        content: h("div", null, [h("p", { domProps: { innerHTML: html } }, null)]),
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          //
          remoteUpgrade(this.nodeId).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });

              this.startCheckUpgradeStatus(res.msg);
            }
          });
        },
      });
    },
  },
};
</script>
<style scoped>
.main {
  background-color: #fff;
  margin: -15px -30px 0 -15px;
  padding: 15px;
}

.upload-btn {
  margin-top: 20px;
}
</style>
