<template>
  <div class="code-mirror-div">
    <a-spin tip="加载编辑器中" :spinning="loading" v-if="loading">
      <a-skeleton />
    </a-spin>
    <template v-else>
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
              <a-select-option v-for="item in themeList" :key="item.theme">{{ item.name }}</a-select-option>
            </a-select>
          </div>
          <div>
            语言：
            <a-select
              v-model:value="cmOptions.mode"
              @select="handleSelectMode"
              show-search
              :filter-option="filterOption"
              placeholder="请选择语言模式"
              style="width: 150px"
            >
              <a-select-option value="">请选择语言模式</a-select-option>
              <a-select-option v-for="item in modeList" :key="item.mode">{{ item.name }}</a-select-option>
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
        <Codemirror
          v-model:value="data"
          :options="cmOptions"
          @change="onCmCodeChanges"
          @ready="onReady"
          placeholder="请输入内容"
        />
      </div>
    </template>
  </div>
</template>

<script>
import Codemirror from 'codemirror-editor-vue3'
import 'codemirror/lib/codemirror.css'

// language
import 'codemirror/mode/javascript/javascript.js'
import 'codemirror/mode/css/css.js'
import 'codemirror/mode/diff/diff.js'
import 'codemirror/mode/dockerfile/dockerfile.js'
import 'codemirror/mode/go/go.js'
import 'codemirror/mode/groovy/groovy.js'
import 'codemirror/mode/http/http.js'
import 'codemirror/mode/python/python.js'
import 'codemirror/mode/rpm/rpm.js'
import 'codemirror/mode/sass/sass.js'
import 'codemirror/mode/ruby/ruby.js'
import 'codemirror/mode/shell/shell.js'
import 'codemirror/mode/vue/vue.js'
import 'codemirror/mode/xml/xml.js'
import 'codemirror/mode/yaml/yaml.js'
import 'codemirror/mode/vb/vb.js'
import 'codemirror/mode/sql/sql.js'
import 'codemirror/mode/powershell/powershell.js'
import 'codemirror/mode/nginx/nginx.js'
import 'codemirror/mode/cmake/cmake.js'
import 'codemirror/mode/properties/properties.js'
import 'codemirror/mode/php/php.js'
import 'codemirror/mode/htmlmixed/htmlmixed.js'
import 'codemirror/mode/yaml-frontmatter/yaml-frontmatter.js'
const modes = [
  { name: 'shell', mode: 'shell' },
  { name: 'python', mode: 'python' },
  { name: 'powershell', mode: 'powershell' },
  { name: 'nginx', mode: 'nginx' },
  { name: 'dockerfile', mode: 'dockerfile' },
  { name: 'yaml', mode: 'yaml' },
  { name: 'properties', mode: 'properties' },
  { name: 'htmlmixed', mode: 'htmlmixed' },
  { name: 'go', mode: 'go' },
  { name: 'php', mode: 'php' },
  { name: 'rpm', mode: 'rpm' },
  { name: 'sass', mode: 'sass' },
  { name: 'vue', mode: 'vue' },
  { name: 'xml', mode: 'xml' },
  { name: 'sql', mode: 'sql' },
  { name: 'javascript', mode: 'javascript' },
  { name: 'css', mode: 'css' },
  { name: 'diff', mode: 'diff' },
  { name: 'vb', mode: 'vb' },
  { name: 'http', mode: 'http' },
  { name: 'ruby', mode: 'ruby' },
  { name: 'groovy', mode: 'groovy' },
  { name: 'cmake', mode: 'cmake' }
]
//

