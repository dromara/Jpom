<template>
  <div>
    <a-page-header :back-icon="false">
      <template #title> {{ $tl('p.k1') }}{{ getUserInfo.name }}{{ $tl('p.k2') }}</template>
      <template #subTitle>{{ $tl('p.k3') }} </template>
      <template #tags>
        <a-tag color="blue">
          <template v-if="getUserInfo.demoUser">{{ $tl('p.k4') }}</template>
          <template v-else-if="getUserInfo.superSystemUser">{{ $tl('p.k5') }}</template>
          <template v-else-if="getUserInfo.systemUser">{{ $tl('p.k6') }}</template>
          <template v-else>{{ $tl('p.k7') }}</template>
        </a-tag>
      </template>
      <template #extra>
        <a-tooltip :title="$tl('p.k8')">
          <a-button :loading="loading" @click="init">
            <template #icon><ReloadOutlined /></template>
          </a-button>
        </a-tooltip>
        <!-- // 擅自修改或者删除版权信息有法律风险，请尊重开源协议，不要擅自修改版本信息，否则可能承担法律责任。 -->
        <a-tooltip :title="$tl('p.k9')">
          <a-button @click="toAbout">
            <template #icon><ExclamationCircleOutlined /></template>
          </a-button>
        </a-tooltip>
      </template>
      <a-space>
        <span> {{ $tl('p.k10') }} <a-badge color="blue" :count="statData['workspaceCount'] || '0'" show-zero /> </span>
        <span>{{ $tl('p.k11') }}<a-badge color="cyan" :count="statData['clusterCount'] || '0'" show-zero /></span>
      </a-space>
    </a-page-header>
    <a-divider dashed />

    <a-row :gutter="[16, 16]">
      <a-col :span="6">
        <a-card size="small">
          <template #title> {{ $tl('p.k12') }} </template>

          <a-list :data-source="['all', ...Object.keys(nodeStatusMap)]">
            <template #renderItem="{ item }">
              <a-list-item v-if="item === 'all'">
                {{ $tl('p.totalCount')
                }}<a-badge
                  :color="item.color"
                  :count="
                    (statData.nodeStat &&
                      statData.nodeStat.reduce(function (sum, item2) {
                        return sum + Number(item2.count)
                      }, 0)) ||
                    '0'
                  "
                  show-zero
                />
              </a-list-item>
              <a-list-item v-else>
                {{ nodeStatusMap[item] }}：<a-badge
                  :color="Number(item) === 1 ? 'green' : ''"
                  :count="
                    (statData.nodeStat &&
                      statData.nodeStat.find((item2) => {
                        return item2.status === Number(item)
                      }) &&
                      statData.nodeStat.find((item2) => {
                        return item2.status === Number(item)
                      }).count) ||
                    '0'
                  "
                  show-zero
                />
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <template #title> {{ $tl('p.k13') }} </template>

          <a-list :data-source="['all', ...Object.keys(sshStatusMap)]">
            <template #renderItem="{ item }">
              <a-list-item v-if="item === 'all'">
                {{ $tl('p.totalCount')
                }}<a-badge
                  :color="item.color"
                  :count="
                    (statData.sshStat &&
                      statData.sshStat.reduce(function (sum, item2) {
                        return sum + Number(item2.count)
                      }, 0)) ||
                    '0'
                  "
                  show-zero
                />
              </a-list-item>
              <a-list-item v-else>
                {{ sshStatusMap[item].desc }}：<a-badge
                  :color="sshStatusMap[item].color"
                  :count="
                    (statData.sshStat &&
                      statData.sshStat.find((item2) => {
                        return item2.status === Number(item)
                      }) &&
                      statData.sshStat.find((item2) => {
                        return item2.status === Number(item)
                      }).count) ||
                    '0'
                  "
                  show-zero
                />
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <template #title> {{ $tl('p.k14') }} </template>

          <a-list :data-source="['all', ...Object.keys(dockerStatusMap)]">
            <template #renderItem="{ item }">
              <a-list-item v-if="item === 'all'">
                {{ $tl('p.totalCount')
                }}<a-badge
                  :color="item.color"
                  :count="
                    (statData.dockerStat &&
                      statData.dockerStat.reduce(function (sum, item2) {
                        return sum + Number(item2.count)
                      }, 0)) ||
                    '0'
                  "
                  show-zero
                />
              </a-list-item>
              <a-list-item v-else>
                {{ dockerStatusMap[item].desc }}：<a-badge
                  :color="dockerStatusMap[item].color"
                  :count="
                    (statData.dockerStat &&
                      statData.dockerStat.find((item2) => {
                        return item2.status === Number(item)
                      }) &&
                      statData.dockerStat.find((item2) => {
                        return item2.status === Number(item)
                      }).count) ||
                    '0'
                  "
                  show-zero
                />
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <template #title> {{ $tl('p.k15') }} </template>

          <a-list
            :data-source="[
              { name: $tl('p.k16'), field: 'userCount', color: 'red' },
              { name: $tl('p.k17'), field: 'systemUserCount', color: 'pink' },
              { name: $tl('p.k18'), field: 'openTwoFactorAuth', color: 'green' },
              { name: $tl('p.k19'), field: 'disableUserCount', color: 'yellow' }
            ]"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                {{ item.name }}：<a-badge :color="item.color" :count="statData[item.field] || '0'" show-zero />
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { statSystemOverview } from '@/api/user/user'

import { statusMap as nodeStatusMap } from '@/api/system/assets-machine'
import { statusMap as sshStatusMap } from '@/api/system/assets-ssh'
import { statusMap as dockerStatusMap } from '@/api/system/assets-docker'
import { useUserStore } from '@/stores/user'
import { mapState } from 'pinia'

import { Empty } from 'ant-design-vue'
export default {
  components: {},
  data() {
    return {
      Empty,
      dockerStatusMap,
      nodeStatusMap,
      sshStatusMap,
      loading: true,
      statData: {}
    }
  },
  computed: {
    ...mapState(useUserStore, ['getUserInfo'])
  },
  created() {
    this.init()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.system.overview.${key}`, ...args)
    },
    init() {
      // 数据
      this.loading = true
      statSystemOverview()
        .then((res) => {
          if (res.code == 200 && res.data) {
            this.statData = res.data || {}
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    toAbout() {
      this.$router.push({
        path: '/about'
      })
    }
  }
}
</script>

<style scoped>
:deep(.ant-divider-horizontal) {
  margin: 5px 0;
}
</style>
