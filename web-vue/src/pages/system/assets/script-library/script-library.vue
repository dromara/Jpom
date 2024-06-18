<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="script-library"
      :empty-description="$t('i18n_ef9c90d393')"
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
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
          <a-button type="primary" @click="createScript">{{ $t('i18n_d9ac9228e8') }}</a-button>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $t('i18n_5936ed11ab') }}</div>

            <div>
              <ul>
                <li>{{ $t('i18n_fd93f7f3d7') }}</li>
                <li>
                  {{ $t('i18n_beafc90157') }}
                </li>
                <li>
                  {{ $t('i18n_3f1d478da4') }}
                </li>
              </ul>
            </div>
          </template>
          <QuestionCircleOutlined />
        </a-tooltip>
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
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- pages.system.assets.script-library.ad207008区 -->
    <CustomModal
      v-if="editScriptVisible"
      v-model:open="editScriptVisible"
      destroy-on-close
      :title="$t('i18n_f038f48ce5')"
      :mask-closable="false"
      width="80vw"
      :confirm-loading="confirmLoading"
      @ok="handleEditScriptOk"
    >
      <a-form ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-form-item v-if="temp.id" :label="$t('i18n_fe2df04a16')" name="id">
          <a-input v-model:value="temp.version" disabled read-only />
        </a-form-item>
        <a-form-item :label="$t('i18n_deea5221aa')" name="tag">
          <a-input
            v-model:value="temp.tag"
            :max-length="50"
            :placeholder="$t('i18n_8c4db236e1')"
            :disabled="!!temp.id"
          />
        </a-form-item>
        <a-form-item :label="$t('i18n_2d711b09bd')" name="script">
          <a-form-item-rest>
            <code-editor
              v-model:content="temp.script"
              :show-tool="true"
              height="40vh"
              :options="{ mode: 'shell', tabSize: 2 }"
            >
            </code-editor>
          </a-form-item-rest>
        </a-form-item>

        <a-form-item :label="$t('i18n_3bdd08adab')" name="description">
          <a-textarea
            v-model:value="temp.description"
            :max-length="200"
            :rows="3"
            style="resize: none"
            :placeholder="$t('i18n_bd6c436195')"
          />
        </a-form-item>

        <a-form-item>
          <template #label>
            <a-tooltip
              >{{ $t('i18n_2606b9d0d2') }}<template #title>{{ $t('i18n_73b7b05e6e') }}</template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <template #help>{{ $t('i18n_0c2487d394') }}</template>
          <a-select
            v-model:value="temp.chooseNode"
            show-search
            :filter-option="false"
            :placeholder="$t('i18n_8e6a77838a')"
            mode="multiple"
            @search="searchMachineList"
          >
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </CustomModal>
  </div>
</template>
<script>
import { getScriptLibraryList, editScriptLibrary, delScriptLibrary } from '@/api/system/script-library'
import codeEditor from '@/components/codeEditor'
import { machineSearch } from '@/api/system/assets-machine'
import { CRON_DATA_SOURCE } from '@/utils/const-i18n'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

// import { getWorkSpaceListAll } from '@/api/workspace'

export default {
  components: {
    codeEditor
  },
  props: {},

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

      rules: {
        // name: [{ required: true, message: this.$tl('p.inputScriptName'), trigger: 'blur' }],
        // context: [{ required: true, message: this.$tl('p.inputScriptContent'), trigger: 'blur' }]
      },
      confirmLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  watch: {},
  created() {
    // this.columns.push(
    // );
  },
  mounted() {
    // this.calcTableHeight();

    this.loadData()
  },
  methods: {
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
    // 获取所有节点
    searchMachineList(name) {
      machineSearch({
        name: name,
        limit: 10,
        appendIds: this.temp.machineIds || ''
      }).then((res) => {
        this.nodeList = res.data || []
      })
    },
    createScript() {
      this.temp = {}

      this.editScriptVisible = true
      this.searchMachineList()
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)

      //this.commandParams = data?.defArgs ? JSON.parse(data.defArgs) : []

      this.temp = {
        ...this.temp,
        chooseNode: record?.machineIds ? record.machineIds.split(',') : []
      }
      this.editScriptVisible = true
      this.searchMachineList()
      // getScriptItem({
      //   id: record.id
      // }).then((res) => {
      //   if (res.code === 200) {
      //     const data = res.data.data
      //   }
      // })
    },
    // 提交 Script 数据
    handleEditScriptOk() {
      // 检验表单
      this.$refs['editScriptForm'].validate().then(() => {
        // 提交数据
        this.temp.machineIds = this.temp?.chooseNode?.join(',')
        delete this.temp.nodeList
        this.confirmLoading = true
        editScriptLibrary(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.editScriptVisible = false
              this.loadData()
              this.$refs['editScriptForm'].resetFields()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_a9886f95b6'),
        zIndex: 1009,
        okText: this.$t('i18n_38cf16f220'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return delScriptLibrary({
            id: record.id
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
    },

    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }
  }
}
</script>
