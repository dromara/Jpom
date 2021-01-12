<template>
  <div>
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.group" allowClear placeholder="请选择分组"
        class="filter-item" @change="handleFilter">
        <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
      </a-select>
      <a-button type="primary" @click="handleFilter">搜索</a-button>
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 表格 -->
    <a-table :loading="loading" :columns="columns" :data-source="list" :scroll="{x: '80vw'}" bordered rowKey="id" :pagination="false">
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="branchName" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="status" slot-scope="text">
        <span v-if="text === 0">未构建</span>
        <span v-else-if="text === 1">构建中</span>
        <span v-else-if="text === 2">构建成功</span>
        <span v-else-if="text === 3">构建失败</span>
        <span v-else-if="text === 4">发布中</span>
        <span v-else-if="text === 5">发布成功</span>
        <span v-else-if="text === 6">发布失败</span>
        <span v-else-if="text === 7">取消构建</span>
        <span v-else>未知状态</span>
      </template>
      <a-tooltip slot="buildIdStr" slot-scope="text, record" placement="topLeft" :title="text + ' ( 点击查看日志 ) '">
        <span v-if="record.buildId <= 0"></span>
        <a-tag v-else color="#108ee9" @click="handleBuildLog(record)">{{ text }}</a-tag>
      </a-tooltip>
      <a-tooltip slot="modifyUser" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="primary" @click="handleTrigger(record)">触发器</a-button>
        <a-button type="danger" v-if="record.status === 1 || record.status === 4" @click="handleStopBuild(record)">停止</a-button>
        <a-button type="primary" v-else @click="handleStartBuild(record)">构建</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
        <a-button type="danger" :disabled="!record.sourceExist" @click="handleClear(record)">清除构建</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editBuildVisible" title="编辑节点" @ok="handleEditBuildOk" :maskClosable="false">
      <a-form-model ref="editBuildForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="temp.name" placeholder="名称"/>
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
        <a-form-model-item label="仓库地址" prop="gitUrl">
          <a-input v-model="temp.gitUrl" placeholder="仓库地址"/>
        </a-form-model-item>
        <a-form-model-item label="仓库类型" prop="repoType">
          <a-radio-group v-model="temp.repoType" name="repoType">
            <a-radio :value="0">GIT</a-radio>
            <a-radio :value="1">SVN</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="登录用户" prop="userName">
          <a-input v-model="temp.userName" placeholder="登录用户"/>
        </a-form-model-item>
        <a-form-model-item label="登录密码" prop="password">
          <a-input-password v-model="temp.password" placeholder="登录密码"/>
        </a-form-model-item>
        <a-form-model-item label="分支" prop="branchName">
          <a-row>
            <a-col :span="18">
              <a-select v-model="temp.branchName" placeholder="请先填写仓库地址和账号信息">
                <a-select-option v-for="branch in branchList" :key="branch">{{ branch }}</a-select-option>
              </a-select>
            </a-col>
            <a-col :span="6">
              <a-button type="primary" class="btn-add" @click="loadBranchList">获取分支</a-button>
            </a-col>
          </a-row>
        </a-form-model-item>
        <a-form-model-item label="构建命令" prop="script">
          <a-input v-model="temp.script" type="textarea" :rows="3" style="resize: none;" placeholder="构建执行的命令，如：mvn clean package"/>
        </a-form-model-item>
        <a-form-model-item label="产物目录" prop="resultDirFile">
          <a-input v-model="temp.resultDirFile" placeholder="构建产物目录，相对路径"/>
        </a-form-model-item>
        <a-form-model-item label="发布操作" prop="releaseMethod">
          <a-radio-group v-model="temp.releaseMethod" name="releaseMethod">
            <a-radio :value="0">不发布</a-radio>
            <a-radio :value="1">节点分发</a-radio>
            <a-radio :value="2">项目</a-radio>
            <a-radio :value="3">SSH</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <!-- 节点 -->

        <!-- 项目 -->
        <a-form-model-item v-if="temp.releaseMethod === 2" label="发布项目" prop="releaseMethodDataId">
          <a-cascader v-model="temp.releaseMethodDataIdList" :options="cascaderList" placeholder="Please select" @change="onChangeProject" />
        </a-form-model-item>
        <a-form-model-item v-if="temp.releaseMethod === 2" label="发布后操作" prop="afterOpt">
          <a-select v-model="temp.afterOpt" placeholder="请选择发布后操作">
            <a-select-option :key="0">不做任何操作</a-select-option>
            <a-select-option :key="1">并发重启</a-select-option>
            <a-select-option :key="2">完整顺序重启(有重启失败将结束本次)</a-select-option>
            <a-select-option :key="3">顺序重启(有重启失败将继续)</a-select-option>
          </a-select>
        </a-form-model-item>
        <!-- SSH -->
        <a-form-model-item v-if="temp.releaseMethod === 3" label="SSH" prop="releaseMethodDataId">
          <a-select v-model="temp.releaseMethodDataId_3" placeholder="请先填写仓库地址和账号信息">
            <a-select-option v-for="ssh in sshList" :key="ssh.id">{{ ssh.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item v-if="temp.releaseMethod === 3" label="发布目录" prop="releasePath">
          <a-input v-model="temp.releasePath" placeholder="发布目录"/>
        </a-form-model-item>
        <a-form-model-item v-if="temp.releaseMethod === 3" label="发布命令" prop="releaseCommand">
          <a-input v-model="temp.releaseCommand" type="textarea" :rows="3" style="resize: none;" placeholder="发布执行的命令，如：mvn clean package"/>
        </a-form-model-item>
        <a-form-model-item v-if="temp.releaseMethod === 2 || temp.releaseMethod === 3" label="清空发布" prop="clearOld">
          <a-switch v-model="temp.clearOld" checked-children="是" un-checked-children="否"/>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 触发器 -->
    <a-modal v-model="triggerVisible" title="触发器" :footer="null" :maskClosable="false">
      <a-form-model ref="editTriggerForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-model-item label="触发器地址" prop="triggerBuildUrl">
          <a-input v-model="temp.triggerBuildUrl" type="textarea" readOnly :rows="3" style="resize: none;" placeholder="触发器地址"/>
        </a-form-model-item>
        <a-row>
          <a-col :span="6"></a-col>
          <a-col :span="16">
            <a-button type="primary" class="btn-add" @click="resetTrigger">重置</a-button>
          </a-col>
        </a-row>
      </a-form-model>
    </a-modal>
    <!-- 构建日志 -->
    <a-modal v-model="buildLogVisible" title="构建日志" :footer="null" :maskClosable="false" @cancel="closeBuildLogModel">
      <build-log v-if="buildLogVisible" :temp="temp" />
    </a-modal>
  </div>
</template>
<script>
import BuildLog from './log';
import { 
  getBuildGroupList, getBuildList, getBranchList, editBuild, deleteBuild,
  getTriggerUrl, resetTrigger, clearBuid, startBuild, stopBuild
} from '../../api/build';
import { getNodeProjectList } from '../../api/node'
import { getSshList } from '../../api/ssh'
import { parseTime } from '../../utils/time';
export default {
  components: {
    BuildLog
  },
  data() {
    return {
      loading: false,
      listQuery: {},
      groupList: [],
      list: [],
      branchList: [],
      cascaderList: [],
      sshList: [],
      temp: {},
      editBuildVisible: false,
      addGroupvisible: false,
      triggerVisible: false,
      buildLogVisible: false,
      columns: [
        {title: '名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '分支', dataIndex: 'branchName', width: 100, ellipsis: true, scopedSlots: {customRender: 'branchName'}},
        {title: '状态', dataIndex: 'status', width: 100, ellipsis: true, scopedSlots: {customRender: 'status'}},
        {title: '构建 ID', dataIndex: 'buildIdStr', width: 120, ellipsis: true, scopedSlots: {customRender: 'buildIdStr'}},
        {title: '修改人', dataIndex: 'modifyUser', width: 150, ellipsis: true, scopedSlots: {customRender: 'modifyUser'}},
        {title: '修改时间', dataIndex: 'modifyTime', customRender: (text) => {
          if (!text) {
            return '';
          }
          return parseTime(text);
        }, width: 180},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 440, align: 'left'}
      ],
      rules: {
        name: [
          { required: true, message: 'Please input build name', trigger: 'blur' }
        ],
        script: [
          { required: true, message: 'Please input build script', trigger: 'blur' }
        ],
        resultDirFile: [
          { required: true, message: 'Please input build target path', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.loadGroupList();
    this.handleFilter();
  },
  methods: {
    // 分组列表
    loadGroupList() {
      getBuildGroupList().then(res => {
        if (res.code === 200) {
          this.groupList = res.data;
        }
      })
    },
    // 加载数据
    loadData() {
      this.list = [];
      this.loading = true;
      getBuildList(this.listQuery).then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 加载节点项目列表
    loadNodeProjectList() {
      this.cascaderList = [];
      getNodeProjectList().then(res => {
        if (res.code === 200) {
          res.data.forEach(node => {
            const nodeItem = {
              label: node.name,
              value: node.id,
              children: []
            }
            node.projects.forEach(project => {
              const projectItem = {
                label: project.name,
                value: project.id
              }
              nodeItem.children.push(projectItem);
            })
            this.cascaderList.push(nodeItem)
          })
        }
      })
    },
    // 加载 SSH 列表
    loadSshList() {
      this.sshList = [];
      getSshList().then(res => {
        if (res.code === 200) {
          this.sshList = res.data;
        }
      })
    },
    // 筛选
    handleFilter() {
      this.loadData();
    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.loadNodeProjectList();
      this.loadSshList();
      this.editBuildVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign(record);
      this.temp.tempGroup = '';
      if (record.releaseMethodDataId) {
        this.temp.releaseMethodDataIdList = record.releaseMethodDataId.split(':');
      }
      this.loadBranchList();
      this.loadNodeProjectList();
      this.loadSshList();
      this.editBuildVisible = true;
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
    },
    // 获取仓库分支
    loadBranchList() {
      this.branchList = [];
      const params = {
        url: this.temp.gitUrl,
        userName: this.temp.userName,
        userPwd: this.temp.password
      }
      getBranchList(params).then(res => {
        if (res.code === 200) {
          this.branchList = res.data;
        }
      })
    },
    // 选择节点项目
    onChangeProject(value) {
      console.log(value)
    },
    // 提交节点数据
    handleEditBuildOk() {
      // 检验表单
      this.$refs['editBuildForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 设置参数
        if (this.temp.releaseMethod === 2) {
          if (this.temp.releaseMethodDataIdList.length < 2) {
            this.$notification.warn({
              message: '请选择节点项目',
              duration: 2
            });
            return false;
          }
          this.temp.releaseMethodDataId_2_node = this.temp.releaseMethodDataIdList[0];
          this.temp.releaseMethodDataId_2_project = this.temp.releaseMethodDataIdList[1];
        }
        // 提交数据
        editBuild(this.temp).then(res => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            this.$refs['editBuildForm'].resetFields();
            this.editBuildVisible = false;
            this.handleFilter();
            this.loadGroupList();
          }
        })
      })
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除构建信息么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteBuild(record.id).then((res) => {
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
    // 触发器
    handleTrigger(record) {
      this.temp = Object.assign(record);
      getTriggerUrl(record.id).then(res => {
        if (res.code === 200) {
          this.temp.triggerBuildUrl = `${location.protocol}${location.host}${res.data.triggerBuildUrl}`;
          this.triggerVisible = true;
        }
      })
    },
    // 重置触发器
    resetTrigger() {
      resetTrigger(this.temp.id).then(res => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
            duration: 2
          });
          this.triggerVisible = false;
          this.handleTrigger(this.temp);
        }
      })
    },
    // 清除构建
    handleClear(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要清除构建信息么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          clearBuid(record.id).then((res) => {
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
    // 开始构建
    handleStartBuild(record) {
      this.temp = Object.assign(record);
      startBuild(this.temp.id).then(res => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
            duration: 2
          });
          this.handleFilter();
        }
      })
    },
    // 停止构建
    handleStopBuild(record) {
      this.temp = Object.assign(record);
      stopBuild(this.temp.id).then(res => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
            duration: 2
          });
          this.handleFilter();
        }
      })
    },
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.id,
        buildId: record.buildId
      }
      console.log(this.temp)
      this.buildLogVisible = true;
    },
    // 关闭日志对话框
    closeBuildLogModel() {
      this.handleFilter();
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