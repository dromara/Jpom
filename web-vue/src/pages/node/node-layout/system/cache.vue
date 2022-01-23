<template>
  <div class="node-full-content">
    <a-alert message="请勿手动删除数据目录下面文件,如果需要删除需要提前备份或者已经确定对应文件弃用后才能删除" style="margin-top: 10px; margin-bottom: 40px" banner />
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote">数据目录占用空间：{{ temp.dataSize }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">临时文件占用空间：{{ temp.fileSize }}</span>
        <a-button type="primary" v-if="temp.fileSize !== '0'" class="btn" @click="clear('fileSize')">清空</a-button>
      </a-timeline-item>
      <a-timeline-item v-if="temp.oldJarsSize">
        <span class="layui-elem-quote">旧版程序包占有空间：{{ temp.oldJarsSize }}</span>
        <a-button type="primary" v-if="temp.oldJarsSize !== '0'" class="btn" @click="clear('oldJarsSize')">清空</a-button>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">进程名缓存：{{ temp.pidName }}</span>
        <a-button type="primary" v-if="temp.pidName" class="btn" @click="clear('pidName')">清空</a-button>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">进程端口缓存：{{ temp.pidPort }}</span>
        <a-button v-if="temp.pidPort" type="primary" class="btn" @click="clear('pidPort')">清空</a-button>
      </a-timeline-item>
      <!-- <a-timeline-item>
        <span class="layui-elem-quote">错误进程缓存：{{temp.pidError}}</span>
        <a-button type="primary" class="btn" @click="clear('pidError')">清空</a-button>
      </a-timeline-item> -->
      <a-timeline-item>
        <span class="layui-elem-quote">在读取的日志文件数：{{ temp.readFileOnLineCount }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">插件数：{{ temp.pluginSize || 0 }}</span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">运行中的定时任务</span>
        <a-table rowKey="taskId" :columns="taskColumns" :data-source="taskList" :pagination="false">
          <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
          <a-tooltip slot="time" slot-scope="text" placement="topLeft" :title="parseTime(text)">
            <span>{{ parseTime(text) }}</span>
          </a-tooltip>
        </a-table>
      </a-timeline-item>
    </a-timeline>
  </div>
</template>
<script>
import { getNodeCache, clearCache } from "@/api/system";
import { parseTime } from "@/utils/time";
export default {
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      temp: {},
      taskList: [],
      taskColumns: [
        {
          title: "任务ID",
          dataIndex: "taskId",
          defaultSortOrder: "descend",
          sorter: (a, b) => a.taskId - b.taskId,
          ellipsis: true,
          scopedSlots: { customRender: "tooltip" },
          filters: [
            {
              text: "构建",
              value: "build",
            },
            {
              text: "节点脚本",
              value: "script",
            },
            {
              text: "服务端脚本",
              value: "server_script",
            },
            {
              text: "ssh 脚本",
              value: "ssh_command",
            },
          ],
          onFilter: (value, record) => record.taskId.indexOf(value) === 0,
        },
        {
          title: "cron",
          dataIndex: "cron",
          sorter: (a, b) => a.cron.length - b.cron.length,
          sortDirections: ["descend", "ascend"],
        },
        {
          title: "执行次数",
          dataIndex: "executeCount",
          sortDirections: ["descend", "ascend"],
          sorter: (a, b) => a.executeCount || 0 - b.executeCount || 0,
        },
        {
          title: "成功次数",
          dataIndex: "succeedCount",
          sortDirections: ["descend", "ascend"],
          sorter: (a, b) => a.succeedCount || 0 - b.succeedCount || 0,
        },
        {
          title: "失败次数",
          dataIndex: "failedCount",
          sortDirections: ["descend", "ascend"],
          sorter: (a, b) => a.failedCount || 0 - b.failedCount || 0,
        },
        {
          title: "最后一次执行时间",
          dataIndex: "lastExecuteTime",
          sortDirections: ["descend", "ascend"],
          sorter: (a, b) => a.lastExecuteTime || 0 - b.lastExecuteTime || 0,
          ellipsis: true,
          scopedSlots: { customRender: "time" },
        },
      ],
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    parseTime: parseTime,
    // load data
    loadData() {
      getNodeCache(this.node.id).then((res) => {
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
        nodeId: this.node.id,
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
