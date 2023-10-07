<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :columns="columns" :pagination="pagination" @change="changePage" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" :placeholder="$t('common.monitorName')" class="search-input-item" />
          <a-select v-model="listQuery.status" allowClear :placeholder="$t('common.openStatus')" class="search-input-item">
            <a-select-option :value="1">{{$t('common.open')}}</a-select-option>
            <a-select-option :value="0">{{$t('common.close')}}</a-select-option>
          </a-select>
          <a-select v-model="listQuery.autoRestart" allowClear :placeholder="$t('common.autoRestart')" class="search-input-item">
            <a-select-option :value="1">{{$t('common.yes')}}</a-select-option>
            <a-select-option :value="0">{{$t('common.no')}}</a-select-option>
          </a-select>
          <a-select v-model="listQuery.alarm" allowClear :placeholder="$t('common.alarmStatus')" class="search-input-item">
            <a-select-option :value="1">{{$t('common.alarm')}}</a-select-option>
            <a-select-option :value="0">{{$t('common.noAlarm')}}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('monitor.list.goBackP1')">
            <a-button type="primary" :loading="loading" @click="loadData">{{$t('common.search')}}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{$t('common.add')}}</a-button>
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-switch slot="status" size="small" slot-scope="text" :checked="text" disabled :checked-children="$t('common.open')" :un-checked-children="$t('common.close')" />
      <a-switch slot="autoRestart" size="small" slot-scope="text" :checked="text" disabled :checked-children="$t('common.yes')" :un-checked-children="$t('common.no')" />
      <a-switch slot="alarm" size="small" slot-scope="text" :checked="text" disabled :checked-children="$t('common.alarm')" :un-checked-children="$t('common.noAlarm')" />
      <a-tooltip slot="parent" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" size="small" @click="handleEdit(record)">{{$t('common.edit')}}</a-button>
          <a-button type="danger" size="small" @click="handleDelete(record)">{{$t('common.delete')}}</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model="editMonitorVisible" width="60%" :title="$t('common.editMonitor')" @ok="handleEditMonitorOk" :maskClosable="false">
      <a-form-model ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item :label="$t('common.monitorName')" prop="name">
          <a-input v-model="temp.name" :maxLength="50" :placeholder="$t('common.monitorName')" />
        </a-form-model-item>

        <a-form-model-item :label="$t('common.openStatus')" prop="status">
          <a-space size="large">
            <a-switch v-model="temp.status" :checked-children="$t('common.open')" :un-checked-children="$t('common.close')" />
            <div>
              {{ $t('common.autoRestart') }}
              <a-switch v-model="temp.autoRestart" :checked-children="$t('common.open')" :un-checked-children="$t('common.close')" />
            </div>
          </a-space>
        </a-form-model-item>

        <!-- <a-form-model-item label="自动重启" prop="autoRestart">

          </a-form-model-item> -->

        <!-- <a-form-model-item label="监控周期" prop="cycle">
          <a-radio-group v-model="temp.cycle" name="cycle">
            <a-radio :value="1">1 分钟</a-radio>
            <a-radio :value="5">5 分钟</a-radio>
            <a-radio :value="10">10 分钟</a-radio>
            <a-radio :value="30">30 分钟</a-radio>
          </a-radio-group>
        </a-form-model-item> -->

        <a-form-model-item :label="$t('common.execCron')" prop="execCron">
          <a-auto-complete v-model="temp.execCron" :placeholder="$t('monitor.list.autoRun')" option-label-prop="value">
            <template slot="dataSource">
              <a-select-opt-group v-for="group in cronDataSource" :key="group.title">
                <span slot="label">
                  {{ group.title }}
                </span>
                <a-select-option v-for="opt in group.children" :key="opt.title" :value="opt.value"> {{ opt.title }} {{ opt.value }} </a-select-option>
              </a-select-opt-group>
            </template>
          </a-auto-complete>
        </a-form-model-item>
        <a-form-model-item :label="$t('common.monitorProj')" prop="projects">
          <a-select option-label-prop="label" v-model="projectKeys" mode="multiple" :placeholder="$t('monitor.list.selectToMonitor')" show-search option-filter-prop="children">
            <a-select-opt-group :label="nodeMap[nodeItem.node].name" v-for="nodeItem in nodeProjectGroupList" :key="nodeItem.node">
              <a-select-option :label="`${project.name} - ${project.runMode}`" v-for="project in nodeItem.projects" :disabled="!noFileModes.includes(project.runMode)" :key="project.id">
                【{{ project.nodeName }}】{{ project.name }} - {{ project.runMode }}
              </a-select-option>
            </a-select-opt-group>
          </a-select>
        </a-form-model-item>
        <a-form-model-item prop="notifyUser" class="jpom-notify">
          <template slot="label">
            {{$t('common.contact')}}
            <a-tooltip v-show="!temp.id">
              <template slot="title"> {{$t('monitor.list.remindSetEmail')}} </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-transfer
            :data-source="userList"
            :lazy="false"
            :titles="[$t('common.toChoose'), $t('common.chosen')]"
            show-search
            :list-style="{
              width: '18vw',
            }"
            :filter-option="filterOption"
            :target-keys="targetKeys"
            :render="renderItem"
            @change="handleChange"
          />
        </a-form-model-item>
        <a-form-model-item prop="webhook">
          <template slot="label">
            WebHooks
            <a-tooltip v-show="!temp.id">
              <template slot="title">
                <ul>
                  <li>{{$t('monitor.list.reqAtAlarm')}}</li>
                  <li>{{$t('monitor.list.passParam')}}</li>
                  <li>{{$t('monitor.list.judge')}}</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input v-model="temp.webhook" :placeholder="$t('monitor.list.receiveAlarm')" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { deleteMonitor, editMonitor, getMonitorList } from "@/api/monitor";
