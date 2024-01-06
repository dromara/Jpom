<template>
  <div class="code-mirror-div">
    <div class="tool-bar" ref="toolBar" v-if="showTool">
      <slot name="tool_before" />

      <a-space class="tool-bar-end">
        <div>
          皮肤：
          <a-select
            v-model:value="cmOptions.theme"
            @select="handleSelectTheme"
            show-search
            option-filter-prop="children"
            :filter-option="filterOption"
            placeholder="请选择皮肤"
            style="width: 150px"
          >
            <a-select-option v-for="item in themeList" :key="item">{{ item }}</a-select-option>
          </a-select>
        </div>
        <div>
          语言：
          <a-select
            v-model:value="cmOptions.mode"
            @select="handleSelectMode"
            show-search
            option-filter-prop="children"
            :filter-option="filterOption"
            placeholder="请选择语言模式"
            style="width: 150px"
          >
            <a-select-option value="">请选择语言模式</a-select-option>
            <a-select-option v-for="item in modeList" :key="item">{{ item }}</a-select-option>
          </a-select>
        </div>

        <a-tooltip>
          <template v-slot:title>
            <ul>
              <li>Ctrl-F / Cmd-F Start searching</li>
              <li>Ctrl-G / Cmd-G Find next</li>
              <li>Shift-Ctrl-G / Shift-Cmd-G Find previous</li>
              <li>Shift-Ctrl-F / Cmd-Option-F Replace</li>
              <li>Shift-Ctrl-R / Shift-Cmd-Option-F Replace all</li>
              <li>
                Alt-F Persistent search (dialog doesn't autoclose, enter to find next, Shift-Enter to find previous)
              </li>
              <li>Alt-G Jump to line</li>
            </ul>
          </template>
          <QuestionCircleOutlined />
        </a-tooltip>
      </a-space>
    </div>
    <div :style="{ height: codeMirrorHeight }">
      <Codemirror v-model:value="data" :options="cmOptions" @change="onCmCodeChanges" placeholder="请输入内容" />
    </div>
  </div>
</template>

<script>
import Codemirror from 'codemirror-editor-vue3'

// placeholder
import 'codemirror/addon/display/placeholder.js'

// language
import 'codemirror/mode/javascript/javascript.js'
// placeholder
import 'codemirror/addon/display/placeholder.js'
// theme
// import 'codemirror/theme/dracula.css'
// import 'codemirror/theme/blackboard.css'
// import 'codemirror/theme/eclipse.css'
import 'codemirror/lib/codemirror.css'

import 'codemirror/addon/hint/show-hint.css'
import 'codemirror/addon/hint/show-hint.js'
import 'codemirror/addon/hint/javascript-hint.js'
import 'codemirror/addon/hint/xml-hint.js'
import 'codemirror/addon/hint/css-hint.js'
import 'codemirror/addon/hint/html-hint.js'
import 'codemirror/addon/hint/sql-hint.js'
import 'codemirror/addon/hint/anyword-hint.js'
import 'codemirror/addon/lint/lint.css'
import 'codemirror/addon/lint/lint.js'
import 'codemirror/addon/lint/json-lint'
import 'codemirror/addon/lint/javascript-lint.js'

import 'codemirror/addon/fold/foldcode.js'
import 'codemirror/addon/fold/foldgutter.js'
import 'codemirror/addon/fold/foldgutter.css'
import 'codemirror/addon/fold/brace-fold.js'
import 'codemirror/addon/fold/xml-fold.js'
import 'codemirror/addon/fold/comment-fold.js'
import 'codemirror/addon/fold/markdown-fold.js'
import 'codemirror/addon/fold/indent-fold.js'

import 'codemirror/addon/edit/closebrackets.js'
import 'codemirror/addon/edit/closetag.js'
import 'codemirror/addon/edit/matchtags.js'
import 'codemirror/addon/edit/matchbrackets.js'

import 'codemirror/addon/selection/active-line.js'
import 'codemirror/addon/search/jump-to-line.js'
import 'codemirror/addon/dialog/dialog.js'
import 'codemirror/addon/dialog/dialog.css'
import 'codemirror/addon/search/searchcursor.js'
import 'codemirror/addon/search/search.js'
import 'codemirror/addon/display/autorefresh.js'
import 'codemirror/addon/selection/mark-selection.js'
import 'codemirror/addon/search/match-highlighter.js'

// 文件后缀与语言对应表
const fileSuffixToModeMap = {
  html: 'htmlmixed',
  css: 'css',
  yml: 'yaml',
  yaml: 'yaml',
  json: 'json',
  sh: 'shell',
  bat: 'powershell',
  vue: 'vue',
  xml: 'xml',
  sql: 'sql',
  py: 'python',
  php: 'php',
  md: 'markdown',
  dockerfile: 'dockerfile',
  properties: 'properties',
  lua: 'lua',
  go: 'go'
}
export default {
  components: { Codemirror },
  props: {
    // cmHintOptions: {
    //   type: Object,
    //   default() {
    //     return {}
    //   }
    // },
    content: {
      type: String,
      default: ''
    },
    options: {
      type: Object,
      default() {
        return {}
      }
    },
    fileSuffix: {
      type: String
    },
    showTool: {
      type: Boolean,
      default: true
    }
  },
  watch: {
    fileSuffix: {
      handler(v) {
        if (!v) {
          return
        }
        if (v.indexOf('.') > -1) {
          const textArr = v.split('.')
          const suffix = textArr.length ? textArr[textArr.length - 1] : v
          const newMode = fileSuffixToModeMap[suffix]
          if (newMode) {
            this.cmOptions = { ...this.cmOptions, mode: newMode }
          }
        } else {
          const v2 = v.toLowerCase()
          for (let key in fileSuffixToModeMap) {
            if (v2.endsWith(key)) {
              const newMode = fileSuffixToModeMap[key]
              if (newMode) {
                this.cmOptions = { ...this.cmOptions, mode: newMode }
              }
              break
            }
          }
        }
      },
      deep: true,
      immediate: true
    },
    options: {
      handler(n) {
        if (Object.keys(n).length) {
          const options = JSON.parse(JSON.stringify(n))
          this.cmOptions = { ...this.cmOptions, ...options }
        }
      },
      deep: true,
      immediate: true
    },
    content: {
      handler(v) {
        this.data = v || ''
        this.codeMirrorHeight = this.showTool ? `calc( 100% -  50px )` : '100%'
      },
      immediate: true
    }
  },
  data() {
    return {
      codeMirrorHeight: '',
      data: '',
      cmOptions: {
        mode: '', // Language mode
        theme: localStorage.getItem('editorTheme') || 'idea', // Theme
        // // 是否应滚动或换行以显示长行
        lineWrapping: true,
        lineNumbers: true,
        autofocus: true,
        // 自动缩进，设置是否根据上下文自动缩进（和上一行相同的缩进量）。默认为true
        smartIndent: false,
        autocorrect: true,
        dragDrop: false,
        spellcheck: true,
        // scrollbarStyle: "Addons",
        // 指定当前滚动到视图中的文档部分的上方和下方呈现的行数。默认为10 - [integer]
        // // 有点类似于虚拟滚动显示
        // Infinity - 无限制，始终显示全部内容，但是数据量大的时候会造成页面卡顿
        viewportMargin: 10,
        lint: { esversion: '8' },
        gutters: ['CodeMirror-lint-markers', 'CodeMirror-linenumbers', 'CodeMirror-foldgutter'],
        foldGutter: true,
        autoCloseBrackets: true,
        autoCloseTags: true,
        matchTags: { bothTags: true },
        matchBrackets: true,
        styleActiveLine: true,
        autoRefresh: true,
        highlightSelectionMatches: {
          minChars: 2,
          style: 'matchhighlight',
          showToken: true
        },
        styleSelectedText: true,
        enableAutoFormatJson: true,
        defaultJsonIndentation: 2
      },
      modeList: [],
      themeList: []
    }
  },
  mounted() {
    // https://juejin.cn/post/7218032919008624700
    const modules = import.meta.glob('/node_modules/codemirror/mode/**/*.js', { import: 'setup' })
    // const requireAll = (requireContext) => requireContext.keys().map(requireContext)
    // requireAll(modules)
    for (const path in modules) {
      modules[path]().then((mod) => {
        // console.log(path, mod)
        // console.log(path, mod)
        import(/* @vite-ignore */ path).then(() => {})
        const paths = path.split('/')
        this.modeList.push(paths[paths.length - 1].split('.')[0])
      })
    }
    const themes = import.meta.glob('/node_modules/codemirror/theme/**/*.css', { query: '?inline' })
    for (const path in themes) {
      themes[path]().then((mod) => {
        import(/* @vite-ignore */ path).then(() => {})
        const paths = path.split('/')
        this.themeList.push(paths[paths.length - 1].split('.')[0])
      })
    }
  },
  methods: {
    onCmCodeChanges(v) {
      this.$emit('update:content', v)
    },
    // 选择语言
    handleSelectMode(v) {
      this.cmOptions = { ...this.cmOptions, mode: v }
    },

    // 选择皮肤
    handleSelectTheme(v) {
      this.cmOptions = { ...this.cmOptions, theme: v }
      localStorage.setItem('editorTheme', v)
    },
    // 过滤选项
    filterOption(input, option) {
      return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
    }
  }
}
</script>

<style>
.CodeMirror {
  height: 100%;
  min-height: 200px;
}
</style>

<style scoped>
/* .CodeMirror-hints {
  z-index: 3330 !important;
} */
.code-mirror-div {
  height: 100%;
  line-height: 24px;
  border: 1px solid #d9d9d9;
  overflow: hidden;
}
.tool-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  margin: 5px 5px;
  padding-bottom: 5px;
  border-bottom: 1px solid #d9d9d9;
  /* 20px 0 0; */
}

.tool-bar-end {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
/*
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
} */
</style>
