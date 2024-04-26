<template>
  <div>
    <a-alert type="info" show-icon message="Oauth2 使用提示">
      <template #description>
        <ul>
          <li>账号如果开启 MFA(两步验证)，使用 Oauth2 登录不会验证 MFA(两步验证)</li>
          <li>已经存在的账号与外部系统账号不一致时不支持绑定外部系统账号</li>
          <li>开启自动创建用户后第一次登录仅自动创建账号，还需要管理员手动分配权限组</li>
        </ul>
      </template>
    </a-alert>
    <a-tabs>
      <a-tab-pane key="dingtalk" tab="钉钉扫码">
        <a-form ref="editForm" :model="dingtalk" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="是否开启" name="enabled">
            <a-switch v-model:checked="dingtalk.enabled" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item label="客户端ID" name="clientId">
            <a-input v-model:value="dingtalk.clientId" type="text" placeholder="请输入客户端ID [clientId]" />
          </a-form-item>
          <a-form-item label="客户端密钥" name="clientSecret">
            <a-input-password v-model:value="dingtalk.clientSecret" placeholder="请输入客户端密钥 [clientSecret]" />
          </a-form-item>

          <a-form-item label="回调 url" name="redirectUri">
            <template #help>参考地址：{{ `${host}/oauth2-dingtalk` }}</template>
            <a-input
              v-model:value="dingtalk.redirectUri"
              type="text"
              placeholder="请输入回调重定向 url [redirectUri]"
            />
          </a-form-item>

          <a-form-item label="忽略校验 state" name="ignoreCheckState">
            <a-switch v-model:checked="dingtalk.ignoreCheckState" checked-children="忽略" un-checked-children="校验" />
          </a-form-item>
          <a-form-item label="自动创建用户" name="autoCreteUser">
            <a-switch v-model:checked="dingtalk.autoCreteUser" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item v-if="dingtalk.autoCreteUser" label="权限组" name="permissionGroup">
            <template #help>创建用户后自动关联上对应的权限组</template>
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
              placeholder="请选择用户的权限组"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('dingtalk')">提交</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="feishu" tab="飞书扫码">
        <a-form ref="editForm" :model="feishu" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="是否开启" name="enabled">
            <a-switch v-model:checked="feishu.enabled" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item label="客户端ID" name="clientId">
            <a-input v-model:value="feishu.clientId" type="text" placeholder="请输入客户端ID [clientId]" />
          </a-form-item>
          <a-form-item label="客户端密钥" name="clientSecret">
            <a-input-password v-model:value="feishu.clientSecret" placeholder="请输入客户端密钥 [clientSecret]" />
          </a-form-item>

          <a-form-item label="回调 url" name="redirectUri">
            <template #help>参考地址：{{ `${host}/oauth2-feishu` }}</template>
            <a-input v-model:value="feishu.redirectUri" type="text" placeholder="请输入回调重定向 url [redirectUri]" />
          </a-form-item>

          <a-form-item label="忽略校验 state" name="ignoreCheckState">
            <a-switch v-model:checked="feishu.ignoreCheckState" checked-children="忽略" un-checked-children="校验" />
          </a-form-item>
          <a-form-item label="自动创建用户" name="autoCreteUser">
            <a-switch v-model:checked="feishu.autoCreteUser" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item v-if="feishu.autoCreteUser" label="权限组" name="permissionGroup">
            <template #help>创建用户后自动关联上对应的权限组</template>
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
              placeholder="请选择用户的权限组"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('feishu')">提交</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="wechat_enterprise" tab="企业微信扫码">
        <a-form
          ref="editForm"
          :model="wechat_enterprise"
          :rules="rules"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="是否开启" name="enabled">
            <a-switch v-model:checked="wechat_enterprise.enabled" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item label="网页应用ID" name="agentId">
            <a-input v-model:value="wechat_enterprise.agentId" type="text" placeholder="请输入授权方的网页应用ID" />
          </a-form-item>
          <a-form-item label="客户端ID" name="clientId">
            <a-input v-model:value="wechat_enterprise.clientId" type="text" placeholder="请输入客户端ID [clientId]" />
          </a-form-item>
          <a-form-item label="客户端密钥" name="clientSecret">
            <a-input-password
              v-model:value="wechat_enterprise.clientSecret"
              placeholder="请输入客户端密钥 [clientSecret]"
            />
          </a-form-item>

          <a-form-item label="回调 url" name="redirectUri">
            <template #help>参考地址：{{ `${host}/oauth2-wechat_enterprise` }}</template>
            <a-input
              v-model:value="wechat_enterprise.redirectUri"
              type="text"
              placeholder="请输入回调重定向 url [redirectUri]"
            />
          </a-form-item>

          <a-form-item label="忽略校验 state" name="ignoreCheckState">
            <a-switch
              v-model:checked="wechat_enterprise.ignoreCheckState"
              checked-children="忽略"
              un-checked-children="校验"
            />
          </a-form-item>
          <a-form-item label="自动创建用户" name="autoCreteUser">
            <a-switch
              v-model:checked="wechat_enterprise.autoCreteUser"
              checked-children="启用"
              un-checked-children="停用"
            />
          </a-form-item>
          <a-form-item v-if="wechat_enterprise.autoCreteUser" label="权限组" name="permissionGroup">
            <template #help>创建用户后自动关联上对应的权限组</template>
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
              placeholder="请选择用户的权限组"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('wechat_enterprise')">提交</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="maxkey" tab="MaxKey">
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
            <template #help>参考地址：{{ `${host}/oauth2-maxkey` }}</template>
            <a-input v-model:value="maxkey.redirectUri" type="text" placeholder="请输入回调重定向 url [redirectUri]" />
          </a-form-item>

          <a-form-item label="忽略校验 state" name="ignoreCheckState">
            <a-switch v-model:checked="maxkey.ignoreCheckState" checked-children="忽略" un-checked-children="校验" />
          </a-form-item>

          <a-form-item label="自动创建用户" name="autoCreteUser">
            <a-switch v-model:checked="maxkey.autoCreteUser" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item v-if="maxkey.autoCreteUser" label="权限组" name="permissionGroup">
            <template #help>创建用户后自动关联上对应的权限组</template>
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
              placeholder="请选择用户的权限组"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('maxkey')">提交</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="gitee" tab="Gitee">
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
            <template #help>参考地址：{{ `${host}/oauth2-gitee` }}</template>
            <a-input v-model:value="gitee.redirectUri" type="text" placeholder="请输入回调重定向 url [redirectUri]" />
          </a-form-item>

          <a-form-item label="忽略校验 state" name="ignoreCheckState">
            <a-switch v-model:checked="gitee.ignoreCheckState" checked-children="忽略" un-checked-children="校验" />
          </a-form-item>

          <a-form-item label="自动创建用户" name="autoCreteUser">
            <a-switch v-model:checked="gitee.autoCreteUser" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item v-if="gitee.autoCreteUser" label="权限组" name="permissionGroup">
            <template #help>创建用户后自动关联上对应的权限组</template>
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
              placeholder="请选择用户的权限组"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('gitee')">提交</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <a-tab-pane key="mygitlab" tab="自建 Gitlab">
        <a-form ref="editForm" :model="mygitlab" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="是否开启" name="enabled">
            <a-switch v-model:checked="mygitlab.enabled" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item label="服务地址" name="host">
            <template #help>自建 gitlab 访问地址</template>
            <a-input v-model:value="mygitlab.host" type="text" placeholder="请输入服务地址" />
          </a-form-item>
          <a-form-item label="客户端ID" name="clientId">
            <a-input v-model:value="mygitlab.clientId" type="text" placeholder="请输入客户端ID [clientId]" />
          </a-form-item>
          <a-form-item label="客户端密钥" name="clientSecret">
            <a-input-password v-model:value="mygitlab.clientSecret" placeholder="请输入客户端密钥 [clientSecret]" />
          </a-form-item>

          <a-form-item label="回调 url" name="redirectUri">
            <template #help>参考地址：{{ `${host}/oauth2-mygitlab` }}</template>
            <a-input
              v-model:value="mygitlab.redirectUri"
              type="text"
              placeholder="请输入回调重定向 url [redirectUri]"
            />
          </a-form-item>

          <a-form-item label="忽略校验 state" name="ignoreCheckState">
            <a-switch v-model:checked="mygitlab.ignoreCheckState" checked-children="忽略" un-checked-children="校验" />
          </a-form-item>

          <a-form-item label="自动创建用户" name="autoCreteUser">
            <a-switch v-model:checked="mygitlab.autoCreteUser" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item v-if="mygitlab.autoCreteUser" label="权限组" name="permissionGroup">
            <template #help>创建用户后自动关联上对应的权限组</template>
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
              placeholder="请选择用户的权限组"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" @click="onSubmit('mygitlab')">提交</a-button>
          </a-form-item>
        </a-form>
      </a-tab-pane>

      <a-tab-pane key="github" tab="Github">
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
            <template #help>参考地址：{{ `${host}/oauth2-github` }}</template>
            <a-input v-model:value="github.redirectUri" type="text" placeholder="请输入回调重定向 url [redirectUri]" />
          </a-form-item>

          <a-form-item label="忽略校验 state" name="ignoreCheckState">
            <a-switch v-model:checked="github.ignoreCheckState" checked-children="忽略" un-checked-children="校验" />
          </a-form-item>

          <a-form-item label="自动创建用户" name="autoCreteUser">
            <a-switch v-model:checked="github.autoCreteUser" checked-children="启用" un-checked-children="停用" />
          </a-form-item>
          <a-form-item v-if="github.autoCreteUser" label="权限组" name="permissionGroup">
            <template #help>创建用户后自动关联上对应的权限组</template>
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
              placeholder="请选择用户的权限组"
              mode="multiple"
            >
              <a-select-option v-for="item in permissionGroup" :key="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
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
