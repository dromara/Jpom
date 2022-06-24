<template>
  <div class="code-mirror-div">
    <div class="tool-bar" ref="toolBar" v-if="showTool">
      <slot name="tool_before" />
      <a-space>
        <div>
          皮肤：
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="cmOptions.theme"
            @select="handleSelectTheme"
            show-search
            option-filter-prop="children"
            :filter-option="filterOption"
            placeholder="请选择"
            style="width: 150px"
          >
            <a-select-option v-for="item in cmThemeOptions" :key="item">{{ item }}</a-select-option>
          </a-select>
        </div>
        <div>
          语言：
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="cmOptions.mode"
            @select="handleSelectMode"
            show-search
            option-filter-prop="children"
            :filter-option="filterOption"
            placeholder="请选择"
            style="width: 150px"
          >
            <a-select-option v-for="item in cmEditorModeOptions" :key="item">{{ item }}</a-select-option>
          </a-select>
        </div>

        <a-tooltip>
          <template slot="title">
            <ul>
              <li>Ctrl-F / Cmd-F Start searching</li>
              <li>Ctrl-G / Cmd-G Find next</li>
              <li>Shift-Ctrl-G / Shift-Cmd-G Find previous</li>
              <li>Shift-Ctrl-F / Cmd-Option-F Replace</li>
              <li>Shift-Ctrl-R / Shift-Cmd-Option-F Replace all</li>
              <li>Alt-F Persistent search (dialog doesn't autoclose, enter to find next, Shift-Enter to find previous)</li>
              <li>Alt-G Jump to line</li>
            </ul>
          </template>
          <a-icon type="question-circle" theme="filled" />
        </a-tooltip>
      </a-space>

      <slot name="tool_after" />
    </div>
    <div :style="{ height: codeMirrorHeight }">
      <codemirror
        ref="myCm"
        :value="editorValue"
        :options="cmOptions"
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
import {codemirror} from "vue-codemirror";
import "codemirror/lib/codemirror.css";

import "codemirror/theme/blackboard.css";
import "codemirror/theme/eclipse.css";

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
import {JSHINT} from "jshint";

window.JSHINT = JSHINT;

const requireAll = (requireContext) => requireContext.keys().map(requireContext);

// 引入支持的语法
const reqMode = require.context("codemirror/mode/", true, /\.js$/);
requireAll(reqMode);
const modeList = reqMode.keys().map((item) => {
  return item.split("/")[1];
});
modeList.unshift("json");

// 引入支持的皮肤
const reqTheme = require.context("codemirror/theme/", false, /\.css$/);
requireAll(reqTheme);
const themeList = reqTheme.keys().map((item) => {
  return item.substring(2, item.length - 4);
});

// 文件后缀与语言对应表
const fileSuffixToModeMap = {
  html: "htmlmixed",
  css: "css",
  yml: "yaml",
  yaml: "yaml",
  json: "json",
  sh: "shell",
  bat: "powershell",
  vue: "vue",
  xml: "xml",
  sql: "sql",
  py: "python",
  php: "php",
  md: "markdown",
};

export default {
  name: "CodeEditor",
  components: {
    codemirror,
  },
  model: {
    prop: "code",
    event: "input",
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
    fileSuffix: {
      type: String,
    },
    showTool: {
      type: Boolean,
      default: false,
    },
  },

  data() {
    return {
      codeMirrorHeight: "100%",
      editorValue: this.code,
      cmThemeOptions: themeList,
      cmEditorModeOptions: modeList,
      cmOptions: {
        theme: localStorage.getItem("editorTheme") || "idea",
        mode: "json",
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
              if (this.cmOptions.mode == "json" && this.editorValue) {
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
        // defaultJsonIndentation: 2,
      },
      enableAutoFormatJson: true,
      defaultJsonIndentation: 2,
    };
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
    fileSuffix: {
      handler(v) {
        if (!v) {
          return;
        }
        const textArr = v.split(".");
        const suffix = textArr.length ? textArr[textArr.length - 1] : v;
        const newMode = fileSuffixToModeMap[suffix];
        if (newMode) {
          this.cmOptions = { ...this.cmOptions, mode: newMode };
        }
      },
      deep: false,
      immediate: true,
    },
    options: {
      handler(n) {
        if (Object.keys(n).length) {
          const options = JSON.parse(JSON.stringify(n));
          this.cmOptions = { ...this.cmOptions, ...options };
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
      if (this.cmOptions.mode == "json") {
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
    // 选择语言
    handleSelectMode(v) {
      this.cmOptions.mode = v;
    },

    // 选择皮肤
    handleSelectTheme(v) {
      this.cmOptions.theme = v;
      localStorage.setItem("editorTheme", v);
    },

    // 黏贴事件处理函数
    OnPaste() {
      if (this.cmOptions.mode == "json") {
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
    },

    // 格式化字符串为json格式字符串
    formatStrInJson(strValue) {
      this.$emit("checkJson", strValue);
      return JSON.stringify(JSON.parse(strValue), null, this.defaultJsonIndentation);
    },

    // 过滤选项
    filterOption(input, option) {
      return option.componentOptions.children[0].text.toLowerCase().indexOf(input.toLowerCase()) >= 0;
    },
  },
};
</script>
<style>
.CodeMirror {
  height: 100%;
  min-height: 200px;
}
</style>
<style scoped>
.CodeMirror-hints {
  z-index: 3330 !important;
}
.code-mirror-div {
  height: 100%;
  line-height: 24px;
  border: 1px solid #d9d9d9;
}
.tool-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin: 5px 5px;
  padding-bottom: 5px;
  border-bottom: 1px solid #d9d9d9;
  /* 20px 0 0; */
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
