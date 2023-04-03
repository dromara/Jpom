<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table size="middle" :data-source="list" :columns="columns" @change="changePage" :pagination="pagination" bordered rowKey="id" :row-selection="rowSelection">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="名称" class="search-input-item" />
          <a-input v-model="listQuery['%host%']" @pressEnter="loadData" placeholder="host" class="search-input-item" />
          <a-input v-model="listQuery['%swarmId%']" @pressEnter="loadData" placeholder="集群ID" class="search-input-item" />

          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">添加</a-button>
          <a-button :disabled="!this.tableSelections.length" @click="syncToWorkspaceShow()" type="primary"> 批量分配</a-button>
          <a-tooltip title="自动检测服务端所在服务器中是否存在 docker，如果存在将自动添加到列表中">
            <a-button type="dashed" @click="handleTryLocalDocker"> <a-icon type="question-circle" theme="filled" />自动探测 </a-button>
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text, item" :title="text">
        <a-button style="padding: 0" type="link" size="small" @click="handleEdit(item)"> {{ text }}</a-button>
      </a-tooltip>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <template slot="swarmId" slot-scope="text, record">
        <template v-if="text">
          <a-tooltip v-if="record.swarmControlAvailable" title="管理节点">
            <a-icon type="cluster" />
          </a-tooltip>
          <a-tooltip v-else title="工作节点">
            <a-icon type="block" />
          </a-tooltip>
          <a-popover title="集群信息">
            <template slot="content">
              <p>集群ID：{{ record.swarmId }}</p>
              <p>当前节点ID：{{ record.swarmNodeId }}</p>
              <p>当前节点地址：{{ record.swarmNodeAddr }}</p>
              <p>集群创建时间：{{ parseTime(record.swarmCreatedAt) }}</p>
              <p>集群修改时间：{{ parseTime(record.swarmUpdatedAt) }}</p>
            </template>
            {{ text }}
          </a-popover>
        </template>
      </template>

      <a-tooltip slot="tlsVerify" slot-scope="text, record" placement="topLeft" :title="record.tlsVerify ? '开启 TLS 认证,证书信息：' + record.certInfo : '关闭 TLS 认证'">
        <template v-if="record.tlsVerify">
          <template v-if="record.certExist"> <a-switch size="small" v-model="record.tlsVerify" :disabled="true" checked-children="开" un-checked-children="关" /> </template>
          <a-tag v-else color="red"> 证书丢失 </a-tag>
        </template>
        <template v-else> <a-switch size="small" v-model="record.tlsVerify" :disabled="true" checked-children="开" un-checked-children="关" /> </template>
      </a-tooltip>

      <template slot="status" slot-scope="text, record">
        <a-tag color="green" v-if="record.status === 1">正常</a-tag>
        <a-tooltip v-else :title="record.failureMsg">
          <a-tag color="red">无法连接</a-tag>
        </a-tooltip>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" :disabled="record.status !== 1" @click="handleConsole(record)">控制台</a-button>
          <template v-if="!record.swarmId && record.status === 1">
            <a-popover title="集群操作">
              <template slot="content">
                <p><a-button size="small" type="primary" @click="initSwarm(record)">创建集群</a-button></p>
                <p><a-button size="small" type="primary" @click="joinSwarm(record)">加入集群</a-button></p>
              </template>
              <a-button icon="edit" size="small" type="primary">集群</a-button>
            </a-popover>
          </template>
          <template v-else>
            <a-button size="small" icon="select" @click="handleSwarmConsole(record)" :disabled="parseInt(record.status) !== 1" type="primary">集群</a-button>
          </template>
          <a-button size="small" @click="syncToWorkspaceShow(record)" type="primary">分配</a-button>
          <a-button size="small" @click="viewWorkspaceDataHander(record)" type="primary">关联</a-button>
          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()"> 更多 <a-icon type="down" /> </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" :disabled="!record.swarmId || record.status !== 1" type="danger" @click="handleLeaveForce(record)">退出集群</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model="editVisible" width="50%" title="编辑  Docker" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-space direction="vertical">
          <a-alert banner>
            <template slot="message">
              <ul>
                <li>系统使用 docker http 接口实现和 docker 通讯和管理，但是默认<b style="color: red">没有开启任何认证</b></li>
                <li>
                  这样使得
                  <b style="color: red">docker 极不安全</b>
                </li>
                <li>如果端口暴露到公网很<b style="color: red"> 容易出现挖矿情况 </b></li>
                <li>所以这里 我们<b style="color: red">强烈建议您使用 TLS 证书</b>（证书生成方式可以参考文档）来连接 docker 提升安全性</li>
                <li>如果端口<b style="color: red">保证在内网中使用可以忽略 TLS 证书</b></li>
                <li>注意：<b style="color: red">证书的允许的 IP 需要和 docker host 一致</b></li>
              </ul>
            </template>
          </a-alert>
          <div></div>
        </a-space>
        <a-form-model-item label="容器名称" prop="name">
          <a-input v-model="temp.name" placeholder="容器名称" />
        </a-form-model-item>
        <a-form-model-item label="host" prop="host">
          <a-input v-model="temp.host" placeholder="容器地址 tcp://127.0.0.1:2375" />
        </a-form-model-item>

        <a-form-model-item label="TLS 认证" prop="tlsVerify">
          <a-switch v-model="temp.tlsVerify" checked-children="开" un-checked-children="关" />
        </a-form-model-item>

        <a-form-model-item v-if="temp.tlsVerify" label="证书信息" prop="certInfo" help="可以通过证书管理中提前上传或者点击后面选择证书去选择/导入证书">
          <a-input-search
            v-model="temp.certInfo"
            placeholder="请输入证书信息或者选择证书信息,证书信息填写规则：序列号:证书类型"
            enter-button="选择证书"
            @search="
              () => {
                this.certificateVisible = true;
              }
            "
          />
        </a-form-model-item>

        <a-collapse>
          <a-collapse-panel key="1" header="其他配置">
            <a-form-model-item label="超时时间" prop="heartbeatTimeout">
              <a-input-number style="width: 100%" v-model="temp.heartbeatTimeout" placeholder="超时时间 单位秒" />
            </a-form-model-item>
            <a-form-model-item label="仓库地址" prop="registryUrl">
              <a-input v-model="temp.registryUrl" placeholder="仓库地址" />
            </a-form-model-item>
            <a-form-model-item label="仓库账号" prop="registryUsername">
              <a-input v-model="temp.registryUsername" placeholder="仓库账号" />
            </a-form-model-item>
            <a-form-model-item label="仓库密码" prop="registryPassword">
              <a-input-password v-model="temp.registryPassword" placeholder="仓库密码" />
            </a-form-model-item>
            <a-form-model-item label="账号邮箱" prop="registryEmail">
              <a-input v-model="temp.registryEmail" placeholder="账号邮箱" />
            </a-form-model-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form-model>
    </a-modal>
    <!-- 创建集群 -->
    <a-modal destroyOnClose v-model="initSwarmVisible" title="创建 Docker 集群" @ok="handleSwarm" :maskClosable="false">
      <a-form-model ref="initForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-alert message="温馨提示" type="warning">
          <template slot="description"> 创建集群会将尝试获取 docker 中集群信息，如果存在集群信息将自动同步集群信息到系统，反之不存在集群信息将自动创建 swarm 集群 </template>
        </a-alert>
      </a-form-model>
    </a-modal>
    <!-- 加入集群 -->
    <a-modal destroyOnClose v-model="joinSwarmVisible" title="加入 Docker 集群" @ok="handleSwarmJoin" :maskClosable="false">
      <a-form-model ref="joinForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="选择集群" prop="managerId">
          <a-select
            show-search
            option-filter-prop="children"
            @change="
              (v) => {
                tempList = swarmList.filter((item) => {
                  return item.id === v;
                });
                if (tempList.length) {
                  temp = { ...temp, remoteAddr: tempList[0].swarmNodeAddr };
                } else {
                  temp = { ...temp, remoteAddr: '' };
                }
              }
            "
            v-model="temp.managerId"
            allowClear
            placeholder="加入到哪个集群"
          >
            <a-select-option v-for="item in swarmList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>

        <a-form-model-item v-if="temp.remoteAddr" label="集群IP" prop="remoteAddr"><a-input v-model="temp.remoteAddr" placeholder="关联容器标签" /> </a-form-model-item>

        <a-form-model-item label="角色" prop="role">
          <a-radio-group name="role" v-model="temp.role">
            <a-radio value="worker"> 工作节点</a-radio>
            <a-radio value="manager"> 管理节点 </a-radio>
          </a-radio-group>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 控制台 -->
    <!-- <a-drawer destroyOnClose :title="`${temp.name} 控制台`" placement="right" :width="`${this.getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`" :visible="consoleVisible" @close="onClose"> -->
    <console v-if="consoleVisible" :visible="consoleVisible" :machineDockerId="temp.id" urlPrefix="/system/assets/docker" @close="onClose"></console>
    <!-- </a-drawer> -->
    <!-- 集群控制台 -->
    <a-drawer
      destroyOnClose
      :title="`${temp.name} 集群控制台`"
      placement="right"
      :width="`${this.getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
      :visible="swarmConsoleVisible"
      @close="onSwarmClose"
    >
      <swarm-console v-if="swarmConsoleVisible" :id="temp.id" :visible="swarmConsoleVisible" :initMenu="temp.menuKey" urlPrefix="/system/assets"></swarm-console>
    </a-drawer>
    <!-- 分配到其他工作空间 -->
    <a-modal destroyOnClose v-model="syncToWorkspaceVisible" title="分配到其他工作空间" @ok="handleSyncToWorkspace" :maskClosable="false">
      <a-form-model :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item> </a-form-model-item>
        <a-form-model-item label="分配类型" prop="type">
          <a-radio-group v-model="temp.type">
            <a-radio value="docker"> docker </a-radio>
            <a-radio value="swarm" :disabled="temp.swarmId === true ? false : true"> 集群 </a-radio>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="选择工作空间" prop="workspaceId">
          <a-select show-search option-filter-prop="children" v-model="temp.workspaceId" placeholder="请选择工作空间">
            <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 查看 docker 关联工作空间的信息 -->
    <a-modal destroyOnClose v-model="viewWorkspaceDocker" width="50%" title="关联工作空间 docker" :footer="null" :maskClosable="false">
      <a-tabs>
        <a-tab-pane key="1" tab="docker">
          <a-list bordered :data-source="workspaceDockerData && workspaceDockerData.dockerList">
            <a-list-item slot="renderItem" slot-scope="item" style="display: block">
              <a-row>
                <a-col :span="10">Docker 名称：{{ item.name }}</a-col>
                <a-col :span="10">所属工作空间： {{ item.workspace && item.workspace.name }}</a-col>
                <a-col :span="4"> </a-col>
              </a-row>
            </a-list-item>
          </a-list>
        </a-tab-pane>
        <a-tab-pane key="2" tab="集群">
          <a-list bordered :data-source="workspaceDockerData && workspaceDockerData.swarmList">
            <a-list-item slot="renderItem" slot-scope="item" style="display: block">
              <a-row>
                <a-col :span="10">集群名称：{{ item.name }}</a-col>
                <a-col :span="10">所属工作空间： {{ item.workspace && item.workspace.name }}</a-col>
                <a-col :span="4"> </a-col>
              </a-row>
            </a-list-item>
          </a-list>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
    <!-- 选择证书文件 -->
    <a-drawer
      destroyOnClose
      :title="`选择证书文件`"
      placement="right"
      :visible="certificateVisible"
      width="85vw"
      :zIndex="1009"
      @close="
        () => {
          this.certificateVisible = false;
        }
      "
    >
      <certificate
        v-if="certificateVisible"
        @confirm="
          (certInfo) => {
            this.temp = { ...this.temp, certInfo: certInfo };
            this.certificateVisible = false;
          }
        "
        @cancel="
          () => {
            this.certificateVisible = false;
          }
        "
      ></certificate>
    </a-drawer>
  </div>
