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
            :placeholder="$tl('p.backupName')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%version%']"
            :placeholder="$tl('p.version')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.backupType"
            allow-clear
            :placeholder="$tl('p.selectBackupType')"
            class="search-input-item"
          >
            <a-select-option v-for="backupTypeItem in backupTypeList" :key="backupTypeItem.key">
              {{ backupTypeItem.value }}
            </a-select-option>
          </a-select>
          <a-tooltip :title="$tl('p.quickBackToFirstPage')">
            <a-button :loading="loading" type="primary" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $tl('p.createBackup') }}</a-button>
          <a-button type="primary" @click="handleSqlUpload">{{ $tl('p.importBackup') }}</a-button>
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
          <a-tooltip v-if="record.fileExist" :title="`${backupStatusMap[text]} ${$tl('p.copyFilePath')}`">
            <div>
              <a-typography-paragraph :copyable="{ tooltip: false, text: record.filePath }" style="margin-bottom: 0">
                {{ backupStatusMap[text] }}
              </a-typography-paragraph>
            </div>
          </a-tooltip>
          <a-tooltip v-else :title="`${$tl('p.backupNotExist')}:${record.filePath}`">
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
              >{{ $tl('p.download') }}</a-button
            >
            <a-button
              size="small"
              type="primary"
              danger
              :disabled="!record.fileExist || record.status !== 1"
              @click="handleRestore(record)"
              >{{ $tl('p.restore') }}</a-button
            >
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{ $tl('p.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 创建备份信息区 -->
    <a-modal
      v-model:open="createBackupVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$tl('p.createBackupInfo')"
      width="600px"
      :mask-closable="false"
      @ok="handleCreateBackupOk"
    >
      <a-form ref="editBackupForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$tl('c.backupType')" name="backupType">
          <a-radio-group v-model:value="temp.backupType" name="backupType">
            <a-radio v-for="item in backupTypeList" v-show="!item.disabled" :key="item.key" :value="item.key">{{
              item.value
            }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <!-- 部分备份 -->
        <a-form-item
          v-if="temp.backupType === 1"
          :label="$tl('p.selectDataTable')"
          name="tableNameList"
          class="feature jpom-role"
        >
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
      v-model:open="uploadSqlFileVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="300px"
      :title="$tl('p.uploadSqlFile')"
      :mask-closable="true"
      @ok="startSqlUpload"
    >
      <a-upload :file-list="uploadFileList" :before-upload="beforeSqlUpload" accept=".sql" @remove="handleSqlRemove">
        <a-button><UploadOutlined />{{ $tl('p.chooseSqlFile') }}</a-button>
      </a-upload>
      <!-- <br />
        <a-radio-group v-model="backupType" name="backupType">
          <a-radio :value="0">全量备份</a-radio>
          <a-radio :value="1">部分备份</a-radio>
        </a-radio-group>
        <br /> -->
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

      backupType: 0,

      columns: [
        {
          title: this.$tl('p.backupNameLabel'),
          dataIndex: 'name',
          ellipsis: true
        },
        {
          title: this.$tl('p.packageTime'),
          width: 170,
          dataIndex: 'baleTimeStamp',
          // ellipsis: true,
          sorter: true
        },
        {
          title: this.$tl('p.versionLabel'),
          dataIndex: 'version',
          width: 100
          // ellipsis: true,
        },
        {
          title: this.$tl('c.backupType'),
          dataIndex: 'backupType',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('p.fileSize'),
          dataIndex: 'fileSize',
          width: 100
          // ellipsis: true,
        },
        {
          title: this.$tl('p.status'),
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
          title: this.$tl('p.modifier'),
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$tl('p.backupTime'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$tl('p.operation'),
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
    $tl(key, ...args) {
      return this.$t(`pages.system.backup.${key}`, ...args)
    },
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
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmDeleteBackup'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
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
        ${this.$tl('p.confirmRestoreBackup')}
        <ul style='color:red;'>
        <li>${this.$tl('p.adviceRestore')}</li>
        <li>${this.$tl('p.resetInitData')}</li>
        <li>${this.$tl('p.resetInitParam')} <b> --rest:load_init_db </b> </li>
      </ul>${this.$tl('p.operatingDuringRestore')}`

      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
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
          message: this.$tl('p.chooseFile')
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