import { noFileModes } from "@/api/node-project";
import { getUserListAll } from "@/api/user/user";
import { getNodeListAll, getProjectListAll } from "@/api/node";
import { CHANGE_PAGE, COMPUTED_PAGINATION, CRON_DATA_SOURCE, PAGE_DEFAULT_LIST_QUERY, itemGroupBy, parseTime } from "@/utils/const";

export default {
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      cronDataSource: CRON_DATA_SOURCE,
      list: [],
      userList: [],
      nodeProjectList: [],
      nodeProjectGroupList: [],
      nodeMap: {},
      targetKeys: [],
      projectKeys: [],
      // tree 选中的值
      checkedKeys: {},
      noFileModes,
      temp: {},
      editMonitorVisible: false,
      columns: [
        { title: this.$t('common.name'), dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: this.$t('common.execCron'), dataIndex: "execCron", ellipsis: true, scopedSlots: { customRender: "execCron" } },
        { title: this.$t('common.openStatus'), dataIndex: "status", ellipsis: true, scopedSlots: { customRender: "status" }, width: 120 },
        { title: this.$t('autoRestart'), dataIndex: "autoRestart", ellipsis: true, scopedSlots: { customRender: "autoRestart" }, width: 120 },
        { title: this.$t('common.alarmStatus'), dataIndex: "alarm", ellipsis: true, scopedSlots: { customRender: "alarm" }, width: 120 },
        { title: this.$t('common.modifyUser'), dataIndex: "modifyUser", ellipsis: true, align: "center", scopedSlots: { customRender: "modifyUser" }, width: 120 },
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
        { title: this.$t('common.operation'), dataIndex: "operation", ellipsis: true, scopedSlots: { customRender: "operation" }, width: 120 },
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
  },
  methods: {
    // 页面引导
    introGuide() {
      this.$store.dispatch("tryOpenGuide", {
        key: "monitor",
        options: {
          hidePrev: true,
          steps: [
            {
              title: this.$t('common.introGuide'),
              element: document.querySelector(".jpom-notify"),
              intro: this.$t('monitor.list.intro'),
            },
          ],
        },
      });
    },

    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      getMonitorList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 加载用户列表
    loadUserList(fn) {
      getUserListAll().then((res) => {
        if (res.code === 200) {
          this.$nextTick(() => {
            this.userList = res.data.map((element) => {
              let canUse = element.email || element.dingDing || element.workWx;
              return { key: element.id, name: element.name, disabled: !canUse };
            });

            fn && fn();
          });
        }
      });
    },
    // 加载节点项目列表
    loadNodeProjectList(fn) {
      this.nodeProjectList = [];
      this.nodeProjectGroupList = [];
      getProjectListAll().then((res) => {
        if (res.code === 200) {
          getNodeListAll().then((res1) => {
            res1.data.forEach((element) => {
              this.nodeMap[element.id] = element;
            });

            this.nodeProjectList = res.data.map((item) => {
              let nodeInfo = res1.data.filter((nodeItem) => nodeItem.id === item.nodeId);
              item.nodeName = nodeInfo.length > 0 ? nodeInfo[0].name : this.$t('common.unknown');
              return item;
            });
            this.nodeProjectGroupList = itemGroupBy(this.nodeProjectList, "nodeId", "node", "projects");
            // console.log(this.nodeProjectGroupList);
            fn && fn();
          });
        }
      });
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.name.indexOf(inputValue) > -1;
    },
    // 穿梭框 change
    handleChange(targetKeys) {
      this.targetKeys = targetKeys;
    },
    renderItem(item) {
      const customLabel = (
        <a-tooltip title="如果不可以选择则表示对应的用户没有配置邮箱">
          <a-icon type="warning" theme="twoTone" />
          {item.name}
        </a-tooltip>
      );
      return {
        label: item.disabled ? customLabel : item.name,
        value: item.name,
      };
    },
    // 新增
    handleAdd() {
      this.temp = {};
      this.targetKeys = [];
      this.projectKeys = [];
      this.editMonitorVisible = true;
      this.loadUserList();
      this.loadNodeProjectList();
      this.$nextTick(() => {
        setTimeout(() => {
          this.introGuide();
        }, 500);
      });
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record);
      this.temp.projectsTemp = JSON.parse(this.temp.projects);
      this.targetKeys = [];
      this.loadUserList(() => {
        this.targetKeys = JSON.parse(this.temp.notifyUser);

        this.loadNodeProjectList(() => {
          // 设置监控项目
          this.projectKeys = this.nodeProjectList
            .filter((item) => {
              return (
                this.temp.projectsTemp.filter((item2) => {
                  let isNode = item.nodeId === item2.node;
                  if (!isNode) {
                    return false;
                  }
                  return item2.projects.filter((item3) => item.projectId === item3).length > 0;
                }).length > 0
              );
            })
            .map((item) => {
              return item.id;
            });

          this.editMonitorVisible = true;
        });
      });
    },
    handleEditMonitorOk() {
      // 检验表单
      this.$refs["editMonitorForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        let projects = this.nodeProjectList.filter((item) => {
          return this.projectKeys.includes(item.id);
        });
        projects = itemGroupBy(projects, "nodeId", "node", "projects");
        projects.map((item) => {
          item.projects = item.projects.map((item) => {
            return item.projectId;
          });
          return item;
        });

        let targetKeysTemp = this.targetKeys || [];
        targetKeysTemp = this.userList
          .filter((item) => {
            return targetKeysTemp.includes(item.key);
          })
          .map((item) => item.key);

        if (targetKeysTemp.length <= 0 && !this.temp.webhook) {
          this.$notification.warn({
            message: $t('monitor.list.webhookWarn'),
          });
          return false;
        }

        const params = {
          ...this.temp,
          status: this.temp.status ? "on" : "off",
          autoRestart: this.temp.autoRestart ? "on" : "off",
          projects: JSON.stringify(projects),
          notifyUser: JSON.stringify(targetKeysTemp),
        };
        editMonitor(params).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editMonitorForm"].resetFields();
            this.editMonitorVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: this.$t('common.systemPrompt'),
        content: this.$t('monitor.list.deleteMonitor'),
        okText: this.$t('common.confirm'),
        cancelText: this.$t('common.cancel'),
        onOk: () => {
          // 删除
          deleteMonitor(record.id).then((res) => {
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
