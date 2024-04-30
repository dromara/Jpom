<template>
  <div>
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote">
          {{ $tl('p.basicInfo') }}{{ temp.name }} - {{ temp.osType }} - {{ temp.operatingSystem }} -
          <a-tag>{{ temp.architecture }} </a-tag>
          <a-tag>{{ temp.id }}</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">
          {{ $tl('p.version') }}<a-tag>{{ temp.serverVersion }}</a-tag>
          <a-tag>{{ temp.kernelVersion }}</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote"
          >{{ $tl('p.resource') }} <a-tag>cpu:{{ temp.nCPU || temp.NCPU }}</a-tag>
          <a-tag>{{ $tl('p.memory') }}{{ renderSize(temp.memTotal) }}</a-tag>

          <a-tag>{{ $tl('p.containerCount') }}{{ temp.containers }}</a-tag>
          <a-tag>{{ $tl('p.imageCount') }}{{ temp.images }}</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">{{ $tl('p.systemTime') }}{{ temp.systemTime }} </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">{{ $tl('p.runningDirectory') }}{{ temp.dockerRootDir }} </span>
      </a-timeline-item>
      <template v-if="temp.swarm">
        <a-timeline-item>
          <div class="layui-elem-quote">
            {{ $tl('p.clusterInfo') }}
            <div style="padding-left: 10px">
              <a-space direction="vertical" style="width: 100%">
                <div>
                  {{ $tl('p.localStatus') }}<a-tag v-if="temp.swarm.nodeAddr">{{ temp.swarm.nodeAddr }}</a-tag>
                  <a-tag>{{ temp.swarm.localNodeState }}</a-tag>
                </div>
                <div v-if="temp.swarm.remoteManagers">
                  {{ $tl('p.managementList') }}
                  <a-tag v-for="(item, index) in temp.swarm.remoteManagers" :key="index">{{ item.addr }}</a-tag>
                </div>
                <div>{{ $tl('p.managementNode') }}{{ temp.swarm.controlAvailable ? $tl('p.yes') : $tl('p.no') }}</div>
              </a-space>
            </div>
          </div>
        </a-timeline-item>
      </template>
      <a-timeline-item v-if="temp.plugins">
        <div class="layui-elem-quote">
          {{ $tl('p.plugin') }}

          <a-list item-layout="horizontal" :data-source="Object.keys(temp.plugins)" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                {{ item }}
                <a-tag v-for="(item1, index) in temp.plugins[item]" :key="index">{{ item1 }}</a-tag>
              </a-list-item>
            </template>
          </a-list>
        </div>
      </a-timeline-item>
      <a-timeline-item v-if="temp.registryConfig">
        <div class="layui-elem-quote">
          {{ $tl('p.repository') }}
          <a-list item-layout="horizontal" :data-source="Object.keys(temp.registryConfig.indexConfigs)" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                {{ item }}
                <a-tag v-if="temp.registryConfig.indexConfigs[item].official" color="green">{{
                  $tl('p.official')
                }}</a-tag
                ><a-tag v-if="temp.registryConfig.indexConfigs[item].secure" color="green">{{
                  $tl('p.security')
                }}</a-tag>
                <a-tag v-for="(item1, index) in temp.registryConfig.indexConfigs[item].mirrors" :key="index">{{
                  item1
                }}</a-tag>
              </a-list-item>
            </template>
          </a-list>
        </div>
      </a-timeline-item>
    </a-timeline>
  </div>
</template>

<script>
import { dockerInfo } from '@/api/docker-api'
import { renderSize } from '@/utils/const'
export default {
  props: {
    id: {
      type: String,
      default: ''
    },
    urlPrefix: {
      type: String,
      default: ''
    },
    machineDockerId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      temp: {},

      rules: {}
    }
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId
    }
  },
  mounted() {
    this.loadData()
    // console.log(Comparator);
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.docker.info.${key}`, ...args)
    },
    renderSize,
    // load data
    loadData() {
      dockerInfo(this.urlPrefix, {
        id: this.reqDataId
      }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data
        }
      })
    }
  }
}
</script>
