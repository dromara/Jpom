<template>
  <div>
    <log-view :ref="`logView`" height="70vh" marginTop="-10px" />
  </div>
</template>
<script>
import { scriptLog } from "@/api/node-other";
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
      logTimer: null,
      // logText: "loading...",
      line: 1,
    };
  },
  beforeDestroy() {
    if (this.logTimer) {
      clearInterval(this.logTimer);
    }
  },
  mounted() {
    this.init();
  },
  methods: {
    init() {
      this.loadData();
      this.logTimer = setInterval(() => {
        this.loadData();
      }, 2000);
    },
    // 加载日志内容
    loadData() {
      // 加载构建日志
      const params = {
        executeId: this.temp.id,
        id: this.temp.scriptId,
        nodeId: this.temp.nodeId,
        line: this.line,
      };
      scriptLog(params).then((res) => {
        if (res.code === 200) {
          // 停止请求
          if (res.data.run === false) {
            clearInterval(this.logTimer);
          }
          // 更新日志
          // if (this.logText === "loading...") {
          //   this.logText = "";
          // }
          // let lines = res.data.dataLines;
          // lines.forEach((element) => {
          //   this.logText += `${element}\r\n`;
          // });
          this.$refs.logView.appendLine(res.data.dataLines);
          this.line = res.data.line;
          // if (lines.length) {
          //   // 自动滚动到底部
          //   this.$nextTick(() => {
          //     setTimeout(() => {
          //       const textarea = document.getElementById("script-log-textarea");
          //       if (textarea) {
          //         textarea.scrollTop = textarea.scrollHeight;
          //       }
          //     }, 100);
          //   });
          // }
        }
      });
    },
  },
};
</script>
<style scoped></style>
