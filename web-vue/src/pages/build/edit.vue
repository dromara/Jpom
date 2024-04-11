<template>
  <div>
    <a-spin :tip="$tl('p.loadingBuildData')" :spinning="loading">
      <a-card>
        <template #title>
          <a-steps v-model:current="stepsCurrent" size="small" :items="stepsItems" @change="stepsChange"></a-steps>
        </template>
        <a-form ref="editBuildForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
          <div v-show="stepsCurrent === 0">
            <a-alert :message="$tl('p.howToChooseBuildMethod')" type="info" show-icon>
              <template #description>
                <ul>
                  <li>
                    {{ $tl('p.howToList1') }}
                    <ul>
                      <li>{{ $tl('p.howToList2') }}</li>
                      <li>{{ $tl('p.howToList3') }}</li>
                      <li>{{ $tl('p.howToList4') }}</li>
                    </ul>
                  </li>
                  <li>
                    {{ $tl('p.howToList5') }}
                    <ul>
                      <li>{{ $tl('p.howToList6') }}</li>
                    </ul>
                  </li>

                  <li>{{ $tl('p.howToList7') }}</li>
                  <li v-if="getExtendPlugins.indexOf('inDocker') > -1" style="color: red">
                    {{ $tl('p.howToList8') }}
                  </li>
                </ul>
              </template>
            </a-alert>
            <a-form-item :name="['buildMode']">
              <template #label> {{ $tl('c.buildMethod') }} </template>
              <a-space>
                <a-radio-group
                  v-model:value="temp.buildMode"
                  :disabled="temp.id ? true : false"
                  name="buildMode"
                  @change="changeBuildMode"
                >
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
                    {{ $tl('p.viewAvailableContainers') }}
                  </a-button>
                </template>
                <template #label> {{ $tl('p.availableTags') }} </template>
                <a-spin :tip="$tl('p.loadingContainerTags')" :spinning="dockerAllTagLoading">
                  <a-space v-if="dockerAllTagList && dockerAllTagList.length">
                    <a-tag v-for="(item, index) in dockerAllTagList" :key="index">{{ item }}</a-tag>
                  </a-space>
                  <span v-else style="color: red; font-weight: bold">{{ $tl('p.noContainerOrTag') }}</span>
                </a-spin>
              </a-form-item>
              <a-alert :message="$tl('p.containerBuildNote')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $tl('p.containerList1') }}</li>
                    <li>{{ $tl('p.containerList2') }}</li>
                    <li>
                      {{ $tl('p.containerList3') }} <b style="color: red">fromTag</b> {{ $tl('p.containerList4') }}
                    </li>
                  </ul>
                </template>
              </a-alert>
            </template>
          </div>
          <div v-show="stepsCurrent === 1">
            <a-form-item :label="$tl('c.name')" name="name">
              <a-row>
                <a-col :span="10">
                  <a-input v-model:value="temp.name" :max-length="50" :placeholder="$tl('c.name')" />
                </a-col>
                <a-col :span="4" style="text-align: right">{{ $tl('p.groupName') }}</a-col>

                <a-col :span="10">
                  <a-form-item-rest>
                    <custom-select
                      v-model:value="temp.group"
                      :max-length="50"
                      :data="groupList"
                      :input-placeholder="$tl('p.addGroup')"
                      :select-placeholder="$tl('p.selectGroup')"
                    >
                    </custom-select>
                  </a-form-item-rest>
                </a-col>
              </a-row>
            </a-form-item>
            <a-form-item :label="$tl('p.sourceRepository')" name="repositoryId">
              <a-input-search
                :value="`${
                  tempRepository ? tempRepository.name + '[' + tempRepository.gitUrl + ']' : $tl('c.selectRepository')
                }`"
                read-only
                :placeholder="$tl('c.selectRepository')"
                :enter-button="$tl('c.chooseRepository')"
                @search="
                  () => {
                    repositoryisible = true
                  }
                "
              />
            </a-form-item>
            <template v-if="tempRepository && tempRepository.repoType === 0">
              <a-form-item :label="$tl('p.branch')" name="branchName">
                <a-row>
                  <a-col :span="10">
                    <custom-select
                      v-model:value="temp.branchName"
                      :disabled="temp.branchTagName ? true : false"
                      :data="branchList"
                      :can-reload="true"
                      :input-placeholder="$tl('p.customBranchWildcard')"
                      :select-placeholder="$tl('p.selectBranchForBuild')"
                      @on-refresh-select="loadBranchList"
                      @change="
                        () => {
                          $refs['editBuildForm'] && $refs['editBuildForm'].clearValidate()
                        }
                      "
                    >
                      <template #inputTips>
                        <div>
                          {{ $tl('c.supportWildcard') }}(AntPathMatcher)
                          <ul>
                            <li>? {{ $tl('c.matchOneCharacter') }}</li>
                            <li>* {{ $tl('c.matchZeroOrMoreCharacters') }}</li>
                            <li>** {{ $tl('c.matchZeroOrMoreDirectories') }}</li>
                          </ul>
                        </div>
                      </template>
                    </custom-select>
                  </a-col>
                  <a-col :span="4" style="text-align: right"> {{ $tl('p.tag') }}(TAG)：</a-col>
                  <a-col :span="10">
                    <a-form-item-rest>
                      <custom-select
                        v-model:value="temp.branchTagName"
                        :data="branchTagList"
                        :can-reload="true"
                        :input-placeholder="$tl('p.customTagWildcard')"
                        :select-placeholder="$tl('p.selectTagForBuild')"
                        @on-refresh-select="loadBranchList"
                        @change="
                          () => {
                            $refs['editBuildForm'] && $refs['editBuildForm'].clearValidate()
                          }
                        "
                      >
                        <template #inputTips>
                          <div>
                            {{ $tl('c.supportWildcard') }}(AntPathMatcher)
                            <ul>
                              <li>? {{ $tl('c.matchOneCharacter') }}</li>
                              <li>* {{ $tl('c.matchZeroOrMoreCharacters') }}</li>
                              <li>** {{ $tl('c.matchZeroOrMoreDirectories') }}</li>
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
                :label="$tl('p.cloneDepth')"
                name="cloneDepth"
              >
                <a-input-number
                  v-model:value="tempExtraData.cloneDepth"
                  style="width: 100%"
                  :placeholder="$tl('p.customCloneDepth')"
                />
              </a-form-item>
            </template>
          </div>

          <!-- 构建流程 -->
          <div v-show="stepsCurrent === 2">
            <a-form-item v-if="temp.buildMode === 0" name="script">
              <template #label>
                <a-tooltip>
                  {{ $tl('p.buildCommand') }}
                  <template #title>
                    {{ $tl('p.buildCommandL1') }}<b>{{ $tl('p.buildCommandL2') }} </b>{{ $tl('p.buildCommandL3') }}
                    <b>cd xxx && mvn clean package</b>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <template #help>
                {{ $tl('p.buildCommandHelp') }}
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
                        {{ $tl('p.commonBuildCommandExample') }}
                      </a-button>
                      <a-button
                        type="link"
                        @click="
                          () => {
                            chooseScriptVisible = 2
                          }
                        "
                      >
                        {{ $tl('p.scriptTemplate') }}
                      </a-button>
                    </a-space>
                  </template>
                </code-editor>
              </a-form-item-rest>
            </a-form-item>
            <a-form-item v-if="temp.buildMode === 1" name="script">
              <template #label>
                <a-tooltip>
                  DSL {{ $tl('p.content') }}
                  <template #title>
                    <p>{{ $tl('p.dsl1') }}</p>
                    <ul>
                      <li>{{ $tl('p.dsl2') }}</li>
                      <li>{{ $tl('p.dsl3') }}</li>
                      <li>{{ $tl('p.dsl4') }}</li>
                    </ul>
                    <div>
                      {{ $tl('p.dsl5') }}
                      <ol>
                        <li>
                          {{ $tl('p.dsl6') }}
                        </li>
                        <li>{{ $tl('p.dsl7') }}</li>
                        <li>{{ $tl('p.dsl8') }}</li>
                        <li>
                          {{ $tl('p.dsl9') }}
                        </li>
                        <li>
                          {{ $tl('p.dsl10') }}
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
                  :placeholder="$tl('p.buildDslConfigContent')"
                >
                  <template #tool_before>
                    <a-segmented
                      v-model:value="dslEditTabKey"
                      :options="[
                        {
                          label: `DSL ${$tl('c.configuration')}`,
                          value: 'content'
                        },
                        { label: $tl('c.configurationExample'), value: 'demo' }
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
                          label: `DSL ${$tl('c.configuration')}`,
                          value: 'content'
                        },
                        { label: $tl('c.configurationExample'), value: 'demo' }
                      ]"
                    />
                  </template> </code-editor
              ></a-form-item-rest>
            </a-form-item>
            <a-form-item v-if="temp.buildMode !== undefined" name="resultDirFile" class="jpom-target-dir">
              <template #label>
                <a-tooltip>
                  {{ $tl('p.artifactDirectory') }}
                  <template #title>
                    <div>
                      {{ $tl('p.artifact1') }}
                      <b>mvn clean package</b> {{ $tl('p.artifact2') }}
                      <b> modules/server/target/server-2.4.2-release</b>
                    </div>
                    <div><br /></div>
                    <!-- 只有本地构建支持 模糊匹配 -->
                    <div v-if="temp.buildMode === 0">
                      {{ $tl('c.supportWildcard') }}(AntPathMatcher){{ $tl('p.onlyUseFirstMatched') }}
                      <ul>
                        <li>? {{ $tl('c.matchOneCharacter') }}</li>
                        <li>* {{ $tl('c.matchZeroOrMoreCharacters') }}</li>
                        <li>** {{ $tl('c.matchZeroOrMoreDirectories') }}</li>
                      </ul>
                    </div>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input
                v-model:value="temp.resultDirFile"
                :max-length="200"
                :placeholder="$tl('p.buildArtifactsPath')"
              />
            </a-form-item>

            <a-alert v-if="temp.buildMode === undefined" :message="$tl('p.noBuildMethodSelected')" banner />
            <template v-else>
              <a-form-item :label="$tl('p.environmentVariables')" name="buildEnvParameter">
                <a-textarea
                  v-model:value="temp.buildEnvParameter"
                  :placeholder="$tl('p.enterBuildEnvVars')"
                  :auto-size="{ minRows: 3, maxRows: 5 }"
                />
              </a-form-item>
              <a-form-item :label="$tl('p.executionMethod')" name="commandExecMode">
                <a-radio-group v-model:value="tempExtraData.commandExecMode" button-style="solid">
                  <a-radio-button value="default">{{ $tl('p.default') }}</a-radio-button>
                  <a-radio-button value="apache_exec">{{ $tl('p.multiThread') }}</a-radio-button>
                </a-radio-group>
                <template #help>{{ $tl('p.experimentalFeature') }}</template>
              </a-form-item>
            </template>
          </div>

          <div v-show="stepsCurrent === 3">
            <a-form-item name="releaseMethod">
              <template #label>
                <a-tooltip>
                  {{ $tl('c.publishOperation') }}
                  <template #title>
                    <ul>
                      <li>{{ $tl('p.publish1') }}</li>
                      <li>{{ $tl('p.publish2') }}</li>
                      <li>{{ $tl('p.publish3') }}</li>
                      <li>
                        {{ $tl('p.publish4') }}
                      </li>
                      <li>{{ $tl('p.publish5') }}</li>
                      <li>
                        {{ $tl('p.publish6') }}
                      </li>
                      <li>{{ $tl('p.publish7') }}</li>
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
              {{ $tl('p.selectPublishMethod') }}
            </div>
            <template v-else>
              <template v-if="temp.releaseMethod === 0">
                {{ $tl('p.noPublish') }},{{ $tl('p.noPublishProcess') }}
              </template>
              <!-- 节点分发 -->
              <template v-if="temp.releaseMethod === 1">
                <a-form-item :label="$tl('p.distributeProject')" name="releaseMethodDataId">
                  <a-select
                    v-model:value="tempExtraData.releaseMethodDataId_1"
                    show-search
                    allow-clear
                    :placeholder="$tl('p.selectDistributeProject')"
                  >
                    <a-select-option v-for="dispatch in dispatchList" :key="dispatch.id"
                      >{{ dispatch.name }}
                    </a-select-option>
                    <template #suffixIcon>
                      <ReloadOutlined @click="loadDispatchList" />
                    </template>
                  </a-select>
                </a-form-item>
                <a-form-item name="projectSecondaryDirectory" :label="$tl('c.secondLevelDirectory')">
                  <a-input
                    v-model:value="tempExtraData.projectSecondaryDirectory"
                    :placeholder="$tl('p.useNodeDistributeConfig')"
                  />
                </a-form-item>
              </template>

              <!-- 项目 -->
              <template v-if="temp.releaseMethod === 2">
                <a-form-item :label="$tl('p.publishProject')" name="releaseMethodDataIdList">
                  <a-cascader
                    v-model:value="temp.releaseMethodDataIdList"
                    :options="cascaderList"
                    :placeholder="$tl('p.selectNodeProject')"
                  >
                    <template #suffixIcon>
                      <ReloadOutlined @click="loadNodeProjectList" />
                    </template>
                  </a-cascader>
                </a-form-item>
                <a-form-item :label="$tl('p.postBuildActions')" name="afterOpt">
                  <a-select
                    v-model:value="tempExtraData.afterOpt"
                    show-search
                    allow-clear
                    :placeholder="$tl('p.publishPostOperation')"
                  >
                    <a-select-option v-for="opt in afterOptListSimple" :key="opt.value">{{
                      opt.title
                    }}</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item name="projectSecondaryDirectory" :label="$tl('c.secondLevelDirectory')">
                  <a-input
                    v-model:value="tempExtraData.projectSecondaryDirectory"
                    :placeholder="$tl('p.publishToRoot')"
                  />
                </a-form-item>
              </template>
              <!-- SSH -->
              <template v-if="temp.releaseMethod === 3">
                <a-form-item name="releaseMethodDataId" :help="$tl('c.sshNotConfigured')">
                  <template #label>
                    <a-tooltip>
                      {{ $tl('p.publishViaSSH') }}
                      <template #title>
                        {{ $tl('c.sshNotConfigured') }}
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
                        :placeholder="$tl('c.selectSSH')"
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
                <a-form-item name="releaseMethodDataId" :help="$tl('c.sshDirectoryConfiguration')">
                  <template #label>
                    <a-tooltip :title="$tl('c.sshDirectoryConfiguration')">
                      {{ $tl('p.publishDirectory') }}
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-input-group compact>
                    <a-select
                      v-model:value="tempExtraData.releaseSshDir"
                      show-search
                      allow-clear
                      style="width: 30%"
                      :placeholder="$tl('c.selectSSH')"
                    >
                      <a-select-option v-for="item in selectSshDirs" :key="item">
                        <a-tooltip :title="item">{{ item }}</a-tooltip>
                      </a-select-option>
                    </a-select>
                    <a-form-item-rest>
                      <a-input
                        v-model:value="tempExtraData.releasePath2"
                        style="width: 70%"
                        :placeholder="$tl('p.releasePath2P')"
                      />
                    </a-form-item-rest>
                  </a-input-group>
                </a-form-item>
              </template>

              <a-form-item v-if="temp.releaseMethod === 3" name="releaseBeforeCommand">
                <!-- sshCommand -->
                <template #label>
                  <a-tooltip>
                    {{ $tl('p.prePublishCommand') }}
                    <template #title>
                      {{ $tl('p.prePublish1') }}
                      <ul>
                        <li>{{ $tl('p.prePublish2') }}</li>
                        <li>{{ $tl('p.prePublish3') }}</li>
                      </ul>
                    </template>
                    <QuestionCircleOutlined v-if="!temp.id" />
                  </a-tooltip>
                </template>
                <template #help>
                  {{ $tl('p.prePublishHelp') }}
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
                      <a-tag>{{ $tl('p.nonMandatory') }}</a-tag></template
                    >
                  </code-editor>
                </a-form-item-rest>
              </a-form-item>
              <a-form-item v-if="temp.releaseMethod === 3 || temp.releaseMethod === 4" name="releaseCommand">
                <!-- sshCommand LocalCommand -->
                <template #label>
                  <a-tooltip>
                    {{ $tl('p.postPublishCommand') }}
                    <template #title>
                      {{ $tl('p.postPublish1') }}
                      <ul>
                        <li>{{ $tl('p.postPublish2') }}</li>
                        <li>{{ $tl('p.postPublish3') }}</li>
                      </ul>
                    </template>

                    <QuestionCircleOutlined v-if="!temp.id" />
                  </a-tooltip>
                </template>
                <template #help>
                  {{ $tl('p.postPublishHelp') }}
                </template>
                <a-form-item-rest>
                  <code-editor
                    v-model:content="tempExtraData.releaseCommand"
                    height="40vh"
                    :show-tool="true"
                    :options="{ mode: 'shell' }"
                  >
                    <template #tool_before>
                      <a-tag>{{ $tl('p.mandatory') }}</a-tag></template
                    >
                  </code-editor>
                </a-form-item-rest>
              </a-form-item>

              <a-form-item v-if="temp.releaseMethod === 2 || temp.releaseMethod === 3" name="clearOld">
                <template #label>
                  <a-tooltip>
                    {{ $tl('p.clearPublish') }}
                    <template #title>
                      {{ $tl('p.clearPublishTip') }}
                    </template>
                    <QuestionCircleOutlined v-if="!temp.id" />
                  </a-tooltip>
                </template>
                <a-form-item-rest>
                  <a-row>
                    <a-col :span="4">
                      <a-switch
                        v-model:checked="tempExtraData.clearOld"
                        :checked-children="$tl('c.yes')"
                        :un-checked-children="$tl('c.no')"
                      />
                    </a-col>
                    <template v-if="temp.releaseMethod === 2">
                      <a-col :span="4" style="text-align: right">
                        <a-tooltip>
                          {{ $tl('p.diffPublish') }}
                          <template #title>
                            {{ $tl('p.diffTip1') }}
                            <ul>
                              <li>
                                {{ $tl('p.diffTip2') }}
                              </li>
                              <li>{{ $tl('p.diffTip3') }}</li>
                            </ul>
                          </template>
                          <QuestionCircleOutlined v-if="!temp.id" />
                        </a-tooltip>
                      </a-col>
                      <a-col :span="4">
                        <a-switch
                          v-model:checked="tempExtraData.diffSync"
                          :checked-children="$tl('c.yes')"
                          :un-checked-children="$tl('c.no')"
                        />
                      </a-col>
                      <a-col :span="4" style="text-align: right">
                        <a-tooltip>
                          {{ $tl('p.preStopPublish') }}
                          <template #title>
                            {{ $tl('p.preStopPublishTip') }}
                          </template>
                          <QuestionCircleOutlined v-if="!temp.id" />
                        </a-tooltip>
                      </a-col>
                      <a-col :span="4">
                        <a-switch
                          v-model:checked="tempExtraData.projectUploadCloseFirst"
                          :checked-children="$tl('c.yes')"
                          :un-checked-children="$tl('c.no')"
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
                      {{ $tl('p.executeContainer') }}
                      <template #title>
                        {{ $tl('p.executeContainerTip') }}
                      </template>
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-input v-model:value="tempExtraData.fromTag" :placeholder="$tl('p.executeContainerTag')" />
                </a-form-item>

                <a-tooltip :title="$tl('p.dockerfilePath')">
                  <a-form-item name="dockerfile" label="Dockerfile">
                    <a-input v-model:value="tempExtraData.dockerfile" :placeholder="$tl('p.dockerfilePathTip')" />
                  </a-form-item>
                </a-tooltip>
                <a-form-item name="dockerTag" :label="$tl('p.imageTag')">
                  <a-tooltip :title="$tl('p.containerTag')">
                    <a-input v-model:value="tempExtraData.dockerTag" :placeholder="$tl('p.containerTags')" />
                  </a-tooltip>
                </a-form-item>
                <a-form-item name="dockerBuildArgs" :label="$tl('p.buildParams')">
                  <a-row>
                    <a-col :span="10">
                      <a-tooltip :title="$tl('p.buildParamsInput')">
                        <a-input v-model:value="tempExtraData.dockerBuildArgs" :placeholder="$tl('p.buildParamsTip')" />
                      </a-tooltip>
                    </a-col>
                    <a-col :span="4" style="text-align: right">{{ $tl('p.imageTags') }}</a-col>
                    <a-col :span="10">
                      <a-form-item-rest>
                        <a-tooltip :title="$tl('p.imageTagInput')">
                          <a-input
                            v-model:value="tempExtraData.dockerImagesLabels"
                            :placeholder="$tl('p.imageTagParamsTip')"
                          /> </a-tooltip
                      ></a-form-item-rest>
                    </a-col>
                  </a-row>
                </a-form-item>
                <a-form-item name="swarmId">
                  <template #label>
                    <a-tooltip>
                      {{ $tl('p.publishCluster') }}
                      <template #title>
                        {{ $tl('p.dockerSwarmCluster') }}
                      </template>
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-select
                    v-model:value="tempExtraData.dockerSwarmId"
                    show-search
                    allow-clear
                    :placeholder="$tl('p.selectClusterForPublish')"
                    @change="selectSwarm()"
                  >
                    <a-select-option value="">{{ $tl('p.noClusterPublish') }}</a-select-option>
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
                            {{ $tl('p.pushToRepositoryLabel') }}
                            <template #title>
                              {{ $tl('p.pushToRepository') }}
                            </template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.pushToRepository"
                            :checked-children="$tl('c.yes')"
                            :un-checked-children="$tl('c.no')"
                          />
                        </a-space>
                      </a-col>
                      <a-col :span="6" style="text-align: right">
                        <a-space>
                          <a-tooltip>
                            {{ $tl('p.versionIncrement') }}
                            <template #title>
                              {{ $tl('p.versionIncrementTip') }}
                            </template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.dockerTagIncrement"
                            :checked-children="$tl('c.yes')"
                            :un-checked-children="$tl('c.no')"
                          />
                        </a-space>
                      </a-col>
                      <a-col :span="6" style="text-align: right">
                        <a-space>
                          <a-tooltip>
                            no-cache
                            <template #title>{{ $tl('p.noCacheBuild') }} </template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.dockerNoCache"
                            :checked-children="$tl('c.yes')"
                            :un-checked-children="$tl('c.no')"
                          />
                        </a-space>
                      </a-col>
                      <a-col :span="6" style="text-align: right">
                        <a-space>
                          <a-tooltip>
                            {{ $tl('p.updateImage') }}
                            <template #title>{{ $tl('p.attemptToUpdateBaseImage') }} </template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.dockerBuildPull"
                            :checked-children="$tl('c.yes')"
                            :un-checked-children="$tl('c.no')"
                          />
                        </a-space>
                      </a-col>
                    </a-row>
                  </a-form-item-rest>
                </a-form-item>
                <a-form-item v-if="tempExtraData.dockerSwarmId" name="dockerSwarmServiceName">
                  <a-form-item-rest>
                    <template #label>
                      <a-tooltip>
                        {{ $tl('p.clusterService') }}
                        <template #title>
                          {{ $tl('p.selectServiceForPublish') }}
                        </template>
                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                    </template>
                    <a-select
                      v-model:value="tempExtraData.dockerSwarmServiceName"
                      allow-clear
                      :placeholder="$tl('p.selectedServiceForPublish')"
                    >
                      <a-select-option v-for="item2 in swarmServiceListOptions" :key="item2.spec.name">{{
                        item2.spec.name
                      }}</a-select-option>
                    </a-select>
                  </a-form-item-rest>
                </a-form-item>
              </template>
            </template>
          </div>

          <div v-show="stepsCurrent === 4">
            <a-form-item name="cacheBuild">
              <template #label>
                <a-tooltip>
                  {{ $tl('p.cacheBuild') }}
                  <template #title>
                    {{ $tl('p.cacheBuildTip') }}
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-form-item-rest>
                <a-row>
                  <a-col :span="2">
                    <a-tooltip :title="$tl('p.cacheBuildDirectory')">
                      <a-switch
                        v-model:checked="tempExtraData.cacheBuild"
                        :checked-children="$tl('c.yes')"
                        :un-checked-children="$tl('c.no')"
                      />
                    </a-tooltip>
                  </a-col>
                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        {{ $tl('p.retainBuildArtifacts') }}
                        <template #title> {{ $tl('p.retainBuildArtifactsInfo') }} </template>

                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-switch
                        v-model:checked="tempExtraData.saveBuildFile"
                        :checked-children="$tl('c.yes')"
                        :un-checked-children="$tl('c.no')"
                      />
                    </a-space>
                  </a-col>

                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        {{ $tl('p.diffBuild') }}
                        <template #title>
                          {{ $tl('p.incrementalBuild') }}
                        </template>

                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-switch
                        v-model:checked="tempExtraData.checkRepositoryDiff"
                        :checked-children="$tl('c.yes')"
                        :un-checked-children="$tl('c.no')"
                      />
                    </a-space>
                  </a-col>
                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        {{ $tl('p.strictExecution') }}
                        <template #title>
                          {{ $tl('p.strictExecutionTip') }}
                        </template>
                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-switch
                        v-model:checked="tempExtraData.strictlyEnforce"
                        :checked-children="$tl('c.yes')"
                        :un-checked-children="$tl('c.no')"
                      />
                    </a-space>
                  </a-col>
                </a-row>
              </a-form-item-rest>
            </a-form-item>
            <a-form-item name="webhook">
              <template #label>
                <a-tooltip>
                  WebHooks
                  <template #title>
                    <ul>
                      <li>{{ $tl('p.webHookTip1') }}</li>
                      <li>{{ $tl('p.webHookTip2') }}</li>
                      <li>{{ $tl('p.webHookTip3') }}</li>
                      <li>{{ $tl('p.webHookTip4') }}</li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input v-model:value="temp.webhook" :placeholder="$tl('p.buildProcessRequest')" />
            </a-form-item>
            <a-form-item :label="$tl('p.timedBuild')" name="autoBuildCron">
              <a-auto-complete
                v-model:value="temp.autoBuildCron"
                :placeholder="$tl('p.cronExpression')"
                :options="CRON_DATA_SOURCE"
              >
                <template #option="item"> {{ item.title }} {{ item.value }} </template>
              </a-auto-complete>
            </a-form-item>
            <a-form-item name="noticeScriptId">
              <template #label>
                <a-tooltip>
                  {{ $tl('p.eventScript') }}
                  <template #title>
                    <ul>
                      <li>{{ $tl('p.eventScriptTip1') }}</li>
                      <li>
                        {{ $tl('p.eventScriptTip2') }}
                      </li>
                      <li>{{ $tl('p.eventScriptTip3') }}</li>
                      <li>
                        <b>{{ $tl('p.eventScriptTip4') }}</b>
                      </li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input-search
                :value="`${
                  tempExtraData ? tempExtraData.noticeScriptId || $tl('c.selectScript') : $tl('c.selectScript')
                }`"
                read-only
                :placeholder="$tl('c.selectScript')"
                :enter-button="$tl('c.chooseScript')"
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
                    {{ $tl('p.resetSelection') }}
                  </span>
                </template>
              </a-input-search>
            </a-form-item>
            <a-form-item name="attachEnv">
              <template #label>
                <a-tooltip>
                  {{ $tl('p.additionalEnvVars') }}
                  <template #title>
                    <ul>
                      <li>附加环境变量是指读取仓库指定环境变量文件来新增到执行构建运行时</li>
                      <li>比如常见的 .env 文件</li>
                      <li>文件内容格式要求：env_name=xxxxx 不满足格式的行将自动忽略</li>
                      <li>也支持 URL 参数格式：test_par=123abc&test_par2=abc21</li>
                      <li>
                        支持配置系统参数：<b>USE_TAR_GZ=1</b>
                        表示构建产物为文件夹时将打包为
                        <b>tar.gz</b> 压缩包进行发布
                      </li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input v-model:value="tempExtraData.attachEnv" :placeholder="$tl('p.additionalEnvVarsInput')" />
            </a-form-item>
            <a-form-item name="cacheBuild">
              <template #label>
                <a-tooltip>
                  {{ $tl('p.fileManagementCenter') }}
                  <template #title>
                    {{ $tl('p.syncToFile') }}
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-row>
                <a-col :span="4">
                  <a-switch
                    v-model:checked="tempExtraData.syncFileStorage"
                    :checked-children="$tl('p.sync')"
                    :un-checked-children="$tl('p.noSync')"
                  />
                </a-col>
                <a-col :span="6" style="text-align: right">
                  <a-form-item-rest>
                    <a-space>
                      <a-tooltip>
                        {{ $tl('p.publishHiddenFiles') }}
                        <template #title>
                          {{ $tl('p.defaultIgnoreHiddenFiles') }},{{ $tl('p.publishAllFiles') }}
                        </template>

                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-switch
                        v-model:checked="tempExtraData.releaseHideFile"
                        :checked-children="$tl('c.yes')"
                        :un-checked-children="$tl('c.no')"
                      />
                    </a-space>
                  </a-form-item-rest>
                </a-col>

                <a-col :span="7" style="text-align: right">
                  <a-form-item-rest>
                    <a-space>
                      <a-tooltip>
                        {{ $tl('p.retainDays') }}
                        <template #title>
                          构建产物保留天数，小于等于 0
                          为跟随全局保留配置。注意自动清理仅会清理记录状态为：（构建结束、发布中、发布失败、发布失败）的数据避免一些异常构建影响保留个数
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
                        {{ $tl('p.retainCount') }}
                        <template #title>
                          构建产物保留个数，小于等于 0 为跟随全局保留配置（如果数值大于 0
                          将和全局配置对比最小值来参考）。注意自动清理仅会清理记录状态为：（构建结束、发布中、发布失败、发布失败）的数据避免一些异常构建影响保留个数。
                          将在创建新的构建记录时候检查保留个数
                        </template>
                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-input-number v-model:value="tempExtraData.resultKeepCount" :min="0" />
                    </a-space>
                  </a-form-item-rest>
                </a-col>
              </a-row>
            </a-form-item>
            <a-form-item :label="$tl('p.aliasCode')" name="aliasCode" :help="$tl('p.aliasCodeInput')">
              <a-row>
                <a-col :span="10">
                  <a-input-search
                    v-model:value="temp.aliasCode"
                    :max-length="50"
                    :placeholder="$tl('p.enterAliasCode')"
                    @search="
                      () => {
                        temp = { ...temp, aliasCode: randomStr(6) }
                      }
                    "
                  >
                    <template #enterButton>
                      <a-button type="primary">
                        {{ $tl('p.generateAliasCode') }}
                      </a-button>
                    </template>
                  </a-input-search>
                </a-col>
                <a-col :span="1" style="text-align: right"></a-col>
                <a-col :span="10">
                  <a-form-item-rest>
                    <a-tooltip>
                      {{ $tl('p.retentionDays') }}
                      <template #title>
                        {{ $tl('p.retainDaysForArtifacts') }}
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
                  {{ $tl('p.excludePublish') }}
                  <template #title>
                    <ul>
                      <li>{{ $tl('p.excludePublishAntExpression') }}</li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input v-model:value="tempExtraData.excludeReleaseAnt" :placeholder="$tl('p.exclusionForPublish')" />
            </a-form-item>
          </div>
        </a-form>
      </a-card>
    </a-spin>
    <!-- 选择仓库 -->
    <a-drawer
      destroy-on-close
      :title="`${$tl('c.chooseRepository')}`"
      placement="right"
      :open="repositoryisible"
      width="85vw"
      :z-index="1009"
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
            {{ $tl('c.cancel') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['repository'].handerConfirm()
              }
            "
          >
            {{ $tl('c.confirm') }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 选择脚本 -->
    <a-drawer
      destroy-on-close
      :title="`${$tl('c.chooseScript')}`"
      placement="right"
      :open="chooseScriptVisible != 0"
      width="70vw"
      :z-index="1009"
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
            : temp.script && temp.script.indexOf('$ref.script.') != -1
              ? temp.script.replace('$ref.script.')
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
            {{ $tl('c.cancel') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptPage'].handerConfirm()
              }
            "
          >
            {{ $tl('c.confirm') }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 查看容器 -->
    <a-drawer
      destroy-on-close
      :title="`${$tl('p.viewContainer')}`"
      placement="right"
      :open="dockerListVisible != 0"
      width="70vw"
      :z-index="1009"
      @close="
        () => {
          dockerListVisible = 0
        }
      "
    >
      <docker-list v-if="dockerListVisible" ref="dockerlist"></docker-list>
    </a-drawer>

    <!-- 查看命令示例 -->
    <a-modal
      v-model:open="viewScriptTemplVisible"
      destroy-on-close
      width="50vw"
      :title="$tl('p.buildCommandExample')"
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
    </a-modal>
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
  getBuildGet
} from '@/api/build-info'
import { getSshListAll } from '@/api/ssh'
import { getRepositoryInfo } from '@/api/repository'
import { getNodeListAll, getProjectListAll } from '@/api/node'
// import { getScriptListAll } from "@/api/server-script";
import { getDishPatchListAll } from '@/api/dispatch'
import { itemGroupBy, CRON_DATA_SOURCE, randomStr } from '@/utils/const'

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
  emits: ['confirm', 'update:editSteps'],
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
      viewScriptTemplVisible: false,
      buildScipts: [
        {
          title: 'Java 项目(示例参考，具体还需要根据项目实际情况来决定)',
          children: [
            {
              title: '不执行，也不编译测试用例 mvn clean package -Dmaven.test.skip=true',
              value: 'mvn clean package -Dmaven.test.skip=true'
            },
            {
              title: '打包生产环境包 mvn clean package -Dmaven.test.skip=true -Pprod',
              value: 'mvn clean package -Dmaven.test.skip=true -Pprod'
            },
            {
              title: '打包测试环境包 mvn clean package -Dmaven.test.skip=true -Ptest',
              value: 'mvn clean package -Dmaven.test.skip=true -Ptest'
            },
            {
              title: '不执行，但是编译测试用例 mvn clean package -DskipTests',
              value: 'mvn clean package -DskipTests'
            },
            {
              title: 'mvn clean package',
              value: 'mvn clean package'
            },
            {
              title: '指定 pom 文件打包 mvn -f xxx/pom.xml clean package',
              value: 'mvn -f xxx/pom.xml clean package'
            },
            {
              title: '指定 settings 文件打包 mvn -s xxx/settings.xml clean package',
              value: 'mvn -s xxx/settings.xml clean package'
            }
          ]
        },
        {
          title: 'vue 项目(示例参考，具体还需要根据项目实际情况来决定)',
          children: [
            {
              title: '需要先安装依赖 npm i && npm run build',
              value: 'npm i && npm run build'
            },
            {
              title: '打包正式环境 npm i && npm run build:prod',
              value: 'npm i && npm run build:prod'
            },
            {
              title: '打包预发布环境 npm i && npm run build:stage',
              value: 'npm i && npm run build:stage'
            },
            {
              title: '需要先安装依赖 yarn && yarn run build',
              value: 'yarn && yarn run build'
            },
            {
              title: '指定目录打包 yarn && yarn --cwd xxx build',
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
      dockerSwarmList: [],
      //集群下 服务下拉数据
      swarmServiceListOptions: [],
      // scriptList: [],
      groupList: [],
      temp: {},
      rules: {
        name: [{ required: true, message: '请填写构建名称', trigger: 'blur' }],
        buildMode: [{ required: true, message: '请选择构建方式', trigger: 'blur' }],
        releaseMethod: [{ required: true, message: '请选择发布操作', trigger: 'blur' }],
        branchName: [{ required: true, message: '请选择分支', trigger: 'blur' }],
        script: [{ required: true, message: '请填写构建命令', trigger: 'blur' }],
        resultDirFile: [{ required: true, message: '请填写产物目录', trigger: 'blur' }],
        // releasePath: [{ required: true, message: '请填写发布目录', trigger: 'blur' }],
        repositoryId: [
          {
            required: true,
            message: this.$tl('p.repositorySelection'),
            trigger: 'blur'
          }
        ]
      },
      rulesSteps: [
        ['buildMode'],
        ['name', 'branchName', 'repositoryId'],
        ['script', 'resultDirFile'],
        ['releaseMethod']
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
          title: this.$tl('c.buildMethod')
        },
        {
          title: this.$tl('p.basicInfo')

          // status: 'process'
        },
        {
          title: this.$tl('p.buildProcess')

          // status: 'wait'
        },
        {
          title: this.$tl('c.publishOperation')

          // status: 'wait'
        },
        {
          title: this.$tl('p.additionalConfig')

          // status: 'wait'
        }
      ],
      dslDefault:
        '# 基础镜像 目前仅支持 ubuntu-latest\n' +
        'runsOn: ubuntu-latest\n' +
        '# 使用哪个 docker 构建,填写 docker 标签 默认查询可用的第一个,如果 tag 查询出多个也选择第一个结果\n' +
        'fromTag: xxx\n' +
        '# version 需要在对应镜像源中存在\n' +
        '# java 镜像源 https://mirrors.tuna.tsinghua.edu.cn/Adoptium/\n' +
        '# maven 镜像源 https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/\n' +
        '# node 镜像源 https://registry.npmmirror.com/-/binary/node/\n' +
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
        '# 将容器中的 maven 仓库文件缓存到 docker 卷中\n' +
        '  - uses: cache\n' +
        '    path: /root/.m2\n' +
        '# 将此目录缓存到全局（多个构建可以共享此缓存目录）\n' +
        '    type: global\n' +
        '# 将容器中的 node_modules 文件缓存到 docker 卷中\n' +
        '  - uses: cache\n' +
        '    path: ${JPOM_WORKING_DIR}/web-vue/node_modules\n' +
        '# 使用 copy 文件的方式缓存，反之使用软链的形式。copy 文件方式缓存 node_modules 可以避免 npm WARN reify Removing non-directory\n' +
        '    mode: copy\n' +
        '  - run: npm config set registry https://registry.npmmirror.com\n' +
        '# 内置变量 ${JPOM_WORKING_DIR} ${JPOM_BUILD_ID}\n' +
        '  - run: cd  ${JPOM_WORKING_DIR}/web-vue && npm i && npm run build\n' +
        '  - run: cd ${JPOM_WORKING_DIR} && mvn package -s script/settings.xml\n' +
        '# 宿主机目录和容器目录挂载 /host:/container:ro\n' +
        '# binds:\n' +
        '#  - /Users/user/.m2/settings.xml:/root/.m2/\n' +
        '# 宿主机文件上传到容器 /host:/container:true\n' +
        '# dirChildrenOnly = true will create /var/data/titi and /var/data/tata dirChildrenOnly = false will create /var/data/root/titi and /var/data/root/tata\n' +
        '# copy:\n' +
        '#  - /Users/user/.m2/settings.xml:/root/.m2/:false\n' +
        '# 给容器新增环境变量\n' +
        'env:\n' +
        '  NODE_OPTIONS: --max-old-space-size=900\n' +
        '#配置说明：https://docs.docker.com/engine/api/v1.43/#tag/Container/operation/ContainerCreate\n' +
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
    buildModeArray() {
      return Object.keys(this.buildModeMap).map((item) => {
        return {
          value: parseInt(item),
          disabled: parseInt(item) === 0 && this.getExtendPlugins.indexOf('inDocker') > -1,
          name: this.buildModeMap[item]
        }
      })
    }
  },
  watch: {
    editSteps: {
      handler(v) {
        this.stepsCurrent = v
      },
      immediate: true
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
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.build.edit.${key}`, ...args)
    },
    randomStr,
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
      this.temp = { resultKeepDay: 0 }
      this.branchList = []
      // this.tempRepository = {};
      // this.loadRepositoryList();
      this.loadDispatchList()
      this.loadNodeProjectList()
      this.loadSshList()
      this.loadDockerSwarmListAll()
      // this.loadScriptListList();

      this.tempExtraData = {
        cacheBuild: true,
        saveBuildFile: true,
        resultKeepCount: 0,
        fileStorageKeepDay: 0
      }
      this.$refs['editBuildForm']?.resetFields()
    },
    // 修改
    handleEdit(record) {
      this.$refs['editBuildForm']?.resetFields()
      this.temp = Object.assign({}, record)
      this.temp.buildMode = this.temp.buildMode || 0
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
      }
      this.tempExtraData = { ...this.tempExtraData }
      this.changeRepositpry(true)

      this.loadDispatchList()
      this.loadDockerSwarmListAll()
      this.loadNodeProjectList()
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
              item.label = findArra.length ? findArra[0].name : this.$tl('p.unknown')
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
      // 检验表单
      this.$refs['editBuildForm']
        .validate()
        .then(() => {
          const tempExtraData = Object.assign({}, this.tempExtraData)
          // 设置参数
          if (this.temp.releaseMethod === 2) {
            if (this.temp.releaseMethodDataIdList.length < 2) {
              $notification.warn({
                message: this.$tl('p.nodeProjectSelection')
              })
              return false
            }
            tempExtraData.releaseMethodDataId_2_node = this.temp.releaseMethodDataIdList[0]
            tempExtraData.releaseMethodDataId_2_project = this.temp.releaseMethodDataIdList[1]
          } else if (this.temp.releaseMethod === 3) {
            //  (this. tempExtraData.releasePath || '').slice(releaseSshDir.length);
            tempExtraData.releasePath = (
              (tempExtraData.releaseSshDir || '') +
              '/' +
              (tempExtraData.releasePath2 || '')
            ).replace(new RegExp('//', 'gm'), '/')
            tempExtraData.releaseMethodDataId_3 = (tempExtraData.releaseMethodDataId_3 || []).join(',')
          }

          this.temp = {
            ...this.temp,
            extraData: JSON.stringify(tempExtraData),
            resultKeepDay: this.temp.resultKeepDay || 0
          }
          // 提交数据
          editBuild(this.temp).then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              //
              this.$emit('confirm', build, res.data, this.temp.buildEnvParameter)
            }
          })
        })
        .catch(({ errorFields }) => {
          if (errorFields && errorFields[0]) {
            const msg = errorFields[0].errors && errorFields[0].errors[0]
            if (msg) {
              $notification.warn({
                message: msg
              })
              // console.log(error)
            }
            // 切换到对应的流程
            const filedName = errorFields[0].name && errorFields[0].name[0]
            filedName &&
              this.rulesSteps.forEach((item, index) => {
                if (item.includes(filedName)) {
                  this.stepsChange(index)
                }
              })
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
      if (e.target.value === 1) {
        this.loadDockerAllTag()
      }
      this.temp = { ...this.temp, script: '' }
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
