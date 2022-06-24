<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" :columns="columns" size="middle" :pagination="pagination" bordered @change="changePage" :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="工作空间名称" allowClear class="search-input-item" />

          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
          <a-tooltip>
            <template slot="title">
              <ul>
                <li>工作空间用于隔离数据,工作空间下面可以有不同数据,不同权限,不同菜单等来实现权限控制</li>
                <li>工作空间环境变量用于构建命令相关</li>
              </ul>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="description" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-button size="small" type="primary" @click="viewEnvVar(record)">变量</a-button>
          <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editVisible" title="编辑工作空间" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="工作空间名称" />
        </a-form-model-item>

        <a-form-model-item label="描述" prop="description">
          <a-input v-model="temp.description" maxLength="200" type="textarea" :rows="5" placeholder="工作空间描述" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 环境变量 -->
    <a-modal v-model="envVarListVisible" :title="`${temp.name} 工作空间环境变量`" width="80vw" :footer="null" :maskClosable="false">
      <div ref="filter" class="filter">
        <a-space>
          <a-input v-model="envVarListQuery['%name%']" placeholder="名称" @pressEnter="loadDataEnvVar" allowClear class="search-input-item" />
          <a-input v-model="envVarListQuery['%value%']" placeholder="值" @pressEnter="loadDataEnvVar" allowClear class="search-input-item" />
          <a-input v-model="envVarListQuery['%description%']" placeholder="描述" @pressEnter="loadDataEnvVar" allowClear class="search-input-item" />
          <a-button type="primary" @click="loadDataEnvVar">搜索</a-button>
          <a-button type="primary" @click="addEnvVar">新增</a-button>
        </a-space>
      </div>
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
        <template slot="operation" slot-scope="text, record">
          <a-space>
            <a-button size="small" type="primary" @click="handleEnvEdit(record)">编辑</a-button>
            <a-button size="small" type="danger" @click="handleEnvDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table>
    </a-modal>
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
          <a-input v-model="envTemp.description" maxLength="200" type="textarea" :rows="5" placeholder="变量描述" />
        </a-form-model-item>
        <a-form-model-item prop="privacy">
          <template slot="label">
            隐私变量
            <a-tooltip v-show="!envTemp.id">
              <template slot="title"> 隐私变量是指一些密码字段或者关键密钥等重要信息，隐私字段只能修改不能查看（编辑弹窗中无法看到对应值）。 隐私字段一旦创建后将不能切换为非隐私字段 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-switch v-model="envTemp.privacy" :disabled="envTemp.id" checked-children="隐私" un-checked-children="非隐私" />
        </a-form-model-item>
        <a-form-model-item>
          <template slot="label">
            分发节点
            <a-tooltip v-show="!envTemp.id">
              <template slot="title"> 分发节点是指将变量同步到对应节点，在节点脚本中也可以使用当前变量</template>
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
            v-model="envTemp.chooseNode"
          >
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import {deleteWorkspace, deleteWorkspaceEnv, editWorkSpace, editWorkspaceEnv, getWorkspaceEnvList, getWorkSpaceList} from "@/api/workspace";
import {parseTime} from "@/utils/time";
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";
import {getNodeListByWorkspace} from "@/api/node";

export default {
  data() {
    return {
      loading: false,
      envVarLoading: false,
      list: [],
      envVarList: [],
      nodeList: [],
      envVarListQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editVisible: false,
      envVarListVisible: false,
      editEnvVisible: false,
      temp: {},
      envTemp: {},
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "描述", dataIndex: "description", ellipsis: true, scopedSlots: { customRender: "description" } },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",

          customRender: (text) => {
            if (!text) {
              return "";
            }
            return parseTime(text);
          },
          sorter: true,
          width: 180,
        },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 180 },
      ],
      envVarColumns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "值", dataIndex: "value", ellipsis: true, scopedSlots: { customRender: "value" } },
        { title: "描述", dataIndex: "description", ellipsis: true, scopedSlots: { customRender: "description" } },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          customRender: (text) => {
            if (!text) {
              return "";
            }
            return parseTime(text);
          },
          sorter: true,
          width: 180,
        },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 120 },
      ],
      // 表单校验规则
      rules: {
        name: [{ required: true, message: "请输入工作空间名称", trigger: "blur" }],
        description: [{ required: true, message: "请输入工作空间描述", trigger: "blur" }],
      },
      // 表单校验规则
      rulesEnv: {
        name: [{ required: true, message: "请输入变量名称", trigger: "blur" }],
        description: [{ required: true, message: "请输入变量描述", trigger: "blur" }],
        value: [{ required: true, message: "请输入变量值", trigger: "blur" }],
      },
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
    envVarPagination() {
      return COMPUTED_PAGINATION(this.envVarListQuery);
    },
  },
  created() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      getWorkSpaceList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    loadDataEnvVar(pointerEvent) {
      this.envVarLoading = true;
      this.envVarListQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.envVarListQuery.page;
      getWorkspaceEnvList(this.envVarListQuery).then((res) => {
        if (res.code === 200) {
          this.envVarList = res.data.result;
          this.envVarListQuery.total = res.data.total;
        }
        this.envVarLoading = false;
      });
    },
    viewEnvVar(record) {
      this.temp = record;
      this.envTemp = {
        workspaceId: this.temp.id,
      };
      this.envVarListQuery.workspaceId = record.id;
      this.envVarListVisible = true;
      this.loadDataEnvVar();
    },
    addEnvVar() {
      this.envTemp = {
        workspaceId: this.temp.id,
      };

      this.editEnvVisible = true;
      this.$refs["editEnvForm"] && this.$refs["editEnvForm"].resetFields();
      this.getAllNodeList(this.envTemp.workspaceId);
    },
    handleAdd() {
      this.temp = {};
      this.editVisible = true;
      this.$refs["editForm"] && this.$refs["editForm"].resetFields();
    },
    handleEdit(record) {
      this.temp = record;
      this.editVisible = true;
    },
    handleEnvEdit(record) {
      this.envTemp = record;
      this.envTemp.workspaceId = this.temp.id;
      this.envTemp = { ...this.envTemp, chooseNode: record.nodeIds ? record.nodeIds.split(",") : [] };
      this.editEnvVisible = true;
      this.getAllNodeList(this.envTemp.workspaceId);
    },
    handleEditOk() {
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        editWorkSpace(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editForm"].resetFields();
            this.editVisible = false;
            this.loadData();
          }
        });
      });
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
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
    changeListeEnvVar(pagination, filters, sorter) {
      this.envVarListQuery = CHANGE_PAGE(this.envVarListQuery, { pagination, sorter });

      this.loadDataEnvVar();
    },
    //
    handleEnvDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的当前工作空间么,删除前需要将关联数据都删除后才能删除当前工作空间？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteWorkspaceEnv({
            id: record.id,
            workspaceId: this.temp.id,
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
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的当前工作空间么,删除前需要将关联数据都删除后才能删除当前工作空间？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteWorkspace(record.id).then((res) => {
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
    // 获取所有节点
    getAllNodeList(workspaceId) {
      getNodeListByWorkspace({
        workspaceId: workspaceId,
      }).then((res) => {
        this.nodeList = res.data || [];
      });
    },
  },
};
</script>
<style scoped></style>
