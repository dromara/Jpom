<template>
  <div>
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote">服务端文件缓存：{{temp.cacheFileSize}} (10分钟刷新一次)</span>
        <a-button type="primary" class="btn" @click="clear('serviceCacheFileSize')">清空</a-button>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">黑名单 IP 数量：{{temp.ipSize}}</span>
        <a-button type="primary" class="btn" @click="clear('serviceIpSize')">清空</a-button>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">在读取的日志文件数：{{temp.readFileOnLineCount}}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">在线构建文件占用空间：{{temp.cacheBuildFileSize}} (10分钟刷新一次)</span>
      </a-timeline-item>
    </a-timeline>
  </div>
</template>
<script>
import { getServerCache, clearCache } from '../../api/system';
export default {
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
      getServerCache().then(res => {
        if (res.code === 200) {
          this.temp = res.data;
        }
      })
    },
    // clear
    clear(type) {
      const params = {
        type: type,
        nodeId: ''
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
