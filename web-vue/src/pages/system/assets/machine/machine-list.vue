<template>
  <div>
    <!-- <a-card :body-style="{ padding: '10px' }"> -->
    <!-- 卡片视图 -->
    <!-- <template v-if="layoutType === 'card'"> </template> -->
    <!-- 表格视图 -->
    <!-- <template v-else-if="layoutType === 'table'"> -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      table-name="buildList"
      :empty-description="$t('pages.system.assets.machine.machine-list.90ec9874')"
      :active-page="activePage"
      :columns="columns"
      :data-source="list"
      bordered
      size="middle"
      row-key="id"
      :pagination="pagination"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="getMachineList"
      @change-table-layout="
        (layoutType) => {
          tableSelections = []
          listQuery = CHANGE_PAGE(listQuery, {
            pagination: { limit: layoutType === 'card' ? 8 : 10 }
          })
          getMachineList()
        }
      "
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            class="search-input-item"
            :placeholder="$t('pages.system.assets.machine.machine-list.2c9fdbef')"
            @press-enter="getMachineList"
          />
          <a-input
            v-model:value="listQuery['%jpomUrl%']"
            class="search-input-item"
            :placeholder="$t('pages.system.assets.machine.machine-list.a918ed23')"
            @press-enter="getMachineList"
          />
          <a-input
            v-model:value="listQuery['%jpomVersion%']"
            class="search-input-item"
            :placeholder="$t('pages.system.assets.machine.machine-list.24fef8f1')"
            @press-enter="getMachineList"
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
            :placeholder="$t('pages.system.assets.machine.machine-list.e740d8cb')"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery['order_field']"
            allow-clear
            :placeholder="$t('pages.system.assets.machine.machine-list.62d392df')"
            class="search-input-item"
          >
            <a-select-option value="networkDelay">{{
              $t('pages.system.assets.machine.machine-list.5cba575b')
            }}</a-select-option>
            <a-select-option value="osOccupyCpu">cpu</a-select-option>
            <a-select-option value="osOccupyDisk">{{
              $t('pages.system.assets.machine.machine-list.e9213f19')
            }}</a-select-option>
            <a-select-option value="osOccupyMemory">{{
              $t('pages.system.assets.machine.machine-list.d5f99ae')
            }}</a-select-option>
            <a-select-option value="modifyTimeMillis">{{
              $t('pages.system.assets.machine.machine-list.3d55d8de')
            }}</a-select-option>
            <a-select-option value="createTimeMillis">{{
              $t('pages.system.assets.machine.machine-list.8e3f3e65')
            }}</a-select-option>
          </a-select>
          <a-button :loading="loading" type="primary" @click="getMachineList">{{
            $t('pages.system.assets.machine.machine-list.53c2763c')
          }}</a-button>
          <a-button type="primary" @click="addMachine">{{
            $t('pages.system.assets.machine.machine-list.7d46652a')
          }}</a-button>

          <a-dropdown v-if="tableSelections && tableSelections.length">
            <template #overlay>
              <a-menu>
                <a-menu-item key="1" @click="syncToWorkspaceShow()">
                  {{ $t('pages.system.assets.machine.machine-list.777582f8') }}
                </a-menu-item>
                <a-menu-item key="2" @click="syncNodeWhiteConfig">
                  {{ $t('pages.system.assets.machine.machine-list.911b1498') }}
                </a-menu-item>
                <a-menu-item key="3" @click="syncNodeConfig">
                  {{ $t('pages.system.assets.machine.machine-list.dd3e77a3') }}
                </a-menu-item>
              </a-menu>
            </template>
            <a-button type="primary">
              {{ $t('pages.system.assets.machine.machine-list.766122c2') }} <DownOutlined />
            </a-button>
          </a-dropdown>
          <a-tooltip v-else :title="$t('pages.system.assets.machine.machine-list.db24fc53')">
            <a-button :disabled="true" type="primary">
              {{ $t('pages.system.assets.machine.machine-list.766122c2') }}<DownOutlined
            /></a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <ul>
              <li>{{ $t('pages.system.assets.machine.machine-list.1a699f77') }}</li>
              <li>{{ $t('pages.system.assets.machine.machine-list.de2a9a7b') }}</li>
              <li>{{ $t('pages.system.assets.machine.machine-list.11dc9ca6') }}</li>
            </ul>
          </template>
          <QuestionCircleOutlined />
        </a-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip :title="text">
            <a-button style="padding: 0" type="link" size="small" @click="showMachineInfo(record)">
              {{ text }}
            </a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <a-tooltip :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip
            :title="`${$t('pages.system.assets.machine.machine-list.df91bc15')}${statusMap[record.status]} ${
              record.statusMsg
                ? $t('pages.system.assets.machine.machine-list.b062018a') + record.statusMsg
                : $t('pages.system.assets.machine.machine-list.2a5a5df2')
            } `"
          >
            <a-tag :color="record.status === 1 ? 'green' : 'pink'" style="margin-right: 0">
              {{ statusMap[record.status] }}
            </a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.duration">
          <a-tooltip placement="topLeft" :title="formatDuration(text)">
            <span>{{ formatDuration(text, '', 2) }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.duration2">
          <a-tooltip placement="topLeft" :title="formatDuration((text || 0) * 1000)">
            <span>{{ formatDuration((text || 0) * 1000, '', 2) }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.percent2Number">
          <a-tooltip placement="topLeft" :title="`${(text && formatPercent2Number(text) + '%') || '-'}`">
            <span>{{ (text && formatPercent2Number(text) + '%') || '-' }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleEdit(record)">{{
              $t('pages.system.assets.machine.machine-list.e1224c34')
            }}</a-button>
            <a-button type="primary" size="small" @click="syncToWorkspaceShow(record)">{{
              $t('pages.system.assets.machine.machine-list.672432f2')
            }}</a-button>
            <a-button type="primary" danger size="small" @click="deleteMachineInfo(record)">{{
              $t('pages.system.assets.machine.machine-list.2f14e7d4')
            }}</a-button>
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
                    <div>{{ $t('pages.system.assets.machine.machine-list.3614bbe4') }}{{ item.name }}</div>
                    <div>{{ $t('pages.system.assets.machine.machine-list.a918ed23') }}：{{ item.jpomUrl }}</div>
                  </template>
                  <span style="cursor: pointer" @click="showMachineInfo(item)">
                    {{ item.name }}
                  </span>
                </a-tooltip>
              </a-col>
              <a-col :span="7" style="text-align: right" class="text-overflow-hidden">
                <a-tooltip
                  :title="`${$t('pages.system.assets.machine.machine-list.df91bc15')}${statusMap[item.status]} ${
                    item.statusMsg
                      ? $t('pages.system.assets.machine.machine-list.b062018a') + item.statusMsg
                      : $t('pages.system.assets.machine.machine-list.2a5a5df2')
                  } `"
                >
                  <a-tag :color="item.status === 1 ? 'green' : 'pink'" style="margin-right: 0">
                    {{ statusMap[item.status] }}</a-tag
                  >
                </a-tooltip>
              </a-col>
            </a-row>
          </template>

          <a-tooltip :title="item.osName">
            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">{{
                $t('pages.system.assets.machine.machine-list.163de925')
              }}</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                <a-button
                  :disabled="!item.osName"
                  style="padding: 0; height: auto"
                  type="link"
                  size="small"
                  @click="showMachineInfo(item)"
                >
                  {{ item.osName || '-' }}
                </a-button>
              </a-col>
            </a-row>
          </a-tooltip>
          <a-tooltip :title="item.osVersion">
            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">{{
                $t('pages.system.assets.machine.machine-list.41180a8c')
              }}</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                {{ item.osVersion || '-' }}
              </a-col>
            </a-row>
          </a-tooltip>
          <a-tooltip :title="item.osLoadAverage">
            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">{{
                $t('pages.system.assets.machine.machine-list.6d18733e')
              }}</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                {{ item.osLoadAverage || '-' }}
              </a-col>
            </a-row>
          </a-tooltip>
          <a-tooltip :title="item.jpomVersion">
            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">{{
                $t('pages.system.assets.machine.machine-list.e2009285')
              }}</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                <a-button
                  :disabled="!item.jpomVersion"
                  style="padding: 0; height: auto"
                  type="link"
                  size="small"
                  @click="showMachineUpgrade(item)"
                >
                  {{ item.jpomVersion || '-' }}
                </a-button>
              </a-col>
            </a-row>
          </a-tooltip>
          <a-row type="flex" align="middle" justify="center" style="margin-top: 10px">
            <a-button-group>
              <a-button type="primary" size="small" @click="handleEdit(item)">
                {{ $t('pages.system.assets.machine.machine-list.e1224c34') }}
              </a-button>
              <a-button type="primary" size="small" @click="showMachineInfo(item)">{{
                $t('pages.system.assets.machine.machine-list.151c73eb')
              }}</a-button>
              <a-button type="primary" size="small" @click="syncToWorkspaceShow(item)">{{
                $t('pages.system.assets.machine.machine-list.672432f2')
              }}</a-button>
              <a-button type="primary" size="small" @click="viewMachineNode(item)">{{
                $t('pages.system.assets.machine.machine-list.602a0a5e')
              }}</a-button>
              <a-button size="small" @click="deleteMachineInfo(item)">{{
                $t('pages.system.assets.machine.machine-list.2f14e7d4')
              }}</a-button>
            </a-button-group>
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
                  getMachineList()
                }
              "
              @change="getMachineList"
            />
          </a-col> </a-row
      ></template> -->
    </CustomTable>
    <!-- </template> -->
    <!-- </a-card> -->
    <!-- 编辑区 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50%"
      :title="$t('pages.system.assets.machine.machine-list.22013d63')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editNodeForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
        <a-form-item :label="$t('pages.system.assets.machine.machine-list.2c9fdbef')" name="name">
          <a-input
            v-model:value="temp.name"
            :max-length="50"
            :placeholder="$t('pages.system.assets.machine.machine-list.2c9fdbef')"
          />
        </a-form-item>
        <a-form-item :label="$t('pages.system.assets.machine.machine-list.aeb7bb0b')" name="groupName">
          <custom-select
            v-model:value="temp.groupName"
            :data="groupList"
            :input-placeholder="$t('pages.system.assets.machine.machine-list.c50ead9c')"
            :select-placeholder="$t('pages.system.assets.machine.machine-list.c385f859')"
          >
          </custom-select>
        </a-form-item>

        <a-form-item name="jpomUrl">
          <template #label>
            <a-tooltip>
              {{ $t('pages.system.assets.machine.machine-list.a918ed23') }}
              <template #title
                >{{ $t('pages.system.assets.machine.machine-list.2571644c') }}3
                <ul>
                  <li>{{ $t('pages.system.assets.machine.machine-list.e09a5dce') }}</li>
                  <li>{{ $t('pages.system.assets.machine.machine-list.77cbe2a0') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <template #help>{{ $t('pages.system.assets.machine.machine-list.7b7aa723') }}</template>
          <a-input v-model:value="temp.jpomUrl" :placeholder="$t('pages.system.assets.machine.machine-list.da42e57d')">
            <template #addonBefore>
              <a-select
                v-model:value="temp.jpomProtocol"
                :placeholder="$t('pages.system.assets.machine.machine-list.66b6ce82')"
                style="width: 160px"
              >
                <a-select-option value="Http"> Http:// </a-select-option>
                <a-select-option value="Https"> Https:// </a-select-option>
              </a-select>
            </template>
          </a-input>
        </a-form-item>

        <a-form-item :label="$t('pages.system.assets.machine.machine-list.d0ad1e12')" name="loginName">
          <a-input
            v-model:value="temp.jpomUsername"
            :placeholder="$t('pages.system.assets.machine.machine-list.2dea28d3')"
          />
          <template #help>{{ $t('pages.system.assets.machine.machine-list.2b392488') }}</template>
        </a-form-item>
        <a-form-item :name="`${temp.id ? 'loginPwd-update' : 'loginPwd'}`">
          <template #label>
            <a-tooltip>
              {{ $t('pages.system.assets.machine.machine-list.176dcb24') }}
              <template #title>
                {{ $t('pages.system.assets.machine.machine-list.8657af17') }}_authorize.json
                {{ $t('pages.system.assets.machine.machine-list.635f865') }}
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-input-password
            v-model:value="temp.jpomPassword"
            :placeholder="$t('pages.system.assets.machine.machine-list.8a914f8c')"
          />
        </a-form-item>

        <a-collapse>
          <a-collapse-panel key="1" :header="$t('pages.system.assets.machine.machine-list.1eadb274')">
            <a-form-item :label="$t('pages.system.assets.machine.machine-list.d50785bc')" name="templateNode" help="">
              <a-switch
                v-model:checked="temp.templateNode"
                :checked-children="$t('pages.system.assets.machine.machine-list.1998fedc')"
                :un-checked-children="$t('pages.system.assets.machine.machine-list.90612f8a')"
                default-checked
              />
              {{ $t('pages.system.assets.machine.machine-list.b19e63c8') }},{{
                $t('pages.system.assets.machine.machine-list.d7d0c747')
              }}
            </a-form-item>

            <a-form-item :label="$t('pages.system.assets.machine.machine-list.e67b0df3')" name="timeOut">
              <a-input-number
                v-model:value="temp.jpomTimeout"
                :min="0"
                :placeholder="$t('pages.system.assets.machine.machine-list.65206e07')"
                style="width: 100%"
              />
            </a-form-item>

            <a-form-item :label="$t('pages.system.assets.machine.machine-list.f9f255b0')" name="jpomHttpProxy">
              <a-input
                v-model:value="temp.jpomHttpProxy"
                :placeholder="$t('pages.system.assets.machine.machine-list.775fdae9')"
              >
                <template #addonBefore>
                  <a-select
                    v-model:value="temp.jpomHttpProxyType"
                    :placeholder="$t('pages.system.assets.machine.machine-list.b9029351')"
                    default-value="HTTP"
                    style="width: 100px"
                  >
                    <a-select-option value="HTTP">HTTP</a-select-option>
                    <a-select-option value="SOCKS">SOCKS</a-select-option>
                    <a-select-option value="DIRECT">DIRECT</a-select-option>
                  </a-select>
                </template>
              </a-input>
            </a-form-item>

            <a-form-item :label="$t('pages.system.assets.machine.machine-list.529a92f9')" name="transportEncryption">
              <a-select
                v-model:value="temp.transportEncryption"
                show-search
                default-value="0"
                :placeholder="$t('pages.system.assets.machine.machine-list.bce1535d')"
              >
                <a-select-option :value="0">{{
                  $t('pages.system.assets.machine.machine-list.339ddd8c')
                }}</a-select-option>
                <a-select-option :value="1">BASE64</a-select-option>
                <a-select-option :value="2">AES</a-select-option>
              </a-select>
            </a-form-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form>
    </a-modal>
    <!-- 机器信息组件 -->

    <machine-info
      v-if="drawerVisible"
      :machine-id="temp.id"
      :name="temp.name"
      @close="
        () => {
          drawerVisible = false
        }
      "
    />
    <!-- 机器在线升级相关信息 -->
    <machine-info
      v-if="drawerUpgradeVisible"
      :machine-id="temp.id"
      :name="temp.name"
      tab="upgrade"
      @close="
        () => {
          drawerUpgradeVisible = false
        }
      "
    />

    <!-- 分配到其他工作空间 -->
    <a-modal
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('pages.system.assets.machine.machine-list.1a45dd32')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item :label="$t('pages.system.assets.machine.machine-list.7ef9d8fb')" name="workspaceId">
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
            :placeholder="$t('pages.system.assets.machine.machine-list.3a321a02')"
          >
            <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 查看机器关联节点 -->
    <a-modal
      v-model:open="viewLinkNode"
      destroy-on-close
      width="50%"
      :title="$t('pages.system.assets.machine.machine-list.2e8b9ada')"
      :footer="null"
      :mask-closable="false"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert
          v-if="nodeList && nodeList.length"
          :message="$t('pages.system.assets.machine.machine-list.fcea55f5')"
          type="info"
          show-icon
        />
        <a-list bordered :data-source="nodeList">
          <template #renderItem="{ item }">
            <a-list-item style="display: block">
              <a-row>
                <a-col :span="10">{{ $t('pages.system.assets.machine.machine-list.3614bbe4') }}{{ item.name }}</a-col>
                <a-col :span="10"
                  >{{ $t('pages.system.assets.machine.machine-list.76d6b646')
                  }}{{ item.workspace && item.workspace.name }}</a-col
                >
                <a-col :span="4">
                  <a-button type="link" @click="toNode(item.id, item.name, item.workspace && item.workspace.id)">
                    <LoginOutlined /> </a-button
                ></a-col>
              </a-row>
            </a-list-item>
          </template>
        </a-list>
      </a-space>
    </a-modal>
    <!-- 分发节点授权 -->
    <a-modal
      v-model:open="whiteConfigVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50%"
      :title="$t('pages.system.assets.machine.machine-list.8e66162a')"
      :mask-closable="false"
      @ok="onSubmitWhitelist"
    >
      <a-alert
        :message="`${$t('pages.system.assets.machine.machine-list.ccba8d7f')},${$t(
          'pages.system.assets.machine.machine-list.d732193c'
        )},${$t('pages.system.assets.machine.machine-list.f9c4e189')}`"
        style="margin-top: 10px; margin-bottom: 20px"
        banner
      />
      <a-form ref="editWhiteForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item :label="$t('pages.system.assets.machine.machine-list.d50785bc')">
          <a-select
            v-model:value="temp.templateNodeId"
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
            :placeholder="$t('pages.system.assets.machine.machine-list.16c9816c')"
            @change="(id) => loadWhitelistData(id)"
          >
            <a-select-option v-for="item in templateNodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('pages.system.assets.machine.machine-list.5b716424')" name="project">
          <a-textarea
            v-model:value="temp.project"
            :rows="5"
            style="resize: none"
            :placeholder="$t('pages.system.assets.machine.machine-list.f2ff8439')"
          />
        </a-form-item>

        <a-form-item :label="$t('pages.system.assets.machine.machine-list.5ad2c7b8')" name="allowEditSuffix">
          <a-textarea
            v-model:value="temp.allowEditSuffix"
            :rows="5"
            style="resize: none"
            :placeholder="$t('pages.system.assets.machine.machine-list.70f03f24')"
          />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 分发机器配置 -->
    <CustomModal
      v-model:open="nodeConfigVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50%"
      :title="$t('pages.system.assets.machine.machine-list.19334ef6')"
      :mask-closable="false"
    >
      <template #footer>
        <a-space>
          <a-button type="primary" :disabled="!temp.content" @click="onNodeSubmit(false)">{{
            $t('pages.system.assets.machine.machine-list.b033d8c5')
          }}</a-button>
          <a-button type="primary" :disabled="!temp.content" @click="onNodeSubmit(true)">{{
            $t('pages.system.assets.machine.machine-list.fda40980')
          }}</a-button>
        </a-space>
      </template>
      <a-alert
        :message="`${$t('pages.system.assets.machine.machine-list.666f85d5')},${$t(
          'pages.system.assets.machine.machine-list.d732193c'
        )},${$t('pages.system.assets.machine.machine-list.f9c4e189')}`"
        style="margin-top: 10px; margin-bottom: 20px"
        banner
      />
      <a-form ref="editNodeConfigForm" :model="temp">
        <a-form-item :label="$t('pages.system.assets.machine.machine-list.cfd49c0b')">
          <a-select
            v-model:value="temp.templateNodeId"
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
            :placeholder="$t('pages.system.assets.machine.machine-list.43c9b591')"
            @change="(id) => loadNodeConfig(id)"
          >
            <a-select-option v-for="item in templateNodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :wrapper-col="{ span: 24 }">
          <code-editor
            v-model:content="temp.content"
            height="40vh"
            :options="{ mode: 'yaml', tabSize: 2 }"
          ></code-editor>
        </a-form-item>
      </a-form>
    </CustomModal>
  </div>
</template>
<script>
import {
  machineListData,
  machineListGroup,
  statusMap,
  machineEdit,
  machineDelete,
  machineDistribute,
  machineListNode,
  machineListTemplateNode,
  saveWhitelist,
  saveNodeConfig
} from '@/api/system/assets-machine'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  // PAGE_DEFAULT_SHOW_TOTAL,
  formatDuration,
  parseTime,
  formatPercent2Number
  // getCachePageLimit
} from '@/utils/const'
import CustomSelect from '@/components/customSelect'
import { useAppStore } from '@/stores/app'
import { mapState } from 'pinia'
import machineInfo from './machine-func'
import { getWorkSpaceListAll } from '@/api/workspace'
// import Upgrade from "@/pages/node/node-layout/system/upgrade.vue";

import { getWhiteList } from '@/api/node-system'
import { getConfigData } from '@/api/system'
import codeEditor from '@/components/codeEditor'

export default {
  components: {
    CustomSelect,
    machineInfo,

    codeEditor
  },
  data() {
    return {
      statusMap,
      listQuery: Object.assign({ order: 'descend', order_field: 'networkDelay' }, PAGE_DEFAULT_LIST_QUERY, {}),
      // sizeOptions: ['8', '12', '16', '20', '24'],
      list: [],
      groupList: [],
      loading: true,
      editVisible: false,
      syncToWorkspaceVisible: false,
      temp: {},
      rules: {
        name: [
          { required: true, message: this.$t('pages.system.assets.machine.machine-list.255211fb'), trigger: 'blur' }
        ]
      },
      drawerVisible: false,
      drawerUpgradeVisible: false,
      workspaceList: [],
      viewLinkNode: false,
      nodeList: [],
      layoutType: null,
      columns: [
        {
          title: this.$t('pages.system.assets.machine.machine-list.bb769c1d'),
          dataIndex: 'name',
          width: 150,
          ellipsis: true
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.1c01cf58'),
          dataIndex: 'osName',
          width: 150,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.7fb10499'),
          dataIndex: 'hostName',
          width: 150,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.a918ed23'),
          dataIndex: 'jpomUrl',
          width: 150,
          sorter: true,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.12d0e469'),
          dataIndex: 'groupName',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.9c32c887'),
          dataIndex: 'status',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.abd23cf2'),
          sorter: true,
          dataIndex: 'osSystemUptime',
          width: 150,
          ellipsis: true,
          duration2: true
        },
        {
          title: `CPU${this.$t('pages.system.assets.machine.machine-list.5a6bc27e')}`,
          sorter: true,
          align: 'center',
          dataIndex: 'osOccupyCpu',
          width: '100px',
          ellipsis: true,
          percent2Number: true
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.10917337'),
          sorter: true,
          align: 'center',
          dataIndex: 'osOccupyMemory',
          width: '100px',
          ellipsis: true,
          percent2Number: true
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.62a10802'),
          sorter: true,
          align: 'center',
          dataIndex: 'osOccupyDisk',
          width: '100px',
          ellipsis: true,
          percent2Number: true
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.93e5dd72'),
          dataIndex: 'jpomVersion',
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.d50785bc'),
          dataIndex: 'templateNode',
          width: '90px',
          align: 'center',
          ellipsis: true,
          customRender: ({ text }) => {
            return text
              ? this.$t('pages.system.assets.machine.machine-list.1998fedc')
              : this.$t('pages.system.assets.machine.machine-list.90612f8a')
          }
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.8e3f3e65'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.a2b40316'),
          dataIndex: 'modifyTimeMillis',
          customRender: ({ text }) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: this.$t('pages.system.assets.machine.machine-list.3bb962bf'),
          dataIndex: 'operation',
          width: '120px',
          fixed: 'right',
          align: 'center'
        }
      ],

      tableSelections: [],
      whiteConfigVisible: false,
      nodeConfigVisible: false,
      templateNodeList: [],
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useAppStore, ['getCollapsed']),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
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
  mounted() {
    this.loadGroupList()
    this.getMachineList()
  },
  methods: {
    parseTime,
    formatDuration,
    formatPercent2Number,
    CHANGE_PAGE,
    // PAGE_DEFAULT_SHOW_TOTAL,
    // getCachePageLimit,
    // 获取所有的分组
    loadGroupList() {
      machineListGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    getMachineList(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      machineListData(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.getMachineList()
    },
    addMachine() {
      this.temp = {
        // 默认设置节点地址协议
        jpomProtocol: 'Http'
      }
      this.editVisible = true
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      delete this.temp.statusMsg
      this.editVisible = true
    },
    // 提交节点数据
    handleEditOk() {
      // 检验表单
      this.$refs['editNodeForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        machineEdit(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editNodeForm'].resetFields()
              this.editVisible = false
              this.loadGroupList()
              this.getMachineList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    showMachineInfo(item) {
      this.temp = { ...item }
      this.drawerVisible = true
    },
    // 删除机器
    deleteMachineInfo(item) {
      $confirm({
        title: this.$t('pages.system.assets.machine.machine-list.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.system.assets.machine.machine-list.2613d1af'),
        okText: this.$t('pages.system.assets.machine.machine-list.7da4a591'),
        cancelText: this.$t('pages.system.assets.machine.machine-list.43105e21'),
        onOk: () =>
          machineDelete({
            id: item.id
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.getMachineList()
            }
          })
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
          ids: item.id
        }
      }
    },
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: this.$t('pages.system.assets.machine.machine-list.3a321a02')
        })
        return false
      }
      if (!this.temp.ids) {
        this.temp = { ...this.temp, ids: this.tableSelections.join(',') }
        this.tableSelections = []
      }
      // 同步
      this.confirmLoading = true
      machineDistribute(this.temp)
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
    // 显示节点版本信息
    showMachineUpgrade(item) {
      this.temp = { ...item }
      this.drawerUpgradeVisible = true
    },
    // 查看机器关联的节点
    viewMachineNode(item) {
      machineListNode({
        id: item.id
      }).then((res) => {
        if (res.code === 200) {
          this.viewLinkNode = true
          this.nodeList = res.data
        }
      })
    },
    toNode(nodeId, name, wid) {
      const newpage = this.$router.resolve({
        path: '/node/list',
        query: {
          ...this.$route.query,
          nodeId: nodeId,
          pId: 'manage',
          id: 'manageList',
          wid: wid,
          searchNodeName: name
        }
      })
      window.open(newpage.href, '_blank')
    },

    syncNodeWhiteConfig() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warn({
          message: this.$t('pages.system.assets.machine.machine-list.68a8edd4')
        })
        return
      }
      machineListTemplateNode().then((res) => {
        //
        if (res.code === 200) {
          if (res.data && res.data.length) {
            this.whiteConfigVisible = true
            this.templateNodeList = res.data
            this.temp = {
              ...this.temp,
              templateNodeId: this.templateNodeList[0].id
            }
            this.loadWhitelistData(this.temp.templateNodeId)
          } else {
            $notification.warn({
              message: this.$t('pages.system.assets.machine.machine-list.e1a176fb')
            })
          }
        }
      })
    },

    // 加载节点授权分发配置
    loadWhitelistData(id) {
      getWhiteList({
        machineId: id
      }).then((res) => {
        if (res.code === 200 && res.data) {
          this.temp = Object.assign({}, this.temp, res.data)
          // { ...thie.temp,res.data };
        }
      })
    },
    onSubmitWhitelist() {
      this.confirmLoading = true
      saveWhitelist({
        ...this.temp,
        ids: this.tableSelections.join(',')
      })
        .then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })
            this.tableSelections = []
            this.whiteConfigVisible = false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    syncNodeConfig() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warn({
          message: this.$t('pages.system.assets.machine.machine-list.2ee12552')
        })
        return
      }
      machineListTemplateNode().then((res) => {
        //
        if (res.code === 200) {
          if (res.data && res.data.length) {
            this.nodeConfigVisible = true
            this.templateNodeList = res.data
            this.temp = {
              ...this.temp,
              templateNodeId: this.templateNodeList[0].id
            }
            this.loadNodeConfig(this.temp.templateNodeId)
          } else {
            $notification.warn({
              message: this.$t('pages.system.assets.machine.machine-list.e1a176fb')
            })
          }
        }
      })
    },

    // 修改模版节点
    loadNodeConfig(id) {
      getConfigData({ machineId: id }).then((res) => {
        if (res.code === 200) {
          this.temp = { ...this.temp, content: res.data.content }
        }
      })
    },
    // submit
    onNodeSubmit(restart) {
      $confirm({
        title: this.$t('pages.system.assets.machine.machine-list.a8fe4c17'),
        content: restart
          ? this.$t('pages.system.assets.machine.machine-list.b812712c')
          : this.$t('pages.system.assets.machine.machine-list.85768a41'),
        okText: this.$t('pages.system.assets.machine.machine-list.7da4a591'),
        zIndex: 1009,
        cancelText: this.$t('pages.system.assets.machine.machine-list.43105e21'),
        onOk: () => {
          this.confirmLoading = true
          return saveNodeConfig({
            ...this.temp,
            restart: restart,
            ids: this.tableSelections.join(',')
          })
            .then((res) => {
              if (res.code === 200) {
                // 成功
                $notification.success({
                  message: res.msg
                })
                this.nodeConfigVisible = false
                this.tableSelections = []
              }
            })
            .finally(() => {
              this.confirmLoading = false
            })
        }
      })
    }
  }
}
</script>
<style scoped>
.item-info {
  padding: 4px 0;
}
.item-info .title {
}
.item-info .content {
}
</style>
