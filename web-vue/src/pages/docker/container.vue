<template>
  <div>
    <a-table :data-source="list" size="middle" :columns="columns" :pagination="false" bordered rowKey="id" @expand="expand">
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

      <!-- stats -->
      <a-table slot="expandedRowRender" :rowKey="(record, index) => index" slot-scope="parentRecord" :columns="statsColumns" :data-source="statsMap[parentRecord.id]" :pagination="false">
        <template slot="cpus" slot-scope="text, record">
          {{ (record.cpuStats && record.cpuStats.percpuUsage) || (record.cpuStats && record.cpuStats.onlineCpus) }}
        </template>
        <template slot="cpuRatio" slot-scope="text, record">
          {{
            (
              ((((record.cpuStats && record.cpuStats.cpuUsage && record.cpuStats.cpuUsage.totalUsage) || 0) -
                ((record.precpuStats && record.precpuStats.cpuUsage && record.precpuStats.cpuUsage.totalUsage) || 0)) /
                ((record.cpuStats && record.cpuStats.systemCpuUsage) || 0) -
                ((record.precpuStats && record.precpuStats.systemCpuUsage) || 0)) *
              100.0
            ).toFixed(4)
          }}
          %
        </template>
        <template slot="memory" slot-scope="text, record">
          <!-- record.state !== 'running' -->
          <!-- text === 'running' -->
          <template v-if="parentRecord.state === 'running'">
            <a-button type="link" icon="edit" @click="editContainer(parentRecord)">
              {{ renderSize(((record.memoryStats && record.memoryStats.usage) || 0) - ((record.memoryStats && record.memoryStats.stats && record.memoryStats.stats.cache) || 0)) }}
              /
              {{ renderSize((record.memoryStats && record.memoryStats.limit) || 0) }}
            </a-button>
          </template>
          <template v-else>
            {{ renderSize(((record.memoryStats && record.memoryStats.usage) || 0) - ((record.memoryStats && record.memoryStats.stats && record.memoryStats.stats.cache) || 0)) }}
            /
            {{ renderSize((record.memoryStats && record.memoryStats.limit) || 0) }}
          </template>
        </template>
        <template slot="memoryRatio" slot-scope="text, record">
          <!-- memoryRatio -->
          {{
            (
              ((((record.memoryStats && record.memoryStats.usage) || 0) - ((record.memoryStats && record.memoryStats.stats && record.memoryStats.stats.cache) || 0)) /
                (record.memoryStats && record.memoryStats.limit)) *
              100.0
            ).toFixed(4)
          }}
          %
        </template>
        <template slot="blockIo" slot-scope="text, record">
          <a-tooltip
            :title="`${
              (record.blkioStats && record.blkioStats.ioServiceBytesRecursive && record.blkioStats.ioServiceBytesRecursive[0] && record.blkioStats.ioServiceBytesRecursive[0].op) || 'blkioStats'
            }`"
          >
            {{ renderSize(record.blkioStats && record.blkioStats.ioServiceBytesRecursive && record.blkioStats.ioServiceBytesRecursive[0] && record.blkioStats.ioServiceBytesRecursive[0].value) || 0 }}
          </a-tooltip>
          /
          <a-tooltip
            :title="`${
              (record.blkioStats && record.blkioStats.ioServiceBytesRecursive && record.blkioStats.ioServiceBytesRecursive[1] && record.blkioStats.ioServiceBytesRecursive[1].op) || 'blkioStats'
            }`"
          >
            {{ renderSize(record.blkioStats && record.blkioStats.ioServiceBytesRecursive && record.blkioStats.ioServiceBytesRecursive[1] && record.blkioStats.ioServiceBytesRecursive[1].value) || 0 }}
          </a-tooltip>
        </template>
        <template slot="netIo" slot-scope="text, record">
          <!-- // rx_bytes 网卡接收流量 -->
          <!-- // tx_bytes 网卡输出流量 -->

          <div :key="index" v-for="(item, index) in Object.keys(record.networks || {})">
            <a-tooltip :title="`${item} 接收流量`">
              {{ renderSize(record.networks[item] && record.networks[item].rxBytes) || 0 }}
            </a-tooltip>
            /
            <a-tooltip :title="`${item} 输出流量`">
              {{ renderSize(record.networks[item] && record.networks[item].txBytes) || 0 }}
            </a-tooltip>
          </div>
        </template>
        <!-- // 进程或线程的数量 -->
        <template slot="pids" slot-scope="text, record"> {{ record.pidsStats && record.pidsStats.current }}</template>
      </a-table>
    </a-table>
    <!-- 日志 -->
    <a-modal :width="'80vw'" v-model="logVisible" title="执行日志" :footer="null" :maskClosable="false">
      <log-view v-if="logVisible" :id="this.id" :containerId="temp.id" />
    </a-modal>
    <!-- Terminal -->
    <a-modal
      v-model="terminalVisible"
      width="80vw"
      :bodyStyle="{
        padding: '0px 10px',
        paddingTop: '10px',
        marginRight: '10px',
        height: `70vh`,
      }"
      :title="`docker cli ${(temp.names || []).join(',')}`"
      :footer="null"
      :maskClosable="false"
    >
      <terminal v-if="terminalVisible" :id="this.id" :containerId="temp.id" />
    </a-modal>
    <!-- 编辑容器配置 -->
    <a-modal v-model="editVisible" title="配置容器" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :model="temp" :label-col="{ span: 7 }" :wrapper-col="{ span: 17 }">
        <a-form-model-item prop="blkioWeight">
          <template slot="label">
            Block IO 权重
            <a-tooltip>
              <template slot="title"> Block IO 权重（相对权重）。 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input-number style="width: 100%" v-model="temp.blkioWeight" :min="0" :max="1000" />
        </a-form-model-item>
        <a-form-model-item prop="cpuShares">
          <template slot="label">
            CPU 权重
            <a-tooltip>
              <template slot="title"> 一个整数值，表示此容器相对于其他容器的相对 CPU 权重。 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input-number style="width: 100%" v-model="temp.cpuShares" />
        </a-form-model-item>
        <a-form-model-item prop="cpusetCpus">
          <template slot="label">
            执行的 CPU
            <a-tooltip>
              <template slot="title"> 允许执行的 CPU（例如，0-3、0,1）。 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input style="width: 100%" v-model="temp.cpusetCpus" />
        </a-form-model-item>
        <a-form-model-item prop="cpusetMems">
          <template slot="label">
            CpusetMems
            <a-tooltip>
              <template slot="title"> 允许执行的内存节点 (MEM) (0-3, 0,1)。 仅在 NUMA 系统上有效。 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input style="width: 100%" v-model="temp.cpusetMems" />
        </a-form-model-item>
        <a-form-model-item prop="cpuPeriod">
          <template slot="label">
            CPU 周期
            <a-tooltip>
              <template slot="title"> CPU 周期的长度，以微秒为单位。 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input-number style="width: 100%" v-model="temp.cpuPeriod" />
        </a-form-model-item>
        <a-form-model-item prop="cpuQuota">
          <template slot="label">
            CPU 时间
            <a-tooltip>
              <template slot="title"> 容器在一个 CPU 周期内可以获得的 CPU 时间的微秒。 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input-number style="width: 100%" v-model="temp.cpuQuota" />
        </a-form-model-item>

        <a-form-model-item prop="memory">
          <template slot="label">
            内存
            <a-tooltip>
              <template slot="title"> 设置内存限制。 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input style="width: 100%" v-model="temp.memory" />
        </a-form-model-item>
        <a-form-model-item prop="memorySwap">
          <template slot="label">
            总内存
            <a-tooltip>
              <template slot="title"> 总内存（内存 + 交换）。 设置为 -1 以禁用交换。 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input style="width: 100%" v-model="temp.memorySwap" />
        </a-form-model-item>
        <a-form-model-item prop="memoryReservation">
          <template slot="label">
            软内存
            <a-tooltip>
              <template slot="title"> 软内存限制。 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input style="width: 100%" v-model="temp.memoryReservation" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import {parseTime, renderSize} from "@/utils/time";
import {
  dockerContainerList,
  dockerContainerRemove,
  dockerContainerRestart,
  dockerContainerStart,
  dockerContainerStats,
  dockerContainerStop,
  dockerInspectContainer,
  dockerUpdateContainer,
} from "@/api/docker-api";
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
      statsColumns: [
        { title: "CPUS", width: "80px", dataIndex: "cpus", ellipsis: true, scopedSlots: { customRender: "cpus" } },
        { title: "CPU %", width: 100, dataIndex: "cpuRatio", ellipsis: true, scopedSlots: { customRender: "cpuRatio" } },

        { title: "MEM USAGE / LIMIT", dataIndex: "memory", ellipsis: true, scopedSlots: { customRender: "memory" } },
        { title: "MEM %", width: 100, dataIndex: "memoryRatio", ellipsis: true, scopedSlots: { customRender: "memoryRatio" } },
        { title: "NET I/O", dataIndex: "netIo", ellipsis: true, scopedSlots: { customRender: "netIo" } },
        { title: "BLOCK I/O", dataIndex: "blockIo", ellipsis: true, scopedSlots: { customRender: "blockIo" } },
        { title: "PIDS", width: "80px", dataIndex: "pids", ellipsis: true, scopedSlots: { customRender: "pids" } },
      ],

      statsMap: {},
      expandedRowKeys: [],
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
      editVisible: false,
    };
  },
  beforeDestroy() {
    this.autoUpdateTime && clearTimeout(this.autoUpdateTime);
  },
  mounted() {
    this.loadData();
  },
  methods: {
    renderSize,
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
          this.pullStats();
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
              if (actionKey === "remove") {
                this.expandedRowKeys = this.expandedRowKeys.filter((item2) => item2 !== record.id);
              }
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
    // 展开
    expand(status, item) {
      if (status) {
        this.expandedRowKeys.push(item.id);
        this.pullStats();
      } else {
        this.expandedRowKeys = this.expandedRowKeys.filter((item2) => item2 !== item.id);
      }
    },
    //  获取数据
    pullStats() {
      if (!this.expandedRowKeys.length) {
        return;
      }
      dockerContainerStats({
        id: this.id,
        containerId: this.expandedRowKeys.join(","),
      }).then((res) => {
        if (res.code === 200) {
          Object.keys(res.data).forEach((item) => {
            this.statsMap = { ...this.statsMap, [item]: [res.data[item]] };
          });
        }
        // console.log(res);
      });
    },
    // 编辑容器
    editContainer(record) {
      dockerInspectContainer({
        id: this.id,
        containerId: record.id,
      }).then((res) => {
        if (res.code === 200) {
          this.editVisible = true;

          const hostConfig = res.data.hostConfig || {};
          const data = {
            containerId: record.id,
            cpusetCpus: hostConfig.cpusetCpus,
            cpusetMems: hostConfig.cpusetMems,
            cpuPeriod: hostConfig.cpuPeriod,
            cpuShares: hostConfig.cpuShares,
            cpuQuota: hostConfig.cpuQuota,
            blkioWeight: hostConfig.blkioWeight,
            memoryReservation: renderSize(hostConfig.memoryReservation, hostConfig.memoryReservation),
            // Deprecated: This field is deprecated as the kernel 5.4 deprecated kmem.limit_in_bytes.
            // kernelMemory: hostConfig.kernelMemory,
            memory: renderSize(hostConfig.memory, hostConfig.memory),
            memorySwap: renderSize(hostConfig.memorySwap, hostConfig.memorySwap),
          };

          this.temp = Object.assign({}, data);
        }
      });
    },
    handleEditOk() {
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const temp = Object.assign({}, this.temp, { id: this.id });
        dockerUpdateContainer(temp).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            this.editVisible = false;
          }
        });
      });
    },
  },
};
</script>
