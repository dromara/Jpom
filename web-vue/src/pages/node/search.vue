<template>
  <div class="">
    <template v-if="useSuggestions">
      <a-result :title="$tl('p.noProjectOrNode')" :sub-title="$tl('p.addNewAsset')"> </a-result>
    </template>

    <CustomTable
      v-else
      ref="nodeProjectSearch"
      table-name="nodeProjectSearch"
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      :active-page="activePage"
      :data-source="projList"
      :columns="columns"
      size="middle"
      bordered
      :pagination="pagination"
      :row-selection="rowSelection"
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="getNodeProjectData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-select
            v-if="!nodeId"
            v-model:value="listQuery.nodeId"
            allow-clear
            :placeholder="$tl('c.selectNode')"
            class="search-input-item"
          >
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.group"
            allow-clear
            :placeholder="$tl('p.selectGroup')"
            class="search-input-item"
            @change="getNodeProjectData"
          >
            <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
          </a-select>
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$tl('p.searchProjectName')"
            class="search-input-item"
            @press-enter="getNodeProjectData"
          />
          <a-input
            v-model:value="listQuery['%projectId%']"
            :placeholder="$tl('p.searchProjectId')"
            class="search-input-item"
            @press-enter="getNodeProjectData"
          />

          <a-select
            v-model:value="listQuery.runMode"
            allow-clear
            :placeholder="$tl('c.runMode')"
            class="search-input-item"
          >
            <a-select-option v-for="item in runModeList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip :title="$tl('p.quickToFirstPage')">
            <a-button :loading="loading" type="primary" @click="getNodeProjectData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>

          <!-- <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="silenceLoadData" /> -->
        </a-space>
      </template>
      <template #toolPrefix>
        <a-dropdown v-if="selectedRowKeys && selectedRowKeys.length">
          <a-button type="primary" size="small"> {{ $tl('c.batchOperation') }} <DownOutlined /> </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item>
                <a-button type="primary" @click="batchStart">{{ $tl('c.batchStart') }}</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="primary" @click="batchRestart">{{ $tl('p.batchRestart') }}</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="primary" danger @click="batchStop">{{ $tl('p.batchClose') }}</a-button>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-button v-else type="primary" size="small" :disabled="true">
          {{ $tl('c.batchOperation') }} <DownOutlined />
        </a-button>

        <a-button type="primary" size="small" @click="openAdd"><PlusOutlined />{{ $tl('p.addNew') }}</a-button>
        <template v-if="!nodeId">
          <a-dropdown v-if="nodeMap && Object.keys(nodeMap).length">
            <a-button type="primary" size="small" danger> {{ $tl('c.synchronization') }} <DownOutlined /></a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item v-for="(nodeName, key) in nodeMap" :key="key" @click="reSyncProject(key)">
                  <a href="javascript:;">{{ nodeName }} <SyncOutlined /></a>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
        <a-button v-else type="primary" size="small" danger @click="reSyncProject(nodeId)">
          <SyncOutlined />{{ $tl('c.synchronization') }}
        </a-button>

        <a-button v-if="nodeId" size="small" type="primary" @click="handlerExportData()"
          ><DownloadOutlined />{{ $tl('p.exportData') }}</a-button
        >
        <a-dropdown v-if="nodeId">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1">
                <a-button type="primary" size="small" @click="handlerImportTemplate()">{{
                  $tl('p.downloadTemplate')
                }}</a-button>
              </a-menu-item>
            </a-menu>
          </template>

          <a-upload
            name="file"
            accept=".csv"
            action=""
            :show-upload-list="false"
            :multiple="false"
            :before-upload="importBeforeUpload"
          >
            <a-button size="small" type="primary"
              ><UploadOutlined /> {{ $tl('p.importData') }} <DownOutlined />
            </a-button>
          </a-upload>
        </a-dropdown>
      </template>
      <template #tableHelp>
        <a-tooltip placement="left">
          <template #title>
            <div>
              <ul>
                <li>{{ $tl('p.statusDataDelay') }}</li>
                <li>{{ $tl('p.fileListOrder') }}</li>
              </ul>
            </div>
          </template>
          <QuestionCircleOutlined /> </a-tooltip
      ></template>
      <template #tableBodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text">
            <a-button type="link" style="padding: 0" size="small" @click="openEdit(record)">
              <ApartmentOutlined v-if="record.outGivingProject" />
              <span>{{ text }}</span>
            </a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'nodeId'">
          <a-tooltip placement="topLeft" :title="text">
            <a-button type="link" style="padding: 0" size="small" @click="toNode(text)">
              <span>{{ nodeMap[text] }}</span>
              <FullscreenOutlined />
            </a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'path'">
          <a-tooltip placement="topLeft" :title="(record.whitelistDirectory || '') + (record.lib || '')">
            <span>{{ (record.whitelistDirectory || '') + (record.lib || '') }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text || '' }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <template
            v-if="
              projectStatusMap[record.nodeId] &&
              projectStatusMap[record.nodeId][record.projectId] &&
              projectStatusMap[record.nodeId][record.projectId].error
            "
          >
            <a-tooltip
              :title="
                projectStatusMap[record.nodeId] &&
                projectStatusMap[record.nodeId][record.projectId] &&
                projectStatusMap[record.nodeId][record.projectId].error
              "
            >
              <WarningOutlined />
            </a-tooltip>
          </template>
          <template v-else>
            <a-tooltip
              v-if="noFileModes.includes(record.runMode)"
              :title="`${$tl('p.statusControl')}   ${(projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].statusMsg) || ''}`"
            >
              <a-switch
                :checked="
                  projectStatusMap[record.nodeId] &&
                  projectStatusMap[record.nodeId][record.projectId] &&
                  projectStatusMap[record.nodeId][record.projectId].pid > 0
                "
                disabled
                :checked-children="$tl('p.statusOn')"
                :un-checked-children="$tl('p.statusOff')"
              />
            </a-tooltip>
            <span v-else>-</span>
          </template>
        </template>

        <template v-else-if="column.dataIndex === 'port'">
          <a-tooltip
            placement="topLeft"
            :title="`${$tl('p.processId')}${((projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].pids) || [(projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].pid) || '-']).join(',')} / ${$tl('p.portNumber')}${(projectStatusMap[record.nodeId] && projectStatusMap[record.nodeId][record.projectId] && projectStatusMap[record.nodeId][record.projectId].port) || '-'}`"
          >
            <span
              >{{
                (projectStatusMap[record.nodeId] &&
                  projectStatusMap[record.nodeId][record.projectId] &&
                  projectStatusMap[record.nodeId][record.projectId].port) ||
                '-'
              }}/{{
                (
                  (projectStatusMap[record.nodeId] &&
                    projectStatusMap[record.nodeId][record.projectId] &&
                    projectStatusMap[record.nodeId][record.projectId].pids) || [
                    (projectStatusMap[record.nodeId] &&
                      projectStatusMap[record.nodeId][record.projectId] &&
                      projectStatusMap[record.nodeId][record.projectId].pid) ||
                      '-'
                  ]
                ).join(',')
              }}</span
            >
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleFile(record)">{{ $tl('p.fileType') }}</a-button>
            <template v-if="noFileModes.includes(record.runMode)">
              <a-button size="small" type="primary" @click="handleConsole(record)">{{ $tl('c.console') }}</a-button>
            </template>
            <template v-else>
              <a-tooltip :title="$tl('p.noConsoleForFile')">
                <a-button size="small" type="primary" :disabled="true">{{ $tl('c.console') }}</a-button></a-tooltip
              >
            </template>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $tl('p.moreOptions') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <template v-if="noFileModes.includes(record.runMode)">
                      <a-button size="small" type="primary" @click="handleTrigger(record)">{{
                        $tl('c.trigger')
                      }}</a-button>
                    </template>
                    <template v-else>
                      <a-tooltip :title="$tl('p.noTriggerForFile')">
                        <a-button size="small" type="primary" :disabled="true">{{
                          $tl('c.trigger')
                        }}</a-button></a-tooltip
                      >
                    </template>
                  </a-menu-item>
                  <a-menu-item v-if="noFileModes.includes(record.runMode)">
                    <a-button size="small" type="primary" @click="handleLogBack(record)"
                      >{{ $tl('p.projectLog') }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="copyItem(record)">{{ $tl('p.copyAction') }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record, '')">{{
                      $tl('p.logicalDelete')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record, 'thorough')">{{
                      $tl('p.permanentDelete')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="migrateWorkspace(record)">{{
                      $tl('p.migrateWorkspace')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'top')"
                      >{{ $tl('p.topPriority') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'up')"
                      >上移</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total"
                      @click="sortItemHander(record, index, 'down')"
                    >
                      下移
                    </a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 项目文件组件 -->
    <a-drawer
      destroy-on-close
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :open="drawerFileVisible"
      @close="onFileClose"
    >
      <file
        v-if="drawerFileVisible"
        :node-id="temp.nodeId"
        :project-id="temp.projectId"
        :run-mode="temp.runMode"
        @go-console="goConsole"
        @go-read-file="goReadFile"
      />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer
      destroy-on-close
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :open="drawerConsoleVisible"
      @close="onConsoleClose"
    >
      <console
        v-if="drawerConsoleVisible"
        :id="temp.id"
        :node-id="temp.nodeId"
        :project-id="temp.projectId"
        @go-file="goFile"
      />
    </a-drawer>
    <!-- 项目跟踪文件组件 -->
    <a-drawer
      destroy-on-close
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :open="drawerReadFileVisible"
      @close="onReadFileClose"
    >
      <file-read
        v-if="drawerReadFileVisible"
        :id="temp.id"
        :node-id="temp.nodeId"
        :read-file-path="temp.readFilePath"
        :project-id="temp.projectId"
        @go-file="goFile"
      />
    </a-drawer>
    <!-- 批量操作状态 -->
    <a-modal v-model:open="batchVisible" destroy-on-close :title="temp.title" :footer="null" @cancel="batchClose">
      <a-list bordered :data-source="temp.data">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <!-- <template #description> :="item.whitelistDirectory" </template> -->
              <template #title>
                <a> {{ item.name }}</a>
              </template>
            </a-list-item-meta>
            <div>
              <a-tooltip :title="`${item.cause || $tl('c.notStarted')}`"
                >{{ item.cause || $tl('c.notStarted') }}
              </a-tooltip>
            </div>
          </a-list-item>
        </template>
      </a-list>
    </a-modal>
    <!-- 触发器 -->
    <a-modal
      v-model:open="triggerVisible"
      destroy-on-close
      :title="$tl('c.trigger')"
      width="50%"
      :footer="null"
      :mask-closable="false"
    >
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$tl('p.resetTriggerToken')">
              <a-button type="primary" size="small" @click="resetTrigger">{{ $tl('p.resetAction') }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$tl('p.executeAction')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$tl('c.warmTip')" type="warning">
                <template #description>
                  <ul>
                    <li>{{ $tl('p.triggerAddressInfo') }}</li>
                    <li>{{ $tl('p.resetTriggerAddress') }}</li>
                    <li>{{ $tl('p.batchTriggerParams') }}</li>
                  </ul>
                </template>
              </a-alert>

              <a-alert
                v-for="item in triggerUses"
                :key="item.value"
                type="info"
                :message="`${item.desc}${$tl('p.triggerAddress')}(${$tl('c.copyTip')})`"
              >
                <template #description>
                  <a-typography-paragraph
                    :copyable="{ tooltip: false, text: `${temp.triggerUrl}?action=${item.value}` }"
                  >
                    <a-tag>GET</a-tag>
                    <span>{{ `${temp.triggerUrl}?action=${item.value}` }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>

              <a-alert type="info" :message="`${$tl('p.batchTriggerAddress')}(${$tl('c.copyTip')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.batchTriggerUrl }">
                    <a-tag>POST</a-tag> <span>{{ temp.batchTriggerUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
        </a-tabs>
      </a-form>
    </a-modal>
    <!-- 编辑区 -->
    <a-modal
      v-model:open="editProjectVisible"
      destroy-on-close
      width="60vw"
      :title="$tl('p.editProject')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="
        () => {
          confirmLoading = true
          $refs.edit.handleOk().finally(() => {
            confirmLoading = false
          })
        }
      "
    >
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$tl('p.selectNode')" :help="$tl('p.switchNodeDuringEdit')">
          <a-select v-model:value="temp.nodeId" allow-clear :placeholder="$tl('c.selectNode')">
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>

      <project-edit
        v-if="temp.nodeId"
        ref="edit"
        :data="temp"
        :node-id="temp.nodeId"
        :project-id="temp.id"
        @close="
          () => {
            editProjectVisible = false
            getNodeProjectData()
            loadGroupList()
          }
        "
      />
    </a-modal>
    <!-- 迁移到其他工作空间 -->
    <a-modal
      v-model:open="migrateWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50vw"
      :title="$tl('p.migrateToOtherWorkspace')"
      :mask-closable="false"
      @ok="migrateWorkspaceOk"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert :message="$tl('c.warmTip')" type="warning" show-icon>
          <template #description>
            {{ $tl('p.projectSupportAssociatedData') }}
            <ul>
              <li>
                {{ $tl('p.onlineBuild') }}

                <ol>
                  <li>{{ $tl('p.cannotMigrateIfBuildRepoIsBoundByMultipleBuilds') }}</li>
                  <li>{{ $tl('p.resolveDuplicateReposAfterAutoMigration') }}</li>
                </ol>
              </li>
              <li>{{ $tl('p.nodeDistributionNotSupported') }}</li>
              <li>{{ $tl('p.projectMonitoringNotSupported') }}</li>
              <li>{{ $tl('p.logReadingNotSupported') }}</li>
            </ul>
          </template>
        </a-alert>
        <a-alert :message="$tl('p.riskReminder')" type="error" show-icon>
          <template #description>
            <ul>
              <li>{{ $tl('p.logicalDeletionBeforeMigration') }}</li>
              <li>{{ $tl('p.migrationIsNotTransactional') }}</li>
              <li>{{ $tl('p.checkConnectionAndNetworkStatusBeforeMigration') }}</li>
            </ul>
          </template>
        </a-alert>
      </a-space>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$tl('p.selectWorkspace')" name="workspaceId">
          <a-select
            v-model:value="temp.workspaceId"
            show-search
            :filter-option="
              (input, option) => {
                const children = option.children && option.children()
                return (
                  children &&
                  children[0].children &&
                  children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                )
              }
            "
            :placeholder="$tl('c.selectWorkspace')"
            @change="loadMigrateWorkspaceNodeList"
          >
            <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$tl('p.selectLogicalNode')" name="nodeId">
          <a-select
            v-model:value="temp.nodeId"
            show-search
            :filter-option="
              (input, option) => {
                const children = option.children && option.children()
                return (
                  children &&
                  children[0].children &&
                  children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                )
              }
            "
            :placeholder="$tl('c.selectLogicNode')"
          >
            <a-select-option v-for="item in migrateWorkspaceNodeList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 日志备份 -->
    <a-modal
      v-model:open="lobbackVisible"
      destroy-on-close
      :title="$tl('p.logBackupList')"
      width="850px"
      :footer="null"
      :mask-closable="false"
    >
      <ProjectLog v-if="lobbackVisible" :node-id="temp.nodeId" :project-id="temp.projectId"></ProjectLog>
    </a-modal>
  </div>
</template>

<script>
import { getNodeListAll, getProjectList, sortItemProject, syncProject } from '@/api/node'
import {
  getRuningProjectInfo,
  noFileModes,
  operateProject,
  runModeList,
  getProjectTriggerUrl,
  getProjectGroupAll,
  deleteProject,
  migrateWorkspace,
  importTemplate,
  exportData,
  importData
} from '@/api/node-project'
import File from '@/pages/node/node-layout/project/project-file'
import Console from '../node/node-layout/project/project-console'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  concurrentJobs,
  itemGroupBy,
  parseTime
} from '@/utils/const'
import FileRead from '@/pages/node/node-layout/project/project-file-read'
import ProjectEdit from '@/pages/node/node-layout/project/project-edit'
import CustomTable from '@/components/customTable/index.vue'

import { mapState } from 'pinia'
import { useUserStore } from '@/stores/user'
import { getWorkSpaceListAll } from '@/api/workspace'
import ProjectLog from '@/pages/node/node-layout/project/project-log.vue'
export default {
  components: {
    File,
    Console,
    FileRead,
    ProjectEdit,
    ProjectLog,
    CustomTable
  },
  props: {
    nodeId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      projList: [],
      projectStatusMap: {},
      groupList: [],
      runModeList,
      selectedRowKeys: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      noFileModes,
      nodeMap: {},
      drawerTitle: '',
      loading: true,
      temp: {},
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      drawerReadFileVisible: false,
      batchVisible: false,

      columns: [
        {
          title: this.$tl('p.projectId'),
          dataIndex: 'projectId',
          width: 100,
          ellipsis: true
        },

        {
          title: this.$tl('p.projectName'),
          dataIndex: 'name',
          // width: 200,
          ellipsis: true
        },
        {
          title: this.$tl('p.projectGroup'),
          dataIndex: 'group',
          sorter: true,
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.nodeName'),
          dataIndex: 'nodeId',
          width: 90,
          ellipsis: true
        },
        {
          title: this.$tl('p.runningStatus'),
          dataIndex: 'status',
          align: 'center',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('p.projectPath'),
          dataIndex: 'path',
          ellipsis: true,
          width: 120
        },
        {
          title: this.$tl('p.logPath'),
          dataIndex: 'logPath',
          ellipsis: true,
          width: 120
        },

        {
          title: this.$tl('p.portOrPid'),
          dataIndex: 'port',
          width: 100,
          ellipsis: true
        },

        {
          title: this.$tl('c.runMode'),
          dataIndex: 'runMode',
          width: 90,
          ellipsis: true
        },
        {
          title: 'webhook',
          dataIndex: 'token',
          // width: 120,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.createTime'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.updateTime'),
          dataIndex: 'modifyTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.modifier'),
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          sorter: true
        },
        {
          title: this.$tl('p.sortValue'),
          dataIndex: 'sortValue',
          sorter: true,
          width: '80px'
        },
        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',
          align: 'center',
          fixed: 'right',

          width: '190px'
        }
      ],
      triggerVisible: false,
      triggerUses: [
        { desc: this.$tl('p.viewStatus'), value: 'status' },
        { desc: this.$tl('p.startProject'), value: 'start' },
        { desc: this.$tl('p.stopProject'), value: 'stop' },
        { desc: this.$tl('p.restartProject'), value: 'restart' }
      ],
      editProjectVisible: false,
      // countdownTime: Date.now(),
      // refreshInterval: 5,
      migrateWorkspaceVisible: false,
      workspaceList: [],
      migrateWorkspaceNodeList: [],
      lobbackVisible: false,
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useUserStore, ['getUserInfo']),
    filePath() {
      return (this.temp.whitelistDirectory || '') + (this.temp.lib || '')
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        onChange: this.onSelectChange,
        columnWidth: '40px',
        getCheckboxProps: this.getCheckboxProps
      }
    },
    useSuggestions() {
      if (this.loading) {
        // 加载中不提示
        return false
      }
      if (!this.getUserInfo || !this.getUserInfo.systemUser) {
        // 没有登录或者不是超级管理员
        return false
      }
      if (Object.keys(this.nodeMap).length) {
        return false
      }
      return true
      // if (this.listQuery.page !== 1 || this.listQuery.total > 0) {
      //   // 不是第一页 或者总记录数大于 0
      //   return false;
      // }
      // // 判断是否存在搜索条件
      // const nowKeys = Object.keys(this.listQuery);
      // const defaultKeys = Object.keys(PAGE_DEFAULT_LIST_QUERY);
      // const dictOrigin = nowKeys.filter((item) => !defaultKeys.includes(item));
      // return dictOrigin.length === 0;
    }
  },
  mounted() {
    getNodeListAll().then((res) => {
      if (res.code === 200) {
        res.data.forEach((item) => {
          this.nodeMap = { ...this.nodeMap, [item.id]: item.name }
        })
        this.getNodeProjectData()
      }
    })
    this.countdownTime = Date.now() + this.refreshInterval * 1000
    //
    this.loadGroupList()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.node.search.${key}`, ...args)
    },
    getNodeProjectData(pointerEvent, loading) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.nodeId && (this.listQuery.nodeId = this.nodeId)

      getProjectList(this.listQuery, loading)
        .then((res) => {
          if (res.code === 200) {
            const resultList = res.data.result

            // const tempList = resultList.filter((item) => item.runMode !== 'File')
            // const fileList = resultList.filter((item) => item.runMode === 'File')
            this.projList = resultList // tempList.concat(fileList)

            this.listQuery.total = res.data.total

            const nodeProjects = itemGroupBy(this.projList, 'nodeId')
            this.getRuningProjectInfo(nodeProjects)

            // 重新计算倒计时
            // this.countdownTime = Date.now() + this.refreshInterval * 1000
            this.$refs.nodeProjectSearch.countDownChange()
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadGroupList() {
      getProjectGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    getRuningProjectInfo(nodeProjects) {
      if (nodeProjects.length <= 0) {
        return
      }
      concurrentJobs(
        nodeProjects.map((item, index) => {
          return index
        }),
        3,
        (citem) => {
          // console.log(i);
          const data = nodeProjects[citem]
          return new Promise((resolve, reject) => {
            const ids = data.data.map((item) => {
              return item.projectId
            })
            if (ids.length <= 0) {
              return
            }
            const tempParams = {
              nodeId: data.type,
              ids: JSON.stringify(ids)
            }
            getRuningProjectInfo(tempParams, 'noTip')
              .then((res2) => {
                if (res2.code === 200) {
                  this.projectStatusMap = {
                    ...this.projectStatusMap,
                    [data.type]: res2.data
                  }
                  resolve()
                } else {
                  const data2 = {}
                  this.projList.forEach((item) => {
                    data2[item.projectId] = {
                      port: 0,
                      pid: 0,
                      error: res2.msg
                    }
                  })
                  this.projectStatusMap = {
                    ...this.projectStatusMap,
                    [data.type]: data2
                  }
                  reject()
                }
                // this.getRuningProjectInfo(nodeProjects, i + 1);
              })
              .catch(() => {
                const data2 = {}
                this.projList.forEach((item) => {
                  data2[item.projectId] = {
                    port: 0,
                    pid: 0,
                    error: this.$tl('p.networkException')
                  }
                })
                this.projectStatusMap = {
                  ...this.projectStatusMap,
                  [data.type]: data2
                }
                reject()
              })
          })
        }
      )
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `${this.$tl('p.fileManagement')}(${this.temp.name})`
      this.drawerFileVisible = true
    },
    // 关闭文件管理对话框
    onFileClose() {
      this.drawerFileVisible = false
      this.getNodeProjectData()
    },
    // 控制台
    handleConsole(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `${this.$tl('c.console')}(${this.temp.name})`
      this.drawerConsoleVisible = true
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false
      this.getNodeProjectData()
    },
    //前往文件
    goFile() {
      // 关闭控制台
      this.onConsoleClose()
      this.onReadFileClose()
      this.handleFile(this.temp)
    },
    //前往控制台
    goConsole() {
      //关闭文件
      this.onFileClose()
      this.handleConsole(this.temp)
    },
    // 跟踪文件
    goReadFile(path, filename) {
      this.onFileClose()
      this.drawerReadFileVisible = true
      this.temp.readFilePath = (path + '/' + filename).replace(new RegExp('//', 'gm'), '/')
      this.drawerTitle = `${this.$tl('p.trackFile')}(${filename})`
    },
    onReadFileClose() {
      this.drawerReadFileVisible = false
    },
    //选中项目
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys
    },
    batchClose() {
      this.batchVisible = false
      this.getNodeProjectData()
      this.selectedRowKeys = []
    },
    /**
     * 将选中的 key 转为 data
     */
    selectedRowKeysToId() {
      return this.projList
        .filter((item) => {
          return this.selectedRowKeys.includes(item.id)
        })
        .map((item) => {
          return {
            projectId: item.projectId,
            nodeId: item.nodeId,
            runMode: item.runMode,
            name: item.name
          }
        })
      // return this.selectedRowKeys.map((item) => {})
    },
    // 更新数据
    updateBatchData(index, data2) {
      const data = this.temp.data
      data[index] = { ...data[index], ...data2 }

      this.temp = {
        ...this.temp,
        data: [...data]
      }
    },
    //批量开始
    batchStart() {
      if (this.selectedRowKeys.length <= 0) {
        $notification.warning({
          message: this.$tl('p.pleaseSelectProjectToStart')
        })
        return
      }
      this.temp = {
        title: this.$tl('c.batchStart'),
        data: this.selectedRowKeysToId()
      }

      this.batchVisible = true
      this.batchOptInfo(0, this.$tl('p.start'), 'start')
    },
    // 批量操作
    batchOptInfo(index, msg, opt) {
      if (index >= (this.temp?.data?.length || -1)) {
        return
      }
      const value = this.temp.data[index]
      value.cause = msg + this.$tl('p.statusMedium')
      this.updateBatchData(index, value)
      if (value.runMode !== 'File') {
        const params = {
          nodeId: value.nodeId,
          id: value.projectId,
          opt: opt
        }

        operateProject(params)
          .then((data) => {
            value.cause = data.msg
            this.updateBatchData(index, value)
            this.batchOptInfo(index + 1, msg, opt)
          })
          .catch(() => {
            value.cause = msg + this.$tl('p.failed')
            this.updateBatchData(index, value)
            this.batchOptInfo(index + 1, msg, opt)
          })
      } else {
        value.cause = this.$tl('p.skip')
        this.updateBatchData(index, value)
        this.batchOptInfo(index + 1, msg, opt)
      }
    },

    //批量重启
    batchRestart() {
      if (this.selectedRowKeys.length <= 0) {
        $notification.warning({
          message: this.$tl('p.pleaseSelectProjectToRestart')
        })
        return
      }
      this.temp = {
        title: this.$tl('p.batchRestart_1'),
        data: this.selectedRowKeysToId()
      }
      this.batchVisible = true
      this.batchOptInfo(0, this.$tl('p.restartAction'), 'restart')
    },

    //批量关闭
    batchStop() {
      if (this.selectedRowKeys.length <= 0) {
        $notification.warning({
          message: this.$tl('p.pleaseSelectProjectToStop')
        })
      }
      this.temp = {
        title: this.$tl('p.batchStopStart'),
        data: this.selectedRowKeysToId()
      }
      this.batchVisible = true
      this.batchOptInfo(0, this.$tl('p.stopAction'), 'stop')
    },

    // 获取复选框属性 判断是否可以勾选
    getCheckboxProps(record) {
      return {
        // props: {
        disabled: record.runMode === 'File',
        name: record.name
        // }
      }
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.getNodeProjectData()
    },
    reSyncProject(nodeId) {
      $confirm({
        title: this.$tl('c.systemTip'),
        zIndex: 1009,
        content: this.$tl('p.confirmResyncCache'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return syncProject(nodeId).then((res) => {
            if (res.code == 200) {
              $notification.success({
                message: res.msg
              })
              this.getNodeProjectData()
            }
          })
        }
      })
    },
    // 排序
    sortItemHander(record, index, method) {
      const msgData = {
        top: this.$tl('p.pinToTop'),
        up: this.$tl('p.moveUp'),
        down: this.$tl('p.moveDown')
      }
      let msg = msgData[method] || this.$tl('p.operate')
      if (!record.sortValue) {
        msg += `${this.$tl('p.defaultStatus')},${this.$tl('p.unexpectedOrder')},${this.$tl('p.expectedOrder')}`
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.projList[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      $confirm({
        title: this.$tl('c.systemTip'),
        zIndex: 1009,
        content: msg,
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return sortItemProject({
            id: record.id,
            method: method,
            compareId: compareId
          }).then((res) => {
            if (res.code == 200) {
              $notification.success({
                message: res.msg
              })

              this.getNodeProjectData()
            }
          })
        }
      })
      // console.log(record, index, method);
    },
    // 触发器
    handleTrigger(record) {
      this.temp = Object.assign({}, record)

      getProjectTriggerUrl({
        id: record.id
      }).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res)
          this.triggerVisible = true
        }
      })
    },
    // 重置触发器
    resetTrigger() {
      getProjectTriggerUrl({
        id: this.temp.id,
        rest: 'rest'
      }).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.fillTriggerResult(res)
        }
      })
    },
    fillTriggerResult(res) {
      this.temp.triggerUrl = `${location.protocol}//${location.host}${res.data.triggerUrl}`
      this.temp.batchTriggerUrl = `${location.protocol}//${location.host}${res.data.batchTriggerUrl}`

      this.temp = { ...this.temp }
    },
    toNode(nodeId) {
      const newpage = this.$router.resolve({
        path: '/node/list',
        query: {
          ...this.$route.query,
          nodeId: nodeId,
          pId: 'manage',
          id: 'manageList'
        }
      })
      window.open(newpage.href, '_blank')
    },
    // 打开编辑
    openEdit(data) {
      // this.temp = {
      //   id: data.projectId,
      //   nodeId: data.nodeId,
      // };
      this.temp = { ...data, id: data.projectId }

      this.editProjectVisible = true
    },
    // 复制
    copyItem(record) {
      const temp = Object.assign({}, record)
      delete temp.id
      delete temp.createTimeMillis
      delete temp.outGivingProject
      this.temp = {
        ...temp,
        name: temp.name + this.$tl('p.copy'),
        id: temp.projectId + '_copy',
        lib: temp.lib + '_copy'
      }

      this.editProjectVisible = true
    },
    // 打开编辑
    openAdd() {
      this.temp = {
        nodeId: this.listQuery.nodeId
      }
      this.editProjectVisible = true
    },
    // 删除
    handleDelete(record, thorough) {
      $confirm({
        title: this.$tl('c.systemTip'),
        zIndex: 1009,
        content: thorough ? this.$tl('p.deleteForever') : this.$tl('p.deleteProject'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return deleteProject({
            nodeId: record.nodeId,
            id: record.projectId,
            thorough: thorough
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.getNodeProjectData()
            }
          })
        }
      })
    },
    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data
        }
      })
    },
    // 迁移到其他工作空间
    migrateWorkspace(record) {
      this.temp = {
        id: record.id,
        originalWorkspaceId: record.workspaceId,
        originalNodeId: record.nodeId
      }
      // delete this.temp.workspaceId;

      this.migrateWorkspaceVisible = true
      this.loadWorkSpaceListAll()
    },
    // 获取节点
    loadMigrateWorkspaceNodeList() {
      if (!this.temp.workspaceId) {
        return
      }
      getNodeListAll({
        workspaceId: this.temp.workspaceId
      }).then((res) => {
        this.migrateWorkspaceNodeList = res.data || []
      })
    },
    // 迁移确认
    migrateWorkspaceOk() {
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: this.$tl('c.selectWorkspace')
        })
        return false
      }
      if (!this.temp.nodeId) {
        $notification.warn({
          message: this.$tl('c.selectLogicNode')
        })
        return false
      }
      // 同步
      this.confirmLoading = true
      migrateWorkspace({
        id: this.temp.id,
        toWorkspaceId: this.temp.workspaceId,
        toNodeId: this.temp.nodeId
      })
        .then((res) => {
          if (res.code == 200) {
            $notification.success({
              message: res.msg
            })
            this.migrateWorkspaceVisible = false
            this.getNodeProjectData()
            return false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 下载导入模板
    handlerImportTemplate() {
      window.open(
        importTemplate({
          nodeId: this.listQuery.nodeId
        }),
        '_blank'
      )
    },
    handlerExportData() {
      window.open(exportData({ ...this.listQuery }), '_blank')
    },
    // 导入
    importBeforeUpload(file) {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('nodeId', this.listQuery.nodeId)
      importData(formData).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    },
    // 日志备份列表
    handleLogBack(record) {
      this.temp = Object.assign({}, record)
      this.lobbackVisible = true
    }
  }
}
</script>

<!-- <style scoped>
:deep(.ant-statistic div) {
  display: inline-block;
}

:deep(.ant-statistic-content-value, .ant-statistic-content) {
  font-size: 16px;
}
</style> -->
