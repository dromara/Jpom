<template>
  <div>
    <div ref="model-filter" class="filter">
      <a-space direction="vertical">
        <a-tag>控制台日志只是启动项目输出的日志信息,并非项目日志。可以关闭控制台日志备份功能：<b>jpom.project.log.auto-backup-to-file: false</b></a-tag>

        <a-tag color="orange" v-if="project.logPath">
          控制台日志路径: {{ project.logPath }}
          <template v-if="project.logSize">
            当前日志文件大小：{{ project.logSize }}
            <a-button @click="handleDownload" type="link" icon="download" size="small"> 导出 </a-button>
          </template>
        </a-tag>

        <a-tag color="orange" v-if="project.logBackPath">控制台日志备份路径: {{ project.logBackPath }}</a-tag>
      </a-space>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="logBackList" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
      <a-tooltip slot="filename" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="fileSize" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" @click="handleDownloadLogback(record)">下载</a-button>
          <a-button type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
  </div>
</template>
<script>
import { getLogBackList, deleteProjectLogBackFile, downloadProjectLogBackFile, getProjectLogSize, downloadProjectLogFile } from "@/api/node-project";
export default {
  props: {
    nodeId: {
      type: String,
    },
    projectId: {
      type: String,
    },
  },
  data() {
    return {
      loading: true,
      project: {},
      logBackList: [],
      columns: [
        { title: "文件名称", dataIndex: "filename", width: 150, ellipsis: true, scopedSlots: { customRender: "filename" } },
        { title: "修改时间", dataIndex: "modifyTime", width: 150, ellipsis: true, scopedSlots: { customRender: "modifyTime" } },
        { title: "文件大小", dataIndex: "fileSize", width: 100, ellipsis: true, scopedSlots: { customRender: "fileSize" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 130 },
      ],
    };
  },
  mounted() {
    this.loadFileSize();
    this.loadData();
  },
  methods: {
    // 加载日志文件大小
    loadFileSize() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
      };
      getProjectLogSize(params).then((res) => {
        if (res.code === 200) {
          this.project = { ...this.project, logSize: res.data.logSize };
        }
      });
    },
    loadData() {
      this.loading = true;
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
      };
      getLogBackList(params).then((res) => {
        if (res.code === 200) {
          this.logBackList = res.data.array;
          this.project = { ...this.project, logPath: res.data.logPath, logBackPath: res.data.logBackPath };
        }
        this.loading = false;
      });
    },
    // 下载日志文件
    handleDownload() {
      this.$notification.info({
        message: "正在下载，请稍等...",
      });
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
      };
      // 请求接口拿到 blob
      window.open(downloadProjectLogFile(params), "_blank");
    },
    // 下载日志备份文件
    handleDownloadLogback(record) {
      this.$notification.info({
        message: "正在下载，请稍等...",
      });
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,

        key: record.filename,
      };
      // 请求接口拿到 blob
      window.open(downloadProjectLogBackFile(params), "_blank");
    },
    // 删除日志备份文件
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除文件么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,

            name: record.filename,
          };
          // 删除
          deleteProjectLogBackFile(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.handleLogBack();
            }
          });
        },
      });
    },
  },
};
</script>
