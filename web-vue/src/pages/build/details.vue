<template>
  <div>
    <a-descriptions bordered size="small">
      <template #title>
        <a-space>
          {{ data.name }}
          <a-tooltip :title="$t('build.details.refreshTooltip')">
            <a-button type="link" size="small" icon="reload" @click="refresh" />
          </a-tooltip>
        </a-space>
      </template>

      <a-descriptions-item :label="$t('common.group')">
        {{ data.group }}
      </a-descriptions-item>
      <a-descriptions-item :label="$t('build.common.label')"> {{ data.branchName }} {{ data.branchTagName }} </a-descriptions-item>
      <a-descriptions-item :label="$t('build.common.buildMethod')">
        <template v-if="data.buildMode === 1">
          <a-icon type="cloud" />
          {{ $t('build.common.containerBuild') }}
        </template>
        <template v-else>
          <a-icon type="code" />
          {{ $t('build.common.containerBuild') }}
        </template>
      </a-descriptions-item>

      <a-descriptions-item :label="$t('build.common.buildId')">
        <span v-if="data.buildId <= 0" />
        <a-tag v-else color="#108ee9">#{{ data.buildId }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item :label="$t('build.common.buildStatus')">
        <a-tooltip :title="data.statusMsg">
          <a-tag :color="statusColor[data.status]"> {{ statusMap[data.status] || $t('common.unknown') }} <a-icon type="info-circle" v-if="data.statusMsg" /></a-tag>
        </a-tooltip>
      </a-descriptions-item>
      <a-descriptions-item :label="$t('build.common.releaseMethod')">{{ releaseMethodMap[data.releaseMethod] }} </a-descriptions-item>
      <a-descriptions-item :label="$t('build.common.autoBuildCron')"> {{ data.autoBuildCron }} </a-descriptions-item>
      <a-descriptions-item :label="$t('build.common.aliasCode')"> {{ data.aliasCode }} </a-descriptions-item>
      <a-descriptions-item :label="$t('build.common.buildDir')">
        <a-tag>{{ data.sourceDirExist ? $t('common.exists') : $t('common.notExists') }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item :label="$t('common.createTime')"> {{ parseTime(data.createTimeMillis) }} </a-descriptions-item>
      <a-descriptions-item :label="$t('common.lastModifiedTime')"> {{ parseTime(data.modifyTimeMillis) }}</a-descriptions-item>
      <a-descriptions-item :label="$t('common.lastModifiedBy')">{{ data.modifyUser }}</a-descriptions-item>
      <a-descriptions-item :label="$t('common.product')" :span="3"> {{ data.resultDirFile }} </a-descriptions-item>
      <a-descriptions-item :label="$t('common.sourceRepository')" :span="3" v-if="tempRepository">{{ `${tempRepository ? tempRepository.name + "[" + tempRepository.gitUrl + "]" : ""}` }}</a-descriptions-item>
      <a-descriptions-item :label="$t('common.repository') + 'last commit'" :span="3">{{ data.repositoryLastCommitId }}</a-descriptions-item>
    </a-descriptions>

    <a-row type="flex" justify="center">
      <a-divider v-if="listQuery.total > 0" dashed>{{ $t('build.common.buildHistory') }}</a-divider>
      <a-timeline mode="alternate" style="width: 100%">
        <a-timeline-item v-for="item in this.historyList" :key="item.id" :color="statusColor[item.status]">
          <template slot="dot"> #{{ item.buildNumberId }}</template>
          <a-space direction="vertical" :size="1">
            <div v-if="item.buildRemark">{{ $t('build.common.buildRemark') }}{{ $t('common.colon') }}{{ item.buildRemark }}</div>
            <div>
              <a-tooltip :title="item.statusMsg || statusMap[item.status] || $t('common.unknown')">
                {{ $t('common.status') }}{{ $t('common.colon') }}<a-tag :color="statusColor[item.status]">{{ statusMap[item.status] || $t('common.unknown') }}</a-tag>
              </a-tooltip>
            </div>
            <div>{{ $t('common.time') }}{{ $t('common.colon') }}{{ parseTime(item.startTime) }} ~ {{ parseTime(item.endTime) }}</div>
            <div>{{ $t('build.common.triggerType') }}{{ $t('common.colon') }}{{ triggerBuildTypeMap[item.triggerBuildType] || $t('common.unknown') }}</div>
            <div>{{ $t('build.common.space') }}{{ $t('common.colon') }}{{ renderSize(item.resultFileSize) }}({{ $t('common.product') }})/{{ renderSize(item.buildLogFileSize) }}({{ $t('common.log') }})</div>

            <div>{{ $t('build.common.elapsedTime') }}{{ $t('common.colon') }}{{ formatDuration((item.endTime || 0) - (item.startTime || 0), "", 2) }}</div>
            <div>
              {{ $t('build.common.releaseMethod') }}{{ $t('common.colon') }}
              <a-tag> {{ releaseMethodMap[item.releaseMethod] || $t('common.unknown') }}</a-tag>
            </div>
            <div>
              {{ $t('common.operation') }}{{ $t('common.colon') }}
              <a-space>
                <a-tooltip :title="$t('build.details.downloadNoLogTooltip')">
                  <a-button size="small" icon="download" type="primary" :disabled="!item.hasLog" @click="handleDownload(item)">{{ $t('common.log') }}</a-button>
                </a-tooltip>

                <a-tooltip :title="$t('build.details.downloadNoFileTooltip')">
                  <a-button size="small" icon="download" type="primary" :disabled="!item.hasFile" @click="handleFile(item)">{{ $t('common.product') }}</a-button>
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
import { getRepositoryInfo } from "@/api/repository";
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
      tempRepository: null,
    };
  },
  created() {
    this.refresh();
  },
  methods: {
    parseTime,
    formatDuration,
    PAGE_DEFAULT_SHOW_TOTAL,
    renderSize,
    refresh() {
      this.getData();
      this.listHistory();
    },
    // 选择仓库
    getRepositpry() {
      getRepositoryInfo({
        id: this.data?.repositoryId,
      }).then((res) => {
        if (res.code === 200) {
          this.tempRepository = res.data;
        }
      });
    },
    // 获取构建数据
    getData() {
      // 构建基础信息
      getBuildGet({
        id: this.id,
      }).then((res) => {
        if (res.data) {
          this.data = res.data;
          this.getRepositpry();
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
