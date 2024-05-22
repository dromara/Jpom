<template>
  <div>
    <a-tabs default-active-key="1">
      <a-tab-pane key="1" :tab="$tl('p.cacheInfo')">
        <a-descriptions bordered title="" layout="vertical" size="middle">
          <a-descriptions-item :span="3">
            <template #label>
              <a-row>
                <a-col :span="12"> {{ $tl('p.tip') }} </a-col>
                <a-col :span="12" style="text-align: right">
                  <a-button size="small" type="link" @click="refreshCache"
                    >{{ $tl('p.manualRefreshStats') }}<ReloadOutlined
                  /></a-button>
                </a-col>
              </a-row>
            </template>
            <div style="color: red; font-weight: bold; font-size: 16px">
              <p>{{ $tl('p.dataDirWarning') }}</p>
              <p>{{ $tl('p.deleteBackupWarning') }}</p>
            </div>
            <a-tag color="orange">{{ $tl('p.clusterId') }}{{ temp.clusterId }}</a-tag>
            <a-tag color="blue">{{ $tl('p.installId') }}{{ temp.installId }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="$tl('p.dataDirSpace')" :span="1">
            {{ renderSize(temp.dataSize) }} ({{ $tl('c.refreshTime') }})
            <a-tooltip>
              <template #title>
                <ul>
                  <li>{{ $tl('p.dataDirDescription') }}</li>
                  <li>{{ $tl('p.dataDirContents') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </a-descriptions-item>
          <a-descriptions-item :label="$tl('p.tempSpace')" :span="1">
            <a-space>
              <span>{{ renderSize(temp.cacheFileSize) }} (10{{ $tl('p.refreshInterval') }})</span>
              <a-button
                v-if="temp.cacheFileSize !== '0'"
                size="small"
                type="primary"
                class="btn"
                @click="clear('serviceCacheFileSize')"
                >{{ $tl('c.clear') }}</a-button
              >
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item :label="$tl('p.buildSpace')">
            {{ renderSize(temp.cacheBuildFileSize) }} ({{ $tl('c.refreshTime') }})
            <a-tooltip>
              <template #title>
                <ul>
                  <li>{{ $tl('p.buildDirDescription') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </a-descriptions-item>

          <a-descriptions-item :label="$tl('p.dataDirPath')" :span="1">
            {{ temp.dataPath }}
          </a-descriptions-item>
          <a-descriptions-item :label="$tl('p.tempDirPath')" :span="1"> {{ temp.tempPath }} </a-descriptions-item>
          <a-descriptions-item :label="$tl('p.buildDirPath')">
            {{ temp.buildPath }}
          </a-descriptions-item>

          <a-descriptions-item :label="$tl('p.serverTime')" :span="1">
            {{ temp.dateTime }} <a-tag>{{ temp.timeZoneId }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="$tl('p.oldPackageSpace')">
            <a-space>
              <span>{{ renderSize(temp.oldJarsSize) }}</span>
              <a-button
                v-if="temp.oldJarsSize !== '0'"
                size="small"
                type="primary"
                class="btn"
                @click="clear('serviceOldJarsSize')"
                >{{ $tl('c.clear') }}</a-button
              >
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item :label="$tl('c.blockIP')">
            <a-space>
              <a-popover :title="$tl('c.blockIP')">
                <template #content
                  ><a-list size="small" bordered :data-source="temp.errorIp">
                    <template #renderItem="{ item }">
                      <a-list-item>
                        {{ item.key }} <a-tag>{{ item.obj }}{{ $tl('p.times') }}</a-tag>
                        <a-tag>{{ $tl('p.expiryTime') }}{{ formatDuration(item.ttl, '') }}</a-tag>
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
                >{{ $tl('c.clear') }}</a-button
              >
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item :label="$tl('p.logFilesCount')">
            {{ temp.readFileOnLineCount }}
          </a-descriptions-item>
          <a-descriptions-item :label="$tl('p.pluginsCount')"> {{ temp.pluginSize || 0 }} </a-descriptions-item>
          <a-descriptions-item :label="$tl('p.shardingOperationsCount')"> {{ temp.shardingSize }} </a-descriptions-item>
          <a-descriptions-item :label="$tl('p.buildingCount')">
            <a-popover :title="$tl('p.isBuilding')">
              <template #content>
                <p v-for="item in temp.buildKeys || []" :key="item">{{ item }}</p>
              </template>
              <a-space>
                <span>{{ (temp.buildKeys || []).length }}</span>
                <UnorderedListOutlined />
              </a-space>
            </a-popover>
          </a-descriptions-item>
          <a-descriptions-item :label="$tl('p.threadSynchronizer')">
            <a-popover :title="$tl('p.runningSynchronizer')">
              <template #content>
                <p v-for="item in temp.syncFinisKeys || []" :key="item">{{ item }}</p>
              </template>
              <a-space>
                <span>{{ (temp.syncFinisKeys || []).length }}</span>
                <UnorderedListOutlined />
              </a-space>
            </a-popover>
          </a-descriptions-item>
          <a-descriptions-item :label="$tl('c.errorWorkspaceData')">
            <a-popover :title="$tl('c.errorWorkspaceData')">
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
      <a-tab-pane key="2" :tab="$tl('p.runningTasks')" force-render>
        <task-stat :task-list="taskList" @refresh="loadData" />
      </a-tab-pane>
      <a-tab-pane key="3" :tab="$tl('p.triggerManagement')">
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
        title: this.$tl('p.systemTip'),
        zIndex: 1009,
        content: this.$tl('p.confirmDelete') + tableName + this.$tl('p.confirmErrorMsg'),
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
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
