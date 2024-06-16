<template>
  <div class="">
    <template v-if="useSuggestions">
      <a-result :title="$t('pages.node.list.2788c13d')">
        <template #subTitle> {{ $t('pages.node.list.b48f7ec3') }} </template>
        <template #extra>
          <a-button type="primary" @click="fastInstallNodeShow">{{ $t('pages.node.list.e083d2f7') }} </a-button>
          <router-link to="/system/assets/machine-list">
            <a-button key="console" type="primary">{{ $t('pages.node.list.2f070f1') }}</a-button></router-link
          >
        </template>
        <a-alert :message="$t('pages.node.list.a5e1426d')" type="info" show-icon>
          <template #description>
            <ol>
              <li>{{ $t('pages.node.list.c716210a') }}</li>
              <li>{{ $t('pages.node.list.1ef2234b') }}</li>
            </ol>
          </template>
        </a-alert>
      </a-result>
    </template>
    <template v-else>
      <!-- <a-card :body-style="{ padding: '10px' }"> -->
      <CustomTable
        is-show-tools
        default-auto-refresh
        :auto-refresh-time="30"
        :active-page="activePage"
        table-name="nodeSearch"
        :empty-description="$t('pages.node.list.c2a3f0cd')"
        :columns="columns"
        :data-source="list"
        bordered
        size="middle"
        row-key="id"
        :pagination="pagination"
        :scroll="{
          x: 'max-content'
        }"
        :row-selection="rowSelection"
        @change="
          (pagination, filters, sorter) => {
            listQuery = CHANGE_PAGE(listQuery, {
              pagination,
              sorter
            })
            loadData()
          }
        "
        @refresh="loadData"
        @change-table-layout="
          (layoutType) => {
            tableSelections = []
            listQuery = CHANGE_PAGE(listQuery, {
              pagination: { limit: layoutType === 'card' ? 8 : 10 }
            })
            loadData()
          }
        "
      >
        <template #title>
          <a-space>
            <a-input
              v-model:value="listQuery['%name%']"
              :placeholder="$t('pages.node.list.3614bbe4')"
              @press-enter="loadData"
            />

            <a-select
              v-model:value="listQuery.group"
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
              :placeholder="$t('pages.node.list.3228ecae')"
              class="search-input-item"
            >
              <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
            </a-select>
            <a-tooltip :title="$t('pages.node.list.767472f6')">
              <a-button :loading="loading" type="primary" @click="loadData">{{
                $t('pages.node.list.53c2763c')
              }}</a-button>
            </a-tooltip>
            <a-button
              type="primary"
              @click="
                () => {
                  fastInstallNode = true
                }
              "
              >{{ $t('pages.node.list.e083d2f7') }}
            </a-button>
            <a-dropdown v-if="layoutType === 'table'">
              <a-button
                type="primary"
                :disabled="!tableSelections || !tableSelections.length"
                @click="syncToWorkspaceShow"
                >{{ $t('pages.node.list.aa37b575') }}</a-button
              >
            </a-dropdown>
            <a-tooltip v-else :title="$t('pages.node.list.11795378')">
              <a-button :disabled="true" type="primary"> {{ $t('pages.node.list.aa37b575') }} </a-button>
            </a-tooltip>
          </a-space>
        </template>
        <template #tableHelp>
          <a-tooltip placement="bottom">
            <template #title>
              <div>
                <ul>
                  <li>{{ $t('pages.node.list.43ace0d9') }}</li>
                  <li>{{ $t('pages.node.list.8b8c076') }}</li>
                  <li>{{ $t('pages.node.list.36616e52') }}</li>
                  <li>{{ $t('pages.node.list.6a4a7d4') }}</li>
                  <li>{{ $t('pages.node.list.62be4cf2') }}</li>
                </ul>
              </div>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </template>
        <template #tableBodyCell="{ column, text, record, index }">
          <template v-if="column.dataIndex === 'url'">
            <a-tooltip placement="topLeft" :title="text">
              <template v-if="record.machineNodeData">
                <span>{{ record.machineNodeData.jpomProtocol }}://{{ record.machineNodeData.jpomUrl }}</span>
              </template>
              <span v-else> - </span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'name'">
            <template v-if="record.openStatus !== 1">
              <a-tooltip :title="`${text}`">
                <span>{{ text || '' }}</span>
              </a-tooltip>
            </template>
            <template v-else>
              <a-tooltip :title="`${text} ${$t('pages.node.list.546159c')}`" @click="handleNode(record)">
                <a-button type="link" style="padding: 0" size="small">
                  <FullscreenOutlined /><span>{{ text }}</span>
                </a-button>
              </a-tooltip>
            </template>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tooltip
              placement="topLeft"
              :title="`${
                statusMap[record.machineNodeData && record.machineNodeData.status] || $t('pages.node.list.5f51a112')
              } ${record.machineNodeData && record.machineNodeData.statusMsg}`"
            >
              <template v-if="record.openStatus === 1">
                <a-tag
                  :color="record.machineNodeData && record.machineNodeData.status === 1 ? 'green' : 'pink'"
                  style="margin-right: 0"
                >
                  {{
                    statusMap[record.machineNodeData && record.machineNodeData.status] || $t('pages.node.list.5f51a112')
                  }}
                </a-tag>
              </template>
              <a-tag v-else>{{ $t('pages.node.list.edb35bf0') }}</a-tag>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'osName'">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ record.machineNodeData && record.machineNodeData.osName }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'javaVersion'">
            <a-tooltip placement="topLeft" :title="record.machineNodeData && record.machineNodeData.javaVersion">
              <span>{{ record.machineNodeData && record.machineNodeData.javaVersion }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'jvmInfo'">
            <a-tooltip
              placement="topLeft"
              :title="`${$t('pages.node.list.56e59e5')}${renderSize(
                record.machineNodeData && record.machineNodeData.jvmFreeMemory
              )} ${$t('pages.node.list.8b8cc8a1')}${renderSize(
                record.machineNodeData && record.machineNodeData.jvmTotalMemory
              )}`"
            >
              <span
                >{{ renderSize(record.machineNodeData && record.machineNodeData.jvmFreeMemory) }}
                /
                {{ renderSize(record.machineNodeData && record.machineNodeData.jvmTotalMemory) }}</span
              >
            </a-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'runTime'">
            <a-tooltip
              placement="topLeft"
              :title="formatDuration(record.machineNodeData && record.machineNodeData.jpomUptime)"
            >
              <span>{{ formatDuration(record.machineNodeData && record.machineNodeData.jpomUptime, '', 2) }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'jpomProjectCount'">
            <div v-if="record.machineNodeData && record.machineNodeData.status === 1" @click="syncNode(record)">
              <a-tooltip placement="topLeft">
                <template #title>
                  <ul>
                    <li>{{ $t('pages.node.list.a3b72d82') }}{{ text || 0 }}</li>
                    <li>{{ $t('pages.node.list.44708423') }}{{ record.machineNodeData.jpomProjectCount }}</li>
                    <li>{{ $t('pages.node.list.a1d9a917') }}</li>
                  </ul>
                </template>
                <a-tag>{{ text || 0 }} </a-tag>
                <SyncOutlined />
              </a-tooltip>
            </div>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'jpomScriptCount'">
            <div v-if="record.machineNodeData && record.machineNodeData.status === 1" @click="syncNodeScript(record)">
              <a-tooltip placement="topLeft">
                <template #title>
                  <ul>
                    <li>{{ $t('pages.node.list.e27607ad') }}{{ text || 0 }}</li>
                    <li>{{ $t('pages.node.list.ae871dd1') }}{{ record.machineNodeData.jpomScriptCount }}</li>
                    <li>{{ $t('pages.node.list.c3ba1425') }}</li>
                  </ul>
                </template>
                <a-tag>{{ text || 0 }} </a-tag>
                <SyncOutlined />
              </a-tooltip>
            </div>
            <span v-else>-</span>
          </template>

          <template v-else-if="column.dataIndex === 'operation'">
            <a-space>
              <a-tooltip :title="$t('pages.node.list.68b2a086')">
                <a-button size="small" type="primary" :disabled="record.openStatus !== 1" @click="handleNode(record)">{{
                  $t('pages.node.list.63b11e73')
                }}</a-button>
              </a-tooltip>
              <a-tooltip :title="$t('pages.node.list.423745e3')">
                <a-button size="small" type="primary" :disabled="!record.sshId" @click="handleTerminal(record)"
                  ><CodeOutlined />{{ $t('pages.node.list.b5a97ef7') }}</a-button
                >
              </a-tooltip>

              <a-dropdown>
                <a @click="(e) => e.preventDefault()">
                  {{ $t('pages.node.list.6e071067') }}
                  <DownOutlined />
                </a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item>
                      <a-button size="small" type="primary" @click="handleEdit(record)">{{
                        $t('pages.node.list.64603c01')
                      }}</a-button>
                    </a-menu-item>

                    <a-menu-item>
                      <a-tooltip placement="leftBottom" :title="$t('pages.node.list.d24d9b6d')">
                        <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                          $t('pages.node.list.dd20d11c')
                        }}</a-button>
                      </a-tooltip>
                    </a-menu-item>
                    <a-menu-item>
                      <a-tooltip placement="leftBottom" :title="$t('pages.node.list.ca6be4de')">
                        <a-button size="small" type="primary" danger @click="handleUnbind(record)">{{
                          $t('pages.node.list.4c957529')
                        }}</a-button>
                      </a-tooltip>
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item>
                      <a-button
                        size="small"
                        type="primary"
                        :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                        @click="sortItemHander(record, index, 'top')"
                        >{{ $t('pages.node.list.9e850907') }}</a-button
                      >
                    </a-menu-item>
                    <a-menu-item>
                      <a-button
                        size="small"
                        type="primary"
                        :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                        @click="sortItemHander(record, index, 'up')"
                        >{{ $t('pages.node.list.6e79de86') }}</a-button
                      >
                    </a-menu-item>
                    <a-menu-item>
                      <a-button
                        size="small"
                        type="primary"
                        :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total"
                        @click="sortItemHander(record, index, 'down')"
                      >
                        {{ $t('pages.node.list.78c0cb41') }}
                      </a-button>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
        <template #cardBodyCell="{ item }">
          <a-card :head-style="{ padding: '0 6px' }" :body-style="{ padding: '10px' }">
            <template #title>
              <a-row :gutter="[4, 0]">
                <a-col :span="17" style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                  <a-tooltip>
                    <template #title>
                      {{ $t('pages.node.list.546159c') }}
                      <div>{{ $t('pages.node.list.fa8d810f') }}{{ item.name }}</div>
                      <div>{{ $t('pages.node.list.a0d52737') }}{{ item.url }}</div>
                    </template>

                    <a-button type="link" style="padding: 0" size="small" @click="handleNode(item)">
                      <span> {{ item.name }}</span>
                    </a-button>
                  </a-tooltip>
                </a-col>
                <a-col :span="7" style="text-align: right">
                  <a-tooltip>
                    <template #title>
                      <div>
                        {{ $t('pages.node.list.10775be3')
                        }}{{ statusMap[item.machineNodeData && item.machineNodeData.status] }}
                      </div>
                      <div>
                        {{ $t('pages.node.list.a622e852')
                        }}{{ (item.machineNodeData && item.machineNodeData.statusMsg) || '' }}
                      </div>
                    </template>
                    <a-tag
                      :color="item.machineNodeData && item.machineNodeData.status === 1 ? 'green' : 'pink'"
                      style="margin-right: 0"
                    >
                      {{ statusMap[item.machineNodeData && item.machineNodeData.status] }}
                    </a-tag>
                  </a-tooltip>
                </a-col>
              </a-row>
            </template>

            <a-row :gutter="[8, 8]">
              <a-col :span="8" style="text-align: center">
                <a-tooltip
                  :title="`CPU ${$t('pages.node.list.6b3a1f7c')}${item.occupyCpu}%`"
                  @click="handleHistory(item, 'nodeTop')"
                >
                  <a-progress
                    type="circle"
                    :size="80"
                    :stroke-color="{
                      '0%': '#87d068',
                      '30%': '#87d068',
                      '100%': '#108ee9'
                    }"
                    status="active"
                    :percent="item.occupyCpu"
                  />
                </a-tooltip>
              </a-col>
              <a-col :span="8" style="text-align: center">
                <a-tooltip
                  :title="`${$t('pages.node.list.d98a95f3')}${item.occupyDisk}%`"
                  @click="handleHistory(item, 'nodeTop')"
                >
                  <a-progress
                    type="circle"
                    :size="80"
                    :stroke-color="{
                      '0%': '#87d068',
                      '30%': '#87d068',
                      '100%': '#108ee9'
                    }"
                    status="active"
                    :percent="item.occupyDisk"
                  />
                </a-tooltip>
              </a-col>
              <a-col :span="8" style="text-align: center">
                <a-tooltip
                  :title="`${$t('pages.node.list.7da0f839')}${item.occupyMemory}%`"
                  @click="handleHistory(item, 'nodeTop')"
                >
                  <a-progress
                    :size="80"
                    type="circle"
                    :stroke-color="{
                      '0%': '#87d068',
                      '30%': '#87d068',
                      '100%': '#108ee9'
                    }"
                    status="active"
                    :percent="item.occupyMemory"
                  />
                </a-tooltip>
              </a-col>
            </a-row>

            <a-row :gutter="[8, 8]" style="text-align: center">
              <a-col :span="8">
                <a-tooltip
                  :title="`${
                    $t('pages.node.list.6e2c9ac6') +
                    (formatDuration(item.machineNodeData && item.machineNodeData.networkDelay, '', 2) || '-') +
                    $t('pages.node.list.4db198af')
                  }`"
                  @click="handleHistory(item, 'networkDelay')"
                >
                  <a-statistic
                    :title="$t('pages.node.list.6e2c9ac6')"
                    :value="item.machineNodeData && item.machineNodeData.networkDelay"
                    :value-style="statValueStyle"
                    :formatter="
                      (v) => {
                        return formatDuration(item.machineNodeData && item.machineNodeData.networkDelay, '', 2) || '-'
                      }
                    "
                  />
                </a-tooltip>
              </a-col>
              <a-col :span="8">
                <a-tooltip
                  :title="formatDuration(item.machineNodeData && item.machineNodeData.jpomUptime, '', 1) || '-'"
                >
                  <a-statistic
                    :title="$t('pages.node.list.4a4e5b97')"
                    :value-style="statValueStyle"
                    :formatter="
                      (v) => {
                        return formatDuration(item.machineNodeData && item.machineNodeData.jpomUptime, '', 2) || '-'
                      }
                    "
                  />
                </a-tooltip>
              </a-col>
              <a-col :span="8">
                <a-tooltip :title="`${parseTime(item.machineNodeData && item.machineNodeData.modifyTimeMillis)}`">
                  <a-statistic
                    :title="$t('pages.node.list.3d55d8de')"
                    :value-style="statValueStyle"
                    :formatter="
                      (v) => {
                        return parseTime(item.machineNodeData && item.machineNodeData.modifyTimeMillis, 'HH:mm:ss')
                      }
                    "
                  />
                </a-tooltip>
              </a-col>
            </a-row>
          </a-card>
        </template>
        <!-- <template #cardPageTool>
          <a-row type="flex" justify="center">
            <a-divider v-if="listQuery.total / listQuery.limit > 1" dashed />
            <a-col>
              <a-pagination
                v-model:current="listQuery.page"
                v-model:pageSize="listQuery.limit"
                :show-total="
                  (total) => {
                    return PAGE_DEFAULT_SHOW_TOTAL(total, listQuery)
                  }
                "
                :show-size-changer="true"
                :page-size-options="sizeOptions"
                :total="listQuery.total"
                :hide-on-single-page="true"
                show-less-items
                @show-size-change="
                  (current, size) => {
                    listQuery.limit = size
                    loadData()
                  }
                "
                @change="loadData"
              />
            </a-col>
          </a-row>
        </template> -->
      </CustomTable>
      <!-- <template v-else-if="layoutType === 'card'">
          <a-row :gutter="[16, 16]">
            <template v-if="list && list.length">
              <a-col v-for="item in list" :key="item.id" :span="6">

              </a-col>
            </template>
            <a-col v-else :span="24">
              <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" description="没有任何节点" />
            </a-col>
          </a-row>


        </template> -->
      <!-- </a-card> -->
    </template>

    <!-- 编辑区 -->
    <customModal
      v-if="editNodeVisible"
      v-model:open="editNodeVisible"
      destroy-on-close
      width="50%"
      :title="$t('pages.node.list.9fa2f20c')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleEditNodeOk"
    >
      <a-form ref="editNodeForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
        <a-form-item :label="$t('pages.node.list.3614bbe4')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('pages.node.list.3614bbe4')" />
        </a-form-item>
        <a-form-item :label="$t('pages.node.list.12d0e469')" name="group">
          <custom-select
            v-model:value="temp.group"
            :data="groupList"
            :input-placeholder="$t('pages.node.list.c50ead9c')"
            :select-placeholder="$t('pages.node.list.c385f859')"
          >
          </custom-select>
        </a-form-item>

        <a-form-item :label="$t('pages.node.list.87c09576')" name="openStatus">
          <a-switch
            :checked="temp.openStatus == 1"
            :checked-children="$t('pages.node.list.149265a9')"
            :un-checked-children="$t('pages.node.list.287f3530')"
            default-checked
            @change="
              (checked) => {
                temp.openStatus = checked ? 1 : 0
              }
            "
          />
        </a-form-item>
        <a-form-item :label="$t('pages.node.list.72435e8f')" name="sshId">
          <a-select
            v-model:value="temp.sshId"
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
            :placeholder="$t('pages.node.list.9275502b')"
          >
            <a-select-option value="">{{ $t('pages.node.list.33711dff') }}</a-select-option>
            <a-select-option v-for="ssh in sshList" :key="ssh.id" :disabled="ssh.disabled">{{
              ssh.name
            }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </customModal>
    <!-- 管理节点 -->
    <NodeFunc v-if="drawerVisible" :id="temp.id" :name="temp.name" @close="onClose"></NodeFunc>
    <!-- Terminal -->
    <customModal
      v-if="terminalVisible"
      v-model:open="terminalVisible"
      :body-style="{
        padding: '0 10px',
        paddingTop: '10px',
        marginRight: '10px',
        height: `70vh`
      }"
      width="80%"
      title="Terminal"
      :footer="null"
      :mask-closable="false"
    >
      <terminal1 v-if="terminalVisible" :ssh-id="temp.sshId" :node-id="temp.id" />
    </customModal>

    <!-- 快速安装插件端 -->
    <CustomModal
      v-if="fastInstallNode"
      v-model:open="fastInstallNode"
      destroy-on-close
      width="80%"
      :title="$t('pages.node.list.c0108a0')"
      :footer="null"
      :mask-closable="false"
      @cancel="
        () => {
          fastInstallNode = false
          loadData()
        }
      "
    >
      <fastInstall v-if="fastInstallNode"></fastInstall>
    </CustomModal>
    <!-- 同步到其他工作空间 -->
    <customModal
      v-if="syncToWorkspaceVisible"
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('pages.node.list.d3b55aa0')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-alert :message="$t('pages.node.list.d5e02e8a')" type="warning">
        <template #description>
          <ul>
            <li>{{ $t('pages.node.list.b74cd503') }}</li>
            <li>{{ $t('pages.node.list.852c00af') }}</li>
            <li>{{ $t('pages.node.list.c79623c0') }}</li>
          </ul>
        </template>
      </a-alert>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$t('pages.node.list.7ef9d8fb')" name="workspaceId">
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
            :placeholder="$t('pages.node.list.3a321a02')"
          >
            <a-select-option v-for="item in workspaceList" :key="item.id" :disabled="getWorkspaceId() === item.id">{{
              item.name
            }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </customModal>
    <!-- 历史监控 -->
    <customModal
      v-if="monitorVisible"
      v-model:open="monitorVisible"
      destroy-on-close
      width="75%"
      :title="`${temp.name}${$t('pages.node.list.d298e120')}`"
      :footer="null"
      :mask-closable="false"
    >
      <node-top v-if="monitorVisible" :type="temp.type" :node-id="temp.id"></node-top>
    </customModal>
  </div>
</template>
<script>
import { mapState } from 'pinia'
import { Empty } from 'ant-design-vue'
import {
  deleteNode,
  editNode,
  getNodeGroupAll,
  getNodeList,
  syncProject,
  syncToWorkspace,
  unbind,
  sortItem
} from '@/api/node'
import { getSshListAll } from '@/api/ssh'
import { syncScript } from '@/api/node-other'
import NodeFunc from './node-func'
import Terminal1 from '@/pages/ssh/terminal'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  formatDuration,
  renderSize,
  formatPercent2Number,
  parseTime
  // PAGE_DEFAULT_SHOW_TOTAL,
  // getCachePageLimit
} from '@/utils/const'
import { getWorkSpaceListAll } from '@/api/workspace'
import CustomSelect from '@/components/customSelect'
import fastInstall from './fast-install.vue'
import { statusMap } from '@/api/system/assets-machine'
import NodeTop from '@/pages/node/node-layout/node-top'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
export default {
  components: {
    NodeFunc,
    Terminal1,
    CustomSelect,
    fastInstall,
    NodeTop
  },
  data() {
    return {
      loading: true,
      Empty,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      statusMap,
      sshList: [],
      list: [],
      sizeOptions: ['8', '12', '16', '20', '24'],
      groupList: [],
      // refreshInterval: 5,
      // deadline: 0,
      temp: {},
      monitorVisible: false,
      layoutType: null,
      editNodeVisible: false,
      drawerVisible: false,
      terminalVisible: false,

      fastInstallNode: false,
      syncToWorkspaceVisible: false,

      columns: [
        {
          title: this.$t('pages.node.list.3614bbe4'),
          dataIndex: 'name',
          width: 200,
          sorter: true,
          key: 'name',
          ellipsis: true
        },
        {
          title: this.$t('pages.node.list.969229b3'),
          dataIndex: 'status',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('pages.node.list.f48a6e61'),
          dataIndex: 'url',
          key: 'url',
          width: '190px',
          ellipsis: true
        },
        {
          title: this.$t('pages.node.list.163de925'),
          dataIndex: 'osName',
          key: 'osName',
          width: '100px',
          ellipsis: true
        },
        {
          title: `JDK ${this.$t('pages.node.list.d826aba2')}`,
          dataIndex: 'javaVersion',
          width: 100,
          key: 'javaVersion',
          ellipsis: true
        },
        {
          title: `JVM ${this.$t('pages.node.list.2edcd34c')}`,
          dataIndex: 'jvmInfo',
          width: 100,
          ellipsis: true
        },
        // { title: "JVM 剩余内存", dataIndex: "machineNodeData.jvmFreeMemory", ellipsis: true, },

        {
          title: this.$t('pages.node.list.293598cd'),
          dataIndex: 'jpomProjectCount',
          width: '90px'
        },
        {
          title: this.$t('pages.node.list.f680ea25'),
          dataIndex: 'jpomScriptCount',
          width: '90px'
        },

        {
          title: this.$t('pages.node.list.b4c3a25d'),
          dataIndex: 'runTime',
          width: '100px',
          key: 'runTime',
          ellipsis: true
        },
        {
          title: this.$t('pages.node.list.f5b90169'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.node.list.a2b40316'),
          dataIndex: 'modifyTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.node.list.f5049383'),
          dataIndex: 'sortValue',
          sorter: true,
          width: '80px'
        },
        {
          title: this.$t('pages.node.list.3bb962bf'),
          dataIndex: 'operation',
          key: 'operation',
          fixed: 'right',
          width: '200px',

          align: 'center'
        }
      ],

      rules: {
        name: [{ required: true, message: this.$t('pages.node.list.807342c3'), trigger: 'blur' }]
      },
      workspaceList: [],
      tableSelections: [],
      statValueStyle: {
        fontSize: '14px',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap'
      },
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useUserStore, ['getUserInfo']),
    ...mapState(useAppStore, ['getWorkspaceId']),
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
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
      if (this.listQuery.page !== 1 || this.listQuery.total > 0) {
        // 不是第一页 或者总记录数大于 0
        return false
      }
      // 判断是否存在搜索条件
      const nowKeys = Object.keys(this.listQuery)
      const defaultKeys = Object.keys(PAGE_DEFAULT_LIST_QUERY)
      const dictOrigin = nowKeys.filter((item) => !defaultKeys.includes(item))
      return dictOrigin.length === 0
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections
      }
    }
  },
  watch: {},
  created() {
    const searchNodeName = this.$route.query.searchNodeName
    if (searchNodeName) {
      this.listQuery = { ...this.listQuery, '%name%': searchNodeName }
    }

    this.loadData()
    this.loadGroupList()
  },

  methods: {
    formatDuration,
    renderSize,
    // PAGE_DEFAULT_SHOW_TOTAL,
    parseTime,
    CHANGE_PAGE,
    // 获取所有的分组
    loadGroupList() {
      getNodeGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 加载 SSH 列表
    loadSshList() {
      getSshListAll().then((res) => {
        if (res.code === 200) {
          this.sshList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      return new Promise((resolve) => {
        this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
        this.loading = true
        getNodeList(this.listQuery)
          .then((res) => {
            if (res.code === 200) {
              this.list =
                res.data.result &&
                res.data.result.map((item) => {
                  // console.log(item);
                  item.occupyCpu = formatPercent2Number(item.machineNodeData?.osOccupyCpu)

                  item.occupyDisk = formatPercent2Number(item.machineNodeData?.osOccupyDisk)
                  item.occupyMemory = formatPercent2Number(item.machineNodeData?.osOccupyMemory)
                  return item
                })
              this.listQuery.total = res.data.total
              let nodeId = this.$route.query.nodeId
              this.list.map((item) => {
                if (nodeId === item.id) {
                  this.handleNode(item)
                }
              })

              resolve()
              // this.refreshInterval = 30
              // this.deadline = Date.now() + this.refreshInterval * 1000
            }
          })
          .finally(() => {
            this.loading = false
          })
      })
    },

    // 进入终端
    handleTerminal(record) {
      this.temp = Object.assign({}, record)
      this.terminalVisible = true
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      this.loadSshList()
      // this.temp.tempGroup = "";
      this.editNodeVisible = true
    },
    // 提交节点数据
    handleEditNodeOk() {
      // 检验表单
      this.$refs['editNodeForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        editNode(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editNodeForm'].resetFields()
              this.editNodeVisible = false
              this.loadData()
              this.loadGroupList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    handleDelete(record) {
      $confirm({
        title: this.$t('pages.node.list.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.node.list.6a7b3336'),
        okText: this.$t('pages.node.list.7da4a591'),
        cancelText: this.$t('pages.node.list.43105e21'),
        onOk: () => {
          return deleteNode(record.id).then((res) => {
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
    // 解绑
    handleUnbind(record) {
      const html = `
      <b style='font-size: 20px;'>${this.$t('pages.node.list.6d453736')}</b>
      <ul style='font-size: 20px;color:red;font-weight: bold;'>
        <li>${this.$t('pages.node.list.95f857c5')}</li>
        <li>${this.$t('pages.node.list.260e16bd')}</li>
        <li>${this.$t('pages.node.list.f9775b85')}</li>
      </ul>

      `
      $confirm({
        title: this.$t('pages.node.list.cd503941'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { size: 'small', danger: true, type: 'primary' },
        cancelButtonProps: { type: 'primary' },
        okText: this.$t('pages.node.list.7da4a591'),
        cancelText: this.$t('pages.node.list.43105e21'),
        onOk: () => {
          return unbind(record.id).then((res) => {
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
    // 管理节点
    handleNode(record) {
      this.temp = Object.assign({}, record)

      this.drawerVisible = true
      let nodeId = this.$route.query.nodeId
      if (nodeId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, nodeId: record.id }
        })
      }
    },
    syncNode(node) {
      syncProject(node.id).then((res) => {
        if (res.code == 200) {
          $notification.success({
            message: res.msg
          })
          return false
        }
      })
    },
    syncNodeScript(node) {
      syncScript({
        nodeId: node.id
      }).then((res) => {
        if (res.code == 200) {
          $notification.success({
            message: res.msg
          })
        }
      })
    },
    // 关闭抽屉层
    onClose() {
      this.drawerVisible = false
      let query = Object.assign({}, this.$route.query)
      delete query.nodeId, delete query.id, delete query.pId
      this.$router.replace({
        query: query
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
    syncToWorkspaceShow() {
      this.syncToWorkspaceVisible = true
      this.loadWorkSpaceListAll()
      this.temp = {
        workspaceId: undefined
      }
    },
    //
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: this.$t('pages.node.list.3a321a02')
        })
        return false
      }
      // 同步
      this.confirmLoading = true
      syncToWorkspace({
        ids: this.tableSelections.join(','),
        toWorkspaceId: this.temp.workspaceId
      })
        .then((res) => {
          if (res.code == 200) {
            $notification.success({
              message: res.msg
            })
            this.tableSelections = []
            this.syncToWorkspaceVisible = false
            return false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 排序
    sortItemHander(record, index, method) {
      const msgData = {
        top: this.$t('pages.node.list.a3089bc1'),
        up: this.$t('pages.node.list.2b25234c'),
        down: this.$t('pages.node.list.8ad93f78')
      }
      let msg = msgData[method] || this.$t('pages.node.list.644ef343')
      if (!record.sortValue) {
        msg += `${this.$t('pages.node.list.8a8e4923')},${this.$t('pages.node.list.4deb73b2')},${this.$t(
          'pages.node.list.d9c5971d'
        )}`
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.list[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      $confirm({
        title: this.$t('pages.node.list.a8fe4c17'),
        zIndex: 1009,
        content: msg,
        okText: this.$t('pages.node.list.7da4a591'),
        cancelText: this.$t('pages.node.list.43105e21'),
        onOk: () => {
          return sortItem({
            id: record.id,
            method: method,
            compareId: compareId
          }).then((res) => {
            if (res.code == 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // // 切换视图
    // changeLayout() {
    //   if (!this.layoutType) {
    //     const layoutType = localStorage.getItem('tableLayout')
    //     // 默认表格
    //     this.layoutType = layoutType === 'card' ? 'card' : 'table'
    //   } else {
    //     this.layoutType = this.layoutType === 'card' ? 'table' : 'card'
    //     localStorage.setItem('tableLayout', this.layoutType)
    //   }
    //   this.listQuery = {
    //     ...this.listQuery,
    //     limit: this.layoutType === 'card' ? 8 : getCachePageLimit()
    //   }
    //   this.loadData()
    // },
    onFinish() {
      if (this.drawerVisible) {
        // 打开节点 不刷新
        return
      }
      if (this.$attrs.routerUrl !== this.$route.path) {
        // 重新计算倒计时
        // this.deadline = Date.now() + this.refreshInterval * 1000
        return
      }
      this.loadData()
    },
    // 历史图表
    handleHistory(record, type) {
      this.monitorVisible = true
      this.temp = record
      this.temp = { ...this.temp, type }
    },
    fastInstallNodeShow() {
      this.fastInstallNode = true
    }
  }
}
</script>
