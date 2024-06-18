<template>
  <div>
    <!-- 表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="repository-list"
      :empty-description="$t('i18n_e07cbb381c')"
      size="middle"
      :columns="columns"
      :data-source="list"
      bordered
      row-key="id"
      :row-selection="choose ? rowSelection : null"
      :pagination="pagination"
      :scroll="{
        x: 'max-content'
      }"
      @change="
        (pagination, filters, sorter) => {
          listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
          loadData()
        }
      "
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            class="search-input-item"
            :placeholder="$t('i18n_f967131d9d')"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%gitUrl%']"
            class="search-input-item"
            :placeholder="$t('i18n_e4bea943de')"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.repoType"
            allow-clear
            :placeholder="$t('i18n_4ce606413e')"
            class="search-input-item"
          >
            <a-select-option :value="'0'">GIT</a-select-option>
            <a-select-option :value="'1'">SVN</a-select-option>
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

          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('i18n_66ab5e9f24') }}</a-button>
          <a-tooltip>
            <template #title> {{ $t('i18n_77c262950c') }} </template>
            <a-button type="primary" @click="handleAddGitee"
              ><QuestionCircleOutlined />{{ $t('i18n_e354969500') }}</a-button
            >
          </a-tooltip>
        </a-space>
      </template>
      <template #toolPrefix>
        <a-button type="primary" size="small" @click="handlerExportData"
          ><DownloadOutlined />{{ $t('i18n_55405ea6ff') }}</a-button
        >
        <a-dropdown>
          <template #overlay>
            <a-menu>
              <a-menu-item key="1">
                <a-button type="primary" size="small" @click="handlerImportTemplate()">{{
                  $t('i18n_2e505d23f7')
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
            <a-button type="primary" size="small"
              ><UploadOutlined /> {{ $t('i18n_8d9a071ee2') }} <DownOutlined />
            </a-button>
          </a-upload>
        </a-dropdown>
      </template>
      <template #tableBodyCell="{ column, text, record, index }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'repoType'">
          <span v-if="text === 0">GIT</span>
          <span v-else-if="text === 1">SVN</span>
          <span v-else>{{ $t('i18n_1622dc9b6b') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'protocol'">
          <span v-if="text === 0">HTTP(S)</span>
          <span v-else-if="text === 1">SSH</span>
          <!-- if no protocol value, get a default value from gitUrl -->
          <span v-else>{{ record.gitUrl.indexOf('http') > -1 ? 'HTTP(S)' : 'SSH' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <a-tag v-if="text === 'GLOBAL'">{{ $t('i18n_2be75b1044') }}</a-tag>
          <a-tag v-else>{{ $t('i18n_98d69f8b62') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            <a-button v-if="global" type="primary" size="small" @click="viewBuild(record)">{{
              $t('i18n_1c3cf7f5f0')
            }}</a-button>
            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</a-button>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $t('i18n_0ec9eaf9c3') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
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
    </CustomTable>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_ed39deafd8')"
      :mask-closable="false"
      width="60%"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$t('i18n_f967131d9d')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_f967131d9d')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_829abe5a8d')" name="group">
          <custom-select
            v-model:value="temp.group"
            :data="groupList"
            :input-placeholder="$t('i18n_bd0362bed3')"
            :select-placeholder="$t('i18n_9cac799f2f')"
          >
          </custom-select>
        </a-form-item>
        <a-form-item :label="$t('i18n_e4bea943de')" name="gitUrl">
          <a-input-group compact>
            <a-form-item-rest>
              <a-select
                v-model:value="temp.repoType"
                style="width: 20%"
                name="repoType"
                :placeholder="$t('i18n_4ce606413e')"
              >
                <a-select-option :value="0">GIT</a-select-option>
                <a-select-option :value="1">SVN</a-select-option>
              </a-select>
            </a-form-item-rest>
            <a-input
              v-model:value="temp.gitUrl"
              style="width: 80%"
              :max-length="250"
              :placeholder="$t('i18n_e4bea943de')"
            />
          </a-input-group>
        </a-form-item>
        <a-form-item :label="$t('i18n_faa1ad5e5c')" name="protocol">
          <a-radio-group v-model:value="temp.protocol" name="protocol">
            <a-radio :value="0">HTTP(S)</a-radio>
            <a-radio :value="1">SSH</a-radio>
          </a-radio-group>
        </a-form-item>
        <!-- HTTP(S) protocol use password -->
        <template v-if="temp.protocol === 0">
          <a-form-item name="userName">
            <template #label>
              <a-tooltip>
                {{ $t('i18n_7035c62fb0') }}
                <template #title>
                  {{ $t('i18n_f0a1428f65') }}<b>$ref.wEnv.xxxx</b> xxxx {{ $t('i18n_c1b72e7ded') }}</template
                >
                <QuestionCircleOutlined v-if="!temp.id" />
              </a-tooltip>
            </template>

            <custom-input
              :input="temp.userName"
              :env-list="envVarList"
              type="text"
              :placeholder="`${$t('i18n_fc4e2c6151')}`"
              @change="
                (v) => {
                  temp = { ...temp, userName: v }
                }
              "
            >
            </custom-input>
          </a-form-item>
          <a-form-item name="password">
            <template #label>
              <a-tooltip>
                {{ $t('i18n_a810520460') }}
                <template #title>
                  {{ $t('i18n_63dd96a28a') }}<b>$ref.wEnv.xxxx</b> xxxx {{ $t('i18n_c1b72e7ded') }}</template
                >
                <QuestionCircleOutlined v-if="!temp.id" />
              </a-tooltip>
            </template>

            <custom-input
              :input="temp.password"
              :env-list="envVarList"
              :placeholder="`${!temp.id ? $t('i18n_2646b813e8') : $t('i18n_b90a30dd20')}`"
              @change="
                (v) => {
                  temp = { ...temp, password: v }
                }
              "
            >
            </custom-input>
            <template #help>
              <a-tooltip v-if="temp.id" :title="$t('i18n_b408105d69')">
                <a-button style="margin: 5px" size="small" type="primary" danger @click="restHideField(temp)">{{
                  $t('i18n_4403fca0c0')
                }}</a-button>
              </a-tooltip>
            </template>
          </a-form-item>
        </template>
        <a-form-item v-if="temp.repoType === 1 && temp.protocol === 1" :label="$t('i18n_7035c62fb0')" name="userName">
          <a-input v-model:value="temp.userName" :placeholder="$t('i18n_f04a289502')">
            <template #prefix>
              <UserOutlined />
            </template>
            <template #suffix>
              <a-tooltip v-if="temp.id" :title="$t('i18n_b408105d69')">
                <a-button size="small" type="primary" danger @click="restHideField(temp)">{{
                  $t('i18n_4403fca0c0')
                }}</a-button>
              </a-tooltip>
            </template>
          </a-input>
        </a-form-item>
        <!-- SSH protocol use rsa private key -->
        <template v-if="temp.protocol === 1">
          <a-form-item name="password">
            <template #label>
              <a-tooltip>
                {{ $t('i18n_a810520460') }}
                <template #title>
                  {{ $t('i18n_63dd96a28a') }}<b>$ref.wEnv.xxxx</b> xxxx {{ $t('i18n_c1b72e7ded') }}</template
                >
                <QuestionCircleOutlined v-if="!temp.id" />
              </a-tooltip>
            </template>
            <custom-input
              :input="temp.password"
              :env-list="envVarList"
              :placeholder="`${$t('i18n_45028ad61d')}`"
              @change="
                (v) => {
                  temp = { ...temp, password: v }
                }
              "
            >
            </custom-input>
          </a-form-item>
          <a-form-item :label="$t('i18n_d0eddb45e2')" name="rsaPrv">
            <a-tooltip placement="topLeft">
              <template #title>
                <div>
                  <p style="color: #faa">
                    {{ $t('i18n_43c61e76e7') }} "{{ $t('i18n_3bc5e602b2') }}" {{ $t('i18n_9e560a4162') }} <br />{{
                      $t('i18n_8c66392870')
                    }}
                    "{{ $t('i18n_3bc5e602b2') }}" {{ $t('i18n_d0a864909b') }}<br />
                  </p>
                  <p>{{ $t('i18n_8fb7785809') }}</p>
                  <p>{{ $t('i18n_0af04cdc22') }}</p>
                  <p>
                    1. {{ $t('i18n_f5d0b69533') }}: <br />-----BEGIN RSA PRIVATE KEY-----
                    <br />
                    ..... <br />
                    -----END RSA PRIVATE KEY-----
                  </p>
                  <p>
                    2. {{ $t('i18n_becc848a54') }}: {{ $t('i18n_4c9bb42608') }}) {{ $t('i18n_bcc4f9e5ca') }}:
                    <br />file:/Users/Hotstrip/.ssh/id_rsa
                  </p>
                </div>
              </template>
              <a-textarea
                v-model:value="temp.rsaPrv"
                :auto-size="{ minRows: 3, maxRows: 3 }"
                :placeholder="$t('i18n_d7ee59f327')"
              ></a-textarea>
            </a-tooltip>
          </a-form-item>
          <!-- 公钥暂时没用到 -->
          <a-form-item v-if="false" :label="$t('i18n_b939d47e23')" name="rsaPub">
            <a-textarea
              v-model:value="temp.rsaPub"
              :auto-size="{ minRows: 3, maxRows: 3 }"
              :placeholder="$t('i18n_db686f0328')"
            ></a-textarea>
          </a-form-item>
        </template>
        <a-form-item v-if="workspaceId !== 'GLOBAL'" :label="$t('i18n_fffd3ce745')" name="global">
          <a-radio-group v-model:value="temp.global">
            <a-radio :value="true"> {{ $t('i18n_2be75b1044') }}</a-radio>
            <a-radio :value="false"> {{ $t('i18n_691b11e443') }}</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item :label="$t('i18n_67425c29a5')" name="timeout">
          <a-input-number
            v-model:value="temp.timeout"
            :min="0"
            :placeholder="$t('i18n_ea9f824647')"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </CustomModal>
    <CustomModal
      v-if="giteeImportVisible"
      v-model:open="giteeImportVisible"
      destroy-on-close
      :title="$t('i18n_c8633b4b77')"
      width="80%"
      :footer="null"
      :mask-closable="false"
    >
      <a-form
        ref="giteeImportForm"
        :label-col="{ span: 4 }"
        :rules="giteeImportFormRules"
        :model="giteeImportForm"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item name="token" :label="$t('i18n_8ba971a184')" :help="$t('i18n_e30a93415b')">
          <a-form-item-rest>
            <a-tooltip
              :title="`${giteeImportForm.type} ${$t('i18n_32d0576d85')}${importTypePlaceholder[giteeImportForm.type]}`"
            >
              <a-input-group compact>
                <a-select v-model:value="giteeImportForm.type" style="width: 10%" @change="importChange">
                  <a-select-option v-for="item in Object.keys(providerData)" :key="item" :value="item">
                    {{ item }}</a-select-option
                  >
                </a-select>

                <a-input-search
                  v-model:value="giteeImportForm.token"
                  style="width: 90%; margin-top: 1px"
                  enter-button
                  :loading="importLoading"
                  :placeholder="importTypePlaceholder[giteeImportForm.type]"
                  @search="handleGiteeImportFormOk"
                />
              </a-input-group>
            </a-tooltip>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item name="address" :label="$t('i18n_7650487a87')">
          <a-input v-model:value="giteeImportForm.address" :placeholder="$t('i18n_9412eb8f99')" />
        </a-form-item>
        <a-form-item
          v-if="providerData[giteeImportForm.type]?.query"
          name="condition"
          :label="$t('i18n_e5f71fc31e')"
          :help="$t('i18n_bf0e1e0c16', { slot1: $t('i18n_e5f71fc31e') })"
        >
          <a-input v-model:value="giteeImportForm.condition" :placeholder="$t('i18n_e72f2b8806')" />
        </a-form-item>
      </a-form>
      <a-table
        :loading="importLoading"
        size="middle"
        :columns="reposColumns"
        :data-source="repos"
        bordered
        row-key="full_name"
        :pagination="reposPagination"
        @change="reposChange"
      >
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'private'">
            <a-switch size="small" :disabled="true" :checked="record.private" />
          </template>
          <template v-else-if="column.dataIndex === 'name'">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'full_name'">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'url'">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'description'">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'operation'">
            <a-button type="primary" size="small" :disabled="record.exists" @click="handleGiteeRepoAdd(record)">{{
              record.exists ? $t('i18n_cb951984f2') : $t('i18n_66ab5e9f24')
            }}</a-button>
          </template>
        </template>
      </a-table>
    </CustomModal>
    <!-- 选择仓库确认区域 -->
    <!-- <div style="padding-top: 50px" v-if="this.choose">
      <div
        :style="{
          position: 'absolute',
          right: 0,
          bottom: 0,
          width: '100%',
          borderTop: '1px solid #e9e9e9',
          padding: '10px 16px',
          background: '#fff',
          textAlign: 'right',
          zIndex: 1
        }"
      >
        <a-space>
          <a-button
            @click="
              () => {
                this.$emit('cancel')
              }
            "
          >
            取消
          </a-button>
          <a-button type="primary" @click="handerConfirm"> 确定 </a-button>
        </a-space>
      </div> -->
    <!-- </div> -->
    <!-- 关联构建 -->
    <CustomModal
      v-if="viewBuildVisible"
      v-model:open="viewBuildVisible"
      destroy-on-close
      width="80vw"
      :title="$t('i18n_1c13276448')"
      :mask-closable="false"
      :footer="null"
    >
      <buildList-component v-if="viewBuildVisible" :repository-id="temp.id" :full-content="false" />
      <a-spin v-else>loading....</a-spin>
    </CustomModal>
  </div>
</template>
<script>
import CustomInput from '@/components/customInput'
import {
  providerInfo,
  authorizeRepos,
  deleteRepository,
  editRepository,
  getRepositoryList,
  restHideField,
  sortItem,
  exportData,
  importTemplate,
  importData,
  listRepositoryGroup
} from '@/api/repository'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { getWorkspaceEnvAll } from '@/api/workspace'
import CustomSelect from '@/components/customSelect'
export default {
  components: {
    CustomInput,
    CustomSelect,
    buildListComponent: defineAsyncComponent(() => import('@/pages/build/list-info'))
  },
  props: {
    choose: {
      type: Boolean,
      default: false
    },
    workspaceId: {
      type: String,
      default: ''
    },
    global: {
      type: Boolean,
      default: false
    },
    chooseVal: {
      type: String,
      default: ''
    }
  },
  emits: ['cancel', 'confirm'],
  data() {
    return {
      loading: false,
      PAGE_DEFAULT_SIZW_OPTIONS: ['15', '20', '25', '30', '35', '40', '50'],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      groupList: [],
      providerData: {
        gitee: {
          baseUrl: 'https://gitee.com',
          name: 'gitee',
          query: true
        }
      },
      total: 0,
      temp: {},
      isSystem: false,
      editVisible: false,
      giteeImportVisible: false,
      repos: [],
      username: null,

      columns: [
        {
          title: this.$t('i18n_f967131d9d'),
          dataIndex: 'name',
          width: 200,
          sorter: true,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_b37b786351'),
          dataIndex: 'group',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: this.$t('i18n_e4bea943de'),
          dataIndex: 'gitUrl',
          width: 300,
          sorter: true,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_4ce606413e'),
          dataIndex: 'repoType',
          width: 100,
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('i18n_faa1ad5e5c'),
          dataIndex: 'protocol',
          width: 100,
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('i18n_fffd3ce745'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('i18n_95a43eaa59'),
          dataIndex: 'createUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
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
          fixed: 'right',
          align: 'center',
          width: this.global ? '240px' : '180px'
        }
      ],

      reposColumns: [
        {
          title: this.$t('i18n_f967131d9d'),
          dataIndex: 'name',
          ellipsis: true
        },
        {
          title: this.$t('i18n_42b6bd1b2f'),
          dataIndex: 'full_name',
          ellipsis: true
        },
        {
          title: 'GitUrl',
          dataIndex: 'url',
          ellipsis: true
        },

        {
          title: this.$t('i18n_3bdd08adab'),
          dataIndex: 'description',

          ellipsis: true
        },
        {
          title: this.$t('i18n_3dc5185d81'),
          dataIndex: 'private',
          width: 80,
          ellipsis: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          width: 100,

          align: 'left'
        }
      ],

      giteeImportForm: Object.assign({}, PAGE_DEFAULT_LIST_QUERY, {
        limit: 15,
        type: 'gitee',
        address: 'https://gitee.com'
      }),
      giteeImportFormRules: {
        token: [{ required: true, message: this.$t('i18n_76530bff27'), trigger: 'blur' }]
        // address: [{ required: true, message: "请填写平台地址", trigger: "blur" }],
      },
      rules: {
        name: [{ required: true, message: this.$t('i18n_9f0de3800b'), trigger: 'blur' }],
        gitUrl: [{ required: true, message: this.$t('i18n_0cf81d77bb'), trigger: 'blur' }]
      },
      importTypePlaceholder: {
        gitee: this.$t('i18n_233fb56ab2'),
        github: this.$t('i18n_4b1835640f'),
        gitlab_v3: this.$t('i18n_5bd1d267a9'),
        gitlab: this.$t('i18n_5bd1d267a9'),
        gitea: this.$t('i18n_cd1aedc667'),
        gogs: this.$t('i18n_cd1aedc667'),
        other: this.$t('i18n_76530bff27')
      },
      tableSelections: [],
      envVarList: [],

      viewBuildVisible: false,
      confirmLoading: false,
      importLoading: false
    }
  },
  computed: {
    // 分页
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    reposPagination() {
      return COMPUTED_PAGINATION(this.giteeImportForm, this.PAGE_DEFAULT_SIZW_OPTIONS)
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
        type: 'radio'
      }
    }
  },
  watch: {},
  created() {
    this.loadData()
    //
    providerInfo().then((response) => {
      if (response.code === 200) {
        this.providerData = response.data
      }
    })
    this.getWorkEnvList()
    this.loadGroupList()

    if (this.chooseVal) {
      this.tableSelections = [this.chooseVal]
    }
  },
  methods: {
    CHANGE_PAGE,
    // 分组数据
    loadGroupList() {
      listRepositoryGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    getWorkEnvList() {
      getWorkspaceEnvAll({
        workspaceId: this.workspaceId + (this.global ? ',GLOBAL' : '')
      }).then((res) => {
        if (res.code === 200) {
          this.envVarList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      if (this.workspaceId) {
        this.listQuery = { ...this.listQuery, workspaceId: this.workspaceId }
      }
      getRepositoryList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    importChange(value) {
      this.giteeImportForm.address = this.providerData[value].baseUrl
    },
    // // 筛选
    // handleFilter() {
    //   this.loadData();
    // },
    // 新增
    handleAdd() {
      this.temp = {
        repoType: 0,
        protocol: 0
      }
      if (!this.global) {
        this.temp = { ...this.temp, workspaceId: 'GLOBAL', global: true }
      }

      this.editVisible = true
    },
    handleAddGitee() {
      this.giteeImportVisible = true
    },
    // 下载导入模板
    handlerImportTemplate() {
      window.open(importTemplate(), '_blank')
    },
    handlerExportData() {
      window.open(exportData({ ...this.listQuery, workspaceId: this.workspaceId }), '_blank')
    },
    beforeUpload(file) {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('workspaceId', this.workspaceId)
      importData(formData).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    },
    handleGiteeImportFormOk() {
      this.$refs['giteeImportForm'].validate().then(() => {
        this.importLoading = true
        authorizeRepos(this.giteeImportForm)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              //this.username = res.data.username;
              this.giteeImportForm.total = res.data.total
              this.repos = res.data.result
            }
          })
          .finally(() => {
            this.importLoading = false
          })
      })
    },
    reposChange(pagination) {
      this.giteeImportForm.page = pagination.current
      this.giteeImportForm.limit = pagination.pageSize
      this.handleGiteeImportFormOk()
    },
    handleGiteeRepoAdd(record) {
      let data = {
        repoType: 0,
        protocol: 0,
        userName: record.username,
        password: this.giteeImportForm.token,
        name: record.name,
        gitUrl: record.url
      }
      if (!this.global) {
        data = { ...data, workspaceId: 'GLOBAL', global: true }
      }
      editRepository(data).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
          record.exists = true
          this.loadData()
        }
      })
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      if (this.temp.protocol === undefined) {
        this.temp.protocol = this.temp.gitUrl.indexOf('http') > -1 ? 0 : 1
      }
      this.temp = {
        ...this.temp,
        global: record.workspaceId === 'GLOBAL',
        workspaceId: ''
      }
      this.editVisible = true
    },
    // 提交节点数据
    handleEditOk() {
      // 检验表单
      this.$refs['editForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        editRepository(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.editVisible = false
              this.loadData()
              this.$refs['editForm'].resetFields()
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
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_7dfc7448ec'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        zIndex: 1009,
        onOk: () => {
          return deleteRepository({
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

    // 清除隐藏字段
    restHideField(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_664c205cc3'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        zIndex: 1009,
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

    // 排序
    sortItemHander(record, index, method) {
      const msgData = {
        top: this.$t('i18n_0079d91f95'),
        up: this.$t('i18n_b166a66d67'),
        down: this.$t('i18n_7a7e25e9eb')
      }
      let msg = msgData[method] || this.$t('i18n_49574eee58')
      if (!record.sortValue) {
        msg += ` ${this.$t('i18n_57c0a41ec6')},${this.$t('i18n_066f903d75')},${this.$t('i18n_c4e2cd2266')}`
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.list[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: msg,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        zIndex: 1009,
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
              return false
            }
          })
        }
      })
    },
    // 确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$t('i18n_be381ac957')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return item.id === this.tableSelections[0]
      })[0]

      this.$emit('confirm', `${selectData.id}`)
    },
    // 查看关联构建
    viewBuild(data) {
      this.temp = { id: data.id }
      this.viewBuildVisible = true
    }
  }
}
</script>
<style scoped>
/* .filter {
  margin-bottom: 10px;
}

.btn-add {
  margin-left: 10px;
  margin-right: 0;
} */
</style>
