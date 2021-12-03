<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <!-- <a-button type="primary" @click="loadData">刷新</a-button> -->
    </div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      :loading="loading"
      :columns="columns"
      :style="{ 'max-height': tableHeight + 'px' }"
      :scroll="{ x: 760, y: tableHeight - 60 }"
      :pagination="this.pagination"
      bordered
      :rowKey="(record, index) => index"
    >
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <!-- <a-button type="danger" @click="handleDelete(record)">删除</a-button> -->
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
import { getWorkSpaceList, editWorkSpace } from "@/api/workspace";
import { parseTime } from "@/utils/time";

export default {
  data() {
    return {
      loading: false,
      tableHeight: "70vh",
      list: [],
      total: 0,
      listQuery: {},
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
        total: this.total,
        current: this.listQuery.page || 1,
        pageSize: this.listQuery.limit || 10,
        pageSizeOptions: ["10", "20", "50", "100"],
        showSizeChanger: true,
        showTotal: (total) => {
          if (total <= this.listQuery.limit) {
            return "";
          }
          return `总计 ${total} 条`;
        },
      };
    },
  },
  created() {
    this.calcTableHeight();
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      getWorkSpaceList().then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
        }
        this.loading = false;
      });
    },
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - this.$refs["filter"].clientHeight - 135;
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
  },
};
</script>
<style scoped>
.filter {
  margin-bottom: 10px;
}

.feature {
  max-height: 400px;
  overflow-y: auto;
}

.ant-btn {
  margin-right: 10px;
}

.box-1 {
  margin-left: 10px;
}

.box-2 {
  margin-left: 40px;
}

.box-3 {
  margin-left: 70px;
}
</style>
