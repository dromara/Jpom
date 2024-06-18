<template>
  <defaultBg :show-footer="true">
    <template #content>
      <a-card class="login-card" hoverable>
        <a-card-meta :title="`${loginTitle}`" style="text-align: center" description="" />
        <br />
        <template v-if="action === 'login'">
          <a-form :model="loginForm" :label-col="{ span: 0 }" :wrapper-col="{ span: 24 }" @finish="handleLogin">
            <a-form-item name="loginName" :rules="[{ required: true, message: $t('i18n_08b1fa1304') }]">
              <a-input v-model:value="loginForm.loginName" autocomplete="true" :placeholder="$t('i18n_819767ada1')" />
            </a-form-item>
            <a-form-item name="userPwd" :rules="[{ required: true, message: $t('i18n_e39ffe99e9') }]">
              <a-input-password
                v-model:value="loginForm.userPwd"
                autocomplete="true"
                :placeholder="$t('i18n_a810520460')"
              />
            </a-form-item>
            <a-form-item
              v-if="!disabledCaptcha"
              name="code"
              :rules="[{ required: true, message: $t('i18n_d0c06a0df1') }]"
            >
              <a-row>
                <a-col :span="14">
                  <a-input v-model:value="loginForm.code" :placeholder="$t('i18n_983f59c9d4')" />
                </a-col>
                <a-col :offset="2" :span="8">
                  <div class="rand-code">
                    <img v-if="randCode" :src="randCode" @click="changeCode" />
                    <loading-outlined v-else />
                  </div>
                </a-col>
              </a-row>
            </a-form-item>
            <a-form-item :wrapper-col="{ span: 24 }">
              <a-button type="primary" html-type="submit" class="btn-login" :loading="loading">
                {{ $t('i18n_402d19e50f') }}
              </a-button>
            </a-form-item>
            <template v-if="enabledOauth2Provides.length">
              <a-divider>{{ $t('i18n_0f004c4cf7') }}</a-divider>
              <a-form-item :wrapper-col="{ span: 24 }">
                <a-space :size="20" wrap>
                  <template v-for="(item, index) in oauth2AllProvides">
                    <div v-if="enabledOauth2Provides.includes(item.key)" :key="index" class="oauth2-item">
                      <a-tooltip :title="item.name" @click="toOauth2Url(item.key)">
                        <img :alt="item.name" :src="item.img" />
                      </a-tooltip>
                    </div>
                  </template>
                  <!-- <div v-if="enabledOauth2Provides.includes('maxkey')" class="oauth2-item">
                    <a-tooltip title="maxkey" @click="toOauth2Url('maxkey')">
                      <img alt="maxkey" :src="maxkeyImg" />
                    </a-tooltip>
                  </div>
                  <div v-if="enabledOauth2Provides.includes('github')" class="oauth2-item">
                    <a-tooltip title="github 账号登录" @click="toOauth2Url('github')">
                      <img alt="github" :src="githubImg" />
                    </a-tooltip>
                  </div>
                  <div v-if="enabledOauth2Provides.includes('dingtalk')" class="oauth2-item">
                    <a-tooltip title="钉钉账号登录" @click="toOauth2Url('dingtalk')">
                      <img alt="dingtalk" :src="dingtalkImg" />
                    </a-tooltip>
                  </div>
                  <div v-if="enabledOauth2Provides.includes('feishu')" class="oauth2-item">
                    <a-tooltip title="飞书账号登录" @click="toOauth2Url('feishu')">
                      <img alt="dingtalk" :src="feishuImg" />
                    </a-tooltip>
                  </div>
                  <div v-if="enabledOauth2Provides.includes('mygitlab')" class="oauth2-item">
                    <a-tooltip title="自建 Gitlab 账号登录" @click="toOauth2Url('mygitlab')">
                      <img alt="mygitlab" :src="gitlabImg" />
                    </a-tooltip>
                  </div> -->
                </a-space>
              </a-form-item>
            </template>
          </a-form>
        </template>
        <template v-if="action === 'mfa'">
          <a-form
            ref="mfaDataForm"
            :label-col="{ span: 5 }"
            :wrapper-col="{ span: 19 }"
            :model="mfaData"
            @finish="handleMfa"
          >
            <a-form-item
              :label="$t('i18n_983f59c9d4')"
              name="mfaCode"
              :help="$t('i18n_5c4d3c836f')"
              :rules="[
                { required: true, message: $t('i18n_7e866fece6') },
                { pattern: /^\d{6}$/, message: $t('i18n_da1abf0865') }
              ]"
            >
              <a-input v-model:value="mfaData.mfaCode" :placeholder="$t('i18n_674e7808b5')" />
            </a-form-item>

            <a-button type="primary" html-type="submit" class="btn-login">
              {{ $t('i18n_e83a256e4f') }}
            </a-button>
          </a-form>
        </template>
      </a-card>
    </template>
  </defaultBg>
