<template>
  <div class="node-full-content">
    <!-- <div ref="filter" class="filter"></div> -->
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="false" bordered rowKey="id" @expand="expand">
      <template slot="title">
        <a-alert message="tomcat 管理功能暂时没有计划维护,如需要使用到该功能请多多测试后再使用（建议使用 DSL 模式代替）" style="margin-top: 10px; margin-bottom: 40px" banner />
        <a-space>
          <a-button type="primary" @click="handleAdd">新增</a-button>
          <a-button type="primary" @click="handleFilter">刷新</a-button>
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="path" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" @click="handleLog(record)">日志</a-button>
          <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-button type="primary" @click="handleUploadWar(record)">上传 WAR 文件</a-button>
          <a-button :disabled="record.tomcatStatus !== 0" type="primary" @click="handleStart(record)">启动</a-button>
          <a-button :disabled="record.tomcatStatus === 0" type="danger" @click="handleStop(record)">停止</a-button>
          <a-button :disabled="record.tomcatStatus === 0" type="danger" @click="handleRestart(record)">重启</a-button>
          <a-button type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
      <!-- 嵌套表格 -->
      <a-table
        slot="expandedRowRender"
        slot-scope="text"
        :scroll="{ x: 1380 }"
        :loading="childLoading"
        :columns="childColumns"
        :data-source="text.children"
        :pagination="false"
        :rowKey="(record, index) => record.path + index"
      >
        <a-tooltip slot="path" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="status" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text === "running" ? "运行中" : "未运行" }}</span>
        </a-tooltip>
        <template slot="operation" slot-scope="operation, record">
          <a-space>
            <a-button type="primary" @click="handleProjectFile(record, text)">管理</a-button>
            <a-button :disabled="record.status === 'running'" type="primary" @click="handleProjectCommand(record, text, 'start')">启动</a-button>
            <a-button :disabled="record.status === 'stopped'" type="danger" @click="handleProjectCommand(record, text, 'stop')">停止</a-button>
            <a-button :disabled="record.status === 'stopped'" type="danger" @click="handleProjectCommand(record, text, 'reload')">重启</a-button>
            <a-button type="danger" @click="handleProjectCommand(record, text, 'undeploy')">删除</a-button>
          </a-space>
        </template>
      </a-table>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editTomcatVisible" title="编辑 Tomcat" @ok="handleEditTomcatOk" :maskClosable="false" width="700px">
      <a-form-model ref="editTomcatForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-model-item label="Tomcat 名称" prop="name">
          <a-input v-model="temp.name" placeholder="名称" />
        </a-form-model-item>
        <a-form-model-item label="Tomcat 路径" prop="path">
          <a-input v-model="temp.path" placeholder="Tomcat 路径" />
        </a-form-model-item>
        <a-form-model-item label="Tomcat 端口" prop="port">
          <a-input v-model="temp.port" placeholder="Tomcat 端口" />
        </a-form-model-item>
        <a-form-model-item label="appBase 路径" prop="appBase">
          <a-input v-model="temp.appBase" placeholder="appBase 路径" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- Tomcat 日志组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerLogVisible" @close="onLogClose">
      <tomcat-log v-if="drawerLogVisible" :nodeId="node.id" :tomcatId="temp.id" />
    </a-drawer>
    <!-- 项目文件组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerFileVisible" @close="onFileClose">
      <tomcat-project-file v-if="drawerFileVisible" :nodeId="node.id" :tomcatId="temp.id" :path="temp.projectPath" />
    </a-drawer>
    <!-- 上传文件 -->
    <a-modal v-model="uploadFileVisible" width="300px" title="上传 WAR 文件" :footer="null" :maskClosable="true">
      <a-upload :file-list="uploadFileList" :remove="handleRemove" :before-upload="beforeUpload" :accept="'.war'" multiple>
        <a-button><a-icon type="upload" />选择 WAR 文件</a-button>
      </a-upload>
      <br />
      <a-button type="primary" :disabled="uploadFileList.length === 0" @click="startUpload">开始上传</a-button>
    </a-modal>
  </div>
