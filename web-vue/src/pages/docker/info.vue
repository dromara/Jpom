<template>
  <div>
    <a-timeline>
      <a-timeline-item>
        <span class="layui-elem-quote">
          {{ $t('i18n_0c5c8d2d11') }}{{ temp.name }} - {{ temp.osType }} - {{ temp.operatingSystem }} -
          <a-tag>{{ temp.architecture }} </a-tag>
          <a-tag>{{ temp.id }}</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">
          {{ $t('i18n_2684c4634d') }}<a-tag>{{ temp.serverVersion }}</a-tag>
          <a-tag>{{ temp.kernelVersion }}</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote"
          >{{ $t('i18n_3d06693eb5') }} <a-tag>cpu:{{ temp.nCPU || temp.NCPU }}</a-tag>
          <a-tag>{{ $t('i18n_af708b659f') }}{{ renderSize(temp.memTotal) }}</a-tag>

          <a-tag>{{ $t('i18n_6b189bf02d') }}{{ temp.containers }}</a-tag>
          <a-tag>{{ $t('i18n_897d865225') }}{{ temp.images }}</a-tag>
        </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">{{ $t('i18n_089a88ecee') }}{{ temp.systemTime }} </span>
      </a-timeline-item>
      <a-timeline-item>
        <span class="layui-elem-quote">{{ $t('i18n_b6728e74a4') }}{{ temp.dockerRootDir }} </span>
      </a-timeline-item>
      <template v-if="temp.swarm">
        <a-timeline-item>
          <div class="layui-elem-quote">
            {{ $t('i18n_e414392917') }}
            <div style="padding-left: 10px">
              <a-space direction="vertical" style="width: 100%">
                <div>
                  {{ $t('i18n_1862c48f72') }}<a-tag v-if="temp.swarm.nodeAddr">{{ temp.swarm.nodeAddr }}</a-tag>
                  <a-tag>{{ temp.swarm.localNodeState }}</a-tag>
                </div>
                <div v-if="temp.swarm.remoteManagers">
                  {{ $t('i18n_2f6989595f') }}
                  <a-tag v-for="(item, index) in temp.swarm.remoteManagers" :key="index">{{ item.addr }}</a-tag>
                </div>
                <div>
                  {{ $t('i18n_47072e451e')
                  }}{{ temp.swarm.controlAvailable ? $t('i18n_0a60ac8f02') : $t('i18n_c9744f45e7') }}
                </div>
              </a-space>
            </div>
          </div>
        </a-timeline-item>
      </template>
      <a-timeline-item v-if="temp.plugins">
        <div class="layui-elem-quote">
          {{ $t('i18n_b9bcb4d623') }}

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
          {{ $t('i18n_92f3fdb65f') }}
          <a-list item-layout="horizontal" :data-source="Object.keys(temp.registryConfig.indexConfigs)" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                {{ item }}
                <a-tag v-if="temp.registryConfig.indexConfigs[item].official" color="green">{{
                  $t('i18n_f5c3795be5')
                }}</a-tag
                ><a-tag v-if="temp.registryConfig.indexConfigs[item].secure" color="green">{{
                  $t('i18n_fdbc77bd19')
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
