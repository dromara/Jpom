<template>
  <div class="">
    <template v-if="useSuggestions">
      <a-result :title="$t('pages.node.search.2520ef6')" :sub-title="$t('pages.node.search.faab031b')"> </a-result>
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
            :placeholder="$t('pages.node.search.2388531c')"
            class="search-input-item"
          >
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.group"
            allow-clear
            :placeholder="$t('pages.node.search.761c903a')"
            class="search-input-item"
            @change="getNodeProjectData"
          >
            <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
          </a-select>
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('pages.node.search.33226e19')"
            class="search-input-item"
            @press-enter="getNodeProjectData"
          />
          <a-input
            v-model:value="listQuery['%projectId%']"
            :placeholder="$t('pages.node.search.9dc0bd7e')"
            class="search-input-item"
            @press-enter="getNodeProjectData"
          />

          <a-select
            v-model:value="listQuery.runMode"
            allow-clear
            :placeholder="$t('pages.node.search.7fbd7a7e')"
            class="search-input-item"
          >
            <a-select-option v-for="item in runModeList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('pages.node.search.a89bd71f')">
            <a-button :loading="loading" type="primary" @click="getNodeProjectData">{{
              $t('pages.node.search.53c2763c')
            }}</a-button>
          </a-tooltip>

          <!-- <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="silenceLoadData" /> -->
        </a-space>
      </template>
      <template #toolPrefix>
        <a-dropdown v-if="selectedRowKeys && selectedRowKeys.length">
          <a-button type="primary" size="small"> {{ $t('pages.node.search.766122c2') }} <DownOutlined /> </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item>
                <a-button type="primary" @click="batchStart">{{ $t('pages.node.search.8080545f') }}</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="primary" @click="batchRestart">{{ $t('pages.node.search.b15d1c0') }}</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="primary" danger @click="batchStop">{{ $t('pages.node.search.777ebf18') }}</a-button>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-button v-else type="primary" size="small" :disabled="true">
          {{ $t('pages.node.search.766122c2') }} <DownOutlined />
        </a-button>

        <a-button type="primary" size="small" @click="openAdd"
          ><PlusOutlined />{{ $t('pages.node.search.e141baa9') }}</a-button
        >
        <template v-if="!nodeId">
          <a-dropdown v-if="nodeMap && Object.keys(nodeMap).length">
            <a-button type="primary" size="small" danger>
              {{ $t('pages.node.search.830dbf3c') }} <DownOutlined
            /></a-button>
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
          <SyncOutlined />{{ $t('pages.node.search.830dbf3c') }}
        </a-button>

        <a-button v-if="nodeId" size="small" type="primary" @click="handlerExportData()"
          ><DownloadOutlined />{{ $t('pages.node.search.405389ab') }}</a-button
        >
        <a-dropdown v-if="nodeId">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1">
                <a-button type="primary" size="small" @click="handlerImportTemplate()">{{
                  $t('pages.node.search.9b6f4751')
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
              ><UploadOutlined /> {{ $t('pages.node.search.bc386e1b') }} <DownOutlined />
            </a-button>
          </a-upload>
        </a-dropdown>
      </template>
      <template #tableHelp>
        <a-tooltip placement="left">
          <template #title>
            <div>
              <ul>
                <li>{{ $t('pages.node.search.e9efc582') }}</li>
                <li>{{ $t('pages.node.search.e966e9a1') }}</li>
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
              :title="`${$t('pages.node.search.be1be888')}   ${
                (projectStatusMap[record.nodeId] &&
                  projectStatusMap[record.nodeId][record.projectId] &&
                  projectStatusMap[record.nodeId][record.projectId].statusMsg) ||
                ''
              }`"
            >
              <a-switch
                :checked="
                  projectStatusMap[record.nodeId] &&
                  projectStatusMap[record.nodeId][record.projectId] &&
                  projectStatusMap[record.nodeId][record.projectId].pid > 0
                "
                disabled
                :checked-children="$t('pages.node.search.7d4fd556')"
                :un-checked-children="$t('pages.node.search.3c7f1afc')"
              />
            </a-tooltip>
            <span v-else>-</span>
          </template>
        </template>

        <template v-else-if="column.dataIndex === 'port'">
          <a-tooltip
            placement="topLeft"
            :title="`${$t('pages.node.search.3cf2b4f7')}${(
              (projectStatusMap[record.nodeId] &&
                projectStatusMap[record.nodeId][record.projectId] &&
                projectStatusMap[record.nodeId][record.projectId].pids) || [
                (projectStatusMap[record.nodeId] &&
                  projectStatusMap[record.nodeId][record.projectId] &&
                  projectStatusMap[record.nodeId][record.projectId].pid) ||
                  '-'
              ]
            ).join(',')} / ${$t('pages.node.search.284af1b3')}${
              (projectStatusMap[record.nodeId] &&
                projectStatusMap[record.nodeId][record.projectId] &&
                projectStatusMap[record.nodeId][record.projectId].port) ||
              '-'
            }`"
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
            <a-button size="small" type="primary" @click="handleFile(record)">{{
              $t('pages.node.search.741604c2')
            }}</a-button>
            <template v-if="noFileModes.includes(record.runMode)">
              <a-button size="small" type="primary" @click="handleConsole(record)">{{
                $t('pages.node.search.c474c963')
              }}</a-button>
            </template>
            <template v-else>
              <a-tooltip :title="$t('pages.node.search.2e4bb15d')">
                <a-button size="small" type="primary" :disabled="true">{{
                  $t('pages.node.search.c474c963')
                }}</a-button></a-tooltip
              >
            </template>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $t('pages.node.search.3b96efcb') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <template v-if="noFileModes.includes(record.runMode)">
                      <a-button size="small" type="primary" @click="handleTrigger(record)">{{
                        $t('pages.node.search.e81c0988')
                      }}</a-button>
                    </template>
                    <template v-else>
                      <a-tooltip :title="$t('pages.node.search.7da2710e')">
                        <a-button size="small" type="primary" :disabled="true">{{
                          $t('pages.node.search.e81c0988')
                        }}</a-button></a-tooltip
                      >
                    </template>
                  </a-menu-item>
                  <a-menu-item v-if="noFileModes.includes(record.runMode)">
                    <a-button size="small" type="primary" @click="handleLogBack(record)"
                      >{{ $t('pages.node.search.afa17fb5') }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="copyItem(record)">{{
                      $t('pages.node.search.167d3e23')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record, '')">{{
                      $t('pages.node.search.f4272b09')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record, 'thorough')">{{
                      $t('pages.node.search.e12b0aaa')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="migrateWorkspace(record)">{{
                      $t('pages.node.search.d5a5b82b')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'top')"
                      >{{ $t('pages.node.search.24c6eea1') }}</a-button
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
              <a-tooltip :title="`${item.cause || $t('pages.node.search.725a49a2')}`"
                >{{ item.cause || $t('pages.node.search.725a49a2') }}
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
      :title="$t('pages.node.search.e81c0988')"
      width="50%"
      :footer="null"
      :mask-closable="false"
    >
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$t('pages.node.search.e172ddd8')">
              <a-button type="primary" size="small" @click="resetTrigger">{{
                $t('pages.node.search.6da09b16')
              }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$t('pages.node.search.47bbbde8')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('pages.node.search.40ad503e')" type="warning">
                <template #description>
                  <ul>
                    <li>{{ $t('pages.node.search.8f9bc485') }}</li>
                    <li>{{ $t('pages.node.search.21ae4cfc') }}</li>
                    <li>{{ $t('pages.node.search.789c025c') }}</li>
                  </ul>
                </template>
              </a-alert>

              <a-alert
                v-for="item in triggerUses"
                :key="item.value"
                type="info"
                :message="`${item.desc}${$t('pages.node.search.27313a78')}(${$t('pages.node.search.4c8d1a3b')})`"
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

              <a-alert
                type="info"
                :message="`${$t('pages.node.search.4bd083f4')}(${$t('pages.node.search.4c8d1a3b')})`"
              >
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
      :title="$t('pages.node.search.18b73209')"
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
        <a-form-item :label="$t('pages.node.search.580e6c10')" :help="$t('pages.node.search.ad3ec391')">
          <a-select v-model:value="temp.nodeId" allow-clear :placeholder="$t('pages.node.search.2388531c')">
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
      :title="$t('pages.node.search.d3aa1785')"
      :mask-closable="false"
      @ok="migrateWorkspaceOk"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert :message="$t('pages.node.search.40ad503e')" type="warning" show-icon>
          <template #description>
            {{ $t('pages.node.search.57868338') }}
            <ul>
              <li>
                {{ $t('pages.node.search.4822e6e') }}

                <ol>
                  <li>{{ $t('pages.node.search.67023ad') }}</li>
                  <li>{{ $t('pages.node.search.7d50dc4f') }}</li>
                </ol>
              </li>
              <li>{{ $t('pages.node.search.9718d189') }}</li>
              <li>{{ $t('pages.node.search.a15327bb') }}</li>
              <li>{{ $t('pages.node.search.eb0f266a') }}</li>
            </ul>
          </template>
        </a-alert>
        <a-alert :message="$t('pages.node.search.8e8fe98c')" type="error" show-icon>
          <template #description>
            <ul>
              <li>{{ $t('pages.node.search.9e237bfe') }}</li>
              <li>{{ $t('pages.node.search.6fb792df') }}</li>
              <li>{{ $t('pages.node.search.5e3f37cb') }}</li>
            </ul>
          </template>
        </a-alert>
      </a-space>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$t('pages.node.search.7ef9d8fb')" name="workspaceId">
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
            :placeholder="$t('pages.node.search.3a321a02')"
            @change="loadMigrateWorkspaceNodeList"
          >
            <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('pages.node.search.237e0d5a')" name="nodeId">
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
            :placeholder="$t('pages.node.search.59427711')"
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
      :title="$t('pages.node.search.111eece')"
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
          title: this.$t('pages.node.search.4eaba425'),
          dataIndex: 'projectId',
          width: 100,
          ellipsis: true
        },

        {
          title: this.$t('pages.node.search.e06912d'),
          dataIndex: 'name',
          // width: 200,
          ellipsis: true
        },
        {
          title: this.$t('pages.node.search.c12c986a'),
          dataIndex: 'group',
          sorter: true,
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.node.search.fa8d810f'),
          dataIndex: 'nodeId',
          width: 90,
          ellipsis: true
        },
        {
          title: this.$t('pages.node.search.ca637451'),
          dataIndex: 'status',
          align: 'center',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('pages.node.search.5b716424'),
          dataIndex: 'path',
          ellipsis: true,
          width: 120
        },
        {
          title: this.$t('pages.node.search.eed26c71'),
          dataIndex: 'logPath',
          ellipsis: true,
          width: 120
        },

        {
          title: this.$t('pages.node.search.5fd4d3fb'),
          dataIndex: 'port',
          width: 100,
          ellipsis: true
        },

        {
          title: this.$t('pages.node.search.7fbd7a7e'),
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
          title: this.$t('pages.node.search.f5b90169'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.node.search.3d55d8de'),
          dataIndex: 'modifyTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.node.search.916db24b'),
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          sorter: true
        },
        {
          title: this.$t('pages.node.search.f5049383'),
          dataIndex: 'sortValue',
          sorter: true,
          width: '80px'
        },
        {
          title: this.$t('pages.node.search.3bb962bf'),
          dataIndex: 'operation',
          align: 'center',
          fixed: 'right',

          width: '190px'
        }
      ],
      triggerVisible: false,
      triggerUses: [
        { desc: this.$t('pages.node.search.fc48a099'), value: 'status' },
        { desc: this.$t('pages.node.search.da6bd2c9'), value: 'start' },
        { desc: this.$t('pages.node.search.8e0aca19'), value: 'stop' },
        { desc: this.$t('pages.node.search.e6919f0c'), value: 'restart' }
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
                    error: this.$t('pages.node.search.5374a803')
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
      this.drawerTitle = `${this.$t('pages.node.search.502f94')}(${this.temp.name})`
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
      this.drawerTitle = `${this.$t('pages.node.search.c474c963')}(${this.temp.name})`
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
      this.drawerTitle = `${this.$t('pages.node.search.c7a1ee83')}(${filename})`
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
          message: this.$t('pages.node.search.b9d35477')
        })
        return
      }
      this.temp = {
        title: this.$t('pages.node.search.8080545f'),
        data: this.selectedRowKeysToId()
      }

      this.batchVisible = true
      this.batchOptInfo(0, this.$t('pages.node.search.15f9c981'), 'start')
    },
    // 批量操作
    batchOptInfo(index, msg, opt) {
      if (index >= (this.temp?.data?.length || -1)) {
        return
      }
      const value = this.temp.data[index]
      value.cause = msg + this.$t('pages.node.search.b524b645')
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
            value.cause = msg + this.$t('pages.node.search.79b90eec')
            this.updateBatchData(index, value)
            this.batchOptInfo(index + 1, msg, opt)
          })
      } else {
        value.cause = this.$t('pages.node.search.1d23b766')
        this.updateBatchData(index, value)
        this.batchOptInfo(index + 1, msg, opt)
      }
    },

    //批量重启
    batchRestart() {
      if (this.selectedRowKeys.length <= 0) {
        $notification.warning({
          message: this.$t('pages.node.search.4c9b5f8e')
        })
        return
      }
      this.temp = {
        title: this.$t('pages.node.search.170a6d47'),
        data: this.selectedRowKeysToId()
      }
      this.batchVisible = true
      this.batchOptInfo(0, this.$t('pages.node.search.69a3e8e1'), 'restart')
    },

    //批量关闭
    batchStop() {
      if (this.selectedRowKeys.length <= 0) {
        $notification.warning({
          message: this.$t('pages.node.search.ca674286')
        })
      }
      this.temp = {
        title: this.$t('pages.node.search.4974c64c'),
        data: this.selectedRowKeysToId()
      }
      this.batchVisible = true
      this.batchOptInfo(0, this.$t('pages.node.search.2f450533'), 'stop')
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
        title: this.$t('pages.node.search.d3367221'),
        zIndex: 1009,
        content: this.$t('pages.node.search.837dc86b'),
        okText: this.$t('pages.node.search.7da4a591'),
        cancelText: this.$t('pages.node.search.43105e21'),
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
        top: this.$t('pages.node.search.a8734c35'),
        up: this.$t('pages.node.search.6e79de86'),
        down: this.$t('pages.node.search.78c0cb41')
      }
      let msg = msgData[method] || this.$t('pages.node.search.23f08eca')
      if (!record.sortValue) {
        msg += `${this.$t('pages.node.search.c7b66a29')},${this.$t('pages.node.search.e5d84892')},${this.$t(
          'pages.node.search.dad3b017'
        )}`
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.projList[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      $confirm({
        title: this.$t('pages.node.search.d3367221'),
        zIndex: 1009,
        content: msg,
        okText: this.$t('pages.node.search.7da4a591'),
        cancelText: this.$t('pages.node.search.43105e21'),
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
        name: temp.name + this.$t('pages.node.search.a8ef5999'),
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
      const msg = thorough ? this.$t('pages.node.search.56e17a4c') : this.$t('pages.node.search.8ced9e02')
      const html = thorough
        ? "<b style='font-size: 24px;color:red;font-weight: bold;'>" + msg + '</b>'
        : "<b style='font-size: 20px;font-weight: bold;'>" + msg + '</b>'
      $confirm({
        title: this.$t('pages.node.search.d3367221'),
        zIndex: 1009,
        content: h('p', { innerHTML: html }, null),
        okButtonProps: { type: 'primary', danger: !!thorough, size: thorough ? 'small' : 'middle' },
        okText: this.$t('pages.node.search.7da4a591'),
        cancelText: this.$t('pages.node.search.43105e21'),
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
          message: this.$t('pages.node.search.3a321a02')
        })
        return false
      }
      if (!this.temp.nodeId) {
        $notification.warn({
          message: this.$t('pages.node.search.59427711')
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
