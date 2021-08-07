<template>
  <div>
    <!-- 搜索区 -->
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.repoType" allowClear placeholder="请选择仓库类型"
        class="filter-item" @change="handleFilter">
        <a-select-option :value="'0'">GIT</a-select-option>
        <a-select-option :value="'1'">SVN</a-select-option>
      </a-select>
      <a-button type="primary" @click="handleFilter">搜索</a-button>
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 表格 -->
    <a-table :loading="loading" :columns="columns" :data-source="list" :style="{'max-height': tableHeight + 'px' }"
      :scroll="{x: 970, y: tableHeight - 120}" bordered rowKey="id" :pagination="pagination">
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="gitUrl" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="repoType" slot-scope="text">
          <span v-if="text === 0">GIT</span>
          <span v-else-if="text === 1">SVN</span>
          <span v-else>未知</span>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editVisible" title="编辑仓库" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editRoleForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item label="仓库名称" prop="name">
          <a-input v-model="temp.name" placeholder="仓库名称"/>
        </a-form-model-item>
        <a-form-model-item label="仓库地址" prop="gitUrl">
          <a-input-group compact>
            <a-select style="width: 20%" v-model="temp.repoType" name="repoType" placeholder="仓库类型">
              <a-select-option :value="0">GIT</a-select-option>
              <a-select-option :value="1">SVN</a-select-option>
            </a-select>
            <a-input style="width: 80%" v-model="temp.gitUrl" placeholder="仓库地址"/>
          </a-input-group>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { getRepositoryList } from '../../api/repository';
import {parseTime} from '../../utils/time';

export default {
  components: {
  },
  data() {
    return {
      loading: false,
      listQuery: {
        page: 1,
        limit: 20
      },
      tableHeight: '70vh',
      list: [],
      total: 0,
      temp: {},
      editVisible: false,
      columns: [
        {title: '仓库名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {
          title: '仓库地址',
          dataIndex: 'gitUrl',
          width: 300,
          ellipsis: true,
          scopedSlots: {customRender: 'gitUrl'}
        },
        {
          title: '仓库类型',
          dataIndex: 'repoType',
          width: 100,
          ellipsis: true,
          scopedSlots: {customRender: 'repoType'}
        },
        {
          title: '修改时间', dataIndex: 'modifyTime', customRender: (text) => {
            if (!text) {
              return '';
            }
            return parseTime(text);
          }, width: 180
        },
        {
            title: '操作',
            dataIndex: 'operation',
            width: 240,
            scopedSlots: {customRender: 'operation'},
            align: 'left'
        }
      ],
      rules: {
          name: [
              {required: true, message: 'Please input build name', trigger: 'blur'}
          ],
          gitUrl: [
              {required: true, message: 'Please input git url', trigger: 'blur'}
          ]
        }
      }
    },
    computed: {
      // 分页
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
    watch: {
      getGuideFlag() {
        this.introGuide();
      }
    },
    created() {
      this.calcTableHeight();
      this.handleFilter();
    },
    methods: {
      // 计算表格高度
      calcTableHeight() {
        this.$nextTick(() => {
          this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 135;
        })
      },
      // 加载数据
      loadData() {
        this.list = [];
        this.loading = true;
        getRepositoryList(this.listQuery).then(res => {
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
        this.temp = {
          repoType: 0
        };
        this.editVisible = true;
      },
      // 修改
      handleEdit(record) {
          this.temp = Object.assign(record);
          this.editBuildVisible = true;
      },
      // 提交节点数据
      handleEditOk() {
        // 检验表单
        this.$refs['editBuildForm'].validate((valid) => {
          if (!valid) {
              return false;
          }
          // 提交数据
          // editBuild(this.temp).then(res => {
          //     if (res.code === 200) {
          //         // 成功
          //         this.$notification.success({
          //             message: res.msg,
          //             duration: 2
          //         });
          //         this.$refs['editBuildForm'].resetFields();
          //         this.editBuildVisible = false;
          //         this.handleFilter();
          //     }
          // })
        })
    },
    // 删除
    handleDelete(record) {
      console.log(record)
      this.$confirm({
        title: '系统提示',
        content: '真的要删除构建信息么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          // deleteBuild(record.id).then((res) => {
          //   if (res.code === 200) {
          //     this.$notification.success({
          //       message: res.msg,
          //       duration: 2
          //     });
          //     this.loadData();
          //   }
          // })
          }
        }
      );
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

.btn-add {
    margin-left: 10px;
    margin-right: 0;
}
</style>
