<template>
  <div>
    <a-table :data-source="list" size="middle" :columns="columns" bordered :pagination="false" :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['nodeId']" placeholder="id" class="search-input-item" />
          <a-input v-model="listQuery['nodeName']" placeholder="名称" class="search-input-item" />
          <a-select show-search option-filter-prop="children" v-model="listQuery['nodeRole']" allowClear placeholder="角色" class="search-input-item">
            <a-select-option key="worker">工作节点</a-select-option>
            <a-select-option key="manager">管理节点</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt 键点击按钮快速回到第一页">
            <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <a-tooltip slot="status" slot-scope="text, item" placement="topLeft" :title="text">
        <a-tag :color="text === 'READY' ? 'green' : text === 'DOWN' ? 'red' : ''">
          {{ text }}-<template v-if="item.spec">{{ item.spec.role }}</template>
        </a-tag>
      </a-tooltip>
      <a-tooltip slot="os" slot-scope="text, item" placement="topLeft" :title="text">
        <span>
          <a-tag>{{ text }}-{{ item.description && item.description.platform && item.description.platform.architecture }}</a-tag>
        </span>
      </a-tooltip>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()"> 更多 <a-icon type="down" /> </a>
            <a-menu slot="overlay">
              <a-menu-item> </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleUnbind(record)">解绑</a-button>
              </a-menu-item>
            </a-menu></a-dropdown
          >
        </a-space>
      </template>
    </a-table>
  </div>
</template>

<script>
import { editDockerSwarm, dockerSwarmNodeList, unbindSwarm } from "@/api/docker-swarm";

export default {
  components: {},
  props: {
    id: {
      type: String,
    },
  },
  data() {
    return {
      loading: false,
      listQuery: {},
      list: [],
      temp: {},
      editVisible: false,
      initSwarmVisible: false,
      columns: [
        { title: "节点Id", dataIndex: "id", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "节点地址", dataIndex: "status.address", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "状态", width: 160, align: "center", dataIndex: "status.state", ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "主机名", dataIndex: "description.hostname", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "系统类型", width: 140, align: "center", dataIndex: "description.platform.os", ellipsis: true, scopedSlots: { customRender: "os" } },
        {
          title: "创建时间",
          dataIndex: "createdAt",

          ellipsis: true,
          scopedSlots: { customRender: "tooltip" },
          width: 170,
        },
        {
          title: "修改时间",
          dataIndex: "updatedAt",

          ellipsis: true,
          scopedSlots: { customRender: "tooltip" },
          width: 170,
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, align: "center", width: 140 },
      ],
    };
  },
  computed: {},
  mounted() {
    this.list = [];
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.id = this.id;
      dockerSwarmNodeList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
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
        content: "真的要解绑该集群么？解绑至少删除在本系统的关联数据,不会删除容器里面数据",
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
  },
};
</script>
