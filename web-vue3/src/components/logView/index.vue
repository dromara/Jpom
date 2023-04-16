<template>
  <div :style="`margin-top:${this.marginTop}`">
    <div class="log-filter">
      <template>
        <a-row type="flex" align="middle">
          <a-col>
            <slot name="before"></slot>
          </a-col>

          <a-col v-if="extendBar" style="padding-left: 10px">
            <a-space>
              <a-tooltip title="清空当前缓冲区内容">
                <a-button type="link" style="padding: 0" @click="clearLogCache" icon="delete"
                  ><span style="margin-left: 2px">清空</span></a-button
                >
              </a-tooltip>
              <a-tooltip title="内容超过边界自动换行">
                <a-switch
                  v-model="temp.wordBreak"
                  checked-children="自动换行"
                  un-checked-children="不换行"
                  @change="onChange"
                />
              </a-tooltip>
              <a-tooltip title="有新内容后是否自动滚动到底部">
                <a-switch
                  v-model="temp.logScroll"
                  checked-children="自动滚动"
                  un-checked-children="不滚动"
                  @change="onChange"
                />
              </a-tooltip>
              <!-- <a-dropdown>
                <a-button type="link" style="padding: 0" icon="setting"> 设置 <down-outlined /></a-button>
                <template #overlay>
                <a-menu >
                  <a-menu-item key="0"> </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="3"> </a-menu-item>
                </a-menu>
                </template>
              </a-dropdown> -->
            </a-space>
          </a-col>
        </a-row>
      </template>
    </div>
    <!-- <pre class="log-view" :id="`${this.id}`" :style="`height:${this.height}`">{{ defText }}</pre> -->
    <viewPre ref="viewPre" :height="height" :config="temp"></viewPre>
  </div>
</template>

<script>
import viewPre from './view-pre'

export default {
  name: 'LogView',
  components: {
    viewPre
    // VNodes: {
    //   functional: true,
    //   render: (h, ctx) => ctx.props.vnodes,
    // },
  },
  computed: {
    regModifier() {
      return this.regModifiers.join('')
    }
  },
  props: {
    height: {
      String,
      default: '50vh'
    },
    marginTop: {
      String,
      default: '0px'
    },
    extendBar: {
      Boolean,
      default: true
    }
  },
  data() {
    return {
      temp: {
        logScroll: true,
        // 自动换行
        wordBreak: false
      }
    }
  },
  mounted() {
    const cacehJson = localStorage.getItem('log-view-cache') || '{}'
    try {
      const cacheData = JSON.parse(cacehJson)
      this.temp = Object.assign({}, this.temp, cacheData)
    } catch (e) {
      console.error(e)
    }
  },
  methods: {
    appendLine(data) {
      this.$refs.viewPre.appendLine(data)
    },
    clearLogCache() {
      this.$refs.viewPre.clearLogCache()
    },
    onChange() {
      localStorage.setItem('log-view-cache', JSON.stringify(this.temp))
    }
  }
}
</script>

<style scoped>
.log-filter {
  /* margin-top: -22px; */
  /* margin-bottom: 10px; */
  padding: 0 10px;
  padding-top: 0;
  padding-bottom: 10px;
  line-height: 0;
}

/deep/ .ant-checkbox-group-item {
  display: flex;
  align-items: center;
}
</style>
