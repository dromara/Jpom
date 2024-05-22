<template>
  <div>
    <a-alert type="info" show-icon :message="$tl('p.oauth2Tip')">
      <template #description>
        <ul>
          <li>{{ $tl('p.mfaNotice') }}</li>
          <li>{{ $tl('p.externalAccountBindNotice') }}</li>
          <li>{{ $tl('p.autoCreateUserNotice') }}</li>
        </ul>
      </template>
    </a-alert>
    <a-tabs>
      <a-tab-pane key="dingtalk" :tab="$tl('p.dingtalkScan')">
        <a-form ref="editForm" :model="dingtalk" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$tl('c.isEnabled')" name="enabled">
            <a-switch
              v-model:checked="dingtalk.enabled"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item :label="$tl('c.clientId')" name="clientId">
            <a-input v-model:value="dingtalk.clientId" type="text" :placeholder="$tl('c.clientIdInput')" />
          </a-form-item>
          <a-form-item :label="$tl('c.clientSecret')" name="clientSecret">
            <a-input-password v-model:value="dingtalk.clientSecret" :placeholder="$tl('c.clientSecretInput')" />
          </a-form-item>

          <a-form-item :label="$tl('c.redirectUrl')" name="redirectUri">
            <template #help>{{ $tl('c.referenceUrl') }}{{ `${host}/oauth2-dingtalk` }}</template>
            <a-input v-model:value="dingtalk.redirectUri" type="text" :placeholder="$tl('c.redirectUriInput')" />
          </a-form-item>

          <a-form-item :label="$tl('c.ignoreStateCheck')" name="ignoreCheckState">
            <a-switch
              v-model:checked="dingtalk.ignoreCheckState"
              :checked-children="$tl('c.ignore')"
              :un-checked-children="$tl('c.verify')"
            />
          </a-form-item>
          <a-form-item :label="$tl('c.autoCreateUser')" name="autoCreteUser">
            <a-switch
              v-model:checked="dingtalk.autoCreteUser"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item v-if="dingtalk.autoCreteUser" :label="$tl('c.permissionGroup')" name="permissionGroup">
            <template #help>{{ $tl('c.autoAssignPermissionGroup') }}</template>
            <a-select
              v-model:value="dingtalk.permissionGroup"
              show-search
              :filter-option="
                (input, option) => {
                  const children = option.children && option.children()
                  return (
                    children &&
                    children[0].children &&
                    children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  )
                }
              "
              :placeholder="$tl('c.selectPermissionGroup')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('dingtalk')">{{ $tl('c.submit') }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="feishu" :tab="$tl('p.feishuScan')">
        <a-form ref="editForm" :model="feishu" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$tl('c.isEnabled')" name="enabled">
            <a-switch
              v-model:checked="feishu.enabled"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item :label="$tl('c.clientId')" name="clientId">
            <a-input v-model:value="feishu.clientId" type="text" :placeholder="$tl('c.clientIdInput')" />
          </a-form-item>
          <a-form-item :label="$tl('c.clientSecret')" name="clientSecret">
            <a-input-password v-model:value="feishu.clientSecret" :placeholder="$tl('c.clientSecretInput')" />
          </a-form-item>

          <a-form-item :label="$tl('c.redirectUrl')" name="redirectUri">
            <template #help>{{ $tl('c.referenceUrl') }}{{ `${host}/oauth2-feishu` }}</template>
            <a-input v-model:value="feishu.redirectUri" type="text" :placeholder="$tl('c.redirectUriInput')" />
          </a-form-item>

          <a-form-item :label="$tl('c.ignoreStateCheck')" name="ignoreCheckState">
            <a-switch
              v-model:checked="feishu.ignoreCheckState"
              :checked-children="$tl('c.ignore')"
              :un-checked-children="$tl('c.verify')"
            />
          </a-form-item>
          <a-form-item :label="$tl('c.autoCreateUser')" name="autoCreteUser">
            <a-switch
              v-model:checked="feishu.autoCreteUser"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item v-if="feishu.autoCreteUser" :label="$tl('c.permissionGroup')" name="permissionGroup">
            <template #help>{{ $tl('c.autoAssignPermissionGroup') }}</template>
            <a-select
              v-model:value="feishu.permissionGroup"
              show-search
              :filter-option="
                (input, option) => {
                  const children = option.children && option.children()
                  return (
                    children &&
                    children[0].children &&
                    children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  )
                }
              "
              :placeholder="$tl('c.selectPermissionGroup')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('feishu')">{{ $tl('c.submit') }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="wechat_enterprise" :tab="$tl('p.wechatWorkScan')">
        <a-form
          ref="editForm"
          :model="wechat_enterprise"
          :rules="rules"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item :label="$tl('c.isEnabled')" name="enabled">
            <a-switch
              v-model:checked="wechat_enterprise.enabled"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item :label="$tl('p.webAppId')" name="agentId">
            <a-input v-model:value="wechat_enterprise.agentId" type="text" :placeholder="$tl('p.webAppIdInput')" />
          </a-form-item>
          <a-form-item :label="$tl('c.clientId')" name="clientId">
            <a-input v-model:value="wechat_enterprise.clientId" type="text" :placeholder="$tl('c.clientIdInput')" />
          </a-form-item>
          <a-form-item :label="$tl('c.clientSecret')" name="clientSecret">
            <a-input-password
              v-model:value="wechat_enterprise.clientSecret"
              :placeholder="$tl('c.clientSecretInput')"
            />
          </a-form-item>

          <a-form-item :label="$tl('c.redirectUrl')" name="redirectUri">
            <template #help>{{ $tl('c.referenceUrl') }}{{ `${host}/oauth2-wechat_enterprise` }}</template>
            <a-input
              v-model:value="wechat_enterprise.redirectUri"
              type="text"
              :placeholder="$tl('c.redirectUriInput')"
            />
          </a-form-item>

          <a-form-item :label="$tl('c.ignoreStateCheck')" name="ignoreCheckState">
            <a-switch
              v-model:checked="wechat_enterprise.ignoreCheckState"
              :checked-children="$tl('c.ignore')"
              :un-checked-children="$tl('c.verify')"
            />
          </a-form-item>
          <a-form-item :label="$tl('c.autoCreateUser')" name="autoCreteUser">
            <a-switch
              v-model:checked="wechat_enterprise.autoCreteUser"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item v-if="wechat_enterprise.autoCreteUser" :label="$tl('c.permissionGroup')" name="permissionGroup">
            <template #help>{{ $tl('c.autoAssignPermissionGroup') }}</template>
            <a-select
              v-model:value="wechat_enterprise.permissionGroup"
              show-search
              :filter-option="
                (input, option) => {
                  const children = option.children && option.children()
                  return (
                    children &&
                    children[0].children &&
                    children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  )
                }
              "
              :placeholder="$tl('c.selectPermissionGroup')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('wechat_enterprise')">{{ $tl('c.submit') }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="maxkey" tab="MaxKey">
        <a-form ref="editForm" :model="maxkey" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$tl('c.isEnabled')" name="enabled">
            <a-switch
              v-model:checked="maxkey.enabled"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item :label="$tl('c.clientId')" name="clientId">
            <a-input v-model:value="maxkey.clientId" type="text" :placeholder="$tl('c.clientIdInput')" />
          </a-form-item>
          <a-form-item :label="$tl('c.clientSecret')" name="clientSecret">
            <a-input-password v-model:value="maxkey.clientSecret" :placeholder="$tl('c.clientSecretInput')" />
          </a-form-item>
          <a-form-item :label="$tl('p.authorizationUrl')" name="authorizationUri">
            <a-input
              v-model:value="maxkey.authorizationUri"
              type="text"
              :placeholder="$tl('p.authorizationUriInput')"
            />
          </a-form-item>
          <a-form-item :label="$tl('p.tokenUrl')" name="accessTokenUri">
            <a-input v-model:value="maxkey.accessTokenUri" type="text" :placeholder="$tl('p.accessTokenUriInput')" />
          </a-form-item>
          <a-form-item :label="$tl('p.userInfoUrl')" name="userInfoUri">
            <a-input v-model:value="maxkey.userInfoUri" type="text" :placeholder="$tl('p.userInfoUriInput')" />
          </a-form-item>
          <a-form-item :label="$tl('c.redirectUrl')" name="redirectUri">
            <template #help>{{ $tl('c.referenceUrl') }}{{ `${host}/oauth2-maxkey` }}</template>
            <a-input v-model:value="maxkey.redirectUri" type="text" :placeholder="$tl('c.redirectUriInput')" />
          </a-form-item>

          <a-form-item :label="$tl('c.ignoreStateCheck')" name="ignoreCheckState">
            <a-switch
              v-model:checked="maxkey.ignoreCheckState"
              :checked-children="$tl('c.ignore')"
              :un-checked-children="$tl('c.verify')"
            />
          </a-form-item>

          <a-form-item :label="$tl('c.autoCreateUser')" name="autoCreteUser">
            <a-switch
              v-model:checked="maxkey.autoCreteUser"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item v-if="maxkey.autoCreteUser" :label="$tl('c.permissionGroup')" name="permissionGroup">
            <template #help>{{ $tl('c.autoAssignPermissionGroup') }}</template>
            <a-select
              v-model:value="maxkey.permissionGroup"
              show-search
              :filter-option="
                (input, option) => {
                  const children = option.children && option.children()
                  return (
                    children &&
                    children[0].children &&
                    children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  )
                }
              "
              :placeholder="$tl('c.selectPermissionGroup')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('maxkey')">{{ $tl('c.submit') }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="gitee" tab="Gitee">
        <a-form ref="editForm" :model="gitee" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$tl('c.isEnabled')" name="enabled">
            <a-switch
              v-model:checked="gitee.enabled"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item :label="$tl('c.clientId')" name="clientId">
            <a-input v-model:value="gitee.clientId" type="text" :placeholder="$tl('c.clientIdInput')" />
          </a-form-item>
          <a-form-item :label="$tl('c.clientSecret')" name="clientSecret">
            <a-input-password v-model:value="gitee.clientSecret" :placeholder="$tl('c.clientSecretInput')" />
          </a-form-item>
          <a-form-item :label="$tl('c.redirectUrl')" name="redirectUri">
            <template #help>{{ $tl('c.referenceUrl') }}{{ `${host}/oauth2-gitee` }}</template>
            <a-input v-model:value="gitee.redirectUri" type="text" :placeholder="$tl('c.redirectUriInput')" />
          </a-form-item>

          <a-form-item :label="$tl('c.ignoreStateCheck')" name="ignoreCheckState">
            <a-switch
              v-model:checked="gitee.ignoreCheckState"
              :checked-children="$tl('c.ignore')"
              :un-checked-children="$tl('c.verify')"
            />
          </a-form-item>

          <a-form-item :label="$tl('c.autoCreateUser')" name="autoCreteUser">
            <a-switch
              v-model:checked="gitee.autoCreteUser"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item v-if="gitee.autoCreteUser" :label="$tl('c.permissionGroup')" name="permissionGroup">
            <template #help>{{ $tl('c.autoAssignPermissionGroup') }}</template>
            <a-select
              v-model:value="gitee.permissionGroup"
              show-search
              :filter-option="
                (input, option) => {
                  const children = option.children && option.children()
                  return (
                    children &&
                    children[0].children &&
                    children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  )
                }
              "
              :placeholder="$tl('c.selectPermissionGroup')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('gitee')">{{ $tl('c.submit') }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="mygitlab" :tab="$tl('p.selfHostedGitlab')">
        <a-form ref="editForm" :model="mygitlab" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$tl('c.isEnabled')" name="enabled">
            <a-switch
              v-model:checked="mygitlab.enabled"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item :label="$tl('p.serviceAddress')" name="host">
            <template #help>{{ $tl('p.selfHostedGitlabUrl') }}</template>
            <a-input v-model:value="mygitlab.host" type="text" :placeholder="$tl('p.serviceAddressInput')" />
          </a-form-item>
          <a-form-item :label="$tl('c.clientId')" name="clientId">
            <a-input v-model:value="mygitlab.clientId" type="text" :placeholder="$tl('c.clientIdInput')" />
          </a-form-item>
          <a-form-item :label="$tl('c.clientSecret')" name="clientSecret">
            <a-input-password v-model:value="mygitlab.clientSecret" :placeholder="$tl('c.clientSecretInput')" />
          </a-form-item>

          <a-form-item :label="$tl('c.redirectUrl')" name="redirectUri">
            <template #help>{{ $tl('c.referenceUrl') }}{{ `${host}/oauth2-mygitlab` }}</template>
            <a-input v-model:value="mygitlab.redirectUri" type="text" :placeholder="$tl('c.redirectUriInput')" />
          </a-form-item>

          <a-form-item :label="$tl('c.ignoreStateCheck')" name="ignoreCheckState">
            <a-switch
              v-model:checked="mygitlab.ignoreCheckState"
              :checked-children="$tl('c.ignore')"
              :un-checked-children="$tl('c.verify')"
            />
          </a-form-item>

          <a-form-item :label="$tl('c.autoCreateUser')" name="autoCreteUser">
            <a-switch
              v-model:checked="mygitlab.autoCreteUser"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item v-if="mygitlab.autoCreteUser" :label="$tl('c.permissionGroup')" name="permissionGroup">
            <template #help>{{ $tl('c.autoAssignPermissionGroup') }}</template>
            <a-select
              v-model:value="mygitlab.permissionGroup"
              show-search
              :filter-option="
                (input, option) => {
                  const children = option.children && option.children()
                  return (
                    children &&
                    children[0].children &&
                    children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  )
                }
              "
              :placeholder="$tl('c.selectPermissionGroup')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('mygitlab')">{{ $tl('c.submit') }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>

      <a-tab-pane key="github" tab="Github">
        <a-form ref="editForm" :model="github" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$tl('c.isEnabled')" name="enabled">
            <a-switch
              v-model:checked="github.enabled"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item :label="$tl('c.clientId')" name="clientId">
            <a-input v-model:value="github.clientId" type="text" :placeholder="$tl('c.clientIdInput')" />
          </a-form-item>
          <a-form-item :label="$tl('c.clientSecret')" name="clientSecret">
            <a-input-password v-model:value="github.clientSecret" :placeholder="$tl('c.clientSecretInput')" />
          </a-form-item>

          <a-form-item :label="$tl('c.redirectUrl')" name="redirectUri">
            <template #help>{{ $tl('c.referenceUrl') }}{{ `${host}/oauth2-github` }}</template>
            <a-input v-model:value="github.redirectUri" type="text" :placeholder="$tl('c.redirectUriInput')" />
          </a-form-item>

          <a-form-item :label="$tl('c.ignoreStateCheck')" name="ignoreCheckState">
            <a-switch
              v-model:checked="github.ignoreCheckState"
              :checked-children="$tl('c.ignore')"
              :un-checked-children="$tl('c.verify')"
            />
          </a-form-item>

          <a-form-item :label="$tl('c.autoCreateUser')" name="autoCreteUser">
            <a-switch
              v-model:checked="github.autoCreteUser"
              :checked-children="$tl('c.enable')"
              :un-checked-children="$tl('c.disable')"
            />
          </a-form-item>
          <a-form-item v-if="github.autoCreteUser" :label="$tl('c.permissionGroup')" name="permissionGroup">
            <template #help>{{ $tl('c.autoAssignPermissionGroup') }}</template>
            <a-select
              v-model:value="github.permissionGroup"
              show-search
              :filter-option="
                (input, option) => {
                  const children = option.children && option.children()
                  return (
                    children &&
                    children[0].children &&
                    children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  )
                }
              "
              :placeholder="$tl('c.selectPermissionGroup')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('github')">{{ $tl('c.submit') }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import { oauthConfigOauth2, oauthConfigOauth2Save } from '@/api/system'
import { getUserPermissionListAll } from '@/api/user/user-permission'
export default {
  data() {
    return {
      maxkey: {},
      gitee: {},
      github: {},
      dingtalk: {},
      feishu: {},
      mygitlab: {},
      wechat_enterprise: {},
      rules: {},
      provides: ['gitee', 'maxkey', 'github', 'dingtalk', 'feishu', 'mygitlab', 'wechat_enterprise'],
      host: '',
      permissionGroup: []
    }
  },
  mounted() {
    this.host = `${location.protocol}//${location.host}`
    this.loadData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.system.oauthConfig.${key}`, ...args)
    },
    // load data
    loadData() {
      this.provides.forEach((item) => {
        oauthConfigOauth2({
          provide: item
        }).then((res) => {
          if (res.code === 200) {
            const permissionGroup = res.data?.permissionGroup?.split('@') || []
            this[item] = Object.assign(res.data || {}, { provide: item, permissionGroup: permissionGroup })
          }
        })
      })
      this.listUserPermissionListAll()
    },
    // submit
    onSubmit(key) {
      let data = this[key]
      data = { ...data, permissionGroup: data.permissionGroup.join('@') }
      oauthConfigOauth2Save(data).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
        }
      })
    },
    listUserPermissionListAll() {
      getUserPermissionListAll().then((res) => {
        if (res.code === 200 && res.data) {
          this.permissionGroup = res.data
        }
      })
    }
  }
}
</script>
