<template>
  <div>
    <a-tabs v-model:activeKey="tabKey">
      <a-tab-pane :key="1" :tab="$tl('p.operationLog')">
        <!-- 数据表格 -->
        <a-table
          size="middle"
          :data-source="operatelist"
          :columns="operatecolumns"
          :pagination="operatecpagination"
          bordered
          row-key="id"
          @change="
            (pagination, filters, sorter) => {
              operatelistQuery = CHANGE_PAGE(operatelistQuery, {
                pagination,
                sorter
              })
              operaterloadData()
            }
          "
        >
          <template #title>
            <a-space>
              <a-select
                v-model:value="operatelistQuery.classFeature"
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
                allow-clear
                :placeholder="$tl('c.function')"
                class="search-input-item"
              >
                <a-select-option v-for="item in classFeature" :key="item.value">{{ item.title }}</a-select-option>
              </a-select>
              <a-select
                v-model:value="operatelistQuery.methodFeature"
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
                allow-clear
                :placeholder="$tl('c.method')"
                class="search-input-item"
              >
                <a-select-option v-for="item in methodFeature" :key="item.value">{{ item.title }}</a-select-option>
              </a-select>
              <a-range-picker
                :show-time="{ format: 'HH:mm:ss' }"
                format="YYYY-MM-DD HH:mm:ss"
                @change="
                  (value, dateString) => {
                    operatelistQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
                  }
                "
              />
              <a-tooltip :title="$tl('c.shortcut')">
                <a-button type="primary" :loading="operateloading" @click="operaterloadData">{{
                  $tl('c.search')
                }}</a-button>
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
                :title="`${$tl('p.defaultStatusCode')},${$tl('p.partialStatusCode')},${$tl('p.statusCode0')}`"
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
      <a-tab-pane :key="2" :tab="$tl('p.loginLog')">
        <a-table
          size="middle"
          :data-source="loginlist"
          :columns="logincolumns"
          :pagination="loginpagination"
          bordered
          row-key="id"
          @change="
            (pagination, filters, sorter) => {
              loginlistQuery = CHANGE_PAGE(loginlistQuery, {
                pagination,
                sorter
              })
              loginloadData()
            }
          "
        >
          <template #title>
            <a-space>
              <a-input
                v-model:value="loginlistQuery['%username%']"
                :placeholder="$tl('p.username')"
                class="search-input-item"
                @press-enter="loginloadData"
              />
              <a-input
                v-model:value="loginlistQuery['%ip%']"
                :placeholder="$tl('p.loginIp')"
                class="search-input-item"
                @press-enter="loginloadData"
              />
              <a-range-picker
                :show-time="{ format: 'HH:mm:ss' }"
                format="YYYY-MM-DD HH:mm:ss"
                @change="
                  (value, dateString) => {
                    loginlistQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
                  }
                "
              />
              <a-tooltip :title="$tl('c.shortcut')">
                <a-button type="primary" :loading="loginloading" @click="loginloadData">{{ $tl('c.search') }}</a-button>
              </a-tooltip>
            </a-space>
          </template>
          <template #bodyCell="{ column, text }">
            <template v-if="column.dataIndex === 'success'">
              <a-tooltip placement="topLeft" :title="text ? $tl('c.success') : $tl('c.failure')">
                <a-tag v-if="text" color="green">{{ $tl('c.success') }}</a-tag>
                <a-tag v-else color="pink">{{ $tl('c.failure') }}</a-tag>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'useMfa'">
              <a-tooltip placement="topLeft" :title="text ? $tl('c.usage') : $tl('c.unused')">
                <a-tag>{{ text ? $tl('c.usage') : $tl('c.unused') }}</a-tag>
              </a-tooltip>
            </template>

            <template v-else-if="column.tooltip">
              <a-tooltip placement="topLeft" :title="text">
                <span>{{ text }}</span>
              </a-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'operateCode'">
              <a-tooltip placement="topLeft" :title="operateCode[text] || $tl('c.unknown')">
                {{ operateCode[text] || $tl('c.unknown') }}
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
          title: this.$tl('p.operator'),
          dataIndex: 'username',
          ellipsis: true
        },
        { title: 'IP', dataIndex: 'ip', ellipsis: true, width: '130px' },
        {
          title: this.$tl('p.node'),
          dataIndex: 'nodeId',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$tl('p.dataName'),
          dataIndex: 'dataName',
          /*width: 240,*/
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.workspaceName'),
          dataIndex: 'workspaceName',
          /*width: 240,*/
          ellipsis: true,
          tooltip: true
        },
        // { title: "数据 ID", dataIndex: "dataId", /*width: 240,*/ ellipsis: true,},
        {
          title: this.$tl('c.function'),
          dataIndex: 'classFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$tl('c.method'),
          dataIndex: 'methodFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$tl('p.statusCode'),
          dataIndex: 'optStatus',
          width: 90
        },
        {
          title: this.$tl('p.operationTime'),
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
          title: this.$tl('p.userId'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.userName'),
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
          title: this.$tl('p.browser'),
          dataIndex: 'userAgent',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.successFlag'),
          dataIndex: 'success',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$tl('p.mfaUsage'),
          dataIndex: 'useMfa',
          ellipsis: true,
          width: '130px'
        },
        {
          title: this.$tl('p.resultDescription'),
          dataIndex: 'operateCode',
          /*width: 240,*/ ellipsis: true
        },

        {
          title: this.$tl('p.loginTime'),
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
    $tl(key, ...args) {
      return this.$t(`pages.layout.userLog.${key}`, ...args)
    },
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
