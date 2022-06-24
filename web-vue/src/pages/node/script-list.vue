<template>
  <div class="node-full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :columns="columns" @change="changePage" :pagination="pagination" bordered rowKey="id">
      <template slot="title">
        <a-space>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="listQuery.nodeId"
            allowClear
            placeholder="请选择节点"
            class="search-input-item"
          >
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="名称" allowClear class="search-input-item" />
          <a-input v-model="listQuery['%autoExecCron%']" @pressEnter="loadData" placeholder="定时执行" class="search-input-item" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
          </a-tooltip>

          <a-tooltip>
            <template slot="title">
              <div>节点脚本模版是存储在节点中的命令脚本用于在线管理一些脚本命令，如初始化软件环境、管理应用程序等</div>

              <div>
                <ul>
                  <li>执行时候默认不加载全部环境变量、需要脚本里面自行加载</li>
                  <li>命令文件将在 ${插件端数据目录}/script/xxxx.sh 、bat 执行</li>
                  <li>添加脚本模版需要到节点管理中去添加</li>
                </ul>
              </div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>

          <a-tooltip placement="topLeft" title="清除服务端缓存节点所有的脚步模版信息, 需要重新同步">
            <a-icon @click="delAll()" type="delete" />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="`${nodeMap[text]} 节点ID： ${text}`">
        <span>{{ nodeMap[text] }}</span>
      </a-tooltip>
      <a-tooltip slot="name" @click="handleEdit(record)" slot-scope="text, record" placement="topLeft" :title="text">
        <!-- <span>{{ text }}</span> -->
        <a-button type="link" style="padding: 0px" size="small">{{ text }}</a-button>
      </a-tooltip>
      <template slot="scriptType" slot-scope="text">
        <a-tooltip v-if="text === 'server-sync'" title="服务端分发的脚本">
          <a-icon type="cluster" />
        </a-tooltip>
        <a-tooltip v-else title="本地脚本">
          <a-icon type="file-text" />
        </a-tooltip>
      </template>

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
          <a-button size="small" type="primary" @click="handleLog(record)">日志</a-button>
          <!-- <a-button size="small" :type="`${record.scriptType === 'server-sync' ? '' : 'primary'}`" @click="handleEdit(record)">{{ record.scriptType === "server-sync" ? "查看" : " 编辑" }}</a-button> -->
          <a-tooltip :title="`${record.scriptType === 'server-sync' ? '服务端分发同步的脚本不能直接删除,需要到服务端去解绑' : '删除'}`">
            <a-button size="small" :disabled="record.scriptType === 'server-sync'" type="danger" @click="handleDelete(record)">删除</a-button>
          </a-tooltip>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editScriptVisible" title="编辑 Script" @ok="handleEditScriptOk" :maskClosable="false" width="80vw">
      <a-form-model ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-alert v-if="this.temp.scriptType === 'server-sync'" message="服务端同步的脚本不能在此修改" banner />
        <a-form-model-item label="Script 名称" prop="name">
          <a-input v-model="temp.name" placeholder="名称" />
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
          <a-input v-model="temp.description" type="textarea" :rows="3" style="resize: none" placeholder="详细描述" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 脚本控制台组件 -->
    <a-drawer
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :visible="drawerConsoleVisible"
      @close="
        () => {
          this.drawerConsoleVisible = false;
        }
      "
    >
      <script-console v-if="drawerConsoleVisible" :nodeId="temp.nodeId" :defArgs="temp.defArgs" :id="temp.id" :scriptId="temp.scriptId" />
    </a-drawer>
    <!-- 脚本日志 -->
    <a-modal :title="drawerTitle" width="85vw" v-model="drawerLogVisible" :footer="null" :maskClosable="false">
      <script-log v-if="drawerLogVisible" :scriptId="temp.scriptId" :nodeId="temp.nodeId" />
    </a-modal>
  </div>
</template>
<script>
import {delAllCache, deleteScript, editScript, getScriptListAll, itemScript} from "@/api/node-other";
import codeEditor from "@/components/codeEditor";
import {getNodeListAll} from "@/api/node";
import ScriptConsole from "@/pages/node/node-layout/other/script-console";
import {CHANGE_PAGE, COMPUTED_PAGINATION, CRON_DATA_SOURCE, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";
import {parseTime} from "@/utils/time";
import ScriptLog from "@/pages/node/node-layout/other/script-log";

export default {
  components: {
    ScriptConsole,
    codeEditor,
    ScriptLog,
  },
  props: {},
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      cronDataSource: CRON_DATA_SOURCE,
      list: [],
      temp: {},
      nodeMap: {},
      editScriptVisible: false,
      drawerTitle: "",
      drawerConsoleVisible: false,
      drawerLogVisible: false,
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "节点名称", dataIndex: "nodeId", ellipsis: true, scopedSlots: { customRender: "nodeId" } },
        { title: "类型", dataIndex: "scriptType", width: 70, align: "center", ellipsis: true, scopedSlots: { customRender: "scriptType" } },
        { title: "定时执行", dataIndex: "autoExecCron", ellipsis: true, width: 120, scopedSlots: { customRender: "autoExecCron" } },
        { title: "修改时间", dataIndex: "modifyTimeMillis", width: 170, sorter: true, ellipsis: true, scopedSlots: { customRender: "modifyTimeMillis" } },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },
        { title: "最后操作人", dataIndex: "lastRunUser", ellipsis: true, scopedSlots: { customRender: "lastRunUser" } },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 170 },
      ],
      rules: {
        name: [{ required: true, message: "Please input Script name", trigger: "blur" }],
        context: [{ required: true, message: "Please input Script context", trigger: "blur" }],
      },
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  mounted() {
    // this.calcTableHeight();

    getNodeListAll().then((res) => {
      if (res.code === 200) {
        res.data.forEach((item) => {
          this.nodeMap[item.id] = item.name;
        });
      }
      this.loadData();
    });
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getScriptListAll(this.listQuery).then((res) => {
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
    // 修改
    handleEdit(record) {
      itemScript({
        id: record.scriptId,
        nodeId: record.nodeId,
      }).then((res) => {
        if (res.code === 200) {
          this.temp = Object.assign({}, res.data);
          this.temp.nodeId = record.nodeId;
          //
          this.editScriptVisible = true;
        }
      });
    },
    // 提交 Script 数据
    handleEditScriptOk() {
      if (this.temp.scriptType === "server-sync") {
        this.$notification.warning({
          message: "服务端同步的脚本不能在此修改",
        });
        return;
      }
      // 检验表单
      this.$refs["editScriptForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
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
            nodeId: record.nodeId,
            id: record.scriptId,
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
      this.temp = Object.assign({}, record);
      this.drawerTitle = `控制台(${this.temp.name})`;
      this.drawerConsoleVisible = true;
    },
    handleLog(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `日志(${this.temp.name})`;
      this.drawerLogVisible = true;
    },
    // // 关闭 console
    // onConsoleClose() {
    //   this.drawerConsoleVisible = false;
    // },
    delAll() {
      this.$confirm({
        title: "系统提示",
        content: "确定要清除服务端所有的脚步模版缓存信息吗？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          delAllCache().then((res) => {
            if (res.code == 200) {
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
  },
};
</script>
<style scoped></style>
