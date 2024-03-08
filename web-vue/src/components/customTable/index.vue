<template>
  <div class="custom-table">
    <div v-if="props.isShowTools" class="custom-table__box">
      <!-- 增加工具栏部分 -->
      <!-- <a-space class="table-action"> -->
      <a-card size="small">
        <a-form layout="inline">
          <a-space>
            <template #split>
              <a-divider type="vertical" />
            </template>
            <template v-if="slots.toolPrefix">
              <slot name="toolPrefix"></slot>
            </template>
            <a-form-item label="自动刷新">
              <a-space v-if="!props.isHideAutoRefresh">
                <a-switch
                  v-model:checked="countdownSwitch"
                  checked-children="开"
                  un-checked-children="关"
                  @change="countDownChange"
                />
                <a-divider v-if="countdownSwitch" type="vertical" />
                <a-statistic-countdown
                  v-if="countdownSwitch"
                  format="&nbsp; s 秒"
                  title="刷新倒计时"
                  :value="countdownNumber"
                  @finish="countDownFinish"
                />
              </a-space>
            </a-form-item>
            <a-form-item>
              <a-tooltip v-if="!props.isHideRefresh" title="刷新">
                <ReloadOutlined class="table-action__icon" @click="refreshClick" />
              </a-tooltip>
            </a-form-item>

            <a-form-item>
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
            </a-form-item>

            <a-form-item>
              <a-popover v-if="props.tableName" trigger="click" placement="bottomRight">
                <template #title>
                  <div class="custom-column-list__title">
                    <div>列设置</div>
                    <a-button type="link" size="small" @click="resetCustomColumn">重置</a-button>
                  </div>
                </template>
                <template #content>
                  <a-checkbox-group class="custom-column-list" :value="customCheckColumnList" @change="onCheckChange">
                    <Container drag-handle-selector=".custom-column-list__icon" orientation="vertical" @drop="onDrop">
                      <Draggable v-for="(item, index) in customColumnList" :key="index">
                        <HolderOutlined class="custom-column-list__icon" />
                        <a-checkbox :value="item.dataIndex">
                          {{ item.title }}
                        </a-checkbox>
                      </Draggable>
                    </Container>
                  </a-checkbox-group>
                </template>
                <a-tooltip title="列设置">
                  <SettingOutlined />
                </a-tooltip>
              </a-popover>
            </a-form-item>

            <template v-if="slots.tableHelp">
              <a-form-item>
                <slot name="tableHelp"></slot>
              </a-form-item>
            </template>
          </a-space>
        </a-form>
      </a-card>

      <!-- </a-space> -->
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
import { SizeType } from 'ant-design-vue/es/config-provider'
import { Container, Draggable } from 'vue3-smooth-dnd'
import { CheckboxValueType } from 'ant-design-vue/es/checkbox/interface'
import { CatchStorageType, CustomColumnType, CustomTableSlotsType } from './types'
import { compareArrays } from './utils'
import { tableSizeList } from './dict'
import { customTableProps } from './props'

