<template>
  <div>
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.selectUser" allowClear placeholder="请选择操作者"
        class="filter-item" @change="handleFilter">
        <a-select-option value="system">系统管理员</a-select-option>
      </a-select>
      <a-select v-model="listQuery.selectNode" allowClear placeholder="请选择节点"
        class="filter-item" @change="handleFilter">
        <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
      </a-select>
      <a-range-picker class="filter-item" :show-time="{format: 'HH:mm:ss'}" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime"/>
      <a-button type="primary" @click="handleFilter">搜索</a-button>
      <a-button type="primary" @click="handleFilter">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns"
      :pagination="pagination" :style="{'max-height': tableHeight + 'px' }" :scroll="{x: 1300, y: tableHeight - 120}" bordered
      :rowKey="(record, index) => index" @change="change">
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="dataId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="optTypeMsg" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleDetail(record)">详情</a-button>
      </template>
    </a-table>
    <!-- 详情区 -->
    <a-modal v-model="detailVisible" width="600px" title="详情信息" :footer="null">
      <a-list item-layout="horizontal" :data-source="detailData">
        <a-list-item slot="renderItem" slot-scope="item">
          <a-list-item-meta :description="item.description">
            <h4 slot="title">{{ item.title }}</h4>
          </a-list-item-meta>
        </a-list-item>
      </a-list>
    </a-modal>
  </div>
</template>
<script>
import { getOperationLogList } from '../../api/operation-log';
import { getNodeList } from '../../api/node';
import { getUserList } from '../../api/user';
import { parseTime } from '../../utils/time';
export default {
  data() {
    return {
      loading: false,
      list: [],
      nodeList: [],
      userList: [],
      total: 0,
      listQuery: {
        page: 1,
        limit: 20
      },
      timeRange: '',
      tableHeight: '70vh',
      temp: {},
      detailVisible: false,
      detailData: [],
      columns: [
        {title: '操作者', dataIndex: 'userId', width: 100},
        {title: 'IP', dataIndex: 'ip', /*width: 130*/},
        {title: '节点 ID', dataIndex: 'nodeId', width: 200, ellipsis: true, scopedSlots: {customRender: 'nodeId'}},
        {title: '数据 ID', dataIndex: 'dataId', /*width: 240,*/ ellipsis: true, scopedSlots: {customRender: 'dataId'}},
        {title: '操作类型', dataIndex: 'optTypeMsg', width: 100, ellipsis: true, scopedSlots: {customRender: 'optTypeMsg'}},
        {title: '执行结果', dataIndex: 'optStatusMsg', width: 100},
        {title: '操作时间', dataIndex: 'optTime', customRender: (text) => {
          return parseTime(text);
        }, /*width: 180*/},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 120, fixed: 'right'}
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
          if(total<=this.listQuery.limit){
            return '';
          }
          return `总计 ${total} 条`;
        }
      }
    }
  },
  created() {
    this.calcTableHeight();
    this.loadData();
    this.loadUserList();
    this.loadNodeList();
  },
  methods: {
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 135;
      })
    },
    // 加载 node
    loadNodeList() {
      getNodeList().then(res => {
        if (res.code === 200) {
          this.nodeList = res.data;
        }
      })
    },
    // 加载数据
    loadData() {
      this.loading = true;
      this.listQuery.time = this.timeRange;
      getOperationLogList(this.listQuery).then(res => {
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
    // 加载用户列表
    loadUserList() {
      getUserList().then(res => {
        if (res.code === 200) {
          this.userList = res.data;
        }
      })
    },
    // 选择时间
    onchangeTime(value, dateString) {
      this.timeRange = `${dateString[0]} ~ ${dateString[1]}`;
    },
    // 搜索
    handleFilter() {
      this.listQuery.page = 1;
      this.loadData();
    },
    // 查看详情
    handleDetail(record) {
      this.detailData = [];
      this.detailVisible = true;
      this.temp = Object.assign(record);
      this.detailData.push({title: '浏览器标识', description: this.temp.userAgent});
      this.detailData.push({title: '请求参数', description: this.temp.reqData});
      this.detailData.push({title: '响应结果', description: this.temp.resultMsg});
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
