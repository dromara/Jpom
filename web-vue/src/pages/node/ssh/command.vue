<template>
  <div class="full-content">
    <div ref="filter" class="filter">
      <a-input v-model="listQuery['%name%']" placeholder="搜索命令" class="search-input-item" />
      <a-tooltip title="按住 Ctr 或者 Alt 键点击按钮快速回到第一页">
        <a-button type="primary" @click="getCommandData">搜索</a-button>
      </a-tooltip>
      <a-button type="primary" @click="createCommand">新建命令</a-button>
    </div>
    <a-table :loading="loading" :data-source="commandList" :columns="columns" bordered :pagination="pagination" @change="changePage" :rowKey="(record, index) => index">
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="desc" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="primary" @click="handleExecute(record)">执行</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>

    <a-modal v-model="editCommandVisible" width="80vw" title="编辑 命令" @ok="handleEditCommandOk" :maskClosable="false">
      <a-form-model ref="editCommandForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="命令名称" prop="name">
          <a-input v-model="temp.name" placeholder="命令名称" />
        </a-form-model-item>

        <a-form-model-item label="命令内容" prop="command">
          <div style="height: 40vh; overflow-y: scroll">
            <code-editor v-model="temp.command" :options="{ mode: 'shell', tabSize: 2, theme: 'abcdef' }"></code-editor>
          </div>
        </a-form-model-item>
        <a-form-model-item label="SSH节点">
          <a-select show-search option-filter-prop="children" mode="multiple" v-model="chooseSsh">
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
          <a-input v-model="temp.autoExecCron" placeholder="如果需要定时自动执行则填写,cron 表达式" />
        </a-form-model-item>
        <a-form-model-item label="命令描述" prop="desc">
          <a-input v-model="temp.desc" type="textarea" :rows="3" style="resize: none" placeholder="命令详细描述" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>

    <a-modal v-model="executeCommandVisible" width="600px" title="执行 命令" @ok="handleExecuteCommandOk" :maskClosable="false">
      <a-form-model :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="命令名称" prop="name">
          <a-input v-model="temp.name" :disabled="true" placeholder="命令名称" />
        </a-form-model-item>

        <a-form-model-item label="SSH节点" required>
          <a-select show-search option-filter-prop="children" mode="multiple" v-model="chooseSsh">
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
  </div>
</template>

<script>
import { deleteCommand, editCommand, executeBatch, getCommandList } from "@/api/command";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_LIST_QUERY, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_SIZW_OPTIONS } from "@/utils/const";
import { parseTime } from "@/utils/time";
import { getSshListAll } from "@/api/ssh";
import codeEditor from "@/components/codeEditor";
import CommandLog from "./command-view-log";

export default {
  components: { codeEditor, CommandLog },
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
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

        {
          title: "创建时间",
          dataIndex: "createTimeMillis",

          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        {
          title: "最后操作人",
          dataIndex: "modifyUser",
          width: 120,
          ellipsis: true,
          scopedSlots: { customRender: "modifyUser" },
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 300 },
      ],
    };
  },
  computed: {
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
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      if (sorter) {
        this.listQuery.order = sorter.order;
        this.listQuery.order_field = sorter.field;
      }
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
  },
};
</script>
<style scoped>
.config-editor {
  overflow-y: scroll;
  max-height: 300px;
}

.ant-btn {
  margin-right: 10px;
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
