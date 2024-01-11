<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      @change="changePage"
      bordered
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            v-model:value="listQuery['%name%']"
            @pressEnter="loadData"
            placeholder="监控名称"
            class="search-input-item"
          />
          <a-select v-model:value="listQuery.status" allowClear placeholder="开启状态" class="search-input-item">
            <a-select-option :value="1">开启</a-select-option>
            <a-select-option :value="0">关闭</a-select-option>
          </a-select>
          <a-select v-model:value="listQuery.autoRestart" allowClear placeholder="自动重启" class="search-input-item">
            <a-select-option :value="1">是</a-select-option>
            <a-select-option :value="0">否</a-select-option>
          </a-select>
          <a-select v-model:value="listQuery.alarm" allowClear placeholder="报警状态" class="search-input-item">
            <a-select-option :value="1">报警中</a-select-option>
            <a-select-option :value="0">未报警</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch size="small" :checked="text" disabled checked-children="开启" un-checked-children="关闭" />
        </template>
        <template v-else-if="column.dataIndex === 'autoRestart'">
          <a-switch size="small" :checked="text" disabled checked-children="是" un-checked-children="否" />
        </template>
        <template v-else-if="column.dataIndex === 'alarm'">
          <a-switch size="small" :checked="text" disabled checked-children="报警中" un-checked-children="未报警" />
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button type="primary" danger size="small" @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="editMonitorVisible"
      width="60%"
      title="编辑监控"
      @ok="handleEditMonitorOk"
      :maskClosable="false"
    >
      <a-form ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="监控名称" name="name">
          <a-input v-model:value="temp.name" :maxLength="50" placeholder="监控名称" />
        </a-form-item>

        <a-form-item label="开启状态" name="status">
          <a-space size="large">
            <a-switch v-model:checked="temp.status" checked-children="开" un-checked-children="关" />
            <div>
              自动重启:
              <a-form-item-rest>
                <a-switch v-model:checked="temp.autoRestart" checked-children="开" un-checked-children="关" />
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

        <a-form-item label="监控周期" name="execCron">
          <a-auto-complete
            v-model:value="temp.execCron"
            placeholder="如果需要定时自动执行则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）"
            :options="CRON_DATA_SOURCE"
          >
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item label="监控项目" name="projects">
          <a-select
            option-label-prop="label"
            v-model:value="projectKeys"
            mode="multiple"
            placeholder="选择要监控的项目,file 类型项目不可以监控"
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
              :label="nodeMap[nodeItem.node].name"
              v-for="nodeItem in nodeProjectGroupList"
              :key="nodeItem.node"
            >
              <a-select-option
                :label="`${project.name} - ${project.runMode}`"
                v-for="project in nodeItem.projects"
                :disabled="!noFileModes.includes(project.runMode)"
                :key="project.id"
              >
                【{{ project.nodeName }}】{{ project.name }} -
                {{ project.runMode }}
              </a-select-option>
            </a-select-opt-group>
          </a-select>
        </a-form-item>
        <a-form-item name="notifyUser" class="jpom-notify">
          <template v-slot:label>
            <a-tooltip>
              联系人
              <template v-slot:title>
                如果这里的报警联系人无法选择，说明这里面的管理员没有设置邮箱，在右上角下拉菜单里面的用户资料里可以设置。
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-transfer
            :data-source="userList"
            :lazy="false"
            :titles="['待选择', '已选择']"
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
                <a-tooltip title="如果不可以选择则表示对应的用户没有配置邮箱">
                  <WarningTwoTone />
                  {{ item.name }}
                </a-tooltip>
              </template>
              <template v-else> {{ item.name }}</template>
            </template>
          </a-transfer>
        </a-form-item>
        <a-form-item name="webhook">
          <template v-slot:label>
            <a-tooltip>
              WebHooks
              <template v-slot:title>
                <ul>
                  <li>发生报警时候请求</li>
                  <li>
                    传入参数有：monitorId、monitorName、nodeId、nodeName、projectId、projectName、title、content、runStatus
                  </li>
                  <li>runStatus 值为 true 表示项目当前为运行中(异常恢复),false 表示项目当前未运行(发生异常)</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-input v-model:value="temp.webhook" placeholder="接收报警消息,非必填，GET请求" />
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
          title: '名称',
          dataIndex: 'name',
          ellipsis: true
        },
        {
          title: '监控周期',
          dataIndex: 'execCron',
          ellipsis: true
        },
        {
          title: '开启状态',
          dataIndex: 'status',
          ellipsis: true,

          width: 120
        },
        {
          title: '自动重启',
          dataIndex: 'autoRestart',
          ellipsis: true,

          width: 120
        },
        {
          title: '报警状态',
          dataIndex: 'alarm',
          ellipsis: true,

          width: 120
        },
        {
          title: '修改人',
          dataIndex: 'modifyUser',
          ellipsis: true,
          align: 'center',

          width: 120
        },
        {
          title: '修改时间',
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
          title: '操作',
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
            message: '请输入监控名称',
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
              item.nodeName = nodeInfo.length > 0 ? nodeInfo[0].name : '未知'
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
            message: '请选择一位报警联系人或者填写webhook'
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
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除监控么？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            deleteMonitor(record.id)
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
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }
  }
}
</script>
