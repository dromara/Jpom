///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

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

export type TableLayoutType = 'table' | 'card' | undefined

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
  tableHelp?: any
  toolPrefix?: any
  tableBodyCell?: any
  expandedRowRender?: any
  expandColumnTitle?: any
  emptyDescription: string
  bodyCell?: (props: { text: any; value: any; record: Record<string, any>; index: number; column: ColumnType }) => void
  headerCell?: (props: { title: any; column: ColumnType }) => void
  customFilterIcon?: any
  customFilterDropdown?: any
  default: any
}>
