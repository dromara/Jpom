<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table size="middle" :data-source="list" :columns="columns" :pagination="pagination" @change="changePage" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" :placeholder=$t('common.monitorName') class="search-input-item" />
          <a-select v-model="listQuery.status" allowClear :placeholder=$t('common.openStatus') class="search-input-item">
            <a-select-option :value="1">{{$t('common.open')}}</a-select-option>
            <a-select-option :value="0">{{$t('common.close')}}</a-select-option>
          </a-select>
          <a-tooltip :title=$t('monitor.list.goBackP1')>
            <a-button type="primary" :loading="loading" @click="loadData">{{$t('common.search')}}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{$t('common.add')}}</a-button>
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-switch slot="status" size="small" slot-scope="text" :checked="text" :checked-children=$t('common.open') :un-checked-children=$t('common.close') />
      <!-- <a-switch slot="autoRestart" slot-scope="text" :checked="text" checked-children="是" un-checked-children="否" /> -->
      <!-- <a-switch slot="alarm" slot-scope="text" :checked="text" disabled checked-children="报警中" un-checked-children="未报警" /> -->
      <a-tooltip slot="parent" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">{{$t('common.edit')}}</a-button>
          <a-button size="small" type="danger" @click="handleDelete(record)">{{$t('common.delete')}}</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model="editOperateMonitorVisible" width="50vw" :title=$t('common.editMonitor') @ok="handleEditOperateMonitorOk" :maskClosable="false">
      <a-form-model ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-model-item :label=$t('common.monitorName') prop="name">
          <a-input v-model="temp.name" :maxLength="50" :placeholder=$t('common.monitorName') />
        </a-form-model-item>
        <a-form-model-item :label=$t('common.openStatus') prop="status">
          <a-switch v-model="temp.start" :checked-children=$t('common.on') :un-checked-children=$t('common.off') />
        </a-form-model-item>
        <a-form-model-item :label=$t('common.monitorUser') prop="monitorUser">
          <a-transfer
            :data-source="monitorUserList"
            :lazy="false"
            show-search
            :filter-option="filterOption"
            :target-keys="monitorUserKeys"
            :render="(item) => item.title"
            @change="handleMonitorUserChange"
          />
        </a-form-model-item>
        <a-form-model-item :label=$t('common.monitorOpt') prop="monitorOpt">
          <a-transfer
            :data-source="classFeature"
            :lazy="false"
            show-search
            :filter-option="filterOption"
            :target-keys="classFeatureKeys"
            :render="(item) => item.title"
            @change="handleClassFeatureChange"
          />
        </a-form-model-item>
        <a-form-model-item :label=$t('common.monitorOperation') prop="monitorOpt">
          <a-transfer
            :data-source="methodFeature"
            :lazy="false"
            show-search
            :filter-option="filterOption"
            :target-keys="methodFeatureKeys"
            :render="(item) => item.title"
            @change="handleMethodFeatureChange"
          />
        </a-form-model-item>
        <a-form-model-item prop="notifyUser" class="jpom-monitor-notify">
          <template slot="label">
            {{ $t('common.notifyUser') }}
            <a-tooltip v-show="!temp.id">
              <template slot="title"> {{$t('monitor.log.notifyUser')}} </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-transfer :data-source="userList" :lazy="false" show-search :filter-option="filterOption" :target-keys="notifyUserKeys" :render="(item) => item.title" @change="handleNotifyUserChange" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { deleteMonitorOperate, editMonitorOperate, getMonitorOperateLogList, getMonitorOperateTypeList } from "@/api/monitor";
import { getUserListAll } from "@/api/user/user";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from "@/utils/const";

