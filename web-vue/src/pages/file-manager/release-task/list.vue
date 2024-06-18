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
            :placeholder="$t('i18n_ce23a42b47')"
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
            :placeholder="$t('i18n_3fea7ca76c')"
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
            :placeholder="$t('i18n_8aa25f5fbe')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in taskTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
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
          <a-tag v-if="text === 2" color="green">{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</a-tag>
          <a-tag v-else-if="text === 0 || text === 1" color="orange">{{
            statusMap[text] || $t('i18n_1622dc9b6b')
          }}</a-tag>
          <a-tag v-else-if="text === 4" color="blue">
            {{ statusMap[text] || $t('i18n_1622dc9b6b') }}
          </a-tag>
          <a-tag v-else-if="text === 3" color="red">{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</a-tag>
          <a-tag v-else>{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'taskType'">
          <span>{{ taskTypeMap[text] || $t('i18n_1622dc9b6b') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'fileType'">
          <span v-if="text == 2">{{ $t('i18n_28f6e7a67b') }}</span>
          <span v-else>{{ $t('i18n_26183c99bf') }}</span>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleView(record)">{{ $t('i18n_607e7a4f37') }}</a-button>

            <a-button type="primary" size="small" @click="handleRetask(record)">{{ $t('i18n_9e09315960') }}</a-button>
            <a-button
              type="primary"
              danger
              size="small"
              :disabled="!(record.status === 0 || record.status === 1)"
              @click="handleCancelTask(record)"
              >{{ $t('i18n_625fb26b4b') }}</a-button
            >
            <a-button
              type="primary"
              danger
              size="small"
              :disabled="record.status === 0 || record.status === 1"
              @click="handleDelete(record)"
              >{{ $t('i18n_2f4aaddde3') }}</a-button
            >
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 任务详情 -->
    <CustomDrawer
      v-if="detailsVisible"
      :title="$t('i18n_4a98bf0c68')"
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
    </CustomDrawer>
    <!-- 重建任务 -->
    <CustomModal
      v-if="releaseFileVisible"
      v-model:open="releaseFileVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_7e930b95ef')"
      width="70%"
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
        <a-form-item :label="$t('i18n_ce23a42b47')" name="name">
          <a-input v-model:value="temp.name" :placeholder="$t('i18n_5f4c724e61')" :max-length="50" />
        </a-form-item>

        <a-form-item :label="$t('i18n_f98994f7ec')" name="taskType">
          <a-radio-group v-model:value="temp.taskType" :disabled="true">
            <a-radio :value="0"> SSH </a-radio>
            <a-radio :value="1"> {{ $t('i18n_3bf3c0a8d6') }} </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item v-if="temp.taskType === 0" name="taskDataIds" :label="$t('i18n_b188393ea7')">
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
                :placeholder="$t('i18n_260a3234f2')"
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
        <a-form-item v-else-if="temp.taskType === 1" name="taskDataIds" :label="$t('i18n_473badc394')">
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
                :placeholder="$t('i18n_f8a613d247')"
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

        <a-form-item name="releasePathParent" :label="$t('i18n_dbb2df00cf')">
          <a-input v-model:value="temp.releasePath" :placeholder="$t('i18n_ee9a51488f')" :disabled="true" />
        </a-form-item>

        <a-form-item name="releasePathParent" :label="$t('i18n_a91ce167c1')">
          <a-input v-model:value="temp.fileId" :placeholder="$t('i18n_ea8a79546f')" />
        </a-form-item>

        <a-form-item :label="$t('i18n_cfb00269fd')" name="releaseBeforeCommand">
          <a-form-item-rest>
            <a-tabs tab-position="right">
              <a-tab-pane key="before" :tab="$t('i18n_d0c879f900')">
                <code-editor
                  v-model:content="temp.beforeScript"
                  height="40vh"
                  :options="{
                    mode: 'shell'
                  }"
                ></code-editor>

                <div style="margin-top: 10px">{{ $t('i18n_00de0ae1da') }}</div>
              </a-tab-pane>
              <a-tab-pane key="after" :tab="$t('i18n_9b1c5264a0')">
                <code-editor
                  v-model:content="temp.afterScript"
                  height="40vh"
                  :options="{
                    mode: 'shell'
                  }"
                ></code-editor>

                <div style="margin-top: 10px">{{ $t('i18n_08ac1eace7') }}</div>
              </a-tab-pane>
            </a-tabs>
          </a-form-item-rest>
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 查看文件 -->
    <CustomModal
      v-if="viewFileVisible"
      v-model:open="viewFileVisible"
      destroy-on-close
      :title="`${$t('i18n_9de72a79fe')}`"
      :footer="null"
      :mask-closable="false"
    >
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$t('i18n_29139c2a1a')" name="name">
          {{ temp.name }}
        </a-form-item>
        <a-form-item :label="$t('i18n_0ff425e276')" name="name">
          {{ temp.id }}
        </a-form-item>
        <a-form-item :label="$t('i18n_396b7d3f91')" name="size">
          {{ renderSize(temp.size) }}
        </a-form-item>
        <a-form-item v-if="temp.validUntil" :label="$t('i18n_1fa23f4daa')" name="validUntil">
          {{ parseTime(temp.validUntil) }}
        </a-form-item>
        <a-form-item v-if="temp.workspaceId" :label="$t('i18n_3a6970ac26')" name="global">
          {{ temp.workspaceId === 'GLOBAL' ? $t('i18n_2be75b1044') : $t('i18n_98d69f8b62') }}
        </a-form-item>
        <a-form-item :label="$t('i18n_8d6f38b4b1')" name="description">
          {{ temp.description }}
        </a-form-item>
      </a-form>
    </CustomModal>
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
          title: this.$t('i18n_78caf7115c'),
          dataIndex: 'name',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('i18n_9e2e02ef08'),
          dataIndex: 'taskType',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_9d577fe51b'),
          dataIndex: 'fileType',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: 'status',
          width: '100px',
          ellipsis: true
        },

        {
          title: this.$t('i18n_920f05031b'),
          dataIndex: 'statusMsg',
          ellipsis: true,
          width: 200,
          tooltip: true
        },
        {
          title: this.$t('i18n_0ff425e276'),
          dataIndex: 'fileId',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_dbb2df00cf'),
          dataIndex: 'releasePath',
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_a497562c8e'),
          dataIndex: 'modifyUser',
          width: '120px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_b341f9a861'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_4871f7722d'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },

        {
          title: this.$t('i18n_2b6bc0f293'),
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
        name: [{ required: true, message: this.$t('i18n_89d18c88a3'), trigger: 'blur' }],

        taskDataIds: [{ required: true, message: this.$t('i18n_3e51d1bc9c'), trigger: 'blur' }]
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_50fe3400c7'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_7824ed010c'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
                message: this.$t('i18n_3e445d03aa')
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
                message: this.$t('i18n_3e445d03aa')
              })
            }
          }
        })
      }
    }
  }
}
</script>
