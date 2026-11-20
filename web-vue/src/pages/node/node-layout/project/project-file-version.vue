<!--

    Copyright (c) 2019 Of Him Code Technology Studio
    Jpom is licensed under Mulan PSL v2.
    You can use this software according to the terms and conditions of the Mulan PSL v2.
    You may obtain a copy of Mulan PSL v2 at:
    			http://license.coscl.org.cn/MulanPSL2
    THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
    See the Mulan PSL v2 for more details.

-->
<template>
  <div>
    <a-alert
      v-if="!loading && !enabled && !hasVersions"
      type="warning"
      show-icon
      :message="$t('i18n_f971771775')"
      style="margin-bottom: 12px"
    />
    <template v-else>
      <a-space wrap style="margin-bottom: 12px">
        <a-tag v-if="path">{{ $t('i18n_835b950031') }}：{{ path }}</a-tag>
        <a-tag>{{ $t('i18n_0e9a916105') }}：{{ versionCount > 0 ? versionCount : '—' }}</a-tag>
        <a-checkbox v-model:checked="restart">{{ $t('i18n_eac2e936c1') }}</a-checkbox>
        <a-button size="small" type="primary" @click="loadData">{{ $t('i18n_90b5a467c1') }}</a-button>
      </a-space>
      <a-table
        size="middle"
        :data-source="list"
        :loading="loading"
        :columns="columns"
        :pagination="false"
        row-key="versionId"
        bordered
        :scroll="{ x: 'max-content' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'versionId'">
            <a-space>
              <span>{{ record.versionId }}</span>
              <a-tag v-if="isCurrent(record)" color="#52c41a">{{ $t('i18n_34601006f4') }}</a-tag>
            </a-space>
          </template>
          <template v-else-if="column.dataIndex === 'source'">
            {{ sourceText(record.source) }}
          </template>
          <template v-else-if="column.dataIndex === 'fileSize'">
            {{ record.fileSize ? renderSize(record.fileSize) : '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ record.createTime ? parseTime(record.createTime) : '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'operation'">
            <a-space>
              <a-button
                size="small"
                type="primary"
                :disabled="isCurrent(record)"
                :loading="switchLoading"
                @click="handleSwitch(record)"
              >
                {{ $t('i18n_757d7164bb') }}
              </a-button>
              <a-button size="small" type="primary" danger :disabled="isCurrent(record)" @click="handleDelete(record)">
                {{ $t('i18n_2f4aaddde3') }}
              </a-button>
            </a-space>
          </template>
        </template>
        <template #emptyText>
          <a-empty :description="$t('i18n_8052cf3aee')" />
        </template>
      </a-table>
    </template>
  </div>
</template>
<script>
import { versionDelete, versionList, versionSwitch } from '@/api/node-project-version'
import { parseTime, renderSize } from '@/utils/const'
import { notification } from 'ant-design-vue'

const SOURCE_TEXT = {
  baseline: 'i18n_ac6f69a7e9',
  build: 'i18n_f826a97a56',
  auto: 'i18n_8f0fecf967'
}

export default {
  name: 'ProjectFileVersion',
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    projectId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: false,
      switchLoading: false,
      restart: false,
      enabled: true,
      hasVersions: false,
      versionCount: 0,
      path: '',
      current: null,
      list: [],
      columns: [
        { title: this.$t('i18n_fe2df04a16'), dataIndex: 'versionId', width: 220 },
        { title: this.$t('i18n_26ca20b161'), dataIndex: 'source', width: 140 },
        { title: this.$t('i18n_b904ab3dc3'), dataIndex: 'buildNumberId', width: 100 },
        { title: this.$t('i18n_ad35f58fb3'), dataIndex: 'fileSize', width: 120 },
        { title: this.$t('i18n_eca37cb072'), dataIndex: 'createTime', width: 180 },
        { title: this.$t('i18n_2b6bc0f293'), dataIndex: 'operation', width: 240 }
      ]
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    parseTime,
    renderSize,
    sourceText(source) {
      const key = SOURCE_TEXT[source]
      return key ? this.$t(key) : source || '-'
    },
    isCurrent(record) {
      return this.current && this.current === record.versionId
    },
    loadData() {
      this.loading = true
      versionList({ nodeId: this.nodeId, id: this.projectId })
        .then((res) => {
          if (res.code === 200) {
            const data = res.data || {}
            this.enabled = data.enabled !== false
            // 插件端配置为 0 但磁盘上已有版本（构建按默认 3 生成）时，仍然要展示并允许切换/删除
            this.hasVersions = data.hasVersions === true || (data.list || []).length > 0
            this.versionCount = data.versionCount || 0
            this.path = data.path || ''
            this.current = data.current
            this.list = data.list || []
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleSwitch(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_d85262b6f5', { v: record.versionId }),
        okText: this.$t('i18n_38cf16f220'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          this.switchLoading = true
          return versionSwitch({
            nodeId: this.nodeId,
            id: this.projectId,
            versionId: record.versionId,
            after: this.restart ? 1 : 0,
            snapshotCurrent: true
          })
            .then((res) => {
              if (res.code === 200) {
                notification.success({ message: this.$t('i18n_665c2431e8') })
                this.loadData()
              }
            })
            .finally(() => {
              this.switchLoading = false
            })
        }
      })
    },
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_f2da3dad73'),
        okText: this.$t('i18n_38cf16f220'),
        cancelText: this.$t('i18n_625fb26b4b'),
        okType: 'danger',
        onOk: () =>
          versionDelete({ nodeId: this.nodeId, id: this.projectId, versionId: record.versionId }).then((res) => {
            if (res.code === 200) {
              notification.success({ message: res.msg })
              this.loadData()
            }
          })
      })
    }
  }
}
</script>
