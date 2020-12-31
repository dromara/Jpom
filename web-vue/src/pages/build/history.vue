<template>
  <div>
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.buildDataId" allowClear placeholder="请选择构建名称"
        class="filter-item" @change="handleFilter">
        <a-select-option v-for="build in buildList" :key="build.id">{{ build.name }}</a-select-option>
      </a-select>
      <a-select v-model="listQuery.status" allowClear placeholder="请选择状态"
        class="filter-item" @change="handleFilter">
        <!-- <a-select-option :value="0">未构建</a-select-option> -->
        <a-select-option :value="1">构建中</a-select-option>
        <a-select-option :value="2">构建成功</a-select-option>
        <a-select-option :value="3">构建失败</a-select-option>
        <a-select-option :value="4">发布中</a-select-option>
        <a-select-option :value="5">发布成功</a-select-option>
        <a-select-option :value="6">发布失败</a-select-option>
        <a-select-option :value="7">取消构建</a-select-option>
      </a-select>
      <a-range-picker class="filter-item" :show-time="{format: 'HH:mm:ss'}" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime"/>
      <a-button type="primary" @click="handleFilter">搜索</a-button>
      <a-button type="primary" @click="handleFilter">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns"
      :pagination="pagination" :scroll="{x: '80vw', y: tableHeight }" bordered
      :rowKey="(record, index) => index" @change="change">
      <a-tooltip slot="buildName" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="releaseMethod" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="buildUser" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleDownload(record)">下载日志</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
  </div>
</template>
<script>
import { geteBuildHistory, getBuildList, downloadBuildLog, deleteBuildHistory } from '../../api/build';
import { parseTime } from '../../utils/time';
export default {
  data() {
    return {
      loading: false,
      list: [],
      buildList: [],
      total: 0,
      listQuery: {
        page: 1,
        limit: 20
      },
      timeRange: '',
      tableHeight: '70vh',
      temp: {},
      columns: [
        {title: '构建名称', dataIndex: 'buildName', width: 120, ellipsis: true, scopedSlots: {customRender: 'buildName'}},
        {title: '构建 ID', dataIndex: 'buildIdStr', width: 100, ellipsis: true, scopedSlots: {customRender: 'buildIdStr'}},
        {title: '状态', dataIndex: 'status', width: 120, ellipsis: true, scopedSlots: {customRender: 'nodeId'}},
        {title: '开始时间', dataIndex: 'startTime', customRender: (text) => {
          return parseTime(text);
        }, width: 180},
        {title: '结束时间', dataIndex: 'endTime', customRender: (text) => {
          return parseTime(text);
        }, width: 180},
        {title: '发布方式', dataIndex: 'releaseMethod', width: 100, ellipsis: true, scopedSlots: {customRender: 'releaseMethod'}},
        {title: '构建人', dataIndex: 'buildUser', width: 150, ellipsis: true, scopedSlots: {customRender: 'buildUser'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 230, fixed: 'right'}
      ]
    }
  },
  computed: {
    pagination() {
      return {
        total: this.total,
        current: this.listQuery.page || 1,
        pageSize: this.listQuery.limit || 10,
        pageSizeOptions: ['10', '20', '50', '100'],
        showSizeChanger: true,
        showTotal: (total) => {
          return `Total ${total} items`;
        }
      }
    }
  },
  created() {
    this.calcTableHeight();
    this.loadBuildList();
    this.loadData();
  },
  methods: {
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 220;
      })
    },
    // 加载构建列表
    loadBuildList() {
      getBuildList().then(res => {
        if (res.code === 200) {
          this.buildList = res.data;
        }
      })
    },
    // 加载数据
    loadData() {
      this.loading = true;
      this.listQuery.time = this.timeRange;
      geteBuildHistory(this.listQuery).then(res => {
        if (res.code === 200) {
          this.list = res.data;
          this.total = res.total;
        }
        this.loading = false;
      })
    },
    // 分页、排序、筛选变化时触发
    change(pagination) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      this.loadData();
    },
    // 选择时间
    onchangeTime(value, dateString) {
      if (!dateString[0] || !dateString[1]) {
        this.timeRange = '';
      } else {
        this.timeRange = `${dateString[0]} ~ ${dateString[1]}`;
      }
    },
    // 搜索
    handleFilter() {
      this.listQuery.page = 1;
      this.loadData();
    },
    // 下载构建日志
    handleDownload(record) {
      // 请求接口拿到 blob
      downloadBuildLog(record.id).then(blob => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement('a');
        link.style.display = 'none';
        link.href = url;
        link.setAttribute('download', `${record.name}.log`);
        document.body.appendChild(link);
        link.click();
      })
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除构建历史记录么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteBuildHistory(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              this.handleFilter();
            }
          })
        }
      });
    }
  }
}
</script>
<style scoped>
.filter {
  margin-bottom: 10px;
}
.ant-btn {
  margin-right: 10px;
}
.filter-item {
  width: 150px;
  margin-right: 10px;
}
</style>