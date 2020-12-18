<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="exportStack">导出堆栈信息</a-button>
      <a-button type="primary" @click="exportMem">导出内存信息</a-button>
      <a-button type="danger" @click="checkThread">查看线程</a-button>
    </div>
    <!-- 系统内存 -->
    <a-table :data-source="list1" :loading="loading" :columns="columns1" :scroll="{x: '80vh'}" :pagination="false" bordered :rowKey="(record, index) => index">
    </a-table>
    <br/>
    <!-- 端口信息 -->
    <a-table :data-source="list2" :loading="loading" :columns="columns2" :scroll="{x: '80vh'}" :pagination="false" bordered :rowKey="(record, index) => index">
    </a-table>
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
      list1: [],
      list2: [],
      columns1: [
        {title: '进程 ID', dataIndex: 'pid', width: 150, ellipsis: true, scopedSlots: {customRender: 'pid'}},
        {title: '进程名称', dataIndex: 'command', width: 180, ellipsis: true, scopedSlots: {customRender: 'command'}},
        {title: '所有者', dataIndex: 'user', width: 180, ellipsis: true, scopedSlots: {customRender: 'user'}},
        {title: '使用物理内存', dataIndex: 'res', width: 150, ellipsis: true, scopedSlots: {customRender: 'res'}},
        {title: '进程状态', dataIndex: 'status', width: 100, ellipsis: true, scopedSlots: {customRender: 'status'}},
        {title: '占用 CPU', dataIndex: 'cpu', width: 100, ellipsis: true, scopedSlots: {customRender: 'cpu'}},
        {title: '占用物理内存', dataIndex: 'mem', width: 150, ellipsis: true, scopedSlots: {customRender: 'mem'}},
        {title: '时间总计', dataIndex: 'time', width: 100, ellipsis: true, scopedSlots: {customRender: 'time'}},
        {title: '优先级', dataIndex: 'pr', width: 100, ellipsis: true, scopedSlots: {customRender: 'pr'}},
        {title: 'nice 值', dataIndex: 'ni', width: 100, ellipsis: true, scopedSlots: {customRender: 'ni'}},
        {title: '使用虚拟内存', dataIndex: 'virt', width: 150, ellipsis: true, scopedSlots: {customRender: 'virt'}},
        {title: '共享内存', dataIndex: 'shr', width: 100, ellipsis: true, scopedSlots: {customRender: 'shr'}}
      ],
      columns2: [
        {title: '进程 ID/项目名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '连接协议', dataIndex: 'protocol', width: 180, ellipsis: true, scopedSlots: {customRender: 'protocol'}},
        {title: '本地地址', dataIndex: 'local', width: 180, ellipsis: true, scopedSlots: {customRender: 'local'}},
        {title: '远程地址', dataIndex: 'foreign', width: 150, ellipsis: true, scopedSlots: {customRender: 'foreign'}},
        {title: '状态', dataIndex: 'status', width: 100, ellipsis: true, scopedSlots: {customRender: 'status'}},
        {title: '接收队列', dataIndex: 'receive', width: 100, ellipsis: true, scopedSlots: {customRender: 'receive'}},
        {title: '发送队列', dataIndex: 'send', width: 100, ellipsis: true, scopedSlots: {customRender: 'send'}}
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
        if (res.code === 200) {
          this.list1.push(res.data.process);
          this.list2 = res.data.netstat;
        }
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