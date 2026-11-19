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
  <div>
    <log-view1
      :ref="`logView`"
      :title-name="$t('i18n_c84ddfe8a6')"
      :visible="visible"
      @close="
        () => {
          $emit('close')
        }
      "
    />
  </div>
</template>
<script>
import { scriptLog } from '@/api/node-other'
import LogView1 from '@/components/logView'
export default {
  components: {
    LogView1
  },
  props: {
    temp: {
      type: Object,
      default: () => ({})
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['close'],
  data() {
    return {
      logTimer: null,
      // logText: "loading...",
      line: 1
    }
  },
  beforeUnmount() {
    if (this.logTimer) {
      clearInterval(this.logTimer)
    }
  },
  mounted() {
    this.init()
  },
  methods: {
    init() {
      this.loadData()
      this.logTimer = setInterval(() => {
        this.loadData()
      }, 2000)
    },
    // 加载日志内容
    loadData() {
      // 加载构建日志
      const params = {
        executeId: this.temp.id,
        id: this.temp.scriptId,
        nodeId: this.temp.nodeId,
        line: this.line
      }
      scriptLog(params).then((res) => {
        if (res.code === 200) {
          // 停止请求
          if (res.data.run === false) {
            clearInterval(this.logTimer)
          }
          // 更新日志
          // if (this.logText === "loading...") {
          //   this.logText = "";
          // }
          // let lines = res.data.dataLines;
          // lines.forEach((element) => {
          //   this.logText += `${element}\r\n`;
          // });
          this.$refs.logView.appendLine(res.data.dataLines)
          this.line = res.data.line
        }
      })
    }
  }
}
</script>
