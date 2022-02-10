<template>
  <div>
    <div class="filter">
      <template>
        <a-space>
          <a-input-group compact style="width: 200px">
            <a-select v-model="logScroll">
              <a-select-option value="true"> 自动滚动 </a-select-option>
              <a-select-option value="false"> 关闭滚动 </a-select-option>
            </a-select>
            <a-input style="width: 50%" v-model="logShowLine" placeholder="显示行数" />
          </a-input-group>

          <a-input-search placeholder="搜索关键词" style="width: 200px" @search="onSearch" />
        </a-space>
      </template>
    </div>
    <pre class="log-view" :id="this.id" :style="`height:${this.height}`">
      loading context...
    </pre>
  </div>
</template>

<script>
export default {
  name: "LogView",
  props: {
    seg: {
      String,
      default: "</br>",
    },
    height: {
      String,
      default: "50vh",
    },
  },
  data() {
    return {
      regReplaceText: "<b style='color:red;'>$1</b>",
      searchReg: null,
      // 日志内容
      logContextArray: [],
      logScroll: "true",
      logShowLine: 500,
      defLogShowLine: 500,
      defId: "logScrollArea",
      id: "",
    };
  },
  mounted() {
    this.id = this.defId + new Date().getTime();
  },
  methods: {
    appendLine(data) {
      if (!data) {
        return;
      }
      const dataArray = Array.isArray(data) ? data : [data];
      dataArray.forEach((item) => {
        if (this.searchReg) {
          this.logContextArray.push(item.replace(this.searchReg, this.regReplaceText));
        } else {
          this.logContextArray.push(item);
        }
      });

      let logShowLineTemp = parseInt(this.logShowLine);
      logShowLineTemp = isNaN(logShowLineTemp) ? this.defLogShowLine : logShowLineTemp;
      logShowLineTemp = logShowLineTemp > 0 ? logShowLineTemp : 1;
      if (this.logScroll === "true") {
        this.logContextArray = this.logContextArray.slice(-logShowLineTemp);
      }

      // 自动滚动到底部
      this.$nextTick(() => {
        const projectConsole = document.getElementById(this.id);
        if (!projectConsole) {
          return;
        }
        projectConsole.innerHTML = this.logContextArray.join(this.seg);
        if (this.logScroll === "true") {
          setTimeout(() => {
            projectConsole.scrollTop = projectConsole.scrollHeight;
          }, 100);
        }
      });
    },
    // 搜索
    onSearch(value) {
      this.searchReg = value ? new RegExp("(" + value + ")", "ig") : null;

      this.logContextArray = this.logContextArray.map((item) => {
        item = item.replace(/<b[^>]*>([^>]*)<\/b[^>]*>/gi, "$1");
        if (this.searchReg) {
          item = item.replace(this.searchReg, this.regReplaceText);
        }
        return item;
      });
      this.$nextTick(() => {
        const projectConsole = document.getElementById(this.id);
        if (!projectConsole) {
          return;
        }
        projectConsole.innerHTML = this.logContextArray.join(this.seg);
      });
    },
  },
};
</script>

<style scoped>
.log-view {
  padding: 5px;
  color: #fff;
  font-size: 14px;
  background-color: black;
  width: 100%;
  height: calc(100vh - 120px);
  overflow-y: auto;
  border: 1px solid #e2e2e2;
  border-radius: 5px 5px;
}
.filter {
  margin-top: -22px;
  margin-bottom: 10px;
}
</style>
