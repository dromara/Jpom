<template>
  <div class="user-header">
    <a-button-group>
      <!-- 工作空间信息 -->
      <a-button v-if="mode === 'normal'" type="dashed" class="workspace jpom-workspace btn-group-item">
        <div class="workspace-name">
          <a-tooltip placement="bottom">
            <template #title>
              <!-- 【】\u3011 -->
              {{ $t('i18n_8f36f2ede7') }}{{ selectWorkspace.name }} {{ `\u3010` }}{{ $t('i18n_3bf9c5b8af')
              }}{{ selectWorkspace.group || $t('i18n_71dc8feb59') }}
              {{ `\u3011` }}
            </template>
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
          <a-tooltip :title="`${$t('i18n_f668c8c881')}${selectCluster && selectCluster.name}`" placement="bottom">
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
                  <a-button type="link"><RetweetOutlined />{{ $t('i18n_ccb2fdd838') }}</a-button>
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
                        {{ item.name || $t('i18n_71dc8feb59') }}
                        <template v-if="myClusterList.length > 1 && item.clusterInfoId">
                          <!-- 【】\u3010\u3011 -->
                          \u3010{{
                            myClusterList.find((item2) => {
                              return item2.id === item.clusterInfoId
                            }) &&
                            myClusterList.find((item2) => {
                              return item2.id === item.clusterInfoId
                            }).name
                          }}\u3011
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
                          {{ item1.value || $t('i18n_71dc8feb59') }}
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
                              \u3010
                              {{
                                myClusterList.find((item2) => {
                                  return item2.id === item.clusterInfoId
                                }) &&
                                myClusterList.find((item2) => {
                                  return item2.id === item.clusterInfoId
                                }).name
                              }}
                              \u3011
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
                  <a-button type="link"><RetweetOutlined />{{ $t('i18n_d61b8fde35') }}</a-button>
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
              <a-button type="link"> <lock-outlined />{{ $t('i18n_629a6ad325') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="handleUpdateUser">
              <a-button type="link"><profile-outlined /> {{ $t('i18n_d7cc44bc02') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="handleUserlog">
              <a-button type="link"><bars-outlined /> {{ $t('i18n_cda84be2f6') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="customize">
              <a-button type="link"><skin-outlined /> {{ $t('i18n_b4fd7afd31') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="logOut">
              <a-button type="link"> <logout-outlined />{{ $t('i18n_44efd179aa') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="logOutSwap">
              <a-button type="link"> <SwapOutlined />{{ $t('i18n_86c1eb397d') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="logOutAll">
              <a-button type="link"><RestOutlined /> {{ $t('i18n_a795fa52cd') }} </a-button>
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </a-button-group>

    <!-- 修改密码区 -->
    <CustomModal
      v-if="updateNameVisible"
      v-model:open="updateNameVisible"
      destroy-on-close
      :width="'60vw'"
      :title="$t('i18n_629a6ad325')"
      :footer="null"
      :mask-closable="false"
    >
      <a-tabs v-model:active-key="temp.tabActiveKey" @change="tabChange">
        <a-tab-pane :key="1" :tab="$t('i18n_7fc88aeeda')">
          <a-spin tip="Loading..." :spinning="confirmLoading">
            <a-form
              ref="pwdForm"
              :rules="rules"
              :model="temp"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 14 }"
              @finish="handleUpdatePwdOk"
            >
              <a-form-item :label="$t('i18n_01e94436d1')" name="oldPwd">
                <a-input-password v-model:value="temp.oldPwd" :placeholder="$t('i18n_9c19a424dc')" />
              </a-form-item>
              <a-form-item :label="$t('i18n_bf7da0bf02')" name="newPwd">
                <a-input-password v-model:value="temp.newPwd" :placeholder="$t('i18n_abdd7ea830')" />
              </a-form-item>
              <a-form-item :label="$t('i18n_3fbdde139c')" name="confirmPwd">
                <a-input-password v-model:value="temp.confirmPwd" :placeholder="$t('i18n_a7a9a2156a')" />
              </a-form-item>
              <a-form-item>
                <a-row type="flex" justify="center">
                  <a-col :span="2">
                    <a-button type="primary" html-type="submit" :loading="confirmLoading">{{
                      $t('i18n_80cfc33cbe')
                    }}</a-button>
                  </a-col>
                </a-row>
              </a-form-item>
            </a-form>
          </a-spin>
        </a-tab-pane>
        <a-tab-pane :key="2" :tab="$t('i18n_dbad1e89f7')">
          <a-row>
            <a-col :span="24">
              <a-alert v-if="temp.needVerify" type="warning">
                <template #message> {{ $t('i18n_02d9819dda') }} </template>
                <template #description>
                  <ul style="color: red">
                    <li>{{ $t('i18n_0ac9e3e675') }}</li>
                    <li>{{ $t('i18n_8c24b5e19c') }}</li>
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
                <a-form-item :label="$t('i18n_6bf1f392c0')" name="status">
                  <a-switch
                    v-model:checked="temp.status"
                    :checked-children="$t('i18n_4fb95949e5')"
                    disabled
                    :un-checked-children="$t('i18n_4f52df6e44')"
                  />
                </a-form-item>
                <template v-if="temp.needVerify">
                  <a-form-item :label="$t('i18n_22b03c024d')">
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
                <a-form-item v-if="temp.needVerify" :label="$t('i18n_983f59c9d4')" name="twoCode">
                  <a-input ref="twoCode" v-model:value="temp.twoCode" :placeholder="$t('i18n_3f18d14961')" />
                </a-form-item>
                <a-form-item v-if="temp.needVerify">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" html-type="submit">{{ $t('i18n_b7cfa07d78') }}</a-button>
                    </a-col>
                  </a-row>
                </a-form-item>
                <!-- 不能使用  template 包裹 否则验证不能正常启用 -->
                <a-form-item v-if="!temp.needVerify && temp.status" :label="$t('i18n_983f59c9d4')" name="twoCode">
                  <a-input ref="twoCode" v-model:value="temp.twoCode" :placeholder="$t('i18n_3f18d14961')" />
                </a-form-item>
                <a-form-item v-if="!temp.needVerify && temp.status">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" html-type="submit">{{ $t('i18n_e8e3bfbbfe') }}</a-button>
                    </a-col>
                  </a-row>
                </a-form-item>

                <a-form-item v-if="!temp.needVerify && !temp.status">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" @click="openMfaFn">{{ $t('i18n_1b8fff7308') }}</a-button>
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-form>
            </a-col>
            <a-col :span="12">
              <h3 :id="$t('i18n_ceffe5d643')">{{ $t('i18n_ceffe5d643') }}</h3>
              <p v-for="(html, index) in MFA_APP_TIP_ARRAY" :key="index" v-html="html" />
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </CustomModal>
    <!-- 修改用户资料区 -->
    <CustomModal
      v-if="updateUserVisible"
      v-model:open="updateUserVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_ed367abd1a')"
      :mask-closable="false"
      @ok="handleUpdateUserOk"
    >
      <a-form ref="userForm" :rules="rules" :model="temp" :label-col="{ span: 8 }" :wrapper-col="{ span: 15 }">
        <a-form-item :label="$t('i18n_e0f937d57f')" name="token">
          <a-input v-model:value="temp.token" disabled placeholder="Token">
            <template #suffix>
              <a-typography-paragraph style="margin-bottom: 0" :copyable="{ tooltip: true, text: temp.token }">
              </a-typography-paragraph>
            </template>
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('i18n_e6bf31e8e6')" name="md5Token">
          <a-input v-model:value="temp.md5Token" disabled placeholder="Token">
            <template #suffix>
              <a-typography-paragraph style="margin-bottom: 0" :copyable="{ tooltip: true, text: temp.md5Token }">
              </a-typography-paragraph>
            </template>
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('i18n_23eb0e6024')" name="name">
          <a-input v-model:value="temp.name" :placeholder="$t('i18n_23eb0e6024')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_6ab78fa2c4')" name="email">
          <a-input v-model:value="temp.email" :placeholder="$t('i18n_6ab78fa2c4')" />
        </a-form-item>
        <a-form-item v-show="showCode" :label="$t('i18n_e3cf0abd35')" name="code">
          <a-row :gutter="8">
            <a-col :span="15">
              <a-input v-model:value="temp.code" :placeholder="$t('i18n_e3cf0abd35')" />
            </a-col>
            <a-col :span="4">
              <a-button type="primary" :disabled="!temp.email" @click="sendEmailCode">{{
                $t('i18n_c5c3583bfc')
              }}</a-button>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item :label="$t('i18n_55e99f5106')" name="dingDing">
          <a-input v-model:value="temp.dingDing" :placeholder="$t('i18n_55e99f5106')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_2246d128cb')" name="workWx">
          <a-input v-model:value="temp.workWx" :placeholder="$t('i18n_2246d128cb')" />
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 个性配置区 -->
    <CustomModal
      v-if="customizeVisible"
      v-model:open="customizeVisible"
      destroy-on-close
      :title="$t('i18n_cb09b98416')"
      :footer="null"
      :mask-closable="false"
      width="50%"
    >
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-alert banner>
          <template #message> {{ $t('i18n_bf93517805') }},{{ $t('i18n_52b526ab9e') }} </template>
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
        <a-form-item :label="$t('i18n_156af3b3d1')">
          <template #help>{{ $t('i18n_ecdf9093d0') }}</template>

          <a-switch
            :checked-children="$t('i18n_0a60ac8f02')"
            :checked="menuMultipleFlag"
            :un-checked-children="$t('i18n_c9744f45e7')"
            @click="toggleMenuMultiple"
          />
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
        <a-form-item :label="$t('i18n_0113fc41fc')">
          <template #help>{{ $t('i18n_b5fdd886b6') }}</template>

          <a-switch
            :checked-children="$t('i18n_185926bf98')"
            :checked="fullscreenViewLog"
            :un-checked-children="$t('i18n_c5a2c23d89')"
            @click="toggleFullscreenViewLog"
          />
        </a-form-item>
        <a-form-item :label="$t('i18n_5d9c139f38')">
          <a-radio-group v-model:value="themeView" button-style="solid">
            <a-radio-button v-for="item in getSupportThemes" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-radio-button>
          </a-radio-group>

          <template #help>{{ $t('i18n_2b4bb321d7') }}</template>
        </a-form-item>
        <a-form-item :label="$t('i18n_593e04dfad')">
          <a-radio-group v-model:value="menuThemeView" button-style="solid">
            <a-radio-button value="light">{{ $t('i18n_48d0a09bdd') }}</a-radio-button>
            <a-radio-button value="dark">{{ $t('i18n_41e8e8b993') }}</a-radio-button>
          </a-radio-group>

          <template #help>{{ $t('i18n_fbfeb76b33') }}</template>
        </a-form-item>

        <a-form-item :label="$t('i18n_4f50cd2a5e')">
          <a-switch
            :checked-children="$t('i18n_03e59bb33c')"
            :checked="compactView"
            :un-checked-children="$t('i18n_43e534acf9')"
            @click="toggleCompactView"
          />
        </a-form-item>
        <a-form-item :label="$t('i18n_295bb704f5')">
          <template #help>{{ $t('i18n_92f9a3c474') }}</template>

          <a-select v-model:value="locale" style="width: 220px">
            <a-select-option v-for="item in supportLang" :key="item.value" :value="item.value">{{
              item.label
            }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- mfa 提示 -->
    <CustomModal
      v-if="bindMfaTip"
      v-model:open="bindMfaTip"
      destroy-on-close
      :title="$t('i18n_84777ebf8b')"
      :footer="null"
      :mask-closable="false"
      :closable="false"
      :keyboard="false"
    >
      <a-space direction="vertical">
        <a-alert :message="$t('i18n_84777ebf8b')" :description="$t('i18n_4af980516d')" type="error" :closable="false" />
        <a-row align="middle" type="flex" justify="center">
          <a-button type="danger" @click="toBindMfa"> {{ $t('i18n_97a19328a8') }} </a-button>
        </a-row>
      </a-space>
    </CustomModal>
    <!-- 查看操作日志 -->
    <CustomModal
      v-if="viewLogVisible"
      v-model:open="viewLogVisible"
      destroy-on-close
      :width="'90vw'"
      :title="$t('i18n_cda84be2f6')"
      :footer="null"
      :mask-closable="false"
    >
      <user-log v-if="viewLogVisible"></user-log>
    </CustomModal>
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
import { itemGroupBy } from '@/utils/const'
import { MFA_APP_TIP_ARRAY } from '@/utils/const-i18n'
import UserLog from './user-log.vue'
import { mapState } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { useGuideStore } from '@/stores/guide'
import { supportLang } from '@/i18n'
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
      supportLang,
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
          { required: true, message: this.$t('i18n_916ff9eddd'), trigger: 'blur' },
          { max: 10, message: this.$t('i18n_6446b6c707'), trigger: 'blur' },
          { min: 2, message: this.$t('i18n_6446b6c707'), trigger: 'blur' }
        ],

        oldPwd: [
          { required: true, message: this.$t('i18n_9c19a424dc'), trigger: 'blur' },
          { max: 20, message: this.$t('i18n_f4b7c18635'), trigger: 'blur' },
          { min: 6, message: this.$t('i18n_f4b7c18635'), trigger: 'blur' }
        ],

        newPwd: [
          { required: true, message: this.$t('i18n_abdd7ea830'), trigger: 'blur' },
          { max: 20, message: this.$t('i18n_f4b7c18635'), trigger: 'blur' },
          { min: 6, message: this.$t('i18n_f4b7c18635'), trigger: 'blur' }
        ],

        confirmPwd: [
          { required: true, message: this.$t('i18n_a7a9a2156a'), trigger: 'blur' },
          { max: 20, message: this.$t('i18n_f4b7c18635'), trigger: 'blur' },
          { min: 6, message: this.$t('i18n_f4b7c18635'), trigger: 'blur' }
        ],

        email: [
          // { required: true, message: "请输入邮箱", trigger: "blur" }
        ],
        twoCode: [
          { required: true, message: this.$t('i18n_7e866fece6'), trigger: ['change', 'blur'] },
          { pattern: /^\d{6}$/, message: this.$t('i18n_da1abf0865'), trigger: ['change', 'blur'] }
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
    ...mapState(useAppStore, ['getWorkspaceId']),
    ...mapState(useGuideStore, [
      'getGuideCache',
      'getDisabledGuide',
      'getThemeView',
      'getMenuThemeView',
      'getLocale',
      'getSupportThemes'
    ]),
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
        return useGuideStore().getCatchThemeView()
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
              message: this.$t('i18n_fe231ff92f')
            })
          } else {
            $notification.success({
              message: this.$t('i18n_c75d0beca8')
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
              message: this.$t('i18n_63c9d63eeb')
            })
          } else {
            $notification.success({
              message: this.$t('i18n_1498557b2d')
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
              message: this.$t('i18n_ef28d3bff2')
            })
          } else {
            $notification.success({
              message: this.$t('i18n_ba6ea3d480')
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
              message: this.$t('i18n_af51211a73')
            })
          } else {
            $notification.success({
              message: this.$t('i18n_1afdb4a364')
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
              message: this.$t('i18n_82b89bd049')
            })
          } else {
            $notification.success({
              message: this.$t('i18n_57978c11d1')
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
              message: this.$t('i18n_6e60d2fc75')
            })
          } else {
            $notification.success({
              message: this.$t('i18n_702430b89d')
            })
          }
        })
    },
    restGuide() {
      useGuideStore()
        .restGuide()
        .then(() => {
          $notification.success({
            message: this.$t('i18n_dddf944f5f')
          })
        })
    },
    // 彻底退出登录
    logOutAll() {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_8e38d55231'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return useUserStore()
            .logOut()
            .then(() => {
              $notification.success({
                message: this.$t('i18n_499f058a0b')
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_ac783bca36'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return useUserStore()
            .logOut()
            .then(() => {
              $notification.success({
                message: this.$t('i18n_499f058a0b')
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_70b9a2c450'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return useUserStore()
            .logOut()
            .then(() => {
              $notification.success({
                message: this.$t('i18n_499f058a0b')
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
          message: this.$t('i18n_6f15f0beea')
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
          message: this.$t('i18n_2ba4c81587')
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
          message: this.$t('i18n_db2d99ed33')
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
            message: this.$t('i18n_d1b8eaaa9e')
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
          title: this.$t('i18n_c4535759ee'),
          zIndex: 1009,
          content: this.$t('i18n_2b21998b7b'),
          okText: this.$t('i18n_e83a256e4f'),
          cancelText: this.$t('i18n_625fb26b4b'),
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
