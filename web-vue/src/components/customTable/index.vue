<template>
  <div class="custom-table">
    <!-- class="custom-table__box" -->
    <div v-if="props.isShowTools">
      <!-- 增加工具栏部分 -->
      <a-card size="small" :body-style="{ padding: '1px 0px 0px 0px' }">
        <template #title>
          <a-row justify="end">
            <a-form layout="inline">
              <a-space>
                <template #split>
                  <a-divider type="vertical" />
                </template>
                <template v-if="slots.toolPrefix">
                  <slot name="toolPrefix"></slot>
                </template>
                <a-form-item>
                  <template #label>
                    <span style="font-weight: normal">自动刷新</span>
                  </template>
                  <a-space v-if="!props.isHideAutoRefresh">
                    <a-switch
                      v-model:checked="countdownSwitch"
                      checked-children="开"
                      un-checked-children="关"
                      @change="countDownChange"
                    />
                    <a-divider v-if="countdownSwitch" type="vertical" />
                    <div class="header-statistic">
                      <a-statistic-countdown
                        v-if="countdownSwitch"
                        format="&nbsp; s 秒"
                        title="刷新倒计时"
                        :value="countdownNumber"
                        @finish="countDownFinish"
                      />
                    </div>
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
                      <a-checkbox-group
                        class="custom-column-list"
                        :value="customCheckColumnList"
                        @change="onCheckChange"
                      >
                        <Container
                          drag-handle-selector=".custom-column-list__icon"
                          non-drag-area-selector=".not-draggable"
                          orientation="vertical"
                          @drop="onDrop"
                        >
                          <Draggable
                            v-for="(item, index) in customColumnList"
                            :key="index"
                            :class="!!item.fixed ? 'not-draggable' : ''"
                          >
                            <VerticalLeftOutlined v-if="!!item.fixed" class="custom-column-list__icon" />
                            <HolderOutlined v-else class="custom-column-list__icon" />
                            <a-checkbox :value="item.dataIndex" :disabled="!!item.fixed">
                              {{ item.title }}
                            </a-checkbox>
                            <a-divider style="margin: 2px 0" />
                          </Draggable>
                        </Container>
                      </a-checkbox-group>
                    </template>
                    <a-tooltip title="列设置">
                      <SettingOutlined />
                    </a-tooltip>
                  </a-popover>
                </a-form-item>

                <a-form-item v-if="canChangeLayout">
                  <a-tooltip title="切换视图">
                    <!-- <ReloadOutlined   /> -->
                    <TableOutlined v-if="tableLayout === 'card'" class="table-action__icon" @click="tableLayoutClick" />
                    <LayoutOutlined v-else class="table-action__icon" @click="tableLayoutClick" />
                  </a-tooltip>
                </a-form-item>

                <template v-if="slots.tableHelp">
                  <a-form-item>
                    <slot name="tableHelp"></slot>
                  </a-form-item>
                </template>
              </a-space>
            </a-form>
          </a-row>
        </template>

        <a-card :body-style="{ padding: '10px' }" :bordered="false">
          <template #title="slotProps">
            <template v-if="slots.title">
              <slot name="title" v-bind="slotProps"></slot>
            </template>
          </template>
          <template v-if="tableLayout === 'table'">
            <a-table v-bind="props" :pagination="paginationByLayout" :columns="customColumn" :size="tableSize">
              <template v-if="slots.tableBodyCell" #bodyCell="slotProps">
                <slot name="tableBodyCell" v-bind="slotProps"></slot>
              </template>
            </a-table>
          </template>
          <template v-else-if="tableLayout === 'card'">
            <a-space direction="vertical" style="width: 100%">
              <a-row :gutter="[16, 16]">
                <template v-if="props.dataSource && props.dataSource.length">
                  <a-col v-for="(item, index) in props.dataSource" :key="item.id" :span="6">
                    <slot name="cardBodyCell" :item="item" :index="index"></slot>
                  </a-col>
                </template>
                <a-col v-else :span="24">
                  <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" :description="props.emptyDescription" />
                </a-col>
              </a-row>
              <div class="card-pagination">
                <a-pagination v-bind="paginationByLayout" size="small" @change="paginationChange" />
              </div>
            </a-space>
            <!-- <slot name="cardPageTool"></slot> -->
          </template>
          <template v-else>未知的表格类型</template>
        </a-card>
      </a-card>
    </div>
  </div>
