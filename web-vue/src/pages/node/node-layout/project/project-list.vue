<template>
  <div class="node-full-content">
    <div ref="filter" class="filter">
      <!-- <a-select v-model="listQuery.group" allowClear placeholder="请选择分组" class="filter-item" @change="loadData">
        <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
      </a-select> -->
      <a-input class="search-input-item" v-model="listQuery['%projectId%']" placeholder="项目ID" />
      <a-input class="search-input-item" v-model="listQuery['%name%']" placeholder="项目名称" />
      <a-button type="primary" @click="loadData">搜索</a-button>
      <a-button type="primary" @click="handleAdd">新增</a-button>

      <a-button type="primary" @click="batchStart">批量启动</a-button>
      <a-button type="primary" @click="batchRestart">批量重启</a-button>
      <a-button type="danger" @click="batchStop">批量关闭</a-button>
      状态数据是异步获取有一定时间延迟
    </div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      :loading="loading"
      :columns="columns"
      :pagination="pagination"
      @change="changePage"
      :row-selection="{ onChange: onSelectChange, columnWidth: '25px', getCheckboxProps: getCheckboxProps, hideDefaultSelections: true }"
      bordered
      :rowKey="(record, index) => index"
    >
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="`名称：${text}`">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="time" slot-scope="text, record" placement="topLeft">
        <a-tooltip :title="`创建时间：${parseTime(record.createTimeMillis)}，${record.modifyTimeMillis ? '修改时间：' + parseTime(record.modifyTimeMillis) : ''}`">
          <span>{{ parseTime(record.modifyTimeMillis) }}</span
          ><br />
          <span>{{ parseTime(record.createTimeMillis) }}</span>
        </a-tooltip>
      </template>
      <a-tooltip slot="modifyUser" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="status" slot-scope="text, record">
        <a-tooltip v-if="record.runMode !== 'File'" title="状态操作请到控制台中控制">
          <a-switch :checked="text" disabled checked-children="开" un-checked-children="关" />
        </a-tooltip>
        <span v-if="record.runMode === 'File'">-</span>
      </template>

      <a-tooltip slot="port" slot-scope="text, record" placement="topLeft" :title="`进程号：${record.pid},  端口号：${record.port}`">
        <span v-if="record.pid">{{ record.port }}/{{ record.pid }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleFile(record)">文件</a-button>
        <a-tooltip :title="`${record.runMode === 'File' ? 'File 类型项目不能使用控制台功能' : '到控制台去管理项目状态'}`">
          <a-button type="primary" @click="handleConsole(record)" :disabled="record.runMode === 'File'">控制台</a-button>
        </a-tooltip>
        <a-dropdown>
          <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
            更多
            <a-icon type="down" />
          </a>
          <a-menu slot="overlay">
            <a-menu-item>
              <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
            </a-menu-item>
            <a-menu-item>
              <a-button type="primary" @click="handleMonitor(record)" v-show="record.runMode !== 'File'" :disabled="!record.status">监控 </a-button>
            </a-menu-item>
            <a-menu-item>
              <a-button type="primary" @click="handleReplica(record)" v-show="record.runMode !== 'File'" :disabled="!record.javaCopyItemList">副本集 </a-button>
            </a-menu-item>
            <a-menu-item>
              <a-button type="danger" @click="handleDelete(record)">删除</a-button>
            </a-menu-item>
          </a-menu>
        </a-dropdown>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editProjectVisible" width="800px" title="编辑项目" @ok="handleEditProjectOk" :maskClosable="false">
      <a-form-model ref="editProjectForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="项目 ID" prop="id">
          <a-input v-model="temp.id" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改" />
        </a-form-model-item>
        <a-form-model-item label="项目名称" prop="name">
          <a-input v-model="temp.name" placeholder="项目名称" />
        </a-form-model-item>
        <a-form-model-item prop="runMode">
          <template slot="label">
            运行方式
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul>
                  <li><b>ClassPath</b> java -classpath xxx 运行项目</li>
                  <li><b>Jar</b> java -jar xxx 运行项目</li>
                  <li><b>JarWar</b> java -jar Springboot war 运行项目</li>
                  <li><b>JavaExtDirsCp</b> java -Djava.ext.dirs=lib -cp conf:run.jar $MAIN_CLASS 运行项目</li>
                  <li><b>File</b> 项目为静态文件夹,没有项目状态以及控制等功能</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-select v-model="temp.runMode" placeholder="请选择运行方式">
            <a-select-option v-for="runMode in runModeList" :key="runMode">{{ runMode }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item prop="whitelistDirectory" class="jpom-node-project-whitelist">
          <template slot="label">
            项目白名单路径
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul>
                  <li>白名单路径是指项目文件存放到服务中的文件夹</li>
                  <li>可以到节点管理中的【系统管理】=>【白名单配置】修改</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-select v-model="temp.whitelistDirectory" placeholder="请选择项目白名单路径">
            <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item prop="lib">
          <template slot="label">
            项目文件夹
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul>
                  <li>项目文件夹是项目实际存放的目录名称</li>
                  <li>项目文件会存放到 <br />&nbsp;&nbsp;<b>项目白名单路径+项目文件夹</b></li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input v-model="temp.lib" placeholder="项目存储的文件夹" @blur.native="checkLibIndexExist" />
          <span class="lib-exist" v-show="temp.libExist">当前文件夹已存在,创建成功后会自动同步文件.</span>
        </a-form-model-item>
        <a-form-model-item v-show="filePath !== ''" label="项目完整目录">
          <a-alert :message="filePath" type="success" />
        </a-form-model-item>
        <a-form-model-item v-show="temp.runMode && temp.runMode !== 'File'">
          <template slot="label">
            日志目录
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul>
                  <li>日志目录是指控制台日志存储目录</li>
                  <li>默认是和项目文件夹父级</li>
                  <li>可选择的列表和项目白名单目录是一致的，即相同配置</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-select v-model="temp.logPath" placeholder="请选择项目白名单路径">
            <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
          </a-select>
        </a-form-model-item>

        <a-form-model-item label="JDK" prop="jdkId" v-show="temp.runMode && temp.runMode !== 'File'" class="jpom-node-project-jdk">
          <a-select v-model="temp.jdkId" placeholder="请选择 JDK">
            <a-select-option v-for="jdk in jdkList" :key="jdk.id">{{ jdk.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="Main Class" prop="mainClass" v-show="temp.runMode && temp.runMode !== 'Jar' && temp.runMode !== 'File'">
          <a-input v-model="temp.mainClass" placeholder="程序运行的 main 类(jar 模式运行可以不填)" />
        </a-form-model-item>
        <a-form-model-item label="JavaExtDirsCp" prop="javaExtDirsCp" v-show="temp.runMode === 'JavaExtDirsCp' && temp.runMode !== 'File'">
          <a-input v-model="temp.javaExtDirsCp" placeholder="-Dext.dirs=xxx: -cp xx  填写【xxx:xx】" />
        </a-form-model-item>
        <a-form-model-item label="JVM 参数" prop="jvm" v-show="temp.runMode && temp.runMode !== 'File'">
          <a-textarea v-model="temp.jvm" :auto-size="{ minRows: 3, maxRows: 3 }" placeholder="jvm参数,非必填.如：-Xms512m -Xmx512m" />
        </a-form-model-item>
        <a-form-model-item label="args 参数" prop="args" v-show="temp.runMode && temp.runMode !== 'File'">
          <a-textarea v-model="temp.args" :auto-size="{ minRows: 3, maxRows: 3 }" placeholder="Main 函数 args 参数，非必填. 如：--server.port=8080" />
        </a-form-model-item>
        <!-- 副本信息 -->
        <a-row v-for="replica in temp.javaCopyItemList" :key="replica.id">
          <a-form-model-item :label="`副本 ${replica.id} JVM 参数`" prop="jvm">
            <a-textarea v-model="replica.jvm" :auto-size="{ minRows: 3, maxRows: 3 }" class="replica-area" placeholder="jvm参数,非必填.如：-Xms512m -Xmx512m" />
          </a-form-model-item>
          <a-form-model-item :label="`副本 ${replica.id} args 参数`" prop="args">
            <a-textarea v-model="replica.args" :auto-size="{ minRows: 3, maxRows: 3 }" class="replica-area" placeholder="Main 函数 args 参数，非必填. 如：--server.port=8080" />
          </a-form-model-item>
          <a-tooltip placement="topLeft" title="已经添加成功的副本需要在副本管理页面去删除" class="replica-btn-del">
            <a-button :disabled="!replica.deleteAble" type="danger" @click="handleDeleteReplica(replica)">删除</a-button>
          </a-tooltip>
        </a-row>
        <!-- 添加副本 -->
        <a-form-model-item v-show="temp.runMode && temp.runMode !== 'File'">
          <template slot="label">
            副本操作
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul>
                  <li>副本是指同一个项目在一个节点（服务器）中运行多份</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-button type="primary" @click="handleAddReplica">添加副本</a-button>
        </a-form-model-item>
        <a-form-model-item label="WebHooks" prop="token" v-show="temp.runMode && temp.runMode !== 'File'" class="jpom-node-project-token">
          <a-input v-model="temp.token" placeholder="关闭程序时自动请求,非必填，GET请求" />
        </a-form-model-item>
        <a-form-model-item v-show="temp.type === 'edit' && temp.runMode !== 'File'" label="日志路径" prop="log">
          <a-alert :message="temp.log" type="success" />
        </a-form-model-item>
        <a-form-model-item v-show="temp.type === 'edit' && temp.runMode !== 'File'" label="运行命令" prop="runCommand">
          <a-alert :message="temp.runCommand || '无'" type="success" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 项目文件组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerFileVisible" @close="onFileClose">
      <file v-if="drawerFileVisible" :nodeId="node.id" :projectId="temp.id" :runMode="temp.runMode" :absPath="(temp.whitelistDirectory || '') + (temp.lib || '')" @goConsole="goConsole" />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :nodeId="node.id" :projectId="temp.id" @goFile="goFile" />
    </a-drawer>
    <!-- 项目监控组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerMonitorVisible" @close="onMonitorClose">
      <monitor v-if="drawerMonitorVisible" :node="node" :project="temp" />
    </a-drawer>
    <!-- 项目副本集组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerReplicaVisible" @close="onReplicaClose">
      <replica v-if="drawerReplicaVisible" :node="node" :project="temp" />
    </a-drawer>
  </div>
</template>
<script>
import { mapGetters } from "vuex";
import File from "./project-file";
import Console from "./project-console";
import Monitor from "./project-monitor";
import Replica from "./project-replica";
import { parseTime } from "@/utils/time";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";

import {
  getJdkList,
  getRuningProjectInfo,
  getProjectData,
  deleteProject,
  getProjectList,
  getProjectAccessList,
  editProject,
  nodeJudgeLibExist,
  restartProject,
  startProject,
  stopProject,
} from "@/api/node-project";

export default {
  props: {
    node: {
      type: Object,
    },
  },
  components: {
    File,
    Console,
    Monitor,
    Replica,
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      accessList: [],
      jdkList: [],
      runModeList: ["ClassPath", "Jar", "JarWar", "JavaExtDirsCp", "File"],
      list: [],
      temp: {},
      editProjectVisible: false,
      drawerTitle: "",
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      drawerMonitorVisible: false,
      drawerReplicaVisible: false,
      // addGroupvisible: false,
      libExist: false,
      selectedRows: [],
      checkRecord: "",
      columns: [
        { title: "项目名称", dataIndex: "name", sorter: true, width: 60, ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "修改/创建时间", sorter: true, dataIndex: "modifyTimeMillis", width: 90, ellipsis: true, scopedSlots: { customRender: "time" } },

        {
          title: "最后操作人",
          dataIndex: "modifyUser",
          width: 60,
          ellipsis: true,
          sorter: true,
          scopedSlots: { customRender: "modifyUser" },
        },
        { title: "运行状态", dataIndex: "status", width: 50, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "端口/PID", dataIndex: "port", width: 50, ellipsis: true, scopedSlots: { customRender: "port" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 120 },
      ],
      rules: {
        id: [{ required: true, message: "请输入项目ID", trigger: "blur" }],
        name: [{ required: true, message: "请输入项目名称", trigger: "blur" }],
        runMode: [{ required: true, message: "请选择项目运行方式", trigger: "blur" }],
        whitelistDirectory: [{ required: true, message: "请选择项目白名单路径", trigger: "blur" }],
        lib: [{ required: true, message: "请输入项目文件夹", trigger: "blur" }],
      },
    };
  },
  computed: {
    ...mapGetters(["getGuideFlag"]),
    filePath() {
      return (this.temp.whitelistDirectory || "") + (this.temp.lib || "");
    },
    pagination() {
      return {
        total: this.listQuery.total || 0,
        current: this.listQuery.page || 1,
        pageSize: this.listQuery.limit || PAGE_DEFAULT_LIMIT,
        pageSizeOptions: PAGE_DEFAULT_SIZW_OPTIONS,
        showSizeChanger: true,
        showTotal: (total) => {
          return PAGE_DEFAULT_SHOW_TOTAL(total, this.listQuery);
        },
      };
    },
  },
  watch: {
    getGuideFlag() {
      this.introGuide();
    },
  },
  mounted() {
    this.loadData();
  },
  methods: {
    parseTime(v) {
      return parseTime(v);
    },
    // 页面引导
    introGuide() {
      if (this.getGuideFlag) {
        this.$introJs()
          .setOptions({
            hidePrev: true,
            steps: [
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".jpom-node-project-whitelist"),
                intro: "这里是选择节点设置的白名单目录，白名单的设置在侧边栏菜单<b>系统管理</b>里面。",
              },
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".jpom-node-project-jdk"),
                intro: "这里选择 JDK，JDK 需要在侧边栏菜单里手动添加，并非直接读取节点服务器里面的 JDK。",
              },
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".jpom-node-project-token"),
                intro: "这里可以理解为当程序停止时会给这个地址发送一个 HTTP GET 请求。",
              },
            ],
          })
          .start();
        return false;
      }
      this.$introJs().exit();
    },

    // 加载项目白名单列表
    loadAccesList() {
      getProjectAccessList(this.node.id).then((res) => {
        if (res.code === 200) {
          this.accessList = res.data;
        }
      });
    },
    // 加载 JDK 列表
    loadJdkList() {
      getJdkList(this.node.id).then((res) => {
        if (res.code === 200) {
          this.jdkList = res.data;
        }
      });
    },
    // 加载数据
    loadData() {
      this.loading = true;

      this.listQuery.nodeId = this.node.id;
      getProjectList(this.listQuery).then((res1) => {
        if (res1.code === 200) {
          let resultList = res1.data.result;
          this.listQuery.total = res1.data.total;

          // TODO: 由于Ant Design Vue的bug，当表格中首行为disabled时，表格的全选按钮也无法选择。
          // 目前的解决方案是：把需要disabled的元素放到最后。
          // 如果运行模式是文件，则无需批量启动/重启/关闭
          let tempList = resultList.filter((item) => item.runMode !== "File");
          let fileList = resultList.filter((item) => item.runMode === "File");
          this.list = tempList.concat(fileList);
          // 项目ID 字段更新
          this.list = this.list.map((element) => {
            element.dataId = element.id;
            element.id = element.projectId;
            return element;
          });

          let ids = tempList.map((item) => {
            return item.projectId;
          });
          // 如果 ids 有数据就继续请求
          if (ids.length > 0) {
            const tempParams = {
              nodeId: this.node.id,
              ids: JSON.stringify(ids),
            };
            getRuningProjectInfo(tempParams).then((res2) => {
              if (res2.code === 200) {
                this.list = this.list.map((element) => {
                  if (res2.data[element.projectId]) {
                    element.port = res2.data[element.projectId].port;
                    element.pid = res2.data[element.projectId].pid;
                    element.status = true;
                  }
                  return element;
                });
                // this.list.forEach((element) => {});
              }
            });
          }
        }
        this.loading = false;
      });
    },
    // 添加
    handleAdd() {
      this.temp = {
        type: "add",
        logPath: "",
        javaCopyItemList: [],
      };
      this.loadAccesList();
      this.loadJdkList();
      this.editProjectVisible = true;
      this.$nextTick(() => {
        setTimeout(() => {
          this.introGuide();
        }, 500);
      });
    },
    // 编辑
    handleEdit(record) {
      const params = {
        id: record.id,
        nodeId: this.node.id,
      };
      this.loadAccesList();
      this.loadJdkList();
      getProjectData(params).then((res) => {
        if (res.code === 200) {
          this.temp = {
            ...res.data,
            type: "edit",
          };
          if (!this.temp.javaCopyItemList) {
            this.temp = {
              ...this.temp,
              javaCopyItemList: [],
            };
          }
          this.editProjectVisible = true;
        }
      });
    },
    // 添加副本
    handleAddReplica() {
      const $chars = "ABCDEFGHJKMNPQRSTWXYZ0123456789";
      /****默认去掉了容易混淆的字符oOLl,9gq,Vv,Uu,I1****/
      const maxPos = $chars.length;
      let repliccaId = "";
      for (let i = 0; i < 2; i++) {
        repliccaId += $chars.charAt(Math.floor(Math.random() * maxPos));
      }
      this.temp.javaCopyItemList.push({
        id: repliccaId,
        jvm: "",
        args: "",
        deleteAble: true,
      });
    },
    // 移除副本
    handleDeleteReplica(reeplica) {
      const index = this.temp.javaCopyItemList.findIndex((element) => element.id === reeplica.id);
      const newList = this.temp.javaCopyItemList.slice();
      newList.splice(index, 1);
      this.temp.javaCopyItemList = newList;
    },
    // 提交
    handleEditProjectOk() {
      if (this.temp.outGivingProject) {
        this.$notification.warning({
          message: "独立的项目分发请到分发管理中去修改",
          
        });
        return;
      }
      // 检验表单
      this.$refs["editProjectForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const params = {
          ...this.temp,
          nodeId: this.node.id,
        };
        // 额外参数
        const replicaParams = {};
        let javaCopyIds = "";
        this.temp.javaCopyItemList.forEach((element) => {
          javaCopyIds += `${element.id},`;
          replicaParams[`jvm_${element.id}`] = element.jvm;
          replicaParams[`args_${element.id}`] = element.args;
        });
        replicaParams["javaCopyIds"] = javaCopyIds.substring(0, javaCopyIds.length - 1);
        editProject(params, replicaParams).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
              
            });
            this.$refs["editProjectForm"].resetFields();
            this.editProjectVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 文件管理
    handleFile(record) {
      this.checkRecord = record;
      this.temp = Object.assign(record);
      this.drawerTitle = `文件管理(${this.temp.name})`;
      this.drawerFileVisible = true;
    },
    // 关闭文件管理对话框
    onFileClose() {
      this.drawerFileVisible = false;
    },
    // 控制台
    handleConsole(record) {
      this.checkRecord = record;
      this.temp = Object.assign(record);
      this.drawerTitle = `控制台(${this.temp.name})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false;
      this.loadData();
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
    // 副本集
    handleReplica(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `副本集(${this.temp.name})`;
      this.drawerReplicaVisible = true;
    },
    // 关闭副本集
    onReplicaClose() {
      this.drawerReplicaVisible = false;
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除项目么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          const params = {
            nodeId: this.node.id,
            id: record.id,
          };
          deleteProject(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                
              });
              this.loadData();
            }
          });
        },
      });
    },

    //检查节点是否存在
    checkLibIndexExist() {
      // 检查是否输入完整
      if (this.temp.lib && this.temp.lib.length !== 0 && this.temp.whitelistDirectory && this.temp.whitelistDirectory.length !== 0) {
        const params = {
          nodeId: this.node.id,
          newLib: this.temp.whitelistDirectory + this.temp.lib,
        };
        nodeJudgeLibExist(params).then((res) => {
          if (res.code === 401) {
            this.temp.libExist = true;
            this.temp = { ...this.temp };
          }
          if (res.code !== 200) {
            this.$notification.warning({
              message: res.msg,
              description: '提示',
              
            });
          }
        });
      }
    },
    //前往控制台
    goConsole() {
      //关闭文件
      this.onFileClose();
      this.handleConsole(this.checkRecord);
    },
    //前往文件
    goFile() {
      // 关闭控制台
      this.onConsoleClose();
      this.handleFile(this.checkRecord);
    },
    // 获取复选框属性 判断是否可以勾选
    getCheckboxProps(record) {
      return {
        props: {
          disabled: record.runMode === "File",
          name: record.name,
        },
      };
    },
    //选中项目
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRows = selectedRows;
      //console.log(selectedRowKeys, selectedRows);
    },
    // onSelectAll() {

    // },
    //批量开始
    batchStart() {
      if (this.selectedRows.length == 0) {
        this.$notification.warning({
          message: "请选中要启动的项目",
          
        });
      }
      this.selectedRows.forEach((value) => {
        if (value.status == false && value.runMode !== "File") {
          const params = {
            nodeId: this.node.id,
            id: value.id,
          };
          //console.log(this.list[value]);
          startProject(params).then(() => {
            this.loadData();
          });
        }
      });
    },
    //批量重启
    batchRestart() {
      if (this.selectedRows.length == 0) {
        this.$notification.warning({
          message: "请选中要重启的项目",
          
        });
      }
      this.selectedRows.forEach((value) => {
        if (value.runMode != "File") {
          const params = {
            nodeId: this.node.id,
            id: value.id,
          };
          restartProject(params).then(() => {
            this.loadData();
          });
        }
      });
    },
    //批量关闭
    batchStop() {
      if (this.selectedRows.length == 0) {
        this.$notification.warning({
          message: "请选中要关闭的项目",
          
        });
      }
      this.selectedRows.forEach((value) => {
        if (value.status == true && value.runMode != "File") {
          const params = {
            nodeId: this.node.id,
            id: value.id,
          };
          stopProject(params).then(() => {
            this.loadData();
          });
        }
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      if (sorter) {
        this.listQuery.order = sorter.order;
        this.listQuery.order_field = sorter.field;
      }
      this.loadData();
    },
  },
};
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

.replica-area {
  width: 340px;
}

.replica-btn-del {
  position: absolute;
  right: 120px;
  top: 74px;
}

.lib-exist {
  color: #faad14;
}
</style>