</template>
<script>
import {
  dockerList,
  editDocker,
  tryLocalDocker,
  deleteDcoker,
  initDockerSwarm,
  joinDockerSwarm,
  dockerSwarmListAll,
  dcokerSwarmLeaveForce,
  machineDockerDistribute,
  dockerListWorkspace,
} from "@/api/system/assets-docker";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from "@/utils/const";
import { getWorkSpaceListAll } from "@/api/workspace";
import Console from "@/pages/docker/console";
import SwarmConsole from "@/pages/docker/swarm/console.vue";
import { mapGetters } from "vuex";
import certificate from "./certificate.vue";
export default {
  components: {
    Console,
    SwarmConsole,
    certificate,
  },
  props: {},
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      temp: {},
      editVisible: false,
      templateVisible: false,
      consoleVisible: false,
      swarmConsoleVisible: false,
      initSwarmVisible: false,
      joinSwarmVisible: false,
      swarmList: [],
      columns: [
        { title: "名称", dataIndex: "name", width: 120, ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "host", dataIndex: "host", width: 120, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "docker版本", dataIndex: "dockerVersion", ellipsis: true, width: "100px", scopedSlots: { customRender: "tooltip" } },

        { title: "状态", dataIndex: "status", ellipsis: true, align: "center", width: "100px", scopedSlots: { customRender: "status" } },
        { title: "TLS 认证", dataIndex: "tlsVerify", width: 100, align: "center", ellipsis: true, scopedSlots: { customRender: "tlsVerify" } },
        { title: "集群", dataIndex: "swarmId", ellipsis: true, scopedSlots: { customRender: "swarmId" } },
        // { title: "apiVersion", dataIndex: "apiVersion", width: 100, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "最后修改人", dataIndex: "modifyUser", width: 120, ellipsis: true, scopedSlots: { customRender: "modifyUser" } },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          ellipsis: true,
          sorter: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, fixed: "right", align: "center", width: "300px" },
      ],
      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: "请填写容器名称", trigger: "blur" }],
        host: [{ required: true, message: "请填写容器地址", trigger: "blur" }],

        managerId: [{ required: true, message: "请选择要加入到哪个集群", trigger: "blur" }],
        role: [{ required: true, message: "请选择节点角色", trigger: "blur" }],
        remoteAddr: [
          { required: true, message: "请填写集群IP", trigger: "blur" },
          {
            pattern: /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/,
            message: "填写正确的IP地址",
          },
        ],
      },
      syncToWorkspaceVisible: false,
      workspaceList: [],
      viewWorkspaceDocker: false,
      workspaceDockerData: {
        dockerList: [],
        swarmList: [],
      },
      tableSelections: [],
      certificateVisible: false,
    };
  },
  computed: {
    ...mapGetters(["getCollapsed"]),
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
  mounted() {
    this.loadData();
  },
  methods: {
    //
    parseTime,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;

      dockerList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
          //
          const dockerId = this.$route.query.dockerId;
          const type = this.$route.query.type;
          this.list.map((item) => {
            if (dockerId === item.id && type === "docker") {
              this.handleConsole(item);
            } else if (dockerId === item.id && type === "swarm") {
              this.handleSwarmConsole(item);
            }
          });
        }
        this.loading = false;
      });
    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.editVisible = true;

      this.$refs["editForm"]?.resetFields();
    },
    // 控制台
    handleConsole(record) {
      this.temp = { ...record };
      this.consoleVisible = true;

      let dockerId = this.$route.query.dockerId;
      if (dockerId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, dockerId: record.id, type: "docker" },
        });
      }
    },
    // 集群控制台
    handleSwarmConsole(record) {
      this.temp = { ...record };
      this.swarmConsoleVisible = true;
      let dockerId = this.$route.query.dockerId;
      if (dockerId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, dockerId: record.id, type: "swarm" },
        });
      }
    },
    // 关闭抽屉层
    onClose() {
      this.consoleVisible = false;
      const query = Object.assign({}, this.$route.query);
      delete query.dockerId;
      delete query.type;
      this.$router.replace({
        query: query,
      });
    },
    onSwarmClose() {
      this.swarmConsoleVisible = false;
      const query = Object.assign({}, this.$route.query);
      delete query.dockerId;
      delete query.type;
      this.$router.replace({
        query: query,
      });
    },
    // 修改
    handleEdit(record) {
      this.temp = { ...record };
      this.editVisible = true;

      // this.temp = { ...this.temp };

      this.$refs["editForm"]?.resetFields();
    },

    // 提交  数据
    handleEditOk() {
      // 检验表单
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const temp = Object.assign({}, this.temp);

        editDocker(temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.editVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除该 Docker 么？删除只会检查本地系统的数据关联,不会删除 docker 容器中数据",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: record.id,
          };
          deleteDcoker(params).then((res) => {
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
    // 强制解绑
    handleLeaveForce(record) {
      const html =
        "<h1 style='color:red;'>真的要强制退出集群吗？</h1> " +
        "<h3 style='color:red;'>如果当前集群还存在可能出现数据不一致问题奥</h3> " +
        "<ul style='color:red;'>" +
        "<li>请提前备份数据再操作奥</li>" +
        "<li style='font-weight: bold;'>请不要优先退出管理节点</li>" +
        "<li>操作不能撤回奥</li>" +
        " </ul>";
      const h = this.$createElement;
      this.$confirm({
        title: "系统提示",
        content: h("div", null, [h("p", { domProps: { innerHTML: html } }, null)]),
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: record.id,
          };
          dcokerSwarmLeaveForce(params).then((res) => {
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
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },

    // 创建集群
    initSwarm(record) {
      this.temp = {
        id: record.id,
      };
      this.initSwarmVisible = true;
      this.$refs["initForm"]?.resetFields();
    },
    handleSwarm() {
      this.$refs["initForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        initDockerSwarm(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.initSwarmVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 加入集群
    joinSwarm(record) {
      dockerSwarmListAll().then((res) => {
        this.swarmList = res.data;
        this.temp = {
          id: record.id,
        };
        this.joinSwarmVisible = true;
        this.$refs["joinForm"]?.resetFields();
      });
    },
    // 处理加入集群
    handleSwarmJoin() {
      this.$refs["joinForm"].validate((valid) => {
        if (!valid) {
          return false;
        }

        joinDockerSwarm(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.joinSwarmVisible = false;
            this.loadData();
          }
        });
      });
    },
    //
    handleTryLocalDocker() {
      tryLocalDocker().then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
          this.loadData();
        }
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
    // 同步到其他工作情况
    syncToWorkspaceShow(item) {
      this.syncToWorkspaceVisible = true;
      this.loadWorkSpaceListAll();
      if (item) {
        this.temp = {
          ids: item.id,
          swarmId: item.swarmId ? true : false,
        };
      } else {
        this.temp = {
          swarmId: true,
        };
      }
    },
    handleSyncToWorkspace() {
      if (!this.temp.type) {
        this.$notification.warn({
          message: "请选择分配类型",
        });
        return false;
      }
      if (!this.temp.workspaceId) {
        this.$notification.warn({
          message: "请选择工作空间",
        });
        return false;
      }
      if (!this.temp.ids) {
        this.temp = { ...this.temp, ids: this.tableSelections.join(",") };
        this.tableSelections = [];
      }
      // 同步
      machineDockerDistribute(this.temp).then((res) => {
        if (res.code == 200) {
          this.$notification.success({
            message: res.msg,
          });

          this.syncToWorkspaceVisible = false;
          return false;
        }
      });
    },
    // 查看工作空间的 docker 信息
    viewWorkspaceDataHander(item) {
      this.workspaceDockerData = {};
      dockerListWorkspace({
        id: item.id,
      }).then((res) => {
        if (res.code === 200) {
          this.viewWorkspaceDocker = true;
          this.workspaceDockerData = res.data;
        }
      });
    },
  },
};
</script>
<style scoped></style>
