<template>
  <div>
    <div>
      <!-- 数据表格 -->
      <a-table
        :data-source="list"
        size="middle"
        :columns="columns"
        :pagination="pagination"
        @change="
          (pagination, filters, sorter) => {
            this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
            this.loadData()
          }
        "
        bordered
        rowKey="id"
        :row-selection="rowSelection"
        :scroll="{
          x: 'max-content'
        }"
      >
        <template v-slot:title>
          <a-space>
            <a-input
              v-model:value="listQuery['%name%']"
              @pressEnter="loadData"
              placeholder="文件名称"
              class="search-input-item"
            />

            <a-input
              v-model:value="listQuery['extName']"
              @pressEnter="loadData"
              placeholder="后缀,精准搜索"
              class="search-input-item"
            />
            <a-input
              v-model:value="listQuery['id']"
              @pressEnter="loadData"
              placeholder="文件id,精准搜索"
              class="search-input-item"
            />
            <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
              <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
            </a-tooltip>
            <!-- <a-button type="primary" @click="handleUpload">上传文件</a-button> -->
            <a-button type="primary" @click="reScanner">扫描</a-button>

            <a-button
              type="primary"
              danger
              :disabled="!tableSelections || tableSelections.length <= 0"
              @click="handleBatchDelete"
            >
              批量删除
            </a-button>
          </a-space>
        </template>
        <template #bodyCell="{ column, text, record, index }">
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
            <a-popover title="文件信息">
              <template v-slot:content>
                <p>文件ID：{{ record.id }}</p>
                <p>文件名：{{ text }}</p>
                <p>文件描述：{{ record.description }}</p>
              </template>
              <!-- {{ text }} -->
              <a-button type="link" style="padding: 0" @click="handleEdit(record)" size="small">{{ text }}</a-button>
            </a-popover>
          </template>

          <template v-else-if="column.dataIndex === 'size'">
            <a-tooltip placement="topLeft" :title="renderSize(text)">
              <span>{{ renderSize(text) }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'source'">
            <a-tooltip placement="topLeft" :title="`${sourceMap[text] || '未知'}`">
              <span>{{ sourceMap[text] || '未知' }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'status'">
            <a-tag v-if="text === 1" color="green">存在</a-tag>
            <a-tag v-else color="red">丢失</a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'type'">
            <a-tag v-if="text === 1">文件</a-tag>
            <a-tag v-else>文件夹</a-tag>
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
                下载</a-button
              >
              <a-button
                size="small"
                :disabled="!(record.status === 1 && record.type === 1)"
                type="primary"
                @click="handleReleaseFile(record)"
                >发布</a-button
              >
              <a-button type="primary" danger size="small" @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <!-- 编辑文件 -->
      <a-modal
        destroyOnClose
        v-model:open="editVisible"
        :title="`修改文件`"
        :confirmLoading="confirmLoading"
        @ok="handleEditOk"
        :maskClosable="false"
      >
        <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-form-item label="文件名" name="name">
            <a-input placeholder="文件名" :disabled="true" v-model:value="temp.name" />
          </a-form-item>

          <a-form-item label="文件描述" name="description">
            <a-textarea v-model:value="temp.description" placeholder="请输入文件描述" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 断点下载 -->
      <a-modal
        destroyOnClose
        v-model:open="triggerVisible"
        title="断点/分片下载"
        width="50%"
        :footer="null"
        :maskClosable="false"
      >
        <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-tabs default-active-key="1">
            <template v-slot:rightExtra>
              <a-tooltip title="重置下载 token 信息,重置后之前的下载 token 将失效">
                <a-button type="primary" size="small" @click="resetTrigger">重置</a-button>
              </a-tooltip>
            </template>
            <a-tab-pane key="1" tab="断点/分片单文件下载">
              <a-space direction="vertical" style="width: 100%">
                <a-alert type="info" :message="`下载地址(点击可以复制)`">
                  <template v-slot:description>
                    <a-typography-paragraph :copyable="{ text: temp.triggerDownloadUrl }">
                      <a-tag>GET</a-tag>
                      <span>{{ `${temp.triggerDownloadUrl}` }} </span>
                    </a-typography-paragraph>
                  </template>
                </a-alert>
                <a :href="temp.triggerDownloadUrl" target="_blank">
                  <a-button size="small" type="primary"><DownloadOutlined />立即下载</a-button>
                </a>
              </a-space>
            </a-tab-pane>
            <a-tab-pane tab="断点/分片别名下载" v-if="temp.triggerAliasDownloadUrl">
              <a-space direction="vertical" style="width: 100%">
                <a-alert message="温馨提示" type="warning">
                  <template v-slot:description>
                    <ul>
                      <li>
                        支持自定义排序字段：sort=createTimeMillis:desc

                        <p>描述根据创建时间升序第一个</p>
                      </li>
                      <li>支持的字段可以通过接口返回的查看</li>
                      <li>通用的字段有：createTimeMillis、modifyTimeMillis</li>
                    </ul>
                  </template>
                </a-alert>
                <a-alert type="info" :message="`下载地址(点击可以复制)`">
                  <template v-slot:description>
                    <a-typography-paragraph :copyable="{ text: temp.triggerAliasDownloadUrl }">
                      <a-tag>GET</a-tag>
                      <span>{{ `${temp.triggerAliasDownloadUrl}` }} </span>
                    </a-typography-paragraph>
                  </template>
                </a-alert>
                <a :href="temp.triggerAliasDownloadUrl" target="_blank">
                  <a-button size="small" type="primary"><DownloadOutlined />立即下载</a-button>
                </a>
              </a-space>
            </a-tab-pane>
          </a-tabs>
        </a-form>
      </a-modal>
      <!-- 发布文件 -->
      <a-modal
        destroyOnClose
        v-model:open="releaseFileVisible"
        title="发布文件"
        width="50%"
        :maskClosable="false"
        :confirmLoading="confirmLoading"
        @ok="releaseFileOk()"
      >
        <releaseFile ref="releaseFile" v-if="releaseFileVisible" @commit="handleCommitTask"></releaseFile>
      </a-modal>
    </div>
    <!-- 选择确认区域
    <div style="padding-top: 50px" v-if="this.choose">
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
                this.$emit('cancel')
              }
            "
          >
            取消
          </a-button>
          <a-button type="primary" @click="handerConfirm"> 确定 </a-button>
        </a-space>
      </div>
    </div>
    -->
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

export default {
  components: {
    releaseFile
  },
  props: {
    choose: {
      // "radio"
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: true,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      columns: [
        {
          title: '名称',
          dataIndex: 'name',
          ellipsis: true,
          width: 150
        },
        {
          title: '描述',
          dataIndex: 'description',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: '路径',
          dataIndex: 'absolutePath',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: '大小',
          dataIndex: 'size',
          sorter: true,
          ellipsis: true,

          width: '100px'
        },
        {
          title: '后缀',
          dataIndex: 'extName',
          ellipsis: true,

          tooltip: true,
          width: '80px'
        },
        {
          title: '类型',
          dataIndex: 'type',
          ellipsis: true,

          width: '80px'
        },
        {
          title: '文件状态',
          dataIndex: 'status',
          ellipsis: true,

          width: '80px'
        },

        {
          title: '文件修改时间',
          dataIndex: 'lastModified',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },

        {
          title: '操作',
          dataIndex: 'operation',
          align: 'center',
          ellipsis: true,

          fixed: 'right',
          width: '170px'
        }
      ],
      rules: {
        name: [{ required: true, message: '请输入文件名称', trigger: 'blur' }],
        url: [{ required: true, message: '请输入远程地址', trigger: 'blur' }]
      },

      temp: {},

      fileList: [],
      percentage: 0,
      percentageInfo: {},
      uploading: false,
      uploadVisible: false,
      editVisible: false,

      tempVue: null,
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
              this.$notification.success({
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
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除当前文件么？' + record.name,
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          delFile({
            id: record.id,
            thorough: false
          }).then((res) => {
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
    // 批量删除
    handleBatchDelete() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        this.$notification.warning({
          message: '没有选择任何数据'
        })
        return
      }
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除这些文件么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          delFile({
            ids: this.tableSelections.join(','),
            thorough: false
          }).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
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
      this.tempVue = Vue
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
          this.$notification.success({
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
            this.$notification.success({
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
        this.$notification.warning({
          message: '请选择要使用的文件'
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return this.tableSelections.indexOf(item.id) > -1
      })
      if (!selectData.length) {
        this.$notification.warning({
          message: '请选择要使用的文件'
        })
        return
      }
      this.$emit('confirm', selectData)
    },
    // 扫描
    reScanner() {
      staticScanner({}).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    }
  },
  emits: ['cancel', 'confirm']
}
</script>

<style scoped>
/deep/ .ant-progress-text {
  width: auto;
}
</style>
