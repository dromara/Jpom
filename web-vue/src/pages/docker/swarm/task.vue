<template>
  <div>
    <a-table :data-source="list" size="middle" :columns="columns" bordered :pagination="false" :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['serviceId']" @pressEnter="loadData" v-if="!this.serviceId" placeholder="服务id" class="search-input-item" />
          <a-input v-model="listQuery['taskName']" @pressEnter="loadData" placeholder="任务名称" class="search-input-item" />
          <a-input v-model="listQuery['taskId']" @pressEnter="loadData" placeholder="任务id" class="search-input-item" />
          <a-input v-model="listQuery['taskNode']" @pressEnter="loadData" placeholder="节点id" class="search-input-item" />

          <a-tooltip :title="TASK_STATE[listQuery['taskState']]">
            <a-select
              :getPopupContainer="
                (triggerNode) => {
                  return triggerNode.parentNode || document.body;
                }
              "
              show-search
              option-filter-prop="children"
              v-model="listQuery['taskState']"
              allowClear
              placeholder="状态"
              class="search-input-item"
            >
              <a-select-option :key="key" v-for="(item, key) in TASK_STATE">{{ item }}- {{ key }}</a-select-option>
              <a-select-option value="">状态</a-select-option>
            </a-select>
          </a-tooltip>
          <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <a-tooltip slot="address" slot-scope="text, item" placement="topLeft" :title="text">
        <a-icon v-if="item.managerStatus && item.managerStatus.leader" type="cloud-server" />
        {{ text }}
      </a-tooltip>
      <a-popover :title="`状态信息：${TASK_STATE[text]}`" slot="desiredState" slot-scope="text, item" placement="topLeft">
        <template slot="content">
          <p>
            当前状态：<a-tag>{{ text }}-{{ TASK_STATE[text] }}</a-tag>
          </p>
          <p v-if="item.status && item.status.err">错误信息：{{ item.status.err }}</p>
          <p v-if="item.status && item.status.state">
            状态：<a-tag>{{ item.status.state }}</a-tag>
          </p>

          <p v-if="item.status && item.status.message">
            信息：<a-tag>{{ item.status.message }} </a-tag>
          </p>
          <p v-if="item.status && item.status.timestamp">
            更新时间：<a-tag>{{ parseTime(item.status.timestamp) }} </a-tag>
          </p>
        </template>

        <a-tag :color="`${item.status && item.status.err ? 'orange' : text === 'RUNNING' ? 'green' : ''}`">
          {{ text }}
        </a-tag>
      </a-popover>

      <a-tooltip slot="os" slot-scope="text, item" placement="topLeft" :title="text">
        <span>
          <a-tag>{{ text }}-{{ item.description && item.description.platform && item.description.platform.architecture }}</a-tag>
        </span>
      </a-tooltip>
      <a-tooltip slot="updatedAt" slot-scope="text, item" placement="topLeft" :title="`修改时间：${text} 创建时间：${item.createdAt}`">
        <span>
          {{ parseTime(text) }}
        </span>
      </a-tooltip>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <template>
            <a-button size="small" type="primary" @click="handleLog(record)">日志</a-button>
          </template>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑节点 -->
    <a-modal v-model="editVisible" title="编辑节点" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
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
      </a-form-model>
    </a-modal>
    <!-- 查看日志 -->
    <a-modal v-model="logVisible" title="查看日志" width="80vw" :footer="null" :maskClosable="false">
      <pull-log v-if="logVisible" :id="this.id" :dataId="this.temp.id" type="taks" />
    </a-modal>
  </div>
</template>

<script>
import {dockerSwarmNodeLeave, dockerSwarmNodeUpdate, dockerSwarmServicesTaskList, TASK_STATE} from "@/api/docker-swarm";
import {parseTime} from "@/utils/time";
import PullLog from "./pull-log";

export default {
  components: { PullLog },
  props: {
    id: {
      type: String,
    },
    serviceId: { type: String },
    taskState: {
      type: String,
    },
    visible: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      TASK_STATE,
      loading: false,
      listQuery: {},
      list: [],
      temp: {},
      editVisible: false,
      initSwarmVisible: false,
      autoUpdateTime: null,
      logVisible: false,
      rules: {
        role: [{ required: true, message: "请选择节点角色", trigger: "blur" }],
        availability: [{ required: true, message: "请选择节点状态", trigger: "blur" }],
      },
      columns: [
        { title: "序号", width: 80, ellipsis: true, align: "center", customRender: (text, record, index) => `${index + 1}` },
        { title: "任务Id", dataIndex: "id", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "节点Id", dataIndex: "nodeId", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "服务ID", dataIndex: "serviceId", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "镜像", dataIndex: "spec.containerSpec.image", ellipsis: true, width: 120, scopedSlots: { customRender: "tooltip" } },
        // { title: "副本数", dataIndex: "spec.mode.replicated.replicas", width: 90, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        // { title: "端点", dataIndex: "spec.endpointSpec.mode", ellipsis: true, width: 100, scopedSlots: { customRender: "tooltip" } },
        // { title: "节点地址", width: 150, dataIndex: "status.address", ellipsis: true, scopedSlots: { customRender: "address" } },
        { title: "状态", width: 140, dataIndex: "desiredState", ellipsis: true, scopedSlots: { customRender: "desiredState" } },
        { title: "slot", width: 50, dataIndex: "slot", ellipsis: true, scopedSlots: { customRender: "tooltip" } },

        // { title: "系统类型", width: 140, align: "center", dataIndex: "description.platform.os", ellipsis: true, scopedSlots: { customRender: "os" } },
        // {
        //   title: "创建时间",
        //   dataIndex: "createdAt",

        //   ellipsis: true,
        //   scopedSlots: { customRender: "tooltip" },
        //   width: 170,
        // },
        {
          title: "修改时间",
          dataIndex: "updatedAt",
          ellipsis: true,
          scopedSlots: { customRender: "updatedAt" },
          sorter: (a, b) => new Date(a.updatedAt).getTime() - new Date(b.updatedAt).getTime(),
          sortDirections: ["descend", "ascend"],
          defaultSortOrder: "descend",
          width: 180,
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, align: "center", width: 80 },
      ],
    };
  },
  computed: {},
  beforeDestroy() {
    this.autoUpdateTime && clearTimeout(this.autoUpdateTime);
  },
  mounted() {
    this.listQuery.taskState = this.taskState;
    this.loadData();
  },
  methods: {
    parseTime,
    // 加载数据
    loadData() {
      if (!this.visible) {
        return;
      }
      this.loading = true;
      if (this.serviceId) {
        this.listQuery.serviceId = this.serviceId;
      }
      this.listQuery.id = this.id;
      dockerSwarmServicesTaskList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
        clearTimeout(this.autoUpdateTime);
        this.autoUpdateTime = setTimeout(() => {
          this.loadData();
        }, 3000);
      });
    },
    // 日志
    handleLog(record) {
      this.logVisible = true;
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