</template>
<script lang="ts" setup>
import { login, loginConfig, mfaVerify, oauth2Url, oauth2Login, loginRandCode } from '@/api/user/user'
import { checkSystem } from '@/api/install'
import sha1 from 'js-sha1'
import defaultBg from '@/pages/layout/default-bg.vue'
import maxkeyImg from '@/assets/images/maxkey.png'
import giteeImg from '@/assets/images/gitee.svg'
import dingtalkImg from '@/assets/images/dingtalk.svg'
import githubImg from '@/assets/images/github.svg'
import feishuImg from '@/assets/images/feishu.svg'
import gitlabImg from '@/assets/images/gitlab.svg'
// import weixinImg from '@/assets/images/weixin.svg'
import qyWeixinImg from '@/assets/images/qyweixin.svg'
import { useGuideStore } from '@/stores/guide'
import { Button } from 'ant-design-vue'

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()

const oauth2AllProvides = ref([
  {
    name: $t('i18n_4ba304e77a'),
    key: 'dingtalk',
    img: dingtalkImg
  },
  {
    name: $t('i18n_5516b3130c'),
    key: 'feishu',
    img: feishuImg
  },
  {
    name: $t('i18n_af3a9b6303'),
    key: 'wechat_enterprise',
    img: qyWeixinImg
  },
  {
    name: `gitee ${$t('i18n_efae7764ac')}`,
    key: 'gitee',
    img: giteeImg
  },
  {
    name: `maxkey ${$t('i18n_b6e8fb4106')}`,
    key: 'maxkey',
    img: maxkeyImg
  },
  {
    name: `github ${$t('i18n_efae7764ac')}`,
    key: 'github',
    img: githubImg
  },
  {
    name: $t('i18n_ab13dd3381'),
    key: 'mygitlab',
    img: gitlabImg
  }
])

interface IFormState {
  loginName: string
  userPwd: string
  code: string
}
const guideStore = useGuideStore()

const theme = computed(() => {
  return guideStore.getThemeView()
})

const router = useRouter()
const route = useRoute()

const loginTitle = ref($t('i18n_0de68f5626'))
const loginForm = reactive<IFormState>({
  loginName: '',
  userPwd: '',
  code: ''
})
const mfaData = reactive({
  mfaCode: '',
  token: ''
})
const loading = ref(false)
const action = ref<'mfa' | 'login'>('login')
const enabledOauth2Provides = ref<string[]>([])

const randCode = ref('')
// const dynamicBg = ref(localStorage.getItem('dynamicBg') === 'true')
const disabledCaptcha = ref(false)

// const backgroundImage = computed(() => {
//   const color =
//     theme.value === 'light' ? 'linear-gradient(#1890ff, #66a9c9)' : 'linear-gradient(rgb(38 46 55), rgb(27 33 36))'
//   // background: linear-gradient(#1890ff, #66a9c9);
//   return { background: color }
// })

// 检查是否需要初始化
const beginCheckSystem = () => {
  checkSystem().then((res) => {
    if (res.code !== 200) {
      $notification.warn({
        message: res.msg
      })
    }
    if (res.code === 999) {
      router.push('/prohibit-access')
    } else if (res.code === 222) {
      router.push('/install')
    }
    if (res.data?.loginTitle) {
      loginTitle.value = res.data.loginTitle
    }

    checkOauth2()
  })
}

