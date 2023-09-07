<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      :columns="columns"
      size="middle"
      :pagination="pagination"
      bordered
      @change="
        (pagination, filters, sorter) => {
          this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
          this.loadData();
        }
      "
      :rowKey="(record, index) => index"
    >
      <template slot="title">
        <a-space direction="vertical">
          <div>
            <template v-for="(val, key) in groupMap">
              <span :key="key">{{ key }}：</span>
              <template v-for="(tag, index) in val">
                <a-tag :key="`${tag.id}_${key}`" :color="`${index === 0 ? 'blue' : 'orange'}`">
                  {{ tag.name }}
                </a-tag>
              </template>
            </template>
          </div>
          <div v-if="groupList.filter((item) => !groupMap[item]).length">
            未绑定集群的分组：
            <template v-for="(item, index) in groupList">
              <a-tag v-if="!groupMap[item]" :key="index">{{ item }}</a-tag>
            </template>
          </div>
          <a-space>
            <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="集群名称" allowClear class="search-input-item" />
            <a-input v-model="listQuery['%url%']" @pressEnter="loadData" placeholder="集群地址" allowClear class="search-input-item" />
            <a-input v-model="listQuery['%localHostName%']" @pressEnter="loadData" placeholder="主机名" allowClear class="search-input-item" />
            <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
              <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
            </a-tooltip>

            <a-tooltip>
              <template slot="title">
                <ul>
                  <li>集群不能手动创建，创建需要多个服务端使用通一个数据库，并且配置不同的集群 id 来自动创建集群信息</li>
                  <li>新集群需要手动配置集群管理资产分组、集群访问地址</li>
                  <li>新机器还需要绑定工作空间，因为我们建议将不同集群资源分配到不同的工作空间来管理</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </a-space>
        </a-space>
      </template>

      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="url" slot-scope="text, item" :title="`集群名称：${item.name || ''}/地址：${item.url || ''}/状态消息：${item.statusMsg || ''}`">
        <a-button v-if="item.url" type="link" @click="openUrl(item.url)" size="small">
          {{ text }}
        </a-button>
        <span v-else>{{ item.statusMsg }}</span>
        <!-- -->
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>

    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model="editVisible" title="编辑集群" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="工作空间名称" />
        </a-form-model-item>
        <a-form-model-item label="关联分组" prop="linkGroups">
          <template #help>
            关联分组主要用于资产监控来实现不同服务端执行不同分组下面的资产监控
            <div style="color: red">注意：同一个分组不建议被多个集群绑定</div>
          </template>
          <a-select show-search mode="multiple" option-filter-prop="children" v-model="temp.linkGroups" allowClear placeholder="关联分组">
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="集群地址" prop="url">
          <template #help> 集群地址主要用于切换工作空间自动跳转到对应的集群 </template>
          <a-input v-model="temp.url" placeholder="集群访问地址" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from "@/utils/const";
import { getClusterList, deleteCluster, listLinkGroups, editCluster } from "@/api/system/cluster";
export default {
  data() {
    return {
      loading: false,
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      columns: [
        { title: "安装ID", dataIndex: "id", ellipsis: true, width: "100px", scopedSlots: { customRender: "tooltip" } },
        { title: "集群ID", dataIndex: "clusterId", ellipsis: true, width: "100px", scopedSlots: { customRender: "tooltip" } },
        { title: "名称", dataIndex: "name", ellipsis: true, width: 200, scopedSlots: { customRender: "tooltip" } },
        { title: "集群地址", dataIndex: "url", ellipsis: true, width: 200, scopedSlots: { customRender: "url" } },
        { title: "集群主机名", dataIndex: "localHostName", ellipsis: true, width: "100px", scopedSlots: { customRender: "tooltip" } },
        { title: "版本号", dataIndex: "jpomVersion", ellipsis: true, width: "100px", scopedSlots: { customRender: "tooltip" } },
        {
          title: "最后心跳时间",
          dataIndex: "lastHeartbeat",
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },

        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          customRender: (text) => parseTime(text),
          sorter: true,
          width: "170px",
        },
        { title: "操作", dataIndex: "operation", fixed: "right", align: "center", scopedSlots: { customRender: "operation" }, width: "120px" },
      ],
      // 表单校验规则
      rules: {
        name: [{ required: true, message: "请输入集群名称", trigger: "blur" }],
        linkGroups: [{ required: true, message: "请输入选择关联分组", trigger: "blur" }],
        // url: [{ required: true, message: "请输入集群访问地址", trigger: "blur" }],
      },
      editVisible: false,
      temp: {},
      groupList: [],
      groupMap: {},
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  created() {
    this.loadData();
    this.loadGroupList();
  },
  methods: {
    parseTime,
    CHANGE_PAGE,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      getClusterList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除该集群信息么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteCluster(record.id).then((res) => {
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
    // 获取所有的分组
    loadGroupList() {
      listLinkGroups().then((res) => {
        if (res.data) {
          this.groupList = res.data.linkGroups || [];
          this.groupMap = res.data.groupMap || {};
        }
      });
    },
    // 编辑
    handleEdit(record) {
      this.loadGroupList();
      this.temp = Object.assign({}, record, {
        linkGroups: (record.linkGroup || "").split(",").filter((item) => item),
      });
      this.editVisible = true;
    },
    handleEditOk() {
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const newData = { ...this.temp };
        const linkGroups = newData.linkGroups;
        if (!linkGroups) {
          this.$notification.error({
            message: "请选择集群关联分组",
          });
          return false;
        }
        delete newData.linkGroups;
        newData.linkGroup = linkGroups.join(",");
        editCluster(newData).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editForm"].resetFields();
            this.editVisible = false;
            this.loadData();
          }
        });
      });
    },
    //
    openUrl(url) {
      window.open(url);
    },
  },
};
</script>
