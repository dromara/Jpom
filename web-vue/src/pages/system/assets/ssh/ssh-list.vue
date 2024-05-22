<template>
  <div>
    <a-tabs default-active-key="1">
      <a-tab-pane key="1" :tab="$tl('p.manage')">
        <!-- 数据表格 -->
        <CustomTable
          is-show-tools
          default-auto-refresh
          :auto-refresh-time="5"
          table-name="assets-ssh-list"
          :empty-description="$tl('p.noAssetSsh')"
          :active-page="activePage"
          :data-source="list"
          :columns="columns"
          size="middle"
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
                class="search-input-item"
                :placeholder="$tl('p.sshName')"
                @press-enter="loadData"
              />
              <a-input
                v-model:value="listQuery['%host%']"
                class="search-input-item"
                placeholder="host"
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
                :placeholder="$tl('p.group')"
                class="search-input-item"
              >
                <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
              </a-select>

              <a-tooltip :title="$tl('p.goToFirstPage')">
                <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }} </a-button>
              </a-tooltip>

              <a-button type="primary" @click="handleAdd">{{ $tl('p.add') }}</a-button>
              <a-button :disabled="!tableSelections.length" type="primary" @click="syncToWorkspaceShow()">
                {{ $tl('p.batchAssign') }}</a-button
              >
              <a-button type="primary" @click="handlerExportData()"><DownloadOutlined />{{ $tl('p.export') }}</a-button>
              <a-dropdown>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="1">
                      <a-button type="primary" @click="handlerImportTemplate()">{{
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
                  :before-upload="beforeUpload"
                >
                  <a-button type="primary"><UploadOutlined /> {{ $tl('p.import') }}<DownOutlined /> </a-button>
                </a-upload>
              </a-dropdown>
            </a-space>
          </template>
          <template #tableHelp>
            <a-tooltip>
              <template #title>
                <div>
                  <ul>
                    <li>{{ $tl('p.nodeStatus') }}</li>
                    <li>{{ $tl('p.javaEnvAutoDetect') }}</li>
                    <li>{{ $tl('p.associatedNodeStatus') }}</li>
                  </ul>
                </div>
              </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </template>
          <template #tableBodyCell="{ column, text, record }">
            <template v-if="column.dataIndex === 'name'">
              <a-tooltip :title="text">
                <a-button style="padding: 0" type="link" size="small" @click="handleEdit(record)"> {{ text }}</a-button>
              </a-tooltip>
            </template>
            <template v-else-if="column.tooltip">
              <a-tooltip :title="text"> {{ text }}</a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'host'">
              <a-tooltip :title="`${text}:${record.port}`"> {{ text }}:{{ record.port }}</a-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'osName'">
              <a-popover :title="$tl('p.systemInfo')">
                <template #content>
                  <p>{{ $tl('p.systemName') }}{{ record.osName }}</p>
                  <p>{{ $tl('p.systemVersion') }}{{ record.osVersion }}</p>
                  <p>CPU{{ $tl('p.model') }}{{ record.osCpuIdentifierName }}</p>
                  <p>{{ $tl('p.hostname') }}{{ record.hostName }}</p>
                  <p>{{ $tl('p.bootTime') }}{{ formatDuration(record.osSystemUptime) }}</p>
                </template>
                {{ text || $tl('c.unknown') }}
              </a-popover>
            </template>
            <template v-else-if="column.dataIndex === 'nodeId'">
              <template v-if="record.status !== 2">
                <!-- 禁用监控不显示 -->
                <div v-if="record.javaVersion">
                  <a-popover v-if="record.jpomAgentPid > 0" :title="$tl('p.javaInfo')">
                    <template #content>
                      <p>{{ $tl('p.pluginProcessId') }}{{ record.jpomAgentPid }}</p>
                      <p>java{{ $tl('c.version') }}{{ record.javaVersion }}</p>
                    </template>
                    <a-tag color="green"> {{ record.jpomAgentPid }}</a-tag>
                  </a-popover>
                  <a-button v-else size="small" type="primary" @click="install(record)">{{
                    $tl('p.installNode')
                  }}</a-button>
                </div>

                <a-tag v-else color="orange">no java</a-tag>
              </template>
              <template v-else>-</template>
            </template>
            <template v-else-if="column.dataIndex === 'dockerInfo'">
              <template v-if="record.status !== 2">
                <!-- 禁用监控不显示 -->
                <a-popover v-if="record.dockerInfo" :title="$tl('p.dockerInfo')">
                  <template #content>
                    <p>{{ $tl('p.path') }}{{ JSON.parse(record.dockerInfo).path }}</p>
                    <p>{{ $tl('c.version') }}{{ JSON.parse(record.dockerInfo).version }}</p>
                  </template>
                  <a-tag color="green">{{ $tl('p.exists') }}</a-tag>
                </a-popover>

                <a-tag v-else>{{ $tl('p.notExists') }}</a-tag>
              </template>
              <template v-else>-</template>
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <a-tooltip :title="`${record.statusMsg || $tl('p.noStatusMsg')}`">
                <a-tag :color="statusMap[record.status] && statusMap[record.status].color">{{
                  (statusMap[record.status] && statusMap[record.status].desc) || $tl('c.unknown')
                }}</a-tag>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'renderSize'">
              <a-tooltip placement="topLeft" :title="renderSize(text)">
                <span>{{ renderSize(text) }}</span>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'osOccupyMemory'">
              <a-tooltip
                placement="topLeft"
                :title="`${$tl('p.memoryUsage')}${formatPercent(record.osOccupyMemory)},${$tl('p.totalMemory')}${renderSize(record.osMoneyTotal)}`"
              >
                <span>{{ formatPercent(record.osOccupyMemory) }}/{{ renderSize(record.osMoneyTotal) }}</span>
              </a-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'osMaxOccupyDisk'">
              <a-popover :title="$tl('p.diskInfo')">
                <template #content>
                  <p>{{ $tl('p.diskTotal') }}{{ renderSize(record.osFileStoreTotal) }}</p>
                  <p>{{ $tl('p.maxDiskUsage') }}{{ formatPercent(record.osMaxOccupyDisk) }}</p>
                  <p>{{ $tl('p.maxUsagePartition') }}{{ record.osMaxOccupyDiskName }}</p>
                </template>
                <span>{{ formatPercent(record.osMaxOccupyDisk) }} / {{ renderSize(record.osFileStoreTotal) }}</span>
              </a-popover>
            </template>

            <template v-else-if="column.dataIndex === 'osOccupyCpu'">
              <a-tooltip
                placement="topLeft"
                :title="`CPU${$tl('p.usage')}${formatPercent2Number(record.osOccupyCpu)}%,CPU${$tl('p.count')}${record.osCpuCores}`"
              >
                <span>{{ (formatPercent2Number(record.osOccupyCpu) || '-') + '%' }} / {{ record.osCpuCores }}</span>
              </a-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'operation'">
              <a-space>
                <a-dropdown>
                  <a-button size="small" type="primary" @click="handleTerminal(record, false)"
                    >{{ $tl('p.terminal') }}<DownOutlined
                  /></a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item key="1">
                        <a-button size="small" type="primary" @click="handleTerminal(record, true)">
                          <FullscreenOutlined />{{ $tl('p.fullScreenTerminal') }}
                        </a-button>
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
                <a-button size="small" type="primary" @click="syncToWorkspaceShow(record)">{{
                  $tl('p.assign')
                }}</a-button>
                <a-button size="small" type="primary" @click="handleFile(record)">{{ $tl('p.file') }}</a-button>
                <a-button size="small" type="primary" @click="handleViewWorkspaceSsh(record)">{{
                  $tl('p.associate')
                }}</a-button>

                <a-dropdown>
                  <a @click="(e) => e.preventDefault()">
                    {{ $tl('p.more') }}
                    <DownOutlined />
                  </a>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item>
                        <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('p.edit') }}</a-button>
                      </a-menu-item>
                      <a-menu-item>
                        <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                          $tl('c.delete')
                        }}</a-button>
                      </a-menu-item>
                      <a-menu-item>
                        <a-button size="small" type="primary" @click="handleViewLog(record)">{{
                          $tl('p.terminalLog')
                        }}</a-button>
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
          v-model:open="editSshVisible"
          destroy-on-close
          :confirm-loading="confirmLoading"
          width="600px"
          :title="$tl('p.editSsh')"
          :mask-closable="false"
          @ok="handleEditSshOk"
        >
          <a-form ref="editSshForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
            <a-form-item :label="$tl('c.sshName')" name="name">
              <a-input v-model:value="temp.name" :max-length="50" :placeholder="$tl('c.sshName')" />
            </a-form-item>
            <a-form-item :label="$tl('p.groupName')" name="group">
              <custom-select
                v-model:value="temp.groupName"
                :data="groupList"
                :input-placeholder="$tl('p.addNewGroup')"
                :select-placeholder="$tl('p.selectGroupName')"
              >
              </custom-select>
            </a-form-item>
            <a-form-item label="Host" name="host">
              <a-input-group compact name="host">
                <a-input v-model:value="temp.host" style="width: 70%" :placeholder="$tl('p.host')" />
                <a-form-item-rest>
                  <a-input-number v-model:value="temp.port" style="width: 30%" :min="1" :placeholder="$tl('p.port')" />
                </a-form-item-rest>
              </a-input-group>
            </a-form-item>
            <a-form-item :label="$tl('p.authType')" name="connectType">
              <a-radio-group v-model:value="temp.connectType" :options="options" />
            </a-form-item>
            <a-form-item name="user">
              <template #label>
                <a-tooltip>
                  {{ $tl('c.username') }}
                  <template #title>
                    {{ $tl('p.accountSupportVarRef') }}<b>$ref.wEnv.xxxx</b> xxxx {{ $tl('c.variableName') }}</template
                  >
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input v-model:value="temp.user" :placeholder="$tl('p.user')">
                <template #suffix>
                  <a-tooltip v-if="temp.id" :title="$tl('p.passwordNote')">
                    <a-button size="small" type="primary" danger @click="handerRestHideField(temp)">{{
                      $tl('p.clear')
                    }}</a-button>
                  </a-tooltip>
                </template>
              </a-input>
            </a-form-item>
            <!-- 新增时需要填写 -->
            <!--				<a-form-item v-if="temp.type === 'add'" label="Password" name="password">-->
            <!--					<a-input-password v-model="temp.password" placeholder="密码"/>-->
            <!--				</a-form-item>-->
            <!-- 修改时可以不填写 -->
            <a-form-item
              :name="`${temp.type === 'add' && temp.connectType === 'PASS' ? 'password' : 'password-update'}`"
            >
              <template #label>
                <a-tooltip>
                  {{ $tl('c.password') }}
                  <template #title>
                    {{ $tl('p.passwordSupportVarRef') }}<b>$ref.wEnv.xxxx</b> xxxx {{ $tl('c.variableName') }}</template
                  >
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <!-- <a-input-password v-model="temp.password" :placeholder="`${temp.type === 'add' ? '密码' : '密码若没修改可以不用填写'}`" /> -->
              <custom-input
                :input="temp.password"
                :env-list="envVarList"
                :placeholder="`${temp.type === 'add' ? $tl('c.password') : $tl('c.passwordNote2')}`"
                @change="
                  (v) => {
                    temp = { ...temp, password: v }
                  }
                "
              >
              </custom-input>
            </a-form-item>
            <a-form-item v-if="temp.connectType === 'PUBKEY'" name="privateKey">
              <template #label>
                <a-tooltip placement="topLeft">
                  {{ $tl('p.privateKeyContent') }}
                  <template #title>{{ $tl('p.privateKeyDefaultNote') }} </template>
                  <QuestionCircleOutlined v-if="temp.type !== 'edit'" />
                </a-tooltip>
              </template>

              <a-textarea
                v-model:value="temp.privateKey"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                :placeholder="$tl('p.privateKeyContent2')"
              />
            </a-form-item>
            <a-form-item :label="$tl('c.encoding')" name="charset">
              <a-input v-model:value="temp.charset" :placeholder="$tl('c.encoding')" />
            </a-form-item>
            <a-form-item :label="$tl('p.timeout')" name="timeout">
              <a-input-number
                v-model:value="temp.timeout"
                :min="1"
                :placeholder="$tl('p.timeoutUnit')"
                style="width: 100%"
              />
            </a-form-item>
            <a-form-item :label="$tl('c.fileSuffix')" name="suffix">
              <template #help>
                {{ $tl('p.serverManagementNote') }}, {{ $tl('p.workspaceSshConfigNote')
                }}<span style="color: red">{{ $tl('p.sshConfigMethod') }}</span
                >）{{ $tl('p.punctuation') }}
              </template>
              <a-textarea
                v-model:value="temp.allowEditSuffix"
                :rows="5"
                style="resize: none"
                :placeholder="$tl('c.fileSuffixAndEncoding')"
              />
            </a-form-item>
          </a-form>
        </a-modal>
        <!-- 安装节点 -->
        <a-modal
          v-model:open="nodeVisible"
          destroy-on-close
          width="80%"
          :title="$tl('p.pluginInstallation')"
          :footer="null"
          :mask-closable="false"
          @cancel="
            () => {
              nodeVisible = false
              loadData()
            }
          "
        >
          <fastInstall v-if="nodeVisible"></fastInstall>
        </a-modal>
        <!-- 文件管理 -->
        <a-drawer
          destroy-on-close
          :title="`${temp.name} ${$tl('p.fileManagement')}`"
          placement="right"
          width="90vw"
          :open="drawerVisible"
          @close="
            () => {
              drawerVisible = false
            }
          "
        >
          <ssh-file v-if="drawerVisible" :machine-ssh-id="temp.id" />
        </a-drawer>
        <!-- Terminal -->
        <a-modal
          v-model:open="terminalVisible"
          destroy-on-close
          :style="{
            maxWidth: '100vw',
            top: terminalFullscreen ? 0 : false,
            paddingBottom: 0
          }"
          :width="terminalFullscreen ? '100vw' : '80vw'"
          :body-style="{
            padding: '0 10px',
            paddingTop: '10px',
            marginRight: '10px',
            height: `${terminalFullscreen ? 'calc(100vh - 80px)' : '70vh'}`,
            display: 'flex',
            flexDirection: 'column'
          }"
          :title="temp.name"
          :footer="null"
          :mask-closable="false"
        >
          <terminal2 v-if="terminalVisible" :machine-ssh-id="temp.id" />
        </a-modal>
        <!-- 操作日志 -->
        <a-modal
          v-model:open="viewOperationLog"
          destroy-on-close
          :title="$tl('p.operationLog')"
          width="80vw"
          :footer="null"
          :mask-closable="false"
        >
          <OperationLog v-if="viewOperationLog" :machine-ssh-id="temp.id"></OperationLog>
        </a-modal>
        <!-- 查看 ssh 关联工作空间的信息 -->
        <a-modal
          v-model:open="viewWorkspaceSsh"
          destroy-on-close
          width="50%"
          :title="$tl('p.associateWorkspaceSsh')"
          :footer="null"
          :mask-closable="false"
        >
          <a-space direction="vertical" style="width: 100%">
            <a-alert
              v-if="workspaceSshList && workspaceSshList.length"
              :message="$tl('p.sshDeletionNote')"
              type="info"
              show-icon
            />
            <a-list bordered :data-source="workspaceSshList">
              <template #renderItem="{ item }">
                <a-list-item style="display: block">
                  <a-row>
                    <a-col :span="10">SSH{{ $tl('p.name') }}{{ item.name }}</a-col>
                    <a-col :span="10"
                      >{{ $tl('p.belongingWorkspace') }}{{ item.workspace && item.workspace.name }}</a-col
                    >
                    <a-col :span="4">
                      <a-button v-if="item.workspace" size="small" type="primary" @click="configWorkspaceSsh(item)"
                        >{{ $tl('p.configuration') }}
                      </a-button>
                      <a-button v-else size="small" type="primary" danger @click="handleDeleteWorkspaceItem(item)"
                        >{{ $tl('c.delete') }}
                      </a-button>
                    </a-col>
                  </a-row>
                </a-list-item>
              </template>
            </a-list>
          </a-space>
        </a-modal>
        <a-modal
          v-model:open="configWorkspaceSshVisible"
          destroy-on-close
          :confirm-loading="confirmLoading"
          width="50%"
          :title="$tl('p.configureSsh')"
          :mask-closable="false"
          @ok="handleConfigWorkspaceSshOk"
        >
          <a-form
            ref="editConfigWorkspaceSshForm"
            :rules="rules"
            :model="temp"
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 18 }"
          >
            <a-form-item label="" :label-col="{ span: 0 }" :wrapper-col="{ span: 24 }">
              <a-alert :message="$tl('p.workspaceSpecificConfigNote')" banner />
            </a-form-item>
            <a-form-item :label="$tl('c.sshName')">
              <a-input v-model:value="temp.name" :disabled="true" :max-length="50" :placeholder="$tl('c.sshName')" />
            </a-form-item>
            <a-form-item :label="$tl('c.workspaceName')">
              <a-input
                v-model:value="temp.workspaceName"
                :disabled="true"
                :max-length="50"
                :placeholder="$tl('c.workspaceName')"
              />
            </a-form-item>

            <a-form-item name="fileDirs">
              <template #label>
                <a-tooltip>
                  {{ $tl('p.fileDirectory') }}
                  <template #title> {{ $tl('p.onlineManagementNote') }} </template>
                  <QuestionCircleOutlined />
                </a-tooltip>
              </template>
              <a-textarea
                v-model:value="temp.fileDirs"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                :placeholder="$tl('p.authorizedDirectories')"
              />
            </a-form-item>

            <a-form-item :label="$tl('c.fileSuffix')" name="suffix">
              <a-textarea
                v-model:value="temp.allowEditSuffix"
                :rows="5"
                style="resize: none"
                :placeholder="$tl('c.fileSuffixAndEncoding')"
              />
            </a-form-item>
            <a-form-item name="notAllowedCommand">
              <template #label>
                <a-tooltip>
                  {{ $tl('p.forbiddenCommands') }}
                  <template #title>
                    {{ $tl('p.forbiddenCommandsNote') }}
                    <ul>
                      <li>{{ $tl('p.superAdminNote') }}</li>
                      <li>{{ $tl('p.otherUsersNote') }}</li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined />
                </a-tooltip>
              </template>
              <a-textarea
                v-model:value="temp.notAllowedCommand"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                :placeholder="$tl('p.forbiddenCommandsDetail')"
              />
            </a-form-item>
          </a-form>
        </a-modal>
        <!-- 分配到其他工作空间 -->
        <a-modal
          v-model:open="syncToWorkspaceVisible"
          destroy-on-close
          :confirm-loading="confirmLoading"
          :title="$tl('p.assignToOtherWorkspaces')"
          :mask-closable="false"
          @ok="handleSyncToWorkspace"
        >
          <a-space direction="vertical" style="width: 100%">
            <a-alert :message="$tl('p.note')" type="warning" show-icon>
              <template #description>{{ $tl('p.configInstructions') }}</template>
            </a-alert>
            <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
              <a-form-item :label="$tl('p.workspaceSelection')" name="workspaceId">
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
          </a-space>
        </a-modal>
      </a-tab-pane>
      <a-tab-pane key="2" :tab="$tl('p.commandLog')"> <OperationLog type="machinessh"></OperationLog></a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import {
  machineSshListData,
  machineSshListGroup,
  machineSshEdit,
  machineSshDelete,
  machineListGroupWorkspaceSsh,
  machineSshSaveWorkspaceConfig,
  machineSshDistribute,
  restHideField,
  importTemplate,
  exportData,
  importData,
  statusMap
} from '@/api/system/assets-ssh'
import {
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime,
  CHANGE_PAGE,
  renderSize,
  formatPercent,
  formatDuration,
  formatPercent2Number
} from '@/utils/const'
import fastInstall from '@/pages/node/fast-install.vue'
import CustomSelect from '@/components/customSelect'
import CustomInput from '@/components/customInput'
import SshFile from '@/pages/ssh/ssh-file'
import Terminal2 from '@/pages/ssh/terminal.vue'
import OperationLog from '@/pages/system/assets/ssh/operation-log'
import { deleteForeSsh } from '@/api/ssh'
import { getWorkspaceEnvAll, getWorkSpaceListAll } from '@/api/workspace'

