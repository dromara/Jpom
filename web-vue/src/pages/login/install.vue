<template>
  <defaultBg>
    <template #content>
      <a-card v-if="canInstall" :style="`${setpCurrent === 1 ? 'width: 60vw' : 'width: 550px'}`" hoverable>
        <template #title>
          {{ $t('pages.login.install.cd8e34c0') }}
          <a href="https://jpom.top" target="_blank">{{ $t('pages.login.install.10496d2') }}</a>
        </template>
        <a-steps :current="setpCurrent">
          <a-step
            :title="$t('pages.login.install.9a866c70')"
            status="process"
            :description="$t('pages.login.install.ed09be07')"
          >
            <template #icon>
              <user-outlined />
            </template>
          </a-step>
          <a-step :title="$t('pages.login.install.431078ce')" :description="$t('pages.login.install.496c5eb5')">
            <template #icon>
              <solution-outlined />
            </template>
          </a-step>
        </a-steps>
        <a-row type="flex" justify="center">
          <a-col v-if="setpCurrent === 0" :span="16">
            <a-card-meta :title="$t('pages.login.install.51094d43')">
              <template #description>
                <ol>
                  <li>{{ $t('pages.login.install.cf24b73a') }}</li>
                  <li style="color: red">
                    {{ $t('pages.login.install.56e34e7') }}
                    <b>admin{{ $t('pages.login.install.dfd58d9d') }}</b
                    >{{ $t('pages.login.install.70a1f38a') }}
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
                :rules="[{ required: true, message: $t('pages.login.install.fee12658') }]"
              >
                <a-input v-model:value="loginForm.userName" :placeholder="$t('pages.login.install.dc861290')" />
              </a-form-item>
              <a-form-item
                class="init-user-password"
                name="userPwd"
                :rules="[
                  { required: true, message: $t('pages.login.install.75eeeeaf') },
                  {
                    pattern: /^(?![\d]+$)(?![a-zA-Z]+$)(?![^\da-zA-Z]+$).{6,18}$/,
                    message: $t('pages.login.install.5f2f73f9')
                  }
                ]"
              >
                <a-input-password v-model:value="loginForm.userPwd" :placeholder="$t('pages.login.install.3562f10a')" />
              </a-form-item>
              <a-form-item>
                <a-button type="primary" html-type="submit" block :loading="loading">
                  {{ $t('pages.login.install.a93d849c') }}
                </a-button>
              </a-form-item>
            </a-form>
          </a-col>
          <a-col v-if="setpCurrent === 1" :span="22">
            <a-alert banner>
              <template #message>
                <ul class="maf-tips">
                  <li>{{ $t('pages.login.install.73ca3cd9') }}</li>
                  <li>{{ $t('pages.login.install.1dc0b420') }}</li>
                  <li>{{ $t('pages.login.install.94e139a1') }}</li>
                </ul>
              </template>
            </a-alert>
            <br />
            <a-row>
              <a-col :span="12">
                <a-form-item>
                  <h3 :id="$t('pages.login.install.f907f6d7')">{{ $t('pages.login.install.f907f6d7') }}</h3>
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
                  <a-form-item :label="$t('pages.login.install.e3ff2149')">
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
                    :label="$t('pages.login.install.3c14fe26')"
                    name="twoCode"
                    :rules="[
                      { required: true, message: $t('pages.login.install.e789cea') },
                      { pattern: /^\d{6}$/, message: $t('pages.login.install.389e70c5') }
                    ]"
                  >
                    <a-input v-model:value="mfaForm.twoCode" :placeholder="$t('pages.login.install.2cef177e')" />
                  </a-form-item>

                  <a-form-item>
                    <a-row type="flex" justify="center">
                      <a-col :span="10">
                        <a-space>
                          <a-button type="primary" html-type="submit" class="btn" :loading="loading">
                            {{ $t('pages.login.install.4f983817') }}
                          </a-button>
                          <a-button type="dashed" @click="handleIgnoreBindMfa">
                            {{ $t('pages.login.install.674bd7aa') }}
                          </a-button>
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
        <a-result
          status="warning"
          :title="$t('pages.login.install.56a72339')"
          :sub-title="$t('pages.login.install.e16a0388')"
        >
          <template #extra>
            <a-button type="primary" @click="goHome"> {{ $t('pages.login.install.97f5410c') }} </a-button>
          </template>
        </a-result>
      </div>
    </template>
  </defaultBg>
</template>
<script lang="ts" setup>
import { bindMfa } from '@/api/user/user'
import { MFA_APP_TIP_ARRAY } from '@/utils/const'
import sha1 from 'js-sha1'
import { checkSystem } from '@/api/install'
import { initInstall } from '@/api/install'
// import { onMounted, reactive, ref } from 'vue'

import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import defaultBg from '@/pages/layout/default-bg.vue'

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
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
    title: $t('pages.login.install.b22d55a0'),
    content: $t('pages.login.install.250da8d7'),
    okText: $t('pages.login.install.e5a2dcc3'),
    cancelText: $t('pages.login.install.33a5532c'),
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
