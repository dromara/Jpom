<template>
  <div class="full-content">
    <template v-if="this.useSuggestions">
      <a-result title="当前工作空间还没有项目并且也没有任何节点" sub-title="需要您先添加资产机器再分配机器节点（逻辑节点）到当前工作空间"> </a-result>
    </template>
    <a-table v-else :data-source="projList" :columns="columns" size="middle" bordered :pagination="pagination" @change="changePage" :row-selection="rowSelection" :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-select v-if="!nodeId" v-model="listQuery.nodeId" allowClear placeholder="请选择节点" class="search-input-item">
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-select v-model="listQuery.group" allowClear placeholder="请选择分组" class="search-input-item" @change="getNodeProjectData">
            <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
          </a-select>
          <a-input v-model="listQuery['%name%']" @pressEnter="getNodeProjectData" placeholder="搜索项目" class="search-input-item" />

          <a-select v-model="listQuery.runMode" allowClear placeholder="项目类型" class="search-input-item">
            <a-select-option v-for="item in runModeList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="getNodeProjectData">搜索</a-button>
          </a-tooltip>

          <a-dropdown v-if="this.selectedRowKeys && this.selectedRowKeys.length">
            <a-button type="primary"> 批量操作 <a-icon type="down" /> </a-button>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button type="primary" @click="batchStart">批量启动</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="primary" @click="batchRestart">批量重启</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="danger" @click="batchStop">批量关闭</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
          <a-button v-else type="primary" :disabled="true"> 批量操作 <a-icon type="down" /> </a-button>

          <a-tooltip placement="topLeft" title="清除服务端缓存节点所有的项目信息, 需要重新同步：可以通过节点列表逐个同步">
            <a-button type="danger" @click="delAll()" icon="delete"> 删除缓存 </a-button>
          </a-tooltip>

          <a-button type="primary" @click="openAdd">创建项目</a-button>

          <a-button v-if="nodeId" icon="download" type="primary" @click="handlerExportData()">导出</a-button>
          <a-dropdown v-if="nodeId">
            <a-menu slot="overlay">
              <a-menu-item key="1"> <a-button type="primary" @click="handlerImportTemplate()">下载导入模板</a-button> </a-menu-item>
            </a-menu>

            <a-upload name="file" accept=".csv" action="" :showUploadList="false" :multiple="false" :before-upload="importBeforeUpload">
              <a-button type="primary" icon="upload"> 导入 <a-icon type="down" /> </a-button>
            </a-upload>
          </a-dropdown>

          <a-tooltip>
            <template slot="title">
              <div>
                <ul>
                  <li>状态数据是异步获取有一定时间延迟</li>
                  <li>在单页列表里面 file 类型项目将自动排序到最后</li>
                </ul>
              </div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>
          <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="silenceLoadData" />
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text, record" placement="topLeft" :title="text">
        <a-button type="link" style="padding: 0" size="small" @click="openEdit(record)">
          <a-icon v-if="record.outGivingProject" type="apartment" />
          <span>{{ text }}</span>
        </a-button>
      </a-tooltip>
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <a-button type="link" style="padding: 0" size="small" @click="toNode(text)">
          <span>{{ nodeMap[text] }}</span>
          <a-icon type="fullscreen" />
        </a-button>
      </a-tooltip>
      <a-tooltip slot="path" slot-scope="text, item" placement="topLeft" :title="item.whitelistDirectory + item.lib">
        <span>{{ item.whitelistDirectory + item.lib }}</span>
      </a-tooltip>

      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <template slot="status" slot-scope="text, record">
        <template v-if="projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].error">
          <a-tooltip :title="projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].error">
            <a-icon type="warning" />
          </a-tooltip>
        </template>
        <template v-else>
          <a-tooltip
            v-if="noFileModes.includes(record.runMode)"
            :title="`状态操作请到控制台中控制   ${
              (projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].statusMsg) || ''
            }`"
          >
            <a-switch
              :checked="projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].pid > 0"
              disabled
              checked-children="开"
              un-checked-children="关"
            />
          </a-tooltip>
          <span v-else>-</span>
        </template>
      </template>

      <a-tooltip
        slot="port"
        slot-scope="text, record"
        placement="topLeft"
        :title="`进程号：${(
          (projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].pids) || [
            (projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].pid) || '-',
          ]
        ).join(',')} / 端口号：${(projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].port) || '-'}`"
      >
        <span
          >{{ (projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].port) || "-" }}/{{
            (
              (projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].pids) || [
                (projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].pid) || "-",
              ]
            ).join(",")
          }}</span
        >
      </a-tooltip>
      <template slot="operation" slot-scope="text, record, index">
        <a-space>
          <a-button size="small" type="primary" @click="handleFile(record)">文件</a-button>
          <template v-if="noFileModes.includes(record.runMode)">
            <a-button size="small" type="primary" @click="handleConsole(record)">控制台</a-button>
          </template>
          <template v-else>
            <a-tooltip title="文件类型没有控制台功能"> <a-button size="small" type="primary" :disabled="true">控制台</a-button></a-tooltip>
          </template>

          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
              更多
              <a-icon type="down" />
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <template v-if="noFileModes.includes(record.runMode)">
                  <a-button size="small" type="primary" @click="handleTrigger(record)">触发器</a-button>
                </template>
                <template v-else>
                  <a-tooltip title="文件类型没有触发器功能"> <a-button size="small" type="primary" :disabled="true">触发器</a-button></a-tooltip>
                </template>
              </a-menu-item>
              <a-menu-item v-if="noFileModes.includes(record.runMode)">
                <a-button size="small" type="primary" @click="handleLogBack(record)">项目日志 </a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="primary" @click="copyItem(record)">复制</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleDelete(record)">逻辑删除</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleDelete(record, 'thorough')">彻底删除</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="migrateWorkspace(record)">迁移工作空间</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="primary" :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1" @click="sortItemHander(record, index, 'top')">置顶</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="primary" :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1" @click="sortItemHander(record, index, 'up')">上移</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="primary" :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total" @click="sortItemHander(record, index, 'down')">
                  下移
                </a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
    </a-table>
    <!-- 项目文件组件 -->
    <a-drawer destroyOnClose :title="drawerTitle" placement="right" width="85vw" :visible="drawerFileVisible" @close="onFileClose">
      <file v-if="drawerFileVisible" :nodeId="temp.nodeId" :projectId="temp.projectId" @goConsole="goConsole" @goReadFile="goReadFile" />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer destroyOnClose :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :id="temp.id" :nodeId="temp.nodeId" :projectId="temp.projectId" @goFile="goFile" />
    </a-drawer>
    <!-- 项目跟踪文件组件 -->
    <a-drawer destroyOnClose :title="drawerTitle" placement="right" width="85vw" :visible="drawerReadFileVisible" @close="onReadFileClose">
      <file-read v-if="drawerReadFileVisible" :nodeId="temp.nodeId" :readFilePath="temp.readFilePath" :id="temp.id" :projectId="temp.projectId" @goFile="goFile" />
    </a-drawer>
    <!-- 批量操作状态 -->
    <a-modal destroyOnClose v-model="batchVisible" :title="temp.title" :footer="null" @cancel="batchClose">
      <a-list bordered :data-source="temp.data">
        <a-list-item slot="renderItem" slot-scope="item">
          <a-list-item-meta>
            <!-- <template #description> :="item.whitelistDirectory" </template> -->
            <a slot="title"> {{ item.name }}</a>
          </a-list-item-meta>
          <div>
            <a-tooltip :title="`${item.cause || '未开始'}`">{{ item.cause || "未开始" }} </a-tooltip>
          </div>
        </a-list-item>
      </a-list>
    </a-modal>
    <!-- 触发器 -->
    <a-modal destroyOnClose v-model="triggerVisible" title="触发器" width="50%" :footer="null" :maskClosable="false">
      <a-form-model ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template slot="tabBarExtraContent">
            <a-tooltip title="重置触发器 token 信息,重置后之前的触发器 token 将失效">
              <a-button type="primary" size="small" @click="resetTrigger">重置</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" tab="执行">
            <a-space style="display: block" direction="vertical" align="baseline">
              <a-alert message="温馨提示" type="warning">
                <template slot="description">
                  <ul>
                    <li>单个触发器地址中：第一个随机字符串为项目ID(服务端)，第二个随机字符串为 token</li>
                    <li>重置为重新生成触发地址,重置成功后之前的触发器地址将失效,触发器绑定到生成触发器到操作人上,如果将对应的账号删除触发器将失效</li>
                    <li>批量触发参数 BODY json： [ { "id":"1", "token":"a","action":"status" } ]</li>
                  </ul>
                </template>
              </a-alert>

              <a-alert
                :key="item.value"
                v-for="item in triggerUses"
                v-clipboard:copy="`${temp.triggerUrl}?action=${item.value}`"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' });
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' });
                  }
                "
                type="info"
                :message="`${item.desc}触发器地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>GET</a-tag> <span>{{ `${temp.triggerUrl}?action=${item.value}` }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>

              <a-alert
                v-clipboard:copy="temp.batchTriggerUrl"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' });
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' });
                  }
                "
                type="info"
                :message="`批量触发器地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>POST</a-tag> <span>{{ temp.batchTriggerUrl }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
        </a-tabs>
      </a-form-model>
    </a-modal>
    <!-- 编辑区 -->
    <a-modal
      destroyOnClose
      v-model="editProjectVisible"
      width="60vw"
      title="编辑项目"
      @ok="
        () => {
          this.$refs.edit.handleOk();
        }
      "
      :maskClosable="false"
    >
      <a-form-model :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="选择节点">
          <a-select v-model="temp.nodeId" :disabled="!!temp.nodeId" allowClear placeholder="请选择节点">
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>

      <project-edit
        v-if="temp.nodeId"
        ref="edit"
        @close="
          () => {
            editProjectVisible = false;
            this.getNodeProjectData();
            this.loadGroupList();
          }
        "
        :data="temp"
        :nodeId="temp.nodeId"
        :projectId="temp.id"
      />
    </a-modal>
    <!-- 迁移到其他工作空间 -->
    <a-modal destroyOnClose v-model="migrateWorkspaceVisible" width="50vw" title="迁移到其他工作空间" @ok="migrateWorkspaceOk" :maskClosable="false">
      <a-space style="display: block" direction="vertical">
        <a-alert message="温馨提示" type="warning">
          <template slot="description">
            项目可能支持关联如下数据：
            <ul>
              <li>
                在线构建（构建关联仓库、构建历史）

                <ol>
                  <li>如果关联的构建关联的仓库被多个构建绑定（使用）不能迁移</li>
                  <li>仓库自动迁移后可能会重复存在请手动解决</li>
                </ol>
              </li>
              <li>节点分发【暂不支持迁移】</li>
              <li>项目监控 【暂不支持迁移】</li>
              <li>日志阅读 【暂不支持迁移】</li>
            </ul>
          </template>
        </a-alert>
        <a-alert message="风险提醒" type="error">
          <template slot="description">
            <ul>
              <li>如果垮机器（资产机器）迁移之前机器中的项目数据仅是逻辑删除（项目文件和日志均会保留）</li>
              <li>迁移操作不具有事务性质，如果流程被中断或者限制条件不满足可能产生冗余数据！！！！</li>
              <li>迁移前您检查迁出机器和迁入机器的连接状态和网络状态避免未知错误或者中断造成流程失败产生冗余数据！！！！</li>
            </ul>
          </template>
        </a-alert>
      </a-space>
      <a-form-model :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item> </a-form-model-item>
        <a-form-model-item label="选择工作空间" prop="workspaceId">
          <a-select show-search option-filter-prop="children" v-model="temp.workspaceId" placeholder="请选择工作空间" @change="loadMigrateWorkspaceNodeList">
            <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="选择逻辑节点" prop="nodeId">
          <a-select show-search option-filter-prop="children" v-model="temp.nodeId" placeholder="请选择逻辑节点">
            <a-select-option v-for="item in migrateWorkspaceNodeList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 日志备份 -->
    <a-modal destroyOnClose v-model="lobbackVisible" title="日志备份列表" width="850px" :footer="null" :maskClosable="false">
      <ProjectLog v-if="lobbackVisible" :nodeId="temp.nodeId" :projectId="temp.projectId"></ProjectLog>
    </a-modal>
  </div>
