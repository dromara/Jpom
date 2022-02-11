<template>
  <div>
    <log-view :ref="`logView`" height="70vh" marginTop="-10px" />
  </div>
</template>
<script>
import LogView from "@/components/logView";
import { dockerImagePullImageLog } from "@/api/docker-api";
export default {
  components: {
    LogView,
  },
  props: {
    id: {
      type: String,
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
      this.logTimer && clearTimeout(this.logTimer);
      // 加载构建日志
      this.logTimer = setTimeout(() => {
        this.pullLog();
      }, 2000);
    },
    // 加载日志内容
    pullLog() {
      const params = {
        id: this.id,
        line: this.line,
      };
      dockerImagePullImageLog(params).then((res) => {
        let next = true;
        if (res.code === 200) {
          // 停止请求
          const dataLines = res.data.dataLines;
          if (dataLines && dataLines[dataLines.length - 1] === "pull end") {
            this.logTimer && clearTimeout(this.logTimer);
            next = false;
          }

          this.$refs.logView.appendLine(dataLines);
          this.line = res.data.line;
        }
        // 继续拉取日志
        if (next) this.nextPull();
      });
    },
  },
};
</script>
<style scoped></style>
