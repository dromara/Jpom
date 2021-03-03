<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleLink">添加关联项目</a-button>
      <a-button type="primary" @click="handleAdd">创建分发项目</a-button>
      <a-button type="primary" @click="handleFilter">刷新</a-button>
    </div>
    <!-- 表格 -->
    <a-table :loading="loading" :columns="columns" :data-source="list" bordered rowKey="id" class="node-table"
     @expand="expand" :pagination="false">
      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="outGivingProject" slot-scope="text">
        <span v-if="text">独立</span>
        <span v-else>关联</span>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleReload(record)">刷新</a-button>
        <a-button type="primary" @click="handleDispatch(record)">分发文件</a-button>
        <a-button type="primary" v-if="record.outGivingProject" @click="handleEditDispatchProject(record)">编辑</a-button>
        <a-button type="primary" v-else @click="handleEditDispatch(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
      <!-- 嵌套表格 -->
      <a-table slot="expandedRowRender" slot-scope="text" :scroll="{x: '80vw'}" :loading="childLoading" :columns="childColumns" :data-source="text.children"
        :pagination="false" :rowKey="(record, index) => record.nodeId + record.projectId + index">
        <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="projectName" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-switch slot="projectStatus" slot-scope="text" :checked="text" checked-children="运行中" un-checked-children="未运行"/>
        <template slot="child-operation" slot-scope="text, record">
          <a-button :disabled="!record.projectName" type="primary" @click="handleFile(record)">文件</a-button>
          <a-button :disabled="!record.projectName" type="primary" @click="handleConsole(record)">控制台</a-button>
        </template>
      </a-table>
    </a-table>
    <!-- 添加关联项目 -->
    <a-modal v-model="linkDispatchVisible" width="600px" title="编辑关联项目" @ok="handleLinkDispatchOk" :maskClosable="false">
      <a-form-model ref="linkDispatchForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="分发 ID" prop="id">
          <a-input v-model="temp.id" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改"/>
        </a-form-model-item>
        <a-form-model-item label="分发名称" prop="name">
          <a-input v-model="temp.name" placeholder="分发名称"/>
        </a-form-model-item>
        <a-form-model-item label="分发项目" prop="projectId">
          <a-select v-model="temp.projectId" placeholder="请选择需要分发的项目" @select="selectProject">
            <a-select-option v-for="project in projectList" :key="project.id">{{ project.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="勾选节点" prop="nodeId">
          <a-transfer
            :data-source="nodeList"
            show-search
            :filter-option="filterOption"
            :target-keys="targetKeys"
            :render="item => item.title"
            @change="handleChange"
          />
        </a-form-model-item>
        <a-form-model-item label="分发后操作" prop="afterOpt">
          <a-select v-model="temp.afterOpt" placeholder="请选择发布后操作">
            <a-select-option :key="0">不做任何操作</a-select-option>
            <a-select-option :key="1">并发重启</a-select-option>
            <a-select-option :key="2">完整顺序重启(有重启失败将结束本次)</a-select-option>
            <a-select-option :key="3">顺序重启(有重启失败将继续)</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 创建分发项目 -->
    <a-modal v-model="editDispatchVisible" width="600px" title="创建分发项目" @ok="handleEditDispatchOk" :maskClosable="false">
      <a-form-model ref="editDispatchForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
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
        <a-form-model-item label="Main Class" prop="mainClass" v-show="temp.runMode !== 'Jar'">
          <a-input v-model="temp.mainClass" placeholder="程序运行的 main 类(jar 模式运行可以不填)"/>
        </a-form-model-item>
        <a-form-model-item label="项目白名单路径" prop="whitelistDirectory">
          <a-select v-model="temp.whitelistDirectory" placeholder="请选择项目白名单路径">
            <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="项目文件夹" prop="lib">
          <a-input v-model="temp.lib" placeholder="项目存储的文件夹，jar 包存放的文件夹"/>
        </a-form-model-item>
        <a-form-model-item label="分发后操作" prop="afterOpt">
          <a-select v-model="temp.afterOpt" placeholder="请选择发布后操作">
            <a-select-option :key="0">不做任何操作</a-select-option>
            <a-select-option :key="1">并发重启</a-select-option>
            <a-select-option :key="2">完整顺序重启(有重启失败将结束本次)</a-select-option>
            <a-select-option :key="3">顺序重启(有重启失败将继续)</a-select-option>
          </a-select>
        </a-form-model-item>
        <!-- 节点 -->
        <a-form-model-item label="分发节点节点" prop="nodeId">
          <a-select v-model="temp.nodeIdList" mode="multiple" placeholder="请选择分发节点">
            <a-select-option v-for="node in nodeList" :key="node.key">{{ `${node.title} ( ${node.key} )` }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-collapse>
          <a-collapse-panel v-for="nodeId in temp.nodeIdList" :key="nodeId" :header="nodeId">
            <a-form-model-item label="WebHooks" prop="token">
              <a-input v-model="temp[`${nodeId}_token`]" placeholder="关闭程序时自动请求,非必填，GET请求"/>
            </a-form-model-item>
            <a-form-model-item label="JVM 参数" prop="jvm">
              <a-textarea v-model="temp[`${nodeId}_jvm`]" :auto-size="{ minRows: 3, maxRows: 3 }" placeholder="jvm参数,非必填.如：-Xmin=512m -Xmax=512m"/>
            </a-form-model-item>
            <a-form-model-item label="args 参数" prop="args">
              <a-textarea v-model="temp[`${nodeId}_args`]" :auto-size="{ minRows: 3, maxRows: 3 }" placeholder="Main 函数 args 参数，非必填. 如：--service.port=8080"/>
            </a-form-model-item>
            <!-- 副本信息 -->
            <a-row v-for="replica in temp[`${nodeId}_javaCopyItemList`]" :key="replica.id">
              <a-form-model-item :label="`副本 ${replica.id} JVM 参数`" prop="jvm">
                <a-textarea v-model="replica.jvm" :auto-size="{ minRows: 3, maxRows: 3 }" class="replica-area" placeholder="jvm参数,非必填.如：-Xmin=512m -Xmax=512m"/>
              </a-form-model-item>
              <a-form-model-item :label="`副本 ${replica.id} args 参数`" prop="args">
                <a-textarea v-model="replica.args" :auto-size="{ minRows: 3, maxRows: 3 }" class="replica-area" placeholder="Main 函数 args 参数，非必填. 如：--service.port=8080"/>
              </a-form-model-item>
              <a-tooltip placement="topLeft" title="已经添加成功的副本需要在副本管理页面去删除" class="replica-btn-del">
                <a-button :disabled="!replica.deleteAble" type="danger" @click="handleDeleteReplica(nodeId, replica)">删除</a-button>
              </a-tooltip>
            </a-row>
            <!-- 添加副本 -->
            <a-form-model-item label="操作" >
              <a-button type="primary" @click="handleAddReplica(nodeId)">添加副本</a-button>
            </a-form-model-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form-model>
    </a-modal>
    <!-- 分发项目 -->
    <a-modal v-model="dispatchVisible" width="600px" title="分发项目" @ok="handleDispatchOk" :maskClosable="false">
      <a-form-model ref="dispatchForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="选择分发文件" prop="clearOld">
          <a-upload :file-list="fileList" :remove="handleRemove" :before-upload="beforeUpload" accept=".zip,.tar,.gz,.bz2">
            <a-button type="primary"><a-icon type="upload" />选择文件上传</a-button>
          </a-upload>
        </a-form-model-item>
        <a-form-model-item label="清空发布" prop="clearOld">
          <a-switch v-model="temp.clearOld" checked-children="是" un-checked-children="否"/>
        </a-form-model-item>
        <a-form-model-item label="分发后操作" prop="afterOpt">
          <a-select v-model="temp.afterOpt" placeholder="请选择发布后操作">
            <a-select-option :key="0">不做任何操作</a-select-option>
            <a-select-option :key="1">并发重启</a-select-option>
            <a-select-option :key="2">完整顺序重启(有重启失败将结束本次)</a-select-option>
            <a-select-option :key="3">顺序重启(有重启失败将继续)</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 项目文件组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw"
      :visible="drawerFileVisible" @close="onFileClose">
      <file v-if="drawerFileVisible" :nodeId="temp.nodeId" :projectId="temp.projectId" />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw"
      :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :nodeId="temp.nodeId" :projectId="temp.projectId" />
    </a-drawer>
  </div>
</template>
<script>
import File from '../node/node-layout/project/project-file';
import Console from '../node/node-layout/project/project-console';
import { getDishPatchList, getDispatchProject, getReqId, editDispatch, editDispatchProject, uploadDispatchFile, getDispatchWhiteList, deleteDisPatch } from '../../api/dispatch';
import { getNodeProjectList } from '../../api/node';
import { getProjectData } from '../../api/node-project';
export default {
  components: {
    File,
    Console
  },
  data() {
    return {
      loading: false,
      childLoading: false,
      list: [],
      accessList: [],
      nodeList: [],
      projectList: [],
      nodeProjectMap: {},
      targetKeys: [],
      reqId: '',
      temp: {},
      fileList: [],
      runModeList: [
        'ClassPath',
        'Jar',
        'JarWar',
        'JavaExtDirsCp',
        // 'File'
      ],
      linkDispatchVisible: false,
      editDispatchVisible: false,
      dispatchVisible: false,
      drawerTitle: '',
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      columns: [
        {title: '分发 ID', dataIndex: 'id', width: 100, ellipsis: true, scopedSlots: {customRender: 'id'}},
        {title: '分发名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '类型', dataIndex: 'outGivingProject', width: 100, ellipsis: true, scopedSlots: {customRender: 'outGivingProject'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 380, align: 'left'}
      ],
      childColumns: [
        {title: '节点名称', dataIndex: 'nodeId', width: 100, ellipsis: true, scopedSlots: {customRender: 'nodeId'}},
        {title: '项目名称', dataIndex: 'projectName', width: 120, ellipsis: true, scopedSlots: {customRender: 'projectName'}},
        {title: '项目状态', dataIndex: 'projectStatus', width: 150, ellipsis: true, scopedSlots: {customRender: 'projectStatus'}},
        {title: '分发状态', dataIndex: 'outGivingStatus', width: 180},
        {title: '最后分发时间', dataIndex: 'lastTime', width: 180, ellipsis: true, scopedSlots: {customRender: 'lastTime'}},
        {title: '操作', dataIndex: 'child-operation', scopedSlots: {customRender: 'child-operation'}, width: 200, align: 'left'}
      ],
      rules: {
        id: [
          { required: true, message: 'Please input dispatch id', trigger: 'blur' }
        ],
        name: [
          { required: true, message: 'Please input dispatch name', trigger: 'blur' }
        ],
        projectId: [
          { required: true, message: 'Please select project', trigger: 'blur' }
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
  created() {
    this.handleFilter();
  },
  methods: {
    // 加载数据
    loadData() {
      this.list = [];
      this.loading = true;
      this.childLoading = false;
      getDishPatchList().then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 加载节点项目列表
    loadNodeProjectList() {
      this.nodeList = [];
      this.projectList = [];
      // nodeProjectMap 这个对象用来判断项目对应的节点是否该禁用
      this.nodeProjectMap = {};
      getNodeProjectList().then(res => {
        if (res.code === 200) {
          res.data.forEach(node => {
            const nodeItem = {
              title: node.name,
              key: node.id,
              disabled: true
            }
            node.projects.forEach(project => {
              // 如果项目 ID 存在就不用继续添加
              const index = this.projectList.findIndex(p => p.id === project.id);
              if (index === -1) {
                const projectItem = {
                  name: `${project.name} ( ${project.id} )`,
                  id: project.id
                }
                this.projectList.push(projectItem);
              }
              // 判断对象是否存在
              if (!this.nodeProjectMap[`${project.id}`]) {
                this.nodeProjectMap[`${project.id}`] = [
                  ...this.nodeProjectMap[`${project.id}`] || [],
                  node.id
                ];
              } else {
                const tempIndex = this.nodeProjectMap[`${project.id}`].findIndex(nodeId => node.id === nodeId);
                if (tempIndex === -1) {
                  this.nodeProjectMap[`${project.id}`].push(node.id);
                }
              }
            })
            this.nodeList.push(nodeItem);
          })
        }
      })
    },
    // 获取 reqId
    loadReqId() {
      getReqId().then(res => {
        if (res.code === 200) {
          this.reqId = res.data;
        }
      })
    },
    // 加载项目白名单列表
    loadAccesList() {
      getDispatchWhiteList().then(res => {
        if (res.code === 200) {
          this.accessList = res.data;
        }
      })
    },
    // 展开行
    expand(expanded, record) {
      if (expanded) {
        this.handleReload(record);
      }
    },
    // 筛选
    handleFilter() {
      this.loadData();
      this.loadNodeProjectList();
    },
    // 关联分发
    handleLink() {
      this.temp = {
        type: 'add'
      };
      this.loadReqId();
      this.linkDispatchVisible = true;
      this.$nextTick(() => {
        this.$refs['linkDispatchForm'].resetFields();
      })
    },
    // 编辑分发
    handleEditDispatch(record) {
      this.temp = Object.assign({}, record)
      this.temp.type = 'edit';
      this.targetKeys = [];
      // 设置选中的项目和节点
      record.outGivingNodeProjectList.forEach(ele => {
        if (!this.temp.projectId) {
          this.temp.projectId = ele.projectId;
        }
        this.targetKeys.push(ele.nodeId);
      })
      this.loadReqId();
      this.linkDispatchVisible = true;
      this.$nextTick(() => {
        this.$refs['linkDispatchForm'].resetFields();
      })
    },
    // 选择项目
    selectProject(value) {
      this.targetKeys = [];
      this.nodeList.forEach(node => {
        node.disabled = true;
      })
      this.nodeProjectMap[value].forEach(nodeId => {
        this.nodeList.forEach(node => {
          if (node.key === nodeId) {
            node.disabled = false;
          }
        })
      })
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.title.indexOf(inputValue) > -1;
    },
    // 穿梭框 change
    handleChange(targetKeys) {
      this.targetKeys = targetKeys;
    },
    // 提交关联项目
    handleLinkDispatchOk() {
      // 检验表单
      this.$refs['linkDispatchForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 检查
        if (this.targetKeys.length < 2) {
          this.$notification.warn({
            message: '请至少选择 2 个节点',
            duration: 2
          });
          return false;
        }
        // 设置 reqId
        this.temp.reqId = this.reqId;
        this.targetKeys.forEach(key => {
          this.temp[`node_${key}`] = this.temp.projectId;
        })
        // 提交
        editDispatch(this.temp).then(res => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            this.targetKeys = [];
            this.$refs['linkDispatchForm'].resetFields();
            this.linkDispatchVisible = false;
            this.handleFilter();
          } else {
            this.targetKeys = [];
          }
        })
      })
    },
    // 添加分发项目
    handleAdd() {
      this.temp = {
        type: 'add',
        nodeIdList: []
      };
      // 添加 javaCopyItemList
      this.nodeList.forEach(node => {
        this.temp[`${node.key}_javaCopyItemList`] = [];
      })
      this.loadAccesList();
      this.loadReqId();
      this.editDispatchVisible = true;
      this.$nextTick(() => {
        this.$refs['editDispatchForm'].resetFields();
      })
    },
    // 编辑分发项目
    handleEditDispatchProject(record) {
      this.temp = {};
      record.outGivingNodeProjectList.forEach(ele => {
        const params = {
          id: ele.projectId,
          nodeId: ele.nodeId
        }
        getProjectData(params).then(res => {
          if (res.code === 200) {
            // 如果 temp.id 不存在
            if (!this.temp.id) {
              this.temp = {
                id: res.data.id,
                name: res.data.name,
                type: 'edit',
                afterOpt: res.data.afterOpt,
                runMode: res.data.runMode,
                mainClass: res.data.mainClass,
                javaExtDirsCp: res.data.javaExtDirsCp,
                whitelistDirectory: res.data.whitelistDirectory,
                lib: res.data.lib,
                nodeIdList: []
              }
            }
            // 添加 nodeIdList
            this.temp.nodeIdList.push(ele.nodeId);
            // 添加 javaCopyItemList
            this.temp[`${ele.nodeId}_javaCopyItemList`] = res.data.javaCopyItemList || [];
          }
        })
      })
      // 加载其他数据
      this.loadAccesList();
      this.loadReqId();
      this.editDispatchVisible = true;
      this.$nextTick(() => {
        this.$refs['editDispatchForm'].resetFields();
      })
    },
    // 添加副本
    handleAddReplica(nodeId) {
      const $chars = 'ABCDEFGHJKMNPQRSTWXYZ0123456789';
      /****默认去掉了容易混淆的字符oOLl,9gq,Vv,Uu,I1****/
      const maxPos = $chars.length;
      let repliccaId = '';
      for (let i = 0; i < 2; i++) {
        repliccaId += $chars.charAt(Math.floor(Math.random() * maxPos));
      }
      this.temp[`${nodeId}_javaCopyItemList`].push({
        id: repliccaId,
        jvm: '',
        args: '',
        deleteAble: true
      })
      this.temp = {...this.temp};
    },
    // 移除副本
    handleDeleteReplica(nodeId, reeplica) {
      const index = this.temp[`${nodeId}_javaCopyItemList`].findIndex(element => element.id === reeplica.id);
      const newList = this.temp[`${nodeId}_javaCopyItemList`].slice();
      newList.splice(index, 1);
      this.temp[`${nodeId}_javaCopyItemList`] = newList;
    },
    // 提交创建分发项目
    handleEditDispatchOk() {
      // 检验表单
      this.$refs['editDispatchForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 检查
        if (this.temp.nodeIdList.length < 2) {
          this.$notification.warn({
            message: '请至少选择 2 个节点',
            duration: 2
          });
          return false;
        }
        // 设置 reqId
        this.temp.reqId = this.reqId;
        // 设置节点
        this.temp.nodeIdList.forEach(key => {
          this.temp[`add_${key}`] = key;
          // 设置副本
          this.temp[`${key}_javaCopyIds`] = '';
          this.temp[`${key}_javaCopyItemList`].forEach(element => {
            this.temp[`${key}_javaCopyIds`] += `${element.id},`;
            this.temp[`${key}_jvm_${element.id}`] = element.jvm;
            this.temp[`${key}_args_${element.id}`] = element.args;
          })
          // 移除多余的后缀 ,
          this.temp[`${key}_javaCopyIds`] = this.temp[`${key}_javaCopyIds`].substring(0, this.temp[`${key}_javaCopyIds`].length-1);
        })
        // 提交
        editDispatchProject(this.temp).then(res => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            this.$refs['editDispatchForm'].resetFields();
            this.editDispatchVisible = false;
            this.handleFilter();
          }
        })
      })
    },
    // 刷新
    handleReload(record) {
      this.childLoading = true;
      getDispatchProject(record.id).then(res => {
        if (res.code === 200) {
          record.children = res.data;
        }
        this.childLoading = false;
      })
    },
    // 处理分发
    handleDispatch(record) {
      this.temp = Object.assign({}, record);
      this.dispatchVisible = true;
    },
    // 处理文件移除
    handleRemove(file) {
      const index = this.fileList.indexOf(file);
      const newFileList = this.fileList.slice();
      newFileList.splice(index, 1);
      this.fileList = newFileList;
    },
    // 准备上传文件
    beforeUpload(file) {
      // 只允许上传单个文件
      this.fileList = [file];
      return false;
    },
    // 提交分发文件
    handleDispatchOk() {
      // 检验表单
      this.$refs['dispatchForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 判断文件
        if (this.fileList.length === 0) {
          this.$notification.error({
            message: '请选择文件',
            duration: 2
          });
          return false;
        }
        this.$message.loading('正在上传文件...', 2);
        // 上传文件
        const formData = new FormData();
        formData.append('file', this.fileList[0]);
        formData.append('id', this.temp.id);
        formData.append('afterOpt', this.temp.afterOpt);
        formData.append('clearOld', this.temp.clearOld);
        uploadDispatchFile(formData).then(res => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            this.$refs['dispatchForm'].resetFields();
            this.fileList = [];
            this.loadData();
            this.dispatchVisible = false;
          }
        })
      })
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除分发信息么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteDisPatch(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              this.handleFilter();
            }
          })
        }
      });
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `文件管理(${this.temp.projectId})`
      this.drawerFileVisible = true;
    },
    // 关闭文件管理对话框
    onFileClose() {
      this.drawerFileVisible = false;
    },
    // 控制台
    handleConsole(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `控制台(${this.temp.projectId})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false;
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
.replica-area {
  width: 300px;
}
.replica-btn-del {
  position: absolute;
  right: 0px;
  top: 74px;
}
</style>