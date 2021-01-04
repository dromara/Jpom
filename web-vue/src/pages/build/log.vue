<template>
  <div>
    <a-input v-model="logText" type="textarea" class="console" readOnly :rows="15" style="resize: none;"/>
  </div>
</template>
<script>
import { loadBuildLog } from '../../api/build';
export default {
  props: {
    temp: {
      type: Object
    }
  },
  data() {
    return {
      logTimer: null,
      logText: 'loading...'
    }
  },
  beforeDestroy() {
    if (this.logTimer) {
      clearInterval(this.logTimer);
    }
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // 加载日志内容
    loadData() {
      // 加载构建日志
      this.logTimer = setInterval(() => {
        const params = {
          id: this.temp.id,
          buildId: this.temp.buildId,
          line: 1
        }
        loadBuildLog(params).then(res => {
          if (res.code === 200) {
            // 停止请求
            if (res.data.run === false) {
              clearInterval(this.logTimer);
            }
            // 更新日志
            this.logText = '';
            res.data.dataLines.forEach(element => {
              this.logText += `${element}\r\n`;
            });
          }
        })
      }, 2000);
    }
  }
}
</script>
<style scoped>
.console {
  padding: 5px;
  color: #fff;
  font-size: 14px;
  background-color: black;
  width: 100%;
  overflow-y: auto;
  border: 1px solid #e2e2e2;
  border-radius: 5px 5px;
}
</style>