export default defineComponent({
  name: 'CustomTable',
  components: {
    Container,
    Draggable
  },
  inheritAttrs: false,
  props: customTableProps,
  slots: Object as CustomTableSlotsType,
  emits: ['refresh'],
  setup(props, { attrs, slots, emit }) {
    // 倒计时
    const getCountdown = () => {
      return Date.now() + 1000 * (props.autoRefreshTime + 0)
    }
    const countdownSwitch = ref(props.defaultAutoRefresh)
    const countdownNumber = ref(0)
    const countDownFinish = () => {
      emit('refresh', 'silence')

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
    /** 是否缓存配置 */
    const isCatchOPtions = () => {
      return props.tableName && userStore?.userInfo?.id
    }
    /** 获取缓存key */
    const getTableCatchKey = (type: string) => {
      return `table:catch__${userStore.userInfo.id}__${props.tableName}__${type}`
    }
    /** 获取缓存key */
    const refreshClick = () => {
      countdownNumber.value = getCountdown()
      emit('refresh', 'click')
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

    let customColumnList = ref<CustomColumnType[]>([])
    const customCheckColumnList = computed(() => {
      return customColumnList.value.filter((item) => item.checked).map((item) => String(item.dataIndex))
    })
    const customColumn = computed(() => {
      if (!isCatchOPtions()) return props.columns
      return customColumnList.value.filter((item) => item.checked)
    })
    const resetCustomColumn = () => {
      customColumnList.value = props.columns.map((item) => ({ ...item, checked: true }))
    }
    const onCheckChange = (checkedValues: CheckboxValueType[]) => {
      customColumnList.value = customColumnList.value.map((item) => ({
        ...item,
        checked: checkedValues.includes(String(item.dataIndex))
      }))
    }
    const applyDrag = (
      arr: CustomColumnType[],
      dragResult: { removedIndex: number; addedIndex: number; payload: CustomColumnType }
    ) => {
      const { removedIndex, addedIndex, payload } = dragResult

      if (removedIndex === null && addedIndex === null) return arr
      const result = [...arr]
      let itemToAdd = payload

      if (removedIndex !== null) {
        itemToAdd = result.splice(removedIndex, 1)[0]
      }
      if (addedIndex !== null) {
        result.splice(addedIndex, 0, itemToAdd)
      }
      return result
    }

    const onDrop = (dropResult: any) => {
      customColumnList.value = applyDrag(customColumnList.value, dropResult)
    }

    /** 设置默认列 */
    const setDefaultCustomColumnList = () => {
      customColumnList.value = props.columns.map((item) => ({ ...item, checked: true }))
    }

    // 监听列变化,同步至缓存customColumnList中
    watch(
      () => props.columns,
      (val) => {
        if (!isCatchOPtions()) return
        const catchStorage = JSON.parse(localStorage.getItem(getTableCatchKey(COLUMN)) || '[]') as CatchStorageType[]
        if (
          catchStorage.length == 0 ||
          (catchStorage.length > 0 && catchStorage.some((key) => typeof key === 'string')) ||
          !compareArrays(
            val.map((item) => String(item.dataIndex)),
            catchStorage.map((item) => item.key)
          )
        ) {
          console.log(
            catchStorage.length == 0,
            catchStorage.length > 0 && catchStorage.some((key) => typeof key === 'string'),
            !compareArrays(
              val.map((item) => String(item.key)),
              catchStorage.map((item) => item.key)
            )
          )
          console.log('setDefaultCustomColumnList')
          return setDefaultCustomColumnList()
        } else {
          const tmpObj: { [key: string]: CustomColumnType } = {}
          val.forEach((item) => {
            tmpObj[String(item.dataIndex || '_d')] = item
          })
          console.log('catchStorage')
          customColumnList.value = catchStorage.map((item) => {
            const key = item.key
            console.log(item)
            return {
              ...tmpObj[key],
              checked: item.checked
            }
          })
          console.log(customColumnList.value)
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
        if (JSON.stringify(val) !== JSON.stringify(props.columns)) {
          localStorage.setItem(
            getTableCatchKey(COLUMN),
            JSON.stringify(
              customColumnList.value.map((item) => {
                return {
                  key: item.dataIndex,
                  checked: item.checked
                } as CatchStorageType
              })
            )
          )
        } else {
          localStorage.removeItem(getTableCatchKey(COLUMN))
        }
      },
      {
        immediate: true
      }
    )
    return {
      onDrop,
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
      customCheckColumnList,
      resetCustomColumn,
      onCheckChange
    }
  }
})
</script>
<style lang="less" scoped>
.custom-table {
  position: relative;
  padding-top: 36px;
  &__box {
    padding: 0 px;
    height: 36px;
    box-sizing: border-box;
    border-radius: 4px 4px 0 0px;
    position: absolute;
    right: 0px;
    top: 0;
    //background: #fff;
    // border: 1px solid rgb(240, 240, 240);
    border-bottom: 0px;
    display: flex;
    align-items: center;
    // box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }
}

.table-action {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  &__icon {
    // font-size: 18px;
  }
}
.custom-column-list {
  display: block;
  &__title {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  &__icon {
    // color: rgba(0, 0, 0, 0.6);
    margin-right: 10px;
  }
}
.custom-size-list {
  display: block;
}
</style>
<style scoped>
:deep(.ant-form-item) {
  margin-inline-end: 0;
}
:deep(.ant-statistic) {
  line-height: 0;
}
:deep(.ant-statistic div) {
  display: inline-block;
  font-weight: normal;
  color: unset;
}
:deep(.ant-statistic-content-value, .ant-statistic-content, .ant-statistic-title) {
  font-size: 16px;
  color: unset;
}
</style>
