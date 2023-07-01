<template>
  <div class="full-content">
    <div class="search-wrapper">
      <a-space>
        <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="工作空间名称" allowClear
          class="search-input-item" />

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
          <question-circle-filled />
        </a-tooltip>
      </a-space>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :columns="columns" size="middle" :pagination="pagination" bordered @change="changePage"
      rowKey="id">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip>{{ text }}</a-tooltip>
        </template>

        <template v-if="column.dataIndex === 'description'">
          <a-tooltip>{{ text }}</a-tooltip>
        </template>

        <template v-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
            <a-button size="small" type="primary" @click="configMeun(record)">菜单</a-button>
            <a-button size="small" type="primary" @click="viewEnvVar(record)">变量</a-button>
            <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 工作空间表单 -->
    <a-modal destroyOnClose v-model:visible="showWrokspaceForm"
      :title="`${workSpaceFormType === 'add' ? '新增' : '编辑'}工作空间`" @ok="handleEditOk" :maskClosable="false">
      <a-form ref="workspaceForm" :model="workspaceFormData" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item label="名称" name="name" :rules="[{ required: true, message: '请输入工作空间名称' }]">
          <a-input v-model:value="workspaceFormData.name" :maxLength="50" placeholder="工作空间名称" />
        </a-form-item>

        <a-form-item label="描述" name="description" :rules="[{ required: true, message: '请输入工作空间描述' }]">
          <a-input v-model:value="workspaceFormData.description" :maxLength="200" type="textarea" :rows="5"
            placeholder="工作空间描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 环境变量 -->
    <a-modal destroyOnClose v-model="envVarListVisible" :title="`${temp.name} 工作空间环境变量`" width="80vw" :footer="null"
      :maskClosable="false">
      <workspaceEnv ref="workspaceEnv" :workspaceId="temp.id" />
    </a-modal>
    <!-- 工作空间菜单 -->
    <a-modal destroyOnClose v-model="configMenuVisible" :title="`${temp.name} 工作空间菜单`" @ok="onSubmitMenus"
      :maskClosable="false">
      <a-form ref="editWhiteForm" :model="menusConfigData">
        <a-row type="flex" justify="center">
          <a-alert :message="`菜单配置只对非超级管理员生效`" style="margin-top: 10px; margin-bottom: 20px" banner />
          <a-col :span="12">
            <a-card title="服务端菜单" :bordered="false">
              <a-tree show-icon v-if="menusConfigData.serverMenus" checkable :tree-data="menusConfigData.serverMenus"
                :replaceFields="replaceFields" v-model="menusConfigData.serverMenuKeys">
                <a-icon #switcherIcon type="down" />

                <template #custom slot-scope="{ dataRef }">
                  <a-icon :type="dataRef.icon_v3" />
                </template>
              </a-tree>
            </a-card>
          </a-col>
          <a-col :span="12">
            <a-card title="节点菜单" :bordered="false">
              <a-tree show-icon v-if="menusConfigData.nodeMenus" checkable :tree-data="menusConfigData.nodeMenus"
                :replaceFields="replaceFields" v-model="menusConfigData.nodeMenuKeys">
                <a-icon #switcherIcon type="down" />

                <template #custom slot-scope="{ dataRef }">
                  <a-icon :type="dataRef.icon_v3" />
                </template>
              </a-tree>
            </a-card>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import { deleteWorkspace, editWorkSpace, getWorkSpaceList, getMenusConfig, saveMenusConfig } from '@/api/workspace'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import workspaceEnv from './workspace-env.vue'
import { ColumnsType } from 'ant-design-vue/es/table'
const loading = ref(false)
const list = ref([])
const listQuery = reactive(Object.assign({}, PAGE_DEFAULT_LIST_QUERY))
const envVarListVisible = ref(false)
const temp = reactive({})

const columns: ColumnsType = [
  { title: '名称', dataIndex: 'name', ellipsis: true, width: 200 },
  {
    title: '描述',
    dataIndex: 'description',
    ellipsis: true,
    width: 200,
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
    width: '240px'
  }
]
const configMenuVisible = ref(false)
const menusConfigData = reactive({})
const replaceFields = reactive({ children: 'childs', title: 'title', key: 'id' })


const pagination = computed(() => {
  return COMPUTED_PAGINATION(listQuery)
})


function loadData(pointerEvent?: PointerEvent) {
  loading.value = true
  listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : listQuery.page
  getWorkSpaceList(listQuery).then((res) => {
    if (res.code === 200) {
      list.value = res.data.result
      listQuery.total = res.data.total
    }
  }).finally(() => {
    loading.value = false
  })
}

// 新增\编辑工作空间
interface IWorkspaceFormData {
  id?: string,
  name: string
  description: string
}
const workspaceForm = ref()
const workSpaceFormType = ref<'add' | 'edit'>('add')
const showWrokspaceForm = ref(false)
const workspaceFormData = reactive<IWorkspaceFormData>({

  name: '',
  description: ''
})


function handleAdd() {
  showWrokspaceForm.value = true
  workSpaceFormType.value = 'add'
}

function handleEdit(record: IWorkspaceFormData) {
  showWrokspaceForm.value = true
  workSpaceFormType.value = 'edit'
  workspaceFormData.description = record.description
  workspaceFormData.name = record.name
  workspaceFormData.id = record.id
}


function handleEditOk() {
  workspaceForm.value?.validate().then(() => {
    editWorkSpace(workspaceFormData).then((res) => {
      if (res.code === 200) {
        // 成功
        $notification.success({
          message: res.msg
        })
        showWrokspaceForm.value = false
        workspaceForm.value?.resetFields()
        loadData()
      }
    })
  })
}

function viewEnvVar(record) {
  this.temp = Object.assign({}, record)
  // this.envTemp = {
  //   workspaceId: this.temp.id,
  // };
  // this.envVarListQuery.workspaceId = record.id;
  this.envVarListVisible = true
  nextTick(() => {
    this.$refs.workspaceEnv.loadDataEnvVar()
  })
}




// 分页、排序、筛选变化时触发
function changePage(pagination, filters, sorter) {
  this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
  this.loadData()
}
// 删除
function handleDelete(record) {
  $confirm({
    title: '系统提示',
    content: '真的当前工作空间么,删除前需要将关联数据都删除后才能删除当前工作空间？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      // 删除
      deleteWorkspace(record.id).then((res) => {
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
function configMeun(record) {
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
}
function onSubmitMenus() {
  saveMenusConfig({
    serverMenuKeys: this.menusConfigData.serverMenuKeys.join(','),
    nodeMenuKeys: this.menusConfigData.nodeMenuKeys.join(','),
    workspaceId: this.temp.id
  }).then((res) => {
    if (res.code === 200) {
      // 成功
      $notification.success({
        message: res.msg
      })
      this.configMenuVisible = false
    }
  })
}
onMounted(() => {
  loadData()
})

</script>
<style scoped></style>
