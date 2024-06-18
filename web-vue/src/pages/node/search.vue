<template>
  <div class="">
    <template v-if="useSuggestions">
      <a-result :title="$t('i18n_a396da3e22')" :sub-title="$t('i18n_13d947ea19')"> </a-result>
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
            :placeholder="$t('i18n_f8a613d247')"
            class="search-input-item"
          >
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.group"
            allow-clear
            :placeholder="$t('i18n_ec22193ed1')"
            class="search-input-item"
            @change="getNodeProjectData"
          >
            <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
          </a-select>
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_1e93bdad2a')"
            class="search-input-item"
            @press-enter="getNodeProjectData"
          />
          <a-input
            v-model:value="listQuery['%projectId%']"
            :placeholder="$t('i18n_47dd8dbc7d')"
            class="search-input-item"
            @press-enter="getNodeProjectData"
          />

          <a-select
            v-model:value="listQuery.runMode"
            allow-clear
            :placeholder="$t('i18n_17d444b642')"
            class="search-input-item"
          >
            <a-select-option v-for="item in runModeList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button :loading="loading" type="primary" @click="getNodeProjectData">{{
              $t('i18n_e5f71fc31e')
            }}</a-button>
          </a-tooltip>

          <!-- <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="silenceLoadData" /> -->
        </a-space>
      </template>
      <template #toolPrefix>
        <a-dropdown v-if="selectedRowKeys && selectedRowKeys.length">
          <a-button type="primary" size="small"> {{ $t('i18n_7f7c624a84') }} <DownOutlined /> </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item>
                <a-button type="primary" @click="batchStart">{{ $t('i18n_93e894325d') }}</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="primary" @click="batchRestart">{{ $t('i18n_73651ba2db') }}</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="primary" danger @click="batchStop">{{ $t('i18n_a03c00714f') }}</a-button>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-button v-else type="primary" size="small" :disabled="true">
          {{ $t('i18n_7f7c624a84') }} <DownOutlined />
        </a-button>

        <a-button type="primary" size="small" @click="openAdd"><PlusOutlined />{{ $t('i18n_66ab5e9f24') }}</a-button>
        <template v-if="!nodeId">
          <a-dropdown v-if="nodeMap && Object.keys(nodeMap).length">
            <a-button type="primary" size="small" danger> {{ $t('i18n_6a620e3c07') }} <DownOutlined /></a-button>
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
          <SyncOutlined />{{ $t('i18n_6a620e3c07') }}
        </a-button>

        <a-button v-if="nodeId" size="small" type="primary" @click="handlerExportData()"
          ><DownloadOutlined />{{ $t('i18n_55405ea6ff') }}</a-button
        >
        <a-dropdown v-if="nodeId">
          <template #overlay>
            <a-menu>
              <a-menu-item key="1">
                <a-button type="primary" size="small" @click="handlerImportTemplate()">{{
                  $t('i18n_2e505d23f7')
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
              ><UploadOutlined /> {{ $t('i18n_8d9a071ee2') }} <DownOutlined />
            </a-button>
          </a-upload>
        </a-dropdown>
      </template>
      <template #tableHelp>
        <a-tooltip placement="left">
          <template #title>
            <div>
              <ul>
                <li>{{ $t('i18n_2141ffaec9') }}</li>
                <li>{{ $t('i18n_74d980d4f4') }}</li>
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
              :title="`${$t('i18n_d7bebd0e5e')}   ${
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
                :checked-children="$t('i18n_8493205602')"
                :un-checked-children="$t('i18n_d58a55bcee')"
              />
            </a-tooltip>
            <span v-else>-</span>
          </template>
        </template>

        <template v-else-if="column.dataIndex === 'port'">
          <a-tooltip
            placement="topLeft"
            :title="`${$t('i18n_2b04210d33')}${(
              (projectStatusMap[record.nodeId] &&
                projectStatusMap[record.nodeId][record.projectId] &&
                projectStatusMap[record.nodeId][record.projectId].pids) || [
                (projectStatusMap[record.nodeId] &&
                  projectStatusMap[record.nodeId][record.projectId] &&
                  projectStatusMap[record.nodeId][record.projectId].pid) ||
                  '-'
              ]
            ).join(',')} / ${$t('i18n_4c096c51a3')}${
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
            <a-button size="small" type="primary" @click="handleFile(record)">{{ $t('i18n_2a0c4740f1') }}</a-button>
            <template v-if="noFileModes.includes(record.runMode)">
              <a-button size="small" type="primary" @click="handleConsole(record)">{{
                $t('i18n_b5c3770699')
              }}</a-button>
            </template>
            <template v-else>
              <a-tooltip :title="$t('i18n_904615588b')">
                <a-button size="small" type="primary" :disabled="true">{{ $t('i18n_b5c3770699') }}</a-button></a-tooltip
              >
            </template>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $t('i18n_0ec9eaf9c3') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <template v-if="noFileModes.includes(record.runMode)">
                      <a-button size="small" type="primary" @click="handleTrigger(record)">{{
                        $t('i18n_4696724ed3')
                      }}</a-button>
                    </template>
                    <template v-else>
                      <a-tooltip :title="$t('i18n_4e54369108')">
                        <a-button size="small" type="primary" :disabled="true">{{
                          $t('i18n_4696724ed3')
                        }}</a-button></a-tooltip
                      >
                    </template>
                  </a-menu-item>
                  <a-menu-item v-if="noFileModes.includes(record.runMode)">
                    <a-button size="small" type="primary" @click="handleLogBack(record)"
                      >{{ $t('i18n_2926598213') }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="copyItem(record)">{{
                      $t('i18n_79d3abe929')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record, '')">{{
                      $t('i18n_c0f4a31865')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record, 'thorough')">{{
                      $t('i18n_7327966572')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="migrateWorkspace(record)">{{
                      $t('i18n_3adb55fbb5')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'top')"
                      >{{ $t('i18n_3d43ff1199') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'up')"
                      >{{ $t('i18n_315eacd193') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total"
                      @click="sortItemHander(record, index, 'down')"
                      >{{ $t('i18n_17acd250da') }}</a-button
                    >
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 项目文件组件 -->
    <CustomDrawer
      v-if="drawerFileVisible"
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
    </CustomDrawer>
    <!-- 项目控制台组件 -->
    <CustomDrawer
      v-if="drawerConsoleVisible"
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
    </CustomDrawer>
    <!-- 项目跟踪文件组件 -->
    <CustomDrawer
      v-if="drawerReadFileVisible"
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
    </CustomDrawer>
    <!-- 批量操作状态 -->
    <CustomModal
      v-if="batchVisible"
      v-model:open="batchVisible"
      destroy-on-close
      :title="temp.title"
      :footer="null"
      @cancel="batchClose"
    >
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
              <a-tooltip :title="`${item.cause || $t('i18n_dd4e55c39c')}`"
                >{{ item.cause || $t('i18n_dd4e55c39c') }}
              </a-tooltip>
            </div>
          </a-list-item>
        </template>
      </a-list>
    </CustomModal>
    <!-- 触发器 -->
    <CustomModal
      v-if="triggerVisible"
      v-model:open="triggerVisible"
      destroy-on-close
      :title="$t('i18n_4696724ed3')"
      width="50%"
      :footer="null"
      :mask-closable="false"
    >
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$t('i18n_01ad26f4a9')">
              <a-button type="primary" size="small" @click="resetTrigger">{{ $t('i18n_4b9c3271dc') }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$t('i18n_1a6aa24e76')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('i18n_947d983961')" type="warning">
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_9a00e13160') }}</li>
                    <li>{{ $t('i18n_632a907224') }}</li>
                    <li>{{ $t('i18n_cb9b3ec760') }}</li>
                  </ul>
                </template>
              </a-alert>

              <a-alert
                v-for="item in triggerUses"
                :key="item.value"
                type="info"
                :message="`${item.desc}${$t('i18n_b9a4098131')}(${$t('i18n_00a070c696')})`"
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

              <a-alert type="info" :message="`${$t('i18n_8d202b890c')}(${$t('i18n_00a070c696')})`">
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
    </CustomModal>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editProjectVisible"
      v-model:open="editProjectVisible"
      destroy-on-close
      width="60vw"
      :title="$t('i18n_bd49bc196c')"
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
        <a-form-item :label="$t('i18n_7e2b40fc86')" :help="$t('i18n_dd23fdf796')">
          <a-select v-model:value="temp.nodeId" allow-clear :placeholder="$t('i18n_f8a613d247')">
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
    </CustomModal>
    <!-- 迁移到其他工作空间 -->
    <CustomModal
      v-if="migrateWorkspaceVisible"
      v-model:open="migrateWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50vw"
      :title="$t('i18n_c6f6a9b234')"
      :mask-closable="false"
      @ok="migrateWorkspaceOk"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert :message="$t('i18n_947d983961')" type="warning" show-icon>
          <template #description>
            {{ $t('i18n_8e6184c0d3') }}
            <ul>
              <li>
                {{ $t('i18n_829706defc') }}

                <ol>
                  <li>{{ $t('i18n_3c070ea334') }}</li>
                  <li>{{ $t('i18n_d84323ba8d') }}</li>
                </ol>
              </li>
              <li>{{ $t('i18n_cb28aee4f0') }}</li>
              <li>{{ $t('i18n_44ef546ded') }}</li>
              <li>{{ $t('i18n_cb46672712') }}</li>
            </ul>
          </template>
        </a-alert>
        <a-alert :message="$t('i18n_b15689296a')" type="error" show-icon>
          <template #description>
            <ul>
              <li>{{ $t('i18n_24ad6f3354') }}</li>
              <li>{{ $t('i18n_8f0c429b46') }}</li>
              <li>{{ $t('i18n_471c6b19cf') }}</li>
            </ul>
          </template>
        </a-alert>
      </a-space>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$t('i18n_b4a8c78284')" name="workspaceId">
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
            :placeholder="$t('i18n_b3bda9bf9e')"
            @change="loadMigrateWorkspaceNodeList"
          >
            <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('i18n_6953a488e3')" name="nodeId">
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
            :placeholder="$t('i18n_1d53247d61')"
          >
            <a-select-option v-for="item in migrateWorkspaceNodeList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 日志备份 -->
    <CustomModal
      v-if="lobbackVisible"
      v-model:open="lobbackVisible"
      destroy-on-close
      :title="$t('i18n_15f01c43e8')"
      width="850px"
      :footer="null"
      :mask-closable="false"
    >
      <ProjectLog v-if="lobbackVisible" :node-id="temp.nodeId" :project-id="temp.projectId"></ProjectLog>
    </CustomModal>
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
          title: this.$t('i18n_33c9e2388e'),
          dataIndex: 'projectId',
          width: 100,
          ellipsis: true
        },

        {
          title: this.$t('i18n_738a41f965'),
          dataIndex: 'name',
          // width: 200,
          ellipsis: true
        },
        {
          title: this.$t('i18n_d438e83c16'),
          dataIndex: 'group',
          sorter: true,
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_b1785ef01e'),
          dataIndex: 'nodeId',
          width: 90,
          ellipsis: true
        },
        {
          title: this.$t('i18n_e4b51d5cd0'),
          dataIndex: 'status',
          align: 'center',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_aabdc3b7c0'),
          dataIndex: 'path',
          ellipsis: true,
          width: 120
        },
        {
          title: this.$t('i18n_03a74a9a8a'),
          dataIndex: 'logPath',
          ellipsis: true,
          width: 120
        },

        {
          title: this.$t('i18n_504c43b70a'),
          dataIndex: 'port',
          width: 100,
          ellipsis: true
        },

        {
          title: this.$t('i18n_17d444b642'),
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
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          sorter: true
        },
        {
          title: this.$t('i18n_c35c1a1330'),
          dataIndex: 'sortValue',
          sorter: true,
          width: '80px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          align: 'center',
          fixed: 'right',

          width: '190px'
        }
      ],

      triggerVisible: false,
      triggerUses: [
        { desc: this.$t('i18n_0e16902c1e'), value: 'status' },
        { desc: this.$t('i18n_6b29a6e523'), value: 'start' },
        { desc: this.$t('i18n_d75c02d050'), value: 'stop' },
        { desc: this.$t('i18n_f3e93355ee'), value: 'restart' }
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
                    error: this.$t('i18n_44ed625b19')
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
      this.drawerTitle = `${this.$t('i18n_8780e6b3d1')}(${this.temp.name})`
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
      this.drawerTitle = `${this.$t('i18n_b5c3770699')}(${this.temp.name})`
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
      this.drawerTitle = `${this.$t('i18n_5854370b86')}(${filename})`
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
          message: this.$t('i18n_576669e450')
        })
        return
      }
      this.temp = {
        title: this.$t('i18n_93e894325d'),
        data: this.selectedRowKeysToId()
      }

      this.batchVisible = true
      this.batchOptInfo(0, this.$t('i18n_8e54ddfe24'), 'start')
    },
    // 批量操作
    batchOptInfo(index, msg, opt) {
      if (index >= (this.temp?.data?.length || -1)) {
        return
      }
      const value = this.temp.data[index]
      value.cause = msg + this.$t('i18n_aed1dfbc31')
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
            value.cause = msg + this.$t('i18n_acd5cb847a')
            this.updateBatchData(index, value)
            this.batchOptInfo(index + 1, msg, opt)
          })
      } else {
        value.cause = this.$t('i18n_92636e8c8f')
        this.updateBatchData(index, value)
        this.batchOptInfo(index + 1, msg, opt)
      }
    },

    //批量重启
    batchRestart() {
      if (this.selectedRowKeys.length <= 0) {
        $notification.warning({
          message: this.$t('i18n_03580275cb')
        })
        return
      }
      this.temp = {
        title: this.$t('i18n_7737f088de'),
        data: this.selectedRowKeysToId()
      }
      this.batchVisible = true
      this.batchOptInfo(0, this.$t('i18n_01b4e06f39'), 'restart')
    },

    //批量关闭
    batchStop() {
      if (this.selectedRowKeys.length <= 0) {
        $notification.warning({
          message: this.$t('i18n_373a1efdc0')
        })
      }
      this.temp = {
        title: this.$t('i18n_b30d07c036'),
        data: this.selectedRowKeysToId()
      }
      this.batchVisible = true
      this.batchOptInfo(0, this.$t('i18n_095e938e2a'), 'stop')
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_24384ba6c1'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
        top: this.$t('i18n_0079d91f95'),
        up: this.$t('i18n_b166a66d67'),
        down: this.$t('i18n_7a7e25e9eb')
      }
      let msg = msgData[method] || this.$t('i18n_49574eee58')
      if (!record.sortValue) {
        msg += `${this.$t('i18n_57c0a41ec6')},${this.$t('i18n_066f903d75')},${this.$t('i18n_c4e2cd2266')}`
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.projList[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: msg,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
        name: temp.name + this.$t('i18n_0428b36ab1'),
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
      const msg = thorough ? this.$t('i18n_8580ad66b0') : this.$t('i18n_954fb7fa21')
      const html = thorough
        ? "<b style='font-size: 24px;color:red;font-weight: bold;'>" + msg + '</b>'
        : "<b style='font-size: 20px;font-weight: bold;'>" + msg + '</b>'
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: h('p', { innerHTML: html }, null),
        okButtonProps: { type: 'primary', danger: !!thorough, size: thorough ? 'small' : 'middle' },
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
          message: this.$t('i18n_b3bda9bf9e')
        })
        return false
      }
      if (!this.temp.nodeId) {
        $notification.warn({
          message: this.$t('i18n_1d53247d61')
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