// theme
import 'codemirror/theme/3024-day.css'
import 'codemirror/theme/3024-night.css'
import 'codemirror/theme/abcdef.css'
import 'codemirror/theme/ambiance-mobile.css'
import 'codemirror/theme/ayu-dark.css'
import 'codemirror/theme/ambiance.css'
import 'codemirror/theme/ayu-mirage.css'
import 'codemirror/theme/abbott.css'
import 'codemirror/theme/base16-dark.css'
import 'codemirror/theme/base16-light.css'
import 'codemirror/theme/bespin.css'
import 'codemirror/theme/blackboard.css'
import 'codemirror/theme/cobalt.css'
import 'codemirror/theme/colorforth.css'
import 'codemirror/theme/darcula.css'
import 'codemirror/theme/dracula.css'
import 'codemirror/theme/duotone-dark.css'
import 'codemirror/theme/duotone-light.css'
import 'codemirror/theme/eclipse.css'
import 'codemirror/theme/elegant.css'
import 'codemirror/theme/erlang-dark.css'
import 'codemirror/theme/gruvbox-dark.css'
import 'codemirror/theme/hopscotch.css'
import 'codemirror/theme/icecoder.css'
import 'codemirror/theme/idea.css'
import 'codemirror/theme/isotope.css'
import 'codemirror/theme/juejin.css'
import 'codemirror/theme/lesser-dark.css'
import 'codemirror/theme/liquibyte.css'
import 'codemirror/theme/lucario.css'
import 'codemirror/theme/material-darker.css'
import 'codemirror/theme/material-palenight.css'
import 'codemirror/theme/material-ocean.css'
import 'codemirror/theme/material.css'
import 'codemirror/theme/mbo.css'
import 'codemirror/theme/mdn-like.css'
import 'codemirror/theme/midnight.css'
import 'codemirror/theme/monokai.css'
import 'codemirror/theme/moxer.css'
import 'codemirror/theme/neat.css'
import 'codemirror/theme/neo.css'
import 'codemirror/theme/night.css'
import 'codemirror/theme/nord.css'
import 'codemirror/theme/oceanic-next.css'
import 'codemirror/theme/panda-syntax.css'
import 'codemirror/theme/paraiso-dark.css'
import 'codemirror/theme/paraiso-light.css'
import 'codemirror/theme/pastel-on-dark.css'
import 'codemirror/theme/railscasts.css'
import 'codemirror/theme/rubyblue.css'
import 'codemirror/theme/seti.css'
import 'codemirror/theme/shadowfox.css'
import 'codemirror/theme/solarized.css'
import 'codemirror/theme/ssms.css'
import 'codemirror/theme/the-matrix.css'
import 'codemirror/theme/tomorrow-night-bright.css'
import 'codemirror/theme/tomorrow-night-eighties.css'
import 'codemirror/theme/ttcn.css'
import 'codemirror/theme/twilight.css'
import 'codemirror/theme/vibrant-ink.css'
import 'codemirror/theme/xq-dark.css'
import 'codemirror/theme/xq-light.css'
import 'codemirror/theme/yeti.css'
import 'codemirror/theme/yonce.css'
import 'codemirror/theme/zenburn.css'
const theme = [
  // { name: '深色', theme: 'dracula' },
  // { name: '浅色', theme: 'eclipse' },
  // { name: '深色2', theme: 'blackboard' },
  // { name: '', theme: 'abbott' },
  // { name: '深白', theme: 'abcdef' },
  // { name: '黑白', theme: 'ambiance' },
  // { name: 'ayu深', theme: 'ayu-dark' },
  // { name: 'ayu深2', theme: 'ayu-mirage' },
  // { name: '浅灰', theme: 'bespin' }

  { name: '灰绿 abbott', theme: 'abbott' },
  { name: '灰绿 abcdef', theme: 'abcdef' },
  { name: '黑白 ambiance-mobile', theme: 'ambiance-mobile' },
  { name: '黑白 ambiance', theme: 'ambiance' },
  { name: '深色 dracula', theme: 'dracula' },
  { name: '浅色 eclipse', theme: 'eclipse' },
  { name: '深色2 blackboard', theme: 'blackboard' },
  { name: 'ayu-dark', theme: 'ayu-dark' },
  { name: 'ayu-mirage', theme: 'ayu-mirage' },
  { name: 'base16-dark', theme: 'base16-dark' },
  { name: 'base16-light', theme: 'base16-light' },
  { name: 'bespin', theme: 'bespin' },

  { name: 'cobalt', theme: 'cobalt' },
  { name: 'colorforth', theme: 'colorforth' },

  { name: 'duotone-dark', theme: 'duotone-dark' },
  { name: 'duotone-light', theme: 'duotone-light' },

  { name: 'elegant', theme: 'elegant' },
  { name: 'erlang-dark', theme: 'erlang-dark' },
  { name: 'gruvbox-dark', theme: 'gruvbox-dark' },
  { name: 'hopscotch', theme: 'hopscotch' },
  { name: 'icecoder', theme: 'icecoder' },
  { name: 'idea', theme: 'idea' },
  { name: 'isotope', theme: 'isotope' },
  { name: 'juejin', theme: 'juejin' },
  { name: 'lesser-dark', theme: 'lesser-dark' },
  { name: 'liquibyte', theme: 'liquibyte' },
  { name: 'lucario', theme: 'lucario' },
  { name: 'material-darker', theme: 'material-darker' },
  { name: 'material-ocean', theme: 'material-ocean' },
  { name: 'material-palenight', theme: 'material-palenight' },
  { name: 'material', theme: 'material' },
  { name: 'mbo', theme: 'mbo' },
  { name: 'mdn-like', theme: 'mdn-like' },
  { name: 'midnight', theme: 'midnight' },
  { name: 'monokai', theme: 'monokai' },
  { name: 'moxer', theme: 'moxer' },
  { name: 'neat', theme: 'neat' },
  { name: 'neo', theme: 'neo' },
  { name: 'night', theme: 'night' },
  { name: 'nord', theme: 'nord' },
  { name: 'oceanic-next', theme: 'oceanic-next' },
  { name: 'panda-syntax', theme: 'panda-syntax' },
  { name: 'paraiso-dark', theme: 'paraiso-dark' },
  { name: 'paraiso-light', theme: 'paraiso-light' },
  { name: 'pastel-on-dark', theme: 'pastel-on-dark' },
  { name: 'railscasts', theme: 'railscasts' },
  { name: 'rubyblue', theme: 'rubyblue' },
  { name: 'seti', theme: 'seti' },
  { name: 'shadowfox', theme: 'shadowfox' },
  { name: 'solarized', theme: 'solarized' },
  { name: 'ssms', theme: 'ssms' },
  { name: 'the-matrix', theme: 'the-matrix' },
  { name: 'tomorrow-night-bright', theme: 'tomorrow-night-bright' },
  { name: 'tomorrow-night-eighties', theme: 'tomorrow-night-eighties' },
  { name: 'ttcn', theme: 'ttcn' },
  { name: 'twilight', theme: 'twilight' },
  { name: 'vibrant-ink', theme: 'vibrant-ink' },
  { name: 'xq-dark', theme: 'xq-dark' },
  { name: 'xq-light', theme: 'xq-light' },
  { name: 'yeti', theme: 'yeti' },
  { name: 'yonce', theme: 'yonce' },
  { name: 'zenburn', theme: 'zenburn' },
  { name: '3024-day', theme: '3024-day' },
  { name: '3024-night', theme: '3024-night' }
]
//
//
//
//
//

