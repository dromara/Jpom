<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :columns="columns" :pagination="pagination" @change="changePage" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="日志名称" class="search-input-item" />

          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button>
          <a-button type="primary" size="small" @click="handleLogRead(record)">查看</a-button>
          <a-button type="danger" size="small" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editVisible" width="60%" title="编辑日志阅读" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="日志名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="日志项目名称" />
        </a-form-model-item>
        <a-form-model-item label="分发节点" required>
          <a-row v-for="(item, index) in temp.projectList" :key="index">
            <a-col :span="11">
              <span>节点: </span>
              <a-select
                style="width: 80%"
                v-model="item.nodeId"
                placeholder="请选择节点"
                @change="
                  () => {
                    temp = {
                      ...temp,
                      projectList: temp.projectList.map((item, index1) => {
                        if (index1 === index && item.projectId) {
                          return Object.assign(item, { projectId: undefined });
                        }
                        return item;
                      }),
                    };
                  }
                "
              >
                <a-select-option v-for="nodeItem in nodeList" :key="nodeItem.id" :disabled="!nodeProjectList[nodeItem.id] || !nodeProjectList[nodeItem.id].projects || nodeItem.openStatus !== 1">
                  {{ nodeItem.name }}
                </a-select-option>
              </a-select>
            </a-col>
            <a-col :span="11">
              <span>项目: </span>
              <a-select
                :getPopupContainer="
                  (triggerNode) => {
                    return triggerNode.parentNode || document.body;
                  }
                "
                :disabled="!item.nodeId"
                style="width: 80%"
                v-model="item.projectId"
                :placeholder="`请选择项目`"
              >
                <!-- <a-select-option value=""> 请先选择节点</a-select-option> -->
                <template v-if="nodeProjectList[item.nodeId]">
                  <a-select-option
                    v-for="project in nodeProjectList[item.nodeId].projects"
                    :disabled="
                      temp.projectList.filter((item, nowIndex) => {
                        return item.nodeId === project.nodeId && item.projectId === project.projectId && nowIndex !== index;
                      }).length > 0
                    "
                    :key="project.projectId"
                  >
                    {{ project.name }}
                  </a-select-option>
                </template>
              </a-select>
            </a-col>
            <a-col :span="2">
              <a-button type="danger" @click="() => temp.projectList.splice(index, 1)" icon="delete"></a-button>
            </a-col>
          </a-row>

          <a-button type="primary" @click="() => temp.projectList.push({})">添加</a-button>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 实时阅读 -->
    <a-drawer
      placement="right"
      :width="`${this.getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
      :visible="logReadVisible"
      @close="
        () => {
          this.logReadVisible = false;
          this.loadData();
        }
      "
    >
      <template #title>
        搜索查看
        {{ temp.cacheData && temp.cacheData.logFile ? ":" + temp.cacheData.logFile : "" }}
      </template>
      <logReadView
        v-if="logReadVisible"
        :data="this.temp"
        @changeTitle="
          (logFile) => {
            const cacheData = { ...this.temp.cacheData, logFile: logFile };
            this.temp = { ...this.temp, cacheData: cacheData };
          }
        "
      ></logReadView>
    </a-drawer>
  </div>
</template>
<script>
import {deleteLogRead, editLogRead, getLogReadList} from "@/api/log-read";
import {itemGroupBy, parseTime} from "@/utils/time";
import {getNodeListAll, getProjectListAll} from "@/api/node";
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";

import {mapGetters} from "vuex";
import logReadView from "./logReadView";

export default {
  components: {
    logReadView,
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      nodeList: [],
      nodeName: {},
      nodeProjectList: [],
      logReadVisible: false,
      temp: {},
      editVisible: false,
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },

        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, align: "center", scopedSlots: { customRender: "modifyUser" }, width: 120 },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          customRender: (text) => {
            if (!text || text === "0") {
              return "";
            }
            return parseTime(text);
          },
          width: 180,
        },
        { title: "操作", dataIndex: "operation", ellipsis: true, scopedSlots: { customRender: "operation" }, width: 180, align: "center" },
      ],
      rules: {
        name: [{ required: true, message: "请填写日志项目名称", trigger: "blur" }],
      },
    };
  },
  computed: {
    ...mapGetters(["getCollapsed"]),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  watch: {},
  created() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      getLogReadList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 加载节点以及项目
    loadNodeList() {
      return new Promise((resolve) => {
        this.loadNodeList2().then(() => {
          this.getProjectListAll().then(() => {
            resolve();
          });
        });
      });
    },
    // 加载节点以及项目
    loadNodeList2() {
      return new Promise((resolve) => {
        getNodeListAll().then((res) => {
          if (res.code === 200) {
            this.nodeList = res.data;
            this.nodeName = res.data.groupBy((item) => item.id);
            resolve();
          }
        });
      });
    },
    // 加载用户列表
    getProjectListAll() {
      return new Promise((resolve) => {
        getProjectListAll().then((res) => {
          if (res.code === 200) {
            this.nodeProjectList = itemGroupBy(res.data, "nodeId", "id", "projects").groupBy((item) => item.id);
            resolve();
            // console.log(this.nodeList);
            // console.log(this.nodeProjectList);
          }
        });
      });
    },
    // 新增
    handleAdd() {
      this.temp = {
        projectList: [],
      };
      this.loadNodeList().then(() => {
        this.editVisible = true;
      });
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record, { projectList: JSON.parse(record.nodeProject) });

      this.loadNodeList().then(() => {
        this.editVisible = true;
      });
    },
    handleEditOk() {
      // 检验表单
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const temp = Object.assign({}, this.temp);
        temp.projectList = temp.projectList?.filter((item) => {
          return item.nodeId && item.projectId;
        });
        if (!temp.projectList || !temp.projectList.length) {
          this.$notification.warn({
            message: "至少选择一个节点和项目",
          });
          return false;
        }
        // console.log(temp);

        editLogRead(temp).then((res) => {
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
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除日志阅读么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteLogRead(record.id).then((res) => {
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
    // 打开阅读
    handleLogRead(record) {
      // console.log(record);
      this.temp = Object.assign({}, record, { projectList: JSON.parse(record.nodeProject), cacheData: JSON.parse(record.cacheData || "{}") });
      this.logReadVisible = true;
      //
    },
  },
};
</script>
<style scoped></style>
