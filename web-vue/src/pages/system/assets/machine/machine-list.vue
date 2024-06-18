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
      :empty-description="$t('i18n_de3394b14e')"
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
            :placeholder="$t('i18n_e4013f8b81')"
            @press-enter="getMachineList"
          />
          <a-input
            v-model:value="listQuery['%jpomUrl%']"
            class="search-input-item"
            :placeholder="$t('i18n_c1786d9e11')"
            @press-enter="getMachineList"
          />
          <a-input
            v-model:value="listQuery['%jpomVersion%']"
            class="search-input-item"
            :placeholder="$t('i18n_a912a83e6f')"
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
            :placeholder="$t('i18n_829abe5a8d')"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery['order_field']"
            allow-clear
            :placeholder="$t('i18n_88f5c7ac4a')"
            class="search-input-item"
          >
            <a-select-option value="networkDelay">{{ $t('i18n_204222d167') }}</a-select-option>
            <a-select-option value="osOccupyCpu">cpu</a-select-option>
            <a-select-option value="osOccupyDisk">{{ $t('i18n_1d650a60a5') }}</a-select-option>
            <a-select-option value="osOccupyMemory">{{ $t('i18n_9932551cd5') }}</a-select-option>
            <a-select-option value="modifyTimeMillis">{{ $t('i18n_a001a226fd') }}</a-select-option>
            <a-select-option value="createTimeMillis">{{ $t('i18n_eca37cb072') }}</a-select-option>
          </a-select>
          <a-button :loading="loading" type="primary" @click="getMachineList">{{ $t('i18n_e5f71fc31e') }}</a-button>
          <a-button type="primary" @click="addMachine">{{ $t('i18n_66ab5e9f24') }}</a-button>

          <a-dropdown v-if="tableSelections && tableSelections.length">
            <template #overlay>
              <a-menu>
                <a-menu-item key="1" @click="syncToWorkspaceShow()">
                  {{ $t('i18n_5c89a5353d') }}
                </a-menu-item>
                <a-menu-item key="2" @click="syncNodeWhiteConfig">
                  {{ $t('i18n_542a0e7db4') }}
                </a-menu-item>
                <a-menu-item key="3" @click="syncNodeConfig">
                  {{ $t('i18n_51c92e6956') }}
                </a-menu-item>
              </a-menu>
            </template>
            <a-button type="primary"> {{ $t('i18n_7f7c624a84') }} <DownOutlined /> </a-button>
          </a-dropdown>
          <a-tooltip v-else :title="$t('i18n_98cd2bdc03')">
            <a-button :disabled="true" type="primary"> {{ $t('i18n_7f7c624a84') }}<DownOutlined /></a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <ul>
              <li>{{ $t('i18n_9b74c734e5') }}</li>
              <li>{{ $t('i18n_e1fefde80f') }}</li>
              <li>{{ $t('i18n_39b68185f0') }}</li>
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
            :title="`${$t('i18n_e703c7367c')}${statusMap[record.status]} ${
              record.statusMsg ? $t('i18n_8d13037eb7') + record.statusMsg : $t('i18n_77e100e462')
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
            <a-button type="primary" size="small" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            <a-button type="primary" size="small" @click="syncToWorkspaceShow(record)">{{
              $t('i18n_e39de3376e')
            }}</a-button>
            <a-button type="primary" danger size="small" @click="deleteMachineInfo(record)">{{
              $t('i18n_2f4aaddde3')
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
                    <div>{{ $t('i18n_5d83794cfa') }}{{ item.name }}</div>
                    <div>{{ $t('i18n_c1786d9e11') }}：{{ item.jpomUrl }}</div>
                  </template>
                  <span style="cursor: pointer" @click="showMachineInfo(item)">
                    {{ item.name }}
                  </span>
                </a-tooltip>
              </a-col>
              <a-col :span="7" style="text-align: right" class="text-overflow-hidden">
                <a-tooltip
                  :title="`${$t('i18n_e703c7367c')}${statusMap[item.status]} ${
                    item.statusMsg ? $t('i18n_8d13037eb7') + item.statusMsg : $t('i18n_77e100e462')
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
              <a-col :span="6" class="title text-overflow-hidden">{{ $t('i18n_2027743b8d') }}</a-col>
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
              <a-col :span="6" class="title text-overflow-hidden">{{ $t('i18n_3006a3da65') }}</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                {{ item.osVersion || '-' }}
              </a-col>
            </a-row>
          </a-tooltip>
          <a-tooltip :title="item.osLoadAverage">
            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">{{ $t('i18n_9e96d9c8d3') }}</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                {{ item.osLoadAverage || '-' }}
              </a-col>
            </a-row>
          </a-tooltip>
          <a-tooltip :title="item.jpomVersion">
            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">{{ $t('i18n_4a346aae15') }}</a-col>
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
                {{ $t('i18n_95b351c862') }}
              </a-button>
              <a-button type="primary" size="small" @click="showMachineInfo(item)">{{
                $t('i18n_f26225bde6')
              }}</a-button>
              <a-button type="primary" size="small" @click="syncToWorkspaceShow(item)">{{
                $t('i18n_e39de3376e')
              }}</a-button>
              <a-button type="primary" size="small" @click="viewMachineNode(item)">{{
                $t('i18n_3bf3c0a8d6')
              }}</a-button>
              <a-button size="small" @click="deleteMachineInfo(item)">{{ $t('i18n_2f4aaddde3') }}</a-button>
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
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50%"
      :title="$t('i18n_6eb39e706c')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editNodeForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
        <a-form-item :label="$t('i18n_e4013f8b81')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_e4013f8b81')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_93e1df604a')" name="groupName">
          <custom-select
            v-model:value="temp.groupName"
            :data="groupList"
            :input-placeholder="$t('i18n_bd0362bed3')"
            :select-placeholder="$t('i18n_9cac799f2f')"
          >
          </custom-select>
        </a-form-item>

        <a-form-item name="jpomUrl">
          <template #label>
            <a-tooltip>
              {{ $t('i18n_c1786d9e11') }}
              <template #title
                >{{ $t('i18n_899fe0c5dd') }}3
                <ul>
                  <li>{{ $t('i18n_9c3c05d91b') }}</li>
                  <li>{{ $t('i18n_1ece1616bf') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <template #help>{{ $t('i18n_6f8da7dcca') }}</template>
          <a-input v-model:value="temp.jpomUrl" :placeholder="$t('i18n_1235b052ff')">
            <template #addonBefore>
              <a-select v-model:value="temp.jpomProtocol" :placeholder="$t('i18n_e825ec7800')" style="width: 160px">
                <a-select-option value="Http"> Http:// </a-select-option>
                <a-select-option value="Https"> Https:// </a-select-option>
              </a-select>
            </template>
          </a-input>
        </a-form-item>

        <a-form-item :label="$t('i18n_86fb7b5421')" name="loginName">
          <a-input v-model:value="temp.jpomUsername" :placeholder="$t('i18n_f8460626f0')" />
          <template #help>{{ $t('i18n_eec342f34e') }}</template>
        </a-form-item>
        <a-form-item :name="`${temp.id ? 'loginPwd-update' : 'loginPwd'}`">
          <template #label>
            <a-tooltip>
              {{ $t('i18n_8bd3f73502') }}
              <template #title>
                {{ $t('i18n_1062619d5a') }}_authorize.json
                {{ $t('i18n_ff3bdecc5e') }}
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-input-password v-model:value="temp.jpomPassword" :placeholder="$t('i18n_e5a63852fd')" />
        </a-form-item>

        <a-collapse>
          <a-collapse-panel key="1" :header="$t('i18n_9ab433e930')">
            <a-form-item :label="$t('i18n_04edc35414')" name="templateNode" help="">
              <a-switch
                v-model:checked="temp.templateNode"
                :checked-children="$t('i18n_0a60ac8f02')"
                :un-checked-children="$t('i18n_c9744f45e7')"
                default-checked
              />
              {{ $t('i18n_8e34aa1a59') }},{{ $t('i18n_715ec3b393') }}
            </a-form-item>

            <a-form-item :label="$t('i18n_67425c29a5')" name="timeOut">
              <a-input-number
                v-model:value="temp.jpomTimeout"
                :min="0"
                :placeholder="$t('i18n_84d331a137')"
                style="width: 100%"
              />
            </a-form-item>

            <a-form-item :label="$t('i18n_fc954d25ec')" name="jpomHttpProxy">
              <a-input v-model:value="temp.jpomHttpProxy" :placeholder="$t('i18n_dcf14deb0e')">
                <template #addonBefore>
                  <a-select
                    v-model:value="temp.jpomHttpProxyType"
                    :placeholder="$t('i18n_b04070fe42')"
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

            <a-form-item :label="$t('i18n_7156088c6e')" name="transportEncryption">
              <a-select
                v-model:value="temp.transportEncryption"
                show-search
                default-value="0"
                :placeholder="$t('i18n_3c8eada338')"
              >
                <a-select-option :value="0">{{ $t('i18n_8a3e316cd7') }}</a-select-option>
                <a-select-option :value="1">BASE64</a-select-option>
                <a-select-option :value="2">AES</a-select-option>
              </a-select>
            </a-form-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form>
    </CustomModal>
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
    <CustomModal
      v-if="syncToWorkspaceVisible"
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_ef8525efce')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
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
          >
            <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </CustomModal>

    <!-- 查看机器关联节点 -->
    <CustomModal
      v-if="viewLinkNode"
      v-model:open="viewLinkNode"
      destroy-on-close
      width="50%"
      :title="$t('i18n_222316382d')"
      :footer="null"
      :mask-closable="false"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert v-if="nodeList && nodeList.length" :message="$t('i18n_566c67e764')" type="info" show-icon />
        <a-list bordered :data-source="nodeList">
          <template #renderItem="{ item }">
            <a-list-item style="display: block">
              <a-row>
                <a-col :span="10">{{ $t('i18n_5d83794cfa') }}{{ item.name }}</a-col>
                <a-col :span="10">{{ $t('i18n_2358e1ef49') }}{{ item.workspace && item.workspace.name }}</a-col>
                <a-col :span="4">
                  <a-button type="link" @click="toNode(item.id, item.name, item.workspace && item.workspace.id)">
                    <LoginOutlined /> </a-button
                ></a-col>
              </a-row>
            </a-list-item>
          </template>
        </a-list>
      </a-space>
    </CustomModal>
    <!-- 分发节点授权 -->
    <CustomModal
      v-if="whiteConfigVisible"
      v-model:open="whiteConfigVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50%"
      :title="$t('i18n_e19cc5ed70')"
      :mask-closable="false"
      @ok="onSubmitWhitelist"
    >
      <a-alert
        :message="`${$t('i18n_6fa1229ea9')},${$t('i18n_acf14aad3c')},${$t('i18n_332ba869d9')}`"
        style="margin-top: 10px; margin-bottom: 20px"
        banner
      />
      <a-form ref="editWhiteForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item :label="$t('i18n_04edc35414')">
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
            :placeholder="$t('i18n_8d92fb62a7')"
            @change="(id) => loadWhitelistData(id)"
          >
            <a-select-option v-for="item in templateNodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('i18n_aabdc3b7c0')" name="project">
          <a-textarea
            v-model:value="temp.project"
            :rows="5"
            style="resize: none"
            :placeholder="$t('i18n_631d5b88ab')"
          />
        </a-form-item>

        <a-form-item :label="$t('i18n_649231bdee')" name="allowEditSuffix">
          <a-textarea
            v-model:value="temp.allowEditSuffix"
            :rows="5"
            style="resize: none"
            :placeholder="$t('i18n_afa8980495')"
          />
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 分发机器配置 -->
    <CustomModal
      v-if="nodeConfigVisible"
      v-model:open="nodeConfigVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50%"
      :title="$t('i18n_6f8907351b')"
      :mask-closable="false"
    >
      <template #footer>
        <a-space>
          <a-button type="primary" :disabled="!temp.content" @click="onNodeSubmit(false)">{{
            $t('i18n_be5fbbe34c')
          }}</a-button>
          <a-button type="primary" :disabled="!temp.content" @click="onNodeSubmit(true)">{{
            $t('i18n_6aab88d6a3')
          }}</a-button>
        </a-space>
      </template>
      <a-alert
        :message="`${$t('i18n_10c385b47e')},${$t('i18n_acf14aad3c')},${$t('i18n_332ba869d9')}`"
        style="margin-top: 10px; margin-bottom: 20px"
        banner
      />
      <a-form ref="editNodeConfigForm" :model="temp">
        <a-form-item :label="$t('i18n_798f660048')">
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
            :placeholder="$t('i18n_353c7f29da')"
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
        name: [{ required: true, message: this.$t('i18n_cbdc4f58f6'), trigger: 'blur' }]
      },
      drawerVisible: false,
      drawerUpgradeVisible: false,
      workspaceList: [],
      viewLinkNode: false,
      nodeList: [],
      layoutType: null,
      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          width: 150,
          ellipsis: true
        },
        {
          title: this.$t('i18n_cdc478d90c'),
          dataIndex: 'osName',
          width: 150,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_6707667676'),
          dataIndex: 'hostName',
          width: 150,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_c1786d9e11'),
          dataIndex: 'jpomUrl',
          width: 150,
          sorter: true,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_b37b786351'),
          dataIndex: 'groupName',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: 'status',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_64eee9aafa'),
          sorter: true,
          dataIndex: 'osSystemUptime',
          width: 150,
          ellipsis: true,
          duration2: true
        },
        {
          title: `CPU${this.$t('i18n_2f97ed65db')}`,
          sorter: true,
          align: 'center',
          dataIndex: 'osOccupyCpu',
          width: '100px',
          ellipsis: true,
          percent2Number: true
        },
        {
          title: this.$t('i18n_883848dd37'),
          sorter: true,
          align: 'center',
          dataIndex: 'osOccupyMemory',
          width: '100px',
          ellipsis: true,
          percent2Number: true
        },
        {
          title: this.$t('i18n_ed145eba38'),
          sorter: true,
          align: 'center',
          dataIndex: 'osOccupyDisk',
          width: '100px',
          ellipsis: true,
          percent2Number: true
        },
        {
          title: this.$t('i18n_2482a598a3'),
          dataIndex: 'jpomVersion',
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_04edc35414'),
          dataIndex: 'templateNode',
          width: '90px',
          align: 'center',
          ellipsis: true,
          customRender: ({ text }) => {
            return text ? this.$t('i18n_0a60ac8f02') : this.$t('i18n_c9744f45e7')
          }
        },
        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          customRender: ({ text }) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_9c66f7b345'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
          message: this.$t('i18n_b3bda9bf9e')
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
          message: this.$t('i18n_d82b19148f')
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
              message: this.$t('i18n_d7ef19d05b')
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
          message: this.$t('i18n_1e07b9f9ce')
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
              message: this.$t('i18n_d7ef19d05b')
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
        title: this.$t('i18n_c4535759ee'),
        content: restart ? this.$t('i18n_0cf4f0ba82') : this.$t('i18n_863a95c914'),
        okText: this.$t('i18n_e83a256e4f'),
        zIndex: 1009,
        cancelText: this.$t('i18n_625fb26b4b'),
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
