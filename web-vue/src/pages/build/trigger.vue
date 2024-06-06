<template>
  <div>
    <div v-if="triggerVisible">
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1" type="card">
          <template #rightExtra>
            <a-tooltip :title="$t('pages.build.trigger.c70e60e6')">
              <a-button type="primary" size="small" @click="resetTrigger">{{
                $t('pages.build.trigger.da1d2343')
              }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$t('pages.build.trigger.ca9188db')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('pages.build.trigger.7feba425')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('pages.build.trigger.c16a1a40') }}</li>
                    <li>
                      {{ $t('pages.build.trigger.58634bfa') }}
                    </li>
                    <li>
                      {{ $t('pages.build.trigger.2f647b6c') }} BODY json： [ { "id":"1", "token":"a", "delay":"0" } ]
                    </li>
                    <li>
                      {{ $t('pages.build.trigger.b100eecf') }}
                    </li>
                    <li>
                      {{ $t('pages.build.trigger.c607de59') }} BODY json： [ { "id":"1", "token":"a",
                      "delay":"0","branchName":"test","branchTagName":"1.*","script":"mvn clean
                      package","resultDirFile":"/target/","webhook":"http://test.com/webhook" } ]
                    </li>
                    <li>
                      {{ $t('pages.build.trigger.5f0e8fe3') }} useQueue=true
                      {{ $t('pages.build.trigger.2809bf75') }}
                    </li>
                    <li>{{ $t('pages.build.trigger.b8b6a2e4') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                type="info"
                :message="`${$t('pages.build.trigger.2cd9ba62')}(${$t('pages.build.trigger.a5873c3e')})`"
              >
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerBuildUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.triggerBuildUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert
                type="info"
                :message="`${$t('pages.build.trigger.4bd083f4')}(${$t('pages.build.trigger.a5873c3e')})`"
              >
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.batchTriggerBuildUrl }">
                    <a-tag>POST</a-tag>
                    <span>{{ temp.batchTriggerBuildUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="2" :tab="$t('pages.build.trigger.d18c2cea')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('pages.build.trigger.7feba425')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('pages.build.trigger.b2f32fef') }}</li>
                    <li>{{ $t('pages.build.trigger.a96b1198') }}</li>
                    <li>
                      <a-tag>No(0, "{{ $t('pages.build.trigger.d0284f2b') }}")</a-tag>,
                      <a-tag>Ing(1, "{{ $t('pages.build.trigger.dbbd0a62') }}")</a-tag>,
                      <a-tag>Success(2, "{{ $t('pages.build.trigger.a2e56f86') }}")</a-tag>,
                      <a-tag>Error(3, "{{ $t('pages.build.trigger.7cc0beda') }}")</a-tag>,
                      <a-tag>PubIng(4, "{{ $t('pages.build.trigger.19653803') }}")</a-tag>,
                      <a-tag>PubSuccess(5, "{{ $t('pages.build.trigger.5f7b06e9') }}")</a-tag>,
                      <a-tag>PubError(6, "{{ $t('pages.build.trigger.50ad1198') }}")</a-tag>,
                      <a-tag>Cancel(7, "{{ $t('pages.build.trigger.65add658') }}")</a-tag>,
                    </li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                type="info"
                :message="`${$t('pages.build.trigger.2547e040')}(${$t('pages.build.trigger.a5873c3e')})`"
              >
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.batchBuildStatusUrl2 }">
                    <a-tag>GET</a-tag>
                    <span>{{ temp.batchBuildStatusUrl2 }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert
                type="info"
                :message="`${$t('pages.build.trigger.dd421d7f')}(${$t('pages.build.trigger.a5873c3e')})`"
              >
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.batchBuildStatusUrl }">
                    <a-tag>POST</a-tag>
                    <span>{{ temp.batchBuildStatusUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="3" :tab="$t('pages.build.trigger.6b0809e7')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('pages.build.trigger.7feba425')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('pages.build.trigger.5c5f1276') }}</li>
                    <li>{{ $t('pages.build.trigger.b8b189ad') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                type="info"
                :message="`${$t('pages.build.trigger.fee7ed9b')}(${$t('pages.build.trigger.a5873c3e')})`"
              >
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
      <a-result :title="$t('pages.build.trigger.2dfb8e28')">
        <template #extra>
          <a-button key="console" type="primary" @click="handleTrigger">
            {{ $t('pages.build.trigger.e8fee578') }}
          </a-button>
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