export default {
  components: {
    fastInstall,
    CustomSelect,
    Terminal2,
    SshFile,
    OperationLog,
    CustomInput
  },
  data() {
    return {
      loading: true,
      groupList: [],
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editSshVisible: false,
      temp: {},
      statusMap,
      // tempPwd: '',
      options: [
        { label: this.$tl('c.password'), value: 'PASS' },
        { label: this.$tl('p.certificate'), value: 'PUBKEY' }
      ],
      columns: [
        {
          title: this.$tl('p.name_1'),
          dataIndex: 'name',
          width: 120,
          sorter: true,
          ellipsis: true
        },

        {
          title: 'Host',
          dataIndex: 'host',
          width: 120,
          sorter: true,
          ellipsis: true
        },
        // { title: "Port", dataIndex: "port", sorter: true, width: 80, ellipsis: true,},
        {
          title: this.$tl('c.username'),
          dataIndex: 'user',
          sorter: true,
          width: '80px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.systemName_1'),
          dataIndex: 'osName',
          width: 120,
          sorter: true,
          ellipsis: true
        },
        {
          title: 'CPU',
          dataIndex: 'osOccupyCpu',
          sorter: true,
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$tl('p.memory'),
          dataIndex: 'osOccupyMemory',
          sorter: true,
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$tl('p.disk'),
          dataIndex: 'osMaxOccupyDisk',
          sorter: true,
          width: '100px',
          ellipsis: true
        },
        // { title: "编码格式", dataIndex: "charset", sorter: true, width: 120, ellipsis: true,},
        {
          title: this.$tl('p.connectionStatus'),
          dataIndex: 'status',
          ellipsis: true,
          align: 'center',
          width: '100px'
        },
        {
          title: this.$tl('p.nodeStatus_1'),
          dataIndex: 'nodeId',

          width: '80px',
          ellipsis: true
        },
        {
          title: 'docker',
          dataIndex: 'dockerInfo',

          width: '80px',
          ellipsis: true
        },
        {
          title: this.$tl('p.creationTime'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.modificationTime'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',

          width: '310px',
          align: 'center',
          // ellipsis: true,
          fixed: 'right'
        }
      ],
      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$tl('p.inputName'), trigger: 'blur' }],
        host: [{ required: true, message: this.$tl('p.inputHostAddress'), trigger: 'blur' }],
        port: [{ required: true, message: this.$tl('p.inputPortNumber'), trigger: 'blur' }],
        connectType: [
          {
            required: true,
            message: this.$tl('p.selectConnectionType'),
            trigger: 'blur'
          }
        ],
        user: [{ required: true, message: this.$tl('p.inputAccountName'), trigger: 'blur' }],
        password: [{ required: true, message: this.$tl('p.inputLoginPassword'), trigger: 'blur' }]
      },
      nodeVisible: false,

      terminalVisible: false,
      terminalFullscreen: false,
      viewOperationLog: false,
      drawerVisible: false,
      workspaceSshList: [],
      viewWorkspaceSsh: false,
      configWorkspaceSshVisible: false,
      syncToWorkspaceVisible: false,
      workspaceList: [],
      tableSelections: [],
      envVarList: [],
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
  created() {
    this.loadData()
    this.loadGroupList()
    this.getWorkEnvList()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.system.assets.ssh.sshList.${key}`, ...args)
    },
    formatDuration,
    renderSize,
    formatPercent,
    formatPercent2Number,
    getWorkEnvList() {
      getWorkspaceEnvAll({
        workspaceId: 'GLOBAL'
      }).then((res) => {
        if (res.code === 200) {
          this.envVarList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      machineSshListData(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
          //
        }
        this.loading = false
      })
    },
    // 获取所有的分组
    loadGroupList() {
      machineSshListGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 新增 SSH
    handleAdd() {
      this.temp = {
        charset: 'UTF-8',
        port: 22,
        timeout: 5,
        connectType: 'PASS'
      }
      this.editSshVisible = true
      // @author jzy 08-04
      this.$refs['editSshForm'] && this.$refs['editSshForm'].resetFields()
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record, {
        allowEditSuffix: record.allowEditSuffix ? JSON.parse(record.allowEditSuffix).join('\r\n') : ''
      })

      this.temp = {
        ...this.temp,

        timeout: record.timeout || 5
      }
      this.editSshVisible = true
      // @author jzy 08-04
      this.$refs['editSshForm'] && this.$refs['editSshForm'].resetFields()
      this.loadGroupList()
    },
    // 提交 SSH 数据
    handleEditSshOk() {
      // 检验表单
      this.$refs['editSshForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        machineSshEdit(this.temp)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.editSshVisible = false
              this.loadData()
              this.loadGroupList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // 安装节点
    install() {
      this.nodeVisible = true
    },
    // 进入终端
    handleTerminal(record, terminalFullscreen) {
      this.temp = Object.assign({}, record)
      this.terminalVisible = true
      this.terminalFullscreen = terminalFullscreen
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$tl('c.systemPrompt'),
        content: this.$tl('p.confirmDeleteMachineSsh'),
        zIndex: 1009,
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return machineSshDelete({
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

    // 操作日志
    handleViewLog(record) {
      this.temp = Object.assign({}, record)
      this.viewOperationLog = true
    },
    // 查看工作空间的 ssh
    handleViewWorkspaceSsh(item) {
      machineListGroupWorkspaceSsh({
        id: item.id
      }).then((res) => {
        if (res.code === 200) {
          this.temp = {
            machineSshId: item.id
          }
          this.viewWorkspaceSsh = true
          this.workspaceSshList = res.data
        }
      })
    },
    // 配置 ssh
    configWorkspaceSsh(item) {
      this.temp = {
        ...this.temp,
        id: item.id,
        name: item.name,
        fileDirs: item.fileDirs ? JSON.parse(item.fileDirs).join('\r\n') : '',
        allowEditSuffix: item.allowEditSuffix ? JSON.parse(item.allowEditSuffix).join('\r\n') : '',
        workspaceName: item.workspace?.name,
        notAllowedCommand: item.notAllowedCommand
      }
      this.configWorkspaceSshVisible = true
    },
    // 提交 SSH 配置 数据
    handleConfigWorkspaceSshOk() {
      // 检验表单
      this.$refs['editConfigWorkspaceSshForm'].validate().then(() => {
        this.confirmLoading = true
        // 提交数据
        machineSshSaveWorkspaceConfig(this.temp)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.configWorkspaceSshVisible = false
              machineListGroupWorkspaceSsh({
                id: this.temp.machineSshId
              }).then((res) => {
                if (res.code === 200) {
                  this.workspaceSshList = res.data
                }
              })
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除工作空间的数据
    handleDeleteWorkspaceItem(record) {
      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmDeleteWorkspaceSsh'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: async () => {
          const { code, msg } = await deleteForeSsh(record.id)
          if (code === 200) {
            $notification.success({
              message: msg
            })
            const res = await machineListGroupWorkspaceSsh({
              id: this.temp.machineSshId
            })
            if (res.code === 200) {
              this.workspaceSshList = res.data
            }
          }
        }
      })
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, record)

      this.drawerVisible = true
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
          ids: item.id
        }
      }
    },
    handleSyncToWorkspace() {
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
      this.confirmLoading = true
      // 同步
      machineSshDistribute(this.temp)
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
    // 清除隐藏字段
    handerRestHideField(record) {
      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmClearSshHiddenInfo'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return restHideField(record.id).then((res) => {
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
    // 下载导入模板
    handlerImportTemplate() {
      window.open(importTemplate(), '_blank')
    },
    handlerExportData() {
      window.open(exportData({ ...this.listQuery }), '_blank')
    },
    beforeUpload(file) {
      const formData = new FormData()
      formData.append('file', file)
      importData(formData).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    }
  }
}
</script>
