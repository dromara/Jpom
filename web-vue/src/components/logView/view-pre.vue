<template>
  <pre class="log-view" :id="`${this.domId}`" :style="`height:${this.height}`">{{ defText }}</pre>
</template>

<script>
export default {
  components: {},
  props: {
    height: {
      String,
      default: "50vh",
    },
    searchReg: {
      String,
      default: "",
    },
    config: Object,
    id: {
      String,
      default: "logScrollArea",
    },
    seg: {
      String,
      default: "</br>",
    },
  },
  data() {
    return {
      regReplaceText: "<b style='color:red'>$1</b>",
      regRemove: /<b[^>]*>([^>]*)<\/b[^>]*>/g,
      regRemoveSpan: /<span[^>]*>([^>]*)<\/span[^>]*>/g,
      //   searchReg: null,
      // 日志内容
      logContextArray: [],
      //   defId: "logScrollArea",
      defText: "loading context...",
      domId: "",
    };
  },
  mounted() {
    this.domId = this.id + new Date().getTime();
    //let html = "<span><b><b style='color:OrangeRed;'>222</b></b></span>";
    //console.log(html.replace(this.regRemove, "$1").replace(this.regRemove, "$1").replace(this.regRemoveSpan, "$1"));
  },
  methods: {
    //
    appendLine(data) {
      if (!data) {
        return;
      }
      const dataArray = Array.isArray(data) ? data : [data];
      dataArray.forEach((item) => {
        item = item.replace(/[<>&]/g, function (match) {
          //  pos, originalText
          switch (match) {
            case "<":
              return "&lt;";
            case ">":
              return "&gt;";
            case "&":
              return "&amp;";
            case '"':
              return "&quot;";
          }
        });
        this.logContextArray.push(this.lineFormat(item));
      });

      //   console.log(this.config.logScroll, this.logContextArray);
      if (this.config.logScroll) {
        this.logContextArray = this.logContextArray.slice(-this.config.logShowLine);
      }

      // 自动滚动到底部
      this.$nextTick(() => {
        this.toHtml();
        const projectConsole = document.getElementById(this.domId);
        if (!projectConsole) {
          return;
        }
        // console.log("111");
        if (this.config.logScroll) {
          projectConsole.scrollTop = projectConsole.scrollHeight;
        }
      });
    },
    toHtml() {
      const projectConsole = document.getElementById(this.domId);
      if (!projectConsole) {
        return;
      }
      projectConsole.innerHTML = this.logContextArray.join("") || this.defText;
      //   console.log(this.config.logScroll, this.logContextArray);
      return projectConsole;
    },
    lineFormat(item) {
      // console.log(item.match(this.regRemove), item.replace(this.regRemove, "$1"));
      item = item.replace(this.regRemove, "$1").replace(this.regRemove, "$1").replace(this.regRemoveSpan, "$1");
      // console.log(item);
      if (this.searchReg) {
        item = item.replace(this.searchReg, this.regReplaceText);
      }
      if (item.match(/error|Exception/i)) {
        item = "<b style='color:OrangeRed'>" + item + "</b>";
      } else if (item.match(/WARNING|failed/i)) {
        item = "<b style='color:orange'>" + item + "</b>";
      }
      item = "<span>" + item + "</span>";
      // console.log(item);
      return item;
    },
    clearLogCache() {
      this.logContextArray = [];
      this.$nextTick(() => {
        const projectConsole = document.getElementById(this.domId);
        projectConsole.innerHTML = this.defText;
      });
    },
    changeBuffer() {
      // console.log(this.searchReg);
      this.logContextArray = this.logContextArray.map((item) => this.lineFormat(item));
      this.$nextTick(() => {
        this.toHtml();
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
</style>

<style>
.log-view span {
  display: block;
  counter-increment: line;
}
.log-view span:before {
  content: counter(line);
  display: inline-block;
  padding: 0 5px;
  /* border-right: 1px solid #e2e2e2; */
  color: #888;
}
</style>
