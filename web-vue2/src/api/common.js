import axios from "./config";

/**
 * 生成分片上传 id
 */
export function generateShardingId() {
  return axios({
    url: "/generate-sharding-id",
    method: "get",
    data: {},
  });
}
