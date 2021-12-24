<template>
  <div class="node-full-content">
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.nodeId" allowClear placeholder="请选择节点" class="search-input-item">
        <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
      </a-select>
      <a-input v-model="listQuery['%name%']" placeholder="名称" allowClear class="search-input-item" />
      <a-tooltip title="按住 Ctr 或者 Alt 键点击按钮快速回到第一页">
        <a-button type="primary" @click="loadData">搜索</a-button>
      </a-tooltip>
      <a-space>
        <a-tooltip>
          <template slot="title">
            <div>脚本模版是存储在节点中的命令脚本用于在线管理一些脚本命令，如初始化软件环境、管理应用程序等</div>

            <div>
              <ul>
                <li>执行时候默认不加载全部环境变量、需要脚本里面自行加载</li>
                <li>命令文件将在 ${jpom插件端数据目录}/script/xxxx.sh 执行</li>
                <li>添加脚本模版需要到节点管理中去添加</li>
              </ul>
            </div>
          </template>
          <a-icon type="question-circle" theme="filled" />
        </a-tooltip>

        <a-tooltip placement="topLeft" title="清除服务端缓存节点所有的脚步模版信息, 需要重新同步">
          <a-icon @click="delAll()" type="delete" />
        </a-tooltip>
      </a-space>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" @change="changePage" :pagination="pagination" bordered rowKey="id">
      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ nodeMap[text] }}</span>
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
        <a-button type="primary" @click="handleExec(record)">执行</a-button>
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editScriptVisible" title="编辑 Script" @ok="handleEditScriptOk" :maskClosable="false" width="1200px">
      <a-form-model ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-form-model-item label="Script 名称" prop="name">
          <a-input v-model="temp.name" placeholder="名称" />
        </a-form-model-item>
        <a-form-model-item label="Script 内容" prop="context">
          <code-editor v-model="temp.context" :options="{ mode: 'shell', tabSize: 2, theme: 'abcdef' }"></code-editor>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 脚本控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <script-console v-if="drawerConsoleVisible" :nodeId="temp.nodeId" :id="temp.id" :scriptId="temp.scriptId" />
    </a-drawer>
  </div>
</template>
<script>
import { getScriptListAll, editScript, deleteScript, itemScript, delAllCache } from "@/api/node-other";
import codeEditor from "@/components/codeEditor";
import { getNodeListAll } from "@/api/node";
import ScriptConsole from "@/pages/node/node-layout/other/script-console";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";
import { parseTime } from "@/utils/time";
export default {
  components: {
    ScriptConsole,codeEditor
  },
  props: {},
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      temp: {},
      nodeMap: {},
      editScriptVisible: false,
      drawerTitle: "",
      drawerConsoleVisible: false,

      columns: [
        // { title: "Script ID", dataIndex: "id", width: 200, ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "节点名称", dataIndex: "nodeId", ellipsis: true, scopedSlots: { customRender: "nodeId" } },
        { title: "修改时间", dataIndex: "modifyTimeMillis", width: 170, ellipsis: true, scopedSlots: { customRender: "modifyTimeMillis" } },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },
        { title: "最后操作人", dataIndex: "lastRunUser", ellipsis: true, scopedSlots: { customRender: "lastRunUser" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 260 },
      ],
      rules: {
        name: [{ required: true, message: "Please input Script name", trigger: "blur" }],
        context: [{ required: true, message: "Please input Script context", trigger: "blur" }],
      },
    };
  },
  computed: {
    pagination() {
      return {
        total: this.listQuery.total,
        current: this.listQuery.page || 1,
        pageSize: this.listQuery.limit || PAGE_DEFAULT_LIMIT,
        pageSizeOptions: PAGE_DEFAULT_SIZW_OPTIONS,
        showSizeChanger: true,
        showTotal: (total) => {
          return PAGE_DEFAULT_SHOW_TOTAL(total, this.listQuery);
        },
      };
    },
  },
  mounted() {
    // this.calcTableHeight();

    getNodeListAll().then((res) => {
      if (res.code === 200) {
        res.data.forEach((item) => {
          this.nodeMap[item.id] = item.name;
        });
      }
      this.loadData();
    });
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getScriptListAll(this.listQuery).then((res) => {
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
    // 修改
    handleEdit(record) {
      itemScript({
        id: record.scriptId,
        nodeId: record.nodeId,
      }).then((res) => {
        this.temp = Object.assign(res.data);
        this.temp.nodeId = record.nodeId;
        //
        this.editScriptVisible = true;
      });
    },
    // 提交 Script 数据
    handleEditScriptOk() {
      // 检验表单
      this.$refs["editScriptForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
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
            nodeId: record.nodeId,
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
      this.temp = Object.assign(record);
      this.drawerTitle = `控制台(${this.temp.name})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭 console
    onConsoleClose() {
      this.drawerConsoleVisible = false;
    },
    delAll() {
      this.$confirm({
        title: "系统提示",
        content: "确定要清除服务端所有的脚步模版缓存信息吗？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          delAllCache().then((res) => {
            if (res.code == 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      if (sorter) {
        this.listQuery.order = sorter.order;
        this.listQuery.order_field = sorter.field;
      }
      this.loadData();
    },
  },
};
</script>
<style scoped>
.filter {
  margin-bottom: 10px;
}
.ant-btn {
  margin-right: 10px;
}
.node-table {
  overflow-x: auto;
}
</style>
