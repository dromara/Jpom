<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="script-library"
      empty-description="没有任何的脚本库"
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
            placeholder="脚本标记"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%version%']"
            placeholder="版本"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%description%']"
            placeholder="描述"
            class="search-input-item"
            @press-enter="loadData"
          />

          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="createScript">创建</a-button>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>脚本库用于存储管理通用的脚本,脚本库中的脚本不能直接执行。</div>

            <div>
              <ul>
                <li>可以将脚本分发到机器节点中在 DSL 项目中引用，达到多个项目共用相同脚本</li>
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
            <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <a-modal
      v-model:open="editScriptVisible"
      destroy-on-close
      :z-index="1009"
      title="编辑脚本"
      :mask-closable="false"
      width="80vw"
      :confirm-loading="confirmLoading"
      @ok="handleEditScriptOk"
    >
      <a-form ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-form-item v-if="temp.id" label="版本" name="id">
          <a-input v-model:value="temp.version" disabled read-only />
        </a-form-item>
        <a-form-item label="标记" name="tag">
          <a-input
            v-model:value="temp.tag"
            :max-length="50"
            placeholder="请输入脚本标记，标记只能是字母或者数字长度需要小于 20 并且全局唯一"
            :disabled="!!temp.id"
          />
        </a-form-item>
        <a-form-item label="内容" name="script">
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

        <a-form-item label="描述" name="description">
          <a-textarea
            v-model:value="temp.description"
            :max-length="200"
            :rows="3"
            style="resize: none"
            placeholder="请输入脚本描述"
          />
        </a-form-item>

        <a-form-item>
          <template #label>
            <a-tooltip>
              分发机器
              <template #title> 将脚本分发到对应的机器节点中，对应的机器节点可以引用对应的脚本 </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <template #help>下拉搜索默认搜索关键词相关的前 10 个，以及已经选择的机器节点</template>
          <a-select
            v-model:value="temp.chooseNode"
            show-search
            :filter-option="false"
            placeholder="请选择要分发到的机器节点"
            mode="multiple"
            @search="searchMachineList"
          >
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { getScriptLibraryList, editScriptLibrary, delScriptLibrary } from '@/api/system/script-library'
import codeEditor from '@/components/codeEditor'
import { machineSearch } from '@/api/system/assets-machine'

import { CHANGE_PAGE, COMPUTED_PAGINATION, CRON_DATA_SOURCE, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

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
          title: '标记',
          dataIndex: 'tag',
          ellipsis: true,
          sorter: true,
          width: 150
        },
        {
          title: '版本',
          dataIndex: 'version',
          ellipsis: true,
          sorter: true,
          width: '100px',
          tooltip: true
        },
        {
          title: '描述',
          dataIndex: 'description',
          ellipsis: true,
          width: 100,
          tooltip: true
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: '创建人',
          dataIndex: 'createUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: '修改人',
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },

        {
          title: '操作',
          dataIndex: 'operation',
          align: 'center',

          fixed: 'right',
          width: '240px'
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
        title: '系统提示',
        content: '确定要删除此脚本库吗？',
        zIndex: 1009,
        okText: '确定',
        cancelText: '取消',
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
