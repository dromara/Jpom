<template>
  <div>
    <a-spin :tip="$t('pages.build.edit.272043cf')" :spinning="loading">
      <a-card>
        <template #title>
          <a-steps v-model:current="stepsCurrent" size="small" :items="stepsItems" @change="stepsChange"></a-steps>
        </template>
        <a-form ref="editBuildForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
          <div v-show="stepsCurrent === 0">
            <a-alert :message="$t('pages.build.edit.f638b7f7')" type="info" show-icon>
              <template #description>
                <ul>
                  <li>
                    {{ $t('pages.build.edit.d5256619') }}
                    <ul>
                      <li>{{ $t('pages.build.edit.4c2c37a3') }}</li>
                      <li>{{ $t('pages.build.edit.3b2b0735') }}</li>
                      <li>{{ $t('pages.build.edit.a54f9296') }}</li>
                    </ul>
                  </li>
                  <li>
                    {{ $t('pages.build.edit.d248a200') }}
                    <ul>
                      <li>{{ $t('pages.build.edit.4b41f3ba') }}</li>
                    </ul>
                  </li>

                  <li>{{ $t('pages.build.edit.3c46c32c') }}</li>
                  <li v-if="getExtendPlugins.indexOf('inDocker') > -1" style="color: red">
                    {{ $t('pages.build.edit.acf9debd') }}
                  </li>
                </ul>
              </template>
            </a-alert>
            <a-form-item :name="['buildMode']">
              <template #label> {{ $t('pages.build.edit.b5dfedd9') }} </template>
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
                    {{ $t('pages.build.edit.e50d0e17') }}
                  </a-button>
                </template>
                <template #label> {{ $t('pages.build.edit.9dbe9df') }} </template>
                <a-spin :tip="$t('pages.build.edit.b46f11d2')" :spinning="dockerAllTagLoading">
                  <a-space v-if="dockerAllTagList && dockerAllTagList.length">
                    <a-tag v-for="(item, index) in dockerAllTagList" :key="index">{{ item }}</a-tag>
                  </a-space>
                  <span v-else style="color: red; font-weight: bold">{{ $t('pages.build.edit.1eb28553') }}</span>
                </a-spin>
              </a-form-item>
              <a-alert :message="$t('pages.build.edit.827a52c4')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('pages.build.edit.33ba7e9') }}</li>
                    <li>{{ $t('pages.build.edit.9a32f653') }}</li>
                    <li>
                      {{ $t('pages.build.edit.ed35c6c5') }} <b style="color: red">fromTag</b>
                      {{ $t('pages.build.edit.73515366') }}
                    </li>
                  </ul>
                </template>
              </a-alert>
            </template>
          </div>
          <div v-show="stepsCurrent === 1">
            <a-form-item :label="$t('pages.build.edit.3e34ec28')" name="name">
              <a-row>
                <a-col :span="10">
                  <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('pages.build.edit.3e34ec28')" />
                </a-col>
                <a-col :span="4" style="text-align: right">{{ $t('pages.build.edit.12d0e469') }}</a-col>

                <a-col :span="10">
                  <a-form-item-rest>
                    <custom-select
                      v-model:value="temp.group"
                      :max-length="50"
                      :data="groupList"
                      :input-placeholder="$t('pages.build.edit.c50ead9c')"
                      :select-placeholder="$t('pages.build.edit.761c903a')"
                    >
                    </custom-select>
                  </a-form-item-rest>
                </a-col>
              </a-row>
            </a-form-item>
            <a-form-item :label="$t('pages.build.edit.1beebcf2')" name="repositoryId">
              <a-input-search
                :value="`${
                  tempRepository
                    ? tempRepository.name + '[' + tempRepository.gitUrl + ']'
                    : $t('pages.build.edit.d62c383b')
                }`"
                read-only
                :placeholder="$t('pages.build.edit.d62c383b')"
                :enter-button="$t('pages.build.edit.e95bc1a3')"
                @search="
                  () => {
                    repositoryisible = true
                  }
                "
              />
            </a-form-item>
            <template v-if="tempRepository && tempRepository.repoType === 0">
              <a-form-item :label="$t('pages.build.edit.5cb4b684')" name="branchName">
                <a-row>
                  <a-col :span="10">
                    <custom-select
                      v-model:value="temp.branchName"
                      :disabled="temp.branchTagName ? true : false"
                      :data="branchList"
                      :can-reload="true"
                      :input-placeholder="$t('pages.build.edit.8f70dedd')"
                      :select-placeholder="$t('pages.build.edit.aa5164b0')"
                      @on-refresh-select="loadBranchList"
                      @change="
                        () => {
                          $refs['editBuildForm'] && $refs['editBuildForm'].clearValidate()
                        }
                      "
                    >
                      <template #inputTips>
                        <div>
                          {{ $t('pages.build.edit.c0b8ef64') }}(AntPathMatcher)
                          <ul>
                            <li>? {{ $t('pages.build.edit.8bef7f5') }}</li>
                            <li>* {{ $t('pages.build.edit.49902a8e') }}</li>
                            <li>** {{ $t('pages.build.edit.6bd4dac4') }}</li>
                          </ul>
                        </div>
                      </template>
                    </custom-select>
                  </a-col>
                  <a-col :span="4" style="text-align: right"> {{ $t('pages.build.edit.83d5a14e') }}(TAG)：</a-col>
                  <a-col :span="10">
                    <a-form-item-rest>
                      <custom-select
                        v-model:value="temp.branchTagName"
                        :data="branchTagList"
                        :can-reload="true"
                        :input-placeholder="$t('pages.build.edit.b328c96c')"
                        :select-placeholder="$t('pages.build.edit.a9be5a35')"
                        @on-refresh-select="loadBranchList"
                        @change="
                          () => {
                            $refs['editBuildForm'] && $refs['editBuildForm'].clearValidate()
                          }
                        "
                      >
                        <template #inputTips>
                          <div>
                            {{ $t('pages.build.edit.c0b8ef64') }}(AntPathMatcher)
                            <ul>
                              <li>? {{ $t('pages.build.edit.8bef7f5') }}</li>
                              <li>* {{ $t('pages.build.edit.49902a8e') }}</li>
                              <li>** {{ $t('pages.build.edit.6bd4dac4') }}</li>
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
                :label="$t('pages.build.edit.7cc9f1bf')"
                name="cloneDepth"
              >
                <a-input-number
                  v-model:value="tempExtraData.cloneDepth"
                  style="width: 100%"
                  :placeholder="$t('pages.build.edit.718275d4')"
                />
              </a-form-item>
            </template>
          </div>

          <!-- 构建流程 -->
          <div v-show="stepsCurrent === 2">
            <a-form-item v-if="temp.buildMode === 0" name="script">
              <template #label>
                <a-tooltip>
                  {{ $t('pages.build.edit.264f7e9c') }}
                  <template #title>
                    {{ $t('pages.build.edit.2682adae') }}<b>{{ $t('pages.build.edit.bf8bfc14') }} </b
                    >{{ $t('pages.build.edit.c88ccc82') }}
                    <b>cd xxx && mvn clean package</b>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <template #help>
                {{ $t('pages.build.edit.a4a03ee3') }}
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
                        {{ $t('pages.build.edit.24573653') }}
                      </a-button>
                      <a-button
                        type="link"
                        @click="
                          () => {
                            chooseScriptVisible = 2
                          }
                        "
                      >
                        {{ $t('pages.build.edit.c086fc82') }}
                      </a-button>
                    </a-space>
                  </template>
                </code-editor>
              </a-form-item-rest>
            </a-form-item>
            <a-form-item v-if="temp.buildMode === 1" name="script">
              <template #label>
                <a-tooltip>
                  DSL {{ $t('pages.build.edit.99ff48c8') }}
                  <template #title>
                    <p>{{ $t('pages.build.edit.be74afcb') }}</p>
                    <ul>
                      <li>{{ $t('pages.build.edit.277dfe71') }}</li>
                      <li>{{ $t('pages.build.edit.507acee7') }}</li>
                      <li>{{ $t('pages.build.edit.ce1e5b44') }}</li>
                    </ul>
                    <div>
                      {{ $t('pages.build.edit.b9196bd2') }}
                      <ol>
                        <li>
                          {{ $t('pages.build.edit.20103a68') }}
                        </li>
                        <li>{{ $t('pages.build.edit.57170afe') }}</li>
                        <li>{{ $t('pages.build.edit.c7a8176f') }}</li>
                        <li>
                          {{ $t('pages.build.edit.b0af27f9') }}
                        </li>
                        <li>
                          {{ $t('pages.build.edit.f8d3b0b6') }}
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
                  :placeholder="$t('pages.build.edit.5303e645')"
                >
                  <template #tool_before>
                    <a-segmented
                      v-model:value="dslEditTabKey"
                      :options="[
                        {
                          label: `DSL ${$t('pages.build.edit.28f9e270')}`,
                          value: 'content'
                        },
                        { label: $t('pages.build.edit.36177d5c'), value: 'demo' }
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
                          label: `DSL ${$t('pages.build.edit.28f9e270')}`,
                          value: 'content'
                        },
                        { label: $t('pages.build.edit.36177d5c'), value: 'demo' }
                      ]"
                    />
                  </template> </code-editor
              ></a-form-item-rest>
            </a-form-item>
            <a-form-item v-if="temp.buildMode !== undefined" name="resultDirFile" class="jpom-target-dir">
              <template #label>
                <a-tooltip>
                  {{ $t('pages.build.edit.ed762599') }}
                  <template #title>
                    <div>
                      {{ $t('pages.build.edit.9374a2e6') }}
                      <b>mvn clean package</b> {{ $t('pages.build.edit.a7df35c') }}
                      <b> modules/server/target/server-2.4.2-release</b>
                    </div>
                    <div><br /></div>
                    <!-- 只有本地构建支持 模糊匹配 -->
                    <div v-if="temp.buildMode === 0">
                      {{ $t('pages.build.edit.c0b8ef64') }}(AntPathMatcher){{ $t('pages.build.edit.507fac16') }}
                      <ul>
                        <li>? {{ $t('pages.build.edit.8bef7f5') }}</li>
                        <li>* {{ $t('pages.build.edit.49902a8e') }}</li>
                        <li>** {{ $t('pages.build.edit.6bd4dac4') }}</li>
                      </ul>
                    </div>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input
                v-model:value="temp.resultDirFile"
                :max-length="200"
                :placeholder="$t('pages.build.edit.908d376f')"
              />
            </a-form-item>

            <a-alert v-if="temp.buildMode === undefined" :message="$t('pages.build.edit.89aba1d1')" banner />
            <template v-else>
              <a-form-item :label="$t('pages.build.edit.c81b2c2e')" name="buildEnvParameter">
                <a-textarea
                  v-model:value="temp.buildEnvParameter"
                  :placeholder="$t('pages.build.edit.97d35a9f')"
                  :auto-size="{ minRows: 3, maxRows: 5 }"
                />
              </a-form-item>
              <a-form-item :label="$t('pages.build.edit.c41bc440')" name="commandExecMode">
                <a-radio-group v-model:value="tempExtraData.commandExecMode" button-style="solid">
                  <a-radio-button value="default">{{ $t('pages.build.edit.846478be') }}</a-radio-button>
                  <a-radio-button value="apache_exec">{{ $t('pages.build.edit.c48dabf9') }}</a-radio-button>
                </a-radio-group>
                <template #help>{{ $t('pages.build.edit.cf7b8625') }}</template>
              </a-form-item>
            </template>
          </div>

          <div v-show="stepsCurrent === 3">
            <a-form-item name="releaseMethod">
              <template #label>
                <a-tooltip>
                  {{ $t('pages.build.edit.7be4f983') }}
                  <template #title>
                    <ul>
                      <li>{{ $t('pages.build.edit.b86d61cb') }}</li>
                      <li>{{ $t('pages.build.edit.21643071') }}</li>
                      <li>{{ $t('pages.build.edit.566300e7') }}</li>
                      <li>
                        {{ $t('pages.build.edit.c8079544') }}
                      </li>
                      <li>{{ $t('pages.build.edit.bf00a5d2') }}</li>
                      <li>
                        {{ $t('pages.build.edit.2609f468') }}
                      </li>
                      <li>{{ $t('pages.build.edit.510ec4fe') }}</li>
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
              {{ $t('pages.build.edit.43706169') }}
            </div>
            <template v-else>
              <template v-if="temp.releaseMethod === 0">
                {{ $t('pages.build.edit.b31e12fe') }},{{ $t('pages.build.edit.e3e80b89') }}
              </template>
              <!-- 节点分发 -->
              <template v-if="temp.releaseMethod === 1">
                <a-form-item :label="$t('pages.build.edit.d935498a')" name="releaseMethodDataId">
                  <a-select
                    v-model:value="tempExtraData.releaseMethodDataId_1"
                    show-search
                    allow-clear
                    :placeholder="$t('pages.build.edit.145023e0')"
                  >
                    <a-select-option v-for="dispatch in dispatchList" :key="dispatch.id"
                      >{{ dispatch.name }}
                    </a-select-option>
                    <template #suffixIcon>
                      <ReloadOutlined @click="loadDispatchList" />
                    </template>
                  </a-select>
                </a-form-item>
                <a-form-item name="projectSecondaryDirectory" :label="$t('pages.build.edit.b07e882b')">
                  <a-input
                    v-model:value="tempExtraData.projectSecondaryDirectory"
                    :placeholder="$t('pages.build.edit.d5deb410')"
                  />
                </a-form-item>
              </template>

              <!-- 项目 -->
              <template v-if="temp.releaseMethod === 2">
                <a-form-item :label="$t('pages.build.edit.1fc809b5')" name="releaseMethodDataIdList">
                  <a-cascader
                    v-model:value="temp.releaseMethodDataIdList"
                    :options="cascaderList"
                    :placeholder="$t('pages.build.edit.496f23b1')"
                  >
                    <template #suffixIcon>
                      <ReloadOutlined @click="loadNodeProjectList" />
                    </template>
                  </a-cascader>
                </a-form-item>
                <a-form-item :label="$t('pages.build.edit.23d7171a')" name="afterOpt">
                  <a-select
                    v-model:value="tempExtraData.afterOpt"
                    show-search
                    allow-clear
                    :placeholder="$t('pages.build.edit.70a4cf1e')"
                  >
                    <a-select-option v-for="opt in afterOptListSimple" :key="opt.value">{{
                      opt.title
                    }}</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item name="projectSecondaryDirectory" :label="$t('pages.build.edit.b07e882b')">
                  <a-input
                    v-model:value="tempExtraData.projectSecondaryDirectory"
                    :placeholder="$t('pages.build.edit.63b7357e')"
                  />
                </a-form-item>
              </template>
              <!-- SSH -->
              <template v-if="temp.releaseMethod === 3">
                <a-form-item name="releaseMethodDataId" :help="$t('pages.build.edit.8ef4635d')">
                  <template #label>
                    <a-tooltip>
                      {{ $t('pages.build.edit.324c2f16') }}
                      <template #title>
                        {{ $t('pages.build.edit.8ef4635d') }}
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
                        :placeholder="$t('pages.build.edit.b8bf68b')"
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
                <a-form-item name="releaseMethodDataId" :help="$t('pages.build.edit.8d23f3a0')">
                  <template #label>
                    <a-tooltip :title="$t('pages.build.edit.8d23f3a0')">
                      {{ $t('pages.build.edit.e0389a4e') }}
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-input-group compact>
                    <a-select
                      v-model:value="tempExtraData.releaseSshDir"
                      show-search
                      allow-clear
                      style="width: 30%"
                      :placeholder="$t('pages.build.edit.b8bf68b')"
                    >
                      <a-select-option v-for="item in selectSshDirs" :key="item">
                        <a-tooltip :title="item">{{ item }}</a-tooltip>
                      </a-select-option>
                    </a-select>
                    <a-form-item-rest>
                      <a-input
                        v-model:value="tempExtraData.releasePath2"
                        style="width: 70%"
                        :placeholder="$t('pages.build.edit.ec3e7305')"
                      />
                    </a-form-item-rest>
                  </a-input-group>
                </a-form-item>
              </template>

              <a-form-item v-if="temp.releaseMethod === 3" name="releaseBeforeCommand">
                <!-- sshCommand -->
                <template #label>
                  <a-tooltip>
                    {{ $t('pages.build.edit.8ed8f471') }}
                    <template #title>
                      {{ $t('pages.build.edit.4001461f') }}
                      <ul>
                        <li>{{ $t('pages.build.edit.d90817a5') }}</li>
                        <li>{{ $t('pages.build.edit.ae0f2733') }}</li>
                      </ul>
                    </template>
                    <QuestionCircleOutlined v-if="!temp.id" />
                  </a-tooltip>
                </template>
                <template #help>
                  {{ $t('pages.build.edit.f8795640') }}
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
                      <a-tag>{{ $t('pages.build.edit.f3096daa') }}</a-tag></template
                    >
                  </code-editor>
                </a-form-item-rest>
              </a-form-item>
              <a-form-item v-if="temp.releaseMethod === 3 || temp.releaseMethod === 4" name="releaseCommand">
                <!-- sshCommand LocalCommand -->
                <template #label>
                  <a-tooltip>
                    {{ $t('pages.build.edit.51d17d70') }}
                    <template #title>
                      {{ $t('pages.build.edit.3bf075cd') }}
                      <ul>
                        <li>{{ $t('pages.build.edit.a2f92477') }}</li>
                        <li>{{ $t('pages.build.edit.d5fe14e1') }}</li>
                      </ul>
                    </template>

                    <QuestionCircleOutlined v-if="!temp.id" />
                  </a-tooltip>
                </template>
                <template #help>
                  {{ $t('pages.build.edit.48a510df') }}
                </template>
                <a-form-item-rest>
                  <code-editor
                    v-model:content="tempExtraData.releaseCommand"
                    height="40vh"
                    :show-tool="true"
                    :options="{ mode: 'shell' }"
                  >
                    <template #tool_before>
                      <a-tag>{{ $t('pages.build.edit.ae45b99') }}</a-tag></template
                    >
                  </code-editor>
                </a-form-item-rest>
              </a-form-item>

              <a-form-item v-if="temp.releaseMethod === 2 || temp.releaseMethod === 3" name="clearOld">
                <template #label>
                  <a-tooltip>
                    {{ $t('pages.build.edit.fa8d15d') }}
                    <template #title>
                      {{ $t('pages.build.edit.54849cc4') }}
                    </template>
                    <QuestionCircleOutlined v-if="!temp.id" />
                  </a-tooltip>
                </template>
                <a-form-item-rest>
                  <a-row>
                    <a-col :span="4">
                      <a-switch
                        v-model:checked="tempExtraData.clearOld"
                        :checked-children="$t('pages.build.edit.d2fbce36')"
                        :un-checked-children="$t('pages.build.edit.1c77d6fb')"
                      />
                    </a-col>
                    <template v-if="temp.releaseMethod === 2">
                      <a-col :span="4" style="text-align: right">
                        <a-tooltip>
                          {{ $t('pages.build.edit.5ac78351') }}
                          <template #title>
                            {{ $t('pages.build.edit.a0286c94') }}
                            <ul>
                              <li>
                                {{ $t('pages.build.edit.39213d2e') }}
                              </li>
                              <li>{{ $t('pages.build.edit.4e260db8') }}</li>
                            </ul>
                          </template>
                          <QuestionCircleOutlined v-if="!temp.id" />
                        </a-tooltip>
                      </a-col>
                      <a-col :span="4">
                        <a-switch
                          v-model:checked="tempExtraData.diffSync"
                          :checked-children="$t('pages.build.edit.d2fbce36')"
                          :un-checked-children="$t('pages.build.edit.1c77d6fb')"
                        />
                      </a-col>
                      <a-col :span="4" style="text-align: right">
                        <a-tooltip>
                          {{ $t('pages.build.edit.d6cd4b4') }}
                          <template #title>
                            {{ $t('pages.build.edit.13c98505') }}
                          </template>
                          <QuestionCircleOutlined v-if="!temp.id" />
                        </a-tooltip>
                      </a-col>
                      <a-col :span="4">
                        <a-switch
                          v-model:checked="tempExtraData.projectUploadCloseFirst"
                          :checked-children="$t('pages.build.edit.d2fbce36')"
                          :un-checked-children="$t('pages.build.edit.1c77d6fb')"
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
                      {{ $t('pages.build.edit.96f8e2b0') }}
                      <template #title>
                        {{ $t('pages.build.edit.6abeeb6a') }}
                      </template>
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-input v-model:value="tempExtraData.fromTag" :placeholder="$t('pages.build.edit.21b4e4a5')" />
                </a-form-item>

                <a-tooltip :title="$t('pages.build.edit.83573626')">
                  <a-form-item name="dockerfile" label="Dockerfile">
                    <a-input v-model:value="tempExtraData.dockerfile" :placeholder="$t('pages.build.edit.301d915f')" />
                  </a-form-item>
                </a-tooltip>
                <a-form-item name="dockerTag" :label="$t('pages.build.edit.5af27a88')">
                  <a-tooltip :title="$t('pages.build.edit.e83d88b9')">
                    <a-input v-model:value="tempExtraData.dockerTag" :placeholder="$t('pages.build.edit.a95bf9ab')" />
                  </a-tooltip>
                </a-form-item>
                <a-form-item name="dockerBuildArgs" :label="$t('pages.build.edit.6d85135e')">
                  <a-row>
                    <a-col :span="10">
                      <a-tooltip :title="$t('pages.build.edit.ebcb5889')">
                        <a-input
                          v-model:value="tempExtraData.dockerBuildArgs"
                          :placeholder="$t('pages.build.edit.ead1cb46')"
                        />
                      </a-tooltip>
                    </a-col>
                    <a-col :span="4" style="text-align: right">{{ $t('pages.build.edit.f8373663') }}</a-col>
                    <a-col :span="10">
                      <a-form-item-rest>
                        <a-tooltip :title="$t('pages.build.edit.6ae9b418')">
                          <a-input
                            v-model:value="tempExtraData.dockerImagesLabels"
                            :placeholder="$t('pages.build.edit.190b5c5e')"
                          /> </a-tooltip
                      ></a-form-item-rest>
                    </a-col>
                  </a-row>
                </a-form-item>
                <a-form-item name="swarmId">
                  <template #label>
                    <a-tooltip>
                      {{ $t('pages.build.edit.d5beb0cf') }}
                      <template #title>
                        {{ $t('pages.build.edit.f50958d4') }}
                      </template>
                      <QuestionCircleOutlined v-if="!temp.id" />
                    </a-tooltip>
                  </template>
                  <a-select
                    v-model:value="tempExtraData.dockerSwarmId"
                    show-search
                    allow-clear
                    :placeholder="$t('pages.build.edit.4ebd6b12')"
                    @change="selectSwarm()"
                  >
                    <a-select-option value="">{{ $t('pages.build.edit.515f42e5') }}</a-select-option>
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
                            {{ $t('pages.build.edit.2098cd5f') }}
                            <template #title>
                              {{ $t('pages.build.edit.e5cfe61c') }}
                            </template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.pushToRepository"
                            :checked-children="$t('pages.build.edit.d2fbce36')"
                            :un-checked-children="$t('pages.build.edit.1c77d6fb')"
                          />
                        </a-space>
                      </a-col>
                      <a-col :span="6" style="text-align: right">
                        <a-space>
                          <a-tooltip>
                            {{ $t('pages.build.edit.b80c07b9') }}
                            <template #title>
                              {{ $t('pages.build.edit.fcb6e167') }}
                            </template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.dockerTagIncrement"
                            :checked-children="$t('pages.build.edit.d2fbce36')"
                            :un-checked-children="$t('pages.build.edit.1c77d6fb')"
                          />
                        </a-space>
                      </a-col>
                      <a-col :span="6" style="text-align: right">
                        <a-space>
                          <a-tooltip>
                            no-cache
                            <template #title>{{ $t('pages.build.edit.4ad1689a') }} </template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.dockerNoCache"
                            :checked-children="$t('pages.build.edit.d2fbce36')"
                            :un-checked-children="$t('pages.build.edit.1c77d6fb')"
                          />
                        </a-space>
                      </a-col>
                      <a-col :span="6" style="text-align: right">
                        <a-space>
                          <a-tooltip>
                            {{ $t('pages.build.edit.a2b28fd3') }}
                            <template #title>{{ $t('pages.build.edit.eab8e65b') }} </template>
                            <QuestionCircleOutlined v-if="!temp.id" />
                          </a-tooltip>

                          <a-switch
                            v-model:checked="tempExtraData.dockerBuildPull"
                            :checked-children="$t('pages.build.edit.d2fbce36')"
                            :un-checked-children="$t('pages.build.edit.1c77d6fb')"
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
                        {{ $t('pages.build.edit.5dd8de32') }}
                        <template #title>
                          {{ $t('pages.build.edit.ee2fca') }}
                        </template>
                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                    </template>
                    <a-select
                      v-model:value="tempExtraData.dockerSwarmServiceName"
                      allow-clear
                      :placeholder="$t('pages.build.edit.c2dca952')"
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
                  {{ $t('pages.build.edit.fefad82f') }}
                  <template #title>
                    {{ $t('pages.build.edit.5756ab9a') }}
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-form-item-rest>
                <a-row>
                  <a-col :span="2">
                    <a-tooltip :title="$t('pages.build.edit.a3affb5c')">
                      <a-switch
                        v-model:checked="tempExtraData.cacheBuild"
                        :checked-children="$t('pages.build.edit.d2fbce36')"
                        :un-checked-children="$t('pages.build.edit.1c77d6fb')"
                      />
                    </a-tooltip>
                  </a-col>
                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        {{ $t('pages.build.edit.57a58211') }}
                        <template #title> {{ $t('pages.build.edit.47d0ed86') }} </template>

                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-switch
                        v-model:checked="tempExtraData.saveBuildFile"
                        :checked-children="$t('pages.build.edit.d2fbce36')"
                        :un-checked-children="$t('pages.build.edit.1c77d6fb')"
                      />
                    </a-space>
                  </a-col>

                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        {{ $t('pages.build.edit.8692041e') }}
                        <template #title>
                          {{ $t('pages.build.edit.e8e74466') }}
                        </template>

                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-switch
                        v-model:checked="tempExtraData.checkRepositoryDiff"
                        :checked-children="$t('pages.build.edit.d2fbce36')"
                        :un-checked-children="$t('pages.build.edit.1c77d6fb')"
                      />
                    </a-space>
                  </a-col>
                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        {{ $t('pages.build.edit.419b3529') }}
                        <template #title>
                          {{ $t('pages.build.edit.82be0bf8') }}
                        </template>
                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-switch
                        v-model:checked="tempExtraData.strictlyEnforce"
                        :checked-children="$t('pages.build.edit.d2fbce36')"
                        :un-checked-children="$t('pages.build.edit.1c77d6fb')"
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
                      <li>{{ $t('pages.build.edit.3b8c1c97') }}</li>
                      <li>{{ $t('pages.build.edit.a2854d2d') }}</li>
                      <li>{{ $t('pages.build.edit.d5827dbb') }}</li>
                      <li>{{ $t('pages.build.edit.4be6e818') }}</li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input v-model:value="temp.webhook" :placeholder="$t('pages.build.edit.c73fae64')" />
            </a-form-item>
            <a-form-item :label="$t('pages.build.edit.3f5140f1')" name="autoBuildCron">
              <a-auto-complete
                v-model:value="temp.autoBuildCron"
                :placeholder="$t('pages.build.edit.50fcce7a')"
                :options="CRON_DATA_SOURCE"
              >
                <template #option="item"> {{ item.title }} {{ item.value }} </template>
              </a-auto-complete>
            </a-form-item>
            <a-form-item name="noticeScriptId">
              <template #label>
                <a-tooltip>
                  {{ $t('pages.build.edit.ff095cb0') }}
                  <template #title>
                    <ul>
                      <li>{{ $t('pages.build.edit.d8d80300') }}</li>
                      <li>
                        {{ $t('pages.build.edit.41d152ba') }}
                      </li>
                      <li>{{ $t('pages.build.edit.36d6622c') }}</li>
                      <li>
                        <b>{{ $t('pages.build.edit.a8b2f78f') }}</b>
                      </li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input-search
                :value="`${
                  tempExtraData
                    ? tempExtraData.noticeScriptId || $t('pages.build.edit.2b4badd4')
                    : $t('pages.build.edit.2b4badd4')
                }`"
                read-only
                :placeholder="$t('pages.build.edit.2b4badd4')"
                :enter-button="$t('pages.build.edit.2fcc9ae3')"
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
                    {{ $t('pages.build.edit.311e1644') }}
                  </span>
                </template>
              </a-input-search>
            </a-form-item>
            <a-form-item name="attachEnv">
              <template #label>
                <a-tooltip>
                  {{ $t('pages.build.edit.3b9612e1') }}
                  <template #title>
                    <ul>
                      <li>{{ $t('pages.build.edit.54eaab4b') }}</li>
                      <li>{{ $t('pages.build.edit.cde3faf1') }}</li>
                      <li>{{ $t('pages.build.edit.bae4ca67') }}</li>
                      <li>{{ $t('pages.build.edit.24805fc4') }}</li>
                      <li>
                        {{ $t('pages.build.edit.53876f52') }}<b>USE_TAR_GZ=1</b>
                        {{ $t('pages.build.edit.ca8e3ee8') }}
                        <b>tar.gz</b> {{ $t('pages.build.edit.bd890e7e') }}
                      </li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input v-model:value="tempExtraData.attachEnv" :placeholder="$t('pages.build.edit.336147d2')" />
            </a-form-item>
            <a-form-item name="cacheBuild">
              <template #label>
                <a-tooltip>
                  {{ $t('pages.build.edit.49370d30') }}
                  <template #title>
                    {{ $t('pages.build.edit.34a0a0e1') }}
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-row>
                <a-col :span="4">
                  <a-switch
                    v-model:checked="tempExtraData.syncFileStorage"
                    :checked-children="$t('pages.build.edit.c97e1761')"
                    :un-checked-children="$t('pages.build.edit.d23107d6')"
                  />
                </a-col>
                <a-col :span="6" style="text-align: right">
                  <a-form-item-rest>
                    <a-space>
                      <a-tooltip>
                        {{ $t('pages.build.edit.67ac1467') }}
                        <template #title>
                          {{ $t('pages.build.edit.ecb919ff') }},{{ $t('pages.build.edit.b71cfd19') }}
                        </template>

                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-switch
                        v-model:checked="tempExtraData.releaseHideFile"
                        :checked-children="$t('pages.build.edit.d2fbce36')"
                        :un-checked-children="$t('pages.build.edit.1c77d6fb')"
                      />
                    </a-space>
                  </a-form-item-rest>
                </a-col>

                <a-col :span="7" style="text-align: right">
                  <a-form-item-rest>
                    <a-space>
                      <a-tooltip>
                        {{ $t('pages.build.edit.98db1c5') }}
                        <template #title>
                          {{ $t('pages.build.edit.897dca6a') }}
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
                        {{ $t('pages.build.edit.cae4df7d') }}
                        <template #title>
                          {{ $t('pages.build.edit.8d20fad4') }}
                        </template>
                        <QuestionCircleOutlined v-if="!temp.id" />
                      </a-tooltip>
                      <a-input-number v-model:value="tempExtraData.resultKeepCount" :min="0" />
                    </a-space>
                  </a-form-item-rest>
                </a-col>
              </a-row>
            </a-form-item>
            <a-form-item
              :label="$t('pages.build.edit.9dfaff03')"
              name="aliasCode"
              :help="$t('pages.build.edit.fcf063a3')"
            >
              <a-row>
                <a-col :span="10">
                  <a-input-search
                    v-model:value="temp.aliasCode"
                    :max-length="50"
                    :placeholder="$t('pages.build.edit.85499185')"
                    @search="
                      () => {
                        temp = { ...temp, aliasCode: randomStr(6) }
                      }
                    "
                  >
                    <template #enterButton>
                      <a-button type="primary">
                        {{ $t('pages.build.edit.9aa1afd7') }}
                      </a-button>
                    </template>
                  </a-input-search>
                </a-col>
                <a-col :span="1" style="text-align: right"></a-col>
                <a-col :span="10">
                  <a-form-item-rest>
                    <a-tooltip>
                      {{ $t('pages.build.edit.d3bbfc0c') }}
                      <template #title>
                        {{ $t('pages.build.edit.31270908') }}
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
                  {{ $t('pages.build.edit.d8413005') }}
                  <template #title>
                    <ul>
                      <li>{{ $t('pages.build.edit.c61621e9') }}</li>
                    </ul>
                  </template>
                  <QuestionCircleOutlined v-if="!temp.id" />
                </a-tooltip>
              </template>
              <a-input v-model:value="tempExtraData.excludeReleaseAnt" :placeholder="$t('pages.build.edit.397732b1')" />
            </a-form-item>
          </div>
        </a-form>
      </a-card>
    </a-spin>
    <!-- 选择仓库 -->
    <a-drawer
      destroy-on-close
      :title="`${$t('pages.build.edit.e95bc1a3')}`"
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
            {{ $t('pages.build.edit.43105e21') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['repository'].handerConfirm()
              }
            "
          >
            {{ $t('pages.build.edit.7da4a591') }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 选择脚本 -->
    <a-drawer
      destroy-on-close
      :title="`${$t('pages.build.edit.2fcc9ae3')}`"
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
            {{ $t('pages.build.edit.43105e21') }}
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptPage'].handerConfirm()
              }
            "
          >
            {{ $t('pages.build.edit.7da4a591') }}
          </a-button>
        </a-space>
      </template>
    </a-drawer>
    <!-- 查看容器 -->
    <a-drawer
      destroy-on-close
      :title="`${$t('pages.build.edit.eed6df5e')}`"
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
      :title="$t('pages.build.edit.e7ebed80')"
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
          title: this.$t('pages.build.edit.2a9ef0eb'),
          children: [
            {
              title: this.$t('pages.build.edit.9d427063'),
              value: 'mvn clean package -Dmaven.test.skip=true'
            },
            {
              title: this.$t('pages.build.edit.5659520'),
              value: 'mvn clean package -Dmaven.test.skip=true -Pprod'
            },
            {
              title: this.$t('pages.build.edit.f869e0df'),
              value: 'mvn clean package -Dmaven.test.skip=true -Ptest'
            },
            {
              title: this.$t('pages.build.edit.34d8a5b'),
              value: 'mvn clean package -DskipTests'
            },
            {
              title: 'mvn clean package',
              value: 'mvn clean package'
            },
            {
              title: this.$t('pages.build.edit.73512cae'),
              value: 'mvn -f xxx/pom.xml clean package'
            },
            {
              title: this.$t('pages.build.edit.4304fb7e'),
              value: 'mvn -s xxx/settings.xml clean package'
            }
          ]
        },
        {
          title: this.$t('pages.build.edit.7e95f38d'),
          children: [
            {
              title: this.$t('pages.build.edit.455f3ab2'),
              value: 'npm i && npm run build'
            },
            {
              title: this.$t('pages.build.edit.fda856b3'),
              value: 'npm i && npm run build:prod'
            },
            {
              title: this.$t('pages.build.edit.2b4b3931'),
              value: 'npm i && npm run build:stage'
            },
            {
              title: this.$t('pages.build.edit.4d7ec7a'),
              value: 'yarn && yarn run build'
            },
            {
              title: this.$t('pages.build.edit.9b30b8b1'),
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
        name: [{ required: true, message: this.$t('pages.build.edit.2d3fee7b'), trigger: 'blur' }],
        buildMode: [{ required: true, message: this.$t('pages.build.edit.e4d6d7d6'), trigger: 'blur' }],
        releaseMethod: [{ required: true, message: this.$t('pages.build.edit.bbc5e270'), trigger: 'blur' }],
        branchName: [{ required: true, message: this.$t('pages.build.edit.622dac4d'), trigger: 'blur' }],
        script: [{ required: true, message: this.$t('pages.build.edit.622dac4d'), trigger: 'blur' }],
        resultDirFile: [{ required: true, message: this.$t('pages.build.edit.7a12ba09'), trigger: 'blur' }],
        // releasePath: [{ required: true, message: '请填写发布目录', trigger: 'blur' }],
        repositoryId: [
          {
            required: true,
            message: this.$t('pages.build.edit.fdf774f1'),
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
          title: this.$t('pages.build.edit.b5dfedd9')
        },
        {
          title: this.$t('pages.build.edit.ccf301b7')

          // status: 'process'
        },
        {
          title: this.$t('pages.build.edit.2e988cde')

          // status: 'wait'
        },
        {
          title: this.$t('pages.build.edit.7be4f983')

          // status: 'wait'
        },
        {
          title: this.$t('pages.build.edit.8a367db')

          // status: 'wait'
        }
      ],

      dslDefault:
        this.$t('pages.build.edit.157cc1d7') +
        'runsOn: ubuntu-latest\n' +
        this.$t('pages.build.edit.29dc8c24') +
        'fromTag: xxx\n' +
        this.$t('pages.build.edit.2c1567a3') +
        this.$t('pages.build.edit.e97ba49b') +
        this.$t('pages.build.edit.e989032a') +
        this.$t('pages.build.edit.abb166fd') +
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
        this.$t('pages.build.edit.8d3bf857') +
        '  - uses: cache\n' +
        '    path: /root/.m2\n' +
        this.$t('pages.build.edit.f4c0f558') +
        '    type: global\n' +
        this.$t('pages.build.edit.2aacda2d') +
        '  - uses: cache\n' +
        '    path: ${JPOM_WORKING_DIR}/web-vue/node_modules\n' +
        this.$t('pages.build.edit.19594154') +
        '    mode: copy\n' +
        '  - run: npm config set registry https://registry.npmmirror.com\n' +
        this.$t('pages.build.edit.f3de78b') +
        '  - run: cd  ${JPOM_WORKING_DIR}/web-vue && npm i && npm run build\n' +
        '  - run: cd ${JPOM_WORKING_DIR} && mvn package -s script/settings.xml\n' +
        this.$t('pages.build.edit.c8899a6d') +
        '# binds:\n' +
        '#  - /Users/user/.m2/settings.xml:/root/.m2/\n' +
        this.$t('pages.build.edit.a6fcd6f4') +
        '# dirChildrenOnly = true will create /var/data/titi and /var/data/tata dirChildrenOnly = false will create /var/data/root/titi and /var/data/root/tata\n' +
        '# copy:\n' +
        '#  - /Users/user/.m2/settings.xml:/root/.m2/:false\n' +
        this.$t('pages.build.edit.af8e732e') +
        'env:\n' +
        '  NODE_OPTIONS: --max-old-space-size=900\n' +
        this.$t('pages.build.edit.5f8d5d96') +
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
              item.label = findArra.length ? findArra[0].name : this.$t('pages.build.edit.ca1cdfa6')
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
                message: this.$t('pages.build.edit.8e910076')
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
