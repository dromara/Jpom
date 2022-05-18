<template>
  <div class="node-full-content">
    <!-- <div ref="filter" class="filter"></div> -->
    <!-- 数据表格 -->
    <a-alert message="jdk 管理功能暂时没有计划维护（建议使用 DSL 模式代替）" style="margin-top: 10px; margin-bottom: 40px" banner />
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-button type="primary" @click="handleAdd">新增</a-button>
          <a-button type="primary" @click="loadData">刷新</a-button>
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="version" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="path" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-button type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editJdkVisible" title="编辑 JDK" @ok="handleEditJdkOk" :maskClosable="false">
      <a-form-model ref="editJdkForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="temp.name" placeholder="名称" />
        </a-form-model-item>
        <a-form-model-item label="JDK 路径" prop="path">
          <a-input v-model="temp.path" placeholder="JDK 路径" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { getJdkList, editJdk, deleteJdk } from "../../../../api/node-project";
export default {
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      loading: false,
      tableHeight: "70vh",
      list: [],
      temp: {},
      editJdkVisible: false,
      columns: [
        { title: "项目名称", dataIndex: "name", width: 150, ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "版本", dataIndex: "version", width: 170, ellipsis: true, scopedSlots: { customRender: "version" } },
        { title: "路径", dataIndex: "path", width: 170, ellipsis: true, scopedSlots: { customRender: "path" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 200 },
      ],
      rules: {
        name: [{ required: true, message: "Please input JDK name", trigger: "blur" }],
        path: [{ required: true, message: "Please input JDK path", trigger: "blur" }],
      },
    };
  },
  created() {
    // this.calcTableHeight();
    this.loadData();
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
      this.loading = true;
      getJdkList(this.node.id).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      });
    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.editJdkVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record);
      this.editJdkVisible = true;
    },
    // 提交 JDK 数据
    handleEditJdkOk() {
      // 检验表单
      this.$refs["editJdkForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.temp.nodeId = this.node.id;
        // 提交数据
        editJdk(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editJdkForm"].resetFields();
            this.editJdkVisible = false;
            this.loadData();
          }
        });
      });
    },
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除 JDK 么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            nodeId: this.node.id,
            id: record.id,
          };
          // 删除
          deleteJdk(params).then((res) => {
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
  },
};
</script>
<style scoped></style>
