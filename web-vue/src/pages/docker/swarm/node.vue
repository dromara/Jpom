<template>
  <div>
    <a-table :data-source="list" size="middle" :columns="columns" bordered :pagination="false" :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['nodeId']" @pressEnter="loadData" placeholder="id" class="search-input-item" />
          <a-input v-model="listQuery['nodeName']" @pressEnter="loadData" placeholder="名称" class="search-input-item" />
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            v-model="listQuery['nodeRole']"
            allowClear
            placeholder="角色"
            class="search-input-item"
          >
            <a-select-option key="worker">工作节点</a-select-option>
            <a-select-option key="manager">管理节点</a-select-option>
          </a-select>

          <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <!-- <a-popover :title="`状态信息：${TASK_STATE[text]}`" slot="desiredState" slot-scope="text, item" placement="topLeft"> -->
      <a-popover slot="hostname" slot-scope="hostname, item" placement="topLeft" :title="`主机名：${hostname}`">
        <template slot="content">
          <p>
            节点Id: <a-tag>{{ item.id }}</a-tag>
          </p>
          <template v-if="item.description && item.description.resources">
            <p>
              nanoCPUs: <a-tag>{{ item.description.resources.nanoCPUs }}</a-tag>
            </p>
            <p>
              memoryBytes: <a-tag>{{ item.description.resources.memoryBytes }}</a-tag>
            </p>
          </template>
          <template v-if="item.description && item.description.engine">
            <p>
              版本: <a-tag>{{ item.description.engine.engineVersion }}</a-tag>
            </p>
          </template>
        </template>

        <span>{{ hostname }}</span>
      </a-popover>

      <a-tooltip slot="status" slot-scope="text, item" placement="topLeft" :title="`节点状态：${text} 节点可用性：${item.spec ? item.spec.availability || '' : ''}`">
        <a-tag :color="(item.spec && item.spec.availability) === 'ACTIVE' && item.status && item.status.state === 'READY' ? 'green' : 'red'">
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

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <template v-if="record.managerStatus && record.managerStatus.leader">
            <a-button size="small" type="primary" @click="handleEdit(record)">修改</a-button>
            <a-tooltip title="主节点不能直接剔除">
              <a-button size="small" type="danger" :disabled="true">剔除</a-button>
            </a-tooltip>
          </template>
          <template v-else>
            <a-button size="small" type="primary" @click="handleEdit(record)">修改</a-button>
            <a-button size="small" type="danger" @click="handleLeava(record)">剔除</a-button>
          </template>

          <!-- <template v-else>
            <a-button size="small" type="danger" v-if="!item.managerStatus.leader" @click="handleLeava(record)">剔除</a-button>
          </template> -->
          <!-- <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()"> 更多 <a-icon type="down" /> </a>
            <a-menu slot="overlay">
              <a-menu-item> </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleUnbind(record)">解绑</a-button>
              </a-menu-item>
            </a-menu></a-dropdown
          > -->
        </a-space>
      </template>
    </a-table>
    <!-- 编辑节点 -->
    <a-modal v-model="editVisible" title="编辑节点" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="角色" prop="role">
          <a-radio-group name="role" v-model="temp.role" :disabled="temp.leader">
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
  </div>
</template>

<script>
import {dockerSwarmNodeLeave, dockerSwarmNodeList, dockerSwarmNodeUpdate} from "@/api/docker-swarm";

export default {
  components: {},
  props: {
    id: {
      type: String,
    },
    visible: {
      type: Boolean,
      default: false,
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
      rules: {
        role: [{ required: true, message: "请选择节点角色", trigger: "blur" }],
        availability: [{ required: true, message: "请选择节点状态", trigger: "blur" }],
      },
      autoUpdateTime: null,
      columns: [
        { title: "序号", width: 80, ellipsis: true, align: "center", customRender: (text, record, index) => `${index + 1}` },
        // { title: "节点Id", dataIndex: "id", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "主机名", dataIndex: "description.hostname", ellipsis: true, scopedSlots: { customRender: "hostname" } },
        { title: "节点地址", width: 150, dataIndex: "status.address", ellipsis: true, scopedSlots: { customRender: "address" } },
        { title: "状态", width: 140, dataIndex: "status.state", ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "角色", width: 110, dataIndex: "spec.role", ellipsis: true, scopedSlots: { customRender: "role" } },

        { title: "系统类型", width: 140, align: "center", dataIndex: "description.platform.os", ellipsis: true, scopedSlots: { customRender: "os" } },
        // {
        //   title: "资源",
        //   dataIndex: "description.resources",
        //   ellipsis: true,
        //   scopedSlots: { customRender: "resources" },
        //   width: 170,
        // },
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
  beforeDestroy() {
    this.autoUpdateTime && clearTimeout(this.autoUpdateTime);
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      if (!this.visible) {
        return;
      }
      this.loading = true;

      this.listQuery.id = this.id;
      dockerSwarmNodeList(this.listQuery).then((res) => {
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
    handleEdit(record) {
      this.editVisible = true;
      this.temp = {
        nodeId: record.id,
        role: record.spec.role,
        availability: record.spec.availability,
        leader: record.managerStatus && record.managerStatus.leader,
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
