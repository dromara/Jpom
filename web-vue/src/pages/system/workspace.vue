<template>
  <div class="full-content">
    <div ref="filter" class="filter">
      <a-input-search placeholder="搜索工作空间" enter-button @search="search" class="search" />

      <a-button type="primary" @click="handleAdd">新增</a-button>
      <!-- <a-button type="primary" @click="loadData">刷新</a-button> -->
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="this.pagination" bordered @change="changePage" :rowKey="(record, index) => index">
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editVisible" title="编辑工作空间" @ok="handleEditRoleOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="temp.name" placeholder="工作空间名称" />
        </a-form-model-item>

        <a-form-model-item label="描述" prop="description">
          <a-input v-model="temp.description" type="textarea" :rows="5" placeholder="工作空间描述" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { getWorkSpaceList, editWorkSpace, deleteWorkspace } from "@/api/workspace";
import { parseTime } from "@/utils/time";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";

export default {
  data() {
    return {
      loading: false,
      list: [],

      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editVisible: false,
      temp: {},
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true },
        { title: "描述", dataIndex: "description", ellipsis: true },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          customRender: (text) => {
            if (!text) {
              return "";
            }
            return parseTime(text);
          },
          sorter: true,
          width: 180,
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: "300px" },
      ],
      // 表单校验规则
      rules: {
        name: [{ required: true, message: "请输入工作空间名称", trigger: "blur" }],
        description: [{ required: true, message: "请输入工作空间描述", trigger: "blur" }],
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
  created() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      getWorkSpaceList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    handleAdd() {
      this.temp = {};
      this.editVisible = true;
    },
    handleEdit(record) {
      this.temp = record;
      this.editVisible = true;
    },
    handleEditRoleOk() {
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        editWorkSpace(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2,
            });
            this.$refs["editForm"].resetFields();
            this.editVisible = false;
            this.loadData();
          }
        });
      });
    },
    search(value) {
      this.listQuery["%name%"] = value;
      this.loadData();
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
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的当前工作空间么,删除前需要将关联数据都删除后才能删除当前工作空间？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteWorkspace(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2,
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
<style scoped>
.filter {
  margin-bottom: 10px;
}

.ant-btn {
  margin-left: 10px;
}

.search {
  width: 200px;
}
</style>
