<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      table-name="assets-docker-list"
      :empty-description="$tl('p.noAssetDocker')"
      :active-page="activePage"
      size="middle"
      :data-source="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$tl('c.name')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%host%']"
            placeholder="host"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%swarmId%']"
            :placeholder="$tl('p.clusterId')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.groupName"
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
            allow-clear
            :placeholder="$tl('c.group')"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip :title="$tl('p.quickBackFirstPage')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $tl('p.add') }}</a-button>
          <a-button :disabled="!tableSelections.length" type="primary" @click="syncToWorkspaceShow()">
            {{ $tl('p.batchAssign') }}</a-button
          >
          <a-tooltip :title="$tl('p.autoDetectDocker')">
            <a-button type="dashed" @click="handleTryLocalDocker">
              <QuestionCircleOutlined />{{ $tl('p.autoProbe') }}
            </a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip :title="text">
            <a-button style="padding: 0" type="link" size="small" @click="handleEdit(record)"> {{ text }}</a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'swarmId'">
          <template v-if="text">
            <a-tooltip v-if="record.swarmControlAvailable" :title="$tl('c.manageNode')">
              <ClusterOutlined />
            </a-tooltip>
            <a-tooltip v-else :title="$tl('c.workNode')">
              <BlockOutlined />
            </a-tooltip>
            <a-popover :title="$tl('p.clusterInfo')">
              <template #content>
                <p>{{ $tl('p.clusterIdLabel') }}{{ record.swarmId }}</p>
                <p>{{ $tl('p.currentNodeId') }}{{ record.swarmNodeId }}</p>
                <p>{{ $tl('p.currentNodeAddress') }}{{ record.swarmNodeAddr }}</p>
                <p>{{ $tl('p.clusterCreateTime') }}{{ parseTime(record.swarmCreatedAt) }}</p>
                <p>{{ $tl('p.clusterUpdateTime') }}{{ parseTime(record.swarmUpdatedAt) }}</p>
              </template>
              {{ text }}
            </a-popover>
          </template>
        </template>

        <template v-else-if="column.dataIndex === 'tlsVerify'">
          <a-tooltip
            placement="topLeft"
            :title="record.tlsVerify ? $tl('p.tlsEnableCertInfo') + record.certInfo : $tl('p.tlsDisable')"
          >
            <template v-if="record.tlsVerify">
              <template v-if="record.certExist">
                <a-switch
                  v-model:checked="record.tlsVerify"
                  size="small"
                  :disabled="true"
                  :checked-children="$tl('c.statusOn')"
                  :un-checked-children="$tl('c.statusOff')"
                />
              </template>
              <a-tag v-else color="red"> {{ $tl('p.certLost') }} </a-tag>
            </template>
            <template v-else>
              <a-switch
                v-model:checked="record.tlsVerify"
                size="small"
                :disabled="true"
                :checked-children="$tl('c.statusOn')"
                :un-checked-children="$tl('c.statusOff')"
              />
            </template>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="record.failureMsg">
            <a-tag :color="statusMap[record.status].color">{{
              statusMap[record.status].desc || $tl('p.unknown')
            }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" :disabled="record.status !== 1" @click="handleConsole(record)">{{
              $tl('p.console')
            }}</a-button>
            <template v-if="!record.swarmId && record.status === 1">
              <a-popover :title="$tl('p.clusterOperation')">
                <template #content>
                  <p>
                    <a-button size="small" type="primary" @click="initSwarm(record)">{{
                      $tl('p.createCluster')
                    }}</a-button>
                  </p>
                  <p>
                    <a-button size="small" type="primary" @click="joinSwarm(record)">{{
                      $tl('p.joinCluster')
                    }}</a-button>
                  </p>
                </template>
                <a-button size="small" type="primary"><EditOutlined />{{ $tl('c.cluster') }}</a-button>
              </a-popover>
            </template>
            <template v-else>
              <a-button
                size="small"
                :disabled="parseInt(record.status) !== 1"
                type="primary"
                @click="handleSwarmConsole(record)"
                ><SelectOutlined />{{ $tl('c.cluster') }}</a-button
              >
            </template>
            <a-button size="small" type="primary" @click="syncToWorkspaceShow(record)">{{ $tl('p.assign') }}</a-button>
            <a-button size="small" type="primary" @click="viewWorkspaceDataHander(record)">{{
              $tl('p.associate')
            }}</a-button>
            <a-dropdown>
              <a @click="(e) => e.preventDefault()"> {{ $tl('p.more') }} <DownOutlined /> </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('p.edit') }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                      $tl('p.delete')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      :disabled="!record.swarmId || record.status !== 1"
                      type="primary"
                      danger
                      @click="handleLeaveForce(record)"
                      >{{ $tl('p.exitCluster') }}</a-button
                    >
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      width="50%"
      :title="$tl('p.editDocker')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-space direction="vertical" style="width: 100%">
          <a-alert banner>
            <template #message>
              <template v-if="temp.enableSsh">
                <ul>
                  <li>SSH {{ $tl('p.sshConnectInfo') }}</li>
                  <li>docker {{ $tl('p.versionRequirement') }}.09 {{ $tl('p.sshConnectAvailable') }}</li>
                  <li>{{ $tl('p.sshNoDockerDetected') }}</li>
                  <li>
                    {{ $tl('p.sshDockerNotDetected') }}>{{ $tl('p.modifyConfigFile') }} <b>ssh/monitor-script.sh</b>
                    {{ $tl('p.compatibleMachine') }}
                  </li>
                </ul>
              </template>
              <template v-else>
                <ul>
                  <li>
                    {{ $tl('p.useDockerHttpInterface') }}<b style="color: red">{{ $tl('p.noAuthentication') }}</b>
                  </li>
                  <li>
                    {{ $tl('p.makesItUnsafe') }}
                    <b style="color: red">docker {{ $tl('p.extremelyUnsafe') }}</b>
                  </li>
                  <li>
                    {{ $tl('p.exposedToInternet') }}<b style="color: red"> {{ $tl('p.easyToMine') }} </b>
                  </li>
                  <li>
                    {{ $tl('p.stronglyRecommendTLS') }}<b style="color: red">{{ $tl('p.useTLSCertificate') }}</b
                    >（{{ $tl('p.certificateGeneration') }}
                  </li>
                  <li>
                    {{ $tl('p.ifPortIsInternal') }}<b style="color: red">{{ $tl('p.ignoreTLSCertificate') }}</b>
                  </li>
                  <li>
                    {{ $tl('p.note') }}<b style="color: red">{{ $tl('p.ipMustMatchDockerHost') }}</b>
                  </li>
                </ul>
              </template>
            </template>
          </a-alert>
          <div></div>
        </a-space>
        <a-form-item :label="$tl('c.containerName')" name="name">
          <a-input v-model:value="temp.name" :placeholder="$tl('c.containerName')" />
        </a-form-item>
        <a-form-item :label="$tl('c.group')" name="groupName">
          <custom-select
            v-model:value="temp.groupName"
            :data="groupList"
            :input-placeholder="$tl('p.addNewGroup')"
            :select-placeholder="$tl('p.selectGroupName')"
          >
          </custom-select>
        </a-form-item>
        <a-form-item :label="$tl('p.enableSSHAccess')" name="enableSsh">
          <a-switch
            v-model:checked="temp.enableSsh"
            :checked-children="$tl('c.statusOn')"
            :un-checked-children="$tl('c.statusOff')"
          />
        </a-form-item>
        <a-form-item v-if="temp.enableSsh" :label="$tl('c.sshConnectionInfo')" name="enableSsh">
          <a-select
            v-model:value="temp.machineSshId"
            allow-clear
            :placeholder="$tl('c.sshConnectionInfo')"
            class="search-input-item"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :disabled="!item.dockerInfo" :value="item.id">
              <a-tooltip :title="`${item.name}(${item.host})`">
                <template #title>
                  {{ item.name }}({{ item.host }})[{{
                    (item.dockerInfo && JSON.parse(item.dockerInfo) && JSON.parse(item.dockerInfo).version) ||
                    $tl('c.dockerMonitoringDisabled')
                  }}]
                </template>
                {{ item.name }}({{ item.host }}) [{{
                  (item.dockerInfo && JSON.parse(item.dockerInfo) && JSON.parse(item.dockerInfo).version) ||
                  $tl('c.dockerMonitoringDisabled')
                }}]</a-tooltip
              >
            </a-select-option>
          </a-select>
          <template #help>{{ $tl('p.sshMonitoringForDocker') }}</template>
        </a-form-item>
        <template v-if="!temp.enableSsh">
          <a-form-item label="host" name="host">
            <a-input v-model:value="temp.host" :placeholder="`${$tl('p.containerAddressTCP')}://127.0.0.1:2375`" />
          </a-form-item>

          <a-form-item :label="$tl('p.tlsAuthentication')" name="tlsVerify">
            <a-switch
              v-model:checked="temp.tlsVerify"
              :checked-children="$tl('c.statusOn')"
              :un-checked-children="$tl('c.statusOff')"
            />
          </a-form-item>
          <a-form-item
            v-if="temp.tlsVerify"
            :label="$tl('p.certificateInfo')"
            name="certInfo"
            :help="$tl('p.uploadOrSelectCertificate')"
          >
            <a-input-search
              v-model:value="temp.certInfo"
              :placeholder="$tl('p.enterOrSelectCertificateInfo')"
              :enter-button="$tl('p.selectCertificate')"
              @search="
                () => {
                  certificateVisible = true
                }
              "
            />
          </a-form-item>
        </template>

        <a-collapse>
          <a-collapse-panel key="1" :header="$tl('p.otherConfiguration')">
            <a-form-item :label="$tl('p.timeout')" name="heartbeatTimeout">
              <a-input-number
                v-model:value="temp.heartbeatTimeout"
                style="width: 100%"
                :placeholder="$tl('p.timeoutInSeconds')"
              />
            </a-form-item>
            <a-form-item :label="$tl('c.repoAddress')" name="registryUrl">
              <a-input v-model:value="temp.registryUrl" :placeholder="$tl('c.repoAddress')" />
            </a-form-item>
            <a-form-item :label="$tl('c.repoAccount')" name="registryUsername">
              <a-input v-model:value="temp.registryUsername" :placeholder="$tl('c.repoAccount')" />
            </a-form-item>
            <a-form-item :label="$tl('c.repoPassword')" name="registryPassword">
              <a-input-password v-model:value="temp.registryPassword" :placeholder="$tl('c.repoPassword')" />
            </a-form-item>
            <a-form-item :label="$tl('c.accountEmail')" name="registryEmail">
              <a-input v-model:value="temp.registryEmail" :placeholder="$tl('c.accountEmail')" />
            </a-form-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form>
    </a-modal>
    <!-- 创建集群 -->
    <a-modal
      v-model:open="initSwarmVisible"
      destroy-on-close
      :title="$tl('p.createDockerCluster')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleSwarm"
    >
      <a-form ref="initForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-alert :message="$tl('p.reminder')" type="warning">
          <template #description> {{ $tl('p.createClusterBehavior') }} </template>
        </a-alert>
      </a-form>
    </a-modal>
    <!-- 加入集群 -->
    <a-modal
      v-model:open="joinSwarmVisible"
      destroy-on-close
      :title="$tl('p.joinDockerCluster')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleSwarmJoin"
    >
      <a-form ref="joinForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$tl('p.selectCluster')" name="managerId">
          <a-select
            v-model:value="temp.managerId"
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
            allow-clear
            :placeholder="$tl('p.joinToCluster')"
            @change="
              (v) => {
                tempList = swarmList.filter((item) => {
                  return item.id === v
                })
                if (tempList.length) {
                  temp = { ...temp, remoteAddr: tempList[0].swarmNodeAddr }
                } else {
                  temp = { ...temp, remoteAddr: '' }
                }
              }
            "
          >
            <a-select-option v-for="item in swarmList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item v-if="temp.remoteAddr" :label="$tl('p.clusterIP')" name="remoteAddr"
          ><a-input v-model:value="temp.remoteAddr" :placeholder="$tl('p.containerLabel')" />
        </a-form-item>

        <a-form-item :label="$tl('p.role')" name="role">
          <a-radio-group v-model:value="temp.role" name="role">
            <a-radio value="worker"> {{ $tl('c.workNode') }}</a-radio>
            <a-radio value="manager"> {{ $tl('c.manageNode') }} </a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 控制台 -->

    <console
      v-if="consoleVisible"
      :visible="consoleVisible"
      :machine-docker-id="temp.id"
      url-prefix="/system/assets/docker"
      @close="onClose"
    ></console>
    <!-- </a-drawer> -->
    <!-- 集群控制台 -->

    <swarm-console
      v-if="swarmConsoleVisible"
      :id="temp.id"
      :visible="swarmConsoleVisible"
      :init-menu="temp.menuKey"
      url-prefix="/system/assets"
      @close="onSwarmClose"
    ></swarm-console>

    <!-- 分配到其他工作空间 -->
    <a-modal
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$tl('p.assignToOtherWorkspace')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$tl('p.assignmentType')" name="type">
          <a-radio-group v-model:value="temp.type">
            <a-radio value="docker"> docker </a-radio>
            <a-radio value="swarm" :disabled="temp.swarmId === true ? false : true"> {{ $tl('c.cluster') }} </a-radio>
          </a-radio-group>
        </a-form-item>
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
          >
            <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 查看 docker 关联工作空间的信息 -->
    <a-modal
      v-model:open="viewWorkspaceDocker"
      destroy-on-close
      width="50%"
      :title="$tl('p.associatedWorkspaceDocker')"
      :footer="null"
      :mask-closable="false"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert
          v-if="
            workspaceDockerData && (workspaceDockerData.dockerList?.length || workspaceDockerData.swarmList?.length)
          "
          :message="$tl('p.cannotDeleteDirectly')"
          type="info"
          show-icon
        />
        <a-tabs>
          <a-tab-pane key="1" tab="docker">
            <a-list bordered :data-source="workspaceDockerData && workspaceDockerData.dockerList">
              <template #renderItem="{ item }">
                <a-list-item style="display: block">
                  <a-row>
                    <a-col :span="10">Docker {{ $tl('p.name') }}{{ item.name }}</a-col>
                    <a-col :span="10">{{ $tl('c.belongWorkspace') }}{{ item.workspace && item.workspace.name }}</a-col>
                    <a-col :span="4"> </a-col>
                  </a-row>
                </a-list-item>
              </template>
            </a-list>
          </a-tab-pane>
          <a-tab-pane key="2" :tab="$tl('c.cluster')">
            <a-list bordered :data-source="workspaceDockerData && workspaceDockerData.swarmList">
              <template #renderItem="{ item }">
                <a-list-item style="display: block">
                  <a-row>
                    <a-col :span="10">{{ $tl('p.clusterName') }}{{ item.name }}</a-col>
                    <a-col :span="10">{{ $tl('c.belongWorkspace') }}{{ item.workspace && item.workspace.name }}</a-col>
                    <a-col :span="4"> </a-col>
                  </a-row>
                </a-list-item>
              </template>
            </a-list>
          </a-tab-pane>
        </a-tabs>
      </a-space>
    </a-modal>
    <!-- 选择证书文件 -->
    <a-drawer
      v-if="certificateVisible"
      destroy-on-close
      :title="`${$tl('p.selectCertificateFile')}`"
      placement="right"
      :open="certificateVisible"
      width="85vw"
      :z-index="1009"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          certificateVisible = false
        }
      "
    >
      <certificate
        v-if="certificateVisible"
        ref="certificate"
        :show-all="true"
        @confirm="
          (certInfo) => {
            temp = { ...temp, certInfo: certInfo }
            certificateVisible = false
          }
        "
        @cancel="
          () => {
            certificateVisible = false
          }
        "
      ></certificate>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                certificateVisible = false
              }
            "
          >
            {{ $tl('c.cancel') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['certificate'].handerConfirm()
              }
            "
          >
            {{ $tl('c.confirm') }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>

<script>
import {
  dockerList,
  editDocker,
  tryLocalDocker,
  deleteDcoker,
  initDockerSwarm,
  joinDockerSwarm,
  dockerSwarmListAll,
  dcokerSwarmLeaveForce,
  machineDockerDistribute,
  dockerListWorkspace,
  dockerListGroup,
  statusMap
} from '@/api/system/assets-docker'
import { machineSshListData } from '@/api/system/assets-ssh'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { getWorkSpaceListAll } from '@/api/workspace'
import Console from '@/pages/docker/console'
import SwarmConsole from '@/pages/docker/swarm/console.vue'

import certificate from '@/pages/certificate/list.vue'
import CustomSelect from '@/components/customSelect'

export default {
  components: {
    Console,
    SwarmConsole,
    certificate,
    CustomSelect
  },
  props: {},
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      statusMap,
      list: [],
      groupList: [],
      temp: {},
      editVisible: false,
      templateVisible: false,
      consoleVisible: false,
      swarmConsoleVisible: false,
      initSwarmVisible: false,
      joinSwarmVisible: false,
      swarmList: [],
      sshList: [],
      columns: [
        {
          title: this.$tl('c.name'),
          dataIndex: 'name',
          ellipsis: true,

          width: 150
        },
        {
          title: 'host',
          dataIndex: 'host',
          ellipsis: true,
          tooltip: true,

          width: 150
        },
        {
          title: `docker${this.$tl('p.version')}`,
          dataIndex: 'dockerVersion',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },

        {
          title: this.$tl('p.status'),
          dataIndex: 'status',
          ellipsis: true,
          align: 'center',
          width: '100px'
        },
        {
          title: 'TLS',
          dataIndex: 'tlsVerify',
          width: '80px',
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$tl('p.groupName'),
          dataIndex: 'groupName',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: this.$tl('c.cluster'),
          dataIndex: 'swarmId',
          ellipsis: true
        },
        // { title: "apiVersion", dataIndex: "apiVersion", width: 100, ellipsis: true, },
        {
          title: this.$tl('p.lastModifier'),
          dataIndex: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$tl('p.createTime'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.updateTime'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',

          fixed: 'right',
          align: 'center',
          width: '320px'
        }
      ],
      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: this.$tl('p.containerName'), trigger: 'blur' }],
        // host: [{ required: true, message: "请填写容器地址", trigger: "blur" }],

        managerId: [
          {
            required: true,
            message: this.$tl('p.clusterToJoin'),
            trigger: 'blur'
          }
        ],
        role: [{ required: true, message: this.$tl('p.nodeRole'), trigger: 'blur' }],
        remoteAddr: [
          { required: true, message: this.$tl('p.clusterIp'), trigger: 'blur' },
          {
            pattern:
              /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/,
            message: this.$tl('p.correctIp')
          }
        ]
      },
      syncToWorkspaceVisible: false,
      workspaceList: [],
      viewWorkspaceDocker: false,
      workspaceDockerData: {
        dockerList: [],
        swarmList: []
      },
      tableSelections: [],
      certificateVisible: false,
      confirmLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections
      }
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  mounted() {
    this.sshListData()
    this.loadData()
    this.loadGroupList()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.system.assets.docker.list.${key}`, ...args)
    },
    //
    parseTime,
    // 获取所有的分组
    loadGroupList() {
      dockerListGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    sshListData() {
      machineSshListData().then((res) => {
        if (res.code === 200) {
          this.sshList = res.data.result
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page

      dockerList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
          //
          const dockerId = this.$route.query.dockerId
          const type = this.$route.query.type
          this.list.map((item) => {
            if (dockerId === item.id && type === 'docker') {
              this.handleConsole(item)
            } else if (dockerId === item.id && type === 'swarm') {
              this.handleSwarmConsole(item)
            }
          })
        }
        this.loading = false
      })
    },
    // 新增
    handleAdd() {
      this.temp = {}
      this.editVisible = true

      this.$refs['editForm']?.resetFields()
    },
    // 控制台
    handleConsole(record) {
      this.temp = { ...record }
      this.consoleVisible = true

      let dockerId = this.$route.query.dockerId
      if (dockerId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, dockerId: record.id, type: 'docker' }
        })
      }
    },
    // 集群控制台
    handleSwarmConsole(record) {
      this.temp = { ...record }
      this.swarmConsoleVisible = true
      let dockerId = this.$route.query.dockerId
      if (dockerId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, dockerId: record.id, type: 'swarm' }
        })
      }
    },
    // 关闭抽屉层
    onClose() {
      this.consoleVisible = false
      const query = Object.assign({}, this.$route.query)
      delete query.dockerId
      delete query.type
      this.$router.replace({
        query: query
      })
    },
    onSwarmClose() {
      this.swarmConsoleVisible = false
      const query = Object.assign({}, this.$route.query)
      delete query.dockerId
      delete query.type
      this.$router.replace({
        query: query
      })
    },
    // 修改
    handleEdit(record) {
      this.temp = { ...record }
      this.editVisible = true

      // this.temp = { ...this.temp };

      this.$refs['editForm']?.resetFields()
    },

    // 提交  数据
    handleEditOk() {
      // 检验表单

      this.$refs['editForm'].validate().then(() => {
        const temp = Object.assign({}, this.temp)
        if (temp.enableSsh) {
          if (!temp.machineSshId) {
            $message.warning(this.$tl('p.sshConnectionInfo'))
            return false
          }
        } else {
          if (!temp.host) {
            $message.warning(this.$tl('p.hostInput'))
            return false
          }
        }
        this.confirmLoading = true
        editDocker(temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.editVisible = false
              this.loadData()
              this.loadGroupList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$tl('c.systemTip'),
        zIndex: 1009,
        content: this.$tl('p.confirmDockerDeletion'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return deleteDcoker({
            id: record.id
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 强制解绑
    handleLeaveForce(record) {
      const html = `
      <h1 style='color:red;'>${this.$tl('p.confirmForceExitCluster')}</h1>
      <h3 style='color:red;'>${this.$tl('p.dataInconsistencyWarning')}</h3>
      <ul style='color:red;'>
        <li>${this.$tl('p.backupDataWarning')}</li>
        <li>${this.$tl('p.backupDataWarning')}</li>
        <li style='font-weight: bold;'>${this.$tl('p.avoidExitingManagerNode')}</li>
        <li>${this.$tl('p.operationIrreversible')}</li>
      </ul>
      `
      $confirm({
        title: this.$tl('c.systemTip'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return dcokerSwarmLeaveForce({
            id: record.id
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },

    // 创建集群
    initSwarm(record) {
      this.temp = {
        id: record.id
      }
      this.initSwarmVisible = true
      this.$refs['initForm']?.resetFields()
    },
    handleSwarm() {
      this.$refs['initForm'].validate().then(() => {
        this.confirmLoading = true
        initDockerSwarm(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.initSwarmVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 加入集群
    joinSwarm(record) {
      dockerSwarmListAll().then((res) => {
        this.swarmList = res.data
        this.temp = {
          id: record.id
        }
        this.joinSwarmVisible = true
        this.$refs['joinForm']?.resetFields()
      })
    },
    // 处理加入集群
    handleSwarmJoin() {
      this.$refs['joinForm'].validate().then(() => {
        this.confirmLoading = true

        joinDockerSwarm(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.joinSwarmVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    //
    handleTryLocalDocker() {
      tryLocalDocker().then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
          this.loadData()
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
    // 同步到其他工作情况
    syncToWorkspaceShow(item) {
      this.syncToWorkspaceVisible = true
      this.loadWorkSpaceListAll()
      if (item) {
        this.temp = {
          ids: item.id,
          swarmId: item.swarmId ? true : false
        }
      } else {
        this.temp = {
          swarmId: true
        }
      }
    },
    handleSyncToWorkspace() {
      if (!this.temp.type) {
        $notification.warn({
          message: this.$tl('p.allocationType')
        })
        return false
      }
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: this.$tl('c.selectWorkspace')
        })
        return false
      }
      if (!this.temp.ids) {
        this.temp = { ...this.temp, ids: this.tableSelections.join(',') }
        this.tableSelections = []
      }
      // 同步
      this.confirmLoading = true
      machineDockerDistribute(this.temp)
        .then((res) => {
          if (res.code == 200) {
            $notification.success({
              message: res.msg
            })

            this.syncToWorkspaceVisible = false
            return false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 查看工作空间的 docker 信息
    viewWorkspaceDataHander(item) {
      this.workspaceDockerData = {}
      dockerListWorkspace({
        id: item.id
      }).then((res) => {
        if (res.code === 200) {
          this.viewWorkspaceDocker = true
          this.workspaceDockerData = res.data
        }
      })
    }
  }
}
</script>
