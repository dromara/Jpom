<template>
  <div class="full-content">
    <template v-if="this.getUserInfo && this.getUserInfo.systemUser && !this.loading && this.listQuery.total <= 0">
      <a-result title="当前工作空间还没有SSH" sub-title="请到【系统管理】-> 【资产管理】-> 【SSH管理】添加SSH，或者将已添加的SSH授权关联、分配到此工作空间">
        <template #extra>
          <router-link to="/system/assets/ssh-list"> <a-button key="console" type="primary">现在就去</a-button></router-link>
        </template>
      </a-result>
    </template>
    <!-- 数据表格 -->
    <a-table v-else :data-source="list" :columns="columns" size="middle" :pagination="pagination" @change="changePage" bordered rowKey="id" :row-selection="rowSelection">
      <template slot="title">
        <a-space>
          <a-input class="search-input-item" @pressEnter="loadData" v-model="listQuery['%name%']" placeholder="ssh名称" />
          <a-select show-search option-filter-prop="children" v-model="listQuery.group" allowClear placeholder="分组" class="search-input-item">
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>

          <a-button type="primary" :disabled="!tableSelections || !tableSelections.length" @click="syncToWorkspaceShow">工作空间同步</a-button>
          <a-tooltip>
            <template slot="title">
              <div>
                <ul>
                  <li>关联节点数据是异步获取有一定时间延迟</li>
                  <li>关联节点会自动识别服务器中是否存在 java 环境,如果没有 Java 环境不能快速安装节点</li>
                  <li>关联节点如果服务器存在 java 环境,但是插件端未运行则会显示快速安装按钮</li>
                </ul>
              </div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" :title="text"> {{ text }}</a-tooltip>
      <a-tooltip slot="host" slot-scope="text, record" :title="`${record.machineSsh && record.machineSsh.host}:${record.machineSsh && record.machineSsh.port}`">
        {{ record.machineSsh && record.machineSsh.host }}:{{ record.machineSsh && record.machineSsh.port }}
      </a-tooltip>
      <template slot="status" slot-scope="text, record">
        <a-tooltip :title="record.machineSsh && record.machineSsh.statusMsg">
          <a-tag :color="record.machineSsh && record.machineSsh.status === 1 ? 'green' : 'red'">{{ record.machineSsh && record.machineSsh.status === 1 ? "正常" : "无法连接" }}</a-tag>
        </a-tooltip>
      </template>
      <a-popover title="系统信息" slot="osName" slot-scope="text, record">
        <template slot="content">
          <p>系统名：{{ record.machineSsh && record.machineSsh.osName }}</p>
          <p>系统版本：{{ record.machineSsh && record.machineSsh.osVersion }}</p>
          <p>CPU型号：{{ record.machineSsh && record.machineSsh.osCpuIdentifierName }}</p>
          <p>主机名：{{ record.machineSsh && record.machineSsh.hostName }}</p>
          <p>开机时间：{{ formatDuration(record.machineSsh && record.machineSsh.osSystemUptime) }}</p>
        </template>
        {{ text || "未知" }}
      </a-popover>
      <a-tooltip
        slot="osOccupyMemory"
        slot-scope="text, record"
        placement="topLeft"
        :title="`内存使用率：${formatPercent(record.machineSsh && record.machineSsh.osOccupyMemory)},总内存：${renderSize(record.machineSsh && record.machineSsh.osMoneyTotal)}`"
      >
        <span>{{ formatPercent(record.machineSsh && record.machineSsh.osOccupyMemory) }}/{{ renderSize(record.machineSsh && record.machineSsh.osMoneyTotal) }}</span>
      </a-tooltip>

      <a-tooltip
        slot="osOccupyCpu"
        slot-scope="text, record"
        placement="topLeft"
        :title="`CPU使用率：${formatPercent2Number(record.machineSsh && record.machineSsh.osOccupyCpu)}%,CPU数：${record.machineSsh && record.machineSsh.osCpuCores}`"
      >
        <span>{{ (formatPercent2Number(record.machineSsh && record.machineSsh.osOccupyCpu) || "-") + "%" }} / {{ record.machineSsh && record.machineSsh.osCpuCores }}</span>
      </a-tooltip>

      <a-popover title="硬盘信息" slot="osMaxOccupyDisk" slot-scope="text, record">
        <template slot="content">
          <p>硬盘总量：{{ renderSize(record.machineSsh && record.machineSsh.osMoneyTotal) }}</p>
          <p>硬盘最大的使用率：{{ formatPercent(record.machineSsh && record.machineSsh.osMaxOccupyDisk) }}</p>
          <p>使用率最大的分区：{{ record.machineSsh && record.machineSsh.osMaxOccupyDiskName }}</p>
        </template>
        <span>{{ formatPercent(record.machineSsh && record.machineSsh.osMaxOccupyDisk) }} / {{ renderSize(record.machineSsh && record.machineSsh.osMoneyTotal) }}</span>
      </a-popover>

      <template slot="nodeId" slot-scope="text, record">
        <template v-if="record.linkNode">
          <a-tooltip placement="topLeft" :title="`节点名称：${record.linkNode.name}`">
            <a-button size="small" style="width: 90px; padding: 0 10px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis" type="" @click="toNode(record.linkNode)">
              {{ record.linkNode.name }}
            </a-button>
          </a-tooltip>
        </template>
        <template v-else>-</template>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-dropdown>
            <a-button size="small" type="primary" @click="handleTerminal(record, false)">终端<a-icon type="down" /></a-button>
            <a-menu slot="overlay">
              <a-menu-item key="1">
                <a-button size="small" type="primary" icon="fullscreen" @click="handleTerminal(record, true)">全屏终端</a-button>
              </a-menu-item>
              <a-menu-item key="2">
                <router-link target="_blank" :to="{ path: '/full-terminal', query: { id: record.id, wid: getWorkspaceId } }">
                  <a-button size="small" type="primary" icon="fullscreen"> 新标签终端</a-button>
                </router-link>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
          <template v-if="record.fileDirs">
            <a-button size="small" type="primary" @click="handleFile(record)">文件</a-button>
          </template>
          <template v-else>
            <a-tooltip placement="topLeft" title="如果按钮不可用,请去资产管理 ssh 列表的关联中添加当前工作空间允许管理的授权文件夹">
              <a-button size="small" type="primary" :disabled="true">文件</a-button>
            </a-tooltip>
          </template>

          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
              更多
              <a-icon type="down" />
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="primary" @click="handleViewLog(record)">终端日志</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model="editSshVisible" width="600px" title="编辑 SSH" @ok="handleEditSshOk" :maskClosable="false">
      <a-form-model ref="editSshForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="SSH 名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="SSH 名称" />
        </a-form-model-item>
        <a-form-model-item label="分组名称" prop="group">
          <custom-select v-model="temp.group" :data="groupList" suffixIcon="" inputPlaceholder="添加分组" selectPlaceholder="选择分组名"> </custom-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>

    <!-- 文件管理 -->
    <a-drawer
      destroyOnClose
      :visible="drawerVisible"
      @close="
        () => {
          this.drawerVisible = false;
        }
      "
      :title="`${this.temp.name} 文件管理`"
      placement="right"
      width="90vw"
    >
      <ssh-file v-if="drawerVisible" :sshId="temp.id" />
    </a-drawer>
    <!-- Terminal -->
    <a-modal
      destroyOnClose
      :dialogStyle="{
        maxWidth: '100vw',
        top: this.terminalFullscreen ? 0 : false,
        paddingBottom: 0,
      }"
      :width="this.terminalFullscreen ? '100vw' : '80vw'"
      :bodyStyle="{
        padding: '0px 10px',
        paddingTop: '10px',
        marginRight: '10px',
        height: `${this.terminalFullscreen ? 'calc(100vh - 56px)' : '70vh'}`,
      }"
      v-model="terminalVisible"
      :title="temp.name"
      :footer="null"
      :maskClosable="false"
    >
      <terminal v-if="terminalVisible" :sshId="temp.id" />
    </a-modal>
    <!-- 操作日志 -->
    <a-modal destroyOnClose v-model="viewOperationLog" title="操作日志" width="80vw" :footer="null" :maskClosable="false">
      <OperationLog v-if="viewOperationLog" :sshId="temp.id"></OperationLog>
    </a-modal>
    <!-- 同步到其他工作空间 -->
    <a-modal destroyOnClose v-model="syncToWorkspaceVisible" title="同步到其他工作空间" @ok="handleSyncToWorkspace" :maskClosable="false">
      <a-alert message="温馨提示" type="warning">
        <template slot="description">
          <ul>
            <li>同步机制采用 IP+PORT+连接方式 确定是同一个服务器</li>
            <li>当目标工作空间不存在对应的 SSH 时候将自动创建一个新的 SSH</li>
            <li>当目标工作空间已经存在 SSH 时候将自动同步 SSH 账号、密码、私钥等信息</li>
          </ul>
        </template>
      </a-alert>
      <a-form-model :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item> </a-form-model-item>
        <a-form-model-item label="选择工作空间" prop="workspaceId">
          <a-select show-search option-filter-prop="children" v-model="temp.workspaceId" placeholder="请选择工作空间">
            <a-select-option :disabled="getWorkspaceId === item.id" v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { deleteSsh, editSsh, getSshList, syncToWorkspace, getSshGroupAll } from "@/api/ssh";
