<template>
  <div>
    <a-table :data-source="list" size="middle" :columns="columns" bordered :pagination="false" :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['serviceId']" placeholder="id" class="search-input-item" />
          <a-input v-model="listQuery['serviceName']" placeholder="名称" class="search-input-item" />

          <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <a-tooltip slot="status" slot-scope="text, item" placement="topLeft" :title="`节点状态：${text} 节点可用性：${item.spec ? item.spec.availability || '' : ''}`">
        <a-tag :color="(item.spec && item.spec.availability) === 'ACTIVE' ? 'green' : 'red'">
          {{ text }}
          <template v-if="item.spec">{{ item.spec.availability }}</template>
        </a-tag>
      </a-tooltip>
      <!-- 角色显示 -->
      <a-tooltip
        slot="role"
        slot-scope="text, item"
        placement="topLeft"
        :title="`角色：${text} ${item.managerStatus && item.managerStatus.reachability === 'REACHABLE' ? '管理状态：' + item.managerStatus.reachability : ''}`"
      >
        <a-tag :color="`${item.managerStatus && item.managerStatus.reachability === 'REACHABLE' ? 'green' : ''}`">
          {{ text }}
        </a-tag>
      </a-tooltip>
      <a-tooltip slot="address" slot-scope="text, item" placement="topLeft" :title="text">
        <a-icon v-if="item.managerStatus && item.managerStatus.leader" type="cloud-server" />
        {{ text }}
      </a-tooltip>

      <a-tooltip slot="os" slot-scope="text, item" placement="topLeft" :title="text">
        <span>
          <a-tag>{{ text }}-{{ item.description && item.description.platform && item.description.platform.architecture }}</a-tag>
        </span>
      </a-tooltip>
      <a-tooltip slot="updatedAt" slot-scope="text, item" placement="topLeft" :title="`修改时间：${text} 创建时间：${item.createdAt}`">
        <span>
          <a-tag>{{ text }}</a-tag>
        </span>
      </a-tooltip>

      <a-tooltip slot="replicas" slot-scope="text, record" placement="topLeft" :title="text" @click="handleTask(record)">
        <a-tag>{{ text }}</a-tag>
      </a-tooltip>

      <!-- <template slot="operation" slot-scope="text, record">
        <a-space>
          <template v-if="record.managerStatus && record.managerStatus.leader"> - </template>
          <template v-else>
            <a-button size="small" type="primary" @click="handleEdit(record)">修改</a-button>
            <a-button size="small" type="danger" @click="handleLeava(record)">剔除</a-button>
          </template>
        </a-space>
      </template> -->
    </a-table>
    <!-- 编辑节点 -->
    <a-modal v-model="editVisible" title="编辑节点" @ok="handleEditOk" :maskClosable="false">
      <!-- <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="角色" prop="role">
          <a-radio-group name="role" v-model="temp.role">
            <a-radio value="WORKER"> 工作节点</a-radio>
            <a-radio value="MANAGER"> 管理节点 </a-radio>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="状态" prop="availability">
          <a-radio-group name="availability" v-model="temp.availability">
            <a-radio value="ACTIVE"> 活跃</a-radio>
            <a-radio value="PAUSE"> 暂停 </a-radio>
            <a-radio value="DRAIN"> 排空 </a-radio>
          </a-radio-group>
        </a-form-model-item>
      </a-form-model> -->
    </a-modal>
    <!-- 查看任务 -->
    <a-modal v-model="taskVisible" title="查看任务" width="80vw" :footer="null" :maskClosable="false">
      <swarm-task v-if="taskVisible" :id="this.id" :serviceId="this.temp.id" />
    </a-modal>
  </div>
</template>

<script>
import { dockerSwarmServicesList, dockerSwarmNodeLeave, dockerSwarmNodeUpdate } from "@/api/docker-swarm";
import SwarmTask from "./task";
export default {
  components: { SwarmTask },
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
      taskVisible: false,
      rules: {
        role: [{ required: true, message: "请选择节点角色", trigger: "blur" }],
        availability: [{ required: true, message: "请选择节点状态", trigger: "blur" }],
      },
      columns: [
        { title: "服务Id", dataIndex: "id", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "名称", dataIndex: "spec.name", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "模式", dataIndex: "spec.mode.mode", ellipsis: true, width: 120, scopedSlots: { customRender: "tooltip" } },
        { title: "副本数", dataIndex: "spec.mode.replicated.replicas", width: 90, ellipsis: true, scopedSlots: { customRender: "replicas" } },
        { title: "端点", dataIndex: "spec.endpointSpec.mode", ellipsis: true, width: 100, scopedSlots: { customRender: "tooltip" } },

        {
          title: "修改时间",
          dataIndex: "updatedAt",

          ellipsis: true,
          scopedSlots: { customRender: "updatedAt" },
          width: 170,
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, align: "center", width: 120 },
      ],
    };
  },
  computed: {},
  mounted() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;

      this.listQuery.id = this.id;
      dockerSwarmServicesList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      });
    },
    //  任务
    handleTask(record) {
      this.taskVisible = true;
      this.temp = record;
    },
    handleEdit(record) {
      this.editVisible = true;
      this.temp = {
        nodeId: record.id,
        role: record.spec.role,
        availability: record.spec.availability,
      };
    },
    handleEditOk() {
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.temp.id = this.id;
        dockerSwarmNodeUpdate(this.temp).then((res) => {
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
    handleLeava(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要在该集群剔除此节点么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            nodeId: record.id,
            id: this.id,
          };
          dockerSwarmNodeLeave(params).then((res) => {
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