const login_tip_key = 'login-tip'

const getLoginConfig = () => {
  loginConfig().then((res) => {
    if (res.data && res.data.demo) {
      const demo = res.data.demo
      const p = h('p', { innerHTML: demo.msg }, [])
      $notification.info({
        message: $t('i18n_947d983961'),
        description: h('div', {}, [p]),
        key: login_tip_key,
        duration: null
      })
      loginForm.loginName = demo.user
    }
    disabledCaptcha.value = res.data.disabledCaptcha
    enabledOauth2Provides.value = res.data?.oauth2Provides || []

    changeCode()
  })
}
// change Code
const changeCode = () => {
  if (disabledCaptcha.value) {
    return
  }
  loginRandCode({ theme: theme.value, t: new Date().getTime() }).then((res) => {
    if (res.code === 200) {
      randCode.value = res.data
      loginForm.code = ''
    }
  })
}
const jpomWindow_ = jpomWindow()

const parseOauth2Provide = () => {
  if (jpomWindow_.oauth2Provide === '<oauth2Provide>') {
    const pathname = location.pathname.substring(1)
    const pathArray = pathname.split('-')
    return pathArray[pathArray.length - 1]
    // console.log(location.pathname.substring(1))
  }
  return jpomWindow_.oauth2Provide
}

const checkOauth2 = () => {
  if (route.query.code) {
    loading.value = true
    oauth2Login({
      code: route.query.code,
      state: route.query.state,
      provide: parseOauth2Provide()
    })
      .then((res) => {
        // 删除参数，避免刷新页面 code 已经被使用提示错误信息
        let query = Object.assign({}, route.query)
        delete query.code, delete query.state
        router.replace({
          query: query
        })
        // 登录不成功，更新验证码
        if (res.code !== 200) {
          changeCode()
        } else {
          startDispatchLogin(res)
        }
      })
      .finally(() => {
        loading.value = false
      })
  }
}
// 跳转到第三方系统
const toOauth2Url = (provide: string) => {
  oauth2Url({ provide: provide }).then((res) => {
    if (res.code === 200 && res.data) {
      $message.loading({ content: $t('i18n_4c83203419'), key: 'oauth2', duration: 0 })
      location.href = res.data.toUrl
    }
  })
}
const startDispatchLogin = (res: any) => {
  $notification.success({
    message: res.msg
  })
  const existWorkspace = res.data.bindWorkspaceModels.find((item: any) => item.id === appStore().getWorkspaceId())
  if (existWorkspace) {
    // 缓存的还存在
    dispatchLogin(res.data)
  } else {
    // 之前的工作空间已经不存在,切换到当前列表的第一个
    // 还没有选择工作空间，默认选中第一个 用户加载菜单
    let firstWorkspace = res.data.bindWorkspaceModels[0]
    appStore()
      .changeWorkspace(firstWorkspace.id)
      .then(() => {
        dispatchLogin(res.data)
      })
  }
}
const useUserStore = userStore()

const dispatchLogin = (data: any) => {
  // 调用 store action 存储当前登录的用户名和 token
  useUserStore.login({ token: data.token, longTermToken: data.longTermToken }).then(() => {
    // 跳转主页面
    router.push({ path: '/' })
  })
}

const handleLogin = (values: IFormState) => {
  const params = {
    ...values,
    userPwd: sha1(loginForm.userPwd)
  }
  loading.value = true
  login(params)
    .then((res) => {
      if (res.code === 201) {
        action.value = 'mfa'
        mfaData.token = res.data.tempToken
        return
      }
      // 登录不成功，更新验证码
      if (res.code !== 200) {
        changeCode()
      } else {
        startDispatchLogin(res)
      }
    })
    .finally(() => {
      loading.value = false
    })
}

