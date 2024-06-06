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
              :placeholder="$t('pages.file-manager.fileStorage.list.a6eb2ded')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['%aliasCode%']"
              :placeholder="$t('pages.file-manager.fileStorage.list.aaee5dc9')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['extName']"
              :placeholder="$t('pages.file-manager.fileStorage.list.52822a45')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['id']"
              :placeholder="$t('pages.file-manager.fileStorage.list.48f7e53e')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-tooltip :title="$t('pages.file-manager.fileStorage.list.cb5a8131')">
              <a-button type="primary" :loading="loading" @click="loadData">{{
                $t('pages.file-manager.fileStorage.list.53c2763c')
              }}</a-button>
            </a-tooltip>
            <a-button type="primary" @click="handleUpload">{{
              $t('pages.file-manager.fileStorage.list.487993f7')
            }}</a-button>
            <a-button type="primary" @click="handleRemoteDownload">{{
              $t('pages.file-manager.fileStorage.list.367f115c')
            }}</a-button>
            <a-button
              type="primary"
              danger
              :disabled="!tableSelections || tableSelections.length <= 0"
              @click="handleBatchDelete"
            >
              {{ $t('pages.file-manager.fileStorage.list.b5139d46') }}
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
            <a-popover :title="$t('pages.file-manager.fileStorage.list.334162bc')">
              <template #content>
                <p>{{ $t('pages.file-manager.fileStorage.list.6a721706') }}{{ text }}</p>
                <p>{{ $t('pages.file-manager.fileStorage.list.7cee55c0') }}{{ record.description }}</p>
                <p v-if="record.status !== undefined">
                  {{ $t('pages.file-manager.fileStorage.list.5e3ea76a')
                  }}{{ statusMap[record.status] || $t('pages.file-manager.fileStorage.list.5f51a112') }}
                </p>
                <p v-if="record.progressDesc">
                  {{ $t('pages.file-manager.fileStorage.list.a622e852') }}{{ record.progressDesc }}
                </p>
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
            <a-tooltip
              placement="topLeft"
              :title="sourceMap[text] || $t('pages.file-manager.fileStorage.list.5f51a112')"
            >
              <span>{{ sourceMap[text] || $t('pages.file-manager.fileStorage.list.5f51a112') }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'exists'">
            <a-tag v-if="text" color="green">{{ $t('pages.file-manager.fileStorage.list.a78774e2') }}</a-tag>
            <a-tag v-else color="red">{{ $t('pages.file-manager.fileStorage.list.a51066b1') }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'workspaceId'">
            <a-tag v-if="text === 'GLOBAL'">{{ $t('pages.file-manager.fileStorage.list.fd0310d0') }}</a-tag>
            <a-tag v-else>{{ $t('pages.file-manager.fileStorage.list.afacc4cb') }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'operation'">
            <a-space>
              <!-- <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button> -->
              <a-button size="small" :disabled="!record.exists" type="primary" @click="handleDownloadUrl(record)">{{
                $t('pages.file-manager.fileStorage.list.42c8e9c6')
              }}</a-button>
              <a-button size="small" :disabled="!record.exists" type="primary" @click="handleReleaseFile(record)">{{
                $t('pages.file-manager.fileStorage.list.f97d7b7c')
              }}</a-button>
              <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
                $t('pages.file-manager.fileStorage.list.dd20d11c')
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
        :title="`${$t('pages.file-manager.fileStorage.list.487993f7')}`"
        :mask-closable="false"
        @ok="handleUploadOk"
      >
        <a-form ref="form" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-form-item :label="$t('pages.file-manager.fileStorage.list.2a688d49')" name="file">
            <a-progress v-if="percentage" :percent="percentage">
              <template #format="percent">
                {{ percent }}%
                <template v-if="percentageInfo.total"> ({{ renderSize(percentageInfo.total) }}) </template>
                <template v-if="percentageInfo.duration">
                  {{ $t('pages.file-manager.fileStorage.list.dbdaa87c') }}:{{ formatDuration(percentageInfo.duration) }}
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
              <a-button v-else type="primary"
                ><UploadOutlined />{{ $t('pages.file-manager.fileStorage.list.2a688d49') }}</a-button
              >
            </a-upload>
          </a-form-item>
          <a-form-item :label="$t('pages.file-manager.fileStorage.list.86a4093a')" name="keepDay">
            <a-input-number
              v-model:value="temp.keepDay"
              :min="1"
              style="width: 100%"
              :placeholder="$t('pages.file-manager.fileStorage.list.4915fce1')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.file-manager.fileStorage.list.ef32cc33')" name="global">
            <a-radio-group v-model:value="temp.global">
              <a-radio :value="true"> {{ $t('pages.file-manager.fileStorage.list.fd0310d0') }} </a-radio>
              <a-radio :value="false"> {{ $t('pages.file-manager.fileStorage.list.55b7e6b6') }} </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item
            :label="$t('pages.file-manager.fileStorage.list.aaee5dc9')"
            name="aliasCode"
            :help="$t('pages.file-manager.fileStorage.list.b88f3e29')"
          >
            <a-input-search
              v-model:value="temp.aliasCode"
              :max-length="50"
              :placeholder="$t('pages.file-manager.fileStorage.list.4aa61be9')"
              @search="
                () => {
                  temp = { ...temp, aliasCode: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <a-button type="primary"> {{ $t('pages.file-manager.fileStorage.list.8a1c9dde') }} </a-button>
              </template>
            </a-input-search>
          </a-form-item>
          <a-form-item :label="$t('pages.file-manager.fileStorage.list.38259739')" name="description">
            <a-textarea
              v-model:value="temp.description"
              :placeholder="$t('pages.file-manager.fileStorage.list.1f789f94')"
            />
          </a-form-item>
        </a-form>
      </a-modal>
      <!-- 编辑文件 -->
      <a-modal
        v-model:open="editVisible"
        destroy-on-close
        :confirm-loading="confirmLoading"
        :title="`${$t('pages.file-manager.fileStorage.list.41bfad43')}`"
        :mask-closable="false"
        @ok="handleEditOk"
      >
        <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-form-item label="文件名" name="name">
            <a-input v-model:value="temp.name" placeholder="文件名" />
          </a-form-item>
          <a-form-item :label="$t('pages.file-manager.fileStorage.list.86a4093a')" name="keepDay">
            <a-input-number
              v-model:value="temp.keepDay"
              :min="1"
              style="width: 100%"
              :placeholder="$t('pages.file-manager.fileStorage.list.4915fce1')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.file-manager.fileStorage.list.ef32cc33')" name="global">
            <a-radio-group v-model:value="temp.global">
              <a-radio :value="true"> {{ $t('pages.file-manager.fileStorage.list.fd0310d0') }} </a-radio>
              <a-radio :value="false"> {{ $t('pages.file-manager.fileStorage.list.55b7e6b6') }} </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item
            :label="$t('pages.file-manager.fileStorage.list.aaee5dc9')"
            name="aliasCode"
            :help="$t('pages.file-manager.fileStorage.list.b88f3e29')"
          >
            <a-input-search
              v-model:value="temp.aliasCode"
              :max-length="50"
              :placeholder="$t('pages.file-manager.fileStorage.list.4aa61be9')"
              @search="
                () => {
                  temp = { ...temp, aliasCode: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <a-button type="primary"> {{ $t('pages.file-manager.fileStorage.list.8a1c9dde') }} </a-button>
              </template>
            </a-input-search>
          </a-form-item>
          <a-form-item :label="$t('pages.file-manager.fileStorage.list.38259739')" name="description">
            <a-textarea
              v-model:value="temp.description"
              :placeholder="$t('pages.file-manager.fileStorage.list.1f789f94')"
            />
          </a-form-item>
        </a-form>
      </a-modal>
      <!--远程下载  -->
      <a-modal
        v-model:open="uploadRemoteFileVisible"
        destroy-on-close
        :title="$t('pages.file-manager.fileStorage.list.d78f10d3')"
        :mask-closable="false"
        :confirm-loading="confirmLoading"
        @ok="handleRemoteUpload"
      >
        <a-form ref="remoteForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" :rules="rules">
          <a-form-item :label="$t('pages.file-manager.fileStorage.list.3afe9b11')" name="url">
            <a-input v-model:value="temp.url" :placeholder="$t('pages.file-manager.fileStorage.list.40ced483')" />
          </a-form-item>
          <a-form-item :label="$t('pages.file-manager.fileStorage.list.86a4093a')" name="keepDay">
            <a-input-number
              v-model:value="temp.keepDay"
              :min="1"
              style="width: 100%"
              :placeholder="$t('pages.file-manager.fileStorage.list.4915fce1')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.file-manager.fileStorage.list.ef32cc33')" name="global">
            <a-radio-group v-model:value="temp.global">
              <a-radio :value="true"> {{ $t('pages.file-manager.fileStorage.list.fd0310d0') }} </a-radio>
              <a-radio :value="false"> {{ $t('pages.file-manager.fileStorage.list.55b7e6b6') }} </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item
            :label="$t('pages.file-manager.fileStorage.list.aaee5dc9')"
            name="aliasCode"
            :help="$t('pages.file-manager.fileStorage.list.b88f3e29')"
          >
            <a-input-search
              v-model:value="temp.aliasCode"
              :max-length="50"
              :placeholder="$t('pages.file-manager.fileStorage.list.4aa61be9')"
              @search="
                () => {
                  temp = { ...temp, aliasCode: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <a-button type="primary"> {{ $t('pages.file-manager.fileStorage.list.8a1c9dde') }} </a-button>
              </template>
            </a-input-search>
          </a-form-item>
          <a-form-item :label="$t('pages.file-manager.fileStorage.list.38259739')" name="description">
            <a-textarea
              v-model:value="temp.description"
              :placeholder="$t('pages.file-manager.fileStorage.list.1f789f94')"
            />
          </a-form-item>
        </a-form>
      </a-modal>
      <!-- 断点下载 -->
      <a-modal
        v-model:open="triggerVisible"
        destroy-on-close
        :title="$t('pages.file-manager.fileStorage.list.a7210bdc')"
        width="50%"
        :footer="null"
        :mask-closable="false"
      >
        <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-tabs default-active-key="1">
            <template #rightExtra>
              <a-tooltip :title="$t('pages.file-manager.fileStorage.list.27e4eccc')">
                <a-button type="primary" size="small" @click="resetTrigger">{{
                  $t('pages.file-manager.fileStorage.list.da1d2343')
                }}</a-button>
              </a-tooltip>
            </template>
            <a-tab-pane key="1" :tab="$t('pages.file-manager.fileStorage.list.12b6d613')">
              <a-space direction="vertical" style="width: 100%">
                <a-alert
                  type="info"
                  :message="`${$t('pages.file-manager.fileStorage.list.f0c647b3')}(${$t(
                    'pages.file-manager.fileStorage.list.a5873c3e'
                  )})`"
                >
                  <template #description>
                    <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerDownloadUrl }">
                      <a-tag>GET</a-tag>
                      <span>{{ `${temp.triggerDownloadUrl}` }} </span>
                    </a-typography-paragraph>
                  </template>
                </a-alert>
                <a :href="temp.triggerDownloadUrl" target="_blank">
                  <a-button size="small" type="primary"
                    ><DownloadOutlined />{{ $t('pages.file-manager.fileStorage.list.255cc40a') }}</a-button
                  >
                </a>
              </a-space>
            </a-tab-pane>
            <a-tab-pane v-if="temp.triggerAliasDownloadUrl" :tab="$t('pages.file-manager.fileStorage.list.c28d3bfb')">
              <a-space direction="vertical" style="width: 100%">
                <a-alert :message="$t('pages.file-manager.fileStorage.list.46d9b29')" type="warning">
                  <template #description>
                    <ul>
                      <li>
                        {{ $t('pages.file-manager.fileStorage.list.9568db65') }}=createTimeMillis:desc

                        <p>{{ $t('pages.file-manager.fileStorage.list.c0aa2813') }}</p>
                      </li>
                      <li>{{ $t('pages.file-manager.fileStorage.list.ce130623') }}</li>
                      <li>{{ $t('pages.file-manager.fileStorage.list.7d507a40') }}</li>
                    </ul>
                  </template>
                </a-alert>
                <a-alert
                  type="info"
                  :message="`${$t('pages.file-manager.fileStorage.list.f0c647b3')}(${$t(
                    'pages.file-manager.fileStorage.list.a5873c3e'
                  )})`"
                >
                  <template #description>
                    <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerAliasDownloadUrl }">
                      <a-tag>GET</a-tag>
                      <span>{{ `${temp.triggerAliasDownloadUrl}` }} </span>
                    </a-typography-paragraph>
                  </template>
                </a-alert>
                <a :href="temp.triggerAliasDownloadUrl" target="_blank">
                  <a-button size="small" type="primary"
                    ><DownloadOutlined />{{ $t('pages.file-manager.fileStorage.list.255cc40a') }}</a-button
                  >
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
        :title="$t('pages.file-manager.fileStorage.list.b104cb8')"
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
            {{ $t('pages.file-manager.fileStorage.list.43105e21') }}
          </a-button>
          <a-button type="primary" @click="handerConfirm">
            {{ $t('pages.file-manager.fileStorage.list.e8e9db25') }}
          </a-button>
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
          title: this.$t('pages.file-manager.fileStorage.list.d7154d9e'),
          dataIndex: 'id',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('pages.file-manager.fileStorage.list.bb769c1d'),
          dataIndex: 'name',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('pages.file-manager.fileStorage.list.aaee5dc9'),
          dataIndex: 'aliasCode',
          ellipsis: true,
          width: 100,
          tooltip: true
        },
        {
          title: this.$t('pages.file-manager.fileStorage.list.1295c671'),
          dataIndex: 'size',
          sorter: true,
          ellipsis: true,

          width: '100px'
        },
        {
          title: this.$t('pages.file-manager.fileStorage.list.be38837'),
          dataIndex: 'extName',
          ellipsis: true,
          tooltip: true,
          width: '80px'
        },
        {
          title: this.$t('pages.file-manager.fileStorage.list.f4be5920'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('pages.file-manager.fileStorage.list.b8b8d2e8'),
          dataIndex: 'source',
          ellipsis: true,

          width: '80px'
        },
        {
          title: this.$t('pages.file-manager.fileStorage.list.38140da6'),
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
          title: this.$t('pages.file-manager.fileStorage.list.34e8b7bd'),
          dataIndex: 'exists',
          ellipsis: true,

          width: '80px'
        },
        {
          title: this.$t('pages.file-manager.fileStorage.list.db3c9202'),
          dataIndex: 'createUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$t('pages.file-manager.fileStorage.list.916db24b'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$t('pages.file-manager.fileStorage.list.f06e8846'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.file-manager.fileStorage.list.61164914'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.file-manager.fileStorage.list.3bb962bf'),
          dataIndex: 'operation',
          align: 'center',
          ellipsis: true,

          fixed: 'right',
          width: '170px'
        }
      ],
      rules: {
        name: [{ required: true, message: this.$t('pages.file-manager.fileStorage.list.20c02197'), trigger: 'blur' }],
        url: [{ required: true, message: this.$t('pages.file-manager.fileStorage.list.cc2bc679'), trigger: 'blur' }]
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
            message: this.$t('pages.file-manager.fileStorage.list.25d31749')
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
                      message: `${this.$t('pages.file-manager.fileStorage.list.67888d26')},${this.$t(
                        'pages.file-manager.fileStorage.list.6a721706'
                      )}${res.data.name} ,${this.$t('pages.file-manager.fileStorage.list.e83695d7')}${
                        res.data.workspaceId === 'GLOBAL'
                          ? this.$t('pages.file-manager.fileStorage.list.f5bb2364')
                          : this.$t('pages.file-manager.fileStorage.list.5edb2e8a')
                      }`
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
        title: this.$t('pages.file-manager.fileStorage.list.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.file-manager.fileStorage.list.533f191c') + record.name,
        okText: this.$t('pages.file-manager.fileStorage.list.7da4a591'),
        cancelText: this.$t('pages.file-manager.fileStorage.list.43105e21'),
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
          message: this.$t('pages.file-manager.fileStorage.list.2ad67cae')
        })
        return
      }
      $confirm({
        title: this.$t('pages.file-manager.fileStorage.list.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.file-manager.fileStorage.list.8cb13ac8'),
        okText: this.$t('pages.file-manager.fileStorage.list.7da4a591'),
        cancelText: this.$t('pages.file-manager.fileStorage.list.43105e21'),
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
          message: this.$t('pages.file-manager.fileStorage.list.e1f69633')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return this.tableSelections.indexOf(item.id) > -1
      })
      if (!selectData.length) {
        $notification.warning({
          message: this.$t('pages.file-manager.fileStorage.list.e1f69633')
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
