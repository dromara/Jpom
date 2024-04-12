<!-- Jpom 为开源软件，请基于开源协议用于商业用途 -->

<!-- 开源不等同于免费，如果您基于 Jpom 二次开发修改了 logo、名称、版权等，请找我们授权，否则会有法律风险。
  我们有权利追诉破坏开源并因此获利的团队个人的全部违法所得，也欢迎给我们提供侵权线索。 -->

<!-- 二次修改不可删除或者修改版权，否则可能承担法律责任 -->

<!-- 擅自修改或者删除版权信息有法律风险，请尊重开源协议，不要擅自修改版本信息，否则可能承担法律责任。 -->

<template>
  <div>
    <a-alert type="warning" show-icon>
      <template #message>
        <a-space>
          <template #split> <a-divider type="vertical" /> </template>
          <a href="https://jpom.top/pages/legal-risk/" target="_blank"> 法律风险<LinkOutlined /> </a>
          <a href="https://jpom.top" target="_blank"> 官方文档<LinkOutlined /> </a>
        </a-space>
      </template>
      <template #description>
        <ul>
          <li>
            <div>
              <b style="color: red">开源不等同于免费</b>，如果您基于 Jpom 二次开发修改了
              <b>logo、名称、版权等</b>，请找我们授权，否则会有法律风险。
              <div>我们有权利追诉破坏开源并因此获利的团队个人的全部违法所得，也欢迎给我们提供侵权线索。</div>
            </div>
          </li>
          <li>
            <div>
              <b style="color: red">擅自修改或者删除版权信息有法律风险</b
              >，请尊重开源协议，不要擅自修改版本信息，否则可能承担法律责任。
            </div>
          </li>
        </ul>
      </template>
    </a-alert>
    <a-tabs>
      <a-tab-pane key="0" tab="支持开源">
        <h2>Jpom 是一款开源软件您使用这个项目并感觉良好，或是想支持我们继续开发，您可以通过如下方式支持我们：</h2>

        <ul>
          <li>
            Star 并向您的朋友推荐或分享： <a href="https://gitee.com/dromara/Jpom" target="_blank">Gitee</a> /
            <a href="https://github.com/dromara/Jpom" target="_blank">Github</a>
          </li>
          <li>通过以下二维码进行一次性捐款赞助，请作者喝一杯咖啡☕️</li>
          <li>付费加入我们的技术交流群优先解答您所有疑问</li>
          <li>
            选择企业版本或者购买授权：<a href="https://jpom.top/pages/enterprise-service/" target="_blank">企业服务</a>
          </li>
        </ul>
        <div></div>
        <a-card style="text-align: center">
          <template #title> 一次性捐款赞助 </template>
          <template #extra>
            <a href="https://jpom.top/pages/praise/" target="_blank">更多说明</a>
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
                  <a-card-meta title="微信赞赏" description="扫码赞赏支持开源项目长期发展"> </a-card-meta>
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
                  <a-card-meta title="支付宝转账" description="扫码转账支持开源项目长期发展"> </a-card-meta>

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
      <a-tab-pane key="3" tab="联系我们">
        <div>
          <h2 style="display: inline">联系时请备注来意</h2>
          备注示例：<a-tag>付费社群</a-tag><a-tag>企业服务</a-tag><a-tag>广告投放</a-tag><a-tag>合作咨询</a-tag>
          <a-tag>免费社群</a-tag>
        </div>

        <ul>
          <li>
            邮箱：<a-typography-paragraph
              style="display: inline"
              :copyable="{ tooltip: false, text: 'bwcx_jzy@dromara.org' }"
            >
              <a target="_blank" href="mailto:bwcx_jzy@dromara.org">bwcx_jzy@dromara.org</a>
            </a-typography-paragraph>
          </li>
          <li>
            微信：<a-typography-paragraph style="display: inline" :copyable="{ tooltip: false, text: 'jpom66' }">
              <a href="https://jpom.top/pages/praise/join/" target="_blank"> jpom66</a>
            </a-typography-paragraph>
          </li>
        </ul>

        <a-card title="微信二维码">
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
      <a-tab-pane key="1" tab="开源协议">
        <pre style="white-space: pre-wrap">{{ licenseText }}</pre>
      </a-tab-pane>
      <a-tab-pane key="2" tab="软件致谢">
        <h1>Jpom 中使用了如下开源软件，我们衷心感谢有了他们的开源 Jpom 才能更完善</h1>
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
            <div>排名按照字母 a-z 排序</div>
          </template>
          <template #footer>
            <div>还有更多相关依赖开源组件</div>
          </template>
        </a-list></a-tab-pane
      >
    </a-tabs>
  </div>
</template>
<script setup lang="ts">
// 擅自修改或者删除版权信息有法律风险，请尊重开源协议，不要擅自修改版本信息，否则可能承担法律责任。
import { getLicense, getThankDependency } from '@/api/about'
import alipayQrcorde from '@/assets/images/qrcode/alipay-small.png'
import weixinQrcorde from '@/assets/images/qrcode/weixin-small.png'
import alipayPraiseQrcorde from '@/assets/images/qrcode/alipay-praise.jpg'
import weixinPraiseQrcorde from '@/assets/images/qrcode/weixin-praise.jpg'
import weixQrcodeJpom66 from '@/assets/images/qrcode/weix-qrcode-jpom66.jpg'
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
