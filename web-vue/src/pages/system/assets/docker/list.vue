<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      table-name="assets-docker-list"
      :empty-description="$t('pages.system.assets.docker.list.7e22d7ff')"
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
            :placeholder="$t('pages.system.assets.docker.list.3e34ec28')"
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
            :placeholder="$t('pages.system.assets.docker.list.29c6f407')"
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
            :placeholder="$t('pages.system.assets.docker.list.b1765e98')"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('pages.system.assets.docker.list.33064c22')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.system.assets.docker.list.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('pages.system.assets.docker.list.7d46652a') }}</a-button>
          <a-button :disabled="!tableSelections.length" type="primary" @click="syncToWorkspaceShow()">
            {{ $t('pages.system.assets.docker.list.fd234860') }}</a-button
          >
          <a-tooltip :title="$t('pages.system.assets.docker.list.408e85c9')">
            <a-button type="dashed" @click="handleTryLocalDocker">
              <QuestionCircleOutlined />{{ $t('pages.system.assets.docker.list.5aa124eb') }}
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
            <a-tooltip v-if="record.swarmControlAvailable" :title="$t('pages.system.assets.docker.list.37786c01')">
              <ClusterOutlined />
            </a-tooltip>
            <a-tooltip v-else :title="$t('pages.system.assets.docker.list.e4035840')">
              <BlockOutlined />
            </a-tooltip>
            <a-popover :title="$t('pages.system.assets.docker.list.5775aec5')">
              <template #content>
                <p>{{ $t('pages.system.assets.docker.list.a5265715') }}{{ record.swarmId }}</p>
                <p>{{ $t('pages.system.assets.docker.list.f8ec3bb8') }}{{ record.swarmNodeId }}</p>
                <p>{{ $t('pages.system.assets.docker.list.a04b7d9c') }}{{ record.swarmNodeAddr }}</p>
                <p>{{ $t('pages.system.assets.docker.list.a6076557') }}{{ parseTime(record.swarmCreatedAt) }}</p>
                <p>{{ $t('pages.system.assets.docker.list.6eebbce0') }}{{ parseTime(record.swarmUpdatedAt) }}</p>
              </template>
              {{ text }}
            </a-popover>
          </template>
        </template>

        <template v-else-if="column.dataIndex === 'tlsVerify'">
          <a-tooltip
            placement="topLeft"
            :title="
              record.tlsVerify
                ? $t('pages.system.assets.docker.list.51e7a8c3') + record.certInfo
                : $t('pages.system.assets.docker.list.f1cb89d8')
            "
          >
            <template v-if="record.tlsVerify">
              <template v-if="record.certExist">
                <a-switch
                  v-model:checked="record.tlsVerify"
                  size="small"
                  :disabled="true"
                  :checked-children="$t('pages.system.assets.docker.list.b1d6efbd')"
                  :un-checked-children="$t('pages.system.assets.docker.list.b6bb836')"
                />
              </template>
              <a-tag v-else color="red"> {{ $t('pages.system.assets.docker.list.53916e88') }} </a-tag>
            </template>
            <template v-else>
              <a-switch
                v-model:checked="record.tlsVerify"
                size="small"
                :disabled="true"
                :checked-children="$t('pages.system.assets.docker.list.b1d6efbd')"
                :un-checked-children="$t('pages.system.assets.docker.list.b6bb836')"
              />
            </template>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="record.failureMsg">
            <a-tag :color="statusMap[record.status].color">{{
              statusMap[record.status].desc || $t('pages.system.assets.docker.list.ca1cdfa6')
            }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" :disabled="record.status !== 1" @click="handleConsole(record)">{{
              $t('pages.system.assets.docker.list.5139b7d7')
            }}</a-button>
            <template v-if="!record.swarmId && record.status === 1">
              <a-popover :title="$t('pages.system.assets.docker.list.364e6c4d')">
                <template #content>
                  <p>
                    <a-button size="small" type="primary" @click="initSwarm(record)">{{
                      $t('pages.system.assets.docker.list.31cebbac')
                    }}</a-button>
                  </p>
                  <p>
                    <a-button size="small" type="primary" @click="joinSwarm(record)">{{
                      $t('pages.system.assets.docker.list.54a2a9a1')
                    }}</a-button>
                  </p>
                </template>
                <a-button size="small" type="primary"
                  ><EditOutlined />{{ $t('pages.system.assets.docker.list.17b26f41') }}</a-button
                >
              </a-popover>
            </template>
            <template v-else>
              <a-button
                size="small"
                :disabled="parseInt(record.status) !== 1"
                type="primary"
                @click="handleSwarmConsole(record)"
                ><SelectOutlined />{{ $t('pages.system.assets.docker.list.17b26f41') }}</a-button
              >
            </template>
            <a-button size="small" type="primary" @click="syncToWorkspaceShow(record)">{{
              $t('pages.system.assets.docker.list.9510043a')
            }}</a-button>
            <a-button size="small" type="primary" @click="viewWorkspaceDataHander(record)">{{
              $t('pages.system.assets.docker.list.2ef699f7')
            }}</a-button>
            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $t('pages.system.assets.docker.list.6e071067') }} <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="handleEdit(record)">{{
                      $t('pages.system.assets.docker.list.64603c01')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                      $t('pages.system.assets.docker.list.dd20d11c')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      :disabled="!record.swarmId || record.status !== 1"
                      type="primary"
                      danger
                      @click="handleLeaveForce(record)"
                      >{{ $t('pages.system.assets.docker.list.ef53ea15') }}</a-button
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
      :title="$t('pages.system.assets.docker.list.2e8ce846')"
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
                  <li>SSH {{ $t('pages.system.assets.docker.list.c6c093e5') }}</li>
                  <li>
                    docker {{ $t('pages.system.assets.docker.list.8119bea8') }}.09
                    {{ $t('pages.system.assets.docker.list.e20fe67d') }}
                  </li>
                  <li>{{ $t('pages.system.assets.docker.list.36361237') }}</li>
                  <li>
                    {{ $t('pages.system.assets.docker.list.a1434b2') }}>{{
                      $t('pages.system.assets.docker.list.6b0e27ae')
                    }}
                    <b>ssh/monitor-script.sh</b>
                    {{ $t('pages.system.assets.docker.list.4a0c80d4') }}
                  </li>
                </ul>
              </template>
              <template v-else>
                <ul>
                  <li>
                    {{ $t('pages.system.assets.docker.list.b831d501')
                    }}<b style="color: red">{{ $t('pages.system.assets.docker.list.2ceaa4d9') }}</b>
                  </li>
                  <li>
                    {{ $t('pages.system.assets.docker.list.ca19c509') }}
                    <b style="color: red">docker {{ $t('pages.system.assets.docker.list.292e1bdc') }}</b>
                  </li>
                  <li>
                    {{ $t('pages.system.assets.docker.list.95c6cf86')
                    }}<b style="color: red"> {{ $t('pages.system.assets.docker.list.c82fb3de') }} </b>
                  </li>
                  <li>
                    {{ $t('pages.system.assets.docker.list.32893d5c')
                    }}<b style="color: red">{{ $t('pages.system.assets.docker.list.6f521cc7') }}</b
                    >（{{ $t('pages.system.assets.docker.list.f3fb3cdb') }}
                  </li>
                  <li>
                    {{ $t('pages.system.assets.docker.list.f12e0a8b')
                    }}<b style="color: red">{{ $t('pages.system.assets.docker.list.d0ebc18') }}</b>
                  </li>
                  <li>
                    {{ $t('pages.system.assets.docker.list.2ae8180f')
                    }}<b style="color: red">{{ $t('pages.system.assets.docker.list.f44ea303') }}</b>
                  </li>
                </ul>
              </template>
            </template>
          </a-alert>
          <div></div>
        </a-space>
        <a-form-item :label="$t('pages.system.assets.docker.list.cddbe6bd')" name="name">
          <a-input v-model:value="temp.name" :placeholder="$t('pages.system.assets.docker.list.cddbe6bd')" />
        </a-form-item>
        <a-form-item :label="$t('pages.system.assets.docker.list.b1765e98')" name="groupName">
          <custom-select
            v-model:value="temp.groupName"
            :data="groupList"
            :input-placeholder="$t('pages.system.assets.docker.list.95c41d82')"
            :select-placeholder="$t('pages.system.assets.docker.list.c385f859')"
          >
          </custom-select>
        </a-form-item>
        <a-form-item :label="$t('pages.system.assets.docker.list.81809417')" name="enableSsh">
          <a-switch
            v-model:checked="temp.enableSsh"
            :checked-children="$t('pages.system.assets.docker.list.b1d6efbd')"
            :un-checked-children="$t('pages.system.assets.docker.list.b6bb836')"
          />
        </a-form-item>
        <a-form-item v-if="temp.enableSsh" :label="$t('pages.system.assets.docker.list.3c5f6a8d')" name="enableSsh">
          <a-select
            v-model:value="temp.machineSshId"
            allow-clear
            :placeholder="$t('pages.system.assets.docker.list.3c5f6a8d')"
            class="search-input-item"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :disabled="!item.dockerInfo" :value="item.id">
              <a-tooltip :title="`${item.name}(${item.host})`">
                <template #title>
                  {{ item.name }}({{ item.host }})[{{
                    (item.dockerInfo && JSON.parse(item.dockerInfo) && JSON.parse(item.dockerInfo).version) ||
                    $t('pages.system.assets.docker.list.68116479')
                  }}]
                </template>
                {{ item.name }}({{ item.host }}) [{{
                  (item.dockerInfo && JSON.parse(item.dockerInfo) && JSON.parse(item.dockerInfo).version) ||
                  $t('pages.system.assets.docker.list.68116479')
                }}]</a-tooltip
              >
            </a-select-option>
          </a-select>
          <template #help>{{ $t('pages.system.assets.docker.list.f624d3a5') }}</template>
        </a-form-item>
        <template v-if="!temp.enableSsh">
          <a-form-item label="host" name="host">
            <a-input
              v-model:value="temp.host"
              :placeholder="`${$t('pages.system.assets.docker.list.aa3b5f80')}://127.0.0.1:2375`"
            />
          </a-form-item>

          <a-form-item :label="$t('pages.system.assets.docker.list.9a380d24')" name="tlsVerify">
            <a-switch
              v-model:checked="temp.tlsVerify"
              :checked-children="$t('pages.system.assets.docker.list.b1d6efbd')"
              :un-checked-children="$t('pages.system.assets.docker.list.b6bb836')"
            />
          </a-form-item>
          <a-form-item
            v-if="temp.tlsVerify"
            :label="$t('pages.system.assets.docker.list.c3a4f261')"
            name="certInfo"
            :help="$t('pages.system.assets.docker.list.de472e9b')"
          >
            <a-input-search
              v-model:value="temp.certInfo"
              :placeholder="$t('pages.system.assets.docker.list.b8cb1f76')"
              :enter-button="$t('pages.system.assets.docker.list.c5dce2af')"
              @search="
                () => {
                  certificateVisible = true
                }
              "
            />
          </a-form-item>
        </template>

        <a-collapse>
          <a-collapse-panel key="1" :header="$t('pages.system.assets.docker.list.5de4135b')">
            <a-form-item :label="$t('pages.system.assets.docker.list.e67b0df3')" name="heartbeatTimeout">
              <a-input-number
                v-model:value="temp.heartbeatTimeout"
                style="width: 100%"
                :placeholder="$t('pages.system.assets.docker.list.36526abf')"
              />
            </a-form-item>
            <a-form-item :label="$t('pages.system.assets.docker.list.7a3a2453')" name="registryUrl">
              <a-input v-model:value="temp.registryUrl" :placeholder="$t('pages.system.assets.docker.list.7a3a2453')" />
            </a-form-item>
            <a-form-item :label="$t('pages.system.assets.docker.list.a421d76')" name="registryUsername">
              <a-input
                v-model:value="temp.registryUsername"
                :placeholder="$t('pages.system.assets.docker.list.a421d76')"
              />
            </a-form-item>
            <a-form-item :label="$t('pages.system.assets.docker.list.5d688166')" name="registryPassword">
              <a-input-password
                v-model:value="temp.registryPassword"
                :placeholder="$t('pages.system.assets.docker.list.5d688166')"
              />
            </a-form-item>
            <a-form-item :label="$t('pages.system.assets.docker.list.e4aff6de')" name="registryEmail">
              <a-input
                v-model:value="temp.registryEmail"
                :placeholder="$t('pages.system.assets.docker.list.e4aff6de')"
              />
            </a-form-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form>
    </a-modal>
    <!-- 创建集群 -->
    <a-modal
      v-model:open="initSwarmVisible"
      destroy-on-close
      :title="$t('pages.system.assets.docker.list.72c164e9')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleSwarm"
    >
      <a-form ref="initForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-alert :message="$t('pages.system.assets.docker.list.7ae524f6')" type="warning">
          <template #description> {{ $t('pages.system.assets.docker.list.aaeec58d') }} </template>
        </a-alert>
      </a-form>
    </a-modal>
    <!-- 加入集群 -->
    <a-modal
      v-model:open="joinSwarmVisible"
      destroy-on-close
      :title="$t('pages.system.assets.docker.list.eb93f2a5')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      @ok="handleSwarmJoin"
    >
      <a-form ref="joinForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('pages.system.assets.docker.list.f75f99f7')" name="managerId">
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
            :placeholder="$t('pages.system.assets.docker.list.cccc0a77')"
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

        <a-form-item v-if="temp.remoteAddr" :label="$t('pages.system.assets.docker.list.87200b2')" name="remoteAddr"
          ><a-input v-model:value="temp.remoteAddr" :placeholder="$t('pages.system.assets.docker.list.9d684818')" />
        </a-form-item>

        <a-form-item :label="$t('pages.system.assets.docker.list.b23c6871')" name="role">
          <a-radio-group v-model:value="temp.role" name="role">
            <a-radio value="worker"> {{ $t('pages.system.assets.docker.list.e4035840') }}</a-radio>
            <a-radio value="manager"> {{ $t('pages.system.assets.docker.list.37786c01') }} </a-radio>
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
      :title="$t('pages.system.assets.docker.list.7f2f69fe')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$t('pages.system.assets.docker.list.3e34c732')" name="type">
          <a-radio-group v-model:value="temp.type">
            <a-radio value="docker"> docker </a-radio>
            <a-radio value="swarm" :disabled="temp.swarmId === true ? false : true">
              {{ $t('pages.system.assets.docker.list.17b26f41') }}
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('pages.system.assets.docker.list.7ef9d8fb')" name="workspaceId">
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
            :placeholder="$t('pages.system.assets.docker.list.3a321a02')"
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
      :title="$t('pages.system.assets.docker.list.8e816e93')"
      :footer="null"
      :mask-closable="false"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert
          v-if="
            workspaceDockerData && (workspaceDockerData.dockerList?.length || workspaceDockerData.swarmList?.length)
          "
          :message="$t('pages.system.assets.docker.list.5c18eb94')"
          type="info"
          show-icon
        />
        <a-tabs>
          <a-tab-pane key="1" tab="docker">
            <a-list bordered :data-source="workspaceDockerData && workspaceDockerData.dockerList">
              <template #renderItem="{ item }">
                <a-list-item style="display: block">
                  <a-row>
                    <a-col :span="10">Docker {{ $t('pages.system.assets.docker.list.bb769c1d') }}{{ item.name }}</a-col>
                    <a-col :span="10"
                      >{{ $t('pages.system.assets.docker.list.2a91c128')
                      }}{{ item.workspace && item.workspace.name }}</a-col
                    >
                    <a-col :span="4"> </a-col>
                  </a-row>
                </a-list-item>
              </template>
            </a-list>
          </a-tab-pane>
          <a-tab-pane key="2" :tab="$t('pages.system.assets.docker.list.17b26f41')">
            <a-list bordered :data-source="workspaceDockerData && workspaceDockerData.swarmList">
              <template #renderItem="{ item }">
                <a-list-item style="display: block">
                  <a-row>
                    <a-col :span="10">{{ $t('pages.system.assets.docker.list.c2dfe194') }}{{ item.name }}</a-col>
                    <a-col :span="10"
                      >{{ $t('pages.system.assets.docker.list.2a91c128')
                      }}{{ item.workspace && item.workspace.name }}</a-col
                    >
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
      :title="`${$t('pages.system.assets.docker.list.5540289f')}`"
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
            {{ $t('pages.system.assets.docker.list.43105e21') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['certificate'].handerConfirm()
              }
            "
          >
            {{ $t('pages.system.assets.docker.list.7da4a591') }}
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
          title: this.$t('pages.system.assets.docker.list.3e34ec28'),
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
          title: `docker${this.$t('pages.system.assets.docker.list.d826aba2')}`,
          dataIndex: 'dockerVersion',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },

        {
          title: this.$t('pages.system.assets.docker.list.9c32c887'),
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
          title: this.$t('pages.system.assets.docker.list.12d0e469'),
          dataIndex: 'groupName',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: this.$t('pages.system.assets.docker.list.17b26f41'),
          dataIndex: 'swarmId',
          ellipsis: true
        },
        // { title: "apiVersion", dataIndex: "apiVersion", width: 100, ellipsis: true, },
        {
          title: this.$t('pages.system.assets.docker.list.49942d36'),
          dataIndex: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('pages.system.assets.docker.list.f5b90169'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.system.assets.docker.list.3d55d8de'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.system.assets.docker.list.3bb962bf'),
          dataIndex: 'operation',

          fixed: 'right',
          align: 'center',
          width: '320px'
        }
      ],
      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: this.$t('pages.system.assets.docker.list.98c4138b'), trigger: 'blur' }],
        // host: [{ required: true, message: "请填写容器地址", trigger: "blur" }],

        managerId: [
          {
            required: true,
            message: this.$t('pages.system.assets.docker.list.b0c35cac'),
            trigger: 'blur'
          }
        ],
        role: [{ required: true, message: this.$t('pages.system.assets.docker.list.f3c77563'), trigger: 'blur' }],
        remoteAddr: [
          { required: true, message: this.$t('pages.system.assets.docker.list.331c207a'), trigger: 'blur' },
          {
            pattern:
              /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/,
            message: this.$t('pages.system.assets.docker.list.cf0e57ee')
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
            $message.warning(this.$t('pages.system.assets.docker.list.8c4bd72e'))
            return false
          }
        } else {
          if (!temp.host) {
            $message.warning(this.$t('pages.system.assets.docker.list.b8b92e78'))
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
        title: this.$t('pages.system.assets.docker.list.d3367221'),
        zIndex: 1009,
        content: this.$t('pages.system.assets.docker.list.9b222154'),
        okText: this.$t('pages.system.assets.docker.list.7da4a591'),
        cancelText: this.$t('pages.system.assets.docker.list.43105e21'),
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
      <h1 style='color:red;'>${this.$t('pages.system.assets.docker.list.848a78c4')}</h1>
      <h3 style='color:red;'>${this.$t('pages.system.assets.docker.list.732c87bc')}</h3>
      <ul style='color:red;'>
        <li>${this.$t('pages.system.assets.docker.list.3e4f81f9')}</li>
        <li>${this.$t('pages.system.assets.docker.list.3e4f81f9')}</li>
        <li style='font-weight: bold;'>${this.$t('pages.system.assets.docker.list.5d61d6da')}</li>
        <li>${this.$t('pages.system.assets.docker.list.f698fdd9')}</li>
      </ul>
      `
      $confirm({
        title: this.$t('pages.system.assets.docker.list.d3367221'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: this.$t('pages.system.assets.docker.list.7da4a591'),
        cancelText: this.$t('pages.system.assets.docker.list.43105e21'),
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
          message: this.$t('pages.system.assets.docker.list.a8707f4a')
        })
        return false
      }
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: this.$t('pages.system.assets.docker.list.3a321a02')
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
