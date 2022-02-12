<template>
  <div>
    <log-view :ref="`logView`" height="70vh" marginTop="-10px" />
  </div>
</template>
<script>
import LogView from "@/components/logView";
import { loadBuildLog } from "@/api/build-info";
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
      logText: "loading...",
      line: 1,
    };
  },
  beforeDestroy() {
    this.logTimer && clearTimeout(this.logTimer);
  },
  mounted() {
    this.pullLog();
  },
  methods: {
    nextPull() {
      if (this.logTimer) {
        clearTimeout(this.logTimer);
      }
      // 加载构建日志
      this.logTimer = setTimeout(() => {
        this.pullLog();
      }, 2000);
    },
    // 加载日志内容
    pullLog() {
      const params = {
        id: this.temp.id,
        buildId: this.temp.buildId,
        line: this.line,
      };
      loadBuildLog(params).then((res) => {
        let next = true;
        if (res.code === 200) {
          // 停止请求
          if (res.data.run === false) {
            clearInterval(this.logTimer);
            next = false;
          }
          this.$refs.logView.appendLine(res.data.dataLines);
          this.line = res.data.line;
        } else if (res.code !== 201) {
          // 201 是当前构建且没有日志
          this.$notification.warn({
            message: res.msg,
          });
          clearInterval(this.logTimer);
          next = false;
          this.$refs.logView.appendLine(res.msg);
        }
        // 继续拉取日志
        if (next) this.nextPull();
      });
    },
  },
};
</script>
<style scoped></style>
