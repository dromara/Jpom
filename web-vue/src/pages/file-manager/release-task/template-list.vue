<template>
  <div>
    <a-table
      size="middle"
      :data-source="list"
      :columns="columns"
      bordered
      :pagination="pagination"
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="
        (pagination, filters, sorter) => {
          listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
          loadData()
        }
      "
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            placeholder="模板名称"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%templateTag%']"
            placeholder="模板标记"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.fileType"
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
            placeholder="模板来源"
            class="search-input-item"
          >
            <a-select-option :key="1">文件中心</a-select-option>
            <a-select-option :key="2">静态文件</a-select-option>
          </a-select>

          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'fileId'">
          <a-tooltip :title="text">
            <a-button type="link" style="padding: 0" size="small" @click="handleViewFile(record)">{{
              (text || '').slice(0, 8)
            }}</a-button>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'fileType'">
          <span v-if="text == 2">{{ $t('i18n_28f6e7a67b') }}</span>
          <span v-else>{{ $t('i18n_26183c99bf') }}</span>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script>
import { listTaskTemplate, deleteTaskTemplate } from '@/api/file-manager/release-task-log'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  components: {},
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      loading: false,
      temp: {},

      confirmLoading: false,
      columns: [
        {
          title: '模板名称',
          dataIndex: 'name',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: '模板标记',
          dataIndex: 'templateTag',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: '模板来源',
          dataIndex: 'fileType',
          width: '100px',
          ellipsis: true
        },

        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },

        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          align: 'center',

          fixed: 'right',
          width: '130px'
        }
      ]
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    CHANGE_PAGE,

    parseTime,

    // 获取命令数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      listTaskTemplate(this.listQuery).then((res) => {
        if (200 === res.code) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },

    //  删除命令
    handleDelete(row) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: '确定要删除该发布任务模板吗？',
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteTaskTemplate({
            id: row.id
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
