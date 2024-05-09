<template>
  <div class="user-header">
    <a-button-group>
      <!-- 工作空间信息 -->
      <a-button v-if="mode === 'normal'" type="dashed" class="workspace jpom-workspace btn-group-item">
        <div class="workspace-name">
          <a-tooltip
            :title="`${$tl('p.workspaceName')}${selectWorkspace.name} ${$tl('p.groupName')}${selectWorkspace.group || '${$tl('c.unConfigured')}'}${$tl('c.bracketRight')}`"
            placement="bottom"
          >
            <SwitcherOutlined />
            {{ selectWorkspace.name }}
            <template v-if="myClusterList.length > 1 && selectWorkspace.clusterInfoId">
              /
              {{
                myClusterList.find((item) => {
                  return item.id === selectWorkspace.clusterInfoId
                }) &&
                myClusterList.find((item) => {
                  return item.id === selectWorkspace.clusterInfoId
                }).name
              }}
            </template>
            <template v-if="!inClusterUrl">
              <SwapOutlined @click="handleClusterChange(selectCluster)" />
            </template>
          </a-tooltip>
        </div>
      </a-button>
      <a-button v-if="mode === 'management'" type="dashed">
        <div class="workspace-name">
          <a-tooltip :title="`${$tl('p.clusterName')}${selectCluster && selectCluster.name}`" placement="bottom">
            <ClusterOutlined />
            {{ selectCluster && selectCluster.name }}
          </a-tooltip>
        </div>
      </a-button>
      <a-button type="primary" class="btn-group-item">
        <div class="user-name">
          <a-tooltip :title="getUserInfo.name" placement="bottom"> {{ getUserInfo.name }} </a-tooltip>
        </div>
      </a-button>
      <a-dropdown>
        <a-button type="primary" class="btn-group-item"><DownOutlined /></a-button>
        <template #overlay>
          <a-menu>
            <!-- 工作空间信息 -->
            <template v-if="mode === 'normal'">
              <a-sub-menu>
                <template #title>
                  <a-button type="link"><RetweetOutlined />{{ $tl('p.switchWorkspace') }}</a-button>
                </template>
                <template v-if="myWorkspaceList.length == 1">
                  <template v-for="(item, index) in myWorkspaceList[0].children">
                    <a-menu-item
                      v-if="index != -1"
                      :key="index"
                      :disabled="item.id === selectWorkspace.id"
                      @click="handleWorkspaceChange(item)"
                    >
                      <a-button type="link" :disabled="item.id === selectWorkspace.id">
                        {{ item.name || $tl('c.unConfigured') }}
                        <template v-if="myClusterList.length > 1 && item.clusterInfoId">
                          {{ $tl('c.bracketLeft')
                          }}{{
                            myClusterList.find((item2) => {
                              return item2.id === item.clusterInfoId
                            }) &&
                            myClusterList.find((item2) => {
                              return item2.id === item.clusterInfoId
                            }).name
                          }}{{ $tl('c.bracketRight') }}
                        </template>
                      </a-button>
                    </a-menu-item>
                    <a-menu-divider v-if="index < myWorkspaceList[0].children.length - 1" :key="`${item.id}-divider`" />
                  </template>
                </template>
                <template v-if="myWorkspaceList.length > 1">
                  <template v-for="(item1, index1) in myWorkspaceList" :key="index1">
                    <a-sub-menu>
                      <template #title>
                        <a-button type="link">
                          {{ item1.value || $tl('c.unConfigured') }}
                        </a-button>
                      </template>
                      <template v-for="(item, index) in item1.children">
                        <a-menu-item
                          v-if="index != -1"
                          :key="index"
                          :disabled="item.id === selectWorkspace.id"
                          @click="handleWorkspaceChange(item)"
                        >
                          <a-button type="link" :disabled="item.id === selectWorkspace.id">
                            {{ item.name }}
                            <template v-if="myClusterList.length > 1 && item.clusterInfoId">
                              {{ $tl('c.bracketLeft')
                              }}{{
                                myClusterList.find((item2) => {
                                  return item2.id === item.clusterInfoId
                                }) &&
                                myClusterList.find((item2) => {
                                  return item2.id === item.clusterInfoId
                                }).name
                              }}{{ $tl('c.bracketRight') }}
                            </template>
                          </a-button>
                        </a-menu-item>
                        <a-menu-divider v-if="index < item1.children.length - 1" :key="`${index1}-${index}-divider`" />
                      </template>
                    </a-sub-menu>
                    <a-menu-divider v-if="index1 < myWorkspaceList.length - 1" :key="`${index1}-divider`" />
                  </template>
                </template>
              </a-sub-menu>
              <a-menu-divider />
            </template>
            <!-- 集群信息 -->
            <template v-if="mode === 'management'">
              <a-sub-menu>
                <template #title>
                  <a-button type="link"><RetweetOutlined />{{ $tl('p.switchCluster') }}</a-button>
                </template>
                <template v-for="(item, index) in myClusterList">
                  <a-menu-item
                    v-if="index != -1"
                    :key="index"
                    :disabled="item.id === selectCluster.id || !item.url"
                    @click="handleClusterChange(item)"
                  >
                    <a-button type="link" :disabled="item.id === selectCluster.id || !item.url">
                      {{ item.name }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-divider v-if="index < myClusterList.length - 1" :key="`${item.id}-divider`" />
                </template>
              </a-sub-menu>
            </template>
            <a-menu-item @click="handleUpdatePwd">
              <a-button type="link"> <lock-outlined />{{ $tl('c.securityManagement') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="handleUpdateUser">
              <a-button type="link"><profile-outlined /> {{ $tl('p.userProfile') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="handleUserlog">
              <a-button type="link"><bars-outlined /> {{ $tl('c.operationLog') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="customize">
              <a-button type="link"><skin-outlined /> {{ $tl('p.personalConfiguration') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="logOut">
              <a-button type="link"> <logout-outlined />{{ $tl('p.logout') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="logOutSwap">
              <a-button type="link"> <SwapOutlined />{{ $tl('p.switchAccount') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="logOutAll">
              <a-button type="link"><RestOutlined /> {{ $tl('p.completeLogout') }} </a-button>
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </a-button-group>

    <!-- 修改密码区 -->
    <a-modal
      v-model:open="updateNameVisible"
      destroy-on-close
      :width="'60vw'"
      :title="$tl('c.securityManagement')"
      :footer="null"
      :mask-closable="false"
    >
      <a-tabs v-model:active-key="temp.tabActiveKey" @change="tabChange">
        <a-tab-pane :key="1" :tab="$tl('p.changePassword')">
          <a-spin tip="Loading..." :spinning="confirmLoading">
            <a-form
              ref="pwdForm"
              :rules="rules"
              :model="temp"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 14 }"
              @finish="handleUpdatePwdOk"
            >
              <a-form-item :label="$tl('p.originalPassword')" name="oldPwd">
                <a-input-password v-model:value="temp.oldPwd" :placeholder="$tl('c.oldPassword')" />
              </a-form-item>
              <a-form-item :label="$tl('p.newPassword')" name="newPwd">
                <a-input-password v-model:value="temp.newPwd" :placeholder="$tl('c.newPassword')" />
              </a-form-item>
              <a-form-item :label="$tl('p.confirmPassword')" name="confirmPwd">
                <a-input-password v-model:value="temp.confirmPwd" :placeholder="$tl('c.confirmPassword')" />
              </a-form-item>
              <a-form-item>
                <a-row type="flex" justify="center">
                  <a-col :span="2">
                    <a-button type="primary" html-type="submit" :loading="confirmLoading">{{
                      $tl('p.confirmReset')
                    }}</a-button>
                  </a-col>
                </a-row>
              </a-form-item>
            </a-form>
          </a-spin>
        </a-tab-pane>
        <a-tab-pane :key="2" :tab="$tl('p.twoStepVerification')">
          <a-row>
            <a-col :span="24">
              <a-alert v-if="temp.needVerify" type="warning">
                <template #message> {{ $tl('p.prompt') }} </template>
                <template #description>
                  <ul style="color: red">
                    <li>{{ $tl('p.mfaKey') }}</li>
                    <li>{{ $tl('p.pleaseScanQRCodeAndBindToken') }}</li>
                  </ul>
                </template>
              </a-alert>
            </a-col>
            <a-col :span="12">
              <a-form
                ref="mfaForm"
                :model="temp"
                :rules="rules"
                :label-col="{ span: 6 }"
                :wrapper-col="{ span: 14 }"
                @finish="handleBindMfa"
              >
                <a-form-item :label="$tl('p.currentStatus')" name="status">
                  <a-switch
                    v-model:checked="temp.status"
                    :checked-children="$tl('p.opening')"
                    disabled
                    :un-checked-children="$tl('p.closing')"
                  />
                </a-form-item>
                <template v-if="temp.needVerify">
                  <a-form-item :label="$tl('p.qrCode')">
                    <a-row>
                      <a-col :span="14">
                        <a-qrcode :value="temp.url" :status="temp.url ? 'active' : 'loading'" />
                      </a-col>
                    </a-row>
                  </a-form-item>
                  <a-form-item label="MFA key">
                    <a-input v-model:value="temp.mfaKey" read-only disabled>
                      <template #suffix>
                        <a-typography-paragraph
                          style="margin-bottom: 0"
                          :copyable="{ tooltip: true, text: temp.mfaKey }"
                        >
                        </a-typography-paragraph>
                      </template>
                    </a-input>
                  </a-form-item>
                </template>
                <!-- 不能使用  template 包裹 否则验证不能正常启用 -->
                <a-form-item v-if="temp.needVerify" :label="$tl('c.verificationCode')" name="twoCode">
                  <a-input ref="twoCode" v-model:value="temp.twoCode" :placeholder="$tl('c.twoStepVerificationCode')" />
                </a-form-item>
                <a-form-item v-if="temp.needVerify">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" html-type="submit">{{ $tl('p.confirmBind') }}</a-button>
                    </a-col>
                  </a-row>
                </a-form-item>
                <!-- 不能使用  template 包裹 否则验证不能正常启用 -->
                <a-form-item v-if="!temp.needVerify && temp.status" :label="$tl('c.verificationCode')" name="twoCode">
                  <a-input ref="twoCode" v-model:value="temp.twoCode" :placeholder="$tl('c.twoStepVerificationCode')" />
                </a-form-item>
                <a-form-item v-if="!temp.needVerify && temp.status">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" html-type="submit">{{ $tl('p.confirmClose') }}</a-button>
                    </a-col>
                  </a-row>
                </a-form-item>

                <a-form-item v-if="!temp.needVerify && !temp.status">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" @click="openMfaFn">{{ $tl('p.enableMFA') }}</a-button>
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-form>
            </a-col>
            <a-col :span="12">
              <h3 :id="$tl('c.twoStepVerificationApp')">{{ $tl('c.twoStepVerificationApp') }}</h3>
              <p v-for="(html, index) in MFA_APP_TIP_ARRAY" :key="index" v-html="html" />
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
    <!-- 修改用户资料区 -->
    <a-modal
      v-model:open="updateUserVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$tl('p.editUserProfile')"
      :mask-closable="false"
      @ok="handleUpdateUserOk"
    >
      <a-form ref="userForm" :rules="rules" :model="temp" :label-col="{ span: 8 }" :wrapper-col="{ span: 15 }">
        <a-form-item :label="$tl('p.temporaryToken')" name="token">
          <a-input v-model:value="temp.token" disabled placeholder="Token">
            <template #suffix>
              <a-typography-paragraph style="margin-bottom: 0" :copyable="{ tooltip: true, text: temp.token }">
              </a-typography-paragraph>
            </template>
          </a-input>
        </a-form-item>
        <a-form-item :label="$tl('p.permanentToken')" name="md5Token">
          <a-input v-model:value="temp.md5Token" disabled placeholder="Token">
            <template #suffix>
              <a-typography-paragraph style="margin-bottom: 0" :copyable="{ tooltip: true, text: temp.md5Token }">
              </a-typography-paragraph>
            </template>
          </a-input>
        </a-form-item>
        <a-form-item :label="$tl('c.nickname')" name="name">
          <a-input v-model:value="temp.name" :placeholder="$tl('c.nickname')" />
        </a-form-item>
        <a-form-item :label="$tl('c.emailAddress')" name="email">
          <a-input v-model:value="temp.email" :placeholder="$tl('c.emailAddress')" />
        </a-form-item>
        <a-form-item v-show="showCode" :label="$tl('c.emailVerificationCode')" name="code">
          <a-row :gutter="8">
            <a-col :span="15">
              <a-input v-model:value="temp.code" :placeholder="$tl('c.emailVerificationCode')" />
            </a-col>
            <a-col :span="4">
              <a-button type="primary" :disabled="!temp.email" @click="sendEmailCode">{{
                $tl('p.sendVerificationCode')
              }}</a-button>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item :label="$tl('c.dingNotificationAddress')" name="dingDing">
          <a-input v-model:value="temp.dingDing" :placeholder="$tl('c.dingNotificationAddress')" />
        </a-form-item>
        <a-form-item :label="$tl('c.enterpriseWeChatNotificationAddress')" name="workWx">
          <a-input v-model:value="temp.workWx" :placeholder="$tl('c.enterpriseWeChatNotificationAddress')" />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 个性配置区 -->
    <a-modal
      v-model:open="customizeVisible"
      destroy-on-close
      :title="$tl('p.personalConfigArea')"
      :footer="null"
      :mask-closable="false"
    >
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-alert banner>
          <template #message>
            {{ $tl('p.theseConfigsOnlyEffectiveInCurrentBrowser') }},{{
              $tl('p.clearingBrowserCacheWillRestoreDefaults')
            }}
          </template>
        </a-alert>
        <!-- <a-form-item label="页面导航">
          <a-space>
            <a-switch
              checked-children="开"
              @click="toggleGuide"
              :checked="!this.guideStatus"
              :disabled="this.getDisabledGuide"
              un-checked-children="关"
            />

            <div v-if="!this.guideStatus">
              重置导航
              <RestOutlined @click="restGuide" />
            </div>
          </a-space>
        </a-form-item> -->
        <a-form-item :label="$tl('p.menuConfig')">
          <a-space>
            <a-switch
              :checked-children="$tl('p.yes')"
              :checked="menuMultipleFlag"
              :un-checked-children="$tl('p.no')"
              @click="toggleMenuMultiple"
            />
            {{ $tl('p.expandMultipleAtOnce') }}
          </a-space>
        </a-form-item>
        <!-- <a-form-item label="页面配置">
          <a-space>
            自动撑开：
            <a-switch
              checked-children="是"
              @click="toggleFullScreenFlag"
              :checked="this.fullScreenFlag"
              un-checked-children="否"
            />
          </a-space>
        </a-form-item>
        <a-form-item label="滚动条显示">
          <a-space>
            全局配置：
            <a-switch
              checked-children="显示"
              @click="toggleScrollbarFlag"
              :checked="this.scrollbarFlag"
              un-checked-children="不显示"
            />
          </a-space>
        </a-form-item> -->
        <a-form-item :label="$tl('p.fullScreenLog')">
          <a-space>
            <a-switch
              :checked-children="$tl('p.fullScreen')"
              :checked="fullscreenViewLog"
              un-checked-children="非{{$tl('p.fullScreen')}}"
              @click="toggleFullscreenViewLog"
            />
            {{ $tl('p.viewLogInFullScreen') }}
          </a-space>
        </a-form-item>
        <a-form-item :label="$tl('p.contentTheme')">
          <a-space>
            <a-radio-group v-model:value="themeView" button-style="solid">
              <a-radio-button value="light">{{ $tl('c.lightTheme') }}</a-radio-button>
              <a-radio-button value="dark">{{ $tl('c.darkTheme') }}</a-radio-button>
              <a-radio-button value="auto">{{ $tl('p.followSystem') }}</a-radio-button>
            </a-radio-group>
            {{ $tl('p.contentAreaThemeSwitch') }}
          </a-space>
        </a-form-item>
        <a-form-item :label="$tl('p.menuTheme')">
          <a-space>
            <a-radio-group v-model:value="menuThemeView" button-style="solid">
              <a-radio-button value="light">{{ $tl('c.lightTheme') }}</a-radio-button>
              <a-radio-button value="dark">{{ $tl('c.darkTheme') }}</a-radio-button>
            </a-radio-group>
            {{ $tl('p.leftMenuThemeSwitch') }}
          </a-space>
        </a-form-item>

        <a-form-item :label="$tl('p.compactMode')">
          <a-space>
            <a-switch
              :checked-children="$tl('p.compact')"
              :checked="compactView"
              :un-checked-children="$tl('p.loose')"
              @click="toggleCompactView"
            />
            {{ $tl('p.fontSpacingAdjustment') }}({{ $tl('p.onlyEffectiveInDarkMode') }})
          </a-space>
        </a-form-item>
        <a-form-item v-if="!isProduction" :label="$tl('p.language')">
          <a-space>
            <a-radio-group v-model:value="locale" button-style="solid">
              <a-radio-button value="zh-cn">{{ $tl('p.chinese') }}</a-radio-button>
              <a-radio-button value="en-us">English</a-radio-button>
            </a-radio-group>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- mfa 提示 -->
    <a-modal
      v-model:open="bindMfaTip"
      destroy-on-close
      :title="$tl('c.securityReminder')"
      :footer="null"
      :mask-closable="false"
      :closable="false"
      :keyboard="false"
    >
      <a-space direction="vertical">
        <a-alert
          :message="$tl('c.securityReminder')"
          :description="
            $tl('p.forYourAccountSecurityTheSystemRequiresTwoStepVerificationToBeEnabledToEnsureAccountSafety')
          "
          type="error"
          :closable="false"
        />
        <a-row align="middle" type="flex" justify="center">
          <a-button type="danger" @click="toBindMfa"> {{ $tl('p.enableImmediately') }} </a-button>
        </a-row>
      </a-space>
    </a-modal>
    <!-- 查看操作日志 -->
    <a-modal
      v-model:open="viewLogVisible"
      destroy-on-close
      :width="'90vw'"
      :title="$tl('c.operationLog')"
      :footer="null"
      :mask-closable="false"
    >
      <user-log v-if="viewLogVisible"></user-log>
    </a-modal>
  </div>
</template>
<script>
import {
  bindMfa,
  closeMfa,
  editUserInfo,
  generateMfa,
  getUserInfo,
  myWorkspace,
  sendEmailCode,
  updatePwd,
  clusterList
} from '@/api/user/user'

import sha1 from 'js-sha1'
// import Vue from 'vue'
import { MFA_APP_TIP_ARRAY, itemGroupBy } from '@/utils/const'
import UserLog from './user-log.vue'
import { mapState } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { useGuideStore } from '@/stores/guide'

import { useAllMenuStore } from '@/stores/menu2'
export default {
  components: {
    UserLog
  },
  inject: ['reload'],
  props: {
    mode: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      collapsed: false,
      // 修改密码框
      updateNameVisible: false,
      updateUserVisible: false,
      temp: {},

      myWorkspaceList: [],
      myClusterList: [],
      currentClusterId: '',
      selectWorkspace: {},
      customizeVisible: false,
      // 表单校验规则
      rules: {
        name: [
          { required: true, message: this.$tl('p.nickname'), trigger: 'blur' },
          { max: 10, message: this.$tl('c.nicknameLength'), trigger: 'blur' },
          { min: 2, message: this.$tl('c.nicknameLength'), trigger: 'blur' }
        ],
        oldPwd: [
          { required: true, message: this.$tl('c.oldPassword'), trigger: 'blur' },
          { max: 20, message: this.$tl('c.passwordLength'), trigger: 'blur' },
          { min: 6, message: this.$tl('c.passwordLength'), trigger: 'blur' }
        ],
        newPwd: [
          { required: true, message: this.$tl('c.newPassword'), trigger: 'blur' },
          { max: 20, message: this.$tl('c.passwordLength'), trigger: 'blur' },
          { min: 6, message: this.$tl('c.passwordLength'), trigger: 'blur' }
        ],
        confirmPwd: [
          { required: true, message: this.$tl('c.confirmPassword'), trigger: 'blur' },
          { max: 20, message: this.$tl('c.passwordLength'), trigger: 'blur' },
          { min: 6, message: this.$tl('c.passwordLength'), trigger: 'blur' }
        ],
        email: [
          // { required: true, message: "请输入邮箱", trigger: "blur" }
        ],
        twoCode: [
          { required: true, message: this.$tl('p.twoStepVerificationCode'), trigger: ['change', 'blur'] },
          { pattern: /^\d{6}$/, message: this.$tl('p.verificationCode6'), trigger: ['change', 'blur'] }
        ]
      },
      MFA_APP_TIP_ARRAY,
      bindMfaTip: false,
      viewLogVisible: false,
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useUserStore, ['getToken', 'getUserInfo']),
    ...mapState(useAppStore, ['getWorkspaceId', 'isProduction']),
    ...mapState(useGuideStore, ['getGuideCache', 'getDisabledGuide', 'getThemeView', 'getMenuThemeView', 'getLocale']),
    showCode() {
      return this.getUserInfo.email !== this.temp.email
    },
    guideStatus() {
      return this.getGuideCache.close
    },
    menuMultipleFlag() {
      return this.getGuideCache.menuMultipleFlag === undefined ? true : this.getGuideCache.menuMultipleFlag
    },
    fullScreenFlag() {
      return this.getGuideCache.fullScreenFlag === undefined ? true : this.getGuideCache.fullScreenFlag
    },
    scrollbarFlag() {
      return this.getGuideCache.scrollbarFlag === undefined ? true : this.getGuideCache.scrollbarFlag
    },
    compactView() {
      return this.getGuideCache.compactView === undefined ? false : this.getGuideCache.compactView
    },
    themeView: {
      set: function (value) {
        useGuideStore().toggleThemeView(value)
      },
      get: function () {
        return useGuideStore().guideCache.themeView
        // return this.getThemeView()
      }
    },
    menuThemeView: {
      set: function (value) {
        useGuideStore().toggleMenuThemeView(value)
      },
      get: function () {
        return this.getMenuThemeView()
      }
    },
    locale: {
      set: function (value) {
        useGuideStore().changeLocale(value)
      },
      get: function () {
        return useGuideStore().getLocale()
      }
    },
    fullscreenViewLog() {
      return !!this.getGuideCache.fullscreenViewLog
    },
    selectCluster: {
      get: function () {
        const temp = this.myClusterList.find((item) => {
          return item.id === this.currentClusterId
        })
        return temp
      }
    },
    inClusterUrl() {
      const data = this.selectCluster
      if (!data || !data.url) {
        // 没有配置集群地址
        return true
      }
      return window.location.href.indexOf(data && data.url) === 0
    }
  },

  created() {
    this.init()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.layout.userHeader.${key}`, ...args)
    },
    customize() {
      this.customizeVisible = true
    },

    init() {
      if (this.mode === 'normal') {
        // 获取工作空间
        myWorkspace().then((res) => {
          if (res.code == 200 && res.data) {
            const tempArray = res.data

            this.myWorkspaceList = itemGroupBy(tempArray, 'group', 'value', 'children')

            let wid = this.$route.query.wid
            wid = wid ? wid : this.getWorkspaceId()
            const existWorkspace = tempArray.find((item) => item.id === wid)

            if (existWorkspace) {
              this.$router.push({
                query: { ...this.$route.query, wid: wid }
              })
              this.selectWorkspace = existWorkspace
            } else {
              this.handleWorkspaceChange(res.data[0])
            }
          }
        })
      }
      // 获取集群
      clusterList().then((res) => {
        if (res.code == 200 && res.data) {
          this.myClusterList = res.data.list || []
          this.currentClusterId = res.data.currentId
        }
      })
      this.checkMfa()
    },
    checkMfa() {
      if (!this.getUserInfo) {
        return
      }
      if (this.getUserInfo.forceMfa === true && this.getUserInfo.bindMfa === false) {
        this.bindMfaTip = true
      }
    },
    toBindMfa() {
      this.bindMfaTip = false
      this.updateNameVisible = true
      this.tabChange(2)
    },
    // 切换引导
    toggleGuide() {
      useGuideStore()
        .toggleGuideFlag()
        .then((flag) => {
          if (flag) {
            $notification.success({
              message: this.$tl('p.closeOperationGuide')
            })
          } else {
            $notification.success({
              message: this.$tl('p.openOperationGuide')
            })
          }
        })
    },
    // 切换菜单打开
    toggleMenuMultiple() {
      useGuideStore()
        .toggleMenuFlag()
        .then((flag) => {
          if (flag) {
            $notification.success({
              message: this.$tl('p.multipleMenusExpand')
            })
          } else {
            $notification.success({
              message: this.$tl('p.singleMenuExpand')
            })
          }
        })
    },
    // 页面全屏
    toggleFullScreenFlag() {
      useGuideStore()
        .toggleFullScreenFlag()
        .then((flag) => {
          if (flag) {
            $notification.success({
              message: this.$tl('p.autoExpandContent')
            })
          } else {
            $notification.success({
              message: this.$tl('p.fullScreen_1')
            })
          }
        })
    },
    // 切换滚动条是否显示
    toggleScrollbarFlag() {
      useGuideStore()
        .toggleScrollbarFlag()
        .then((flag) => {
          if (flag) {
            $notification.success({
              message: this.$tl('p.scrollableContent')
            })
          } else {
            $notification.success({
              message: this.$tl('p.hiddenScrollbar')
            })
          }
        })
    },
    // 切换全屏查看日志
    toggleFullscreenViewLog() {
      useGuideStore()
        .toggleFullscreenViewLog()
        .then((fullscreenViewLog) => {
          if (fullscreenViewLog) {
            $notification.success({
              message: this.$tl('p.logDialogFullScreen')
            })
          } else {
            $notification.success({
              message: this.$tl('p.logDialogNonFullScreen')
            })
          }
        })
    },
    toggleCompactView() {
      useGuideStore()
        .toggleCompactView()
        .then((compact) => {
          if (compact) {
            $notification.success({
              message: this.$tl('p.compactMode_1')
            })
          } else {
            $notification.success({
              message: this.$tl('p.looseMode')
            })
          }
        })
    },
    restGuide() {
      useGuideStore()
        .restGuide()
        .then(() => {
          $notification.success({
            message: this.$tl('p.resetOperationGuideSuccess')
          })
        })
    },
    // 彻底退出登录
    logOutAll() {
      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.quitConfirmation'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return useUserStore()
            .logOut()
            .then(() => {
              $notification.success({
                message: this.$tl('c.logoutSuccess')
              })
              localStorage.clear()
              this.$router.replace({
                path: '/login',
                query: {}
              })
            })
        }
      })
    },
    // 切换账号登录
    logOutSwap() {
      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.switchAccountConfirmation'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return useUserStore()
            .logOut()
            .then(() => {
              $notification.success({
                message: this.$tl('c.logoutSuccess')
              })
              useAppStore().changeWorkspace('')
              this.$router.replace({
                path: '/login',
                query: {}
              })
            })
        }
      })
    },
    // 退出登录
    logOut() {
      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.quitSystemConfirmation'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return useUserStore()
            .logOut()
            .then(() => {
              $notification.success({
                message: this.$tl('c.logoutSuccess')
              })
              const query = Object.assign({}, this.$route.query)
              this.$router.replace({
                path: '/login',
                query: query
              })
            })
        }
      })
    },
    // 加载修改密码对话框
    handleUpdatePwd() {
      this.updateNameVisible = true
      this.tabChange(1)
    },
    // 修改密码
    handleUpdatePwdOk() {
      // 判断两次新密码是否一致
      if (this.temp.newPwd !== this.temp.confirmPwd) {
        $notification.error({
          message: this.$tl('p.passwordsNotMatch')
        })
        return
      }
      // 提交修改
      const params = {
        oldPwd: sha1(this.temp.oldPwd),
        newPwd: sha1(this.temp.newPwd)
      }
      this.confirmLoading = true
      updatePwd(params)
        .then((res) => {
          // 修改成功
          if (res.code === 200) {
            // 退出登录
            userStore()
              .logOut()
              .then(() => {
                $notification.success({
                  message: res.msg
                })

                this.updateNameVisible = false
                this.$router.push('/login')
              })
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
      // })
    },
    // 加载修改用户资料对话框
    handleUpdateUser() {
      getUserInfo().then((res) => {
        if (res.code === 200) {
          this.temp = res.data
          this.temp.token = this.getToken()
          //this.temp.md5Token = res.data.md5Token;
          this.updateUserVisible = true
        }
      })
    },
    // 发送邮箱验证码
    sendEmailCode() {
      if (!this.temp.email) {
        $notification.error({
          message: this.$tl('p.emailAddress')
        })
        return
      }
      sendEmailCode(this.temp.email).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
        }
      })
    },
    // 修改用户资料
    handleUpdateUserOk() {
      // 检验表单
      this.$refs['userForm'].validate().then(() => {
        const tempData = Object.assign({}, this.temp)
        delete tempData.token, delete tempData.md5Token
        this.confirmLoading = true
        editUserInfo(tempData)
          .then((res) => {
            // 修改成功
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              // 清空表单校验
              this.$refs['userForm'].resetFields()
              this.updateUserVisible = false
              userStore().refreshUserInfo()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 工作空间切换
    handleWorkspaceChange(item) {
      const cluster = this.myClusterList.find((item2) => {
        return item2.id === item.clusterInfoId
      })
      if (cluster && cluster.url && location.href.indexOf(cluster.url) !== 0) {
        let url = `${cluster.url}/#/${this.$route.fullPath}`.replace(/[\\/]+[\\/]/g, '/').replace(':/', '://')
        url = url.replace(`wid=${this.selectWorkspace.id}`, `wid=${item.id}`)
        // console.log(location.href.indexOf(cluster.url), url);
        location.href = url
      } else {
        appStore()
          .changeWorkspace(item.id)
          .then(() => {
            this.$router
              .push({
                query: { ...this.$route.query, wid: item.id }
              })
              .then(() => {
                useAllMenuStore().restLoadSystemMenus('normal')
                this.reload()
              })
          })
      }
    },
    // 集群切换
    handleClusterChange(item) {
      if (item.url) {
        const url = `${item.url}/#/${this.$route.fullPath}`.replace(/[\\/]+[\\/]/g, '/').replace(':/', '://')
        // console.log(url);
        location.href = url
      } else {
        $notification.error({
          message: this.$tl('p.clusterNotConfigured')
        })
      }
    },
    tabChange(key) {
      if (key === 1) {
        this.temp = { tabActiveKey: key }
      } else if (key == 2) {
        this.temp = { tabActiveKey: key }
        getUserInfo().then((res) => {
          if (res.code === 200) {
            this.temp = { ...this.temp, status: res.data.bindMfa }
            this.$nextTick(() => {
              this.$refs?.twoCode?.focus()
            })
          }
        })
      }
    },

    // 关闭 mfa

    // mfa 状态切换
    openMfaFn() {
      //
      generateMfa().then((res) => {
        if (res.code === 200) {
          Object.assign(this.temp, {
            status: true,
            mfaKey: res.data.mfaKey,
            url: res.data.url,
            needVerify: true,
            showSaveTip: true,
            twoCode: ''
          })
          this.temp = { ...this.temp }

          $notification.info({
            // placement: "",
            message: this.$tl('p.verificationCodeRequired')
          })
        }
      })
    },
    handleBindMfa() {
      // @click="closeMfaFn"
      if (this.temp.needVerify) {
        bindMfa({
          mfa: this.temp.mfaKey,
          twoCode: this.temp.twoCode
        }).then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.temp = { ...this.temp, needVerify: false, twoCode: '' }
          }
        })
      } else {
        $confirm({
          title: this.$tl('c.systemPrompt'),
          zIndex: 1009,
          content: this.$tl('p.disableTwoStepVerificationConfirmation'),
          okText: this.$tl('c.confirm'),
          cancelText: this.$tl('c.cancel'),
          onOk: () => {
            return closeMfa({
              code: this.temp.twoCode
            }).then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })
                this.temp = { ...this.temp, needVerify: false, status: false }
              }
            })
          }
        })
      }
    },
    handleUserlog() {
      this.viewLogVisible = true
    }
  }
}
</script>
<style scoped>
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
</style>
