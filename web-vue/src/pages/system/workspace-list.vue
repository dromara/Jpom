<template>
  <div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :columns="columns" size="middle" :pagination="pagination" bordered @change="changePage" :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="工作空间名称" allowClear class="search-input-item" />
          <a-select show-search option-filter-prop="children" v-model="listQuery.group" allowClear placeholder="分组" class="search-input-item">
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-select show-search option-filter-prop="children" v-model="listQuery.clusterInfoId" allowClear placeholder="集群" class="search-input-item">
            <a-select-option v-for="item in clusterList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
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
      <a-tooltip
        slot="clusterInfoId"
        slot-scope="text"
        placement="topLeft"
        :title="
          (clusterList.find((item) => {
            return item.id === text;
          }) &&
            clusterList.find((item) => {
              return item.id === text;
            }).name) ||
          ''
        "
      >
        <span>{{
          clusterList.find((item) => {
            return item.id === text;
          }) &&
          clusterList.find((item) => {
            return item.id === text;
          }).name
        }}</span>
      </a-tooltip>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-button size="small" type="primary" @click="configMeun(record)">菜单</a-button>
          <a-button size="small" type="primary" @click="configWhiteDir(record)">分发配置</a-button>
          <a-button size="small" type="primary" @click="viewEnvVar(record)">变量</a-button>

          <a-tooltip v-if="record.id === 'DEFAULT'" title="不能删除默认工作空间">
            <a-button size="small" type="danger" :disabled="true">删除</a-button>
          </a-tooltip>
          <a-button v-else size="small" type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model="editVisible" title="编辑工作空间" @ok="handleEditOk" :maskClosable="false">
      <a-alert message="温馨提醒" type="info" show-icon>
        <template #description>
          <ul>
            <li>创建工作空间后还需要在对应工作空间中分别管理对应数据</li>
            <li>如果要将工作空间分配给其他用户还需要到权限组管理</li>
            <li>工作空间的菜单、环境变量、节点分发白名单需要逐一配置</li>
          </ul>
        </template>
      </a-alert>
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }" style="padding-top: 15px">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="工作空间名称" />
        </a-form-model-item>
        <a-form-model-item label="绑定集群" prop="clusterInfoId">
          <a-select show-search option-filter-prop="children" v-model="temp.clusterInfoId" allowClear placeholder="绑定集群">
            <a-select-option v-for="item in clusterList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="分组" prop="group">
          <custom-select v-model="temp.group" :data="groupList" suffixIcon="" inputPlaceholder="添加分组" selectPlaceholder="选择分组名"> </custom-select>
        </a-form-model-item>

        <a-form-model-item label="描述" prop="description">
          <a-input v-model="temp.description" :maxLength="200" type="textarea" :rows="5" placeholder="工作空间描述" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 环境变量 -->
    <a-modal destroyOnClose v-model="envVarListVisible" :title="`${temp.name} 工作空间环境变量`" width="80vw" :footer="null" :maskClosable="false">
      <workspaceEnv ref="workspaceEnv" :workspaceId="temp.id" />
    </a-modal>
    <!-- 工作空间菜单 -->
    <a-modal destroyOnClose v-model="configMenuVisible" :title="`${temp.name} 工作空间菜单`" @ok="onSubmitMenus" :maskClosable="false">
      <a-form-model ref="editWhiteForm" :model="menusConfigData">
        <a-row type="flex" justify="center">
          <a-alert :message="`菜单配置只对非超级管理员生效`" style="margin-top: 10px; margin-bottom: 20px" banner />
          <a-col :span="12">
            <a-card title="服务端菜单" :bordered="false">
              <a-tree show-icon v-if="menusConfigData.serverMenus" checkable :tree-data="menusConfigData.serverMenus" :replaceFields="replaceFields" v-model="menusConfigData.serverMenuKeys">
                <a-icon slot="switcherIcon" type="down" />

                <template slot="custom" slot-scope="{ dataRef }">
                  <a-icon :type="dataRef.icon_v3" />
                </template>
              </a-tree>
            </a-card>
          </a-col>
          <a-col :span="12">
            <a-card title="节点菜单" :bordered="false">
              <a-tree show-icon v-if="menusConfigData.nodeMenus" checkable :tree-data="menusConfigData.nodeMenus" :replaceFields="replaceFields" v-model="menusConfigData.nodeMenuKeys">
                <a-icon slot="switcherIcon" type="down" />

                <template slot="custom" slot-scope="{ dataRef }">
                  <a-icon :type="dataRef.icon_v3" />
                </template>
              </a-tree>
            </a-card>
          </a-col>
        </a-row>
      </a-form-model>
    </a-modal>
    <!-- 配置授权目录 -->
    <a-modal
      destroyOnClose
      v-model="configDir"
      :title="`配置授权目录`"
      :footer="null"
      :maskClosable="false"
      @cancel="
        () => {
          this.configDir = false;
        }
      "
    >
      <whiteList
        v-if="configDir"
        :workspaceId="temp.id"
        @cancel="
          () => {
            this.configDir = false;
          }
        "
      ></whiteList>
    </a-modal>
    <!-- 删除工作空间检查 -->
    <a-modal
      destroyOnClose
      v-model="preDeleteVisible"
      :title="`删除工作空间确认`"
      :maskClosable="false"
      @ok="handleDeleteOk"
      @cancel="
        () => {
          this.preDeleteVisible = false;
        }
      "
    >
      <a-alert message="操作提示" type="error">
        <template #description> 真的当前工作空间么,删除前需要将关联数据都删除后才能删除当前工作空间？</template>
      </a-alert>

      <a-tree :tree-data="treeData" default-expand-all :replaceFields="preDeleteReplaceFields" :showLine="true">
        <template slot="title" slot-scope="{ dataRef }">
          <a-icon type="check" v-if="dataRef.count === 0" style="color: green" />
          <a-icon type="close" v-else style="color: red" />
          {{ dataRef.name }}

          <template v-if="dataRef.count > 0">
            <a-tag color="pink"> 存在 {{ dataRef.count }} 条数据 </a-tag>

            <a-tag v-if="dataRef.workspaceBind === 2" color="cyan">自动删除</a-tag>
            <a-tag v-else-if="dataRef.workspaceBind === 3" color="blue">父级不存在自动删除</a-tag>
            <a-tag v-else color="purple">手动删除</a-tag>
          </template>
        </template>
      </a-tree>
    </a-modal>
  </div>
