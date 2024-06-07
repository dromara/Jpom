<template>
  <div>
    <a-tabs v-model:activeKey="tabKey">
      <a-tab-pane :key="1" :tab="$t('pages.layout.user-log.9c06953e')">
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
                :placeholder="$t('pages.layout.user-log.3ce54760')"
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
                :placeholder="$t('pages.layout.user-log.4b5fa133')"
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
              <a-tooltip :title="$t('pages.layout.user-log.d8b36ec1')">
                <a-button type="primary" :loading="operateloading" @click="operaterloadData">{{
                  $t('pages.layout.user-log.a1f640f4')
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
                :title="`${$t('pages.layout.user-log.f8c26fc9')},${$t('pages.layout.user-log.c0820e90')},${$t(
                  'pages.layout.user-log.819da0e0'
                )}`"
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
      <a-tab-pane :key="2" :tab="$t('pages.layout.user-log.b75360fe')">
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
                :placeholder="$t('pages.layout.user-log.c28c6dc1')"
                class="search-input-item"
                @press-enter="loginloadData"
              />
              <a-input
                v-model:value="loginlistQuery['%ip%']"
                :placeholder="$t('pages.layout.user-log.11294c8f')"
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
              <a-tooltip :title="$t('pages.layout.user-log.d8b36ec1')">
                <a-button type="primary" :loading="loginloading" @click="loginloadData">{{
                  $t('pages.layout.user-log.a1f640f4')
                }}</a-button>
              </a-tooltip>
            </a-space>
          </template>
          <template #bodyCell="{ column, text }">
            <template v-if="column.dataIndex === 'success'">
              <a-tooltip
                placement="topLeft"
                :title="text ? $t('pages.layout.user-log.9d77d967') : $t('pages.layout.user-log.d3ec0514')"
              >
                <a-tag v-if="text" color="green">{{ $t('pages.layout.user-log.9d77d967') }}</a-tag>
                <a-tag v-else color="pink">{{ $t('pages.layout.user-log.d3ec0514') }}</a-tag>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'useMfa'">
              <a-tooltip
                placement="topLeft"
                :title="text ? $t('pages.layout.user-log.c5d442d') : $t('pages.layout.user-log.812ad3aa')"
              >
                <a-tag>{{ text ? $t('pages.layout.user-log.c5d442d') : $t('pages.layout.user-log.812ad3aa') }}</a-tag>
              </a-tooltip>
            </template>

            <template v-else-if="column.tooltip">
              <a-tooltip placement="topLeft" :title="text">
                <span>{{ text }}</span>
              </a-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'operateCode'">
              <a-tooltip placement="topLeft" :title="operateCode[text] || $t('pages.layout.user-log.5f51a112')">
                {{ operateCode[text] || $t('pages.layout.user-log.5f51a112') }}
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
          title: this.$t('pages.layout.user-log.ed74cc37'),
          dataIndex: 'username',
          ellipsis: true
        },
        { title: 'IP', dataIndex: 'ip', ellipsis: true, width: '130px' },
        {
          title: this.$t('pages.layout.user-log.602a0a5e'),
          dataIndex: 'nodeId',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('pages.layout.user-log.cd2442c1'),
          dataIndex: 'dataName',
          /*width: 240,*/
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.layout.user-log.8d20cb3f'),
          dataIndex: 'workspaceName',
          /*width: 240,*/
          ellipsis: true,
          tooltip: true
        },
        // { title: "数据 ID", dataIndex: "dataId", /*width: 240,*/ ellipsis: true,},
        {
          title: this.$t('pages.layout.user-log.3ce54760'),
          dataIndex: 'classFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$t('pages.layout.user-log.4b5fa133'),
          dataIndex: 'methodFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$t('pages.layout.user-log.2cdc5dc2'),
          dataIndex: 'optStatus',
          width: 90
        },
        {
          title: this.$t('pages.layout.user-log.45e88a2c'),
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
          title: this.$t('pages.layout.user-log.8384e057'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.layout.user-log.62bec2ff'),
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
          title: this.$t('pages.layout.user-log.bd29775b'),
          dataIndex: 'userAgent',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.layout.user-log.b2624f7d'),
          dataIndex: 'success',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('pages.layout.user-log.e8d822fc'),
          dataIndex: 'useMfa',
          ellipsis: true,
          width: '130px'
        },
        {
          title: this.$t('pages.layout.user-log.5f2a060b'),
          dataIndex: 'operateCode',
          /*width: 240,*/ ellipsis: true
        },

        {
          title: this.$t('pages.layout.user-log.dea71d69'),
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
