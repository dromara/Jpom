<template>
  <div>
    <a-tabs default-active-key="1">
      <a-tab-pane key="1" :tab="$t('pages.system.cache.f84aaf3f')">
        <a-descriptions bordered title="" layout="vertical" size="middle">
          <a-descriptions-item :span="3">
            <template #label>
              <a-row>
                <a-col :span="12"> {{ $t('pages.system.cache.c8dfae81') }} </a-col>
                <a-col :span="12" style="text-align: right">
                  <a-button size="small" type="link" @click="refreshCache"
                    >{{ $t('pages.system.cache.39a91e50') }}<ReloadOutlined
                  /></a-button>
                </a-col>
              </a-row>
            </template>
            <div style="color: red; font-weight: bold; font-size: 16px">
              <p>{{ $t('pages.system.cache.3d1252d1') }}</p>
              <p>{{ $t('pages.system.cache.404fcd19') }}</p>
            </div>
            <a-tag color="orange">{{ $t('pages.system.cache.29c6f407') }}{{ temp.clusterId }}</a-tag>
            <a-tag color="blue">{{ $t('pages.system.cache.43a8a564') }}{{ temp.installId }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.a77a5385')" :span="1">
            {{ renderSize(temp.dataSize) }} ({{ $t('pages.system.cache.e597af7') }})
            <a-tooltip>
              <template #title>
                <ul>
                  <li>{{ $t('pages.system.cache.3a18b93') }}</li>
                  <li>{{ $t('pages.system.cache.3754c87e') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.3ea7d5bd')" :span="1">
            <a-space>
              <span>{{ renderSize(temp.cacheFileSize) }} (10{{ $t('pages.system.cache.4f15720') }})</span>
              <a-button
                v-if="temp.cacheFileSize !== '0'"
                size="small"
                type="primary"
                class="btn"
                @click="clear('serviceCacheFileSize')"
                >{{ $t('pages.system.cache.3907eb5b') }}</a-button
              >
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.da1578f3')">
            {{ renderSize(temp.cacheBuildFileSize) }} ({{ $t('pages.system.cache.e597af7') }})
            <a-tooltip>
              <template #title>
                <ul>
                  <li>{{ $t('pages.system.cache.85e8a976') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </a-descriptions-item>

          <a-descriptions-item :label="$t('pages.system.cache.bd29cd92')" :span="1">
            {{ temp.dataPath }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.aa466cbc')" :span="1">
            {{ temp.tempPath }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.6cdb35d0')">
            {{ temp.buildPath }}
          </a-descriptions-item>

          <a-descriptions-item :label="$t('pages.system.cache.d4fbafe7')" :span="1">
            {{ temp.dateTime }} <a-tag>{{ temp.timeZoneId }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.890d7cd0')">
            <a-space>
              <span>{{ renderSize(temp.oldJarsSize) }}</span>
              <a-button
                v-if="temp.oldJarsSize !== '0'"
                size="small"
                type="primary"
                class="btn"
                @click="clear('serviceOldJarsSize')"
                >{{ $t('pages.system.cache.3907eb5b') }}</a-button
              >
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.30a7724e')">
            <a-space>
              <a-popover :title="$t('pages.system.cache.30a7724e')">
                <template #content
                  ><a-list size="small" bordered :data-source="temp.errorIp">
                    <template #renderItem="{ item }">
                      <a-list-item>
                        {{ item.key }} <a-tag>{{ item.obj }}{{ $t('pages.system.cache.97577282') }}</a-tag>
                        <a-tag>{{ $t('pages.system.cache.19741306') }}{{ formatDuration(item.ttl, '') }}</a-tag>
                      </a-list-item>
                    </template>
                  </a-list>
                </template>
                {{ (temp.errorIp && temp.errorIp.length) || 0 }}
                <UnorderedListOutlined />
              </a-popover>
              <a-button
                v-if="temp.errorIp && temp.errorIp.length"
                size="small"
                type="primary"
                class="btn"
                @click="clear('serviceIpSize')"
                >{{ $t('pages.system.cache.3907eb5b') }}</a-button
              >
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.523292ce')">
            {{ temp.readFileOnLineCount }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.3f0bc541')">
            {{ temp.pluginSize || 0 }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.d3fa4a5a')">
            {{ temp.shardingSize }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.671ddeb8')">
            <a-popover :title="$t('pages.system.cache.1292ef0b')">
              <template #content>
                <p v-for="item in temp.buildKeys || []" :key="item">{{ item }}</p>
              </template>
              <a-space>
                <span>{{ (temp.buildKeys || []).length }}</span>
                <UnorderedListOutlined />
              </a-space>
            </a-popover>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.a9e6f116')">
            <a-popover :title="$t('pages.system.cache.1a77cfa7')">
              <template #content>
                <p v-for="item in temp.syncFinisKeys || []" :key="item">{{ item }}</p>
              </template>
              <a-space>
                <span>{{ (temp.syncFinisKeys || []).length }}</span>
                <UnorderedListOutlined />
              </a-space>
            </a-popover>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('pages.system.cache.d1b72f8b')">
            <a-popover :title="$t('pages.system.cache.d1b72f8b')">
              <template #content>
                <a-collapse>
                  <a-collapse-panel v-for="(item, key) in temp.errorWorkspace" :key="key" :header="key">
                    <p v-for="(item2, index) in item" :key="index">{{ item2 }}</p>
                    <template #extra>
                      <DeleteOutlined
                        @click="
                          (e) => {
                            handleClearErrorWorkspaceClick(e, key)
                          }
                        "
                      />
                    </template>
                  </a-collapse-panel>
                </a-collapse>
              </template>
              <a-space>
                <span>{{ Object.keys(temp.errorWorkspace || {}).length }}</span>
                <UnorderedListOutlined />
              </a-space>
            </a-popover>
          </a-descriptions-item>
        </a-descriptions>
        <!-- <a-timeline>
          <a-timeline-item> </a-timeline-item>
          <a-timeline-item> </a-timeline-item>
        </a-timeline> -->
      </a-tab-pane>
      <a-tab-pane key="2" :tab="$t('pages.system.cache.38a00cac')" force-render>
        <task-stat :task-list="taskList" @refresh="loadData" />
      </a-tab-pane>
      <a-tab-pane key="3" :tab="$t('pages.system.cache.b62d1886')">
        <TriggerToken />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>
<script>
import { getServerCache, clearCache, clearErrorWorkspace, asyncRefreshCache } from '@/api/system'
import TaskStat from '@/pages/system/taskStat'
import TriggerToken from '@/pages/system/trigger-token'
import { renderSize, formatDuration } from '@/utils/const'
export default {
  components: {
    TaskStat,
    TriggerToken
  },
  data() {
    return {
      temp: {},
      taskList: []
    }
  },
  mounted() {
    this.loadData()
    // console.log(Comparator);
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.system.cache.${key}`, ...args)
    },
    renderSize,
    formatDuration,
    // load data
    loadData() {
      getServerCache().then((res) => {
        if (res.code === 200) {
          this.temp = res.data
          this.taskList = res.data?.taskList
        }
      })
    },
    refreshCache() {
      asyncRefreshCache().then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
        }
      })
    },
    // clear
    clear(type) {
      const params = {
        type: type,
        nodeId: ''
      }
      clearCache(params).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    },
    handleClearErrorWorkspaceClick(event, tableName) {
      // If you don't want click extra trigger collapse, you can prevent this:
      event.stopPropagation()
      $confirm({
        title: this.$t('pages.system.cache.e422d0eb'),
        zIndex: 1009,
        content: this.$t('pages.system.cache.987c2cd6') + tableName + this.$t('pages.system.cache.7d2dcb27'),
        okText: this.$t('pages.system.cache.e8e9db25'),
        cancelText: this.$t('pages.system.cache.b12468e9'),
        onOk: () => {
          return clearErrorWorkspace({ tableName }).then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    }
  }
}
</script>
