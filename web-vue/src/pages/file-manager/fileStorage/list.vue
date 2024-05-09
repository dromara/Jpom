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
              v-model:value="listQuery['%aliasCode%']"
              :placeholder="$tl('c.aliasCode')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['extName']"
              :placeholder="$tl('p.suffix')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['id']"
              :placeholder="$tl('p.fileId')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-tooltip :title="$tl('p.quickBackToFirstPage')">
              <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
            </a-tooltip>
            <a-button type="primary" @click="handleUpload">{{ $tl('c.uploadFile') }}</a-button>
            <a-button type="primary" @click="handleRemoteDownload">{{ $tl('p.remoteDownload') }}</a-button>
            <a-button
              type="primary"
              danger
              :disabled="!tableSelections || tableSelections.length <= 0"
              @click="handleBatchDelete"
            >
              {{ $tl('p.batchDelete') }}
            </a-button>
          </a-space>
        </template>
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.tooltip">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ (text || '').slice(0, 8) }}</span>
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
                <p>{{ $tl('c.fileName') }}{{ text }}</p>
                <p>{{ $tl('p.fileDescription') }}{{ record.description }}</p>
                <p v-if="record.status !== undefined">
                  {{ $tl('p.downloadStatus') }}{{ statusMap[record.status] || $tl('c.unknown') }}
                </p>
                <p v-if="record.progressDesc">{{ $tl('p.statusDescription') }}{{ record.progressDesc }}</p>
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
            <a-tooltip placement="topLeft" :title="sourceMap[text] || $tl('c.unknown')">
              <span>{{ sourceMap[text] || $tl('c.unknown') }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'exists'">
            <a-tag v-if="text" color="green">{{ $tl('p.exist') }}</a-tag>
            <a-tag v-else color="red">{{ $tl('p.lost') }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'workspaceId'">
            <a-tag v-if="text === 'GLOBAL'">{{ $tl('c.global') }}</a-tag>
            <a-tag v-else>{{ $tl('p.workspace') }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'operation'">
            <a-space>
              <!-- <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button> -->
              <a-button size="small" :disabled="!record.exists" type="primary" @click="handleDownloadUrl(record)">{{
                $tl('p.download')
              }}</a-button>
              <a-button size="small" :disabled="!record.exists" type="primary" @click="handleReleaseFile(record)">{{
                $tl('p.release')
              }}</a-button>
              <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
                $tl('p.delete')
              }}</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
      <!-- 上传文件 -->
      <a-modal
        v-model:open="uploadVisible"
        destroy-on-close
        :confirm-loading="confirmLoading"
        :closable="!uploading"
        :footer="uploading ? null : undefined"
        :keyboard="false"
        :title="`${$tl('c.uploadFile')}`"
        :mask-closable="false"
        @ok="handleUploadOk"
      >
        <a-form ref="form" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-form-item :label="$tl('c.selectFile')" name="file">
            <a-progress v-if="percentage" :percent="percentage">
              <template #format="percent">
                {{ percent }}%
                <template v-if="percentageInfo.total"> ({{ renderSize(percentageInfo.total) }}) </template>
                <template v-if="percentageInfo.duration">
                  {{ $tl('p.usageTime') }}:{{ formatDuration(percentageInfo.duration) }}
                </template>
              </template>
            </a-progress>

            <a-upload
              :file-list="fileList"
              :disabled="!!percentage"
              :before-upload="
                (file) => {
                  // 只允许上传单个文件
                  fileList = [file]
                  return false
                }
              "
              @remove="
                (file) => {
                  fileList = []
                  return true
                }
              "
            >
              <LoadingOutlined v-if="percentage" />
              <a-button v-else type="primary"><UploadOutlined />{{ $tl('c.selectFile') }}</a-button>
            </a-upload>
          </a-form-item>
          <a-form-item :label="$tl('c.retentionDays')" name="keepDay">
            <a-input-number
              v-model:value="temp.keepDay"
              :min="1"
              style="width: 100%"
              :placeholder="$tl('c.fileSaveDays')"
            />
          </a-form-item>
          <a-form-item :label="$tl('c.fileShare')" name="global">
            <a-radio-group v-model:value="temp.global">
              <a-radio :value="true"> {{ $tl('c.global') }} </a-radio>
              <a-radio :value="false"> {{ $tl('c.currentWorkspace') }} </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item :label="$tl('c.aliasCode')" name="aliasCode" :help="$tl('c.fileType')">
            <a-input-search
              v-model:value="temp.aliasCode"
              :max-length="50"
              :placeholder="$tl('c.enterAliasCode')"
              @search="
                () => {
                  temp = { ...temp, aliasCode: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <a-button type="primary"> {{ $tl('c.randomGenerate') }} </a-button>
              </template>
            </a-input-search>
          </a-form-item>
          <a-form-item :label="$tl('c.fileDescription')" name="description">
            <a-textarea v-model:value="temp.description" :placeholder="$tl('c.enterFileDescription')" />
          </a-form-item>
        </a-form>
      </a-modal>
      <!-- 编辑文件 -->
      <a-modal
        v-model:open="editVisible"
        destroy-on-close
        :confirm-loading="confirmLoading"
        :title="`${$tl('p.modifyFile')}`"
        :mask-closable="false"
        @ok="handleEditOk"
      >
        <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-form-item label="文件名" name="name">
            <a-input v-model:value="temp.name" placeholder="文件名" />
          </a-form-item>
          <a-form-item :label="$tl('c.retentionDays')" name="keepDay">
            <a-input-number
              v-model:value="temp.keepDay"
              :min="1"
              style="width: 100%"
              :placeholder="$tl('c.fileSaveDays')"
            />
          </a-form-item>
          <a-form-item :label="$tl('c.fileShare')" name="global">
            <a-radio-group v-model:value="temp.global">
              <a-radio :value="true"> {{ $tl('c.global') }} </a-radio>
              <a-radio :value="false"> {{ $tl('c.currentWorkspace') }} </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item :label="$tl('c.aliasCode')" name="aliasCode" :help="$tl('c.fileType')">
            <a-input-search
              v-model:value="temp.aliasCode"
              :max-length="50"
              :placeholder="$tl('c.enterAliasCode')"
              @search="
                () => {
                  temp = { ...temp, aliasCode: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <a-button type="primary"> {{ $tl('c.randomGenerate') }} </a-button>
              </template>
            </a-input-search>
          </a-form-item>
          <a-form-item :label="$tl('c.fileDescription')" name="description">
            <a-textarea v-model:value="temp.description" :placeholder="$tl('c.enterFileDescription')" />
          </a-form-item>
        </a-form>
      </a-modal>
      <!--远程下载  -->
      <a-modal
        v-model:open="uploadRemoteFileVisible"
        destroy-on-close
        :title="$tl('p.remoteDownloadFile')"
        :mask-closable="false"
        :confirm-loading="confirmLoading"
        @ok="handleRemoteUpload"
      >
        <a-form ref="remoteForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" :rules="rules">
          <a-form-item :label="$tl('p.remoteDownloadUrl')" name="url">
            <a-input v-model:value="temp.url" :placeholder="$tl('p.remoteDownloadAddress')" />
          </a-form-item>
          <a-form-item :label="$tl('c.retentionDays')" name="keepDay">
            <a-input-number
              v-model:value="temp.keepDay"
              :min="1"
              style="width: 100%"
              :placeholder="$tl('c.fileSaveDays')"
            />
          </a-form-item>
          <a-form-item :label="$tl('c.fileShare')" name="global">
            <a-radio-group v-model:value="temp.global">
              <a-radio :value="true"> {{ $tl('c.global') }} </a-radio>
              <a-radio :value="false"> {{ $tl('c.currentWorkspace') }} </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item :label="$tl('c.aliasCode')" name="aliasCode" :help="$tl('c.fileType')">
            <a-input-search
              v-model:value="temp.aliasCode"
              :max-length="50"
              :placeholder="$tl('c.enterAliasCode')"
              @search="
                () => {
                  temp = { ...temp, aliasCode: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <a-button type="primary"> {{ $tl('c.randomGenerate') }} </a-button>
              </template>
            </a-input-search>
          </a-form-item>
          <a-form-item :label="$tl('c.fileDescription')" name="description">
            <a-textarea v-model:value="temp.description" :placeholder="$tl('c.enterFileDescription')" />
          </a-form-item>
        </a-form>
      </a-modal>
      <!-- 断点下载 -->
      <a-modal
        v-model:open="triggerVisible"
        destroy-on-close
        :title="$tl('p.resumableDownload')"
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
            <a-tab-pane key="1" :tab="$tl('p.resumableSingleFileDownload')">
              <a-space direction="vertical" style="width: 100%">
                <a-alert type="info" :message="`${$tl('c.downloadAddress')}(${$tl('c.copyByClick')})`">
                  <template #description>
                    <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerDownloadUrl }">
                      <a-tag>GET</a-tag>
                      <span>{{ `${temp.triggerDownloadUrl}` }} </span>
                    </a-typography-paragraph>
                  </template>
                </a-alert>
                <a :href="temp.triggerDownloadUrl" target="_blank">
                  <a-button size="small" type="primary"><DownloadOutlined />{{ $tl('c.immediateDownload') }}</a-button>
                </a>
              </a-space>
            </a-tab-pane>
            <a-tab-pane v-if="temp.triggerAliasDownloadUrl" :tab="$tl('p.resumableAliasDownload')">
              <a-space direction="vertical" style="width: 100%">
                <a-alert :message="$tl('p.warmPrompt')" type="warning">
                  <template #description>
                    <ul>
                      <li>
                        {{ $tl('p.customSortField') }}=createTimeMillis:desc

                        <p>{{ $tl('p.sortDesc') }}</p>
                      </li>
                      <li>{{ $tl('p.supportedFields') }}</li>
                      <li>{{ $tl('p.commonFields') }}</li>
                    </ul>
                  </template>
                </a-alert>
                <a-alert type="info" :message="`${$tl('c.downloadAddress')}(${$tl('c.copyByClick')})`">
                  <template #description>
                    <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerAliasDownloadUrl }">
                      <a-tag>GET</a-tag>
                      <span>{{ `${temp.triggerAliasDownloadUrl}` }} </span>
                    </a-typography-paragraph>
                  </template>
                </a-alert>
                <a :href="temp.triggerAliasDownloadUrl" target="_blank">
                  <a-button size="small" type="primary"><DownloadOutlined />{{ $tl('c.immediateDownload') }}</a-button>
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
        :confirm-loading="confirmLoading"
        :title="$tl('p.releaseFile')"
        width="60%"
        :mask-closable="false"
        @ok="releaseFileOk()"
      >
        <releaseFile v-if="releaseFileVisible" ref="releaseFile" @commit="handleCommitTask"></releaseFile>
      </a-modal>
    </div>
    <!-- 选择确认区域 -->
    <div v-if="choose" style="padding-top: 50px">
      <div
        :style="{
          position: 'absolute',
          right: 0,
          bottom: 0,
          width: '100%',
          borderTop: '1px solid #e9e9e9',
          padding: '10px 16px',
          background: '#fff',
          textAlign: 'right',
          zIndex: 1
        }"
      >
        <a-space>
          <a-button
            @click="
              () => {
                $emit('cancel')
              }
            "
          >
            {{ $tl('c.cancel') }}
          </a-button>
          <a-button type="primary" @click="handerConfirm"> {{ $tl('p.confirm') }} </a-button>
        </a-space>
      </div>
    </div>
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
import {
  fileStorageList,
  uploadFile,
  uploadFileMerge,
  fileEdit,
  hasFile,
  delFile,
  sourceMap,
  remoteDownload,
  statusMap,
  triggerUrl
} from '@/api/file-manager/file-storage'
import { uploadPieces } from '@/utils/upload-pieces'

import releaseFile from './releaseFile.vue'
import { addReleaseTask } from '@/api/file-manager/release-task-log'

export default {
  components: {
    releaseFile
  },
  inject: ['globalLoading'],
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
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      columns: [
        {
          title: this.$tl('p.fileMD5'),
          dataIndex: 'id',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$tl('p.name'),
          dataIndex: 'name',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$tl('c.aliasCode'),
          dataIndex: 'aliasCode',
          ellipsis: true,
          width: 100,
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
          title: this.$tl('p.contentSuffix'),
          dataIndex: 'extName',
          ellipsis: true,
          tooltip: true,
          width: '80px'
        },
        {
          title: this.$tl('p.shared'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$tl('p.source'),
          dataIndex: 'source',
          ellipsis: true,

          width: '80px'
        },
        {
          title: this.$tl('p.expireDays'),
          dataIndex: 'validUntil',
          sorter: true,
          customRender: ({ text }) => {
            if (!text) {
              return '-'
            }
            return Math.floor((new Date(Number(text)).getTime() - Date.now()) / (60 * 60 * 24 * 1000))
          },
          width: '100px'
        },
        {
          title: this.$tl('p.fileStatus'),
          dataIndex: 'exists',
          ellipsis: true,

          width: '80px'
        },
        {
          title: this.$tl('p.creator'),
          dataIndex: 'createUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$tl('p.modifier'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$tl('p.creationTime'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.modificationTime'),
          dataIndex: 'modifyTimeMillis',
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
      sourceMap,
      statusMap,
      fileList: [],
      percentage: 0,
      percentageInfo: {},
      uploading: false,
      uploadVisible: false,
      editVisible: false,
      uploadRemoteFileVisible: false,

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
      return this.$t(`pages.fileManager.fileStorage.list.${key}`, ...args)
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
      fileStorageList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    handleUpload() {
      this.temp = {
        global: false
      }
      this.uploadVisible = true
      this.$refs['form']?.resetFields()
    },
    // 上传文件
    handleUploadOk() {
      // 检验表单
      this.$refs['form'].validate().then(() => {
        // 判断文件
        if (this.fileList.length === 0) {
          $notification.success({
            message: this.$tl('p.pleaseSelectFile')
          })
          return false
        }
        this.percentage = 0
        this.percentageInfo = {}
        this.uploading = true
        this.confirmLoading = true
        uploadPieces({
          file: this.fileList[0],
          resolveFileProcess: (msg) => {
            this.globalLoading({
              spinning: true,
              tip: msg
            })
          },
          resolveFileEnd: () => {
            this.globalLoading(false)
          },
          uploadBeforeAbrot: (md5) => {
            return new Promise((resolve) => {
              hasFile({
                fileSumMd5: md5
              }).then((res) => {
                if (res.code === 200) {
                  if (res.data) {
                    //
                    $notification.warning({
                      message: `${this.$tl('p.fileAlreadyExists')},${this.$tl('c.fileName')}${res.data.name} ,${this.$tl('p.isShared')}${res.data.workspaceId === 'GLOBAL' ? this.$tl('p.yes') : this.$tl('p.no')}`
                    })
                    //
                    this.uploading = false
                    this.confirmLoading = false
                  } else {
                    resolve()
                  }
                }
              })
            })
          },
          process: (process, end, total, duration) => {
            this.percentage = Math.max(this.percentage, process)
            this.percentageInfo = { end, total, duration }
          },
          success: (uploadData) => {
            // 准备合并
            uploadFileMerge(Object.assign({}, { ...uploadData[0] }, this.temp))
              .then((res) => {
                if (res.code === 200) {
                  this.fileList = []
                  this.loadData()

                  $notification.success({
                    message: res.msg
                  })
                }
                setTimeout(() => {
                  this.percentage = 0
                  this.percentageInfo = {}
                }, 2000)
                this.uploadVisible = false
                this.uploading = false
              })
              .catch(() => {
                this.uploading = false
              })
              .finally(() => {
                this.confirmLoading = false
              })
          },
          error: (msg) => {
            $notification.error({
              message: msg
            })
            this.uploading = false
            this.confirmLoading = false
          },
          uploadCallback: (formData) => {
            return new Promise((resolve, reject) => {
              // 上传文件
              uploadFile(formData)
                .then((res) => {
                  if (res.code === 200) {
                    resolve()
                  } else {
                    reject()
                  }
                })
                .catch(() => {
                  reject()
                })
            })
          }
        })

        return true
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
        content: this.$tl('p.reallyDeleteCurrentFile') + record.name,
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return delFile({
            id: record.id
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
        $notification.error({
          message: this.$tl('p.noDataSelected')
        })
        return
      }
      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.reallyDeleteTheseFiles'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return delFile({ ids: this.tableSelections.join(',') }).then((res) => {
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
    // 远程下载
    handleRemoteDownload() {
      this.uploadRemoteFileVisible = true
      this.temp = {
        global: false
      }
      this.$refs['remoteForm']?.resetFields()
    },
    // 开始远程下载
    handleRemoteUpload() {
      //
      this.$refs['remoteForm'].validate().then(() => {
        this.confirmLoading = true
        remoteDownload(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.uploadRemoteFileVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
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
      addReleaseTask({ ...data, fileId: this.temp.fileId, fileType: 1 })
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
    }
  }
}
</script>

<style scoped>
:deep(.ant-progress-text) {
  width: auto;
}
</style>
