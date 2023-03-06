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
    machineDockerId: {
      type: String,
      default: "",
    },
    urlPrefix: {
      type: String,
    },
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId;
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
        id: this.reqDataId,
        line: this.line,
      };
      dockerImagePullImageLog(this.urlPrefix, params).then((res) => {
        let next = true;
        if (res.code === 200) {
          // 停止请求
          const dataLines = res.data.dataLines;
          if (dataLines && dataLines.length && dataLines[dataLines.length - 1].indexOf("pull end") > -1) {
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
