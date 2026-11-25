<!--

    Copyright (c) 2019 Of Him Code Technology Studio
    Jpom is licensed under Mulan PSL v2.
    You can use this software according to the terms and conditions of the Mulan PSL v2.
    You may obtain a copy of Mulan PSL v2 at:
    			http://license.coscl.org.cn/MulanPSL2
    THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
    See the Mulan PSL v2 for more details.

-->
<template>
  <div
    class="code-mirror-div"
    :style="{
      height: height
    }"
  >
    <a-spin v-if="loading" :tip="$t('i18n_6a8c30bd06')" :spinning="loading">
      <a-skeleton />
    </a-spin>
    <template v-else>
      <div v-if="showTool" ref="toolBar" class="tool-bar">
        <slot name="tool_before" />

        <a-space class="tool-bar-end">
          <div>
            {{ $t('i18n_117a9cbc8d')
            }}<a-select
              v-model:value="cmOptions.mode"
              show-search
              :filter-option="
                (input, option) => {
                  const children = option.children && option.children()
                  return (
                    children &&
                    children[0].children &&
                    children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  )
                }
              "
              :placeholder="$t('i18n_773b1a5ef6')"
              style="width: 150px"
              @select="handleSelectMode"
            >
              <a-select-option value="">{{ $t('i18n_773b1a5ef6') }}</a-select-option>
              <a-select-option v-for="item in modeList" :key="item.mode">{{ item.name }}</a-select-option>
            </a-select>
          </div>

          <a-tooltip>
            <template #title>
              <ul>
                <li><b>Ctrl-F / Cmd-F</b>&nbsp;{{ $t('i18n_99d3e5c718') }}</li>
                <li><b>Ctrl-G / Cmd-G</b>&nbsp;{{ $t('i18n_6292498392') }}</li>
                <li><b>Shift-Ctrl-G / Shift-Cmd-G</b>&nbsp;{{ $t('i18n_11e88c95ee') }}</li>
                <li><b>Shift-Ctrl-F / Cmd-Option-F</b>&nbsp;{{ $t('i18n_d324f8b5c9') }}</li>
                <li><b>Shift-Ctrl-R / Shift-Cmd-Option-F</b>&nbsp;{{ $t('i18n_b9c4cf7483') }}</li>
                <li><b>Alt-F</b>&nbsp;{{ $t('i18n_9402665a2c') }}</li>
                <li><b>Alt-G</b>&nbsp;{{ $t('i18n_5fc6c33832') }}</li>
              </ul>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </a-space>
      </div>
      <div :style="{ height: codeMirrorHeight }">
        <Codemirror
          ref="cmRef"
          v-model:value="data"
          v-model:cminstance="cmInstance"
          :options="cmOptions"
          :placeholder="placeholder"
          @change="onCmCodeChanges"
          @ready="onReady"
        />
      </div>
    </template>
  </div>
</template>
<script>
import Codemirror from 'codemirror-editor-vue3'
import { useGuideStore } from '@/stores/guide'
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
  { name: 'powershell', mode: 'powershell' },
  { name: 'dockerfile', mode: 'dockerfile' },
  { name: 'yaml', mode: 'yaml' },
  { name: 'properties', mode: 'properties' },
  { name: 'htmlmixed', mode: 'htmlmixed' },
  { name: 'nginx', mode: 'nginx' },
  { name: 'python', mode: 'python' },
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
import 'codemirror/theme/idea.css'
import 'codemirror/theme/material-darker.css'

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
import { t } from '@/i18n/index'

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
    height: {
      type: String,
      default: '100%'
    },
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
      type: String,
      default: ''
    },
    showTool: {
      type: Boolean,
      default: false
    },
    placeholder: {
      type: String,
      default: function () {
        return t('i18n_a11cc7a65b')
      }
    }
  },
  emits: ['update:content'],
  computed: {
    // 系统主题（含「跟随系统」自动识别）→ 编辑器主题，只有深/浅两种
    ...mapState(useGuideStore, ['getThemeView']),
    editorTheme() {
      const view = typeof this.getThemeView === 'function' ? this.getThemeView() : 'light'
      return view === 'dark' ? 'material-darker' : 'idea'
    }
  },
  data() {
    return {
      codeMirrorHeight: '',
      data: '',
      cmOptions: {
        mode: '', // Language mode
        // 主题只有两种：浅色 idea / 深色 material-darker，由系统主题自动推导（见 computed.editorTheme）
        theme: 'idea', // Theme
        // // 是否应滚动或换行以显示长行
        lineWrapping: true,
        lineNumbers: true,
        autofocus: false,
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
        //       $message.error('格式化代码出错：' + e.toString())
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
      // CodeMirror 实例（v-model:cminstance 由 codemirror-editor-vue3 回传），用于运行时切主题
      cmInstance: null,

      loading: true
    }
  },
  watch: {
    // 主题切换（亮/暗/跟随系统）时同步给 CodeMirror 实例，不重建组件、不丢光标
    editorTheme: {
      handler(theme) {
        this.applyEditorTheme(theme)
      },
      immediate: true
    },
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


    applyEditorTheme(theme) {
      const next = theme || this.editorTheme
      if (this.cmOptions.theme !== next) {
        this.cmOptions = { ...this.cmOptions, theme: next }
      }
      const instance = this.cmInstance || (this.$refs.cmRef && this.$refs.cmRef.cminstance)
      if (instance && typeof instance.setOption === 'function') {
        instance.setOption('theme', next)
      }
    },
    onReady(instance) {
      if (instance) {
        this.cmInstance = instance
        // 实例就绪时组件可能已处于深色主题，补一次
        this.applyEditorTheme()
      }
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
  /* 极小视口（如 1600x400）下不再撑破容器：高度由外部容器决定，最小 140px */
  min-height: 140px;
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
