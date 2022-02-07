<template>
  <a-table :data-source="list" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
    <template slot="title">
      <a-space>
        <!-- <a-input v-model="listQuery['name']" @keyup.enter="loadData" placeholder="名称" class="search-input-item" /> -->
        <div>
          显示所有
          <a-switch checked-children="是" un-checked-children="否" v-model="listQuery['showAll']" />
        </div>
        <div>
          悬空
          <a-switch checked-children="是" un-checked-children="否" v-model="listQuery['dangling']" />
        </div>

        <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
      </a-space>
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
      <span> {{ text.split(":")[1].slice(0, 12) }}</span>
    </a-tooltip>
    <template slot="operation" slot-scope="text, record">
      <a-space>
        <!-- <a-tooltip title="停止" v-if="record.state === 'running'">
          <a-button size="small" type="link" @click="doAction(record, 'stop')"><a-icon type="stop" /></a-button>
        </a-tooltip>
        <a-tooltip title="启动" v-else>
          <a-button size="small" type="link" @click="doAction(record, 'start')"> <a-icon type="play-circle" /></a-button>
        </a-tooltip>
        <a-tooltip title="重启">
          <a-button size="small" type="link" :disabled="record.state !== 'running'" @click="doAction(record, 'restart')"><a-icon type="reload" /></a-button>
        </a-tooltip> -->
        <a-tooltip title="删除">
          <a-button size="small" type="link" @click="doAction(record, 'remove')"><a-icon type="delete" /></a-button>
        </a-tooltip>
      </a-space>
    </template>
  </a-table>
</template>
<script>
import { parseTime, renderSize } from "@/utils/time";
import { dockerImagesList, dockerImageRemove } from "@/api/docker-api";
export default {
  props: {
    id: {
      type: String,
    },
  },
  data() {
    return {
      list: [],
      loading: false,
      listQuery: {
        showAll: false,
      },
      renderSize,
      columns: [
        { title: "名称", dataIndex: "repoTags", ellipsis: true, scopedSlots: { customRender: "repoTags" } },
        { title: "镜像ID", dataIndex: "id", ellipsis: true, width: 150, scopedSlots: { customRender: "id" } },
        { title: "占用空间", dataIndex: "size", ellipsis: true, width: 120, scopedSlots: { customRender: "size" } },
        {
          title: "创建时间",
          dataIndex: "created",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 80 },
      ],
      action: {
        remove: {
          msg: "您确定要删除当前镜像吗？",
          api: dockerImageRemove,
        },
      },
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      //this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.id = this.id;
      dockerImagesList(this.listQuery).then((res) => {
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
            id: this.id,
            imageId: record.id,
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
  },
};
</script>
