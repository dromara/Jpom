<template>
  <div class="full-content">
    <template v-if="this.getUserInfo && this.getUserInfo.systemUser && !this.loading && this.listQuery.total <= 0">
      <a-result title="当前工作空间还没有 Docker" sub-title="请到【系统管理】-> 【资产管理】-> 【Docker管理】添加Docker，或者将已添加的Docker授权关联、分配到此工作空间">
        <template #extra>
          <router-link to="/system/assets/docker-list"> <a-button key="console" type="primary">现在就去</a-button></router-link>
        </template>
      </a-result>
    </template>
    <!-- 数据表格 -->
    <a-table v-else size="middle" :data-source="list" :columns="columns" @change="changePage" :pagination="pagination" bordered rowKey="id" :row-selection="rowSelection">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="名称" class="search-input-item" />

          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" :disabled="!tableSelections || !tableSelections.length" @click="syncToWorkspaceShow">工作空间同步</a-button>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <template slot="status" slot-scope="text, record">
        <template v-if="record.machineDocker">
          <a-tag color="green" v-if="record.machineDocker.status === 1">正常</a-tag>
          <a-tooltip v-else :title="record.machineDocker.failureMsg">
            <a-tag color="red">无法连接</a-tag>
          </a-tooltip>
        </template>

        <a-tooltip v-else title="集群关联的 docker 信息丢失,不能继续使用管理功能">
          <a-tag color="red">信息丢失</a-tag>
        </a-tooltip>
      </template>
      <template slot="tags" slot-scope="tags">
        <a-tag v-for="item in (tags || '').split(':').filter((item) => item)" :key="item"> {{ item }}</a-tag>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" :disabled="!record.machineDocker || record.machineDocker.status !== 1" @click="handleConsole(record)">控制台</a-button>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model="editVisible" title="编辑  Docker" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="容器名称" prop="name">
          <a-input v-model="temp.name" placeholder="容器名称" />
        </a-form-model-item>

        <a-form-model-item label="标签" prop="tagInput" help="标签用于容器构建选择容器功能（fromTag）">
          <template>
            <div>
              <a-tooltip :key="index" :title="tag" v-for="(tag, index) in temp.tagsArray">
                <a-tag
                  :key="tag"
                  :closable="true"
                  @close="
                    () => {
                      temp.tagsArray = temp.tagsArray.filter((removedTag) => tag !== removedTag);
                    }
                  "
                >
                  {{ `${tag}` }}
                </a-tag>
              </a-tooltip>
            </div>
          </template>
          <a-input
            v-if="temp.inputVisible"
            ref="tagInput"
            type="text"
            size="small"
            placeholder="请输入标签名 字母数字 长度 1-10"
            v-model="temp.tagInput"
            @blur="handleInputConfirm"
            @keyup.enter="handleInputConfirm"
          />
          <template v-else>
            <a-tag v-if="!temp.tagsArray || temp.tagsArray.length < 10" style="background: #fff; borderstyle: dashed" @click="showInput"> <a-icon type="plus" /> 添加 </a-tag>
          </template>
        </a-form-model-item>
      </a-form-model>
    </a-modal>

    <!-- 控制台 -->
    <a-drawer destroyOnClose :title="`${temp.name} 控制台`" placement="right" :width="`${this.getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`" :visible="consoleVisible" @close="onClose">
      <console v-if="consoleVisible" :visible="consoleVisible" :id="temp.id" urlPrefix="/docker"></console>
    </a-drawer>
    <!-- 同步到其他工作空间 -->
    <a-modal destroyOnClose v-model="syncToWorkspaceVisible" title="同步到其他工作空间" @ok="handleSyncToWorkspace" :maskClosable="false">
      <a-alert message="温馨提示" type="warning">
        <template slot="description">
          <ul>
            <li>同步机制采用容器 host 确定是同一个服务器（docker）</li>
            <li>当目标工作空间不存在对应的节点时候将自动创建一个新的docker（逻辑docker）</li>
            <li>当目标工作空间已经存在节点时候将自动同步 docker 仓库配置信息</li>
          </ul>
        </template>
      </a-alert>
      <a-form-model :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item> </a-form-model-item>
        <a-form-model-item label="选择工作空间" prop="workspaceId">
          <a-select show-search option-filter-prop="children" v-model="temp.workspaceId" placeholder="请选择工作空间">
            <a-select-option :disabled="getWorkspaceId === item.id" v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { deleteDcoker, dockerList, editDocker, syncToWorkspace } from "@/api/docker-api";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from "@/utils/const";

