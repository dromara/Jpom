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
            :placeholder="$t('i18n_d7ec2d3fea')"
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
            :placeholder="$t('i18n_464f3d4ea3')"
            class="search-input-item"
          >
            <a-select-option key="worker">{{ $t('i18n_41e9f0c9c6') }}</a-select-option>
            <a-select-option key="manager">{{ $t('i18n_a6269ede6c') }}</a-select-option>
          </a-select>

          <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          <a-statistic-countdown format="s" :title="$t('i18n_0f8403d07e')" :value="countdownTime" @finish="loadData">
            <template #suffix>
              <div style="font-size: 12px">{{ $t('i18n_ee6ce96abb') }}</div>
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
            :title="`${$t('i18n_07a0e44145')}${record.description && record.description.hostname}`"
          >
            <template #content>
              <p>
                {{ $t('i18n_a472019766') }}: <a-tag>{{ record.id }}</a-tag>
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
                  {{ $t('i18n_fe2df04a16') }}:
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
            :title="`${$t('i18n_9b3e947cc9')}${record.status && record.status.state} ${$t('i18n_fb91527ce5')}${
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
            :title="`${$t('i18n_20f32e1979')}${record.spec && record.spec.role} ${
              record.managerStatus && record.managerStatus.reachability === 'REACHABLE'
                ? $t('i18n_88c5680d0d') + record.managerStatus.reachability
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
            :title="`${$t('i18n_bf94b97d1a')}${text} ${$t('i18n_312f45014a')}${record.createdAt}`"
          >
            <span>
              {{ text }}
            </span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <template v-if="record.managerStatus && record.managerStatus.leader">
              <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_8347a927c0') }}</a-button>
              <a-tooltip :title="$t('i18n_28c1c35cd9')">
                <a-button size="small" type="primary" danger :disabled="true">{{ $t('i18n_b3b1f709d4') }}</a-button>
              </a-tooltip>
            </template>
            <template v-else>
              <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_8347a927c0') }}</a-button>
              <a-button size="small" type="primary" danger @click="handleLeava(record)">{{
                $t('i18n_b3b1f709d4')
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
      :title="$t('i18n_61e7fa1227')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('i18n_464f3d4ea3')" name="role">
          <a-radio-group v-model:value="temp.role" name="role" :disabled="temp.leader">
            <a-radio value="WORKER"> {{ $t('i18n_41e9f0c9c6') }}</a-radio>
            <a-radio value="MANAGER"> {{ $t('i18n_a6269ede6c') }} </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('i18n_3fea7ca76c')" name="availability">
          <a-radio-group v-model:value="temp.availability" name="availability">
            <a-radio value="ACTIVE"> {{ $t('i18n_fe32def462') }}</a-radio>
            <a-radio value="PAUSE"> {{ $t('i18n_8d63ef388e') }} </a-radio>
            <a-radio value="DRAIN"> {{ $t('i18n_f113c10ade') }} </a-radio>
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
        role: [{ required: true, message: this.$t('i18n_9d7d471b77'), trigger: 'blur' }],
        availability: [{ required: true, message: this.$t('i18n_4c7c58b208'), trigger: 'blur' }]
      },

      columns: [
        {
          title: this.$t('i18n_faaadc447b'),
          width: 80,
          ellipsis: true,
          align: 'center',
          customRender: ({ index }) => `${index + 1}`
        },
        // { title: "节点Id", dataIndex: "id", ellipsis: true, },
        {
          title: this.$t('i18n_6707667676'),
          dataIndex: 'hostname',
          ellipsis: true
        },
        {
          title: this.$t('i18n_c1786d9e11'),
          width: 150,
          dataIndex: 'address',
          ellipsis: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          width: 140,
          dataIndex: 'state',
          ellipsis: true
        },
        {
          title: this.$t('i18n_464f3d4ea3'),
          width: 110,
          dataIndex: 'role',
          ellipsis: true
        },

        {
          title: this.$t('i18n_996dc32a98'),
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
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'updatedAt',

          ellipsis: true,

          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_f5399c620e'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
