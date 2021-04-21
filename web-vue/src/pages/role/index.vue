<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :style="{'max-height': tableHeight + 'px' }" :scroll="{x: 760, y: tableHeight - 60}" :pagination="false" bordered :rowKey="(record, index) => index">
      <template slot="operation" slot-scope="text, record">
        <a-button :disabled="fullscreeLoading" type="primary" @click="handlePermission(record)">动态</a-button>
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
      <div v-for="(val, key, index) in dynamicData" :key="index">
        <h3>{{ getDynamicNameById(key) }}</h3>
        <div v-for="item in val" :key="item.id">
          <a-checkbox :value="item.id" :disabled="item.disabled" :checked="item.checked" class="box-1" @change="changeCheckBox(item.id)">{{ item.title }}</a-checkbox><br/>
          <div v-for="child in item.children" :key="child.id">
            <a-checkbox :value="child.id" :disabled="child.disabled" :checked="child.checked" class="box-2" @change="changeCheckBox(child.id)">{{ child.title }}</a-checkbox><br/>
            <div v-for="ele in child.children" :key="ele.id">
              <a-checkbox :value="ele.id" :checked="ele.checked" class="box-3" @change="changeCheckBox(ele.id)">{{ ele.title }}</a-checkbox>
            </div>
          </div>
        </div>
        <a-divider />
      </div>
    </a-modal>
  </div>
</template>
<script>
import { getRoleList, getRoleFeature, editRole, deleteRole, getDynamicList, getRoleDynamicList, editRoleDynamic } from '../../api/role';
export default {
  data() {
    return {
      loading: false,
      tableHeight: '70vh',
      list: [],
      // 权限列表
      featureList: [],
      editRoleVisible: false,
      dynamicList: [],
      dynamicData: {},
      editDynamicVisible: false,
      fullscreeLoading: false,
      temp: {},
      // tree 选中的值
      checkedKeys: {},
      checkedDynamicKeys: [],
      replaceFields: {
        children: 'children',
        key: 'id',
        title: 'title'
      },
      columns: [
        {title: '角色名称', dataIndex: 'name', ellipsis: true, width: 150},
        {title: '授权人数', dataIndex: 'bindCount', ellipsis: true, width: 150},
        {title: '修改时间', dataIndex: 'updateTime', ellipsis: true, width: 150},
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
    this.calcTableHeight();
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
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 135;
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
      this.fullscreeLoading = true;
      this.$notification.success({
        message: '加载数据中，请稍候...',
        duration: 3
      });
      this.checkedDynamicKeys = [];
      this.temp = Object.assign({}, record);
      const res = await getDynamicList();
      if (res.code === 200) {
        this.dynamicList = res.data;
        this.dynamicList = [...this.dynamicList];
        this.loadDynamicData().then(() => {
          this.editDynamicVisible = true;
          this.fullscreeLoading = false;
        })
      }
    },
    // 加载动态数据
    loadDynamicData() {
      return new Promise((resolve) => {
        let count = 0;
        this.dynamicList.forEach(ele => {
          const params = {
            id: this.temp.id,
            dynamic: ele.id
          }
          getRoleDynamicList(params).then(res => {
            if (res.code === 200) {
              ele.dynamicList = res.data;
              this.dynamicList = [...this.dynamicList];
              if (res.data){
                this.doDynamicListParam(this.dynamicList);
              }
              count++;
              if (this.dynamicList.length === count) {
                resolve();
              }
            }
          })
        })
      })
    },
    // 编辑 dynamic
    handleEditDynamicOk() {
      let dynamic = JSON.parse(JSON.stringify(this.dynamicData));
      Object.keys(dynamic).forEach((key) => {
        let index = dynamic[key].findIndex(p => !p.disabled && !p.checked);
        while(index > -1) {
          dynamic[key].splice(index, 1);
          index = dynamic[key].findIndex(p => !p.disabled && !p.checked);
        }
        dynamic[key].forEach(ele => {
          if (ele.children) {
            ele.children.forEach(child => {
              index = child.children.findIndex(p => !p.disabled && !p.checked);
              while(index > -1) {
                child.children.splice(index, 1);
                index = child.children.findIndex(p => !p.disabled && !p.checked);
              }
            })
          }
        })
      })
      // 组装参数
      const params = {
        id: this.temp.id,
        dynamic: JSON.stringify(dynamic)
      }
      // 提交数据
      editRoleDynamic(params).then(res => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
            duration: 2
          });
          this.editDynamicVisible = false;
        }
      })
    },
    // 处理 dynamic dynamicList 参数
    doDynamicListParam(list) {
      list.forEach(ele => {
        if (!ele.parent) {
          this.dynamicData[ele.id] = [];
          if (ele.dynamicList) {
            ele.dynamicList.forEach(p => {
              let children = this.doChildrenParam(p);
              let temp = {
                id: p.id,
                title: p.title,
                checked: p.checked || false,
                disabled: (p.id.indexOf(':NODE') === 0 || p.id.indexOf('PROJECT') === 0) || false
              }
              if (children.length > 0) {
                temp.children = children;
              }
              this.dynamicData[ele.id].push(temp)
            })
          }
        }
      })
      this.dynamicData = {...this.dynamicData};
    },
    // 处理 dynamic children 参数
    doChildrenParam(element) {
      let children = [];
      // 判断 children 是否存在
      if (element.children && element.children.length > 0) {
        element.children.forEach(c => {
          let temp = {
            id: c.id,
            title: c.title,
            checked: c.checked || false,
            disabled: (c.id.indexOf(':NODE') === 0 || c.id.indexOf('PROJECT') === 0) || false
          }
          // 判断 children 是否存在
          let tempChildren = this.doChildrenParam(c);
          if (tempChildren.length > 0) {
            temp.children = tempChildren;
          }
          children.push(temp);
        })
      }
      return children;
    },
    // 根据 id 加载动态的名称
    getDynamicNameById(id) {
      const index = this.dynamicList.findIndex(ele => ele.id === id);
      return this.dynamicList[index].name;
    },
    // 选中 checkbox
    changeCheckBox(id) {
      Object.keys(this.dynamicData).forEach(key => {
        this.dynamicData[key].forEach(ele => {
          if (ele.id === id) {
            ele.checked = !ele.checked;
          }
          if (ele.children) {
            ele.children.forEach(child => {
              if (child.id === id) {
                child.checked = !child.checked;
              }
              if (child.children) {
                child.children.forEach(p => {
                  if (p.id === id) {
                    p.checked = !p.checked;
                  }
                })
              }
            })
          }
        })
      })
      this.dynamicData = {...this.dynamicData};
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
.box-1 {
  margin-left: 10px;
}
.box-2 {
  margin-left: 40px;
}
.box-3 {
  margin-left: 70px;
}
</style>