</template>
<script lang="ts">
import { useUserStore } from '@/stores/user'
import { Empty } from 'ant-design-vue'
import { SizeType } from 'ant-design-vue/es/config-provider'
import { Container, Draggable } from 'vue3-smooth-dnd'
import { CheckboxValueType } from 'ant-design-vue/es/checkbox/interface'
import { CatchStorageType, CustomColumnType, CustomTableSlotsType, TableLayoutType } from './types'
import { compareArrays } from './utils'
import { tableSizeList } from './dict'
import { customTableProps } from './props'
import { StorageService } from './utils/StorageService'
import { dropApplyDrag } from '@/utils/const'

export default defineComponent({
  name: 'CustomTable',
  components: {
    Container,
    Draggable
  },
  inheritAttrs: false,
  props: customTableProps,
  slots: Object as CustomTableSlotsType,
  emits: ['refresh', 'change', 'changeTableLayout'],
  setup(props, { attrs, slots, emit }) {
    const userStore = useUserStore()
    const storageService: StorageService = new StorageService(props.tableName, {
      provide: 'localStorage',
      prefix: 'table:catch__' + userStore?.userInfo?.id,
      // 存储前拦截器
      beforeStorage(storageObject, defaultConfig) {
        // 判断刷新是否为默认
        const defaultAutoRefresh: number = props.defaultAutoRefresh ? 1 : 0
        if (defaultAutoRefresh === storageObject?.refresh?.isAutoRefresh) {
          storageObject.refresh = defaultConfig.refresh
        }
        // 判断表格大小是否为默认
        if (storageObject?.tableSize === props?.size) {
          storageObject.tableSize = defaultConfig?.tableSize
        }
        // layout
        if (storageObject?.layout === props?.layout) {
          storageObject.layout = defaultConfig?.layout
        }
        if (
          storageObject?.column?.length === 0 ||
          JSON.stringify(storageObject?.column?.filter((item) => item.checked)?.map((item) => item.key) || []) ===
            JSON.stringify(props.columns.map((item) => item.dataIndex))
        ) {
          storageObject.column = defaultConfig.column
        }
        return storageObject
      }
    })

    const { autoRefreshTime, isAutoRefresh } = storageService.getRefreshConfig()
    // 倒计时
    const getCountdown = () => {
      return Date.now() + 1000 * (autoRefreshTime !== -1 ? autoRefreshTime : props.autoRefreshTime + 0)
    }
    const countdownSwitch = ref(isAutoRefresh !== -1 ? isAutoRefresh : props.defaultAutoRefresh)
    const countdownNumber = ref(0)
    const countDownFinish = () => {
      if (props.activePage) {
        // 仅当页面处于活跃才刷新
        emit('refresh', 'silence')
      }
      countdownNumber.value = getCountdown()
    }
    const countDownChange = () => {
      if (countdownSwitch.value) {
        countdownNumber.value = getCountdown()
      } else {
        countdownNumber.value = 0
      }
      storageService.setRefreshConfig({
        isAutoRefresh: countdownSwitch.value ? 1 : 0,
        autoRefreshTime: props.autoRefreshTime
      })
    }
    onMounted(() => {
      if (countdownSwitch.value && !props.isHideAutoRefresh) {
        countdownNumber.value = getCountdown()
      }
    })

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
        if (!storageService.exitOpenStorage()) return
        storageService.setTableSizeConfig(val)
      }
    )
    // 组件加载 从存储中读取
    onMounted(() => {
      // 判断是否需要存储
      const size = storageService.getTableSizeConfig()
      tableSize.value = size ? size : props.size || 'middle'
    })

    // 视图模式
    const tableLayout = ref<TableLayoutType>('table')
    const canChangeLayout = computed(() => {
      if (props.layout) {
        return false
      }
      return Object.keys(slots).filter((key) => key === 'cardBodyCell').length > 0
    })
    const sizeOptions = computed(() => {
      if (tableLayout.value === 'card') {
        return ['8', '12', '16', '20', '24']
      }
      return ['5', '10', '15', '20', '25', '30', '35', '40', '50']
    })
    const paginationByLayout = computed(() => {
      if (props.pagination === false) {
        return false
      }
      return { ...props.pagination, pageSizeOptions: sizeOptions.value }
    })

    onMounted(() => {
      // 判断是否需要存储
      if (props.layout) {
        tableLayout.value = props.layout as TableLayoutType
      } else {
        const layout = storageService.getLayoutConfig()
        tableLayout.value = (layout || 'table') as TableLayoutType
      }
    })
    const tableLayoutClick = () => {
      tableLayout.value = tableLayout.value === 'card' ? 'table' : 'card'
      emit('changeTableLayout', tableLayout.value)
    }
    watch(
      () => tableLayout.value,
      (val) => {
        if (!storageService.exitOpenStorage()) return
        // 判断是否需要存储
        storageService.setLayoutConfig(val)
      }
    )
    let customColumnList = ref<CustomColumnType[]>([])
    const customCheckColumnList = computed(() => {
      return customColumnList.value
        .filter((item: CustomColumnType) => item.checked)
        .map((item: CustomColumnType) => String(item.dataIndex))
    })
    const customColumn = computed(() => {
      if (!storageService.exitOpenStorage()) return props.columns
      return customColumnList.value.filter((item: CustomColumnType) => item.checked)
    })
    const resetCustomColumn = () => {
      customColumnList.value = props.columns.map((item: CustomColumnType) => ({ ...item, checked: true }))
    }
    const onCheckChange = (checkedValues: CheckboxValueType[]) => {
      customColumnList.value = customColumnList.value.map((item: CustomColumnType) => ({
        ...item,
        checked: checkedValues.includes(String(item.dataIndex))
      }))
    }
    const onDrop = (dropResult: any) => {
      customColumnList.value = dropApplyDrag<CustomColumnType>(customColumnList.value, dropResult)
    }
    /** 设置默认列 */
    const setDefaultCustomColumnList = () => {
      customColumnList.value = props.columns.map((item: CustomColumnType) => ({ ...item, checked: true }))
    }
    // 监听列变化,同步至缓存customColumnList中
    watch(
      () => props.columns,
      (val) => {
        if (!storageService.exitOpenStorage()) return
        const catchStorage = storageService.getColumnConfig() || []
        if (
          catchStorage.length == 0 ||
          (catchStorage.length > 0 && catchStorage.some((key) => typeof key === 'string')) ||
          !compareArrays(
            val.map((item: CustomColumnType) => String(item.dataIndex)),
            catchStorage.filter((item) => item.key).map((item) => String(item.key))
          )
        ) {
          return setDefaultCustomColumnList()
        } else {
          const tmpObj: { [key: string]: CustomColumnType } = {}
          val.forEach((item: CustomColumnType) => {
            tmpObj[String(item.dataIndex || '_d')] = item
          })
          customColumnList.value = catchStorage.map((item) => {
            const key = item.key
            return {
              ...tmpObj[String(key)],
              checked: item.checked
            }
          })
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
        if (!storageService.exitOpenStorage()) return

        if (JSON.stringify(val) !== JSON.stringify(props.columns)) {
          storageService.setColumnConfig(
            customColumnList.value.map((item) => {
              return {
                key: item.dataIndex,
                checked: item.checked
              } as CatchStorageType
            })
          )
        } else {
          storageService.setColumnConfig([])
        }
      },
      {
        immediate: true
      }
    )

    const paginationChange = (page: number, size: number) => {
      emit(
        'change',
        {
          ...props.pagination,
          current: page,
          pageSize: size
        },
        {},
        {}
      )
    }
    return {
      onDrop,
      countdownSwitch,
      countDownFinish,
      countdownNumber,
      countDownChange,
      attrs,
      props,
      slots,
      Empty,
      refreshClick,
      // otherSlots,
      tableLayoutClick,
      canChangeLayout,
      tableLayout,
      tableSizeList,
      tableSize,
      customColumnList,
      customColumn,
      customCheckColumnList,
      resetCustomColumn,
      onCheckChange,
      paginationChange,
      paginationByLayout
    }
  }
})
</script>
<style lang="less" scoped>
.custom-table {
  // position: relative;
  // padding-top: 36px;
  &__box {
    padding: 0 px;
    // height: 36px;
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
.card-pagination {
  display: flex;
  justify-content: flex-end;
}
</style>
<style scoped>
:deep(.ant-form-item) {
  margin-inline-end: 0;
}
/* :deep(.ant-statistic) {
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
} */
.header-statistic :deep(.ant-statistic) {
  line-height: 0;
}
.header-statistic :deep(.ant-statistic div) {
  display: inline-block;
  font-weight: normal;
  color: unset;
}
.header-statistic :deep(.ant-statistic-content-value, .ant-statistic-content) {
  font-size: 16px;
  color: unset;
}
</style>
