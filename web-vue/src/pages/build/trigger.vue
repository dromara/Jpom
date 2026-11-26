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
    <TriggerUrlDialog
      v-if="id"
      :id="id"
      :api="triggerApi"
      :links="links"
      :empty-text="$t('i18n_475a349f32')"
      :empty-action-text="$t('i18n_a1a3a7d853')"
    >
      <!-- 执行构建：参数与队列说明（构建专属文案，留在业务页） -->
      <template #extra-1>
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
      </template>
      <!-- 查看当前状态：状态码说明 -->
      <template #extra-2>
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
      </template>
      <!-- 查看构建日志：参数说明 -->
      <template #extra-3>
        <a-alert :message="$t('i18n_947d983961')" type="warning" show-icon>
          <template #description>
            <ul>
              <li>{{ $t('i18n_178ad7e9bc') }}</li>
              <li>{{ $t('i18n_0215b91d97') }}</li>
            </ul>
          </template>
        </a-alert>
      </template>
    </TriggerUrlDialog>
  </div>
</template>
<script>
import { getTriggerUrl } from '@/api/build-info'

export default {
  name: 'BuildTrigger',
  props: {
    id: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      // 交给通用组件的取地址接口（组件内部会带 id / rest 参数调用）
      triggerApi: getTriggerUrl
    }
  },
  computed: {
    // 构建的 4 个地址：单个触发、批量触发、状态查询（POST 与 GET 带参数两种形态）、构建日志
    links() {
      const copyable = `(${this.$t('i18n_00a070c696')})`
      const query = (extra) => (res) => `?id=${res.data.id}&token=${res.data.token}${extra}`
      return [
        {
          label: this.$t('i18n_de78b73dab') + copyable,
          field: 'triggerBuildUrl',
          method: 'GET',
          group: '1',
          groupLabel: this.$t('i18n_3d3b918f49')
        },
        {
          label: this.$t('i18n_8d202b890c') + copyable,
          field: 'batchTriggerBuildUrl',
          method: 'POST',
          group: '1'
        },
        {
          label: this.$t('i18n_dcd72e6014') + copyable,
          field: 'batchBuildStatusUrl',
          method: 'GET',
          group: '2',
          groupLabel: this.$t('i18n_251a89efa9'),
          suffix: query('')
        },
        {
          label: this.$t('i18n_cac6ff1d82') + copyable,
          field: 'batchBuildStatusUrl',
          method: 'POST',
          group: '2'
        },
        {
          label: this.$t('i18n_2f5e885bc6') + copyable,
          field: 'buildLogUrl',
          method: 'GET',
          group: '3',
          groupLabel: this.$t('i18n_d3ded43cee'),
          suffix: query('&buildNumId=0')
        }
      ]
    }
  }
}
</script>
