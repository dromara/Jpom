<template>
  <div class="user-header">
    <a-button-group>
      <a-button v-if="mode === 'normal'" type="dashed" class="workspace jpom-workspace btn-group-item">
        <div class="workspace-name">
          <a-tooltip
            :title="selectWorkspace && myWorkspaceList.filter((item:any) => item.id === selectWorkspace)[0].name"
          >
            {{ selectWorkspace && myWorkspaceList.filter((item: any) => item.id === selectWorkspace)[0].name }}
          </a-tooltip>
        </div>
      </a-button>
      <a-button type="primary" class="btn-group-item">
        <div class="user-name">
          <a-tooltip :title="userInfo.name"> {{ userInfo.name }} </a-tooltip>
        </div>
      </a-button>
      <a-dropdown>
        <a-button type="primary" class="jpom-user-operation btn-group-item">
          <template #icon> <DownOutlined /> </template>
        </a-button>
        <template #overlay>
          <a-menu>
            <template v-if="mode === 'normal'">
              <a-sub-menu>
                <template #title>
                  <a-button type="link"><swap-outlined />切换工作空间</a-button>
                </template>
                <template v-for="(item, index) in myWorkspaceList">
                  <a-menu-item
                    v-if="index != -1"
                    :disabled="item.id === selectWorkspace"
                    @click="handleWorkspaceChange(item.id)"
                    :key="index"
                  >
                    <a-button type="link" :disabled="item.id === selectWorkspace">
                      {{ item.name }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-divider v-if="index != -1" :key="`${item.id}-divider`" />
                </template>
              </a-sub-menu>
              <a-menu-divider />
            </template>
            <a-menu-item @click="handleUpdatePwd">
              <a-button type="link"><lock-outlined /> 安全管理 </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="handleUpdateUser">
              <a-button type="link"><profile-outlined /> 用户资料 </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="handleUserlog">
              <a-button type="link"> <bars-outlined /> 操作日志 </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="customize">
              <a-button type="link"><skin-outlined /> 个性配置 </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="logOut">
              <a-button type="link"><logout-outlined /> 退出登录 </a-button>
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </a-button-group>

    <!-- 修改密码区 -->
    <a-modal
      destroyOnClose
      v-model="updateNameVisible"
      :width="'60vw'"
      title="安全管理"
      :footer="null"
      :maskClosable="false"
    >
      <a-tabs v-model="tabActiveKey" @change="tabChange">
        <a-tab-pane :key="1" tab="修改密码">
          <a-form :rules="pwdRules" :model="pwdState" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
            <a-form-item label="原密码" name="oldPwd">
              <a-input-password v-model:value="pwdState.oldPwd" placeholder="请输入原密码" />
            </a-form-item>
            <a-form-item label="新密码" name="newPwd">
              <a-input-password v-model:value="pwdState.newPwd" placeholder="请输入新密码" />
            </a-form-item>
            <a-form-item label="确认密码" name="confirmPwd">
              <a-input-password v-model:value="pwdState.confirmPwd" placeholder="请输入确认密码" />
            </a-form-item>
            <a-form-item>
              <a-row type="flex" justify="center">
                <a-col :span="2">
                  <a-button type="primary" @click="handleUpdatePwdOk">确认重置</a-button>
                </a-col>
              </a-row>
            </a-form-item>
          </a-form>
        </a-tab-pane>
        <a-tab-pane :key="2" tab="两步验证">
          <a-row>
            <a-alert type="warning" v-if="mfaState.needVerify">
              <template slot="message"> 提示 </template>
              <template slot="description">
                <ul style="color: red">
                  <li>绑定成功后将不再显示,强烈建议保存此二维码或者下面的 MFA key</li>
                  <li>请使用应用扫码绑定令牌,然后输入验证码确认绑定才生效</li>
                </ul>
              </template>
            </a-alert>
            <a-col :span="12">
              <a-form
                ref="mfaForm"
                :model="mfaState"
                :rules="mfaRules"
                :label-col="{ span: 6 }"
                :wrapper-col="{ span: 14 }"
              >
                <a-form-item label="当前状态" name="status">
                  <a-switch checked-children="开启中" disabled un-checked-children="关闭中" v-model="mfaState.status" />
                </a-form-item>
                <template v-if="mfaState.needVerify">
                  <a-form-item label="二维码">
                    <a-row>
                      <a-col :span="14">
                        <div class="qrcode" ref="qrCodeUrl" id="qrCodeUrl"></div>
                      </a-col>
                    </a-row>
                  </a-form-item>
                  <a-form-item label="MFA key">
                    <a-input
                      v-clipboard:copy="mfaState.mfaKey"
                      v-clipboard:success="
                        () => {
                          // tempVue.prototype.$notification.success({ message: '复制成功' })
                        }
                      "
                      v-clipboard:error="
                        () => {
                          // tempVue.prototype.$notification.error({ message: '复制失败' })
                        }
                      "
                      readOnly
                      disabled
                      v-model="mfaState.mfaKey"
                    >
                      <a-icon slot="prefix" type="copy" />
                    </a-input>
                  </a-form-item>
                </template>
                <!-- 不能使用  template 包裹 否则验证不能正常启用 -->
                <a-form-item v-if="mfaState.needVerify" label="验证码" name="twoCode">
                  <a-input v-model="mfaState.twoCode" ref="twoCode" placeholder="两步验证码" />
                </a-form-item>
                <a-form-item v-if="mfaState.needVerify">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" html-type="submit" @click="handleBindMfa">确认绑定</a-button>
                    </a-col>
                  </a-row>
                </a-form-item>
                <!-- 不能使用  template 包裹 否则验证不能正常启用 -->
                <a-form-item v-if="!mfaState.needVerify && mfaState.status" label="验证码" name="twoCode">
                  <a-input v-model="mfaState.twoCode" ref="twoCode" placeholder="两步验证码" />
                </a-form-item>
                <a-form-item v-if="!mfaState.needVerify && mfaState.status">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" html-type="submit" @click="closeMfaFn">确认关闭</a-button>
                    </a-col>
                  </a-row>
                </a-form-item>

                <a-form-item v-if="!mfaState.needVerify && !mfaState.status">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" @click="openMfaFn">开启 MFA</a-button>
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-form>
            </a-col>
            <a-col :span="12">
              <h3 id="两步验证应用">两步验证应用</h3>
              <p v-for="(html, index) in MFA_APP_TIP_ARRAY" :key="index" v-html="html" />
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </a-modal>

    <!-- 修改用户资料区 -->
    <a-modal
      destroyOnClose
      v-model="updateUserVisible"
      title="修改用户资料"
      @ok="handleUpdateUserOk"
      :maskClosable="false"
    >
      <a-form :rules="userRules" :model="temp" :label-col="{ span: 8 }" :wrapper-col="{ span: 15 }">
        <a-form-item label="临时token" name="token">
          <a-input disabled v-model="userState.token" placeholder="Token">
            <a-tooltip
              slot="suffix"
              title="复制"
              v-clipboard:copy="userState.token"
              v-clipboard:success="
                () => {
                  // tempVue.prototype.$notification.success({ message: '复制成功' })
                }
              "
              v-clipboard:error="
                () => {
                  // tempVue.prototype.$notification.error({ message: '复制失败' })
                }
              "
            >
              <a-icon type="copy" />
            </a-tooltip>
          </a-input>
        </a-form-item>
        <a-form-item label="长期token" name="md5Token">
          <a-input disabled v-model="userState.md5Token" placeholder="Token">
            <a-tooltip
              slot="suffix"
              title="复制"
              v-clipboard:copy="userState.md5Token"
              v-clipboard:success="
                () => {
                  // tempVue.prototype.$notification.success({ message: '复制成功' })
                }
              "
              v-clipboard:error="
                () => {
                  // tempVue.prototype.$notification.error({ message: '复制失败' })
                }
              "
            >
              <a-icon type="copy" />
            </a-tooltip>
          </a-input>
        </a-form-item>
        <a-form-item label="昵称" name="name">
          <a-input v-model="userState.name" placeholder="昵称" />
        </a-form-item>
        <a-form-item label="邮箱地址" name="email">
          <a-input v-model="userState.email" placeholder="邮箱地址" />
        </a-form-item>
        <a-form-item v-show="showCode" label="邮箱验证码" name="code">
          <a-row :gutter="8">
            <a-col :span="15">
              <a-input v-model="userState.code" placeholder="邮箱验证码" />
            </a-col>
            <a-col :span="4">
              <a-button type="primary" :disabled="!userState.email" @click="handleSendEmailCode">发送验证码</a-button>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item label="钉钉通知地址" name="dingDing">
          <a-input v-model="userState.dingDing" placeholder="钉钉通知地址" />
        </a-form-item>
        <a-form-item label="企业微信通知地址" name="workWx">
          <a-input v-model="userState.workWx" placeholder="企业微信通知地址" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 个性配置区 -->
    <a-modal destroyOnClose v-model="customizeVisible" title="个性配置区" :footer="null" :maskClosable="false">
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-alert banner>
          <template slot="message"> 下列配置信息仅在当前浏览器生效,清空浏览器缓存配置将恢复默认 </template>
        </a-alert>
        <!-- <a-form-item label="页面导航" name="token">
          <a-space>
            <a-switch checked-children="开" @click="toggleGuide" :checked="!this.guideStatus" :disabled="getDisabledGuide"
              un-checked-children="关" />

            <div v-if="!this.guideStatus">
              重置导航
              <a-icon type="rest" @click="restGuide" />
            </div>
          </a-space>
        </a-form-item> -->
        <a-form-item label="菜单配置" name="token">
          <a-space>
            同时展开多个：
            <a-switch
              checked-children="是"
              @click="toggleMenuMultiple"
              :checked="guideCache.menuMultipleFlag"
              un-checked-children="否"
            />
          </a-space>
        </a-form-item>
        <a-form-item label="页面配置" name="token">
          <a-space>
            自动撑开：
            <a-switch
              checked-children="是"
              @click="toggleFullScreenFlag"
              :checked="guideCache.fullScreenFlag"
              un-checked-children="否"
            />
          </a-space>
        </a-form-item>
        <a-form-item label="滚动条显示" name="token">
          <a-space>
            全局配置：
            <a-switch
              checked-children="显示"
              @click="toggleScrollbarFlag"
              :checked="guideCache.scrollbarFlag"
              un-checked-children="不显示"
            />
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- mfa 提示 -->
    <a-modal
      destroyOnClose
      v-model="bindMfaTip"
      title="安全提醒"
      :footer="null"
      :maskClosable="false"
      :closable="false"
      :keyboard="false"
    >
      <a-space direction="vertical">
        <a-alert
          message="安全提醒"
          description="为了您的账号安全系统要求必须开启两步验证来确保账号的安全性"
          type="error"
          :closable="false"
        />
        <a-row align="middle" type="flex" justify="center">
          <a-button type="danger" @click="toBindMfa"> 立即开启 </a-button>
        </a-row>
      </a-space>
    </a-modal>
    <!-- 查看操作日志 -->
    <a-modal
      destroyOnClose
      v-model="viewLogVisible"
      :width="'90vw'"
      title="操作日志"
      :footer="null"
      :maskClosable="false"
    >
      <user-log v-if="viewLogVisible"></user-log>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
// import { mapGetters } from "vuex";
import {
  bindMfa,
  closeMfa,
  editUserInfo,
  generateMfa,
  getUserInfo,
  myWorkspace,
  sendEmailCode,
  updatePwd
} from '@/api/user/user'
// import QRCode from "qrcodejs2";
import sha1 from 'js-sha1'
import { MFA_APP_TIP_ARRAY } from '@/utils/const'
import UserLog from './user-log.vue'
import { Form } from 'ant-design-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useGuideStore } from '@/stores/guide'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()
const guideStore = useGuideStore()

const props = defineProps<{
  mode: string
}>()

const { userInfo } = toRefs(userStore)
const { guideCache } = toRefs(guideStore)

const collapsed = ref(false)
const updateNameVisible = ref(false)
const updateUserVisible = ref(false)
const temp = reactive({})
const myWorkspaceList = ref(<any>[])
const selectWorkspace = ref('')
const customizeVisible = ref(false)

const mFA_APP_TIP_ARRAY = ref(MFA_APP_TIP_ARRAY)
const isBindMfa = ref(false)
const viewLogVisible = ref(false)

const showCode = computed(() => {
  return userInfo.value.email !== userState.value.email
})

const customize = () => {
  customizeVisible.value = true
}

// const creatQrCode = (qrCodeDom, text) =>{
//   new QRCode(qrCodeDom, {
//     text: text || "xxxx",
//     width: 120,
//     height: 120,
//     colorDark: "#000000",
//     colorLight: "#ffffff",
//     correctLevel: QRCode.CorrectLevel.H,
//   });
// }

const init = () => {
  if (props.mode === 'normal') {
    myWorkspace().then((res) => {
      if (res.code == 200 && res.data) {
        myWorkspaceList.value = res.data
        let wid = route.query.wid
        wid = wid ? wid : appStore.getWorkspaceId
        const existWorkspace = myWorkspaceList.value.filter((item: any) => item.id === wid)
        if (existWorkspace.length) {
          router.push({
            query: { ...route.query, wid: wid }
          })
          selectWorkspace.value = wid as string
        } else {
          handleWorkspaceChange(res.data[0]?.id || '')
        }
      }
    })
  }
  checkMfa()
}

const bindMfaTip = ref(false)
const checkMfa = () => {
  if (!userInfo) {
    return
  }
  if (userInfo?.forceMfa === true && userInfo.bindMfa === false) {
    bindMfaTip.value = true
  }
}

const toBindMfa = () => {
  bindMfaTip.value = false
  updateNameVisible.value = true
  tabChange(2)
}

// 切换引导
// const toggleGuide = () => {
//   guideStore.toggleGuideFlag().then((flag: boolean) => {
//     $notification.success({
//       message: flag ? '关闭页面操作引导、导航' : '开启页面操作引导、导航'
//     })
//   })
// }

// 切换菜单打开
const toggleMenuMultiple = () => {
  guideStore.toggleMenuFlag().then((flag: boolean) => {
    $notification.success({
      message: flag ? '可以同时展开多个菜单' : '同时只能展开一个菜单'
    })
  })
}

// 页面全屏
const toggleFullScreenFlag = () => {
  guideStore.toggleFullScreenFlag().then((flag: boolean) => {
    $notification.success({
      message: flag ? '页面全屏，高度 100%。局部区域可以滚动' : '页面内容自动撑开出现屏幕滚动条'
    })
  })
}

// 切换滚动条是否显示
const toggleScrollbarFlag = () => {
  guideStore.toggleScrollbarFlag().then((flag: boolean) => {
    $notification.success({
      message: flag ? '页面内容会出现滚动条' : '隐藏滚动条。纵向滚动方式提醒：滚轮，横行滚动方式：Shift+滚轮'
    })
  })
}

// const restGuide = () => {
//   guideStore.restGuide().then(() => {
//     $notification.success({
//       message: '重置页面操作引导、导航成功'
//     })
//   })
// }

// 退出登录
const logOut = () => {
  $confirm({
    title: '系统提示',
    content: '真的要退出系统么？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      userStore.logOut().then(() => {
        $notification.success({
          message: '退出登录成功'
        })
        const query = Object.assign({}, route.query)
        router.replace({
          path: '/login',
          query: query
        })
      })
    }
  })
}

