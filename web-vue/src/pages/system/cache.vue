<template>
  <div class="full-content">
    <a-alert message="请勿手动删除数据目录下面文件,如果需要删除需要提前备份或者已经确定对应文件弃用后才能删除" style="margin-top: 10px; margin-bottom: 40px" banner />
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote">数据目录占用空间：{{ temp.dataSize }} (10分钟刷新一次)</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">临时文件占用空间：{{ temp.cacheFileSize }} (10分钟刷新一次)</span>
        <a-button type="primary" class="btn" @click="clear('serviceCacheFileSize')">清空</a-button>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">黑名单 IP 数量：{{ temp.ipSize }}</span>
        <a-button type="primary" class="btn" @click="clear('serviceIpSize')">清空</a-button>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">在读取的日志文件数：{{ temp.readFileOnLineCount }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">在线构建文件占用空间：{{ temp.cacheBuildFileSize }} (10分钟刷新一次)</span>
      </a-timeline-item>
    </a-timeline>
    <a-list v-if="taskList && taskList.length" bordered :data-source="taskList">
      <a-list-item slot="renderItem" slot-scope="item">
        <a-list-item-meta :description="item.taskId">
          <a slot="title"> {{ item.id }}</a>
        </a-list-item-meta>
        <div>
          {{ item.cron }}
        </div>
      </a-list-item>
    </a-list>
  </div>
</template>
<script>
import { getServerCache, clearCache } from "@/api/system";
export default {
  data() {
    return {
      temp: {},
      taskList: [],
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // load data
    loadData() {
      getServerCache().then((res) => {
        if (res.code === 200) {
          this.temp = res.data;
          this.taskList = res.data?.taskList;
        }
      });
    },
    // clear
    clear(type) {
      const params = {
        type: type,
        nodeId: "",
      };
      clearCache(params).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
          this.loadData();
        }
      });
    },
  },
};
</script>
<style scoped>
.btn {
  margin-left: 20px;
}
</style>
