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
          placeholder="名称"
          class="search-input-item"
          @press-enter="loadData"
        />

        <div>
          悬空
          <a-switch v-model:checked="listQuery['dangling']" checked-children="是" un-checked-children="否" />
        </div>

        <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
      </a-space>
    </template>
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.dataIndex === 'CreatedAt'">
        <a-tooltip placement="topLeft" :title="record.rawValues && record.rawValues['CreatedAt']">
          <span>{{ parseTime(record.rawValues && record.rawValues['CreatedAt']) }}</span>
        </a-tooltip>
      </template>

      <template v-else-if="column.dataIndex === 'name'">
        <a-popover v-if="record.labels" title="卷标签">
          <template #content>
            <p v-for="(value, key) in record.labels" :key="key">{{ key }}<ArrowRightOutlined />{{ value }}</p>
          </template>
          <PushpinOutlined />
        </a-popover>

        <a-tooltip :title="text">
          {{ text }}
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
          <a-tooltip title="删除">
            <a-button size="small" type="link" @click="doAction(record, 'remove')"><DeleteOutlined /></a-button>
          </a-tooltip>
        </a-space>
      </template>
    </template>
  </a-table>
</template>

<script>
import { renderSize, parseTime } from '@/utils/const'
import { dockerVolumesList, dockerVolumesRemove } from '@/api/docker-api'
export default {
  props: {
    id: {
      type: String,
      default: ''
    },
    machineDockerId: {
      type: String,
      default: ''
    },
    urlPrefix: {
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
          title: '序号',
          width: 80,
          ellipsis: true,
          align: 'center',
          customRender: ({ index }) => `${index + 1}`
        },
        {
          title: '名称',
          dataIndex: 'name',
          ellipsis: true
        },
        {
          title: '挂载点',
          dataIndex: 'mountpoint',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '类型',
          dataIndex: 'driver',
          ellipsis: true,
          width: 80,
          tooltip: true
        },
        {
          title: '创建时间',
          dataIndex: 'CreatedAt',
          ellipsis: true,
          width: 180,
          sorter: (a, b) => new Date(a.rawValues.CreatedAt).getTime() - new Date(b.rawValues.CreatedAt).getTime(),
          sortDirections: ['descend', 'ascend'],
          defaultSortOrder: 'descend'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          fixed: 'right',
          width: '80px'
        }
      ],
      action: {
        remove: {
          msg: '您确定要删除当前卷吗？',
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
      dockerVolumesList(this.urlPrefix, this.listQuery).then((res) => {
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
        title: '系统提示',
        content: action.msg,
        zIndex: 1009,
        okText: '确认',
        cancelText: '取消',
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