</template>
<script>
import { delAllProjectCache, getNodeListAll, getProjectList, sortItemProject } from "@/api/node";
import {
  getRuningProjectInfo,
  noFileModes,
  operateProject,
  runModeList,
  getProjectTriggerUrl,
  getProjectGroupAll,
  deleteProject,
  migrateWorkspace,
  importTemplate,
  exportData,
  importData,
} from "@/api/node-project";
import File from "@/pages/node/node-layout/project/project-file";
import Console from "../node/node-layout/project/project-console";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, concurrentExecution, itemGroupBy, parseTime } from "@/utils/const";
import FileRead from "@/pages/node/node-layout/project/project-file-read";
import ProjectEdit from "@/pages/node/node-layout/project/project-edit";
import Vue from "vue";
import { mapGetters } from "vuex";
import { getWorkSpaceListAll } from "@/api/workspace";
import ProjectLog from "@/pages/node/node-layout/project/project-log.vue";
export default {
  components: {
    File,
    Console,
    FileRead,
    ProjectEdit,
    ProjectLog,
  },
  props: {
    nodeId: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      projList: [],
      projectStatusMap: {},
      groupList: [],
      runModeList: runModeList,
      selectedRowKeys: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      noFileModes: noFileModes,
      nodeMap: {},
      drawerTitle: "",
      loading: true,
      temp: {},
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      drawerReadFileVisible: false,
      batchVisible: false,

      columns: [
        { title: "项目名称", dataIndex: "name", width: 200, ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "项目分组", dataIndex: "group", sorter: true, width: "100px", ellipsis: true, scopedSlots: { customRender: "group" } },
        { title: "节点名称", dataIndex: "nodeId", width: 90, ellipsis: true, scopedSlots: { customRender: "nodeId" } },
        {
          title: "项目路径",
          dataIndex: "path",
          ellipsis: true,
          width: 120,
          scopedSlots: { customRender: "path" },
        },
        { title: "运行状态", dataIndex: "status", align: "center", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "端口/PID", dataIndex: "port", width: 100, ellipsis: true, scopedSlots: { customRender: "port" } },

        { title: "运行方式", dataIndex: "runMode", width: 90, ellipsis: true, scopedSlots: { customRender: "runMode" } },
        {
          title: "webhook",
          dataIndex: "token",
          width: 120,
          ellipsis: true,
          scopedSlots: { customRender: "tooltip" },
        },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          ellipsis: true,
          sorter: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        { title: "操作", dataIndex: "operation", align: "center", fixed: "right", scopedSlots: { customRender: "operation" }, width: "190px" },
      ],
      triggerVisible: false,
      triggerUses: [
        { desc: "查看状态", value: "status" },
        { desc: "启动项目", value: "start" },
        { desc: "停止项目", value: "stop" },
        { desc: "重启项目", value: "restart" },
      ],
      editProjectVisible: false,
      countdownTime: Date.now(),
      refreshInterval: 5,
      migrateWorkspaceVisible: false,
      workspaceList: [],
      migrateWorkspaceNodeList: [],
      lobbackVisible: false,
    };
  },
  computed: {
    ...mapGetters(["getUserInfo"]),
    filePath() {
      return (this.temp.whitelistDirectory || "") + (this.temp.lib || "");
    },
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        onChange: this.onSelectChange,
        columnWidth: "40px",
        getCheckboxProps: this.getCheckboxProps,
      };
    },
    useSuggestions() {
      if (this.loading) {
        // 加载中不提示
        return false;
      }
      if (!this.getUserInfo || !this.getUserInfo.systemUser) {
        // 没有登录或者不是超级管理员
        return false;
      }
      if (Object.keys(this.nodeMap).length) {
        return false;
      }
      return true;
      // if (this.listQuery.page !== 1 || this.listQuery.total > 0) {
      //   // 不是第一页 或者总记录数大于 0
      //   return false;
      // }
      // // 判断是否存在搜索条件
      // const nowKeys = Object.keys(this.listQuery);
      // const defaultKeys = Object.keys(PAGE_DEFAULT_LIST_QUERY);
      // const dictOrigin = nowKeys.filter((item) => !defaultKeys.includes(item));
      // return dictOrigin.length === 0;
    },
  },
  mounted() {
    getNodeListAll().then((res) => {
      if (res.code === 200) {
        res.data.forEach((item) => {
          this.nodeMap = { ...this.nodeMap, [item.id]: item.name };
        });
        this.getNodeProjectData();
      }
    });
    this.countdownTime = Date.now() + this.refreshInterval * 1000;
    //
    this.loadGroupList();
  },
  methods: {
    silenceLoadData() {
      if (this.$attrs.routerUrl !== this.$route.path) {
        // 重新计算倒计时
        this.countdownTime = Date.now() + this.refreshInterval * 1000;
        return;
      }
      this.getNodeProjectData(null, false);
    },
    getNodeProjectData(pointerEvent, loading) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.nodeId && (this.listQuery.nodeId = this.nodeId);

      getProjectList(this.listQuery, loading)
        .then((res) => {
          if (res.code === 200) {
            const resultList = res.data.result;

            const tempList = resultList.filter((item) => item.runMode !== "File");
            const fileList = resultList.filter((item) => item.runMode === "File");
            this.projList = tempList.concat(fileList);

            this.listQuery.total = res.data.total;

            const nodeProjects = itemGroupBy(this.projList, "nodeId");
            this.getRuningProjectInfo(nodeProjects);

            // 重新计算倒计时
            this.countdownTime = Date.now() + this.refreshInterval * 1000;
          }
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadGroupList() {
      getProjectGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data;
        }
      });
    },
    getRuningProjectInfo(nodeProjects) {
      if (nodeProjects.length <= 0) {
        return;
      }
      concurrentExecution(
        nodeProjects.map((item, index) => {
          return index;
        }),
        3,
        (citem) => {
          // console.log(i);
          const data = nodeProjects[citem];
          return new Promise((resolve, reject) => {
            const ids = data.data.map((item) => {
              return item.projectId;
            });
            if (ids.length <= 0) {
              return;
            }
            const tempParams = {
              nodeId: data.type,
              ids: JSON.stringify(ids),
            };
            getRuningProjectInfo(tempParams, "noTip")
              .then((res2) => {
                if (res2.code === 200) {
                  this.projectStatusMap = { ...this.projectStatusMap, [data.type]: res2.data };
                  resolve();
                } else {
                  const data2 = {};
                  this.projList.forEach((item) => {
                    data2[item.projectId] = {
                      port: 0,
                      pid: 0,
                      error: res2.msg,
                    };
                  });
                  this.projectStatusMap = { ...this.projectStatusMap, [data.type]: data2 };
                  reject();
                }
                // this.getRuningProjectInfo(nodeProjects, i + 1);
              })
              .catch(() => {
                const data2 = {};
                this.projList.forEach((item) => {
                  data2[item.projectId] = {
                    port: 0,
                    pid: 0,
                    error: "网络异常",
                  };
                });
                this.projectStatusMap = { ...this.projectStatusMap, [data.type]: data2 };
                reject();
              });
          });
        }
      );
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `文件管理(${this.temp.name})`;
      this.drawerFileVisible = true;
    },
    // 关闭文件管理对话框
    onFileClose() {
      this.drawerFileVisible = false;
      this.getNodeProjectData();
    },
    // 控制台
    handleConsole(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `控制台(${this.temp.name})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false;
      this.getNodeProjectData();
    },
    //前往文件
    goFile() {
      // 关闭控制台
      this.onConsoleClose();
      this.onReadFileClose();
      this.handleFile(this.temp);
    },
    //前往控制台
    goConsole() {
      //关闭文件
      this.onFileClose();
      this.handleConsole(this.temp);
    },
    // 跟踪文件
    goReadFile(path, filename) {
      this.onFileClose();
      this.drawerReadFileVisible = true;
      this.temp.readFilePath = (path + "/" + filename).replace(new RegExp("//", "gm"), "/");
      this.drawerTitle = `跟踪文件(${filename})`;
    },
    onReadFileClose() {
      this.drawerReadFileVisible = false;
    },
    //选中项目
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys;
    },
    batchClose() {
      this.batchVisible = false;
      this.getNodeProjectData();
      this.selectedRowKeys = [];
    },
    /**
     * 将选中的 key 转为 data
     */
    selectedRowKeysToId() {
      return this.selectedRowKeys.map((item) => {
        return {
          projectId: this.projList[item]?.projectId,
          nodeId: this.projList[item]?.nodeId,
          runMode: this.projList[item]?.runMode,
          name: this.projList[item]?.name,
        };
      });
    },
    // 更新数据
    updateBatchData(index, data2) {
      const data = this.temp.data;
      data[index] = { ...data[index], ...data2 };

      this.temp = {
        ...this.temp,
        data: [...data],
      };
    },
    //批量开始
    batchStart() {
      if (this.selectedRowKeys.length <= 0) {
        this.$notification.warning({
          message: "请选中要启动的项目",
        });
        return;
      }
      this.temp = {
        title: "批量启动",
        data: this.selectedRowKeysToId(),
      };

      this.batchVisible = true;
      this.batchOptInfo(0, "启动", operateProject, "start");
    },
    // 批量操作
    batchOptInfo(index, msg, api, opt) {
      if (index >= (this.temp?.data?.length || -1)) {
        return;
      }
      const value = this.temp.data[index];
      value.cause = msg + "中";
      this.updateBatchData(index, value);
      if (value.runMode !== "File") {
        const params = {
          nodeId: value.nodeId,
          id: value.projectId,
          opt: opt,
        };

        api(params)
          .then((data) => {
            value.cause = data.msg;
            this.updateBatchData(index, value);
            this.batchOptInfo(index + 1, msg, api);
          })
          .catch(() => {
            value.cause = msg + "失败";
            this.updateBatchData(index, value);
            this.batchOptInfo(index + 1, msg, api);
          });
      } else {
        value.cause = "跳过";
        this.updateBatchData(index, value);
        this.batchOptInfo(index + 1, msg, api);
      }
    },

    //批量重启
    batchRestart() {
      if (this.selectedRowKeys.length <= 0) {
        this.$notification.warning({
          message: "请选中要重启的项目",
        });
        return;
      }
      this.temp = {
        title: "批量重新启动",
        data: this.selectedRowKeysToId(),
      };
      this.batchVisible = true;
      this.batchOptInfo(0, "重启", operateProject, "restart");
    },

    //批量关闭
    batchStop() {
      if (this.selectedRowKeys.length <= 0) {
        this.$notification.warning({
          message: "请选中要关闭的项目",
        });
      }
      this.temp = {
        title: "批量关闭启动",
        data: this.selectedRowKeysToId(),
      };
      this.batchVisible = true;
      this.batchOptInfo(0, "停止", operateProject, "stop");
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
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.getNodeProjectData();
    },
    delAll() {
      this.$confirm({
        title: "系统提示",
        content: "确定要清除服务端所有的项目缓存信息吗？清除后需要重新同步节点项目才能正常使用项目相关功能",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          delAllProjectCache().then((res) => {
            if (res.code == 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.getNodeProjectData();
            }
          });
        },
      });
    },
    // 排序
    sortItemHander(record, index, method) {
      const msgData = {
        top: "确定要将此数据置顶吗？",
        up: "确定要将此数上移吗？",
        down: "确定要将此数据下移吗？下移操作可能因为列表后续数据没有排序值操作无效！",
      };
      let msg = msgData[method] || "确定要操作吗？";
      if (!record.sortValue) {
        msg += " 当前数据为默认状态,操后上移或者下移可能不会达到预期排序,还需要对相关数据都操作后才能达到预期排序";
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.projList[method === "top" ? index : method === "up" ? index - 1 : index + 1].id;
      this.$confirm({
        title: "系统提示",
        content: msg,
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 解锁
          sortItemProject({
            id: record.id,
            method: method,
            compareId: compareId,
          }).then((res) => {
            if (res.code == 200) {
              this.$notification.success({
                message: res.msg,
              });

              this.getNodeProjectData();
              return false;
            }
          });
        },
      });
      // console.log(record, index, method);
    },
    // 触发器
    handleTrigger(record) {
      this.temp = Object.assign({}, record);
      this.tempVue = Vue;
      getProjectTriggerUrl({
        id: record.id,
      }).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res);
          this.triggerVisible = true;
        }
      });
    },
    // 重置触发器
    resetTrigger() {
      getProjectTriggerUrl({
        id: this.temp.id,
        rest: "rest",
      }).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.fillTriggerResult(res);
        }
      });
    },
    fillTriggerResult(res) {
      this.temp.triggerUrl = `${location.protocol}//${location.host}${res.data.triggerUrl}`;
      this.temp.batchTriggerUrl = `${location.protocol}//${location.host}${res.data.batchTriggerUrl}`;

      this.temp = { ...this.temp };
    },
    toNode(nodeId) {
      const newpage = this.$router.resolve({
        name: "node_" + nodeId,
        path: "/node/list",
        query: {
          ...this.$route.query,
          nodeId: nodeId,
          pId: "manage",
          id: "manageList",
        },
      });
      window.open(newpage.href, "_blank");
    },
    // 打开编辑
    openEdit(data) {
      this.temp = {
        id: data.projectId,
        nodeId: data.nodeId,
      };

      this.editProjectVisible = true;
    },
    // 复制
    copyItem(record) {
      const temp = Object.assign({}, record);
      delete temp.id;
      delete temp.createTimeMillis;
      delete temp.outGivingProject;
      this.temp = { ...temp, name: temp.name + "副本", id: temp.projectId + "_copy", lib: temp.lib + "_copy" };

      this.editProjectVisible = true;
    },
    // 打开编辑
    openAdd() {
      this.temp = {
        nodeId: this.listQuery.nodeId,
      };
      this.editProjectVisible = true;
    },
    // 删除
    handleDelete(record, thorough) {
      this.$confirm({
        title: "系统提示",
        content: thorough
          ? "真的要彻底删除项目么？彻底项目会自动删除项目相关文件奥(包含项目日志，日志备份，项目文件)"
          : "真的要删除项目么？删除项目不会删除项目相关文件奥,建议先清理项目相关文件再删除项目",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          const params = {
            nodeId: record.nodeId,
            id: record.projectId,
            thorough: thorough,
          };
          deleteProject(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });

              this.getNodeProjectData();
            }
          });
        },
      });
    },
    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data;
        }
      });
    },
    // 迁移到其他工作空间
    migrateWorkspace(record) {
      this.temp = { id: record.id, originalWorkspaceId: record.workspaceId, originalNodeId: record.nodeId };
      // delete this.temp.workspaceId;

      this.migrateWorkspaceVisible = true;
      this.loadWorkSpaceListAll();
    },
    // 获取节点
    loadMigrateWorkspaceNodeList() {
      if (!this.temp.workspaceId) {
        return;
      }
      getNodeListAll({
        workspaceId: this.temp.workspaceId,
      }).then((res) => {
        this.migrateWorkspaceNodeList = res.data || [];
      });
    },
    // 迁移确认
    migrateWorkspaceOk() {
      if (!this.temp.workspaceId) {
        this.$notification.warn({
          message: "请选择工作空间",
        });
        return false;
      }
      if (!this.temp.nodeId) {
        this.$notification.warn({
          message: "请选择逻辑节点",
        });
        return false;
      }
      // 同步
      migrateWorkspace({
        id: this.temp.id,
        toWorkspaceId: this.temp.workspaceId,
        toNodeId: this.temp.nodeId,
      }).then((res) => {
        if (res.code == 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.migrateWorkspaceVisible = false;
          this.getNodeProjectData();
          return false;
        }
      });
    },
    // 下载导入模板
    handlerImportTemplate() {
      window.open(
        importTemplate({
          nodeId: this.listQuery.nodeId,
        }),
        "_blank"
      );
    },
    handlerExportData() {
      window.open(exportData({ ...this.listQuery }), "_blank");
    },
    // 导入
    importBeforeUpload(file) {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("nodeId", this.listQuery.nodeId);
      importData(formData).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.loadData();
        }
      });
    },
    // 日志备份列表
    handleLogBack(record) {
      this.temp = Object.assign({}, record);
      this.lobbackVisible = true;
    },
  },
};
</script>
<style scoped>
/deep/ .ant-statistic div {
  display: inline-block;
}

/deep/ .ant-statistic-content-value,
/deep/ .ant-statistic-content {
  font-size: 16px;
}
</style>
