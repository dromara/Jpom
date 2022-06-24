<template>
  <div class="full-content">
    <!-- 表格 -->
    <a-table size="middle" :columns="columns" :data-source="list" bordered rowKey="id" @change="changePage" :pagination="pagination">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="请输入备份名称" class="search-input-item" />
          <a-input v-model="listQuery['%version%']" @pressEnter="loadData" placeholder="请输入版本" class="search-input-item" />
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="listQuery.backupType"
            allowClear
            placeholder="请选择备份类型"
            class="search-input-item"
          >
            <a-select-option v-for="backupType in backupTypeList" :key="backupType.key">{{ backupType.value }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">创建备份</a-button>
          <a-button type="primary" @click="handleSqlUpload">导入备份</a-button>
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="backupType" slot-scope="text" placement="topleft" :title="text">
        <span>{{ backupTypeMap[text] }}</span>
      </template>
      <template slot="baleTimeStamp" slot-scope="text">
        <a-tooltip placement="topLeft" :title="`${parseTime(text)}`"> {{ parseTime(text) }} </a-tooltip>
      </template>
      <a-tooltip slot="status" slot-scope="text, reocrd" placement="topLeft" :title="`${backupStatusMap[text]} 点击复制文件路径`">
        <div
          v-clipboard:copy="reocrd.filePath"
          v-clipboard:success="
            () => {
              tempVue.prototype.$notification.success({
                message: '复制成功',
              });
            }
          "
          v-clipboard:error="
            () => {
              tempVue.prototype.$notification.error({
                message: '复制失败',
              });
            }
          "
        >
          {{ backupStatusMap[text] }}
          <a-icon type="copy" />
        </div>
      </a-tooltip>
      <a-tooltip slot="fileSize" slot-scope="text, reocrd" placement="topLeft" :title="renderSizeFormat(text) + ' ' + reocrd.sha1Sum">
        <a-tag color="#108ee9">{{ renderSizeFormat(text) }}</a-tag>
      </a-tooltip>
      <!-- <a-tooltip slot="filePath" slot-scope="text, record" placement="topLeft" :title="text + ' ' + (record.sha1Sum || '')">
        <span

          >{{ text }}
        </span>
      </a-tooltip> -->
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleDownload(record)">下载</a-button>
          <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
          <a-button size="small" type="danger" :disabled="record.status !== 1" @click="handleRestore(record)">还原</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 创建备份信息区 -->
    <a-modal v-model="createBackupVisible" title="创建备份信息" @ok="handleCreateBackupOk" width="600px" :maskClosable="false">
      <a-form-model ref="editBackupForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item label="备份类型" prop="backupType">
          <a-radio-group v-model="temp.backupType" name="backupType">
            <a-radio v-for="item in backupTypeList" v-show="!item.disabled" :key="item.key" :value="item.key">{{ item.value }}</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <!-- 部分备份 -->
        <a-form-model-item v-if="temp.backupType === 1" label="勾选数据表" prop="tableNameList" class="feature jpom-role">
          <a-transfer :data-source="tableNameList" show-search :filter-option="filterOption" :target-keys="targetKeys" :render="(item) => item.title" @change="handleChange" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 上传 SQL 备份文件 -->
    <a-modal v-model="uploadSqlFileVisible" width="300px" title="上传 SQL 文件" :footer="null" :maskClosable="true">
      <a-upload :file-list="uploadFileList" :remove="handleSqlRemove" :before-upload="beforeSqlUpload" accept=".sql">
        <a-button><a-icon type="upload" />选择 SQL 文件</a-button>
      </a-upload>
      <!-- <br />
      <a-radio-group v-model="backupType" name="backupType">
        <a-radio :value="0">全量备份</a-radio>
        <a-radio :value="1">部分备份</a-radio>
      </a-radio-group>
      <br /> -->
      <br />
      <a-progress v-if="percentage" :percent="percentage" status="success"></a-progress>
      <br />
      <a-space>
        <a-button type="primary" :disabled="fileUploadDisabled" @click="startSqlUpload">开始上传</a-button>
        <a-tag color="green" :visible="successSize !== 0" :closable="true" class="successTag"> 上传成功: {{ successSize }} 个文件! </a-tag>
      </a-space>
    </a-modal>
  </div>
</template>
<script>
import {backupStatusMap, backupTypeArray, backupTypeMap, createBackup, deleteBackup, downloadBackupFile, getBackupList, getTableNameList, restoreBackup, uploadBackupFile} from "@/api/backup-info";
import {parseTime, renderSize} from "@/utils/time";
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";
import Vue from "vue";

export default {
  components: {},
  data() {
    return {
      backupTypeMap: backupTypeMap,
      backupStatusMap: backupStatusMap,
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      backupTypeList: backupTypeArray,
      list: [],
      total: 0,
      tableNameList: [],
      targetKeys: [],
      uploadFileList: [],
      temp: {},
      createBackupVisible: false,
      uploadSqlFileVisible: false,
      // 是否是上传状态
      uploading: false,
      percentage: 0,
      backupType: 0,
      successSize: 0,
      columns: [
        { title: "备份名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        {
          title: "打包时间",
          width: 170,
          dataIndex: "baleTimeStamp",
          // ellipsis: true,
          sorter: true,
          scopedSlots: { customRender: "baleTimeStamp" },
        },
        {
          title: "版本",
          dataIndex: "version",
          width: 100,
          // ellipsis: true,
          scopedSlots: { customRender: "version" },
        },
        { title: "备份类型", dataIndex: "backupType", width: 100, ellipsis: true, scopedSlots: { customRender: "backupType" } },
        {
          title: "文件大小",
          dataIndex: "fileSize",
          width: 100,
          // ellipsis: true,
          scopedSlots: { customRender: "fileSize" },
        },
        { title: "状态", dataIndex: "status", width: 120, scopedSlots: { customRender: "status" } },
        // {
        //   title: "文件地址",
        //   dataIndex: "filePath",
        //   // width: 150,
        //   ellipsis: true,
        //   scopedSlots: { customRender: "filePath" },
        // },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },
        {
          title: "备份时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          customRender: (text) => {
            if (!text) {
              return "";
            }
            return parseTime(text);
          },
          width: 170,
        },
        {
          title: "操作",
          dataIndex: "operation",
          width: 180,
          scopedSlots: { customRender: "operation" },
          align: "center",
          // fixed: "right",
        },
      ],
      rules: {
        name: [{ required: true, message: "Please input build name", trigger: "blur" }],
        script: [{ required: true, message: "Please input build script", trigger: "blur" }],
        resultDirFile: [{ required: true, message: "Please input build target path", trigger: "blur" }],
        releasePath: [{ required: true, message: "Please input release path", trigger: "blur" }],
      },
      tempVue: Vue,
    };
  },
  computed: {
    // 计算上传文件是否禁用
    fileUploadDisabled() {
      return this.uploadFileList.length === 0 || this.uploading;
    },
    // 分页
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  created() {
    // console.log(backupTypeMap);
    this.loadData();
  },
  methods: {
    // 格式化文件大小
    renderSizeFormat(value) {
      return renderSize(value);
    },
    parseTime: parseTime,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;

      getBackupList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 加载数据库表名列表
    loadTableNameList() {
      this.tableNameList = [];
      getTableNameList().then((res) => {
        if (res.code === 200) {
          res.data.forEach((element) => {
            this.tableNameList.push({ key: element.tableName, title: element.tableDesc });
          });
        }
      });
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
      };
      this.loadTableNameList();
      this.createBackupVisible = true;
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
            });
            this.$refs["editBackupForm"].resetFields();
            this.createBackupVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 下载
    handleDownload(record) {
      window.open(downloadBackupFile(record.id), "_self");
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
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 还原备份
    handleRestore(record) {
      const html =
        "真的要还原备份信息么？<ul style='color:red;'>" +
        "<li>建议还原和当前版本一致的文件或者临近版本的文件</li>" +
        "<li>如果版本相差大需要重新初始化数据来保证和当前程序里面字段一致</li>" +
        "<li>重置初始化在启动时候传人参数 <b> --rest:load_init_db </b> </li>" +
        " </ul>还原过程中不能操作哦...";
      const h = this.$createElement;
      this.$confirm({
        title: "系统提示",
        content: h("div", null, [h("p", { domProps: { innerHTML: html } }, null)]),
        okText: "确认",
        cancelText: "取消",
        width: 600,
        onOk: () => {
          // 还原
          restoreBackup(record.id).then((res) => {
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
    // 上传压缩文件
    handleSqlUpload() {
      this.successSize = 0;
      this.uploadSqlFileVisible = true;
    },
    handleSqlRemove() {
      this.uploadFileList = [];
    },
    beforeSqlUpload(file) {
      this.uploadFileList = [file];
      return false;
    },
    // 开始上传 SQL 文件
    startSqlUpload() {
      this.$notification.info({
        message: "正在上传文件，请稍后...",
      });
      // 设置上传状态
      this.uploading = true;
      const timer = setInterval(() => {
        this.percentage = this.percentage > 99 ? 99 : this.percentage + 1;
      }, 1000);

      // 上传文件
      const file = this.uploadFileList[0];
      const formData = new FormData();
      formData.append("file", file);
      formData.append("backupType", this.backupType);
      // 上传文件
      uploadBackupFile(formData).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.successSize++;
          this.percentage = 100;
          setTimeout(() => {
            this.percentage = 0;
            this.uploading = false;
            clearInterval(timer);
            this.uploadFileList = [];
            this.loadData();
          }, 1000);
        }
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
  },
};
</script>
<style scoped></style>
