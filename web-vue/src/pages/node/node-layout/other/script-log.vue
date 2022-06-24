<template>
  <div class="node-full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :columns="columns" @change="changePage" :pagination="pagination" bordered rowKey="id">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" placeholder="名称" allowClear class="search-input-item" />
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            v-model="listQuery.triggerExecType"
            allowClear
            placeholder="触发类型"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in triggerExecTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-range-picker
            v-model="listQuery['createTimeMillis']"
            allowClear
            inputReadOnly
            class="search-input-item"
            :show-time="{ format: 'HH:mm:ss' }"
            :placeholder="['执行时间开始', '执行时间结束']"
            format="YYYY-MM-DD HH:mm:ss"
            valueFormat="YYYY-MM-DD HH:mm:ss"
          />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-tooltip>
            <template slot="title">
              <div>脚本模版是存储在节点(插件端),执行也都将在节点里面执行,服务端会定时去拉取执行日志,拉取频率为 100 条/分钟</div>
              <div>
                <ul>
                  <li>数据可能出现一定时间延迟</li>
                </ul>
              </div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="scriptName" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="modifyUser" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="triggerExecTypeMap" slot-scope="text">
        <span>{{ triggerExecTypeMap[text] || "未知" }}</span>
      </template>
      <a-tooltip slot="createTimeMillis" slot-scope="text, record" :title="`${parseTime(record.createTimeMillis)}`">
        <span>{{ parseTime(record.createTimeMillis) }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="viewLog(record)">查看日志</a-button>

          <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 日志 -->
    <a-modal :width="'80vw'" v-model="logVisible" title="执行日志" :footer="null" :maskClosable="false">
      <script-log-view v-if="logVisible" :temp="temp" />
    </a-modal>
  </div>
</template>
<script>
import {getScriptLogList, scriptDel} from "@/api/node-other";
import {triggerExecTypeMap} from "@/api/node-script";
import ScriptLogView from "@/pages/node/node-layout/other/script-log-view";
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";
import {parseTime} from "@/utils/time";

export default {
  components: {
    ScriptLogView,
  },
  props: {
    nodeId: {
      type: String,
    },
    scriptId: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign(
        {
          scriptId: this.scriptId,
        },
        PAGE_DEFAULT_LIST_QUERY
      ),
      triggerExecTypeMap: triggerExecTypeMap,
      list: [],
      temp: {},
      logVisible: false,
      columns: [
        { title: "名称", dataIndex: "scriptName", ellipsis: true, scopedSlots: { customRender: "scriptName" } },
        { title: "执行时间", dataIndex: "createTimeMillis", ellipsis: true, scopedSlots: { customRender: "createTimeMillis" } },
        { title: "触发类型", dataIndex: "triggerExecType", width: 100, ellipsis: true, scopedSlots: { customRender: "triggerExecTypeMap" } },
        { title: "执行人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" } },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 140 },
      ],
    };
  },
  computed: {
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
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.nodeId = this.nodeId;
      this.loading = true;
      getScriptLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    parseTime(v) {
      return parseTime(v);
    },
    viewLog(record) {
      this.logVisible = true;
      this.temp = record;
    },
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除执行记录么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            nodeId: this.nodeId,
            id: record.scriptId,
            executeId: record.id,
          };
          // 删除
          scriptDel(params).then((res) => {
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
<style scoped>
.filter {
  margin-bottom: 10px;
}
</style>