const handleMfa = () => {
  mfaVerify({
    token: mfaData.token,
    code: mfaData.mfaCode
  }).then((res) => {
    if (res.code === 201) {
      // 过期需要重新登录
      action.value = 'login'
      mfaData.token = ''
      mfaData.mfaCode = ''
    } else if (res.code === 200) {
      startDispatchLogin(res)
    }
  })
}

const tip_has_login_key = `tipHasLoginInfo`

const checkHasLoginInfo = () => {
  if (useUserStore.userInfo && useUserStore.getToken()) {
    const p = h(
      'p',
      {
        innerHTML: `${$t('i18n_cfbb3341d5')}<b>${useUserStore.userInfo.name || ''}</b> ${$t('i18n_17006d4d51')}`
      },
      []
    )
    $notification.open({
      message: $t('i18n_697d60299e'),
      description: h('div', {}, [p]),
      btn: () =>
        h(
          Button,
          {
            type: 'primary',
            size: 'small',
            onClick: () => {
              $notification.close(tip_has_login_key)
              router.push({ path: '/' })
            }
          },
          { default: () => $t('i18n_7653297de3') }
        ),
      key: tip_has_login_key,
      duration: null
    })
  } else {
    $notification.close(tip_has_login_key)
  }
}

const listener = () => {
  if (document.hidden || document.visibilityState === 'hidden') {
    //this.hidden()
  } else {
    checkHasLoginInfo()
  }
}

onMounted(() => {
  beginCheckSystem()

  getLoginConfig()
  checkHasLoginInfo()
  document.addEventListener('visibilitychange', listener)
  if (/^((?!chrome|android).)*safari/i.test(navigator.userAgent)) {
    window.addEventListener('pageshow', checkHasLoginInfo)
  }
})

onBeforeUnmount(() => {
  $notification.close(tip_has_login_key)
  $notification.close(login_tip_key)
  document.removeEventListener('visibilitychange', listener)
  window.removeEventListener('pageshow', checkHasLoginInfo)
})

// export default {
//   data() {
//     return {
//       loginForm: {
//         loginName: '',
//         userPwd: '',
//         code: '',
//       },
//       mfaData: {},
//       action: 'login',
//       randCode: 'randCode.png',
//       dynamicBg: localStorage.getItem('dynamicBg') === 'true',
//       loginTitle: '登录JPOM',
//       rules: {
//         loginName: [{ required: true, message: '请输入用户名' }],
//         userPwd: [{ required: true, message: '请输入密码' }],
//         code: [{ required: true, message: '请输入验证码' }],
//         mfaCode: [
//           { required: true, message: '请输入两步验证码' },
//           { pattern: /^\d{6}$/, message: '验证码 6 为纯数字' },
//         ],
//       },
//       disabledCaptcha: false,
//       enabledOauth2Provides: [],
//       maxkeyImg: require(`@/assets/images/maxkey.png`),
//       giteeImg: require(`@/assets/images/gitee.svg`),
//       githubImg: require(`@/assets/images/github.png`),
//     }
//   },
//   created() {
//     this.checkSystem()
//     //this.getBg();

//     this.changeCode()
//     this.getLoginConfig()
//   },
//   computed: {
//     ...mapGetters(['getWorkspaceId']),
//     backgroundImage: function () {
//       if (this.dynamicBg) {
//         return {
//           backgroundImage: `url(https://picsum.photos/${screen.width}/${screen.height}/?random)`,
//         }
//       }
//       return {}
//     },
//   },
//   methods: {
//     // Get background pic
//     // getBg() {},
//     //
//   },
// }
</script>
<style scoped>
.login-card {
  min-width: 380px;
  max-width: 400px;
  border-radius: 8px;
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.rand-code {
  width: 100%;
  height: 32px;
}

.rand-code img {
  width: 100%;
  height: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  display: inherit;
}

.btn-login {
  width: 100%;
  margin: 10px 0;
}

:deep(.ant-card-meta-title) {
  font-size: 30px;
}

:deep(.ant-card-body) {
  padding: 30px;
}

.oauth2-item {
  width: 40px;
  height: 40px;
}

.oauth2-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
