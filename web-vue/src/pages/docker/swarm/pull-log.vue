<template>
  <div>
    <log-view :ref="`logView`" height="70vh" marginTop="-10px" />
  </div>
</template>
<script>
import LogView from "@/components/logView";
import { dockerSwarmServicesPullLog, dockerSwarmServicesStartLog } from "@/api/docker-swarm";
export default {
  components: {
    LogView,
  },
  props: {
    dataId: {
      type: String,
    },
    id: {
      type: String,
    },
    type: {
      type: String,
    },
    urlPrefix: {
      type: String,
    },
  },
  data() {
    return {
      logTimer: null,
      logId: "",
      line: 1,
    };
  },
  beforeDestroy() {
    this.logTimer && clearTimeout(this.logTimer);
  },
  mounted() {
    //
    this.init();
  },
  methods: {
    init() {
      dockerSwarmServicesStartLog(this.urlPrefix, {
        type: this.type,
        dataId: this.dataId,
        id: this.id,
      }).then((res) => {
        if (res.code === 200) {
          this.logId = res.data;
          this.pullLog();
        } else {
          this.$refs.logView.appendLine(res.msg);
        }
      });
    },
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
        id: this.logId,
        line: this.line,
      };
      dockerSwarmServicesPullLog(this.urlPrefix, params).then((res) => {
        let next = true;
        if (res.code === 200) {
          // 停止请求
          const dataLines = res.data.dataLines;

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
