<template>
  <div>
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.buildDataId" allowClear placeholder="请选择构建名称"
        class="filter-item" @change="handleFilter">
        <a-select-option v-for="build in buildList" :key="build.id">{{ build.name }}</a-select-option>
      </a-select>
      <a-select v-model="listQuery.status" allowClear placeholder="请选择状态"
        class="filter-item" @change="handleFilter">
        <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
      </a-select>
      <a-range-picker class="filter-item" :show-time="{format: 'HH:mm:ss'}" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime"/>
      <a-button type="primary" @click="handleFilter">搜索</a-button>
      <a-button type="primary" @click="handleFilter">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns"
      :pagination="pagination" :style="{'max-height': tableHeight + 'px' }" :scroll="{x: 1360, y: tableHeight - 120}" bordered
      :rowKey="(record, index) => index" @change="change">
      <a-tooltip slot="buildName" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="buildIdStr" slot-scope="text, record" placement="topLeft" :title="text + ' ( 点击查看日志 ) '">
        <span v-if="record.buildId <= 0"></span>
        <a-tag v-else color="#108ee9" @click="handleBuildLog(record)">{{ text }}</a-tag>
      </a-tooltip>
      <template slot="status" slot-scope="text" placement="topleft" :title="text">
        <span>{{ statusMap[text] }}</span>
      </template>
      <template slot="releaseMethod" slot-scope="text" placement="topleft" :title="text">
        <span>{{ releaseMethodMap[text] }}</span>
      </template>
      <a-tooltip slot="buildUser" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleDownload(record)">下载日志</a-button>
        <a-button type="primary" :disabled="!record.hashFile"  @click="handleFile(record)">下载产物</a-button>
        <a-dropdown>
          <a class="ant-dropdown-link" @click="e => e.preventDefault()">
            更多
            <a-icon type="down"/>
          </a>
          <a-menu slot="overlay">
            <a-menu-item>
              <a-button :disabled="!record.hashFile || record.releaseMethod === 0" type="danger"
                        @click="handleRollback(record)">回滚
              </a-button>
            </a-menu-item>
            <a-menu-item>
              <a-button type="danger" @click="handleDelete(record)">删除</a-button>
            </a-menu-item>
          </a-menu>
        </a-dropdown>
      </template>
    </a-table>
    <!-- 构建日志 -->
    <a-modal :width="'80vw'" v-model="buildLogVisible" title="构建日志" :footer="null" :maskClosable="false" @cancel="closeBuildLogModel">
      <build-log v-if="buildLogVisible" :temp="temp" />
    </a-modal>
  </div>
</template>
<script>
import BuildLog from './log';
import {
  geteBuildHistory,
  getBuildList,
  downloadBuildLog,
  rollback,
  deleteBuildHistory,
  releaseMethodMap
} from '../../api/build';
import { parseTime } from '../../utils/time';
export default {
  components: {
    BuildLog
  },
  data() {
    return {
      releaseMethodMap:releaseMethodMap,
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
      buildLogVisible: false,
      columns: [
        {title: '构建名称', dataIndex: 'buildName', /*width: 120,*/ ellipsis: true, scopedSlots: {customRender: 'buildName'}},
        {title: '构建 ID', dataIndex: 'buildIdStr', width: 100, ellipsis: true, scopedSlots: {customRender: 'buildIdStr'}},
        {title: '状态', dataIndex: 'status', width: 120, ellipsis: true, scopedSlots: {customRender: 'status'}},
        {title: '开始时间', dataIndex: 'startTime', customRender: (text) => {
          return parseTime(text);
        }, width: 180},
        {title: '结束时间', dataIndex: 'endTime', customRender: (text) => {
          return parseTime(text);
        }, width: 180},
        {title: '发布方式', dataIndex: 'releaseMethod', width: 100, ellipsis: true, scopedSlots: {customRender: 'releaseMethod'}},
        {title: '构建人', dataIndex: 'buildUser', /*width: 150,*/ ellipsis: true, scopedSlots: {customRender: 'buildUser'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 320, fixed: 'right'}
      ],
      statusMap: {
        1: '构建中',
        2: '构建成功',
        3: '构建失败',
        4: '发布中',
        5: '发布成功',
        6: '发布失败',
        7: '取消构建',
      }
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
    this.loadBuildList();
    this.loadData();
  },
  methods: {
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 135;
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
    handleFile(record){
      window.open("./build/download_file.html?logId="+record.id);
      // downloadBuildFile(record.id).then(blob => {
      //   const url = window.URL.createObjectURL(blob);
      //   let link = document.createElement('a');
      //   link.style.display = 'none';
      //   link.href = url;
      //   link.setAttribute('download', `${record.name}.log`);
      //   document.body.appendChild(link);
      //   link.click();
      // })
    },
    // 回滚
    handleRollback(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要回滚该构建历史记录么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 重新发布
          rollback(record.id).then((res) => {
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
    },
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.buildDataId,
        buildId: record.buildNumberId
      }
      this.buildLogVisible = true;
    },
    // 关闭日志对话框
    closeBuildLogModel() {
      this.handleFilter();
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
