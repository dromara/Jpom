<!--

    Copyright (c) 2019 Of Him Code Technology Studio
    Jpom is licensed under Mulan PSL v2.
    You can use this software according to the terms and conditions of the Mulan PSL v2.
    You may obtain a copy of Mulan PSL v2 at:
    			http://license.coscl.org.cn/MulanPSL2
    THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
    See the Mulan PSL v2 for more details.

-->
<template>
  <div>
    <a-spin :tip="$t('i18n_bebcd7388f')" :spinning="loading">
      <a-card>
        <template #title>
          <a-steps v-model:current="stepsCurrent" size="small" :items="stepsItems" @change="stepsChange"></a-steps>
        </template>
        <a-form ref="editBuildForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
          <div v-show="stepsCurrent === 0">
            <a-alert :message="$t('i18n_514b320d25')" type="info" show-icon>
              <template #description>
                <ul>
                  <li>
                    {{ $t('i18n_85cfcdd88b') }}
                    <ul>
                      <li>{{ $t('i18n_7a5dd04619') }}</li>
                      <li>{{ $t('i18n_a08cbeb238') }}</li>
                      <li>{{ $t('i18n_8813ff5cf8') }}</li>
                    </ul>
                  </li>
                  <li>
                    {{ $t('i18n_92f0744426') }}
                    <ul>
                      <li>{{ $t('i18n_1de9b781bd') }}</li>
                    </ul>
                  </li>

                  <li>{{ $t('i18n_200707a186') }}</li>
                  <li v-if="getExtendPlugins.indexOf('inDocker') > -1" style="color: red">
                    {{ $t('i18n_f5f65044ea') }}
                  </li>
                </ul>
              </template>
            </a-alert>
            <!-- 构建方式是一级选择：节点构建作为第三种方式，展开区里再选具体节点 -->
            <a-form-item :name="['buildMode']">
              <template #label> {{ $t('i18n_17a74824de') }} </template>
              <a-space>
                <a-radio-group v-model:value="temp.buildMode" name="buildMode" @change="changeBuildMode">
                  <a-radio
                    v-for="item in buildModeArray"
                    :key="item.value"
                    :disabled="item.disabled"
                    :value="item.value"
                    >{{ item.name }}</a-radio
                  >
                </a-radio-group>
              </a-space>
            </a-form-item>
            <template v-if="temp.buildMode === 1">
              <a-form-item>
                <template #help>
                  <a-button
                    type="link"
                    @click="
                      () => {
                        dockerListVisible = 1
                      }
                    "
                  >
                    {{ $t('i18n_e06497b0fb') }}
                  </a-button>
                </template>
                <template #label> {{ $t('i18n_b44479d4b8') }} </template>
                <a-spin :tip="$t('i18n_73c980987a')" :spinning="dockerAllTagLoading">
                  <a-space v-if="dockerAllTagList && dockerAllTagList.length">
                    <a-tag v-for="(item, index) in dockerAllTagList" :key="index">{{ item }}</a-tag>
                  </a-space>
                  <span v-else style="color: red; font-weight: bold">{{ $t('i18n_28e0fcdf93') }}</span>
                </a-spin>
              </a-form-item>
              <a-alert :message="$t('i18n_3174d1022d')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_148484b985') }}</li>
                    <li>{{ $t('i18n_21d81c6726') }}</li>
                    <li>
                      {{ $t('i18n_caa9b5cd94') }} <b style="color: red">fromTag</b>
                      {{ $t('i18n_9caecd931b') }}
                    </li>
                  </ul>
                </template>
              </a-alert>
            </template>
            <!-- 节点构建：与容器构建同级的一种构建方式，展开区选择由哪台构建节点执行 -->
            <template v-if="temp.buildMode === 2">
              <!-- build-node-item：给浏览器 E2E 一个稳定的定位钩子（构建方式里也有「容器构建/节点构建」文本，按文本定位会串到单选组） -->
              <a-form-item class="build-node-item" :label="$t('i18n_1eb1c081f0')" name="buildNodeId" required>
                <a-select
                  v-model:value="temp.buildNodeId"
                  show-search
                  allow-clear
                  :loading="buildNodeLoading"
                  :options="buildNodeOptions"
                  :placeholder="$t('i18n_d355cedabf')"
                  style="width: 100%"
                  @change="changeBuildNode"
                />
                <template #help>{{ $t('i18n_df2bfdd78c') }}</template>
              </a-form-item>
              <a-alert
                v-if="buildNodeLoadError"
                :message="$t('i18n_1fea4ba832')"
                type="warning"
                show-icon
                style="margin-bottom: 16px"
              />
              <a-alert
                v-else-if="!buildNodeLoading && buildNodeList.length === 0"
                :message="$t('i18n_a9d91ff2a0')"
                type="info"
                show-icon
                style="margin-bottom: 16px"
              />
              <a-alert
                v-if="buildNodeUnavailable"
                :message="$t('i18n_84890974d0')"
                type="warning"
                show-icon
                style="margin-bottom: 16px"
              />
              <a-alert
                v-else-if="temp.buildNodeId"
                :message="$t('i18n_68a638ab32')"
                type="info"
                show-icon
                style="margin-bottom: 16px"
              />
            </template>
          </div>
          <div v-show="stepsCurrent === 1">
            <a-form-item :label="$t('i18n_d7ec2d3fea')" name="name">
              <a-row>
                <a-col :span="10">
                  <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_d7ec2d3fea')" />
                </a-col>
                <a-col :span="4" style="text-align: right">{{ $t('i18n_1b973fc4d1') }}</a-col>

                <a-col :span="10">
                  <a-form-item-rest>
                    <custom-select
                      v-model:value="temp.group"
                      :max-length="50"
                      :data="groupList"
                      :input-placeholder="$t('i18n_bd0362bed3')"
                      :select-placeholder="$t('i18n_3e8c9c54ee')"
                    >
                      <template #suffix>
                        <a-tooltip>
                          <template #title>
                            <div>
                              {{ $t('i18n_bd7c7abc8c') }}
                            </div>
                          </template>
                          <QuestionCircleOutlined />
                        </a-tooltip>
                      </template>
                    </custom-select>
                  </a-form-item-rest>
                </a-col>
              </a-row>
            </a-form-item>
            <a-form-item :label="$t('i18n_b3ef35a359')" name="repositoryId">
              <a-input-search
                :value="`${
                  tempRepository ? tempRepository.name + '[' + tempRepository.gitUrl + ']' : $t('i18n_ad311f3211')
                }`"
                read-only
                :placeholder="$t('i18n_ad311f3211')"
                :enter-button="$t('i18n_f89fa9b6c6')"
                @search="
                  () => {
                    repositoryisible = true
                  }
                "
              />
            </a-form-item>
            <template v-if="tempRepository && tempRepository.repoType === 0">
              <a-form-item :label="$t('i18n_bfc04cfda7')" name="branchName">
                <a-row>
                  <a-col :span="10">
                    <custom-select
                      v-model:value="temp.branchName"
                      :disabled="temp.branchTagName ? true : false"
                      :data="branchList"
                      :can-reload="true"
                      :input-placeholder="$t('i18n_c618659cea')"
                      :select-placeholder="$t('i18n_c0a9e33e29')"
                      @on-refresh-select="loadBranchList"
                      @change="
                        () => {
                          $refs['editBuildForm'] && $refs['editBuildForm'].clearValidate()
                        }
                      "
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
                  </a-col>
                  <a-col :span="4" style="text-align: right"> {{ $t('i18n_14d342362f') }}(TAG)：</a-col>
                  <a-col :span="10">
                    <a-form-item-rest>
                      <custom-select
                        v-model:value="temp.branchTagName"
                        :data="branchTagList"
                        :can-reload="true"
                        :input-placeholder="$t('i18n_30e6f71a18')"
                        :select-placeholder="$t('i18n_2d58b0e650')"
                        @on-refresh-select="loadBranchList"
                        @change="
                          () => {
                            $refs['editBuildForm'] && $refs['editBuildForm'].clearValidate()
                          }
                        "
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
                      </custom-select></a-form-item-rest
                    >
                  </a-col>
                </a-row>
              </a-form-item>
              <a-form-item
                v-if="getExtendPlugins.indexOf('system-git') > -1"
                :label="$t('i18n_0253279fb8')"
                name="cloneDepth"
              >
                <a-input-number
                  v-model:value="tempExtraData.cloneDepth"
                  style="width: 100%"
                  :placeholder="$t('i18n_a59d075d85')"
                />
              </a-form-item>
            </template>
          </div>

          <!-- 构建流程 -->
          <div v-show="stepsCurrent === 2">
            <!-- 节点构建的节点已在「构建方式」步骤选定：这里只读回显 + 一键回去修改，同一字段不在两处重复出现 -->
            <a-form-item v-if="temp.buildMode === 2" :label="$t('i18n_1eb1c081f0')">
              <a-space :size="8">
                <a-tag color="blue">{{ currentBuildNodeLabel }}</a-tag>
                <a-button type="link" size="small" style="padding: 0" @click="stepsChange(0)">
                  {{ $t('i18n_53e7f5513b') }}
                </a-button>
              </a-space>
            </a-form-item>
            <a-form-item v-if="temp.buildMode !== 1" name="script">
              <template #label>
                <a-tooltip>
                  {{ $t('i18n_c2ee58c247') }}
                  <template #title>
                    {{ $t('i18n_c7689f4c9a') }}<b>{{ $t('i18n_2b607a562a') }} </b>{{ $t('i18n_7e33f94952') }}
                    <b>cd xxx && mvn clean package</b>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <template #help>
                <div>{{ $t('i18n_1ad696efdc') }}</div>
                <div>{{ $t('i18n_872ad6c96e') }}</div>
              </template>

              <!-- <a-textarea
                  v-model:value="temp.script"
                  :auto-size="{ minRows: 2, maxRows: 6 }"
                  allow-clear
                  placeholder="构建执行的命令(非阻塞命令)，如：mvn clean package、npm run build。支持变量：${BUILD_ID}、${BUILD_NAME}、${BUILD_SOURCE_FILE}、${BUILD_NUMBER_ID}、仓库目录下 .env、工作空间变量"
                /> -->
              <a-form-item-rest>
                <code-editor v-model:content="temp.script" :show-tool="true" :options="{ mode: 'shell' }" height="40vh">
                  <template #tool_before>
                    <a-space>
                      <a-button
                        type="link"
                        @click="
                          () => {
                            viewScriptTemplVisible = true
                          }
                        "
                      >
                        {{ $t('i18n_e3ead2bd0d') }}
                      </a-button>
                      <a-button
                        type="link"
                        @click="
                          () => {
                            chooseScriptVisible = 2
                          }
                        "
                      >
                        {{ $t('i18n_36d4046bd6') }}
                      </a-button>
                    </a-space>
                  </template>
                </code-editor>
              </a-form-item-rest>
            </a-form-item>
            <a-form-item v-if="temp.buildMode === 1" name="script">
              <template #label>
                <a-tooltip>
                  DSL {{ $t('i18n_2d711b09bd') }}
                  <template #title>
                    <p>{{ $t('i18n_ba3a679655') }}</p>
                    <ul>
                      <li>{{ $t('i18n_55d4a79358') }}</li>
                      <li>{{ $t('i18n_713c986135') }}</li>
                      <li>{{ $t('i18n_db81a464ba') }}</li>
                    </ul>
                    <div>
                      {{ $t('i18n_590b9ce766') }}
                      <ol>
                        <li>
                          {{ $t('i18n_fdfd501269') }}
                        </li>
                        <li>{{ $t('i18n_0793aa7ba3') }}</li>
                        <li>{{ $t('i18n_0bfcab4978') }}</li>
                        <li>
                          {{ $t('i18n_e5915f5dbb') }}
                        </li>
                        <li>
                          {{ $t('i18n_48fe457960') }}
                        </li>
                      </ol>
                    </div>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <!-- <a-tabs>
                <a-tab-pane key="1" tab="DSL 配置">
                  <a-form-item-rest>
                    <code-editor
                      height="40vh"
                      v-model:content="temp.script"
                      :options="{ mode: 'yaml', tabSize: 2 }"
                    ></code-editor>
                  </a-form-item-rest>
                </a-tab-pane>
                <a-tab-pane key="2" tab="配置示例">
                  <a-form-item-rest>
                    <code-editor
                      height="40vh"
                      v-model:content="dslDefault"
                      :options="{
                        mode: 'yaml',
                        tabSize: 2,

                        readOnly: true
                      }"
                    ></code-editor>
                  </a-form-item-rest>
                </a-tab-pane>
              </a-tabs> -->
              <a-form-item-rest>
                <code-editor
                  v-show="dslEditTabKey === 'content'"
                  v-model:content="temp.script"
                  height="40vh"
                  :show-tool="true"
                  :options="{ mode: 'yaml', tabSize: 2 }"
                  :placeholder="$t('i18n_af14cd6893')"
                >
                  <template #tool_before>
                    <a-segmented
                      v-model:value="dslEditTabKey"
                      :options="[
                        {
                          label: `DSL ${$t('i18n_224e2ccda8')}`,
                          value: 'content'
                        },
                        { label: $t('i18n_da79c2ec32'), value: 'demo' }
                      ]"
                    />
                  </template>
                </code-editor>
                <code-editor
                  v-show="dslEditTabKey === 'demo'"
                  v-model:content="dslDefault"
                  height="40vh"
                  :show-tool="true"
                  :options="{ mode: 'yaml', tabSize: 2, readOnly: true }"
                >
                  <template #tool_before>
                    <a-segmented
                      v-model:value="dslEditTabKey"
                      :options="[
                        {
                          label: `DSL ${$t('i18n_224e2ccda8')}`,
                          value: 'content'
                        },
                        { label: $t('i18n_da79c2ec32'), value: 'demo' }
                      ]"
                    />
                  </template> </code-editor
              ></a-form-item-rest>
            </a-form-item>
            <a-form-item v-if="temp.buildMode !== undefined" name="resultDirFile" class="jpom-target-dir">
              <template #label>
                <a-tooltip>
                  {{ $t('i18n_c972010694') }}
                  <template #title>
                    <div>
                      {{ $t('i18n_84592cd99c') }}
                      <b>mvn clean package</b> {{ $t('i18n_1a55f76ace') }}
                      <b> modules/server/target/server-2.4.2-release</b>
                    </div>
                    <div><br /></div>
                    <!-- 本地构建与节点构建都支持模糊匹配（容器构建不支持） -->
                    <div v-if="temp.buildMode !== 1">
                      {{ $t('i18n_89f5ca6928') }}(AntPathMatcher){{ $t('i18n_35cb4b85a9') }}
                      <ul>
                        <li>? {{ $t('i18n_9973159a4d') }}</li>
                        <li>* {{ $t('i18n_32f882ae24') }}</li>
                        <li>** {{ $t('i18n_45b88fc569') }}</li>
                      </ul>
                    </div>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input v-model:value="temp.resultDirFile" :max-length="200" :placeholder="$t('i18n_2cdbbdabf1')" />
            </a-form-item>

            <a-alert v-if="temp.buildMode === undefined" :message="$t('i18n_46032a715e')" banner />
            <template v-else>
              <a-form-item :label="$t('i18n_3867e350eb')" name="buildEnvParameter">
                <a-textarea
                  v-model:value="temp.buildEnvParameter"
                  :placeholder="$t('i18n_b3913b9bb7')"
                  :auto-size="{ minRows: 3, maxRows: 5 }"
                />
              </a-form-item>
              <a-form-item :label="$t('i18n_0227161b3e')" name="commandExecMode">
                <a-radio-group v-model:value="tempExtraData.commandExecMode" button-style="solid">
                  <a-radio-button value="default">{{ $t('i18n_18c63459a2') }}</a-radio-button>
                  <a-radio-button value="apache_exec">{{ $t('i18n_c9daf4ad6b') }}</a-radio-button>
                </a-radio-group>
                <template #help>{{ $t('i18n_75c63f427a') }}</template>
              </a-form-item>
            </template>
          </div>

          <div v-show="stepsCurrent === 3">
            <a-form-item name="releaseMethod">
              <template #label>
                <a-tooltip>
                  {{ $t('i18n_3c91490844') }}
                  <template #title>
                    <ul>
                      <li>{{ $t('i18n_2c635c80ec') }}</li>
                      <li>{{ $t('i18n_df9d1fedc5') }}</li>
                      <li>{{ $t('i18n_7e9f0d2606') }}</li>
                      <li>
                        {{ $t('i18n_58cbd04f02') }}
                      </li>
                      <li>{{ $t('i18n_cbee7333e4') }}</li>
                      <li>
                        {{ $t('i18n_6f5b238dd2') }}
                      </li>
                      <li>{{ $t('i18n_5fbde027e3') }}</li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-space>
                <a-radio-group v-model:value="temp.releaseMethod" name="releaseMethod">
                  <a-radio v-for="(val, key) in releaseMethodMap" :key="key" :value="parseInt(key)">{{ val }}</a-radio>
                </a-radio-group>
              </a-space>
            </a-form-item>
            <div v-if="!temp.releaseMethod" style="text-align: center">
              {{ $t('i18n_29b48a76be') }}
            </div>
            <template v-else>
              <template v-if="temp.releaseMethod === 0">
                {{ $t('i18n_52ef46c618') }},{{ $t('i18n_657f3883e3') }}
              </template>
              <!-- 节点分发 -->
              <template v-if="temp.releaseMethod === 1">
                <a-form-item :label="$t('i18n_bc8752e529')" name="releaseMethodDataId">
                  <a-select
                    v-model:value="tempExtraData.releaseMethodDataId_1"
                    show-search
                    allow-clear
                    :placeholder="$t('i18n_2560e962cf')"
                  >
                    <a-select-option v-for="dispatch in dispatchList" :key="dispatch.id"
                      >{{ dispatch.name }}
                    </a-select-option>
                    <template #suffixIcon>
                      <ReloadOutlined @click="loadDispatchList" />
                    </template>
                  </a-select>
                </a-form-item>
                <a-form-item name="projectSecondaryDirectory" :label="$t('i18n_871cc8602a')">
                  <a-input
                    v-model:value="tempExtraData.projectSecondaryDirectory"
                    :placeholder="$t('i18n_f9f061773e')"
                  />
                </a-form-item>
              </template>

              <!-- 项目 -->
              <template v-if="temp.releaseMethod === 2">
                <a-form-item :label="$t('i18n_dbba7e107a')" name="releaseMethodDataIdList">
                  <a-cascader
                    v-model:value="temp.releaseMethodDataIdList"
                    :options="cascaderList"
                    :placeholder="$t('i18n_35488f5ba8')"
                  >
                    <template #suffixIcon>
                      <ReloadOutlined @click="loadNodeProjectList" />
                    </template>
                  </a-cascader>
                </a-form-item>
                <a-form-item :label="$t('i18n_89050136f8')" name="afterOpt">
                  <a-select
                    v-model:value="tempExtraData.afterOpt"
                    show-search
                    allow-clear
                    :placeholder="$t('i18n_3322338140')"
                  >
                    <a-select-option v-for="opt in afterOptListSimple" :key="opt.value">{{
                      opt.title
                    }}</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item name="projectSecondaryDirectory" :label="$t('i18n_871cc8602a')">
                  <a-input
                    v-model:value="tempExtraData.projectSecondaryDirectory"
                    :placeholder="$t('i18n_9c99e8bec9')"
                  />
                </a-form-item>
              </template>
              <!-- SSH -->
              <template v-if="temp.releaseMethod === 3">
                <a-form-item name="releaseMethodDataId" :help="$t('i18n_7de5541032')">
                  <template #label>
                    <a-tooltip>
                      {{ $t('i18n_b188393ea7') }}
                      <template #title>
                        {{ $t('i18n_7de5541032') }}
                      </template>
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-row>
                    <a-col :span="22">
                      <a-select
                        v-model:value="tempExtraData.releaseMethodDataId_3"
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
                        mode="multiple"
                        :placeholder="$t('i18n_260a3234f2')"
                      >
                        <a-select-option v-for="ssh in sshList" :key="ssh.id" :disabled="!ssh.fileDirs">
                          <a-tooltip :title="ssh.name"> {{ ssh.name }}</a-tooltip>
                        </a-select-option>
                      </a-select>
                    </a-col>
                    <a-col :span="1" style="margin-left: 10px">
                      <ReloadOutlined @click="loadSshList" />
                    </a-col>
                  </a-row>
                </a-form-item>
                <a-form-item name="releaseMethodDataId" :help="$t('i18n_abb6b7260b')">
                  <template #label>
                    <a-tooltip :title="$t('i18n_abb6b7260b')">
                      {{ $t('i18n_dbb2df00cf') }}
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-input-group compact>
                    <a-select
                      v-model:value="tempExtraData.releaseSshDir"
                      show-search
                      allow-clear
                      style="width: 30%"
                      :placeholder="$t('i18n_260a3234f2')"
                    >
                      <a-select-option v-for="item in selectSshDirs" :key="item">
                        <a-tooltip :title="item">{{ item }}</a-tooltip>
                      </a-select-option>
                    </a-select>
                    <a-form-item-rest>
                      <a-input
                        v-model:value="tempExtraData.releasePath2"
                        style="width: 70%"
                        :placeholder="$t('i18n_a75a5a9525')"
                      />
                    </a-form-item-rest>
                  </a-input-group>
                </a-form-item>
              </template>

              <!-- SSH 项目 -->
              <template v-if="temp.releaseMethod === 7">
                <a-form-item name="releaseMethodDataId" :help="$t('i18n_7bba17e190')">
                  <template #label>
                    <a-tooltip>
                      {{ $t('i18n_8215da30b4') }}
                      <template #title>{{ $t('i18n_7bba17e190') }}</template>
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-row>
                    <a-col :span="22">
                      <a-select
                        v-model:value="tempExtraData.releaseMethodDataId_7"
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
                        :placeholder="$t('i18n_ab38038377')"
                      >
                        <a-select-option v-for="project in sshProjectList" :key="project.id" :value="project.id">
                          {{ project.name }}
                        </a-select-option>
                      </a-select>
                    </a-col>
                    <a-col :span="1" style="margin-left: 10px">
                      <ReloadOutlined @click="loadSshProjectList" />
                    </a-col>
                  </a-row>
                </a-form-item>
                <a-form-item :label="$t('i18n_dbb2df00cf')">
                  <a-alert type="info" show-icon :message="$t('i18n_a2d654831b')">
                    <template #description>
                      <span v-if="selectedSshProject"
                        >{{ selectedSshProject.name }}: {{ selectedSshProject.path }}</span
                      >
                    </template>
                  </a-alert>
                </a-form-item>
              </template>

              <a-form-item v-if="temp.releaseMethod === 3" name="releaseBeforeCommand">
                <!-- sshCommand -->
                <template #label>
                  <a-tooltip>
                    {{ $t('i18n_e44f59f2d9') }}
                    <template #title>
                      {{ $t('i18n_b53dedd3e0') }}
                      <ul>
                        <li>{{ $t('i18n_9be8ff8367') }}</li>
                        <li>{{ $t('i18n_5fbde027e3') }}</li>
                      </ul>
                    </template>
                    <QuestionCircleOutlined v-if="!temp.id" />
                  </a-tooltip>
                </template>
                <template #help>
                  {{ $t('i18n_234e967afe') }}
                </template>
                <!-- <a-textarea
                  v-model:value="tempExtraData.releaseBeforeCommand"
                  allow-clear
                  :auto-size="{ minRows: 2, maxRows: 10 }"
                  :rows="3"
                  placeholder=""
                /> -->
                <a-form-item-rest>
                  <code-editor
                    v-model:content="tempExtraData.releaseBeforeCommand"
                    height="40vh"
                    :show-tool="true"
                    :options="{ mode: 'shell' }"
                  >
                    <template #tool_before>
                      <a-tag>{{ $t('i18n_eb5bab1c31') }}</a-tag></template
                    >
                  </code-editor>
                </a-form-item-rest>
              </a-form-item>
              <a-form-item v-if="temp.releaseMethod === 3 || temp.releaseMethod === 4" name="releaseCommand">
                <!-- sshCommand LocalCommand -->
                <template #label>
                  <a-tooltip>
                    {{ $t('i18n_923f8d2688') }}
                    <template #title>
                      {{ $t('i18n_fb9d826b2f') }}
                      <ul>
                        <li>{{ $t('i18n_9be8ff8367') }}</li>
                        <li>{{ $t('i18n_5fbde027e3') }}</li>
                      </ul>
                    </template>

                    <QuestionCircleOutlined v-if="!temp.id" />
                  </a-tooltip>
                </template>
                <template #help>
                  <div>{{ $t('i18n_9fb12a2d14') }}</div>
                  <div>{{ $t('i18n_872ad6c96e') }}</div>
                </template>
                <a-form-item-rest>
                  <code-editor
                    v-model:content="tempExtraData.releaseCommand"
                    height="40vh"
                    :show-tool="true"
                    :options="{ mode: 'shell' }"
                  >
                    <template #tool_before>
                      <a-tag>{{ $t('i18n_537b39a8b5') }}</a-tag></template
                    >
                  </code-editor>
                </a-form-item-rest>
              </a-form-item>

              <a-form-item
                v-if="temp.releaseMethod === 2 || temp.releaseMethod === 3 || temp.releaseMethod === 7"
                name="clearOld"
              >
                <template #label>
                  <a-tooltip>
                    {{ $t('i18n_2223ff647d') }}
                    <template #title>
                      {{ $t('i18n_b343663a14') }}
                    </template>
                    <QuestionCircleOutlined v-if="!temp.id" />
                  </a-tooltip>
                </template>
                <a-form-item-rest>
                  <a-row>
                    <a-col :span="4">
                      <a-switch
                        v-model:checked="tempExtraData.clearOld"
                        :checked-children="$t('i18n_0a60ac8f02')"
                        :un-checked-children="$t('i18n_c9744f45e7')"
                      />
                    </a-col>
                    <template v-if="temp.releaseMethod === 2">
                      <a-col :span="4" style="text-align: right">
                        <a-tooltip>
                          {{ $t('i18n_702afc34a0') }}
                          <template #title>
                            {{ $t('i18n_762e05a901') }}
                            <ul>
                              <li>
                                {{ $t('i18n_19f974ef6a') }}
                              </li>
                              <li>{{ $t('i18n_7764df7ccc') }}</li>
                            </ul>
                          </template>
                          <QuestionCircleOutlined v-if="!temp.id" />
                        </a-tooltip>
                      </a-col>
                      <a-col :span="4">
                        <a-switch
                          v-model:checked="tempExtraData.diffSync"
                          :checked-children="$t('i18n_0a60ac8f02')"
                          :un-checked-children="$t('i18n_c9744f45e7')"
                        />
                      </a-col>
                      <a-col :span="4" style="text-align: right">
                        <a-tooltip>
                          {{ $t('i18n_7b2cbfada9') }}
                          <template #title>
                            {{ $t('i18n_300fbf3891') }}
                          </template>
                          <QuestionCircleOutlined v-if="!temp.id" />
                        </a-tooltip>
                      </a-col>
                      <a-col :span="4">
                        <a-switch
                          v-model:checked="tempExtraData.projectUploadCloseFirst"
                          :checked-children="$t('i18n_0a60ac8f02')"
                          :un-checked-children="$t('i18n_c9744f45e7')"
                        />
                      </a-col>
                    </template>
                  </a-row>
                </a-form-item-rest>
              </a-form-item>
              <!-- docker -->
              <template v-if="temp.releaseMethod === 5">
                <a-form-item name="fromTag">
                  <template #label>
                    <a-tooltip>
                      {{ $t('i18n_690a3d1a69') }}
                      <template #title>
                        {{ $t('i18n_bd5d9b3e93') }}
                      </template>
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-input v-model:value="tempExtraData.fromTag" :placeholder="$t('i18n_2be2175cd7')" />
                </a-form-item>

                <a-tooltip :title="$t('i18n_61bfa4e925')">
                  <a-form-item name="dockerfile" label="Dockerfile">
                    <a-input v-model:value="tempExtraData.dockerfile" :placeholder="$t('i18n_ab9a0ee5bd')" />
                  </a-form-item>
                </a-tooltip>
                <a-form-item name="dockerTag" :label="$t('i18n_9a77f3523e')">
                  <a-tooltip :title="$t('i18n_fa57a7afad')">
                    <a-input v-model:value="tempExtraData.dockerTag" :placeholder="$t('i18n_250a999bb2')" />
                  </a-tooltip>
                </a-form-item>
                <a-form-item name="dockerBuildArgs" :label="$t('i18n_244d5a0ed8')">
                  <a-row>
                    <a-col :span="10">
                      <a-tooltip :title="$t('i18n_a34545bd16')">
                        <a-input v-model:value="tempExtraData.dockerBuildArgs" :placeholder="$t('i18n_6e70d2fb91')" />
                      </a-tooltip>
                    </a-col>
                    <a-col :span="4" style="text-align: right">{{ $t('i18n_3f016aa454') }}</a-col>
                    <a-col :span="10">
                      <a-form-item-rest>
                        <a-tooltip :title="$t('i18n_7a4ecc606c')">
                          <a-input
                            v-model:value="tempExtraData.dockerImagesLabels"
                            :placeholder="$t('i18n_b922323119')"
                          /> </a-tooltip
                      ></a-form-item-rest>
                    </a-col>
                  </a-row>
                </a-form-item>
                <a-form-item :label="$t('i18n_7ddbe15c84')">
                  <a-auto-complete
                    v-model:value="tempExtraData.dockerImagesNetworkMode"
                    :placeholder="$t('i18n_abd9ee868a')"
                    :options="[
                      {
                        title: $t('i18n_c36ab9a223'),
                        value: 'bridge'
                      },
                      {
                        title: $t('i18n_3d6acaa5ca'),
                        value: 'none'
                      },
                      {
                        title: $t('i18n_fcaef5b17a'),
                        value: 'container:<name|id>'
                      },
                      {
                        title: $t('i18n_ff39c45fbc'),
                        value: 'host'
                      }
                    ]"
                  >
                    <template #option="item">
                      {{ item.title }}
                    </template>
                  </a-auto-complete>
                </a-form-item>
                <a-form-item name="swarmId">
                  <template #label>
                    <a-tooltip>
                      {{ $t('i18n_5011e53403') }}
                      <template #title>
                        {{ $t('i18n_639fd37242') }}
                      </template>
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-select
                    v-model:value="tempExtraData.dockerSwarmId"
                    show-search
                    allow-clear
                    :placeholder="$t('i18n_a5e9874a96')"
                    @change="selectSwarm()"
                  >
                    <a-select-option value="">{{ $t('i18n_1e88a0cfaf') }}</a-select-option>
                    <a-select-option v-for="item1 in dockerSwarmList" :key="item1.id">{{ item1.name }}</a-select-option>
                    <template #suffixIcon>
                      <ReloadOutlined @click="loadDockerSwarmListAll" />
                    </template>
                  </a-select>
                </a-form-item>
                <a-form-item name="pushToRepository" :label="` `" :colon="false">
                  <a-form-item-rest>
                    <a-row>
                      <a-col :span="6" style="text-align: right">
                        <a-space>
                          <a-tooltip>
                            {{ $t('i18n_601426f8f2') }}
                            <template #title>
                              {{ $t('i18n_6d5f0fb74b') }}
                            </template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.pushToRepository"
                            :checked-children="$t('i18n_0a60ac8f02')"
                            :un-checked-children="$t('i18n_c9744f45e7')"
                          />
                        </a-space>
                      </a-col>
                      <a-col :span="6" style="text-align: right">
                        <a-space>
                          <a-tooltip
                            >{{ $t('i18n_c43743d213') }}<template #title>{{ $t('i18n_f1e3ef0def') }}</template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.pushToRepositoryAfterDelete"
                            :disabled="!tempExtraData.pushToRepository"
                            :checked-children="$t('i18n_0a60ac8f02')"
                            :un-checked-children="$t('i18n_c9744f45e7')"
                          />
                        </a-space>
                      </a-col>
                      <a-col :span="6" style="text-align: right">
                        <a-space>
                          <a-tooltip>
                            {{ $t('i18n_159a3a8037') }}
                            <template #title>{{ $t('i18n_ab968d842f') }} </template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.dockerBuildPull"
                            :checked-children="$t('i18n_0a60ac8f02')"
                            :un-checked-children="$t('i18n_c9744f45e7')"
                          />
                        </a-space>
                      </a-col>
                    </a-row>
                  </a-form-item-rest>
                </a-form-item>
                <a-form-item name="pushToRepository2" :label="` `" :colon="false">
                  <a-form-item-rest>
                    <a-row>
                      <a-col :span="6" style="text-align: right">
                        <a-space>
                          <a-tooltip>
                            {{ $t('i18n_9fb61a9936') }}
                            <template #title>
                              {{ $t('i18n_8c7ce1da57') }}
                            </template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.dockerTagIncrement"
                            :checked-children="$t('i18n_0a60ac8f02')"
                            :un-checked-children="$t('i18n_c9744f45e7')"
                          />
                        </a-space>
                      </a-col>
                      <a-col :span="6" style="text-align: right">
                        <a-space>
                          <a-tooltip>
                            no-cache
                            <template #title>{{ $t('i18n_28b69f9233') }} </template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.dockerNoCache"
                            :checked-children="$t('i18n_0a60ac8f02')"
                            :un-checked-children="$t('i18n_c9744f45e7')"
                          />
                        </a-space>
                      </a-col>
                    </a-row>
                  </a-form-item-rest>
                </a-form-item>
                <a-form-item v-if="tempExtraData.dockerSwarmId" name="dockerSwarmServiceName">
                  <template #label>
                    <a-tooltip>
                      {{ $t('i18n_b5ce5efa6e') }}
                      <template #title>
                        {{ $t('i18n_2f67a19f9d') }}
                      </template>
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-form-item-rest>
                    <a-select
                      v-model:value="tempExtraData.dockerSwarmServiceName"
                      allow-clear
                      :placeholder="$t('i18n_2ad3428664')"
                    >
                      <a-select-option v-for="item2 in swarmServiceListOptions" :key="item2.spec.name">{{
                        item2.spec.name
                      }}</a-select-option>
                    </a-select>
                  </a-form-item-rest>
                </a-form-item>
              </template>

              <!-- FTP -->
              <template v-if="temp.releaseMethod === 6">
                <a-form-item name="releaseMethodDataId" :help="$t('i18n_eaa85849f3')">
                  <template #label>
                    <a-tooltip
                      >{{ $t('i18n_32e3c8b702') }}<template #title>{{ $t('i18n_eaa85849f3') }}</template>
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-row>
                    <a-col :span="22">
                      <a-select
                        v-model:value="tempExtraData.releaseMethodDataId_6"
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
                        mode="multiple"
                        :placeholder="$t('i18n_cf93cd2cde')"
                      >
                        <a-select-option v-for="ftp in ftpList" :key="ftp.id" :disabled="!ftp.fileDirs">
                          <a-tooltip :title="ftp.name"> {{ ftp.name }}</a-tooltip>
                        </a-select-option>
                      </a-select>
                    </a-col>
                    <a-col :span="1" style="margin-left: 10px">
                      <ReloadOutlined @click="loadFtpList" />
                    </a-col>
                  </a-row>
                </a-form-item>
                <a-form-item name="releaseMethodDataId" :help="$t('i18n_8339e5e8e9')">
                  <template #label>
                    <a-tooltip :title="$t('i18n_8339e5e8e9')">
                      {{ $t('i18n_dbb2df00cf') }}
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-input-group compact>
                    <a-select
                      v-model:value="tempExtraData.releaseFtpDir"
                      show-search
                      allow-clear
                      style="width: 30%"
                      :placeholder="$t('i18n_cf93cd2cde')"
                    >
                      <a-select-option v-for="item in selectFtpDirs" :key="item">
                        <a-tooltip :title="item">{{ item }}</a-tooltip>
                      </a-select-option>
                    </a-select>
                    <a-form-item-rest>
                      <a-input
                        v-model:value="tempExtraData.releasePath3"
                        style="width: 70%"
                        :placeholder="$t('i18n_a75a5a9525')"
                      />
                    </a-form-item-rest>
                  </a-input-group>
                </a-form-item>
              </template>
            </template>
          </div>

          <div v-show="stepsCurrent === 4">
            <a-form-item name="cacheBuild">
              <template #label>
                <a-tooltip>
                  {{ $t('i18n_b6a828205d') }}
                  <template #title>
                    {{ $t('i18n_44473c1406') }}
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-form-item-rest>
                <a-row>
                  <a-col :span="2">
                    <a-tooltip :title="$t('i18n_12afa77947')">
                      <a-switch
                        v-model:checked="tempExtraData.cacheBuild"
                        :checked-children="$t('i18n_0a60ac8f02')"
                        :un-checked-children="$t('i18n_c9744f45e7')"
                      />
                    </a-tooltip>
                  </a-col>
                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        {{ $t('i18n_2499b03cc5') }}
                        <template #title> {{ $t('i18n_e0ae638e73') }} </template>

                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-switch
                        v-model:checked="tempExtraData.saveBuildFile"
                        :checked-children="$t('i18n_0a60ac8f02')"
                        :un-checked-children="$t('i18n_c9744f45e7')"
                      />
                    </a-space>
                  </a-col>

                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        {{ $t('i18n_36d00eaa3f') }}
                        <template #title>
                          {{ $t('i18n_4cbc5505c7') }}
                        </template>

                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-switch
                        v-model:checked="tempExtraData.checkRepositoryDiff"
                        :checked-children="$t('i18n_0a60ac8f02')"
                        :un-checked-children="$t('i18n_c9744f45e7')"
                      />
                    </a-space>
                  </a-col>
                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        {{ $t('i18n_a9c52ffd40') }}
                        <template #title>
                          {{ $t('i18n_07b6bb5e40') }}
                        </template>
                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-switch
                        v-model:checked="tempExtraData.strictlyEnforce"
                        :checked-children="$t('i18n_0a60ac8f02')"
                        :un-checked-children="$t('i18n_c9744f45e7')"
                      />
                    </a-space>
                  </a-col>
                </a-row>
              </a-form-item-rest>
            </a-form-item>
            <a-form-item v-if="tempExtraData.checkRepositoryDiff">
              <template #label>
                <a-tooltip>
                  {{ $t('i18n_0b23d2f584_include') }}
                  <template #title>
                    {{ $t('i18n_diff_include_desc') }}
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input
                v-model:value="tempExtraData.diffIncludePath"
                :placeholder="$t('i18n_diff_include_ph')"
                allow-clear
              />
            </a-form-item>
            <a-form-item v-if="tempExtraData.checkRepositoryDiff">
              <template #label>
                <a-tooltip>
                  {{ $t('i18n_0b23d2f584_exclude') }}
                  <template #title>
                    {{ $t('i18n_diff_exclude_desc') }}
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input
                v-model:value="tempExtraData.diffExcludePath"
                :placeholder="$t('i18n_diff_exclude_ph')"
                allow-clear
              />
            </a-form-item>
            <a-form-item name="webhook">
              <template #label>
                <a-tooltip>
                  WebHooks
                  <template #title>
                    <ul>
                      <li>{{ $t('i18n_78b2da536d') }}</li>
                      <li>{{ $t('i18n_9f6090c819') }}</li>
                      <li>{{ $t('i18n_a805615d15') }}</li>
                      <li>{{ $t('i18n_c96f47ec1b') }}</li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input v-model:value="temp.webhook" :placeholder="$t('i18n_7561bc005e')" />
            </a-form-item>
            <a-form-item :label="$t('i18n_db9296212a')" name="autoBuildCron">
              <a-auto-complete
                v-model:value="temp.autoBuildCron"
                :placeholder="$t('i18n_8ffded102f')"
                :options="CRON_DATA_SOURCE"
              >
                <template #option="item"> {{ item.title }} {{ item.value }} </template>
              </a-auto-complete>
            </a-form-item>
            <a-form-item name="noticeScriptId">
              <template #label>
                <a-tooltip>
                  {{ $t('i18n_192496786d') }}
                  <template #title>
                    <ul>
                      <li>{{ $t('i18n_6a49f994b1') }}</li>
                      <li>
                        {{ $t('i18n_9ff5504901') }}
                      </li>
                      <li>{{ $t('i18n_27054fefec') }}</li>
                      <li>
                        <b>{{ $t('i18n_edb881412a') }}</b>
                      </li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <template #help>{{ $t('i18n_b095ceda99') }}</template>
              <a-input-search
                :value="`${
                  tempExtraData ? tempExtraData.noticeScriptId || $t('i18n_b9b176e37a') : $t('i18n_b9b176e37a')
                }`"
                read-only
                :placeholder="$t('i18n_b9b176e37a')"
                :enter-button="$t('i18n_a056d9c4b3')"
                @search="
                  () => {
                    chooseScriptVisible = 1
                  }
                "
              >
                <template v-if="tempExtraData && tempExtraData.noticeScriptId" #addonBefore>
                  <span
                    @click="
                      () => {
                        tempExtraData = {
                          ...tempExtraData,
                          noticeScriptId: ''
                        }
                      }
                    "
                  >
                    {{ $t('i18n_54506fe138') }}
                  </span>
                </template>
              </a-input-search>
            </a-form-item>
            <a-form-item name="attachEnv">
              <template #label>
                <a-tooltip>
                  {{ $t('i18n_2351006eae') }}
                  <template #title>
                    <ul>
                      <li>{{ $t('i18n_7ef30cfd31') }}</li>
                      <li>{{ $t('i18n_2d7020be7d') }}</li>
                      <li>{{ $t('i18n_4f4c28a1fb') }}</li>
                      <li>{{ $t('i18n_b437a4d41d') }}</li>
                      <li>
                        {{ $t('i18n_6928f50eb3') }}<b>USE_TAR_GZ=1</b>
                        {{ $t('i18n_d5269713c7') }}
                        <b>tar.gz</b> {{ $t('i18n_045f89697e') }}
                      </li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input v-model:value="tempExtraData.attachEnv" :placeholder="$t('i18n_14dd5937e4')" />
            </a-form-item>
            <a-form-item name="cacheBuild">
              <template #label>
                <a-tooltip>
                  {{ $t('i18n_0f5fc9f300') }}
                  <template #title>
                    {{ $t('i18n_1819d0cdda') }}
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-row>
                <a-col :span="4">
                  <a-switch
                    v-model:checked="tempExtraData.syncFileStorage"
                    :checked-children="$t('i18n_6a620e3c07')"
                    :un-checked-children="$t('i18n_db709d591b')"
                  />
                </a-col>
                <a-col :span="6" style="text-align: right">
                  <a-form-item-rest>
                    <a-space>
                      <a-tooltip>
                        {{ $t('i18n_7f7ee903da') }}
                        <template #title> {{ $t('i18n_ba619a0942') }},{{ $t('i18n_8a1767a0d2') }} </template>

                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-switch
                        v-model:checked="tempExtraData.releaseHideFile"
                        :checked-children="$t('i18n_0a60ac8f02')"
                        :un-checked-children="$t('i18n_c9744f45e7')"
                      />
                    </a-space>
                  </a-form-item-rest>
                </a-col>

                <a-col :span="7" style="text-align: right">
                  <a-form-item-rest>
                    <a-space>
                      <a-tooltip>
                        {{ $t('i18n_824607be6b') }}
                        <template #title>
                          {{ $t('i18n_50f975c08e') }}
                        </template>
                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-input-number v-model:value="temp.resultKeepDay" :min="0" />
                    </a-space>
                  </a-form-item-rest>
                </a-col>

                <a-col :span="7" style="text-align: right">
                  <a-form-item-rest>
                    <a-space>
                      <a-tooltip>
                        {{ $t('i18n_50411665d7') }}
                        <template #title>
                          {{ $t('i18n_5dc78cb700') }}
                        </template>
                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-input-number v-model:value="tempExtraData.resultKeepCount" :min="0" />
                    </a-space>
                  </a-form-item-rest>
                </a-col>
              </a-row>
            </a-form-item>
            <a-form-item name="versionSnapshot" :label="$t('i18n_387b56ef22')" :help="$t('i18n_ddf32c340f')">
              <a-switch v-model:checked="tempExtraData.versionSnapshot" @change="onVersionSnapshotChange" />
            </a-form-item>
            <a-form-item name="versionKeepCount" :label="$t('i18n_fbf8959896')">
              <a-input-number
                v-model:value="tempExtraData.versionKeepCount"
                :min="0"
                :placeholder="$t('i18n_fbf8959896')"
              />
            </a-form-item>
            <a-form-item :label="$t('i18n_2f5e828ecd')" name="aliasCode" :help="$t('i18n_8c67370ee5')">
              <a-row>
                <a-col :span="10">
                  <a-input-search
                    v-model:value="temp.aliasCode"
                    :max-length="50"
                    :placeholder="$t('i18n_8fbcdbc785')"
                    @search="
                      () => {
                        temp = { ...temp, aliasCode: randomStr(6) }
                      }
                    "
                  >
                    <template #enterButton>
                      <a-button type="primary">
                        {{ $t('i18n_6709f4548f') }}
                      </a-button>
                    </template>
                  </a-input-search>
                </a-col>
                <a-col :span="1" style="text-align: right"></a-col>
                <a-col :span="10">
                  <a-form-item-rest>
                    <a-tooltip>
                      {{ $t('i18n_6f73c7cf47') }}
                      <template #title>
                        {{ $t('i18n_bb5aac6004') }}
                      </template>
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                    <a-input-number v-model:value="tempExtraData.fileStorageKeepDay" :min="0" />
                  </a-form-item-rest>
                </a-col>
              </a-row>
            </a-form-item>
            <a-form-item name="excludeReleaseAnt">
              <template #label>
                <a-tooltip>
                  {{ $t('i18n_eb164b696d') }}
                  <template #title>
                    <ul>
                      <li>{{ $t('i18n_f86324a429') }}</li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input v-model:value="tempExtraData.excludeReleaseAnt" :placeholder="$t('i18n_1e5ca46c26')" />
            </a-form-item>
          </div>
        </a-form>
      </a-card>
    </a-spin>
    <!-- 选择仓库 -->
    <CustomDrawer
      v-if="repositoryisible"
      destroy-on-close
      :title="`${$t('i18n_f89fa9b6c6')}`"
      placement="right"
      :open="repositoryisible"
      width="85vw"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          repositoryisible = false
        }
      "
    >
      <repository
        v-if="repositoryisible"
        ref="repository"
        :choose="true"
        :choose-val="tempRepository && tempRepository.id"
        @confirm="
          (repositoryId) => {
            temp = {
              ...temp,
              repositoryId: repositoryId,
              branchName: '',
              branchTagName: ''
            }
            repositoryisible = false
            changeRepositpry()
          }
        "
        @cancel="
          () => {
            repositoryisible = false
          }
        "
      >
      </repository>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                repositoryisible = false
              }
            "
          >
            {{ $t('i18n_625fb26b4b') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['repository'].handerConfirm()
              }
            "
          >
            {{ $t('i18n_e83a256e4f') }}
          </a-button>
        </a-space>
      </template>
    </CustomDrawer>
    <!-- 选择脚本 -->
    <CustomDrawer
      v-if="chooseScriptVisible != 0"
      destroy-on-close
      :title="`${$t('i18n_a056d9c4b3')}`"
      placement="right"
      :open="chooseScriptVisible != 0"
      width="70vw"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          chooseScriptVisible = 0
        }
      "
    >
      <scriptPage
        v-if="chooseScriptVisible"
        ref="scriptPage"
        :choose="chooseScriptVisible === 1 ? 'checkbox' : 'radio'"
        :choose-val="
          chooseScriptVisible === 1
            ? tempExtraData.noticeScriptId
            : temp.script?.indexOf('$ref.script.') != -1
              ? temp.script.replace('$ref.script.', '')
              : ''
        "
        mode="choose"
        @confirm="
          (id) => {
            if (chooseScriptVisible === 1) {
              tempExtraData = { ...tempExtraData, noticeScriptId: id }
            } else if (chooseScriptVisible === 2) {
              temp = { ...temp, script: '$ref.script.' + id }
            }
            chooseScriptVisible = 0
          }
        "
        @cancel="
          () => {
            chooseScriptVisible = 0
          }
        "
      ></scriptPage>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                chooseScriptVisible = false
              }
            "
          >
            {{ $t('i18n_625fb26b4b') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptPage'].handerConfirm()
              }
            "
          >
            {{ $t('i18n_e83a256e4f') }}
          </a-button>
        </a-space>
      </template>
    </CustomDrawer>
    <!-- 查看容器 -->
    <CustomDrawer
      v-if="dockerListVisible != 0"
      destroy-on-close
      :title="`${$t('i18n_998b7c48a8')}`"
      placement="right"
      :open="dockerListVisible != 0"
      width="70vw"
      @close="
        () => {
          dockerListVisible = 0
        }
      "
    >
      <docker-list v-if="dockerListVisible" ref="dockerlist"></docker-list>
    </CustomDrawer>

    <!-- 查看命令示例 -->
    <CustomModal
      v-if="viewScriptTemplVisible"
      v-model:open="viewScriptTemplVisible"
      destroy-on-close
      width="50vw"
      :title="$t('i18n_f087eb347c')"
      :footer="null"
      :mask-closable="false"
    >
      <a-collapse
        :active-key="
          buildScipts.map((item, index) => {
            return index + ''
          })
        "
      >
        <a-collapse-panel v-for="(group, index) in buildScipts" :key="`${index}`" :header="group.title">
          <a-list size="small" bordered :data-source="group.children">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-space>
                  {{ item.title }}

                  <SwapOutlined
                    @click="
                      () => {
                        temp = { ...temp, script: item.value }
                        viewScriptTemplVisible = false
                      }
                    "
                  />
                </a-space>
              </a-list-item>
            </template>
          </a-list>
        </a-collapse-panel>
      </a-collapse>
    </CustomModal>
  </div>
</template>
<script>
import codeEditor from '@/components/codeEditor'
import repository from '@/pages/repository/list.vue'
import scriptPage from '@/pages/script/script-list.vue'
import DockerList from '@/pages/docker/list'
import CustomSelect from '@/components/customSelect'
import { dockerSwarmListAll, dockerSwarmServicesList } from '@/api/docker-swarm'
import {
  getBuildGroupAll,
  editBuild,
  getBranchList,
  buildModeMap,
  releaseMethodMap,
  getBuildGet,
  getBuildNodeList
} from '@/api/build-info'
import { getSshListAll } from '@/api/ssh'
import { getSshProjectList } from '@/api/ssh-project'
import { getFtpListAll } from '@/api/ftp'
import { getRepositoryInfo } from '@/api/repository'
import { getNodeListAll, getProjectListAll } from '@/api/node'
// import { getScriptListAll } from "@/api/server-script";
import { getDishPatchListAll } from '@/api/dispatch'
import { itemGroupBy, randomStr } from '@/utils/const'
import { CRON_DATA_SOURCE } from '@/utils/const-i18n'

import { useGuideStore } from '@/stores/guide'
import { afterOptListSimple } from '@/api/dispatch'
import { dockerAllTag } from '@/api/docker-api'
export default {
  components: {
    CustomSelect,
    codeEditor,
    repository,
    scriptPage,
    DockerList
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    data: {
      type: Object,
      default: null
    },
    editSteps: {
      type: Number,
      default: 0
    }
  },
  emits: ['confirm', 'update:editSteps', 'changeBuildMode', 'saveChange'],
  data() {
    return {
      //   afterOptList,
      afterOptListSimple,
      releaseMethodMap,
      CRON_DATA_SOURCE,
      buildModeMap,
      // 当前仓库信息
      tempRepository: null,
      repositoryisible: false,
      chooseScriptVisible: 0,
      // 当前构建信息的 extraData 属性
      tempExtraData: {},
      // 可作为构建节点的节点列表（后端失败时降级为「本机（Server）」，不阻塞页面）
      buildNodeList: [],
      buildNodeLoading: false,
      buildNodeLoadError: false,
      viewScriptTemplVisible: false,
      buildScipts: [
        {
          title: this.$t('i18n_044b38221e'),
          children: [
            {
              title: this.$t('i18n_2b0f199da0'),
              value: 'mvn clean package -Dmaven.test.skip=true'
            },
            {
              title: this.$t('i18n_461fdd1576'),
              value: 'mvn clean package -Dmaven.test.skip=true -Pprod'
            },
            {
              title: this.$t('i18n_2a1d1da97a'),
              value: 'mvn clean package -Dmaven.test.skip=true -Ptest'
            },
            {
              title: this.$t('i18n_b36e87fe5b'),
              value: 'mvn clean package -DskipTests'
            },
            {
              title: 'mvn clean package',
              value: 'mvn clean package'
            },
            {
              title: this.$t('i18n_1ae2955867'),
              value: 'mvn -f xxx/pom.xml clean package'
            },
            {
              title: this.$t('i18n_ad9a677940'),
              value: 'mvn -s xxx/settings.xml clean package'
            }
          ]
        },
        {
          title: this.$t('i18n_2c921271d5'),
          children: [
            {
              title: this.$t('i18n_b7f770d80b'),
              value: 'npm i && npm run build'
            },
            {
              title: this.$t('i18n_e0ea800e34'),
              value: 'npm i && npm run build:prod'
            },
            {
              title: this.$t('i18n_88b4b85562'),
              value: 'npm i && npm run build:stage'
            },
            {
              title: this.$t('i18n_fcbf0d0a55'),
              value: 'yarn && yarn run build'
            },
            {
              title: this.$t('i18n_329e2e0b2e'),
              value: 'yarn && yarn --cwd xxx build'
            }
          ]
        }
      ],

      branchList: [],
      branchTagList: [],
      dispatchList: [],
      cascaderList: [],
      sshList: [],
      sshProjectList: [],
      ftpList: [],
      dockerSwarmList: [],
      //集群下 服务下拉数据
      swarmServiceListOptions: [],
      // scriptList: [],
      groupList: [],
      temp: {},
      rules: {
        name: [{ required: true, message: this.$t('i18n_fea996d31e'), trigger: 'blur' }],
        buildMode: [{ required: true, message: this.$t('i18n_e3e85de50c'), trigger: 'blur' }],
        releaseMethod: [{ required: true, message: this.$t('i18n_6d7f0f06be'), trigger: 'blur' }],
        branchName: [{ required: true, message: this.$t('i18n_50951f5e74'), trigger: 'blur' }],
        script: [{ required: true, message: this.$t('i18n_67aa1c0169'), trigger: 'blur' }],
        resultDirFile: [{ required: true, message: this.$t('i18n_cc92cf1e25'), trigger: 'blur' }],
        aliasCode: [{ pattern: /^\w+$/, message: this.$t('i18n_8b996c2815'), trigger: ['change', 'blur'] }],
        // releasePath: [{ required: true, message: '请填写发布目录', trigger: 'blur' }],
        repositoryId: [
          {
            required: true,
            message: this.$t('i18n_03c1f7c142'),
            trigger: 'blur'
          }
        ]
      },
      rulesSteps: [
        ['buildMode'],
        ['name', 'branchName', 'repositoryId'],
        ['script', 'resultDirFile'],
        ['releaseMethod'],
        ['aliasCode']
        // name: [{ required: true, message: '请填写构建名称', trigger: 'blur', : 1 }],
        // buildMode: [{ required: true, message: '请选择构建方式', trigger: 'blur', : 0 }],
        // releaseMethod: [{ required: true, message: '请选择发布操作', trigger: 'blur', : 3 }],
        // branchName: [{ required: true, message: '请选择分支', trigger: 'blur', : 1 }],
        // script: [{ required: true, message: '请填写构建命令', trigger: 'blur', : 2 }],
        // resultDirFile: [{ required: true, message: '请填写产物目录', trigger: 'blur', : 2 }],
        // // releasePath: [{ required: true, message: '请填写发布目录', trigger: 'blur' }],
        // repositoryId: [{ required: true, message: '请填选择构建的仓库', trigger: 'blur', : 1 }]
      ],
      stepsCurrent: 0,
      stepsItems: [
        {
          title: this.$t('i18n_17a74824de')
        },
        {
          title: this.$t('i18n_6ea1fe6baa')

          // status: 'process'
        },
        {
          title: this.$t('i18n_a2ae15f8a7')

          // status: 'wait'
        },
        {
          title: this.$t('i18n_3c91490844')

          // status: 'wait'
        },
        {
          title: this.$t('i18n_9ab433e930')

          // status: 'wait'
        }
      ],

      dslDefault:
        this.$t('i18n_ee19907fad') +
        '\n' +
        this.$t('i18n_d242bc3990') +
        '\n' +
        this.$t('i18n_429d4dbc55') +
        '\n' +
        'runsOn: ubuntu-1ms-latest\n' +
        this.$t('i18n_f1a2a46f52') +
        '\n' +
        'fromTag: xxx\n' +
        this.$t('i18n_36df970248') +
        '\n' +
        this.$t('i18n_30ff009ab3') +
        '\n' +
        this.$t('i18n_ed40308fe9') +
        '\n' +
        this.$t('i18n_df5f80946d') +
        '\n' +
        this.$t('i18n_2296651945') +
        '\n' +
        'steps:\n' +
        '  - uses: java\n' +
        '    version: 8\n' +
        '  - uses: maven\n' +
        '    version: 3.8.7\n' +
        '  - uses: node\n' +
        '    version: 16.3.0\n' +
        '#  - uses: go\n' +
        '#    version: 1.17.6\n' +
        '#  - uses: python3\n' +
        '#    version: 3.6.6\n' +
        this.$t('i18n_23559b6453') +
        '\n' +
        '  - uses: cache\n' +
        '    path: /root/.m2\n' +
        this.$t('i18n_1abf39bdb6') +
        '\n' +
        '    type: global\n' +
        this.$t('i18n_f63345630c') +
        '\n' +
        '  - uses: cache\n' +
        '    path: ${JPOM_WORKING_DIR}/web-vue/node_modules\n' +
        this.$t('i18n_5457c2e99f') +
        '\n' +
        '    mode: copy\n' +
        '  - run: npm config set registry https://registry.npmmirror.com\n' +
        this.$t('i18n_7bcc3f169c') +
        '\n' +
        '  - run: cd  ${JPOM_WORKING_DIR}/web-vue && npm i && npm run build\n' +
        '  - run: cd ${JPOM_WORKING_DIR} && mvn package -s script/settings.xml\n' +
        this.$t('i18n_ea89a319ec') +
        '\n' +
        '# binds:\n' +
        '#  - /Users/user/.m2/settings.xml:/root/.m2/\n' +
        this.$t('i18n_8d90b15eaf') +
        '\n' +
        '# dirChildrenOnly = true will create /var/data/titi and /var/data/tata dirChildrenOnly = false will create /var/data/root/titi and /var/data/root/tata\n' +
        '# copy:\n' +
        '#  - /Users/user/.m2/settings.xml:/root/.m2/:false\n' +
        this.$t('i18n_2b94686a65') +
        '\n' +
        'env:\n' +
        '  NODE_OPTIONS: --max-old-space-size=900\n' +
        this.$t('i18n_993a5c7eee') +
        '\n' +
        '#hostConfig:\n' +
        '#  CpuShares: 1',
      loading: false,
      dockerListVisible: 0,
      dockerAllTagList: [],
      dockerAllTagLoading: true,
      dslEditTabKey: 'content'
    }
  },
  computed: {
    ...mapGetters(useGuideStore, ['getExtendPlugins']),
    selectSshDirs() {
      if (!this.sshList || this.sshList.length <= 0) {
        return []
      }
      const findArray = this.sshList.filter((item) => {
        if (Array.isArray(this.tempExtraData.releaseMethodDataId_3)) {
          return item.id === this.tempExtraData.releaseMethodDataId_3[0]
        }
        return item.id === this.tempExtraData.releaseMethodDataId_3
      })
      if (findArray.length) {
        const fileDirs = findArray[0].fileDirs
        if (!fileDirs) {
          return []
        }
        return JSON.parse(fileDirs).map((item) => {
          return (item + '/').replace(new RegExp('//', 'gm'), '/')
        })
      }
      return []
    },
    selectFtpDirs() {
      if (!this.ftpList || this.ftpList.length <= 0) {
        return []
      }
      const findArray = this.ftpList.filter((item) => {
        if (Array.isArray(this.tempExtraData.releaseMethodDataId_6)) {
          return item.id === this.tempExtraData.releaseMethodDataId_6[0]
        }
        return item.id === this.tempExtraData.releaseMethodDataId_6
      })
      if (findArray.length) {
        const fileDirs = findArray[0].fileDirs
        if (!fileDirs) {
          return []
        }
        return JSON.parse(fileDirs).map((item) => {
          return (item + '/').replace(new RegExp('//', 'gm'), '/')
        })
      }
      return []
    },
    buildModeArray() {
      return Object.keys(this.buildModeMap).map((item) => {
        const value = parseInt(item)
        // 编辑已有构建：容器构建与其它构建方式脚本结构不同（DSL vs shell），创建后不允许互切；
        // 本地构建 ↔ 节点构建 只是执行位置不同、脚本一致，可以随时切换
        const editing = !!this.temp.id
        const editingContainer = this.temp.buildMode === 1
        return {
          value,
          disabled:
            // Server 自身运行在容器里时，本地构建无法在宿主机执行
            (value === 0 && this.getExtendPlugins.indexOf('inDocker') > -1) ||
            // 一个可用的构建节点都没有时，节点构建选了也无法执行
            (value === 2 && this.buildNodeNoAvailable) ||
            // 容器构建与其它方式互斥（仅编辑态）
            (editing && ((value === 1 && !editingContainer) || (value !== 1 && editingContainer))),
          name: this.buildModeMap[item]
        }
      })
    },
    // 构建节点下拉：只列可作为构建节点的节点（离线节点追加标注，仍可回显）
    buildNodeOptions() {
      return this.buildNodeList.map((item) => {
        const offline = item.online === false
        return {
          value: item.id,
          label: offline ? `${item.name} (${this.$t('i18n_50d4a8502e')})` : item.name
        }
      })
    },
    // 没有任何可用构建节点（节点列表加载成功但为空，且当前也没选中节点）
    buildNodeNoAvailable() {
      if (this.buildNodeLoading || this.buildNodeLoadError || this.temp.buildNodeId) {
        return false
      }
      return this.buildNodeList.length === 0
    },
    // 构建节点的可读名称：节点被删除时回退显示 id
    currentBuildNodeLabel() {
      if (!this.temp.buildNodeId) {
        return this.$t('i18n_d355cedabf')
      }
      const node = this.selectedBuildNode
      return node && node.name ? node.name : this.temp.buildNodeId
    },
    // 当前选中的构建节点（用于回显校验）
    selectedBuildNode() {
      if (!this.temp.buildNodeId) {
        return null
      }
      return this.buildNodeList.find((item) => item.id === this.temp.buildNodeId) || null
    },
    // 已选的构建节点不可用（被删除 / 离线 / 关闭了构建能力）
    buildNodeUnavailable() {
      if (this.buildNodeLoading || this.buildNodeLoadError) {
        return false
      }
      return !!this.temp.buildNodeId && !this.selectedBuildNode && this.buildNodeList.length > 0
    },
    // 当前选中的 SSH 项目（用于展示「发布目录」即项目目录）
    selectedSshProject() {
      const id = this.tempExtraData.releaseMethodDataId_7
      if (!id) {
        return null
      }
      return this.sshProjectList.find((item) => item.id === id) || null
    }
  },
  watch: {
    editSteps: {
      handler(v) {
        this.stepsCurrent = v
      },
      immediate: true
    },
    // 切换发布方式时清理其它分支的临时值，避免脏数据随 extraData 一起提交
    'temp.releaseMethod'(val) {
      if (!this.tempExtraData) {
        return
      }
      if (val !== 7) {
        delete this.tempExtraData.releaseMethodDataId_7
        // 「SSH 项目」用 releaseMethodDataId 直接提交，其余分支由后端按各自的 *_N 重新写入
        delete this.tempExtraData.releaseMethodDataId
      }
    }
  },
  created() {
    if (this.id) {
      this.refresh()
    } else {
      if (Object.keys(this.data).length) {
        // 复制
        this.handleEdit(this.data)
      } else {
        this.handleAdd()
      }
    }
    this.loadGroupList()
    this.loadBuildNodeList()
  },
  methods: {
    randomStr,
    // 版本快照：打开开关时若保留个数没填（默认 0），自动补默认值
    // 历史上 0 会被插件端当成「未启用」，发布成功但一条版本记录都不写，快速回滚进去永远是空列表
    onVersionSnapshotChange(checked) {
      if (checked && !(this.tempExtraData.versionKeepCount > 0)) {
        this.tempExtraData.versionKeepCount = 3
      }
    },
    refresh() {
      this.loading = true
      getBuildGet({
        id: this.id
      })
        .then((res) => {
          if (res.data) {
            this.handleEdit(res.data)
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    // 新增
    handleAdd() {
      // releaseMethodDataIdList 必须是数组：发布方式选「项目」后 handleEditBuildOk 会直接读 .length
      // 构建方式默认本地构建；buildNodeId 只在选择「节点构建」后才需要
      this.temp = { resultKeepDay: 0, releaseMethodDataIdList: [], buildMode: 0, buildNodeId: '' }
      this.branchList = []
      // this.tempRepository = {};
      // this.loadRepositoryList();
      this.loadDispatchList()
      this.loadNodeProjectList()
      this.loadSshList()
      this.loadSshProjectList()
      this.loadFtpList()
      this.loadDockerSwarmListAll()
      // this.loadScriptListList();

      this.tempExtraData = {
        cacheBuild: true,
        saveBuildFile: true,
        resultKeepCount: 0,
        fileStorageKeepDay: 0,
        versionSnapshot: false,
        versionKeepCount: 0
      }
      this.$refs['editBuildForm']?.resetFields()
    },
    // 修改
    handleEdit(record) {
      this.$refs['editBuildForm']?.resetFields()
      this.temp = Object.assign({}, record)
      this.temp.buildMode = this.temp.buildMode || 0
      // 构建节点回显：只有「节点构建」才有节点；老服务端无此字段时按空处理
      this.temp.buildNodeId = this.temp.buildNodeId || ''
      if (this.temp.buildMode === 1) {
        this.loadDockerAllTag()
      }
      // 设置当前临时的 额外构建信息
      this.tempExtraData = JSON.parse(record.extraData || '{}') || {}
      if (typeof this.tempExtraData === 'string') {
        this.tempExtraData = JSON.parse(this.tempExtraData)
      }
      if (this.tempExtraData.cacheBuild === undefined) {
        this.tempExtraData.cacheBuild = true
      }
      if (this.tempExtraData.saveBuildFile === undefined) {
        this.tempExtraData.saveBuildFile = true
      }
      if (this.tempExtraData.resultKeepCount === undefined) {
        this.tempExtraData.resultKeepCount = 0
      }
      if (this.tempExtraData.fileStorageKeepDay === undefined) {
        this.tempExtraData.fileStorageKeepDay = 0
      }
      if (this.tempExtraData.versionSnapshot === undefined) {
        this.tempExtraData.versionSnapshot = false
      }
      if (this.tempExtraData.versionKeepCount === undefined) {
        this.tempExtraData.versionKeepCount = 0
      }

      // 设置发布方式的数据
      if (this.tempExtraData.releaseMethodDataId) {
        if (record.releaseMethod === 1) {
          this.tempExtraData.releaseMethodDataId_1 = this.tempExtraData.releaseMethodDataId
        }
        if (record.releaseMethod === 2) {
          // 数据迁移后修改原始字段
          this.temp = {
            ...this.temp,
            releaseMethodDataIdList: (record.releaseMethodDataId || this.tempExtraData.releaseMethodDataId).split(':')
          }
        }
        if (record.releaseMethod === 3) {
          this.tempExtraData.releaseMethodDataId_3 = this.tempExtraData.releaseMethodDataId.split(',')
        }
        if (record.releaseMethod === 6) {
          this.tempExtraData.releaseMethodDataId_6 = this.tempExtraData.releaseMethodDataId.split(',')
        }
        if (record.releaseMethod === 7) {
          // SSH 项目：releaseMethodDataId 就是项目 id（单选）
          this.tempExtraData.releaseMethodDataId_7 = this.tempExtraData.releaseMethodDataId
        }
      }
      this.tempExtraData = { ...this.tempExtraData }
      this.changeRepositpry(true)

      this.loadDispatchList()
      this.loadDockerSwarmListAll()
      this.loadNodeProjectList()
      this.loadSshProjectList()
      // this.loadScriptListList();
      this.loadSshList().then(() => {
        if (this.tempExtraData.releaseMethodDataId_3) {
          //
          const findDirs = this.selectSshDirs
            .filter((item) => {
              return this.tempExtraData.releasePath && this.tempExtraData.releasePath.indexOf(item) > -1
            })
            .sort((item1, item2) => {
              return item2.length - item1.length
            })
          const releaseSshDir = findDirs[0] || ''
          this.tempExtraData = {
            ...this.tempExtraData,
            releaseSshDir: releaseSshDir,
            releasePath2: (this.tempExtraData.releasePath || '').slice(releaseSshDir.length)
          }
        }
      })

      this.loadFtpList().then(() => {
        if (this.tempExtraData.releaseMethodDataId_6) {
          //
          const findDirs = this.selectFtpDirs
            .filter((item) => {
              return this.tempExtraData.releaseFtpPath && this.tempExtraData.releaseFtpPath.indexOf(item) > -1
            })
            .sort((item1, item2) => {
              return item2.length - item1.length
            })
          const releaseFtpDir = findDirs[0] || ''
          this.tempExtraData = {
            ...this.tempExtraData,
            releaseFtpDir: releaseFtpDir,
            releasePath3: (this.tempExtraData.releaseFtpPath || '').slice(releaseFtpDir.length)
          }
        }
      })
      // 默认打开构建流程
      // this.stepsCurrent = this.editSteps
    },
    // // 加载脚本列表
    // loadScriptListList() {
    //   getScriptListAll().then((res) => {
    //     if (res.code === 200) {
    //       this.scriptList = res.data;
    //     }
    //   });
    // },
    // 加载可作为构建节点的节点列表
    loadBuildNodeList() {
      this.buildNodeLoading = true
      this.buildNodeLoadError = false
      return getBuildNodeList()
        .then((res) => {
          if (res && res.code === 200) {
            this.buildNodeList = Array.isArray(res.data) ? res.data : []
          } else {
            // 后端未就绪 / 无权限：降级为本机构建，不阻塞页面
            this.buildNodeList = []
            this.buildNodeLoadError = true
            console.warn('[build-edit] 构建节点列表加载失败', res)
          }
        })
        .catch((e) => {
          this.buildNodeList = []
          this.buildNodeLoadError = true
          console.warn('[build-edit] 构建节点列表加载失败', e)
        })
        .finally(() => {
          this.buildNodeLoading = false
        })
    },
    // 切换构建节点（仅「节点构建」方式下可选）
    changeBuildNode(value) {
      this.temp = { ...this.temp, buildNodeId: value || '' }
    },
    // 加载节点分发列表
    loadDispatchList() {
      this.dispatchList = []
      getDishPatchListAll().then((res) => {
        if (res.code === 200) {
          this.dispatchList = res.data
        }
      })
    },
    // 加载节点项目列表
    loadNodeProjectList() {
      this.cascaderList = []
      getNodeListAll().then((res0) => {
        if (res0.code !== 200) {
          return
        }
        getProjectListAll().then((res) => {
          if (res.code === 200) {
            let temp = itemGroupBy(res.data, 'nodeId', 'value', 'children')

            this.cascaderList = temp.map((item) => {
              let findArra = res0.data.filter((res0Item) => {
                return res0Item.id === item.value
              })
              item.label = findArra.length ? findArra[0].name : this.$t('i18n_1622dc9b6b')
              item.children = item.children.map((item2) => {
                return {
                  label: item2.name,
                  value: item2.projectId
                }
              })
              return item
            })
          }
        })
      })
    },
    // 获取仓库分支
    loadBranchList() {
      if (this.tempRepository?.repoType !== 0) {
        return
      }
      this.loadBranchListById(this.tempRepository?.id)
    },
    loadBranchListById(id) {
      this.branchList = []
      this.branchTagList = []
      const params = {
        repositoryId: id
      }
      this.loading = true
      getBranchList(params)
        .then((res) => {
          if (res.code === 200) {
            this.branchList = res.data?.branch || []
            this.branchTagList = res.data?.tags || []
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    // 提交节点数据
    handleEditBuildOk(build) {
      this.$emit('saveChange', true)
      const buildForm = this.$refs['editBuildForm']
      if (!buildForm) {
        // 组件未挂载/已被销毁时静默返回会让用户以为「点了没反应」，必须可见提示
        this.$emit('saveChange', false)
        console.warn('[build-edit] 表单未就绪，保存已取消')
        $notification.error({
          message: this.$t('i18n_de928c32b3')
        })
        return
      }
      // 检验表单
      buildForm
        .validate()
        .then(() => {
          // 节点构建必须指定执行节点（构建方式选对了但没选节点时，保存前明确提示）
          if (this.temp.buildMode === 2 && !this.temp.buildNodeId) {
            $notification.warn({
              message: this.$t('i18n_d1d1fbf1dd')
            })
            return false
          }
          const tempExtraData = Object.assign({}, this.tempExtraData)
          // 设置参数
          if (this.temp.releaseMethod === 2) {
            if ((this.temp.releaseMethodDataIdList || []).length < 2) {
              $notification.warn({
                message: this.$t('i18n_8309cec640')
              })
              return false
            }
            tempExtraData.releaseMethodDataId_2_node = this.temp.releaseMethodDataIdList[0]
            tempExtraData.releaseMethodDataId_2_project = this.temp.releaseMethodDataIdList[1]
          } else if (this.temp.releaseMethod === 3) {
            // ssh
            //  (this. tempExtraData.releasePath || '').slice(releaseSshDir.length);
            tempExtraData.releasePath = (
              (tempExtraData.releaseSshDir || '') +
              '/' +
              (tempExtraData.releasePath2 || '')
            ).replace(new RegExp('//', 'gm'), '/')
            tempExtraData.releaseMethodDataId_3 = (tempExtraData.releaseMethodDataId_3 || []).join(',')
          } else if (this.temp.releaseMethod === 6) {
            // ftp
            tempExtraData.releaseFtpPath = (
              (tempExtraData.releaseFtpDir || '') +
              '/' +
              (tempExtraData.releasePath3 || '')
            ).replace(new RegExp('//', 'gm'), '/')
            tempExtraData.releaseMethodDataId_6 = (tempExtraData.releaseMethodDataId_6 || []).join(',')
          } else if (this.temp.releaseMethod === 7) {
            // SSH 项目：releaseMethodDataId 直接放项目 id，后端 doSshProject 只读该字段与项目自身目录，不读 releasePath
            const sshProjectId = tempExtraData.releaseMethodDataId_7
            if (!sshProjectId) {
              $notification.warn({
                message: this.$t('i18n_8a8e690119')
              })
              return false
            }
            tempExtraData.releaseMethodDataId = sshProjectId
          } else {
            // 其它发布方式不使用该字段，清掉历史残留（后端会按各自分支重写）
            delete tempExtraData.releaseMethodDataId
          }

          this.temp = {
            ...this.temp,
            extraData: JSON.stringify(tempExtraData),
            resultKeepDay: this.temp.resultKeepDay || 0
          }
          this.$emit('saveChange', true)
          // 提交数据
          editBuild(this.temp)
            .then((res) => {
              if (res.code === 200) {
                // 成功
                $notification.success({
                  message: res.msg
                })
                //
                this.$emit('confirm', build, res.data, this.temp.buildEnvParameter)
              }
            })
            .catch((e) => {
              // 接口层失败（网络/服务端异常）同样必须可见，不能只转个 loading
              console.error('[build-edit] 保存接口失败', e)
              $notification.error({
                message: (e && e.message) || this.$t('i18n_de928c32b3')
              })
            })
            .finally(() => {
              this.$emit('saveChange', false)
            })
        })
        .catch((e) => {
          this.$emit('saveChange', false)
          const errorFields = e && e.errorFields
          if (!errorFields) {
            // 不是表单校验不通过，而是保存过程自身抛错（例如某个字段是 undefined）。
            // 这类异常原先被这里吞掉：用户点「保存」毫无反应、无请求、无提示，输入全部白填。
            console.error('[build-edit] 保存失败', e)
            $notification.error({
              message: this.$t('i18n_de928c32b3')
            })
            return
          }
          if (errorFields[0]) {
            // console.log(errorFields[0])
            const msg = errorFields[0].errors && errorFields[0].errors[0]
            if (msg) {
              $notification.warn({
                message: msg
              })
              // console.log(error)
            }
            // 切换到对应的流程
            const filedName = errorFields[0].name && errorFields[0].name[0]
            if (filedName) {
              for (let itemIndex in this.rulesSteps) {
                const item = this.rulesSteps[itemIndex]
                // console.log(itemIndex, filedName)
                if (item.includes(filedName)) {
                  this.stepsChange(Number(itemIndex))
                  break
                }

                // this.rulesSteps.forEach((item, index) => {
                //   if (item.includes(filedName)) {
                //     this.stepsChange(index)
                //   }
                // })
              }
            }
          }
        })
    },
    // 选择仓库
    changeRepositpry(noPullBranch) {
      getRepositoryInfo({
        id: this.temp.repositoryId
      }).then((res) => {
        if (res.code === 200) {
          this.tempRepository = res.data
          if (noPullBranch === true) {
            //
          } else {
            // 刷新分支
            this.loadBranchList()
          }
        }
      })
    },
    // 加载 SSH 项目列表（发布方式 =「SSH 项目」）
    loadSshProjectList() {
      return getSshProjectList({ page: 1, limit: 100 })
        .then((res) => {
          this.sshProjectList = res.code === 200 && res.data ? res.data.result || [] : []
        })
        .catch(() => {
          // 接口层已统一提示，这里只保证下拉不为 undefined
          this.sshProjectList = []
        })
    },
    // 加载 SSH 列表
    loadSshList() {
      return new Promise((resolve) => {
        this.sshList = []
        getSshListAll().then((res) => {
          if (res.code === 200) {
            this.sshList = res.data
            resolve()
          }
        })
      })
    },
    // 加载 FTP 列表
    loadFtpList() {
      return new Promise((resolve) => {
        this.ftpList = []
        getFtpListAll().then((res) => {
          if (res.code === 200) {
            this.ftpList = res.data
            resolve()
          }
        })
      })
    },
    //
    loadDockerSwarmListAll() {
      dockerSwarmListAll().then((res) => {
        this.dockerSwarmList = res.data
      })
    },
    // 选择发布集群时 渲染服务名称 数据
    selectSwarm() {
      this.swarmServiceListOptions = []
      this.tempExtraData = {
        ...this.tempExtraData,
        dockerSwarmServiceName: undefined
      }
      if (this.tempExtraData.dockerSwarmId) {
        // 选中时才处理
        dockerSwarmServicesList('', {
          id: this.tempExtraData.dockerSwarmId
        }).then((res) => {
          if (res.code === 200) {
            this.swarmServiceListOptions = res.data
          }
        })
      } else {
        this.swarmServiceListOptions = []
      }
    },
    // 分组数据
    loadGroupList() {
      getBuildGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    changeBuildMode(e) {
      const buildMode = e.target.value
      if (buildMode === 1) {
        this.loadDockerAllTag()
      }
      // 本地构建 ↔ 节点构建脚本一致，切换时保留脚本；容器构建脚本是 DSL，与其它方式互切时清空
      const script = (buildMode === 1) !== (this.temp.buildMode === 1) ? '' : this.temp.script
      // 构建节点只属于「节点构建」方式：切走时清空，避免切换方式后残留脏数据；
      // 首次切到节点构建且只有一个可选节点时直接带上，少点一次
      const buildNodeId =
        buildMode === 2
          ? this.temp.buildNodeId || (this.buildNodeList.length === 1 ? this.buildNodeList[0].id : '')
          : ''
      this.temp = { ...this.temp, script, buildMode, buildNodeId }
      this.$emit('changeBuildMode', buildMode)
    },
    // 查询 docker tag
    loadDockerAllTag() {
      this.dockerAllTagLoading = true
      dockerAllTag()
        .then((res) => {
          if (res.code === 200) {
            this.dockerAllTagList = res.data || []
          }
        })
        .finally(() => {
          this.dockerAllTagLoading = false
        })
    },
    stepsChange(current) {
      this.$emit('update:editSteps', current)
    }
  }
}
</script>
