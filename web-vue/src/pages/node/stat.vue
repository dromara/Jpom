<template>
  <div class="full-content">
    <div>
      <a-card>
        <template slot="title">
          <a-row>
            <a-space>
              <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="节点名称" />

              <a-select show-search option-filter-prop="children" v-model="listQuery.group" allowClear placeholder="分组" class="search-input-item">
                <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
              </a-select>

              <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
                <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
              </a-tooltip>
              <a-tooltip placement="bottom">
                <template slot="title">
                  <div>
                    <ul>
                      <li>监控数据目前采用原生命令获取,和真实情况有一定差异可以当做参考依据</li>
                      <li>监控频率可以到服务端配置文件中修改</li>
                      <li>悬停到仪表盘上显示具体含义</li>
                      <li>点击仪表盘查看监控历史数据</li>
                      <li>点击延迟可以查看对应节点网络延迟历史数据</li>
                      <li>只有 linux 系统才有内存Used 值</li>
                      <li>为了避免部分节点不能及时响应造成监控阻塞,节点统计超时时间不受节点超时配置影响将采用默认超时时间(5秒)</li>
                    </ul>
                  </div>
                </template>
                <a-icon type="question-circle" theme="filled" />
              </a-tooltip>
              <a-col :span="3"> <a-statistic-countdown format="s 秒" title="" :value="deadline" @finish="onFinish" /> </a-col>
            </a-space>
          </a-row>
        </template>

        <a-row :gutter="[16, 16]">
          <template v-if="list && list.length">
            <a-col v-for="item in list" :key="item.id" :span="6">
              <template>
                <a-card :headStyle="{ padding: '0 6px' }" :bodyStyle="{ padding: '10px' }">
                  <template slot="title">
                    <a-row :gutter="[4, 0]">
                      <a-col :span="17" style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                        <a-tooltip>
                          <template slot="title">
                            <div>节点名称：{{ item.name }}</div>
                            <div>节点地址：{{ item.url }}</div>
                          </template>
                          {{ item.name }}
                        </a-tooltip>
                      </a-col>
                      <a-col :span="7" style="text-align: right">
                        <a-tooltip>
                          <template slot="title">
                            <div>当前状态：{{ statusMap[item.machineNodeData && item.machineNodeData.status] }}</div>
                            <div>状态描述：{{ (item.machineNodeData && item.machineNodeData.statusMsg) || "" }}</div>
                          </template>
                          <a-tag :color="item.machineNodeData && item.machineNodeData.status === 1 ? 'green' : 'pink'" style="margin-right: 0px">
                            {{ statusMap[item.machineNodeData && item.machineNodeData.status] }}
                          </a-tag>
                        </a-tooltip>
                      </a-col>
                    </a-row>
                  </template>

                  <a-row :gutter="[8, 8]">
                    <a-col :span="8">
                      <a-tooltip @click="handleHistory(item, 'nodeTop')" :title="`CPU 占用率：${item.occupyCpu}%`">
                        <a-progress
                          type="circle"
                          :width="80"
                          :stroke-color="{
                            '0%': '#87d068',
                            '30%': '#87d068',
                            '100%': '#108ee9',
                          }"
                          size="small"
                          status="active"
                          :percent="item.occupyCpu"
                        />
                      </a-tooltip>
                    </a-col>
                    <a-col :span="8">
                      <a-tooltip @click="handleHistory(item, 'nodeTop')" :title="`硬盘占用率：${item.occupyDisk}%`">
                        <a-progress
                          type="circle"
                          :width="80"
                          :stroke-color="{
                            '0%': '#87d068',
                            '30%': '#87d068',
                            '100%': '#108ee9',
                          }"
                          size="small"
                          status="active"
                          :percent="item.occupyDisk"
                        />
                      </a-tooltip>
                    </a-col>
                    <a-col :span="8">
                      <a-tooltip @click="handleHistory(item, 'nodeTop')" :title="`内存占用率：${item.occupyMemory}%`">
                        <a-progress
                          :width="80"
                          type="circle"
                          :stroke-color="{
                            '0%': '#87d068',
                            '30%': '#87d068',
                            '100%': '#108ee9',
                          }"
                          size="small"
                          status="active"
                          :percent="item.occupyMemory"
                        />
                      </a-tooltip>
                    </a-col>
                  </a-row>

                  <a-row :gutter="[8, 8]" style="text-align: center">
                    <a-col :span="8">
                      <a-tooltip
                        @click="handleHistory(item, 'networkDelay')"
                        :title="`${'延迟' + (formatDuration(item.machineNodeData && item.machineNodeData.networkDelay, '', 2) || '-') + ' 点击查看历史趋势'}`"
                      >
                        <a-statistic
                          title="延迟"
                          :value="item.machineNodeData && item.machineNodeData.networkDelay"
                          valueStyle="font-size: 14px;overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
                          :formatter="
                            (v) => {
                              return formatDuration(item.machineNodeData && item.machineNodeData.networkDelay, '', 2) || '-';
                            }
                          "
                        />
                      </a-tooltip>
                    </a-col>
                    <a-col :span="8">
                      <a-tooltip :title="formatDuration(item.machineNodeData && item.machineNodeData.jpomUptime, '', 1) || '-'">
                        <a-statistic
                          title="运行时间"
                          valueStyle="font-size: 14px;overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
                          :formatter="
                            (v) => {
                              return formatDuration(item.machineNodeData && item.machineNodeData.jpomUptime, '', 2) || '-';
                            }
                          "
                        />
                      </a-tooltip>
                    </a-col>
                    <a-col :span="8">
                      <a-tooltip :title="`${parseTime(item.machineNodeData && item.machineNodeData.modifyTimeMillis)}`">
                        <a-statistic
                          title="更新时间"
                          valueStyle="font-size: 14px;overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
                          :formatter="
                            (v) => {
                              return parseTime(item.machineNodeData && item.machineNodeData.modifyTimeMillis, '{h}:{i}:{s}');
                            }
                          "
                        />
                      </a-tooltip>
                    </a-col>
                  </a-row>
                </a-card>
              </template>
            </a-col>
          </template>
          <a-col v-else :span="24">
            <a-empty description="没有任何节点" />
          </a-col>
          <!-- v-if="this.listQuery.total / this.listQuery.limit > 1" -->
        </a-row>
        <a-row>
          <a-col>
            <a-pagination
              v-model="listQuery.page"
              v-if="listQuery.total / listQuery.limit > 1"
              :showTotal="
                (total) => {
                  return PAGE_DEFAULT_SHOW_TOTAL(total, listQuery);
                }
              "
              :showSizeChanger="true"
              :pageSizeOptions="sizeOptions"
              :pageSize="listQuery.limit"
              :total="listQuery.total"
              @showSizeChange="
                (current, size) => {
                  this.listQuery.limit = size;
                  this.loadData();
                }
              "
              @change="this.loadData"
              show-less-items
            />
          </a-col>
        </a-row>
      </a-card>
    </div>
    <!-- 历史监控 -->
    <a-modal destroyOnClose v-model="monitorVisible" width="75%" :title="`${this.temp.name}历史监控图表`" :footer="null" :maskClosable="false">
      <node-top v-if="monitorVisible" :type="this.temp.type" :nodeId="this.temp.id"></node-top>
    </a-modal>
  </div>
