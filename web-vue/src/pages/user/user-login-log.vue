<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="systemUserLoginLog"
      :empty-description="$t('pages.user.user-login-log.61b2c18c')"
      :loading="loading"
      :data-source="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="change"
      @refresh="loadData"
    >
      <template #title>
        <a-space>
          <a-input
            v-model:value="listQuery['%modifyUser%']"
            :placeholder="$t('pages.user.user-login-log.c28c6dc1')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%username%']"
            :placeholder="$t('pages.user.user-login-log.9b5a95d2')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%ip%']"
            :placeholder="$t('pages.user.user-login-log.11294c8f')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-range-picker
            class="search-input-item"
            style="width: 220px"
            :show-time="{ format: 'HH:mm:ss' }"
            format="YYYY-MM-DD HH:mm:ss"
            @change="onChangeTime"
          />
          <a-tooltip :title="$t('pages.user.user-login-log.1bfed54a')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.user.user-login-log.53c2763c')
            }}</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, text }">
        <template v-if="column.dataIndex === 'userAgent'">
          <a-tooltip :title="text">{{ text }} </a-tooltip>
        </template>

        <template v-if="column.dataIndex === 'success'">
          <a-tag v-if="text" color="green">{{ $t('pages.user.user-login-log.83aa7d3') }}</a-tag>
          <a-tag v-else color="pink">{{ $t('pages.user.user-login-log.46a17ba0') }}</a-tag>
        </template>

        <template v-if="column.dataIndex === 'useMfa'">
          <a-tag>{{
            text ? $t('pages.user.user-login-log.9c128103') : $t('pages.user.user-login-log.731ee562')
          }}</a-tag>
        </template>

        <template v-if="column.dataIndex === 'operateCode'">
          {{ operateCodeMap[text] || $t('pages.user.user-login-log.ca1cdfa6') }}
        </template>
      </template>
    </CustomTable>
  </div>
</template>
<script lang="ts" setup>
import { userLoginLgin, operateCodeMap } from '@/api/user/user-login-log'
import { IPageQuery } from '@/interface/common'
import { CustomColumnType } from '@/components/customTable/types'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()

const loading = ref(false)
const list = ref([])
// const operateCode = operateCodeMap;
const listQuery = ref<IPageQuery>({ ...PAGE_DEFAULT_LIST_QUERY })

const route = useRoute()
const attrs = useAttrs()
const activePage = computed(() => {
  return attrs.routerUrl === route.path
})

const columns = ref<CustomColumnType[]>([
  { title: $t('pages.user.user-login-log.8384e057'), dataIndex: 'modifyUser', width: 100 },
  { title: $t('pages.user.user-login-log.9b5a95d2'), dataIndex: 'username', width: 120 },
  { title: 'IP', dataIndex: 'ip', width: 120 },

  {
    title: $t('pages.user.user-login-log.2bff48a0'),
    dataIndex: 'success',
    width: 90,
    align: 'center'
  },
  {
    title: $t('pages.user.user-login-log.b13cc4c9'),
    dataIndex: 'useMfa',
    align: 'center',
    width: 110
  },
  {
    title: $t('pages.user.user-login-log.5f2a060b'),
    dataIndex: 'operateCode',
    ellipsis: true,
    width: 180
  },
  {
    title: $t('pages.user.user-login-log.dea71d69'),
    dataIndex: 'createTimeMillis',
    sorter: true,
    customRender: ({ text, record }) => parseTime(text || record.optTime),
    width: '170px'
  },
  { title: $t('pages.user.user-login-log.bd29775b'), dataIndex: 'userAgent', ellipsis: true, width: 100 }
])

const pagination = computed(() => {
  return COMPUTED_PAGINATION(listQuery.value)
})

const loadData = (pointerEvent?: any) => {
  loading.value = true
  listQuery.value.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : listQuery.value.page
  userLoginLgin(listQuery.value)
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
