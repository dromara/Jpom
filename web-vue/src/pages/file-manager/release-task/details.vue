<template>
  <div>
    <a-form-model :model="temp" :label-col="{ span: 2 }" :wrapper-col="{ span: 20 }">
      <a-form-model-item label="任务名" prop="name">
        <a-input placeholder="请输入任务名" :disabled="true" :value="temp.taskData && temp.taskData.name" />
      </a-form-model-item>

      <a-form-model-item label="发布方式" prop="taskType">
        <a-radio-group :value="temp.taskData && temp.taskData.taskType" :disabled="true">
          <a-radio :value="0"> SSH </a-radio>
          <a-radio :value="1"> 节点 </a-radio>
        </a-radio-group>
      </a-form-model-item>

      <a-form-model-item prop="taskDataIds" label="发布的SSH" v-if="temp.taskType === 0">
        <a-row>
          <a-col :span="22">
            <a-select show-search option-filter-prop="children" mode="multiple" v-model="temp.taskDataIds" placeholder="请选择SSH">
              <a-select-option v-for="ssh in sshList" :key="ssh.id">
                <a-tooltip :title="ssh.name"> {{ ssh.name }}</a-tooltip>
              </a-select-option>
            </a-select>
          </a-col>
          <a-col :span="1" style="margin-left: 10px">
            <a-icon type="reload" @click="loadSshList" />
          </a-col>
        </a-row>
      </a-form-model-item>

      <a-form-model-item prop="releasePath" label="发布目录">
        <a-input placeholder="请输入任务名" :disabled="true" :value="temp.taskData && temp.taskData.releasePath" />
      </a-form-model-item>
      <a-form-model-item prop="releasePath" label="状态" :help="temp.taskData && temp.taskData.statusMsg">
        {{ statusMap[temp.taskData && temp.taskData.status] || "未知" }}
      </a-form-model-item>

      <a-form-model-item label="执行日志">
        <a-tabs :activeKey="activeKey" @change="tabCallback">
          <a-tab-pane v-for="item in temp.taskList" :key="item.id">
            <span slot="tab">
              <a-icon v-if="!logMap[item.id] || logMap[item.id].run" type="loading" />
              {{
                sshList.filter((sshItem) => {
                  return sshItem.id === item.taskDataId;
                })[0] &&
                sshList.filter((sshItem) => {
                  return sshItem.id === item.taskDataId;
                })[0].name
              }}
            </span>
            <log-view :ref="`logView-${item.id}`" height="60vh" />
          </a-tab-pane>
        </a-tabs>
      </a-form-model-item>
      <a-form-model-item label="执行脚本" prop="releaseBeforeCommand">
        <a-tabs tabPosition="right">
          <a-tab-pane key="before" tab="上传前">
            <div style="height: 40vh; overflow-y: scroll">
              <code-editor
                :code="temp.taskData && temp.taskData.beforeScript"
                :options="{ mode: temp.taskData && temp.taskData.taskType === 0 ? 'shell' : '', tabSize: 2, theme: 'abcdef', readOnly: true }"
              ></code-editor>
            </div>
          </a-tab-pane>
          <a-tab-pane key="after" tab="上传后">
            <div style="height: 40vh; overflow-y: scroll">
              <code-editor
                :code="temp.taskData && temp.taskData.afterScript"
                :options="{ mode: temp.taskData && temp.taskData.taskType === 0 ? 'shell' : '', tabSize: 2, theme: 'abcdef', readOnly: true }"
              ></code-editor>
            </div>
          </a-tab-pane>
        </a-tabs>
      </a-form-model-item>
    </a-form-model>
  </div>
</template>
<script>
import { taskDetails, statusMap, taskLogInfoList } from "@/api/file-manager/release-task-log";
import LogView from "@/components/logView";
import codeEditor from "@/components/codeEditor";
import { getSshListAll } from "@/api/ssh";

export default {
  components: {
    LogView,
    codeEditor,
  },
  props: {
    taskId: {
      type: String,
    },
  },
  data() {
    return {
      statusMap,
      logList: [],
      activeKey: "",
      logTimerMap: {},
      logMap: {},
      temp: {},
      sshList: [],
    };
  },
  beforeDestroy() {
    if (this.logTimerMap) {
      this.temp.taskList?.forEach((item) => {
        clearInterval(this.logTimerMap[item.id]);
      });
    }
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // 加载日志内容
    loadData() {
      this.activeKey = this.temp.id || "";
      taskDetails({
        id: this.taskId,
      }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data;
          if (this.temp.taskData?.taskType === 0) {
            this.loadSshList();
          }

          if (!this.activeKey) {
            this.activeKey = this.temp.taskList && this.temp.taskList[0].id;
          }
          this.tabCallback(this.activeKey);
        }
      });
    },
    // 加载 SSH 列表
    loadSshList() {
      return new Promise((resolve) => {
        this.sshList = [];
        getSshListAll().then((res) => {
          if (res.code === 200) {
            this.sshList = res.data;
            resolve();
          }
        });
      });
    },
    initItemTimer(item) {
      if (!item) {
        return;
      }
      // 加载构建日志
      this.logMap[item.id] = {
        line: 1,
        run: true,
      };
      this.pullLog(item);
      this.logTimerMap[item.id] = setInterval(() => {
        this.pullLog(item);
      }, 2000);
    },
    pullLog(item) {
      const params = {
        id: item.id,
        line: this.logMap[item.id].line,
        tryCount: 0,
      };

      taskLogInfoList(params).then((res) => {
        if (res.code === 200) {
          if (!res.data) {
            this.$notification.warning({
              message: res.msg,
            });
            if (res.data.status !== 0) {
              // 还未开始的不计算次数
              this.logMap[item.id].tryCount = this.logMap[item.id].tryCount + 1;
              if (this.logMap[item.id].tryCount > 10) {
                clearInterval(this.logTimerMap[item.id]);
              }
            }
            return false;
          }
          // 停止请求
          if (!res.data.run) {
            clearInterval(this.logTimerMap[item.id]);
          }
          this.logMap[item.id].run = res.data.run;
          // 更新日志

          this.$refs[`logView-${item.id}`][0]?.appendLine(res.data.dataLines);

          this.logMap[item.id].line = res.data.line;

          this.logMap = { ...this.logMap };
        }
      });
    },
    tabCallback(key) {
      if (!key) {
        return;
      }
      this.activeKey = key;
      // console.log(this.$refs);
      if (this.logTimerMap[key]) {
        return;
      }
      this.$nextTick(() => {
        const data = this.temp.taskList?.filter((item1) => {
          return item1.id === key;
        })[0];
        this.initItemTimer(data);
      });
    },
  },
};
</script>
<style scoped></style>
