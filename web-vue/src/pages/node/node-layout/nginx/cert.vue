<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleAdd">导入证书</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :scroll="{x: '80vw', y: 500}" :pagination="false" bordered :rowKey="(record, index) => index">
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="domain" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="primary" @click="handleEdit(record)">导出</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editCertVisible" title="编辑 Cert" @ok="handleEditJdkOk" :maskClosable="false">
      <a-form-model ref="editCertForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="证书 ID" prop="id">
          <a-input v-model="temp.name" placeholder="证书 ID"/>
        </a-form-model-item>
        <a-form-model-item label="证书名称" prop="name">
          <a-input v-model="temp.name" placeholder="证书名称"/>
        </a-form-model-item>
        <a-form-model-item label="证书路径" prop="path">
          <a-input v-model="temp.path" placeholder="证书路径"/>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { getCertWhiteList, getCertList } from '../../../../api/node-nginx';
export default {
  props: {
    node: {
      type: Object
    }
  },
  data() {
    return {
      loading: false,
      whiteList: [],
      list: [],
      temp: {},
      editCertVisible: false,
      columns: [
        {title: 'ID', dataIndex: 'id', width: 150, ellipsis: true, scopedSlots: {customRender: 'id'}},
        {title: '名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '域名', dataIndex: 'domain', width: 170, ellipsis: true, scopedSlots: {customRender: 'domain'}},
        {title: '生效时间', dataIndex: 'effectiveTime', width: 180, ellipsis: true, scopedSlots: {customRender: 'effectiveTime'}},
        {title: '到期时间', dataIndex: 'expirationTime', width: 180, ellipsis: true, scopedSlots: {customRender: 'expirationTime'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 300}
      ],
    }
  },
  created() {
    this.loadData();
    this.loadCertWhiteList();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      const params = {
        nodeId: this.node.id
      }
      getCertList(params).then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 加载 cert 白名单
    loadCertWhiteList() {
      const params = {
        nodeId: this.node.id
      }
      getCertWhiteList(params).then(res => {
        if (res.code === 200) {
          this.whiteList = res.data;
        }
      })
    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.editCertVisible = true;
    },
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
</style>