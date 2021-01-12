<template>
  <div>
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote">文件缓存：{{temp.fileSize}}</span>
        <a-button type="primary" class="btn" @click="clear('fileSize')">清空</a-button>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">进程名缓存：{{temp.pidName}}</span>
        <a-button type="primary" class="btn" @click="clear('pidName')">清空</a-button>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">进程端口缓存：{{temp.pidPort}}</span>
        <a-button type="primary" class="btn" @click="clear('pidPort')">清空</a-button>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">错误进程缓存：{{temp.pidError}}</span>
        <a-button type="primary" class="btn" @click="clear('pidError')">清空</a-button>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">在读取的日志文件数：{{temp.readFileOnLineCount}}</span>
      </a-timeline-item>
    </a-timeline>
  </div>
</template>
<script>
import { getNodeCache, clearCache } from '../../../../api/system';
export default {
  props: {
    node: {
      type: Object
    }
  },
  data() {
    return {
      temp: {}
    }
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // load data
    loadData() {
      getNodeCache(this.node.id).then(res => {
        if (res.code === 200) {
          this.temp = res.data;
        }
      })
    },
    // clear
    clear(type) {
      const params = {
        type: type,
        nodeId: this.node.id
      }
      clearCache(params).then(res => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
            duration: 2
          });
          this.loadData();
        }
      })
    }
  }
}
</script>
<style scoped>
.btn {
  margin-left: 20px;
}
</style>