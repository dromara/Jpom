<template>
  <div>
    <div ref="model-filter" class="filter">
      <a-space direction="vertical">
        <a-tag>{{$t('node.node_layout.project.log.closeBackupFunc')}}<b>jpom.project.log.auto-backup-to-file: false</b></a-tag>

        <a-tag color="orange" v-if="project.logPath">
          {{$t('node.node_layout.project.log.consoleLogDir')}} {{ project.logPath }}
          <template v-if="project.logSize">
            {{$t('node.node_layout.project.log.currentLogSize')}}{{ project.logSize }}
            <a-button @click="handleDownload" type="link" icon="download" size="small"> {{$t('common.export')}} </a-button>
          </template>
        </a-tag>

        <a-tag color="orange" v-if="project.logBackPath">{{$t('node.node_layout.project.log.consoleBackupDir')}} {{ project.logBackPath }}</a-tag>
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
          <a-button type="primary" @click="handleDownloadLogback(record)">{{$t('common.download')}}</a-button>
          <a-button type="danger" @click="handleDelete(record)">{{$t('common.delete')}}</a-button>
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
    copyId: {
      type: String,
    },
  },
  data() {
    return {
      loading: true,
      project: {},
      logBackList: [],
      columns: [
        { title: this.$t('common.fileName'), dataIndex: "filename", width: 150, ellipsis: true, scopedSlots: { customRender: "filename" } },
        { title: this.$t('common.modifyTime'), dataIndex: "modifyTime", width: 150, ellipsis: true, scopedSlots: { customRender: "modifyTime" } },
        { title: this.$t('common.fileSize'), dataIndex: "fileSize", width: 100, ellipsis: true, scopedSlots: { customRender: "fileSize" } },
        { title: this.$t('common.operation'), dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 130 },
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
        copyId: this.copyId,
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
        copyId: this.copyId,
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
        message: this.$t('node.node_layout.project.file.downloading'),
      });
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        copyId: this.copyId,
      };
      // 请求接口拿到 blob
      window.open(downloadProjectLogFile(params), "_blank");
    },
    // 下载日志备份文件
    handleDownloadLogback(record) {
      this.$notification.info({
        message: this.$t('node.node_layout.project.file.downloading'),
      });
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        copyId: this.copyId,
        key: record.filename,
      };
      // 请求接口拿到 blob
      window.open(downloadProjectLogBackFile(params), "_blank");
    },
    // 删除日志备份文件
    handleDelete(record) {
      this.$confirm({
        title: this.$t('common.systemPrompt'),
        content: this.$t('common.deleteFile'),
        okText: this.$t('common.confirm'),
        cancelText: this.$t('common.cancel'),
        onOk: () => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,
            copyId: this.copyId,
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
