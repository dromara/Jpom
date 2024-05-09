<template>
  <div>
    <div>
      <!-- 数据表格 -->
      <a-table
        :data-source="list"
        size="middle"
        :columns="columns"
        :pagination="pagination"
        bordered
        row-key="id"
        :row-selection="rowSelection"
        :scroll="{
          x: 'max-content'
        }"
        @change="
          (pagination, filters, sorter) => {
            listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
            loadData()
          }
        "
      >
        <template #title>
          <a-space>
            <a-input
              v-model:value="listQuery['%name%']"
              :placeholder="$tl('p.fileName')"
              class="search-input-item"
              @press-enter="loadData"
            />

            <a-input
              v-model:value="listQuery['extName']"
              :placeholder="$tl('p.suffixSearch')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['id']"
              :placeholder="$tl('p.fileIdSearch')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-tooltip :title="$tl('p.quickBackToFirstPage')">
              <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
            </a-tooltip>
            <!-- <a-button type="primary" @click="handleUpload">上传文件</a-button> -->
            <a-button type="primary" @click="reScanner">{{ $tl('p.scan') }}</a-button>

            <a-button
              type="primary"
              danger
              :disabled="!tableSelections || tableSelections.length <= 0"
              @click="handleBatchDelete"
            >
              {{ $tl('p.batchDelete') }}
            </a-button>
            <a-button
              size="small"
              type="link"
              @click="
                () => {
                  configDir = true
                }
              "
            >
              <InfoCircleOutlined /> {{ $tl('p.configDirectory') }}
            </a-button>
          </a-space>
        </template>
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.tooltip">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'id'">
            <a-tooltip placement="topLeft" :title="text">
              <span v-if="record.status === 0 || record.status === 2">-</span>
              <span v-else>{{ (text || '').slice(0, 8) }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'name'">
            <a-popover :title="$tl('p.fileInfo')">
              <template #content>
                <p>{{ $tl('p.fileId') }}{{ record.id }}</p>
                <p>{{ $tl('p.fileNameLabel') }}{{ text }}</p>
                <p>{{ $tl('p.fileDescriptionLabel') }}{{ record.description }}</p>
              </template>
              <!-- {{ text }} -->
              <a-button type="link" style="padding: 0" size="small" @click="handleEdit(record)">{{ text }}</a-button>
            </a-popover>
          </template>

          <template v-else-if="column.dataIndex === 'size'">
            <a-tooltip placement="topLeft" :title="renderSize(text)">
              <span>{{ renderSize(text) }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'source'">
            <a-tooltip placement="topLeft" :title="`${sourceMap[text] || $tl('c.content1')}`">
              <span>{{ sourceMap[text] || $tl('c.content1') }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'status'">
            <a-tag v-if="text === 1" color="green">{{ $tl('p.exist') }}</a-tag>
            <a-tag v-else color="red">{{ $tl('p.missing') }}</a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'type'">
            <a-tag v-if="text === 1">{{ $tl('p.file') }}</a-tag>
            <a-tag v-else>{{ $tl('p.folder') }}</a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'operation'">
            <a-space>
              <!-- <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button> -->
              <a-button
                size="small"
                :disabled="!(record.status === 1 && record.type === 1)"
                type="primary"
                @click="handleDownloadUrl(record)"
              >
                {{ $tl('p.download') }}</a-button
              >
              <a-button
                size="small"
                :disabled="!(record.status === 1 && record.type === 1)"
                type="primary"
                @click="handleReleaseFile(record)"
                >{{ $tl('p.publish') }}</a-button
              >
              <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
                $tl('p.deleteFile')
              }}</a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <!-- 编辑文件 -->
      <a-modal
        v-model:open="editVisible"
        destroy-on-close
        :title="`${$tl('p.modifyFile')}`"
        :confirm-loading="confirmLoading"
        :mask-closable="false"
        @ok="handleEditOk"
      >
        <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-form-item :label="$tl('c.fileName')" name="name">
            <a-input v-model:value="temp.name" :placeholder="$tl('c.fileName')" :disabled="true" />
          </a-form-item>

          <a-form-item :label="$tl('p.fileDescription')" name="description">
            <a-textarea v-model:value="temp.description" :placeholder="$tl('p.pleaseEnterFileDescription')" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 断点下载 -->
      <a-modal
        v-model:open="triggerVisible"
        destroy-on-close
        :title="$tl('p.chunkedDownload')"
        width="50%"
        :footer="null"
        :mask-closable="false"
      >
        <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-tabs default-active-key="1">
            <template #rightExtra>
              <a-tooltip :title="$tl('p.resetDownloadToken')">
                <a-button type="primary" size="small" @click="resetTrigger">{{ $tl('p.reset') }}</a-button>
              </a-tooltip>
            </template>
            <a-tab-pane key="1" :tab="$tl('p.chunkedSingleFileDownload')">
              <a-space direction="vertical" style="width: 100%">
                <a-alert type="info" :message="`${$tl('c.downloadAddress')}(${$tl('c.copyTip')})`">
                  <template #description>
                    <a-typography-paragraph :copyable="{ text: temp.triggerDownloadUrl }">
                      <a-tag>GET</a-tag>
                      <span>{{ `${temp.triggerDownloadUrl}` }} </span>
                    </a-typography-paragraph>
                  </template>
                </a-alert>
                <a :href="temp.triggerDownloadUrl" target="_blank">
                  <a-button size="small" type="primary"><DownloadOutlined />{{ $tl('c.downloadNow') }}</a-button>
                </a>
              </a-space>
            </a-tab-pane>
            <a-tab-pane v-if="temp.triggerAliasDownloadUrl" :tab="$tl('p.chunkedAliasDownload')">
              <a-space direction="vertical" style="width: 100%">
                <a-alert :message="$tl('p.warmPrompt')" type="warning">
                  <template #description>
                    <ul>
                      <li>
                        {{ $tl('p.customSortField') }}=createTimeMillis:desc

                        <p>{{ $tl('p.descSortAscFirst') }}</p>
                      </li>
                      <li>{{ $tl('p.supportedFields') }}</li>
                      <li>{{ $tl('p.commonFields') }}</li>
                    </ul>
                  </template>
                </a-alert>
                <a-alert type="info" :message="`${$tl('c.downloadAddress')}(${$tl('c.copyTip')})`">
                  <template #description>
                    <a-typography-paragraph :copyable="{ text: temp.triggerAliasDownloadUrl }">
                      <a-tag>GET</a-tag>
                      <span>{{ `${temp.triggerAliasDownloadUrl}` }} </span>
                    </a-typography-paragraph>
                  </template>
                </a-alert>
                <a :href="temp.triggerAliasDownloadUrl" target="_blank">
                  <a-button size="small" type="primary"><DownloadOutlined />{{ $tl('c.downloadNow') }}</a-button>
                </a>
              </a-space>
            </a-tab-pane>
          </a-tabs>
        </a-form>
      </a-modal>
      <!-- 发布文件 -->
      <a-modal
        v-model:open="releaseFileVisible"
        destroy-on-close
        :title="$tl('p.publishFile')"
        width="50%"
        :mask-closable="false"
        :confirm-loading="confirmLoading"
        @ok="releaseFileOk()"
      >
        <releaseFile v-if="releaseFileVisible" ref="releaseFile" @commit="handleCommitTask"></releaseFile>
      </a-modal>
    </div>

    <!-- 配置工作空间授权目录 -->
    <a-modal
      v-model:open="configDir"
      destroy-on-close
      :title="`${$tl('p.configAuthDirectory')}`"
      :footer="null"
      width="50vw"
      :mask-closable="false"
      @cancel="
        () => {
          configDir = false
        }
      "
    >
      <whiteList
        v-if="configDir"
        @cancel="
          () => {
            configDir = false
            loadData()
          }
        "
      ></whiteList>
    </a-modal>
  </div>
</template>

<script>
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime,
  renderSize,
  formatDuration,
  randomStr
} from '@/utils/const'
// import { uploadFile, uploadFileMerge, hasFile } from "@/api/file-manager/file-storage";
import { staticFileStorageList, delFile, triggerUrl, fileEdit, staticScanner } from '@/api/file-manager/static-storage'

