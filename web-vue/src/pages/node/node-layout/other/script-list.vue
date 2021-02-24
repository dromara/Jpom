<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleFilter">刷新</a-button>
      <a-button type="primary" @click="handleAdd">新增</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="false" bordered rowKey="id" class="node-table">
      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleExec(record)">执行</a-button>
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editScriptVisible" title="编辑 Script" @ok="handleEditScriptOk" :maskClosable="false" width="700px">
      <a-form-model ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-model-item label="Script 名称" prop="name">
          <a-input v-model="temp.name" placeholder="名称"/>
        </a-form-model-item>
        <a-form-model-item label="Script 内容" prop="context">
          <a-input v-model="temp.context" type="textarea" :rows="10" style="resize: none" placeholder="Script 内容"/>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 项目控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw"
      :visible="drawerConsoleVisible" @close="onConsoleClose">
      <script-console v-if="drawerConsoleVisible" :nodeId="node.id" :scriptId="temp.id" />
    </a-drawer>
  </div>
</template>
<script>
import { getScriptList, editScript, deleteScript } from '../../../../api/node-other';
import ScriptConsole from './script-console';
export default {
  components: {
    ScriptConsole
  },
  props: {
    node: {
      type: Object
    }
  },
  data() {
    return {
      loading: false,
      list: [],
      temp: {},
      editScriptVisible: false,
      drawerTitle: '',
      drawerConsoleVisible: false,
      columns: [
        {title: 'Script ID', dataIndex: 'id', width: 200, ellipsis: true, scopedSlots: {customRender: 'id'}},
        {title: 'Script 名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '修改时间', dataIndex: 'modifyTime', width: 180, ellipsis: true, scopedSlots: {customRender: 'modifyTime'}},
        {title: '最后操作人', dataIndex: 'lastRunUser', width: 150, ellipsis: true, scopedSlots: {customRender: 'lastRunUser'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 260}
      ],
      rules: {
        name: [
          { required: true, message: 'Please input Script name', trigger: 'blur' }
        ],
        context: [
          { required: true, message: 'Please input Script context', trigger: 'blur' }
        ]
      }
    }
  },
  mounted() {
    this.handleFilter();
  },
  methods: {
    // 加载数据
    loadData() {
      this.list = [];
      this.loading = true;
      getScriptList(this.node.id).then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 筛选
    handleFilter() {
      this.loadData();
    },
    // 添加
    handleAdd() {
      this.temp = {
        type: 'add'
      };
      this.editScriptVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign(record);
      this.editScriptVisible = true;
    },
    // 提交 Script 数据
    handleEditScriptOk() {
       // 检验表单
      this.$refs['editScriptForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.temp.nodeId = this.node.id;
        // 提交数据
        editScript(this.temp).then(res => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            this.$refs['editScriptForm'].resetFields();
            this.editScriptVisible = false;
            this.loadData();
          }
        })
      })
    },
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除脚本么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 组装参数
          const params = {
            nodeId: this.node.id,
            id: record.id
          }
          // 删除
          deleteScript(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              this.loadData();
            }
          })
        }
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
    }
  }
}
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