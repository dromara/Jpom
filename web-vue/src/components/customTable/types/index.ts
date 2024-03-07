import { ColumnType } from 'ant-design-vue/es/table'
import { RenderExpandIconProps } from 'ant-design-vue/es/vc-table/interface'
import { CustomSlotsType } from 'ant-design-vue/es/_util/type'
export type CustomColumnType = ColumnType & {
  checked?: boolean
}
export type CatchStorageType = {
  key: string
  checked: boolean
}

export type CustomTableType = {
  columns: CustomColumnType[]
  storageKey: string
}

export type CustomTableSlotsType = CustomSlotsType<{
  emptyText?: any
  expandIcon?: RenderExpandIconProps<any>
  title?: any
  footer?: any
  summary?: any
  expandedRowRender?: any
  expandColumnTitle?: any
  bodyCell?: (props: { text: any; value: any; record: Record<string, any>; index: number; column: ColumnType }) => void
  headerCell?: (props: { title: any; column: ColumnType }) => void
  customFilterIcon?: any
  customFilterDropdown?: any
  default: any
}>