import releaseFile from '@/pages/file-manager/fileStorage/releaseFile'
import { addReleaseTask } from '@/api/file-manager/release-task-log'
import whiteList from '@/pages/dispatch/white-list'
export default {
  components: {
    releaseFile,
    whiteList
  },
  props: {
    choose: {
      // "radio"
      type: String,
      default: ''
    }
  },
  emits: ['cancel', 'confirm'],
  data() {
    return {
      loading: true,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      columns: [
        {
          title: this.$tl('p.name'),
          dataIndex: 'name',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$tl('p.desc'),
          dataIndex: 'description',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$tl('p.path'),
          dataIndex: 'absolutePath',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$tl('p.size'),
          dataIndex: 'size',
          sorter: true,
          ellipsis: true,

          width: '100px'
        },
        {
          title: this.$tl('p.suffix'),
          dataIndex: 'extName',
          ellipsis: true,

          tooltip: true,
          width: '80px'
        },
        {
          title: this.$tl('p.type'),
          dataIndex: 'type',
          ellipsis: true,

          width: '80px'
        },
        {
          title: this.$tl('p.fileStatus'),
          dataIndex: 'status',
          ellipsis: true,

          width: '80px'
        },

        {
          title: this.$tl('p.fileModifyTime'),
          dataIndex: 'lastModified',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },

        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',
          align: 'center',
          ellipsis: true,

          fixed: 'right',
          width: '170px'
        }
      ],
      rules: {
        name: [{ required: true, message: this.$tl('p.pleaseEnterFileName'), trigger: 'blur' }],
        url: [{ required: true, message: this.$tl('p.pleaseEnterRemoteAddress'), trigger: 'blur' }]
      },

      temp: {},

      fileList: [],
      percentage: 0,
      percentageInfo: {},
      uploading: false,
      uploadVisible: false,
      editVisible: false,
      configDir: false,
      triggerVisible: false,
      releaseFileVisible: false,
      tableSelections: [],
      confirmLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections,
        type: this.choose || 'checkbox'
      }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.fileManager.staticFileStorage.list.${key}`, ...args)
    },
    randomStr,
    CHANGE_PAGE,
    renderSize,
    formatDuration,
    parseTime,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      staticFileStorageList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },

    // 编辑
    handleEdit(item) {
      this.temp = {
        ...item,
        global: item.workspaceId === 'GLOBAL',
        workspaceId: ''
      }
      this.editVisible = true
      this.$refs['editForm']?.resetFields()
    },
    // 编辑确认
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        this.confirmLoading = true
        fileEdit(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.editVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除文件
    handleDelete(record) {
      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmDeleteFile') + record.name,
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return delFile({
            id: record.id,
            thorough: false
          }).then((res) => {
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
    // 批量删除
    handleBatchDelete() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warning({
          message: this.$tl('p.noDataSelected')
        })
        return
      }
      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmDeleteFiles'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return delFile({
            ids: this.tableSelections.join(','),
            thorough: false
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.tableSelections = []
              this.loadData()
            }
          })
        }
      })
    },

    // 下载地址
    handleDownloadUrl(record) {
      this.temp = Object.assign({}, record)

      triggerUrl({
        id: record.id
      }).then((res) => {
        if (res.code === 200) {
          this.fillDownloadUrlResult(res)
          this.$nextTick(() => {
            this.triggerVisible = true
          })
        }
      })
    },
    // 重置触发器
    resetTrigger() {
      triggerUrl({
        id: this.temp.id,
        rest: 'rest'
      }).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.fillDownloadUrlResult(res)
        }
      })
    },
    fillDownloadUrlResult(res) {
      this.temp = {
        ...this.temp,
        triggerDownloadUrl: `${location.protocol}//${location.host}${res.data.triggerDownloadUrl}`,
        triggerAliasDownloadUrl: res.data?.triggerAliasDownloadUrl
          ? `${location.protocol}//${location.host}${res.data.triggerAliasDownloadUrl}?sort=createTimeMillis:desc`
          : ''
      }
    },
    // 发布文件
    handleReleaseFile(record) {
      this.releaseFileVisible = true
      this.temp = { fileId: record.id }
    },

    handleCommitTask(data) {
      this.confirmLoading = true
      addReleaseTask({ ...data, fileId: this.temp.fileId, fileType: 2 })
        .then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })

            this.releaseFileVisible = false
            this.loadData()
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },

    releaseFileOk() {
      this.$refs.releaseFile?.tryCommit()
    },
    // 选择确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$tl('c.pleaseSelectFile')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return this.tableSelections.indexOf(item.id) > -1
      })
      if (!selectData.length) {
        $notification.warning({
          message: this.$tl('c.pleaseSelectFile')
        })
        return
      }
      this.$emit('confirm', selectData)
    },
    // 扫描
    reScanner() {
      staticScanner({}).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    }
  }
}
</script>

<style scoped>
:deep(.ant-progress-text) {
  width: auto;
}
</style>
