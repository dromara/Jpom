<template>
  <div class="full-content">
    <a-tabs default-active-key="1">
      <a-tab-pane key="1" tab="缓存信息">
        <a-descriptions bordered title="" layout="vertical" size="middle">
          <a-descriptions-item label="提示" :span="4">
            <div style="color: red; font-weight: bold; font-size: 16px">
              <p>请勿手动删除数据目录下面文件 !!!!</p>
              <p>如果需要删除需要提前备份或者已经确定对应文件弃用后才能删除 !!!!</p>
            </div>
            <a-tag color="orange">集群ID:{{ temp.clusterId }}</a-tag>
            <a-tag color="blue">安装ID:{{ temp.installId }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="服务端时间" :span="1">
            {{ temp.dateTime }} <a-tag>{{ temp.timeZoneId }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="数据目录占用空间" :span="1">
            {{ renderSize(temp.dataSize) }} (10分钟刷新一次)
            <a-tooltip>
              <template slot="title">
                <ul>
                  <li>数据目录是指程序在运行过程中产生的文件以及数据存储目录</li>
                  <li>数据目录大小包含：临时文件、在线构建文件、数据库文件等</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </a-descriptions-item>
          <a-descriptions-item label="临时文件占用空间" :span="1">
            <a-space>
              <span>{{ renderSize(temp.cacheFileSize) }} (10分钟刷新一次)</span>
              <a-button size="small" type="primary" v-if="temp.cacheFileSize !== '0'" class="btn" @click="clear('serviceCacheFileSize')">清空</a-button>
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item label="在线构建文件占用空间">
            {{ renderSize(temp.cacheBuildFileSize) }} (10分钟刷新一次)
            <a-tooltip>
              <template slot="title">
                <ul>
                  <li>在线构建文件主要保存，仓库文件，构建历史产物等。不支持主动清除，如果文件占用过大可以配置保留规则和对单个构建配置是否保存仓库、产物文件等</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </a-descriptions-item>
          <a-descriptions-item label="旧版程序包占有空间">
            <a-space>
              <span>{{ renderSize(temp.oldJarsSize) }}</span>
              <a-button size="small" v-if="temp.oldJarsSize !== '0'" type="primary" class="btn" @click="clear('serviceOldJarsSize')">清空</a-button>
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item label="黑名单 IP">
            <a-space>
              <a-popover title="黑名单 IP">
                <template slot="content"
                  ><a-list size="small" bordered :data-source="temp.errorIp">
                    <a-list-item slot="renderItem" slot-scope="item">
                      {{ item.key }} <a-tag>{{ item.obj }}次</a-tag> <a-tag>过期时间：{{ formatDuration(item.ttl, "") }}</a-tag>
                    </a-list-item>
                  </a-list>
                </template>
                {{ (temp.errorIp && temp.errorIp.length) || 0 }}
                <a-icon type="unordered-list" />
              </a-popover>
              <a-button size="small" type="primary" v-if="temp.errorIp && temp.errorIp.length" class="btn" @click="clear('serviceIpSize')">清空</a-button>
            </a-space>
          </a-descriptions-item>
          <a-descriptions-item label="在读取的日志文件数">
            {{ temp.readFileOnLineCount }}
          </a-descriptions-item>
          <a-descriptions-item label="插件数"> {{ temp.pluginSize || 0 }} </a-descriptions-item>
          <a-descriptions-item label="分片操作数"> {{ temp.shardingSize }} </a-descriptions-item>
          <a-descriptions-item label="正在构建数">
            <a-popover title="正在构建">
              <template slot="content">
                <p v-for="item in temp.buildKeys || []" :key="item">{{ item }}</p>
              </template>
              <a-space>
                <span>{{ (temp.buildKeys || []).length }}</span>
                <a-icon type="unordered-list" />
              </a-space>
            </a-popover>
          </a-descriptions-item>
          <a-descriptions-item label="线程同步器">
            <a-popover title="正在运行的线程同步器">
              <template slot="content">
                <p v-for="item in temp.syncFinisKeys || []" :key="item">{{ item }}</p>
              </template>
              <a-space>
                <span>{{ (temp.syncFinisKeys || []).length }}</span>
                <a-icon type="unordered-list" />
              </a-space>
            </a-popover>
          </a-descriptions-item>
          <a-descriptions-item label="错误的工作空间数据">
            <a-popover title="错误的工作空间数据">
              <template slot="content">
                <a-collapse>
                  <a-collapse-panel :header="key" v-for="(item, key) in temp.errorWorkspace" :key="key">
                    <p v-for="(item2, index) in item" :key="index">{{ item2 }}</p>
                    <a-icon
                      slot="extra"
                      type="delete"
                      @click="
                        (e) => {
                          handleClearErrorWorkspaceClick(e, key);
                        }
                      "
                    />
                  </a-collapse-panel>
                </a-collapse>
              </template>
              <a-space>
                <span>{{ Object.keys(temp.errorWorkspace || {}).length }}</span>
                <a-icon type="unordered-list" />
              </a-space>
            </a-popover>
          </a-descriptions-item>
        </a-descriptions>
        <!-- <a-timeline>
          <a-timeline-item> </a-timeline-item>
          <a-timeline-item> </a-timeline-item>
        </a-timeline> -->
      </a-tab-pane>
      <a-tab-pane key="2" tab="运行中的定时任务" force-render> <task-stat :taskList="taskList" @refresh="loadData" /></a-tab-pane>
    </a-tabs>
  </div>
</template>
<script>
import { getServerCache, clearCache, clearErrorWorkspace } from "@/api/system";
import TaskStat from "@/pages/system/taskStat";
import { renderSize, formatDuration } from "@/utils/const";
export default {
  components: {
    TaskStat,
  },
  data() {
    return {
      temp: {},
      taskList: [],
    };
  },
  mounted() {
    this.loadData();
    // console.log(Comparator);
  },
  methods: {
    renderSize,
    formatDuration,
    // load data
    loadData() {
      getServerCache().then((res) => {
        if (res.code === 200) {
          this.temp = res.data;
          this.taskList = res.data?.taskList;
        }
      });
    },
    // clear
    clear(type) {
      const params = {
        type: type,
        nodeId: "",
      };
      clearCache(params).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
          this.loadData();
        }
      });
    },
    handleClearErrorWorkspaceClick(event, tableName) {
      // If you don't want click extra trigger collapse, you can prevent this:
      event.stopPropagation();
      this.$confirm({
        title: "系统提示",
        content: "真的要删除" + tableName + "表中的错误数据吗？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          clearErrorWorkspace({ tableName }).then((res) => {
            if (res.code === 200) {
              // 成功
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
  },
};
</script>
