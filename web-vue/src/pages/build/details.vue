<template>
  <div>
    <a-descriptions bordered>
      <a-descriptions-item label="构建名称"> {{ data.name }}</a-descriptions-item>
      <a-descriptions-item label="最新构建ID">
        <a-tag color="#108ee9">#{{ data.buildId }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="分组"> {{ data.group }} </a-descriptions-item>
      <a-descriptions-item label="分组/标签"> {{ data.branchName }} </a-descriptions-item>
      <a-descriptions-item label="构建方式">
        <template v-if="data.bubuildMode === 1">
          <a-icon type="cloud" />
          容器构建
        </template>
        <template v-else>
          <a-icon type="code" />
          本地构建
        </template>
      </a-descriptions-item>
      <a-descriptions-item label="产物目录"> {{ data.resultDirFile }} </a-descriptions-item>
      <a-descriptions-item label="构建状态">
        <a-tag :color="statusColor[data.status]">{{ statusMap[data.status] || "未知" }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="发布方式">{{ releaseMethodMap[data.releaseMethod] }} </a-descriptions-item>
      <a-descriptions-item label="定时构建"> {{ data.autoBuildCron }} </a-descriptions-item>
      <a-descriptions-item label="创建时间"> {{ parseTime(data.createTimeMillis) }} </a-descriptions-item>
      <a-descriptions-item label="最后修改时间"> {{ parseTime(data.modifyTimeMillis) }}</a-descriptions-item>
      <a-descriptions-item label="最后修改人">{{ data.modifyUser }}</a-descriptions-item>
    </a-descriptions>

    <a-row type="flex" justify="center">
      <a-divider v-if="listQuery.total > 1" dashed>构建历史</a-divider>
      <a-timeline mode="alternate" style="width: 100%">
        <a-timeline-item v-for="item in this.historyList" :key="item.id" :color="statusColor[item.status]">
          <template slot="dot"> #{{ item.buildNumberId }}</template>
          <a-space direction="vertical" :size="1">
            <div v-if="item.buildRemark">构建备注：{{ item.buildRemark }}</div>
            <div>
              状态：<a-tag :color="statusColor[item.status]">{{ statusMap[item.status] || "未知" }}</a-tag>
            </div>
            <div>时间：{{ parseTime(item.startTime) }} ~ {{ parseTime(item.endTime) }}</div>
            <div>触发类型：{{ triggerBuildTypeMap[item.triggerBuildType] || "未知" }}</div>
            <div>占用空间：{{ renderSize(item.resultFileSize) }}(产物)/{{ renderSize(item.buildLogFileSize) }}(日志)</div>

            <div>构建耗时：{{ formatDuration((item.endTime || 0) - (item.startTime || 0), "", 2) }}</div>
            <div>
              发布方式：
              <a-tag> {{ releaseMethodMap[item.releaseMethod] || "未知" }}</a-tag>
            </div>
            <div>
              操作：
              <a-space>
                <a-tooltip title="下载构建日志,如果按钮不可用表示日志文件不存在,一般是构建历史相关文件被删除">
                  <a-button size="small" icon="download" type="primary" :disabled="!item.hasLog" @click="handleDownload(item)">日志</a-button>
                </a-tooltip>

                <a-tooltip title="下载构建产物,如果按钮不可用表示产物文件不存在,一般是构建没有产生对应的文件或者构建历史相关文件被删除">
                  <a-button size="small" icon="download" type="primary" :disabled="!item.hasFile" @click="handleFile(item)"> 产物 </a-button>
                </a-tooltip>
              </a-space>
            </div>
          </a-space>
        </a-timeline-item>
      </a-timeline>
      <a-divider v-if="listQuery.total / listQuery.limit > 1" dashed />
      <a-col>
        <a-pagination
          v-model="listQuery.page"
          :showTotal="
            (total) => {
              return PAGE_DEFAULT_SHOW_TOTAL(total, listQuery);
            }
          "
          :showSizeChanger="true"
          :pageSizeOptions="PAGE_DEFAULT_SIZW_OPTIONS"
          :pageSize="listQuery.limit"
          :total="listQuery.total"
          :hideOnSinglePage="true"
          @showSizeChange="
            (current, size) => {
              this.listQuery.limit = size;
              this.listHistory();
            }
          "
          @change="this.listHistory"
          show-less-items
        />
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { getBuildGet, releaseMethodMap, statusMap, geteBuildHistory, statusColor, triggerBuildTypeMap, downloadBuildFile, downloadBuildLog } from "@/api/build-info";
import { parseTime, PAGE_DEFAULT_LIST_QUERY, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, renderSize, formatDuration } from "@/utils/const";
export default {
  props: {
    id: {
      type: String,
    },
  },
  data() {
    return {
      PAGE_DEFAULT_SIZW_OPTIONS,
      triggerBuildTypeMap,
      releaseMethodMap,
      statusColor,
      statusMap,
      data: {},
      listQuery: Object.assign({ buildDataId: this.id }, PAGE_DEFAULT_LIST_QUERY),
      historyList: [],
    };
  },
  created() {
    this.getData();
    this.listHistory();
  },
  methods: {
    parseTime,
    formatDuration,
    PAGE_DEFAULT_SHOW_TOTAL,
    renderSize,
    // 获取构建数据
    getData() {
      // 构建基础信息
      getBuildGet({
        id: this.id,
      }).then((res) => {
        if (res.data) {
          this.data = res.data;
        }
      });
    },
    listHistory() {
      // 构建历史
      geteBuildHistory(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.historyList = res.data.result;
          this.listQuery.total = res.data.total;
        }
      });
    },
    // 下载构建日志
    handleDownload(record) {
      window.open(downloadBuildLog(record.id), "_blank");
    },

    // 下载构建产物
    handleFile(record) {
      window.open(downloadBuildFile(record.id), "_blank");
    },
  },
};
</script>
<style scoped></style>
