<template>
  <div>
    <a-table :data-source="list" size="middle" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['name']" @pressEnter="loadData" placeholder="名称" class="search-input-item" />
          <a-input v-model="listQuery['containerId']" @pressEnter="loadData" placeholder="容器id" class="search-input-item" />
          <!-- <a-input v-model="listQuery['imageId']" @keyup.enter="loadData" placeholder="镜像id" class="search-input-item" /> -->
          <div>
            显示所有
            <a-switch checked-children="是" un-checked-children="否" v-model="listQuery['showAll']" />
          </div>
          <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
        </a-space>
      </template>

      <a-popover :title="`容器名称：${(text || []).join(',')}`" slot="names" slot-scope="text, record">
        <template slot="content">
          <p>容器Id: {{ record.id }}</p>
          <p>镜像：{{ record.image }}</p>
          <p>镜像Id: {{ record.imageId }}</p>
        </template>

        <span>{{ (text || []).join(",") }}</span>
      </a-popover>

      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span style="display: none"> {{ (array = text.split(":")) }}</span>
        <span>{{ array[array.length - 1].slice(0, 12) }}</span>
      </a-tooltip>

      <a-popover
        slot="ports"
        slot-scope="text, record"
        placement="topLeft"
        :title="`
         网络信息 ${(text || [])
           .map((item) => {
             return item.type + ' ' + (item.ip || '') + ':' + (item.publicPort || '') + ':' + item.privatePort;
           })
           .join('/')}
       `"
      >
        <template slot="content">
          <template v-if="record.networkSettings">
            <template v-if="record.networkSettings.networks">
              <template v-if="record.networkSettings.networks.bridge">
                桥接模式：
                <p v-if="record.networkSettings.networks.bridge.ipAddress">
                  IP: <a-tag>{{ record.networkSettings.networks.bridge.ipAddress }}</a-tag>
                </p>
                <p v-if="record.networkSettings.networks.bridge.macAddress">
                  MAC: <a-tag>{{ record.networkSettings.networks.bridge.macAddress }}</a-tag>
                </p>
                <p v-if="record.networkSettings.networks.bridge.gateway">
                  网关: <a-tag>{{ record.networkSettings.networks.bridge.gateway }}</a-tag>
                </p>
                <p v-if="record.networkSettings.networks.bridge.networkID">
                  networkID: <a-tag>{{ record.networkSettings.networks.bridge.networkID }}</a-tag>
                </p>
                <p v-if="record.networkSettings.networks.bridge.endpointId">
                  endpointId: <a-tag>{{ record.networkSettings.networks.bridge.endpointId }}</a-tag>
                </p>
              </template>
              <template v-if="record.networkSettings.networks.ingress">
                <p v-if="record.networkSettings.networks.ingress.ipAddress">
                  IP: <a-tag>{{ record.networkSettings.networks.ingress.ipAddress }}</a-tag>
                </p>
                <p v-if="record.networkSettings.networks.ingress.macAddress">
                  MAC: <a-tag>{{ record.networkSettings.networks.ingress.macAddress }}</a-tag>
                </p>
                <p v-if="record.networkSettings.networks.ingress.gateway">
                  网关: <a-tag>{{ record.networkSettings.networks.ingress.gateway }}</a-tag>
                </p>
                <p v-if="record.networkSettings.networks.ingress.networkID">
                  networkID: <a-tag>{{ record.networkSettings.networks.ingress.networkID }}</a-tag>
                </p>
                <p v-if="record.networkSettings.networks.ingress.endpointId">
                  endpointId: <a-tag>{{ record.networkSettings.networks.ingress.endpointId }}</a-tag>
                </p>
              </template>
            </template>
          </template>
        </template>
        <span>{{
          (text || [])
            .map((item) => {
              return item.type + " " + (item.publicPort || "") + ":" + item.privatePort;
            })
            .join("/")
        }}</span>
      </a-popover>

      <template slot="state" slot-scope="text, record">
        <a-tooltip :title="(record.status || '') + ' 点击查看日志'" @click="viewLog(record)">
          <a-switch :checked="text === 'running'" :disabled="true">
            <a-icon slot="checkedChildren" type="check-circle" />
            <a-icon slot="unCheckedChildren" type="warning" />
          </a-switch>
        </a-tooltip>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-tooltip title="容器是运行中可以进入终端">
            <a-button size="small" type="link" :disabled="record.state !== 'running'" @click="handleTerminal(record)"><a-icon type="code" /></a-button>
          </a-tooltip>
          <a-tooltip title="停止" v-if="record.state === 'running'">
            <a-button size="small" type="link" @click="doAction(record, 'stop')"><a-icon type="stop" /></a-button>
          </a-tooltip>
          <a-tooltip title="启动" v-else>
            <a-button size="small" type="link" @click="doAction(record, 'start')"> <a-icon type="play-circle" /></a-button>
          </a-tooltip>
          <a-tooltip title="重启">
            <a-button size="small" type="link" :disabled="record.state !== 'running'" @click="doAction(record, 'restart')"><a-icon type="reload" /></a-button>
          </a-tooltip>
          <a-tooltip title="删除">
            <a-button size="small" type="link" @click="doAction(record, 'remove')"><a-icon type="delete" /></a-button>
          </a-tooltip>
          <a-tooltip title="点击查看日志">
            <a-button size="small" type="link" @click="viewLog(record)"><a-icon type="message" /></a-button>
          </a-tooltip>
        </a-space>
      </template>
    </a-table>
    <!-- 日志 -->
    <a-modal :width="'80vw'" v-model="logVisible" title="执行日志" :footer="null" :maskClosable="false">
      <log-view v-if="logVisible" :id="this.id" :containerId="temp.id" />
    </a-modal>
    <!-- Terminal -->
    <a-modal v-model="terminalVisible" width="80vw" :title="`docker cli ${(temp.names || []).join(',')}`" :footer="null" :maskClosable="false">
      <terminal v-if="terminalVisible" :id="this.id" :containerId="temp.id" />
    </a-modal>
  </div>
</template>
<script>
import { parseTime } from "@/utils/time";
import { dockerContainerList, dockerContainerRemove, dockerContainerRestart, dockerContainerStart, dockerContainerStop } from "@/api/docker-api";
import LogView from "@/pages/docker/log-view";
import Terminal from "@/pages/docker/terminal";
export default {
  components: {
    LogView,
    Terminal,
  },
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
      list: [],
      loading: false,
      listQuery: {
        showAll: true,
      },
      terminalVisible: false,
      logVisible: false,
      temp: {},
      autoUpdateTime: null,
      columns: [
        { title: "序号", width: 80, ellipsis: true, align: "center", customRender: (text, record, index) => `${index + 1}` },
        { title: "名称", dataIndex: "names", ellipsis: true, scopedSlots: { customRender: "names" } },
        { title: "容器ID", dataIndex: "id", ellipsis: true, width: 150, scopedSlots: { customRender: "id" } },
        // { title: "镜像", dataIndex: "image", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        // { title: "镜像ID", dataIndex: "imageId", ellipsis: true, width: 150, scopedSlots: { customRender: "id" } },
        { title: "端口", dataIndex: "ports", ellipsis: true, width: 150, scopedSlots: { customRender: "ports" } },
        { title: "状态", dataIndex: "state", ellipsis: true, align: "center", width: 90, scopedSlots: { customRender: "state" } },
        {
          title: "创建时间",
          dataIndex: "created",

          ellipsis: true,
          sorter: (a, b) => new Number(a.created) - new Number(b.created),
          sortDirections: ["descend", "ascend"],
          defaultSortOrder: "descend",
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 200 },
      ],
      action: {
        remove: {
          msg: "您确定要删除当前容器吗？",
          api: dockerContainerRemove,
        },
        stop: {
          msg: "您确定要停止当前容器吗？",
          api: dockerContainerStop,
        },
        restart: {
          msg: "您确定要重启当前容器吗？",
          api: dockerContainerRestart,
        },
        start: {
          msg: "您确定要启动当前容器吗？",
          api: dockerContainerStart,
        },
      },
    };
  },
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
      //this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.id = this.id;
      dockerContainerList(this.listQuery).then((res) => {
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
    doAction(record, actionKey) {
      const action = this.action[actionKey];
      if (!action) {
        return;
      }
      this.$confirm({
        title: "系统提示",
        content: action.msg,
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: this.id,
            containerId: record.id,
          };
          action.api(params).then((res) => {
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
    viewLog(record) {
      this.logVisible = true;
      this.temp = record;
    },
    // 进入终端
    handleTerminal(record) {
      this.temp = Object.assign({}, record);
      this.terminalVisible = true;
    },
  },
};
</script>