import Console from "./console";
import { mapGetters } from "vuex";
import { getWorkSpaceListAll } from "@/api/workspace";

export default {
  components: {
    Console,
  },
  props: {},
  data() {
    return {
      loading: true,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),

      list: [],

      temp: {},
      editVisible: false,
      templateVisible: false,
      consoleVisible: false,

      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, width: 100, scopedSlots: { customRender: "tooltip" } },
        { title: "host", dataIndex: "machineDocker.host", width: 120, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "状态", dataIndex: "machineDocker.status", ellipsis: true, align: "center", width: "100px", scopedSlots: { customRender: "status" } },
        { title: "docker版本", dataIndex: "machineDocker.dockerVersion", ellipsis: true, width: "120px", scopedSlots: { customRender: "tooltip" } },
        { title: "标签", dataIndex: "tags", width: 100, ellipsis: true, scopedSlots: { customRender: "tags" } },
        { title: "最后修改人", dataIndex: "modifyUser", width: "120px", ellipsis: true, scopedSlots: { customRender: "modifyUser" } },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          ellipsis: true,
          sorter: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, fixed: "right", align: "center", width: "190px" },
      ],
      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: "请填写容器名称", trigger: "blur" }],
        host: [{ required: true, message: "请填写容器地址", trigger: "blur" }],
        tagInput: [
          // { required: true, message: "Please input ID", trigger: "blur" },
          { pattern: /^\w{1,10}$/, message: "标签限制为字母数字且长度 1-10" },
        ],

        tag: [
          { required: true, message: "请填写关联容器标签", trigger: "blur" },
          { pattern: /^\w{1,10}$/, message: "标签限制为字母数字且长度 1-10" },
        ],
      },
      workspaceList: [],
      tableSelections: [],
      syncToWorkspaceVisible: false,
    };
  },
  computed: {
    ...mapGetters(["getCollapsed", "getWorkspaceId", "getUserInfo"]),
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
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;

      dockerList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
          //
          const dockerId = this.$route.query.dockerId;
          this.list.map((item) => {
            if (dockerId === item.id) {
              this.handleConsole(item);
            }
          });
        }
        this.loading = false;
      });
    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.editVisible = true;

      this.$refs["editForm"]?.resetFields();
    },
    // 控制台
    handleConsole(record) {
      this.temp = { ...record };
      this.consoleVisible = true;

      let dockerId = this.$route.query.dockerId;
      if (dockerId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, dockerId: record.id, type: "docker" },
        });
      }
    },
    // 关闭抽屉层
    onClose() {
      this.consoleVisible = false;
      const query = Object.assign({}, this.$route.query);
      delete query.dockerId;
      delete query.type;
      this.$router.replace({
        query: query,
      });
    },
    // 修改
    handleEdit(record) {
      this.temp = { ...record };
      this.editVisible = true;

      let tagsArray = (record.tags || "").split(":");
      // console.log(tagsArray);
      tagsArray = tagsArray.filter((item) => item.length);
      this.temp = { ...this.temp, tagsArray: tagsArray };
      //.tags = (this.temp.tagsArray || []).join(",");
      this.$refs["editForm"]?.resetFields();
    },

    // 提交  数据
    handleEditOk() {
      // 检验表单
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const temp = Object.assign({}, this.temp);

        temp.tags = (temp.tagsArray || []).join(",");
        delete temp.tagsArray;
        delete temp.inputVisible;
        delete temp.tagInput;

        editDocker(temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.editVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除该记录么？删除后构建关联的容器标签将无法使用",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: record.id,
          };
          deleteDcoker(params).then((res) => {
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

    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
    // handleClose(removedTag) {},
    showInput() {
      this.temp = { ...this.temp, inputVisible: true };
      this.$nextTick(function () {
        this.$refs.tagInput.focus();
      });
    },
    handleInputConfirm() {
      this.$refs["editForm"].validateField("tagInput", (errmsg) => {
        if (errmsg) {
          // console.log(err);
          this.$notification.warn({
            message: errmsg,
          });
          return false;
        }
        const inputValue = this.temp.tagInput;
        let tags = this.temp.tagsArray || [];
        if (inputValue && tags.indexOf(inputValue) === -1) {
          tags = [...tags, inputValue];
        }

        this.temp = { ...this.temp, tagsArray: tags, tagInput: "", inputVisible: false };
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
        toWorkspaceId: this.temp.workspaceId,
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
