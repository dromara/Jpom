<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :columns="columns" @change="changePage" :pagination="pagination" bordered rowKey="id" :row-selection="rowSelection">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" placeholder="名称" @pressEnter="loadData" allowClear class="search-input-item" />
          <a-input v-model="listQuery['%description%']" placeholder="描述" @pressEnter="loadData" class="search-input-item" />
          <a-input v-model="listQuery['%autoExecCron%']" placeholder="定时执行" @pressEnter="loadData" class="search-input-item" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="createScript">新建脚本</a-button>
          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()"> 更多 <a-icon type="down" /> </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button type="primary" :disabled="!tableSelections || !tableSelections.length" @click="syncToWorkspaceShow">工作空间同步</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
          <a-tooltip>
            <template slot="title">
              <div>脚本模版是存储在服务端中的命令脚本用于在线管理一些脚本命令，如初始化软件环境、管理应用程序等</div>

              <div>
                <ul>
                  <li>执行时候默认不加载全部环境变量、需要脚本里面自行加载</li>
                  <li>命令文件将在 ${数据目录}/script/xxxx.sh、bat 执行</li>
                  <li>分发节点是指在编辑完脚本后自动将脚本内容同步节点的脚本,一般用户节点分发功能中的 DSL 模式</li>
                </ul>
              </div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ nodeMap[text] }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip
        slot="modifyTimeMillis"
        slot-scope="text, record"
        :title="`创建时间：${parseTime(record.createTimeMillis)} ${record.modifyTimeMillis ? '修改时间：' + parseTime(record.modifyTimeMillis) : ''}`"
      >
        <span>{{ parseTime(record.modifyTimeMillis) }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleExec(record)">执行</a-button>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
              更多
              <a-icon type="down" />
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" :disabled="!record.nodeIds" @click="handleUnbind(record)">解绑</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editScriptVisible" title="编辑 Script" @ok="handleEditScriptOk" :maskClosable="false" width="80vw">
      <a-form-model ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-form-model-item v-if="temp.id" label="ScriptId" prop="id">
          <a-input v-model="temp.id" disabled readOnly />
        </a-form-model-item>
        <a-form-model-item label="Script 名称" prop="name">
          <a-input :maxLength="50" v-model="temp.name" placeholder="名称" />
        </a-form-model-item>
        <a-form-model-item label="Script 内容" prop="context">
          <div style="height: 40vh; overflow-y: scroll">
            <code-editor v-model="temp.context" :options="{ mode: 'shell', tabSize: 2, theme: 'abcdef' }"></code-editor>
          </div>
        </a-form-model-item>
        <a-form-model-item label="默认参数" prop="defArgs">
          <a-input v-model="temp.defArgs" placeholder="默认参数" />
        </a-form-model-item>
        <a-form-model-item label="定时执行" prop="autoExecCron">
          <a-auto-complete v-model="temp.autoExecCron" placeholder="如果需要定时自动执行则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）" option-label-prop="value">
            <template slot="dataSource">
              <a-select-opt-group v-for="group in cronDataSource" :key="group.title">
                <span slot="label">
                  {{ group.title }}
                </span>
                <a-select-option v-for="opt in group.children" :key="opt.title" :value="opt.value"> {{ opt.title }} {{ opt.value }} </a-select-option>
              </a-select-opt-group>
            </template>
          </a-auto-complete>
        </a-form-model-item>
        <a-form-model-item label="描述" prop="description">
          <a-input v-model="temp.description" maxLength="200" type="textarea" :rows="3" style="resize: none" placeholder="详细描述" />
        </a-form-model-item>
        <a-form-model-item>
          <template slot="label">
            分发节点
            <a-tooltip v-show="!temp.id">
              <template slot="title"> 分发节点是指在编辑完脚本后自动将脚本内容同步节点的脚本中 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            placeholder="请选择分发到的节点"
            mode="multiple"
            v-model="temp.chooseNode"
          >
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 脚本控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <script-console v-if="drawerConsoleVisible" :defArgs="temp.defArgs" :id="temp.id" />
    </a-drawer>
    <!-- 同步到其他工作空间 -->
    <a-modal v-model="syncToWorkspaceVisible" title="同步到其他工作空间" @ok="handleSyncToWorkspace" :maskClosable="false">
      <a-alert message="温馨提示" type="warning">
        <template slot="description">
          <ul>
            <li>同步机制采用<b>脚本名称</b>确定是同一个脚本</li>
            <li>当目标工作空间不存在对应的 脚本 时候将自动创建一个新的 脚本</li>
            <li>当目标工作空间已经存在 脚本 时候将自动同步 脚本内容、默认参数、定时执行、描述</li>
          </ul>
        </template>
      </a-alert>
      <a-form-model :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item> </a-form-model-item>
        <a-form-model-item label="选择工作空间" prop="workspaceId">
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            v-model="temp.workspaceId"
            placeholder="请选择工作空间"
          >
            <a-select-option :disabled="getWorkspaceId === item.id" v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import {deleteScript, editScript, getScriptList, syncToWorkspace, unbindScript} from "@/api/server-script";
import codeEditor from "@/components/codeEditor";
import {getNodeListAll} from "@/api/node";
import ScriptConsole from "@/pages/script/script-console";
import {CHANGE_PAGE, COMPUTED_PAGINATION, CRON_DATA_SOURCE, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";
import {parseTime} from "@/utils/time";
import {mapGetters} from "vuex";
import {getWorkSpaceListAll} from "@/api/workspace";

export default {
  components: {
    ScriptConsole,
    codeEditor,
  },
  props: {},
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      cronDataSource: CRON_DATA_SOURCE,
      list: [],
      temp: {},
      nodeList: [],
      editScriptVisible: false,
      drawerTitle: "",
      drawerConsoleVisible: false,
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "描述", dataIndex: "description", ellipsis: true, scopedSlots: { customRender: "description" } },
        { title: "定时执行", dataIndex: "autoExecCron", ellipsis: true, scopedSlots: { customRender: "autoExecCron" } },
        { title: "修改时间", dataIndex: "modifyTimeMillis", sorter: true, width: 170, ellipsis: true, scopedSlots: { customRender: "modifyTimeMillis" } },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },
        { title: "最后操作人", dataIndex: "lastRunUser", ellipsis: true, scopedSlots: { customRender: "lastRunUser" } },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 180 },
      ],
      rules: {
        name: [{ required: true, message: "Please input Script name", trigger: "blur" }],
        context: [{ required: true, message: "Please input Script context", trigger: "blur" }],
      },
      tableSelections: [],
      syncToWorkspaceVisible: false,
      workspaceList: [],
    };
  },
  computed: {
    ...mapGetters(["getWorkspaceId"]),
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
    // this.calcTableHeight();

    this.loadData();
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getScriptList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    parseTime(v) {
      return parseTime(v);
    },
    // 获取所有节点
    getAllNodeList() {
      getNodeListAll().then((res) => {
        this.nodeList = res.data || [];
      });
    },
    createScript() {
      this.temp = {};
      this.editScriptVisible = true;
      this.getAllNodeList();
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record);
      // record;
      //
      // this.temp.;
      this.temp = { ...this.temp, chooseNode: record.nodeIds ? record.nodeIds.split(",") : [] };
      this.editScriptVisible = true;
      this.getAllNodeList();
    },
    // 提交 Script 数据
    handleEditScriptOk() {
      // 检验表单
      this.$refs["editScriptForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
        this.temp.nodeIds = this.temp?.chooseNode?.join(",");
        editScript(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });

            this.editScriptVisible = false;
            this.loadData();
            this.$refs["editScriptForm"].resetFields();
          }
        });
      });
    },
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除脚本么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: record.id,
          };
          // 删除
          deleteScript(params).then((res) => {
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
    // 执行 Script
    handleExec(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `控制台(${this.temp.name})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭 console
    onConsoleClose() {
      this.drawerConsoleVisible = false;
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
    // 解绑
    handleUnbind(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要解绑脚本关联的节点么？解绑不会真实请求节点解绑,一般用于服务器无法连接且已经确定不再使用。如果误操作可能冗余数据",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 解绑
          unbindScript({
            id: record.id,
          }).then((res) => {
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
        workspaceId: this.temp.workspaceId,
      }).then((res) => {
        if (res.code == 200) {
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
