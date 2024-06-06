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
              :placeholder="$t('pages.file-manager.staticFileStorage.list.a6eb2ded')"
              class="search-input-item"
              @press-enter="loadData"
            />

            <a-input
              v-model:value="listQuery['extName']"
              :placeholder="$t('pages.file-manager.staticFileStorage.list.4fd0f685')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['id']"
              :placeholder="$t('pages.file-manager.staticFileStorage.list.8200285')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-tooltip :title="$t('pages.file-manager.staticFileStorage.list.cb5a8131')">
              <a-button type="primary" :loading="loading" @click="loadData">{{
                $t('pages.file-manager.staticFileStorage.list.53c2763c')
              }}</a-button>
            </a-tooltip>
            <!-- <a-button type="primary" @click="handleUpload">上传文件</a-button> -->
            <a-button type="primary" @click="reScanner">{{
              $t('pages.file-manager.staticFileStorage.list.21e651b5')
            }}</a-button>

            <a-button
              type="primary"
              danger
              :disabled="!tableSelections || tableSelections.length <= 0"
              @click="handleBatchDelete"
            >
              {{ $t('pages.file-manager.staticFileStorage.list.b5139d46') }}
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
              <InfoCircleOutlined /> {{ $t('pages.file-manager.staticFileStorage.list.1ab4dc38') }}
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
            <a-popover :title="$t('pages.file-manager.staticFileStorage.list.334162bc')">
              <template #content>
                <p>{{ $t('pages.file-manager.staticFileStorage.list.48f7e53e') }}{{ record.id }}</p>
                <p>{{ $t('pages.file-manager.staticFileStorage.list.10902f4f') }}{{ text }}</p>
                <p>{{ $t('pages.file-manager.staticFileStorage.list.fa56a3f7') }}{{ record.description }}</p>
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
              :title="`${sourceMap[text] || $t('pages.file-manager.staticFileStorage.list.da636096')}`"
            >
              <span>{{ sourceMap[text] || $t('pages.file-manager.staticFileStorage.list.da636096') }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'status'">
            <a-tag v-if="text === 1" color="green">{{
              $t('pages.file-manager.staticFileStorage.list.a78774e2')
            }}</a-tag>
            <a-tag v-else color="red">{{ $t('pages.file-manager.staticFileStorage.list.979a03b8') }}</a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'type'">
            <a-tag v-if="text === 1">{{ $t('pages.file-manager.staticFileStorage.list.69cad40b') }}</a-tag>
            <a-tag v-else>{{ $t('pages.file-manager.staticFileStorage.list.b90a456') }}</a-tag>
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
                {{ $t('pages.file-manager.staticFileStorage.list.42c8e9c6') }}</a-button
              >
              <a-button
                size="small"
                :disabled="!(record.status === 1 && record.type === 1)"
                type="primary"
                @click="handleReleaseFile(record)"
                >{{ $t('pages.file-manager.staticFileStorage.list.dfaeb420') }}</a-button
              >
              <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
                $t('pages.file-manager.staticFileStorage.list.d7cfa4c5')
              }}</a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <!-- 编辑文件 -->
      <a-modal
        v-model:open="editVisible"
        destroy-on-close
        :title="`${$t('pages.file-manager.staticFileStorage.list.41bfad43')}`"
        :confirm-loading="confirmLoading"
        :mask-closable="false"
        @ok="handleEditOk"
      >
        <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-form-item :label="$t('pages.file-manager.staticFileStorage.list.6a721706')" name="name">
            <a-input
              v-model:value="temp.name"
              :placeholder="$t('pages.file-manager.staticFileStorage.list.6a721706')"
              :disabled="true"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.file-manager.staticFileStorage.list.7cee55c0')" name="description">
            <a-textarea
              v-model:value="temp.description"
              :placeholder="$t('pages.file-manager.staticFileStorage.list.3d17ed2c')"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 断点下载 -->
      <a-modal
        v-model:open="triggerVisible"
        destroy-on-close
        :title="$t('pages.file-manager.staticFileStorage.list.b1634db3')"
        width="50%"
        :footer="null"
        :mask-closable="false"
      >
        <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-tabs default-active-key="1">
            <template #rightExtra>
              <a-tooltip :title="$t('pages.file-manager.staticFileStorage.list.27e4eccc')">
                <a-button type="primary" size="small" @click="resetTrigger">{{
                  $t('pages.file-manager.staticFileStorage.list.da1d2343')
                }}</a-button>
              </a-tooltip>
            </template>
            <a-tab-pane key="1" :tab="$t('pages.file-manager.staticFileStorage.list.f0649f07')">
              <a-space direction="vertical" style="width: 100%">
                <a-alert
                  type="info"
                  :message="`${$t('pages.file-manager.staticFileStorage.list.f0c647b3')}(${$t(
                    'pages.file-manager.staticFileStorage.list.4c8d1a3b'
                  )})`"
                >
                  <template #description>
                    <a-typography-paragraph :copyable="{ text: temp.triggerDownloadUrl }">
                      <a-tag>GET</a-tag>
                      <span>{{ `${temp.triggerDownloadUrl}` }} </span>
                    </a-typography-paragraph>
                  </template>
                </a-alert>
                <a :href="temp.triggerDownloadUrl" target="_blank">
                  <a-button size="small" type="primary"
                    ><DownloadOutlined />{{ $t('pages.file-manager.staticFileStorage.list.2fadc8ac') }}</a-button
                  >
                </a>
              </a-space>
            </a-tab-pane>
            <a-tab-pane
              v-if="temp.triggerAliasDownloadUrl"
              :tab="$t('pages.file-manager.staticFileStorage.list.81630ff')"
            >
              <a-space direction="vertical" style="width: 100%">
                <a-alert :message="$t('pages.file-manager.staticFileStorage.list.46d9b29')" type="warning">
                  <template #description>
                    <ul>
                      <li>
                        {{ $t('pages.file-manager.staticFileStorage.list.9568db65') }}=createTimeMillis:desc

                        <p>{{ $t('pages.file-manager.staticFileStorage.list.8c75e50e') }}</p>
                      </li>
                      <li>{{ $t('pages.file-manager.staticFileStorage.list.ce130623') }}</li>
                      <li>{{ $t('pages.file-manager.staticFileStorage.list.7d507a40') }}</li>
                    </ul>
                  </template>
                </a-alert>
                <a-alert
                  type="info"
                  :message="`${$t('pages.file-manager.staticFileStorage.list.f0c647b3')}(${$t(
                    'pages.file-manager.staticFileStorage.list.4c8d1a3b'
                  )})`"
                >
                  <template #description>
                    <a-typography-paragraph :copyable="{ text: temp.triggerAliasDownloadUrl }">
                      <a-tag>GET</a-tag>
                      <span>{{ `${temp.triggerAliasDownloadUrl}` }} </span>
                    </a-typography-paragraph>
                  </template>
                </a-alert>
                <a :href="temp.triggerAliasDownloadUrl" target="_blank">
                  <a-button size="small" type="primary"
                    ><DownloadOutlined />{{ $t('pages.file-manager.staticFileStorage.list.2fadc8ac') }}</a-button
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
        :title="$t('pages.file-manager.staticFileStorage.list.68203efe')"
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
      :title="`${$t('pages.file-manager.staticFileStorage.list.4457e1f9')}`"
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
          title: this.$t('pages.file-manager.staticFileStorage.list.bb769c1d'),
          dataIndex: 'name',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('pages.file-manager.staticFileStorage.list.eee04e4f'),
          dataIndex: 'description',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('pages.file-manager.staticFileStorage.list.ee016914'),
          dataIndex: 'absolutePath',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('pages.file-manager.staticFileStorage.list.1295c671'),
          dataIndex: 'size',
          sorter: true,
          ellipsis: true,

          width: '100px'
        },
        {
          title: this.$t('pages.file-manager.staticFileStorage.list.52822a45'),
          dataIndex: 'extName',
          ellipsis: true,

          tooltip: true,
          width: '80px'
        },
        {
          title: this.$t('pages.file-manager.staticFileStorage.list.698bb532'),
          dataIndex: 'type',
          ellipsis: true,

          width: '80px'
        },
        {
          title: this.$t('pages.file-manager.staticFileStorage.list.34e8b7bd'),
          dataIndex: 'status',
          ellipsis: true,

          width: '80px'
        },

        {
          title: this.$t('pages.file-manager.staticFileStorage.list.c15532f1'),
          dataIndex: 'lastModified',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },

        {
          title: this.$t('pages.file-manager.staticFileStorage.list.3bb962bf'),
          dataIndex: 'operation',
          align: 'center',
          ellipsis: true,

          fixed: 'right',
          width: '170px'
        }
      ],
      rules: {
        name: [
          { required: true, message: this.$t('pages.file-manager.staticFileStorage.list.20c02197'), trigger: 'blur' }
        ],
        url: [
          { required: true, message: this.$t('pages.file-manager.staticFileStorage.list.cc2bc679'), trigger: 'blur' }
        ]
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
        title: this.$t('pages.file-manager.staticFileStorage.list.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.file-manager.staticFileStorage.list.639bed63') + record.name,
        okText: this.$t('pages.file-manager.staticFileStorage.list.7da4a591'),
        cancelText: this.$t('pages.file-manager.staticFileStorage.list.43105e21'),
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
          message: this.$t('pages.file-manager.staticFileStorage.list.2ad67cae')
        })
        return
      }
      $confirm({
        title: this.$t('pages.file-manager.staticFileStorage.list.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.file-manager.staticFileStorage.list.cfd66404'),
        okText: this.$t('pages.file-manager.staticFileStorage.list.7da4a591'),
        cancelText: this.$t('pages.file-manager.staticFileStorage.list.43105e21'),
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
          message: this.$t('pages.file-manager.staticFileStorage.list.e1f69633')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return this.tableSelections.indexOf(item.id) > -1
      })
      if (!selectData.length) {
        $notification.warning({
          message: this.$t('pages.file-manager.staticFileStorage.list.e1f69633')
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
