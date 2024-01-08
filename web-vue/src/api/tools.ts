import axios from "./config";

/**
 *
 * @param data
 */
export function cronTools(data) {
  return axios({
    url: "/tools/cron",
    method: "get",
    params: data,
  });
}
