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
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['nodeId']"
            placeholder="id"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['nodeName']"
            :placeholder="$tl('p.name')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery['nodeRole']"
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
            :placeholder="$tl('c.role')"
            class="search-input-item"
          >
            <a-select-option key="worker">{{ $tl('c.workNode') }}</a-select-option>
            <a-select-option key="manager">{{ $tl('c.managementNode') }}</a-select-option>
          </a-select>

          <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          <a-statistic-countdown
            format=" {{$tl('p.sSeconds')}}"
            :title="$tl('p.refreshCountdown')"
            :value="countdownTime"
            @finish="loadData"
          />
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'hostname'">
          <a-popover
            placement="topLeft"
            :title="`${$tl('p.hostName')}${record.description && record.description.hostname}`"
          >
            <template #content>
              <p>
                {{ $tl('p.nodeId') }}: <a-tag>{{ record.id }}</a-tag>
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
                  {{ $tl('p.version') }}: <a-tag>{{ record.description.engine.engineVersion }}</a-tag>
                </p>
              </template>
            </template>

            <span>{{ record.description && record.description.hostname }}</span>
          </a-popover>
        </template>

        <template v-else-if="column.dataIndex === 'state'">
          <a-tooltip
            placement="topLeft"
            :title="`${$tl('p.nodeStatus')}${record.status && record.status.state} ${$tl('p.nodeAvailability')}${record.spec ? record.spec.availability || '' : ''}`"
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
            :title="`${$tl('p.roleLabel')}${record.spec && record.spec.role} ${record.managerStatus && record.managerStatus.reachability === 'REACHABLE' ? $tl('p.managementStatus') + record.managerStatus.reachability : ''}`"
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
          <a-tooltip
            placement="topLeft"
            :title="`${$tl('p.modifyTime')}${text} ${$tl('p.createTime')}${record.createdAt}`"
          >
            <span>
              {{ text }}
            </span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <template v-if="record.managerStatus && record.managerStatus.leader">
              <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('c.modify') }}</a-button>
              <a-tooltip :title="$tl('p.primaryNodeCannotBeRemoved')">
                <a-button size="small" type="primary" danger :disabled="true">{{ $tl('c.remove') }}</a-button>
              </a-tooltip>
            </template>
            <template v-else>
              <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('c.modify') }}</a-button>
              <a-button size="small" type="primary" danger @click="handleLeava(record)">{{ $tl('c.remove') }}</a-button>
            </template>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑节点 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$tl('p.editNode')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$tl('c.role')" name="role">
          <a-radio-group v-model:value="temp.role" name="role" :disabled="temp.leader">
            <a-radio value="WORKER"> {{ $tl('c.workNode') }}</a-radio>
            <a-radio value="MANAGER"> {{ $tl('c.managementNode') }} </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$tl('c.status')" name="availability">
          <a-radio-group v-model:value="temp.availability" name="availability">
            <a-radio value="ACTIVE"> {{ $tl('p.active') }}</a-radio>
            <a-radio value="PAUSE"> {{ $tl('p.paused') }} </a-radio>
            <a-radio value="DRAIN"> {{ $tl('p.drained') }} </a-radio>
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
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String,
      default: ''
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
        role: [{ required: true, message: this.$tl('p.pleaseSelectNodeRole'), trigger: 'blur' }],
        availability: [{ required: true, message: this.$tl('p.pleaseSelectNodeStatus'), trigger: 'blur' }]
      },

      columns: [
        {
          title: this.$tl('p.serialNumber'),
          width: 80,
          ellipsis: true,
          align: 'center',
          customRender: ({ index }) => `${index + 1}`
        },
        // { title: "节点Id", dataIndex: "id", ellipsis: true, },
        {
          title: this.$tl('p.host'),
          dataIndex: 'hostname',
          ellipsis: true
        },
        {
          title: this.$tl('p.nodeAddress'),
          width: 150,
          dataIndex: 'address',
          ellipsis: true
        },
        {
          title: this.$tl('c.status'),
          width: 140,
          dataIndex: 'state',
          ellipsis: true
        },
        {
          title: this.$tl('c.role'),
          width: 110,
          dataIndex: 'role',
          ellipsis: true
        },

        {
          title: this.$tl('p.systemType'),
          width: 140,
          align: 'center',
          dataIndex: 'os',
          ellipsis: true
        },
        // {
        //   title: "资源",
        //   dataIndex: "description.resources",
        //   ellipsis: true,

        //   width: 170,
        // },
        {
          title: this.$tl('p.modificationTime'),
          dataIndex: 'updatedAt',

          ellipsis: true,

          width: '170px'
        },
        {
          title: this.$tl('p.operation'),
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
    $tl(key, ...args) {
      return this.$t(`pages.docker.swarm.node.${key}`, ...args)
    },
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
      $confirm({
        title: this.$tl('p.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.areYouSureToRemoveThisNodeFromTheCluster'),
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
        onOk: () => {
          return dockerSwarmNodeLeave({
            nodeId: record.id,
            id: this.id
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