</template>
<script>
// import { getStatist, status, statusStat } from "@/api/node-stat";
import {} from "@/api/node";
import { PAGE_DEFAULT_LIST_QUERY, PAGE_DEFAULT_SHOW_TOTAL, formatDuration, parseTime, formatPercent2Number } from "@/utils/const";
import NodeTop from "@/pages/node/node-layout/node-top";
import { getNodeGroupAll, getNodeList } from "@/api/node";
import { statusMap } from "@/api/system/assets-machine";

export default {
  components: { NodeTop },
  data() {
    return {
      loading: false,
      statusMap,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY, {
        limit: 8,
      }),
      sizeOptions: ["8", "12", "16", "20", "24"],
      list: [],

      monitorVisible: false,
      deadline: 0,
      temp: {},
      groupList: [],
      refreshInterval: 5,
    };
  },
  computed: {},
  watch: {},
  created() {
    this.loadData();
    this.loadGroupList();
  },
  destroyed() {},
  methods: {
    PAGE_DEFAULT_SHOW_TOTAL,
    parseTime,
    formatDuration,
    // 加载数据
    loadData(pointerEvent) {
      //this.list = [];
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getNodeList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list =
            res.data.result &&
            res.data.result.map((item) => {
              // console.log(item);
              item.occupyCpu = formatPercent2Number(item.machineNodeData?.osOccupyCpu);

              item.occupyDisk = formatPercent2Number(item.machineNodeData?.osOccupyDisk);
              item.occupyMemory = formatPercent2Number(item.machineNodeData?.osOccupyMemory);
              return item;
            });
          this.listQuery.total = res.data.total;
          this.refreshInterval = 30;
          this.deadline = Date.now() + this.refreshInterval * 1000;
        }
        this.loading = false;
      });
    },

    onFinish() {
      if (this.$attrs.routerUrl !== this.$route.path) {
        // 重新计算倒计时
        this.deadline = Date.now() + this.refreshInterval * 1000;
        return;
      }
      this.loadData();
    },

    // 历史图表
    handleHistory(record, type) {
      this.monitorVisible = true;
      this.temp = record;
      this.temp = { ...this.temp, type };
    },
    // 获取所有的分组
    loadGroupList() {
      getNodeGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data;
        }
      });
    },
  },
};
</script>
<style scoped>
/* /deep/ .ant-statistic div {
  display: inline-block;
}

/deep/ .ant-statistic-content-value,
/deep/ .ant-statistic-content {
  font-size: 16px;
} */
</style>
