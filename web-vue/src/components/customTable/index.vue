<template>
  <div class="custom-table">
    <div v-if="props.isShowTools" class="custom-table__box">
      <!-- 增加工具栏部分 -->
      <a-space class="table-action">
        <a-space v-if="!props.isHideAutoRefresh">
          <span>自动刷新:</span>
          <a-switch
            v-model:checked="countdownSwitch"
            checked-children="开"
            un-checked-children="关"
            @change="countDownChange"
          />
          <a-statistic-countdown
            v-if="countdownSwitch"
            format="&nbsp; s 秒"
            title="刷新倒计时"
            :value="countdownNumber"
            @finish="countDownFinish"
          />
          |
        </a-space>
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
    <a-table v-bind="props" :columns="customColumn" :size="tableSize">
      <template v-if="slots.title" #title="slotProps">
        <slot name="title" v-bind="slotProps"></slot>
      </template>
      <template v-for="key in otherSlots" #[key]="slotProps" :key="key">
        <slot :name="key" v-bind="slotProps"></slot>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts">
import { useUserStore } from '@/stores/user'
import { CustomSlotsType } from 'ant-design-vue/es/_util/type'
import { SizeType } from 'ant-design-vue/es/config-provider'
import { ColumnType, tableProps } from 'ant-design-vue/es/table'
import { RenderExpandIconProps } from 'ant-design-vue/es/vc-table/interface'

import { initDefaultProps } from 'ant-design-vue/es/_util/props-util'
import { CheckboxValueType } from 'ant-design-vue/es/checkbox/interface'
// 表格大小处理
const tableSizeList = [
  {
    value: 'large',
    label: '超大'
  },
  {
    value: 'middle',
    label: '中等'
  },
  {
    value: 'small',
    label: '紧凑'
  }
]

export default defineComponent({
  // name: 'CustomTable',
  inheritAttrs: false,
  props: initDefaultProps(
    {
      ...tableProps(),
      columns: {
        type: Array<ColumnType>,
        default: () => []
      },
      /** 是否显示工具栏 */
      isShowTools: Boolean,
      /** 是否隐藏刷新按钮 */
      isHideRefresh: Boolean,
      /** tableName 全局唯一值，存储需要 * */
      tableName: {
        type: String,
        required: true
      },
      /** 是否隐藏自动刷新 */
      isHideAutoRefresh: Boolean,
      /** 默认自动刷新 */
      defaultAutoRefresh: {
        type: Boolean,
        default: false
      },
      /** 自动刷新时间 s 秒，不建议小于 10 秒 */
      autoRefreshTime: {
        type: Number,
        default: 10
      }
    },
    {
      defaultAutoRefresh: false,
      isHideAutoRefresh: false,
      isShowTools: false,
      isHideRefresh: false,
      autoRefreshTime: 10
    }
  ),
  slots: Object as CustomSlotsType<{
    emptyText?: any
    expandIcon?: RenderExpandIconProps<any>
    title?: any
    footer?: any
    summary?: any
    expandedRowRender?: any
    expandColumnTitle?: any
    bodyCell?: (props: {
      text: any
      value: any
      record: Record<string, any>
      index: number
      column: ColumnType
    }) => void
    headerCell?: (props: { title: any; column: ColumnType }) => void
    customFilterIcon?: any
    customFilterDropdown?: any
    default: any
  }>,
  emits: ['refresh'],
  setup(props, { attrs, slots, emit }) {
    // 倒计时
    const getCountdown = () => {
      return Date.now() + 1000 * props.autoRefreshTime
    }
    const countdownSwitch = ref(props.defaultAutoRefresh)
    const countdownNumber = ref(0)
    const countDownFinish = () => {
      emit('refresh')
      countdownNumber.value = getCountdown()
    }
    const countDownChange = () => {
      if (countdownSwitch.value) {
        countdownNumber.value = getCountdown()
      } else {
        countdownNumber.value = 0
      }
    }
    onMounted(() => {
      if (countdownSwitch.value && !props.isHideAutoRefresh) {
        countdownNumber.value = getCountdown()
      }
    })

    const otherSlots = computed(() => {
      return Object.keys(slots).filter((key) => key !== 'title')
    })
    const userStore = useUserStore()
    const COLUMN = 'column'
    const SIZE = 'size'
    // 是否缓存配置
    const isCatchOPtions = () => {
      return props.tableName && userStore?.userInfo?.id
    }
    // 获取缓存key
    const getTableCatchKey = (type: string) => {
      return `table:catch__${userStore.userInfo.id}__${props.tableName}__${type}`
    }
    const refreshClick = () => {
      emit('refresh')
    }
    // 表格列宽调整hooks
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
        return customColumnList.value.includes(String(item.dataIndex))
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
    return {
      countdownSwitch,
      countDownFinish,
      countdownNumber,
      countDownChange,
      attrs,
      props,
      slots,
      refreshClick,
      otherSlots,
      tableSizeList: tableSizeList,
      tableSize,
      customColumnList,
      customColumn,
      resetCustomColumn
    }
  }
})
</script>
<style lang="less" scoped>
.custom-table {
  position: relative;
  padding-top: 36px;
  &__box {
    padding: 0 10px;
    height: 36px;
    box-sizing: border-box;
    border-radius: 4px 4px 0 0px;
    position: absolute;
    right: 10px;
    top: 0;
    background: #fff;
    border: 1px solid rgb(240, 240, 240);
    border-bottom: 0px;
    display: flex;
    align-items: center;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }
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
