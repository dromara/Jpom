<template>
  <div>
    <a-page-header :backIcon="false">
      <template #title> 欢迎【{{ getUserInfo.name }}】您来到系统管理中心</template>
      <template #subTitle>当前区域为系统管理、资产管理中心 </template>
      <template v-slot:tags>
        <a-tag color="blue">
          <template v-if="getUserInfo.demoUser">演示账号</template>
          <template v-else-if="getUserInfo.superSystemUser">超级管理员</template>
          <template v-else-if="getUserInfo.systemUser">管理员</template>
          <template v-else>普通用户</template>
        </a-tag>
      </template>
      <template v-slot:extra>
        <a-button @click="init" :loading="loading">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </template>
      <a-space>
        <span> 工作空间总数： <a-badge color="blue" :count="statData['workspaceCount'] || '0'" showZero /> </span>
        <span>集群数：<a-badge color="cyan" :count="statData['clusterCount'] || '0'" showZero /></span>
      </a-space>
    </a-page-header>
    <a-divider dashed />

    <a-row :gutter="[16, 16]">
      <a-col :span="6">
        <a-card size="small">
          <template #title> 机器节点 </template>

          <a-list :data-source="['all', ...Object.keys(nodeStatusMap)]">
            <template #renderItem="{ item }">
              <a-list-item v-if="item === 'all'">
                总数：<a-badge
                  :color="item.color"
                  :count="
                    (statData.nodeStat &&
                      statData.nodeStat.reduce(function (sum, item2) {
                        return sum + Number(item2.count)
                      }, 0)) ||
                    '0'
                  "
                  showZero
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
                  showZero
                />
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <template #title> 机器SSH </template>

          <a-list :data-source="['all', ...Object.keys(sshStatusMap)]">
            <template #renderItem="{ item }">
              <a-list-item v-if="item === 'all'">
                总数：<a-badge
                  :color="item.color"
                  :count="
                    (statData.sshStat &&
                      statData.sshStat.reduce(function (sum, item2) {
                        return sum + Number(item2.count)
                      }, 0)) ||
                    '0'
                  "
                  showZero
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
                  showZero
                />
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <template #title> 机器DOCKER </template>

          <a-list :data-source="['all', ...Object.keys(dockerStatusMap)]">
            <template #renderItem="{ item }">
              <a-list-item v-if="item === 'all'">
                总数：<a-badge
                  :color="item.color"
                  :count="
                    (statData.dockerStat &&
                      statData.dockerStat.reduce(function (sum, item2) {
                        return sum + Number(item2.count)
                      }, 0)) ||
                    '0'
                  "
                  showZero
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
                  showZero
                />
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <template #title> 用户数据 </template>

          <a-list
            :data-source="[
              { name: '用户总数', field: 'userCount', color: 'red' },
              { name: '管理员数', field: 'systemUserCount', color: 'pink' },
              { name: '开启MFA数', field: 'openTwoFactorAuth', color: 'green' },
              { name: '禁用数量', field: 'disableUserCount', color: 'yellow' }
            ]"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                {{ item.name }}：<a-badge :color="item.color" :count="statData[item.field] || '0'" showZero />
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
  computed: {},
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
    }
  }
}
</script>

<style scoped>
:deep(.ant-divider-horizontal) {
  margin: 5px 0;
}
</style>
