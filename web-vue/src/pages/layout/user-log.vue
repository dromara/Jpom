<template>
  <div>
    <a-tabs v-model:activeKey="tabKey">
      <a-tab-pane :key="1" :tab="$t('i18n_cda84be2f6')">
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
                :placeholder="$t('i18n_8432a98819')"
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
                :placeholder="$t('i18n_a9de52acb0')"
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
              <a-tooltip :title="$t('i18n_4838a3bd20')">
                <a-button type="primary" :loading="operateloading" @click="operaterloadData">{{
                  $t('i18n_e5f71fc31e')
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
                :title="`${$t('i18n_be4b9241ec')},${$t('i18n_69056f4792')},${$t('i18n_27b36afd36')}`"
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
      <a-tab-pane :key="2" :tab="$t('i18n_3fb2e5ec7b')">
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
                :placeholder="$t('i18n_819767ada1')"
                class="search-input-item"
                @press-enter="loginloadData"
              />
              <a-input
                v-model:value="loginlistQuery['%ip%']"
                :placeholder="$t('i18n_b38d6077d6')"
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
              <a-tooltip :title="$t('i18n_4838a3bd20')">
                <a-button type="primary" :loading="loginloading" @click="loginloadData">{{
                  $t('i18n_e5f71fc31e')
                }}</a-button>
              </a-tooltip>
            </a-space>
          </template>
          <template #bodyCell="{ column, text }">
            <template v-if="column.dataIndex === 'success'">
              <a-tooltip placement="topLeft" :title="text ? $t('i18n_330363dfc5') : $t('i18n_acd5cb847a')">
                <a-tag v-if="text" color="green">{{ $t('i18n_330363dfc5') }}</a-tag>
                <a-tag v-else color="pink">{{ $t('i18n_acd5cb847a') }}</a-tag>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'useMfa'">
              <a-tooltip placement="topLeft" :title="text ? $t('i18n_ecff77a8d4') : $t('i18n_869ec83e33')">
                <a-tag>{{ text ? $t('i18n_ecff77a8d4') : $t('i18n_869ec83e33') }}</a-tag>
              </a-tooltip>
            </template>

            <template v-else-if="column.tooltip">
              <a-tooltip placement="topLeft" :title="text">
                <span>{{ text }}</span>
              </a-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'operateCode'">
              <a-tooltip placement="topLeft" :title="operateCode[text] || $t('i18n_1622dc9b6b')">
                {{ operateCode[text] || $t('i18n_1622dc9b6b') }}
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
          title: this.$t('i18n_6b0bc6432d'),
          dataIndex: 'username',
          ellipsis: true
        },
        { title: 'IP', dataIndex: 'ip', ellipsis: true, width: '130px' },
        {
          title: this.$t('i18n_3bf3c0a8d6'),
          dataIndex: 'nodeId',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_5a1419b7a2'),
          dataIndex: 'dataName',
          /*width: 240,*/
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_4524ed750d'),
          dataIndex: 'workspaceName',
          /*width: 240,*/
          ellipsis: true,
          tooltip: true
        },
        // { title: "数据 ID", dataIndex: "dataId", /*width: 240,*/ ellipsis: true,},
        {
          title: this.$t('i18n_8432a98819'),
          dataIndex: 'classFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$t('i18n_a9de52acb0'),
          dataIndex: 'methodFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$t('i18n_771d897d9a'),
          dataIndex: 'optStatus',
          width: 90
        },
        {
          title: this.$t('i18n_7e951d56d9'),
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
          title: this.$t('i18n_30acd20d6e'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_dfb8d511c7'),
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
          title: this.$t('i18n_912302cb02'),
          dataIndex: 'userAgent',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_5e9f2dedca'),
          dataIndex: 'success',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_ae0d608495'),
          dataIndex: 'useMfa',
          ellipsis: true,
          width: '130px'
        },
        {
          title: this.$t('i18n_64c083c0a9'),
          dataIndex: 'operateCode',
          /*width: 240,*/ ellipsis: true
        },

        {
          title: this.$t('i18n_9fca7c455f'),
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
