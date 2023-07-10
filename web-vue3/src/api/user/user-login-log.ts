import axios from "../config";

export function userLoginLgin(params: any) {
  return axios({
    url: "/user/login-log/list-data",
    method: "post",
    data: params,
  });
}

export const operateCodeMap = ["正常登录", "密码错误", "账号被锁定", "自动续期", "账号被禁用", "登录成功,需要验证 MFA", "oauth2"];