// placeholder
import 'codemirror/addon/display/placeholder.js'

// import 'codemirror/addon/hint/show-hint.css'
// import 'codemirror/addon/hint/show-hint.js'
// import 'codemirror/addon/hint/javascript-hint.js'
// import 'codemirror/addon/hint/xml-hint.js'
// import 'codemirror/addon/hint/css-hint.js'
// import 'codemirror/addon/hint/html-hint.js'
// import 'codemirror/addon/hint/sql-hint.js'
// import 'codemirror/addon/hint/anyword-hint.js'
// 自动提示
// import 'codemirror/addon/lint/lint.css'
// import 'codemirror/addon/lint/lint.js'
// import 'codemirror/addon/lint/json-lint'
// import 'codemirror/addon/lint/javascript-lint.js'
// 代码折叠
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
// 当前行高亮
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
      default: false
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
        // extraKeys: {
        //   'Alt-Q': 'autocomplete',
        //   'Ctrl-Alt-L': () => {
        //     try {
        //       if (this.cmOptions.mode == 'json' && this.editorValue) {
        //         this.editorValue = this.formatStrInJson(this.editorValue)
        //       }
        //     } catch (e) {
        //       this.$message.error('格式化代码出错：' + e.toString())
        //     }
        //   }
        // },
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
      modeList: modes,
      themeList: theme,
      loading: true
    }
  },
  mounted() {
    // https://juejin.cn/post/7218032919008624700
    // const modules = import.meta.glob('/node_modules/codemirror/mode/**/*.js', { import: 'setup' })
    // // const requireAll = (requireContext) => requireContext.keys().map(requireContext)
    // // requireAll(modules)
    // for (const path in modules) {
    //   modules[path]().then((mod) => {
    //     const paths = path.split('/')
    //     console.log(path, paths[paths.length - 1].split('.')[0])
    //     // console.log(path, mod)
    //     import(/* @vite-ignore */ path).then(() => {})

    //     this.modeList.push(paths[paths.length - 1].split('.')[0])
    //   })
    // }
    // const themes = import.meta.glob('/node_modules/codemirror/theme/**/*.css', { query: '?inline' })
    // const array = []
    // for (const path in themes) {
    //   themes[path]().then((mod) => {
    //     console.log(`import '${path.replace('/node_modules/', '')}'`)
    //     console.log(path, mod)
    //     import(/* @vite-ignore */ path).then(() => {})
    //     const paths = path.split('/')
    //     array.push({ name: paths[paths.length - 1].split('.')[0], theme: paths[paths.length - 1].split('.')[0] })
    //     this.themeList.push(paths[paths.length - 1].split('.')[0])
    //     console.log(JSON.stringify(array))
    //   })
    // }

    // 延迟渲染，等待资源加载完成
    setTimeout(() => {
      this.loading = false
    }, 1000)
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
    },
    onReady(editor) {
      // console.log(editor)
      // // 绑定其他快捷键, 格式化编辑器代码做示例
      // let autoFormatSelection = () => {
      //   const script_length = editor.getValue().length
      //   const startPos = { line: 0, ch: 0, sticky: null }
      //   const endPos = editor.doc.posFromIndex(script_length)
      //   editor.setSelection(startPos, endPos)
      //   editor.autoFormatRange(startPos, endPos)
      //   editor.commentRange(false, startPos, endPos)
      // }
      // editor.addKeyMap({
      //   'Ctrl-Alt-L': autoFormatSelection
      // })
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
