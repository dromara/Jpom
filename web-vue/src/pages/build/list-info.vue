<template>
  <div>
    <!-- 表格 -->
    <!-- <a-card :body-style="{ padding: '10px' }"> -->

    <!-- <template v-if="layoutType === 'card'">
      <template v-if="list && list.length">
        <a-row :gutter="[16, 16]">
          <a-col v-for="item in list" :key="item.id" :span="6"> </a-col>
        </a-row>
      </template>
      <template v-else>
        <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" description="没有任何构建" />
      </template>
    </template> -->
    <!-- <template v-else-if="layoutType === 'table'"> -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      table-name="buildList"
      :empty-description="$t('pages.build.list-info.623e07ea')"
      :active-page="activePage"
      :layout="layout"
      size="middle"
      :columns="columns"
      :data-source="list"
      bordered
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
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            allow-clear
            class="search-input-item"
            :placeholder="$t('pages.build.list-info.80ebff14')"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.status"
            show-search
            allow-clear
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
            :placeholder="$t('pages.build.list-info.6e06fe4f')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.releaseMethod"
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
            :placeholder="$t('pages.build.list-info.c5607db9')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in releaseMethodMap" :key="key">{{ val }}</a-select-option>
          </a-select>
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
            :placeholder="$t('pages.build.list-info.b1765e98')"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-input
            v-model:value="listQuery['%resultDirFile%']"
            allow-clear
            class="search-input-item"
            :placeholder="$t('pages.build.list-info.63121c12')"
            @press-enter="loadData"
          />
          <a-tooltip :title="$t('pages.build.list-info.6ebd5885')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.build.list-info.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('pages.build.list-info.ebbf5288') }}</a-button>
          <template v-if="tableSelections && tableSelections.length">
            <a-dropdown>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="1" @click="batchBuild"> {{ $t('pages.build.list-info.d9dfcc07') }} </a-menu-item>
                  <a-menu-item key="2" @click="batchCancel"> {{ $t('pages.build.list-info.d91724b3') }} </a-menu-item>
                  <a-menu-item key="3" @click="handleBatchDelete">
                    {{ $t('pages.build.list-info.b5139d46') }}
                  </a-menu-item>
                </a-menu>
              </template>
              <a-button type="primary"> {{ $t('pages.build.list-info.766122c2') }}<DownOutlined /> </a-button>
            </a-dropdown>
          </template>
          <a-tooltip v-else :title="$t('pages.build.list-info.2ed8929')">
            <a-button :disabled="true" type="primary">
              {{ $t('pages.build.list-info.766122c2') }} <DownOutlined />
            </a-button>
          </a-tooltip>

          <!-- <a-button v-if="!layout" type="primary" @click="changeLayout">
            <template #icon>
              <LayoutOutlined v-if="layoutType === 'card'" />
              <TableOutlined v-else />
            </template>
            {{ layoutType === 'card' ? '卡片' : '表格' }}
          </a-button>

          <a-statistic-countdown
            v-if="!choose"
            format=" s 秒"
            title="刷新倒计时"
            :value="countdownTime"
            @finish="silenceLoadData"
          />
          -->
        </a-space>
      </template>
      <template #cardBodyCell="{ item }">
        <a-card :head-style="{ padding: '0 6px' }" :body-style="{ padding: '10px' }">
          <template #title>
            <a-row :gutter="[4, 0]">
              <a-col :span="17" style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                <a-button type="link" style="padding: 0" size="small" @click="handleDetails(item)">
                  <span> {{ item.name }}</span>
                </a-button>
              </a-col>
              <a-col :span="7" style="text-align: right" class="text-overflow-hidden">
                <a-tooltip
                  :title="`${$t('pages.build.list-info.8a8e4923')}${statusMap[item.status]} ${
                    item.statusMsg ? $t('pages.build.list-info.e57df4bc') + item.statusMsg : ''
                  }`"
                >
                  <a-tag :color="statusColor[item.status]" style="margin-right: 0">
                    {{ statusMap[item.status] || $t('pages.build.list-info.3ebb81e0') }}</a-tag
                  >
                </a-tooltip>
              </a-col>
            </a-row>
          </template>

          <a-tooltip>
            <template #title>
              <div v-if="item.branchTagName">
                <div>{{ $t('pages.build.list-info.40d35415') }}{{ item.branchTagName }}</div>
                <div>{{ $t('pages.build.list-info.153a2f31') }}{{ item.repositoryLastCommitId }}</div>
              </div>
              <div v-else>
                <div>{{ $t('pages.build.list-info.c40b3b99') }}{{ item.branchName }}</div>
                <div>{{ $t('pages.build.list-info.153a2f31') }}{{ item.repositoryLastCommitId }}</div>
              </div>
            </template>

            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">{{ $t('pages.build.list-info.990386a2') }}</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                {{ item.branchName }} {{ item.branchTagName }}</a-col
              >
            </a-row>
          </a-tooltip>
          <a-tooltip :title="item.resultDirFile">
            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">{{ $t('pages.build.list-info.b4707ccc') }}</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                {{ item.resultDirFile }}
              </a-col>
            </a-row>
          </a-tooltip>

          <a-row class="item-info">
            <a-col :span="6" class="title text-overflow-hidden">{{ $t('pages.build.list-info.7cf809d7') }}:</a-col>
            <a-col :span="18" class="content text-overflow-hidden">
              <a-tag v-if="item.buildId <= 0">-</a-tag>
              <a-tag v-else color="#108ee9" @click="handleBuildLog(item)">#{{ item.buildId }}</a-tag>
            </a-col>
          </a-row>

          <a-row class="item-info">
            <a-col :span="6" class="title text-overflow-hidden">{{ $t('pages.build.list-info.bc1227cd') }}</a-col>
            <a-col :span="18" class="content text-overflow-hidden">
              <template v-if="item.buildMode === 1">
                <CloudOutlined />
                {{ $t('pages.build.list-info.e1806047') }}
              </template>
              <template v-else>
                <CodeOutlined />
                {{ $t('pages.build.list-info.e227327') }}
              </template>
            </a-col>
          </a-row>
          <a-row class="item-info">
            <a-col :span="6" class="title text-overflow-hidden">{{ $t('pages.build.list-info.907f888f') }}</a-col>
            <a-col :span="18" class="content text-overflow-hidden">
              {{ releaseMethodMap[item.releaseMethod] }}
            </a-col>
          </a-row>

          <a-row type="flex" align="middle" justify="center" style="margin-top: 10px">
            <a-button-group>
              <a-button
                v-if="item.status === 1 || item.status === 4 || item.status === 9"
                size="small"
                type="primary"
                danger
                @click="handleStopBuild(item)"
                >{{ $t('pages.build.list-info.d9418498') }}
              </a-button>
              <a-dropdown v-else>
                <a-button size="small" type="primary" @click="handleConfirmStartBuild(item)">
                  {{ $t('pages.build.list-info.6116e886') }}
                  <DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="1">
                      <a-button
                        size="small"
                        type="primary"
                        @click="reqStartBuild({ id: item.id, buildEnvParameter: item.buildEnvParameter }, true)"
                        >{{ $t('pages.build.list-info.56424415') }}
                      </a-button>
                    </a-menu-item>
                    <a-menu-item key="2">
                      <a-button
                        size="small"
                        type="primary"
                        @click="reqStartBuild({ id: item.id, buildEnvParameter: item.buildEnvParameter }, false)"
                      >
                        {{ $t('pages.build.list-info.d128c3f3') }}
                      </a-button>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-dropdown>
                <a-button size="small" type="primary" @click="handleEdit(item)">{{
                  $t('pages.build.list-info.e1224c34')
                }}</a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleEdit(item, 0)">
                      <a href="javascript:;">{{ $t('pages.build.list-info.b5dfedd9') }}</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 1)">
                      <a href="javascript:;">{{ $t('pages.build.list-info.fbe7a37d') }}</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 2)">
                      <a href="javascript:;">{{ $t('pages.build.list-info.344b9569') }}</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 3)">
                      <a href="javascript:;">{{ $t('pages.build.list-info.7be4f983') }}</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 4)">
                      <a href="javascript:;">{{ $t('pages.build.list-info.17607860') }}</a>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-button size="small" @click="handleDelete(item)">{{ $t('pages.build.list-info.2f14e7d4') }}</a-button>
              <a-tooltip placement="leftBottom" :title="$t('pages.build.list-info.7ed8368b')">
                <a-button size="small" :disabled="!item.sourceDirExist" @click="handleClear(item)"
                  >{{ $t('pages.build.list-info.49cc9441') }}
                </a-button>
              </a-tooltip>
            </a-button-group>
          </a-row>
        </a-card>
      </template>
      <template #tableBodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip
            placement="topLeft"
            :title="`${$t('pages.build.list-info.bb769c1d')}${text} ${$t('pages.build.list-info.ef7334e0')}`"
            @click="handleDetails(record)"
          >
            <a-button type="link" style="padding: 0" size="small"> <FullscreenOutlined />{{ text }}</a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'branchName'">
          <a-tooltip placement="topLeft">
            <template #title>
              <div v-if="record.branchTagName">
                <div>{{ $t('pages.build.list-info.40d35415') }}{{ record.branchTagName }}</div>
                <div>{{ $t('pages.build.list-info.153a2f31') }}{{ record.repositoryLastCommitId }}</div>
              </div>
              <div v-else>
                <div>{{ $t('pages.build.list-info.c40b3b99') }}{{ text }}</div>
                <div>{{ $t('pages.build.list-info.153a2f31') }}{{ record.repositoryLastCommitId }}</div>
              </div>
            </template>
            <span v-if="record.branchTagName"><TagOutlined />{{ record.branchTagName }}</span>
            <span v-else>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'buildMode'">
          <a-tooltip
            placement="topLeft"
            :title="text === 1 ? $t('pages.build.list-info.e1806047') : $t('pages.build.list-info.e227327')"
          >
            <CloudOutlined v-if="text === 1" />
            <CodeOutlined v-else />
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'releaseMethod'">
          <a-tooltip>
            <template #title>
              <ul>
                <li>{{ $t('pages.build.list-info.907f888f') }}{{ releaseMethodMap[text] }}</li>
                <li>{{ $t('pages.build.list-info.27d9deeb') }}{{ record.resultDirFile }}</li>
                <li v-if="record.buildMode !== 1">{{ $t('pages.build.list-info.264f7e9c') }}{{ record.script }}</li>
              </ul>
            </template>
            <span>{{ releaseMethodMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="record.statusMsg || statusMap[text] || $t('pages.build.list-info.5f51a112')">
            <a-tag
              :color="statusColor[record.status]"
              :title="record.statusMsg || statusMap[text] || $t('pages.build.list-info.5f51a112')"
              >{{ statusMap[text] || $t('pages.build.list-info.5f51a112') }}</a-tag
            >
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'buildId'">
          <a-tooltip placement="topLeft" :title="text + ` ( ${$t('pages.build.list-info.b51c8bb3')} ) `">
            <span v-if="record.buildId <= 0"></span>
            <a-tag v-else color="#108ee9" @click="handleBuildLog(record)">#{{ text }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text || '' }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button
              v-if="record.status === 1 || record.status === 4 || record.status === 9"
              size="small"
              type="primary"
              danger
              @click="handleStopBuild(record)"
              >{{ $t('pages.build.list-info.d9418498') }}
            </a-button>
            <a-dropdown v-else>
              <a-button size="small" type="primary" @click="handleConfirmStartBuild(record)"
                >{{ $t('pages.build.list-info.6116e886') }}<DownOutlined
              /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="1">
                    <a-button
                      size="small"
                      type="primary"
                      @click="reqStartBuild({ id: record.id, buildEnvParameter: record.buildEnvParameter }, true)"
                      >{{ $t('pages.build.list-info.56424415') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item key="2">
                    <a-button
                      size="small"
                      type="primary"
                      @click="reqStartBuild({ id: record.id, buildEnvParameter: record.buildEnvParameter }, false)"
                      >{{ $t('pages.build.list-info.d128c3f3') }}</a-button
                    >
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-dropdown>
              <a-button size="small" type="primary" @click="handleEdit(record, 1)">{{
                $t('pages.build.list-info.e1224c34')
              }}</a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleEdit(record, 0)">
                    <a href="javascript:;">{{ $t('pages.build.list-info.b5dfedd9') }}</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 1)">
                    <a href="javascript:;">{{ $t('pages.build.list-info.fbe7a37d') }}</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 2)">
                    <a href="javascript:;">{{ $t('pages.build.list-info.344b9569') }}</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 3)">
                    <a href="javascript:;">{{ $t('pages.build.list-info.7be4f983') }}</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 4)">
                    <a href="javascript:;">{{ $t('pages.build.list-info.17607860') }}</a>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-dropdown>
              <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
                {{ $t('pages.build.list-info.6e071067') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="copyItem(record)">{{
                      $t('pages.build.list-info.a8ef5999')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      :disabled="!record.resultHasFile"
                      type="primary"
                      @click="handleDownloadFile(record)"
                      >{{ $t('pages.build.list-info.44ea38fe') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                      $t('pages.build.list-info.2f14e7d4')
                    }}</a-button>
                  </a-menu-item>

                  <a-menu-item>
                    <a-tooltip placement="leftBottom" :title="$t('pages.build.list-info.7ed8368b')">
                      <a-button
                        size="small"
                        type="primary"
                        danger
                        :disabled="!record.sourceDirExist"
                        @click="handleClear(record)"
                        >{{ $t('pages.build.list-info.49cc9441') }}
                      </a-button>
                    </a-tooltip>
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'top')"
                      >{{ $t('pages.build.list-info.35d93b3e') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'up')"
                      >{{ $t('pages.build.list-info.6e79de86') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total"
                      @click="sortItemHander(record, index, 'down')"
                    >
                      {{ $t('pages.build.list-info.78c0cb41') }}
                    </a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
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
    <!-- </template> -->
    <!-- </a-card> -->

    <!-- 编辑区 -->
    <build-item
      v-if="editBuildVisible != 0"
      :id="temp.id"
      :visible-type="editBuildVisible"
      :edit-steps="editSteps"
      :data="temp"
      @close="
        () => {
          editBuildVisible = 0
        }
      "
      @build="
        (build, buildId, buildEnvParameter) => {
          editBuildVisible = 0
          loadData()
          loadGroupList()
          if (build) {
            reqStartBuild({ id: buildId, buildEnvParameter: buildEnvParameter || temp.buildEnvParameter }, true)
          }
        }
      "
    ></build-item>
    <!-- 构建日志 -->
    <build-log
      v-if="buildLogVisible > 0"
      :temp="temp"
      :visible="buildLogVisible != 0"
      @close="
        () => {
          buildLogVisible = 0
        }
      "
    />
    <!-- 构建确认 -->
    <CustomModal
      v-if="buildConfirmVisible"
      v-model:open="buildConfirmVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="40vw"
      :title="$t('pages.build.list-info.f56a79a2')"
      :mask-closable="false"
      @ok="handleStartBuild"
    >
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$t('pages.build.list-info.bb769c1d')" name="name">
          <a-input v-model:value="temp.name" read-only disabled />
        </a-form-item>
        <a-form-item :label="$t('pages.build.list-info.5cb4b684')" name="branchName">
          <custom-select
            v-model:value="temp.branchName"
            :data="branchList"
            :disabled="temp.branchTagName ? true : false"
            :can-reload="true"
            :input-placeholder="$t('pages.build.list-info.8f70dedd')"
            :select-placeholder="$t('pages.build.list-info.969f2846')"
            @on-refresh-select="loadBranchListById(temp.repositoryId)"
          >
            <template #inputTips>
              <div>
                {{ $t('pages.build.list-info.bb871195') }}(AntPathMatcher)
                <ul>
                  <li>? {{ $t('pages.build.list-info.e0095c49') }}</li>
                  <li>* {{ $t('pages.build.list-info.970e6cdf') }}</li>
                  <li>** {{ $t('pages.build.list-info.e073d65') }}</li>
                </ul>
              </div>
            </template>
          </custom-select>
        </a-form-item>
        <a-form-item
          v-if="(branchTagList && branchTagList.length) || (temp.branchTagName && temp.branchTagName.length)"
          :label="$t('pages.build.list-info.83d5a14e')"
          name="branchTagName"
        >
          <custom-select
            v-model:value="temp.branchTagName"
            :data="branchTagList"
            :can-reload="true"
            :input-placeholder="$t('pages.build.list-info.b328c96c')"
            :select-placeholder="$t('pages.build.list-info.7f712bec')"
            @on-refresh-select="loadBranchListById(temp.repositoryId)"
          >
            <template #inputTips>
              <div>
                {{ $t('pages.build.list-info.bb871195') }}(AntPathMatcher)
                <ul>
                  <li>? {{ $t('pages.build.list-info.e0095c49') }}</li>
                  <li>* {{ $t('pages.build.list-info.970e6cdf') }}</li>
                  <li>** {{ $t('pages.build.list-info.e073d65') }}</li>
                </ul>
              </div>
            </template>
          </custom-select>
        </a-form-item>
        <a-form-item name="resultDirFile" :label="$t('pages.build.list-info.63121c12')">
          <a-input v-model:value="temp.resultDirFile" :placeholder="$t('pages.build.list-info.6f375d88')" />
        </a-form-item>
        <a-form-item name="checkRepositoryDiff" :label="$t('pages.build.list-info.8692041e')" help="">
          <a-space>
            <a-switch
              v-model:checked="temp.checkRepositoryDiff"
              :checked-children="$t('pages.build.list-info.f5bb2364')"
              :un-checked-children="$t('pages.build.list-info.5edb2e8a')"
            />
            <span>
              <a-tooltip>
                <template #title> {{ $t('pages.build.list-info.6f2466a3') }} </template>
                <QuestionCircleOutlined />
              </a-tooltip>
              {{ $t('pages.build.list-info.abad81a4') }}
            </span>
          </a-space>
        </a-form-item>

        <a-form-item
          v-if="temp.releaseMethod === 1 || temp.releaseMethod === 2"
          name="projectSecondaryDirectory"
          :label="$t('pages.build.list-info.597f26e8')"
        >
          <a-input v-model:value="temp.projectSecondaryDirectory" :placeholder="$t('pages.build.list-info.8e2b5cb2')" />
        </a-form-item>
        <a-form-item
          :label="$t('pages.build.list-info.798dbcb4')"
          name="buildEnvParameter"
          :help="$t('pages.build.list-info.c23ac61d')"
        >
          <a-textarea
            v-model:value="temp.buildEnvParameter"
            :placeholder="$t('pages.build.list-info.6cc91892')"
            :auto-size="{ minRows: 3, maxRows: 5 }"
          />
        </a-form-item>
        <a-form-item
          :label="$t('pages.build.list-info.11757b06')"
          name="buildRemark"
          :help="$t('pages.build.list-info.6a69192c')"
        >
          <a-textarea
            v-model:value="temp.buildRemark"
            :max-length="240"
            :placeholder="$t('pages.build.list-info.48ed56a')"
            :auto-size="{ minRows: 2, maxRows: 5 }"
          />
        </a-form-item>
        <a-form-item
          v-if="dispatchProjectList && dispatchProjectList.length"
          name="selectProject"
          :label="$t('pages.build.list-info.ec12e21e')"
          :help="$t('pages.build.list-info.1bdcc00f')"
        >
          <a-select
            v-model:value="temp.dispatchSelectProjectArray"
            mode="multiple"
            :placeholder="$t('pages.build.list-info.3d29208d')"
          >
            <a-select-option
              v-for="item in dispatchProjectList"
              :key="item.id"
              :value="`${item.projectId}@${item.nodeId}`"
            >
              {{ item.nodeName }}-{{ item.cacheProjectName || item.projectId }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </CustomModal>
  </div>
</template>
<script>
import BuildLog from './log'
import BuildItem from './item'
import CustomSelect from '@/components/customSelect'
import { Empty } from 'ant-design-vue'
import {
  clearBuid,
  deleteBuild,
  getBuildGroupAll,
  getBuildList,
  releaseMethodMap,
  downloadBuildFileByBuild,
  startBuild,
  statusMap,
  statusColor,
  stopBuild,
  sortItem,
  getBranchList,
  deleteatchBuild
} from '@/api/build-info'
import { getDispatchProject } from '@/api/dispatch'

import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime
  // PAGE_DEFAULT_SHOW_TOTAL,
  // getCachePageLimit
} from '@/utils/const'

export default {
  components: {
    BuildLog,
    BuildItem,
    CustomSelect
  },
  props: {
    repositoryId: {
      type: String,
      default: ''
    },
    fullContent: {
      type: Boolean,
      default: true
    },
    choose: {
      // "radio"
      type: String,
      default: ''
    },
    layout: {
      type: String,
      default: ''
    }
  },
  emits: ['cancel', 'confirm'],
  data() {
    return {
      Empty,
      sizeOptions: ['8', '12', '16', '20', '24'],
      releaseMethodMap,
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      // 动态列表参数
      groupList: [],
      list: [],
      statusMap,
      statusColor,
      branchTagList: [],
      branchList: [],
      temp: {},
      // 页面控制变量
      editBuildVisible: 0,
      editSteps: null,
      buildLogVisible: 0,
      buildConfirmVisible: false,
      columns: [
        {
          title: this.$t('pages.build.list-info.bb769c1d'),
          dataIndex: 'name',
          sorter: true,
          width: 200,
          ellipsis: true
        },
        {
          title: this.$t('pages.build.list-info.b1765e98'),
          dataIndex: 'group',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.build.list-info.a36b3cc8'),
          dataIndex: 'branchName',
          ellipsis: true,
          width: 100
        },

        {
          title: this.$t('pages.build.list-info.b96b97fb'),
          dataIndex: 'buildMode',
          align: 'center',
          width: '80px',
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('pages.build.list-info.6e06fe4f'),
          dataIndex: 'status',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('pages.build.list-info.7cf809d7'),
          dataIndex: 'buildId',
          width: '90px',
          ellipsis: true,
          align: 'center'
        },

        {
          title: this.$t('pages.build.list-info.c5607db9'),
          dataIndex: 'releaseMethod',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('pages.build.list-info.72370b9a'),
          dataIndex: 'resultDirFile',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.build.list-info.313ac3d0'),
          dataIndex: 'autoBuildCron',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.build.list-info.916db24b'),
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          sorter: true
        },

        {
          title: this.$t('pages.build.list-info.f5b90169'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '160px'
        },
        {
          title: this.$t('pages.build.list-info.a2b40316'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '160px'
        },
        {
          title: this.$t('pages.build.list-info.f5049383'),
          dataIndex: 'sortValue',
          sorter: true,
          width: '80px'
        },
        {
          title: this.$t('pages.build.list-info.3bb962bf'),
          dataIndex: 'operation',
          width: '200px',

          align: 'center',
          fixed: 'right'
        }
      ],

      // countdownTime: Date.now(),
      // refreshInterval: 5,
      tableSelections: [],
      dispatchProjectList: [],
      // layoutType: null,
      confirmLoading: false
    }
  },
  computed: {
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
        selectedRowKeys: this.tableSelections,
        type: this.choose || 'checkbox'
      }
    }
  },
  watch: {},
  created() {
    // if (this.layout) {
    // this.layoutType = this.layout
    this.loadData()
    // } else {
    // this.changeLayout()
    // }
    this.loadGroupList()
    //
    // this.countdownTime = Date.now() + this.refreshInterval * 1000
  },
  methods: {
    CHANGE_PAGE,

    // PAGE_DEFAULT_SHOW_TOTAL,
    // getCachePageLimit,
    // 分组数据
    loadGroupList() {
      getBuildGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.listQuery.repositoryId = this.repositoryId
      this.loading = true
      getBuildList(this.listQuery)
        .then((res) => {
          if (res.code === 200) {
            this.list = res.data.result
            this.listQuery.total = res.data.total
            // 重新计算倒计时
            // this.countdownTime = Date.now() + this.refreshInterval * 1000
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    // silenceLoadData() {
    //   if (this.$attrs.routerUrl !== this.$route.path) {
    //     // 重新计算倒计时
    //     this.countdownTime = Date.now() + this.refreshInterval * 1000
    //     return
    //   }
    //   this.loading = true
    //   getBuildList(this.listQuery, false)
    //     .then((res) => {
    //       if (res.code === 200) {
    //         this.list = res.data.result
    //         this.listQuery.total = res.data.total
    //         // 重新计算倒计时
    //         this.countdownTime = Date.now() + this.refreshInterval * 1000
    //       }
    //     })
    //     .finally(() => {
    //       this.loading = false
    //     })
    // },

    // 新增
    handleAdd() {
      this.temp = {}
      this.editBuildVisible = 2
      this.editSteps = 0
    },
    // 复制
    copyItem(record) {
      const temp = Object.assign({}, record)
      delete temp.id
      delete temp.triggerToken
      temp.name = temp.name + this.$t('pages.build.list-info.307f45c9')
      this.temp = temp
      this.editBuildVisible = 2
      this.editSteps = 1
      // this.handleEdit(temp, 1)
    },
    handleEdit(record, steps) {
      this.temp = { id: record.id, buildEnvParameter: record.buildEnvParameter }
      this.editBuildVisible = 2

      this.editSteps = steps
    },
    handleDetails(record) {
      this.editBuildVisible = 1
      this.editSteps = 2
      this.temp = { id: record.id, buildEnvParameter: record.buildEnvParameter }
    },
    loadBranchListById(id) {
      this.branchList = []
      this.branchTagList = []
      const params = {
        repositoryId: id
      }
      getBranchList(params).then((res) => {
        if (res.code === 200) {
          this.branchList = res.data?.branch || []
          this.branchTagList = res.data?.tags || []
        }
      })
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('pages.build.list-info.cb4b5c9c'),
        zIndex: 1009,
        content: this.$t('pages.build.list-info.2c8deda8'),
        okText: this.$t('pages.build.list-info.7da4a591'),
        cancelText: this.$t('pages.build.list-info.43105e21'),
        onOk: () => {
          // 删除
          return deleteBuild(record.id).then((res) => {
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
    // 批量删除
    handleBatchDelete() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warning({
          message: this.$t('pages.build.list-info.a1dd3707')
        })
        return
      }
      $confirm({
        title: this.$t('pages.build.list-info.cb4b5c9c'),
        zIndex: 1009,
        content: this.$t('pages.build.list-info.cba18cfc'),
        okText: this.$t('pages.build.list-info.7da4a591'),
        cancelText: this.$t('pages.build.list-info.43105e21'),
        onOk: () => {
          // 删除
          return deleteatchBuild({ ids: this.tableSelections.join(',') }).then((res) => {
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
    // 清除构建
    handleClear(record) {
      $confirm({
        title: this.$t('pages.build.list-info.cb4b5c9c'),
        zIndex: 1009,
        content: this.$t('pages.build.list-info.38efbe58'),
        okText: this.$t('pages.build.list-info.7da4a591'),
        cancelText: this.$t('pages.build.list-info.43105e21'),
        onOk: () => {
          return clearBuid(record.id).then((res) => {
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
    // 开始构建
    handleConfirmStartBuild(record) {
      this.dispatchProjectList = []
      // 判断构建方式
      if (record.releaseMethod === 1) {
        // 节点分发
        getDispatchProject(record.releaseMethodDataId, true).then((res) => {
          if (res.code === 200) {
            this.dispatchProjectList = res.data?.projectList || []

            this.showBuildConfirm(record)
          }
        })
      } else {
        this.showBuildConfirm(record)
      }
      // console.log(record);
    },
    showBuildConfirm(record) {
      this.temp = Object.assign({}, record)
      this.buildConfirmVisible = true
      this.branchList = []
      this.branchTagList = []
      //
      try {
        const extraData = JSON.parse(record.extraData) || {}
        this.temp = {
          ...this.temp,
          checkRepositoryDiff: extraData.checkRepositoryDiff,
          projectSecondaryDirectory: extraData.projectSecondaryDirectory
        }
      } catch (e) {
        //
      }
    },
    handleStartBuild() {
      this.confirmLoading = true
      this.reqStartBuild(
        {
          id: this.temp.id,
          buildRemark: this.temp.buildRemark,
          resultDirFile: this.temp.resultDirFile,
          branchTagName: this.temp.branchTagName,
          branchName: this.temp.branchName,
          checkRepositoryDiff: this.temp.checkRepositoryDiff,
          projectSecondaryDirectory: this.temp.projectSecondaryDirectory,
          buildEnvParameter: this.temp.buildEnvParameter,
          dispatchSelectProject:
            (this.temp.dispatchSelectProjectArray && this.temp.dispatchSelectProjectArray.join(',')) || ''
        },
        true
      )
        .then(() => {
          this.buildConfirmVisible = false
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    reqStartBuild(data, openLog) {
      return new Promise((resolve) => {
        startBuild(data).then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.loadData()
            if (openLog) {
              // 自动打开构建日志
              this.handleBuildLog({
                id: data.id,
                buildId: res.data
              })
            }
            resolve()
          }
        })
      })
    },
    // 停止构建
    handleStopBuild(record) {
      $confirm({
        title: this.$t('pages.build.list-info.cb4b5c9c'),
        zIndex: 1009,
        // TODO 后续抽优化
        content: this.$t('pages.build.list-info.a37e5410') + record.name + this.$t('pages.build.list-info.4dfd12e9'),
        okText: this.$t('pages.build.list-info.7da4a591'),
        cancelText: this.$t('pages.build.list-info.43105e21'),
        onOk: () => {
          this.temp = Object.assign({}, record)
          return stopBuild(this.temp.id).then((res) => {
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
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.id,
        buildId: record.buildId
      }
      this.buildLogVisible = new Date() * Math.random()
    },
    // 关闭日志对话框
    closeBuildLogModel() {
      this.loadData()
    },

    // 排序
    sortItemHander(record, index, method) {
      const msgData = {
        top: this.$t('pages.build.list-info.959fa602'),
        up: this.$t('pages.build.list-info.aca73ac1'),
        down: this.$t('pages.build.list-info.9ea36aca')
      }
      let msg = msgData[method] || this.$t('pages.build.list-info.644ef343')
      if (!record.sortValue) {
        msg += this.$t('pages.build.list-info.fd3f32c7')
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.list[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      $confirm({
        title: this.$t('pages.build.list-info.cb4b5c9c'),
        zIndex: 1009,
        content: msg,
        okText: this.$t('pages.build.list-info.7da4a591'),
        cancelText: this.$t('pages.build.list-info.43105e21'),
        onOk: () => {
          return sortItem({
            id: record.id,
            method: method,
            compareId: compareId
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
    // 下载构建产物
    handleDownloadFile(record) {
      window.open(downloadBuildFileByBuild(record.id, record.buildId), '_blank')
    },
    // 批量构建
    batchBuild() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warning({
          message: this.$t('pages.build.list-info.a1dd3707')
        })
        return
      }
      $confirm({
        title: this.$t('pages.build.list-info.cb4b5c9c'),
        zIndex: 1009,
        content: this.$t('pages.build.list-info.b31a6402'),
        okText: this.$t('pages.build.list-info.7da4a591'),
        cancelText: this.$t('pages.build.list-info.43105e21'),
        onOk: () => {
          this.tableSelections.forEach((item) => {
            startBuild({
              id: item
            }).then((res) => {
              if (res.code === 200) {
                //
              }
            })
          })
          this.tableSelections = []
          this.loadData()
        }
      })
    },
    // 批量取消构建
    batchCancel() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warning({
          message: this.$t('pages.build.list-info.a1dd3707')
        })
        return
      }
      $confirm({
        title: this.$t('pages.build.list-info.cb4b5c9c'),
        zIndex: 1009,
        content: this.$t('pages.build.list-info.44523544'),
        okText: this.$t('pages.build.list-info.7da4a591'),
        cancelText: this.$t('pages.build.list-info.43105e21'),
        onOk: () => {
          this.tableSelections.forEach((item) => {
            stopBuild(item).then((res) => {
              if (res.code === 200) {
                //
              }
            })
          })
          this.tableSelections = []
          this.loadData()
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
    // 选择确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$t('pages.build.list-info.afb1ec30')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return this.tableSelections.indexOf(item.id) > -1
      })
      if (!selectData.length) {
        $notification.warning({
          message: this.$t('pages.build.list-info.afb1ec30')
        })
        return
      }
      this.$emit('confirm', selectData)
    }
  }
}
</script>
<style scoped>
.item-info {
  padding: 4px 0;
}
/* :deep(.ant-statistic div) {
  display: inline-block;
  font-weight: normal;
}
:deep(.ant-statistic-content-value, .ant-statistic-content) {
  font-size: 16px;
} */
</style>
