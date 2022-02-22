<template>
  <div class="full-content">
    <a-tabs default-active-key="1">
      <a-tab-pane key="1" tab="缓存信息">
        <a-alert message="请勿手动删除数据目录下面文件,如果需要删除需要提前备份或者已经确定对应文件弃用后才能删除" style="margin-top: 10px; margin-bottom: 40px" banner />
        <a-timeline>
          <a-timeline-item>
            <span class="layui-elem-quote">数据目录占用空间：{{ temp.dataSize }} (10分钟刷新一次)</span>
          </a-timeline-item>
          <a-timeline-item v-if="temp.cacheFileSize">
            <a-space>
              <span class="layui-elem-quote">临时文件占用空间：{{ temp.cacheFileSize }} (10分钟刷新一次)</span>
              <a-button size="small" type="primary" v-if="temp.cacheFileSize !== '0'" class="btn" @click="clear('serviceCacheFileSize')">清空</a-button>
            </a-space>
          </a-timeline-item>
          <a-timeline-item>
            <a-space>
              <span class="layui-elem-quote">黑名单 IP 数量：{{ temp.ipSize }}</span>
              <a-button size="small" type="primary" v-if="temp.ipSize" class="btn" @click="clear('serviceIpSize')">清空</a-button>
            </a-space>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote">在读取的日志文件数：{{ temp.readFileOnLineCount }}</span>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote">在线构建文件占用空间：{{ temp.cacheBuildFileSize }} (10分钟刷新一次)</span>
          </a-timeline-item>
          <a-timeline-item v-if="temp.oldJarsSize">
            <a-space>
              <span class="layui-elem-quote">旧版程序包占有空间：{{ temp.oldJarsSize }}</span>
              <a-button size="small" v-if="temp.oldJarsSize !== '0'" type="primary" class="btn" @click="clear('serviceOldJarsSize')">清空</a-button>
            </a-space>
          </a-timeline-item>
          <a-timeline-item>
            <span class="layui-elem-quote">插件数：{{ temp.pluginSize || 0 }}</span>
          </a-timeline-item>
        </a-timeline>
      </a-tab-pane>
      <a-tab-pane key="2" tab="运行中的定时任务" force-render> <task-stat :taskList="taskList" @refresh="loadData" /></a-tab-pane>
    </a-tabs>
  </div>
</template>
<script>
import { getServerCache, clearCache } from "@/api/system";
import TaskStat from "@/pages/system/taskStat";

export default {
  components: {
    TaskStat,
  },
  data() {
    return {
      temp: {},
      taskList: [],
    };
  },
  mounted() {
    this.loadData();
    // console.log(Comparator);
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
