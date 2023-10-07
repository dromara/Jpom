<template>
  <div class="node-full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :columns="columns" @change="changePage" :pagination="pagination" bordered rowKey="id">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" :placeholder=$t('common.name') allowClear class="search-input-item" />
          <a-select show-search option-filter-prop="children" v-model="listQuery.triggerExecType" allowClear :placeholder=$t('common.triggerExecType') class="search-input-item">
            <a-select-option v-for="(val, key) in triggerExecTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-range-picker
            v-model="listQuery['createTimeMillis']"
            allowClear
            inputReadOnly
            class="search-input-item"
            :show-time="{ format: 'HH:mm:ss' }"
            :placeholder="[$t('node.node_layout.other.script_log.execStart'), $t('node.node_layout.other.script_log.execEnd')]"
            format="YYYY-MM-DD HH:mm:ss"
            valueFormat="YYYY-MM-DD HH:mm:ss"
          />
          <a-tooltip :title=$t('common.goBackP1')>
            <a-button type="primary" :loading="loading" @click="loadData">{{$t('common.search')}}</a-button>
          </a-tooltip>
          <a-tooltip>
            <template slot="title">
              <div>{{$t('node.node_layout.other.script_log.scriptContent')}}</div>
              <div>
                <ul>
                  <li>{{$t('node.node_layout.other.script_log.timeDelay')}}</li>
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
        <span>{{ triggerExecTypeMap[text] || $t('common.unknown') }}</span>
      </template>
      <template slot="global" slot-scope="text">
        <a-tag v-if="text === 'GLOBAL'">{{$t('common.global')}}</a-tag>
        <a-tag v-else>{{$t('common.workSpace')}}</a-tag>
      </template>
      <a-tooltip slot="createTimeMillis" slot-scope="text, record" :title="`${parseTime(record.createTimeMillis)}`">
        <span>{{ parseTime(record.createTimeMillis) }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="viewLog(record)">{{$t('common.look')+$t('common.log')}}</a-button>

          <a-button size="small" type="danger" @click="handleDelete(record)">{{$t('common.delete')}}</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 日志 -->
    <a-modal destroyOnClose :width="'80vw'" v-model="logVisible" :title=$t('common.execute')+$t('common.log') :footer="null" :maskClosable="false">
      <script-log-view v-if="logVisible" :temp="temp" />
    </a-modal>
  </div>
</template>
<script>
import { getScriptLogList, scriptDel, triggerExecTypeMap } from "@/api/node-other";
// import {triggerExecTypeMap} from "@/api/node-script";
import ScriptLogView from "@/pages/node/node-layout/other/script-log-view";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from "@/utils/const";

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
        { title: this.$t('common.name'), dataIndex: "scriptName", ellipsis: true, width: 100, scopedSlots: { customRender: "scriptName" } },
        { title: this.$t('common.execute')+this.$t('common.time'), dataIndex: "createTimeMillis", ellipsis: true, width: "160px", scopedSlots: { customRender: "createTimeMillis" } },
        { title: this.$t('common.triggerExecType'), dataIndex: "triggerExecType", width: 100, ellipsis: true, scopedSlots: { customRender: "triggerExecTypeMap" } },
        { title: this.$t('common.executeArea'), dataIndex: "workspaceId", ellipsis: true, scopedSlots: { customRender: "global" }, width: "90px" },
        { title: this.$t('common.operator'), dataIndex: "modifyUser", ellipsis: true, width: 100, scopedSlots: { customRender: "modifyUser" } },
        { title: this.$t('common.operation'), dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, fixed: "right", width: "140px" },
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
        title: this.$t('common.systemPrompt'),
        content: this.$t('node.node_layout.other.script_log.ifDeleteRec'),
        okText: this.$t('common.confirm'),
        cancelText: this.$t('common.cancel'),
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
<style scoped></style>
