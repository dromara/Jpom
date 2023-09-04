<template>
  <div class="node-full-content">
    <!-- 数据表格 -->
    <a-table size="middle" :data-source="list" :columns="columns" @change="changePage" :pagination="pagination" bordered
             rowKey="id">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" :placeholder=$t('common.name') allowClear class="search-input-item"/>
          <a-tooltip :title=$t('common.goBackP1')>
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('common.search') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('common.add') }}</a-button>

          <a-tooltip placement="topLeft" title="清除服务端缓存节点所有的脚步模版信息并重新同步">
            <a-icon @click="sync()" type="sync" spin/>
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="global" slot-scope="text">
        <a-tag v-if="text === 'GLOBAL'">{{ $t('common.global') }}</a-tag>
        <a-tag v-else>{{ $t('common.workSpace') }}</a-tag>
      </template>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleExec(record)">{{ $t('common.execute') }}</a-button>
          <a-button size="small" :type="`${record.scriptType === 'server-sync' ? '' : 'primary'}`"
                    @click="handleEdit(record)">
            {{ record.scriptType === "server-sync" ? $t('common.look') : $t('common.edit') }}
          </a-button>
          <a-tooltip
              :title="`${record.scriptType === 'server-sync' ? '服务端分发同步的脚本不能直接删除,需要到服务端去操作' : $t('common.delete')}`">
            <a-button size="small" :disabled="record.scriptType === 'server-sync'" type="danger"
                      @click="handleDelete(record)">{{ $t('common.delete') }}
            </a-button>
          </a-tooltip>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model="editScriptVisible" title="编辑 Script" @ok="handleEditScriptOk"
             :maskClosable="false" width="80vw">
      <a-form-model ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }"
                    :wrapper-col="{ span: 18 }">
        <a-alert v-if="this.temp.scriptType === 'server-sync'" message="服务端同步的脚本不能在此修改" banner/>
        <a-form-model-item v-if="temp.id" label="ScriptId" prop="id">
          <a-input v-model="temp.id" disabled readOnly/>
        </a-form-model-item>
        <a-form-model-item label="Script 名称" prop="name">
          <a-input :maxLength="50" v-model="temp.name" :placeholder="名称"/>
        </a-form-model-item>
        <a-form-model-item label="Script 内容" prop="context">
          <div style="height: 40vh; overflow-y: scroll">
            <code-editor v-model="temp.context" :options="{ mode: 'shell', tabSize: 2, theme: 'abcdef' }"></code-editor>
          </div>
        </a-form-model-item>
        <!-- <a-form-model-item label="默认参数" prop="defArgs">
          <a-input v-model="temp.defArgs" placeholder="默认参数" />
        </a-form-model-item> -->
        <a-form-model-item :label=$t('common.defaultParam')>
          <div v-for="(item, index) in commandParams" :key="item.key">
            <a-row type="flex" justify="center" align="middle">
              <a-col :span="22">
                <a-input :addon-before="`参数${index + 1}描述`" v-model="item.desc"
                         placeholder="参数描述,参数描述没有实际作用,仅是用于提示参数的含义"/>
                <a-input :addon-before="`参数${index + 1}值`" v-model="item.value"
                         placeholder="参数值,添加默认参数后在手动执行脚本时需要填写参数值"/>
              </a-col>
              <a-col :span="2">
                <a-row type="flex" justify="center" align="middle">
                  <a-col>
                    <a-icon @click="() => commandParams.splice(index, 1)" type="minus-circle" style="color: #ff0000"/>
                  </a-col>
                </a-row>
              </a-col>
            </a-row>
            <a-divider style="margin: 5px 0"/>
          </div>

          <a-button type="primary" @click="() => commandParams.push({})">{{ $t('common.addParams') }}</a-button>
        </a-form-model-item>
        <a-form-model-item :label=$t('common.share') prop="global">
          <a-radio-group v-model="temp.global">
            <a-radio :value="true"> {{ $t('common.global') }}</a-radio>
            <a-radio :value="false"> {{ $t('common.currentWorkSpace') }}</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item :label=$t('common.timedExec') prop="autoExecCron">
          <a-auto-complete v-model="temp.autoExecCron"
                           placeholder="如果需要定时自动执行则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）"
                           option-label-prop="value">
            <template slot="dataSource">
              <a-select-opt-group v-for="group in cronDataSource" :key="group.title">
                <span slot="label">
                  {{ group.title }}
                </span>
                <a-select-option v-for="opt in group.children" :key="opt.title" :value="opt.value"> {{ opt.title }}
                  {{ opt.value }}
                </a-select-option>
              </a-select-opt-group>
            </template>
          </a-auto-complete>
        </a-form-model-item>
        <a-form-model-item :label=$t('common.description') prop="description">
          <a-input :maxLength="200" v-model="temp.description" type="textarea" :rows="3" style="resize: none"
                   :placeholder="详细描述"/>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 脚本控制台组件 -->
    <a-drawer destroyOnClose :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible"
              @close="onConsoleClose">
      <script-console v-if="drawerConsoleVisible" :nodeId="node.id" :defArgs="temp.defArgs" :id="temp.id"
                      :scriptId="temp.scriptId"/>
    </a-drawer>
  </div>
