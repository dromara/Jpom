<template>
  <div>
    <a-tabs default-active-key="1">
      <a-tab-pane key="1" :tab="$t('i18n_3c6248b364')">
        <a-descriptions bordered title="" layout="vertical" size="middle">
          <a-descriptions-item :span="3">
            <template #label>
              <a-row>
                <a-col :span="12"> {{ $t('i18n_02d9819dda') }} </a-col>
                <a-col :span="12" style="text-align: right">
                  <a-button size="small" type="link" @click="refreshCache"
                    >{{ $t('i18n_96d46bd22e') }}<ReloadOutlined
                  /></a-button>
                </a-col>
              </a-row>
            </template>
            <div style="color: red; font-weight: bold; font-size: 16px">
              <p>{{ $t('i18n_96b78bfb6a') }}</p>
              <p>{{ $t('i18n_7aaee3201a') }}</p>
            </div>
            <a-tag color="orange">{{ $t('i18n_8b3db55fa4') }}{{ temp.clusterId }}</a-tag>
            <a-tag color="blue">{{ $t('i18n_63e975aa63') }}{{ temp.installId }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_f71a30c1b9')" :span="1">
            {{ renderSize(temp.dataSize) }} ({{ $t('i18n_c996a472f7') }})
            <a-tooltip>
              <template #title>
                <ul>
                  <li>{{ $t('i18n_73578c680e') }}</li>
                  <li>{{ $t('i18n_a0f1bfad78') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_c89e9681c7')" :span="1">
            <a-space>
              <span>{{ renderSize(temp.cacheFileSize) }} (10{{ $t('i18n_6af7686e31') }})</span>
              <a-button
                v-if="temp.cacheFileSize !== '0'"
                size="small"
                type="primary"
                class="btn"
                @click="clear('serviceCacheFileSize')"
                >{{ $t('i18n_288f0c404c') }}</a-button
              >
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_ed19a6eb6f')">
            {{ renderSize(temp.cacheBuildFileSize) }} ({{ $t('i18n_c996a472f7') }})
            <a-tooltip>
              <template #title>
                <ul>
                  <li>{{ $t('i18n_d83aae15b5') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </a-descriptions-item>

          <a-descriptions-item :label="$t('i18n_0d50838436')" :span="1">
            {{ temp.dataPath }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_40f8c95345')" :span="1">
            {{ temp.tempPath }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_51d6b830d4')">
            {{ temp.buildPath }}
          </a-descriptions-item>

          <a-descriptions-item :label="$t('i18n_7d23ca925c')" :span="1">
            {{ temp.dateTime }} <a-tag>{{ temp.timeZoneId }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_815492fd8d')">
            <a-space>
              <span>{{ renderSize(temp.oldJarsSize) }}</span>
              <a-button
                v-if="temp.oldJarsSize !== '0'"
                size="small"
                type="primary"
                class="btn"
                @click="clear('serviceOldJarsSize')"
                >{{ $t('i18n_288f0c404c') }}</a-button
              >
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_4d351f3c91')">
            <a-space>
              <a-popover :title="$t('i18n_4d351f3c91')">
                <template #content
                  ><a-list size="small" bordered :data-source="temp.errorIp">
                    <template #renderItem="{ item }">
                      <a-list-item>
                        {{ item.key }} <a-tag>{{ item.obj }}{{ $t('i18n_7229ecc631') }}</a-tag>
                        <a-tag>{{ $t('i18n_8f40b41e89') }}{{ formatDuration(item.ttl, '') }}</a-tag>
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
                >{{ $t('i18n_288f0c404c') }}</a-button
              >
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_8f0bab9a5a')">
            {{ temp.readFileOnLineCount }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_c5099cadcd')">
            {{ temp.pluginSize || 0 }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_1cc82866a4')">
            {{ temp.shardingSize }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_9adf43e181')">
            <a-popover :title="$t('i18n_853d8ab485')">
              <template #content>
                <p v-for="item in temp.buildKeys || []" :key="item">{{ item }}</p>
              </template>
              <a-space>
                <span>{{ (temp.buildKeys || []).length }}</span>
                <UnorderedListOutlined />
              </a-space>
            </a-popover>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_31ac8d3a5d')">
            <a-popover :title="$t('i18n_3a6000f345')">
              <template #content>
                <p v-for="item in temp.syncFinisKeys || []" :key="item">{{ item }}</p>
              </template>
              <a-space>
                <span>{{ (temp.syncFinisKeys || []).length }}</span>
                <UnorderedListOutlined />
              </a-space>
            </a-popover>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('i18n_87dec8f11e')">
            <a-popover :title="$t('i18n_87dec8f11e')">
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
      <a-tab-pane key="2" :tab="$t('i18n_98e115d868')" force-render>
        <task-stat :task-list="taskList" @refresh="loadData" />
      </a-tab-pane>
      <a-tab-pane key="3" :tab="$t('i18n_43250dc692')">
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_c9b0f8e8c8') + tableName + this.$t('i18n_bbcaac136c'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
