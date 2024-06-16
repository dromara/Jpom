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
            :placeholder="$t('pages.docker.swarm.node.bb769c1d')"
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
            :placeholder="$t('pages.docker.swarm.node.377e1844')"
            class="search-input-item"
          >
            <a-select-option key="worker">{{ $t('pages.docker.swarm.node.e4035840') }}</a-select-option>
            <a-select-option key="manager">{{ $t('pages.docker.swarm.node.acf5e304') }}</a-select-option>
          </a-select>

          <a-button type="primary" :loading="loading" @click="loadData">{{
            $t('pages.docker.swarm.node.53c2763c')
          }}</a-button>
          <a-statistic-countdown
            format="s"
            :title="$t('pages.docker.swarm.node.ae8f1e')"
            :value="countdownTime"
            @finish="loadData"
          >
            <template #suffix>
              <div style="font-size: 12px">{{ $t('pages.docker.swarm.node.28cda8') }}</div>
            </template>
          </a-statistic-countdown>
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
            :title="`${$t('pages.docker.swarm.node.7fb10499')}${record.description && record.description.hostname}`"
          >
            <template #content>
              <p>
                {{ $t('pages.docker.swarm.node.ee50ba1c') }}: <a-tag>{{ record.id }}</a-tag>
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
                  {{ $t('pages.docker.swarm.node.d826aba2') }}:
                  <a-tag>{{ record.description.engine.engineVersion }}</a-tag>
                </p>
              </template>
            </template>

            <span>{{ record.description && record.description.hostname }}</span>
          </a-popover>
        </template>

        <template v-else-if="column.dataIndex === 'state'">
          <a-tooltip
            placement="topLeft"
            :title="`${$t('pages.docker.swarm.node.87c09576')}${record.status && record.status.state} ${$t(
              'pages.docker.swarm.node.147fba6c'
            )}${record.spec ? record.spec.availability || '' : ''}`"
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
            :title="`${$t('pages.docker.swarm.node.ec4b9b1')}${record.spec && record.spec.role} ${
              record.managerStatus && record.managerStatus.reachability === 'REACHABLE'
                ? $t('pages.docker.swarm.node.c1956a20') + record.managerStatus.reachability
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
          <a-tooltip
            placement="topLeft"
            :title="`${$t('pages.docker.swarm.node.a2b40316')}${text} ${$t('pages.docker.swarm.node.f5b90169')}${
              record.createdAt
            }`"
          >
            <span>
              {{ text }}
            </span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <template v-if="record.managerStatus && record.managerStatus.leader">
              <a-button size="small" type="primary" @click="handleEdit(record)">{{
                $t('pages.docker.swarm.node.e2f053aa')
              }}</a-button>
              <a-tooltip :title="$t('pages.docker.swarm.node.55ade145')">
                <a-button size="small" type="primary" danger :disabled="true">{{
                  $t('pages.docker.swarm.node.7d868663')
                }}</a-button>
              </a-tooltip>
            </template>
            <template v-else>
              <a-button size="small" type="primary" @click="handleEdit(record)">{{
                $t('pages.docker.swarm.node.e2f053aa')
              }}</a-button>
              <a-button size="small" type="primary" danger @click="handleLeava(record)">{{
                $t('pages.docker.swarm.node.7d868663')
              }}</a-button>
            </template>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑节点 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('pages.docker.swarm.node.9fa2f20c')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('pages.docker.swarm.node.377e1844')" name="role">
          <a-radio-group v-model:value="temp.role" name="role" :disabled="temp.leader">
            <a-radio value="WORKER"> {{ $t('pages.docker.swarm.node.e4035840') }}</a-radio>
            <a-radio value="MANAGER"> {{ $t('pages.docker.swarm.node.acf5e304') }} </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('pages.docker.swarm.node.6e06fe4f')" name="availability">
          <a-radio-group v-model:value="temp.availability" name="availability">
            <a-radio value="ACTIVE"> {{ $t('pages.docker.swarm.node.ac2c5199') }}</a-radio>
            <a-radio value="PAUSE"> {{ $t('pages.docker.swarm.node.a183e300') }} </a-radio>
            <a-radio value="DRAIN"> {{ $t('pages.docker.swarm.node.312559de') }} </a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </CustomModal>
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
        role: [{ required: true, message: this.$t('pages.docker.swarm.node.b559696f'), trigger: 'blur' }],
        availability: [{ required: true, message: this.$t('pages.docker.swarm.node.3f74c0ab'), trigger: 'blur' }]
      },

      columns: [
        {
          title: this.$t('pages.docker.swarm.node.72cebb96'),
          width: 80,
          ellipsis: true,
          align: 'center',
          customRender: ({ index }) => `${index + 1}`
        },
        // { title: "节点Id", dataIndex: "id", ellipsis: true, },
        {
          title: this.$t('pages.docker.swarm.node.2a72f1e6'),
          dataIndex: 'hostname',
          ellipsis: true
        },
        {
          title: this.$t('pages.docker.swarm.node.a0d52737'),
          width: 150,
          dataIndex: 'address',
          ellipsis: true
        },
        {
          title: this.$t('pages.docker.swarm.node.6e06fe4f'),
          width: 140,
          dataIndex: 'state',
          ellipsis: true
        },
        {
          title: this.$t('pages.docker.swarm.node.377e1844'),
          width: 110,
          dataIndex: 'role',
          ellipsis: true
        },

        {
          title: this.$t('pages.docker.swarm.node.c4c0c00a'),
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
          title: this.$t('pages.docker.swarm.node.61164914'),
          dataIndex: 'updatedAt',

          ellipsis: true,

          width: '170px'
        },
        {
          title: this.$t('pages.docker.swarm.node.3bb962bf'),
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
      $confirm({
        title: this.$t('pages.docker.swarm.node.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.docker.swarm.node.b35e191c'),
        okText: this.$t('pages.docker.swarm.node.e8e9db25'),
        cancelText: this.$t('pages.docker.swarm.node.b12468e9'),
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
