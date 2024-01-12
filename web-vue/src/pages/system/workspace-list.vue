<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      :columns="columns"
      size="middle"
      :pagination="pagination"
      bordered
      @change="changePage"
      rowKey="id"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template #title>
        <a-space>
          <a-input
            v-model:value="listQuery['id']"
            @pressEnter="loadData"
            placeholder="空间ID(全匹配)"
            allowClear
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['%name%']"
            @pressEnter="loadData"
            placeholder="工作空间名称"
            allowClear
            class="search-input-item"
          />
          <a-select
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
            v-model:value="listQuery.group"
            allowClear
            placeholder="分组"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-select
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
            v-model:value="listQuery.clusterInfoId"
            allowClear
            placeholder="集群"
            class="search-input-item"
          >
            <a-select-option v-for="item in clusterList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
          <a-tooltip>
            <template #title>
              <ul>
                <li>工作空间用于隔离数据,工作空间下面可以有不同数据,不同权限,不同菜单等来实现权限控制</li>
                <li>工作空间环境变量用于构建命令相关</li>
              </ul>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'description'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'clusterInfoId'">
          <a-tooltip
            placement="topLeft"
            :title="
              (clusterList.find((item) => {
                return item.id === text
              }) &&
                clusterList.find((item) => {
                  return item.id === text
                }).name) ||
              ''
            "
          >
            <span>{{
              clusterList.find((item) => {
                return item.id === text
              }) &&
              clusterList.find((item) => {
                return item.id === text
              }).name
            }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
            <a-button size="small" type="primary" @click="configMeun(record)">菜单</a-button>
            <a-button size="small" type="primary" @click="configWhiteDir(record)">授权配置</a-button>
            <a-button size="small" type="primary" @click="viewEnvVar(record)">变量</a-button>

            <a-tooltip v-if="record.id === 'DEFAULT'" title="不能删除默认工作空间">
              <a-button size="small" type="primary" danger :disabled="true">删除</a-button>
            </a-tooltip>
            <a-button v-else size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="editVisible"
      title="编辑工作空间"
      @ok="handleEditOk"
      :maskClosable="false"
    >
      <a-alert message="温馨提醒" type="info" show-icon>
        <template #description>
          <ul>
            <li>创建工作空间后还需要在对应工作空间中分别管理对应数据</li>
            <li>如果要将工作空间分配给其他用户还需要到权限组管理</li>
            <li>工作空间的菜单、环境变量、节点分发授权需要逐一配置</li>
          </ul>
        </template>
      </a-alert>
      <a-form
        ref="editForm"
        :rules="rules"
        :model="temp"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 14 }"
        style="padding-top: 15px"
      >
        <a-form-item label="名称" name="name">
          <a-input v-model:value="temp.name" :maxLength="50" placeholder="工作空间名称" />
        </a-form-item>
        <a-form-item label="绑定集群" name="clusterInfoId">
          <a-select
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
            v-model:value="temp.clusterInfoId"
            allowClear
            placeholder="绑定集群"
          >
            <a-select-option v-for="item in clusterList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="分组" name="group">
          <custom-select
            v-model:value="temp.group"
            :data="groupList"
            inputPlaceholder="新增分组"
            selectPlaceholder="选择分组名"
          >
          </custom-select>
        </a-form-item>

        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="temp.description" :maxLength="200" :rows="5" placeholder="工作空间描述" />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 环境变量 -->
    <a-modal
      destroyOnClose
      v-model:open="envVarListVisible"
      :title="`${temp.name} 工作空间环境变量`"
      width="80vw"
      :footer="null"
      :maskClosable="false"
    >
      <workspaceEnv v-if="envVarListVisible" ref="workspaceEnv" :workspaceId="temp.id" />
    </a-modal>
    <!-- 工作空间菜单 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="configMenuVisible"
      :title="`${temp.name} 工作空间菜单`"
      @ok="onSubmitMenus"
      :maskClosable="false"
    >
      <a-form ref="editWhiteForm" :model="menusConfigData">
        <a-row type="flex" justify="center">
          <a-alert
            :message="`菜单配置只对非超级管理员生效`"
            style="margin-top: 10px; margin-bottom: 20px; width: 100%"
            banner
          />
          <a-col :span="20">
            <a-card title="服务端菜单" :bordered="true">
              <a-tree
                show-icon
                v-if="menusConfigData.serverMenus"
                checkable
                :tree-data="menusConfigData.serverMenus"
                :fieldNames="replaceFields"
                v-model:checkedKeys="menusConfigData.serverMenuKeys"
              >
                <template #icon="{ dataRef }">
                  <icon :type="dataRef.icon_v3" />
                </template>
              </a-tree>
            </a-card>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
    <!-- 配置授权目录 -->
    <a-modal
      destroyOnClose
      v-model:open="configDir"
      :title="`配置授权目录`"
      :footer="null"
      :maskClosable="false"
      @cancel="
        () => {
          this.configDir = false
        }
      "
    >
      <whiteList
        v-if="configDir"
        :workspaceId="temp.id"
        @cancel="
          () => {
            this.configDir = false
          }
        "
      ></whiteList>
    </a-modal>
    <!-- 删除工作空间检查 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="preDeleteVisible"
      :title="`删除工作空间确认`"
      :maskClosable="false"
      @ok="handleDeleteOk"
      @cancel="
        () => {
          this.preDeleteVisible = false
        }
      "
    >
      <a-alert message="操作提示" type="error" show-icon>
        <template #description> 真的当前工作空间么,删除前需要将关联数据都删除后才能删除当前工作空间？</template>
      </a-alert>

      <a-tree :tree-data="treeData" default-expand-all :fieldNames="preDeleteReplaceFields" :show-line="true">
        <template #title="{ dataRef }">
          <CheckOutlined v-if="dataRef.count === 0" style="color: green" />

          <CloseOutlined v-else style="color: red" />
          {{ dataRef.name }}

          <template v-if="dataRef.count > 0">
            <a-tag color="pink"> 存在 {{ dataRef.count }} 条数据 </a-tag>

            <a-tag v-if="dataRef.workspaceBind === 2" color="cyan">自动删除</a-tag>
            <a-tag v-else-if="dataRef.workspaceBind === 3" color="blue">父级不存在自动删除</a-tag>
            <a-tag v-else color="purple">手动删除</a-tag>
          </template>
        </template>
      </a-tree>
    </a-modal>
  </div>
