<template>
  <div>
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote">
          {{ $t('pages.docker.info.ccf301b7') }}{{ temp.name }} - {{ temp.osType }} - {{ temp.operatingSystem }} -
          <a-tag>{{ temp.architecture }} </a-tag>
          <a-tag>{{ temp.id }}</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">
          {{ $t('pages.docker.info.d826aba2') }}<a-tag>{{ temp.serverVersion }}</a-tag>
          <a-tag>{{ temp.kernelVersion }}</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote"
          >{{ $t('pages.docker.info.86439fa0') }} <a-tag>cpu:{{ temp.nCPU || temp.NCPU }}</a-tag>
          <a-tag>{{ $t('pages.docker.info.d5f99ae') }}{{ renderSize(temp.memTotal) }}</a-tag>

          <a-tag>{{ $t('pages.docker.info.16165c92') }}{{ temp.containers }}</a-tag>
          <a-tag>{{ $t('pages.docker.info.83f87adf') }}{{ temp.images }}</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">{{ $t('pages.docker.info.278a0f66') }}{{ temp.systemTime }} </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">{{ $t('pages.docker.info.b958490a') }}{{ temp.dockerRootDir }} </span>
      </a-timeline-item>
      <template v-if="temp.swarm">
        <a-timeline-item>
          <div class="layui-elem-quote">
            {{ $t('pages.docker.info.5775aec5') }}
            <div style="padding-left: 10px">
              <a-space direction="vertical" style="width: 100%">
                <div>
                  {{ $t('pages.docker.info.c67593be')
                  }}<a-tag v-if="temp.swarm.nodeAddr">{{ temp.swarm.nodeAddr }}</a-tag>
                  <a-tag>{{ temp.swarm.localNodeState }}</a-tag>
                </div>
                <div v-if="temp.swarm.remoteManagers">
                  {{ $t('pages.docker.info.a2ad7935') }}
                  <a-tag v-for="(item, index) in temp.swarm.remoteManagers" :key="index">{{ item.addr }}</a-tag>
                </div>
                <div>
                  {{ $t('pages.docker.info.631a6968')
                  }}{{
                    temp.swarm.controlAvailable ? $t('pages.docker.info.f5bb2364') : $t('pages.docker.info.5edb2e8a')
                  }}
                </div>
              </a-space>
            </div>
          </div>
        </a-timeline-item>
      </template>
      <a-timeline-item v-if="temp.plugins">
        <div class="layui-elem-quote">
          {{ $t('pages.docker.info.e5c8a0f') }}

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
          {{ $t('pages.docker.info.3401dcf1') }}
          <a-list item-layout="horizontal" :data-source="Object.keys(temp.registryConfig.indexConfigs)" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                {{ item }}
                <a-tag v-if="temp.registryConfig.indexConfigs[item].official" color="green">{{
                  $t('pages.docker.info.a2a559bb')
                }}</a-tag
                ><a-tag v-if="temp.registryConfig.indexConfigs[item].secure" color="green">{{
                  $t('pages.docker.info.ff49be77')
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
