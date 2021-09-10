<template>
  <div>
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote">当前程序打包时间：{{temp.timeStamp}}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">当前版本号：{{temp.version}}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">已经运行时间：{{temp.upTime}}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">端口号：{{temp.port}}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">进程号：{{temp.pid}}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">数据存储目录：{{temp.dataPath}}</span>
      </a-timeline-item>
    </a-timeline>
    <a-spin v-show="!temp.debug" :spinning="spinning">
      <a-upload :file-list="fileList" :remove="handleRemove" :before-upload="beforeUpload" accept=".jar">
        <a-button><a-icon type="upload" />选择升级文件</a-button>
      </a-upload>
      <a-button type="primary" class="upload-btn" :disabled="fileList.length === 0" @click="startUpload">上传升级文件</a-button>
    </a-spin>
  </div>
</template>
<script>
import { systemInfo, uploadUpgradeFile } from '../../api/system';
export default {
  data() {
    return {
      temp: {},
      spinning: false,
      fileList: [],
      timer: null
    }
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      systemInfo().then(res => {
        if (res.code === 200) {
          this.temp = res.data;
        }
      })
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
      formData.append('file', this.fileList[0]);
      // 上传文件
      uploadUpgradeFile(formData).then(res => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
            duration: 2
          });
          this.checkVersion();
        }
        this.spinning = false;
      })
      this.fileList = [];
    },
    // 定时查询版本号
    checkVersion() {
      this.timer = setInterval(() => {
        systemInfo().then(res => {
          if (res.code === 200 && res.data.timeStamp !== this.temp.timeStamp) {
            clearInterval(this.timer);
            this.$notification.success({
              message: '升级成功',
              duration: 2
            });
            this.temp = res.data;
            setTimeout(()=>{
              location.reload();
            },1000);
          }
        }).catch(error => {
          console.log(error);
        })
      }, 2000);
    }
  }
}
</script>
<style scoped>
.upload-btn {
  margin-top: 20px;
}
</style>