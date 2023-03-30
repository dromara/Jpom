<template>
  <div class="node-full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :columns="columns" @change="changePage" :pagination="pagination" bordered rowKey="id">
      <template slot="title">
        <a-space>
          <a-select v-model="listQuery.nodeId" allowClear placeholder="请选择节点" class="search-input-item">
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
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="`${nodeMap[text]} 节点ID： ${text}`">
        <span>{{ nodeMap[text] }}</span>
      </a-tooltip>
      <a-tooltip slot="name" @click="handleEdit(record)" slot-scope="text, record" placement="topLeft" :title="text">
        <!-- <span>{{ text }}</span> -->
        <a-button type="link" style="padding: 0px" size="small">{{ text }}</a-button>
      </a-tooltip>
      <template slot="global" slot-scope="text">
        <a-tag v-if="text === 'GLOBAL'">全局</a-tag>
        <a-tag v-else>工作空间</a-tag>
      </template>
      <template slot="scriptType" slot-scope="text">
        <a-tooltip v-if="text === 'server-sync'" title="服务端分发的脚本">
          <a-icon type="cluster" />
        </a-tooltip>
        <a-tooltip v-else title="本地脚本">
          <a-icon type="file-text" />
        </a-tooltip>
      </template>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleExec(record)">执行</a-button>
          <a-button size="small" type="primary" @click="handleLog(record)">日志</a-button>
          <a-button size="small" type="primary" @click="handleTrigger(record)">触发器</a-button>
          <!-- <a-button size="small" :type="`${record.scriptType === 'server-sync' ? '' : 'primary'}`" @click="handleEdit(record)">{{ record.scriptType === "server-sync" ? "查看" : " 编辑" }}</a-button> -->
          <a-tooltip :title="`${record.scriptType === 'server-sync' ? '服务端分发同步的脚本不能直接删除,需要到服务端去操作' : '删除'}`">
            <a-button size="small" :disabled="record.scriptType === 'server-sync'" type="danger" @click="handleDelete(record)">删除</a-button>
          </a-tooltip>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model="editScriptVisible" title="编辑 Script" @ok="handleEditScriptOk" :maskClosable="false" width="80vw">
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
        <!-- <a-form-model-item label="默认参数" prop="defArgs">
          <a-input v-model="temp.defArgs" placeholder="默认参数" />
        </a-form-model-item> -->
        <a-form-model-item label="默认参数">
          <div v-for="(item, index) in commandParams" :key="item.key">
            <a-row type="flex" justify="center" align="middle">
              <a-col :span="22">
                <a-input :addon-before="`参数${index + 1}描述`" v-model="item.desc" placeholder="参数描述,参数描述没有实际作用,仅是用于提示参数的含义" />
                <a-input :addon-before="`参数${index + 1}值`" v-model="item.value" placeholder="参数值,添加默认参数后在手动执行脚本时需要填写参数值" />
              </a-col>
              <a-col :span="2">
                <a-row type="flex" justify="center" align="middle">
                  <a-col>
                    <a-icon @click="() => commandParams.splice(index, 1)" type="minus-circle" style="color: #ff0000" />
                  </a-col>
                </a-row>
              </a-col>
            </a-row>
            <a-divider style="margin: 5px 0" />
          </div>

          <a-button type="primary" @click="() => commandParams.push({})">添加参数</a-button>
        </a-form-model-item>
        <a-form-model-item label="共享" prop="global">
          <a-radio-group v-model="temp.global">
            <a-radio :value="true"> 全局</a-radio>
            <a-radio :value="false"> 当前工作空间</a-radio>
          </a-radio-group>
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
    <a-drawer
      destroyOnClose
      :title="drawerTitle"
      width="50vw"
      :visible="drawerLogVisible"
      @close="
        () => {
          this.drawerLogVisible = false;
        }
      "
    >
      <script-log v-if="drawerLogVisible" :scriptId="temp.scriptId" :nodeId="temp.nodeId" />
    </a-drawer>
    <!-- 触发器 -->
    <a-modal destroyOnClose v-model="triggerVisible" title="触发器" width="50%" :footer="null">
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
                    <li>单个触发器地址中：第一个随机字符串为脚本ID，第二个随机字符串为 token</li>
                    <li>重置为重新生成触发地址,重置成功后之前的触发器地址将失效,触发器绑定到生成触发器到操作人上,如果将对应的账号删除触发器将失效</li>
                    <li>批量触发参数 BODY json： [ { "id":"1", "token":"a" } ]</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.triggerUrl"
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
                :message="`单个触发器地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
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
  </div>
