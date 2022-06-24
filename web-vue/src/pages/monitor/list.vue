<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :columns="columns" :pagination="pagination" @change="changePage" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="监控名称" class="search-input-item" />
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="listQuery.status"
            allowClear
            placeholder="开启状态"
            class="search-input-item"
          >
            <a-select-option :value="1">开启</a-select-option>
            <a-select-option :value="0">关闭</a-select-option>
          </a-select>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="listQuery.autoRestart"
            allowClear
            placeholder="自动重启"
            class="search-input-item"
          >
            <a-select-option :value="1">是</a-select-option>
            <a-select-option :value="0">否</a-select-option>
          </a-select>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="listQuery.alarm"
            allowClear
            placeholder="报警状态"
            class="search-input-item"
          >
            <a-select-option :value="1">报警中</a-select-option>
            <a-select-option :value="0">未报警</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-switch slot="status" size="small" slot-scope="text" :checked="text" disabled checked-children="开启" un-checked-children="关闭" />
      <a-switch slot="autoRestart" size="small" slot-scope="text" :checked="text" disabled checked-children="是" un-checked-children="否" />
      <a-switch slot="alarm" size="small" slot-scope="text" :checked="text" disabled checked-children="报警中" un-checked-children="未报警" />
      <a-tooltip slot="parent" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button>
          <a-button type="danger" size="small" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editMonitorVisible" width="60%" title="编辑监控" @ok="handleEditMonitorOk" :maskClosable="false">
      <a-form-model ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="监控名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="监控名称" />
        </a-form-model-item>

        <a-form-model-item label="开启状态" prop="status">
          <a-space size="large">
            <a-switch v-model="temp.status" checked-children="开" un-checked-children="关" />
            <div>
              自动重启:
              <a-switch v-model="temp.autoRestart" checked-children="开" un-checked-children="关" />
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

        <a-form-model-item label="监控周期" prop="execCron">
          <a-auto-complete v-model="temp.execCron" placeholder="如果需要定时自动执行则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）" option-label-prop="value">
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
        <a-form-model-item label="监控项目" prop="projects">
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            option-label-prop="label"
            v-model="projectKeys"
            mode="multiple"
            placeholder="选择要监控的项目,file 类型项目不可以监控"
            show-search
            option-filter-prop="children"
          >
            <a-select-opt-group :label="nodeMap[nodeItem.node].name" v-for="nodeItem in nodeProjectGroupList" :key="nodeItem.node">
              <a-select-option :label="`${project.name} - ${project.runMode}`" v-for="project in nodeItem.projects" :disabled="!noFileModes.includes(project.runMode)" :key="project.id">
                【{{ project.nodeName }}】{{ project.name }} - {{ project.runMode }}
              </a-select-option>
            </a-select-opt-group>
          </a-select>
        </a-form-model-item>
        <a-form-model-item prop="notifyUser" class="jpom-notify">
          <template slot="label">
            联系人
            <a-tooltip v-show="!temp.id">
              <template slot="title"> 如果这里的报警联系人无法选择，说明这里面的管理员没有设置邮箱，在右上角下拉菜单里面的用户资料里可以设置。 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-transfer
            :data-source="userList"
            :lazy="false"
            :titles="['待选择', '已选择']"
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
                  <li>发生报警时候请求</li>
                  <li>传人参数有：monitorId、monitorName、nodeId、nodeName、projectId、projectName、title、content、runStatus</li>
                  <li>runStatus 值为 true 表示项目当前为运行中(异常恢复),false 表示项目当前未运行(发生异常)</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input v-model="temp.webhook" placeholder="接收报警消息,非必填，GET请求" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import {deleteMonitor, editMonitor, getMonitorList} from "@/api/monitor";
import {noFileModes} from "@/api/node-project";
import {getUserListAll} from "@/api/user";
import {getNodeListAll, getProjectListAll} from "@/api/node";
import {itemGroupBy, parseTime} from "@/utils/time";
import {CHANGE_PAGE, COMPUTED_PAGINATION, CRON_DATA_SOURCE, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";

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
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "监控周期", dataIndex: "execCron", ellipsis: true, scopedSlots: { customRender: "execCron" } },
        { title: "开启状态", dataIndex: "status", ellipsis: true, scopedSlots: { customRender: "status" }, width: 120 },
        { title: "自动重启", dataIndex: "autoRestart", ellipsis: true, scopedSlots: { customRender: "autoRestart" }, width: 120 },
        { title: "报警状态", dataIndex: "alarm", ellipsis: true, scopedSlots: { customRender: "alarm" }, width: 120 },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, align: "center", scopedSlots: { customRender: "modifyUser" }, width: 120 },
        {
          title: "修改时间",
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
        { title: "操作", dataIndex: "operation", ellipsis: true, scopedSlots: { customRender: "operation" }, width: 120 },
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
              title: "导航助手",
              element: document.querySelector(".jpom-notify"),
              intro: "如果这里的报警联系人无法选择，说明这里面的管理员没有设置邮箱，在右上角下拉菜单里面的用户资料里可以设置。",
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
              item.nodeName = nodeInfo.length > 0 ? nodeInfo[0].name : "未知";
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
            message: "请选择一位报警联系人或者填写webhook",
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
        title: "系统提示",
        content: "真的要删除监控么？",
        okText: "确认",
        cancelText: "取消",
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
