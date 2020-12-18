<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="exportStack">导出堆栈信息</a-button>
      <a-button type="primary" @click="exportMem">导出内存信息</a-button>
      <a-button type="danger" @click="checkThread">查看线程</a-button>
    </div>
    <!-- 系统内存 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :scroll="{y: '100%'}" :pagination="false" bordered :rowKey="(record, index) => index">
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="group" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="lib" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="delUser" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
    </a-table>
    <!-- 端口信息 -->
  </div>
</template>
<script>
import { getInternalData } from '../../../../api/node-project';
export default {
  props: {
    node: {
      type: Object
    },
    project: {
      type: Object
    },
    copyId: {
      type: String
    }
  },
  data() {
    return {
      loading: false,
      list: [],
      columns: [
        {title: '项目名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '创建时间', dataIndex: 'createTime', width: 170, ellipsis: true, scopedSlots: {customRender: 'createTime'}},
        {title: '修改时间', dataIndex: 'modifyTime', width: 170, ellipsis: true, scopedSlots: {customRender: 'modifyTime'}},
        {title: '最后操作人', dataIndex: 'modifyUser', width: 150, ellipsis: true, scopedSlots: {customRender: 'modifyUser'}},
        {title: '运行状态', dataIndex: 'status', width: 100, ellipsis: true, scopedSlots: {customRender: 'status'}},
        {title: 'PID', dataIndex: 'pid', width: 100, ellipsis: true, scopedSlots: {customRender: 'pid'}},
        {title: '端口', dataIndex: 'port', width: 100, ellipsis: true, scopedSlots: {customRender: 'port'}}
      ],
    }
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      const params = {
        nodeId: this.node.id,
        tag: this.project.id,
        copyId: this.copyId
      }
      getInternalData(params).then(res => {
        console.log(res);
        this.loading = false;
      })
    },
    // 导出堆栈信息
    exportStack() {

    },
    // 导出内存信息
    exportMem() {

    },
    // 查看线程
    checkThread() {

    },
  }
}
</script>
<style scoped>
.filter {
  margin: 0 0 10px;
}
.ant-btn {
  margin-right: 10px;
}
</style>