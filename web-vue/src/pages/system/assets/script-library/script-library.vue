<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="script-library"
      :empty-description="$t('pages.system.assets.script-library.65418a5e')"
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
            :placeholder="$t('pages.system.assets.script-library.95547f9')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%version%']"
            :placeholder="$t('pages.system.assets.script-library.81634069')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%description%']"
            :placeholder="$t('pages.system.assets.script-library.f89e58f1')"
            class="search-input-item"
            @press-enter="loadData"
          />

          <a-tooltip :title="$t('pages.system.assets.script-library.986e8dc2')">
            <a-button :loading="loading" type="primary" @click="loadData">{{
              $t('pages.system.assets.script-library.43934f6d')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="createScript">{{
            $t('pages.system.assets.script-library.9f3089ce')
          }}</a-button>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $t('pages.system.assets.script-library.41c0cbe5') }}</div>

            <div>
              <ul>
                <li>{{ $t('pages.system.assets.script-library.423e1405') }}</li>
                <li>
                  {{ $t('pages.system.assets.script-library.script-library.8cc019db') }}
                </li>
                <li>
                  {{ $t('pages.system.assets.script-library.script-library.1b3a0157') }}
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
            <a-button size="small" type="primary" @click="handleEdit(record)">{{
              $t('pages.system.assets.script-library.ad207008')
            }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('pages.system.assets.script-library.ecbd7449')
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
      :title="$t('pages.system.assets.script-library.16a6aab6')"
      :mask-closable="false"
      width="80vw"
      :confirm-loading="confirmLoading"
      @ok="handleEditScriptOk"
    >
      <a-form ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-form-item v-if="temp.id" :label="$t('pages.system.assets.script-library.81634069')" name="id">
          <a-input v-model:value="temp.version" disabled read-only />
        </a-form-item>
        <a-form-item :label="$t('pages.system.assets.script-library.2d62ebdb')" name="tag">
          <a-input
            v-model:value="temp.tag"
            :max-length="50"
            :placeholder="$t('pages.system.assets.script-library.e37b1ac9')"
            :disabled="!!temp.id"
          />
        </a-form-item>
        <a-form-item :label="$t('pages.system.assets.script-library.3e7aa0ad')" name="script">
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

        <a-form-item :label="$t('pages.system.assets.script-library.f89e58f1')" name="description">
          <a-textarea
            v-model:value="temp.description"
            :max-length="200"
            :rows="3"
            style="resize: none"
            :placeholder="$t('pages.system.assets.script-library.43075dd9')"
          />
        </a-form-item>

        <a-form-item>
          <template #label>
            <a-tooltip
              >{{ $t('pages.system.assets.script-library.4f5ca5e3')
              }}<template #title>{{ $t('pages.system.assets.script-library.33437c9b') }}</template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <template #help>{{ $t('pages.system.assets.script-library.5251812f') }}</template>
          <a-select
            v-model:value="temp.chooseNode"
            show-search
            :filter-option="false"
            :placeholder="$t('pages.system.assets.script-library.4722ff63')"
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
          title: this.$t('pages.system.assets.script-library.2d62ebdb'),
          dataIndex: 'tag',
          ellipsis: true,
          sorter: true,
          width: 150
        },
        {
          title: this.$t('pages.system.assets.script-library.81634069'),
          dataIndex: 'version',
          ellipsis: true,
          sorter: true,
          width: '100px',
          tooltip: true
        },
        {
          title: this.$t('pages.system.assets.script-library.f89e58f1'),
          dataIndex: 'description',
          ellipsis: true,
          width: 200,
          tooltip: true
        },
        {
          title: this.$t('pages.system.assets.script-library.d3b29478'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: this.$t('pages.system.assets.script-library.efaf9956'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: this.$t('pages.system.assets.script-library.339d15b5'),
          dataIndex: 'createUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$t('pages.system.assets.script-library.8605b4f2'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },

        {
          title: this.$t('pages.system.assets.script-library.fe731dfc'),
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
        title: this.$t('pages.system.assets.script-library.3875bf60'),
        content: this.$t('pages.system.assets.script-library.72df294d'),
        zIndex: 1009,
        okText: this.$t('pages.system.assets.script-library.d507abff'),
        cancelText: this.$t('pages.system.assets.script-library.a0451c97'),
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
