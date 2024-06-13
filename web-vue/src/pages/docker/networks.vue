<template>
  <a-table
    size="middle"
    :data-source="list"
    :columns="columns"
    :pagination="false"
    bordered
    :scroll="{
      x: 'max-content'
    }"
  >
    <template #title>
      <a-space>
        <a-input
          v-model:value="listQuery['name']"
          :placeholder="$t('pages.docker.networks.3e34ec28')"
          class="search-input-item"
          @press-enter="loadData"
        />
        <a-input
          v-model:value="listQuery['networkId']"
          placeholder="id"
          class="search-input-item"
          @press-enter="loadData"
        />

        <a-button type="primary" :loading="loading" @click="loadData">{{
          $t('pages.docker.networks.53c2763c')
        }}</a-button>
      </a-space>
    </template>
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.dataIndex === 'Created'">
        <a-tooltip placement="topLeft" :title="record.rawValues && record.rawValues['Created']">
          <span>{{ parseTime(record.rawValues && record.rawValues['Created']) }}</span>
        </a-tooltip>
      </template>
      <template v-else-if="column.dataIndex === 'ipam'">
        <a-tooltip
          placement="topLeft"
          :title="`${text && text.driver}  ${
            text &&
            text.config &&
            text.config
              .map((item) => {
                return (
                  ($t('pages.docker.networks.73c4a51e') + item.gateway || '') +
                  '#' +
                  ($t('pages.docker.networks.cf0aa0c1') + item.subnet || '')
                )
              })
              .join(',')
          }`"
        >
          <span>{{
            text &&
            text.config &&
            text.config
              .map((item) => {
                return (item.gateway || '') + '#' + (item.subnet || '')
              })
              .join(',')
          }}</span>
        </a-tooltip>
      </template>

      <template v-else-if="column.tooltip">
        <a-tooltip placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
      </template>

      <template v-else-if="column.id">
        <a-tooltip :title="text">
          <span> {{ text.split(':')[1].slice(0, 12) }}</span>
        </a-tooltip>
      </template>
      <template v-else-if="column.dataIndex === 'operation'">
        <a-space>
          <a-tooltip :title="$t('pages.docker.networks.dd20d11c')">
            <a-button size="small" type="link" @click="doAction(record, 'remove')"><DeleteOutlined /></a-button>
          </a-tooltip>
        </a-space>
      </template>
    </template>
  </a-table>
</template>
<script>
import { renderSize, parseTime } from '@/utils/const'
import { dockerNetworksList, dockerVolumesRemove } from '@/api/docker-api'
export default {
  props: {
    id: {
      type: String,
      default: ''
    },
    urlPrefix: {
      type: String,
      default: ''
    },
    machineDockerId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      list: [],
      loading: false,
      listQuery: {
        dangling: false
      },
      renderSize,
      columns: [
        {
          title: this.$t('pages.docker.networks.72cebb96'),
          width: 80,
          ellipsis: true,
          align: 'center',
          customRender: ({ index }) => `${index + 1}`
        },
        {
          title: this.$t('pages.docker.networks.3e34ec28'),
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },
        {
          title: 'id',
          dataIndex: 'id',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.docker.networks.1907c647'),
          dataIndex: 'scope',
          ellipsis: true
        },
        {
          title: 'IPAM',
          dataIndex: 'ipam',
          ellipsis: true
        },
        {
          title: this.$t('pages.docker.networks.698bb532'),
          dataIndex: 'driver',
          ellipsis: true,
          width: 80
        },
        {
          title: this.$t('pages.docker.networks.f06e8846'),
          dataIndex: 'Created',
          ellipsis: true,
          width: 180,
          sorter: (a, b) => new Date(a.rawValues.Created).getTime() - new Date(b.rawValues.Created).getTime(),
          sortDirections: ['descend', 'ascend'],
          defaultSortOrder: 'descend'
        }
        // { title: "操作", dataIndex: "operation", width: 80 },
      ],
      action: {
        remove: {
          msg: this.$t('pages.docker.networks.9e6ac7fa'),
          api: dockerVolumesRemove
        }
      }
    }
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    parseTime,
    // 加载数据
    loadData() {
      this.loading = true
      //this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.id = this.reqDataId
      dockerNetworksList(this.urlPrefix, this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data
        }
        this.loading = false
      })
    },
    doAction(record, actionKey) {
      const action = this.action[actionKey]
      if (!action) {
        return
      }
      $confirm({
        title: this.$t('pages.docker.networks.b22d55a0'),
        zIndex: 1009,
        content: action.msg,
        okText: this.$t('pages.docker.networks.e8e9db25'),
        cancelText: this.$t('pages.docker.networks.b12468e9'),
        onOk: () => {
          return action
            .api(this.urlPrefix, {
              id: this.reqDataId,
              volumeName: record.name
            })
            .then((res) => {
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
