<template>
  <div>
    <div v-if="triggerVisible">
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1" type="card">
          <template #rightExtra>
            <a-tooltip :title="$tl('p.resetTriggerTokenInfo')">
              <a-button type="primary" size="small" @click="resetTrigger">{{ $tl('p.reset') }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$tl('p.executeBuild')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$tl('c.warmPrompt')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $tl('p.warmPrompt1') }}</li>
                    <li>
                      {{ $tl('p.warmPrompt2') }}
                    </li>
                    <li>{{ $tl('p.warmPrompt3') }} BODY json： [ { "id":"1", "token":"a", "delay":"0" } ]</li>
                    <li>
                      {{ $tl('p.warmPrompt4') }}
                    </li>
                    <li>
                      {{ $tl('p.warmPrompt5') }} BODY json： [ { "id":"1", "token":"a",
                      "delay":"0","branchName":"test","branchTagName":"1.*","script":"mvn clean
                      package","resultDirFile":"/target/","webhook":"http://test.com/webhook" } ]
                    </li>
                    <li>
                      {{ $tl('p.warmPrompt6') }} useQueue=true
                      {{ $tl('p.warmPrompt7') }}
                    </li>
                    <li>{{ $tl('p.warmPrompt8') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$tl('p.singleTriggerAddress')}(${$tl('c.copyByClick')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerBuildUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.triggerBuildUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$tl('p.batchTriggerAddress')}(${$tl('c.copyByClick')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.batchTriggerBuildUrl }">
                    <a-tag>POST</a-tag>
                    <span>{{ temp.batchTriggerBuildUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="2" :tab="$tl('p.viewCurrentStatus')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$tl('c.warmPrompt')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $tl('p.batchBuildParamsBodyJson') }}</li>
                    <li>{{ $tl('p.idAndTokenSameAsTriggerBuild') }}</li>
                    <li>
                      <a-tag>No(0, "{{ $tl('p.notBuilt') }}")</a-tag>, <a-tag>Ing(1, "{{ $tl('p.building') }}")</a-tag>,
                      <a-tag>Success(2, "{{ $tl('p.buildEnded') }}")</a-tag>,
                      <a-tag>Error(3, "{{ $tl('p.buildFailed') }}")</a-tag>,
                      <a-tag>PubIng(4, "{{ $tl('p.publishing') }}")</a-tag>,
                      <a-tag>PubSuccess(5, "{{ $tl('p.publishSuccess') }}")</a-tag>,
                      <a-tag>PubError(6, "{{ $tl('p.publishFailed') }}")</a-tag>,
                      <a-tag>Cancel(7, "{{ $tl('p.cancelBuild') }}")</a-tag>,
                    </li>
                  </ul>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$tl('p.getSingleBuildStatusAddress')}(${$tl('c.copyByClick')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.batchBuildStatusUrl2 }">
                    <a-tag>GET</a-tag>
                    <span>{{ temp.batchBuildStatusUrl2 }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$tl('p.batchGetBuildStatusAddress')}(${$tl('c.copyByClick')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.batchBuildStatusUrl }">
                    <a-tag>POST</a-tag>
                    <span>{{ temp.batchBuildStatusUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="3" :tab="$tl('p.viewBuildLog')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$tl('c.warmPrompt')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $tl('p.idAndTokenAndBuildNumIdSameAsTriggerBuild') }}</li>
                    <li>{{ $tl('p.replaceBuildNumIdAccordingToActualSituation') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$tl('p.getSingleBuildLogAddress')}(${$tl('c.copyByClick')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.buildLogUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.buildLogUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
        </a-tabs>
      </a-form>
    </div>
    <template v-else>
      <a-result :title="$tl('p.noTriggerGeneratedForCurrentBuild')">
        <template #extra>
          <a-button key="console" type="primary" @click="handleTrigger"> {{ $tl('p.generateNow') }} </a-button>
        </template>
      </a-result>
    </template>
  </div>
</template>

<script>
import { getTriggerUrl } from '@/api/build-info'
export default {
  props: {
    id: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      temp: {},

      triggerVisible: false
    }
  },
  created() {
    if (this.id) {
      this.handleTrigger()
    }
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.build.trigger.${key}`, ...args)
    },
    // 触发器
    handleTrigger() {
      this.temp = {}

      getTriggerUrl({
        id: this.id
      }).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res)
          this.triggerVisible = true
        }
      })
    },
    // 重置触发器
    resetTrigger() {
      getTriggerUrl({
        id: this.id,
        rest: 'rest'
      }).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.fillTriggerResult(res)
        }
      })
    },
    fillTriggerResult(res) {
      this.temp.triggerBuildUrl = `${location.protocol}//${location.host}${res.data.triggerBuildUrl}`
      this.temp.batchTriggerBuildUrl = `${location.protocol}//${location.host}${res.data.batchTriggerBuildUrl}`
      this.temp.batchBuildStatusUrl = `${location.protocol}//${location.host}${res.data.batchBuildStatusUrl}`
      this.temp.buildLogUrl = `${location.protocol}//${location.host}${res.data.buildLogUrl}`
      // this.temp.id = res.data.id;
      // this.temp.token = res.data.token;
      this.temp.batchBuildStatusUrl2 = `${this.temp.batchBuildStatusUrl}?id=${res.data.id}&token=${res.data.token}`
      this.temp.buildLogUrl = `${this.temp.buildLogUrl}?id=${res.data.id}&token=${res.data.token}&buildNumId=0`
      this.temp = { ...this.temp }
    }
  }
}
</script>
