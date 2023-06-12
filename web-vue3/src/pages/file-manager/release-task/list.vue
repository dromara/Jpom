<template>
  <div class="full-content">
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
    >
      <template #title>
        <a-space>
          <a-input
            v-model="listQuery['%name%']"
            @pressEnter="loadData"
            placeholder="任务名"
            class="search-input-item"
          />
          <a-select
            show-search
            option-filter-prop="children"
            v-model="listQuery.status"
            allowClear
            placeholder="状态"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            show-search
            option-filter-prop="children"
            v-model="listQuery.taskType"
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
      <a-tooltip #tooltip slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <template #fileId slot-scope="text, item">
        <a-button type="link" style="padding: 0" @click="handleViewFile(item)" size="small">{{ text }}</a-button>
      </template>

      <template #status slot-scope="text">
        <a-tag v-if="text === 2" color="green">{{ statusMap[text] || '未知' }}</a-tag>
        <a-tag v-else-if="text === 0 || text === 1" color="orange">{{ statusMap[text] || '未知' }}</a-tag>
        <a-tag v-else-if="text === 4" color="blue"> {{ statusMap[text] || '未知' }} </a-tag>
        <a-tag v-else-if="text === 3" color="red">{{ statusMap[text] || '未知' }}</a-tag>
        <a-tag v-else>{{ statusMap[text] || '未知' }}</a-tag>
      </template>
      <template #taskType slot-scope="text">
        <span>{{ taskTypeMap[text] || '未知' }}</span>
      </template>

      <template #operation slot-scope="text, record">
        <a-space>
          <a-button type="primary" size="small" @click="handleView(record)">查看</a-button>

          <a-button type="primary" size="small" @click="handleRetask(record)">重建</a-button>
          <a-button
            type="danger"
            size="small"
            :disabled="!(record.status === 0 || record.status === 1)"
            @click="handleCancelTask(record)"
            >取消</a-button
          >
          <a-button
            type="danger"
            size="small"
            :disabled="record.status === 0 || record.status === 1"
            @click="handleDelete(record)"
            >删除</a-button
          >
        </a-space>
      </template>
    </a-table>
    <!-- 任务详情 -->
    <a-drawer
      title="任务详情"
      placement="right"
      :width="'80vw'"
      :visible="detailsVisible"
      @close="
        () => {
          this.detailsVisible = false
        }
      "
    >
      <task-details-page v-if="detailsVisible" :taskId="temp.id" />
    </a-drawer>
    <!-- 重建任务 -->
    <a-modal
      destroyOnClose
      v-model="releaseFileVisible"
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
        <a-form-item label="任务名" prop="name">
          <a-input placeholder="请输入任务名" :maxLength="50" v-model="temp.name" />
        </a-form-item>

        <a-form-item label="发布方式" prop="taskType">
          <a-radio-group v-model="temp.taskType" :disabled="true">
            <a-radio :value="0"> SSH </a-radio>
            <a-radio :value="1"> 节点 </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item prop="taskDataIds" label="发布的SSH" v-if="temp.taskType === 0">
          <a-row>
            <a-col :span="22">
              <a-select
                show-search
                option-filter-prop="children"
                mode="multiple"
                v-model="temp.taskDataIds"
                placeholder="请选择SSH"
              >
                <a-select-option v-for="ssh in sshList" :key="ssh.id">
                  <a-tooltip :title="ssh.name"> {{ ssh.name }}</a-tooltip>
                </a-select-option>
              </a-select>
            </a-col>
            <a-col :span="1" style="margin-left: 10px">
              <a-icon type="reload" @click="loadSshList" />
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item prop="taskDataIds" label="发布的节点" v-else-if="temp.taskType === 1">
          <a-row>
            <a-col :span="22">
              <a-select
                show-search
                option-filter-prop="children"
                mode="multiple"
                v-model="temp.taskDataIds"
                placeholder="请选择节点"
              >
                <a-select-option v-for="ssh in nodeList" :key="ssh.id">
                  <a-tooltip :title="ssh.name"> {{ ssh.name }}</a-tooltip>
                </a-select-option>
              </a-select>
            </a-col>
            <a-col :span="1" style="margin-left: 10px">
              <a-icon type="reload" @click="loadNodeList" />
            </a-col>
          </a-row>
        </a-form-item>

        <a-form-item prop="releasePathParent" label="发布目录">
          <a-input placeholder="请输入发布目录" :disabled="true" v-model="temp.releasePath" />
        </a-form-item>

        <a-form-item prop="releasePathParent" label="文件id">
          <a-input placeholder="请输入发布的文件id" v-model="temp.fileId" />
        </a-form-item>

        <a-form-item label="执行脚本" prop="releaseBeforeCommand">
          <a-tabs tabPosition="right">
            <a-tab-pane key="before" tab="上传前">
              <div style="height: 40vh; overflow-y: scroll">
                <code-editor
                  v-model="temp.beforeScript"
                  :options="{ mode: temp.taskType === 0 ? 'shell' : '', tabSize: 2, theme: 'abcdef' }"
                ></code-editor>
              </div>
              <div style="margin-top: 10px">文件上传前需要执行的脚本(非阻塞命令)</div>
            </a-tab-pane>
            <a-tab-pane key="after" tab="上传后">
              <div style="height: 40vh; overflow-y: scroll">
                <code-editor
                  v-model="temp.afterScript"
                  :options="{ mode: temp.taskType === 0 ? 'shell' : '', tabSize: 2, theme: 'abcdef' }"
                ></code-editor>
              </div>
              <div style="margin-top: 10px">文件上传成功后需要执行的脚本(非阻塞命令)</div>
            </a-tab-pane>
          </a-tabs>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 查看文件 -->
    <a-modal destroyOnClose v-model:visible="viewFileVisible" :title="`查看文件`" :footer="null" :maskClosable="false">
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="文件名" prop="name">
          {{ temp.name }}
        </a-form-item>
        <a-form-item label="文件ID" prop="name">
          {{ temp.id }}
        </a-form-item>
        <a-form-item label="文件大小" prop="size">
          {{ renderSize(temp.size) }}
        </a-form-item>
        <a-form-item label="过期时间" prop="keepDay">
          {{ parseTime(temp.validUntil) }}
        </a-form-item>
        <a-form-item label="文件共享" prop="global">
          {{ temp.workspaceId === 'GLOBAL' ? '全局' : '工作空间' }}
        </a-form-item>
        <a-form-item label="文件描述" prop="description">
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
      columns: [
        { title: '任务名称', dataIndex: 'name', ellipsis: true, width: 150, scopedSlots: { customRender: 'tooltip' } },
        {
          title: '分发类型',
          dataIndex: 'taskType',
          width: '100px',
          ellipsis: true,
          scopedSlots: { customRender: 'taskType' }
        },
        { title: '状态', dataIndex: 'status', width: '100px', ellipsis: true, scopedSlots: { customRender: 'status' } },

        {
          title: '状态描述',
          dataIndex: 'statusMsg',
          ellipsis: true,
          width: 200,
          scopedSlots: { customRender: 'tooltip' }
        },
        { title: '文件ID', dataIndex: 'fileId', ellipsis: true, width: 150, scopedSlots: { customRender: 'fileId' } },
        {
          title: '发布目录',
          dataIndex: 'releasePath',
          width: '100px',
          ellipsis: true,
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '执行人',
          dataIndex: 'modifyUser',
          width: '120px',
          ellipsis: true,
          scopedSlots: { customRender: 'modifyUser' }
        },
        {
          title: '任务时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: '170px'
        },
        {
          title: '任务更新时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: '170px'
        },

        {
          title: '操作',
          dataIndex: 'operation',
          align: 'center',
          scopedSlots: { customRender: 'operation' },
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
      $confirm({
        title: '系统提示',
        content: '真的要删除该执行记录吗？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteReleaseTask({
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
      this.$refs['releaseFileForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        reReleaseTask({ ...this.temp, taskDataIds: this.temp.taskDataIds?.join(',') }).then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })

            this.releaseFileVisible = false
            this.loadData()
          }
        })
      })
    },
    // 取消
    handleCancelTask(record) {
      $confirm({
        title: '系统提示',
        content: '真的取消当前发布任务吗？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          cancelReleaseTask({ id: record.id }).then((res) => {
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
</script>
<style scoped></style>
