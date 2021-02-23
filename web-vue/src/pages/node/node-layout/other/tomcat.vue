<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="handleFilter">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="false" bordered rowKey="id" class="node-table" @expand="expand">
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="path" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleFile(record)">日志</a-button>
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="primary" @click="handleConsole(record)">上传 WAR 文件</a-button>
        <a-button type="danger" @click="handleMonitor(record)">停止</a-button>
        <a-button type="danger" @click="handleReplica(record)">重启</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
      <!-- 嵌套表格 -->
      <a-table slot="expandedRowRender" slot-scope="text" :scroll="{x: '80vw'}" :loading="childLoading" :columns="childColumns" :data-source="text.children"
        :pagination="false" :rowKey="(record, index) => record.path + index">
        <a-tooltip slot="path" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="status" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text === 'running' ? '运行中' : '未运行' }}</span>
        </a-tooltip>
        <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleFile(record)">管理</a-button>
        <a-button type="danger" @click="handleMonitor(record)">停止</a-button>
        <a-button type="danger" @click="handleReplica(record)">重启</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
      </a-table>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editTomcatVisible" title="编辑 Tomcat" @ok="handleEditTomcatOk" :maskClosable="false" width="700px">
      <a-form-model ref="editTomcatForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-model-item label="Tomcat 名称" prop="name">
          <a-input v-model="temp.name" placeholder="名称"/>
        </a-form-model-item>
        <a-form-model-item label="Tomcat 路径" prop="path">
          <a-input v-model="temp.path" placeholder="Tomcat 路径"/>
        </a-form-model-item>
        <a-form-model-item label="Tomcat 端口" prop="port">
          <a-input v-model="temp.port" placeholder="Tomcat 端口"/>
        </a-form-model-item>
        <a-form-model-item label="appBase 路径" prop="appBase">
          <a-input v-model="temp.appBase" placeholder="appBase 路径"/>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { getTomcatList, editTomcat, deleteTomcat, getTomcatProjectList } from '../../../../api/node-other';
export default {
  props: {
    node: {
      type: Object
    }
  },
  data() {
    return {
      childLoading: false,
      loading: false,
      list: [],
      temp: {},
      editTomcatVisible: false,
      columns: [
        {title: 'Tomcat 名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: 'Tomcat 路径', dataIndex: 'path', width: 150, ellipsis: true, scopedSlots: {customRender: 'path'}},
        {title: '修改时间', dataIndex: 'modifyTime', width: 180, ellipsis: true, scopedSlots: {customRender: 'modifyTime'}},
        {title: '最后操作人', dataIndex: 'modifyUser', width: 150, ellipsis: true, scopedSlots: {customRender: 'modifyUser'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 540}
      ],
      childColumns: [
        {title: '项目路径', dataIndex: 'path', width: 200, ellipsis: true, scopedSlots: {customRender: 'path'}},
        {title: '运行状态', dataIndex: 'status', width: 120, ellipsis: true, scopedSlots: {customRender: 'status'}},
        {title: 'Session 个数', dataIndex: 'session', width: 150},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}}
      ],
      rules: {
        name: [
          { required: true, message: 'Please input Tomcat name', trigger: 'blur' }
        ],
        path: [
          { required: true, message: 'Please input Tomcat path', trigger: 'blur' }
        ],
        port: [
          { required: true, message: 'Please input Tomcat port', trigger: 'blur' }
        ],
        appBase: [
          { required: true, message: 'Please input appBase path', trigger: 'blur' }
        ]
      }
    }
  },
  mounted() {
    this.handleFilter();
  },
  methods: {
    // 加载数据
    loadData() {
      this.list = [];
      this.loading = true;
      getTomcatList(this.node.id).then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 筛选
    handleFilter() {
      this.loadData();
    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.editTomcatVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign(record);
      this.editTomcatVisible = true;
    },
    // 提交 Tomcat 数据
    handleEditTomcatOk() {
       // 检验表单
      this.$refs['editTomcatForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.temp.nodeId = this.node.id;
        // 提交数据
        editTomcat(this.temp).then(res => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            this.$refs['editTomcatForm'].resetFields();
            this.editTomcatVisible = false;
            this.loadData();
          }
        })
      })
    },
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除 Tomcat 么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 组装参数
          const params = {
            nodeId: this.node.id,
            id: record.id
          }
          // 删除
          deleteTomcat(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              this.loadData();
            }
          })
        }
      });
    },
    // 展开行
    expand(expanded, record) {
      if (expanded) {
        // 请求节点状态数据
        this.childLoading = true;
        const params = {
          nodeId: this.node.id,
          id: record.id
        }
        getTomcatProjectList(params).then(res => {
          if (res.code === 200) {
            record.children = res.data;
          }
          this.childLoading = false;
        })
      }
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
.node-table {
  overflow-x: auto;
}
</style>