</template>
<script>
import { deleteWorkspace, preDeleteWorkspace, editWorkSpace, getWorkSpaceList, getMenusConfig, saveMenusConfig, getWorkSpaceGroupList } from "@/api/workspace";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from "@/utils/const";
import { listClusterAll } from "@/api/system/cluster";
import workspaceEnv from "./workspace-env.vue";
import CustomSelect from "@/components/customSelect";
import whiteList from "@/pages/dispatch/white-list.vue";
export default {
  components: {
    workspaceEnv,
    CustomSelect,
    whiteList,
  },
  data() {
    return {
      loading: false,
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editVisible: false,
      envVarListVisible: false,
      temp: {},
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, width: 200, scopedSlots: { customRender: "name" } },
        { title: "描述", dataIndex: "description", ellipsis: true, width: 200, scopedSlots: { customRender: "description" } },
        { title: "分组名", dataIndex: "group", ellipsis: true, width: "100px", scopedSlots: { customRender: "tooltip" } },
        { title: "集群", dataIndex: "clusterInfoId", ellipsis: true, width: "100px", scopedSlots: { customRender: "clusterInfoId" } },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          customRender: (text) => parseTime(text),
          sorter: true,
          width: "170px",
        },
        { title: "操作", dataIndex: "operation", fixed: "right", align: "center", scopedSlots: { customRender: "operation" }, width: "300px" },
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: "请输入工作空间名称", trigger: "blur" }],
        description: [{ required: true, message: "请输入工作空间描述", trigger: "blur" }],
        clusterInfoId: [{ required: true, message: "请输入选择绑定的集群", trigger: "blur" }],
      },
      configMenuVisible: false,
      replaceFields: { children: "childs", title: "title", key: "id" },
      menusConfigData: {},
      groupList: [],
      configDir: false,
      preDeleteVisible: false,
      preDeleteReplaceFields: {
        children: "children",
        title: "name",
        key: "id",
      },
      treeData: [],
      clusterList: [],
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  created() {
    this.loadData();
    this.loadGroupList();
    this.loadClusterList();
  },
  methods: {
    // 获取所有集群
    loadClusterList() {
      return new Promise((resolve) => {
        listClusterAll().then((res) => {
          if (res.data && res.code === 200) {
            this.clusterList = res.data || [];
            resolve();
          }
        });
      });
    },
    // 获取所有的分组
    loadGroupList() {
      getWorkSpaceGroupList().then((res) => {
        if (res.data) {
          this.groupList = res.data;
        }
      });
    },
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

    viewEnvVar(record) {
      this.temp = Object.assign({}, record);
      // this.envTemp = {
      //   workspaceId: this.temp.id,
      // };
      // this.envVarListQuery.workspaceId = record.id;
      this.envVarListVisible = true;
      this.$nextTick(() => {
        this.$refs.workspaceEnv.loadDataEnvVar();
      });
    },
    handleAdd() {
      this.loadGroupList();
      this.temp = {};
      this.$refs["editForm"] && this.$refs["editForm"].resetFields();
      this.loadClusterList().then(() => {
        if (this.clusterList.length === 1) {
          this.temp = { ...this.temp, clusterInfoId: this.clusterList[0].id };
        }
        this.editVisible = true;
      });
    },
    handleEdit(record) {
      this.loadGroupList();
      this.$refs["editForm"] && this.$refs["editForm"].resetFields();
      this.loadClusterList().then(() => {
        const defData = {};
        if (this.clusterList.length === 1) {
          defData.clusterInfoId = this.clusterList[0].id;
        }
        this.temp = Object.assign({}, record, defData);
        this.editVisible = true;
      });
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
            // this.$refs["editForm"].resetFields();
            this.editVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
    // 删除
    handleDelete(record) {
      this.temp = { ...record };

      preDeleteWorkspace(this.temp.id).then((res) => {
        this.treeData = res.data?.children || [];
        this.preDeleteVisible = true;
      });
    },
    handleDeleteOk() {
      // 删除
      deleteWorkspace(this.temp.id).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.preDeleteVisible = false;
          this.loadData();
        }
      });
    },
    configMeun(record) {
      this.temp = Object.assign({}, record);

      // 加载菜单配置信息
      // loadMenusConfig(id) {},
      getMenusConfig({
        workspaceId: record.id,
      }).then((res) => {
        if (res.code !== 200) {
          return;
        }
        this.menusConfigData = res.data;

        this.menusConfigData.serverMenus = this.menusConfigData?.serverMenus.map((item) => {
          item.scopedSlots = { icon: "custom" };
          item.childs?.map((item2) => {
            item2.id = item.id + ":" + item2.id;
            return item2;
          });
          return item;
        });
        this.menusConfigData.nodeMenus = this.menusConfigData?.nodeMenus.map((item) => {
          item.scopedSlots = { icon: "custom" };
          item.childs?.map((item2) => {
            item2.id = item.id + ":" + item2.id;
            return item2;
          });
          return item;
        });
        if (!this.menusConfigData?.serverMenuKeys) {
          //
          const serverMenuKeys = [];
          this.menusConfigData.serverMenus.forEach((item) => {
            serverMenuKeys.push(item.id);
            if (item.childs) {
              item.childs.forEach((item2) => {
                serverMenuKeys.push(item2.id);
              });
            }
          });
          this.menusConfigData = { ...this.menusConfigData, serverMenuKeys: serverMenuKeys };
        }

        if (!this.menusConfigData?.nodeMenuKeys) {
          //
          const nodeMenuKeys = [];
          this.menusConfigData.nodeMenus.forEach((item) => {
            nodeMenuKeys.push(item.id);
            if (item.childs) {
              item.childs.forEach((item2) => {
                nodeMenuKeys.push(item2.id);
              });
            }
          });
          this.menusConfigData = { ...this.menusConfigData, nodeMenuKeys: nodeMenuKeys };
        }
        this.configMenuVisible = true;
      });
    },
    onSubmitMenus() {
      saveMenusConfig({
        serverMenuKeys: this.menusConfigData.serverMenuKeys.join(","),
        nodeMenuKeys: this.menusConfigData.nodeMenuKeys.join(","),
        workspaceId: this.temp.id,
      }).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
          this.configMenuVisible = false;
        }
      });
    },
    // 配置节点白名单
    configWhiteDir(record) {
      this.temp = Object.assign({}, record);
      this.configDir = true;
    },
  },
};
</script>
<style scoped></style>
