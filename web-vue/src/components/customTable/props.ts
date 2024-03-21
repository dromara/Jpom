///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import { initDefaultProps } from 'ant-design-vue/es/_util/props-util'
import { tableProps } from 'ant-design-vue/es/table'
import { CustomColumnType } from './types'

export const customTableProps = initDefaultProps(
  {
    ...tableProps(),
    columns: {
      type: Array<CustomColumnType>,
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
    isHideAutoRefresh: {
      type: Boolean,
      default: false
    },
    /** 默认自动刷新 */
    defaultAutoRefresh: {
      type: Boolean,
      default: false
    },
    /** 自动刷新时间 s 秒，不建议小于 10 秒 */
    autoRefreshTime: {
      type: Number,
      default: 10
    },
    /**
     * 页面布局方式
     */
    layout: {
      type: String,
      default: null
    },
    /**
     * 当前页面是否激活
     *
     * 自动刷新需要配合使用
     */
    activePage: {
      type: Boolean,
      default: false
    },
    // 空数据时现在内容
    emptyDescription: {
      type: String,
      deafult: '暂无任何数据'
    }
  },
  {
    defaultAutoRefresh: false,
    isHideAutoRefresh: false,
    isShowTools: false,
    isHideRefresh: false,
    autoRefreshTime: 10,
    activePage: false,
    emptyDescription: '暂无任何数据'
  }
)
