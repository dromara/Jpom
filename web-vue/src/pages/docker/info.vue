<template>
  <div class="full-content">
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote">
          基础信息：{{ temp.name }} - {{ temp.osType }} - {{ temp.operatingSystem }} - <a-tag>{{ temp.architecture }}</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">版本：{{ temp.serverVersion }} </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote"
          >资源： <a-tag>cpu:{{ temp.nCPU }}</a-tag> <a-tag>内存:{{ renderSize(temp.memTotal) }}</a-tag>

          <a-tag>容器数：{{ temp.containers }}</a-tag>
          <a-tag>镜像数：{{ temp.images }}</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">系统时间：{{ temp.systemTime }} </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">运行目录：{{ temp.dockerRootDir }} </span>
      </a-timeline-item>
      <template v-if="temp.swarm">
        <a-timeline-item>
          <div class="layui-elem-quote">
            集群信息：
            <div style="padding-left: 10px">
              <a-space direction="vertical">
                <div>
                  本地状态：<a-tag v-if="temp.swarm.nodeAddr">{{ temp.swarm.nodeAddr }}</a-tag> <a-tag>{{ temp.swarm.localNodeState }}</a-tag>
                </div>
                <div v-if="temp.swarm.remoteManagers">
                  管理列表： <a-tag v-for="(item, index) in temp.swarm.remoteManagers" :key="index">{{ item.addr }}</a-tag>
                </div>
              </a-space>
            </div>
          </div>
        </a-timeline-item>
      </template>
      <a-timeline-item v-if="temp.plugins">
        <div class="layui-elem-quote">
          插件：

          <a-list item-layout="horizontal" :data-source="Object.keys(temp.plugins)" size="small">
            <a-list-item slot="renderItem" slot-scope="item">
              {{ item }} <a-tag v-for="(item1, index) in temp.plugins[item]" :key="index">{{ item1 }}</a-tag>
            </a-list-item>
          </a-list>
        </div>
      </a-timeline-item>
      <a-timeline-item v-if="temp.registryConfig">
        <div class="layui-elem-quote">
          仓库：
          <a-list item-layout="horizontal" :data-source="Object.keys(temp.registryConfig.indexConfigs)" size="small">
            <a-list-item slot="renderItem" slot-scope="item">
              {{ item }} <a-tag color="green" v-if="temp.registryConfig.indexConfigs[item].official">官方</a-tag><a-tag color="green" v-if="temp.registryConfig.indexConfigs[item].secure">安全</a-tag>
              <a-tag v-for="(item1, index) in temp.registryConfig.indexConfigs[item].mirrors" :key="index">{{ item1 }}</a-tag>
            </a-list-item>
          </a-list>
        </div>
      </a-timeline-item>
    </a-timeline>
  </div>
</template>
<script>
import { dockerInfo } from "@/api/docker-api";
import { renderSize } from "@/utils/time";
export default {
  props: {
    id: {
      type: String,
    },
  },
  data() {
    return {
      temp: {},
    };
  },
  mounted() {
    this.loadData();
    // console.log(Comparator);
  },
  methods: {
    renderSize,
    // load data
    loadData() {
      dockerInfo({
        id: this.id,
      }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data;
        }
      });
    },
  },
};
</script>
