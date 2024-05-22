export default {
  c: {
    isEnabled: 'Enable',
    enable: 'Enable',
    disable: 'Disable',
    clientId: 'Client ID',
    clientIdInput: 'Please enter the client ID [clientId]',
    clientSecret: 'Client Secret',
    clientSecretInput: 'Please enter the client secret [clientSecret]',
    redirectUrl: 'Redirect URL',
    referenceUrl: 'Reference URL:',
    redirectUriInput: 'Please enter the redirect URI [redirectUri]',
    ignoreStateCheck: 'Ignore state check',
    ignore: 'Ignore',
    verify: 'Verify',
    autoCreateUser: 'Automatically create user',
    permissionGroup: 'Permission Group',
    autoAssignPermissionGroup: 'Automatically associate the corresponding permission group after creating a user',
    selectPermissionGroup: "Please select the user's permission group",
    submit: 'Submit'
  },
  p: {
    oauth2Tip: 'Oauth2 usage tips',
    mfaNotice:
      'If MFA (two-step verification) is enabled for the account, using Oauth2 login will not verify MFA (two-step verification)',
    externalAccountBindNotice:
      'Binding an external system account is not supported when the existing account is inconsistent with the external system account',
    autoCreateUserNotice:
      'After enabling automatic user creation, only the account will be automatically created for the first login, and the administrator still needs to manually assign permission groups',
    dingtalkScan: 'DingTalk QR code scan',
    feishuScan: 'Feishu QR code scan',
    wechatWorkScan: 'WeChat Work QR code scan',
    webAppId: 'Web application ID',
    webAppIdInput: 'Please enter the web application ID of the authorizing party',
    authorizationUrl: 'Authorization URL',
    authorizationUriInput: 'Please enter the authorization URL [authorizationUri]',
    tokenUrl: 'Token URL',
    accessTokenUriInput: 'Please enter the token URL [accessTokenUri]',
    userInfoUrl: 'User Info URL',
    userInfoUriInput: 'Please enter the user info URL [userInfoUri]',
    selfHostedGitlab: 'Self-Hosted Gitlab',
    serviceAddress: 'Service Address',
    selfHostedGitlabUrl: 'Self-Hosted Gitlab URL',
    serviceAddressInput: 'Please enter the service address'
  }
}
