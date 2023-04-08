import axios from "../config";

export function userLoginLgin(params:any) {
  return axios({
    url: "/user/login-log/list-data",
    method: "post",
    data: params,
  });
}

export const operateCodeMap = {
  0: "正常登录",
  1: "密码错误",
  2: "账号被锁定",
  3: "自动续期",
  4: "账号被禁用",
  5: "登录成功,需要验证 MFA",
  6: "oauth2",
};
