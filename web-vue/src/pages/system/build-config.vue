<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" :columns="columns" :pagination="false" size="middle" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-button type="primary" @click="handleAdd">新增</a-button>
        </a-space>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editVisible" title="编辑runs" width="900px" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="runs 名称" />
        </a-form-model-item>
        <a-form-model-item prop="content">
          <template slot="label">
            Dockerfile
            <a-tooltip v-show="!temp.id">
              <template slot="title"> 基础运行环境 Dockerfile</template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <div style="height: 40vh; overflow-y: scroll">
            <code-editor v-model="temp.content" :options="{ mode: 'dockerfile', tabSize: 2, theme: 'abcdef' }"></code-editor>
          </div>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import codeEditor from "@/components/codeEditor";
import { deleteRuns, getRuns, updateRuns } from "@/api/build-config";

export default {
  components: {
    codeEditor,
  },
  data() {
    return {
      loading: false,
      list: [],
      editVisible: false,
      temp: {},
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "路径", dataIndex: "path", ellipsis: true, scopedSlots: { customRender: "path" } },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 180 },
      ],
      // 表单校验规则
      rules: {
        name: [{ required: true, message: "请输入名称", trigger: "blur" }],
        content: [{ required: true, message: "请输入内容", trigger: "blur" }],
      },
    };
  },
  computed: {},
  created() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      getRuns().then((res) => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      });
    },
    handleAdd() {
      this.temp = {};
      this.editVisible = true;
      this.$refs["editForm"] && this.$refs["editForm"].resetFields();
    },
    handleEdit(record) {
      this.temp = record;
      this.editVisible = true;
      this.$refs["editForm"] && this.$refs["editForm"].resetFields();
    },

    handleEditOk() {
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        updateRuns(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });

            this.editVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "删除后，容器构建将无法使用这个作为基础构建环境",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteRuns(record.name).then((res) => {
            if (res.code === 200) {
              // 成功
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
  },
};
</script>
<style scoped></style>