export default {
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],

      classFeature: [],
      methodFeature: [],
      userList: [],
      monitorUserList: [],
      temp: {},
      notifyUserKeys: [],
      monitorUserKeys: [],
      classFeatureKeys: [],
      methodFeatureKeys: [],
      editOperateMonitorVisible: false,
      columns: [
        { title: this.$t('common.name'), dataIndex: "name", scopedSlots: { customRender: "name" } },
        { title: this.$t('common.openStatus'), dataIndex: "status", scopedSlots: { customRender: "status" } },
        { title: this.$t('common.modifyUser'), dataIndex: "modifyUser", scopedSlots: { customRender: "modifyUser" } },
        {
          title: this.$t('common.modifyTime'),
          dataIndex: "modifyTimeMillis",
          sorter: true,
          customRender: (text) => {
            if (!text || text === "0") {
              return "";
            }
            return parseTime(text);
          },
          width: 180,
        },
        { title: this.$t('common.operation'), dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 120 },
      ],
      rules: {
        name: [{ required: true, message: "Please input monitor name", trigger: "blur" }],
      },
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  watch: {},
  created() {
    this.loadData();
    this.loadOptTypeData();
  },
  methods: {
    // 页面引导
    introGuide() {
      this.$store.dispatch("tryOpenGuide", {
        key: "operate-log",
        options: {
          hidePrev: true,
          steps: [
            {
              title: this.$t('common.introGuide'),
              element: document.querySelector(".jpom-monitor-notify"),
              intro: this.$t('monitor.operate_log.introGuide'),
            },
          ],
        },
      });
    },

    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      getMonitorOperateLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 加载操作类型数据
    loadOptTypeData() {
      // this.optTypeList = [];
      getMonitorOperateTypeList().then((res) => {
        if (res.code === 200) {
          this.methodFeature = res.data.methodFeature.map((element) => {
            return { key: element.value, title: element.title, disabled: false };
          });
          this.classFeature = res.data.classFeature.map((element) => {
            return { key: element.value, title: element.title, disabled: false };
          });
        }
      });
    },
    // 加载用户列表
    loadUserList() {
      // this.userList = [];
      getUserListAll().then((res) => {
        if (res.code === 200) {
          // res.data.forEach((element) => {
          //   this.userList.push({ key: element.value, title: element.title, disabled: element.disabled || false });
          // });
          this.userList = res.data.map((element) => {
            let canUse = element.email || element.dingDing || element.workWx;
            return { key: element.id, title: element.name, disabled: !canUse };
          });
          this.monitorUserList = res.data.map((element) => {
            return { key: element.id, title: element.name };
          });
        }
      });
    },
    // 新增
    handleAdd() {
      this.temp = {
        start: false,
      };
      this.notifyUserKeys = [];
      this.classFeatureKeys = [];
      this.methodFeatureKeys = [];
      this.monitorUserKeys = [];
      this.loadUserList();
      this.editOperateMonitorVisible = true;
      this.$nextTick(() => {
        setTimeout(() => {
          this.introGuide();
        }, 500);
      });
    },
    // 修改
    handleEdit(record) {
      this.loadUserList();
      this.temp = Object.assign({}, record);
      this.temp = {
        ...this.temp,
        start: this.temp.status,
      };
      this.notifyUserKeys = JSON.parse(this.temp.notifyUser);
      this.classFeatureKeys = JSON.parse(this.temp.monitorFeature);
      this.methodFeatureKeys = JSON.parse(this.temp.monitorOpt);
      this.monitorUserKeys = JSON.parse(this.temp.monitorUser);
      this.editOperateMonitorVisible = true;
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.title.indexOf(inputValue) > -1;
    },
    // 穿梭框 change
    handleNotifyUserChange(targetKeys) {
      this.notifyUserKeys = targetKeys;
    },
    // 穿梭框 change
    handleMethodFeatureChange(targetKeys) {
      this.methodFeatureKeys = targetKeys;
    },
    handleClassFeatureChange(targetKeys) {
      this.classFeatureKeys = targetKeys;
    },

    // 穿梭框 change
    handleMonitorUserChange(targetKeys) {
      this.monitorUserKeys = targetKeys;
    },
    // 提交
    handleEditOperateMonitorOk() {
      // 检验表单
      this.$refs["editMonitorForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        if (this.monitorUserKeys.length === 0) {
          this.$notification.error({
            message: this.$t('monitor.operate_log.monitorUser'),
          });
          return false;
        }
        if (this.methodFeatureKeys.length === 0) {
          this.$notification.error({
            message: this.$t('monitor.operate_log.methodFeature'),
          });
          return false;
        }
        if (this.classFeatureKeys.length === 0) {
          this.$notification.error({
            message: this.$t('monitor.operate_log.classFeature'),
          });
          return false;
        }
        if (this.notifyUserKeys.length === 0) {
          this.$notification.error({
            message: this.$t('monitor.operate_log.notifyUser'),
          });
          return false;
        }
        // 设置参数
        this.temp.monitorUser = JSON.stringify(this.monitorUserKeys);
        this.temp.monitorOpt = JSON.stringify(this.methodFeatureKeys);
        this.temp.monitorFeature = JSON.stringify(this.classFeatureKeys);
        this.temp.notifyUser = JSON.stringify(this.notifyUserKeys);
        this.temp.start ? (this.temp.status = "on") : (this.temp.status = "no");
        editMonitorOperate(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editMonitorForm"].resetFields();
            this.editOperateMonitorVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: this.$t('common.systemPrompt'),
        content: this.$t('monitor.operate_log.deleteContents'),
        okText: this.$t('common.confirm'),
        cancelText: this.$t('common.cancel'),
        onOk: () => {
          // 删除
          deleteMonitorOperate(record.id).then((res) => {
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
