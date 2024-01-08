<template>
  <div class="init-wrapper">
    <svg
      width="100%"
      height="100%"
      viewBox="0 0 1440 500"
      stroke="none"
      stroke-width="1"
      fill="none"
      fill-rule="evenodd"
    >
      <g>
        <circle stroke="#13C2C2" cx="500" cy="-20" r="6"></circle>
        <circle fill-opacity="0.4" fill="#9EE6E6" cx="166" cy="76" r="8"></circle>
        <circle stroke="#13C2C2" cx="1165" cy="240" r="5"></circle>
        <circle stroke="#CED4D9" cx="1300" cy="10" r="8"></circle>
        <circle stroke="#ffffff" cx="1326.5" cy="180" r="6"></circle>
        <circle fill-opacity="0.4" fill="#9EE6E6" cx="944" cy="250" r="5"></circle>
      </g>
      <g>
        <path
          d="M1182.79367,448.230356 L1186.00213,453.787581 C1186.55442,454.744166 1186.22667,455.967347 1185.27008,456.519632 C1184.96604,456.695168 1184.62116,456.787581 1184.27008,456.787581 L1177.85315,456.787581 C1176.74858,456.787581 1175.85315,455.89215 1175.85315,454.787581 C1175.85315,454.436507 1175.94556,454.091619 1176.1211,453.787581 L1179.32957,448.230356 C1179.88185,447.273771 1181.10503,446.946021 1182.06162,447.498305 C1182.36566,447.673842 1182.61813,447.926318 1182.79367,448.230356 Z"
          stroke="#CED4D9"
        ></path>
        <path
          d="M1376.79367,204.230356 L1380.00213,209.787581 C1380.55442,210.744166 1380.22667,211.967347 1379.27008,212.519632 C1378.96604,212.695168 1378.62116,212.787581 1378.27008,212.787581 L1371.85315,212.787581 C1370.74858,212.787581 1369.85315,211.89215 1369.85315,210.787581 C1369.85315,210.436507 1369.94556,210.091619 1370.1211,209.787581 L1373.32957,204.230356 C1373.88185,203.273771 1375.10503,202.946021 1376.06162,203.498305 C1376.36566,203.673842 1376.61813,203.926318 1376.79367,204.230356 Z"
          stroke="#2F54EB"
        ></path>
      </g>
      <g>
        <rect stroke="#13C2C2" stroke-opacity="0.6" x="120" y="322" width="12" height="12" rx="1"></rect>
        <rect stroke="#CED4D9" x="108" y="1" width="9" height="9" rx="1"></rect>
      </g>
    </svg>
    <a-card
      v-if="canInstall"
      class="card-box"
      :style="`${setpCurrent === 1 ? 'width: 60vw' : 'width: 550px'}`"
      hoverable
      :bodyStyle="{ padding: '24px 0', overflow: 'auto' }"
    >
      <template #title>
        <a-steps :current="setpCurrent">
          <a-step title="初始化系统" status="process" description="设置一个超级管理员账号">
            <template #icon>
              <user-outlined />
            </template>
          </a-step>
          <a-step title="启用两步验证" description="开启两步验证使账号更安全">
            <template #icon>
              <solution-outlined />
            </template>
          </a-step>
        </a-steps>
      </template>

      <a-row type="flex" justify="center">
        <a-col :span="16" v-if="setpCurrent === 0">
          <a-card-meta title="初始化系统账户" style="textalign: center">
            <template #description>
              <ol>
                <li>您需要创建一个账户用以后续登录管理系统,请牢记超级管理员账号密码</li>
                <li>
                  不建议使用常用名称如
                  admin、root、manager等常用用户名，避免被其他用户有意或者无意操作造成登录失败次数过多从而超级管理员账号被异常锁定
                </li>
              </ol>
            </template>
          </a-card-meta>
          <br />
          <a-form
            :model="loginForm"
            name="login"
            :label-col="{ span: 0 }"
            :wrapper-col="{ span: 24 }"
            @finish="handleLogin"
            class="init-form"
          >
            <a-form-item class="init-user-name" name="userName" :rules="[{ required: true, message: '请输入账户名' }]">
              <a-input v-model:value="loginForm.userName" placeholder="账户名称" />
            </a-form-item>
            <a-form-item
              class="init-user-password"
              name="userPwd"
              :rules="[
                { required: true, message: '请输入密码' },
                {
                  pattern: /^(?![\d]+$)(?![a-zA-Z]+$)(?![^\da-zA-Z]+$).{6,18}$/,
                  message: '密码必须包含数字，字母，字符，且大于6位'
                }
              ]"
            >
              <a-input-password v-model:value="loginForm.userPwd" placeholder="密码（6-18位数字、字母、符号组合）" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" block :loading="loading"> 创建账号 </a-button>
            </a-form-item>
          </a-form>
        </a-col>
        <a-col :span="22" v-if="setpCurrent === 1">
          <a-alert banner>
            <template #message>
              <ul class="maf-tips">
                <li>为了考虑系统安全我们强烈建议超级管理员开启两步验证来确保账号的安全性</li>
                <li>绑定成功后将不再显示,强烈建议保存此二维码或者下面的 MFA key</li>
                <li>请使用应用扫码绑定令牌,然后输入验证码确认绑定才生效</li>
              </ul>
            </template>
          </a-alert>
          <br />
          <a-row>
            <a-col :span="22">
              <a-form-item>
                <h3 id="两步验证应用">两步验证应用</h3>
                <p v-for="(html, index) in MFA_APP_TIP_ARRAY" :key="index" v-html="html" />
              </a-form-item>
            </a-col>
            <a-divider type="vertical" />
            <a-col :span="20">
              <a-form
                :model="mfaForm"
                :label-col="{ span: 5 }"
                :wrapper-col="{ span: 18 }"
                @finish="handleMfaSure"
                class="init-form"
              >
                <a-form-item label="二维码" style="margin-bottom: 5px">
                  <div class="qrcode">
                    <qrcode-vue :value="qrCode.value" :size="qrCode.size" level="H" />
                  </div>
                </a-form-item>
                <a-form-item label="MFA key" name="mfa">
                  <a-input-group compact>
                    <a-input v-model:value="mfaForm.mfa" disabled style="width: calc(100% - 32px)"> </a-input>
                    <a-button style="padding: 4px 6px">
                      <a-typography-paragraph :copyable="{ text: mfaForm.mfa }"></a-typography-paragraph>
                    </a-button>
                  </a-input-group>
                </a-form-item>

                <a-form-item
                  label="验证码"
                  name="twoCode"
                  :rules="[
                    { required: true, message: '请输入两步验证码' },
                    { pattern: /^\d{6}$/, message: '验证码 6 为纯数字' }
                  ]"
                >
                  <a-input v-model:value="mfaForm.twoCode" placeholder="两步验证码" />
                </a-form-item>

                <a-form-item>
                  <a-row type="flex" justify="center">
                    <a-col :span="10">
                      <a-space>
                        <a-button type="primary" html-type="submit" class="btn" :loading="loading"> 确认绑定 </a-button>
                        <a-button type="dashed" @click="handleIgnoreBindMfa"> 忽略 </a-button>
                      </a-space>
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-form>
            </a-col>
          </a-row>
        </a-col>
      </a-row>
    </a-card>
    <div v-else>
      <a-result class="card-box" status="404" title="不能初始化" sub-title="当前系统已经初始化过啦,不能重复初始化">
        <template #extra>
          <a-button type="primary" @click="goHome"> 回到首页 </a-button>
        </template>
      </a-result>
    </div>
  </div>
