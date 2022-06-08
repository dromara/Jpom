<template>
  <div class="node-full-content">
    <!-- 数据表格 -->
    <a-table size="middle" :data-source="list" :columns="columns" @change="changePage" :pagination="pagination" bordered rowKey="id">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" placeholder="名称" allowClear class="search-input-item" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
          <a-button type="primary" @click="handleUpload">上传文件</a-button>
          <a-tooltip placement="topLeft" title="清除服务端缓存节点所有的脚步模版信息并重新同步">
            <a-icon @click="sync()" type="sync" spin />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip
        slot="modifyTimeMillis"
        slot-scope="text, record"
        :title="`创建时间：${parseTime(record.createTimeMillis)} ${record.modifyTimeMillis ? '修改时间：' + parseTime(record.modifyTimeMillis) : ''}`"
      >
        <span>{{ parseTime(record.modifyTimeMillis) }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleExec(record)">执行</a-button>
          <a-button size="small" :type="`${record.scriptType === 'server-sync' ? '' : 'primary'}`" @click="handleEdit(record)">{{ record.scriptType === "server-sync" ? "查看" : " 编辑" }}</a-button>
          <a-tooltip :title="`${record.scriptType === 'server-sync' ? '服务端分发同步的脚本不能直接删除,需要到服务端去解绑' : '删除'}`">
            <a-button size="small" :disabled="record.scriptType === 'server-sync'" type="danger" @click="handleDelete(record)">删除</a-button>
          </a-tooltip>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editScriptVisible" title="编辑 Script" @ok="handleEditScriptOk" :maskClosable="false" width="80vw">
      <a-form-model ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 18 }">
        <a-alert v-if="this.temp.scriptType === 'server-sync'" message="服务端同步的脚本不能在此修改" banner />
        <a-form-model-item v-if="temp.id" label="ScriptId" prop="id">
          <a-input v-model="temp.id" disabled readOnly />
        </a-form-model-item>
        <a-form-model-item label="Script 名称" prop="name">
          <a-input :maxLength="50" v-model="temp.name" placeholder="名称" />
        </a-form-model-item>
        <a-form-model-item label="Script 内容" prop="context">
          <div style="height: 40vh; overflow-y: scroll">
            <code-editor v-model="temp.context" :options="{ mode: 'shell', tabSize: 2, theme: 'abcdef' }"></code-editor>
          </div>
        </a-form-model-item>
        <a-form-model-item label="默认参数" prop="defArgs">
          <a-input v-model="temp.defArgs" placeholder="默认参数" />
        </a-form-model-item>
        <a-form-model-item label="定时执行" prop="autoExecCron">
          <a-auto-complete v-model="temp.autoExecCron" placeholder="如果需要定时自动执行则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）" option-label-prop="value">
            <template slot="dataSource">
              <a-select-opt-group v-for="group in cronDataSource" :key="group.title">
                <span slot="label">
                  {{ group.title }}
                </span>
                <a-select-option v-for="opt in group.children" :key="opt.title" :value="opt.value"> {{ opt.title }} {{ opt.value }} </a-select-option>
              </a-select-opt-group>
            </template>
          </a-auto-complete>
        </a-form-model-item>
        <a-form-model-item label="描述" prop="description">
          <a-input maxLength="200" v-model="temp.description" type="textarea" :rows="3" style="resize: none" placeholder="详细描述" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 脚本控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <script-console v-if="drawerConsoleVisible" :nodeId="node.id" :defArgs="temp.defArgs" :id="temp.id" :scriptId="temp.scriptId" />
    </a-drawer>
    <!-- 上传文件 -->
    <a-modal v-model="uploadFileVisible" width="300px" title="上传 bat|bash 文件" :footer="null" :maskClosable="true">
      <a-alert message=" 导入文件将自动安装文件名去重、如果已经存在的将自动覆盖内容" banner />
      <br />
      <a-upload :file-list="uploadFileList" :remove="handleRemove" :before-upload="beforeUpload" :accept="'.bat,.sh'">
        <a-button><a-icon type="upload" />选择 bat|bash 文件</a-button>
      </a-upload>
      <br />
      <a-button type="primary" :disabled="uploadFileList.length === 0" @click="startUpload">开始上传</a-button>
    </a-modal>
  </div>
</template>
<script>
import { getScriptList, editScript, deleteScript, uploadScriptFile, itemScript, syncScript } from "@/api/node-other";
import codeEditor from "@/components/codeEditor";
import ScriptConsole from "./script-console";
import { CRON_DATA_SOURCE, COMPUTED_PAGINATION, CHANGE_PAGE, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";
import { parseTime } from "@/utils/time";
export default {
  components: {
    ScriptConsole,
    codeEditor,
  },
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      temp: {},
      cronDataSource: CRON_DATA_SOURCE,
      editScriptVisible: false,
      drawerTitle: "",
      drawerConsoleVisible: false,
      // 上传脚本文件相关属性
      fileList: [],
      uploadFileList: [],
      uploadFileVisible: false,
      columns: [
        // { title: "Script ID", dataIndex: "id", width: 200, ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "定时执行", dataIndex: "autoExecCron", ellipsis: true, scopedSlots: { customRender: "autoExecCron" } },
        { title: "修改时间", dataIndex: "modifyTimeMillis", width: 180, ellipsis: true, scopedSlots: { customRender: "modifyTimeMillis" } },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },
        { title: "最后操作人", dataIndex: "lastRunUser", ellipsis: true, scopedSlots: { customRender: "lastRunUser" } },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 180 },
      ],
      rules: {
        name: [{ required: true, message: "Please input Script name", trigger: "blur" }],
        context: [{ required: true, message: "Please input Script context", trigger: "blur" }],
      },
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  mounted() {
    // this.calcTableHeight();
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getScriptList({
        nodeId: this.node.id,
      }).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    parseTime(v) {
      return parseTime(v);
    },
    // 添加
    handleAdd() {
      this.temp = {
        type: "add",
      };
      this.editScriptVisible = true;
    },
    // 修改
    handleEdit(record) {
      itemScript({
        id: record.scriptId,
        nodeId: this.node.id,
      }).then((res) => {
        this.temp = Object.assign({}, res.data);
        //
        this.editScriptVisible = true;
      });
    },
    // 提交 Script 数据
    handleEditScriptOk() {
      if (this.temp.scriptType === "server-sync") {
        this.$notification.warning({
          message: "服务端同步的脚本不能在此修改",
        });
        return;
      }
      // 检验表单
      this.$refs["editScriptForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.temp.nodeId = this.node.id;
        // 提交数据
        editScript(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });

            this.editScriptVisible = false;
            this.loadData();
            this.$refs["editScriptForm"].resetFields();
          }
        });
      });
    },
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除脚本么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            nodeId: this.node.id,
            id: record.scriptId,
          };
          // 删除
          deleteScript(params).then((res) => {
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
    // 执行 Script
    handleExec(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `控制台(${this.temp.name})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭 console
    onConsoleClose() {
      this.drawerConsoleVisible = false;
    },
    // 准备上传文件
    handleUpload(record) {
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
        // 上传文件
        uploadScriptFile(formData).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
          }
        });
      });
      setTimeout(() => {
        this.loadData();
      }, 2000);
      this.uploadFileList = [];
    },

    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
    sync() {
      syncScript({
        nodeId: this.node.id,
      }).then((res) => {
        if (res.code == 200) {
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
<style scoped></style>
