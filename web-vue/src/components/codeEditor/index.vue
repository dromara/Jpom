<template>
  <div class="code-mirror-div">
    <div class="tool-bar" ref="toolBar" v-if="showTool">
      <slot name="tool_before" />
      皮肤：
      <a-select v-model="cmOptions.theme" placeholder="请选择" style="width: 150px">
        <a-select-option v-for="item in cmThemeOptions" :key="item">{{ item }}</a-select-option>
      </a-select>
      <slot name="tool_after" />
    </div>
    <div :style="{ height: codeMirrorHeight }">
      <codemirror
        ref="myCm"
        :value="editorValue"
        :options="{ ...cmOptions, ...userOption }"
        @input="$emit('input', $event)"
        @changes="onCmCodeChanges"
        @blur="onCmBlur"
        @keydown.native="onKeyDown"
        @mousedown.native="onMouseDown"
        @paste.native="OnPaste"
      ></codemirror>
    </div>
  </div>
</template>

<script>
import { codemirror } from "vue-codemirror";
import "codemirror/lib/codemirror.css";

import "codemirror/theme/blackboard.css";
import "codemirror/theme/eclipse.css";
import "codemirror/mode/javascript/javascript.js";
import "codemirror/mode/xml/xml.js";
import "codemirror/mode/htmlmixed/htmlmixed.js";
import "codemirror/mode/css/css.js";
import "codemirror/mode/yaml/yaml.js";
import "codemirror/mode/sql/sql.js";
import "codemirror/mode/python/python.js";
import "codemirror/mode/markdown/markdown.js";
import "codemirror/addon/hint/show-hint.css";
import "codemirror/addon/hint/show-hint.js";
import "codemirror/addon/hint/javascript-hint.js";
import "codemirror/addon/hint/xml-hint.js";
import "codemirror/addon/hint/css-hint.js";
import "codemirror/addon/hint/html-hint.js";
import "codemirror/addon/hint/sql-hint.js";
import "codemirror/addon/hint/anyword-hint.js";
import "codemirror/addon/lint/lint.css";
import "codemirror/addon/lint/lint.js";
import "codemirror/addon/lint/json-lint";
// require("script-loader!jsonlint");
import "codemirror/addon/lint/javascript-lint.js";
import "codemirror/addon/fold/foldcode.js";
import "codemirror/addon/fold/foldgutter.js";
import "codemirror/addon/fold/foldgutter.css";
import "codemirror/addon/fold/brace-fold.js";
import "codemirror/addon/fold/xml-fold.js";
import "codemirror/addon/fold/comment-fold.js";
import "codemirror/addon/fold/markdown-fold.js";
import "codemirror/addon/fold/indent-fold.js";
import "codemirror/addon/edit/closebrackets.js";
import "codemirror/addon/edit/closetag.js";
import "codemirror/addon/edit/matchtags.js";
import "codemirror/addon/edit/matchbrackets.js";
import "codemirror/addon/selection/active-line.js";
import "codemirror/addon/search/jump-to-line.js";
import "codemirror/addon/dialog/dialog.js";
import "codemirror/addon/dialog/dialog.css";
import "codemirror/addon/search/searchcursor.js";
import "codemirror/addon/search/search.js";
import "codemirror/addon/display/autorefresh.js";
import "codemirror/addon/selection/mark-selection.js";
import "codemirror/addon/search/match-highlighter.js";
import { JSHINT } from "jshint";

window.JSHINT = JSHINT;

