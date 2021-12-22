<template>
  <div class="full-content">
    <div ref="filter" class="filter">
      <a-input v-model="listQuery['%name%']" placeholder="搜索命令" class="search-input-item"/>
      <a-button type="primary" @click="getCommandData">搜索</a-button>
      <a-button type="primary" @click="createCommand">新建命令</a-button>
    </div>
    <a-table
      :data-source="commandList"
      :columns="columns"
      bordered
      :pagination="pagination"
      @change="changePage"
      :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
      :rowKey="(record, index) => index"
    >
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="primary" @click="handleExecute(record)">执行</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>

    <a-modal v-model="editCommandVisible" width="600px" title="编辑 命令" @ok="handleEditCommandOk" :maskClosable="false">
      <a-form-model ref="editCommandForm" :rules="rules" :model="command" :label-col="{ span: 4 }"
                    :wrapper-col="{ span: 18 }">
        <a-form-model-item label="命令名称" prop="name">
          <a-input v-model="command.name" placeholder="命令名称"/>
        </a-form-model-item>
        <a-form-model-item label="命令描述" prop="desc">
          <a-input
            v-model="command.desc"
            type="textarea"
            :rows="3"
            style="resize: none"
            placeholder="命令详细描述"
          />
        </a-form-model-item>
        <!--        <a-form-model-item label="命令类型" prop="type">-->
        <!--          <a-radio-group v-model="command.type" name="type">-->
        <!--            <a-radio :value="0">SHELL</a-radio>-->
        <!--            <a-radio :value="1">POWERSHELL</a-radio>-->
        <!--          </a-radio-group>-->
        <!--        </a-form-model-item>-->
        <a-form-model-item label="执行用户" prop="executionRole">
          <a-input v-model="command.executionRole" placeholder="执行用户，默认为root"/>
        </a-form-model-item>
        <a-form-model-item label="执行路径" prop="executionPath">
          <a-input v-model="command.executionPath" placeholder="执行路径，默认为~/"/>
        </a-form-model-item>
        <a-form-model-item label="指令内容" prop="command">
          <a-input
            v-model="command.command"
            type="textarea"
            :rows="8"
            style="resize: none"
            placeholder="命令内容"
          />
        </a-form-model-item>
        <a-form-model-item label="超时时间" prop="timeout">
          <a-input v-model="command.timeout" placeholder="超时时间,单位：秒，默认60"/>
        </a-form-model-item>
        <a-form-model-item label="开启参数">
          <a-switch v-model="command.openParam" checked-children="开" un-checked-children="关"
                    @change="handleParamChange"/>
        </a-form-model-item>
        <a-form-model-item label="命令参数" v-if="command.openParam">
          <a-button type="primary" @click="handleAddParam">添加参数</a-button>
          <div v-if="commandParams.length>0">
            <div class="params-item" v-for="(item,index) in commandParams" :key="item.key">
              <div class="item-info">
                <a-input addon-before="参数名" v-model="item.name" placeholder="参数名"/>
                <a-input addon-before="描述" v-model="item.desc" placeholder="参数描述"/>
              </div>
              <div class="item-icon" @click="handleDeleteParam(index)">
                <a-icon type="minus-circle" style="color: #ff0000"/>
              </div>
            </div>
          </div>
        </a-form-model-item>
      </a-form-model>
    </a-modal>

    <a-modal v-model="executeCommandVisible" width="600px" title="执行 命令" @ok="handleExecuteCommandOk"
             :maskClosable="false">
      <a-form-model :model="execute" :label-col="{ span: 4 }"
                    :wrapper-col="{ span: 18 }">
        <a-form-model-item label="命令名称" prop="name">
          <a-input v-model="execute.name" placeholder="命令名称"/>
        </a-form-model-item>
        <a-form-model-item label="命令描述" prop="desc">
          <a-input
            v-model="execute.desc"
            type="textarea"
            :rows="3"
            style="resize: none"
            placeholder="命令详细描述"
          />
        </a-form-model-item>
        <a-form-model-item label="执行用户" prop="executionRole">
          <a-input v-model="execute.executionRole" placeholder="执行用户，默认为root"/>
        </a-form-model-item>
        <a-form-model-item label="执行路径" prop="executionPath">
          <a-input v-model="execute.executionPath" placeholder="执行路径，默认为~/"/>
        </a-form-model-item>
        <a-form-model-item label="执行节点" required>
          <a-select mode="multiple" v-model="chooseSsh">
            <a-select-option v-for="item in sshList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="指令内容" prop="command" class="config-editor">
          <code-editor v-model="execute.command"
                       :options="{ mode: execute.type === 0?'sh':'bat'}"></code-editor>
        </a-form-model-item>
        <a-form-model-item label="超时时间" prop="timeout">
          <a-input v-model="execute.timeout" placeholder="超时时间,单位：秒，默认60"/>
        </a-form-model-item>
        <a-form-model-item label="命令参数" v-if="commandParams&&commandParams.length>0">
          <div v-for="item in commandParams" :key="item.key">
            <a-input addon-before="参数名" v-model="item.name" placeholder="参数名"/>
            <a-input addon-before="参数值" v-model="item.val" placeholder="参数值"/>
          </div>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import {deleteCommand, editCommand, executeBatch, getCommandList} from "@/api/command";
