<template>
  <defaultBg>
    <template #content>
      <a-card v-if="canInstall" :style="`${setpCurrent === 1 ? 'width: 60vw' : 'width: 550px'}`" hoverable>
        <template #title>
          {{ $tl('p.initSystemAccount') }}
          <a href="https://jpom.top" target="_blank">更多开源说明</a>
        </template>
        <a-steps :current="setpCurrent">
          <a-step :title="$tl('p.initSystem')" status="process" :description="$tl('p.setSuperAdminAccount')">
            <template #icon>
              <user-outlined />
            </template>
          </a-step>
          <a-step :title="$tl('p.enableTwoFactorAuth')" :description="$tl('p.secureAccountWithTwoFactor')">
            <template #icon>
              <solution-outlined />
            </template>
          </a-step>
        </a-steps>
        <a-row type="flex" justify="center">
          <a-col v-if="setpCurrent === 0" :span="16">
            <a-card-meta :title="$tl('p.operationInstructions')">
              <template #description>
                <ol>
                  <li>{{ $tl('p.createAccountForFutureLogin') }}</li>
                  <li style="color: red">
                    {{ $tl('p.avoidCommonUsernames') }}
                    <b>admin{{ $tl('p.avoidNamesLike') }}</b
                    >{{ $tl('p.preventAccountLock') }}
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
              class="init-form"
              @finish="handleLogin"
            >
              <a-form-item
                class="init-user-name"
                name="userName"
                :rules="[{ required: true, message: $tl('p.inputAccountName') }]"
              >
                <a-input v-model:value="loginForm.userName" :placeholder="$tl('p.accountName')" />
              </a-form-item>
              <a-form-item
                class="init-user-password"
                name="userPwd"
                :rules="[
                  { required: true, message: $tl('p.inputPassword') },
                  {
                    pattern: /^(?![\d]+$)(?![a-zA-Z]+$)(?![^\da-zA-Z]+$).{6,18}$/,
                    message: $tl('p.passwordRules')
                  }
                ]"
              >
                <a-input-password v-model:value="loginForm.userPwd" :placeholder="$tl('p.passwordFormat')" />
              </a-form-item>
              <a-form-item>
                <a-button type="primary" html-type="submit" block :loading="loading">
                  {{ $tl('p.createAccount') }}
                </a-button>
              </a-form-item>
            </a-form>
          </a-col>
          <a-col v-if="setpCurrent === 1" :span="22">
            <a-alert banner>
              <template #message>
                <ul class="maf-tips">
                  <li>{{ $tl('p.secureWithTwoFactor') }}</li>
                  <li>{{ $tl('p.saveQRCode') }}</li>
                  <li>{{ $tl('p.bindWithApp') }}</li>
                </ul>
              </template>
            </a-alert>
            <br />
            <a-row>
              <a-col :span="12">
                <a-form-item>
                  <h3 :id="$tl('c.twoFactorAuthApp')">{{ $tl('c.twoFactorAuthApp') }}</h3>
                  <p v-for="(html, index) in MFA_APP_TIP_ARRAY" :key="index" v-html="html" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form
                  :model="mfaForm"
                  :label-col="{ span: 5 }"
                  :wrapper-col="{ span: 18 }"
                  class="init-form"
                  @finish="handleMfaSure"
                >
                  <a-form-item :label="$tl('p.qrCode')">
                    <a-qrcode :value="qrCode.value" :status="qrCode.value ? 'active' : 'loading'" />
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
                    :label="$tl('p.verificationCode')"
                    name="twoCode"
                    :rules="[
                      { required: true, message: $tl('p.inputTwoFactorCode') },
                      { pattern: /^\d{6}$/, message: $tl('p.sixDigitCode') }
                    ]"
                  >
                    <a-input v-model:value="mfaForm.twoCode" :placeholder="$tl('p.twoFactorCode')" />
                  </a-form-item>

                  <a-form-item>
                    <a-row type="flex" justify="center">
                      <a-col :span="10">
                        <a-space>
                          <a-button type="primary" html-type="submit" class="btn" :loading="loading">
                            {{ $tl('p.confirmBinding') }}
                          </a-button>
                          <a-button type="dashed" @click="handleIgnoreBindMfa"> {{ $tl('p.ignoreBinding') }} </a-button>
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
        <a-result status="warning" :title="$tl('p.cannotInit')" :sub-title="$tl('p.systemAlreadyInitialized')">
          <template #extra>
            <a-button type="primary" @click="goHome"> {{ $tl('p.goBackToHome') }} </a-button>
          </template>
        </a-result>
      </div>
    </template>
  </defaultBg>
</template>
<script setup lang="ts">
import { bindMfa } from '@/api/user/user'
import { MFA_APP_TIP_ARRAY } from '@/utils/const'
import sha1 from 'js-sha1'
import { checkSystem } from '@/api/install'
import { initInstall } from '@/api/install'
// import { onMounted, reactive, ref } from 'vue'

import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import defaultBg from '@/pages/layout/default-bg.vue'

import { useI18nPage } from '@/i18n/hooks/useI18nPage'
const { $tl } = useI18nPage('pages.login.install')
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
  value: ''
  // size: 120
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
    title: $tl('p.systemPrompt'),
    content: $tl('p.confirmIgnoreTwoFactor'),
    okText: $tl('p.confirmAction'),
    cancelText: $tl('p.cancelAction'),
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
<style scoped></style>