export default {
  name: "CodeEditor",
  components: {
    codemirror,
  },
  model: {
    prop: "code",
    event: "input",
  },
  data() {
    return {
      codeMirrorHeight: "100%",
      editorValue: this.code,
      userOption: {},
      cmThemeOptions: [
        "default",
        "3024-day",
        "3024-night",
        "abcdef",
        "ambiance",
        "ayu-dark",
        "ayu-mirage",
        "base16-dark",
        "base16-light",
        "bespin",
        "blackboard",
        "cobalt",
        "colorforth",
        "darcula",
        "dracula",
        "duotone-dark",
        "duotone-light",
        "eclipse",
        "elegant",
        "erlang-dark",
        "gruvbox-dark",
        "hopscotch",
        "icecoder",
        "idea",
        "isotope",
        "lesser-dark",
        "liquibyte",
        "lucario",
        "material",
        "material-darker",
        "material-palenight",
        "material-ocean",
        "mbo",
        "mdn-like",
        "midnight",
        "monokai",
        "moxer",
        "neat",
        "neo",
        "night",
        "nord",
        "oceanic-next",
        "panda-syntax",
        "paraiso-dark",
        "paraiso-light",
        "pastel-on-dark",
        "railscasts",
        "rubyblue",
        "seti",
        "shadowfox",
        "solarized dark",
        "solarized light",
        "the-matrix",
        "tomorrow-night-bright",
        "tomorrow-night-eighties",
        "ttcn",
        "twilight",
        "vibrant-ink",
        "xq-dark",
        "xq-light",
        "yeti",
        "yonce",
        "zenburn",
      ],
      cmEditorModeOptions: ["default", "json", "sql", "javascript", "xml", "html", "yaml", "markdown", "python"],
      cmOptions: {
        theme: "idea",
        mode: "application/json",
        lineWrapping: true,
        lineNumbers: true,
        autofocus: true,
        smartIndent: false,
        autocorrect: true,
        spellcheck: true,
        hintOptions: this.cmHintOptions || {},
        extraKeys: {
          "Alt-Q": "autocomplete",
          "Ctrl-Alt-L": () => {
            try {
              if (this.cmOptions.mode == "application/json" && this.editorValue) {
                this.editorValue = this.formatStrInJson(this.editorValue);
              }
            } catch (e) {
              this.$message.error("格式化代码出错：" + e.toString());
            }
          },
        },
        lint: { esversion: "8" },
        gutters: ["CodeMirror-lint-markers", "CodeMirror-linenumbers", "CodeMirror-foldgutter"],
        foldGutter: true,
        autoCloseBrackets: true,
        autoCloseTags: true,
        matchTags: { bothTags: true },
        matchBrackets: true,
        styleActiveLine: true,
        autoRefresh: true,
        highlightSelectionMatches: {
          minChars: 2,
          style: "matchhighlight",
          showToken: true,
        },
        styleSelectedText: true,
        enableAutoFormatJson: true,
        defaultJsonIndentation: 2,
      },
      enableAutoFormatJson: true,
      defaultJsonIndentation: 2,
    };
  },
  props: {
    cmHintOptions: {
      type: Object,
      default() {
        return {};
      },
    },
    code: {
      type: String,
      default: "",
    },
    options: {
      type: Object,
      default() {
        return {};
      },
    },
    showTool: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    myCodemirror() {
      return this.$refs.mirror.codemirror;
    },
    inCode: {
      get() {
        return this.code;
      },
      set() {},
    },
  },
  watch: {
    "cmOptions.theme": function (newValue) {
      try {
        let theme = this.cmOptions.theme == "default" ? "blackboard" : newValue;
        require("codemirror/theme/" + theme + ".css");
        this.cmOptions.theme = theme;
        this.resetLint();
      } catch (e) {
        this.$message.error("切换编辑器主题出错：" + e.toString());
      }
    },
    options: {
      handler(n) {
        if (Object.keys(n).length) {
          this.userOption = JSON.parse(JSON.stringify(n));
        }
      },
      deep: true,
      immediate: true,
    },
    code(n) {
      if (n != this.editorValue) {
        try {
          this.editorValue = this.formatStrInJson(n);
        } catch (error) {
          this.editorValue = n;
          // 啥也不做
        }
      }
    },
  },
  mounted() {
    this.codeMirrorHeight = this.showTool ? `calc( 100% - ${this.$refs.toolBar.offsetHeight + 10}px )` : "100%";
  },
  created() {
    try {
      if (!this.editorValue) {
        this.cmOptions.lint = false;
        return;
      }
      if (this.cmOptions.mode == "application/json") {
        if (!this.enableAutoFormatJson) {
          return;
        }
        this.editorValue = this.formatStrInJson(this.editorValue);
      }
    } catch (e) {
      // console.log("初始化codemirror出错：" + e);
      // this.$message.error("初始化codemirror出错：" + e);
    }
  },
  methods: {
    resetLint() {
      if (!this.$refs.myCm.codemirror.getValue()) {
        this.$nextTick(() => {
          this.$refs.myCm.codemirror.setOption("lint", false);
        });
        return;
      }
      this.$refs.myCm.codemirror.setOption("lint", false);
      this.$nextTick(() => {
        this.$refs.myCm.codemirror.setOption("lint", { esversion: "8" });
      });
    },
    resetFoldGutter() {
      this.$refs.myCm.codemirror.setOption("foldGutter", false);
      this.$nextTick(() => {
        this.$refs.myCm.codemirror.setOption("foldGutter", true);
      });
    },
    // 获取值
    getValue() {
      try {
        return this.$refs.myCm.codemirror.getValue();
      } catch (e) {
        let errorInfo = e.toString();
        this.$message.error("获取编辑框内容失败：" + errorInfo);
        return errorInfo;
      }
    },
    // 修改值
    setValue(value) {
      try {
        if (typeof value != typeof "") {
          this.$message.error("修改编辑框内容失败：编辑宽内容只能为字符串");
          return;
        }
        if (this.cmOptions.mode == "application/json") {
          this.editorValue = this.formatStrInJson(value);
        } else {
          this.editorValue = value;
        }
      } catch (e) {
        this.$message.error("修改编辑框内容失败：" + e.toString());
      }
    },
    // 黏贴事件处理函数
    OnPaste() {
      if (this.cmOptions.mode == "application/json") {
        try {
          this.editorValue = this.formatStrInJson(this.editorValue);
        } catch (e) {
          // 啥都不做
        }
      }
    },
    // 失去焦点时处理函数
    onCmBlur(cm) {
      this.$emit("onCmBlur", cm.getValue());
      try {
        let editorValue = cm.getValue();
        if (this.cmOptions.mode == "application/json" && editorValue) {
          if (!this.enableAutoFormatJson) {
            return;
          }
          this.editorValue = this.formatStrInJson(editorValue);
        }
      } catch (e) {
        // 啥也不做
      }
    },
    // 按下键盘事件处理函数
    onKeyDown(event) {
      const keyCode = event.keyCode || event.which || event.charCode;
      const keyCombination = event.ctrlKey || event.altKey || event.metaKey;
      //满足条件触发代码提示
      if (!keyCombination && keyCode > 64 && keyCode < 123) {
        this.$refs.myCm.codemirror.showHint({ completeSingle: false });
      }
    },
    // 按下鼠标时事件处理函数
    onMouseDown() {
      //取消代码提示
      this.$refs.myCm.codemirror.closeHint();
    },
    onCmCodeChanges(cm) {
      this.editorValue = cm.getValue();
      this.resetLint();
    },
    // 格式化字符串为json格式字符串
    formatStrInJson(strValue) {
      this.$emit("checkJson", strValue);
      return JSON.stringify(
        // JSON.parse(strValue.replace(/\s/g, '')),
        JSON.parse(strValue),
        null,
        this.defaultJsonIndentation
      );
    },
  },
};
</script>
<style scoped>
.CodeMirror-hints {
  z-index: 3330 !important;
}
.code-mirror-div {
  height: 100%;
}
.tool-bar {
  margin: 10px 0;
}
.CodeMirror {
  height: 100%;
  border: 1px solid #ccc;
}
.CodeMirror-selected {
  background-color: blue !important;
}

.CodeMirror-selectedtext {
  color: white !important;
}
.cm-matchhighlight {
  background-color: #fef6f6;
}
.CodeMirror-scroll {
  margin: 0;
  padding: 0;
}
.vue-codemirror {
  height: 100%;
}
</style>
