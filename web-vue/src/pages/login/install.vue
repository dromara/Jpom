<template>
  <defaultBg>
    <template #content>
      <a-card v-if="canInstall" :style="`${setpCurrent === 1 ? 'width: 60vw' : 'width: 550px'}`" hoverable>
        <template #title>
          {{ $t('i18n_c2f11fde3a') }}
          <a href="https://jpom.top" target="_blank">{{ $t('i18n_620efec150') }}</a>
        </template>
        <a-steps :current="setpCurrent">
          <a-step :title="$t('i18n_b5c291805e')" status="process" :description="$t('i18n_3904bfe0db')">
            <template #icon>
              <user-outlined />
            </template>
          </a-step>
          <a-step :title="$t('i18n_6f9193ac80')" :description="$t('i18n_f0db5d58cb')">
            <template #icon>
              <solution-outlined />
            </template>
          </a-step>
        </a-steps>
        <a-row type="flex" justify="center">
          <a-col v-if="setpCurrent === 0" :span="16">
            <a-card-meta :title="$t('i18n_08ab230290')">
              <template #description>
                <ol>
                  <li>{{ $t('i18n_2953a9bb97') }}</li>
                  <li style="color: red">
                    {{ $t('i18n_e09d0d8c41') }}
                    <b>admin{{ $t('i18n_cb93a1f4a5') }}</b
                    >{{ $t('i18n_2b788a077e') }}
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
                :rules="[{ required: true, message: $t('i18n_ea7fbabfa1') }]"
              >
                <a-input v-model:value="loginForm.userName" :placeholder="$t('i18n_fec6151b49')" />
              </a-form-item>
              <a-form-item
                class="init-user-password"
                name="userPwd"
                :rules="[
                  { required: true, message: $t('i18n_e39ffe99e9') },
                  {
                    pattern: /^(?![\d]+$)(?![a-zA-Z]+$)(?![^\da-zA-Z]+$).{6,18}$/,
                    message: $t('i18n_974be6600d')
                  }
                ]"
              >
                <a-input-password v-model:value="loginForm.userPwd" :placeholder="$t('i18n_efafd0cbd4')" />
              </a-form-item>
              <a-form-item>
                <a-button type="primary" html-type="submit" block :loading="loading">
                  {{ $t('i18n_94d4fcca1b') }}
                </a-button>
              </a-form-item>
            </a-form>
          </a-col>
          <a-col v-if="setpCurrent === 1" :span="22">
            <a-alert banner>
              <template #message>
                <ul class="maf-tips">
                  <li>{{ $t('i18n_449fa9722b') }}</li>
                  <li>{{ $t('i18n_0ac9e3e675') }}</li>
                  <li>{{ $t('i18n_8c24b5e19c') }}</li>
                </ul>
              </template>
            </a-alert>
            <br />
            <a-row>
              <a-col :span="12">
                <a-form-item>
                  <h3 :id="$t('i18n_ceffe5d643')">{{ $t('i18n_ceffe5d643') }}</h3>
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
                  <a-form-item :label="$t('i18n_22b03c024d')">
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
                    :label="$t('i18n_983f59c9d4')"
                    name="twoCode"
                    :rules="[
                      { required: true, message: $t('i18n_7e866fece6') },
                      { pattern: /^\d{6}$/, message: $t('i18n_da1abf0865') }
                    ]"
                  >
                    <a-input v-model:value="mfaForm.twoCode" :placeholder="$t('i18n_3f18d14961')" />
                  </a-form-item>

                  <a-form-item>
                    <a-row type="flex" justify="center">
                      <a-col :span="10">
                        <a-space>
                          <a-button type="primary" html-type="submit" class="btn" :loading="loading">
                            {{ $t('i18n_b7cfa07d78') }}
                          </a-button>
                          <a-button type="dashed" @click="handleIgnoreBindMfa">
                            {{ $t('i18n_c0d5d68f5f') }}
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
        <a-result status="warning" :title="$t('i18n_65cf4248a8')" :sub-title="$t('i18n_70a6bc1e94')">
          <template #extra>
            <a-button type="primary" @click="goHome"> {{ $t('i18n_0bbc7458b4') }} </a-button>
          </template>
        </a-result>
      </div>
    </template>
  </defaultBg>
</template>
<script lang="ts" setup>
import { bindMfa } from '@/api/user/user'
import { MFA_APP_TIP_ARRAY } from '@/utils/const-i18n'
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
    title: $t('i18n_c4535759ee'),
    content: $t('i18n_dc39b183ea'),
    okText: $t('i18n_e83a256e4f'),
    cancelText: $t('i18n_625fb26b4b'),
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
