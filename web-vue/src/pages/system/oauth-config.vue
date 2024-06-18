<template>
  <div>
    <a-alert type="info" show-icon :message="$t('i18n_db4b998fbd')">
      <template #description>
        <ul>
          <li>{{ $t('i18n_588e33b660') }}</li>
          <li>{{ $t('i18n_66238e0917') }}</li>
          <li>{{ $t('i18n_d301fdfc20') }}</li>
        </ul>
      </template>
    </a-alert>
    <a-tabs>
      <a-tab-pane key="dingtalk" :tab="$t('i18n_9e4ae8a24f')">
        <a-form ref="editForm" :model="dingtalk" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$t('i18n_780afeac65')" name="enabled">
            <a-switch
              v-model:checked="dingtalk.enabled"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item :label="$t('i18n_99593f7623')" name="clientId">
            <a-input v-model:value="dingtalk.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_e0ec07be7d')" name="clientSecret">
            <a-input-password v-model:value="dingtalk.clientSecret" :placeholder="$t('i18n_52c6af8174')" />
          </a-form-item>

          <a-form-item :label="$t('i18n_51d47ddc69')" name="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-dingtalk` }}</template>
            <a-input v-model:value="dingtalk.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </a-form-item>

          <a-form-item :label="$t('i18n_953357d914')" name="ignoreCheckState">
            <a-switch
              v-model:checked="dingtalk.ignoreCheckState"
              :checked-children="$t('i18n_c0d5d68f5f')"
              :un-checked-children="$t('i18n_b7579706a3')"
            />
          </a-form-item>
          <a-form-item :label="$t('i18n_2e1f215c5d')" name="autoCreteUser">
            <a-switch
              v-model:checked="dingtalk.autoCreteUser"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item v-if="dingtalk.autoCreteUser" :label="$t('i18n_f49dfdace4')" name="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
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
              :placeholder="$t('i18n_72d14a3890')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('dingtalk')">{{ $t('i18n_939d5345ad') }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="feishu" :tab="$t('i18n_a436c94494')">
        <a-form ref="editForm" :model="feishu" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$t('i18n_780afeac65')" name="enabled">
            <a-switch
              v-model:checked="feishu.enabled"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item :label="$t('i18n_99593f7623')" name="clientId">
            <a-input v-model:value="feishu.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_e0ec07be7d')" name="clientSecret">
            <a-input-password v-model:value="feishu.clientSecret" :placeholder="$t('i18n_52c6af8174')" />
          </a-form-item>

          <a-form-item :label="$t('i18n_51d47ddc69')" name="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-feishu` }}</template>
            <a-input v-model:value="feishu.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </a-form-item>

          <a-form-item :label="$t('i18n_953357d914')" name="ignoreCheckState">
            <a-switch
              v-model:checked="feishu.ignoreCheckState"
              :checked-children="$t('i18n_c0d5d68f5f')"
              :un-checked-children="$t('i18n_b7579706a3')"
            />
          </a-form-item>
          <a-form-item :label="$t('i18n_2e1f215c5d')" name="autoCreteUser">
            <a-switch
              v-model:checked="feishu.autoCreteUser"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item v-if="feishu.autoCreteUser" :label="$t('i18n_f49dfdace4')" name="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
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
              :placeholder="$t('i18n_72d14a3890')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('feishu')">{{ $t('i18n_939d5345ad') }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="wechat_enterprise" :tab="$t('i18n_9282b1e5da')">
        <a-form
          ref="editForm"
          :model="wechat_enterprise"
          :rules="rules"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item :label="$t('i18n_780afeac65')" name="enabled">
            <a-switch
              v-model:checked="wechat_enterprise.enabled"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item :label="$t('i18n_f66847edb4')" name="agentId">
            <a-input v-model:value="wechat_enterprise.agentId" type="text" :placeholder="$t('i18n_68c55772ca')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_99593f7623')" name="clientId">
            <a-input v-model:value="wechat_enterprise.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_e0ec07be7d')" name="clientSecret">
            <a-input-password v-model:value="wechat_enterprise.clientSecret" :placeholder="$t('i18n_52c6af8174')" />
          </a-form-item>

          <a-form-item :label="$t('i18n_51d47ddc69')" name="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-wechat_enterprise` }}</template>
            <a-input v-model:value="wechat_enterprise.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </a-form-item>

          <a-form-item :label="$t('i18n_953357d914')" name="ignoreCheckState">
            <a-switch
              v-model:checked="wechat_enterprise.ignoreCheckState"
              :checked-children="$t('i18n_c0d5d68f5f')"
              :un-checked-children="$t('i18n_b7579706a3')"
            />
          </a-form-item>
          <a-form-item :label="$t('i18n_2e1f215c5d')" name="autoCreteUser">
            <a-switch
              v-model:checked="wechat_enterprise.autoCreteUser"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item v-if="wechat_enterprise.autoCreteUser" :label="$t('i18n_f49dfdace4')" name="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
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
              :placeholder="$t('i18n_72d14a3890')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('wechat_enterprise')">{{
              $t('i18n_939d5345ad')
            }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="maxkey" tab="MaxKey">
        <a-form ref="editForm" :model="maxkey" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$t('i18n_780afeac65')" name="enabled">
            <a-switch
              v-model:checked="maxkey.enabled"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item :label="$t('i18n_99593f7623')" name="clientId">
            <a-input v-model:value="maxkey.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_e0ec07be7d')" name="clientSecret">
            <a-input-password v-model:value="maxkey.clientSecret" :placeholder="$t('i18n_52c6af8174')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_bcf48bf7a8')" name="authorizationUri">
            <a-input v-model:value="maxkey.authorizationUri" type="text" :placeholder="$t('i18n_543296e005')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_79a7072ee1')" name="accessTokenUri">
            <a-input v-model:value="maxkey.accessTokenUri" type="text" :placeholder="$t('i18n_8704e7bdb7')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_2527efedcd')" name="userInfoUri">
            <a-input v-model:value="maxkey.userInfoUri" type="text" :placeholder="$t('i18n_ce84c416f9')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_51d47ddc69')" name="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-maxkey` }}</template>
            <a-input v-model:value="maxkey.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </a-form-item>

          <a-form-item :label="$t('i18n_953357d914')" name="ignoreCheckState">
            <a-switch
              v-model:checked="maxkey.ignoreCheckState"
              :checked-children="$t('i18n_c0d5d68f5f')"
              :un-checked-children="$t('i18n_b7579706a3')"
            />
          </a-form-item>

          <a-form-item :label="$t('i18n_2e1f215c5d')" name="autoCreteUser">
            <a-switch
              v-model:checked="maxkey.autoCreteUser"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item v-if="maxkey.autoCreteUser" :label="$t('i18n_f49dfdace4')" name="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
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
              :placeholder="$t('i18n_72d14a3890')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('maxkey')">{{ $t('i18n_939d5345ad') }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="gitee" tab="Gitee">
        <a-form ref="editForm" :model="gitee" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$t('i18n_780afeac65')" name="enabled">
            <a-switch
              v-model:checked="gitee.enabled"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item :label="$t('i18n_99593f7623')" name="clientId">
            <a-input v-model:value="gitee.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_e0ec07be7d')" name="clientSecret">
            <a-input-password v-model:value="gitee.clientSecret" :placeholder="$t('i18n_52c6af8174')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_51d47ddc69')" name="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-gitee` }}</template>
            <a-input v-model:value="gitee.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </a-form-item>

          <a-form-item :label="$t('i18n_953357d914')" name="ignoreCheckState">
            <a-switch
              v-model:checked="gitee.ignoreCheckState"
              :checked-children="$t('i18n_c0d5d68f5f')"
              :un-checked-children="$t('i18n_b7579706a3')"
            />
          </a-form-item>

          <a-form-item :label="$t('i18n_2e1f215c5d')" name="autoCreteUser">
            <a-switch
              v-model:checked="gitee.autoCreteUser"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item v-if="gitee.autoCreteUser" :label="$t('i18n_f49dfdace4')" name="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
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
              :placeholder="$t('i18n_72d14a3890')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('gitee')">{{ $t('i18n_939d5345ad') }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="mygitlab" :tab="$t('i18n_dc2c61a605')">
        <a-form ref="editForm" :model="mygitlab" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$t('i18n_780afeac65')" name="enabled">
            <a-switch
              v-model:checked="mygitlab.enabled"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item :label="$t('i18n_f562f75c64')" name="host">
            <template #help>{{ $t('i18n_5a42ea648d') }}</template>
            <a-input v-model:value="mygitlab.host" type="text" :placeholder="$t('i18n_0d48f8e881')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_99593f7623')" name="clientId">
            <a-input v-model:value="mygitlab.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_e0ec07be7d')" name="clientSecret">
            <a-input-password v-model:value="mygitlab.clientSecret" :placeholder="$t('i18n_52c6af8174')" />
          </a-form-item>

          <a-form-item :label="$t('i18n_51d47ddc69')" name="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-mygitlab` }}</template>
            <a-input v-model:value="mygitlab.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </a-form-item>

          <a-form-item :label="$t('i18n_953357d914')" name="ignoreCheckState">
            <a-switch
              v-model:checked="mygitlab.ignoreCheckState"
              :checked-children="$t('i18n_c0d5d68f5f')"
              :un-checked-children="$t('i18n_b7579706a3')"
            />
          </a-form-item>

          <a-form-item :label="$t('i18n_2e1f215c5d')" name="autoCreteUser">
            <a-switch
              v-model:checked="mygitlab.autoCreteUser"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item v-if="mygitlab.autoCreteUser" :label="$t('i18n_f49dfdace4')" name="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
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
              :placeholder="$t('i18n_72d14a3890')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('mygitlab')">{{ $t('i18n_939d5345ad') }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>

      <a-tab-pane key="github" tab="Github">
        <a-form ref="editForm" :model="github" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$t('i18n_780afeac65')" name="enabled">
            <a-switch
              v-model:checked="github.enabled"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item :label="$t('i18n_99593f7623')" name="clientId">
            <a-input v-model:value="github.clientId" type="text" :placeholder="$t('i18n_a0b9b4e048')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_e0ec07be7d')" name="clientSecret">
            <a-input-password v-model:value="github.clientSecret" :placeholder="$t('i18n_52c6af8174')" />
          </a-form-item>

          <a-form-item :label="$t('i18n_51d47ddc69')" name="redirectUri">
            <template #help>{{ $t('i18n_d27cf91998') }}{{ `${host}/oauth2-github` }}</template>
            <a-input v-model:value="github.redirectUri" type="text" :placeholder="$t('i18n_8363193305')" />
          </a-form-item>

          <a-form-item :label="$t('i18n_953357d914')" name="ignoreCheckState">
            <a-switch
              v-model:checked="github.ignoreCheckState"
              :checked-children="$t('i18n_c0d5d68f5f')"
              :un-checked-children="$t('i18n_b7579706a3')"
            />
          </a-form-item>

          <a-form-item :label="$t('i18n_2e1f215c5d')" name="autoCreteUser">
            <a-switch
              v-model:checked="github.autoCreteUser"
              :checked-children="$t('i18n_7854b52a88')"
              :un-checked-children="$t('i18n_5c56a88945')"
            />
          </a-form-item>
          <a-form-item v-if="github.autoCreteUser" :label="$t('i18n_f49dfdace4')" name="permissionGroup">
            <template #help>{{ $t('i18n_434d9bd852') }}</template>
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
              :placeholder="$t('i18n_72d14a3890')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('github')">{{ $t('i18n_939d5345ad') }}</a-button>
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
