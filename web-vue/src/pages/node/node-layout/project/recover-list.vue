<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      :loading="loading"
      :columns="columns"
      :style="{ 'max-height': tableHeight + 'px' }"
      :scroll="{ x: 1040, y: tableHeight - 60 }"
      :pagination="false"
      bordered
      :rowKey="(record, index) => index"
    >
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
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleDetail(record)">{{ $t('common.more') }}</a-button>
      </template>
    </a-table>
    <!-- 详情区 -->
    <a-modal destroyOnClose v-model="detailVisible" width="600px" :title=this.$t('common.moreInf') :footer="null">
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
import { getRecoverList } from "../../../../api/node-project";
export default {
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      loading: false,
      tableHeight: "70vh",
      list: [],
      temp: {},
      detailData: [],
      detailVisible: false,
      columns: [
        { title: this.$t('common.projectName'), dataIndex: "nodeProjectInfoModel.name", width: 150, ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: this.$t('common.projectId'), dataIndex: "nodeProjectInfoModel.id", width: 150, ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: this.$t('common.group'), dataIndex: "nodeProjectInfoModel.group", width: 150, ellipsis: true, scopedSlots: { customRender: "group" } },
        { title: this.$t('common.proDir'), dataIndex: "nodeProjectInfoModel.lib", width: 150, ellipsis: true, scopedSlots: { customRender: "lib" } },
        { title: this.$t('common.delete')+this.$t('common.time'), dataIndex: "delTime", width: 180, ellipsis: true, scopedSlots: { customRender: "delTime" } },
        { title: this.$t('common.operator'), dataIndex: "delUser", width: 150, ellipsis: true, scopedSlots: { customRender: "delUser" } },
        { title: this.$t('common.operation'), dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 100 },
      ],
    };
  },
  mounted() {
    this.calcTableHeight();
    this.loadRecoverList();
  },
  methods: {
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - 120;
      });
    },
    // 加载数据
    loadRecoverList() {
      this.loading = true;
      getRecoverList(this.node.id).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      });
    },
    // 详情
    handleDetail(record) {
      this.detailData = [];
      this.detailVisible = true;
      this.temp = Object.assign({}, record);
      this.detailData.push({
        title: this.$t('common.proInfo'),
        description: `${this.$t('common.projectName')}: ${this.temp.nodeProjectInfoModel.name} | ${this.$t('common.projectID')} ${this.temp.nodeProjectInfoModel.id} | ${this.$t('common.group')} ${this.temp.nodeProjectInfoModel.group}`,
      });
      this.detailData.push({ title: this.$t('common.projectCata'), description: this.temp.nodeProjectInfoModel.lib });
      this.detailData.push({ title: "mainClass", description: this.temp.nodeProjectInfoModel.mainClass });
      this.detailData.push({ title: this.$t('common.logCatalog'), description: this.temp.nodeProjectInfoModel.log });
      this.detailData.push({ title: this.$t('node.node_layout.project.recover_list.JVMParam'), description: this.temp.nodeProjectInfoModel.jvm });
      this.detailData.push({ title: this.$t('node.node_layout.project.recover_list.argsArg'), description: this.temp.nodeProjectInfoModel.args });
      this.detailData.push({ title: "WebHooks", description: this.temp.nodeProjectInfoModel.token });
      this.detailData.push({ title: this.$t('node.node_layout.project.recover_list.buildSig'), description: this.temp.nodeProjectInfoModel.buildTag });
    },
  },
};
</script>
