<template>
  <div>
    <code-editor
      ref="codemirror"
      :style="`height:${this.height}`"
      v-model="showContext"
      :options="{ theme: 'panda-syntax', mode: 'verilog', cursorBlinkRate: -1, tabSize: 2, readOnly: true, styleActiveLine: true, lineWrapping: this.config.wordBreak }"
    ></code-editor>
  </div>
</template>

<script>
import ansiparse from "@/utils/parse-ansi";
import codeEditor from "@/components/codeEditor";

export default {
  components: {
    codeEditor,
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
    };
  },
  computed: {
    wordBreak() {
      this.changeBuffer();
      return this.config.wordBreak || false;
    },
    showContext: {
      get() {
        return this.logContext || this.defText;
      },
      set() {},
    },
  },
  mounted() {},
  methods: {
    //
    appendLine(data) {
      if (!data) {
        return;
      }
      const dataArray = Array.isArray(data) ? data : [data];
      this.logContext += dataArray
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
      if (this.config.logScroll) {
        setTimeout(() => {
          // 延迟触发滚动
          this.$nextTick(() => {
            this.$refs?.codemirror?.scrollToBottom();
          });
        }, 500);
      }
    },

    clearLogCache() {
      this.logContext = "";
    },
  },
};
</script>

<style scoped></style>

<style></style>
