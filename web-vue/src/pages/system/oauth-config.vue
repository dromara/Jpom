<template>
  <div class="full-content">
    <a-tabs>
      <a-tab-pane key="1" tab="自定义 Oauth2">
        <a-form-model ref="editForm" :model="temp" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
          <a-form-model-item label="是否开启" prop="enabled">
            <a-switch v-model="temp.enabled" checked-children="启用" un-checked-children="停用" />
          </a-form-model-item>
          <a-form-model-item label="客户端ID" prop="clientId">
            <a-input v-model="temp.clientId" type="text" placeholder="请输入客户端ID [clientId]" />
          </a-form-model-item>
          <a-form-model-item label="客户端密钥" prop="clientSecret">
            <a-input-password v-model="temp.clientSecret" placeholder="请输入客户端密钥 [clientSecret]" />
          </a-form-model-item>
          <a-form-model-item label="授权 url" prop="authorizationUri">
            <a-input v-model="temp.authorizationUri" type="text" placeholder="请输入授权 url [authorizationUri]" />
          </a-form-model-item>
          <a-form-model-item label="令牌 url" prop="accessTokenUri">
            <a-input v-model="temp.accessTokenUri" type="text" placeholder="请输入令牌 url [accessTokenUri]" />
          </a-form-model-item>
          <a-form-model-item label="用户信息 url" prop="userInfoUri">
            <a-input v-model="temp.userInfoUri" type="text" placeholder="请输入用户信息 url [userInfoUri]" />
          </a-form-model-item>
          <a-form-model-item label="重定向 url" prop="redirectUri">
            <a-input v-model="temp.redirectUri" type="text" placeholder="请输入重定向 url [redirectUri]" />
          </a-form-model-item>
          <a-form-model-item label="自动创建用户" prop="autoCreteUser">
            <a-switch v-model="temp.autoCreteUser" checked-children="启用" un-checked-children="停用" />
          </a-form-model-item>

          <a-form-model-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" class="btn" :disabled="submitAble" @click="onSubmit">提交</a-button>
          </a-form-model-item>
        </a-form-model>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>
<script>
import { oauthConfigOauth2, oauthConfigOauth2Save } from "@/api/system";

export default {
  data() {
    return {
      temp: {},
      submitAble: false,
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // load data
    loadData() {
      oauthConfigOauth2().then((res) => {
        if (res.code === 200) {
          this.temp = res.data || {};
        }
      });
    },
    // submit
    onSubmit() {
      oauthConfigOauth2Save(this.temp).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
        }
      });
    },
  },
};
</script>
<style scoped></style>
