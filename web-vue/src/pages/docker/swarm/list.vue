<template>
  <div>
    <template v-if="useSuggestions">
      <a-result :title="$t('pages.docker.swarm.list.67b40edb')" :sub-title="$t('pages.docker.swarm.list.b037a0fc')">
        <template #extra>
          <router-link to="/system/assets/docker-list">
            <a-button key="console" type="primary">现在就去</a-button></router-link
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
      empty-description="没有docker集群"
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
            :placeholder="$t('pages.docker.swarm.list.3e34ec28')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%tag%']"
            :placeholder="$t('pages.docker.swarm.list.a4954c1c')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-tooltip :title="$t('pages.docker.swarm.list.1bfed54a')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.docker.swarm.list.53c2763c')
            }}</a-button>
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
            <a-tag v-if="record.machineDocker.status === 1" color="green">{{
              $t('pages.docker.swarm.list.c6b7c57f')
            }}</a-tag>
            <a-tooltip v-else :title="record.machineDocker.failureMsg">
              <a-tag color="red">{{ $t('pages.docker.swarm.list.921c8b81') }}</a-tag>
            </a-tooltip>
          </template>

          <a-tooltip v-else :title="$t('pages.docker.swarm.list.f27fdcf3')">
            <a-tag color="red">{{ $t('pages.docker.swarm.list.6e514532') }}</a-tag>
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
                >{{ $t('pages.docker.swarm.list.13ea9c07') }}</a-button
              >
              <a-button
                size="small"
                :disabled="record.machineDocker.status !== 1"
                type="primary"
                @click="handleConsole(record, 'node')"
                >{{ $t('pages.docker.swarm.list.e5687a6b') }}</a-button
              >
            </template>
            <template v-else>
              <a-button size="small" :disabled="true" type="primary">{{
                $t('pages.docker.swarm.list.13ea9c07')
              }}</a-button>
              <a-button size="small" :disabled="true" type="primary">{{
                $t('pages.docker.swarm.list.e5687a6b')
              }}</a-button>
            </template>

            <a-button size="small" type="primary" @click="handleEdit(record)">{{
              $t('pages.docker.swarm.list.64603c01')
            }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('pages.docker.swarm.list.dd20d11c')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑集群区 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('pages.docker.swarm.list.c34f0c3a')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('pages.docker.swarm.list.c2dfe194')" name="name">
          <a-input v-model:value="temp.name" :placeholder="$t('pages.docker.swarm.list.98c4138b')" />
        </a-form-item>

        <a-form-item :label="$t('pages.docker.swarm.list.a4954c1c')" name="tag"
          ><a-input v-model:value="temp.tag" :placeholder="$t('pages.docker.swarm.list.e50751c6')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 控制台 -->
    <!-- <a-drawer
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
          title: this.$t('pages.docker.swarm.list.3e34ec28'),
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },

        {
          title: this.$t('pages.docker.swarm.list.12a8d4cf'),
          dataIndex: 'swarmId',
          ellipsis: true,
          align: 'center',
          tooltip: true
        },
        {
          title: this.$t('pages.docker.swarm.list.e83d88b9'),
          dataIndex: 'tag',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.docker.swarm.list.9c32c887'),
          dataIndex: ['machineDocker', 'status'],
          ellipsis: true,
          align: 'center',
          width: '100px'
        },
        {
          title: this.$t('pages.docker.swarm.list.49942d36'),
          dataIndex: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('pages.docker.swarm.list.61164914'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.docker.swarm.list.fb7465c4'),
          dataIndex: ['machineDocker', 'swarmCreatedAt'],
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.docker.swarm.list.a0835704'),
          dataIndex: ['machineDocker', 'swarmUpdatedAt'],
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.docker.swarm.list.3bb962bf'),
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',
          width: '220px'
        }
      ],
      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: this.$t('pages.docker.swarm.list.b9de83e8'), trigger: 'blur' }],

        tag: [
          { required: true, message: this.$t('pages.docker.swarm.list.226cdc3a'), trigger: 'blur' },
          { pattern: /^\w{1,10}$/, message: this.$t('pages.docker.swarm.list.9976c029') }
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
    $tl(key, ...args) {
      return this.$t(`pages.docker.swarm.list.${key}`, ...args)
    },
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
        title: this.$t('pages.docker.swarm.list.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.docker.swarm.list.987c2cd6'),
        okText: this.$t('pages.docker.swarm.list.e8e9db25'),
        cancelText: this.$t('pages.docker.swarm.list.b12468e9'),
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
