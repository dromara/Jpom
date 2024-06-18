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
            {{ $t('i18n_7dde69267a') }}
            <template v-for="(item, index) in groupList">
              <a-tag v-if="!groupMap[item]" :key="index">{{ item }}</a-tag>
            </template>
          </div>
          <a-space>
            <a-input
              v-model:value="listQuery['%name%']"
              :placeholder="$t('i18n_c3f28b34bb')"
              allow-clear
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['%url%']"
              :placeholder="$t('i18n_8a414f832f')"
              allow-clear
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['%localHostName%']"
              :placeholder="$t('i18n_6707667676')"
              allow-clear
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-tooltip :title="$t('i18n_4838a3bd20')">
              <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
            </a-tooltip>

            <a-tooltip>
              <template #title>
                <ul>
                  <li>{{ $t('i18n_5177c276a0') }}</li>
                  <li>{{ $t('i18n_649d7fcb73') }}</li>
                  <li>{{ $t('i18n_9c84cd926b') }}</li>
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
            :title="`${$t('i18n_f668c8c881')}${record.name || ''}/${$t('i18n_df3833270b')}${record.url || ''}/${$t(
              'i18n_8d13037eb7'
            )}${record.statusMsg || ''}`"
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
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 编辑区 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_8d3d771ab6')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item :label="$t('i18n_d7ec2d3fea')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_6a588459d0')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_79c6b6cff7')" name="linkGroups">
          <template #help>
            {{ $t('i18n_4089cfb557') }}
            <div style="color: red">{{ $t('i18n_f9898595a0') }}</div>
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
            :placeholder="$t('i18n_79c6b6cff7')"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('i18n_8a414f832f')" name="url">
          <template #help> {{ $t('i18n_fcca8452fe') }} </template>
          <a-input v-model:value="temp.url" :placeholder="$t('i18n_8aebf966b2')" />
        </a-form-item>
      </a-form>
    </CustomModal>
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
          title: this.$t('i18n_ed8ea20fe6'),
          dataIndex: 'id',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_7329a2637c'),
          dataIndex: 'clusterId',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_8a414f832f'),
          dataIndex: 'url',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_24d695c8e2'),
          dataIndex: 'localHostName',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_d0b2958432'),
          dataIndex: 'jpomVersion',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_f68f9b1d1b'),
          dataIndex: 'lastHeartbeat',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: 120
        },

        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          customRender: ({ text }) => parseTime(text),
          sorter: true,
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

      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$t('i18n_debdfce084'), trigger: 'blur' }],
        linkGroups: [{ required: true, message: this.$t('i18n_3d3d3ed34c'), trigger: 'blur' }]
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_2e0094d663'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
            message: this.$t('i18n_e0d6976b48')
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
