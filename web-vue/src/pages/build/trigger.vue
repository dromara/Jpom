<template>
  <div>
    <div v-if="triggerVisible">
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1" type="card">
          <template #rightExtra>
            <a-tooltip :title="$t('i18n_01ad26f4a9')">
              <a-button type="primary" size="small" @click="resetTrigger">{{ $t('i18n_4b9c3271dc') }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$t('i18n_3d3b918f49')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('i18n_947d983961')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_314f5aca4e') }}</li>
                    <li>
                      {{ $t('i18n_e319a2a526') }}
                    </li>
                    <li>{{ $t('i18n_3a3c5e739b') }} BODY json： [ { "id":"1", "token":"a", "delay":"0" } ]</li>
                    <li>
                      {{ $t('i18n_3baa9f3d72') }}
                    </li>
                    <li>
                      {{ $t('i18n_dcc846e420') }} BODY json： [ { "id":"1", "token":"a",
                      "delay":"0","branchName":"test","branchTagName":"1.*","script":"mvn clean
                      package","resultDirFile":"/target/","webhook":"http://test.com/webhook" } ]
                    </li>
                    <li>
                      {{ $t('i18n_2adbfb41e9') }} useQueue=true
                      {{ $t('i18n_0c1e9a72b7') }}
                    </li>
                    <li>{{ $t('i18n_68a1faf6e2') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$t('i18n_de78b73dab')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerBuildUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.triggerBuildUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$t('i18n_8d202b890c')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.batchTriggerBuildUrl }">
                    <a-tag>POST</a-tag>
                    <span>{{ temp.batchTriggerBuildUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="2" :tab="$t('i18n_251a89efa9')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('i18n_947d983961')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_3c48d9b970') }}</li>
                    <li>{{ $t('i18n_c34f1dc2b9') }}</li>
                    <li>
                      <a-tag>No(0, "{{ $t('i18n_d30b8b0e43') }}")</a-tag>,
                      <a-tag>Ing(1, "{{ $t('i18n_32493aeef9') }}")</a-tag>,
                      <a-tag>Success(2, "{{ $t('i18n_7f3809d36b') }}")</a-tag>,
                      <a-tag>Error(3, "{{ $t('i18n_41298f56a3') }}")</a-tag>,
                      <a-tag>PubIng(4, "{{ $t('i18n_0baa0e3fc4') }}")</a-tag>,
                      <a-tag>PubSuccess(5, "{{ $t('i18n_2fff079bc7') }}")</a-tag>,
                      <a-tag>PubError(6, "{{ $t('i18n_250688d7c9') }}")</a-tag>,
                      <a-tag>Cancel(7, "{{ $t('i18n_b4fc1ac02c') }}")</a-tag>,
                    </li>
                  </ul>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$t('i18n_dcd72e6014')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.batchBuildStatusUrl2 }">
                    <a-tag>GET</a-tag>
                    <span>{{ temp.batchBuildStatusUrl2 }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$t('i18n_cac6ff1d82')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.batchBuildStatusUrl }">
                    <a-tag>POST</a-tag>
                    <span>{{ temp.batchBuildStatusUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="3" :tab="$t('i18n_d3ded43cee')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('i18n_947d983961')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_178ad7e9bc') }}</li>
                    <li>{{ $t('i18n_0215b91d97') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$t('i18n_2f5e885bc6')}(${$t('i18n_00a070c696')})`">
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
      <a-result :title="$t('i18n_475a349f32')">
        <template #extra>
          <a-button key="console" type="primary" @click="handleTrigger">
            {{ $t('i18n_a1a3a7d853') }}
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
