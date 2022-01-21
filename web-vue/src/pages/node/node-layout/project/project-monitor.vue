<template>
  <div class="node-full-content">
    <div ref="filter" class="filter">
      <a-space>
        <a-button type="primary" @click="exportStack">导出堆栈信息</a-button>
        <a-button type="primary" @click="exportMem">导出内存信息</a-button>
      </a-space>
    </div>
    <!-- 线程信息 -->
    <a-divider>线程信息</a-divider>
    <a-table :data-source="list0" :loading="loading0" :columns="columns0" :scroll="{ x: '80vh' }" :pagination="false" bordered :rowKey="(record, index) => index">
      <a-tooltip slot="isInNative" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text ? "是" : "否" }}</span>
      </a-tooltip>
      <a-tooltip slot="isSuspended" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text ? "是" : "否" }}</span>
      </a-tooltip>
    </a-table>
    <br />
    <!-- 系统内存 -->
    <a-divider>系统内存</a-divider>
    <a-table :data-source="list1" :loading="loading" :columns="columns1" :scroll="{ x: '80vh' }" :pagination="false" bordered :rowKey="(record, index) => index"> </a-table>
    <br />
    <!-- jvm 内存 -->
    <a-divider>JVM 内存</a-divider>
    <a-table :data-source="list3" :loading="loading" :columns="columns3" :scroll="{ x: '80vh' }" :pagination="false" bordered :rowKey="(record, index) => index"> </a-table>
    <br />
    <!-- 端口信息 -->
    <a-divider>端口信息</a-divider>
    <a-table :data-source="list2" :loading="loading" :columns="columns2" :scroll="{ x: '80vh' }" :pagination="false" bordered :rowKey="(record, index) => index"> </a-table>
  </div>
