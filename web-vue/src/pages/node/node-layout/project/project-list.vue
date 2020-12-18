<template>
  <div>
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.group" allowClear placeholder="请选择分组"
        class="filter-item" @change="handleFilter">
        <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
      </a-select>
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="handleFilter">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :scroll="{x: '80vw', y: 500}" :pagination="false" bordered :rowKey="(record, index) => index">
      <a-switch slot="status" slot-scope="text" :checked="text" disabled checked-children="开" un-checked-children="关"/>
      <a-tooltip slot="port" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="primary" @click="handleFile(record)">文件</a-button>
        <a-button type="primary" @click="handleConsole(record)">控制台</a-button>
        <a-button type="primary" @click="handleMonitor(record)">监控</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editProjectVisible" width="800px" title="编辑项目" @ok="handleEditProjectOk" :maskClosable="false">
      <a-form-model ref="editProjectForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="项目 ID" prop="id">
          <a-input v-model="temp.id" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改"/>
        </a-form-model-item>
        <a-form-model-item label="项目名称" prop="name">
          <a-input v-model="temp.name" placeholder="项目名称"/>
        </a-form-model-item>
        <a-form-model-item label="运行方式" prop="runMode">
          <a-select v-model="temp.runMode" placeholder="请选择运行方式">
            <a-select-option v-for="runMode in runModeList" :key="runMode">{{ runMode }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="项目白名单路径" prop="whitelistDirectory">
          <a-select v-model="temp.whitelistDirectory" placeholder="请选择项目白名单路径">
            <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="项目文件夹" prop="lib">
          <a-input v-model="temp.lib" placeholder="项目存储的文件夹，jar 包存放的文件夹"/>
        </a-form-model-item>
        <a-form-model-item label="分组名称" prop="group">
          <a-row>
            <a-col :span="18">
              <a-select v-model="temp.group" placeholder="可手动输入">
                <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
              </a-select>
            </a-col>
            <a-col :span="6">
              <a-popover v-model="addGroupvisible" title="添加分组" trigger="click">
                <template slot="content">
                  <a-row>
                    <a-col :span="18">
                      <a-input v-model="temp.tempGroup" placeholder="分组名称"/>
                    </a-col>
                    <a-col :span="6">
                      <a-button type="primary" @click="handleAddGroup">确认</a-button>
                    </a-col>
                  </a-row>
                </template>
                <a-button type="primary" class="btn-add">添加分组</a-button>
              </a-popover>
            </a-col>
          </a-row>
        </a-form-model-item>
        <a-form-model-item label="JDK" prop="jdkId">
          <a-select v-model="temp.jdkId" placeholder="请选择 JDK">
            <a-select-option v-for="jdk in jdkList" :key="jdk.id">{{ jdk.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="Main Class" prop="mainClass" v-show="temp.runMode !== 'Jar'">
          <a-input v-model="temp.mainClass" placeholder="程序运行的 main 类(jar 模式运行可以不填)"/>
        </a-form-model-item>
        <a-form-model-item label="JavaExtDirsCp" prop="javaExtDirsCp" v-show="temp.runMode === 'JavaExtDirsCp'">
          <a-input v-model="temp.javaExtDirsCp" placeholder="-Dext.dirs=xxx: -cp xx  填写【xxx:xx】"/>
        </a-form-model-item>
        <a-form-model-item label="JVM 参数" prop="jvm">
          <a-textarea v-model="temp.jvm" :auto-size="{ minRows: 3, maxRows: 3 }" placeholder="jvm参数,非必填.如：-Xmin=512m -Xmax=512m"/>
        </a-form-model-item>
        <a-form-model-item label="args参数" prop="args">
          <a-textarea v-model="temp.args" :auto-size="{ minRows: 3, maxRows: 3 }" placeholder="Main 函数 args 参数，非必填. 如：--service.port=8080"/>
        </a-form-model-item>
        <a-form-model-item label="WebHooks" prop="token">
          <a-input v-model="temp.token" placeholder="关闭程序时自动请求,非必填，GET请求"/>
        </a-form-model-item>
        <a-form-model-item v-show="temp.type === 'edit'" label="日志路径" prop="log">
          <a-alert :message="temp.log" type="success" />
        </a-form-model-item>
        <a-form-model-item v-show="temp.type === 'edit'" label="运行命令" prop="runCommand">
          <a-alert :message="temp.runCommand" type="success" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 项目文件组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw"
      :visible="drawerFileVisible" @close="onFileClose">
      <file v-if="drawerFileVisible" :node="node" :project="temp" />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw"
      :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :node="node" :project="temp" />
    </a-drawer>
    <!-- 项目监控组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw"
      :visible="drawerMonitorVisible" @close="onMonitorClose">
      <monitor v-if="drawerMonitorVisible" :node="node" :project="temp" />
    </a-drawer>
  </div>
</template>
<script>
import File from './project-file';
import Console from './project-console';
import Monitor from './project-monitor';
import { getJdkList, getRuningProjectInfo, getProjectById, deleteProject, getProjectList, getPorjectGroupList, getProjectAccessList, editProject } from '../../../../api/node-project';
export default {
  props: {
    node: {
      type: Object
    }
  },
  components: {
    File,
    Console,
    Monitor
  },
  data() {
    return {
      loading: false,
      listQuery: {},
      groupList: [],
      accessList: [],
      jdkList: [],
      runModeList: [
        'ClassPath',
        'Jar',
        'JarWar',
        'JavaExtDirsCp',
        // 'File'
      ],
      list: [],
      temp: {},
      editProjectVisible: false,
      drawerTitle: '',
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      drawerMonitorVisible: false,
      addGroupvisible: false,
      columns: [
        {title: '项目名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '创建时间', dataIndex: 'createTime', width: 170, ellipsis: true, scopedSlots: {customRender: 'createTime'}},
        {title: '修改时间', dataIndex: 'modifyTime', width: 170, ellipsis: true, scopedSlots: {customRender: 'modifyTime'}},
        {title: '最后操作人', dataIndex: 'modifyUser', width: 150, ellipsis: true, scopedSlots: {customRender: 'modifyUser'}},
        {title: '运行状态', dataIndex: 'status', width: 100, ellipsis: true, scopedSlots: {customRender: 'status'}},
        {title: 'PID', dataIndex: 'pid', width: 100, ellipsis: true, scopedSlots: {customRender: 'pid'}},
        {title: '端口', dataIndex: 'port', width: 100, ellipsis: true, scopedSlots: {customRender: 'port'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 420}
      ],
      rules: {
        id: [
          { required: true, message: 'Please input project id', trigger: 'blur' }
        ],
        name: [
          { required: true, message: 'Please input project name', trigger: 'blur' }
        ],
        runMode: [
          { required: true, message: 'Please select project runMode', trigger: 'blur' }
        ],
        whitelistDirectory: [
          { required: true, message: 'Please select project access path', trigger: 'blur' }
        ],
        lib: [
          { required: true, message: 'Please input project lib', trigger: 'blur' }
        ]
      }
    }
  },
  mounted() {
    this.loadGroupList();
    this.loadAccesList();
    this.loadJdkList();
    this.handleFilter();
  },
  methods: {
    // 加载分组列表
    loadGroupList() {
      getPorjectGroupList(this.node.id).then(res => {
        if (res.code === 200) {
          this.groupList = res.data;
        }
      })
    },
    // 加载项目白名单列表
    loadAccesList() {
      getProjectAccessList(this.node.id).then(res => {
        if (res.code === 200) {
          this.accessList = res.data;
        }
      })
    },
    // 加载 JDK 列表
    loadJdkList() {
      getJdkList(this.node.id).then(res => {
        if (res.code === 200) {
          this.jdkList = res.data;
        }
      })
    },
    // 加载数据
    async loadData() {
      this.loading = true;
      const params = {
        nodeId: this.node.id,
        group: this.listQuery.group
      }
      const res1 = await getProjectList(params);
      if (res1.code === 200) {
        this.list = res1.data;
        const ids = [];
        res1.data.forEach(element => {
          ids.push(element.id);
        });
        // 如果 ids 有数据就继续请求
        if (ids.length > 0) {
          const tempParams = {
            nodeId: this.node.id,
            ids: JSON.stringify(ids)
          }
          const res2 = await getRuningProjectInfo(tempParams);
          if (res2.code === 200) {
            this.list.forEach(element => {
              if (res2.data[element.id]) {
                element.port = res2.data[element.id].port;
                element.pid = res2.data[element.id].pid;
              }
            })
          }
        }
      }
      this.loading = false;
    },
    // 筛选
    handleFilter() {
      this.loadData();
    },
    // 添加
    handleAdd() {
      // this.temp = {type: 'add'};
      this.editProjectVisible = true;
    },
    // 编辑
    handleEdit(record) {
      const params = {
        id: record.id,
        nodeId: this.node.id
      }
      getProjectById(params).then(res => {
        if (res.code === 200) {
          this.temp = res.data;
          // this.temp.type = 'edit';
          this.editProjectVisible = true;
        }
      })
    },
    // 提交
    handleEditProjectOk() {
      // 检验表单
      this.$refs['editProjectForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        const params = {
          ...this.temp,
          nodeId: this.node.id
        }
        editProject(params).then(res => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            this.$refs['editProjectForm'].resetFields();
            this.editProjectVisible = false;
            this.handleFilter();
          }
        })
      })
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `文件管理(${this.temp.name})`
      this.drawerFileVisible = true;
    },
    // 关闭文件管理对话框
    onFileClose() {
      this.drawerFileVisible = false;
    },
    // 控制台
    handleConsole(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `控制台(${this.temp.name})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false;
      this.handleFilter();
    },
    // 监控
    handleMonitor(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `监控(${this.temp.name})`;
      this.drawerMonitorVisible = true;
    },
    // 关闭监控
    onMonitorClose() {
      this.drawerMonitorVisible = false;
    },
    // 删除 
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除项目么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          const params = {
            nodeId: this.node.id,
            id: record.id
          }
          deleteProject(params).then((res) => {
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
    // 添加分组
    handleAddGroup() {
      // 添加到分组列表
      if (this.groupList.indexOf(this.temp.tempGroup) === -1) {
        this.groupList.push(this.temp.tempGroup);
      }
      this.temp.tempGroup = '';
      this.$notification.success({
        message: '添加成功',
        duration: 2
      });
      this.addGroupvisible = false;
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