<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="systemUserOperationLog"
      :empty-description="$t('pages.user.operation-log.ced5c864')"
      :data-source="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="change"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-select
            v-model:value="listQuery.userId"
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
            :placeholder="$t('pages.user.operation-log.30f0d93')"
            class="search-input-item"
          >
            <a-select-option v-for="item in userList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.nodeId"
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
            :placeholder="$t('pages.user.operation-log.580e6c10')"
            class="search-input-item"
          >
            <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.classFeature"
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
            :placeholder="$t('pages.user.operation-log.cb132686')"
            class="search-input-item"
          >
            <a-select-option v-for="item in classFeature" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.methodFeature"
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
            :placeholder="$t('pages.user.operation-log.19dfb1ba')"
            class="search-input-item"
          >
            <a-select-option v-for="item in methodFeature" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
          <a-range-picker :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime" />
          <a-tooltip :title="$t('pages.user.operation-log.cb5a8131')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.user.operation-log.53c2763c')
            }}</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'nodeId'">
          <a-tooltip placement="topLeft" :title="nodeMap[text]">
            <span>{{ nodeMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'classFeature'">
          <a-tooltip placement="topLeft" :title="classFeatureMap[text]">
            <span>{{ classFeatureMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'methodFeature'">
          <a-tooltip placement="topLeft" :title="methodFeatureMap[text]">
            <span>{{ methodFeatureMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
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
            :title="`${$t('pages.user.operation-log.f8c26fc9')},${$t('pages.user.operation-log.33046d7b')},${$t(
              'pages.user.operation-log.abc9fa2f'
            )}`"
          >
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-button size="small" type="primary" @click="handleDetail(record)">{{
            $t('pages.user.operation-log.151c73eb')
          }}</a-button>
        </template>
      </template>
    </CustomTable>
    <!-- 详情区 -->
    <a-modal
      v-model:open="detailVisible"
      destroy-on-close
      width="600px"
      :title="$t('pages.user.operation-log.7990de3b')"
      :footer="null"
    >
      <a-list item-layout="horizontal" :data-source="detailData">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <h4>{{ item.title }}</h4>
              </template>
              <template #description>
                <div v-if="item.description">{{ item.description }}</div>
                <pre v-if="item.json" style="overflow: scroll">{{ item.value }}</pre>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-modal>
  </div>
</template>

<script>
import { getOperationLogList } from '@/api/operation-log'
import { getMonitorOperateTypeList } from '@/api/monitor'
import { getNodeListAll } from '@/api/node'
import { getUserListAll } from '@/api/user/user'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  components: {},
  data() {
    return {
      loading: false,
      list: [],
      nodeList: [],
      nodeMap: {},
      userList: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      methodFeature: [],
      classFeature: [],
      methodFeatureMap: {},
      classFeatureMap: {},
      temp: {},
      detailVisible: false,
      detailData: [],
      columns: [
        {
          title: this.$t('pages.user.operation-log.8384e057'),
          dataIndex: 'userId',
          ellipsis: true
        },
        {
          title: this.$t('pages.user.operation-log.7d2228ed'),
          dataIndex: 'username',
          ellipsis: true
        },
        { title: 'IP', dataIndex: 'ip' /*width: 130*/ },
        {
          title: this.$t('pages.user.operation-log.602a0a5e'),
          dataIndex: 'nodeId',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('pages.user.operation-log.cd2442c1'),
          dataIndex: 'dataName',
          /*width: 240,*/
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.user.operation-log.8d20cb3f'),
          dataIndex: 'workspaceName',
          /*width: 240,*/
          ellipsis: true,
          tooltip: true
        },
        // { title: "数据 ID", dataIndex: "dataId", /*width: 240,*/ ellipsis: true, },
        {
          title: this.$t('pages.user.operation-log.cb132686'),
          dataIndex: 'classFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$t('pages.user.operation-log.19dfb1ba'),
          dataIndex: 'methodFeature',
          /*width: 240,*/
          ellipsis: true
        },
        {
          title: this.$t('pages.user.operation-log.2cdc5dc2'),
          dataIndex: 'optStatus',
          width: 90
        },
        {
          title: this.$t('pages.user.operation-log.45e88a2c'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text, record }) => {
            return parseTime(text || record.optTime)
          },
          width: '170px'
        },
        {
          title: this.$t('pages.user.operation-log.a0fe2109'),
          align: 'center',
          dataIndex: 'operation',
          fixed: 'right',
          width: '80px'
        }
      ]
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  created() {
    this.loadData()
    this.loadUserList()
    this.loadNodeList()

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
      return this.$t(`pages.user.operationLog.${key}`, ...args)
    },
    // 加载 node
    loadNodeList() {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data

          res.data.forEach((item) => {
            this.nodeMap[item.id] = item.name
          })
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getOperationLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 分页、排序、筛选变化时触发
    change(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // 加载用户列表
    loadUserList() {
      getUserListAll().then((res) => {
        if (res.code === 200) {
          this.userList = res.data
        }
      })
    },
    // 选择时间
    onchangeTime(value, dateString) {
      this.listQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
    },
    // 查看详情
    handleDetail(record) {
      this.detailData = []

      this.temp = Object.assign({}, record)
      try {
        this.temp.reqData = JSON.parse(this.temp.reqData)
      } catch (e) {
        console.error(e)
      }
      try {
        this.temp.resultMsg = JSON.parse(this.temp.resultMsg)
      } catch (e) {
        console.error(e)
      }
      this.detailData.push({ title: this.$t('pages.user.operation-log.a7ebd858'), description: this.temp.dataId })
      this.detailData.push({
        title: this.$t('pages.user.operation-log.4ade0404'),
        description: this.temp.userAgent
      })
      this.detailData.push({
        title: this.$t('pages.user.operation-log.8749e9ed'),
        json: true,
        value: this.temp.reqData
      })
      this.detailData.push({
        title: this.$t('pages.user.operation-log.9585489'),
        json: true,
        value: this.temp.resultMsg
      })
      this.detailVisible = true
    }
  }
}
</script>
