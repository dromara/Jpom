<template>
  <a-table
    size="middle"
    :data-source="list"
    :columns="columns"
    :pagination="false"
    bordered
    :rowKey="(record, index) => index"
  >
    <template slot="title">
      <a-space>
        <a-input v-model="listQuery['name']" @pressEnter="loadData" placeholder="名称" class="search-input-item" />

        <div>
          悬空
          <a-switch checked-children="是" un-checked-children="否" v-model="listQuery['dangling']" />
        </div>

        <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
      </a-space>
    </template>

    <a-tooltip slot="CreatedAt" slot-scope="text" placement="topLeft" :title="text['CreatedAt']">
      <span>{{ parseTime(text['CreatedAt']) }}</span>
    </a-tooltip>
    <template slot="name" slot-scope="text, record">
      <a-popover title="卷标签" v-if="record.labels">
        <template slot="content">
          <p v-for="(value, key) in record.labels" :key="key">{{ key }}<a-icon type="arrow-right" />{{ value }}</p>
        </template>
        <a-icon type="pushpin" />
      </a-popover>

      <a-tooltip :title="text">
        {{ text }}
      </a-tooltip>
    </template>
    <!-- <a-tooltip slot="name" slot-scope="text, record" placement="topLeft" :title="renderSize(text) + ' ' + renderSize(record.virtualSize)">
      <span>{{ renderSize(text) }}</span>
    </a-tooltip> -->

    <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
      <span>{{ text }}</span>
    </a-tooltip>

    <a-tooltip slot="id" slot-scope="text" :title="text">
      <span> {{ text.split(':')[1].slice(0, 12) }}</span>
    </a-tooltip>
    <template slot="operation" slot-scope="text, record">
      <a-space>
        <a-tooltip title="删除">
          <a-button size="small" type="link" @click="doAction(record, 'remove')"><a-icon type="delete" /></a-button>
        </a-tooltip>
      </a-space>
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
      type: String
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
          customRender: (text, record, index) => `${index + 1}`
        },
        { title: '名称', dataIndex: 'name', ellipsis: true, scopedSlots: { customRender: 'name' } },
        { title: '挂载点', dataIndex: 'mountpoint', ellipsis: true, scopedSlots: { customRender: 'tooltip' } },
        { title: '类型', dataIndex: 'driver', ellipsis: true, width: 80, scopedSlots: { customRender: 'tooltip' } },
        {
          title: '创建时间',
          dataIndex: 'rawValues',
          ellipsis: true,
          width: 180,
          sorter: (a, b) => new Date(a.rawValues.CreatedAt).getTime() - new Date(b.rawValues.CreatedAt).getTime(),
          sortDirections: ['descend', 'ascend'],
          defaultSortOrder: 'descend',
          scopedSlots: { customRender: 'CreatedAt' }
        },
        { title: '操作', dataIndex: 'operation', scopedSlots: { customRender: 'operation' }, width: 80 }
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
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 组装参数
          const params = {
            id: this.reqDataId,
            volumeName: record.name
          }
          action.api(this.urlPrefix, params).then((res) => {
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
