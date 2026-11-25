<!--

    Copyright (c) 2019 Of Him Code Technology Studio
    Jpom is licensed under Mulan PSL v2.
    You can use this software according to the terms and conditions of the Mulan PSL v2.
    You may obtain a copy of Mulan PSL v2 at:
    			http://license.coscl.org.cn/MulanPSL2
    THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
    See the Mulan PSL v2 for more details.

-->
<!--
  通用「触发器地址」组件：打开即请求 -> 渲染地址（GET/POST 标签 + 复制）-> 右上角重置 -> 重置后刷新展示。

  组件只负责「地址列表 + 复制 + 重置」；各业务自己的说明长文案（重置说明、参数说明、状态码说明）
  一律由父组件用插槽传入，避免把某个业务的专属话术固化进公共组件。

  links 元素：
    label       已翻译的文案（渲染为每一条 alert 的标题）
    field       响应 data 里的字段名（相对地址，组件拼成绝对地址）
    method      'GET' | 'POST'（标签文案）
    group       可选，分组标识：> 1 组时用页签展示，分组标题取该组第一项的 groupLabel
    groupLabel  可选，页签标题（已翻译）
    suffix      可选，string 或 (res) => string：拼在地址后面的查询串（如 ?id=xx&token=yy）
-->
<template>
  <div class="trigger-url-dialog">
    <template v-if="loaded">
      <a-tabs v-if="hasTabs" :default-active-key="firstTabKey" type="card">
        <template #rightExtra>
          <a-tooltip :title="$t('i18n_01ad26f4a9')">
            <a-button type="primary" size="small" data-testid="trigger-url-reset" @click="resetTrigger">
              {{ $t('i18n_4b9c3271dc') }}
            </a-button>
          </a-tooltip>
        </template>
        <a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.label">
          <a-space direction="vertical" style="width: 100%">
            <slot :name="'extra-' + tab.key" />
            <a-alert v-for="(link, index) in tab.links" :key="index" type="info" :message="link.label">
              <template #description>
                <a-typography-paragraph :copyable="{ tooltip: false, text: link.url }">
                  <a-tag>{{ link.method }}</a-tag>
                  <span>{{ link.url }}</span>
                </a-typography-paragraph>
              </template>
            </a-alert>
          </a-space>
        </a-tab-pane>
      </a-tabs>
      <div v-else>
        <div class="trigger-url-dialog__head">
          <span v-if="title" class="trigger-url-dialog__title">{{ title }}</span>
          <a-tooltip :title="$t('i18n_01ad26f4a9')">
            <a-button type="primary" size="small" data-testid="trigger-url-reset" @click="resetTrigger">
              {{ $t('i18n_4b9c3271dc') }}
            </a-button>
          </a-tooltip>
        </div>
        <a-space direction="vertical" style="width: 100%">
          <slot />
          <a-alert v-for="(link, index) in flatLinks" :key="index" type="info" :message="link.label">
            <template #description>
              <a-typography-paragraph :copyable="{ tooltip: false, text: link.url }">
                <a-tag>{{ link.method }}</a-tag>
                <span>{{ link.url }}</span>
              </a-typography-paragraph>
            </template>
          </a-alert>
        </a-space>
      </div>
    </template>
    <a-result v-else-if="emptyText" :title="emptyText">
      <template #extra>
        <a-button v-if="emptyActionText" type="primary" @click="load">{{ emptyActionText }}</a-button>
      </template>
    </a-result>
  </div>
</template>
<script>
export default {
  name: 'TriggerUrlDialog',
  props: {
    // 业务数据 ID（构建 ID / 流水线 ID ...）
    id: {
      type: String,
      default: ''
    },
    // 取地址接口：(params) => Promise；组件会用 {id} 与 {id, rest: 'rest'} 两种参数调用
    api: {
      type: Function,
      required: true
    },
    // 要展示的地址列表，见文件头注释
    links: {
      type: Array,
      default: () => []
    },
    // 可选标题（页签模式标题由各页签自带，这里只用于无分组时的头部）
    title: {
      type: String,
      default: ''
    },
    // 还没取到地址时的空态文案（为空则不显示空态块）
    emptyText: {
      type: String,
      default: ''
    },
    // 空态下的重试按钮文案（为空则不显示按钮）
    emptyActionText: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loaded: false,
      // 与 links 下标一一对应的绝对地址
      urls: {}
    }
  },
  computed: {
    tabs() {
      const groups = []
      this.links.forEach((link, index) => {
        const key =
          link.group === undefined || link.group === null || link.group === '' ? 'default' : String(link.group)
        let group = groups.find((item) => item.key === key)
        if (!group) {
          group = { key, label: link.groupLabel || '', links: [] }
          groups.push(group)
        }
        group.links.push({ label: link.label, method: link.method, url: this.urls[index] || '' })
      })
      return groups
    },
    hasTabs() {
      return this.tabs.length > 1
    },
    firstTabKey() {
      return this.tabs.length ? this.tabs[0].key : ''
    },
    flatLinks() {
      return this.tabs.length ? this.tabs[0].links : []
    }
  },
  created() {
    if (this.id) {
      this.load()
    }
  },
  methods: {
    // 取地址：rest 为真表示重置（服务端会重新生成 token）
    request(rest) {
      const params = { id: this.id }
      if (rest) {
        params.rest = 'rest'
      }
      return this.api(params)
    },
    // 打开即请求
    load() {
      this.loaded = false
      this.urls = {}
      return this.request(false).then((res) => {
        if (res.code === 200) {
          this.fill(res)
        }
      })
    },
    // 重置并刷新展示
    resetTrigger() {
      return this.request(true).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.fill(res)
        }
      })
    },
    fill(res) {
      const data = res.data || {}
      const urls = {}
      this.links.forEach((link, index) => {
        let suffix = link.suffix
        if (typeof suffix === 'function') {
          suffix = suffix(res)
        }
        urls[index] = `${location.protocol}//${location.host}${data[link.field] || ''}${suffix || ''}`
      })
      this.urls = urls
      this.loaded = true
    }
  }
}
</script>
<style scoped>
.trigger-url-dialog__head {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.trigger-url-dialog__title {
  margin-right: auto;
  font-weight: 600;
}
</style>
