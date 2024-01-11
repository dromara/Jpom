<template>
  <div>
    <a-tabs v-model:activeKey="tabKey">
      <a-tab-pane :key="1" tab="操作日志">
        <!-- 数据表格 -->
        <a-table
          size="middle"
          :data-source="operatelist"
          :columns="operatecolumns"
          :pagination="operatecpagination"
          bordered
          rowKey="id"
          @change="
            (pagination, filters, sorter) => {
              this.operatelistQuery = CHANGE_PAGE(this.operatelistQuery, {
                pagination,
                sorter
              })
              this.operaterloadData()
            }
          "
        >
          <template #title>
            <a-space>
              <a-select
                show-search
                :filter-option="
                  (input, option) => {
                    const children = option.children && option.children()
                    return (
                      children &&
                      children[0].children &&
                      children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    )
                  }
                "
                v-model:value="operatelistQuery.classFeature"
                allowClear
                placeholder="操作功能"
                class="search-input-item"
              >
                <a-select-option v-for="item in classFeature" :key="item.value">{{ item.title }}</a-select-option>
              </a-select>
              <a-select
                show-search
                :filter-option="
                  (input, option) => {
                    const children = option.children && option.children()
                    return (
                      children &&
                      children[0].children &&
                      children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                    )
                  }
                "
                v-model:value="operatelistQuery.methodFeature"
                allowClear
                placeholder="操作方法"
                class="search-input-item"
              >
                <a-select-option v-for="item in methodFeature" :key="item.value">{{ item.title }}</a-select-option>
              </a-select>
              <a-range-picker
                :show-time="{ format: 'HH:mm:ss' }"
                format="YYYY-MM-DD HH:mm:ss"
                @change="
                  (value, dateString) => {
                    this.operatelistQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
                  }
                "
              />
              <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
                <a-button type="primary" :loading="operateloading" @click="operaterloadData">搜索</a-button>
              </a-tooltip>
            </a-space>
          </template>
          <template #bodyCell="{ column, text }">
            <template v-if="column.dataIndex === 'classFeature'">
              <a-tooltip placement="topLeft" :title="classFeatureMap[text]">
                <span>{{ classFeatureMap[text] }}</span>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'methodFeature'">
              <a-tooltip placement="topLeft" :title="methodFeatureMap[text]">
                <span>{{ methodFeatureMap[text] }}</span>
              </a-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'username'">
              <a-tooltip placement="topLeft" :title="text">
                <span>{{ text || item.userId }}</span>
              </a-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'optStatus'">
              <a-tooltip
                placement="topLeft"
                :title="`默认状态码为 200 表示执行成功,部分操作状态码可能为 0,状态码为 0 的操作大部分为没有操作结果或者异步执行`"
              >
                <span>{{ text }}</span>
              </a-tooltip>
            </template>
            <template v-else-if="column.tooltip">
              <a-tooltip placement="topLeft" :title="text">
                <span>{{ text }}</span>
              </a-tooltip>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane :key="2" tab="登录日志">
        <a-table
          size="middle"
          :data-source="loginlist"
          :columns="logincolumns"
          :pagination="loginpagination"
          bordered
          rowKey="id"
          @change="
            (pagination, filters, sorter) => {
              this.loginlistQuery = CHANGE_PAGE(this.loginlistQuery, {
                pagination,
                sorter
              })
              this.loginloadData()
            }
          "
        >
          <template #title>
            <a-space>
              <a-input
                v-model:value="loginlistQuery['%username%']"
                @pressEnter="loginloadData"
                placeholder="用户名"
                class="search-input-item"
              />
              <a-input
                v-model:value="loginlistQuery['%ip%']"
                @pressEnter="loginloadData"
                placeholder="登录IP"
                class="search-input-item"
              />
              <a-range-picker
                :show-time="{ format: 'HH:mm:ss' }"
                format="YYYY-MM-DD HH:mm:ss"
                @change="
                  (value, dateString) => {
                    this.loginlistQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
                  }
                "
              />
              <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
                <a-button type="primary" :loading="loginloading" @click="loginloadData">搜索</a-button>
              </a-tooltip>
            </a-space>
          </template>
          <template #bodyCell="{ column, text }">
            <template v-if="column.dataIndex === 'success'">
              <a-tooltip placement="topLeft" :title="text ? '成功' : '失败'">
                <a-tag v-if="text" color="green">成功</a-tag>
                <a-tag v-else color="pink">失败</a-tag>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'useMfa'">
              <a-tooltip placement="topLeft" :title="text ? '使用' : '未使用'">
                <a-tag>{{ text ? '使用' : '未使用' }}</a-tag>
              </a-tooltip>
            </template>

            <template v-else-if="column.tooltip">
              <a-tooltip placement="topLeft" :title="text">
                <span>{{ text }}</span>
              </a-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'operateCode'">
              <a-tooltip placement="topLeft" :title="operateCode[text] || '未知'">
                {{ operateCode[text] || '未知' }}
              </a-tooltip>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { listOperaterLog, listLoginLog } from '@/api/user/user'
