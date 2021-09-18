<template>
  <div class="main">
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote">当前程序打包时间：{{ temp.timeStamp }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">当前版本号：{{ temp.version }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">已经运行时间：{{ temp.upTime }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">端口号：{{ temp.port }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">进程号：{{ temp.pid }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">数据存储目录：{{ temp.dataPath }}</span>
      </a-timeline-item>
    </a-timeline>
    <a-spin v-show="!temp.debug" :spinning="spinning">
      <a-upload :file-list="fileList" :remove="handleRemove" :before-upload="beforeUpload" accept=".jar,.zip">
        <a-button><a-icon type="upload" />选择升级文件</a-button>
      </a-upload>
      <a-button type="primary" class="upload-btn" :disabled="fileList.length === 0" @click="startUpload">上传升级文件</a-button>
    </a-spin>
    <div style="margin-top: 20px">
      <markdown-it-vue class="md-body" :content="changelog" :options="markdownOptions" />
    </div>
  </div>
</template>
<script>
import { systemInfo, uploadUpgradeFile, changelog } from "@/api/system";
import MarkdownItVue from "markdown-it-vue";
import "markdown-it-vue/dist/markdown-it-vue.css";
export default {
  components: {
    MarkdownItVue,
  },
  data() {
    return {
      temp: {},
      spinning: false,
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
      systemInfo().then((res) => {
        if (res.code === 200) {
          this.temp = res.data;
        }
      });
      changelog().then((res) => {
        console.log(res);
        this.changelog = res.data;
      });
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
      this.spinning = true;
      const formData = new FormData();
      formData.append("file", this.fileList[0]);
      // 上传文件
      uploadUpgradeFile(formData).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
            duration: 2,
          });
          this.checkVersion();
        }
        this.spinning = false;
      });
      this.fileList = [];
    },
    // 定时查询版本号
    checkVersion() {
      this.timer = setInterval(() => {
        systemInfo()
          .then((res) => {
            if (res.code === 200 && res.data.timeStamp !== this.temp.timeStamp) {
              clearInterval(this.timer);
              this.$notification.success({
                message: "升级成功",
                duration: 2,
              });
              this.temp = res.data;
              setTimeout(() => {
                location.reload();
              }, 1000);
            }
          })
          .catch((error) => {
            console.log(error);
          });
      }, 2000);
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
