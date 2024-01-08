<template>
  <div>
    <a-alert message="账号如果开启 MFA(两步验证)，使用 Oauth2 登录不会验证 MFA(两步验证)" type="info" show-icon />
    <a-tabs>
      <a-tab-pane key="gitee" tab="Gitee Oauth2">
        <a-form ref="editForm" :model="gitee" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="是否开启" name="enabled">
            <a-switch v-model:checked="gitee.enabled" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item label="客户端ID" name="clientId">
            <a-input v-model:value="gitee.clientId" type="text" placeholder="请输入客户端ID [clientId]" />
          </a-form-item>
          <a-form-item label="客户端密钥" name="clientSecret">
            <a-input-password v-model:value="gitee.clientSecret" placeholder="请输入客户端密钥 [clientSecret]" />
          </a-form-item>
          <a-form-item label="回调 url" name="redirectUri">
            <template v-slot:help>参考地址：{{ `${this.host}/oauth2-gitee` }}</template>
            <a-input v-model:value="gitee.redirectUri" type="text" placeholder="请输入回调重定向 url [redirectUri]" />
          </a-form-item>
          <!-- <a-form-item label="登录url">
              <a-input :value="`${this.host}/oauth2-render-gitee`" type="text" />
            </a-form-item> -->
          <a-form-item label="自动创建用户" name="autoCreteUser">
            <a-switch v-model:checked="gitee.autoCreteUser" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item label="忽略校验 state" name="ignoreCheckState">
            <a-switch v-model:checked="gitee.ignoreCheckState" checked-children="忽略" un-checked-children="校验" />
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('gitee')">提交</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="maxkey" tab="MaxKey Oauth2">
        <a-form ref="editForm" :model="maxkey" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="是否开启" name="enabled">
            <a-switch v-model:checked="maxkey.enabled" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item label="客户端ID" name="clientId">
            <a-input v-model:value="maxkey.clientId" type="text" placeholder="请输入客户端ID [clientId]" />
          </a-form-item>
          <a-form-item label="客户端密钥" name="clientSecret">
            <a-input-password v-model:value="maxkey.clientSecret" placeholder="请输入客户端密钥 [clientSecret]" />
          </a-form-item>
          <a-form-item label="授权 url" name="authorizationUri">
            <a-input
              v-model:value="maxkey.authorizationUri"
              type="text"
              placeholder="请输入授权 url [authorizationUri]"
            />
          </a-form-item>
          <a-form-item label="令牌 url" name="accessTokenUri">
            <a-input v-model:value="maxkey.accessTokenUri" type="text" placeholder="请输入令牌 url [accessTokenUri]" />
          </a-form-item>
          <a-form-item label="用户信息 url" name="userInfoUri">
            <a-input v-model:value="maxkey.userInfoUri" type="text" placeholder="请输入用户信息 url [userInfoUri]" />
          </a-form-item>
          <a-form-item label="回调 url" name="redirectUri">
            <template v-slot:help>参考地址：{{ `${this.host}/oauth2-maxkey` }}</template>
            <a-input v-model:value="maxkey.redirectUri" type="text" placeholder="请输入回调重定向 url [redirectUri]" />
          </a-form-item>
          <!-- <a-form-item label="登录url">
              <a-input :value="`${this.host}/oauth2-render-maxkey`" type="text" />
            </a-form-item> -->
          <a-form-item label="自动创建用户" name="autoCreteUser">
            <a-switch v-model:checked="maxkey.autoCreteUser" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item label="忽略校验 state" name="ignoreCheckState">
            <a-switch v-model:checked="maxkey.ignoreCheckState" checked-children="忽略" un-checked-children="校验" />
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('maxkey')">提交</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="github" tab="Github Oauth2">
        <a-form ref="editForm" :model="github" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="是否开启" name="enabled">
            <a-switch v-model:checked="github.enabled" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item label="客户端ID" name="clientId">
            <a-input v-model:value="github.clientId" type="text" placeholder="请输入客户端ID [clientId]" />
          </a-form-item>
          <a-form-item label="客户端密钥" name="clientSecret">
            <a-input-password v-model:value="github.clientSecret" placeholder="请输入客户端密钥 [clientSecret]" />
          </a-form-item>

          <a-form-item label="回调 url" name="redirectUri">
            <template v-slot:help>参考地址：{{ `${this.host}/oauth2-github` }}</template>
            <a-input v-model:value="github.redirectUri" type="text" placeholder="请输入回调重定向 url [redirectUri]" />
          </a-form-item>
          <!-- <a-form-item label="登录url">
              <a-input :value="`${this.host}/oauth2-render-github`" type="text" />
            </a-form-item> -->
          <a-form-item label="自动创建用户" name="autoCreteUser">
            <a-switch v-model:checked="github.autoCreteUser" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item label="忽略校验 state" name="ignoreCheckState">
            <a-switch v-model:checked="github.ignoreCheckState" checked-children="忽略" un-checked-children="校验" />
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('github')">提交</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import { oauthConfigOauth2, oauthConfigOauth2Save } from '@/api/system'

export default {
  data() {
    return {
      maxkey: {},
      gitee: {},
      github: {},
      rules: {},
      provides: ['gitee', 'maxkey', 'github'],
      host: ''
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
            this[item] = Object.assign(res.data || {}, { provide: item })
          }
        })
      })
    },
    // submit
    onSubmit(key) {
      oauthConfigOauth2Save(this[key]).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
        }
      })
    }
  }
}
</script>