</template>
<script>
import {
  getTomcatList,
  editTomcat,
  deleteTomcat,
  getTomcatProjectList,
  getTomcatStatus,
  startTomcat,
  stopTomcat,
  restartTomcat,
  doTomcatProjectCommand,
  uploadTomcatWarFile,
} from "../../../../api/node-other";
import TomcatLog from "./tomcat-log";
import TomcatProjectFile from "./tomcat-project-file";
export default {
  components: {
    TomcatLog,
    TomcatProjectFile,
  },
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      childLoading: false,
      loading: false,
      // tableHeight: "70vh",
      list: [],
      temp: {},
      editTomcatVisible: false,
      drawerTitle: "",
      drawerLogVisible: false,
      drawerFileVisible: false,
      // 上传 war 文件相关属性
      fileList: [],
      uploadFileList: [],
      uploadFileVisible: false,
      columns: [
        { title: "Tomcat 名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "Tomcat 路径", dataIndex: "path", ellipsis: true, scopedSlots: { customRender: "path" } },
        { title: "修改时间", dataIndex: "modifyTime", width: 180, ellipsis: true, scopedSlots: { customRender: "modifyTime" } },
        { title: "最后操作人", dataIndex: "modifyUser", width: 150, ellipsis: true, scopedSlots: { customRender: "modifyUser" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 620 },
      ],
      childColumns: [
        { title: "项目路径", dataIndex: "path", width: 200, ellipsis: true, scopedSlots: { customRender: "path" } },
        { title: "运行状态", dataIndex: "status", width: 120, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "Session 个数", dataIndex: "session", width: 150 },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" } },
      ],
      rules: {
        name: [{ required: true, message: "Please input Tomcat name", trigger: "blur" }],
        path: [{ required: true, message: "Please input Tomcat path", trigger: "blur" }],
        port: [{ required: true, message: "Please input Tomcat port", trigger: "blur" }],
        appBase: [{ required: true, message: "Please input appBase path", trigger: "blur" }],
      },
    };
  },
  mounted() {
    // this.calcTableHeight();
    this.handleFilter();
  },
  methods: {
    // 计算表格高度
    // calcTableHeight() {
    //   this.$nextTick(() => {
    //     this.tableHeight = window.innerHeight - this.$refs["filter"].clientHeight - 155;
    //   });
    // },
    // 加载数据
    loadData() {
      this.list = [];
      this.loading = true;
      getTomcatList(this.node.id).then((res) => {
        if (res.code === 200) {
          if (res.data) {
            res.data.forEach((element) => {
              // 默认 tomcat 状态未运行
              element.tomcatStatus = 0;
              const params = {
                nodeId: this.node.id,
                id: element.id,
              };
              getTomcatStatus(params).then((rsp) => {
                if (rsp.code === 200) {
                  // rsp.data === 0 表示 Tomcat 未运行
                  element.tomcatStatus = rsp.data;
                  this.list = [...this.list];
                }
              });
            });
            this.list = res.data;
          }
        }
        this.loading = false;
      });
    },
    // 筛选
    handleFilter() {
      this.loadData();
    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.editTomcatVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record);
      this.editTomcatVisible = true;
    },
    // 提交 Tomcat 数据
    handleEditTomcatOk() {
      // 检验表单
      this.$refs["editTomcatForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.temp.nodeId = this.node.id;
        // 提交数据
        editTomcat(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editTomcatForm"].resetFields();
            this.editTomcatVisible = false;
            this.loadData();
          }
        });
      });
    },
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除 Tomcat 么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            nodeId: this.node.id,
            id: record.id,
          };
          // 删除
          deleteTomcat(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 展开行
    expand(expanded, record) {
      if (expanded) {
        this.loadTomcatProjectList(record);
      }
    },
    // 加载 Tomcat 项目列表
    loadTomcatProjectList(record) {
      // 请求节点状态数据
      this.childLoading = true;
      const params = {
        nodeId: this.node.id,
        id: record.id,
      };
      getTomcatProjectList(params).then((res) => {
        if (res.code === 200) {
          record.children = res.data;
        }
        this.childLoading = false;
      });
    },
    // 查看日志
    handleLog(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `Tomcat 日志(${this.temp.name})`;
      this.drawerLogVisible = true;
    },
    // 关闭日志对话框
    onLogClose() {
      this.drawerLogVisible = false;
    },
    // 启动 Tomcat
    handleStart(record) {
      this.$confirm({
        title: "系统提示",
        content: "确认启动 Tomcat 么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            nodeId: this.node.id,
            id: record.id,
          };
          startTomcat(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 停止 Tomcat
    handleStop(record) {
      this.$confirm({
        title: "系统提示",
        content: "确认停止 Tomcat 么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            nodeId: this.node.id,
            id: record.id,
          };
          stopTomcat(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 重启 Tomcat
    handleRestart(record) {
      this.$confirm({
        title: "系统提示",
        content: "确认重启 Tomcat 么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            nodeId: this.node.id,
            id: record.id,
          };
          restartTomcat(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 执行 Tomcat 项目命令
    handleProjectCommand(record, tomcatRecord, op) {
      const map = {
        start: "启动",
        stop: "停止",
        reload: "重启",
        undeploy: "删除",
      };
      this.$confirm({
        title: "系统提示",
        content: `确认执行【${map[op]}】命令么？`,
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          const params = {
            nodeId: this.node.id,
            id: tomcatRecord.id,
            path: record.path,
            op: op,
          };
          doTomcatProjectCommand(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              // 刷新 Tomcat 项目数据
              this.loadTomcatProjectList(tomcatRecord);
            }
          });
        },
      });
    },
    // 管理 Tomcat 项目
    handleProjectFile(record, tomcatRecord) {
      this.temp = Object.assign(tomcatRecord);
      this.temp.projectPath = record.path;
      this.drawerTitle = `Tomcat 文件管理(${this.temp.name})`;
      this.drawerFileVisible = true;
    },
    // 关闭文件对话框
    onFileClose() {
      this.drawerFileVisible = false;
    },
    // 上传 WAR 文件
    handleUploadWar(record) {
      this.temp = Object.assign({}, record);
      this.uploadFileVisible = true;
    },
    handleRemove(file) {
      const index = this.uploadFileList.indexOf(file);
      const newFileList = this.uploadFileList.slice();
      newFileList.splice(index, 1);
      this.uploadFileList = newFileList;
    },
    beforeUpload(file) {
      this.uploadFileList = [...this.uploadFileList, file];
      return false;
    },
    // 开始上传文件
    startUpload() {
      this.uploadFileList.forEach((file) => {
        const formData = new FormData();
        formData.append("file", file);
        formData.append("nodeId", this.node.id);
        formData.append("id", this.temp.id);
        // 上传文件
        uploadTomcatWarFile(formData).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
          }
        });
      });
      this.uploadFileList = [];
    },
  },
};
</script>
<style scoped>
.filter {
  margin-bottom: 10px;
}
</style>
