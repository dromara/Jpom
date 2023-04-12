import axios from "axios";

const external = axios.create({
  timeout: 5 * 1000,
  headers: {},
});

// 响应拦截器
external.interceptors.response.use(
  async (response) => {
    return response.data;
  },
  (error) => {
    console.error(error);
    return Promise.reject(error);
  }
);

export function executionRequest(url:string, param:any) {
  return external({
    url: url,
    method: "get",
    params: param,
  });
}
