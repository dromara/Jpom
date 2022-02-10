<template>
  <div :style="`margin-top:${this.marginTop}`">
    <div class="log-filter">
      <template>
        <a-row type="flex">
          <a-col :span="22">
            <a-space>
              <slot name="before"></slot>
              <!-- <a-input-group compact style="width: 200px"> -->
              滚动方式：
              <a-select v-model="temp.logScroll" style="width: 100px">
                <a-select-option value="true"> 自动滚动 </a-select-option>
                <a-select-option value="false"> 关闭滚动 </a-select-option>
              </a-select>
              <a-tooltip title="为避免显示内容太多而造成浏览器卡顿,限制只显示最后多少行日志">
                <a-input-number :min="1" style="width: 100px" v-model="temp.logShowLine" placeholder="显示行数" @pressEnter="onSearch">
                  <template slot="addonAfter">
                    <a-icon type="swap" />
                  </template>
                </a-input-number>
              </a-tooltip>
              <!-- </a-input-group> -->
              <a-tooltip title="关键词高亮,支持正则">
                <a-input placeholder="关键词高亮" v-model="temp.searchValue" style="width: 200px" @pressEnter="onSearch">
                  <template slot="addonAfter">
                    <a-icon type="search" />
                  </template>
                </a-input>
              </a-tooltip>
            </a-space>
          </a-col>
          <a-col :span="2" style="text-align: right">
            <a-button type="link" style="padding: 0" @click="clearLogCache" icon="delete"> 清空 </a-button>
          </a-col>
        </a-row>
      </template>
    </div>
    <pre class="log-view" :id="this.id" :style="`height:${this.height}`">{{ defText }}</pre>
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
    marginTop: {
      String,
      default: "0px",
    },
  },
  data() {
    return {
      regReplaceText: "<b style='color:red;'>$1</b>",
      searchReg: null,
      // 日志内容
      logContextArray: [],
      temp: {
        logScroll: "true",
        logShowLine: 500,
        searchValue: "",
      },
      defLogShowLine: 500,
      defId: "logScrollArea",
      defText: "loading context...",
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
        item = item.replace(/[<>"&]/g, function (match) {
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
        // console.log(item);
        if (this.searchReg) {
          this.logContextArray.push(item.replace(this.searchReg, this.regReplaceText));
        } else {
          this.logContextArray.push(item);
        }
      });

      let logShowLineTemp = parseInt(this.temp.logShowLine);
      logShowLineTemp = isNaN(logShowLineTemp) ? this.defLogShowLine : logShowLineTemp;
      logShowLineTemp = logShowLineTemp > 0 ? logShowLineTemp : 1;
      if (this.temp.logScroll === "true") {
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
    onSearch() {
      this.searchReg = this.temp.searchValue ? new RegExp("(" + this.temp.searchValue + ")", "ig") : null;
      // console.log(this.searchReg);
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
        projectConsole.innerHTML = this.logContextArray.join(this.seg) || this.defText;
      });
    },
    clearLogCache() {
      this.logContextArray = [];
      this.$nextTick(() => {
        const projectConsole = document.getElementById(this.id);
        projectConsole.innerHTML = this.defText;
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
.log-filter {
  /* margin-top: -22px; */
  /* margin-bottom: 10px; */
  padding: 0 10px;
  padding-top: 0;
  padding-bottom: 10px;
  line-height: 0;
}
</style>
