<template>
  <div class="wrapper">
    <RecycleScroller class="scroller" :id="uniqueId" :style="`min-height:${height};height:${height}`" key-field="id" :items="showList" :item-size="itemHeight" :emitUpdate="false">
      <template v-slot="{ index, item }">
        <div class="item">
          <template v-if="!item.warp">
            <span class="linenumber">{{ index + 1 }}</span> {{ item.text }} &nbsp;&nbsp;
          </template>
        </div>
        <!-- <code-editor
          ref="codemirror"
          v-model="item.text"
          :options="{
            theme: 'panda-syntax',
            mode: 'verilog',
            // maxHighlightLength: 5,
            viewportMargin: 1,
            cursorBlinkRate: -1,
            tabSize: 2,
            readOnly: true,
            styleActiveLine: true,
            lineNumbers: true,
            firstLineNumber: index + 1,
            lineWrapping: config.wordBreak,
          }"
        ></code-editor> -->
      </template>
    </RecycleScroller>
  </div>
</template>

<script>
import ansiparse from "@/utils/parse-ansi";
// import codeEditor from "@/components/codeEditor";
import { RecycleScroller } from "vue-virtual-scroller";
import "vue-virtual-scroller/dist/vue-virtual-scroller.css";
export default {
  components: {
    // codeEditor,
    RecycleScroller,
  },
  props: {
    height: {
      String,
      default: "50vh",
    },

    config: Object,
    id: {
      String,
      default: "logScrollArea",
    },
  },
  data() {
    return {
      defText: "loading context...",
      logContext: "",
      dataArray: [],
      idInc: 0,
      visibleStartIndex: -1,
      itemHeight: 24,
      uniqueId: `component_${Math.random().toString(36).substring(2, 15)}`,
    };
  },
  computed: {
    wordBreak() {
      // this.changeBuffer();
      return this.config.wordBreak || false;
    },
    showList() {
      const element = document.querySelector(`#${this.uniqueId}`);

      let result = [...this.dataArray];
      let warp = false;
      if (element) {
        const min = Math.ceil(element.clientHeight / this.itemHeight);
        const le = min - result.length;
        for (let i = 0; i < le; i++) {
          result.push({
            id: "system-warp-empty:" + i,
            warp: true,
          });
          warp = true;
        }
      }
      if (!warp) {
        result = result.concat([
          {
            id: "system-warp-end:1",
            warp: true,
          },
          {
            id: "system-warp-end:2",
            text: "",
            warp: true,
          },
        ]);
      }
      return result;
    },
    // showContext: {
    //   get() {
    //     return this.logContext || this.defText;
    //   },
    //   set() {},
    // },
  },
  mounted() {},
  methods: {
    scrollToBottom() {
      const element = document.querySelector(`#${this.uniqueId}`);
      if (element) {
        // console.log(element, element.scrollHeight);
        element.scrollTop = element.scrollHeight - element.clientHeight;
        // this.scrollTo(element, element.scrollHeight - element.clientHeight, 500);
        // element.scrollIntoView(false);
      }
    },
    scrollTo(element, position) {
      if (!window.requestAnimationFrame) {
        window.requestAnimationFrame = function (cb) {
          return setTimeout(cb, 10);
        };
      }
      var scrollTop = element.scrollTop;
      var step = function () {
        var distance = position - scrollTop;
        scrollTop = scrollTop + distance / 5;
        if (Math.abs(distance) < 1) {
          element.scrollTop = position;
        } else {
          element.scrollTop = scrollTop;
          requestAnimationFrame(step);
        }
      };
      step();
    },
    onUpdate(viewStartIndex, viewEndIndex, visibleStartIndex, visibleEndIndex) {
      const tempArray = this.dataArray.slice(visibleStartIndex, visibleEndIndex);
      this.logContext = tempArray
        .map((item) => {
          return item.text;
        })
        .map((item) => {
          return (
            // gitee isuess I657JR
            ansiparse(item)
              .map((ansiItem) => {
                return ansiItem.text;
              })
              .join("") + "\r\n"
          );
        })
        .join("");
      this.visibleStartIndex = visibleStartIndex;

      // console.log(this.dataArray.length, tempArray.length, visibleStartIndex, visibleEndIndex);
      // console.log(this.logContext);
    },
    //
    appendLine(data) {
      if (!data) {
        return;
      }
      const tempArray = (Array.isArray(data) ? data : [data]).map((item) => {
        return {
          text: ansiparse(item)
            .map((ansiItem) => {
              return ansiItem.text;
            })
            .join(""),
          id: this.idInc++,
        };
      });
      this.dataArray = [...this.dataArray, ...tempArray];
      // console.log(this.dataArray);
      if (this.config.logScroll) {
        setTimeout(() => {
          // 延迟触发滚动
          this.$nextTick(() => {
            this.scrollToBottom(".scroller");
          });
        }, 500);
      }
    },

    clearLogCache() {
      this.dataArray = [];
    },
  },
};
</script>

<style scoped>
.wrapper {
  /* overflow: hidden; */
  /* position: absolute; */
  /* top: 0; */
  /* bottom: 0; */
  /* left: 0; */
  /* right: 0; */
}
.scroller {
  height: 100%;
  width: 100%;
  font-family: Operator Mono, Source Code Pro, Menlo, Monaco, Consolas, Courier New, monospace;
  position: relative;

  overflow-y: scroll;
}
/deep/ .vue-recycle-scroller__item-wrapper {
  background: #292a2b;
  color: #ffb86c;
  white-space: nowrap;
  overflow-x: scroll;
  overflow-y: hidden;
  padding-bottom: 10px;
}
.item {
  padding: 0px 6px;
}

.linenumber {
  color: #e6e6e6;
  padding: 0px 4px;
  opacity: 0.6;
  /* overflow: auto; */
  /* white-space: nowrap; */
}
</style>
