<template>
  <div>
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      table-name="build-pipeline-list"
      empty-description="没有任何流水线"
      :loading="loading"
      :active-page="activePage"
      size="middle"
      :columns="columns"
      :data-source="list"
      bordered
      row-key="id"
      :pagination="pagination"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            allow-clear
            class="search-input-item"
            placeholder="流水线名称"
            @press-enter="loadData"
          />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
        </a-space>
      </template>
      <!-- <template #cardBodyCell="{ item }"> {{ item }}</template> -->
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="`名称：${text} 点击查看详情`" @click="handleDetails(record)">
            <a-button type="link" style="padding: 0" size="small"> <FullscreenOutlined />{{ text }}</a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'"> </template>
      </template>
    </CustomTable>
    <a-drawer
      destroy-on-close
      :open="editVisible"
      :body-style="{
        // padding: '0'
      }"
      :header-style="{
        // padding: '0 10px'
      }"
      width="70vw"
      title="新增流水线"
      :mask-closable="false"
      :footer-style="{ textAlign: 'right' }"
      @close="editVisible = false"
    >
      <EditPipeline v-if="editVisible" ref="editPipelineCom"> </EditPipeline>
      <template #footer>
        <a-space>
          <a-button @click="editVisible = false"> 取消 </a-button>

          <a-divider type="vertical" />

          <a-button type="primary" @click="editPipelineCom.handleEditSave(false)"> 保存 </a-button>
          <!-- <a-button type="primary" @click="$refs.editBuild.handleEditBuildOk(true)"> 保存并构建 </a-button> -->
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>
<script setup lang="ts">
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime
  // PAGE_DEFAULT_SHOW_TOTAL,
  // getCachePageLimit
} from '@/utils/const'
import { buildPipelineList } from '@/api/build/pipeline'
import EditPipeline from './pipeline.vue'
import { CustomColumnType } from '@/components/customTable/types'

const loading = ref(true)
const editPipelineCom = ref()
const list = ref([])
const columns = ref<CustomColumnType[]>([
  {
    title: '名称',
    dataIndex: 'name',
    sorter: true,
    width: 200,
    ellipsis: true
  },
  {
    title: '分组',
    dataIndex: 'group',
    width: 100,
    ellipsis: true,
    tooltip: true
  },
  {
    title: '修改人',
    dataIndex: 'modifyUser',
    width: '130px',
    ellipsis: true,
    sorter: true
  },
  {
    title: '创建时间',
    dataIndex: 'createTimeMillis',
    sorter: true,
    ellipsis: true,
    customRender: ({ text }: any) => parseTime(text),
    width: '160px'
  },
  {
    title: '修改时间',
    dataIndex: 'modifyTimeMillis',
    sorter: true,
    customRender: ({ text }: any) => parseTime(text),
    width: '160px'
  },
  {
    title: '操作',
    dataIndex: 'operation',
    width: '200px',
    align: 'center',
    fixed: 'right'
  }
])
const listQuery = ref(Object.assign({}, PAGE_DEFAULT_LIST_QUERY))
const editVisible = ref(false)

const pagination = computed(() => {
  return COMPUTED_PAGINATION(listQuery.value)
})

onMounted(() => {
  loadData()
})

const loadData = (pointerEvent?: any) => {
  loading.value = true
  listQuery.value.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : listQuery.value.page

  loading.value = true
  buildPipelineList(listQuery.value)
    .then((res) => {
      if (res.code === 200) {
        list.value = res.data.result
        listQuery.value.total = res.data.total
      }
    })
    .finally(() => {
      loading.value = false
    })
}

const changePage = (pagination: any, filters: any, sorter: any) => {
  listQuery.value = CHANGE_PAGE(listQuery, {
    pagination,
    sorter
  })
  loadData()
}

const handleAdd = () => {
  editVisible.value = true
}

const route = useRoute()
const attrs = useAttrs()

const activePage = computed(() => {
  return attrs.routerUrl === route.path
})

//
const temp = ref({})
const handleDetails = (data: any) => {
  // console.log('handleDetails', data)
  temp.value = data
  editVisible.value = true
}
</script>
