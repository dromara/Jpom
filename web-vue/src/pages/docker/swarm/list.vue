<template>
  <div class="full-content">
    <a-table :data-source="list" :columns="columns" @change="changePage" :pagination="listQuery.total / listQuery.limit > 1 ? pagination : false" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="名称" class="search-input-item" />
          <a-input v-model="listQuery['%tag%']" @pressEnter="loadData" placeholder="标签" class="search-input-item" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="status" slot-scope="text, record">
        <a-tooltip :title="`${parseInt(record.status) === 1 ? '运行中' : record.failureMsg || ''}`">
          <a-switch size="small" :checked="parseInt(record.status) === 1" :disabled="true">
            <a-icon slot="checkedChildren" type="check-circle" />
            <a-icon slot="unCheckedChildren" type="warning" />
          </a-switch>
        </a-tooltip>
      </template>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" :disabled="parseInt(record.status) !== 1" type="primary" @click="handleConsole(record, 'server')">服务</a-button>
          <a-button size="small" :disabled="parseInt(record.status) !== 1" type="primary" @click="handleConsole(record, 'node')">节点</a-button>

          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()"> 更多 <a-icon type="down" /> </a>
            <a-menu slot="overlay">
              <a-menu-item> <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button> </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleUnbind(record)">解绑</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
    </a-table>
    <!-- 创建集群区 -->
    <a-modal v-model="editVisible" title="编辑 Docker 集群" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="集群名称" prop="name">
          <a-input v-model="temp.name" placeholder="容器名称" />
        </a-form-model-item>

        <a-form-model-item label="标签" prop="tag"><a-input v-model="temp.tag" placeholder="关联容器标签" /> </a-form-model-item>
      </a-form-model>
    </a-modal>

    <!-- 控制台 -->
    <a-drawer
      :title="`${temp.name} 控制台`"
      placement="right"
      :width="`${this.getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
      :visible="consoleVisible"
      @close="
        () => {
          this.consoleVisible = false;
        }
      "
    >
      <console v-if="consoleVisible" :id="temp.id" :visible="consoleVisible" :initMenu="temp.menuKey"></console>
    </a-drawer>
  </div>
</template>

<script>
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";
import {dockerSwarmList, editDockerSwarm, unbindSwarm} from "@/api/docker-swarm";
import {parseTime} from "@/utils/time";
import {mapGetters} from "vuex";
import Console from "./console";

export default {
  components: {
    Console,
  },
  props: {},
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      temp: {},
      editVisible: false,
      consoleVisible: false,
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "节点地址", dataIndex: "nodeAddr", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "集群ID", dataIndex: "swarmId", ellipsis: true, align: "center", scopedSlots: { customRender: "tooltip" } },
        { title: "容器标签", dataIndex: "tag", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "状态", dataIndex: "status", ellipsis: true, align: "center", width: 80, scopedSlots: { customRender: "status" } },
        { title: "最后修改人", dataIndex: "modifyUser", width: 120, ellipsis: true, scopedSlots: { customRender: "modifyUser" } },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        {
          title: "集群修改时间",
          dataIndex: "swarmUpdatedAt",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, align: "center", width: 180 },
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
    };
  },
  computed: {
    ...mapGetters(["getCollapsed"]),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
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

      dockerSwarmList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 修改
    handleEdit(record) {
      this.temp = record;
      this.editVisible = true;
      this.$refs["editForm"]?.resetFields();
    },
    // 服务
    handleConsole(record, type) {
      this.temp = record;
      this.temp = { ...this.temp, menuKey: type };
      this.consoleVisible = true;
    },

    // 提交  数据
    handleEditOk() {
      // 检验表单
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }

        editDockerSwarm(this.temp).then((res) => {
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
    // 解绑
    handleUnbind(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要解绑该集群么？解绑只删除在本系统的关联数据,不会删除容器里面数据",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: record.id,
          };
          unbindSwarm(params).then((res) => {
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
  },
};
</script>
