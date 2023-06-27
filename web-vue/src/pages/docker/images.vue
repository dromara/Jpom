<template>
  <div>
    <a-table size="middle" :data-source="list" :columns="columns" :pagination="false" bordered rowKey="id" :row-selection="rowSelection">
      <template slot="title">
        <a-space>
          <div>
            显示所有
            <a-switch checked-children="是" un-checked-children="否" v-model="listQuery['showAll']" />
          </div>
          <div>
            悬空
            <a-switch checked-children="是" un-checked-children="否" v-model="listQuery['dangling']" />
          </div>
          <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          <a-button type="danger" :disabled="!tableSelections || !tableSelections.length" @click="batchDelete">批量删除</a-button>
        </a-space>
        |

        <a-input-search v-model="pullImageName" @search="pullImage" style="width: 260px" placeholder="要拉取的镜像名称" class="search-input-item">
          <a-button slot="enterButton"> <a-icon type="cloud-download" /> </a-button>
        </a-input-search>
        <!-- <a-button type="primary" @click="pullImage">拉取</a-button> -->
      </template>

      <a-tooltip slot="repoTags" slot-scope="text" placement="topLeft" :title="(text || []).join(',')">
        <span>{{ (text || []).join(",") }}</span>
      </a-tooltip>
      <a-tooltip slot="size" slot-scope="text, record" placement="topLeft" :title="renderSize(text) + ' ' + renderSize(record.virtualSize)">
        <span>{{ renderSize(text) }}</span>
      </a-tooltip>

      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <a-tooltip slot="id" slot-scope="text" :title="text">
        <span> {{ text && text.split(":")[1].slice(0, 12) }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <!-- <a-tooltip title="停止" v-if="record.state === 'running'">
          <a-button size="small" type="link" @click="doAction(record, 'stop')"><a-icon type="stop" /></a-button>
        </a-tooltip>
        <a-tooltip title="启动" v-else>
          <a-button size="small" type="link" @click="doAction(record, 'start')"> <a-icon type="play-circle" /></a-button>
        </a-tooltip>
        -->
          <a-tooltip title="使用当前镜像创建一个容器">
            <a-button size="small" type="link" @click="createContainer(record)"><a-icon type="select" /></a-button>
          </a-tooltip>
          <a-tooltip title="更新镜像">
            <a-button size="small" type="link" :disabled="!record.repoTags" @click="tryPull(record)"><a-icon type="cloud-download" /></a-button>
          </a-tooltip>

          <a-tooltip title="删除镜像">
            <a-button size="small" type="link" @click="doAction(record, 'remove')"><a-icon type="delete" /></a-button>
          </a-tooltip>
        </a-space>
      </template>
    </a-table>

    <a-drawer
      destroyOnClose
      :visible="buildVisible"
      @close="
        () => {
          this.buildVisible = false;
        }
      "
      width="60vw"
      title="构建容器"
      :maskClosable="false"
    >
      <BuildContainer 
        :id="this.id" 
        :imageId="this.temp.id"
        :machineDockerId="this.machineDockerId" 
        :urlPrefix="this.urlPrefix"
        @cancelBtnClick="
          () => {
            this.buildVisible = false;
          }"
      />
    </a-drawer>
    <!-- 日志 -->
    <a-modal destroyOnClose :width="'80vw'" v-model="logVisible" title="pull日志" :footer="null" :maskClosable="false">
      <pull-image-Log v-if="logVisible" :id="temp.id" :machineDockerId="this.machineDockerId" :urlPrefix="this.urlPrefix" />
    </a-modal>
  </div>
</template>
<script>
import { parseTime, renderSize } from "@/utils/const";
import {dockerImageCreateContainer, dockerImagePullImage, dockerImageRemove, dockerImagesList, dockerImageBatchRemove} from "@/api/docker-api";
import PullImageLog from "@/pages/docker/pull-image-log";
import BuildContainer from "./buildContainer.vue";

export default {
  components: {
    PullImageLog,
    BuildContainer,
  },
  props: {
    id: {
      type: String,
      default: "",
    },
    urlPrefix: {
      type: String,
    },
    machineDockerId: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      list: [],
      loading: false,
      listQuery: {
        showAll: false,
      },
      logVisible: false,
      pullImageName: "",
      renderSize,
      temp: {},
      rules: {
        name: [
          { required: true, message: "容器名称必填", trigger: "blur" },
          { pattern: /[a-zA-Z0-9][a-zA-Z0-9_.-]$/, message: "容器名称数字字母,且长度大于1", trigger: "blur" },
        ],
      },
      columns: [
        { title: "序号", width: 80, ellipsis: true, align: "center", customRender: (text, record, index) => `${index + 1}` },
        { title: "名称", dataIndex: "repoTags", ellipsis: true, scopedSlots: { customRender: "repoTags" } },
        { title: "镜像ID", dataIndex: "id", ellipsis: true, width: 140, align: "center", scopedSlots: { customRender: "id" } },
        { title: "父级ID", dataIndex: "parentId", ellipsis: true, width: 140, align: "center", scopedSlots: { customRender: "id" } },
        { title: "占用空间", dataIndex: "size", ellipsis: true, width: 120, scopedSlots: { customRender: "size" } },
        {
          title: "创建时间",
          dataIndex: "created",
          sorter: (a, b) => new Number(a.created) - new Number(b.created),
          sortDirections: ["descend", "ascend"],
          defaultSortOrder: "descend",
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 180,
        },

        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 120 },
      ],
      action: {
        remove: {
          msg: "您确定要删除当前镜像吗？",
          api: dockerImageRemove,
        },
      },
      buildVisible: false,
      tableSelections: []
    };
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId;
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
    loadData() {
      this.loading = true;
      //this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.id = this.reqDataId;
      dockerImagesList(this.urlPrefix, this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
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
            id: this.reqDataId,
            imageId: record.id,
          };
          action.api(this.urlPrefix, params).then((res) => {
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
    tryPull(record) {
      const repoTags = record?.repoTags[0];
      if (!repoTags) {
        this.$notification.error({
          message: "镜像名称不正确 不能更新",
        });
        return;
      }
      this.pullImageName = repoTags;
      this.pullImage();
    },
    // 构建镜像
    createContainer(record) {
      this.temp = Object.assign({}, record);
      this.buildVisible = true;
    },
    // 创建容器
    handleBuildOk() {
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const temp = {
          id: this.reqDataId,
          autorun: this.temp.autorun,
          imageId: this.temp.imageId,
          name: this.temp.name,
          env: {},
          commands: [],
          networkMode: this.temp.networkMode,
          privileged: this.temp.privileged,
          restartPolicy: this.temp.restartPolicy,
          labels: this.temp.labels,
          runtime: this.temp.runtime,
          hostname: this.temp.hostname,
          storageOpt: {},
        };
        temp.volumes = (this.temp.volumes || [])
          .filter((item) => {
            return item.host;
          })
          .map((item) => {
            return item.host + ":" + item.container;
          })
          .join(",");
        // 处理端口
        temp.exposedPorts = (this.temp.exposedPorts || [])
          .filter((item) => {
            return item.publicPort && item.ip;
          })
          .map((item) => {
            return item.ip + ":" + item.publicPort + ":" + item.port;
          })
          .join(",");
        // 环境变量
        this.temp.env.forEach((item) => {
          if (item.key && item.key) {
            temp.env[item.key] = item.value;
          }
        });
        this.temp.storageOpt.forEach((item) => {
          if (item.key && item.key) {
            temp.storageOpt[item.key] = item.value;
          }
        });
        //
        temp.commands = (this.temp.commands || []).map((item) => {
          return item.value || "";
        });
        dockerImageCreateContainer(this.urlPrefix, temp).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            this.buildVisible = false;
          }
        });
      });
    },
    // 拉取镜像
    pullImage() {
      if (!this.pullImageName) {
        this.$notification.warn({
          message: "请填写要拉取的镜像名称",
        });
        return;
      }
      dockerImagePullImage(this.urlPrefix, {
        id: this.reqDataId,
        repository: this.pullImageName,
      }).then((res) => {
        if (res.code === 200) {
          this.logVisible = true;
          this.temp = {
            id: res.data,
          };
        }
      });
    },
    batchDelete() {
      let ids = this.tableSelections
      this.$confirm({
        title: "系统提示",
        content: "真的要批量删除选择的镜像吗？已经被容器使用的镜像无法删除！",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: this.reqDataId,
            imagesIds: ids.join(','),
          };
          dockerImageBatchRemove(this.urlPrefix, params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    }
  },
};
</script>
