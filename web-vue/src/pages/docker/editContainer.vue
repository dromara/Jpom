<template>
  <div>
    <!-- stats -->
    <a-row>
      <a-col :span="12">
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="CPUS">
            {{
              (statsData.cpuStats && statsData.cpuStats.percpuUsage) ||
              (statsData.cpuStats && statsData.cpuStats.onlineCpus)
            }}
          </a-descriptions-item>
          <a-descriptions-item label="CPU %">
            {{
              (
                ((((statsData.cpuStats && statsData.cpuStats.cpuUsage && statsData.cpuStats.cpuUsage.totalUsage) || 0) -
                  ((statsData.precpuStats &&
                    statsData.precpuStats.cpuUsage &&
                    statsData.precpuStats.cpuUsage.totalUsage) ||
                    0)) /
                  ((statsData.cpuStats && statsData.cpuStats.systemCpuUsage) || 0) -
                  ((statsData.precpuStats && statsData.precpuStats.systemCpuUsage) || 0)) *
                100.0
              ).toFixed(4)
            }}
            %
          </a-descriptions-item>
          <a-descriptions-item label="MEM USAGE">
            {{
              renderSize(
                ((statsData.memoryStats && statsData.memoryStats.usage) || 0) -
                  ((statsData.memoryStats && statsData.memoryStats.stats && statsData.memoryStats.stats.cache) || 0)
              )
            }}
          </a-descriptions-item>
          <a-descriptions-item label="MEM LIMIT">
            {{ renderSize((statsData.memoryStats && statsData.memoryStats.limit) || 0) }}
          </a-descriptions-item>
          <!-- memoryRatio -->
          <a-descriptions-item label="MEM %">
            {{
              (
                ((((statsData.memoryStats && statsData.memoryStats.usage) || 0) -
                  ((statsData.memoryStats && statsData.memoryStats.stats && statsData.memoryStats.stats.cache) || 0)) /
                  (statsData.memoryStats && statsData.memoryStats.limit)) *
                100.0
              ).toFixed(4)
            }}
            %
          </a-descriptions-item>
          <!-- // rx_bytes 网卡接收流量 -->
          <!-- // tx_bytes 网卡输出流量 -->
          <a-descriptions-item label="NET I/O rx">
            <div v-for="(item, index) in Object.keys(statsData.networks || {})" :key="index">
              <a-tooltip :title="`${item} ${$tl('p.receive')}`">
                {{ renderSize(statsData.networks[item] && statsData.networks[item].rxBytes) || 0 }}
              </a-tooltip>
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="NET I/O tx">
            <div v-for="(item, index) in Object.keys(statsData.networks || {})" :key="index">
              <a-tooltip :title="`${item} ${$tl('p.transmit')}`">
                {{ renderSize(statsData.networks[item] && statsData.networks[item].txBytes) || 0 }}
              </a-tooltip>
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="BLOCK I/O">
            <a-tooltip
              :title="`${(statsData.blkioStats && statsData.blkioStats.ioServiceBytesRecursive && statsData.blkioStats.ioServiceBytesRecursive[0] && statsData.blkioStats.ioServiceBytesRecursive[0].op) || 'blkioStats'}`"
            >
              {{
                renderSize(
                  statsData.blkioStats &&
                    statsData.blkioStats.ioServiceBytesRecursive &&
                    statsData.blkioStats.ioServiceBytesRecursive[0] &&
                    statsData.blkioStats.ioServiceBytesRecursive[0].value
                ) || 0
              }}
            </a-tooltip>
            /
            <a-tooltip
              :title="`${(statsData.blkioStats && statsData.blkioStats.ioServiceBytesRecursive && statsData.blkioStats.ioServiceBytesRecursive[1] && statsData.blkioStats.ioServiceBytesRecursive[1].op) || 'blkioStats'}`"
            >
              {{
                renderSize(
                  statsData.blkioStats &&
                    statsData.blkioStats.ioServiceBytesRecursive &&
                    statsData.blkioStats.ioServiceBytesRecursive[1] &&
                    statsData.blkioStats.ioServiceBytesRecursive[1].value
                ) || 0
              }}
            </a-tooltip>
          </a-descriptions-item>

          <!-- // 进程或线程的数量 -->
          <a-descriptions-item label="PIDS">
            {{ statsData.pidsStats && statsData.pidsStats.current }}
          </a-descriptions-item>
        </a-descriptions></a-col
      >
      <a-col :span="12">
        <a-form ref="editForm" :model="temp" :label-col="{ span: 7 }" :wrapper-col="{ span: 17 }">
          <a-form-item name="blkioWeight">
            <template #label>
              Block IO {{ $tl('c.weight') }}
              <a-tooltip>
                <template #title> Block IO {{ $tl('p.weight') }} </template>
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
            <a-input-number
              v-model:value="temp.blkioWeight"
              style="width: 100%"
              :placeholder="$tl('p.blockIOWeight')"
              :min="0"
              :max="1000"
            />
          </a-form-item>
          <a-form-item name="cpuShares">
            <template #label>
              CPU {{ $tl('c.weight') }}
              <a-tooltip>
                <template #title> {{ $tl('c.cpuPeriod') }} </template>
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
            <a-input-number v-model:value="temp.cpuShares" style="width: 100%" :placeholder="$tl('c.cpuPeriod')" />
          </a-form-item>
          <a-form-item name="cpusetCpus">
            <template #label>
              {{ $tl('p.cpuSetCPUs') }}
              <a-tooltip>
                <template #title> {{ $tl('p.cpuSetCPUsString') }},1）。 </template>
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="temp.cpusetCpus"
              style="width: 100%"
              placeholder="允许执行的 CPU（例如，0-3、0,1）。"
            />
          </a-form-item>
          <a-form-item name="cpusetMems">
            <template #label>
              CpusetMems
              <a-tooltip>
                <template #title> {{ $tl('p.cpuSetMEMs') }} (MEM) (0-3, 0,1)。 仅在 NUMA 系统上有效。 </template>
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
            <a-input v-model:value="temp.cpusetMems" style="width: 100%" :placeholder="$tl('p.cpuSetMEMsString')" />
          </a-form-item>
          <a-form-item name="cpuPeriod">
            <template #label>
              CPU {{ $tl('p.period') }}
              <a-tooltip>
                <template #title> CPU {{ $tl('p.periodUsec') }} </template>
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
            <a-input-number
              v-model:value="temp.cpuPeriod"
              style="width: 100%"
              placeholder=" {{$tl('p.cpuPeriodUsec')}}"
            />
          </a-form-item>
          <a-form-item name="cpuQuota">
            <template #label>
              CPU {{ $tl('p.quota') }}
              <a-tooltip>
                <template #title> {{ $tl('c.cpuQuota') }} </template>
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
            <a-input-number v-model:value="temp.cpuQuota" style="width: 100%" :placeholder="$tl('c.cpuQuota')" />
          </a-form-item>

          <a-form-item name="memory">
            <template #label>
              {{ $tl('p.memoryLimit') }}
              <a-tooltip>
                <template #title> {{ $tl('c.memory') }} </template>
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
            <a-input v-model:value="temp.memory" style="width: 100%" :placeholder="$tl('c.memory')" />
          </a-form-item>
          <a-form-item name="memorySwap">
            <template #label>
              {{ $tl('p.memoryTotal') }}
              <a-tooltip>
                <template #title> {{ $tl('c.memorySwap') }} </template>
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
            <a-input v-model:value="temp.memorySwap" style="width: 100%" :placeholder="$tl('c.memorySwap')" />
          </a-form-item>
          <a-form-item name="memoryReservation">
            <template #label>
              {{ $tl('p.memorySoftLimit') }}
              <a-tooltip>
                <template #title> {{ $tl('c.memorySoftLimit') }} </template>
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
            <a-input
              v-model:value="temp.memoryReservation"
              style="width: 100%"
              :placeholder="$tl('c.memorySoftLimit')"
            />
          </a-form-item>
        </a-form>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { dockerContainerStats, dockerInspectContainer, dockerUpdateContainer } from '@/api/docker-api'
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
    },
    containerId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      statsData: {},
      temp: {}
    }
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId
    }
  },
  mounted() {
    this.editContainer()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.docker.editContainer.${key}`, ...args)
    },
    renderSize,
    // 编辑容器
    editContainer() {
      dockerContainerStats(this.urlPrefix, {
        id: this.reqDataId,
        containerId: this.containerId
      }).then((res2) => {
        if (res2.code === 200) {
          this.statsData = (res2.data && res2.data[this.containerId]) || {}
          dockerInspectContainer(this.urlPrefix, {
            id: this.reqDataId,
            containerId: this.containerId
          }).then((res) => {
            if (res.code === 200) {
              //this.editVisible = true

              const hostConfig = res.data.hostConfig || {}
              const data = {
                containerId: this.containerId,
                cpusetCpus: hostConfig.cpusetCpus,
                cpusetMems: hostConfig.cpusetMems,
                cpuPeriod: hostConfig.cpuPeriod,
                cpuShares: hostConfig.cpuShares,
                cpuQuota: hostConfig.cpuQuota,
                blkioWeight: hostConfig.blkioWeight,
                memoryReservation: renderSize(hostConfig.memoryReservation, hostConfig.memoryReservation),
                // Deprecated: This field is deprecated as the kernel 5.4 deprecated kmem.limit_in_bytes.
                // kernelMemory: hostConfig.kernelMemory,
                memory: renderSize(hostConfig.memory, hostConfig.memory),
                memorySwap: renderSize(hostConfig.memorySwap, hostConfig.memorySwap)
              }

              this.temp = Object.assign({}, data)
            }
          })
        }
        // console.log(res);
      })
    },
    handleEditOk() {
      return new Promise((ok, reject) => {
        this.$refs['editForm']
          .validate()
          .then(() => {
            const temp = Object.assign({}, this.temp, { id: this.reqDataId })
            dockerUpdateContainer(this.urlPrefix, temp)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  //this.editVisible = false
                  ok()
                } else {
                  reject()
                }
              })
              .catch(() => {
                reject()
              })
          })
          .catch(() => {
            reject()
          })
      })
    }
  }
}
</script>