import {
  PAGE_DEFAULT_LIMIT,
  PAGE_DEFAULT_LIST_QUERY,
  PAGE_DEFAULT_SHOW_TOTAL,
  PAGE_DEFAULT_SIZW_OPTIONS
} from "@/utils/const";
import {parseTime} from "@/utils/time";
import {getSshListAll} from "@/api/ssh";
import codeEditor from "@/components/codeEditor";

export default {
  components: {codeEditor},
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      commandList: [],
      selectedRowKeys: [],
      editCommandVisible: false,
      executeCommandVisible: false,
      commandParams: [],
      sshList: [],
      chooseSsh: [],
      execute: {},
      command: {
        type: 0,
        openParam: false,
        params: []
      },
      rules: {
        name: [{required: true, message: "Please input name", trigger: "blur"}],
        command: [{required: true, message: "Please input command", trigger: "blur"}],
      },
      columns: [
        {title: "命令名称", dataIndex: "name", ellipsis: true, scopedSlots: {customRender: "name"}},
        {title: "命令描述", dataIndex: "desc", ellipsis: true, scopedSlots: {customRender: "desc"}},
        {title: "超时时间(秒)", dataIndex: "timeout", ellipsis: true, scopedSlots: {customRender: "timeout"}, width: 120},
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
          scopedSlots: {customRender: "modifyUser"},
        },
        {title: "操作", dataIndex: "operation", scopedSlots: {customRender: "operation"}, width: 300},
      ],
    }
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
    this.getAllSSHList();
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
          this.command.params = JSON.stringify(this.commandParams);
        } else {
          this.command.params = "";
        }
        editCommand(this.command).then((res) => {
          this.formLoading = false;
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            this.editCommandVisible = false;
            this.command = {
              type: 0
            };
            this.getCommandData();
          }
        })
      })
    },
    // 获取命令数据
    getCommandData() {
      getCommandList(this.listQuery).then((res) => {
        if (200 === res.code) {
          this.commandList = res.data.result;
        }
      })
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
    //选中项目
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys;
    },
    // 创建命令弹窗
    createCommand() {
      this.editCommandVisible = true;
      // @author jzy 08-04
      this.$refs["editCommandForm"] && this.$refs["editCommandForm"].resetFields();
    },
    // 修改
    handleEdit(row) {
      this.createCommand();
      this.$set(row, 'openParam', false);
      this.commandParams = [];
      if (row.params) {
        this.commandParams = JSON.parse(row.params);
        if (this.commandParams.length > 0) {
          row.openParam = true;
        }
      }
      this.command = row;
    },
    // 执行命令
    handleExecute(row) {
      if (typeof row.params === "string" && row.params) {
        this.commandParams = JSON.parse(row.params);
      } else {
        this.commandParams = [];
      }
      this.execute = row;
      this.executeCommandVisible = true;
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
      })
    },
    // 添加命令参数
    handleAddParam() {
      this.commandParams.push({
        key: new Date().getTime(),
        name: null,
        val: null,
        desc: null
      })
      this.$forceUpdate();
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
          message: '请选择执行节点',
        });
        return false;
      }
      if (this.commandParams.length > 0) {
        for (let key in this.commandParams) {
          let item = this.commandParams[key];
          if (!item.val) {
            this.$notification.error({
              message: '请设置执行参数',
            });
            return false;
          }
        }
        this.execute.params = JSON.stringify(this.commandParams);
      }
      this.execute.nodes = JSON.stringify(this.chooseSsh)
      executeBatch(this.execute).then((res) => {
        console.log(res)
      });
    }
  }
}
</script>
<style scoped>
.filter {
  margin-bottom: 10px;
}

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