import { getMonitorOperateTypeList } from '@/api/monitor'
import { operateCodeMap } from '@/api/user/user-login-log'
export default {
  props: {
    openTab: {
      type: Number,
      default: 1
    }
  },
  data() {
    return {
      operateloading: false,
      operatelist: [],
      operatelistQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      methodFeature: [],
      classFeature: [],
      methodFeatureMap: {},
      classFeatureMap: {},
      operatecolumns: [
        {
          title: '操作者',
          dataIndex: 'username',
          ellipsis: true
        },
        { title: 'IP', dataIndex: 'ip', ellipsis: true, width: '130px' },
        {
          title: '节点',
          dataIndex: 'nodeId',
          width: 120,
          ellipsis: true
        },
        {
          title: '数据名称',
          dataIndex: 'dataName',
          /*width: 240,*/
          ellipsis: true,
          tooltip: true
        },
        {
          title: '工作空间名',
          dataIndex: 'workspaceName',
          /*width: 240,*/
          ellipsis: true,
          tooltip: true
        },
        // { title: "数据 ID", dataIndex: "dataId", /*width: 240,*/ ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        {
          title: '操作功能',
          dataIndex: 'classFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: '操作方法',
          dataIndex: 'methodFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: '状态码',
          dataIndex: 'optStatus',
          width: 90
        },
        {
          title: '操作时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text, item }) => {
            return parseTime(text || item.optTime)
          },
          width: '170px'
        }
      ],
      loginloading: false,
      loginlist: [],
      operateCode: operateCodeMap,
      loginlistQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      logincolumns: [
        {
          title: '用户ID',
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '用户名称',
          dataIndex: 'username',
          ellipsis: true,
          tooltip: true
        },
        {
          title: 'IP',
          dataIndex: 'ip',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '浏览器',
          dataIndex: 'userAgent',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '是否成功',
          dataIndex: 'success',
          ellipsis: true,
          width: '100px'
        },
        {
          title: '是否使用MFA',
          dataIndex: 'useMfa',
          ellipsis: true,
          width: '130px'
        },
        {
          title: '结果描述',
          dataIndex: 'operateCode',
          /*width: 240,*/ ellipsis: true,
          scopedSlots: { customRender: 'operateCode' }
        },

        {
          title: '登录时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text, item }) => {
            return parseTime(text || item.optTime)
          },
          width: '170px'
        }
      ],
      tabKey: 1
    }
  },
  computed: {
    operatecpagination() {
      return COMPUTED_PAGINATION(this.operatelistQuery)
    },
    loginpagination() {
      return COMPUTED_PAGINATION(this.loginlistQuery)
    }
  },
  created() {
    if (this.openTab) {
      this.tabKey = this.openTab
    }
    this.operaterloadData()
    this.loginloadData()
    getMonitorOperateTypeList().then((res) => {
      this.methodFeature = res.data.methodFeature
      this.classFeature = res.data.classFeature
      res.data.methodFeature.forEach((item) => {
        this.methodFeatureMap[item.value] = item.title
      })
      res.data.classFeature.forEach((item) => {
        this.classFeatureMap[item.value] = item.title
      })
    })
  },
  methods: {
    CHANGE_PAGE,
    // 加载数据
    operaterloadData(pointerEvent) {
      this.operateloading = true
      this.operatelistQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.operatelistQuery.page
      listOperaterLog(this.operatelistQuery).then((res) => {
        if (res.code === 200) {
          this.operatelist = res.data.result
          this.operatelistQuery.total = res.data.total
        }
        this.operateloading = false
      })
    },
    loginloadData(pointerEvent) {
      this.loginloading = true
      this.loginlistQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.loginlistQuery.page
      listLoginLog(this.loginlistQuery).then((res) => {
        if (res.code === 200) {
          this.loginlist = res.data.result
          this.loginlistQuery.total = res.data.total
        }
        this.loginloading = false
      })
    }
  }
}
</script>
