export default {
  common: {
    username: "用户名",
    password: "密码",
    captcha: "验证码",
  },

  login: {
    title: "登录 JPOM",
    name: "请输入用户名",
    pwd: "请输入密码",
    code: "请输入验证码",
    mfaCodeEmpty: "请输入两步验证码",
    mfaCodeError: "验证码为 6 位纯数字",
    dynamicBgOn: "开启动态背景",
    dynamicBgOff: "关闭动态背景",

    thirdParty: "第三方登录",
    mfaCaptcha: "MFA 验证码",
  },

  install: {
    title: "创建账号",
    init: "初始化系统账户",
    initDesc: "您需要创建一个账户用以后续登录管理系统,请牢记超级管理员账号密码",
    cannotInit: "不能初始化",
    subCannotInit: "当前系统已经初始化过啦,不能重复初始化",
    ignorePrompt: "确定要忽略绑定两步验证吗？强烈建议超级管理员开启两步验证来保证账号安全性",

    name: "请输入账户名",
    namePlaceholder: "账户名称",
    pwd: "请输入密码",
    pwdMessage: "密码必须包含数字，字母，字符，且大于6位",
    pwdPlaceholder: "密码（6-18位数字、字母、符号组合）",
    guide: {
      title: "导航助手",
      step1: "不要慌，出现这个页面表示您没有设置系统管理员信息，或者需要重置管理员信息",
      step2: "此处需要填写的信息是用以管理系统的系统管理员的账户密码，一定要记住哦，它是登录的唯一凭证",
      step3: "为了您的账户安全，设定的密码需要包含字母、数字、字符，且长度于 6-18 位之间",
    },

    steps: {
      _1: {
        title: "初始化系统",
        desc: "设置一个超级管理员账号",
      },
      _2: {
        title: "启用两步验证",
        desc: "开启两步验证使账号更安全",
      },
    },

    alert: {
      _1: "为了考虑系统安全我们强烈建议超级管理员开启两步验证来确保账号的安全性",
      _2: "绑定成功后将不再显示,强烈建议保存此二维码或者下面的 MFA key",
      _3: "请使用应用扫码绑定令牌,然后输入验证码确认绑定才生效"
    },

    authentication: {
      twoFactorAuthenticationApp:"两步验证应用",
      inputCaptcha: "请输入两步验证码",
      informCaptcha: "验证码为6位纯数字",
      twoFactorCaptcha: "两步验证码"
    },

  },
};
