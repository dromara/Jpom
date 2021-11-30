<template>
  <div>
    <div ref="filter" class="filter">
      <a-input v-model="listQuery.name" placeholder="请输入备份名称" class="filter-item"/>
      <a-select v-model="listQuery.backupType" allowClear placeholder="请选择备份类型" class="filter-item" @change="handleFilter">
        <a-select-option v-for="backupType in backupTypeList" :key="backupType.key">{{ backupType.value }}</a-select-option>
      </a-select>
      <a-button type="primary" @click="handleFilter">搜索</a-button>
      <a-button type="primary" @click="handleAdd">创建备份</a-button>
      <a-button type="primary" @click="handleAdd">导入备份</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 表格 -->
    <a-table
      :loading="loading"
      :columns="columns"
      :data-source="list"
      :style="{ 'max-height': tableHeight + 'px' }"
      :scroll="{ x: 1210, y: tableHeight - 60 }"
      bordered
      rowKey="id"
      :pagination="false"
    >
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="backupType" slot-scope="text" placement="topleft" :title="text">
        <span>{{ backupTypeMap[text] }}</span>
      </template>
      <a-tooltip slot="fileSize" slot-scope="text" placement="topLeft" :title="text">
        <a-tag v-if="text" color="#108ee9">{{ text }}</a-tag>
        <a-tag v-else color="#108ee9"> 0 </a-tag>
      </a-tooltip>
      <a-tooltip slot="status" slot-scope="text" placement="topLeft" :title="backupStatusMap[text]">
        <span>{{ backupStatusMap[text] }}</span>
      </a-tooltip>
      <a-tooltip slot="sha1Sum" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="filePath" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
        <a-button type="warning" :disabled="record.status !== 1" @click="handleRestore(record)">还原备份</a-button>
      </template>
    </a-table>
    <!-- 创建备份信息区 -->
    <a-modal v-model="createBackupVisible" title="创建备份信息" @ok="handleCreateBackupOk" width="600px" :maskClosable="false">
      <a-form-model ref="editBackupForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="备份类型" prop="backupType">
          <a-radio-group v-model="temp.backupType" name="backupType">
            <a-radio :value="0">全量备份</a-radio>
            <a-radio :value="1">部分备份</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <!-- 部分备份 -->
        <a-form-model-item v-if="temp.backupType === 1" label="勾选数据表" prop="tableNameList" class="feature jpom-role">
          <a-transfer
            :data-source="tableNameList"
            show-search
            :filter-option="filterOption"
            :target-keys="targetKeys"
            :render="item => item.title"
            @change="handleChange"
          />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 触发器 -->
    <a-modal v-model="triggerVisible" title="触发器" :footer="null" :maskClosable="false">
      <a-form-model ref="editTriggerForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-model-item label="触发器地址" prop="triggerBuildUrl">
          <a-input v-model="temp.triggerBuildUrl" type="textarea" readOnly :rows="3" style="resize: none" placeholder="触发器地址" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { mapGetters } from "vuex";
import { getBackupList, getTableNameList, createBackup, deleteBackup, restoreBackup, backupTypeMap, backupStatusMap } from "../../api/backup-info";
import { parseTime } from "../../utils/time";

export default {
  components: {
  },
  data() {
    return {
      backupTypeMap: backupTypeMap,
      backupStatusMap: backupStatusMap,
      loading: false,
      listQuery: {},
      tableHeight: "70vh",
      backupTypeList: [
        {key: 0, value: '全量'},
        {key: 1, value: '部分'}
      ],
      list: [],
      tableNameList: [],
      targetKeys: [],
      temp: {},
      createBackupVisible: false,
      columns: [
        { title: "备份名称", dataIndex: "name", width: 150, ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "备份类型", dataIndex: "backupType", width: 150, ellipsis: true, scopedSlots: { customRender: "backupType" } },
        {
          title: "文件大小",
          dataIndex: "fileSize",
          width: 100,
          ellipsis: true,
          scopedSlots: { customRender: "fileSize" },
        },
        { title: "状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        {
          title: "SHA1",
          dataIndex: "sha1Sum",
          width: 80,
          ellipsis: true,
          scopedSlots: { customRender: "sha1Sum" },
        },
        {
          title: "文件地址",
          dataIndex: "filePath",
          width: 150,
          ellipsis: true,
          scopedSlots: { customRender: "filePath" },
        },
        {
          title: "备份时间",
          dataIndex: "createTimeMillis",
          customRender: (text) => {
            if (!text) {
              return "";
            }
            return parseTime(text);
          },
          width: 180,
        },
        {
          title: "操作",
          dataIndex: "operation",
          width: 280,
          scopedSlots: { customRender: "operation" },
          align: "left",
          fixed: "right",
        },
      ],
      rules: {
        name: [{ required: true, message: "Please input build name", trigger: "blur" }],
        script: [{ required: true, message: "Please input build script", trigger: "blur" }],
        resultDirFile: [{ required: true, message: "Please input build target path", trigger: "blur" }],
        releasePath: [{ required: true, message: "Please input release path", trigger: "blur" }],
      },
    };
  },
  computed: {
    ...mapGetters(["getGuideFlag"]),
  },
  created() {
    this.calcTableHeight();
    this.handleFilter();
  },
  methods: {
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - this.$refs["filter"].clientHeight - 135;
      });
    },
    // 加载数据
    loadData() {
      this.list = [];
      this.loading = true;
      getBackupList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      });
    },
    // 加载数据库表名列表
    loadTableNameList() {
      this.tableNameList = [];
      getTableNameList().then((res) => {
        if (res.code === 200) {
          res.data.forEach(element => {
            this.tableNameList.push({key: element, title: element});
          });
        }
      });
    },
    // 筛选
    handleFilter() {
      this.loadData();
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.title.indexOf(inputValue) > -1;
    },
    // 穿梭框 change
    handleChange(targetKeys) {
      this.targetKeys = targetKeys;
    },
    // 创建备份
    handleAdd() {
      this.targetKeys = [];
      this.temp = {
        backupType: 0,
      }
      this.loadTableNameList();
      this.createBackupVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign(record);
      this.temp.tempGroup = "";
      // 设置发布方式的数据
      if (record.releaseMethodDataId) {
        if (record.releaseMethod === 1) {
          this.temp.releaseMethodDataId_1 = record.releaseMethodDataId;
        }
        if (record.releaseMethod === 2) {
          this.temp.releaseMethodDataIdList = record.releaseMethodDataId.split(":");
        }
        if (record.releaseMethod === 3) {
          this.temp.releaseMethodDataId_3 = record.releaseMethodDataId;
        }
      }
      this.editBuildVisible = true;
    },
    // 提交节点数据
    handleCreateBackupOk() {
      // 检验表单
      this.$refs["editBackupForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
        createBackup(this.targetKeys).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2,
            });
            this.$refs["editBackupForm"].resetFields();
            this.createBackupVisible = false;
            this.handleFilter();
          }
        });
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除备份信息么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteBackup(record.id).then((res) => {
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
    // 还原备份
    handleRestore(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要还原备份信息么？还原过程中不能操作哦...",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 还原
          restoreBackup(record.id).then((res) => {
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
    }
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

.filter-item {
  width: 150px;
  margin-right: 10px;
}

.btn-add {
  margin-left: 10px;
  margin-right: 0;
}
</style>
