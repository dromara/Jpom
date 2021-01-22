<template>
  <div>
    <div class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handlePermission(record)">动态</a-button>
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
            :replaceFields="replaceFields" @check="checkNode"/>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 动态区 -->
    <a-modal v-model="editDynamicVisible" title="角色动态" @ok="handleEditDynamicOk" :maskClosable="false">
      <a-collapse @change="changeCollapse">
          <a-collapse-panel v-for="item in dynamicList.filter(ele => !ele.parent)" :key="item.id" :header="item.name">
            <a-tree checkStrictly checkable defaultExpandAll :selectable="false"
              :tree-data="item.dynamicList" 
              :replaceFields="replaceFields" @check="checkDynamicNode"/>
          </a-collapse-panel>
      </a-collapse>
    </a-modal>
  </div>
</template>
<script>
import { getRoleList, getRoleFeature, editRole, deleteRole, getDynamicList, getRoleDynamicList } from '../../api/role';
export default {
  data() {
    return {
      loading: false,
      list: [],
      // 权限列表
      featureList: [],
      editRoleVisible: false,
      dynamicList: [],
      editDynamicVisible: false,
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
    // 选中角色事件
    checkNode(checkedKeys, e) {
      const checked = e.checked;
      const node = e.node.dataRef;
      // 如果选中父节点，子节点默认选中
      if (checked && node.children) {
        node.children.forEach(ele => {
          this.checkedKeys.checked.push(ele.id);
        })
      }
      // 如果取消选中父节点，子节点也取消选中
      if (!checked && node.children) {
        node.children.forEach(ele => {
          const index = this.checkedKeys.checked.findIndex(id => ele.id === id);
          this.checkedKeys.checked.splice(index, 1);
        })
      }
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
    },
    // 分配权限
    async handlePermission(record) {
      this.temp = Object.assign({}, record);
      const res = await getDynamicList();
      if (res.code === 200) {
        this.dynamicList = res.data;
        this.dynamicList = [...this.dynamicList];
        this.editDynamicVisible = true;
      }
    },
    // 切换面板
    changeCollapse(keys) {
      keys.forEach(key => {
        const index = this.dynamicList.findIndex(p => p.id === key);
        // load = true 表示已经加载过了
        if (!this.dynamicList[index].load) {
          const params = {
            id: this.temp.id,
            dynamic: key
          }
          getRoleDynamicList(params).then(res => {
            if (res.code === 200) {
              // 禁用 id = PROJECT
              res.data.forEach(p => {
                if (p.children) {
                  p.children.forEach(ele => {
                    if (ele.id === 'PROJECT') {
                      ele.disabled = true;
                    }
                  })
                }
              })
              this.dynamicList[index].dynamicList = res.data;
              this.dynamicList[index].load = true;
              this.dynamicList = [...this.dynamicList];
            }
          })
        }
      })
    },
    // 选择 dynamic
    checkDynamicNode(checkedKeys, e) {
      console.log(checkedKeys)
      // const checked = e.checked;
      const node = e.node.dataRef;
      node.checkedKeys = checkedKeys;
      // // 如果选中父节点，子节点默认选中
      // if (checked && node.children) {
      //   node.children.forEach(ele => {
      //     if (!node.checkedKeys) {
      //       node.checkedKeys = {};
      //     }
      //     if (!node.checkedKeys.checked) {
      //       node.checkedKeys.checked = [];
      //     }
      //     node.checkedKeys.checked.push(ele.id);
      //   })
      // }
      // // 如果取消选中父节点，子节点也取消选中
      // if (!checked && node.children) {
      //   node.children.forEach(ele => {
      //     const index = node.checkedKeys.checked.findIndex(id => ele.id === id);
      //     node.checkedKeys.checked.splice(index, 1);
      //   })
      // }
    },
    // 编辑 dynamic
    handleEditDynamicOk() {
      console.log(this.dynamicList);
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