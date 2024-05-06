<template>
  <div>
    <a-table
      size="middle"
      :data-source="commandList"
      :columns="columns"
      bordered
      :pagination="pagination"
      row-key="id"
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
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$tl('c.taskName')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.status"
            show-search
            :filter-option="
              (input, option) => {
                const children = option.children && option.children()
                return (
                  children &&
                  children[0].children &&
                  children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                )
              }
            "
            allow-clear
            :placeholder="$tl('c.status')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.taskType"
            show-search
            :filter-option="
              (input, option) => {
                const children = option.children && option.children()
                return (
                  children &&
                  children[0].children &&
                  children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                )
              }
            "
            allow-clear
            :placeholder="$tl('p.publishType')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in taskTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-tooltip :title="$tl('p.fastReturnFirstPage')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'fileId'">
          <a-tooltip :title="text">
            <a-button type="link" style="padding: 0" size="small" @click="handleViewFile(record)">{{
              (text || '').slice(0, 8)
            }}</a-button>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <a-tag v-if="text === 2" color="green">{{ statusMap[text] || $tl('c.unknown') }}</a-tag>
          <a-tag v-else-if="text === 0 || text === 1" color="orange">{{ statusMap[text] || $tl('c.unknown') }}</a-tag>
          <a-tag v-else-if="text === 4" color="blue">
            {{ statusMap[text] || $tl('c.unknown') }}
          </a-tag>
          <a-tag v-else-if="text === 3" color="red">{{ statusMap[text] || $tl('c.unknown') }}</a-tag>
          <a-tag v-else>{{ statusMap[text] || $tl('c.unknown') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'taskType'">
          <span>{{ taskTypeMap[text] || $tl('c.unknown') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'fileType'">
          <span v-if="text == 2">{{ $tl('p.staticFile') }}</span>
          <span v-else>{{ $tl('p.fileCenter') }}</span>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleView(record)">{{ $tl('p.view') }}</a-button>

            <a-button type="primary" size="small" @click="handleRetask(record)">{{ $tl('p.rebuild') }}</a-button>
            <a-button
              type="primary"
              danger
              size="small"
              :disabled="!(record.status === 0 || record.status === 1)"
              @click="handleCancelTask(record)"
              >{{ $tl('c.cancel') }}</a-button
            >
            <a-button
              type="primary"
              danger
              size="small"
              :disabled="record.status === 0 || record.status === 1"
              @click="handleDelete(record)"
              >{{ $tl('p.delete') }}</a-button
            >
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 任务详情 -->
    <a-drawer
      :title="$tl('p.taskDetail')"
      placement="right"
      :width="'80vw'"
      :open="detailsVisible"
      @close="
        () => {
          detailsVisible = false
        }
      "
    >
      <task-details-page v-if="detailsVisible" :task-id="temp.id" />
    </a-drawer>
    <!-- 重建任务 -->
    <a-modal
      v-model:open="releaseFileVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$tl('p.publishFile')"
      width="60%"
      :mask-closable="false"
      @ok="handleReCrateTask"
    >
      <a-form
        ref="releaseFileForm"
        :rules="releaseFileRules"
        :model="temp"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item :label="$tl('c.taskName')" name="name">
          <a-input v-model:value="temp.name" :placeholder="$tl('p.pleaseInputTaskName')" :max-length="50" />
        </a-form-item>

        <a-form-item :label="$tl('p.publishWay')" name="taskType">
          <a-radio-group v-model:value="temp.taskType" :disabled="true">
            <a-radio :value="0"> SSH </a-radio>
            <a-radio :value="1"> {{ $tl('p.node') }} </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item v-if="temp.taskType === 0" name="taskDataIds" :label="$tl('p.publishSsh')">
          <a-row>
            <a-col :span="22">
              <a-select
                v-model:value="temp.taskDataIds"
                show-search
                :filter-option="
                  (input, option) => {
                    const children = option.children && option.children()
                    return (
                      children &&
                      children[0].children &&
                      children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    )
                  }
                "
                mode="multiple"
                :placeholder="$tl('p.pleaseSelectSsh')"
              >
                <a-select-option v-for="ssh in sshList" :key="ssh.id">
                  <a-tooltip :title="ssh.name"> {{ ssh.name }}</a-tooltip>
                </a-select-option>
              </a-select>
            </a-col>
            <a-col :span="1" style="margin-left: 10px">
              <ReloadOutlined @click="loadSshList" />
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item v-else-if="temp.taskType === 1" name="taskDataIds" :label="$tl('p.publishNode')">
          <a-row>
            <a-col :span="22">
              <a-select
                v-model:value="temp.taskDataIds"
                show-search
                :filter-option="
                  (input, option) => {
                    const children = option.children && option.children()
                    return (
                      children &&
                      children[0].children &&
                      children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    )
                  }
                "
                mode="multiple"
                :placeholder="$tl('p.pleaseSelectNode')"
              >
                <a-select-option v-for="ssh in nodeList" :key="ssh.id">
                  <a-tooltip :title="ssh.name"> {{ ssh.name }}</a-tooltip>
                </a-select-option>
              </a-select>
            </a-col>
            <a-col :span="1" style="margin-left: 10px">
              <ReloadOutlined @click="loadNodeList" />
            </a-col>
          </a-row>
        </a-form-item>

        <a-form-item name="releasePathParent" :label="$tl('c.publishDir')">
          <a-input v-model:value="temp.releasePath" :placeholder="$tl('p.pleaseInputPublishDir')" :disabled="true" />
        </a-form-item>

        <a-form-item name="releasePathParent" :label="$tl('p.fileIdInput')">
          <a-input v-model:value="temp.fileId" :placeholder="$tl('p.pleaseInputPublishFileId')" />
        </a-form-item>

        <a-form-item :label="$tl('p.executeScript')" name="releaseBeforeCommand">
          <a-form-item-rest>
            <a-tabs tab-position="right">
              <a-tab-pane key="before" :tab="$tl('p.beforeUpload')">
                <code-editor
                  v-model:content="temp.beforeScript"
                  height="40vh"
                  :options="{
                    mode: 'shell'
                  }"
                ></code-editor>

                <div style="margin-top: 10px">{{ $tl('p.beforeUploadScript') }}</div>
              </a-tab-pane>
              <a-tab-pane key="after" :tab="$tl('p.afterUpload')">
                <code-editor
                  v-model:content="temp.afterScript"
                  height="40vh"
                  :options="{
                    mode: 'shell'
                  }"
                ></code-editor>

                <div style="margin-top: 10px">{{ $tl('p.afterUploadScript') }}</div>
              </a-tab-pane>
            </a-tabs>
          </a-form-item-rest>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 查看文件 -->
    <a-modal
      v-model:open="viewFileVisible"
      destroy-on-close
      :title="`${$tl('p.viewFile')}`"
      :footer="null"
      :mask-closable="false"
    >
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$tl('p.fileName')" name="name">
          {{ temp.name }}
        </a-form-item>
        <a-form-item :label="$tl('c.fileId')" name="name">
          {{ temp.id }}
        </a-form-item>
        <a-form-item :label="$tl('p.fileSize')" name="size">
          {{ renderSize(temp.size) }}
        </a-form-item>
        <a-form-item v-if="temp.validUntil" :label="$tl('p.expireTime')" name="validUntil">
          {{ parseTime(temp.validUntil) }}
        </a-form-item>
        <a-form-item v-if="temp.workspaceId" :label="$tl('p.fileShare')" name="global">
          {{ temp.workspaceId === 'GLOBAL' ? $tl('p.global') : $tl('p.workspace') }}
        </a-form-item>
        <a-form-item :label="$tl('p.fileDescription')" name="description">
          {{ temp.description }}
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import {
  fileReleaseTaskLog,
  statusMap,
  taskTypeMap,
  taskDetails,
  reReleaseTask,
  cancelReleaseTask,
  deleteReleaseTask
} from '@/api/file-manager/release-task-log'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime, renderSize } from '@/utils/const'
import taskDetailsPage from './details.vue'
import { getSshListAll } from '@/api/ssh'
import codeEditor from '@/components/codeEditor'
import { hasFile } from '@/api/file-manager/file-storage'
import { getNodeListAll } from '@/api/node'
import { hasStaticFile } from '@/api/file-manager/static-storage'
export default {
  components: {
    taskDetailsPage,
    codeEditor
  },
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      commandList: [],
      loading: false,
      temp: {},
      statusMap,
      taskTypeMap,
      detailsVisible: false,
      confirmLoading: false,
      columns: [
        {
          title: this.$tl('p.taskName'),
          dataIndex: 'name',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$tl('p.distributeType'),
          dataIndex: 'taskType',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$tl('p.fileSource'),
          dataIndex: 'fileType',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$tl('c.status'),
          dataIndex: 'status',
          width: '100px',
          ellipsis: true
        },

        {
          title: this.$tl('p.statusDescription'),
          dataIndex: 'statusMsg',
          ellipsis: true,
          width: 200,
          tooltip: true
        },
        {
          title: this.$tl('c.fileId'),
          dataIndex: 'fileId',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$tl('c.publishDir'),
          dataIndex: 'releasePath',
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.executor'),
          dataIndex: 'modifyUser',
          width: '120px',
          ellipsis: true
        },
        {
          title: this.$tl('p.taskTime'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.taskUpdateTime'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },

        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',
          align: 'center',

          fixed: 'right',
          width: '230px'
        }
      ],
      sshList: [],
      nodeList: [],
      releaseFileVisible: false,
      releaseFileRules: {
        name: [{ required: true, message: this.$tl('p.pleaseInputFileTaskName'), trigger: 'blur' }],

        taskDataIds: [{ required: true, message: this.$tl('p.pleaseSelectPublishSsh'), trigger: 'blur' }]
      },
      viewFileVisible: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.fileManager.releaseTask.list.${key}`, ...args)
    },
    CHANGE_PAGE,
    renderSize,
    parseTime,
    handleView(row) {
      this.temp = { ...row }
      this.detailsVisible = true
    },

    // 获取命令数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      fileReleaseTaskLog(this.listQuery).then((res) => {
        if (200 === res.code) {
          this.commandList = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },

    //  删除命令
    handleDelete(row) {
      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.reallyDeleteRecord'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return deleteReleaseTask({
            id: row.id
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
    // 加载 SSH 列表
    loadSshList() {
      return new Promise((resolve) => {
        this.sshList = []
        getSshListAll().then((res) => {
          if (res.code === 200) {
            this.sshList = res.data
            resolve()
          }
        })
      })
    },
    // 加载节点
    loadNodeList() {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data
        }
      })
    },
    // 重建任务
    handleRetask(row) {
      taskDetails({
        id: row.id
      }).then((res) => {
        if (res.code === 200) {
          const taskData = res.data?.taskData
          this.temp = taskData
          delete this.temp.statusMsg
          delete this.temp.id
          if (taskData?.taskType === 0) {
            this.loadSshList()
          } else if (taskData?.taskType === 1) {
            this.loadNodeList()
          }
          const taskList = res.data?.taskList || []
          this.temp = {
            ...this.temp,
            taskDataIds: taskList.map((item) => {
              return item.taskDataId
            }),
            parentTaskId: row.id
          }
          this.releaseFileVisible = true
        }
      })
    },
    // 创建任务
    handleReCrateTask() {
      this.$refs['releaseFileForm'].validate().then(() => {
        this.confirmLoading = true
        reReleaseTask({
          ...this.temp,
          taskDataIds: this.temp.taskDataIds?.join(',')
        })
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
      })
    },
    // 取消
    handleCancelTask(record) {
      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.reallyCancelTask'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        oonOk: () => {
          return cancelReleaseTask({ id: record.id }).then((res) => {
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
    // 查看文件
    handleViewFile(record) {
      if (record.fileType === 2) {
        //
        hasStaticFile({
          fileId: record.fileId
        }).then((res) => {
          if (res.code === 200) {
            if (res.data) {
              this.temp = res.data
              this.viewFileVisible = true
            } else {
              $notification.warning({
                message: this.$tl('c.fileNotExist')
              })
            }
          }
        })
      } else {
        hasFile({
          fileSumMd5: record.fileId
        }).then((res) => {
          if (res.code === 200) {
            if (res.data) {
              this.temp = res.data
              this.viewFileVisible = true
            } else {
              $notification.warning({
                message: this.$tl('c.fileNotExist')
              })
            }
          }
        })
      }
    }
  }
}
</script>
