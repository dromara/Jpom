<template>
  <div>
    <a-tabs tab-position="left" @change="changeTabs">
      <template #leftExtra>
        <a-space>
          <a-statistic-countdown format="s" title="" :value="countdownTime" @finish="pullNodeData">
            <template #suffix>
              <div style="font-size: 12px">{{ $t('i18n_ff80d2671c') }}</div>
            </template>
          </a-statistic-countdown>
          <!-- <a-statistic-countdown title="" :value="countdownTime" @finish="pullNodeData" /> -->
        </a-space>
      </template>
      <a-tab-pane key="info" :tab="$t('i18n_6ea1fe6baa')">
        <a-card size="small">
          <template #title>
            {{ machineInfo && machineInfo.name }}
          </template>
          <template #extra>
            <a-tag
              v-if="machineInfo"
              :color="machineInfo && machineInfo.status === 1 ? 'green' : 'pink'"
              style="margin-right: 0"
            >
              {{ statusMap[machineInfo && machineInfo.status] || $t('i18n_1622dc9b6b') }}
            </a-tag>
          </template>
          <a-space direction="vertical" style="display: block">
            <a-alert
              v-if="machineInfo && machineInfo.status !== 1"
              :message="$t('i18n_920f05031b')"
              :description="(machineInfo && machineInfo.statusMsg) || ''"
              type="warning"
              show-icon
            />
            <a-descriptions :column="4" :bordered="true">
              <template #title> </template>

              <a-descriptions-item :label="$t('i18n_cdc478d90c')" :span="2">{{
                machineInfo && machineInfo.osName
              }}</a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_1857e7024c')" :span="2">{{
                machineInfo && machineInfo.osVersion
              }}</a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_375f853ad6')" :span="2">
                {{ machineInfo && machineInfo.osHardwareVersion }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_899dbd7b9a')" :span="2">
                {{ machineInfo && machineInfo.osCpuIdentifierName }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_6707667676')" :span="2">
                {{ machineInfo && machineInfo.hostName }}
              </a-descriptions-item>
              <a-descriptions-item label="IPV4" :span="2">
                <template v-if="machineInfo && machineInfo.ipv4List && machineInfo.ipv4List.length">
                  {{ machineInfo && machineInfo.ipv4List[0] }}
                  <a-popover :title="$t('i18n_b5c5078a5d')">
                    <template #content>
                      <p v-for="item in machineInfo && machineInfo.ipv4List" :key="item">
                        {{ item }}
                      </p>
                    </template>
                    <a-tag>
                      {{ machineInfo && machineInfo.ipv4List && machineInfo.ipv4List.length }}

                      <EllipsisOutlined />
                    </a-tag>
                  </a-popover>
                </template>
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_067638bede')"
                >{{ machineInfo && machineInfo.osCpuCores }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_9932551cd5')"
                >{{ renderSize(machineInfo && machineInfo.osMoneyTotal) }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_844296754e')"
                >{{ renderSize(machineInfo && machineInfo.osVirtualMax) }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_5a879a657b')"
                >{{ renderSize(machineInfo && machineInfo.osSwapTotal) }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_1d650a60a5')"
                >{{ renderSize(machineInfo && machineInfo.osFileStoreTotal) }}
              </a-descriptions-item>

              <a-descriptions-item :label="$t('i18n_4956eb6aaa')"
                >{{ machineInfo && machineInfo.osLoadAverage }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_efb88b3927')"
                >{{ formatDuration(((machineInfo && machineInfo.osSystemUptime) || 0) * 1000, '', 3) }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_a912a83e6f')"
                >{{ machineInfo && machineInfo.jpomVersion }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_04a8742dd7')"
                >{{ formatDuration(machineInfo && machineInfo.jpomUptime, '', 3) }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_efd32e870d')"
                >{{ machineInfo && machineInfo.jpomBuildTime }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_6106de3d87')"
                >{{ machineInfo && machineInfo.javaVersion }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_1818e9c264')"
                >{{ renderSize(machineInfo && machineInfo.jvmTotalMemory) }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_5bb162ecbb')"
                >{{ renderSize(machineInfo && machineInfo.jvmFreeMemory) }}
              </a-descriptions-item>

              <a-descriptions-item :label="$t('i18n_607558dbd4')"
                >{{ machineInfo && machineInfo.jpomProjectCount }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_e39f4a69f4')"
                >{{ machineInfo && machineInfo.jpomScriptCount }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_204222d167')"
                >{{ formatDuration(machineInfo && machineInfo.networkDelay) }}
              </a-descriptions-item>

              <a-descriptions-item :label="$t('i18n_ed145eba38')" :span="4">
                <a-progress
                  :stroke-color="{
                    '0%': '#87d068',
                    '100%': 'red'
                  }"
                  :percent="formatPercent2Number((machineInfo && machineInfo.osOccupyDisk) || 0)"
                />
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_883848dd37')" :span="4">
                <a-progress
                  :stroke-color="{
                    '0%': '#87d068',
                    '100%': 'red'
                  }"
                  :percent="formatPercent2Number((machineInfo && machineInfo.osOccupyMemory) || 0)"
                />
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_80669da961')" :span="4">
                <a-progress
                  :stroke-color="{
                    '0%': '#87d068',
                    '100%': 'red'
                  }"
                  :percent="formatPercent2Number((machineInfo && machineInfo.osOccupyCpu) || 0)"
                />
              </a-descriptions-item>
            </a-descriptions>
          </a-space>
        </a-card>
      </a-tab-pane>
      <a-tab-pane key="stat" :tab="$t('i18n_aa9236568f')">
        <a-space v-if="nodeMonitorLoadStatus === 1" direction="vertical" style="width: 100%">
          <a-card size="small" :title="$t('i18n_6ea1fe6baa')">
            <template #extra>
              <a-button v-if="historyChart" size="small" type="primary" @click="handleHistory('')">
                <AreaChartOutlined />{{ $t('i18n_5068552b18') }}
              </a-button>
            </template>
            <!-- top 图表 -->
            <div id="top-chart" class="chart">loading...</div>
          </a-card>
          <a-card size="small">
            <template #title>
              <a-space :size="4">
                <template #split>
                  <a-divider type="vertical" />
                </template>
                {{ $t('i18n_102e8ec6d5') }}
                <template v-if="monitorConfig?.network?.statExcludeNames">
                  <span>
                    {{ $t('i18n_503660aa89') }}
                    <a-tag v-for="item in monitorConfig?.network?.statExcludeNames?.split(',')">
                      {{ item }}
                    </a-tag>
                  </span>
                </template>
                <template v-if="monitorConfig?.network?.statContainsOnlyNames">
                  <span>
                    {{ $t('i18n_bb7eeae618') }}
                    <a-tag v-for="item in monitorConfig?.network?.statContainsOnlyNames?.split(',')">
                      {{ item }}
                    </a-tag>
                  </span>
                </template>
                <a-popover>
                  <template #title>{{ $t('i18n_fb8fb9cc46') }} </template>
                  <template #content>
                    <b>{{ $t('i18n_13f7bb78ef') }}</b>
                    <div>
                      {{ $t('i18n_475cd76aec') }}
                      <a-tag v-for="item in JSON.parse(machineInfo?.extendInfo || '{}')?.monitorIfsNames?.split(',')">
                        {{ item }}
                      </a-tag>
                    </div>
                  </template>
                  <QuestionCircleOutlined />
                </a-popover>
              </a-space>
            </template>
            <template #extra>
              <a-button v-if="netHistoryChart" size="small" type="primary" @click="handleHistory('network-stat')">
                <AreaChartOutlined />
                {{ $t('i18n_5068552b18') }}
              </a-button>
            </template>
            <!-- 网络流量图表 -->
            <div id="net-chart" class="chart">loading...</div>
          </a-card>
          <a-card size="small" :title="$t('i18n_bbbaeb32fc')">
            <template #extra>
              <a-button v-if="networkDelayChart" size="small" type="primary" @click="handleHistory('networkDelay')">
                <AreaChartOutlined />{{ $t('i18n_5068552b18') }}
              </a-button>
            </template>
            <!-- 机器延迟 图表 -->
            <div id="network-delay-chart" class="chart">loading...</div>
          </a-card>
        </a-space>
        <a-empty
          v-else-if="nodeMonitorLoadStatus === -1"
          :image="Empty.PRESENTED_IMAGE_SIMPLE"
          :description="$t('i18n_536206b587')"
        >
        </a-empty>
        <a-skeleton v-else />
      </a-tab-pane>
      <a-tab-pane key="process" :tab="$t('i18n_d6a5b67779')">
        <a-card size="small">
          <template #title>
            <a-row>
              <a-col :span="18">
                <a-space>
                  <custom-select
                    v-model:value="processSearch.processName"
                    class="search-input-item"
                    sel-style="width: 200px !important"
                    :data="processNames"
                    :popup-container-parent="false"
                    :input-placeholder="$t('i18n_2493ff1a29')"
                    :select-placeholder="$t('i18n_bab17dc6b1')"
                    @change="loadNodeProcess"
                    @add-option="addNodeProcess"
                  >
                    <template #suffixIcon> <DownOutlined /></template>
                  </custom-select>
                  <a-tooltip :title="$t('i18n_5dc7b04caa')">
                    <a-input-number v-model:value="processSearch.processCount" :min="1" @change="loadNodeProcess" />
                  </a-tooltip>
                  <a-tooltip :title="$t('i18n_8400529cfb')">
                    <RestOutlined @click="restProcessNames" />
                  </a-tooltip>
                </a-space>
              </a-col>
            </a-row>
          </template>

          <a-table
            size="middle"
            :loading="loading"
            :columns="processColumns"
            :data-source="processList"
            bordered
            row-key="processId"
            :scroll="{
              x: 'max-content'
            }"
            :pagination="false"
          >
            <template #bodyCell="{ column, text, record }">
              <template v-if="column.percentTooltip">
                <a-tooltip placement="topLeft" :title="formatPercent(text)">
                  {{ formatPercent(text) }}
                </a-tooltip>
              </template>
              <template v-else-if="column.timeTooltip">
                <a-tooltip placement="topLeft" :title="parseTime(text)">
                  {{ parseTime(text) }}
                </a-tooltip>
              </template>
              <template v-else-if="column.durationTooltip">
                <a-tooltip placement="topLeft" :title="formatDuration(text)">
                  {{ formatDuration(text, '', 2) }}
                </a-tooltip>
              </template>

              <template v-else-if="column.sizeTooltip">
                <a-tooltip placement="topLeft" :title="renderSize(text)">
                  {{ renderSize(text) }}
                </a-tooltip>
              </template>
              <template v-else-if="column.tooltip">
                <a-tooltip placement="topLeft" :title="text">
                  {{ text }}
                </a-tooltip>
              </template>
              <template v-else-if="column.dataIndex === 'operation'">
                <a-button type="primary" size="small" @click="kill(record)">Kill</a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
      <a-tab-pane key="disk" :tab="$t('i18n_8dc8bbbc20')">
        <a-table
          size="middle"
          :loading="diskLoading"
          :columns="diskColumns"
          :data-source="diskList"
          bordered
          row-key="uuid"
          :scroll="{
            x: 'max-content'
          }"
          :pagination="false"
        >
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.percentTooltip">
              <a-tooltip placement="topLeft" :title="formatPercent(text)">
                {{ formatPercent(text) }}
              </a-tooltip>
            </template>

            <template v-else-if="column.sizeTooltip">
              <a-tooltip placement="topLeft" :title="renderSize(text)">
                {{ renderSize(text) }}
              </a-tooltip>
            </template>
            <template v-else-if="column.tooltip">
              <a-tooltip placement="topLeft" :title="text">
                {{ text }}
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'operation'">
              <a-button type="primary" size="small" @click="kill(record)">Kill</a-button>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="hw-disk" :tab="$t('i18n_dacc2e0e62')">
        <a-collapse>
          <a-collapse-panel v-for="item in hwDiskList" :key="item.uuid">
            <template #header>
              <a-page-header :title="item.name" :back-icon="false">
                <template #subTitle> {{ item.model }} </template>
                <a-descriptions size="small" :column="4">
                  <a-descriptions-item :label="$t('i18n_faaadc447b')">{{ item.serial }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('i18n_58f9666705')">{{ renderSize(item.size) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('i18n_9f70e40e04')">{{
                    formatDuration(item.transferTime)
                  }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('i18n_e63fb95deb')">
                    {{ item.currentQueueLength }}
                  </a-descriptions-item>
                </a-descriptions>
                <a-descriptions size="small" :column="4">
                  <a-descriptions-item :label="$t('i18n_8900539e06')">
                    {{ renderSize(item.writeBytes) }}
                  </a-descriptions-item>
                  <a-descriptions-item :label="$t('i18n_8fda053c83')">{{ item.writes }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('i18n_86f3ec932c')">
                    {{ renderSize(item.readBytes) }}
                  </a-descriptions-item>
                  <a-descriptions-item :label="$t('i18n_3b14c524f6')">{{ item.reads }}</a-descriptions-item>
                </a-descriptions>
              </a-page-header>
            </template>
            <a-table
              size="middle"
              :columns="hwDiskPartitionColumns"
              :data-source="item.partition"
              :pagination="false"
              :scroll="{
                x: 'max-content'
              }"
            >
              <template v-if="column.tooltip">
                <a-tooltip placement="topLeft" :title="text">
                  {{ text }}
                </a-tooltip>
              </template>
              <template v-else-if="column.sizeTooltip">
                <a-tooltip placement="topLeft" :title="renderSize(text)">
                  {{ renderSize(text) }}
                </a-tooltip>
              </template>
            </a-table>
          </a-collapse-panel>
        </a-collapse>
      </a-tab-pane>
      <a-tab-pane key="networkInterfaces" :tab="$t('i18n_0ac4999a4c')">
        <a-collapse v-if="networkInterfaces && networkInterfaces.length">
          <a-collapse-panel v-for="(item, index) in networkInterfaces" :key="index">
            <template #header>
              {{ item.name }}
              <a-tag>
                {{ item.displayName }}
              </a-tag>
              <a-tag>
                {{ item.ifAlias }}
              </a-tag>
              <!-- /**
           * Up and operational. Ready to pass packets.
           */
          UP(1),
          /**
           * Down and not operational. Not ready to pass packets.
           */
          DOWN(2),
          /**
           * In some test mode.
           */
          TESTING(3),
          /**
           * The interface status is unknown.
           */
          UNKNOWN(4),
          /**
           * The interface is not up, but is in a pending state, waiting for some external event.
           */
          DORMANT(5),
          /**
           * Some component is missing
           */
          NOT_PRESENT(6),
          /**
           * Down due to state of lower-layer interface(s).
           */
          LOWER_LAYER_DOWN(7); -->

              <a-tag v-if="item.ifOperStatus === 'UP'" color="green">{{ item.ifOperStatus }}</a-tag>
              <a-tag
                v-else-if="
                  item.ifOperStatus === 'DOWN' || item.ifOperStatus === 'TESTING' || item.ifOperStatus === 'DORMANT'
                "
                color="orange"
                >{{ item.ifOperStatus }}</a-tag
              >
              <a-tag
                v-else-if="
                  item.ifOperStatus === 'UNKNOWN' ||
                  item.ifOperStatus === 'NOT_PRESENT' ||
                  item.ifOperStatus === 'LOWER_LAYER_DOWN'
                "
                color="red"
                >{{ item.ifOperStatus }}</a-tag
              >
              <a-tag v-else>{{ item.ifOperStatus }}</a-tag>
            </template>
            <a-descriptions title="" bordered :column="4">
              <a-descriptions-item label="MAC">
                {{ item.macaddr }}
              </a-descriptions-item>
              <a-descriptions-item label="MTU">
                {{ item.mtu }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_03f38597a6')">{{ renderBpsSize(item.speed) }} </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_2a3b06a91a')"
                >{{ item.knownVmMacAddr ? $t('i18n_0a60ac8f02') : $t('i18n_c9744f45e7') }}
              </a-descriptions-item>

              <a-descriptions-item label="IPV4" :span="4">
                <a-tag v-for="ipItem in item.ipv4addr || []" :key="ipItem">{{ ipItem }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="IPV6" :span="4">
                <a-tag v-for="ipItem in item.ipv6addr || []" :key="ipItem">{{ ipItem }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_7bcbf81120')">{{ item.packetsRecv }} </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_c6a3ebf3c4')"
                >{{ renderSize(item.bytesRecv) }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_05cfc9af9d')">{{ item.inErrors }} </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_5ef040a79d')">{{ item.tnDrops }} </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_00b04e1bf0')">{{ item.packetsSent }} </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_020d17aac6')"
                >{{ renderSize(item.bytesSent) }}
              </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_235f0b52a1')">{{ item.outErrors }} </a-descriptions-item>
              <a-descriptions-item :label="$t('i18n_3f719b3e32')">{{ item.collisions }} </a-descriptions-item>
            </a-descriptions>
          </a-collapse-panel>
        </a-collapse>
        <a-empty v-else :description="$t('i18n_aa53a4b93a')" />
      </a-tab-pane>
    </a-tabs>

    <!-- 历史监控 -->
    <CustomModal
      v-if="monitorVisible.visible"
      v-model:open="monitorVisible.visible"
      destroy-on-close
      width="75%"
      :title="$t('i18n_5068552b18')"
      :footer="null"
      :mask-closable="false"
    >
      <node-top
        v-if="monitorVisible && monitorVisible.visible"
        :node-id="nodeId"
        :machine-id="machineId"
        :type="monitorVisible.type"
      ></node-top>
    </CustomModal>
  </div>
</template>
<script>
import { nodeMonitorData, getProcessList, killPid } from '@/api/node'
import {
  renderSize,
  formatPercent,
  parseTime,
  formatDuration,
  formatPercent2Number,
  renderBpsSize
} from '@/utils/const'
import CustomSelect from '@/components/customSelect'
import NodeTop from '@/pages/node/node-layout/node-top'
import {
  generateNodeTopChart,
  drawChart,
  machineInfo,
  generateNodeNetChart,
  machineDiskInfo,
  machineHwDiskInfo,
  generateNodeNetworkTimeChart,
  machineNetworkInterfaces
} from '@/api/node-stat'
import { Empty } from 'ant-design-vue'
import { statusMap, machineMonitorConfig } from '@/api/system/assets-machine'
import { useGuideStore } from '@/stores/guide'
import { mapState } from 'pinia'
export default {
  components: {
    CustomSelect,
    NodeTop
  },
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    machineId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      Empty,
      loading: false,
      diskLoading: false,
      statusMap,
      processList: [],
      diskList: [],
      hwDiskList: [],
      defaultProcessNames: ['java', 'python', 'mysql', 'php', 'docker'],
      processNames: [],
      monitorVisible: {
        visible: false,
        type: ''
      },
      timeRange: '',
      historyData: [],
      processSearch: {
        processName: 'java',
        processCount: 20
      },
      processColumns: [
        {
          title: 'ID',
          dataIndex: 'processId',
          width: '80px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          width: '80px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_c76cfefe72'),
          dataIndex: 'port',
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_b05345caad'),
          dataIndex: 'user',
          width: '100px',
          ellipsis: true,
          tooltip: true
        },

        {
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: 'state',
          width: '80px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_844296754e'),
          dataIndex: 'virtualSize',
          width: '100px',
          ellipsis: true,
          sizeTooltip: true
        },
        {
          title: 'CPU',
          dataIndex: 'processCpuLoadCumulative',
          width: '100px',
          ellipsis: true,
          percentTooltip: true
        },
        {
          title: this.$t('i18n_4b5e6872ea'),
          dataIndex: 'residentSetSize',
          width: '100px',
          ellipsis: true,
          sizeTooltip: true
        },
        {
          title: this.$t('i18n_ee8ecb9ee0'),
          dataIndex: 'priority',
          width: '80px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_86cd8dcead'),
          dataIndex: 'startTime',
          width: '180px',
          ellipsis: true,
          timeTooltip: true
        },
        {
          title: this.$t('i18n_9f70e40e04'),
          dataIndex: 'upTime',
          width: '100px',
          ellipsis: true,
          durationTooltip: true
        },
        {
          title: this.$t('i18n_a1e24fe1f6'),
          dataIndex: 'userTime',
          width: '100px',
          ellipsis: true,
          durationTooltip: true
        },
        {
          title: this.$t('i18n_4f35e80da6'),
          dataIndex: 'path',
          width: '180px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_c99a2f7ed8'),
          dataIndex: 'commandLine',
          width: '180px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',

          align: 'center',
          width: '80px',
          fixed: 'right'
        }
      ],

      diskColumns: [
        {
          title: 'uuid',
          dataIndex: 'uuid',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_7088e18ac9'),
          dataIndex: 'mount',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_73a87230e0'),
          dataIndex: 'type',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_3bdd08adab'),
          dataIndex: 'description',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_48735a5187'),
          dataIndex: 'freeSpace',
          ellipsis: true,
          sizeTooltip: true
        },
        {
          title: this.$t('i18n_d88651584f'),
          dataIndex: 'usableSpace',
          ellipsis: true,
          sizeTooltip: true
        },
        {
          title: this.$t('i18n_627c952b5e'),
          dataIndex: 'totalSpace',
          ellipsis: true,
          sizeTooltip: true
        },
        {
          title: this.$t('i18n_ba52103711'),
          dataIndex: 'freeInodes',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_7293bbb0ff'),
          dataIndex: 'totalInodes',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_ea15ae2b7f'),
          dataIndex: 'options',
          ellipsis: true,
          tooltip: true
        }
      ],

      hwDiskPartitionColumns: [
        {
          title: this.$t('i18n_7b36b18865'),
          dataIndex: 'identification',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_42f766b273'),
          dataIndex: 'mountPoint',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_226b091218'),
          dataIndex: 'type',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_58f9666705'),
          dataIndex: 'size',
          ellipsis: true,
          sizeTooltip: true
        },
        {
          title: this.$t('i18n_5d14e91b01'),
          dataIndex: 'major',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_e9c2cb1326'),
          dataIndex: 'minor',
          ellipsis: true,
          tooltip: true
        },
        {
          title: 'uuid',
          dataIndex: 'uuid',
          ellipsis: true,
          tooltip: true
        }
      ],

      refreshInterval: 5,
      historyChart: null,
      netHistoryChart: null,
      networkDelayChart: null,
      countdownTime: Date.now(),
      machineInfo: null,
      networkInterfaces: [],
      nodeMonitorLoadStatus: 0,
      monitorConfig: {}
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getThemeView']),
    idInfo() {
      return {
        nodeId: this.nodeId,
        machineId: this.machineId
      }
    }
  },
  watch: {
    refreshInterval: {
      deep: true,
      handler() {
        this.cacheNodeProcess()
      }
    }
  },
  mounted() {
    this.processNames = Object.assign([], this.defaultProcessNames)
    this.initData()
    window.addEventListener('resize', this.resize)
  },
  unmounted() {
    window.removeEventListener('resize', this.resize)
  },
  methods: {
    formatPercent,
    parseTime,
    formatDuration,
    renderSize,
    formatPercent2Number,
    renderBpsSize,
    getMachineInfo() {
      machineInfo({ ...this.idInfo }).then((res) => {
        //
        if (res.data) {
          this.machineInfo = res.data.data
          if (this.machineInfo) {
            let ipListStr = (this.machineInfo && this.machineInfo.hostIpv4s) || ''
            this.machineInfo = {
              ...this.machineInfo,
              ipv4List: ipListStr.length ? ipListStr.split(',') : ''
            }
          }
          this.refreshInterval = res.data.heartSecond
        }
      })
    },
    addNodeProcess(v) {
      this.processNames = v
      this.cacheNodeProcess()
    },
    restProcessNames() {
      this.processNames = this.defaultProcessNames
      this.processSearch = {
        processName: this.defaultProcessNames[0],
        processCount: 20
      }
      this.cacheNodeProcess()
      this.loadNodeProcess()
    },
    // 初始化页面
    initData() {
      const nodeCache = this.getCacheNodeProcess()

      this.processSearch = {
        ...this.processSearch,
        processName: nodeCache?.processName || this.processSearch.processName,
        processCount: nodeCache?.processCount || this.processSearch.processCount
      }
      this.processNames = nodeCache?.processNames || this.processNames
      // 加载缓存信息
      //this.refreshInterval = this.getCacheNode("refreshInterval", this.refreshInterval);
      //
      this.pullNodeData()
      // 监控配置
      machineMonitorConfig({
        id: this.machineId
      }).then((res) => {
        this.monitorConfig = res.data || {}
      })
    },
    pullNodeData() {
      this.loadNodeTop()
      this.loadNodeProcess()
      this.getMachineInfo()
      this.getMachineDiskInfo()
      this.getMachineNetworkInterfaces()
      this.getMachineHwDiskInfo()
      // 重新计算倒计时
      this.countdownTime = Date.now() + this.refreshInterval * 1000
    },
    changeTabs(activeKey) {
      if (activeKey === 'stat') {
        this.loadNodeTop()
      }
    },
    resize() {
      this.historyChart?.resize()
      this.netHistoryChart?.resize()
      this.networkDelayChart?.resize()
    },
    // 请求 top 命令绘制图表
    loadNodeTop() {
      nodeMonitorData({ ...this.idInfo }, false)
        .then((res) => {
          if (res.code === 200) {
            if (res.data && res.data.length) {
              this.nodeMonitorLoadStatus = 1
              this.handleChartData(res.data)
              return
            }
          }
          this.nodeMonitorLoadStatus = -1
        })
        .catch(() => {
          this.nodeMonitorLoadStatus = -1
        })
    },
    handleChartData(data) {
      this.$nextTick(() => {
        this.historyChart = drawChart(data, 'top-chart', generateNodeTopChart, this.getThemeView())
        this.netHistoryChart = drawChart(data, 'net-chart', generateNodeNetChart, this.getThemeView())
        this.networkDelayChart = drawChart(
          data,
          'network-delay-chart',
          generateNodeNetworkTimeChart,
          this.getThemeView()
        )
      })
    },
    // 加载节点进程列表
    loadNodeProcess(v) {
      this.loading = this.processList.length <= 0
      getProcessList({
        ...this.idInfo,
        processName: this.processSearch.processName,
        count: this.processSearch.processCount
      }).then((res) => {
        if (res.code === 200) {
          this.processList = res.data
        } else {
          this.processList = []
        }

        this.loading = false
      })
      if (v) {
        this.cacheNodeProcess()
      }
    },
    // kill pid
    kill(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_1eb378860a'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return killPid({
            ...this.idInfo,
            pid: record.processId
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadNodeProcess()
            }
          })
        }
      })
    },
    // 历史图表
    handleHistory(type) {
      this.monitorVisible = {
        visible: true,
        type: type
      }
    },
    cacheNodeProcess() {
      const cacheJson = this.getCacheAllNode()
      // console.log(cacheJson);
      const cacheId = this.nodeId || this.machineId
      cacheJson[cacheId] = cacheJson[cacheId] || {}
      cacheJson[cacheId].processNames = this.processNames
      cacheJson[cacheId].processName = this.processSearch.processName
      cacheJson[cacheId].processCount = this.processSearch.processCount
      //cacheJson["refreshInterval"] = this.refreshInterval;
      localStorage.setItem('node-process-name', JSON.stringify(cacheJson))
    },
    getCacheNodeProcess() {
      const cacheId = this.nodeId || this.machineId
      return this.getCacheNode(cacheId, {})
    },

    getCacheAllNode() {
      const str = localStorage.getItem('node-process-name') || ''
      let cacheJson
      try {
        cacheJson = JSON.parse(str)
      } catch (e) {
        cacheJson = {}
      }
      return cacheJson
    },

    getCacheNode(key, defaultValue) {
      const cacheJson = this.getCacheAllNode()

      return cacheJson[key] || defaultValue
    },
    getMachineDiskInfo() {
      this.diskLoading = !this.diskList || this.diskList.length <= 0
      machineDiskInfo({ ...this.idInfo }).then((res) => {
        this.diskList = res.data
        if (this.diskList) {
          this.diskLoading = false
        }
      })
    },
    getMachineNetworkInterfaces() {
      machineNetworkInterfaces({ ...this.idInfo }).then((res) => {
        this.networkInterfaces = (res.data || []).sort((item1, item2) => {
          const item1All = item1.bytesRecv || 0 + item1.bytesSecv || 0
          const item2All = item2.bytesRecv || 0 + item2.bytesSecv || 0
          return item2All - item1All
        })
      })
    },
    getMachineHwDiskInfo() {
      machineHwDiskInfo({ ...this.idInfo }).then((res) => {
        this.hwDiskList = res.data || []
      })
    }
  }
}
</script>
<style scoped>
.chart {
  height: 35vh;
}
.search-input-item {
  width: 200px !important;
  margin-right: 10px;
}
#history-chart {
  height: 60vh;
}
:deep(.ant-statistic div) {
  /* display: inline-block; */
}
:deep(.ant-statistic-content-value, .ant-statistic-content) {
  font-size: 12px;
}

:deep(.ant-page-header),
:deep(.ant-page-header-content) {
  padding: 0;
}
:deep(.ant-page-header-heading-left) {
  margin: 0;
}
</style>