const pwdState = reactive({
  oldPwd: '',
  newPwd: '',
  confirmPwd: ''
})

const pwdRules = {
  oldPwd: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    { max: 20, message: '密码长度为6-20', trigger: 'blur' },
    { min: 6, message: '密码长度为6-20', trigger: 'blur' }
  ],
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { max: 20, message: '密码长度为6-20', trigger: 'blur' },
    { min: 6, message: '密码长度为6-20', trigger: 'blur' }
  ],
  confirmPwd: [
    { required: true, message: '请输入确认密码', trigger: 'blur' },
    { max: 20, message: '密码长度为6-20', trigger: 'blur' },
    { min: 6, message: '密码长度为6-20', trigger: 'blur' }
  ]
}

const pwdForm = Form.useForm(pwdState, pwdRules)

// 加载修改密码对话框
const handleUpdatePwd = () => {
  updateNameVisible.value = true
  tabChange(1)
  pwdForm.resetFields()
}

// 修改密码
const handleUpdatePwdOk = () => {
  // 检验表单
  pwdForm.validate((values: any) => {
    // 判断两次新密码是否一致
    if (pwdState.newPwd !== pwdState.confirmPwd) {
      $notification.error({
        message: '两次密码不一致...'
      })
      return
    }
    // 提交修改
    const params = {
      oldPwd: sha1(pwdState.oldPwd),
      newPwd: sha1(pwdState.newPwd)
    }
    updatePwd(params).then((res) => {
      // 修改成功
      if (res.code === 200) {
        // 退出登录
        userStore.logOut().then(() => {
          $notification.success({
            message: res.msg
          })
          pwdForm.resetFields()
          updateNameVisible.value = false
          router.push('/login')
        })
      }
    })
  })
}

