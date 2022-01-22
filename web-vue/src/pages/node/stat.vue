<template>
  <div class="full-content">
    <a-space direction="vertical">
      <a-card>
        <!-- <template slot="title">
          <a-row>
            <a-col :span="2">状态概况</a-col>
          </a-row>
        </template> -->
        <a-row>
          <a-col :span="4">
            <a-statistic title="节点总数" :value="nodeCount"> </a-statistic>
          </a-col>
          <a-col :span="4" v-for="(desc, key) in statusMap" :key="key">
            <a-statistic :title="desc" :value="statusStatMap[key]"> </a-statistic>
          </a-col>
          <a-col :span="2"> <a-statistic-countdown format="s 秒" title="刷新倒计时" :value="deadline" @finish="onFinish" /> </a-col>
        </a-row>
      </a-card>

      <div ref="filter" class="filter">
        <a-space>
          <a-input v-model="listQuery['%name%']" placeholder="节点名称" />
          <a-input v-model="listQuery['%url%']" placeholder="节点地址" />
          <a-select v-model="listQuery.status" allowClear placeholder="请选择状态" class="search-input-item">
            <a-select-option v-for="(desc, key) in statusMap" :key="key">{{ desc }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
          </a-tooltip>
        </a-space>
      </div>
      <!-- 表格 :scroll="{ x: 1070, y: tableHeight -60 }" scroll 跟 expandedRowRender 不兼容，没法同时使用不然会多出一行数据-->
      <a-table :columns="columns" :data-source="list" bordered rowKey="id" :pagination="(this, pagination)" @change="changePage">
        <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <template slot="status" slot-scope="text, record">
          <a-tooltip v-if="text !== 0" placement="topLeft" :title="record.failureMsg">
            <span>{{ statusMap[text] }}</span>
          </a-tooltip>
          <span v-else>{{ statusMap[text] }}</span>
        </template>

        <template slot="progress" slot-scope="text">
          <a-tooltip placement="topLeft" :title="`${text}%`">
            <a-progress
              :percent="text"
              :stroke-color="{
                from: '#87d068',
                to: '#108ee9',
              }"
              size="small"
              status="active"
              :showInfo="false"
            />
          </a-tooltip>
        </template>
      </a-table>
    </a-space>
  </div>
</template>
<script>
import { getStatist, status, statusStat } from "@/api/node-stat";
import { parseTime } from "@/utils/time";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";

export default {
  components: {},
  data() {
    return {
      loading: false,
      statusMap: status,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      statusStatMap: {},
      nodeCount: 0,
      // nodeMonitorCycle: nodeMonitorCycle,
      deadline: 0,
      temp: {},
      columns: [
        { title: "节点名称", dataIndex: "name", sorter: true, key: "name", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "节点地址", dataIndex: "url", sorter: true, key: "url", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "cpu", dataIndex: "occupyCpu", sorter: true, key: "occupyCpu", ellipsis: true, scopedSlots: { customRender: "progress" } },
        { title: "disk", dataIndex: "occupyDisk", sorter: true, key: "occupyDisk", ellipsis: true, scopedSlots: { customRender: "progress" } },
        { title: "memory", dataIndex: "occupyMemory", sorter: true, key: "occupyMemory", ellipsis: true, scopedSlots: { customRender: "progress" } },
        { title: "memoryUsed", dataIndex: "occupyMemoryUsed", sorter: true, key: "occupyMemoryUsed", ellipsis: true, scopedSlots: { customRender: "progress" } },
        { title: "运行时间", dataIndex: "upTimeStr", sorter: true, key: "upTimeStr", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "状态", dataIndex: "status", sorter: true, key: "status", ellipsis: true, scopedSlots: { customRender: "status" } },
        {
          title: "更新时间",
          dataIndex: "modifyTimeMillis",
          ellipsis: true,
          sorter: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
      ],
    };
  },
  computed: {
    pagination() {
      return {
        total: this.listQuery.total || 0,
        current: this.listQuery.page || 1,
        pageSize: this.listQuery.limit || PAGE_DEFAULT_LIMIT,
        pageSizeOptions: PAGE_DEFAULT_SIZW_OPTIONS,
        showSizeChanger: true,
        showTotal: (total) => {
          return PAGE_DEFAULT_SHOW_TOTAL(total, this.listQuery);
        },
      };
    },
  },
  watch: {},
  created() {
    this.loadData();
  },
  destroyed() {
    if (this.pullFastInstallResultTime) {
      clearInterval(this.pullFastInstallResultTime);
    }
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.list = [];
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getStatist(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
      statusStat().then((res) => {
        if (res.data) {
          this.statusStatMap = res.data;
          let nodeCount2 = 0;
          // console.log(this.statusStatMap);
          Object.values(this.statusStatMap).forEach((element) => {
            nodeCount2 += element;
          });
          this.nodeCount = nodeCount2;
          this.deadline = Date.now() + 30 * 1000;
        }
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      if (sorter) {
        this.listQuery.order = sorter.order;
        this.listQuery.order_field = sorter.field;
      }
      this.loadData();
    },
    onFinish() {
      this.loadData();
    },
  },
};
</script>
<style scoped></style>
