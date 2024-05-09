<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      bordered
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$tl('c.monitorName')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            :placeholder="$tl('c.openStatus')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $tl('c.open') }}</a-select-option>
            <a-select-option :value="0">{{ $tl('c.close') }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.autoRestart"
            allow-clear
            :placeholder="$tl('c.autoRestart')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $tl('c.yes') }}</a-select-option>
            <a-select-option :value="0">{{ $tl('c.no') }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.alarm"
            allow-clear
            :placeholder="$tl('c.alarmStatus')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $tl('c.alarming') }}</a-select-option>
            <a-select-option :value="0">{{ $tl('c.notAlarming') }}</a-select-option>
          </a-select>
          <a-tooltip :title="$tl('p.clickBackToFirstPage')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $tl('p.add') }}</a-button>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch
            size="small"
            :checked="text"
            disabled
            :checked-children="$tl('c.open')"
            :un-checked-children="$tl('c.close')"
          />
        </template>
        <template v-else-if="column.dataIndex === 'autoRestart'">
          <a-switch
            size="small"
            :checked="text"
            disabled
            :checked-children="$tl('c.yes')"
            :un-checked-children="$tl('c.no')"
          />
        </template>
        <template v-else-if="column.dataIndex === 'alarm'">
          <a-switch
            size="small"
            :checked="text"
            disabled
            :checked-children="$tl('c.alarming')"
            :un-checked-children="$tl('c.notAlarming')"
          />
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleEdit(record)">{{ $tl('p.edit') }}</a-button>
            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{ $tl('p.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      v-model:open="editMonitorVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60%"
      :title="$tl('p.editMonitor')"
      :mask-closable="false"
      @ok="handleEditMonitorOk"
    >
      <a-form ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$tl('c.monitorName')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$tl('c.monitorName')" />
        </a-form-item>

        <a-form-item :label="$tl('c.openStatus')" name="status">
          <a-space size="large">
            <a-switch
              v-model:checked="temp.status"
              :checked-children="$tl('c.on')"
              :un-checked-children="$tl('c.off')"
            />
            <div>
              {{ $tl('c.autoRestart') }}:
              <a-form-item-rest>
                <a-switch
                  v-model:checked="temp.autoRestart"
                  :checked-children="$tl('c.on')"
                  :un-checked-children="$tl('c.off')"
                />
              </a-form-item-rest>
            </div>
          </a-space>
        </a-form-item>

        <!-- <a-form-item label="自动重启" name="autoRestart">

            </a-form-item> -->

        <!-- <a-form-item label="监控周期" name="cycle">
            <a-radio-group v-model="temp.cycle" name="cycle">
              <a-radio :value="1">1 分钟</a-radio>
              <a-radio :value="5">5 分钟</a-radio>
              <a-radio :value="10">10 分钟</a-radio>
              <a-radio :value="30">30 分钟</a-radio>
            </a-radio-group>
          </a-form-item> -->

        <a-form-item :label="$tl('c.monitorPeriod')" name="execCron">
          <a-auto-complete v-model:value="temp.execCron" :placeholder="$tl('p.cronExpr')" :options="CRON_DATA_SOURCE">
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item :label="$tl('p.monitorItem')" name="projects">
          <a-select
            v-model:value="projectKeys"
            option-label-prop="label"
            mode="multiple"
            :placeholder="$tl('p.selectMonitorItem')"
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
          >
            <a-select-opt-group
              v-for="nodeItem in nodeProjectGroupList"
              :key="nodeItem.node"
              :label="nodeMap[nodeItem.node].name"
            >
              <a-select-option
                v-for="project in nodeItem.projects"
                :key="project.id"
                :label="`${project.name} - ${project.runMode}`"
                :disabled="!noFileModes.includes(project.runMode)"
              >
                {{ $tl('p.leftBracket') }}{{ project.nodeName }}{{ $tl('p.rightBracket') }}{{ project.name }} -
                {{ project.runMode }}
              </a-select-option>
            </a-select-opt-group>
          </a-select>
        </a-form-item>
        <a-form-item name="notifyUser" class="jpom-notify">
          <template #label>
            <a-tooltip>
              {{ $tl('p.contactPerson') }}
              <template #title> {{ $tl('p.contactPersonNote') }} </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-transfer
            :data-source="userList"
            :lazy="false"
            :titles="[$tl('p.pending'), $tl('p.selected')]"
            show-search
            :list-style="{
              width: '18vw'
            }"
            :filter-option="filterOption"
            :target-keys="targetKeys"
            @change="handleChange"
          >
            <template #render="item">
              <template v-if="item.disabled">
                <a-tooltip :title="$tl('p.noEmailConfigured')">
                  <WarningTwoTone />
                  {{ item.name }}
                </a-tooltip>
              </template>
              <template v-else> {{ item.name }}</template>
            </template>
          </a-transfer>
        </a-form-item>
        <a-form-item name="webhook">
          <template #label>
            <a-tooltip>
              WebHooks
              <template #title>
                <ul>
                  <li>{{ $tl('p.alarmRequest') }}</li>
                  <li>{{ $tl('p.alarmParams') }}</li>
                  <li>
                    runStatus {{ $tl('p.running') }}({{ $tl('p.abnormalRecovery') }}),false {{ $tl('p.notRunning') }}({{
                      $tl('p.abnormal')
                    }})
                  </li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-input v-model:value="temp.webhook" :placeholder="$tl('p.receiveAlarmMsg')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { deleteMonitor, editMonitor, getMonitorList } from '@/api/monitor'
import { noFileModes } from '@/api/node-project'
import { getUserListAll } from '@/api/user/user'
import { getNodeListAll, getProjectListAll } from '@/api/node'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  CRON_DATA_SOURCE,
  PAGE_DEFAULT_LIST_QUERY,
  itemGroupBy,
  parseTime
} from '@/utils/const'

export default {
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      CRON_DATA_SOURCE,
      list: [],
      userList: [],
      nodeProjectList: [],
      nodeProjectGroupList: [],
      nodeMap: {},
      targetKeys: [],
      projectKeys: [],
      // tree 选中的值
      checkedKeys: {},
      noFileModes,
      temp: {},
      editMonitorVisible: false,
      columns: [
        {
          title: this.$tl('p.name'),
          dataIndex: 'name',
          ellipsis: true
        },
        {
          title: this.$tl('c.monitorPeriod'),
          dataIndex: 'execCron',
          ellipsis: true
        },
        {
          title: this.$tl('c.openStatus'),
          dataIndex: 'status',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$tl('c.autoRestart'),
          dataIndex: 'autoRestart',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$tl('c.alarmStatus'),
          dataIndex: 'alarm',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$tl('p.modifier'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          align: 'center',

          width: 120
        },
        {
          title: this.$tl('p.modifiedTime'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => {
            if (!text || text === '0') {
              return ''
            }
            return parseTime(text)
          },
          width: 180
        },
        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',
          ellipsis: true,
          fixed: 'right',
          width: '120px'
        }
      ],
      rules: {
        name: [
          {
            required: true,
            message: this.$tl('p.pleaseEnterMonitorName'),
            trigger: 'blur'
          }
        ]
      },
      confirmLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  watch: {},
  created() {
    this.loadData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages..monitor.list.${key}`, ...args)
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getMonitorList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 加载用户列表
    loadUserList(fn) {
      getUserListAll().then((res) => {
        if (res.code === 200) {
          this.$nextTick(() => {
            this.userList = res.data.map((element) => {
              let canUse = element.email || element.dingDing || element.workWx
              return { key: element.id, name: element.name, disabled: !canUse }
            })

            fn && fn()
          })
        }
      })
    },
    // 加载节点项目列表
    loadNodeProjectList(fn) {
      this.nodeProjectList = []
      this.nodeProjectGroupList = []
      getProjectListAll().then((res) => {
        if (res.code === 200) {
          getNodeListAll().then((res1) => {
            res1.data.forEach((element) => {
              this.nodeMap[element.id] = element
            })

            this.nodeProjectList = res.data.map((item) => {
              let nodeInfo = res1.data.filter((nodeItem) => nodeItem.id === item.nodeId)
              item.nodeName = nodeInfo.length > 0 ? nodeInfo[0].name : this.$tl('p.unknown')
              return item
            })
            this.nodeProjectGroupList = itemGroupBy(this.nodeProjectList, 'nodeId', 'node', 'projects')
            // console.log(this.nodeProjectGroupList);
            fn && fn()
          })
        }
      })
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.name.indexOf(inputValue) > -1
    },
    // 穿梭框 change
    handleChange(targetKeys) {
      this.targetKeys = targetKeys
    },

    // 新增
    handleAdd() {
      this.temp = {}
      this.targetKeys = []
      this.projectKeys = []
      this.editMonitorVisible = true
      this.loadUserList()
      this.loadNodeProjectList()
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      this.temp.projectsTemp = JSON.parse(this.temp.projects)
      this.targetKeys = []
      this.loadUserList(() => {
        this.targetKeys = JSON.parse(this.temp.notifyUser)

        this.loadNodeProjectList(() => {
          // 设置监控项目
          this.projectKeys = this.nodeProjectList
            .filter((item) => {
              return (
                this.temp.projectsTemp.filter((item2) => {
                  let isNode = item.nodeId === item2.node
                  if (!isNode) {
                    return false
                  }
                  return item2.projects.filter((item3) => item.projectId === item3).length > 0
                }).length > 0
              )
            })
            .map((item) => {
              return item.id
            })

          this.editMonitorVisible = true
        })
      })
    },
    handleEditMonitorOk() {
      // 检验表单
      this.$refs['editMonitorForm'].validate().then(() => {
        let projects = this.nodeProjectList.filter((item) => {
          return this.projectKeys.includes(item.id)
        })
        projects = itemGroupBy(projects, 'nodeId', 'node', 'projects')
        projects.map((item) => {
          item.projects = item.projects.map((item) => {
            return item.projectId
          })
          return item
        })

        let targetKeysTemp = this.targetKeys || []
        targetKeysTemp = this.userList
          .filter((item) => {
            return targetKeysTemp.includes(item.key)
          })
          .map((item) => item.key)

        if (targetKeysTemp.length <= 0 && !this.temp.webhook) {
          $notification.warn({
            message: this.$tl('p.pleaseSelectContactOrWebhook')
          })
          return false
        }

        const params = {
          ...this.temp,
          status: this.temp.status ? 'on' : 'off',
          autoRestart: this.temp.autoRestart ? 'on' : 'off',
          projects: JSON.stringify(projects),
          notifyUser: JSON.stringify(targetKeysTemp)
        }
        this.confirmLoading = true
        editMonitor(params)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editMonitorForm'].resetFields()
              this.editMonitorVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$tl('p.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.reallyDeleteMonitor'),
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
        onOk: () => {
          return deleteMonitor(record.id).then((res) => {
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
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }
  }
}
</script>
