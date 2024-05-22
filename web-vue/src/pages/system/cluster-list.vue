<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      :columns="columns"
      size="middle"
      :pagination="pagination"
      bordered
      :scroll="{
        x: 'max-content'
      }"
      row-key="id"
      @change="
        (pagination, filters, sorter) => {
          listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
          loadData()
        }
      "
    >
      <template #title>
        <a-space direction="vertical" style="width: 100%">
          <div>
            <template v-for="(val, key) in groupMap" :key="key">
              <span>{{ key }}：</span>
              <template v-for="(tag, index) in val" :key="`${tag.id}_${key}`">
                <a-tag :color="`${index === 0 ? 'blue' : 'orange'}`">
                  {{ tag.name }}
                </a-tag>
              </template>
            </template>
          </div>
          <div v-if="groupList.filter((item) => !groupMap[item]).length">
            {{ $tl('p.unboundGroup') }}
            <template v-for="(item, index) in groupList">
              <a-tag v-if="!groupMap[item]" :key="index">{{ item }}</a-tag>
            </template>
          </div>
          <a-space>
            <a-input
              v-model:value="listQuery['%name%']"
              :placeholder="$tl('p.clusterName')"
              allow-clear
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['%url%']"
              :placeholder="$tl('c.clusterAddress')"
              allow-clear
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['%localHostName%']"
              :placeholder="$tl('p.hostname')"
              allow-clear
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-tooltip :title="$tl('p.quickReturnFirstPage')">
              <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
            </a-tooltip>

            <a-tooltip>
              <template #title>
                <ul>
                  <li>{{ $tl('p.clusterCreationInfo') }}</li>
                  <li>{{ $tl('p.newClusterConfig') }}</li>
                  <li>{{ $tl('p.workspaceBinding') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </a-space>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'url'">
          <a-tooltip
            :title="`${$tl('p.inputClusterName')}${record.name || ''}/${$tl('p.address')}${record.url || ''}/${$tl('p.statusMessage')}${record.statusMsg || ''}`"
          >
            <a-button v-if="record.url" type="link" size="small" @click="openUrl(record.url)">
              {{ text }}
            </a-button>
            <span v-else>{{ record.statusMsg }}</span>
            <!-- -->
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('p.edit') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{ $tl('p.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 编辑区 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$tl('p.editCluster')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item :label="$tl('c.name')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$tl('p.workspaceName')" />
        </a-form-item>
        <a-form-item :label="$tl('c.associatedGroup')" name="linkGroups">
          <template #help>
            {{ $tl('p.associatedGroupDescription') }}
            <div style="color: red">{{ $tl('p.bindingNotice') }}</div>
          </template>
          <a-select
            v-model:value="temp.linkGroups"
            show-search
            mode="multiple"
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
            :placeholder="$tl('c.associatedGroup')"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$tl('c.clusterAddress')" name="url">
          <template #help> {{ $tl('p.clusterAddressForWorkspace') }} </template>
          <a-input v-model:value="temp.url" :placeholder="$tl('p.clusterAccessAddress')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { getClusterList, deleteCluster, listLinkGroups, editCluster } from '@/api/system/cluster'
export default {
  data() {
    return {
      loading: false,
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      columns: [
        {
          title: this.$tl('p.installationID'),
          dataIndex: 'id',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$tl('p.clusterID'),
          dataIndex: 'clusterId',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$tl('c.name'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$tl('c.clusterAddress'),
          dataIndex: 'url',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$tl('p.clusterHostname'),
          dataIndex: 'localHostName',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$tl('p.version'),
          dataIndex: 'jpomVersion',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$tl('p.lastHeartbeatTime'),
          dataIndex: 'lastHeartbeat',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.modifier'),
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: 120
        },

        {
          title: this.$tl('p.creationTime'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.modificationTime'),
          dataIndex: 'modifyTimeMillis',
          customRender: ({ text }) => parseTime(text),
          sorter: true,
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
      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$tl('p.inputClusterNamePrompt'), trigger: 'blur' }],
        linkGroups: [{ required: true, message: this.$tl('p.inputAssociatedGroupPrompt'), trigger: 'blur' }]
        // url: [{ required: true, message: "请输入集群访问地址", trigger: "blur" }],
      },
      editVisible: false,
      temp: {},
      groupList: [],
      groupMap: {},
      confirmLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  created() {
    this.loadData()
    this.loadGroupList()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.system.clusterList.${key}`, ...args)
    },
    parseTime,
    CHANGE_PAGE,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getClusterList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$tl('p.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.deleteClusterConfirmation'),
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
        onOk: () => {
          return deleteCluster(record.id).then((res) => {
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
    // 获取所有的分组
    loadGroupList() {
      listLinkGroups().then((res) => {
        if (res.data) {
          this.groupList = res.data.linkGroups || []
          this.groupMap = res.data.groupMap || {}
        }
      })
    },
    // 编辑
    handleEdit(record) {
      this.loadGroupList()
      this.temp = Object.assign({}, record, {
        linkGroups: (record.linkGroup || '').split(',').filter((item) => item)
      })
      this.editVisible = true
    },
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        const newData = { ...this.temp }
        const linkGroups = newData.linkGroups
        if (!linkGroups) {
          $notification.error({
            message: this.$tl('p.selectAssociatedGroup')
          })
          return false
        }
        delete newData.linkGroups
        newData.linkGroup = linkGroups.join(',')
        this.confirmLoading = true
        editCluster(newData)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editForm'].resetFields()
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
    openUrl(url) {
      window.open(url)
    }
  }
}
</script>
