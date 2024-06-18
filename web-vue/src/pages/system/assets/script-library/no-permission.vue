<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="script-library-no-permission"
      :empty-description="$t('i18n_824914133f')"
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      :row-selection="rowSelection"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%tag%']"
            :placeholder="$t('i18n_e17a6882b6')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%version%']"
            :placeholder="$t('i18n_fe2df04a16')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%description%']"
            :placeholder="$t('i18n_3bdd08adab')"
            class="search-input-item"
            @press-enter="loadData"
          />

          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button :loading="loading" type="primary" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
        </a-space>
      </template>

      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'nodeId'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ nodeMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_a156349591') }}</a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- pages.system.assets.script-library.ad207008区 -->
    <CustomModal
      v-if="editScriptVisible"
      v-model:open="editScriptVisible"
      destroy-on-close
      :title="$t('i18n_dd1d14efd6')"
      :mask-closable="false"
      width="80vw"
      :footer="false"
    >
      <a-form ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-form-item :label="$t('i18n_fe2df04a16')" name="id">
          <a-input v-model:value="temp.version" disabled read-only />
        </a-form-item>
        <a-form-item :label="$t('i18n_deea5221aa')" name="tag">
          <a-input v-model:value="temp.tag" :max-length="50" disabled />
        </a-form-item>
        <a-form-item :label="$t('i18n_2d711b09bd')" name="script">
          <a-form-item-rest>
            <code-editor
              v-model:content="temp.script"
              :show-tool="true"
              height="40vh"
              :options="{ mode: 'shell', tabSize: 2, readOnly: true }"
            >
            </code-editor>
          </a-form-item-rest>
        </a-form-item>

        <a-form-item :label="$t('i18n_3bdd08adab')" name="description">
          <a-textarea v-model:value="temp.description" :max-length="200" :rows="3" style="resize: none" disabled />
        </a-form-item>
      </a-form>
    </CustomModal>
  </div>
</template>
<script>
import { getScriptLibraryList } from '@/api/system/script-library'
import codeEditor from '@/components/codeEditor'

import { CRON_DATA_SOURCE } from '@/utils/const-i18n'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { increaseZIndex } from '@/utils/utils'
// import { getWorkSpaceListAll } from '@/api/workspace'

export default {
  components: {
    codeEditor
  },
  props: {},
  emits: ['scriptConfirm', 'tagConfirm'],

  data() {
    return {
      // choose: this.choose,
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      CRON_DATA_SOURCE,
      list: [],
      temp: {},
      nodeList: [],
      editScriptVisible: false,
      drawerTitle: '',
      drawerConsoleVisible: false,
      columns: [
        {
          title: this.$t('i18n_deea5221aa'),
          dataIndex: 'tag',
          ellipsis: true,
          sorter: true,
          width: 150
        },
        {
          title: this.$t('i18n_fe2df04a16'),
          dataIndex: 'version',
          ellipsis: true,
          sorter: true,
          width: '100px',
          tooltip: true
        },
        {
          title: this.$t('i18n_3bdd08adab'),
          dataIndex: 'description',
          ellipsis: true,
          width: 200,
          tooltip: true
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: this.$t('i18n_95a43eaa59'),
          dataIndex: 'createUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },

        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          align: 'center',

          fixed: 'right',
          width: '140px'
        }
      ],

      tableSelections: [],
      selectedRowKeys: [],
      rules: {}
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections,
        type: 'radio'
      }
    }
  },
  watch: {},
  created() {},
  mounted() {
    this.loadData()
  },
  methods: {
    increaseZIndex,
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      getScriptLibraryList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    parseTime,
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      this.editScriptVisible = true
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    handerScriptConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$t('i18n_364bea440e')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return item.id === this.tableSelections[0]
      })?.[0]
      this.$emit('scriptConfirm', `${selectData.script}`)
    },
    handerTagConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$t('i18n_364bea440e')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return item.id === this.tableSelections[0]
      })?.[0]
      this.$emit('tagConfirm', `${selectData.tag}`)
    }
  }
}
</script>
