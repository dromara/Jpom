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
            :placeholder="$t('pages.monitor.list.102669ac')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            :placeholder="$t('pages.monitor.list.ae501880')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('pages.monitor.list.c467118a') }}</a-select-option>
            <a-select-option :value="0">{{ $t('pages.monitor.list.cfb79b99') }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.autoRestart"
            allow-clear
            :placeholder="$t('pages.monitor.list.dddb1bfb')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('pages.monitor.list.d2fbce36') }}</a-select-option>
            <a-select-option :value="0">{{ $t('pages.monitor.list.1c77d6fb') }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.alarm"
            allow-clear
            :placeholder="$t('pages.monitor.list.95aeba54')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('pages.monitor.list.25660a73') }}</a-select-option>
            <a-select-option :value="0">{{ $t('pages.monitor.list.8d356eff') }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('pages.monitor.list.2c53fa4f')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.monitor.list.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('pages.monitor.list.7d46652a') }}</a-button>
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
            :checked-children="$t('pages.monitor.list.c467118a')"
            :un-checked-children="$t('pages.monitor.list.cfb79b99')"
          />
        </template>
        <template v-else-if="column.dataIndex === 'autoRestart'">
          <a-switch
            size="small"
            :checked="text"
            disabled
            :checked-children="$t('pages.monitor.list.d2fbce36')"
            :un-checked-children="$t('pages.monitor.list.1c77d6fb')"
          />
        </template>
        <template v-else-if="column.dataIndex === 'alarm'">
          <a-switch
            size="small"
            :checked="text"
            disabled
            :checked-children="$t('pages.monitor.list.25660a73')"
            :un-checked-children="$t('pages.monitor.list.8d356eff')"
          />
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleEdit(record)">{{
              $t('pages.monitor.list.64603c01')
            }}</a-button>
            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('pages.monitor.list.dd20d11c')
            }}</a-button>
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
      :title="$t('pages.monitor.list.d6117b62')"
      :mask-closable="false"
      @ok="handleEditMonitorOk"
    >
      <a-form ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('pages.monitor.list.102669ac')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('pages.monitor.list.102669ac')" />
        </a-form-item>

        <a-form-item :label="$t('pages.monitor.list.ae501880')" name="status">
          <a-space size="large">
            <a-switch
              v-model:checked="temp.status"
              :checked-children="$t('pages.monitor.list.726bd72c')"
              :un-checked-children="$t('pages.monitor.list.8ca0a6dc')"
            />
            <div>
              {{ $t('pages.monitor.list.dddb1bfb') }}:
              <a-form-item-rest>
                <a-switch
                  v-model:checked="temp.autoRestart"
                  :checked-children="$t('pages.monitor.list.726bd72c')"
                  :un-checked-children="$t('pages.monitor.list.8ca0a6dc')"
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

        <a-form-item :label="$t('pages.monitor.list.1283816f')" name="execCron">
          <a-auto-complete
            v-model:value="temp.execCron"
            :placeholder="$t('pages.monitor.list.c2df5e27')"
            :options="CRON_DATA_SOURCE"
          >
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item :label="$t('pages.monitor.list.58d3f8a0')" name="projects">
          <a-select
            v-model:value="projectKeys"
            option-label-prop="label"
            mode="multiple"
            :placeholder="$t('pages.monitor.list.93944349')"
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
                {{ $t('pages.monitor.list.3d3c5ee3') }}{{ project.nodeName }}{{ $t('pages.monitor.list.7ee15942')
                }}{{ project.name }} -
                {{ project.runMode }}
              </a-select-option>
            </a-select-opt-group>
          </a-select>
        </a-form-item>
        <a-form-item name="notifyUser" class="jpom-notify">
          <template #label>
            <a-tooltip>
              {{ $t('pages.monitor.list.4c669a42') }}
              <template #title> {{ $t('pages.monitor.list.e2db3920') }} </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-transfer
            :data-source="userList"
            :lazy="false"
            :titles="[$t('pages.monitor.list.b3ef02d8'), $t('pages.monitor.list.a27bd430')]"
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
                <a-tooltip :title="$t('pages.monitor.list.1bb4f680')">
                  <WarningTwoTone />
                  {{ item.name }}
                </a-tooltip>
              </template>
              <template v-else>
                <a-tooltip :title="item.name"> {{ item.name }}</a-tooltip>
              </template>
            </template>
          </a-transfer>
        </a-form-item>
        <a-form-item name="webhook">
          <template #label>
            <a-tooltip>
              WebHooks
              <template #title>
                <ul>
                  <li>{{ $t('pages.monitor.list.59adf155') }}</li>
                  <li>{{ $t('pages.monitor.list.68ad1baf') }}</li>
                  <li>
                    runStatus {{ $t('pages.monitor.list.481bf10e') }}({{ $t('pages.monitor.list.6af45b93') }}),false
                    {{ $t('pages.monitor.list.926b691b') }}({{ $t('pages.monitor.list.cfdf8071') }})
                  </li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-input v-model:value="temp.webhook" :placeholder="$t('pages.monitor.list.76e16851')" />
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
          title: this.$t('pages.monitor.list.bb769c1d'),
          dataIndex: 'name',
          ellipsis: true
        },
        {
          title: this.$t('pages.monitor.list.1283816f'),
          dataIndex: 'execCron',
          ellipsis: true
        },
        {
          title: this.$t('pages.monitor.list.ae501880'),
          dataIndex: 'status',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('pages.monitor.list.dddb1bfb'),
          dataIndex: 'autoRestart',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('pages.monitor.list.95aeba54'),
          dataIndex: 'alarm',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('pages.monitor.list.916db24b'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          align: 'center',

          width: 120
        },
        {
          title: this.$t('pages.monitor.list.fd921623'),
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
          title: this.$t('pages.monitor.list.3bb962bf'),
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
            message: this.$t('pages.monitor.list.4084f8f4'),
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
              item.nodeName = nodeInfo.length > 0 ? nodeInfo[0].name : this.$t('pages.monitor.list.ca1cdfa6')
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
            message: this.$t('pages.monitor.list.4f5685c5')
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
        title: this.$t('pages.monitor.list.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.monitor.list.6a11a0dd'),
        okText: this.$t('pages.monitor.list.e8e9db25'),
        cancelText: this.$t('pages.monitor.list.b12468e9'),
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
