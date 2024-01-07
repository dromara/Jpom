<template>
  <div class="full-content">
    <!-- 表格 -->
    <a-table
      size="middle"
      :columns="columns"
      :data-source="list"
      bordered
      rowKey="id"
      @change="changePage"
      :pagination="pagination"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template #title>
        <a-space>
          <a-input
            v-model:value="listQuery['%name%']"
            @pressEnter="loadData"
            placeholder="请输入备份名称"
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['%version%']"
            @pressEnter="loadData"
            placeholder="请输入版本"
            class="search-input-item"
          />
          <a-select
            v-model:value="listQuery.backupType"
            allowClear
            placeholder="请选择备份类型"
            class="search-input-item"
          >
            <a-select-option v-for="backupType in backupTypeList" :key="backupType.key">{{
              backupType.value
            }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">创建备份</a-button>
          <a-button type="primary" @click="handleSqlUpload">导入备份</a-button>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'backupType'">
          <a-tooltip :title="text">
            <span>{{ backupTypeMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'baleTimeStamp'">
          <a-tooltip :title="`${parseTime(text)}`">
            {{ parseTime(text) }}
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip v-if="record.fileExist" :title="`${backupStatusMap[text]} 点击复制文件路径`">
            <div>
              <a-typography-paragraph :copyable="{ tooltip: false, text: record.filePath }">
                {{ backupStatusMap[text] }}
              </a-typography-paragraph>
            </div>
          </a-tooltip>
          <a-tooltip v-else :title="`备份文件不存在:${record.filePath}`">
            <WarningOutlined />
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'fileSize'">
          <a-tooltip placement="topLeft" :title="renderSizeFormat(text) + ' ' + record.sha1Sum">
            <a-tag color="#108ee9">{{ renderSizeFormat(text) }}</a-tag>
          </a-tooltip>
        </template>
        <!-- <a-tooltip slot="filePath" slot-scope="text, record" placement="topLeft" :title="text + ' ' + (record.sha1Sum || '')">
          <span

            >{{ text }}
          </span>
        </a-tooltip> -->
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button
              size="small"
              type="primary"
              :disabled="!record.fileExist || record.status !== 1"
              @click="handleDownload(record)"
              >下载</a-button
            >
            <a-button
              size="small"
              type="primary"
              danger
              :disabled="!record.fileExist || record.status !== 1"
              @click="handleRestore(record)"
              >还原</a-button
            >
            <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 创建备份信息区 -->
    <a-modal
      destroyOnClose
      v-model:open="createBackupVisible"
      title="创建备份信息"
      @ok="handleCreateBackupOk"
      width="600px"
      :maskClosable="false"
    >
      <a-form ref="editBackupForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="备份类型" name="backupType">
          <a-radio-group v-model:value="temp.backupType" name="backupType">
            <a-radio v-for="item in backupTypeList" v-show="!item.disabled" :key="item.key" :value="item.key">{{
              item.value
            }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <!-- 部分备份 -->
        <a-form-item v-if="temp.backupType === 1" label="勾选数据表" name="tableNameList" class="feature jpom-role">
          <a-transfer
            :data-source="tableNameList"
            show-search
            :filter-option="filterOption"
            :target-keys="targetKeys"
            :render="(item) => item.title"
            @change="handleChange"
          />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 上传 SQL 备份文件 -->
    <a-modal
      destroyOnClose
      v-model:open="uploadSqlFileVisible"
      width="300px"
      title="上传 SQL 文件"
      :footer="null"
      :maskClosable="true"
    >
      <a-upload :file-list="uploadFileList" :remove="handleSqlRemove" :before-upload="beforeSqlUpload" accept=".sql">
        <a-button><UploadOutlined />选择 SQL 文件</a-button>
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
        <a-tag color="green" :visible="successSize !== 0" :closable="true" class="successTag">
          上传成功: {{ successSize }} 个文件!
        </a-tag>
      </a-space>
    </a-modal>
  </div>
</template>

<script>
import {
  backupStatusMap,
  backupTypeArray,
  backupTypeMap,
  createBackup,
  deleteBackup,
  downloadBackupFile,
  getBackupList,
  getTableNameList,
  restoreBackup,
  uploadBackupFile
} from '@/api/backup-info'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime, renderSize } from '@/utils/const'

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
        {
          title: '备份名称',
          dataIndex: 'name',
          ellipsis: true
        },
        {
          title: '打包时间',
          width: 170,
          dataIndex: 'baleTimeStamp',
          // ellipsis: true,
          sorter: true
        },
        {
          title: '版本',
          dataIndex: 'version',
          width: 100
          // ellipsis: true,
        },
        {
          title: '备份类型',
          dataIndex: 'backupType',
          width: 100,
          ellipsis: true
        },
        {
          title: '文件大小',
          dataIndex: 'fileSize',
          width: 100
          // ellipsis: true,
        },
        {
          title: '状态',
          dataIndex: 'status',
          width: 120
        },
        // {
        //   title: "文件地址",
        //   dataIndex: "filePath",
        //   // width: 150,
        //   ellipsis: true,
        //   scopedSlots: { customRender: "filePath" },
        // },
        {
          title: '修改人',
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: 120
        },
        {
          title: '备份时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          width: '180px',

          align: 'center',
          fixed: 'right'
        }
      ],
      rules: {},

      timer: null
    }
  },
  computed: {
    // 计算上传文件是否禁用
    fileUploadDisabled() {
      return this.uploadFileList.length === 0 || this.uploading
    },
    // 分页
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  created() {
    // console.log(backupTypeMap);
    this.loadData()
  },
  beforeUnmount() {
    this.timer && clearTimeout(this.timer)
  },
  methods: {
    // 格式化文件大小
    renderSizeFormat(value) {
      return renderSize(value)
    },
    parseTime: parseTime,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page

      getBackupList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 加载数据库表名列表
    loadTableNameList() {
      this.tableNameList = []
      getTableNameList().then((res) => {
        if (res.code === 200) {
          res.data.forEach((element) => {
            this.tableNameList.push({
              key: element.tableName,
              title: element.tableDesc
            })
          })
        }
      })
    },

    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.title.indexOf(inputValue) > -1
    },
    // 穿梭框 change
    handleChange(targetKeys) {
      this.targetKeys = targetKeys
    },
    // 创建备份
    handleAdd() {
      this.targetKeys = []
      this.temp = {
        backupType: 0
      }
      this.loadTableNameList()
      this.createBackupVisible = true
    },
    // 提交节点数据
    handleCreateBackupOk() {
      // 检验表单
      this.$refs['editBackupForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        // 提交数据
        createBackup(this.targetKeys).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg
            })
            this.$refs['editBackupForm'].resetFields()
            this.createBackupVisible = false
            this.loadData()
          }
        })
      })
    },
    // 下载
    handleDownload(record) {
      window.open(downloadBackupFile(record.id), '_blank')
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除备份信息么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteBackup(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 还原备份
    handleRestore(record) {
      const html =
        "真的要还原备份信息么？<ul style='color:red;'>" +
        '<li>建议还原和当前版本一致的文件或者临近版本的文件</li>' +
        '<li>如果版本相差大需要重新初始化数据来保证和当前程序里面字段一致</li>' +
        '<li>重置初始化在启动时候传入参数 <b> --rest:load_init_db </b> </li>' +
        ' </ul>还原过程中不能操作哦...'

      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: h('div', null, [h('p', { domProps: { innerHTML: html } }, null)]),
        okText: '确认',
        cancelText: '取消',
        width: 600,
        onOk: () => {
          // 还原
          restoreBackup(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 上传压缩文件
    handleSqlUpload() {
      this.successSize = 0
      this.uploadSqlFileVisible = true
      clearInterval(this.timer)
      this.percentage = 0
      this.uploadFileList = []
      this.uploading = false
    },
    handleSqlRemove() {
      this.handleSqlUpload()
    },
    beforeSqlUpload(file) {
      this.uploadFileList = [file]
      return false
    },
    // 开始上传 SQL 文件
    startSqlUpload() {
      this.$notification.info({
        message: '正在上传文件，请稍后...'
      })
      // 设置上传状态
      this.uploading = true
      this.timer = setInterval(() => {
        this.percentage = this.percentage > 99 ? 99 : this.percentage + 1
      }, 1000)

      // 上传文件
      const file = this.uploadFileList[0]
      const formData = new FormData()
      formData.append('file', file)
      formData.append('backupType', this.backupType)
      // 上传文件
      uploadBackupFile(formData).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg
          })
          this.successSize++
          this.percentage = 100
          setTimeout(() => {
            this.handleSqlRemove()
            this.loadData()
            this.uploadSqlFileVisible = false
          }, 1000)
        }
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }
  }
}
</script>