const userState = ref({
  md5Token: '',
  token: '',
  name: '',
  email: '',
  workWx: '',
  code: '',
  dingDing: ''
})

const userRules = {
  name: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 10, message: '昵称长度为2-10', trigger: 'blur' },
    { min: 2, message: '昵称长度为2-10', trigger: 'blur' }
  ]
}

// 加载修改用户资料对话框
const handleUpdateUser = () => {
  getUserInfo().then((res) => {
    if (res.code === 200) {
      userState.value = { ...res.data, token: userStore.token }
      updateUserVisible.value = true
    }
  })
}

// 发送邮箱验证码
const handleSendEmailCode = () => {
  if (!userState.value.email) {
    $notification.error({
      message: '请输入邮箱地址'
    })
    return
  }
  sendEmailCode(userState.value.email).then((res) => {
    if (res.code === 200) {
      $notification.success({
        message: res.msg
      })
    }
  })
}

const userForm = Form.useForm(userState, userRules)
// 修改用户资料
const handleUpdateUserOk = () => {
  // 检验表单
  userForm.validate((valid) => {
    if (!valid) {
      return false
    }
    const { token, md5Token, ...rest } = userState.value
    editUserInfo(rest).then((res) => {
      // 修改成功
      if (res.code === 200) {
        $notification.success({
          message: res.msg
        })
        // 清空表单校验
        userForm.resetFields()
        updateUserVisible.value = false
        userStore.refreshUserInfo()
      }
    })
  })
}
const pageReload = inject<Function>('reload')
const handleWorkspaceChange = (value: string) => {
  appStore.changeWorkspace(value)

  router
    .push({
      query: { ...route.query, wid: value }
    })
    .then(() => {
      pageReload?.()
    })
}

