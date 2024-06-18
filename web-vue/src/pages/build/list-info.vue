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
      :empty-description="$t('i18n_1c2e9d0c76')"
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
            :placeholder="$t('i18n_50a299c847')"
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
            :placeholder="$t('i18n_3fea7ca76c')"
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
            :placeholder="$t('i18n_f98994f7ec')"
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
            :placeholder="$t('i18n_829abe5a8d')"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-input
            v-model:value="listQuery['%resultDirFile%']"
            allow-clear
            class="search-input-item"
            :placeholder="$t('i18n_c972010694')"
            @press-enter="loadData"
          />
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('i18n_66ab5e9f24') }}</a-button>
          <template v-if="tableSelections && tableSelections.length">
            <a-dropdown>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="1" @click="batchBuild"> {{ $t('i18n_67e3d3e09c') }} </a-menu-item>
                  <a-menu-item key="2" @click="batchCancel"> {{ $t('i18n_32112950da') }} </a-menu-item>
                  <a-menu-item key="3" @click="handleBatchDelete">
                    {{ $t('i18n_7fb62b3011') }}
                  </a-menu-item>
                </a-menu>
              </template>
              <a-button type="primary"> {{ $t('i18n_7f7c624a84') }}<DownOutlined /> </a-button>
            </a-dropdown>
          </template>
          <a-tooltip v-else :title="$t('i18n_98357846a2')">
            <a-button :disabled="true" type="primary"> {{ $t('i18n_7f7c624a84') }} <DownOutlined /> </a-button>
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
                  :title="`${$t('i18n_e703c7367c')}${statusMap[item.status]} ${
                    item.statusMsg ? $t('i18n_8d13037eb7') + item.statusMsg : ''
                  }`"
                >
                  <a-tag :color="statusColor[item.status]" style="margin-right: 0">
                    {{ statusMap[item.status] || $t('i18n_903b25f64e') }}</a-tag
                  >
                </a-tooltip>
              </a-col>
            </a-row>
          </template>

          <a-tooltip>
            <template #title>
              <div v-if="item.branchTagName">
                <div>{{ $t('i18n_8086beecb3') }}{{ item.branchTagName }}</div>
                <div>{{ $t('i18n_ca774ec5b4') }}{{ item.repositoryLastCommitId }}</div>
              </div>
              <div v-else>
                <div>{{ $t('i18n_f240f9d69c') }}{{ item.branchName }}</div>
                <div>{{ $t('i18n_ca774ec5b4') }}{{ item.repositoryLastCommitId }}</div>
              </div>
            </template>

            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">{{ $t('i18n_88ab27cfd0') }}</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                {{ item.branchName }} {{ item.branchTagName }}</a-col
              >
            </a-row>
          </a-tooltip>
          <a-tooltip :title="item.resultDirFile">
            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">{{ $t('i18n_cc637e17a0') }}</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                {{ item.resultDirFile }}
              </a-col>
            </a-row>
          </a-tooltip>

          <a-row class="item-info">
            <a-col :span="6" class="title text-overflow-hidden">{{ $t('i18n_b5d0091ae3') }}:</a-col>
            <a-col :span="18" class="content text-overflow-hidden">
              <a-tag v-if="item.buildId <= 0">-</a-tag>
              <a-tag v-else color="#108ee9" @click="handleBuildLog(item)">#{{ item.buildId }}</a-tag>
            </a-col>
          </a-row>

          <a-row class="item-info">
            <a-col :span="6" class="title text-overflow-hidden">{{ $t('i18n_c530a094f9') }}</a-col>
            <a-col :span="18" class="content text-overflow-hidden">
              <template v-if="item.buildMode === 1">
                <CloudOutlined />
                {{ $t('i18n_685e5de706') }}
              </template>
              <template v-else>
                <CodeOutlined />
                {{ $t('i18n_69c3b873c1') }}
              </template>
            </a-col>
          </a-row>
          <a-row class="item-info">
            <a-col :span="6" class="title text-overflow-hidden">{{ $t('i18n_65894da683') }}</a-col>
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
                >{{ $t('i18n_095e938e2a') }}
              </a-button>
              <a-dropdown v-else>
                <a-button size="small" type="primary" @click="handleConfirmStartBuild(item)">
                  {{ $t('i18n_fcba60e773') }}
                  <DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="1">
                      <a-button
                        size="small"
                        type="primary"
                        @click="reqStartBuild({ id: item.id, buildEnvParameter: item.buildEnvParameter }, true)"
                        >{{ $t('i18n_16b5e7b472') }}
                      </a-button>
                    </a-menu-item>
                    <a-menu-item key="2">
                      <a-button
                        size="small"
                        type="primary"
                        @click="reqStartBuild({ id: item.id, buildEnvParameter: item.buildEnvParameter }, false)"
                      >
                        {{ $t('i18n_f1fdaffdf0') }}
                      </a-button>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-dropdown>
                <a-button size="small" type="primary" @click="handleEdit(item)">{{ $t('i18n_95b351c862') }}</a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleEdit(item, 0)">
                      <a href="javascript:;">{{ $t('i18n_17a74824de') }}</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 1)">
                      <a href="javascript:;">{{ $t('i18n_6ea1fe6baa') }}</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 2)">
                      <a href="javascript:;">{{ $t('i18n_a2ae15f8a7') }}</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 3)">
                      <a href="javascript:;">{{ $t('i18n_3c91490844') }}</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 4)">
                      <a href="javascript:;">{{ $t('i18n_9ab433e930') }}</a>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-button size="small" @click="handleDelete(item)">{{ $t('i18n_2f4aaddde3') }}</a-button>
              <a-tooltip placement="leftBottom" :title="$t('i18n_19675b9d36')">
                <a-button size="small" :disabled="!item.sourceDirExist" @click="handleClear(item)"
                  >{{ $t('i18n_c37ac7f024') }}
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
            :title="`${$t('i18n_d7ec2d3fea')}${text} ${$t('i18n_84632d372f')}`"
            @click="handleDetails(record)"
          >
            <a-button type="link" style="padding: 0" size="small"> <FullscreenOutlined />{{ text }}</a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'branchName'">
          <a-tooltip placement="topLeft">
            <template #title>
              <div v-if="record.branchTagName">
                <div>{{ $t('i18n_8086beecb3') }}{{ record.branchTagName }}</div>
                <div>{{ $t('i18n_ca774ec5b4') }}{{ record.repositoryLastCommitId }}</div>
              </div>
              <div v-else>
                <div>{{ $t('i18n_f240f9d69c') }}{{ text }}</div>
                <div>{{ $t('i18n_ca774ec5b4') }}{{ record.repositoryLastCommitId }}</div>
              </div>
            </template>
            <span v-if="record.branchTagName"><TagOutlined />{{ record.branchTagName }}</span>
            <span v-else>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'buildMode'">
          <a-tooltip placement="topLeft" :title="text === 1 ? $t('i18n_685e5de706') : $t('i18n_69c3b873c1')">
            <CloudOutlined v-if="text === 1" />
            <CodeOutlined v-else />
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'releaseMethod'">
          <a-tooltip>
            <template #title>
              <ul>
                <li>{{ $t('i18n_65894da683') }}{{ releaseMethodMap[text] }}</li>
                <li>{{ $t('i18n_113576ce91') }}{{ record.resultDirFile }}</li>
                <li v-if="record.buildMode !== 1">{{ $t('i18n_1160ab56fd') }}{{ record.script }}</li>
              </ul>
            </template>
            <span>{{ releaseMethodMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="record.statusMsg || statusMap[text] || $t('i18n_1622dc9b6b')">
            <a-tag
              :color="statusColor[record.status]"
              :title="record.statusMsg || statusMap[text] || $t('i18n_1622dc9b6b')"
              >{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</a-tag
            >
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'buildId'">
          <a-tooltip placement="topLeft" :title="text + ` ( ${$t('i18n_aac62bc255')} ) `">
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
              >{{ $t('i18n_095e938e2a') }}
            </a-button>
            <a-dropdown v-else>
              <a-button size="small" type="primary" @click="handleConfirmStartBuild(record)"
                >{{ $t('i18n_fcba60e773') }}<DownOutlined
              /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="1">
                    <a-button
                      size="small"
                      type="primary"
                      @click="reqStartBuild({ id: record.id, buildEnvParameter: record.buildEnvParameter }, true)"
                      >{{ $t('i18n_16b5e7b472') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item key="2">
                    <a-button
                      size="small"
                      type="primary"
                      @click="reqStartBuild({ id: record.id, buildEnvParameter: record.buildEnvParameter }, false)"
                      >{{ $t('i18n_f1fdaffdf0') }}</a-button
                    >
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-dropdown>
              <a-button size="small" type="primary" @click="handleEdit(record, 1)">{{
                $t('i18n_95b351c862')
              }}</a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleEdit(record, 0)">
                    <a href="javascript:;">{{ $t('i18n_17a74824de') }}</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 1)">
                    <a href="javascript:;">{{ $t('i18n_6ea1fe6baa') }}</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 2)">
                    <a href="javascript:;">{{ $t('i18n_a2ae15f8a7') }}</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 3)">
                    <a href="javascript:;">{{ $t('i18n_3c91490844') }}</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 4)">
                    <a href="javascript:;">{{ $t('i18n_9ab433e930') }}</a>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-dropdown>
              <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
                {{ $t('i18n_0ec9eaf9c3') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="copyItem(record)">{{
                      $t('i18n_79d3abe929')
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      :disabled="!record.resultHasFile"
                      type="primary"
                      @click="handleDownloadFile(record)"
                      >{{ $t('i18n_635391aa5d') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                      $t('i18n_2f4aaddde3')
                    }}</a-button>
                  </a-menu-item>

                  <a-menu-item>
                    <a-tooltip placement="leftBottom" :title="$t('i18n_19675b9d36')">
                      <a-button
                        size="small"
                        type="primary"
                        danger
                        :disabled="!record.sourceDirExist"
                        @click="handleClear(record)"
                        >{{ $t('i18n_c37ac7f024') }}
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
                      >{{ $t('i18n_3d43ff1199') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'up')"
                      >{{ $t('i18n_315eacd193') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total"
                      @click="sortItemHander(record, index, 'down')"
                    >
                      {{ $t('i18n_17acd250da') }}
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
      :title="$t('i18n_0a1d18283e')"
      :mask-closable="false"
      @ok="handleStartBuild"
    >
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$t('i18n_d7ec2d3fea')" name="name">
          <a-input v-model:value="temp.name" read-only disabled />
        </a-form-item>
        <a-form-item :label="$t('i18n_bfc04cfda7')" name="branchName">
          <custom-select
            v-model:value="temp.branchName"
            :data="branchList"
            :disabled="temp.branchTagName ? true : false"
            :can-reload="true"
            :input-placeholder="$t('i18n_c618659cea')"
            :select-placeholder="$t('i18n_121e76bb63')"
            @on-refresh-select="loadBranchListById(temp.repositoryId)"
          >
            <template #inputTips>
              <div>
                {{ $t('i18n_89f5ca6928') }}(AntPathMatcher)
                <ul>
                  <li>? {{ $t('i18n_9973159a4d') }}</li>
                  <li>* {{ $t('i18n_32f882ae24') }}</li>
                  <li>** {{ $t('i18n_45b88fc569') }}</li>
                </ul>
              </div>
            </template>
          </custom-select>
        </a-form-item>
        <a-form-item
          v-if="(branchTagList && branchTagList.length) || (temp.branchTagName && temp.branchTagName.length)"
          :label="$t('i18n_977bfe8508')"
          name="branchTagName"
        >
          <custom-select
            v-model:value="temp.branchTagName"
            :data="branchTagList"
            :can-reload="true"
            :input-placeholder="$t('i18n_30e6f71a18')"
            :select-placeholder="$t('i18n_2d58b0e650')"
            @on-refresh-select="loadBranchListById(temp.repositoryId)"
          >
            <template #inputTips>
              <div>
                {{ $t('i18n_89f5ca6928') }}(AntPathMatcher)
                <ul>
                  <li>? {{ $t('i18n_9973159a4d') }}</li>
                  <li>* {{ $t('i18n_32f882ae24') }}</li>
                  <li>** {{ $t('i18n_45b88fc569') }}</li>
                </ul>
              </div>
            </template>
          </custom-select>
        </a-form-item>
        <a-form-item name="resultDirFile" :label="$t('i18n_c972010694')">
          <a-input v-model:value="temp.resultDirFile" :placeholder="$t('i18n_2bef5b58ab')" />
        </a-form-item>
        <a-form-item name="checkRepositoryDiff" :label="$t('i18n_0b23d2f584')" help="">
          <a-space>
            <a-switch
              v-model:checked="temp.checkRepositoryDiff"
              :checked-children="$t('i18n_0a60ac8f02')"
              :un-checked-children="$t('i18n_c9744f45e7')"
            />
            <span>
              <a-tooltip>
                <template #title> {{ $t('i18n_4cbc5505c7') }} </template>
                <QuestionCircleOutlined />
              </a-tooltip>
              {{ $t('i18n_1d263b7efb') }}
            </span>
          </a-space>
        </a-form-item>

        <a-form-item
          v-if="temp.releaseMethod === 1 || temp.releaseMethod === 2"
          name="projectSecondaryDirectory"
          :label="$t('i18n_871cc8602a')"
        >
          <a-input v-model:value="temp.projectSecondaryDirectory" :placeholder="$t('i18n_9c99e8bec9')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_3867e350eb')" name="buildEnvParameter" :help="$t('i18n_220650a1f5')">
          <a-textarea
            v-model:value="temp.buildEnvParameter"
            :placeholder="$t('i18n_b3913b9bb7')"
            :auto-size="{ minRows: 3, maxRows: 5 }"
          />
        </a-form-item>
        <a-form-item :label="$t('i18n_d1498d9dbf')" name="buildRemark" :help="$t('i18n_111e786daa')">
          <a-textarea
            v-model:value="temp.buildRemark"
            :max-length="240"
            :placeholder="$t('i18n_7777a83497')"
            :auto-size="{ minRows: 2, maxRows: 5 }"
          />
        </a-form-item>
        <a-form-item
          v-if="dispatchProjectList && dispatchProjectList.length"
          name="selectProject"
          :label="$t('i18n_c4e0c6b6fe')"
          :help="$t('i18n_25be899f66')"
        >
          <a-select
            v-model:value="temp.dispatchSelectProjectArray"
            mode="multiple"
            :placeholder="$t('i18n_b29fd18c93')"
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
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          sorter: true,
          width: 200,
          ellipsis: true
        },
        {
          title: this.$t('i18n_829abe5a8d'),
          dataIndex: 'group',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_f4bbbaf882'),
          dataIndex: 'branchName',
          ellipsis: true,
          width: 100
        },

        {
          title: this.$t('i18n_7220e4d5f9'),
          dataIndex: 'buildMode',
          align: 'center',
          width: '80px',
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: 'status',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_b5d0091ae3'),
          dataIndex: 'buildId',
          width: '90px',
          ellipsis: true,
          align: 'center'
        },

        {
          title: this.$t('i18n_f98994f7ec'),
          dataIndex: 'releaseMethod',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_7dfcab648d'),
          dataIndex: 'resultDirFile',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_db9296212a'),
          dataIndex: 'autoBuildCron',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          sorter: true
        },

        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '160px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '160px'
        },
        {
          title: this.$t('i18n_c35c1a1330'),
          dataIndex: 'sortValue',
          sorter: true,
          width: '80px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
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
      temp.name = temp.name + this.$t('i18n_0428b36ab1')
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_48281fd3f0'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
          message: this.$t('i18n_5d817c403e')
        })
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_79076b6882'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_e15f22df2d'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        // TODO 后续抽优化
        content: this.$t('i18n_25f6a95de3') + record.name + this.$t('i18n_c16ab7c424'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
        top: this.$t('i18n_0079d91f95'),
        up: this.$t('i18n_b166a66d67'),
        down: this.$t('i18n_7a7e25e9eb')
      }
      let msg = msgData[method] || this.$t('i18n_49574eee58')
      if (!record.sortValue) {
        msg += this.$t('i18n_461e675921')
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.list[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: msg,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
          message: this.$t('i18n_5d817c403e')
        })
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_9341881037'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
          message: this.$t('i18n_5d817c403e')
        })
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_2d3fd578ce'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
          message: this.$t('i18n_2b4cf3d74e')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return this.tableSelections.indexOf(item.id) > -1
      })
      if (!selectData.length) {
        $notification.warning({
          message: this.$t('i18n_2b4cf3d74e')
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
