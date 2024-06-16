<template>
  <div class="user-header">
    <a-button-group>
      <!-- 工作空间信息 -->
      <a-button v-if="mode === 'normal'" type="dashed" class="workspace jpom-workspace btn-group-item">
        <div class="workspace-name">
          <a-tooltip
            :title="`${$t('pages.layout.user-header.8d20cb3f')}${selectWorkspace.name} ${$t(
              'pages.layout.user-header.12d0e469'
            )}${selectWorkspace.group || $t('pages.layout.user-header.50acc5d6')}${$t(
              'pages.layout.user-header.26e6628f'
            )}`"
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
          <a-tooltip
            :title="`${$t('pages.layout.user-header.c2dfe194')}${selectCluster && selectCluster.name}`"
            placement="bottom"
          >
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
                  <a-button type="link"><RetweetOutlined />{{ $t('pages.layout.user-header.4e2553dc') }}</a-button>
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
                        {{ item.name || $t('pages.layout.user-header.50acc5d6') }}
                        <template v-if="myClusterList.length > 1 && item.clusterInfoId">
                          {{ $t('pages.layout.user-header.84fee7a4')
                          }}{{
                            myClusterList.find((item2) => {
                              return item2.id === item.clusterInfoId
                            }) &&
                            myClusterList.find((item2) => {
                              return item2.id === item.clusterInfoId
                            }).name
                          }}{{ $t('pages.layout.user-header.26e6628f') }}
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
                          {{ item1.value || $t('pages.layout.user-header.50acc5d6') }}
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
                              {{ $t('pages.layout.user-header.84fee7a4')
                              }}{{
                                myClusterList.find((item2) => {
                                  return item2.id === item.clusterInfoId
                                }) &&
                                myClusterList.find((item2) => {
                                  return item2.id === item.clusterInfoId
                                }).name
                              }}{{ $t('pages.layout.user-header.26e6628f') }}
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
                  <a-button type="link"><RetweetOutlined />{{ $t('pages.layout.user-header.85538864') }}</a-button>
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
              <a-button type="link"> <lock-outlined />{{ $t('pages.layout.user-header.3bab6668') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="handleUpdateUser">
              <a-button type="link"><profile-outlined /> {{ $t('pages.layout.user-header.fd8cec21') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="handleUserlog">
              <a-button type="link"><bars-outlined /> {{ $t('pages.layout.user-header.86d58c89') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="customize">
              <a-button type="link"><skin-outlined /> {{ $t('pages.layout.user-header.dca6da91') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="logOut">
              <a-button type="link"> <logout-outlined />{{ $t('pages.layout.user-header.d2fe58b4') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="logOutSwap">
              <a-button type="link"> <SwapOutlined />{{ $t('pages.layout.user-header.1da0b754') }} </a-button>
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item @click="logOutAll">
              <a-button type="link"><RestOutlined /> {{ $t('pages.layout.user-header.6892c9ac') }} </a-button>
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
      :title="$t('pages.layout.user-header.3bab6668')"
      :footer="null"
      :mask-closable="false"
    >
      <a-tabs v-model:active-key="temp.tabActiveKey" @change="tabChange">
        <a-tab-pane :key="1" :tab="$t('pages.layout.user-header.cf2bc037')">
          <a-spin tip="Loading..." :spinning="confirmLoading">
            <a-form
              ref="pwdForm"
              :rules="rules"
              :model="temp"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 14 }"
              @finish="handleUpdatePwdOk"
            >
              <a-form-item :label="$t('pages.layout.user-header.1333b135')" name="oldPwd">
                <a-input-password v-model:value="temp.oldPwd" :placeholder="$t('pages.layout.user-header.eda355b6')" />
              </a-form-item>
              <a-form-item :label="$t('pages.layout.user-header.4cb8151c')" name="newPwd">
                <a-input-password v-model:value="temp.newPwd" :placeholder="$t('pages.layout.user-header.4575df08')" />
              </a-form-item>
              <a-form-item :label="$t('pages.layout.user-header.fd09d95a')" name="confirmPwd">
                <a-input-password
                  v-model:value="temp.confirmPwd"
                  :placeholder="$t('pages.layout.user-header.b9c21ba3')"
                />
              </a-form-item>
              <a-form-item>
                <a-row type="flex" justify="center">
                  <a-col :span="2">
                    <a-button type="primary" html-type="submit" :loading="confirmLoading">{{
                      $t('pages.layout.user-header.59bc9c6e')
                    }}</a-button>
                  </a-col>
                </a-row>
              </a-form-item>
            </a-form>
          </a-spin>
        </a-tab-pane>
        <a-tab-pane :key="2" :tab="$t('pages.layout.user-header.80647404')">
          <a-row>
            <a-col :span="24">
              <a-alert v-if="temp.needVerify" type="warning">
                <template #message> {{ $t('pages.layout.user-header.85ddc1a3') }} </template>
                <template #description>
                  <ul style="color: red">
                    <li>{{ $t('pages.layout.user-header.3aee366c') }}</li>
                    <li>{{ $t('pages.layout.user-header.dc59d40') }}</li>
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
                <a-form-item :label="$t('pages.layout.user-header.8a8e4923')" name="status">
                  <a-switch
                    v-model:checked="temp.status"
                    :checked-children="$t('pages.layout.user-header.690faca2')"
                    disabled
                    :un-checked-children="$t('pages.layout.user-header.327893d5')"
                  />
                </a-form-item>
                <template v-if="temp.needVerify">
                  <a-form-item :label="$t('pages.layout.user-header.e3ff2149')">
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
                <a-form-item v-if="temp.needVerify" :label="$t('pages.layout.user-header.f8317f5c')" name="twoCode">
                  <a-input
                    ref="twoCode"
                    v-model:value="temp.twoCode"
                    :placeholder="$t('pages.layout.user-header.8a6e74c3')"
                  />
                </a-form-item>
                <a-form-item v-if="temp.needVerify">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" html-type="submit">{{
                        $t('pages.layout.user-header.d1a3fb5f')
                      }}</a-button>
                    </a-col>
                  </a-row>
                </a-form-item>
                <!-- 不能使用  template 包裹 否则验证不能正常启用 -->
                <a-form-item
                  v-if="!temp.needVerify && temp.status"
                  :label="$t('pages.layout.user-header.f8317f5c')"
                  name="twoCode"
                >
                  <a-input
                    ref="twoCode"
                    v-model:value="temp.twoCode"
                    :placeholder="$t('pages.layout.user-header.8a6e74c3')"
                  />
                </a-form-item>
                <a-form-item v-if="!temp.needVerify && temp.status">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" html-type="submit">{{
                        $t('pages.layout.user-header.1a20a2e7')
                      }}</a-button>
                    </a-col>
                  </a-row>
                </a-form-item>

                <a-form-item v-if="!temp.needVerify && !temp.status">
                  <a-row type="flex" justify="center">
                    <a-col :span="2">
                      <a-button type="primary" @click="openMfaFn">{{
                        $t('pages.layout.user-header.f0d95575')
                      }}</a-button>
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-form>
            </a-col>
            <a-col :span="12">
              <h3 :id="$t('pages.layout.user-header.d0d351d5')">{{ $t('pages.layout.user-header.d0d351d5') }}</h3>
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
      :title="$t('pages.layout.user-header.ef947269')"
      :mask-closable="false"
      @ok="handleUpdateUserOk"
    >
      <a-form ref="userForm" :rules="rules" :model="temp" :label-col="{ span: 8 }" :wrapper-col="{ span: 15 }">
        <a-form-item :label="$t('pages.layout.user-header.135d4403')" name="token">
          <a-input v-model:value="temp.token" disabled placeholder="Token">
            <template #suffix>
              <a-typography-paragraph style="margin-bottom: 0" :copyable="{ tooltip: true, text: temp.token }">
              </a-typography-paragraph>
            </template>
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('pages.layout.user-header.dd885a11')" name="md5Token">
          <a-input v-model:value="temp.md5Token" disabled placeholder="Token">
            <template #suffix>
              <a-typography-paragraph style="margin-bottom: 0" :copyable="{ tooltip: true, text: temp.md5Token }">
              </a-typography-paragraph>
            </template>
          </a-input>
        </a-form-item>
        <a-form-item :label="$t('pages.layout.user-header.57c3af39')" name="name">
          <a-input v-model:value="temp.name" :placeholder="$t('pages.layout.user-header.57c3af39')" />
        </a-form-item>
        <a-form-item :label="$t('pages.layout.user-header.55103071')" name="email">
          <a-input v-model:value="temp.email" :placeholder="$t('pages.layout.user-header.55103071')" />
        </a-form-item>
        <a-form-item v-show="showCode" :label="$t('pages.layout.user-header.989c1e9a')" name="code">
          <a-row :gutter="8">
            <a-col :span="15">
              <a-input v-model:value="temp.code" :placeholder="$t('pages.layout.user-header.989c1e9a')" />
            </a-col>
            <a-col :span="4">
              <a-button type="primary" :disabled="!temp.email" @click="sendEmailCode">{{
                $t('pages.layout.user-header.26b1dc93')
              }}</a-button>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item :label="$t('pages.layout.user-header.dd43783d')" name="dingDing">
          <a-input v-model:value="temp.dingDing" :placeholder="$t('pages.layout.user-header.dd43783d')" />
        </a-form-item>
        <a-form-item :label="$t('pages.layout.user-header.a3bfcd9e')" name="workWx">
          <a-input v-model:value="temp.workWx" :placeholder="$t('pages.layout.user-header.a3bfcd9e')" />
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 个性配置区 -->
    <CustomModal
      v-if="customizeVisible"
      v-model:open="customizeVisible"
      destroy-on-close
      :title="$t('pages.layout.user-header.3d14e1fe')"
      :footer="null"
      :mask-closable="false"
      width="50%"
    >
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-alert banner>
          <template #message>
            {{ $t('pages.layout.user-header.321ca5ab') }},{{ $t('pages.layout.user-header.681d0c4d') }}
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
        <a-form-item :label="$t('pages.layout.user-header.a04d6e8d')">
          <template #help>{{ $t('pages.layout.user-header.158342fd') }}</template>

          <a-switch
            :checked-children="$t('pages.layout.user-header.f5bb2364')"
            :checked="menuMultipleFlag"
            :un-checked-children="$t('pages.layout.user-header.5edb2e8a')"
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
        <a-form-item :label="$t('pages.layout.user-header.a0f7ed4f')">
          <template #help>{{ $t('pages.layout.user-header.378c8cf6') }}</template>

          <a-switch
            :checked-children="$t('pages.layout.user-header.4fc84727')"
            :checked="fullscreenViewLog"
            :un-checked-children="$t('pages.layout.user-header.bcb85748')"
            @click="toggleFullscreenViewLog"
          />
        </a-form-item>
        <a-form-item :label="$t('pages.layout.user-header.cf6a1e7a')">
          <a-radio-group v-model:value="themeView" button-style="solid">
            <a-radio-button v-for="item in getSupportThemes" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-radio-button>
          </a-radio-group>

          <template #help>{{ $t('pages.layout.user-header.ad301071') }}</template>
        </a-form-item>
        <a-form-item :label="$t('pages.layout.user-header.e1e68c8b')">
          <a-radio-group v-model:value="menuThemeView" button-style="solid">
            <a-radio-button value="light">{{ $t('pages.layout.user-header.a9bad4a1') }}</a-radio-button>
            <a-radio-button value="dark">{{ $t('pages.layout.user-header.67b903b1') }}</a-radio-button>
          </a-radio-group>

          <template #help>{{ $t('pages.layout.user-header.1c0c4705') }}</template>
        </a-form-item>

        <a-form-item :label="$t('pages.layout.user-header.15692164')">
          <a-switch
            :checked-children="$t('pages.layout.user-header.e39a73de')"
            :checked="compactView"
            :un-checked-children="$t('pages.layout.user-header.96425f5')"
            @click="toggleCompactView"
          />

          <template #help>
            {{ $t('pages.layout.user-header.1538a492') }}({{ $t('pages.layout.user-header.bb6fe415') }})
          </template>
        </a-form-item>
        <a-form-item :label="$t('pages.layout.user-header.ee091a03')">
          <template #help>{{ $t('pages.layout.user-header.8263b5e7') }}</template>
          <a-radio-group v-model:value="locale" button-style="solid">
            <a-radio-button v-for="item in supportLang" :key="item.value" :value="item.value">{{
              item.label
            }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- mfa 提示 -->
    <CustomModal
      v-if="bindMfaTip"
      v-model:open="bindMfaTip"
      destroy-on-close
      :title="$t('pages.layout.user-header.49889c30')"
      :footer="null"
      :mask-closable="false"
      :closable="false"
      :keyboard="false"
    >
      <a-space direction="vertical">
        <a-alert
          :message="$t('pages.layout.user-header.49889c30')"
          :description="$t('pages.layout.user-header.e805efb')"
          type="error"
          :closable="false"
        />
        <a-row align="middle" type="flex" justify="center">
          <a-button type="danger" @click="toBindMfa"> {{ $t('pages.layout.user-header.c8507450') }} </a-button>
        </a-row>
      </a-space>
    </CustomModal>
    <!-- 查看操作日志 -->
    <CustomModal
      v-if="viewLogVisible"
      v-model:open="viewLogVisible"
      destroy-on-close
      :width="'90vw'"
      :title="$t('pages.layout.user-header.86d58c89')"
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
          { required: true, message: this.$t('pages.layout.user-header.9b5a95d2'), trigger: 'blur' },
          { max: 10, message: this.$t('pages.layout.user-header.dad8bc7b'), trigger: 'blur' },
          { min: 2, message: this.$t('pages.layout.user-header.dad8bc7b'), trigger: 'blur' }
        ],

        oldPwd: [
          { required: true, message: this.$t('pages.layout.user-header.eda355b6'), trigger: 'blur' },
          { max: 20, message: this.$t('pages.layout.user-header.254a970b'), trigger: 'blur' },
          { min: 6, message: this.$t('pages.layout.user-header.254a970b'), trigger: 'blur' }
        ],

        newPwd: [
          { required: true, message: this.$t('pages.layout.user-header.4575df08'), trigger: 'blur' },
          { max: 20, message: this.$t('pages.layout.user-header.254a970b'), trigger: 'blur' },
          { min: 6, message: this.$t('pages.layout.user-header.254a970b'), trigger: 'blur' }
        ],

        confirmPwd: [
          { required: true, message: this.$t('pages.layout.user-header.b9c21ba3'), trigger: 'blur' },
          { max: 20, message: this.$t('pages.layout.user-header.254a970b'), trigger: 'blur' },
          { min: 6, message: this.$t('pages.layout.user-header.254a970b'), trigger: 'blur' }
        ],

        email: [
          // { required: true, message: "请输入邮箱", trigger: "blur" }
        ],
        twoCode: [
          { required: true, message: this.$t('pages.layout.user-header.e794e5fe'), trigger: ['change', 'blur'] },
          { pattern: /^\d{6}$/, message: this.$t('pages.layout.user-header.cf89eb17'), trigger: ['change', 'blur'] }
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
              message: this.$t('pages.layout.user-header.e805511')
            })
          } else {
            $notification.success({
              message: this.$t('pages.layout.user-header.8e6ecbdf')
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
              message: this.$t('pages.layout.user-header.8ef644a1')
            })
          } else {
            $notification.success({
              message: this.$t('pages.layout.user-header.38360e9e')
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
              message: this.$t('pages.layout.user-header.cf4f7e81')
            })
          } else {
            $notification.success({
              message: this.$t('pages.layout.user-header.1f597ac0')
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
              message: this.$t('pages.layout.user-header.c688eb3d')
            })
          } else {
            $notification.success({
              message: this.$t('pages.layout.user-header.d0b833a')
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
              message: this.$t('pages.layout.user-header.401a5b70')
            })
          } else {
            $notification.success({
              message: this.$t('pages.layout.user-header.17b81d98')
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
              message: this.$t('pages.layout.user-header.60dcf8ca')
            })
          } else {
            $notification.success({
              message: this.$t('pages.layout.user-header.c08b9c0c')
            })
          }
        })
    },
    restGuide() {
      useGuideStore()
        .restGuide()
        .then(() => {
          $notification.success({
            message: this.$t('pages.layout.user-header.3ea6d68c')
          })
        })
    },
    // 彻底退出登录
    logOutAll() {
      $confirm({
        title: this.$t('pages.layout.user-header.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.layout.user-header.f63ebe16'),
        okText: this.$t('pages.layout.user-header.7da4a591'),
        cancelText: this.$t('pages.layout.user-header.43105e21'),
        onOk: () => {
          return useUserStore()
            .logOut()
            .then(() => {
              $notification.success({
                message: this.$t('pages.layout.user-header.ba1c6d1f')
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
        title: this.$t('pages.layout.user-header.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.layout.user-header.ed166c40'),
        okText: this.$t('pages.layout.user-header.7da4a591'),
        cancelText: this.$t('pages.layout.user-header.43105e21'),
        onOk: () => {
          return useUserStore()
            .logOut()
            .then(() => {
              $notification.success({
                message: this.$t('pages.layout.user-header.ba1c6d1f')
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
        title: this.$t('pages.layout.user-header.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.layout.user-header.9f940811'),
        okText: this.$t('pages.layout.user-header.7da4a591'),
        cancelText: this.$t('pages.layout.user-header.43105e21'),
        onOk: () => {
          return useUserStore()
            .logOut()
            .then(() => {
              $notification.success({
                message: this.$t('pages.layout.user-header.ba1c6d1f')
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
          message: this.$t('pages.layout.user-header.12d0b15b')
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
          message: this.$t('pages.layout.user-header.4fc329c6')
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
          message: this.$t('pages.layout.user-header.b29d1268')
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
            message: this.$t('pages.layout.user-header.ee23f800')
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
          title: this.$t('pages.layout.user-header.a8fe4c17'),
          zIndex: 1009,
          content: this.$t('pages.layout.user-header.b34c9126'),
          okText: this.$t('pages.layout.user-header.7da4a591'),
          cancelText: this.$t('pages.layout.user-header.43105e21'),
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
