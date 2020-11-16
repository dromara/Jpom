<template>
  <div>
    <div class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary">动态</a-button>
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editRoleVisible" title="编辑角色" @ok="handleEditRoleOk" :maskClosable="false">
      <a-form-model ref="editRoleForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="角色名称" prop="name">
          <a-input v-model="temp.name" placeholder="角色名称"/>
        </a-form-model-item>
        <a-form-model-item label="权限" prop="feature" class="feature">
          <a-tree v-model="checkedKeys" 
            checkStrictly checkable defaultExpandAll :selectable="false" :tree-data="featureList" 
            :replaceFields="replaceFields" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { getRoleList, getRoleFeature, editRole, deleteRole } from '../../api/role';
export default {
  data() {
    return {
      loading: false,
      list: [],
      // 权限列表
      featureList: [],
      editRoleVisible: false,
      temp: {},
      // tree 选中的值
      checkedKeys: {},
      replaceFields: {
        children: 'children',
        key: 'id',
        title: 'title'
      },
      columns: [
        {title: '角色名称', dataIndex: 'name'},
        {title: '授权人数', dataIndex: 'bindCount'},
        {title: '修改时间', dataIndex: 'updateTime'},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: '300px'}
      ],
      // 表单校验规则
      rules: {
        name: [
          { required: true, message: 'Please input role name', trigger: 'blur' }
        ],
      }
    }
  },
  created() {
    this.loadData();
    this.loadRoleFeature();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      getRoleList().then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 加载角色权限
    loadRoleFeature() {
      getRoleFeature().then(res => {
        if (res.code === 200) {
          this.featureList = res.data;
        }
      })
    },
    // 添加角色
    handleAdd() {
      this.temp = {};
      this.checkedKeys = {};
      this.editRoleVisible = true;
    },
    // 编辑角色
    async handleEdit(record) {
      this.temp = {};
      // 请求数据
      const res = await getRoleFeature(record.id);
      // 设置选中的 key
      this.checkedKeys = {checked: [], halfChecked: []};
      res.data.forEach(feature => {
        if (feature.children.length > 0) {
          let countChecked = 0;
          feature.children.forEach(child => {
            if (child.checked) {
              this.checkedKeys.checked.push(child.id);
              countChecked ++;
            }
          })
          // 判断是全选还是半选
          if (countChecked > 0) {
            if (countChecked === feature.children.length) {
              this.checkedKeys.checked.push(feature.id);
            } else {
              this.checkedKeys.halfChecked.push(feature.id);
            }
          }
        }
      });
      this.temp.id = record.id;
      this.temp.name = record.name;
      // 显示对话框
      this.editRoleVisible = true;
    },
    // 提交角色数据
    handleEditRoleOk() {
      // 检验表单
      this.$refs['editRoleForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 判断是否选择了权限
        if (this.checkedKeys.length === 0) {
          this.$notification.error({
            message: '请选择权限',
            duration: 2
          });
          return false;
        }
        // 遍历数组匹配选中的值
        let checkedList = [];
        this.featureList.forEach(feature => {
          if (this.checkedKeys.checked.indexOf(feature.id) > -1 || this.checkedKeys.halfChecked.indexOf(feature.id) > -1) {
            const temp = {
              ...feature,
              children: []
            };
            if (feature.children.length > 0) {
              feature.children.forEach(child => {
                if (this.checkedKeys.checked.indexOf(child.id) > -1 || this.checkedKeys.halfChecked.indexOf(child.id) > -1) {
                  temp.children.push(child);
                }
              })
            }
            checkedList.push(temp);
          }
        })
        this.temp.feature = JSON.stringify(checkedList);
        // 提交数据
        editRole(this.temp).then(res => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            this.$refs['editRoleForm'].resetFields();
            this.editRoleVisible = false;
            this.loadData();
          }
        })
      })
    },
    // 删除角色
    handleDelete(record) {
      console.log(record);
      this.$confirm({
        title: '系统提示',
        content: '真的要删除角色么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteRole(record.id).then((res) => {
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
    }
  }
}
</script>
<style scoped>
.filter {
  margin-bottom: 10px;
}
.feature {
  max-height: 400px;
  overflow-y: auto;
}
.ant-btn {
  margin-right: 10px;
}
</style>