import SshFile from "@/pages/ssh/ssh-file";
import Terminal from "@/pages/ssh/terminal";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime, formatPercent2Number, renderSize, formatDuration, formatPercent } from "@/utils/const";
import { getWorkSpaceListAll } from "@/api/workspace";

import { mapGetters } from "vuex";
import OperationLog from "@/pages/system/assets/ssh/operation-log";
import CustomSelect from "@/components/customSelect";

export default {
  components: {
    SshFile,
    Terminal,
    OperationLog,
    CustomSelect,
  },
  data() {
    return {
      loading: false,
      list: [],
      temp: {},
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editSshVisible: false,

      syncToWorkspaceVisible: false,
      tableSelections: [],
      workspaceList: [],
      tempNode: {},
      // fileList: [],
      sshAgentInfo: {},
      agentData: {},
      formLoading: false,

      drawerVisible: false,
      terminalVisible: false,
      terminalFullscreen: false,
      viewOperationLog: false,

      columns: [
        { title: "名称", dataIndex: "name", sorter: true, ellipsis: true, scopedSlots: { customRender: "tooltip" } },

        { title: "Host", dataIndex: "machineSsh.host", sorter: true, ellipsis: true, scopedSlots: { customRender: "host" } },
        // { title: "Port", dataIndex: "machineSsh.port", sorter: true, width: 80, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "用户名", dataIndex: "machineSsh.user", sorter: true, width: "100px", ellipsis: true, scopedSlots: { customRender: "tooltip" } },

        { title: "系统名", dataIndex: "machineSsh.osName", sorter: true, ellipsis: true, scopedSlots: { customRender: "osName" } },
        // { title: "系统版本", dataIndex: "machineSsh.osVersion", sorter: true, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "CPU", dataIndex: "machineSsh.osOccupyCpu", sorter: true, ellipsis: true, scopedSlots: { customRender: "osOccupyCpu" } },
        { title: "内存", dataIndex: "machineSsh.osOccupyMemory", sorter: true, ellipsis: true, scopedSlots: { customRender: "osOccupyMemory" } },
        { title: "硬盘", dataIndex: "machineSsh.osMaxOccupyDisk", sorter: true, ellipsis: true, scopedSlots: { customRender: "osMaxOccupyDisk" } },
        // { title: "编码格式", dataIndex: "charset", sorter: true, width: 120, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "连接状态", dataIndex: "machineSsh.status", ellipsis: true, align: "center", width: "100px", scopedSlots: { customRender: "status" } },
        // { title: "编码格式", dataIndex: "machineSsh.charset", sorter: true, width: 120, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        {
          title: "关联节点",
          dataIndex: "nodeId",
          scopedSlots: { customRender: "nodeId" },
          width: "120px",
          ellipsis: true,
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: "170px",
        },
        {
          title: "操作",
          dataIndex: "operation",
          scopedSlots: { customRender: "operation" },
          width: "200px",
          align: "center",
          // ellipsis: true,
        },
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: "请输入 SSH 名称", trigger: "blur" }],
      },

      groupList: [],
    };
  },
  computed: {
    ...mapGetters(["getWorkspaceId", "getUserInfo"]),

    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys;
        },
        selectedRowKeys: this.tableSelections,
      };
    },
  },
  created() {
    this.loadData();
    this.loadGroupList();
  },
  methods: {
    formatPercent2Number,
    renderSize,
    formatPercent,
    formatDuration,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      getSshList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
          //
        }
        this.loading = false;
      });
    },
    // 获取所有的分组
    loadGroupList() {
      getSshGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data;
        }
      });
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, { id: record.id, group: record.group, name: record.name });

      this.editSshVisible = true;
      // @author jzy 08-04
      this.$refs["editSshForm"] && this.$refs["editSshForm"].resetFields();
    },
    // 提交 SSH 数据
    handleEditSshOk() {
      // 检验表单
      this.$refs["editSshForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
        editSsh(this.temp).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            //this.$refs['editSshForm'].resetFields();
            // this.fileList = [];
            this.editSshVisible = false;
            this.loadData();
            this.loadGroupList();
          }
        });
      });
    },
    // 进入终端
    handleTerminal(record, terminalFullscreen) {
      this.temp = Object.assign({}, record);
      this.terminalVisible = true;
      this.terminalFullscreen = terminalFullscreen;
    },
    // 操作日志
    handleViewLog(record) {
      this.temp = Object.assign({}, record);
      this.viewOperationLog = true;
    },

    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, { id: record.id, group: record.group, name: record.name });

      this.drawerVisible = true;
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除 SSH 么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteSsh(record.id).then((res) => {
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
    // 前往节点
    toNode(node) {
      const newpage = this.$router.resolve({
        name: "node_" + node.id,
        path: "/node/list",
        query: {
          ...this.$route.query,
          nodeId: node.id,
          pId: "manage",
          id: "manageList",
          wid: node.workspaceId,
          searchNodeName: node.name,
        },
      });
      window.open(newpage.href, "_blank");

      // this.$router.push({
      //   path: "/node/list",
      //   query: {
      //     ...this.$route.query,
      //     tipNodeId: nodeId,
      //   },
      // });
    },

    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },

    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data;
        }
      });
    },
    // 同步到其他工作情况
    syncToWorkspaceShow() {
      this.syncToWorkspaceVisible = true;
      this.loadWorkSpaceListAll();
      this.temp = {
        workspaceId: undefined,
      };
    },
    //
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        this.$notification.warn({
          message: "请选择工作空间",
        });
        return false;
      }
      // 同步
      syncToWorkspace({
        ids: this.tableSelections.join(","),
        toWorkspaceId: this.temp.workspaceId,
      }).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.tableSelections = [];
          this.syncToWorkspaceVisible = false;
          return false;
        }
      });
    },
  },
};
</script>
<style scoped></style>
