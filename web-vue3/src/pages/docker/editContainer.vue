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
            <div :key="index" v-for="(item, index) in Object.keys(statsData.networks || {})">
              <a-tooltip :title="`${item} 接收流量`">
                {{ renderSize(statsData.networks[item] && statsData.networks[item].rxBytes) || 0 }}
              </a-tooltip>
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="NET I/O tx">
            <div :key="index" v-for="(item, index) in Object.keys(statsData.networks || {})">
              <a-tooltip :title="`${item} 输出流量`">
                {{ renderSize(statsData.networks[item] && statsData.networks[item].txBytes) || 0 }}
              </a-tooltip>
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="BLOCK I/O">
            <a-tooltip
              :title="`${
                (statsData.blkioStats &&
                  statsData.blkioStats.ioServiceBytesRecursive &&
                  statsData.blkioStats.ioServiceBytesRecursive[0] &&
                  statsData.blkioStats.ioServiceBytesRecursive[0].op) ||
                'blkioStats'
              }`"
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
              :title="`${
                (statsData.blkioStats &&
                  statsData.blkioStats.ioServiceBytesRecursive &&
                  statsData.blkioStats.ioServiceBytesRecursive[1] &&
                  statsData.blkioStats.ioServiceBytesRecursive[1].op) ||
                'blkioStats'
              }`"
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
          <a-form-item prop="blkioWeight">
            <template slot="label">
              Block IO 权重
              <a-tooltip>
                <template slot="title"> Block IO 权重（相对权重）。 </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input-number
              style="width: 100%"
              v-model="temp.blkioWeight"
              placeholder="Block IO 权重"
              :min="0"
              :max="1000"
            />
          </a-form-item>
          <a-form-item prop="cpuShares">
            <template slot="label">
              CPU 权重
              <a-tooltip>
                <template slot="title"> 一个整数值，表示此容器相对于其他容器的相对 CPU 权重。 </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input-number
              style="width: 100%"
              v-model="temp.cpuShares"
              placeholder="一个整数值，表示此容器相对于其他容器的相对 CPU 权重。"
            />
          </a-form-item>
          <a-form-item prop="cpusetCpus">
            <template slot="label">
              执行的 CPU
              <a-tooltip>
                <template slot="title"> 允许执行的 CPU（例如，0-3、0,1）。 </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input style="width: 100%" v-model="temp.cpusetCpus" placeholder="允许执行的 CPU（例如，0-3、0,1）。" />
          </a-form-item>
          <a-form-item prop="cpusetMems">
            <template slot="label">
              CpusetMems
              <a-tooltip>
                <template slot="title"> 允许执行的内存节点 (MEM) (0-3, 0,1)。 仅在 NUMA 系统上有效。 </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input
              style="width: 100%"
              v-model="temp.cpusetMems"
              placeholder="允许执行的内存节点 (MEM) (0-3, 0,1)。 仅在 NUMA 系统上有效。"
            />
          </a-form-item>
          <a-form-item prop="cpuPeriod">
            <template slot="label">
              CPU 周期
              <a-tooltip>
                <template slot="title"> CPU 周期的长度，以微秒为单位。 </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input-number
              style="width: 100%"
              v-model="temp.cpuPeriod"
              placeholder=" CPU 周期的长度，以微秒为单位。"
            />
          </a-form-item>
          <a-form-item prop="cpuQuota">
            <template slot="label">
              CPU 时间
              <a-tooltip>
                <template slot="title"> 容器在一个 CPU 周期内可以获得的 CPU 时间的微秒。 </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input-number
              style="width: 100%"
              v-model="temp.cpuQuota"
              placeholder="容器在一个 CPU 周期内可以获得的 CPU 时间的微秒。"
            />
          </a-form-item>

          <a-form-item prop="memory">
            <template slot="label">
              内存
              <a-tooltip>
                <template slot="title"> 设置内存限制。 </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input style="width: 100%" v-model="temp.memory" placeholder="设置内存限制。" />
          </a-form-item>
          <a-form-item prop="memorySwap">
            <template slot="label">
              总内存
              <a-tooltip>
                <template slot="title"> 总内存（内存 + 交换）。 设置为 -1 以禁用交换。 </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input
              style="width: 100%"
              v-model="temp.memorySwap"
              placeholder="总内存（内存 + 交换）。 设置为 -1 以禁用交换。"
            />
          </a-form-item>
          <a-form-item prop="memoryReservation">
            <template slot="label">
              软内存
              <a-tooltip>
                <template slot="title"> 软内存限制。 </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input style="width: 100%" v-model="temp.memoryReservation" placeholder="软内存限制。" />
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
      type: String
    },
    machineDockerId: {
      type: String,
      default: ''
    },
    containerId: {
      type: String
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
              this.editVisible = true

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
      this.$refs['editForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        const temp = Object.assign({}, this.temp, { id: this.reqDataId })
        dockerUpdateContainer(this.urlPrefix, temp).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg
            })
            this.editVisible = false
          }
        })
      })
    }
  }
}
</script>
