<template>
  <div class="full-content">
    <a-table :data-source="commandList" :columns="columns" size="middle" bordered :pagination="pagination" @change="changePage" :row-selection="rowSelection" rowKey="id">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="getCommandData" placeholder="搜索命令" class="search-input-item" />
          <a-input v-model="listQuery['%desc%']" @pressEnter="getCommandData" placeholder="描述" class="search-input-item" />
          <a-input v-model="listQuery['%autoExecCron%']" @pressEnter="getCommandData" placeholder="定时执行" class="search-input-item" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="getCommandData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="createCommand">新建命令</a-button>
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
              <div>命令模版是用于在线管理一些脚本命令，如初始化软件环境、管理应用程序等</div>

              <div>
                <ul>
                  <li>命令内容支持工作空间环境变量</li>
                  <li>执行命令将自动替换为 sh 命令文件、并自动加载环境变量：/etc/profile、/etc/bashrc、~/.bashrc、~/.bash_profile</li>
                  <li>执行命令包含：<b>#disabled-template-auto-evn</b> 将取消自动加载环境变量(注意是整行不能包含空格)</li>
                  <li>命令文件将上传至 ${user.home}/.jpom/xxxx.sh 执行完成将自动删除</li>
                </ul>
              </div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="desc" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-button size="small" type="primary" @click="handleExecute(record)">执行</a-button>
          <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑命令 -->
    <a-modal v-model="editCommandVisible" width="80vw" title="编辑 命令" @ok="handleEditCommandOk" :maskClosable="false">
      <a-form-model ref="editCommandForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item label="命令名称" prop="name">
          <a-input v-model="temp.name" :maxLength="100" placeholder="命令名称" />
        </a-form-model-item>

        <a-form-model-item prop="command">
          <template slot="label">
            命令内容
            <a-tooltip v-show="!temp.id">
              <template slot="title">
                <ul>
                  <li>可以引用工作空间的环境变量 变量占位符 #{xxxx} xxxx 为变量名称</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <div style="height: 40vh; overflow-y: scroll">
            <code-editor v-model="temp.command" :options="{ mode: 'shell', tabSize: 2, theme: 'abcdef' }"></code-editor>
          </div>
        </a-form-model-item>
        <a-form-model-item label="SSH节点">
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            placeholder="请选择SSH节点"
            mode="multiple"
            v-model="chooseSsh"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-model-item>

        <a-form-model-item label="默认参数">
          <div class="params-item" v-for="(item, index) in commandParams" :key="item.key">
            <div class="item-info">
              <a-input addon-before="参数值" v-model="item.value" placeholder="参数值" />
              <a-input addon-before="描述" v-model="item.desc" placeholder="参数描述" />
            </div>
            <div class="item-icon" @click="handleDeleteParam(index)">
              <a-icon type="minus-circle" style="color: #ff0000" />
            </div>
          </div>

          <a-button type="primary" @click="handleAddParam">添加参数</a-button>
        </a-form-model-item>
        <a-form-model-item label="自动执行" prop="autoExecCron">
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
        <a-form-model-item label="命令描述" prop="desc">
          <a-input v-model="temp.desc" :maxLength="255" type="textarea" :rows="3" style="resize: none" placeholder="命令详细描述" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>

    <a-modal v-model="executeCommandVisible" width="600px" title="执行 命令" @ok="handleExecuteCommandOk" :maskClosable="false">
      <a-form-model :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="命令名称" prop="name">
          <a-input v-model="temp.name" :disabled="true" placeholder="命令名称" />
        </a-form-model-item>

        <a-form-model-item label="SSH节点" required>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            mode="multiple"
            v-model="chooseSsh"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-model-item>

        <a-form-model-item label="命令参数">
          <div v-for="item in commandParams" :key="item.key">
            <div class="item-info">
              <a-input addon-before="参数值" v-model="item.value" placeholder="参数值" />
              <a-input addon-before="描述" v-model="item.desc" placeholder="参数描述" />
            </div>
            <div class="item-icon" @click="handleDeleteParam(index)">
              <a-icon type="minus-circle" style="color: #ff0000" />
            </div>
          </div>
          <a-button type="primary" @click="handleAddParam">添加参数</a-button>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 构建日志 -->
    <a-modal :width="'80vw'" v-model="logVisible" title="执行日志" :footer="null" :maskClosable="false">
      <command-log v-if="logVisible" :temp="temp" />
    </a-modal>
    <!-- 同步到其他工作空间 -->
    <a-modal v-model="syncToWorkspaceVisible" title="同步到其他工作空间" @ok="handleSyncToWorkspace" :maskClosable="false">
      <a-alert message="温馨提示" type="warning">
        <template slot="description">
          <ul>
            <li>同步机制采用<b>脚本名称</b>确定是同一个脚本</li>
            <li>当目标工作空间不存在对应的 脚本 时候将自动创建一个新的 脚本</li>
            <li>当目标工作空间已经存在 脚本 时候将自动同步 脚本内容、默认参数、自动执行、描述</li>
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
import {deleteCommand, editCommand, executeBatch, getCommandList, syncToWorkspace} from "@/api/command";
import {CHANGE_PAGE, COMPUTED_PAGINATION, CRON_DATA_SOURCE, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";
import {parseTime} from "@/utils/time";
import {getSshListAll} from "@/api/ssh";
import codeEditor from "@/components/codeEditor";
import CommandLog from "./command-view-log";
import {mapGetters} from "vuex";
import {getWorkSpaceListAll} from "@/api/workspace";

export default {
  components: { codeEditor, CommandLog },
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      cronDataSource: CRON_DATA_SOURCE,
      commandList: [],
      loading: false,
      editCommandVisible: false,
      executeCommandVisible: false,
      commandParams: [],
      sshList: [],
      chooseSsh: [],
      temp: {},
      logVisible: false,
      rules: {
        name: [{ required: true, message: "Please input name", trigger: "blur" }],
        command: [{ required: true, message: "Please input command", trigger: "blur" }],
      },
      columns: [
        { title: "命令名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "命令描述", dataIndex: "desc", ellipsis: true, scopedSlots: { customRender: "desc" } },
        { title: "定时执行", dataIndex: "autoExecCron", ellipsis: true, scopedSlots: { customRender: "autoExecCron" } },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          ellipsis: true,
          sorter: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          width: 170,
          ellipsis: true,
          sorter: true,
          customRender: (text) => {
            return parseTime(text);
          },
        },
        {
          title: "最后操作人",
          dataIndex: "modifyUser",
          width: 120,
          ellipsis: true,
          scopedSlots: { customRender: "modifyUser" },
        },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 180 },
      ],
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
    this.getCommandData();
    //this.getAllSSHList();
  },
  methods: {
    // 编辑命令信息
    handleEditCommandOk() {
      this.$refs["editCommandForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.formLoading = true;
        if (this.commandParams && this.commandParams.length > 0) {
          this.temp.defParams = JSON.stringify(this.commandParams);
        } else {
          this.temp.defParams = "";
        }
        this.temp.sshIds = this.chooseSsh.join(",");
        editCommand(this.temp).then((res) => {
          this.formLoading = false;
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            this.editCommandVisible = false;

            this.getCommandData();
          }
        });
      });
    },
    // 获取命令数据
    getCommandData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getCommandList(this.listQuery).then((res) => {
        if (200 === res.code) {
          this.commandList = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.getCommandData();
    },

    // 创建命令弹窗
    createCommand() {
      this.editCommandVisible = true;
      this.getAllSSHList();
      this.chooseSsh = [];
      this.temp = {};
      this.$refs["editCommandForm"] && this.$refs["editCommandForm"].resetFields();
    },
    // 修改
    handleEdit(row) {
      this.editCommandVisible = true;
      this.$refs["editCommandForm"] && this.$refs["editCommandForm"].resetFields();
      this.commandParams = [];
      if (row.defParams) {
        this.commandParams = JSON.parse(row.defParams);
      }
      this.temp = row;
      this.chooseSsh = row.sshIds ? row.sshIds.split(",") : [];
      this.getAllSSHList();
    },
    // 执行命令
    handleExecute(row) {
      if (typeof row.defParams === "string" && row.defParams) {
        this.commandParams = JSON.parse(row.defParams);
      } else {
        this.commandParams = [];
      }
      this.temp = row;
      this.chooseSsh = row.sshIds ? row.sshIds.split(",") : [];
      this.executeCommandVisible = true;
      this.getAllSSHList();
    },
    //  删除命令
    handleDelete(row) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除“" + row.name + "”命令？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteCommand(row.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.getCommandData();
            }
          });
        },
      });
    },
    // 获取所有ssh接点
    getAllSSHList() {
      getSshListAll().then((res) => {
        this.sshList = res.data || [];
      });
    },
    // 添加命令参数
    handleAddParam() {
      this.commandParams.push({});
    },
    // 删除命令参数
    handleDeleteParam(index) {
      this.commandParams.splice(index, 1);
    },
    handleParamChange(check) {
      if (!check) {
        this.commandParams = [];
      }
    },
    handleExecuteCommandOk() {
      if (!this.chooseSsh || this.chooseSsh.length <= 0) {
        this.$notification.error({
          message: "请选择执行节点",
        });
        return false;
      }

      executeBatch({
        id: this.temp.id,
        params: JSON.stringify(this.commandParams),
        nodes: this.chooseSsh.join(","),
      }).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.executeCommandVisible = false;
          this.temp = {
            commandId: this.temp.id,
            batchId: res.data,
          };
          this.logVisible = true;
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
<style scoped>
.config-editor {
  overflow-y: scroll;
  max-height: 300px;
}

.params-item {
  display: flex;
  align-items: center;
  border-bottom: 1px #e2e2e2 solid;
  padding-bottom: 5px;
}

.item-info {
  display: inline-block;
  width: 90%;
}

.item-icon {
  display: inline-block;
  width: 10%;
  text-align: center;
}
</style>
