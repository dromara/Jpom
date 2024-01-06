<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      size="middle"
      :data-source="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      rowKey="id"
      @change="change"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template #title>
        <a-space>
          <a-input
            v-model="listQuery['%username%']"
            @pressEnter="loadData"
            placeholder="用户名"
            class="search-input-item"
          />
          <a-input v-model="listQuery['%ip%']" @pressEnter="loadData" placeholder="登录IP" class="search-input-item" />
          <a-range-picker
            class="search-input-item"
            style="width: 220px"
            :show-time="{ format: 'HH:mm:ss' }"
            format="YYYY-MM-DD HH:mm:ss"
            @change="onChangeTime"
          />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'userAgent'">
          <a-tooltip :title="text">{{ text }} </a-tooltip>
        </template>

        <template v-if="column.dataIndex === 'success'">
          <a-tag v-if="text" color="green">成功</a-tag>
          <a-tag v-else color="pink">失败</a-tag>
        </template>

        <template v-if="column.dataIndex === 'useMfa'">
          <a-tag>{{ text ? '使用' : '未使用' }}</a-tag>
        </template>

        <template v-if="column.dataIndex === 'operateCode'">
          {{ operateCodeMap[text] || '未知' }}
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { userLoginLgin, operateCodeMap } from '@/api/user/user-login-log'
import { IPageQuery } from '@/interface/common'

import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

const loading = ref(false)
const list = ref([])
// const operateCode = operateCodeMap;
const listQuery = ref<IPageQuery>({ ...PAGE_DEFAULT_LIST_QUERY })

const columns = [
  { title: '用户ID', dataIndex: 'modifyUser', width: 100 },
  { title: '用户名称', dataIndex: 'username', width: 120 },
  { title: 'IP', dataIndex: 'ip', width: 120 },

  {
    title: '是否成功',
    dataIndex: 'success',
    width: 90,
    align: 'center'
  },
  {
    title: '是否使用MFA',
    dataIndex: 'useMfa',
    align: 'center',
    width: 110
  },
  {
    title: '结果描述',
    dataIndex: 'operateCode',
    ellipsis: true,
    width: 180
  },
  {
    title: '登录时间',
    dataIndex: 'createTimeMillis',
    sorter: true,
    customRender: ({ text, record }) => parseTime(text || record.optTime),
    width: 160
  },
  { title: '浏览器', dataIndex: 'userAgent', ellipsis: true, width: 100 }
]

const pagination = COMPUTED_PAGINATION(listQuery.value)

const loadData = (pointerEvent?: any) => {
  loading.value = true
  listQuery.value.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : listQuery.value.page
  userLoginLgin(listQuery.value).then((res) => {
    if (res.code === 200) {
      list.value = res.data.result
      listQuery.value.total = res.data.total
    }
    loading.value = false
  })
}

const change = (pagination: any, filters: any, sorter: any) => {
  listQuery.value = CHANGE_PAGE(listQuery.value, { pagination, sorter })
  loadData()
}

const onChangeTime = (value, dateString) => {
  listQuery.value.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
}

onMounted(() => {
  loadData()
})
</script>
<style scoped></style>
