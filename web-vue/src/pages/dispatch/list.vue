<template>
  <div class="full-content">
    <div ref="filter" class="filter">
      <a-input class="search-input-item" v-model="listQuery['%name%']" placeholder="名称" />
      <a-button type="primary" @click="loadData">搜索</a-button>
      <a-button type="primary" @click="handleLink">添加关联项目</a-button>
      <a-button type="primary" @click="handleAdd">创建分发项目</a-button>
    </div>
    <!-- 表格 :scroll="{x: 740, y: tableHeight - 60}" scroll 跟 expandedRowRender 不兼容，没法同时使用不然会多出一行数据-->
    <a-table :loading="loading" :columns="columns" :data-source="list" bordered rowKey="id" @expand="expand" :pagination="this.pagination" @change="changePage">
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
        <a-button type="primary" v-if="list_expanded[record.id]" @click="handleReload(record)">刷新</a-button>
        <a-button type="primary" @click="handleDispatch(record)">分发文件</a-button>
        <a-button type="primary" v-if="record.outGivingProject" @click="handleEditDispatchProject(record)">编辑</a-button>
        <a-button type="primary" v-else @click="handleEditDispatch(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
      <!-- 嵌套表格 -->
      <a-table
        slot="expandedRowRender"
        slot-scope="text"
        :loading="childLoading"
        :columns="childColumns"
        :data-source="text.children"
        :pagination="false"
        :rowKey="(record, index) => record.nodeId + record.projectId + index"
      >
        <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="projectName" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="outGivingResult" slot-scope="text, item" placement="topLeft" :title="text">
          <span>{{ text }} {{ item.errorMsg || "" }}</span>
        </a-tooltip>
        <a-switch slot="projectStatus" slot-scope="text" :checked="text" checked-children="运行中" un-checked-children="未运行" />
        <template slot="child-operation" slot-scope="text, record">
          <a-button :disabled="!record.projectName" type="primary" @click="handleFile(record)">文件</a-button>
          <a-button :disabled="!record.projectName" type="primary" @click="handleConsole(record)">控制台</a-button>
        </template>
      </a-table>
    </a-table>
    <!-- 添加/编辑关联项目 -->
    <a-modal v-model="linkDispatchVisible" width="600px" :title="temp.type === 'edit' ? '编辑关联项目' : '添加关联项目'" @ok="handleLinkDispatchOk" :maskClosable="false" @cancel="clearDispatchList">
      <a-form-model ref="linkDispatchForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="分发 ID" prop="id">
          <a-input v-model="temp.id" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改" />
        </a-form-model-item>
        <a-form-model-item label="分发名称" prop="name">
          <a-input v-model="temp.name" placeholder="分发名称" />
        </a-form-model-item>
        <!-- <a-form-model-item label="分发项目" prop="projectId">
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
        </a-form-model-item> -->
        <a-form-model-item label="分发节点" required>
          <a-list item-layout="horizontal" :data-source="dispatchList">
            <a-list-item slot="renderItem" slot-scope="item, index" v-if="item.status">
              <span>节点: </span>
              <a-select
                placeholder="请选择节点"
                notFoundContent="暂无节点信息"
                style="width: 140px"
                :defaultValue="item.index === '' ? undefined : item.index"
                @change="(value) => handleNodeListChange(value, index)"
                :disabled="item.index === '' ? false : !nodeNameList[item.index].nodeData.openStatus"
              >
                <a-select-option :value="index" v-for="(nodeList, index) in nodeNameList" :key="nodeList.id" :disabled="nodeList.nodeData.openStatus !== 1">
                  {{ nodeList.nodeData.name }}
                </a-select-option>
              </a-select>
              <span>项目: </span>
              <a-select
                style="width: 150px"
                :placeholder="dispatchList[index].placeholder"
                :defaultValue="item.projectId === '' ? undefined : item.projectId"
                notFoundContent="此节点暂无项目"
                @change="(value) => handleProjectChange(value, index)"
                :disabled="dispatchList[index].disabled"
              >
                <a-select-option
                  :value="project.projectId"
                  v-for="project in item.project"
                  :disabled="
                    dispatchList.filter((item, nowIndex) => {
                      return item.projectId === project.projectId && nowIndex !== index;
                    }).length > 0
                  "
                  :key="project.projectId"
                >
                  {{ project.name }}
                </a-select-option>
              </a-select>
              <a-button type="danger" @click="delDispachList(index)" icon="delete"></a-button>
            </a-list-item>
          </a-list>
          <a-button type="primary" @click="addDispachList">添加</a-button>
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
    <!-- 创建/编辑分发项目 -->
    <a-modal v-model="editDispatchVisible" width="600px" :title="temp.type === 'edit' ? '编辑分发项目' : '创建分发项目'" @ok="handleEditDispatchOk" :maskClosable="false">
      <a-form-model ref="editDispatchForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
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

        <a-form-model-item prop="whitelistDirectory" class="jpom-project-whitelist">
          <template slot="label">
            项目白名单路径
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul>
                  <li>白名单路径是指项目文件存放到服务中的文件夹</li>
                  <li>可以到【节点分发】=>【分发白名单配置】修改</li>
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
          <a-input v-model="temp.lib" placeholder="项目存储的文件夹，jar 包存放的文件夹" />
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
          <a-select v-model="temp.logPath" placeholder="请选择日志目录">
            <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="Main Class" prop="mainClass" v-show="temp.runMode && temp.runMode !== 'Jar' && temp.runMode !== 'File'">
          <a-input v-model="temp.mainClass" placeholder="程序运行的 main 类(jar 模式运行可以不填)" />
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
        <a-form-model-item label="分发节点" prop="nodeId">
          <a-select v-model="temp.nodeIdList" mode="multiple" placeholder="请选择分发节点">
            <a-select-option v-for="node in nodeList" :key="node.id">{{ `${node.name} ( ${node.id} )` }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-collapse v-show="temp.runMode && temp.runMode !== 'File'">
          <a-collapse-panel v-for="nodeId in temp.nodeIdList" :key="nodeId" :header="nodeId">
            <a-form-model-item label="WebHooks" prop="token">
              <a-input v-model="temp[`${nodeId}_token`]" placeholder="关闭程序时自动请求,非必填，GET请求" />
            </a-form-model-item>
            <a-form-model-item label="JVM 参数" prop="jvm">
              <a-textarea v-model="temp[`${nodeId}_jvm`]" :auto-size="{ minRows: 3, maxRows: 3 }" placeholder="jvm参数,非必填.如：-Xms512m -Xmx512m" />
            </a-form-model-item>
            <a-form-model-item label="args 参数" prop="args">
              <a-textarea v-model="temp[`${nodeId}_args`]" :auto-size="{ minRows: 3, maxRows: 3 }" placeholder="Main 函数 args 参数，非必填. 如：--server.port=8080" />
            </a-form-model-item>
            <!-- 副本信息 -->
            <a-row v-for="replica in temp[`${nodeId}_javaCopyItemList`]" :key="replica.id">
              <a-form-model-item :label="`副本 ${replica.id} JVM 参数`" prop="jvm">
                <a-textarea v-model="replica.jvm" :auto-size="{ minRows: 3, maxRows: 3 }" class="replica-area" placeholder="jvm参数,非必填.如：-Xms512m -Xmx512m" />
              </a-form-model-item>
              <a-form-model-item :label="`副本 ${replica.id} args 参数`" prop="args">
                <a-textarea v-model="replica.args" :auto-size="{ minRows: 3, maxRows: 3 }" class="replica-area" placeholder="Main 函数 args 参数，非必填. 如：--server.port=8080" />
              </a-form-model-item>
              <a-tooltip placement="topLeft" title="已经添加成功的副本需要在副本管理页面去删除" class="replica-btn-del">
                <a-button :disabled="!replica.deleteAble" type="danger" @click="handleDeleteReplica(nodeId, replica)">删除</a-button>
              </a-tooltip>
            </a-row>
            <!-- 添加副本 -->
            <a-form-model-item>
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
              <a-button type="primary" @click="handleAddReplica(nodeId)">添加副本</a-button>
            </a-form-model-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form-model>
    </a-modal>
    <!-- 分发项目 -->
    <a-modal v-model="dispatchVisible" width="600px" :title="'分发项目----' + temp.name" @ok="handleDispatchOk" :maskClosable="false">
      <a-form-model ref="dispatchForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="方式" prop="type">
          <a-radio-group v-model="temp.type" name="type">
            <a-radio :value="'upload'">上传文件</a-radio>
            <a-radio :value="'download'">远程下载</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="选择分发文件" prop="clearOld" v-if="temp.type == 'upload'">
          <!-- accept=".zip,.tar,.gz,.bz2" -->
          <a-upload :file-list="fileList" :remove="handleRemove" :before-upload="beforeUpload">
            <a-button type="primary"><a-icon type="upload" />选择文件上传</a-button>
          </a-upload>
        </a-form-model-item>
        <a-form-model-item label="远程下载URL" prop="url" v-if="temp.type == 'download'">
          <a-input v-model="temp.url" placeholder="远程下载地址" />
        </a-form-model-item>
        <!-- <a-form-model-item label="是否为压缩包" v-if="temp.type == 'download'">
          <a-switch v-model="temp.unzip" checked-children="是" un-checked-children="否" v-decorator="['unzip', { valuePropName: 'checked' }]" />
        </a-form-model-item> -->
        <a-form-model-item label="清空发布" prop="clearOld">
          <a-switch v-model="temp.clearOld" checked-children="是" un-checked-children="否" />
        </a-form-model-item>
        <a-form-model-item label="是否解压" prop="unzip">
          <a-switch v-model="temp.autoUnzip" checked-children="是" un-checked-children="否" />
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
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerFileVisible" @close="onFileClose">
      <file v-if="drawerFileVisible" :nodeId="temp.nodeId" :projectId="temp.projectId" />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :nodeId="temp.nodeId" :projectId="temp.projectId" />
    </a-drawer>
  </div>
</template>
<script>
import { mapGetters } from "vuex";
import File from "../node/node-layout/project/project-file";
import Console from "../node/node-layout/project/project-console";
import { getDishPatchList, getDispatchProject, editDispatch, editDispatchProject, uploadDispatchFile, getDispatchWhiteList, deleteDisPatch, remoteDownload } from "../../api/dispatch";
import { getNodeListAll, getProjectListAll } from "@/api/node";
import { getProjectData } from "@/api/node-project";
import { itemGroupBy, parseTime } from "@/utils/time";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";
export default {
  components: {
    File,
    Console,
  },
  data() {
    return {
      loading: false,
      childLoading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      accessList: [],
      nodeList: [],
      projectList: [],

      targetKeys: [],
      reqId: "",
      temp: {},
      fileList: [],
      runModeList: ["ClassPath", "Jar", "JarWar", "JavaExtDirsCp", "File"],
      list_expanded: {},
      linkDispatchVisible: false,
      editDispatchVisible: false,
      dispatchVisible: false,
      drawerTitle: "",
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      nodeNameList: [],
      dispatchList: [],
      totalProjectNum: 0,
      columns: [
        { title: "分发 ID", dataIndex: "id", width: 100, ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: "分发名称", dataIndex: "name", width: 150, ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "类型", dataIndex: "outGivingProject", width: 100, ellipsis: true, scopedSlots: { customRender: "outGivingProject" } },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          ellipsis: true,
          sorter: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 380, align: "left" },
      ],
      childColumns: [
        { title: "节点名称", dataIndex: "nodeId", width: 100, ellipsis: true, scopedSlots: { customRender: "nodeId" } },
        { title: "项目名称", dataIndex: "projectName", width: 120, ellipsis: true, scopedSlots: { customRender: "projectName" } },
        { title: "项目状态", dataIndex: "projectStatus", width: 120, ellipsis: true, scopedSlots: { customRender: "projectStatus" } },
        { title: "分发状态", dataIndex: "outGivingStatus", width: 120 },
        { title: "分发结果", dataIndex: "outGivingResult", width: 180, ellipsis: true, scopedSlots: { customRender: "outGivingResult" } },
        { title: "最后分发时间", dataIndex: "lastTime", width: 180, ellipsis: true, scopedSlots: { customRender: "lastTime" } },
        { title: "操作", dataIndex: "child-operation", scopedSlots: { customRender: "child-operation" }, width: 200, align: "left" },
      ],
      rules: {
        id: [{ required: true, message: "请输入项目ID", trigger: "blur" }],
        name: [{ required: true, message: "请输入项目名称", trigger: "blur" }],
        projectId: [{ required: true, message: "请选择项目", trigger: "blur" }],
        runMode: [{ required: true, message: "请选择项目运行方式", trigger: "blur" }],
        whitelistDirectory: [{ required: true, message: "请选择项目白名单路径", trigger: "blur" }],
        lib: [{ required: true, message: "请输入项目文件夹", trigger: "blur" }],
        afterOpt: [{ required: true, message: "请选择发布后操作", trigger: "blur" }],
      },
    };
  },
  computed: {
    ...mapGetters(["getGuideFlag"]),
    filePath() {
      return (this.temp.whitelistDirectory || "") + (this.temp.lib || "");
    },
    // 分页
    pagination() {
      return {
        total: this.listQuery.total,
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
  created() {
    this.loadData();
  },
  methods: {
    // 页面引导
    introGuide() {
      if (this.getGuideFlag) {
        this.$introJs()
          .setOptions({
            hidePrev: true,
            steps: [
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".jpom-project-whitelist"),
                intro: "项目白名单需要在侧边栏菜单<b>分发白名单</b>组件里面去设置",
              },
            ],
          })
          .start();
        return false;
      }
      this.$introJs().exit();
    },

    // 加载数据
    loadData() {
      this.list = [];
      this.loading = true;
      this.childLoading = false;
      getDishPatchList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 加载项目白名单列表
    loadAccesList() {
      getDispatchWhiteList().then((res) => {
        if (res.code === 200) {
          this.accessList = res.data.outGivingArray || [];
        }
      });
    },
    // 展开行
    expand(expanded, record) {
      this.list_expanded[record.id] = expanded;
      this.list_expanded = { ...this.list_expanded };
      if (expanded) {
        this.handleReload(record);
      }
    },
    // // 筛选
    // handleFilter() {
    //   this.loadData();
    // },
    // 关联分发
    handleLink() {
      this.$refs["linkDispatchForm"] && this.$refs["linkDispatchForm"].resetFields();
      this.temp = {
        type: "add",
        id: "",
        name: "",
        projectId: "",
      };
      this.loadNodeList(() => {
        this.loadProjectListAll();
      });
      this.linkDispatchVisible = true;
    },
    // 编辑分发
    handleEditDispatch(record) {
      this.$nextTick(() => {
        this.$refs["linkDispatchForm"] && this.$refs["linkDispatchForm"].resetFields();
      });
      this.loadNodeList(() => {
        this.loadProjectListAll(() => {
          //分发节点重新渲染

          JSON.parse(record.outGivingNodeProjectList).forEach((ele, eleIndex) => {
            let index = "";
            let projects = [];
            this.nodeNameList.forEach((item, idx) => {
              if (item.id === ele.nodeId) {
                index = idx;
                projects = item.projects;
                item.openStatus = false;
              }
            });
            this.temp[`node_${ele.nodeId}_${eleIndex}`] = ele.projectId;
            this.dispatchList.push({
              nodeId: ele.nodeId,
              projectId: ele.projectId,
              index: index,
              project: projects,
              status: true,
            });
            console.log(ele, eleIndex);
          });
          this.temp.type = "edit";
          this.temp.projectId = record.projectId;
          this.temp.name = record.name;
          this.temp.afterOpt = record.afterOpt;
          this.temp.id = record.id;

          this.linkDispatchVisible = true;
        });
      });
    },
    // 选择项目
    selectProject(value) {
      this.targetKeys = [];
      this.nodeList.forEach((node) => {
        node.disabled = true;
      });
      this.nodeProjectMap[value].forEach((nodeId) => {
        this.nodeList.forEach((node) => {
          if (node.key === nodeId) {
            node.disabled = false;
          }
        });
      });
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
      this.$refs["linkDispatchForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 校验分发节点数据
        if (this.dispatchList.length === 0) {
          return this.$message.error("请添加分发节点!");
        } else if (this.dispatchList.length < 2) {
          return this.$message.error("至少选择2个节点项目");
        }

        // 提交
        editDispatch(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2,
            });
            this.targetKeys = [];
            this.$refs["linkDispatchForm"].resetFields();
            this.linkDispatchVisible = false;
            this.clearDispatchList();
            this.handleFilter();
          } else {
            this.targetKeys = [];
          }
        });
      });
    },
    // 添加分发项目
    handleAdd() {
      this.loadNodeList(() => {
        this.temp = {
          type: "add",
          id: "",
          name: "",
          afterOpt: undefined,
          runMode: undefined,
          mainClass: "",
          javaExtDirsCp: "",
          whitelistDirectory: undefined,
          lib: "",
          nodeIdList: [],
        };
        // 添加 javaCopyItemList
        this.nodeList.forEach((node) => {
          this.temp[`${node.key}_javaCopyItemList`] = [];
        });
        this.loadAccesList();

        this.editDispatchVisible = true;
        this.$nextTick(() => {
          this.$refs["editDispatchForm"].resetFields();
          setTimeout(() => {
            this.introGuide();
          }, 500);
        });
      });
    },
    // 编辑分发项目
    handleEditDispatchProject(record) {
      this.$nextTick(() => {
        this.$refs["editDispatchForm"] && this.$refs["editDispatchForm"].resetFields();
        //
        this.temp = {};
        JSON.parse(record.outGivingNodeProjectList).forEach((ele) => {
          const params = {
            id: ele.projectId,
            nodeId: ele.nodeId,
          };
          getProjectData(params).then((res) => {
            if (res.code === 200) {
              // 如果 temp.id 不存在
              if (!this.temp.id) {
                this.temp = {
                  id: res.data.id,
                  name: res.data.name,
                  type: "edit",
                  afterOpt: record.afterOpt,
                  runMode: res.data.runMode,
                  mainClass: res.data.mainClass,
                  javaExtDirsCp: res.data.javaExtDirsCp,
                  whitelistDirectory: res.data.whitelistDirectory,
                  lib: res.data.lib,
                  logPath: res.data.logPath,
                  nodeIdList: [],
                };
              }
              // 添加 nodeIdList
              this.temp.nodeIdList.push(ele.nodeId);
              // 添加 jvm token args
              this.temp[`${ele.nodeId}_jvm`] = res.data.jvm || "";
              this.temp[`${ele.nodeId}_token`] = res.data.token || "";
              this.temp[`${ele.nodeId}_args`] = res.data.args || "";
              // 添加 javaCopyItemList
              this.temp[`${ele.nodeId}_javaCopyItemList`] = res.data.javaCopyItemList || [];
            }
          });
        });
        // 加载其他数据
        this.loadAccesList();

        this.editDispatchVisible = true;
      });
    },
    // 添加副本
    handleAddReplica(nodeId) {
      const $chars = "ABCDEFGHJKMNPQRSTWXYZ0123456789";
      /****默认去掉了容易混淆的字符oOLl,9gq,Vv,Uu,I1****/
      const maxPos = $chars.length;
      let repliccaId = "";
      for (let i = 0; i < 2; i++) {
        repliccaId += $chars.charAt(Math.floor(Math.random() * maxPos));
      }
      this.temp[`${nodeId}_javaCopyItemList`].push({
        id: repliccaId,
        jvm: "",
        args: "",
        deleteAble: true,
      });
      this.temp = { ...this.temp };
    },
    // 移除副本
    handleDeleteReplica(nodeId, reeplica) {
      const index = this.temp[`${nodeId}_javaCopyItemList`].findIndex((element) => element.id === reeplica.id);
      const newList = this.temp[`${nodeId}_javaCopyItemList`].slice();
      newList.splice(index, 1);
      this.temp[`${nodeId}_javaCopyItemList`] = newList;
    },
    // 提交创建分发项目
    handleEditDispatchOk() {
      // 检验表单
      this.$refs["editDispatchForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 检查
        if (this.temp.nodeIdList.length < 2) {
          this.$notification.warn({
            message: "请至少选择 2 个节点",
            duration: 2,
          });
          return false;
        }
        // 设置 reqId
        this.temp.reqId = this.reqId;
        // 设置节点
        this.temp.nodeIdList.forEach((key) => {
          this.temp[`add_${key}`] = key;
          // 设置副本
          this.temp[`${key}_javaCopyIds`] = "";
          this.temp[`${key}_javaCopyItemList`]?.forEach((element) => {
            this.temp[`${key}_javaCopyIds`] += `${element.id},`;
            this.temp[`${key}_jvm_${element.id}`] = element.jvm;
            this.temp[`${key}_args_${element.id}`] = element.args;
          });
          // 移除多余的后缀 ,
          this.temp[`${key}_javaCopyIds`] = this.temp[`${key}_javaCopyIds`].substring(0, this.temp[`${key}_javaCopyIds`].length - 1);
        });
        // 提交
        editDispatchProject(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2,
            });
            this.$refs["editDispatchForm"].resetFields();
            this.editDispatchVisible = false;
            this.handleFilter();
          }
        });
      });
    },
    // 刷新
    handleReload(record) {
      this.childLoading = true;
      getDispatchProject(record.id).then((res) => {
        if (res.code === 200) {
          record.children = res.data;
        }
        this.childLoading = false;
      });
    },
    // 处理分发
    handleDispatch(record) {
      this.temp = Object.assign({ type: "upload" }, record);
      this.dispatchVisible = true;
      this.$refs["dispatchForm"] && this.$refs["dispatchForm"].resetFields();
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
      this.$refs["dispatchForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const key = this.temp.type;
        if (this.temp.type == "upload") {
          // 判断文件
          if (this.fileList.length === 0) {
            this.$notification.error({
              message: "请选择文件",
              duration: 2,
            });
            return false;
          }
          // 上传文件
          const formData = new FormData();
          this.$message.loading({ content: "正在上传文件...", key, duration: 0 });
          formData.append("file", this.fileList[0]);
          formData.append("id", this.temp.id);
          formData.append("afterOpt", this.temp.afterOpt);
          formData.append("clearOld", this.temp.clearOld);
          formData.append("autoUnzip", this.temp.autoUnzip);
          uploadDispatchFile(formData).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2,
              });
              this.$message.success({ content: "上传成功,开始分发!", key, duration: 2 });
              this.$refs["dispatchForm"].resetFields();
              this.fileList = [];
              this.loadData();
              this.dispatchVisible = false;
            }
          });
          return true;
        }
        if (this.temp.type == "download") {
          if (!this.temp.url) {
            this.$notification.error({
              message: "请填写远程URL",
              duration: 2,
            });
            return false;
          }
          // 远程下载
          this.$message.loading({ content: "正在下载文件...", key, duration: 0 });
          remoteDownload(this.temp).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2,
              });
              this.$message.success({ content: "下载成功,开始分发!", key, duration: 2 });
              //this.$refs["dispatchForm"].resetFields();
              this.loadData();
              this.dispatchVisible = false;
            }
          });
          return true;
        }
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除分发信息么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteDisPatch(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2,
              });
              this.handleFilter();
            }
          });
        },
      });
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `文件管理(${this.temp.projectId})`;
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
    },
    loadProjectListAll(fn) {
      getProjectListAll().then((res) => {
        if (res.code === 200) {
          this.totalProjectNum = res.data ? res.data.length : 0;
          this.nodeNameList = itemGroupBy(res.data, "nodeId", "id", "projects");
          this.nodeNameList = this.nodeNameList.map((item) => {
            item.nodeData = this.nodeList.filter((node) => node.id === item.id)[0];
            return item;
          });
          fn && fn();
          console.log(this.nodeNameList);
        }
      });
    },
    // 加载节点以及项目
    loadNodeList(fn) {
      this.nodeList = [];
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data;
          fn && fn();
        }
      });
    },
    // 选择节点
    handleNodeListChange(value, index) {
      if (this.nodeNameList[value].projects.length === 0) {
        this.dispatchList[index].placeholder = "此节点暂无项目";
        this.dispatchList[index].disabled = true;
      } else {
        this.dispatchList[index].placeholder = "请选择项目";
        this.dispatchList[index].disabled = false;
      }
      //this.projectNameList = this.nodeNameList[value].projects;
      this.nodeNameList[value].openStatus = false;
      this.dispatchList[index].project = this.nodeNameList[value].projects;
      this.dispatchList[index].nodeId = this.nodeNameList[value].id;
      this.dispatchList[index].index = value;
    },
    // 选择项目
    handleProjectChange(value, index) {
      this.dispatchList[index].projectId = value;
      this.temp["node_" + this.dispatchList[index].nodeId + "_" + index] = value;
    },
    // 添加分发
    addDispachList() {
      if (this.dispatchList.length >= this.totalProjectNum) {
        this.$message.error("已无更多节点项目，请先创建项目");
        return false;
      }
      this.dispatchList.push({ nodeId: "", projectId: "", index: "", project: [], status: true, placeholder: "请先选择节点", disabled: true });
    },
    // 删除分发
    delDispachList(value) {
      if (this.dispatchList[value].index !== "") {
        this.nodeNameList[this.dispatchList[value].index].openStatus = true;
      }
      delete this.temp[`node_${this.dispatchList[value].nodeId}`];
      this.dispatchList[value].status = false;
      this.dispatchList.splice(value, 1);
    },
    // 清理缓存
    clearDispatchList() {
      this.dispatchList = [];
      for (let node in this.nodeNameList) {
        this.nodeNameList[node].openStatus = true;
      }
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
.replica-area {
  width: 300px;
}
.replica-btn-del {
  position: absolute;
  right: 0px;
  top: 74px;
}
</style>
