<!-- Jpom 为开源软件，请基于开源协议用于商业用途 -->
<template>
  <div>
    <a-alert type="warning" show-icon>
      <template #message>
        <a-space>
          <template #split> <a-divider type="vertical" /> </template>
          <a href="https://jpom.top/pages/legal-risk/" target="_blank">{{ $t('i18n_9f01272a10') }}<LinkOutlined /> </a>
          <a href="https://jpom.top" target="_blank">{{ $t('i18n_19fa0be4d2') }}<LinkOutlined /> </a>
        </a-space>
      </template>
      <template #description>
        <ul>
          <li>
            <div>
              <b style="color: red">{{ $t('i18n_f3947e6581') }}</b
              >，{{ $t('i18n_f30f1859ba') }} <b>logo{{ $t('i18n_cbcc87b3d4') }}</b
              >，{{ $t('i18n_2bb1967887') }}
              <div>{{ $t('i18n_c12ba6ff43') }}</div>
            </div>
          </li>
          <li>
            <div>
              <b style="color: red">{{ $t('i18n_c26e6aaabb') }}</b
              >，{{ $t('i18n_0b9d5ba772') }}
            </div>
          </li>
        </ul>
      </template>
    </a-alert>
    <a-tabs>
      <a-tab-pane key="0" :tab="$t('i18n_6f6ee88ec4')">
        <h2>Jpom {{ $t('i18n_a1c4a75c2d') }}</h2>

        <ul>
          <li>
            Star {{ $t('i18n_80198317ff') }} <a href="https://gitee.com/dromara/Jpom" target="_blank">Gitee</a> /
            <a href="https://github.com/dromara/Jpom" target="_blank">Github</a>
          </li>
          <li>{{ $t('i18n_49a9d6c7e6') }}</li>
          <li>{{ $t('i18n_de4cf8bdfa') }}</li>
          <li>
            {{ $t('i18n_7a93e0a6ae')
            }}<a href="https://jpom.top/pages/enterprise-service/" target="_blank">{{ $t('i18n_da89135649') }}</a>
          </li>
        </ul>
        <div></div>
        <a-card style="text-align: center">
          <template #title> {{ $t('i18n_aeb44d34e6') }} </template>
          <template #extra>
            <a href="https://jpom.top/pages/praise/" target="_blank">{{ $t('i18n_ef734bf850') }}</a>
          </template>
          <a-row :gutter="[16, 16]">
            <a-col :span="6" />
            <a-col :span="6">
              <a-card hoverable style="width: auto; max-width: 300px">
                <template #cover>
                  <a-image
                    :src="weixinQrcorde"
                    :preview="{
                      src: weixinPraiseQrcorde
                    }"
                    style="width: 90%; margin: 5%"
                  >
                  </a-image>
                </template>
                <template #actions>
                  <!-- <setting-outlined key="setting" />
                  <edit-outlined key="edit" />
                  <ellipsis-outlined key="ellipsis" /> -->
                  <a-card-meta :title="$t('i18n_55f01e138a')" :description="$t('i18n_0ee3ca5e88')"> </a-card-meta>
                </template>
              </a-card>
            </a-col>
            <a-col :span="6">
              <a-card hoverable style="width: auto; max-width: 300px">
                <template #cover>
                  <a-image
                    :src="alipayQrcorde"
                    :preview="{
                      src: alipayPraiseQrcorde
                    }"
                    style="width: 90%; margin: 5%"
                  >
                  </a-image>
                  <!-- <img alt="alipay" :src="alipayQrcorde" style="width: 90%; margin: 5%" /> -->
                </template>
                <template #actions>
                  <a-card-meta :title="$t('i18n_a1da57ab69')" :description="$t('i18n_2331a990aa')"> </a-card-meta>

                  <!-- <setting-outlined key="setting" />
                <edit-outlined key="edit" />
                <ellipsis-outlined key="ellipsis" /> -->
                </template>
                <!-- <a-divider style="margin: 0" dashed /> -->
              </a-card>
            </a-col>
          </a-row>
        </a-card>
      </a-tab-pane>
      <a-tab-pane key="3" :tab="$t('i18n_0cbf83cc07')">
        <div>
          <h2 style="display: inline">{{ $t('i18n_a952ba273f') }}</h2>
          {{ $t('i18n_f712d3d040') }}<a-tag>{{ $t('i18n_0ce54ecc25') }}</a-tag
          ><a-tag>{{ $t('i18n_da89135649') }}</a-tag
          ><a-tag>{{ $t('i18n_92dde4c02b') }}</a-tag
          ><a-tag>{{ $t('i18n_712cdd7984') }}</a-tag>
          <a-tag>{{ $t('i18n_73f798a129') }}</a-tag>
        </div>

        <ul>
          <li>
            {{ $t('i18n_da4495b1b4')
            }}<a-typography-paragraph
              style="display: inline"
              :copyable="{ tooltip: false, text: 'bwcx_jzy@dromara.org' }"
            >
              <a target="_blank" href="mailto:bwcx_jzy@dromara.org">bwcx_jzy@dromara.org</a>
            </a-typography-paragraph>
          </li>
          <li>
            {{ $t('i18n_da671a4d16')
            }}<a-typography-paragraph style="display: inline" :copyable="{ tooltip: false, text: 'jpom66' }">
              <a href="https://jpom.top/pages/praise/join/" target="_blank"> jpom66</a>
            </a-typography-paragraph>
          </li>
        </ul>

        <a-card :title="$t('i18n_bdd87b63a6')">
          <div style="text-align: center">
            <a-image width="400px" :src="weixQrcodeJpom66" :preview="true"> </a-image>
          </div>
          <!-- <a-qrcode
            :size="qrCodeSize"
            :icon-size="qrCodeSize / 4"
            error-level="H"
            value="https://u.wechat.com/MP_PrhfdwmlBhmKp35BloEw"
            :icon="jpomLogo"
          /> -->
        </a-card>
      </a-tab-pane>
      <a-tab-pane key="1" :tab="$t('i18n_21dd8f23b4')">
        <pre style="white-space: pre-wrap">{{ licenseText }}</pre>
      </a-tab-pane>
      <a-tab-pane key="2" :tab="$t('i18n_7fb5bdb690')">
        <h1>Jpom {{ $t('i18n_4dc781596b') }}</h1>
        <a-list size="small" bordered :data-source="thankDependency">
          <template #renderItem="{ item }">
            <a-list-item>
              <div>
                <a-space>
                  <b>{{ item.name }}</b>

                  <a v-if="item.link" :href="item.link" target="_blank"> {{ item.link }}<LinkOutlined /></a>
                  <template v-if="item.license">
                    <template v-if="typeof item.license === 'string'">
                      <a-tag>{{ item.license }}</a-tag>
                    </template>
                    <template v-else>
                      <a-tag v-for="(licenseItem, index) in item.license" :key="index">{{ licenseItem }}</a-tag>
                    </template>
                  </template>
                </a-space>
              </div>
            </a-list-item>
          </template>
          <template #header>
            <div>{{ $t('i18n_791b6d0e62') }}</div>
          </template>
          <template #footer>
            <div>{{ $t('i18n_ce1ecd8a5b') }}</div>
          </template>
        </a-list></a-tab-pane
      >
    </a-tabs>
  </div>
</template>
<script lang="ts" setup>
// 擅自修改或者删除版权信息有法律风险，请尊重开源协议，不要擅自修改版本信息，否则可能承担法律责任。
import { getLicense, getThankDependency } from '@/api/about'
import alipayQrcorde from '@/assets/images/qrcode/alipay-small.png'
import weixinQrcorde from '@/assets/images/qrcode/weixin-small.png'
import alipayPraiseQrcorde from '@/assets/images/qrcode/alipay-praise.jpg'
import weixinPraiseQrcorde from '@/assets/images/qrcode/weixin-praise.jpg'
import weixQrcodeJpom66 from '@/assets/images/qrcode/weix-qrcode-jpom66.jpg'
import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
// import jpomLogo from '@/assets/images/jpom.svg'
const licenseText = ref('')

const thankDependency = ref([])

// const qrCodeSize = ref(200)

onMounted(() => {
  getLicense().then((res) => {
    if (res.code === 200) {
      licenseText.value = res.data
    }
  })

  getThankDependency().then((res) => {
    if (res.code === 200) {
      thankDependency.value = res.data || []
    }
  })
})
</script>
