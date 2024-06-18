<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="systemUserLoginLog"
      :empty-description="$t('i18n_ede2c450d1')"
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
            :placeholder="$t('i18n_819767ada1')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%username%']"
            :placeholder="$t('i18n_9a56bb830e')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%ip%']"
            :placeholder="$t('i18n_b38d6077d6')"
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
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, text }">
        <template v-if="column.dataIndex === 'userAgent'">
          <a-tooltip :title="text">{{ text }} </a-tooltip>
        </template>

        <template v-if="column.dataIndex === 'success'">
          <a-tag v-if="text" color="green">{{ $t('i18n_330363dfc5') }}</a-tag>
          <a-tag v-else color="pink">{{ $t('i18n_acd5cb847a') }}</a-tag>
        </template>

        <template v-if="column.dataIndex === 'useMfa'">
          <a-tag>{{ text ? $t('i18n_ecff77a8d4') : $t('i18n_869ec83e33') }}</a-tag>
        </template>

        <template v-if="column.dataIndex === 'operateCode'">
          {{ operateCodeMap[text] || $t('i18n_1622dc9b6b') }}
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
  { title: $t('i18n_30acd20d6e'), dataIndex: 'modifyUser', width: 100 },
  { title: $t('i18n_9a56bb830e'), dataIndex: 'username', width: 120 },
  { title: 'IP', dataIndex: 'ip', width: 120 },

  {
    title: $t('i18n_5e9f2dedca'),
    dataIndex: 'success',
    width: 90,
    align: 'center'
  },
  {
    title: $t('i18n_ae0d608495'),
    dataIndex: 'useMfa',
    align: 'center',
    width: 110
  },
  {
    title: $t('i18n_64c083c0a9'),
    dataIndex: 'operateCode',
    ellipsis: true,
    width: 180
  },
  {
    title: $t('i18n_9fca7c455f'),
    dataIndex: 'createTimeMillis',
    sorter: true,
    customRender: ({ text, record }) => parseTime(text || record.optTime),
    width: '170px'
  },
  { title: $t('i18n_912302cb02'), dataIndex: 'userAgent', ellipsis: true, width: 100 }
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
