<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="envVarList"
      size="middle"
      :loading="envVarLoading"
      :columns="envVarColumns"
      :pagination="envVarPagination"
      @change="changeListeEnvVar"
      bordered
      :rowKey="(record, index) => index"
    >
      <template #title>
        <a-space>
          <a-input v-model="envVarListQuery['%name%']" placeholder="名称" @pressEnter="loadDataEnvVar" allowClear class="search-input-item" />
          <a-input v-model="envVarListQuery['%value%']" placeholder="值" @pressEnter="loadDataEnvVar" allowClear class="search-input-item" />
          <a-input v-model="envVarListQuery['%description%']" placeholder="描述" @pressEnter="loadDataEnvVar" allowClear class="search-input-item" />
          <a-button type="primary" @click="loadDataEnvVar">搜索</a-button>
          <a-button type="primary" @click="addEnvVar">新增</a-button>
          <a-tooltip>
            <template slot="title">
              <div>环境变量是指配置在系统中的一些固定参数值，用于脚本执行时候快速引用。</div>
              <div>环境变量还可以用于仓库账号密码、ssh密码引用</div>
              <div>注意：环境变量存在作用域：当前工作空间或者全局，不能跨工作空间引用</div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="value" slot-scope="text, item" placement="topLeft" :title="item.privacy === 1 ? '隐私字段' : text">
        <a-icon v-if="item.privacy === 1" type="eye-invisible" />
        <span v-else>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="description" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="workspaceId" slot-scope="text">
        <span>{{ text === "GLOBAL" ? "全局" : "当前工作空间" }}</span>
      </template>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEnvEdit(record)">编辑</a-button>
          <a-button size="small" type="primary" @click="handleTrigger(record)">触发器</a-button>
          <a-button size="small" type="danger" @click="handleEnvDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>

    <!-- 环境变量编辑区 -->
    <a-modal v-model="editEnvVisible" title="编辑环境变量" width="50vw" @ok="handleEnvEditOk" :maskClosable="false">
      <a-form-model ref="editEnvForm" :rules="rulesEnv" :model="envTemp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="envTemp.name" :maxLength="50" placeholder="变量名称" />
        </a-form-model-item>
        <a-form-model-item label="值" :prop="`${envTemp.privacy === 1 ? '' : 'value'}`">
          <a-input v-model="envTemp.value" type="textarea" :rows="5" placeholder="变量值" />
        </a-form-model-item>
        <a-form-model-item label="描述" prop="description">
          <a-input v-model="envTemp.description" :maxLength="200" type="textarea" :rows="5" placeholder="变量描述" />
        </a-form-model-item>
        <a-form-model-item prop="privacy">
          <template slot="label">
            隐私变量
            <a-tooltip v-show="!envTemp.id">
              <template slot="title"> 隐私变量是指一些密码字段或者关键密钥等重要信息，隐私字段只能修改不能查看（编辑弹窗中无法看到对应值）。 隐私字段一旦创建后将不能切换为非隐私字段 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-switch
            :checked="envTemp.privacy === 1"
            @change="
              (checked) => {
                envTemp = { ...envTemp, privacy: checked ? 1 : 0 };
              }
            "
            :disabled="envTemp.id !== undefined"
            checked-children="隐私"
            un-checked-children="非隐私"
          />
        </a-form-model-item>
        <a-form-model-item>
          <template slot="label">
            分发节点
            <a-tooltip v-show="!envTemp.id">
              <template slot="title"> 分发节点是指将变量同步到对应节点，在节点脚本中也可以使用当前变量</template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-select show-search option-filter-prop="children" placeholder="请选择分发到的节点" mode="multiple" v-model="envTemp.chooseNode">
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 触发器 -->
    <a-modal destroyOnClose v-model="triggerVisible" title="触发器" width="50%" :footer="null" :maskClosable="false">
      <a-form-model ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template slot="tabBarExtraContent">
            <a-tooltip title="重置触发器 token 信息,重置后之前的触发器 token 将失效">
              <a-button type="primary" size="small" @click="resetTrigger">重置</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" tab="获取">
            <a-space style="display: block" direction="vertical" align="baseline">
              <a-alert message="温馨提示" type="warning">
                <template slot="description">
                  <ul>
                    <li>接口响应 ContentType 均为：text/plain</li>
                    <li>操作成功接口 HTTP 状态码为 200</li>
                    <li>修改接口 HTTP 状态码为 200 并且响应内容为：success 才能确定操作成功反之均可能失败</li>
                    <li>PUT 方式请求接口参数传入到请求体 ContentType 请使用：text/plain</li>
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
                :message="`获取变量值地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.triggerUrlPost"
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
                :message="`修改变量值地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>POST</a-tag> <span>{{ temp.triggerUrlPost }} </span>
                  <a-icon type="copy" />
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
                :message="`修改变量值地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>PUT</a-tag> <span>{{ temp.triggerUrl }} </span>
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
import { deleteWorkspaceEnv, editWorkspaceEnv, getWorkspaceEnvList, getTriggerUrlWorkspaceEnv } from "@/api/workspace";
import { getNodeListByWorkspace } from "@/api/node";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from "@/utils/const";
import Vue from "vue";
export default {
  props: {
    workspaceId: {
      type: String,
      default: "",
    },
    global: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      envVarLoading: false,
      envVarList: [],
      nodeList: [],
      envVarListQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editEnvVisible: false,
      envTemp: {},
      temp: {},
      envVarColumns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "值", dataIndex: "value", ellipsis: true, scopedSlots: { customRender: "value" } },

        { title: "描述", dataIndex: "description", ellipsis: true, scopedSlots: { customRender: "description" } },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },
        { title: "作用域", dataIndex: "workspaceId", ellipsis: true, scopedSlots: { customRender: "workspaceId" }, width: "120px" },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          customRender: (text) => {
            return parseTime(text);
          },
          sorter: true,
          width: "180px",
        },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: "200px" },
      ],
      // 表单校验规则
      rulesEnv: {
        name: [{ required: true, message: "请输入变量名称", trigger: "blur" }],
        description: [{ required: true, message: "请输入变量描述", trigger: "blur" }],
        value: [{ required: true, message: "请输入变量值", trigger: "blur" }],
      },
      triggerVisible: false,
      tempVue: null,
    };
  },
  computed: {
    envVarPagination() {
      return COMPUTED_PAGINATION(this.envVarListQuery);
    },
  },
  mounted() {
    this.loadDataEnvVar();
  },
  methods: {
    loadDataEnvVar(pointerEvent) {
      this.envVarLoading = true;

      this.envVarListQuery.workspaceId = this.workspaceId + (this.global ? ",GLOBAL" : "");
      this.envVarListQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.envVarListQuery.page;
      getWorkspaceEnvList(this.envVarListQuery).then((res) => {
        if (res.code === 200) {
          this.envVarList = res.data.result;
          this.envVarListQuery.total = res.data.total;
        }
        this.envVarLoading = false;
      });
    },
    addEnvVar() {
      this.envTemp = {
        workspaceId: this.workspaceId,
      };

      this.editEnvVisible = true;
      this.$refs["editEnvForm"] && this.$refs["editEnvForm"].resetFields();
      this.getAllNodeList(this.envTemp.workspaceId);
    },
    handleEnvEdit(record) {
      this.envTemp = Object.assign({}, record);
      this.envTemp.workspaceId = this.workspaceId;
      this.envTemp = { ...this.envTemp, chooseNode: record.nodeIds ? record.nodeIds.split(",") : [] };
      this.editEnvVisible = true;
      this.getAllNodeList(this.envTemp.workspaceId);
    },
    handleEnvEditOk() {
      this.$refs["editEnvForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.envTemp.nodeIds = this.envTemp?.chooseNode?.join(",");
        editWorkspaceEnv(this.envTemp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editEnvForm"].resetFields();
            this.editEnvVisible = false;
            this.loadDataEnvVar();
          }
        });
      });
    },
    //
    handleEnvDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的删除当前变量吗？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteWorkspaceEnv({
            id: record.id,
            workspaceId: this.workspaceId,
          }).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadDataEnvVar();
            }
          });
        },
      });
    },
    // 获取所有节点
    getAllNodeList(workspaceId) {
      getNodeListByWorkspace({
        workspaceId: workspaceId,
      }).then((res) => {
        this.nodeList = res.data || [];
      });
    },
    changeListeEnvVar(pagination, filters, sorter) {
      this.envVarListQuery = CHANGE_PAGE(this.envVarListQuery, { pagination, sorter });

      this.loadDataEnvVar();
    },
    // 触发器
    handleTrigger(record) {
      this.temp = Object.assign({}, record);
      this.tempVue = Vue;
      getTriggerUrlWorkspaceEnv({
        id: record.id,
        workspaceId: record.workspaceId,
      }).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res);
          this.triggerVisible = true;
        }
      });
    },
    // 重置触发器
    resetTrigger() {
      getTriggerUrlWorkspaceEnv({
        id: this.temp.id,
        rest: "rest",
        workspaceId: this.temp.workspaceId,
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
      this.temp.triggerUrlPost = `${this.temp.triggerUrl}?value=xxxxx`;
      this.temp = { ...this.temp };
    },
  },
};
</script>
