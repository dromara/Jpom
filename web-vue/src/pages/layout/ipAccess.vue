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
  <defaultBg>
    <template #content>
      <a-card hoverable class="card">
        <a-card-meta>
          <template #description>
            <a-result status="error" :title="$t('i18n_2245cf01a3')">
              <template #extra>
                <a-button> {{ $t('i18n_bb9ef827bf') }} </a-button>
              </template>
            </a-result>
          </template>
        </a-card-meta>
      </a-card>
    </template>
  </defaultBg>
</template>
<script>
import defaultBg from '@/pages/layout/default-bg.vue'
export default {
  components: {
    defaultBg
  },
  data() {
    return {}
  },
  methods: {}
}
</script>