const tabActiveKey = ref(1)

const tabChange = (key: number) => {
  tabActiveKey.value = key
  if (key === 1) {
    getUserInfo().then((res) => {
      if (res.code === 200) {
        mfaState.status = res.data.bindMfa
        nextTick(() => {
          this.$refs?.twoCode?.focus()
        })
      }
    })
  }
}

const mfaState = reactive({
  needVerify: false,
  status: false,
  email: '',
  emailCode: '',
  twoCode: '',
  mfaKey: '',
  url: '',
  showSaveTip: false
})

const mfaRules = {
  twoCode: [
    { required: true, message: '请输入两步验证码', trigger: ['change', 'blur'] },
    { pattern: /^\d{6}$/, message: '验证码 6 为纯数字', trigger: ['change', 'blur'] }
  ]
}

const mfaForm = Form.useForm(mfaState, mfaRules)

// 关闭 mfa
const closeMfaFn = () => {
  mfaForm.validate((valid) => {
    if (!valid) {
      return false
    }
    $confirm({
      title: '系统提示',
      content: '确定要关闭两步验证吗？关闭后账号安全性将受到影响,关闭后已经存在的 mfa key 将失效',
      okText: '确认',
      cancelText: '取消',
      onOk: () => {
        //
        closeMfa({
          code: mfaState.twoCode
        }).then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            mfaState.needVerify = false
            mfaState.status = false
          }
        })
      }
    })
  })
}

