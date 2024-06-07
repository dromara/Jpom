<template>
  <div>
    <a-alert type="info" show-icon :message="$t('pages.system.oauth-config.9a6a56f')">
      <template #description>
        <ul>
          <li>{{ $t('pages.system.oauth-config.ac3bb9b5') }}</li>
          <li>{{ $t('pages.system.oauth-config.689b1a0b') }}</li>
          <li>{{ $t('pages.system.oauth-config.83bcd442') }}</li>
        </ul>
      </template>
    </a-alert>
    <a-tabs>
      <a-tab-pane key="dingtalk" :tab="$t('pages.system.oauth-config.54673869')">
        <a-form ref="editForm" :model="dingtalk" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$t('pages.system.oauth-config.23128e5c')" name="enabled">
            <a-switch
              v-model:checked="dingtalk.enabled"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.1c57b8e3')" name="clientId">
            <a-input
              v-model:value="dingtalk.clientId"
              type="text"
              :placeholder="$t('pages.system.oauth-config.21dfa3c1')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.6111cfe7')" name="clientSecret">
            <a-input-password
              v-model:value="dingtalk.clientSecret"
              :placeholder="$t('pages.system.oauth-config.cc316bed')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.389d082b')" name="redirectUri">
            <template #help>{{ $t('pages.system.oauth-config.af754ac8') }}{{ `${host}/oauth2-dingtalk` }}</template>
            <a-input
              v-model:value="dingtalk.redirectUri"
              type="text"
              :placeholder="$t('pages.system.oauth-config.d8d52dc2')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.27bd380c')" name="ignoreCheckState">
            <a-switch
              v-model:checked="dingtalk.ignoreCheckState"
              :checked-children="$t('pages.system.oauth-config.188e4db1')"
              :un-checked-children="$t('pages.system.oauth-config.b423b110')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.187b8289')" name="autoCreteUser">
            <a-switch
              v-model:checked="dingtalk.autoCreteUser"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item
            v-if="dingtalk.autoCreteUser"
            :label="$t('pages.system.oauth-config.a914f37c')"
            name="permissionGroup"
          >
            <template #help>{{ $t('pages.system.oauth-config.514e5e34') }}</template>
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
              :placeholder="$t('pages.system.oauth-config.4bba534d')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('dingtalk')">{{
              $t('pages.system.oauth-config.2a372810')
            }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="feishu" :tab="$t('pages.system.oauth-config.c30c03b4')">
        <a-form ref="editForm" :model="feishu" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$t('pages.system.oauth-config.23128e5c')" name="enabled">
            <a-switch
              v-model:checked="feishu.enabled"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.1c57b8e3')" name="clientId">
            <a-input
              v-model:value="feishu.clientId"
              type="text"
              :placeholder="$t('pages.system.oauth-config.21dfa3c1')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.6111cfe7')" name="clientSecret">
            <a-input-password
              v-model:value="feishu.clientSecret"
              :placeholder="$t('pages.system.oauth-config.cc316bed')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.389d082b')" name="redirectUri">
            <template #help>{{ $t('pages.system.oauth-config.af754ac8') }}{{ `${host}/oauth2-feishu` }}</template>
            <a-input
              v-model:value="feishu.redirectUri"
              type="text"
              :placeholder="$t('pages.system.oauth-config.d8d52dc2')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.27bd380c')" name="ignoreCheckState">
            <a-switch
              v-model:checked="feishu.ignoreCheckState"
              :checked-children="$t('pages.system.oauth-config.188e4db1')"
              :un-checked-children="$t('pages.system.oauth-config.b423b110')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.187b8289')" name="autoCreteUser">
            <a-switch
              v-model:checked="feishu.autoCreteUser"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item
            v-if="feishu.autoCreteUser"
            :label="$t('pages.system.oauth-config.a914f37c')"
            name="permissionGroup"
          >
            <template #help>{{ $t('pages.system.oauth-config.514e5e34') }}</template>
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
              :placeholder="$t('pages.system.oauth-config.4bba534d')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('feishu')">{{
              $t('pages.system.oauth-config.2a372810')
            }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="wechat_enterprise" :tab="$t('pages.system.oauth-config.237475b4')">
        <a-form
          ref="editForm"
          :model="wechat_enterprise"
          :rules="rules"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item :label="$t('pages.system.oauth-config.23128e5c')" name="enabled">
            <a-switch
              v-model:checked="wechat_enterprise.enabled"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.54ee097d')" name="agentId">
            <a-input
              v-model:value="wechat_enterprise.agentId"
              type="text"
              :placeholder="$t('pages.system.oauth-config.180813dd')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.1c57b8e3')" name="clientId">
            <a-input
              v-model:value="wechat_enterprise.clientId"
              type="text"
              :placeholder="$t('pages.system.oauth-config.21dfa3c1')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.6111cfe7')" name="clientSecret">
            <a-input-password
              v-model:value="wechat_enterprise.clientSecret"
              :placeholder="$t('pages.system.oauth-config.cc316bed')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.389d082b')" name="redirectUri">
            <template #help
              >{{ $t('pages.system.oauth-config.af754ac8') }}{{ `${host}/oauth2-wechat_enterprise` }}</template
            >
            <a-input
              v-model:value="wechat_enterprise.redirectUri"
              type="text"
              :placeholder="$t('pages.system.oauth-config.d8d52dc2')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.27bd380c')" name="ignoreCheckState">
            <a-switch
              v-model:checked="wechat_enterprise.ignoreCheckState"
              :checked-children="$t('pages.system.oauth-config.188e4db1')"
              :un-checked-children="$t('pages.system.oauth-config.b423b110')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.187b8289')" name="autoCreteUser">
            <a-switch
              v-model:checked="wechat_enterprise.autoCreteUser"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item
            v-if="wechat_enterprise.autoCreteUser"
            :label="$t('pages.system.oauth-config.a914f37c')"
            name="permissionGroup"
          >
            <template #help>{{ $t('pages.system.oauth-config.514e5e34') }}</template>
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
              :placeholder="$t('pages.system.oauth-config.4bba534d')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('wechat_enterprise')">{{
              $t('pages.system.oauth-config.2a372810')
            }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="maxkey" tab="MaxKey">
        <a-form ref="editForm" :model="maxkey" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$t('pages.system.oauth-config.23128e5c')" name="enabled">
            <a-switch
              v-model:checked="maxkey.enabled"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.1c57b8e3')" name="clientId">
            <a-input
              v-model:value="maxkey.clientId"
              type="text"
              :placeholder="$t('pages.system.oauth-config.21dfa3c1')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.6111cfe7')" name="clientSecret">
            <a-input-password
              v-model:value="maxkey.clientSecret"
              :placeholder="$t('pages.system.oauth-config.cc316bed')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.a8be12f0')" name="authorizationUri">
            <a-input
              v-model:value="maxkey.authorizationUri"
              type="text"
              :placeholder="$t('pages.system.oauth-config.521ea6d2')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.d1764a71')" name="accessTokenUri">
            <a-input
              v-model:value="maxkey.accessTokenUri"
              type="text"
              :placeholder="$t('pages.system.oauth-config.ecc87338')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.8b2a41a8')" name="userInfoUri">
            <a-input
              v-model:value="maxkey.userInfoUri"
              type="text"
              :placeholder="$t('pages.system.oauth-config.c776e5d9')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.389d082b')" name="redirectUri">
            <template #help>{{ $t('pages.system.oauth-config.af754ac8') }}{{ `${host}/oauth2-maxkey` }}</template>
            <a-input
              v-model:value="maxkey.redirectUri"
              type="text"
              :placeholder="$t('pages.system.oauth-config.d8d52dc2')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.27bd380c')" name="ignoreCheckState">
            <a-switch
              v-model:checked="maxkey.ignoreCheckState"
              :checked-children="$t('pages.system.oauth-config.188e4db1')"
              :un-checked-children="$t('pages.system.oauth-config.b423b110')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.187b8289')" name="autoCreteUser">
            <a-switch
              v-model:checked="maxkey.autoCreteUser"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item
            v-if="maxkey.autoCreteUser"
            :label="$t('pages.system.oauth-config.a914f37c')"
            name="permissionGroup"
          >
            <template #help>{{ $t('pages.system.oauth-config.514e5e34') }}</template>
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
              :placeholder="$t('pages.system.oauth-config.4bba534d')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('maxkey')">{{
              $t('pages.system.oauth-config.2a372810')
            }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="gitee" tab="Gitee">
        <a-form ref="editForm" :model="gitee" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$t('pages.system.oauth-config.23128e5c')" name="enabled">
            <a-switch
              v-model:checked="gitee.enabled"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.1c57b8e3')" name="clientId">
            <a-input
              v-model:value="gitee.clientId"
              type="text"
              :placeholder="$t('pages.system.oauth-config.21dfa3c1')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.6111cfe7')" name="clientSecret">
            <a-input-password
              v-model:value="gitee.clientSecret"
              :placeholder="$t('pages.system.oauth-config.cc316bed')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.389d082b')" name="redirectUri">
            <template #help>{{ $t('pages.system.oauth-config.af754ac8') }}{{ `${host}/oauth2-gitee` }}</template>
            <a-input
              v-model:value="gitee.redirectUri"
              type="text"
              :placeholder="$t('pages.system.oauth-config.d8d52dc2')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.27bd380c')" name="ignoreCheckState">
            <a-switch
              v-model:checked="gitee.ignoreCheckState"
              :checked-children="$t('pages.system.oauth-config.188e4db1')"
              :un-checked-children="$t('pages.system.oauth-config.b423b110')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.187b8289')" name="autoCreteUser">
            <a-switch
              v-model:checked="gitee.autoCreteUser"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item
            v-if="gitee.autoCreteUser"
            :label="$t('pages.system.oauth-config.a914f37c')"
            name="permissionGroup"
          >
            <template #help>{{ $t('pages.system.oauth-config.514e5e34') }}</template>
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
              :placeholder="$t('pages.system.oauth-config.4bba534d')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('gitee')">{{
              $t('pages.system.oauth-config.2a372810')
            }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="mygitlab" :tab="$t('pages.system.oauth-config.10e8cf03')">
        <a-form ref="editForm" :model="mygitlab" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$t('pages.system.oauth-config.23128e5c')" name="enabled">
            <a-switch
              v-model:checked="mygitlab.enabled"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.82d713c8')" name="host">
            <template #help>{{ $t('pages.system.oauth-config.2cce1ccc') }}</template>
            <a-input v-model:value="mygitlab.host" type="text" :placeholder="$t('pages.system.oauth-config.3539fc4')" />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.1c57b8e3')" name="clientId">
            <a-input
              v-model:value="mygitlab.clientId"
              type="text"
              :placeholder="$t('pages.system.oauth-config.21dfa3c1')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.6111cfe7')" name="clientSecret">
            <a-input-password
              v-model:value="mygitlab.clientSecret"
              :placeholder="$t('pages.system.oauth-config.cc316bed')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.389d082b')" name="redirectUri">
            <template #help>{{ $t('pages.system.oauth-config.af754ac8') }}{{ `${host}/oauth2-mygitlab` }}</template>
            <a-input
              v-model:value="mygitlab.redirectUri"
              type="text"
              :placeholder="$t('pages.system.oauth-config.d8d52dc2')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.27bd380c')" name="ignoreCheckState">
            <a-switch
              v-model:checked="mygitlab.ignoreCheckState"
              :checked-children="$t('pages.system.oauth-config.188e4db1')"
              :un-checked-children="$t('pages.system.oauth-config.b423b110')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.187b8289')" name="autoCreteUser">
            <a-switch
              v-model:checked="mygitlab.autoCreteUser"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item
            v-if="mygitlab.autoCreteUser"
            :label="$t('pages.system.oauth-config.a914f37c')"
            name="permissionGroup"
          >
            <template #help>{{ $t('pages.system.oauth-config.514e5e34') }}</template>
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
              :placeholder="$t('pages.system.oauth-config.4bba534d')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('mygitlab')">{{
              $t('pages.system.oauth-config.2a372810')
            }}</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>

      <a-tab-pane key="github" tab="Github">
        <a-form ref="editForm" :model="github" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item :label="$t('pages.system.oauth-config.23128e5c')" name="enabled">
            <a-switch
              v-model:checked="github.enabled"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.1c57b8e3')" name="clientId">
            <a-input
              v-model:value="github.clientId"
              type="text"
              :placeholder="$t('pages.system.oauth-config.21dfa3c1')"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.system.oauth-config.6111cfe7')" name="clientSecret">
            <a-input-password
              v-model:value="github.clientSecret"
              :placeholder="$t('pages.system.oauth-config.cc316bed')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.389d082b')" name="redirectUri">
            <template #help>{{ $t('pages.system.oauth-config.af754ac8') }}{{ `${host}/oauth2-github` }}</template>
            <a-input
              v-model:value="github.redirectUri"
              type="text"
              :placeholder="$t('pages.system.oauth-config.d8d52dc2')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.27bd380c')" name="ignoreCheckState">
            <a-switch
              v-model:checked="github.ignoreCheckState"
              :checked-children="$t('pages.system.oauth-config.188e4db1')"
              :un-checked-children="$t('pages.system.oauth-config.b423b110')"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.oauth-config.187b8289')" name="autoCreteUser">
            <a-switch
              v-model:checked="github.autoCreteUser"
              :checked-children="$t('pages.system.oauth-config.e6a65361')"
              :un-checked-children="$t('pages.system.oauth-config.bd324b84')"
            />
          </a-form-item>
          <a-form-item
            v-if="github.autoCreteUser"
            :label="$t('pages.system.oauth-config.a914f37c')"
            name="permissionGroup"
          >
            <template #help>{{ $t('pages.system.oauth-config.514e5e34') }}</template>
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
              :placeholder="$t('pages.system.oauth-config.4bba534d')"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('github')">{{
              $t('pages.system.oauth-config.2a372810')
            }}</a-button>
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
