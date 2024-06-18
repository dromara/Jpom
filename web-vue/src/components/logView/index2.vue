<template>
  <div
    :style="{
      marginTop: marginTop,
      minHeight: height,
      height: height
    }"
  >
    <div class="log-filter">
      <a-row type="flex" align="middle">
        <a-col>
          <slot name="before"></slot>
        </a-col>

        <a-col v-if="extendBar" style="padding-left: 10px">
          <a-space>
            <a-tooltip :title="$t('i18n_65f66dfe97')">
              <a-button type="primary" size="small" @click="clearLogCache"
                ><DeleteOutlined />{{ $t('i18n_288f0c404c') }}</a-button
              >
            </a-tooltip>
            <!-- <a-tooltip title="内容超过边界自动换行">
                  <a-switch v-model="temp.wordBreak" checked-children="自动换行" un-checked-children="不换行" @change="onChange" />
                </a-tooltip> -->
            <a-tooltip :title="$t('i18n_0693e17fc1')">
              <a-switch
                v-model:checked="temp.logScroll"
                :checked-children="$t('i18n_e0ce74fcac')"
                :un-checked-children="$t('i18n_18b34cf50d')"
                @change="onChange"
              />
            </a-tooltip>
          </a-space>
        </a-col>
      </a-row>
    </div>
    <!-- <pre class="log-view" :id="`${this.id}`" :style="`height:${this.height}`">{{ defText }}</pre> -->
    <viewPre ref="viewPre" :height="`calc(${height} - 35px - 20px)`" :config="temp"></viewPre>
  </div>
</template>
<script>
import viewPre from './view-pre.vue'

export default {
  // name: 'LogView',
  components: {
    viewPre
  },
  props: {
    height: {
      type: String,
      default: '50vh'
    },
    marginTop: {
      type: String,
      default: '0'
    },
    extendBar: {
      type: Boolean,
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
  computed: {
    regModifier() {
      return this.regModifiers.join('')
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
  padding: 0 10px;
  padding-top: 0;
  padding-bottom: 10px;
  line-height: 0;
}
:deep(.ant-checkbox-group-item) {
  display: flex;
  align-items: center;
}
</style>