// mfa 状态切换
const openMfaFn = () => {
  //
  generateMfa().then((res) => {
    if (res.code === 200) {
      mfaState.status = true
      mfaState.mfaKey = res.data.mfaKey
      ;(mfaState.url = res.data.url),
        (mfaState.needVerify = true),
        (mfaState.showSaveTip = true),
        (mfaState.twoCode = '')

      // this.showQrCode()
      $notification.info({
        message: '需要输入验证码,确认绑定后才生效奥'
      })
    }
  })
}

const handleBindMfa = () => {
  bindMfa({
    mfa: mfaState.mfaKey,
    twoCode: mfaState.twoCode
  }).then((res) => {
    if (res.code === 200) {
      $notification.success({
        message: res.msg
      })
      mfaState.needVerify = false
      mfaState.twoCode = ''
    }
  })
}

const handleUserlog = () => {
  viewLogVisible.value = true
}

onMounted(() => {
  init()
})
</script>

<style scoped lang="less">
.btn-group-item {
  padding: 0 5px;
}

.workspace-name {
  min-width: 30px;
  max-width: 200px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-weight: bold;
}

.user-name {
  min-width: 30px;
  max-width: 100px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/deep/ .ant-dropdown-menu-submenu-arrow {
  position: relative;
}
</style>
