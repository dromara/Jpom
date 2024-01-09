<template>
  <div>
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      bordered
      :pagination="false"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            v-model:value="listQuery['nodeId']"
            @pressEnter="loadData"
            placeholder="id"
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['nodeName']"
            @pressEnter="loadData"
            placeholder="名称"
            class="search-input-item"
          />
          <a-select
            show-search
            option-filter-prop="children"
            v-model:value="listQuery['nodeRole']"
            allowClear
            placeholder="角色"
            class="search-input-item"
          >
            <a-select-option key="worker">工作节点</a-select-option>
            <a-select-option key="manager">管理节点</a-select-option>
          </a-select>

          <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="loadData" />
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <!-- <a-popover :title="`状态信息：${TASK_STATE[text]}`" slot="desiredState" slot-scope="text, item" placement="topLeft"> -->
        <template v-else-if="column.dataIndex === 'hostname'">
          <a-popover placement="topLeft" :title="`主机名：${record.description && record.description.hostname}`">
            <template v-slot:content>
              <p>
                节点Id: <a-tag>{{ record.id }}</a-tag>
              </p>
              <template v-if="record.description && record.description.resources">
                <p>
                  nanoCPUs:
                  <a-tag>{{ record.description.resources.nanoCPUs }}</a-tag>
                </p>
                <p>
                  memoryBytes:
                  <a-tag>{{ record.description.resources.memoryBytes }}</a-tag>
                </p>
              </template>
              <template v-if="record.description && record.description.engine">
                <p>
                  版本: <a-tag>{{ record.description.engine.engineVersion }}</a-tag>
                </p>
              </template>
            </template>

            <span>{{ record.description && record.description.hostname }}</span>
          </a-popover>
        </template>

        <template v-else-if="column.dataIndex === 'state'">
          <a-tooltip
            placement="topLeft"
            :title="`节点状态：${record.status && record.status.state} 节点可用性：${
              record.spec ? record.spec.availability || '' : ''
            }`"
          >
            <a-tag
              :color="
                (record.spec && record.spec.availability) === 'ACTIVE' &&
                record.status &&
                record.status.state === 'READY'
                  ? 'green'
                  : 'red'
              "
            >
              {{ record.status && record.status.state }}
              <template v-if="record.spec">{{ record.spec.availability }}</template>
            </a-tag>
          </a-tooltip>
        </template>
        <!-- 角色显示 -->
        <template v-else-if="column.dataIndex === 'role'">
          <a-tooltip
            placement="topLeft"
            :title="`角色：${record.spec && record.spec.role} ${
              record.managerStatus && record.managerStatus.reachability === 'REACHABLE'
                ? '管理状态：' + record.managerStatus.reachability
                : ''
            }`"
          >
            <a-tag
              :color="`${record.managerStatus && record.managerStatus.reachability === 'REACHABLE' ? 'green' : ''}`"
            >
              {{ record.spec && record.spec.role }}
            </a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'address'">
          <a-tooltip placement="topLeft" :title="record.status && record.status.address">
            {{ record.status && record.status.address }}
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'os'">
          <a-tooltip placement="topLeft" :title="text">
            <span>
              <a-tag
                >{{ record.description && record.description.platform && record.description.platform.os }}-{{
                  record.description && record.description.platform && record.description.platform.architecture
                }}
              </a-tag>
            </span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'updatedAt'">
          <a-tooltip placement="topLeft" :title="`修改时间：${text} 创建时间：${record.createdAt}`">
            <span>
              {{ text }}
            </span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <template v-if="record.managerStatus && record.managerStatus.leader">
              <a-button size="small" type="primary" @click="handleEdit(record)">修改</a-button>
              <a-tooltip title="主节点不能直接剔除">
                <a-button size="small" type="primary" danger :disabled="true">剔除</a-button>
              </a-tooltip>
            </template>
            <template v-else>
              <a-button size="small" type="primary" @click="handleEdit(record)">修改</a-button>
              <a-button size="small" type="primary" danger @click="handleLeava(record)">剔除</a-button>
            </template>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑节点 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="editVisible"
      title="编辑节点"
      @ok="handleEditOk"
      :maskClosable="false"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="角色" name="role">
          <a-radio-group name="role" v-model:value="temp.role" :disabled="temp.leader">
            <a-radio value="WORKER"> 工作节点</a-radio>
            <a-radio value="MANAGER"> 管理节点 </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="状态" name="availability">
          <a-radio-group name="availability" v-model:value="temp.availability">
            <a-radio value="ACTIVE"> 活跃</a-radio>
            <a-radio value="PAUSE"> 暂停 </a-radio>
            <a-radio value="DRAIN"> 排空 </a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { dockerSwarmNodeList, dockerSwarmNodeUpdate } from '@/api/docker-swarm'
import { dockerSwarmNodeLeave } from '@/api/system/assets-docker'

export default {
  components: {},
  props: {
    id: {
      type: String
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String
    }
  },
  data() {
    return {
      loading: false,
      listQuery: {},
      list: [],
      temp: {},
      editVisible: false,
      initSwarmVisible: false,
      rules: {
        role: [{ required: true, message: '请选择节点角色', trigger: 'blur' }],
        availability: [{ required: true, message: '请选择节点状态', trigger: 'blur' }]
      },

      columns: [
        {
          title: '序号',
          width: 80,
          ellipsis: true,
          align: 'center',
          customRender: ({ text, record, index }) => `${index + 1}`
        },
        // { title: "节点Id", dataIndex: "id", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        {
          title: '主机名',
          dataIndex: 'hostname',
          ellipsis: true
        },
        {
          title: '节点地址',
          width: 150,
          dataIndex: 'address',
          ellipsis: true
        },
        {
          title: '状态',
          width: 140,
          dataIndex: 'state',
          ellipsis: true
        },
        {
          title: '角色',
          width: 110,
          dataIndex: 'role',
          ellipsis: true
        },

        {
          title: '系统类型',
          width: 140,
          align: 'center',
          dataIndex: 'os',
          ellipsis: true
        },
        // {
        //   title: "资源",
        //   dataIndex: "description.resources",
        //   ellipsis: true,
        //   scopedSlots: { customRender: "resources" },
        //   width: 170,
        // },
        {
          title: '修改时间',
          dataIndex: 'updatedAt',

          ellipsis: true,

          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',
          width: '120px'
        }
      ],
      countdownTime: Date.now(),
      confirmLoading: false
    }
  },
  computed: {},
  beforeUnmount() {},
  mounted() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData() {
      if (!this.visible) {
        return
      }
      this.loading = true

      this.listQuery.id = this.id
      dockerSwarmNodeList(this.urlPrefix, this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data
        }
        this.loading = false
        this.countdownTime = Date.now() + 5 * 1000
      })
    },
    handleEdit(record) {
      this.editVisible = true
      this.temp = {
        nodeId: record.id,
        role: record.spec.role,
        availability: record.spec.availability,
        leader: record.managerStatus && record.managerStatus.leader
      }
    },
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        this.temp.id = this.id
        this.confirmLoading = true
        dockerSwarmNodeUpdate(this.urlPrefix, this.temp)
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
    //
    handleLeava(record) {
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要在该集群剔除此节点么？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 组装参数
            const params = {
              nodeId: record.id,
              id: that.id
            }
            dockerSwarmNodeLeave(params)
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
    }
  }
}
</script>

<style scoped>
:deep(.ant-statistic div) {
  display: inline-block;
}
:deep(.ant-statistic-content-value, .ant-statistic-content) {
  font-size: 16px;
}
</style>