</template>
<script>
import { delAllCache, deleteScript, editScript, getScriptListAll, itemScript, getTriggerUrl } from "@/api/node-other";
import codeEditor from "@/components/codeEditor";
import { getNodeListAll } from "@/api/node";
import ScriptConsole from "@/pages/node/node-layout/other/script-console";
import { CHANGE_PAGE, COMPUTED_PAGINATION, CRON_DATA_SOURCE, PAGE_DEFAULT_LIST_QUERY, parseTime } from "@/utils/const";
import ScriptLog from "@/pages/node/node-layout/other/script-log";
import Vue from "vue";

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
        { title: "scriptId", dataIndex: "scriptId", ellipsis: true, width: 150, scopedSlots: { customRender: "tooltip" } },
        { title: "名称", dataIndex: "name", ellipsis: true, width: 200, scopedSlots: { customRender: "name" } },
        { title: "节点名称", dataIndex: "nodeId", ellipsis: true, width: 150, scopedSlots: { customRender: "nodeId" } },
        { title: "类型", dataIndex: "scriptType", width: 70, align: "center", ellipsis: true, scopedSlots: { customRender: "scriptType" } },
        { title: "共享", dataIndex: "workspaceId", ellipsis: true, scopedSlots: { customRender: "global" }, width: "90px" },
        { title: "定时执行", dataIndex: "autoExecCron", ellipsis: true, width: 120, scopedSlots: { customRender: "autoExecCron" } },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          width: "170px",
          ellipsis: true,
          customRender: (text) => parseTime(text),
        },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          width: "170px",
          ellipsis: true,
          customRender: (text) => parseTime(text),
        },
        { title: "创建人", dataIndex: "createUser", ellipsis: true, scopedSlots: { customRender: "tooltip" }, width: "120px" },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: "120px" },
        { title: "最后操作人", dataIndex: "lastRunUser", ellipsis: true, scopedSlots: { customRender: "lastRunUser" }, width: 120 },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, fixed: "right", width: "240px" },
      ],
      rules: {
        name: [{ required: true, message: "请输入脚本名称", trigger: "blur" }],
        context: [{ required: true, message: "请输入脚本内容", trigger: "blur" }],
      },
      triggerVisible: false,
      commandParams: [],
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
    parseTime,
    // 修改
    handleEdit(record) {
      itemScript({
        id: record.scriptId,
        nodeId: record.nodeId,
      }).then((res) => {
        if (res.code === 200) {
          this.temp = Object.assign({}, res.data, { global: res.data.workspaceId === "GLOBAL", workspaceId: "" });
          this.temp.nodeId = record.nodeId;
          this.commandParams = this.temp.defArgs ? JSON.parse(this.temp.defArgs) : [];
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
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              this.$notification.error({
                message: "请填写第" + (i + 1) + "个参数的描述",
              });
              return false;
            }
          }
          this.temp.defArgs = JSON.stringify(this.commandParams);
        } else {
          this.temp.defArgs = "";
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
    // 触发器
    handleTrigger(record) {
      this.temp = Object.assign({}, record);
      this.tempVue = Vue;
      getTriggerUrl({
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
      getTriggerUrl({
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
  },
};
</script>
<style scoped></style>
