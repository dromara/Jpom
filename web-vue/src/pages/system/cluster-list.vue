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
            {{ $t('pages.system.cluster-list.5cb025f9') }}
            <template v-for="(item, index) in groupList">
              <a-tag v-if="!groupMap[item]" :key="index">{{ item }}</a-tag>
            </template>
          </div>
          <a-space>
            <a-input
              v-model:value="listQuery['%name%']"
              :placeholder="$t('pages.system.cluster-list.c2dfe194')"
              allow-clear
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['%url%']"
              :placeholder="$t('pages.system.cluster-list.7ee4a10d')"
              allow-clear
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['%localHostName%']"
              :placeholder="$t('pages.system.cluster-list.df83aba7')"
              allow-clear
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-tooltip :title="$t('pages.system.cluster-list.3b2221af')">
              <a-button type="primary" :loading="loading" @click="loadData">{{
                $t('pages.system.cluster-list.53c2763c')
              }}</a-button>
            </a-tooltip>

            <a-tooltip>
              <template #title>
                <ul>
                  <li>{{ $t('pages.system.cluster-list.5f69ccd6') }}</li>
                  <li>{{ $t('pages.system.cluster-list.b56f6283') }}</li>
                  <li>{{ $t('pages.system.cluster-list.7857e2ae') }}</li>
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
            :title="`${$t('pages.system.cluster-list.e0b8d55c')}${record.name || ''}/${$t(
              'pages.system.cluster-list.6a7417e0'
            )}${record.url || ''}/${$t('pages.system.cluster-list.e57df4bc')}${record.statusMsg || ''}`"
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
            <a-button size="small" type="primary" @click="handleEdit(record)">{{
              $t('pages.system.cluster-list.64603c01')
            }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('pages.system.cluster-list.dd20d11c')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 编辑区 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('pages.system.cluster-list.d2c18b73')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item :label="$t('pages.system.cluster-list.3e34ec28')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('pages.system.cluster-list.8d20cb3f')" />
        </a-form-item>
        <a-form-item :label="$t('pages.system.cluster-list.4910fc9b')" name="linkGroups">
          <template #help>
            {{ $t('pages.system.cluster-list.98ad98b6') }}
            <div style="color: red">{{ $t('pages.system.cluster-list.5359c2e1') }}</div>
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
            :placeholder="$t('pages.system.cluster-list.4910fc9b')"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('pages.system.cluster-list.7ee4a10d')" name="url">
          <template #help> {{ $t('pages.system.cluster-list.cdc272ed') }} </template>
          <a-input v-model:value="temp.url" :placeholder="$t('pages.system.cluster-list.12f26edc')" />
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
          title: this.$t('pages.system.cluster-list.c6bacf5f'),
          dataIndex: 'id',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('pages.system.cluster-list.12a8d4cf'),
          dataIndex: 'clusterId',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('pages.system.cluster-list.3e34ec28'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('pages.system.cluster-list.7ee4a10d'),
          dataIndex: 'url',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('pages.system.cluster-list.45e7672d'),
          dataIndex: 'localHostName',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('pages.system.cluster-list.d826aba2'),
          dataIndex: 'jpomVersion',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('pages.system.cluster-list.80150c6b'),
          dataIndex: 'lastHeartbeat',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.system.cluster-list.916db24b'),
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: 120
        },

        {
          title: this.$t('pages.system.cluster-list.f06e8846'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.system.cluster-list.61164914'),
          dataIndex: 'modifyTimeMillis',
          customRender: ({ text }) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: this.$t('pages.system.cluster-list.3bb962bf'),
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',

          width: '120px'
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$t('pages.system.cluster-list.2e3ee77e'), trigger: 'blur' }],
        linkGroups: [{ required: true, message: this.$t('pages.system.cluster-list.5d5a32ef'), trigger: 'blur' }]
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
        title: this.$t('pages.system.cluster-list.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.system.cluster-list.492ead09'),
        okText: this.$t('pages.system.cluster-list.e8e9db25'),
        cancelText: this.$t('pages.system.cluster-list.b12468e9'),
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
            message: this.$t('pages.system.cluster-list.dc29e3db')
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
