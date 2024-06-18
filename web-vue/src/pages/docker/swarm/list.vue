<template>
  <div>
    <template v-if="useSuggestions">
      <a-result :title="$t('i18n_55e690333a')" :sub-title="$t('i18n_9878af9db5')">
        <template #extra>
          <router-link to="/system/assets/docker-list">
            <a-button key="console" type="primary">{{ $t('i18n_6dcf6175d8') }}</a-button></router-link
          >
        </template>
      </a-result>
    </template>
    <CustomTable
      v-else
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      table-name="docker-swarm-list"
      :empty-description="$t('i18n_ab9c827798')"
      :active-page="activePage"
      size="middle"
      :data-source="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_d7ec2d3fea')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%tag%']"
            :placeholder="$t('i18n_14d342362f')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
        </a-space>
      </template>

      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('status')">
          <template v-if="record.machineDocker">
            <a-tag v-if="record.machineDocker.status === 1" color="green">{{ $t('i18n_fd6e80f1e0') }}</a-tag>
            <a-tooltip v-else :title="record.machineDocker.failureMsg">
              <a-tag color="red">{{ $t('i18n_757a730c9e') }}</a-tag>
            </a-tooltip>
          </template>

          <a-tooltip v-else :title="$t('i18n_33675a9bb3')">
            <a-tag color="red">{{ $t('i18n_5169b9af9d') }}</a-tag>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <template v-if="record.machineDocker">
              <a-button
                size="small"
                :disabled="record.machineDocker.status !== 1"
                type="primary"
                @click="handleConsole(record, 'server')"
                >{{ $t('i18n_47d68cd0f4') }}</a-button
              >
              <a-button
                size="small"
                :disabled="record.machineDocker.status !== 1"
                type="primary"
                @click="handleConsole(record, 'node')"
                >{{ $t('i18n_3bf3c0a8d6') }}</a-button
              >
            </template>
            <template v-else>
              <a-button size="small" :disabled="true" type="primary">{{ $t('i18n_47d68cd0f4') }}</a-button>
              <a-button size="small" :disabled="true" type="primary">{{ $t('i18n_3bf3c0a8d6') }}</a-button>
            </template>

            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑集群区 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_d8a36a8a25')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('i18n_c3f28b34bb')" name="name">
          <a-input v-model:value="temp.name" :placeholder="$t('i18n_a51cd0898f')" />
        </a-form-item>

        <a-form-item :label="$t('i18n_14d342362f')" name="tag"
          ><a-input v-model:value="temp.tag" :placeholder="$t('i18n_77017a3140')" />
        </a-form-item>
      </a-form>
    </CustomModal>

    <!-- 控制台 -->
    <!-- <CustomDrawer
      destroyOnClose
      :title="`${temp.name} 控制台`"
      placement="right"
      :width="`${this.getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
      :open="consoleVisible"
      @close="
        () => {
          this.consoleVisible = false
        }
      "
    > -->
    <console
      v-if="consoleVisible"
      :id="temp.id"
      :visible="consoleVisible"
      :init-menu="temp.menuKey"
      url-prefix=""
      @close="
        () => {
          consoleVisible = false
        }
      "
    ></console>
    <!-- </a-drawer> -->
  </div>
</template>
<script>
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { dockerSwarmList, editDockerSwarm, delSwarm } from '@/api/docker-swarm'
import { mapState } from 'pinia'
import Console from './console'
import { useGuideStore } from '@/stores/guide'
import { useUserStore } from '@/stores/user'
export default {
  components: {
    Console
  },
  props: {},
  data() {
    return {
      loading: true,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      temp: {},
      editVisible: false,
      consoleVisible: false,
      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },

        {
          title: this.$t('i18n_7329a2637c'),
          dataIndex: 'swarmId',
          ellipsis: true,
          align: 'center',
          tooltip: true
        },
        {
          title: this.$t('i18n_a823cfa70c'),
          dataIndex: 'tag',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: ['machineDocker', 'status'],
          ellipsis: true,
          align: 'center',
          width: '100px'
        },
        {
          title: this.$t('i18n_3bcc1c7a20'),
          dataIndex: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_11724cd00b'),
          dataIndex: ['machineDocker', 'swarmCreatedAt'],
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_fe87269484'),
          dataIndex: ['machineDocker', 'swarmUpdatedAt'],
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',
          width: '220px'
        }
      ],

      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: this.$t('i18n_5ca6c1b6c7'), trigger: 'blur' }],

        tag: [
          { required: true, message: this.$t('i18n_3b9418269c'), trigger: 'blur' },
          { pattern: /^\w{1,10}$/, message: this.$t('i18n_89944d6ccb') }
        ]
      },
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useUserStore, ['getUserInfo']),
    ...mapState(useGuideStore, ['getCollapsed']),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    useSuggestions() {
      if (this.loading) {
        // 加载中不提示
        return false
      }
      if (!this.getUserInfo || !this.getUserInfo.systemUser) {
        // 没有登录或者不是超级管理员
        return false
      }
      if (this.listQuery.page !== 1 || this.listQuery.total > 0) {
        // 不是第一页 或者总记录数大于 0
        return false
      }
      // 判断是否存在搜索条件
      const nowKeys = Object.keys(this.listQuery)
      const defaultKeys = Object.keys(PAGE_DEFAULT_LIST_QUERY)
      const dictOrigin = nowKeys.filter((item) => !defaultKeys.includes(item))
      return dictOrigin.length === 0
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page

      dockerSwarmList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 修改
    handleEdit(record) {
      this.temp = record
      this.editVisible = true
      this.$refs['editForm']?.resetFields()
    },
    // 服务
    handleConsole(record, type) {
      this.temp = record
      this.temp = { ...this.temp, menuKey: type }
      this.consoleVisible = true
    },

    // 提交  数据
    handleEditOk() {
      // 检验表单
      this.$refs['editForm'].validate().then(() => {
        this.confirmLoading = true

        editDockerSwarm(this.temp)
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

    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_1593dc4920'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return delSwarm({
            id: record.id
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

    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }
  }
}
</script>
