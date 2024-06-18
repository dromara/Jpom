<template>
  <div class="">
    <!-- 表格 -->
    <a-table
      size="middle"
      :columns="columns"
      :data-source="list"
      bordered
      row-key="id"
      :pagination="pagination"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
    >
      <template #title>
        <a-space>
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_a1b745fba0')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%version%']"
            :placeholder="$t('i18n_0f4f503547')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.backupType"
            allow-clear
            :placeholder="$t('i18n_43ebf364ed')"
            class="search-input-item"
          >
            <a-select-option v-for="backupTypeItem in backupTypeList" :key="backupTypeItem.key">
              {{ backupTypeItem.value }}
            </a-select-option>
          </a-select>
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button :loading="loading" type="primary" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('i18n_a4006e5c1e') }}</a-button>
          <a-button type="primary" @click="handleSqlUpload">{{ $t('i18n_90c0458a4c') }}</a-button>
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
          <a-tooltip v-if="record.fileExist" :title="`${backupStatusMap[text]} ${$t('i18n_ae12edc5bf')}`">
            <div>
              <a-typography-paragraph :copyable="{ tooltip: false, text: record.filePath }" style="margin-bottom: 0">
                {{ backupStatusMap[text] }}
              </a-typography-paragraph>
            </div>
          </a-tooltip>
          <a-tooltip v-else :title="`${$t('i18n_96283fc523')}:${record.filePath}`">
            <WarningOutlined />
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'fileSize'">
          <a-tooltip placement="topLeft" :title="renderSizeFormat(text) + ' ' + record.sha1Sum">
            <a-tag color="#108ee9">{{ renderSizeFormat(text) }}</a-tag>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button
              size="small"
              type="primary"
              :disabled="!record.fileExist || record.status !== 1"
              @click="handleDownload(record)"
              >{{ $t('i18n_f26ef91424') }}</a-button
            >
            <a-button
              size="small"
              type="primary"
              danger
              :disabled="!record.fileExist || record.status !== 1"
              @click="handleRestore(record)"
              >{{ $t('i18n_69de8d7f40') }}</a-button
            >
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 创建备份信息区 -->
    <CustomModal
      v-if="createBackupVisible"
      v-model:open="createBackupVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_adbec9b14d')"
      width="600px"
      :mask-closable="false"
      @ok="handleCreateBackupOk"
    >
      <a-form ref="editBackupForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$t('i18n_8c61c92b4b')" name="backupType">
          <a-radio-group v-model:value="temp.backupType" name="backupType">
            <a-radio v-for="item in backupTypeList" v-show="!item.disabled" :key="item.key" :value="item.key">{{
              item.value
            }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <!-- 部分备份 -->
        <a-form-item
          v-if="temp.backupType === 1"
          :label="$t('i18n_b8ac664d98')"
          name="tableNameList"
          class="feature jpom-role"
        >
          <a-transfer
            :data-source="tableNameList"
            show-search
            :filter-option="filterOption"
            :target-keys="targetKeys"
            @change="handleChange"
          >
            <template #render="item">
              <a-tooltip :title="item.title">{{ item.title }} </a-tooltip>
            </template>
          </a-transfer>
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 上传 SQL 备份文件 -->
    <CustomModal
      v-if="uploadSqlFileVisible"
      v-model:open="uploadSqlFileVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="300px"
      :title="$t('i18n_b5b51ff786')"
      :mask-closable="true"
      @ok="startSqlUpload"
    >
      <a-upload :file-list="uploadFileList" :before-upload="beforeSqlUpload" accept=".sql" @remove="handleSqlRemove">
        <a-button><UploadOutlined />{{ $t('i18n_c8c452749e') }}</a-button>
      </a-upload>
      <!-- <br />
        <a-radio-group v-model="backupType" name="backupType">
          <a-radio :value="0">全量备份</a-radio>
          <a-radio :value="1">部分备份</a-radio>
        </a-radio-group>
        <br /> -->
    </CustomModal>
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

      backupType: 0,

      columns: [
        {
          title: this.$t('i18n_77b9ecc8b1'),
          dataIndex: 'name',
          ellipsis: true
        },
        {
          title: this.$t('i18n_2c014aeeee'),
          width: 170,
          dataIndex: 'baleTimeStamp',
          // ellipsis: true,
          sorter: true
        },
        {
          title: this.$t('i18n_fe2df04a16'),
          dataIndex: 'version',
          width: 100
          // ellipsis: true,
        },
        {
          title: this.$t('i18n_8c61c92b4b'),
          dataIndex: 'backupType',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_396b7d3f91'),
          dataIndex: 'fileSize',
          width: 100
          // ellipsis: true,
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: 'status',
          width: 120
        },
        // {
        //   title: "文件地址",
        //   dataIndex: "filePath",
        //   // width: 150,
        //   ellipsis: true,

        // },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('i18n_ae0fd9b9d2'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          width: '180px',

          align: 'center',
          fixed: 'right'
        }
      ],

      rules: {},
      confirmLoading: false,
      timer: null
    }
  },
  computed: {
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
      this.$refs['editBackupForm'].validate().then(() => {
        this.confirmLoading = true
        // 提交数据
        createBackup(this.targetKeys)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editBackupForm'].resetFields()
              this.createBackupVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 下载
    handleDownload(record) {
      window.open(downloadBackupFile(record.id), '_blank')
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_814dd5fb7d'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteBackup(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
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
      const html = `
        ${this.$t('i18n_4d18dcbd15')}
        <ul style='color:red;'>
        <li>${this.$t('i18n_6ac61b0e74')}</li>
        <li>${this.$t('i18n_a9eed33cfb')}</li>
        <li>${this.$t('i18n_5ed197a129')} <b> --rest:load_init_db </b> </li>
      </ul>${this.$t('i18n_d0132b0170')}`

      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        width: 600,
        onOk: () => {
          return restoreBackup(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
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
      this.uploadSqlFileVisible = true
      // clearInterval(this.timer)

      this.uploadFileList = []
    },
    handleSqlRemove() {
      this.handleSqlUpload()
      return true
    },
    beforeSqlUpload(file) {
      this.uploadFileList = [file]
      return false
    },
    // 开始上传 SQL 文件
    startSqlUpload() {
      if (this.uploadFileList.length != 1) {
        $notification.warning({
          message: this.$t('i18n_1a704f73c2')
        })
        return
      }

      // 上传文件
      const file = this.uploadFileList[0]
      const formData = new FormData()
      formData.append('file', file)
      formData.append('backupType', this.backupType)
      // 上传文件
      this.confirmLoading = true
      uploadBackupFile(formData)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })

            this.uploadSqlFileVisible = false
            this.loadData()
          }
        })
        .finally(() => {
          this.confirmLoading = false
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
