<template>
  <div>
    <template v-if="type === 'container'">
      <a-table
        :data-source="list"
        size="middle"
        :columns="columns"
        :pagination="false"
        bordered
        row-key="id"
        :scroll="{
          x: 'max-content'
        }"
      >
        <template #title>
          <a-space>
            <a-input
              v-model:value="listQuery['name']"
              :placeholder="$t('i18n_d7ec2d3fea')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['containerId']"
              :placeholder="$t('i18n_74dc77d4f7')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['imageId']"
              :placeholder="$t('i18n_72e7a5d105')"
              class="search-input-item"
              @keyup.enter="loadData"
            />
            <div>
              {{ $t('i18n_607e7a4f37') }}
              <a-switch
                v-model:checked="listQuery['showAll']"
                :checked-children="$t('i18n_9a7b52fc86')"
                :un-checked-children="$t('i18n_d679aea3aa')"
              />
            </div>
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
            <a-statistic-countdown
              format="s"
              :title="$t('i18n_0f8403d07e') + ' '"
              :value="countdownTime"
              @finish="autoUpdate"
            >
              <template #suffix>
                <div style="font-size: 12px">{{ $t('i18n_ee6ce96abb') }}</div>
              </template>
            </a-statistic-countdown>
          </a-space>
        </template>
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'names'">
            <a-popover :title="`${$t('i18n_0c256f73b8')}${(text || []).join(',')}`">
              <template #content>
                <p>{{ $t('i18n_abee751418') }}{{ record.id }}</p>
                <p>{{ $t('i18n_26eccfaad1') }}{{ record.image }}</p>
                <p>{{ $t('i18n_c87bd94cd7') }}{{ record.imageId }}</p>
              </template>

              <span>{{ (text || []).join(',') }}</span>
            </a-popover>
          </template>

          <template v-else-if="column.dataIndex === 'labels'">
            <a-popover :title="`${$t('i18n_89cfb655e0')}`">
              <template #content>
                <template v-if="record.labels">
                  <p v-for="(value, key) in record.labels" :key="key">
                    {{ key }}
                    <ArrowRightOutlined />

                    {{ value }}
                  </p>
                </template>
              </template>
              <template v-if="record.labels && Object.keys(record.labels).length">
                <span>{{ (record.labels && Object.keys(record.labels).length) || 0 }} <TagsOutlined /></span>
              </template>
              <template v-else>-</template>
            </a-popover>
          </template>
          <template v-else-if="column.dataIndex === 'mounts'">
            <a-popover :title="`${$t('i18n_9964d6ed3f')}`">
              <template #content>
                <template v-if="record.mounts">
                  <div v-for="(item, index) in record.mounts" :key="index">
                    <p>
                      {{ $t('i18n_5b47861521') }}{{ item.name }}
                      <a-tag>{{ item.rw ? $t('i18n_2300ad28b8') : $t('i18n_75769d1ac8') }}</a-tag>
                    </p>
                    <p>{{ $t('i18n_e362bc0e8a') }}</p>
                    <a-divider></a-divider>
                  </div>
                </template>
              </template>
              <template v-if="record.mounts && Object.keys(record.mounts).length">
                <span>{{ (record.mounts && Object.keys(record.mounts).length) || 0 }} <ApiOutlined /></span>
              </template>
              <template v-else>-</template>
            </a-popover>
          </template>

          <template v-else-if="column.tooltip">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.showid">
            <a-tooltip placement="topLeft" :title="text">
              <span style="display: none"> {{ (array = text.split(':')) }}</span>
              <span>{{ array[array.length - 1].slice(0, 12) }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'ports'">
            <a-popover placement="topLeft">
              <template #title>
                {{ $t('i18n_d94167ab19') }}
                <ul>
                  <li v-for="(item, index) in text || []" :key="index">
                    {{ item.type + ' ' + (item.ip || '') + ':' + (item.publicPort || '') + ':' + item.privatePort }}
                  </li>
                </ul>
              </template>
              <template #content>
                <template v-if="record.networkSettings">
                  <template v-if="record.networkSettings.networks">
                    <template v-if="record.networkSettings.networks.bridge">
                      {{ $t('i18n_71ee088528') }}
                      <p v-if="record.networkSettings.networks.bridge.ipAddress">
                        IP:
                        <a-tag>{{ record.networkSettings.networks.bridge.ipAddress }}</a-tag>
                      </p>
                      <p v-if="record.networkSettings.networks.bridge.macAddress">
                        MAC:
                        <a-tag>{{ record.networkSettings.networks.bridge.macAddress }}</a-tag>
                      </p>
                      <p v-if="record.networkSettings.networks.bridge.gateway">
                        {{ $t('i18n_f332f2c8df') }}:
                        <a-tag>{{ record.networkSettings.networks.bridge.gateway }}</a-tag>
                      </p>
                      <p v-if="record.networkSettings.networks.bridge.networkID">
                        networkID:
                        <a-tag>{{ record.networkSettings.networks.bridge.networkID }}</a-tag>
                      </p>
                      <p v-if="record.networkSettings.networks.bridge.endpointId">
                        endpointId:
                        <a-tag>{{ record.networkSettings.networks.bridge.endpointId }}</a-tag>
                      </p>
                    </template>
                    <template v-if="record.networkSettings.networks.ingress">
                      <p v-if="record.networkSettings.networks.ingress.ipAddress">
                        IP:
                        <a-tag>{{ record.networkSettings.networks.ingress.ipAddress }}</a-tag>
                      </p>
                      <p v-if="record.networkSettings.networks.ingress.macAddress">
                        MAC:
                        <a-tag>{{ record.networkSettings.networks.ingress.macAddress }}</a-tag>
                      </p>
                      <p v-if="record.networkSettings.networks.ingress.gateway">
                        {{ $t('i18n_f332f2c8df') }}:
                        <a-tag>{{ record.networkSettings.networks.ingress.gateway }}</a-tag>
                      </p>
                      <p v-if="record.networkSettings.networks.ingress.networkID">
                        networkID:
                        <a-tag>{{ record.networkSettings.networks.ingress.networkID }}</a-tag>
                      </p>
                      <p v-if="record.networkSettings.networks.ingress.endpointId">
                        endpointId:
                        <a-tag>{{ record.networkSettings.networks.ingress.endpointId }}</a-tag>
                      </p>
                    </template>
                  </template>
                </template>
              </template>
              <span>{{
                (text || [])
                  .slice(0, 2)
                  .map((item) => {
                    return item.type + ' ' + (item.publicPort || '') + ':' + item.privatePort
                  })
                  .join('/')
              }}</span>
            </a-popover>
          </template>

          <template v-else-if="column.dataIndex === 'state'">
            <a-tooltip :title="(record.status || '') + $t('i18n_aac62bc255')" @click="viewLog(record)">
              <a-switch :checked="text === 'running'" :disabled="true">
                <template #checkedChildren>
                  <CheckCircleOutlined />
                </template>
                <template #unCheckedChildren>
                  <WarningOutlined />
                </template>
              </a-switch>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'operation'">
            <a-space>
              <template v-if="record.state === 'running'">
                <a-tooltip :title="$t('i18n_4fb2400af7')">
                  <a-button
                    size="small"
                    type="link"
                    :disabled="record.state !== 'running'"
                    @click="handleTerminal(record)"
                    ><CodeOutlined
                  /></a-button>
                </a-tooltip>
                <a-tooltip :title="$t('i18n_095e938e2a')">
                  <a-button size="small" type="link" @click="doAction(record, 'stop')"><StopOutlined /></a-button>
                </a-tooltip>
                <a-tooltip :title="$t('i18n_01b4e06f39')">
                  <a-button size="small" type="link" @click="doAction(record, 'restart')"><ReloadOutlined /></a-button>
                </a-tooltip>
              </template>
              <template v-else>
                <a-tooltip :title="$t('i18n_8e54ddfe24')">
                  <a-button size="small" type="link" @click="doAction(record, 'start')">
                    <PlayCircleOutlined />
                  </a-button>
                </a-tooltip>
                <a-tooltip :title="$t('i18n_095e938e2a')">
                  <a-button size="small" type="link" :disabled="true"><StopOutlined /></a-button>
                </a-tooltip>
                <a-tooltip :title="$t('i18n_01b4e06f39')">
                  <a-button size="small" type="link" :disabled="true"><ReloadOutlined /></a-button>
                </a-tooltip>
              </template>

              <a-dropdown>
                <a @click="(e) => e.preventDefault()">
                  <MoreOutlined />
                </a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item>
                      <a-tooltip :title="$t('i18n_48a536d0bb')">
                        <a-button size="small" type="link" @click="rebuild(record)"
                          ><RedoOutlined />{{ $t('i18n_9e09315960') }}</a-button
                        >
                      </a-tooltip>
                    </a-menu-item>
                    <a-menu-item>
                      <a-tooltip :title="$t('i18n_211a60b1d6')">
                        <a-button
                          size="small"
                          type="link"
                          :disabled="record.state !== 'running'"
                          @click="editContainer(record)"
                        >
                          <EditOutlined />{{ $t('i18n_95b351c862') }}
                        </a-button>
                      </a-tooltip>
                    </a-menu-item>
                    <a-menu-item>
                      <a-tooltip :title="$t('i18n_aac62bc255')">
                        <a-button size="small" type="link" @click="viewLog(record)"
                          ><MessageOutlined />{{ $t('i18n_456d29ef8b') }}</a-button
                        >
                      </a-tooltip>
                    </a-menu-item>
                    <a-menu-item>
                      <a-tooltip :title="$t('i18n_ba20f0444c')">
                        <a-button size="small" type="link" @click="doAction(record, 'remove')">
                          <DeleteOutlined />{{ $t('i18n_2f4aaddde3') }}
                        </a-button>
                      </a-tooltip>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>
    <template v-else-if="type === 'compose'">
      <a-card>
        <template #title>
          <a-space>
            <a-input
              v-model:value="listQuery['name']"
              :placeholder="$t('i18n_d7ec2d3fea')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['containerId']"
              :placeholder="$t('i18n_74dc77d4f7')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['imageId']"
              :placeholder="$t('i18n_72e7a5d105')"
              class="search-input-item"
              @keyup.enter="loadData"
            />
            <div>
              {{ $t('i18n_607e7a4f37') }}
              <a-switch
                v-model:checked="listQuery['showAll']"
                :checked-children="$t('i18n_9a7b52fc86')"
                :un-checked-children="$t('i18n_d679aea3aa')"
              />
            </div>
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
            <a-statistic-countdown
              format="s"
              :title="$t('i18n_0f8403d07e') + ' '"
              :value="countdownTime"
              @finish="autoUpdate"
            >
              <template #suffix>
                <div style="font-size: 12px">{{ $t('i18n_ee6ce96abb') }}</div>
              </template>
            </a-statistic-countdown>
          </a-space>
        </template>
        <a-collapse v-if="list && list.length">
          <a-collapse-panel v-for="(item2, index) in list" :key="index">
            <template #header>
              <a-space>
                <span>{{ item2.name }}</span>
                <span>
                  <span style="display: none">
                    {{
                      (array = (item2.child || []).map((item) => {
                        return item.state
                      }))
                    }}
                    {{
                      (runningCount = array
                        .map((item) => {
                          return item === 'running' ? 1 : 0
                        })
                        .reduce((prev, curr) => {
                          return prev + curr
                        }, 0))
                    }}</span
                  >
                  <span v-if="runningCount">Running({{ runningCount }}/{{ array.length }})</span>
                  <span v-else>Exited</span>
                </span>
              </a-space>
            </template>
            <a-table
              :data-source="item2.child"
              size="middle"
              :columns="columns"
              :pagination="false"
              bordered
              row-key="id"
              :scroll="{
                x: 'max-content'
              }"
            >
              <template #bodyCell="{ column, text, record }">
                <template v-if="column.dataIndex === 'names'">
                  <a-popover :title="`${$t('i18n_0c256f73b8')}${(text || []).join(',')}`">
                    <template #content>
                      <p>{{ $t('i18n_abee751418') }}{{ record.id }}</p>
                      <p>{{ $t('i18n_26eccfaad1') }}{{ record.image }}</p>
                      <p>{{ $t('i18n_c87bd94cd7') }}{{ record.imageId }}</p>
                    </template>

                    <span>{{ (text || []).join(',') }}</span>
                  </a-popover>
                </template>

                <template v-else-if="column.dataIndex === 'labels'">
                  <a-popover :title="`${$t('i18n_89cfb655e0')}`">
                    <template #content>
                      <template v-if="record.labels">
                        <p v-for="(value, key) in record.labels" :key="key">
                          {{ key }}

                          <ArrowRightOutlined />
                          {{ value }}
                        </p>
                      </template>
                    </template>
                    <template v-if="record.labels && Object.keys(record.labels).length">
                      <span>{{ (record.labels && Object.keys(record.labels).length) || 0 }} <TagsOutlined /></span>
                    </template>
                    <template v-else>-</template>
                  </a-popover>
                </template>
                <template v-else-if="column.dataIndex === 'mounts'">
                  <a-popover :title="`${$t('i18n_9964d6ed3f')}`">
                    <template #content>
                      <template v-if="record.mounts">
                        <div v-for="(item, idx) in record.mounts" :key="idx">
                          <p>
                            {{ $t('i18n_5b47861521') }}{{ item.name }}
                            <a-tag>{{ item.rw ? $t('i18n_2300ad28b8') : $t('i18n_75769d1ac8') }}</a-tag>
                          </p>
                          <p>
                            {{ $t('i18n_e362bc0e8a', { source: item.source, destination: item.destination }) }}
                          </p>
                          <a-divider></a-divider>
                        </div>
                      </template>
                    </template>
                    <template v-if="record.mounts && Object.keys(record.mounts).length">
                      <span>{{ (record.mounts && Object.keys(record.mounts).length) || 0 }} <ApiOutlined /></span>
                    </template>
                    <template v-else>-</template>
                  </a-popover>
                </template>

                <template v-else-if="column.tooltip">
                  <a-tooltip placement="topLeft" :title="text">
                    <span>{{ text }}</span>
                  </a-tooltip>
                </template>

                <template v-else-if="column.showid">
                  <a-tooltip placement="topLeft" :title="text">
                    <span style="display: none"> {{ (array = text.split(':')) }}</span>
                    <span>{{ array[array.length - 1].slice(0, 12) }}</span>
                  </a-tooltip>
                </template>

                <template v-else-if="column.dataIndex === 'ports'">
                  <a-popover placement="topLeft">
                    <template #title>
                      {{ $t('i18n_d94167ab19') }}
                      <ul>
                        <li v-for="(item, idx) in text || []" :key="idx">
                          {{
                            item.type + ' ' + (item.ip || '') + ':' + (item.publicPort || '') + ':' + item.privatePort
                          }}
                        </li>
                      </ul>
                    </template>
                    <template #content>
                      <template v-if="record.networkSettings">
                        <template v-if="record.networkSettings.networks">
                          <template v-if="record.networkSettings.networks.bridge">
                            {{ $t('i18n_71ee088528') }}
                            <p v-if="record.networkSettings.networks.bridge.ipAddress">
                              IP:
                              <a-tag>{{ record.networkSettings.networks.bridge.ipAddress }}</a-tag>
                            </p>
                            <p v-if="record.networkSettings.networks.bridge.macAddress">
                              MAC:
                              <a-tag>{{ record.networkSettings.networks.bridge.macAddress }}</a-tag>
                            </p>
                            <p v-if="record.networkSettings.networks.bridge.gateway">
                              {{ $t('i18n_f332f2c8df') }}:
                              <a-tag>{{ record.networkSettings.networks.bridge.gateway }}</a-tag>
                            </p>
                            <p v-if="record.networkSettings.networks.bridge.networkID">
                              networkID:
                              <a-tag>{{ record.networkSettings.networks.bridge.networkID }}</a-tag>
                            </p>
                            <p v-if="record.networkSettings.networks.bridge.endpointId">
                              endpointId:
                              <a-tag>{{ record.networkSettings.networks.bridge.endpointId }}</a-tag>
                            </p>
                          </template>
                          <template v-if="record.networkSettings.networks.ingress">
                            <p v-if="record.networkSettings.networks.ingress.ipAddress">
                              IP:
                              <a-tag>{{ record.networkSettings.networks.ingress.ipAddress }}</a-tag>
                            </p>
                            <p v-if="record.networkSettings.networks.ingress.macAddress">
                              MAC:
                              <a-tag>{{ record.networkSettings.networks.ingress.macAddress }}</a-tag>
                            </p>
                            <p v-if="record.networkSettings.networks.ingress.gateway">
                              {{ $t('i18n_f332f2c8df') }}:
                              <a-tag>{{ record.networkSettings.networks.ingress.gateway }}</a-tag>
                            </p>
                            <p v-if="record.networkSettings.networks.ingress.networkID">
                              networkID:
                              <a-tag>{{ record.networkSettings.networks.ingress.networkID }}</a-tag>
                            </p>
                            <p v-if="record.networkSettings.networks.ingress.endpointId">
                              endpointId:
                              <a-tag>{{ record.networkSettings.networks.ingress.endpointId }}</a-tag>
                            </p>
                          </template>
                        </template>
                      </template>
                    </template>
                    <span>{{
                      (text || [])
                        .map((item) => {
                          return item.type + ' ' + (item.publicPort || '') + ':' + item.privatePort
                        })
                        .join('/')
                    }}</span>
                  </a-popover>
                </template>

                <template v-else-if="column.dataIndex === 'state'">
                  <a-tooltip :title="(record.status || '') + $t('i18n_aac62bc255')" @click="viewLog(record)">
                    <a-switch :checked="record.state === 'running'" :disabled="true">
                      <template #checkedChildren>
                        <CheckCircleOutlined />
                      </template>
                      <template #unCheckedChildren>
                        <WarningOutlined />
                      </template>
                    </a-switch>
                  </a-tooltip>
                </template>
                <template v-else-if="column.dataIndex === 'operation'">
                  <a-space>
                    <template v-if="record.state === 'running'">
                      <a-tooltip :title="$t('i18n_4fb2400af7')">
                        <a-button size="small" type="link" @click="handleTerminal(record)"><CodeOutlined /></a-button>
                      </a-tooltip>
                      <a-tooltip :title="$t('i18n_095e938e2a')">
                        <a-button size="small" type="link" @click="doAction(record, 'stop')"><StopOutlined /></a-button>
                      </a-tooltip>
                      <a-tooltip :title="$t('i18n_01b4e06f39')">
                        <a-button size="small" type="link" @click="doAction(record, 'restart')">
                          <ReloadOutlined />
                        </a-button>
                      </a-tooltip>
                    </template>
                    <template v-else>
                      <a-tooltip :title="$t('i18n_8e54ddfe24')">
                        <a-button size="small" type="link" @click="doAction(record, 'start')">
                          <PlayCircleOutlined />
                        </a-button>
                      </a-tooltip>
                      <a-tooltip :title="$t('i18n_095e938e2a')">
                        <a-button size="small" type="link" :disabled="true"><StopOutlined /></a-button>
                      </a-tooltip>
                      <a-tooltip :title="$t('i18n_01b4e06f39')">
                        <a-button size="small" type="link" :disabled="true"><ReloadOutlined /></a-button>
                      </a-tooltip>
                    </template>

                    <a-dropdown>
                      <a @click="(e) => e.preventDefault()">
                        <MoreOutlined />
                      </a>
                      <template #overlay>
                        <a-menu>
                          <a-menu-item>
                            <a-tooltip :title="$t('i18n_48a536d0bb')">
                              <a-button size="small" type="link" @click="rebuild(record)"
                                ><RedoOutlined />{{ $t('i18n_9e09315960') }}</a-button
                              >
                            </a-tooltip>
                          </a-menu-item>
                          <a-menu-item>
                            <a-tooltip :title="$t('i18n_211a60b1d6')">
                              <a-button
                                size="small"
                                type="link"
                                :disabled="record.state !== 'running'"
                                @click="editContainer(record)"
                              >
                                <EditOutlined />{{ $t('i18n_95b351c862') }}
                              </a-button>
                            </a-tooltip>
                          </a-menu-item>
                          <a-menu-item>
                            <a-tooltip :title="$t('i18n_aac62bc255')">
                              <a-button size="small" type="link" @click="viewLog(record)">
                                <MessageOutlined />{{ $t('i18n_456d29ef8b') }}
                              </a-button>
                            </a-tooltip>
                          </a-menu-item>
                          <a-menu-item>
                            <a-tooltip :title="$t('i18n_ba20f0444c')">
                              <a-button size="small" type="link" @click="doAction(record, 'remove')">
                                <DeleteOutlined />{{ $t('i18n_2f4aaddde3') }}
                              </a-button>
                            </a-tooltip>
                          </a-menu-item>
                        </a-menu>
                      </template>
                    </a-dropdown>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-collapse-panel>
        </a-collapse>
        <a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE">
          <template #description>{{ $t('i18n_21efd88b67') }}</template>
        </a-empty>
      </a-card>

      <!-- <a-table
        :data-source="list"
        size="middle"
        :columns="parentColumns"
        :pagination="false"
        bordered
        :scroll="{
          x: 'max-content'
        }"
      >
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.tooltip">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'state'"> </template>
        </template>
        <template #expandedRowRender="{ record }"> </template>
      </a-table> -->
    </template>
    <!-- 日志 -->

    <log-view2
      v-if="logVisible > 0"
      :id="id"
      :visible="logVisible != 0"
      :url-prefix="urlPrefix"
      :machine-docker-id="machineDockerId"
      :container-id="temp.id"
      @close="
        () => {
          logVisible = 0
        }
      "
    />

    <!-- Terminal -->
    <CustomModal
      v-if="terminalVisible"
      v-model:open="terminalVisible"
      width="80vw"
      :body-style="{
        padding: '0 10px',
        paddingTop: '10px',
        marginRight: '10px',
        height: `70vh`
      }"
      :title="`docker cli ${(temp.names || []).join(',')}`"
      :footer="null"
      :mask-closable="false"
    >
      <terminal2 v-if="terminalVisible" :id="id" :machine-docker-id="machineDockerId" :container-id="temp.id" />
    </CustomModal>
    <!-- 编辑容器配置 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60vw"
      :title="$t('i18n_1ba584c974')"
      :mask-closable="false"
      @ok="
        () => {
          $refs.editContainer
            .handleEditOk()
            .then(() => {
              editVisible = false
              loadData()
            })
            .finally(() => {
              confirmLoading = false
            })
        }
      "
    >
      <editContainer
        :id="id"
        ref="editContainer"
        :machine-docker-id="machineDockerId"
        :url-prefix="urlPrefix"
        :container-id="temp.id"
      ></editContainer>
    </CustomModal>
    <!-- rebuild container -->

    <BuildContainer
      v-if="buildVisible"
      :id="id"
      :image-id="temp.imageId"
      :machine-docker-id="machineDockerId"
      :url-prefix="urlPrefix"
      :container-id="temp.id"
      :container-data="temp"
      @cancel-btn-click="
        () => {
          buildVisible = false
        }
      "
      @confirm-btn-click="
        () => {
          buildVisible = false
          loadData()
        }
      "
    />
  </div>
