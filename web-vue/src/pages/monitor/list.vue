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
            :placeholder="$t('i18n_f976e8fcf4')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            :placeholder="$t('i18n_a4f5cae8d2')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('i18n_cc42dd3170') }}</a-select-option>
            <a-select-option :value="0">{{ $t('i18n_b15d91274e') }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.autoRestart"
            allow-clear
            :placeholder="$t('i18n_75528c19c7')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('i18n_0a60ac8f02') }}</a-select-option>
            <a-select-option :value="0">{{ $t('i18n_c9744f45e7') }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.alarm"
            allow-clear
            :placeholder="$t('i18n_db4470d98d')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('i18n_11957d12e4') }}</a-select-option>
            <a-select-option :value="0">{{ $t('i18n_bb667fdb2a') }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('i18n_66ab5e9f24') }}</a-button>
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
            :checked-children="$t('i18n_cc42dd3170')"
            :un-checked-children="$t('i18n_b15d91274e')"
          />
        </template>
        <template v-else-if="column.dataIndex === 'autoRestart'">
          <a-switch
            size="small"
            :checked="text"
            disabled
            :checked-children="$t('i18n_0a60ac8f02')"
            :un-checked-children="$t('i18n_c9744f45e7')"
          />
        </template>
        <template v-else-if="column.dataIndex === 'alarm'">
          <a-switch
            size="small"
            :checked="text"
            disabled
            :checked-children="$t('i18n_11957d12e4')"
            :un-checked-children="$t('i18n_bb667fdb2a')"
          />
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editMonitorVisible"
      v-model:open="editMonitorVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60%"
      :title="$t('i18n_ebc2a1956b')"
      :mask-closable="false"
      @ok="handleEditMonitorOk"
    >
      <a-form ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('i18n_f976e8fcf4')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_f976e8fcf4')" />
        </a-form-item>

        <a-form-item :label="$t('i18n_a4f5cae8d2')" name="status">
          <a-space size="large">
            <a-switch
              v-model:checked="temp.status"
              :checked-children="$t('i18n_8493205602')"
              :un-checked-children="$t('i18n_d58a55bcee')"
            />
            <div>
              {{ $t('i18n_75528c19c7') }}:
              <a-form-item-rest>
                <a-switch
                  v-model:checked="temp.autoRestart"
                  :checked-children="$t('i18n_8493205602')"
                  :un-checked-children="$t('i18n_d58a55bcee')"
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

        <a-form-item :label="$t('i18n_67e7f9e541')" name="execCron">
          <a-auto-complete
            v-model:value="temp.execCron"
            :placeholder="$t('i18n_5dff0d31d0')"
            :options="CRON_DATA_SOURCE"
          >
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item :label="$t('i18n_0e55a594fd')" name="projects">
          <a-select
            v-model:value="projectKeys"
            option-label-prop="label"
            mode="multiple"
            :placeholder="$t('i18n_ac5f3bfa5b')"
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
                <!-- 【】\u3010\u3011 -->
                \u3010{{ project.nodeName }}\u3011{{ project.name }} -
                {{ project.runMode }}
              </a-select-option>
            </a-select-opt-group>
          </a-select>
        </a-form-item>
        <a-form-item name="notifyUser" class="jpom-notify">
          <template #label>
            <a-tooltip>
              {{ $t('i18n_52409da520') }}
              <template #title> {{ $t('i18n_067eb0fa04') }} </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-transfer
            :data-source="userList"
            :lazy="false"
            :titles="[$t('i18n_43d229617a'), $t('i18n_f08afd1f82')]"
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
                <a-tooltip :title="$t('i18n_44876fc0e7')">
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
                  <li>{{ $t('i18n_74dd7594fc') }}</li>
                  <li>{{ $t('i18n_d1f56b0a7e') }}</li>
                  <li>
                    runStatus {{ $t('i18n_808c18d2bb') }}({{ $t('i18n_ad9788b17d') }}),false
                    {{ $t('i18n_22e4da4998') }}({{ $t('i18n_2b52fa609c') }})
                  </li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-input v-model:value="temp.webhook" :placeholder="$t('i18n_77373db7d8')" />
        </a-form-item>
      </a-form>
    </CustomModal>
  </div>
</template>
<script>
import { deleteMonitor, editMonitor, getMonitorList } from '@/api/monitor'
import { noFileModes } from '@/api/node-project'
import { getUserListAll } from '@/api/user/user'
import { getNodeListAll, getProjectListAll } from '@/api/node'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, itemGroupBy, parseTime } from '@/utils/const'
import { CRON_DATA_SOURCE } from '@/utils/const-i18n'

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
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          ellipsis: true
        },
        {
          title: this.$t('i18n_67e7f9e541'),
          dataIndex: 'execCron',
          ellipsis: true
        },
        {
          title: this.$t('i18n_a4f5cae8d2'),
          dataIndex: 'status',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('i18n_75528c19c7'),
          dataIndex: 'autoRestart',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('i18n_db4470d98d'),
          dataIndex: 'alarm',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          align: 'center',

          width: 120
        },
        {
          title: this.$t('i18n_1303e638b5'),
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
          title: this.$t('i18n_2b6bc0f293'),
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
            message: this.$t('i18n_c68dc88c51'),
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
              item.nodeName = nodeInfo.length > 0 ? nodeInfo[0].name : this.$t('i18n_1622dc9b6b')
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
            message: this.$t('i18n_6c24533675')
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_20e0b90021'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
