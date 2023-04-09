<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      :columns="columns"
      size="middle"
      :pagination="pagination"
      bordered
      @change="changePage"
      :rowKey="(record, index) => index"
    >
      <template slot="title">
        <a-space>
          <a-input
            v-model="listQuery['%name%']"
            @pressEnter="loadData"
            placeholder="工作空间名称"
            allowClear
            class="search-input-item"
          />

          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
          <a-tooltip>
            <template slot="title">
              <ul>
                <li>工作空间用于隔离数据,工作空间下面可以有不同数据,不同权限,不同菜单等来实现权限控制</li>
                <li>工作空间环境变量用于构建命令相关</li>
              </ul>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="description" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-button size="small" type="primary" @click="configMeun(record)">菜单</a-button>
          <a-button size="small" type="primary" @click="viewEnvVar(record)">变量</a-button>
          <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model="editVisible" title="编辑工作空间" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item label="名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="工作空间名称" />
        </a-form-item>

        <a-form-item label="描述" prop="description">
          <a-input v-model="temp.description" :maxLength="200" type="textarea" :rows="5" placeholder="工作空间描述" />
        </a-form-item>
      </a-form-model>
    </a-modal>
    <!-- 环境变量 -->
    <a-modal
      destroyOnClose
      v-model="envVarListVisible"
      :title="`${temp.name} 工作空间环境变量`"
      width="80vw"
      :footer="null"
      :maskClosable="false"
    >
      <workspaceEnv ref="workspaceEnv" :workspaceId="temp.id" />
    </a-modal>
    <!-- 工作空间菜单 -->
    <a-modal
      destroyOnClose
      v-model="configMenuVisible"
      :title="`${temp.name} 工作空间菜单`"
      @ok="onSubmitMenus"
      :maskClosable="false"
    >
      <a-form-model ref="editWhiteForm" :model="menusConfigData">
        <a-row type="flex" justify="center">
          <a-alert :message="`菜单配置只对非超级管理员生效`" style="margin-top: 10px; margin-bottom: 20px" banner />
          <a-col :span="12">
            <a-card title="服务端菜单" :bordered="false">
              <a-tree
                show-icon
                v-if="menusConfigData.serverMenus"
                checkable
                :tree-data="menusConfigData.serverMenus"
                :replaceFields="replaceFields"
                v-model="menusConfigData.serverMenuKeys"
              >
                <a-icon slot="switcherIcon" type="down" />

                <template slot="custom" slot-scope="{ dataRef }">
                  <a-icon :type="dataRef.icon_v3" />
                </template>
              </a-tree>
            </a-card>
          </a-col>
          <a-col :span="12">
            <a-card title="节点菜单" :bordered="false">
              <a-tree
                show-icon
                v-if="menusConfigData.nodeMenus"
                checkable
                :tree-data="menusConfigData.nodeMenus"
                :replaceFields="replaceFields"
                v-model="menusConfigData.nodeMenuKeys"
              >
                <a-icon slot="switcherIcon" type="down" />

                <template slot="custom" slot-scope="{ dataRef }">
                  <a-icon :type="dataRef.icon_v3" />
                </template>
              </a-tree>
            </a-card>
          </a-col>
        </a-row>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { deleteWorkspace, editWorkSpace, getWorkSpaceList, getMenusConfig, saveMenusConfig } from '@/api/workspace'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import workspaceEnv from './workspace-env.vue'

export default {
  components: {
    workspaceEnv
  },
  data() {
    return {
      loading: false,
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editVisible: false,
      envVarListVisible: false,
      temp: {},
      columns: [
        { title: '名称', dataIndex: 'name', ellipsis: true, width: 200, scopedSlots: { customRender: 'name' } },
        {
          title: '描述',
          dataIndex: 'description',
          ellipsis: true,
          width: 200,
          scopedSlots: { customRender: 'description' }
        },
        {
          title: '修改人',
          dataIndex: 'modifyUser',
          ellipsis: true,
          scopedSlots: { customRender: 'modifyUser' },
          width: 120
        },
        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: '170px'
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          customRender: (text) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',
          scopedSlots: { customRender: 'operation' },
          width: '220px'
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: '请输入工作空间名称', trigger: 'blur' }],
        description: [{ required: true, message: '请输入工作空间描述', trigger: 'blur' }]
      },
      configMenuVisible: false,
      replaceFields: { children: 'childs', title: 'title', key: 'id' },
      menusConfigData: {}
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getWorkSpaceList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },

    viewEnvVar(record) {
      this.temp = Object.assign({}, record)
      // this.envTemp = {
      //   workspaceId: this.temp.id,
      // };
      // this.envVarListQuery.workspaceId = record.id;
      this.envVarListVisible = true
      this.$nextTick(() => {
        this.$refs.workspaceEnv.loadDataEnvVar()
      })
    },
    handleAdd() {
      this.temp = {}
      this.editVisible = true
      this.$refs['editForm'] && this.$refs['editForm'].resetFields()
    },
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      this.editVisible = true
    },
    handleEditOk() {
      this.$refs['editForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        editWorkSpace(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg
            })
            this.$refs['editForm'].resetFields()
            this.editVisible = false
            this.loadData()
          }
        })
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的当前工作空间么,删除前需要将关联数据都删除后才能删除当前工作空间？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteWorkspace(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    configMeun(record) {
      this.temp = Object.assign({}, record)

      // 加载菜单配置信息
      // loadMenusConfig(id) {},
      getMenusConfig({
        workspaceId: record.id
      }).then((res) => {
        if (res.code !== 200) {
          return
        }
        this.menusConfigData = res.data

        this.menusConfigData.serverMenus = this.menusConfigData?.serverMenus.map((item) => {
          item.scopedSlots = { icon: 'custom' }
          item.childs?.map((item2) => {
            item2.id = item.id + ':' + item2.id
            return item2
          })
          return item
        })
        this.menusConfigData.nodeMenus = this.menusConfigData?.nodeMenus.map((item) => {
          item.scopedSlots = { icon: 'custom' }
          item.childs?.map((item2) => {
            item2.id = item.id + ':' + item2.id
            return item2
          })
          return item
        })
        if (!this.menusConfigData?.serverMenuKeys) {
          //
          const serverMenuKeys = []
          this.menusConfigData.serverMenus.forEach((item) => {
            serverMenuKeys.push(item.id)
            if (item.childs) {
              item.childs.forEach((item2) => {
                serverMenuKeys.push(item2.id)
              })
            }
          })
          this.menusConfigData = { ...this.menusConfigData, serverMenuKeys: serverMenuKeys }
        }

        if (!this.menusConfigData?.nodeMenuKeys) {
          //
          const nodeMenuKeys = []
          this.menusConfigData.nodeMenus.forEach((item) => {
            nodeMenuKeys.push(item.id)
            if (item.childs) {
              item.childs.forEach((item2) => {
                nodeMenuKeys.push(item2.id)
              })
            }
          })
          this.menusConfigData = { ...this.menusConfigData, nodeMenuKeys: nodeMenuKeys }
        }
        this.configMenuVisible = true
      })
    },
    onSubmitMenus() {
      saveMenusConfig({
        serverMenuKeys: this.menusConfigData.serverMenuKeys.join(','),
        nodeMenuKeys: this.menusConfigData.nodeMenuKeys.join(','),
        workspaceId: this.temp.id
      }).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg
          })
          this.configMenuVisible = false
        }
      })
    }
  }
}
</script>
<style scoped></style>
