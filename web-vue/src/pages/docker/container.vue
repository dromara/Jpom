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
              :placeholder="$t('pages.docker.container.3e34ec28')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['containerId']"
              :placeholder="$t('pages.docker.container.cfda881')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['imageId']"
              :placeholder="$t('pages.docker.container.e2840598')"
              class="search-input-item"
              @keyup.enter="loadData"
            />
            <div>
              {{ $t('pages.docker.container.9eea39a0') }}
              <a-switch
                v-model:checked="listQuery['showAll']"
                :checked-children="$t('pages.docker.container.9c048a42')"
                :un-checked-children="$t('pages.docker.container.dd568fba')"
              />
            </div>
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.docker.container.a1f640f4')
            }}</a-button>
            <a-statistic-countdown
              format="s"
              :title="$t('pages.docker.container.c48b0e64') + ' '"
              :value="countdownTime"
              @finish="autoUpdate"
            >
              <template #suffix>
                <div style="font-size: 12px">{{ $t('pages.docker.container.dda10f33') }}</div>
              </template>
            </a-statistic-countdown>
          </a-space>
        </template>
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'names'">
            <a-popover :title="`${$t('pages.docker.container.cddbe6bd')}${(text || []).join(',')}`">
              <template #content>
                <p>{{ $t('pages.docker.container.da9678ab') }}{{ record.id }}</p>
                <p>{{ $t('pages.docker.container.198b1e02') }}{{ record.image }}</p>
                <p>{{ $t('pages.docker.container.93bac13d') }}{{ record.imageId }}</p>
              </template>

              <span>{{ (text || []).join(',') }}</span>
            </a-popover>
          </template>

          <template v-else-if="column.dataIndex === 'labels'">
            <a-popover :title="`${$t('pages.docker.container.ef67c3db')}`">
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
            <a-popover :title="`${$t('pages.docker.container.e65fe05e')}`">
              <template #content>
                <template v-if="record.mounts">
                  <div v-for="(item, index) in record.mounts" :key="index">
                    <p>
                      {{ $t('pages.docker.container.c0d9b398') }}{{ item.name }}
                      <a-tag>{{
                        item.rw ? $t('pages.docker.container.71579f61') : $t('pages.docker.container.f840d349')
                      }}</a-tag>
                    </p>
                    <p>{{ $t('pages.docker.container.6b431921') }}</p>
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
                {{ $t('pages.docker.container.64ea7bda') }}
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
                      {{ $t('pages.docker.container.1976c068') }}
                      <p v-if="record.networkSettings.networks.bridge.ipAddress">
                        IP:
                        <a-tag>{{ record.networkSettings.networks.bridge.ipAddress }}</a-tag>
                      </p>
                      <p v-if="record.networkSettings.networks.bridge.macAddress">
                        MAC:
                        <a-tag>{{ record.networkSettings.networks.bridge.macAddress }}</a-tag>
                      </p>
                      <p v-if="record.networkSettings.networks.bridge.gateway">
                        {{ $t('pages.docker.container.e689dbaa') }}:
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
                        {{ $t('pages.docker.container.e689dbaa') }}:
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
            <a-tooltip :title="(record.status || '') + $t('pages.docker.container.854a0b5d')" @click="viewLog(record)">
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
                <a-tooltip :title="$t('pages.docker.container.e49f7a16')">
                  <a-button
                    size="small"
                    type="link"
                    :disabled="record.state !== 'running'"
                    @click="handleTerminal(record)"
                    ><CodeOutlined
                  /></a-button>
                </a-tooltip>
                <a-tooltip :title="$t('pages.docker.container.d9418498')">
                  <a-button size="small" type="link" @click="doAction(record, 'stop')"><StopOutlined /></a-button>
                </a-tooltip>
                <a-tooltip :title="$t('pages.docker.container.158e24d2')">
                  <a-button size="small" type="link" @click="doAction(record, 'restart')"><ReloadOutlined /></a-button>
                </a-tooltip>
              </template>
              <template v-else>
                <a-tooltip :title="$t('pages.docker.container.43cf4fd2')">
                  <a-button size="small" type="link" @click="doAction(record, 'start')">
                    <PlayCircleOutlined />
                  </a-button>
                </a-tooltip>
                <a-tooltip :title="$t('pages.docker.container.d9418498')">
                  <a-button size="small" type="link" :disabled="true"><StopOutlined /></a-button>
                </a-tooltip>
                <a-tooltip :title="$t('pages.docker.container.158e24d2')">
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
                      <a-tooltip :title="$t('pages.docker.container.9bce7fda')">
                        <a-button size="small" type="link" @click="rebuild(record)"
                          ><RedoOutlined />{{ $t('pages.docker.container.37578386') }}</a-button
                        >
                      </a-tooltip>
                    </a-menu-item>
                    <a-menu-item>
                      <a-tooltip :title="$t('pages.docker.container.a1ba9678')">
                        <a-button
                          size="small"
                          type="link"
                          :disabled="record.state !== 'running'"
                          @click="editContainer(record)"
                        >
                          <EditOutlined />{{ $t('pages.docker.container.e1224c34') }}
                        </a-button>
                      </a-tooltip>
                    </a-menu-item>
                    <a-menu-item>
                      <a-tooltip :title="$t('pages.docker.container.854a0b5d')">
                        <a-button size="small" type="link" @click="viewLog(record)"
                          ><MessageOutlined />{{ $t('pages.docker.container.90985472') }}</a-button
                        >
                      </a-tooltip>
                    </a-menu-item>
                    <a-menu-item>
                      <a-tooltip :title="$t('pages.docker.container.30c29')">
                        <a-button size="small" type="link" @click="doAction(record, 'remove')">
                          <DeleteOutlined />{{ $t('pages.docker.container.2f14e7d4') }}
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
              :placeholder="$t('pages.docker.container.3e34ec28')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['containerId']"
              :placeholder="$t('pages.docker.container.cfda881')"
              class="search-input-item"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['imageId']"
              :placeholder="$t('pages.docker.container.e2840598')"
              class="search-input-item"
              @keyup.enter="loadData"
            />
            <div>
              {{ $t('pages.docker.container.9eea39a0') }}
              <a-switch
                v-model:checked="listQuery['showAll']"
                :checked-children="$t('pages.docker.container.9c048a42')"
                :un-checked-children="$t('pages.docker.container.dd568fba')"
              />
            </div>
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.docker.container.a1f640f4')
            }}</a-button>
            <a-statistic-countdown
              format="s"
              :title="$t('pages.docker.container.c48b0e64') + ' '"
              :value="countdownTime"
              @finish="autoUpdate"
            >
              <template #suffix>
                <div style="font-size: 12px">{{ $t('pages.docker.container.dda10f33') }}</div>
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
                  <a-popover :title="`${$t('pages.docker.container.cddbe6bd')}${(text || []).join(',')}`">
                    <template #content>
                      <p>{{ $t('pages.docker.container.da9678ab') }}{{ record.id }}</p>
                      <p>{{ $t('pages.docker.container.198b1e02') }}{{ record.image }}</p>
                      <p>{{ $t('pages.docker.container.93bac13d') }}{{ record.imageId }}</p>
                    </template>

                    <span>{{ (text || []).join(',') }}</span>
                  </a-popover>
                </template>

                <template v-else-if="column.dataIndex === 'labels'">
                  <a-popover :title="`${$t('pages.docker.container.ef67c3db')}`">
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
                  <a-popover :title="`${$t('pages.docker.container.e65fe05e')}`">
                    <template #content>
                      <template v-if="record.mounts">
                        <div v-for="(item, idx) in record.mounts" :key="idx">
                          <p>
                            {{ $t('pages.docker.container.c0d9b398') }}{{ item.name }}
                            <a-tag>{{
                              item.rw ? $t('pages.docker.container.71579f61') : $t('pages.docker.container.f840d349')
                            }}</a-tag>
                          </p>
                          <p>
                            {{
                              $t('pages.docker.container.6b431921', {
                                source: item.source,
                                destination: item.destination
                              })
                            }}
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
                      {{ $t('pages.docker.container.64ea7bda') }}
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
                            {{ $t('pages.docker.container.1976c068') }}
                            <p v-if="record.networkSettings.networks.bridge.ipAddress">
                              IP:
                              <a-tag>{{ record.networkSettings.networks.bridge.ipAddress }}</a-tag>
                            </p>
                            <p v-if="record.networkSettings.networks.bridge.macAddress">
                              MAC:
                              <a-tag>{{ record.networkSettings.networks.bridge.macAddress }}</a-tag>
                            </p>
                            <p v-if="record.networkSettings.networks.bridge.gateway">
                              {{ $t('pages.docker.container.e689dbaa') }}:
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
                              {{ $t('pages.docker.container.e689dbaa') }}:
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
                  <a-tooltip
                    :title="(record.status || '') + $t('pages.docker.container.854a0b5d')"
                    @click="viewLog(record)"
                  >
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
                      <a-tooltip :title="$t('pages.docker.container.e49f7a16')">
                        <a-button size="small" type="link" @click="handleTerminal(record)"><CodeOutlined /></a-button>
                      </a-tooltip>
                      <a-tooltip :title="$t('pages.docker.container.d9418498')">
                        <a-button size="small" type="link" @click="doAction(record, 'stop')"><StopOutlined /></a-button>
                      </a-tooltip>
                      <a-tooltip :title="$t('pages.docker.container.158e24d2')">
                        <a-button size="small" type="link" @click="doAction(record, 'restart')">
                          <ReloadOutlined />
                        </a-button>
                      </a-tooltip>
                    </template>
                    <template v-else>
                      <a-tooltip :title="$t('pages.docker.container.43cf4fd2')">
                        <a-button size="small" type="link" @click="doAction(record, 'start')">
                          <PlayCircleOutlined />
                        </a-button>
                      </a-tooltip>
                      <a-tooltip :title="$t('pages.docker.container.d9418498')">
                        <a-button size="small" type="link" :disabled="true"><StopOutlined /></a-button>
                      </a-tooltip>
                      <a-tooltip :title="$t('pages.docker.container.158e24d2')">
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
                            <a-tooltip :title="$t('pages.docker.container.9bce7fda')">
                              <a-button size="small" type="link" @click="rebuild(record)"
                                ><RedoOutlined />{{ $t('pages.docker.container.37578386') }}</a-button
                              >
                            </a-tooltip>
                          </a-menu-item>
                          <a-menu-item>
                            <a-tooltip :title="$t('pages.docker.container.a1ba9678')">
                              <a-button
                                size="small"
                                type="link"
                                :disabled="record.state !== 'running'"
                                @click="editContainer(record)"
                              >
                                <EditOutlined />{{ $t('pages.docker.container.e1224c34') }}
                              </a-button>
                            </a-tooltip>
                          </a-menu-item>
                          <a-menu-item>
                            <a-tooltip :title="$t('pages.docker.container.854a0b5d')">
                              <a-button size="small" type="link" @click="viewLog(record)">
                                <MessageOutlined />{{ $t('pages.docker.container.90985472') }}
                              </a-button>
                            </a-tooltip>
                          </a-menu-item>
                          <a-menu-item>
                            <a-tooltip :title="$t('pages.docker.container.30c29')">
                              <a-button size="small" type="link" @click="doAction(record, 'remove')">
                                <DeleteOutlined />{{ $t('pages.docker.container.2f14e7d4') }}
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
          <template #description>{{ $t('pages.docker.container.53e901cf') }}</template>
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
    <a-modal
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
    </a-modal>
    <!-- 编辑容器配置 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60vw"
      :title="$t('pages.docker.container.6cfb3f78')"
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
    </a-modal>
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
          title: this.$t('pages.docker.container.72cebb96'),
          width: '60px',
          // ellipsis: true,
          align: 'center',
          customRender: ({ index }) => `${index + 1}`
        },
        {
          title: this.$t('pages.docker.container.3e34ec28'),
          dataIndex: 'names',
          ellipsis: true
          // width: 150
        },
        {
          title: this.$t('pages.docker.container.5306295'),
          dataIndex: 'id',
          ellipsis: true,
          width: '10px',
          showid: true
        },
        {
          title: this.$t('pages.docker.container.77c97b2c'),
          dataIndex: 'imageId',
          ellipsis: true,
          width: '130px',
          showid: true
        },
        {
          title: this.$t('pages.docker.container.9c32c887'),
          dataIndex: 'state',
          // ellipsis: true,
          align: 'center',
          width: '80px'
        },

        {
          title: this.$t('pages.docker.container.a6c4bfd7'),
          dataIndex: 'ports',
          ellipsis: true,
          width: '100px'
        },

        {
          title: this.$t('pages.docker.container.83d5a14e'),
          dataIndex: 'labels',
          ellipsis: true,
          width: '50px'
        },
        {
          title: this.$t('pages.docker.container.e65fe05e'),
          dataIndex: 'mounts',
          ellipsis: true,
          width: '50px'
        },
        {
          title: this.$t('pages.docker.container.e9f092b5'),
          dataIndex: 'command',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('pages.docker.container.f5b90169'),
          dataIndex: 'created',
          ellipsis: true,
          sorter: (a, b) => Number(a.created) - new Number(b.created),
          sortDirections: ['descend', 'ascend'],
          defaultSortOrder: 'descend',
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.docker.container.3bb962bf'),
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
          msg: this.$t('pages.docker.container.987c2cd6'),
          api: dockerContainerRemove
        },
        stop: {
          msg: this.$t('pages.docker.container.2e391eba'),
          api: dockerContainerStop
        },
        restart: {
          msg: this.$t('pages.docker.container.fb372d35'),
          api: dockerContainerRestart
        },
        start: {
          msg: this.$t('pages.docker.container.965876ac'),
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
    $tl(key, ...args) {
      return this.$t(`pages.docker.container.${key}`, ...args)
    },
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
        title: this.$t('pages.docker.container.b22d55a0'),
        zIndex: 1009,
        content: action.msg,
        okText: this.$t('pages.docker.container.e8e9db25'),
        cancelText: this.$t('pages.docker.container.b12468e9'),
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