</template>
<script setup lang="ts">
import { bindMfa } from '@/api/user/user'
import { MFA_APP_TIP_ARRAY } from '@/utils/const'
import sha1 from 'js-sha1'
import { checkSystem } from '@/api/install'
import { initInstall } from '@/api/install'
// import { onMounted, reactive, ref } from 'vue'
import QrcodeVue from 'qrcode.vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const router = useRouter()

const loginForm = reactive({
  userName: '',
  userPwd: ''
})
const mfaForm = reactive({
  twoCode: '',
  mfa: ''
})
const setpCurrent = ref(0)

const canInstall = ref(true)

const qrCode = reactive({
  value: '',
  size: 120
})

const loading = ref(false)

// login
const handleLogin = (values: any) => {
  const params = {
    ...values,
    userPwd: sha1(values.userPwd)
  }
  loading.value = true
  initInstall(params)
    .then((res) => {
      const userStore = useUserStore()
      const appStore = useAppStore()
      // 登录不成功，更新验证码
      if (res.code === 200) {
        $notification.success({
          message: res.msg
        })
        const tokenData = res.data.tokenData
        mfaForm.mfa = res.data.mfaKey
        setpCurrent.value = 1
        qrCode.value = res.data.url
        userStore.login({ token: tokenData.token, longTermToken: tokenData.longTermToken })

        const firstWorkspace = tokenData.bindWorkspaceModels[0]
        appStore.changeWorkspace(firstWorkspace.id)
      }
    })
    .finally(() => {
      loading.value = false
    })
}

const handleMfaSure = () => {
  loading.value = true
  bindMfa(mfaForm)
    .then((res) => {
      if (res.code === 200) {
        $notification.success({
          message: res.msg
        })
        // 跳转主页面;
        router.push({ path: '/' })
      }
    })
    .finally(() => {
      loading.value = false
    })
}

// 忽略 mfa
const handleIgnoreBindMfa = () => {
  $confirm({
    title: '系统提示',
    content: '确定要忽略绑定两步验证吗？强烈建议超级管理员开启两步验证来保证账号安全性',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      router.push({ path: '/' })
    }
  })
}

const goHome = () => {
  router.replace({ path: '/' })
}

onMounted(() => {
  checkSystem().then((res) => {
    if (res.code === 222) {
      canInstall.value = true
    } else {
      canInstall.value = false
    }
  })
})
</script>
<style scoped>
:global(.ant-steps-horizontal:not(.ant-steps-label-vertical) .ant-steps-item-description) {
  max-width: 172px;
  white-space: normal;
}

.maf-tips {
  color: var(--ant-error-color);
  margin-bottom: 0;
}

.init-wrapper {
  /* width: 100vw; */
  height: 100vh;
  background: linear-gradient(#1890ff, #66a9c9);
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 16px;
  overflow: auto;
}

.card-box {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  text-align: left;
  border-radius: 6px;
  padding: 4px;
}

p {
  margin-bottom: 0;
  line-height: 30px;
}

.qrcode {
  margin-left: 15px;
  width: 120px;
  height: 120px;
  margin-bottom: 20px;
}
</style>