</template>
<script>
import {getScriptList, editScript, deleteScript, itemScript, syncScript} from "@/api/node-other";
import codeEditor from "@/components/codeEditor";
import ScriptConsole from "./script-console";
import {CRON_DATA_SOURCE, COMPUTED_PAGINATION, CHANGE_PAGE, PAGE_DEFAULT_LIST_QUERY, parseTime} from "@/utils/const";

export default {
  components: {
    ScriptConsole,
    codeEditor,
  },
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      temp: {},
      cronDataSource: CRON_DATA_SOURCE,
      editScriptVisible: false,
      drawerTitle: "",
      drawerConsoleVisible: false,

      columns: [
        {title: "Script ID", dataIndex: "scriptId", width: 150, ellipsis: true, scopedSlots: {customRender: "tooltip"}},
        {title: this.$t('common.name'), dataIndex: "name", ellipsis: true, width: 200, scopedSlots: {customRender: "tooltip"}},
        {
          title: this.$t('common.timedExec'),
          dataIndex: "autoExecCron",
          ellipsis: true,
          width: "120px",
          scopedSlots: {customRender: "autoExecCron"}
        },
        {title: this.$t('common.share'), dataIndex: "workspaceId", ellipsis: true, scopedSlots: {customRender: "global"}, width: "90px"},
        {
          title: this.$t('common.createTime'),
          dataIndex: "createTimeMillis",
          ellipsis: true,
          sorter: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        {
          title: this.$t('common.modifyTime'),
          dataIndex: "modifyTimeMillis",
          width: "170px",
          ellipsis: true,
          sorter: true,
          customRender: (text) => parseTime(text),
        },
        {
          title: "创建人",
          dataIndex: "createUser",
          ellipsis: true,
          scopedSlots: {customRender: "tooltip"},
          width: "120px"
        },
        {
          title: "修改人",
          dataIndex: "modifyUser",
          ellipsis: true,
          scopedSlots: {customRender: "modifyUser"},
          width: "120px"
        },
        // { title: "最后操作人", dataIndex: "lastRunUser", ellipsis: true, width: 150, scopedSlots: { customRender: "lastRunUser" } },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          scopedSlots: {customRender: "operation"},
          fixed: "right",
          width: "180px"
        },
      ],
      rules: {
        name: [{required: true, message: "请输入脚本名称", trigger: "blur"}],
        context: [{required: true, message: "请输入脚本内容", trigger: "blur"}],
      },
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
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getScriptList({...this.listQuery, nodeId: this.node.id}).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    parseTime,
    // 添加
    handleAdd() {
      this.temp = {
        type: "add",
      };
      this.commandParams = [];
      this.editScriptVisible = true;
    },
    // 修改
    handleEdit(record) {
      itemScript({
        id: record.scriptId,
        nodeId: this.node.id,
      }).then((res) => {
        this.temp = Object.assign({}, res.data, {global: res.data.workspaceId === "GLOBAL", workspaceId: ""});
        this.commandParams = this.temp.defArgs ? JSON.parse(this.temp.defArgs) : [];
        //
        this.editScriptVisible = true;
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
      // 检验表单
      this.$refs["editScriptForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.temp.nodeId = this.node.id;
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
            nodeId: this.node.id,
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
    // 关闭 console
    onConsoleClose() {
      this.drawerConsoleVisible = false;
    },

    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, {pagination, sorter});
      this.loadData();
    },
    sync() {
      syncScript({
        nodeId: this.node.id,
      }).then((res) => {
        if (res.code == 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.loadData();
        }
      });
    },
  },
};
</script>
<style scoped></style>
