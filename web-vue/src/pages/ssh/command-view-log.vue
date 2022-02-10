<template>
  <div style="margin-top: -10px">
    <a-tabs :activeKey="activeKey" @change="tabCallback">
      <a-tab-pane v-for="item in logList" :key="item.id">
        <span slot="tab">
          <a-icon v-if="!logMap[item.id] || logMap[item.id].run" type="loading" />
          {{ item.sshName }}
        </span>
        <!-- <a-input :id="`build-log-textarea-${item.id}`" v-model="logMap[item.id].logText" type="textarea" class="console" readOnly style="resize: none; height: 60vh" /> -->
        <log-view :ref="`logView-${item.id}`" height="60vh" />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>
<script>
import { getCommandLogBarchList, getCommandLogInfo } from "@/api/command";
import LogView from "@/components/logView";
export default {
  components: {
    LogView,
  },
  props: {
    temp: {
      type: Object,
    },
  },
  data() {
    return {
      logList: [],
      activeKey: "",
      logTimerMap: {},
      logMap: {},
    };
  },
  beforeDestroy() {
    if (this.logTimerMap) {
      this.logList.forEach((item) => {
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
      getCommandLogBarchList({
        commandId: this.temp.commandId,
        batchId: this.temp.batchId,
      }).then((res) => {
        if (res.code === 200) {
          this.logList = res.data;
          if (!this.activeKey) {
            this.activeKey = this.logList[0].id;
          }
          this.tabCallback(this.activeKey);
        }
      });
    },
    initItemTimer(item) {
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
      getCommandLogInfo(params).then((res) => {
        if (res.code === 200) {
          if (!res.data) {
            this.$notification.warning({
              message: res.msg,
            });
            this.logMap[item.id].tryCount = this.logMap[item.id].tryCount + 1;
            if (this.logMap[item.id].tryCount > 10) {
              clearInterval(this.logTimerMap[item.id]);
            }
            return false;
          }
          // 停止请求
          if (res.data.run === false) {
            clearInterval(this.logTimerMap[item.id]);
          }
          this.logMap[item.id].run = res.data.run;
          // 更新日志
          // if (this.logMap[item.id].logText === "loading...") {
          //   this.logMap[item.id].logText = "";
          // }
          // const index = this.logList
          //   .map((item1, key) => {
          //     return item1.id == item.id ? key : -1;
          //   })
          //   .filter((item1) => item1 !== -1)[0];

          // console.log(this.$refs, this.$refs.logView, index);
          this.$refs[`logView-${item.id}`][0]?.appendLine(res.data.dataLines);
          // lines.forEach((element) => {
          //   //this.logMap[item.id].logText += `${element}\r\n`;

          //   // const logViewId = ;
          //   console.log(this.$refs.logView, this.$refs[`"logView- ${item.id}"`]);

          // });
          this.logMap[item.id].line = res.data.line;
          // if (lines.length) {
          //   // 自动滚动到底部
          //   this.$nextTick(() => {
          //     setTimeout(() => {
          //       const textarea = document.getElementById("build-log-textarea-" + item.id);
          //       if (textarea) {
          //         textarea.scrollTop = textarea.scrollHeight;
          //       }
          //     }, 100);
          //   });
          // }
          this.logMap = { ...this.logMap };
          //console.log(this.logMap);
        }
      });
    },
    tabCallback(key) {
      this.activeKey = key;
      // console.log(this.$refs);
      if (this.logTimerMap[key]) {
        return;
      }
      this.$nextTick(() => {
        const index = this.logList
          .map((item1, index) => {
            return item1.id == key ? index : -1;
          })
          .filter((item1) => item1 !== -1)[0];
        this.initItemTimer(this.logList[index]);
      });
    },
  },
};
</script>
<style scoped>
/* .console {
  padding: 5px;
  color: #fff;
  font-size: 14px;
  background-color: black;
  width: 100%;
  overflow-y: auto;
  border: 1px solid #e2e2e2;
  border-radius: 5px 5px;
} */
</style>
