<template>
  <div>
    <a-table
      size="middle"
      :data-source="commandList"
      :columns="columns"
      bordered
      :pagination="pagination"
      @change="
        (pagination, filters, sorter) => {
          this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
          this.loadData()
        }
      "
      rowKey="id"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            v-model:value="listQuery['%name%']"
            @pressEnter="loadData"
            placeholder="任务名"
            class="search-input-item"
          />
          <a-select
            show-search
            option-filter-prop="children"
            v-model:value="listQuery.status"
            allowClear
            placeholder="状态"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            show-search
            option-filter-prop="children"
            v-model:value="listQuery.taskType"
            allowClear
            placeholder="发布类型"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in taskTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'fileId'">
          <a-tooltip :title="text">
            <a-button type="link" style="padding: 0" @click="handleViewFile(record)" size="small">{{
              (text || '').slice(0, 10)
            }}</a-button>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <a-tag v-if="text === 2" color="green">{{ statusMap[text] || '未知' }}</a-tag>
          <a-tag v-else-if="text === 0 || text === 1" color="orange">{{ statusMap[text] || '未知' }}</a-tag>
          <a-tag v-else-if="text === 4" color="blue">
            {{ statusMap[text] || '未知' }}
          </a-tag>
          <a-tag v-else-if="text === 3" color="red">{{ statusMap[text] || '未知' }}</a-tag>
          <a-tag v-else>{{ statusMap[text] || '未知' }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'taskType'">
          <span>{{ taskTypeMap[text] || '未知' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'fileType'">
          <span v-if="text == 2">静态文件</span>
          <span v-else>文件中心</span>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleView(record)">查看</a-button>

            <a-button type="primary" size="small" @click="handleRetask(record)">重建</a-button>
            <a-button
              type="primary"
              danger
              size="small"
              :disabled="!(record.status === 0 || record.status === 1)"
              @click="handleCancelTask(record)"
              >取消</a-button
            >
            <a-button
              type="primary"
              danger
              size="small"
              :disabled="record.status === 0 || record.status === 1"
              @click="handleDelete(record)"
              >删除</a-button
            >
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 任务详情 -->
    <a-drawer
      title="任务详情"
      placement="right"
      :width="'80vw'"
      :open="detailsVisible"
      @close="
        () => {
          this.detailsVisible = false
        }
      "
    >
      <task-details-page v-if="detailsVisible" :taskId="this.temp.id" />
    </a-drawer>
    <!-- 重建任务 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="releaseFileVisible"
      title="发布文件"
      width="50%"
      :maskClosable="false"
      @ok="handleReCrateTask"
    >
      <a-form
        ref="releaseFileForm"
        :rules="releaseFileRules"
        :model="temp"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item label="任务名" name="name">
          <a-input placeholder="请输入任务名" :maxLength="50" v-model:value="temp.name" />
        </a-form-item>

        <a-form-item label="发布方式" name="taskType">
          <a-radio-group v-model:value="temp.taskType" :disabled="true">
            <a-radio :value="0"> SSH </a-radio>
            <a-radio :value="1"> 节点 </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item name="taskDataIds" label="发布的SSH" v-if="temp.taskType === 0">
          <a-row>
            <a-col :span="22">
              <a-select
                show-search
                option-filter-prop="children"
                mode="multiple"
                v-model:value="temp.taskDataIds"
                placeholder="请选择SSH"
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
        <a-form-item name="taskDataIds" label="发布的节点" v-else-if="temp.taskType === 1">
          <a-row>
            <a-col :span="22">
              <a-select
                show-search
                option-filter-prop="children"
                mode="multiple"
                v-model:value="temp.taskDataIds"
                placeholder="请选择节点"
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

        <a-form-item name="releasePathParent" label="发布目录">
          <a-input placeholder="请输入发布目录" :disabled="true" v-model:value="temp.releasePath" />
        </a-form-item>

        <a-form-item name="releasePathParent" label="文件id">
          <a-input placeholder="请输入发布的文件id" v-model:value="temp.fileId" />
        </a-form-item>

        <a-form-item label="执行脚本" name="releaseBeforeCommand">
          <a-form-item-rest>
            <a-tabs tabPosition="right">
              <a-tab-pane key="before" tab="上传前">
                <div style="height: 40vh; overflow-y: scroll">
                  <code-editor
                    v-model:content="temp.beforeScript"
                    :options="{
                      mode: temp.taskType === 0 ? 'shell' : '',
                      tabSize: 2,
                      theme: 'abcdef'
                    }"
                  ></code-editor>
                </div>
                <div style="margin-top: 10px">文件上传前需要执行的脚本(非阻塞命令)</div>
              </a-tab-pane>
              <a-tab-pane key="after" tab="上传后">
                <div style="height: 40vh; overflow-y: scroll">
                  <code-editor
                    v-model:content="temp.afterScript"
                    :options="{
                      mode: temp.taskType === 0 ? 'shell' : '',
                      tabSize: 2,
                      theme: 'abcdef'
                    }"
                  ></code-editor>
                </div>
                <div style="margin-top: 10px">文件上传成功后需要执行的脚本(非阻塞命令)</div>
              </a-tab-pane>
            </a-tabs>
          </a-form-item-rest>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 查看文件 -->
    <a-modal destroyOnClose v-model:open="viewFileVisible" :title="`查看文件`" :footer="null" :maskClosable="false">
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="文件名" name="name">
          {{ temp.name }}
        </a-form-item>
        <a-form-item label="文件ID" name="name">
          {{ temp.id }}
        </a-form-item>
        <a-form-item label="文件大小" name="size">
          {{ renderSize(temp.size) }}
        </a-form-item>
        <a-form-item label="过期时间" name="validUntil" v-if="temp.validUntil">
          {{ parseTime(temp.validUntil) }}
        </a-form-item>
        <a-form-item label="文件共享" name="global" v-if="temp.workspaceId">
          {{ temp.workspaceId === 'GLOBAL' ? '全局' : '工作空间' }}
        </a-form-item>
        <a-form-item label="文件描述" name="description">
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
          title: '任务名称',
          dataIndex: 'name',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: '分发类型',
          dataIndex: 'taskType',
          width: '100px',
          ellipsis: true
        },
        {
          title: '文件来源',
          dataIndex: 'fileType',
          width: '100px',
          ellipsis: true
        },
        {
          title: '状态',
          dataIndex: 'status',
          width: '100px',
          ellipsis: true
        },

        {
          title: '状态描述',
          dataIndex: 'statusMsg',
          ellipsis: true,
          width: 200,
          tooltip: true
        },
        {
          title: '文件ID',
          dataIndex: 'fileId',
          ellipsis: true,
          width: 150
        },
        {
          title: '发布目录',
          dataIndex: 'releasePath',
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '执行人',
          dataIndex: 'modifyUser',
          width: '120px',
          ellipsis: true
        },
        {
          title: '任务时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '任务更新时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },

        {
          title: '操作',
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
        name: [{ required: true, message: '请输入文件任务名', trigger: 'blur' }],

        taskDataIds: [{ required: true, message: '请选择发布的SSH', trigger: 'blur' }]
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
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除该执行记录吗？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            deleteReleaseTask({
              id: row.id
            })
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadData()
                }
                resolve()
              })
              .catch(reject)
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
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的取消当前发布任务吗？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            cancelReleaseTask({ id: record.id })
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadData()
                }
                resolve()
              })
              .catch(reject)
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
                message: '文件不存在啦'
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
                message: '文件不存在啦'
              })
            }
          }
        })
      }
    }
  }
}
</script>
