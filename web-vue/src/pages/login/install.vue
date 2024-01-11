<template>
  <defaultBg #content>
    <a-card v-if="canInstall" :style="`${setpCurrent === 1 ? 'width: 60vw' : 'width: 550px'}`" hoverable>
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
                <li style="color: red">
                  不建议使用常用名称如
                  <b>admin、root、manager</b
                  >等常用用户名，避免被其他用户有意或者无意操作造成登录失败次数过多从而超级管理员账号被异常锁定
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
            <a-col :span="12">
              <a-form-item>
                <h3 id="两步验证应用">两步验证应用</h3>
                <p v-for="(html, index) in MFA_APP_TIP_ARRAY" :key="index" v-html="html" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form
                :model="mfaForm"
                :label-col="{ span: 5 }"
                :wrapper-col="{ span: 18 }"
                @finish="handleMfaSure"
                class="init-form"
              >
                <a-form-item label="二维码">
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
      <a-result status="warning" title="不能初始化" sub-title="当前系统已经初始化过啦,不能重复初始化">
        <template #extra>
          <a-button type="primary" @click="goHome"> 回到首页 </a-button>
        </template>
      </a-result>
    </div>
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
<style scoped></style>
