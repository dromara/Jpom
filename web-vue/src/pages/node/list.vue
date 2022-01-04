<template>
  <div class="full-content">
    <div ref="filter" class="filter">
      <a-input class="search-input-item" v-model="listQuery['%id%']" placeholder="节点ID" />
      <a-input class="search-input-item" v-model="listQuery['%name%']" placeholder="节点名称" />
      <a-input class="search-input-item" v-model="listQuery['%url%']" placeholder="节点地址" />
      <a-tooltip title="按住 Ctr 或者 Alt 键点击按钮快速回到第一页">
        <a-button type="primary" @click="loadData">搜索</a-button>
      </a-tooltip>
      <a-button type="primary jpom-node-manage-add" @click="handleAdd">新增</a-button>
      <a-tooltip>
        <template slot="title">
          <div>点击节点管理进入节点管理页面</div>

          <div>
            <ul>
              <li>节点账号密码为插件端的账号密码,并非用户账号(管理员)密码</li>
              <li>节点账号密码默认由系统生成：可以通过插件端数据目录下 agent_authorize.json 文件查看（如果自定义配置了账号密码将没有此文件）</li>
              <li>节点地址为插件端的 IP:PORT 插件端端口默认为：2123</li>
            </ul>
          </div>
        </template>
        <a-icon type="question-circle" theme="filled" />
      </a-tooltip>
    </div>
    <!-- 表格 :scroll="{ x: 1070, y: tableHeight -60 }" scroll 跟 expandedRowRender 不兼容，没法同时使用不然会多出一行数据-->
    <a-table :loading="loading" :columns="columns" :data-source="list" bordered rowKey="id" @expand="expand" :pagination="(this, pagination)" @change="changePage">
      <a-tooltip slot="url" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button v-if="record.unLockType" type="primary" @click="unlock(record)">解锁节点</a-button>
        <a-button v-else class="jpom-node-manage-btn" type="primary" @click="handleNode(record)" :disabled="record.openStatus !== 1">节点管理</a-button>
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-tooltip title="需要到编辑中去为一个节点绑定一个 ssh信息才能启用该功能">
          <a-button type="primary" @click="handleTerminal(record)" :disabled="!record.sshId">终端</a-button>
        </a-tooltip>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
      <!-- 嵌套表格 -->
      <a-table slot="expandedRowRender" slot-scope="text" :loading="childLoading" :columns="childColumns" :data-source="text.children" :pagination="false" :rowKey="(record, index) => text.id + index">
        <a-tooltip slot="osName" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="javaVersion" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="runTime" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <template slot="projectCount" slot-scope="text, item">
          <div v-if="text" @click="syncNode(item)">
            <a-tooltip placement="topLeft" title="节点中的所有项目数量,点击重新同步节点项目信息">
              <a-tag>{{ text }} </a-tag>
              <a-icon type="sync" />
            </a-tooltip>
          </div>
        </template>
        <template slot="scriptCount" slot-scope="text, item">
          <div v-if="text" @click="syncNodeScript(item)">
            <a-tooltip placement="topLeft" title="节点中的所有脚本模版数量,点击重新同步脚本模版信息">
              <a-tag>{{ text }} </a-tag>
              <a-icon type="sync" />
            </a-tooltip>
          </div>
        </template>
      </a-table>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editNodeVisible" title="编辑节点" @ok="handleEditNodeOk" :maskClosable="false">
      <a-form-model ref="editNodeForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <!-- <a-form-model-item label="节点 ID" prop="id">
          <a-input v-model="temp.id" placeholder="创建之后不能修改" />
        </a-form-model-item> -->
        <a-form-model-item label="节点名称" prop="name">
          <a-input v-model="temp.name" placeholder="节点名称" />
        </a-form-model-item>
        <a-form-model-item label="绑定 SSH " prop="sshId">
          <a-select show-search option-filter-prop="children" v-model="temp.sshId" placeholder="请选择SSH">
            <a-select-option value="">不绑定</a-select-option>
            <a-select-option v-for="ssh in sshList" :key="ssh.id" :disabled="ssh.disabled">{{ ssh.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="监控周期" prop="cycle">
          <a-select v-model="temp.cycle" defaultValue="0" placeholder="监控周期">
            <a-select-option :key="0">不开启</a-select-option>
            <a-select-option :key="-30">30 秒</a-select-option>
            <a-select-option :key="1">1 分钟</a-select-option>
            <a-select-option :key="5">5 分钟</a-select-option>
            <a-select-option :key="10">10 分钟</a-select-option>
            <a-select-option :key="30">30 分钟</a-select-option>
          </a-select>
        </a-form-model-item>

        <a-form-model-item label="节点状态" prop="openStatus">
          <a-switch
            :checked="temp.openStatus == 1"
            @change="
              (checked) => {
                temp.openStatus = checked ? 1 : 0;
              }
            "
            checked-children="启用"
            un-checked-children="停用"
            default-checked
          />
        </a-form-model-item>
        <a-form-model-item prop="url">
          <template slot="label">
            节点地址
            <a-tooltip v-show="!temp.id">
              <template slot="title"
                >节点地址为插件端的 IP:PORT 插件端端口默认为：2123
                <ul>
                  <li>节点地址建议使用内网地址</li>
                  <li>如果插件端正常运行但是连接失败请检查端口是否开放,防火墙规则,云服务器的安全组入站规则</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input v-model="temp.url" placeholder="节点地址 (127.0.0.1:2123)">
            <a-select slot="addonBefore" v-model="temp.protocol" default-value="Http://" style="width: 80px">
              <a-select-option value="Http"> Http:// </a-select-option>
              <a-select-option value="Https"> Https:// </a-select-option>
            </a-select>
            <!--						<a-input v-model="temp.url" placeholder="节点地址 (127.0.0.1:2123)"/>-->
          </a-input>
        </a-form-model-item>
        <!--				<a-form-model-item label="节点协议" prop="protocol">-->
        <!--					<a-select v-model="temp.protocol" defaultValue="http" placeholder="节点协议">-->
        <!--						<a-select-option key="http">HTTP</a-select-option>-->
        <!--						<a-select-option key="htts">HTTPS</a-select-option>-->
        <!--					</a-select>-->
        <!--				</a-form-model-item>-->
        <!--				<a-form-model-item label="节点地址" prop="url">-->
        <!--					<a-input v-model="temp.url" placeholder="节点地址 (127.0.0.1:2123)"/>-->
        <!--				</a-form-model-item>-->
        <div class="node-config">
          <a-form-model-item label="节点账号" prop="loginName">
            <a-input v-model="temp.loginName" placeholder="节点账号,请查看节点启动输出的信息" />
          </a-form-model-item>
          <a-form-model-item :prop="`${temp.id ? 'loginPwd-update' : 'loginPwd'}`">
            <template slot="label">
              节点密码
              <a-tooltip v-show="!temp.id">
                <template slot="title"> 节点账号密码默认由系统生成：可以通过插件端数据目录下 agent_authorize.json 文件查看（如果自定义配置了账号密码将没有此文件） </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input-password v-model="temp.loginPwd" placeholder="节点密码,请查看节点启动输出的信息" />
          </a-form-model-item>
        </div>
        <a-form-model-item label="超时时间(s)" prop="timeOut">
          <a-input-number v-model="temp.timeOut" :min="0" placeholder="秒 (值太小可能会取不到节点状态)" style="width: 100%" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 管理节点 -->
    <a-drawer :title="drawerTitle" placement="right" :width="`${this.getCollapsed === 1 ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`" :visible="drawerVisible" @close="onClose">
      <!-- 节点管理组件 -->
      <node-layout v-if="drawerVisible" :node="temp" />
    </a-drawer>
    <!-- Terminal -->
    <a-modal v-model="terminalVisible" width="80%" title="Terminal" :footer="null" :maskClosable="false">
      <terminal v-if="terminalVisible" :sshId="temp.sshId" :nodeId="temp.id" />
    </a-modal>
    <!-- 解锁节点 -->
    <a-modal v-model="unlockNode" title="解锁节点" @ok="handleUnLockNodeOk" :maskClosable="false">
      <a-form-model :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="绑定工作空间" prop="workspaceId">
          <a-select show-search option-filter-prop="children" v-model="temp.workspaceId" placeholder="请选择工作空间">
            <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { mapGetters } from "vuex";
import { getNodeList, getNodeStatus, editNode, deleteNode, syncProject, unLockWorkspace } from "@/api/node";
import { getSshListAll } from "@/api/ssh";
import { syncScript } from "@/api/node-other";
import NodeLayout from "./node-layout";
import Terminal from "@/pages/ssh/terminal";
import { parseTime } from "@/utils/time";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";
import { getWorkSpaceListAll } from "@/api/workspace";
export default {
  components: {
    NodeLayout,
    Terminal,
  },
  data() {
    return {
      loading: false,
      childLoading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      sshList: [],
      list: [],
      temp: {
        timeOut: 0,
      },
      editNodeVisible: false,
      drawerVisible: false,
      terminalVisible: false,
      unlockNode: false,
      drawerTitle: "",
      columns: [
        // { title: "节点 ID", dataIndex: "id", sorter: true, key: "id", ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: "节点名称", dataIndex: "name", sorter: true, key: "name", ellipsis: true, scopedSlots: { customRender: "name" } },

        { title: "节点协议", dataIndex: "protocol", sorter: true, key: "protocol", width: 100, ellipsis: true, scopedSlots: { customRender: "protocol" } },
        { title: "节点地址", dataIndex: "url", sorter: true, key: "url", ellipsis: true, scopedSlots: { customRender: "url" } },
        { title: "超时时间", dataIndex: "timeOut", sorter: true, key: "timeOut", width: 100, ellipsis: true },
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
        { title: "操作", dataIndex: "operation", key: "operation", width: 360, scopedSlots: { customRender: "operation" }, align: "left" },
      ],
      childColumns: [
        { title: "系统名", dataIndex: "osName", key: "osName", width: 100, ellipsis: true, scopedSlots: { customRender: "osName" } },
        { title: "JDK 版本", dataIndex: "javaVersion", key: "javaVersion", ellipsis: true, scopedSlots: { customRender: "javaVersion" } },
        { title: "JVM 总内存", dataIndex: "totalMemory", key: "totalMemory", width: 120 },
        { title: "JVM 剩余内存", dataIndex: "freeMemory", key: "freeMemory", width: 140 },
        { title: "版本", dataIndex: "jpomVersion", key: "jpomVersion", width: 120 },
        { title: "Java 程序数", dataIndex: "javaVirtualCount", key: "javaVirtualCount", width: 120 },

        { title: "项目数", dataIndex: "count", key: "count", width: 90, scopedSlots: { customRender: "projectCount" } },
        { title: "脚本数", dataIndex: "scriptCount", key: "scriptCount", width: 90, scopedSlots: { customRender: "scriptCount" } },
        { title: "响应时间", dataIndex: "timeOut", key: "timeOut", width: 120 },
        { title: "已运行时间", dataIndex: "runTime", key: "runTime", width: 150, ellipsis: true, scopedSlots: { customRender: "runTime" } },
      ],
      rules: {
        id: [{ required: true, message: "Please input node id", trigger: "blur" }],
        name: [{ required: true, message: "Please input node name", trigger: "blur" }],
        url: [{ required: true, message: "Please input url", trigger: "blur" }],
        loginName: [{ required: true, message: "Please input login name", trigger: "blur" }],
        loginPwd: [{ required: true, message: "Please input login password", trigger: "blur" }],
        timeOut: [{ required: true, message: "Please input timeout", trigger: "blur" }],
      },
      workspaceList: [],
    };
  },
  computed: {
    ...mapGetters(["getCollapsed"]),
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
  watch: {},
  created() {
    this.loadData();
  },
  methods: {
    // 页面引导
    introGuide() {
      this.$store.dispatch("tryOpenGuide", {
        key: "node-list",
        options: {
          hidePrev: true,
          steps: [
            {
              title: "导航助手",
              element: document.querySelector(".node-config"),
              intro: "节点的账号密码可以通过 agent_authorize.json 文件查看",
            },
          ],
        },
      });
    },

    // 加载 SSH 列表
    loadSshList() {
      getSshListAll().then((res) => {
        if (res.code === 200) {
          this.sshList = res.data;
        }
      });
    },
    // 加载数据
    loadData(pointerEvent) {
      this.list = [];
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getNodeList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
          let nodeId = this.$route.query.nodeId;
          this.list.map((item) => {
            if (nodeId === item.id) {
              this.handleNode(item);
            }
          });
        }
        this.loading = false;
      });
    },
    // 展开行
    expand(expanded, record) {
      if (expanded) {
        if (!record.openStatus) {
          this.$notification.error({
            message: "节点未启用",
          });
          return false;
        }
        // 请求节点状态数据
        this.childLoading = true;
        getNodeStatus(record.id).then((res) => {
          if (res.code === 200) {
            // const index = this.list.findIndex(ele => ele.id === record.id);
            // this.list[index].children = res.data;
            record.children = res.data;
          }
          this.childLoading = false;
        });
      }
    },
    // 添加
    handleAdd() {
      this.$nextTick(() => {
        setTimeout(() => {
          this.introGuide();
        }, 500);
        this.$refs["editNodeForm"] && this.$refs["editNodeForm"].resetFields();
        this.temp = {
          type: "add",
          cycle: 0,
          protocol: "http",
          openStatus: 1,
          timeOut: 0,
          loginName: "jpomAgent",
        };
        this.editNodeVisible = true;
      });
      this.loadSshList();
    },
    // 进入终端
    handleTerminal(record) {
      this.temp = Object.assign(record);
      this.terminalVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign(record);
      this.loadSshList();
      // this.temp.tempGroup = "";
      this.editNodeVisible = true;
    },
    // 提交节点数据
    handleEditNodeOk() {
      // 检验表单
      this.$refs["editNodeForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
        editNode(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editNodeForm"].resetFields();
            this.editNodeVisible = false;
            this.loadData();
          }
        });
      });
    },
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除节点么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteNode(record.id).then((res) => {
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
    // 管理节点
    handleNode(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `${this.temp.name} (${this.temp.url})`;
      this.drawerVisible = true;
      let nodeId = this.$route.query.nodeId;
      if (nodeId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, nodeId: record.id },
        });
      }
    },
    syncNode(node) {
      syncProject(node.nodeId).then((res) => {
        if (res.code == 200) {
          this.$notification.success({
            message: res.msg,
          });
          return false;
        }
      });
    },
    syncNodeScript(node) {
      syncScript({
        nodeId: node.nodeId,
      }).then((res) => {
        if (res.code == 200) {
          this.$notification.success({
            message: res.msg,
          });
        }
      });
    },
    // 关闭抽屉层
    onClose() {
      this.drawerVisible = false;
      let query = Object.assign({}, this.$route.query);
      delete query.nodeId, delete query.id, delete query.pId;
      this.$router.replace({
        query: query,
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
    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data;
        }
      });
    },

    unlock(record) {
      this.unlockNode = true;
      this.loadWorkSpaceListAll();

      this.temp = Object.assign({}, record);
      this.temp.workspaceId = "";
    },
    handleUnLockNodeOk() {
      if (!this.temp.workspaceId) {
        this.$notification.warn({
          message: "请选择工作空间",
        });
        return false;
      }
      this.$confirm({
        title: "系统提示",
        content: "确定要将此节点绑定到这个工作空间吗？绑定后不可更改",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          unLockWorkspace({
            id: this.temp.id,
            workspaceId: this.temp.workspaceId,
          }).then((res) => {
            if (res.code == 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.unlockNode = false;
              this.loadData();
              return false;
            }
          });
        },
      });
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
</style>
