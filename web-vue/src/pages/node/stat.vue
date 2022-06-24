<template>
  <div class="full-content">
    <div>
      <a-card :bodyStyle="{ padding: '10px' }">
        <template slot="title">
          <a-row style="text-align: center">
            <a-col :span="3">
              <a-statistic title="节点总数" :value="nodeCount"> </a-statistic>
            </a-col>
            <a-col :span="3" v-for="(desc, key) in statusMap" :key="key">
              <a-statistic :title="desc" :value="statusStatMap[key]">
                <template #suffix>
                  <!-- <a-icon type="question-circle" /> -->
                </template>
              </a-statistic>
            </a-col>

            <a-col :span="3"> <a-statistic-countdown format="s 秒" title="刷新倒计时" :value="deadline" @finish="onFinish" /> </a-col>
          </a-row>
        </template>
        <a-space direction="vertical">
          <div ref="filter" class="filter">
            <a-space>
              <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="节点名称" />
              <a-input v-model="listQuery['%url%']" @pressEnter="loadData" placeholder="节点地址" />
              <a-select
                :getPopupContainer="
                  (triggerNode) => {
                    return triggerNode.parentNode || document.body;
                  }
                "
                v-model="listQuery.status"
                allowClear
                placeholder="请选择状态"
                class="search-input-item"
              >
                <a-select-option v-for="(desc, key) in statusMap" :key="key">{{ desc }}</a-select-option>
              </a-select>
              <a-select
                :getPopupContainer="
                  (triggerNode) => {
                    return triggerNode.parentNode || document.body;
                  }
                "
                show-search
                option-filter-prop="children"
                v-model="listQuery.group"
                allowClear
                placeholder="分组"
                class="search-input-item"
              >
                <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
              </a-select>
              <a-select
                :getPopupContainer="
                  (triggerNode) => {
                    return triggerNode.parentNode || document.body;
                  }
                "
                v-model="listQuery['order_field']"
                allowClear
                placeholder="请选择排序字段"
                class="search-input-item"
              >
                <a-select-option value="networkTime">网络延迟</a-select-option>
                <a-select-option value="occupyCpu">cpu</a-select-option>
                <a-select-option value="occupyDisk">硬盘</a-select-option>
                <a-select-option value="occupyMemoryUsed">内存Used</a-select-option>
                <a-select-option value="occupyMemory">内存</a-select-option>
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
            </a-space>
          </div>
        </a-space>
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
                        <a-tooltip :title="`当前状态：${statusMap[item.status]} ${item.status === 0 ? '' : '异常描述：' + item.failureMsg} `">
                          <a-tag :color="item.status === 0 ? 'green' : 'pink'" style="margin-right: 0px"> {{ statusMap[item.status] }}</a-tag>
                        </a-tooltip>
                      </a-col>
                    </a-row>
                  </template>

                  <a-row :gutter="[8, 8]">
                    <a-col :span="8">
                      <a-tooltip @click="item.status === 4 ? null : handleHistory(item, 'nodeTop')" :title="`CPU 占用率：${item.occupyCpu}%`">
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
                      <a-tooltip @click="item.status === 4 ? null : handleHistory(item, 'nodeTop')" :title="`硬盘占用率：${item.occupyDisk}%`">
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
                      <a-tooltip
                        @click="item.status === 4 ? null : handleHistory(item, 'nodeTop')"
                        :title="`内存占用率：${item.occupyMemoryUsed && item.occupyMemoryUsed !== -1 ? item.occupyMemoryUsed : item.occupyMemory}%`"
                      >
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
                          :percent="item.occupyMemoryUsed && item.occupyMemoryUsed !== -1 ? item.occupyMemoryUsed : item.occupyMemory"
                        />
                      </a-tooltip>
                    </a-col>
                  </a-row>

                  <a-row :gutter="[8, 8]" style="text-align: center">
                    <a-col :span="8">
                      <a-tooltip @click="item.status === 4 ? null : handleHistory(item, 'networkTime')" :title="`${item.status === 4 ? '-' : '延迟' + item.networkTime + 'ms 点击查看历史趋势'}`">
                        <a-statistic
                          title="延迟"
                          :value="item.networkTime"
                          valueStyle="font-size: 14px;overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
                          :formatter="
                            (v) => {
                              return item.networkTime === -1 ? '-' : item.networkTime + 'ms';
                            }
                          "
                        />
                      </a-tooltip>
                    </a-col>
                    <a-col :span="8">
                      <a-tooltip :title="formatDuration(item.upTimeStr) || '-'">
                        <a-statistic
                          title="运行时间"
                          valueStyle="font-size: 14px;overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
                          :formatter="
                            (v) => {
                              return formatDuration(item.upTimeStr, '', 1) || '-';
                            }
                          "
                        />
                      </a-tooltip>
                    </a-col>
                    <a-col :span="8">
                      <a-tooltip :title="`${item.status === 4 ? '-' : parseTime(item.modifyTimeMillis)}`">
                        <a-statistic
                          title="更新时间"
                          valueStyle="font-size: 14px;overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
                          :formatter="
                            (v) => {
                              return item.status === 4 ? '-' : parseTime(item.modifyTimeMillis, '{h}:{i}:{s}');
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
    <a-modal v-model="monitorVisible" width="75%" :title="`${this.temp.name}历史监控图表`" :footer="null" :maskClosable="false">
      <node-top v-if="monitorVisible" :type="this.temp.type" :nodeId="this.temp.id"></node-top>
    </a-modal>
  </div>
</template>
<script>
import {getStatist, status, statusStat} from "@/api/node-stat";
import {formatDuration, parseTime} from "@/utils/time";
import {PAGE_DEFAULT_LIST_QUERY, PAGE_DEFAULT_SHOW_TOTAL} from "@/utils/const";
import NodeTop from "@/pages/node/node-layout/node-top";
import {getNodeGroupAll} from "@/api/node";

export default {
  components: { NodeTop },
  data() {
    return {
      loading: false,
      statusMap: status,
      listQuery: Object.assign({ order: "descend", order_field: "networkTime" }, PAGE_DEFAULT_LIST_QUERY, {
        limit: 8,
      }),
      sizeOptions: ["8", "12", "16", "20", "24"],
      list: [],
      statusStatMap: {},
      // openStatusMap: {},
      nodeCount: 0,
      monitorVisible: false,
      deadline: 0,
      temp: {},
      groupList: [],
    };
  },
  computed: {},
  watch: {},
  created() {
    this.loadData();
    this.loadGroupList();
  },
  destroyed() {
    if (this.pullFastInstallResultTime) {
      clearInterval(this.pullFastInstallResultTime);
    }
  },
  methods: {
    PAGE_DEFAULT_SHOW_TOTAL,
    parseTime,
    formatDuration,
    // 加载数据
    loadData(pointerEvent) {
      //this.list = [];
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getStatist(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
      statusStat().then((res) => {
        if (res.data) {
          this.statusStatMap = res.data.status;
          let nodeCount2 = 0;
          // console.log(this.statusStatMap);
          Object.values(this.statusStatMap).forEach((element) => {
            nodeCount2 += element;
          });
          this.nodeCount = nodeCount2;
          this.deadline = Date.now() + res.data.heartSecond * 1000;
          //
          // this.openStatusMap = res.data.openStatus;
        }
      });
    },

    onFinish() {
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
<style scoped></style>
