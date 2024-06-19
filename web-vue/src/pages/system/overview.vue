<template>
  <div>
    <a-page-header :back-icon="false">
      <!-- 【】\u3010\u3011 -->
      <template #title>
        {{ $t('i18n_60585cf697') }}{{ `\u3010` }}{{ getUserInfo.name }}{{ `\u3011` }}{{ $t('i18n_20a9290498') }}
      </template>
      <template #subTitle>{{ $t('i18n_0af5d9f8e8') }} </template>
      <template #tags>
        <a-tag color="blue">
          <template v-if="getUserInfo.demoUser">{{ $t('i18n_20c8dc0346') }}</template>
          <template v-else-if="getUserInfo.superSystemUser">{{ $t('i18n_302ff00ddb') }}</template>
          <template v-else-if="getUserInfo.systemUser">{{ $t('i18n_b1dae9bc5c') }}</template>
          <template v-else>{{ $t('i18n_7527da8954') }}</template>
        </a-tag>
      </template>
      <template #extra>
        <a-tooltip :title="$t('i18n_498519d1af')">
          <a-button :loading="loading" @click="init">
            <template #icon><ReloadOutlined /></template>
          </a-button>
        </a-tooltip>
        <!-- // 擅自修改或者删除版权信息有法律风险，请尊重开源协议，不要擅自修改版本信息，否则可能承担法律责任。 -->
        <a-tooltip :title="$t('i18n_e166aa424d')">
          <a-button @click="toAbout">
            <template #icon><ExclamationCircleOutlined /></template>
          </a-button>
        </a-tooltip>
      </template>
      <a-space>
        <span>
          {{ $t('i18n_fbee13a873') }}
          <a-badge color="blue" :count="statData['workspaceCount'] || '0'" show-zero />
        </span>
        <span
          >{{ $t('i18n_5866b4bced') }}<a-badge color="cyan" :count="statData['clusterCount'] || '0'" show-zero
        /></span>
      </a-space>
    </a-page-header>
    <a-divider dashed />

    <a-row :gutter="[16, 16]">
      <a-col :span="6">
        <a-card size="small">
          <template #title> {{ $t('i18n_a6bf763ede') }} </template>

          <a-list :data-source="['all', ...Object.keys(nodeStatusMap)]">
            <template #renderItem="{ item }">
              <a-list-item v-if="item === 'all'">
                {{ $t('i18n_ec1f13ff6d')
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
          <template #title> {{ $t('i18n_4ad6e58ebc') }} </template>

          <a-list :data-source="['all', ...Object.keys(sshStatusMap)]">
            <template #renderItem="{ item }">
              <a-list-item v-if="item === 'all'">
                {{ $t('i18n_ec1f13ff6d')
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
          <template #title> {{ $t('i18n_ea58a20cda') }} </template>

          <a-list :data-source="['all', ...Object.keys(dockerStatusMap)]">
            <template #renderItem="{ item }">
              <a-list-item v-if="item === 'all'">
                {{ $t('i18n_ec1f13ff6d')
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
          <template #title> {{ $t('i18n_0da9b12963') }} </template>

          <a-list
            :data-source="[
              { name: $t('i18n_1149274cbd'), field: 'userCount', color: 'red' },
              { name: $t('i18n_a76b4f5000'), field: 'systemUserCount', color: 'pink' },
              { name: $t('i18n_828efdf4e5'), field: 'openTwoFactorAuth', color: 'green' },
              { name: $t('i18n_c03465ca03'), field: 'disableUserCount', color: 'yellow' }
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
