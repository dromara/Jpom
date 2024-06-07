<template>
  <div>
    <a-tabs default-active-key="1">
      <a-tab-pane key="1" :tab="$t('pages.system.assets.ssh.ssh-list.c34007d1')">
        <!-- 数据表格 -->
        <CustomTable
          is-show-tools
          default-auto-refresh
          :auto-refresh-time="5"
          table-name="assets-ssh-list"
          :empty-description="$t('pages.system.assets.ssh.ssh-list.ee8e7216')"
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
                :placeholder="$t('pages.system.assets.ssh.ssh-list.dc77206e')"
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
                :placeholder="$t('pages.system.assets.ssh.ssh-list.e740d8cb')"
                class="search-input-item"
              >
                <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
              </a-select>

              <a-tooltip :title="$t('pages.system.assets.ssh.ssh-list.1bfed54a')">
                <a-button type="primary" :loading="loading" @click="loadData"
                  >{{ $t('pages.system.assets.ssh.ssh-list.53c2763c') }}
                </a-button>
              </a-tooltip>

              <a-button type="primary" @click="handleAdd">{{
                $t('pages.system.assets.ssh.ssh-list.7d46652a')
              }}</a-button>
              <a-button :disabled="!tableSelections.length" type="primary" @click="syncToWorkspaceShow()">
                {{ $t('pages.system.assets.ssh.ssh-list.fd234860') }}</a-button
              >
              <a-button type="primary" @click="handlerExportData()"
                ><DownloadOutlined />{{ $t('pages.system.assets.ssh.ssh-list.a5bebb0f') }}</a-button
              >
              <a-dropdown>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="1">
                      <a-button type="primary" @click="handlerImportTemplate()">{{
                        $t('pages.system.assets.ssh.ssh-list.9b6f4751')
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
                  <a-button type="primary"
                    ><UploadOutlined /> {{ $t('pages.system.assets.ssh.ssh-list.7a7c6386') }}<DownOutlined />
                  </a-button>
                </a-upload>
              </a-dropdown>
            </a-space>
          </template>
          <template #tableHelp>
            <a-tooltip>
              <template #title>
                <div>
                  <ul>
                    <li>{{ $t('pages.system.assets.ssh.ssh-list.87c09576') }}</li>
                    <li>{{ $t('pages.system.assets.ssh.ssh-list.9a2579da') }}</li>
                    <li>{{ $t('pages.system.assets.ssh.ssh-list.5c343a02') }}</li>
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
              <a-popover :title="$t('pages.system.assets.ssh.ssh-list.8397a674')">
                <template #content>
                  <p>{{ $t('pages.system.assets.ssh.ssh-list.163de925') }}{{ record.osName }}</p>
                  <p>{{ $t('pages.system.assets.ssh.ssh-list.41180a8c') }}{{ record.osVersion }}</p>
                  <p>CPU{{ $t('pages.system.assets.ssh.ssh-list.5d15eed7') }}{{ record.osCpuIdentifierName }}</p>
                  <p>{{ $t('pages.system.assets.ssh.ssh-list.df83aba7') }}{{ record.hostName }}</p>
                  <p>
                    {{ $t('pages.system.assets.ssh.ssh-list.abd23cf2') }}{{ formatDuration(record.osSystemUptime) }}
                  </p>
                </template>
                {{ text || $t('pages.system.assets.ssh.ssh-list.5f51a112') }}
              </a-popover>
            </template>
            <template v-else-if="column.dataIndex === 'nodeId'">
              <template v-if="record.status !== 2">
                <!-- 禁用监控不显示 -->
                <div v-if="record.javaVersion">
                  <a-popover v-if="record.jpomAgentPid > 0" :title="$t('pages.system.assets.ssh.ssh-list.ccbea9')">
                    <template #content>
                      <p>{{ $t('pages.system.assets.ssh.ssh-list.5f0646f6') }}{{ record.jpomAgentPid }}</p>
                      <p>java{{ $t('pages.system.assets.ssh.ssh-list.4d6bd516') }}{{ record.javaVersion }}</p>
                    </template>
                    <a-tag color="green"> {{ record.jpomAgentPid }}</a-tag>
                  </a-popover>
                  <a-button v-else size="small" type="primary" @click="install(record)">{{
                    $t('pages.system.assets.ssh.ssh-list.4b3f44bf')
                  }}</a-button>
                </div>

                <a-tag v-else color="orange">no java</a-tag>
              </template>
              <template v-else>-</template>
            </template>
            <template v-else-if="column.dataIndex === 'dockerInfo'">
              <template v-if="record.status !== 2">
                <!-- 禁用监控不显示 -->
                <a-popover v-if="record.dockerInfo" :title="$t('pages.system.assets.ssh.ssh-list.96a2da41')">
                  <template #content>
                    <p>{{ $t('pages.system.assets.ssh.ssh-list.ee016914') }}{{ JSON.parse(record.dockerInfo).path }}</p>
                    <p>
                      {{ $t('pages.system.assets.ssh.ssh-list.4d6bd516') }}{{ JSON.parse(record.dockerInfo).version }}
                    </p>
                  </template>
                  <a-tag color="green">{{ $t('pages.system.assets.ssh.ssh-list.55adcb2b') }}</a-tag>
                </a-popover>

                <a-tag v-else>{{ $t('pages.system.assets.ssh.ssh-list.458d0e40') }}</a-tag>
              </template>
              <template v-else>-</template>
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <a-tooltip :title="`${record.statusMsg || $t('pages.system.assets.ssh.ssh-list.5f479aec')}`">
                <a-tag :color="statusMap[record.status] && statusMap[record.status].color">{{
                  (statusMap[record.status] && statusMap[record.status].desc) ||
                  $t('pages.system.assets.ssh.ssh-list.5f51a112')
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
                :title="`${$t('pages.system.assets.ssh.ssh-list.cc3be96d')}${formatPercent(record.osOccupyMemory)},${$t(
                  'pages.system.assets.ssh.ssh-list.8b8cc8a1'
                )}${renderSize(record.osMoneyTotal)}`"
              >
                <span>{{ formatPercent(record.osOccupyMemory) }}/{{ renderSize(record.osMoneyTotal) }}</span>
              </a-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'osMaxOccupyDisk'">
              <a-popover :title="$t('pages.system.assets.ssh.ssh-list.3cd46bee')">
                <template #content>
                  <p>{{ $t('pages.system.assets.ssh.ssh-list.7065b72c') }}{{ renderSize(record.osFileStoreTotal) }}</p>
                  <p>
                    {{ $t('pages.system.assets.ssh.ssh-list.94143668') }}{{ formatPercent(record.osMaxOccupyDisk) }}
                  </p>
                  <p>{{ $t('pages.system.assets.ssh.ssh-list.7a0b637a') }}{{ record.osMaxOccupyDiskName }}</p>
                </template>
                <span>{{ formatPercent(record.osMaxOccupyDisk) }} / {{ renderSize(record.osFileStoreTotal) }}</span>
              </a-popover>
            </template>

            <template v-else-if="column.dataIndex === 'osOccupyCpu'">
              <a-tooltip
                placement="topLeft"
                :title="`CPU${$t('pages.system.assets.ssh.ssh-list.5a6bc27e')}${formatPercent2Number(
                  record.osOccupyCpu
                )}%,CPU${$t('pages.system.assets.ssh.ssh-list.f59d86c')}${record.osCpuCores}`"
              >
                <span>{{ (formatPercent2Number(record.osOccupyCpu) || '-') + '%' }} / {{ record.osCpuCores }}</span>
              </a-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'operation'">
              <a-space>
                <a-dropdown>
                  <a-button size="small" type="primary" @click="handleTerminal(record, false)"
                    >{{ $t('pages.system.assets.ssh.ssh-list.b5a97ef7') }}<DownOutlined
                  /></a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item key="1">
                        <a-button size="small" type="primary" @click="handleTerminal(record, true)">
                          <FullscreenOutlined />{{ $t('pages.system.assets.ssh.ssh-list.230a04ee') }}
                        </a-button>
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
                <a-button size="small" type="primary" @click="syncToWorkspaceShow(record)">{{
                  $t('pages.system.assets.ssh.ssh-list.9510043a')
                }}</a-button>
                <a-button size="small" type="primary" @click="handleFile(record)">{{
                  $t('pages.system.assets.ssh.ssh-list.69cad40b')
                }}</a-button>
                <a-button size="small" type="primary" @click="handleViewWorkspaceSsh(record)">{{
                  $t('pages.system.assets.ssh.ssh-list.2ef699f7')
                }}</a-button>

                <a-dropdown>
                  <a @click="(e) => e.preventDefault()">
                    {{ $t('pages.system.assets.ssh.ssh-list.6e071067') }}
                    <DownOutlined />
                  </a>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item>
                        <a-button size="small" type="primary" @click="handleEdit(record)">{{
                          $t('pages.system.assets.ssh.ssh-list.64603c01')
                        }}</a-button>
                      </a-menu-item>
                      <a-menu-item>
                        <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                          $t('pages.system.assets.ssh.ssh-list.2f14e7d4')
                        }}</a-button>
                      </a-menu-item>
                      <a-menu-item>
                        <a-button size="small" type="primary" @click="handleViewLog(record)">{{
                          $t('pages.system.assets.ssh.ssh-list.194f04f1')
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
          :title="$t('pages.system.assets.ssh.ssh-list.e943f850')"
          :mask-closable="false"
          @ok="handleEditSshOk"
        >
          <a-form ref="editSshForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
            <a-form-item :label="$t('pages.system.assets.ssh.ssh-list.493a5eda')" name="name">
              <a-input
                v-model:value="temp.name"
                :max-length="50"
                :placeholder="$t('pages.system.assets.ssh.ssh-list.493a5eda')"
              />
            </a-form-item>
            <a-form-item :label="$t('pages.system.assets.ssh.ssh-list.12d0e469')" name="group">
              <custom-select
                v-model:value="temp.groupName"
                :data="groupList"
                :input-placeholder="$t('pages.system.assets.ssh.ssh-list.95c41d82')"
                :select-placeholder="$t('pages.system.assets.ssh.ssh-list.c385f859')"
              >
              </custom-select>
            </a-form-item>
            <a-form-item label="Host" name="host">
              <a-input-group compact name="host">
                <a-input
                  v-model:value="temp.host"
                  style="width: 70%"
                  :placeholder="$t('pages.system.assets.ssh.ssh-list.2a72f1e6')"
                />
                <a-form-item-rest>
                  <a-input-number
                    v-model:value="temp.port"
                    style="width: 30%"
                    :min="1"
                    :placeholder="$t('pages.system.assets.ssh.ssh-list.a6c4bfd7')"
                  />
                </a-form-item-rest>
              </a-input-group>
            </a-form-item>
            <a-form-item :label="$t('pages.system.assets.ssh.ssh-list.c9b8bf26')" name="connectType">
              <a-radio-group v-model:value="temp.connectType" :options="options" />
            </a-form-item>
            <a-form-item name="user">
              <template #label>
                <a-tooltip>
                  {{ $t('pages.system.assets.ssh.ssh-list.e15572a') }}
                  <template #title>
                    {{ $t('pages.system.assets.ssh.ssh-list.ddb0e0ec') }}<b>$ref.wEnv.xxxx</b> xxxx
                    {{ $t('pages.system.assets.ssh.ssh-list.c1175f17') }}</template
                  >
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input v-model:value="temp.user" :placeholder="$t('pages.system.assets.ssh.ssh-list.68c63452')">
                <template #suffix>
                  <a-tooltip v-if="temp.id" :title="$t('pages.system.assets.ssh.ssh-list.acf4d7b4')">
                    <a-button size="small" type="primary" danger @click="handerRestHideField(temp)">{{
                      $t('pages.system.assets.ssh.ssh-list.6f316d08')
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
                  {{ $t('pages.system.assets.ssh.ssh-list.c3891788') }}
                  <template #title>
                    {{ $t('pages.system.assets.ssh.ssh-list.7cb49f83') }}<b>$ref.wEnv.xxxx</b> xxxx
                    {{ $t('pages.system.assets.ssh.ssh-list.c1175f17') }}</template
                  >
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <!-- <a-input-password v-model="temp.password" :placeholder="`${temp.type === 'add' ? '密码' : '密码若没修改可以不用填写'}`" /> -->
              <custom-input
                :input="temp.password"
                :env-list="envVarList"
                :placeholder="`${
                  temp.type === 'add'
                    ? $t('pages.system.assets.ssh.ssh-list.c3891788')
                    : $t('pages.system.assets.ssh.ssh-list.d6753d4f')
                }`"
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
                  {{ $t('pages.system.assets.ssh.ssh-list.4631d7f0') }}
                  <template #title>{{ $t('pages.system.assets.ssh.ssh-list.a341a6f7') }} </template>
                  <QuestionCircleOutlined v-if="temp.type !== 'edit'" />
                </a-tooltip>
              </template>

              <a-textarea
                v-model:value="temp.privateKey"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                :placeholder="$t('pages.system.assets.ssh.ssh-list.a72e7dc6')"
              />
            </a-form-item>
            <a-form-item :label="$t('pages.system.assets.ssh.ssh-list.9e03a812')" name="charset">
              <a-input v-model:value="temp.charset" :placeholder="$t('pages.system.assets.ssh.ssh-list.9e03a812')" />
            </a-form-item>
            <a-form-item :label="$t('pages.system.assets.ssh.ssh-list.e67b0df3')" name="timeout">
              <a-input-number
                v-model:value="temp.timeout"
                :min="1"
                :placeholder="$t('pages.system.assets.ssh.ssh-list.65206e07')"
                style="width: 100%"
              />
            </a-form-item>
            <a-form-item :label="$t('pages.system.assets.ssh.ssh-list.81de6a73')" name="suffix">
              <template #help>
                {{ $t('pages.system.assets.ssh.ssh-list.624c02e3') }},
                {{ $t('pages.system.assets.ssh.ssh-list.993dee9e')
                }}<span style="color: red">{{ $t('pages.system.assets.ssh.ssh-list.423d017d') }}</span
                >）{{ $t('pages.system.assets.ssh.ssh-list.6ca91945') }}
              </template>
              <a-textarea
                v-model:value="temp.allowEditSuffix"
                :rows="5"
                style="resize: none"
                :placeholder="$t('pages.system.assets.ssh.ssh-list.a4ad687c')"
              />
            </a-form-item>
          </a-form>
        </a-modal>
        <!-- 安装节点 -->
        <a-modal
          v-model:open="nodeVisible"
          destroy-on-close
          width="80%"
          :title="$t('pages.system.assets.ssh.ssh-list.88bbfe77')"
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
          :title="`${temp.name} ${$t('pages.system.assets.ssh.ssh-list.502f94')}`"
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
          :title="$t('pages.system.assets.ssh.ssh-list.9c06953e')"
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
          :title="$t('pages.system.assets.ssh.ssh-list.ef4af3d6')"
          :footer="null"
          :mask-closable="false"
        >
          <a-space direction="vertical" style="width: 100%">
            <a-alert
              v-if="workspaceSshList && workspaceSshList.length"
              :message="$t('pages.system.assets.ssh.ssh-list.27af6349')"
              type="info"
              show-icon
            />
            <a-list bordered :data-source="workspaceSshList">
              <template #renderItem="{ item }">
                <a-list-item style="display: block">
                  <a-row>
                    <a-col :span="10">SSH{{ $t('pages.system.assets.ssh.ssh-list.bb769c1d') }}{{ item.name }}</a-col>
                    <a-col :span="10"
                      >{{ $t('pages.system.assets.ssh.ssh-list.c6a70442')
                      }}{{ item.workspace && item.workspace.name }}</a-col
                    >
                    <a-col :span="4">
                      <a-button v-if="item.workspace" size="small" type="primary" @click="configWorkspaceSsh(item)"
                        >{{ $t('pages.system.assets.ssh.ssh-list.7de61746') }}
                      </a-button>
                      <a-button v-else size="small" type="primary" danger @click="handleDeleteWorkspaceItem(item)"
                        >{{ $t('pages.system.assets.ssh.ssh-list.2f14e7d4') }}
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
          :title="$t('pages.system.assets.ssh.ssh-list.72ca2f8d')"
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
              <a-alert :message="$t('pages.system.assets.ssh.ssh-list.fa73eb9')" banner />
            </a-form-item>
            <a-form-item :label="$t('pages.system.assets.ssh.ssh-list.493a5eda')">
              <a-input
                v-model:value="temp.name"
                :disabled="true"
                :max-length="50"
                :placeholder="$t('pages.system.assets.ssh.ssh-list.493a5eda')"
              />
            </a-form-item>
            <a-form-item :label="$t('pages.system.assets.ssh.ssh-list.d83f3e09')">
              <a-input
                v-model:value="temp.workspaceName"
                :disabled="true"
                :max-length="50"
                :placeholder="$t('pages.system.assets.ssh.ssh-list.d83f3e09')"
              />
            </a-form-item>

            <a-form-item name="fileDirs">
              <template #label>
                <a-tooltip>
                  {{ $t('pages.system.assets.ssh.ssh-list.831dbe15') }}
                  <template #title> {{ $t('pages.system.assets.ssh.ssh-list.98ae50d0') }} </template>
                  <QuestionCircleOutlined />
                </a-tooltip>
              </template>
              <a-textarea
                v-model:value="temp.fileDirs"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                :placeholder="$t('pages.system.assets.ssh.ssh-list.fc6eda86')"
              />
            </a-form-item>

            <a-form-item :label="$t('pages.system.assets.ssh.ssh-list.81de6a73')" name="suffix">
              <a-textarea
                v-model:value="temp.allowEditSuffix"
                :rows="5"
                style="resize: none"
                :placeholder="$t('pages.system.assets.ssh.ssh-list.a4ad687c')"
              />
            </a-form-item>
            <a-form-item name="notAllowedCommand">
              <template #label>
                <a-tooltip>
                  {{ $t('pages.system.assets.ssh.ssh-list.a184b188') }}
                  <template #title>
                    {{ $t('pages.system.assets.ssh.ssh-list.7dc091e8') }}
                    <ul>
                      <li>{{ $t('pages.system.assets.ssh.ssh-list.a0d0e02d') }}</li>
                      <li>{{ $t('pages.system.assets.ssh.ssh-list.7ec9cba8') }}</li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined />
                </a-tooltip>
              </template>
              <a-textarea
                v-model:value="temp.notAllowedCommand"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                :placeholder="$t('pages.system.assets.ssh.ssh-list.4c6fdb8f')"
              />
            </a-form-item>
          </a-form>
        </a-modal>
        <!-- 分配到其他工作空间 -->
        <a-modal
          v-model:open="syncToWorkspaceVisible"
          destroy-on-close
          :confirm-loading="confirmLoading"
          :title="$t('pages.system.assets.ssh.ssh-list.41743f79')"
          :mask-closable="false"
          @ok="handleSyncToWorkspace"
        >
          <a-space direction="vertical" style="width: 100%">
            <a-alert :message="$t('pages.system.assets.ssh.ssh-list.2ae8180f')" type="warning" show-icon>
              <template #description>{{ $t('pages.system.assets.ssh.ssh-list.18510b30') }}</template>
            </a-alert>
            <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
              <a-form-item :label="$t('pages.system.assets.ssh.ssh-list.b86b400d')" name="workspaceId">
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
                  :placeholder="$t('pages.system.assets.ssh.ssh-list.3a321a02')"
                >
                  <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-form>
          </a-space>
        </a-modal>
      </a-tab-pane>
      <a-tab-pane key="2" :tab="$t('pages.system.assets.ssh.ssh-list.8a2d08b3')">
        <OperationLog type="machinessh"></OperationLog
      ></a-tab-pane>
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
        { label: this.$t('pages.system.assets.ssh.ssh-list.c3891788'), value: 'PASS' },
        { label: this.$t('pages.system.assets.ssh.ssh-list.e9b5946'), value: 'PUBKEY' }
      ],
      columns: [
        {
          title: this.$t('pages.system.assets.ssh.ssh-list.2bf15b5b'),
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
          title: this.$t('pages.system.assets.ssh.ssh-list.e15572a'),
          dataIndex: 'user',
          sorter: true,
          width: '80px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.system.assets.ssh.ssh-list.1c01cf58'),
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
          title: this.$t('pages.system.assets.ssh.ssh-list.d5f99ae'),
          dataIndex: 'osOccupyMemory',
          sorter: true,
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('pages.system.assets.ssh.ssh-list.e9213f19'),
          dataIndex: 'osMaxOccupyDisk',
          sorter: true,
          width: '100px',
          ellipsis: true
        },
        // { title: "编码格式", dataIndex: "charset", sorter: true, width: 120, ellipsis: true,},
        {
          title: this.$t('pages.system.assets.ssh.ssh-list.1d119f3f'),
          dataIndex: 'status',
          ellipsis: true,
          align: 'center',
          width: '100px'
        },
        {
          title: this.$t('pages.system.assets.ssh.ssh-list.d4246d25'),
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
          title: this.$t('pages.system.assets.ssh.ssh-list.f06e8846'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.system.assets.ssh.ssh-list.61164914'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.system.assets.ssh.ssh-list.3bb962bf'),
          dataIndex: 'operation',

          width: '310px',
          align: 'center',
          // ellipsis: true,
          fixed: 'right'
        }
      ],
      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$t('pages.system.assets.ssh.ssh-list.95105178'), trigger: 'blur' }],
        host: [{ required: true, message: this.$t('pages.system.assets.ssh.ssh-list.12b7c7b1'), trigger: 'blur' }],
        port: [{ required: true, message: this.$t('pages.system.assets.ssh.ssh-list.4d688005'), trigger: 'blur' }],
        connectType: [
          {
            required: true,
            message: this.$t('pages.system.assets.ssh.ssh-list.b3338613'),
            trigger: 'blur'
          }
        ],
        user: [{ required: true, message: this.$t('pages.system.assets.ssh.ssh-list.fee12658'), trigger: 'blur' }],
        password: [{ required: true, message: this.$t('pages.system.assets.ssh.ssh-list.5242e286'), trigger: 'blur' }]
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
        title: this.$t('pages.system.assets.ssh.ssh-list.a8fe4c17'),
        content: this.$t('pages.system.assets.ssh.ssh-list.1879255f'),
        zIndex: 1009,
        okText: this.$t('pages.system.assets.ssh.ssh-list.7da4a591'),
        cancelText: this.$t('pages.system.assets.ssh.ssh-list.43105e21'),
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
        title: this.$t('pages.system.assets.ssh.ssh-list.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.system.assets.ssh.ssh-list.885b965f'),
        okText: this.$t('pages.system.assets.ssh.ssh-list.7da4a591'),
        cancelText: this.$t('pages.system.assets.ssh.ssh-list.43105e21'),
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
          message: this.$t('pages.system.assets.ssh.ssh-list.3a321a02')
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
        title: this.$t('pages.system.assets.ssh.ssh-list.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.system.assets.ssh.ssh-list.8ad9d6d6'),
        okText: this.$t('pages.system.assets.ssh.ssh-list.7da4a591'),
        cancelText: this.$t('pages.system.assets.ssh.ssh-list.43105e21'),
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
