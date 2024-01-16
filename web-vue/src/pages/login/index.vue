<template>
  <defaultBg #content>
    <a-card class="login-card" hoverable>
      <a-card-meta :title="`${loginTitle}`" style="text-align: center" description="" />
      <br />
      <template v-if="action === 'login'">
        <a-form :model="loginForm" :label-col="{ span: 0 }" :wrapper-col="{ span: 24 }" @finish="handleLogin">
          <a-form-item name="loginName" :rules="[{ required: true, message: '请输入用户名' }]">
            <a-input autocomplete="true" v-model:value="loginForm.loginName" placeholder="用户名" />
          </a-form-item>
          <a-form-item name="userPwd" :rules="[{ required: true, message: '请输入密码' }]">
            <a-input-password autocomplete="true" v-model:value="loginForm.userPwd" placeholder="密码" />
          </a-form-item>
          <a-form-item v-if="!disabledCaptcha" name="code" :rules="[{ required: true, message: '请输入验证码' }]">
            <a-row>
              <a-col :span="14">
                <a-input v-model:value="loginForm.code" placeholder="验证码" />
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
            <a-button type="primary" html-type="submit" class="btn-login" :loading="loading"> 登录 </a-button>
          </a-form-item>
          <template v-if="enabledOauth2Provides.length">
            <a-divider>第三方登录</a-divider>
            <a-form-item :wrapper-col="{ span: 24 }">
              <a-space :size="20">
                <div class="oauth2-item" v-if="enabledOauth2Provides.includes('gitee')">
                  <a-tooltip @click="toOauth2Url('gitee')" title="gitee">
                    <img alt="gitee" :src="giteeImg" />
                  </a-tooltip>
                </div>
                <div class="oauth2-item" v-if="enabledOauth2Provides.includes('maxkey')">
                  <a-tooltip @click="toOauth2Url('maxkey')" title="maxkey">
                    <img alt="maxkey" :src="maxkeyImg" />
                  </a-tooltip>
                </div>
                <div class="oauth2-item" v-if="enabledOauth2Provides.includes('github')">
                  <a-tooltip @click="toOauth2Url('github')" title="github">
                    <img alt="github" :src="githubImg" />
                  </a-tooltip>
                </div>
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
            label="验证码"
            name="mfaCode"
            help="需要验证 MFA"
            :rules="[
              { required: true, message: '请输入两步验证码' },
              { pattern: /^\d{6}$/, message: '验证码 6 为纯数字' }
            ]"
          >
            <a-input v-model:value="mfaData.mfaCode" placeholder="mfa 验证码" />
          </a-form-item>

          <a-button type="primary" html-type="submit" class="btn-login"> 确认 </a-button>
        </a-form>
      </template>
    </a-card>
  </defaultBg>
</template>
<script setup lang="ts">
import { login, loginConfig, mfaVerify, oauth2Url, oauth2Login, loginRandCode } from '@/api/user/user'
import { checkSystem } from '@/api/install'
import sha1 from 'js-sha1'
import defaultBg from '@/pages/layout/default-bg.vue'
import maxkeyImg from '@/assets/images/maxkey.png'
import giteeImg from '@/assets/images/gitee.svg'
import githubImg from '@/assets/images/github.png'
import { useGuideStore } from '@/stores/guide'
import { Button } from 'ant-design-vue'
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

const loginTitle = ref('登录JPOM')
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
const dynamicBg = ref(localStorage.getItem('dynamicBg') === 'true')
const disabledCaptcha = ref(false)

const backgroundImage = computed(() => {
  const color =
    theme.value === 'light' ? 'linear-gradient(#1890ff, #66a9c9)' : 'linear-gradient(rgb(38 46 55), rgb(27 33 36))'

  // background: linear-gradient(#1890ff, #66a9c9);
  return { background: color }
})

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

const getLoginConfig = () => {
  loginConfig().then((res) => {
    if (res.data && res.data.demo) {
      const demo = res.data.demo
      const p = h('p', { innerHTML: demo.msg }, [])
      $notification.info({
        message: '温馨提示',
        description: h('div', {}, [p])
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
      $message.loading({ content: '跳转到第三方系统中', key: 'oauth2', duration: 0 })
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

const checkHasLoginInfo = () => {
  const key = `tipHasLoginInfo`
  if (useUserStore.userInfo && useUserStore.getToken()) {
    const p = h(
      'p',
      { innerHTML: `当前登录的账号是：<b>${useUserStore.userInfo.name || ''}</b> 是否自动跳转到系统页面` },
      []
    )
    $notification.open({
      message: '检测到当前已经登录账号',
      description: h('div', {}, [p]),
      btn: () =>
        h(
          Button,
          {
            type: 'primary',
            size: 'small',
            onClick: () => {
              $notification.close(key)
              router.push({ path: '/' })
            }
          },
          { default: () => '跳转' }
        ),
      key,
      duration: null
    })
  } else {
    $notification.close(key)
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