</template>
<script>
import { parseTime } from '@/utils/const'
import {
  dockerContainerList,
  dockerContainerRemove,
  dockerContainerRestart,
  dockerContainerStart,
  dockerContainerStop,
  dockerContainerListCompose
} from '@/api/docker-api'
import LogView2 from '@/pages/docker/log-view'
import Terminal2 from '@/pages/docker/terminal'
import editContainer from './editContainer.vue'
import BuildContainer from './buildContainer.vue'
import { Empty } from 'ant-design-vue'
export default {
  name: 'Container',
  components: {
    LogView2,
    Terminal2,
    editContainer,
    BuildContainer
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String,
      default: ''
    },
    machineDockerId: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      // container  or compose
      default: 'container'
    }
  },
  data() {
    return {
      Empty,
      list: [],
      loading: false,
      listQuery: {
        showAll: true
      },
      terminalVisible: false,
      logVisible: 0,
      temp: {},
      confirmLoading: false,
      columns: [
        {
          title: this.$t('i18n_faaadc447b'),
          width: '60px',
          // ellipsis: true,
          align: 'center',
          customRender: ({ index }) => `${index + 1}`
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'names',
          ellipsis: true
          // width: 150
        },
        {
          title: this.$t('i18n_87d50f8e03'),
          dataIndex: 'id',
          ellipsis: true,
          width: '10px',
          showid: true
        },
        {
          title: this.$t('i18n_40aff14380'),
          dataIndex: 'imageId',
          ellipsis: true,
          width: '130px',
          showid: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: 'state',
          // ellipsis: true,
          align: 'center',
          width: '80px'
        },

        {
          title: this.$t('i18n_c76cfefe72'),
          dataIndex: 'ports',
          ellipsis: true,
          width: '100px'
        },

        {
          title: this.$t('i18n_14d342362f'),
          dataIndex: 'labels',
          ellipsis: true,
          width: '50px'
        },
        {
          title: this.$t('i18n_9964d6ed3f'),
          dataIndex: 'mounts',
          ellipsis: true,
          width: '50px'
        },
        {
          title: this.$t('i18n_ddf7d2a5ce'),
          dataIndex: 'command',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'created',
          ellipsis: true,
          sorter: (a, b) => Number(a.created) - new Number(b.created),
          sortDirections: ['descend', 'ascend'],
          defaultSortOrder: 'descend',
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          fixed: 'right',

          width: '160px'
        }
      ],

      // parentColumns: [
      //   {
      //     title: '序号',
      //     width: '80px',
      //     ellipsis: true,
      //     align: 'center',
      //     customRender: ({ text, record, index }) => `${index + 1}`
      //   },
      //   {
      //     title: '名称',
      //     width: 200,
      //     dataIndex: 'name',
      //     ellipsis: true,
      //     tooltip: true
      //   },
      //   {
      //     title: '状态',
      //     dataIndex: 'state',
      //     width: '150px',
      //     ellipsis: true
      //   },
      //   {
      //     title: '操作',
      //     width: '80px',
      //     ellipsis: true
      //   }
      // ],
      action: {
        remove: {
          msg: this.$t('i18n_c469afafe0'),
          api: dockerContainerRemove
        },
        stop: {
          msg: this.$t('i18n_60b4c08f5c'),
          api: dockerContainerStop
        },
        restart: {
          msg: this.$t('i18n_bf77165638'),
          api: dockerContainerRestart
        },
        start: {
          msg: this.$t('i18n_2b0aa77353'),
          api: dockerContainerStart
        }
      },
      editVisible: false,

      countdownTime: Date.now(),

      buildVisible: false
    }
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId
    }
  },
  beforeUnmount() {},
  mounted() {
    this.autoUpdate()
  },
  methods: {
    autoUpdate() {
      this.loadData()
    },
    // 加载数据
    loadData() {
      if (!this.visible) {
        return
      }
      this.loading = true
      //this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.id = this.reqDataId
      ;(this.type === 'container'
        ? dockerContainerList(this.urlPrefix, this.listQuery)
        : dockerContainerListCompose(this.urlPrefix, this.listQuery)
      ).then((res) => {
        if (res.code === 200) {
          this.list = this.sortPort(res.data || []).map((item) => {
            let child = item.child
            if (child) {
              child = this.sortPort(child)
            }
            return { ...item, child: child }
          })
        }
        this.loading = false
        this.countdownTime = Date.now() + 5 * 1000
      })
    },
    sortPort(list) {
      return list.map((item) => {
        let ports = item.ports
        if (ports) {
          try {
            ports = ports.sort(
              (a, b) =>
                a.privatePort - b.privatePort ||
                (a.type || '').toLowerCase().localeCompare((b.type || '').toLowerCase())
            )
          } catch (e) {
            console.error(e)
          }
        }

        return { ...item, ports: ports }
      })
    },
    doAction(record, actionKey) {
      const action = this.action[actionKey]
      if (!action) {
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: action.msg,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return action
            .api(this.urlPrefix, {
              id: this.reqDataId,
              containerId: record.id
            })
            .then((res) => {
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
    viewLog(record) {
      this.logVisible = new Date() * Math.random()
      this.temp = record
    },
    // 进入终端
    handleTerminal(record) {
      this.temp = Object.assign({}, record)
      this.terminalVisible = true
    },
    editContainer(record) {
      this.temp = Object.assign({}, record)
      this.editVisible = true
      // console.log(this.temp);
    },
    // click rebuild button
    rebuild(record) {
      this.temp = Object.assign({}, record)
      this.buildVisible = true
    }
  }
}
</script>
<style scoped>
:deep(.ant-statistic div) {
  display: inline-block;
  font-weight: normal;
}
:deep(.ant-statistic-content-value, .ant-statistic-content) {
  font-size: 16px;
}
</style>