</template>
<script>
import { getInternalData, exportStack, exportRam, getThreadInfo } from "@/api/node-project";
export default {
  props: {
    node: {
      type: Object,
    },
    project: {
      type: Object,
    },
    copyId: {
      type: String,
    },
  },
  data() {
    return {
      loading0: false,
      loading: false,
      list0: [],
      list1: [],
      list2: [],
      list3: [],
      columns0: [
        { title: "线程 ID", dataIndex: "id", width: 150, ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: "线程名称", dataIndex: "name", width: 180, ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "线程状态", dataIndex: "status", width: 120, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "唤醒次数", dataIndex: "blockedCount", width: 100, ellipsis: true, scopedSlots: { customRender: "blockedCount" } },
        { title: "运行总时间(毫秒)", dataIndex: "blockedTime", width: 150, ellipsis: true, scopedSlots: { customRender: "blockedTime" } },
        { title: "阻塞次数", dataIndex: "waitedCount", width: 100, ellipsis: true, scopedSlots: { customRender: "waitedCount" } },
        { title: "阻塞总时间(毫秒)", dataIndex: "waitedTime", width: 150, ellipsis: true, scopedSlots: { customRender: "waitedTime" } },
        { title: "本地线程", dataIndex: "isInNative", width: 100, ellipsis: true, scopedSlots: { customRender: "isInNative" } },
        { title: "是否挂起", dataIndex: "isSuspended", width: 150, ellipsis: true, scopedSlots: { customRender: "isSuspended" } },
      ],
      columns1: [
        { title: "进程 ID", dataIndex: "pid", width: 150, ellipsis: true, scopedSlots: { customRender: "pid" } },
        { title: "进程名称", dataIndex: "command", width: 180, ellipsis: true, scopedSlots: { customRender: "command" } },
        { title: "所有者", dataIndex: "user", width: 180, ellipsis: true, scopedSlots: { customRender: "user" } },
        { title: "使用物理内存", dataIndex: "res", width: 150, ellipsis: true, scopedSlots: { customRender: "res" } },
        { title: "进程状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "占用 CPU", dataIndex: "cpu", width: 100, ellipsis: true, scopedSlots: { customRender: "cpu" } },
        { title: "占用物理内存", dataIndex: "mem", width: 150, ellipsis: true, scopedSlots: { customRender: "mem" } },
        { title: "时间总计", dataIndex: "time", width: 100, ellipsis: true, scopedSlots: { customRender: "time" } },
        { title: "优先级", dataIndex: "pr", width: 100, ellipsis: true, scopedSlots: { customRender: "pr" } },
        { title: "nice 值", dataIndex: "ni", width: 100, ellipsis: true, scopedSlots: { customRender: "ni" } },
        { title: "使用虚拟内存", dataIndex: "virt", width: 150, ellipsis: true, scopedSlots: { customRender: "virt" } },
        { title: "共享内存", dataIndex: "shr", width: 100, ellipsis: true, scopedSlots: { customRender: "shr" } },
      ],
      columns2: [
        { title: "进程 ID/项目名称", dataIndex: "name", width: 150, ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "连接协议", dataIndex: "protocol", width: 180, ellipsis: true, scopedSlots: { customRender: "protocol" } },
        { title: "本地地址", dataIndex: "local", width: 180, ellipsis: true, scopedSlots: { customRender: "local" } },
        { title: "远程地址", dataIndex: "foreign", width: 150, ellipsis: true, scopedSlots: { customRender: "foreign" } },
        { title: "状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "接收队列", dataIndex: "receive", width: 100, ellipsis: true, scopedSlots: { customRender: "receive" } },
        { title: "发送队列", dataIndex: "send", width: 100, ellipsis: true, scopedSlots: { customRender: "send" } },
      ],
      columns3: [
        { title: "已使用堆内存", dataIndex: "heapUsed", width: 150, ellipsis: true, scopedSlots: { customRender: "heapUsed" } },
        { title: "占用堆内存", dataIndex: "heapProportion", width: 150, ellipsis: true, scopedSlots: { customRender: "heapProportion" } },
        { title: "已分配堆内存", dataIndex: "heapCommitted", width: 150, ellipsis: true, scopedSlots: { customRender: "heapCommitted" } },
        { title: "已使用非堆内存", dataIndex: "nonHeapUsed", width: 150, ellipsis: true, scopedSlots: { customRender: "nonHeapUsed" } },
        { title: "占用非堆内存", dataIndex: "nonHeapProportion", width: 150, ellipsis: true, scopedSlots: { customRender: "nonHeapProportion" } },
        { title: "已分配非堆内存", dataIndex: "nonHeapCommitted", width: 150, ellipsis: true, scopedSlots: { customRender: "nonHeapCommitted" } },
        { title: "挂起的对象", dataIndex: "mount", width: 150, ellipsis: true, scopedSlots: { customRender: "mount" } },
      ],
    };
  },
  mounted() {
    this.loadData();
    this.loadThreadData();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      const params = {
        nodeId: this.node.id,
        tag: this.project.projectId,
        copyId: this.copyId,
      };
      getInternalData(params).then((res) => {
        if (res.code === 200) {
          this.list1.push(res.data.process);
          this.list2 = res.data.netstat;
          this.list3.push(res.data.beanMem);
        }
        this.loading = false;
      });
    },
    // 加载线程数据
    loadThreadData() {
      this.loading0 = true;
      const params = {
        nodeId: this.node.id,
        tag: this.project.id,
        copyId: this.copyId,
      };
      getThreadInfo(params).then((res) => {
        if (res.code === 200) {
          this.list0 = res.data.data;
        }
        this.loading0 = false;
      });
    },
    // 导出堆栈信息
    exportStack() {
      this.$notification.info({
        message: "正在下载，请稍等...",
      });
      // 请求参数
      const params = {
        nodeId: this.node.id,
        tag: this.project.id,
        copyId: this.copyId,
      };
      const fileName = `${this.project.id}_stack.txt`;
      // 请求接口拿到 blob
      exportStack(params).then((blob) => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement("a");
        link.style.display = "none";
        link.href = url;
        link.setAttribute("download", fileName);
        document.body.appendChild(link);
        link.click();
      });
    },
    // 导出内存信息
    exportMem() {
      this.$notification.info({
        message: "正在下载，请稍等...",
      });
      // 请求参数
      const params = {
        nodeId: this.node.id,
        tag: this.project.id,
        copyId: this.copyId,
      };
      const fileName = `${this.project.id}_ram.txt`;
      // 请求接口拿到 blob
      exportRam(params).then((blob) => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement("a");
        link.style.display = "none";
        link.href = url;
        link.setAttribute("download", fileName);
        document.body.appendChild(link);
        link.click();
      });
    },
  },
};
</script>
<style scoped>
.filter {
  margin: 0 0 10px;
}
</style>
