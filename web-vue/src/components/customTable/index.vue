<template>
  <div>
    <a-table v-bind="$attrs" :columns="customColumn" :size="tableSize">
      <template v-if="props.isShowTools || $slots.title" #title="slotProps">
        <div class="table-title">
          <slot v-if="$slots.title" name="title" v-bind="slotProps"></slot>
          <div v-if="props.isShowTools" class="table-action__box">
            <!-- 增加工具栏部分 -->
            <a-space class="table-action">
              <a-tooltip v-if="!props.isHideRefresh" title="刷新">
                <ReloadOutlined class="table-action__icon" @click="refreshClick" />
              </a-tooltip>
              <a-popover title="列宽" trigger="click" placement="bottomRight">
                <template #content>
                  <a-radio-group v-model:value="tableSize" class="custom-size-list">
                    <div v-for="item in tableSizeList" :key="item.value">
                      <a-radio :value="item.value">{{ item.label }}</a-radio>
                    </div>
                  </a-radio-group>
                </template>
                <a-tooltip title="列宽">
                  <ColumnHeightOutlined class="table-action__icon" />
                </a-tooltip>
              </a-popover>
              <a-popover v-if="props.tableName" trigger="click" placement="bottomRight">
                <template #title>
                  <div class="custom-column-list__title">
                    <div>列设置</div>
                    <a-button type="link" size="small" @click="resetCustomColumn">重置</a-button>
                  </div>
                </template>
                <template #content>
                  <a-checkbox-group v-model:value="customColumnList" class="custom-column-list">
                    <div v-for="(item, index) in props.columns" :key="index" :span="24">
                      <a-checkbox :value="item.dataIndex">
                        {{ item.title }}
                      </a-checkbox>
                    </div>
                  </a-checkbox-group>
                </template>
                <a-tooltip title="列设置">
                  <SettingOutlined />
                </a-tooltip>
              </a-popover>
            </a-space>
          </div>
        </div>
      </template>
      <template v-for="key in otherSlots" #[key]="slotProps" :key="key">
        <slot :name="key" v-bind="slotProps"></slot>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts">
export default {
  name: 'CustomTable'
}
</script>

<script lang="ts" setup>
// 处理props
import { useUserStore } from '@/stores/user'
import { CheckboxValueType } from 'ant-design-vue/es/checkbox/interface'
import { SizeType } from 'ant-design-vue/es/config-provider'
import { ColumnProps, TableProps } from 'ant-design-vue/es/table'
import { defineProps } from 'vue'

const userStore = useUserStore()
const $slots = useSlots()
const otherSlots = computed(() => {
  return Object.keys($slots).filter((key) => key !== 'title')
})

const $attrs = useAttrs()
const props = withDefaults(
  defineProps<{
    columns: ColumnProps[]
    tableName?: string
    isShowTools?: boolean
    isHideRefresh?: boolean
  }>(),
  {
    columns: () => [],
    size: 'middle',
    tableName: '', // 表格名称必须唯一
    isShowTools: false, // 是否显示工具栏
    isHideRefresh: false // 是否隐藏刷新
  }
)

const emit = defineEmits(['refresh'])
const refreshClick = () => {
  emit('refresh', 666)
}

const COLUMN = 'column'
const SIZE = 'size'

const isCatchOPtions = () => {
  return props.tableName && userStore?.userInfo?.id
}
const getTableCatchKey = (type: string) => {
  return `table:catch__${userStore.userInfo.id}__${props.tableName}__${type}`
}
// 表格大小处理
const tableSizeList = [
  {
    value: 'large',
    label: '大'
  },
  {
    value: 'middle',
    label: '中'
  },
  {
    value: 'small',
    label: '小'
  }
]
const tableSize = ref<SizeType>('middle')
watch(
  () => tableSize.value,
  (val) => {
    if (!isCatchOPtions()) return
    // 判断是否需要存储
    if (val !== 'middle') {
      localStorage.setItem(getTableCatchKey(SIZE), JSON.stringify(val))
    } else {
      localStorage.removeItem(getTableCatchKey(SIZE))
    }
  }
)
// 组件加载 从存储中读取
onMounted(() => {
  // 判断是否需要存储
  const size = localStorage.getItem(getTableCatchKey(SIZE))
  tableSize.value = size ? JSON.parse(size) : props.size || 'middle'
})

let customColumnList = ref<CheckboxValueType[]>([])
const customColumn = computed(() => {
  if (!isCatchOPtions()) return props.columns
  return props.columns.filter((item) => {
    return customColumnList.value.includes(String(item?.dataIndex))
  })
})
const resetCustomColumn = () => {
  customColumnList.value = props.columns.map((item) => String(item.dataIndex))
}
// 监听列变化,同步至缓存customColumnList中
watch(
  () => props.columns,
  (val) => {
    if (!isCatchOPtions()) return
    const catchKeys = JSON.parse(localStorage.getItem(getTableCatchKey(COLUMN)) || '[]') as string[]
    if (
      catchKeys.filter((key) => val.findIndex((item) => item.dataIndex === key) > -1).length == 0 ||
      catchKeys.length == 0
    ) {
      customColumnList.value = val.map((item) => String(item.dataIndex))
    } else {
      customColumnList.value = catchKeys
    }
  },
  {
    immediate: true
  }
)
// 监听列展示变化,持久化存储
watch(
  () => customColumnList.value,
  (val) => {
    if (!isCatchOPtions()) return
    if (props.columns.filter((item) => !val.includes(String(item.dataIndex))).length > 0 && val.length > 0) {
      localStorage.setItem(getTableCatchKey(COLUMN), JSON.stringify(val))
    } else {
      localStorage.removeItem(getTableCatchKey(COLUMN))
    }
  },
  {
    immediate: true
  }
)
</script>
<style lang="less" scoped>
.table-title {
  position: relative;
}
.table-action {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  &__icon {
    font-size: 18px;
  }
}
.custom-column-list {
  display: block;
  &__title {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}
.custom-size-list {
  display: block;
}
</style>