</template>

<script>
import {
  deleteWorkspace,
  preDeleteWorkspace,
  editWorkSpace,
  getWorkSpaceList,
  getMenusConfig,
  saveMenusConfig,
  getWorkSpaceGroupList
} from '@/api/workspace'
import Icon from '@/components/Icon'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { listClusterAll } from '@/api/system/cluster'
import workspaceEnv from './workspace-env.vue'
import CustomSelect from '@/components/customSelect'
import whiteList from '@/pages/dispatch/white-list.vue'
export default {
  components: {
    workspaceEnv,
    CustomSelect,
    whiteList,
    Icon,
    VNodes: {
      props: {
        vnodes: {
          type: Object,
          required: true
        }
      },
      render() {
        return this.vnodes
      }
    }
  },
  data() {
    return {
      loading: true,
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editVisible: false,
      envVarListVisible: false,
      temp: {},
      columns: [
        {
          title: '名称',
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: '描述',
          dataIndex: 'description',
          ellipsis: true,
          width: 200
        },
        {
          title: '分组名',
          dataIndex: 'group',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: '集群',
          dataIndex: 'clusterInfoId',
          ellipsis: true,
          width: '100px'
        },
        {
          title: '修改人',
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: 120
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
          customRender: ({ text }) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',

          width: '320px'
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: '请输入工作空间名称', trigger: 'blur' }],
        description: [{ required: true, message: '请输入工作空间描述', trigger: 'blur' }],
        clusterInfoId: [{ required: true, message: '请输入选择绑定的集群', trigger: 'blur' }]
      },
      configMenuVisible: false,
      replaceFields: { children: 'childs', title: 'title', key: 'id' },
      menusConfigData: {},
      groupList: [],
      configDir: false,
      preDeleteVisible: false,
      preDeleteReplaceFields: {
        children: 'children',
        title: 'name',
        key: 'id'
      },
      treeData: [],
      clusterList: [],
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
    this.loadClusterList()
  },
  methods: {
    // 获取所有集群
    loadClusterList() {
      return new Promise((resolve) => {
        listClusterAll().then((res) => {
          if (res.data && res.code === 200) {
            this.clusterList = res.data || []
            resolve()
          }
        })
      })
    },
    // 获取所有的分组
    loadGroupList() {
      getWorkSpaceGroupList().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getWorkSpaceList(this.listQuery)
        .then((res) => {
          if (res.code === 200) {
            this.list = res.data.result
            this.listQuery.total = res.data.total
          }
        })
        .finally(() => {
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
      this.loadGroupList()
      this.temp = {}
      this.$refs['editForm'] && this.$refs['editForm'].resetFields()
      this.loadClusterList().then(() => {
        if (this.clusterList.length === 1) {
          this.temp = { ...this.temp, clusterInfoId: this.clusterList[0].id }
        }
        this.editVisible = true
      })
    },
    handleEdit(record) {
      this.loadGroupList()
      this.$refs['editForm'] && this.$refs['editForm'].resetFields()
      this.loadClusterList().then(() => {
        const defData = {}
        if (this.clusterList.length === 1) {
          defData.clusterInfoId = this.clusterList[0].id
        }
        this.temp = Object.assign({}, record, defData)
        this.editVisible = true
      })
    },
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        editWorkSpace(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })

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
      this.temp = { ...record }

      preDeleteWorkspace(this.temp.id).then((res) => {
        this.treeData = res.data?.children || []
        this.preDeleteVisible = true
      })
    },
    handleDeleteOk() {
      // 删除
      this.confirmLoading = true
      deleteWorkspace(this.temp.id)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.preDeleteVisible = false
            this.loadData()
          }
        })
        .finally(() => {
          this.confirmLoading = false
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
          // item.scopedSlots = { icon: 'custom' }
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
          this.menusConfigData = {
            ...this.menusConfigData,
            serverMenuKeys: serverMenuKeys
          }
        }

        this.configMenuVisible = true
      })
    },
    onSubmitMenus() {
      this.confirmLoading = true
      saveMenusConfig({
        serverMenuKeys: this.menusConfigData.serverMenuKeys.join(','),

        workspaceId: this.temp.id
      })
        .then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })
            this.configMenuVisible = false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 配置节点授权
    configWhiteDir(record) {
      this.temp = Object.assign({}, record)
      this.configDir = true
    }
  }
}
</script>
