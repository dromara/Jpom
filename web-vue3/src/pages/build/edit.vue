<template>
  <div>
    <a-form ref="editBuildForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
      <a-form-item label="名称" prop="name">
        <a-row>
          <a-col :span="10">
            <a-input v-model="temp.name" :maxLength="50" placeholder="名称" />
          </a-col>
          <a-col :span="4" style="text-align: right">分组名称：</a-col>
          <a-col :span="10">
            <custom-select
              suffixIcon=""
              :maxLength="50"
              v-model="temp.group"
              :data="groupList"
              inputPlaceholder="添加分组"
              selectPlaceholder="选择分组"
            >
            </custom-select>
          </a-col>
        </a-row>
      </a-form-item>
      <a-form-item label="源仓库" prop="repositoryId">
        <a-input-search
          :value="`${tempRepository ? tempRepository.name + '[' + tempRepository.gitUrl + ']' : '请选择仓库'}`"
          readOnly
          placeholder="请选择仓库"
          enter-button="选择仓库"
          @search="
            () => {
              this.repositoryisible = true
            }
          "
        />
      </a-form-item>
      <a-form-item v-if="tempRepository && tempRepository.repoType === 0" label="分支" prop="branchName">
        <a-row>
          <a-col :span="10">
            <custom-select
              v-model="temp.branchName"
              :disabled="temp.branchTagName ? true : false"
              :data="branchList"
              @onRefreshSelect="loadBranchList"
              inputPlaceholder="自定义分支通配表达式"
              selectPlaceholder="请选择构建对应的分支,必选"
              @change="
                () => {
                  this.$refs['editBuildForm'] && this.$refs['editBuildForm'].clearValidate()
                }
              "
            >
              <div slot="inputTips">
                支持通配符(AntPathMatcher)
                <ul>
                  <li>? 匹配一个字符</li>
                  <li>* 匹配零个或多个字符</li>
                  <li>** 匹配路径中的零个或多个目录</li>
                </ul>
              </div>
            </custom-select>
          </a-col>
          <a-col :span="4" style="text-align: right"> 标签(TAG)：</a-col>
          <a-col :span="10">
            <custom-select
              v-model="temp.branchTagName"
              :data="branchTagList"
              @onRefreshSelect="loadBranchList"
              inputPlaceholder="自定义标签通配表达式"
              selectPlaceholder="选择构建的标签,不选为最新提交"
              @change="
                () => {
                  this.$refs['editBuildForm'] && this.$refs['editBuildForm'].clearValidate()
                }
              "
            >
              <div slot="inputTips">
                支持通配符(AntPathMatcher)
                <ul>
                  <li>? 匹配一个字符</li>
                  <li>* 匹配零个或多个字符</li>
                  <li>** 匹配路径中的零个或多个目录</li>
                </ul>
              </div>
            </custom-select>
          </a-col>
        </a-row>
      </a-form-item>

      <a-collapse :activeKey="['0', '1', '2']" expandIconPosition="right">
        <a-collapse-panel key="0">
          <template slot="header">
            <a-form-item prop="buildMode" style="margin-bottom: 0px">
              <template slot="label">
                <a-tooltip>
                  方式
                  <template slot="title">
                    <ul>
                      <li>本地构建是指直接在服务端中的服务器执行构建命令</li>
                      <li>容器构建是指使用 docker 容器执行构建,这样可以达到和宿主机环境隔离不用安装依赖环境</li>
                      <li>使用容器构建，docker 容器所在的宿主机需要有公网,因为需要远程下载环境依赖的 sdk 和镜像</li>
                      <li>创建后构建方式不支持修改</li>
                      <li v-if="getInDocker">容器安装的服务端不能使用本地构建</li>
                    </ul>
                  </template>
                  <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-radio-group :disabled="temp.id ? true : false" v-model="temp.buildMode" name="buildMode">
                <a-radio
                  v-for="item in buildModeArray"
                  :disabled="item.disabled"
                  :key="item.value"
                  :value="item.value"
                  >{{ item.name }}</a-radio
                >
              </a-radio-group>
            </a-form-item>
          </template>
          <div v-if="temp.buildMode === undefined" style="text-align: center">请选择构建方式</div>

          <a-form-item v-if="temp.buildMode === 0" prop="script">
            <template slot="label">
              <a-tooltip>
                构建命令
                <template slot="title">
                  这里构建命令最终会在服务器上执行。
                  如果有多行命令那么将<b>逐行执行</b>，如果想要切换路径后执行命令则需要
                  <b>cd xxx && mvn clean package</b></template
                >
                <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-popover title="辅助操作">
              <template slot="content">
                <a-space direction="vertical">
                  <a-button
                    type="link"
                    @click="
                      () => {
                        this.viewScriptTemplVisible = true
                      }
                    "
                  >
                    常见构建命令示例 <a-icon type="fullscreen" />
                  </a-button>
                  <a-button
                    type="link"
                    @click="
                      () => {
                        this.chooseScriptVisible = 2
                      }
                    "
                  >
                    引用脚本模板
                  </a-button>
                </a-space>
              </template>
              <a-input
                v-model="temp.script"
                type="textarea"
                :auto-size="{ minRows: 2, maxRows: 6 }"
                allow-clear
                placeholder="构建执行的命令(非阻塞命令)，如：mvn clean package、npm run build。支持变量：${BUILD_ID}、${BUILD_NAME}、${BUILD_SOURCE_FILE}、${BUILD_NUMBER_ID}、仓库目录下 .env、工作空间变量"
              />
            </a-popover>
          </a-form-item>
          <a-form-item v-if="temp.buildMode === 1" prop="script">
            <template slot="label">
              <a-tooltip>
                DSL 内容
                <template slot="title">
                  <p>以 yaml/yml 格式配置</p>
                  <ul>
                    <li>配置需要声明使用具体的 docker 来执行构建相关操作(建议使用服务端所在服务器中的 docker)</li>
                    <li>容器构建会在 docker 中生成相关挂载目录,一般情况不需要人为操作</li>
                    <li>执行构建时会生成一个容器来执行，构建结束后会自动删除对应的容器</li>
                  </ul>
                  <div>
                    目前支持都插件有（更多插件尽情期待）：
                    <ol>
                      <li>
                        java sdk 镜像使用：https://mirrors.tuna.tsinghua.edu.cn/ 支持版本有：8, 9, 10, 11, 12, 13, 14,
                        15, 16, 17
                      </li>
                      <li>maven sdk 镜像使用：https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/</li>
                      <li>node sdk 镜像使用：https://registry.npmmirror.com/-/binary/node</li>
                      <li>
                        (存在兼容问题,实际使用中需要提前测试) python3 sdk
                        镜像使用：https://repo.huaweicloud.com/python/${PYTHON3_VERSION}/Python-${PYTHON3_VERSION}.tar.xz
                      </li>
                      <li>
                        (存在兼容问题,实际使用中需要提前测试) go sdk
                        镜像使用：https://studygolang.com/dl/golang/go${GO_VERSION}.linux-${ARCH}.tar.gz
                      </li>
                    </ol>
                  </div>
                </template>
                <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-tabs>
              <a-tab-pane key="1" tab="DSL 配置">
                <div style="height: 40vh">
                  <code-editor
                    v-model="temp.script"
                    :options="{ mode: 'yaml', tabSize: 2, theme: 'abcdef' }"
                  ></code-editor>
                </div>
              </a-tab-pane>
              <a-tab-pane key="2" tab="配置示例">
                <div style="height: 40vh">
                  <code-editor
                    v-model="dslDefault"
                    :options="{ mode: 'yaml', tabSize: 2, theme: 'abcdef', readOnly: true }"
                  ></code-editor>
                </div>
              </a-tab-pane>
            </a-tabs>
          </a-form-item>
          <a-form-item v-if="temp.buildMode !== undefined" prop="resultDirFile" class="jpom-target-dir">
            <template slot="label">
              <a-tooltip>
                产物目录
                <template slot="title">
                  <div>
                    可以理解为项目打包的目录。 如 Jpom 项目执行（构建命令）
                    <b>mvn clean package</b> 构建命令，构建产物相对路径为：<b
                      >modules/server/target/server-2.4.2-release</b
                    >
                  </div>
                  <div><br /></div>
                  <!-- 只有本地构建支持 模糊匹配 -->
                  <div v-if="temp.buildMode === 0">
                    支持通配符(AntPathMatcher)【目前只使用匹配到的第一项】
                    <ul>
                      <li>? 匹配一个字符</li>
                      <li>* 匹配零个或多个字符</li>
                      <li>** 匹配路径中的零个或多个目录</li>
                    </ul>
                  </div>
                </template>
                <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input
              :maxLength="200"
              v-model="temp.resultDirFile"
              placeholder="构建产物目录,相对仓库的路径,如 java 项目的 target/xxx.jar vue 项目的 dist"
            />
          </a-form-item>
        </a-collapse-panel>
        <a-collapse-panel key="1">
          <template slot="header">
            <a-form-item prop="releaseMethod" style="margin-bottom: 0px">
              <template slot="label">
                <a-tooltip>
                  发布操作
                  <template slot="title">
                    <ul>
                      <li>发布操作是指,执行完构建命令后将构建产物目录中的文件用不同的方式发布(上传)到对应的地方</li>
                      <li>节点分发是指,一个项目部署在多个节点中使用节点分发一步完成多个节点中的项目发布操作</li>
                      <li>项目是指,节点中的某一个项目,需要提前在节点中创建项目</li>
                      <li>
                        SSH 是指,通过 SSH 命令的方式对产物进行发布或者执行多条命令来实现发布(需要到 SSH 中提前去添加)
                      </li>
                      <li>本地命令是指,在服务端本地执行多条命令来实现发布</li>
                      <li>
                        SSH、本地命令发布都执行变量替换,系统预留变量有：${BUILD_ID}、${BUILD_NAME}、${BUILD_RESULT_FILE}、${BUILD_NUMBER_ID}
                      </li>
                      <li>可以引用工作空间的环境变量 变量占位符 ${xxxx} xxxx 为变量名称</li>
                    </ul>
                  </template>
                  <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-radio-group v-model="temp.releaseMethod" name="releaseMethod">
                <a-radio v-for="(val, key) in releaseMethodMap" :key="key" :value="parseInt(key)">{{ val }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </template>
          <div v-if="!temp.releaseMethod" style="text-align: center">请选择发布方式</div>
          <template v-else>
            <template v-if="temp.releaseMethod === 0"> 不发布：只执行构建流程并且保存构建历史,不执行发布流程</template>
            <!-- 节点分发 -->
            <template v-if="temp.releaseMethod === 1">
              <a-form-item label="分发项目" prop="releaseMethodDataId">
                <a-select
                  show-search
                  allowClear
                  v-model="tempExtraData.releaseMethodDataId_1"
                  placeholder="请选择分发项目"
                >
                  <a-select-option v-for="dispatch in dispatchList" :key="dispatch.id"
                    >{{ dispatch.name }}
                  </a-select-option>
                  <a-icon slot="suffixIcon" type="reload" @click="loadDispatchList" />
                </a-select>
              </a-form-item>
              <a-form-item prop="projectSecondaryDirectory" label="二级目录">
                <a-input
                  v-model="tempExtraData.projectSecondaryDirectory"
                  placeholder="不填写则使用节点分发配置的二级目录"
                />
              </a-form-item>
            </template>

            <!-- 项目 -->
            <template v-if="temp.releaseMethod === 2">
              <a-form-item label="发布项目" prop="releaseMethodDataIdList">
                <a-cascader v-model="temp.releaseMethodDataIdList" :options="cascaderList" placeholder="请选择节点项目">
                  <a-icon slot="suffixIcon" type="reload" @click="loadNodeProjectList" />
                </a-cascader>
              </a-form-item>
              <a-form-item label="发布后操作" prop="afterOpt">
                <a-select show-search allowClear v-model="tempExtraData.afterOpt" placeholder="请选择发布后操作">
                  <a-select-option v-for="opt in afterOptListSimple" :key="opt.value">{{ opt.title }}</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item prop="projectSecondaryDirectory" label="二级目录">
                <a-input v-model="tempExtraData.projectSecondaryDirectory" placeholder="不填写则发布至项目的根目录" />
              </a-form-item>
            </template>
            <!-- SSH -->
            <template v-if="temp.releaseMethod === 3">
              <a-form-item prop="releaseMethodDataId" help="如果 ssh 没有配置授权目录是不能选择的哟">
                <template slot="label">
                  <a-tooltip>
                    发布的SSH
                    <template slot="title"> 如果 ssh 没有配置授权目录是不能选择的哟 </template>
                    <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                  </a-tooltip>
                </template>
                <a-row>
                  <a-col :span="22">
                    <a-select
                      show-search
                      option-filter-prop="children"
                      mode="multiple"
                      v-model="tempExtraData.releaseMethodDataId_3"
                      placeholder="请选择SSH"
                    >
                      <a-select-option v-for="ssh in sshList" :disabled="!ssh.fileDirs" :key="ssh.id">
                        <a-tooltip :title="ssh.name"> {{ ssh.name }}</a-tooltip>
                      </a-select-option>
                    </a-select>
                  </a-col>
                  <a-col :span="1" style="margin-left: 10px">
                    <a-icon type="reload" @click="loadSshList" />
                  </a-col>
                </a-row>
              </a-form-item>
              <a-form-item
                prop="releaseMethodDataId"
                help="如果多选 ssh 下面目录只显示选项中的第一项，但是授权目录需要保证每项都配置对应目录"
              >
                <template #label>
                  <a-tooltip title="如果多选 ssh 下面目录只显示选项中的第一项，但是授权目录需要保证每项都配置对应目录">
                    发布目录
                    <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                  </a-tooltip>
                </template>
                <a-input-group compact>
                  <a-select
                    show-search
                    allowClear
                    style="width: 30%"
                    v-model="tempExtraData.releaseSshDir"
                    placeholder="请选择SSH"
                  >
                    <a-select-option v-for="item in selectSshDirs" :key="item">
                      <a-tooltip :title="item">{{ item }}</a-tooltip>
                    </a-select-option>
                  </a-select>

                  <a-input
                    style="width: 70%"
                    v-model="tempExtraData.releasePath2"
                    placeholder="发布目录,构建产物上传到对应目录"
                  />
                </a-input-group>
              </a-form-item>
            </template>

            <a-form-item v-if="temp.releaseMethod === 3" prop="releaseBeforeCommand">
              <!-- sshCommand -->
              <template slot="label">
                <a-tooltip>
                  发布前命令
                  <template slot="title">
                    发布前执行的命令(非阻塞命令),一般是关闭项目命令
                    <ul>
                      <li>支持变量替换：${BUILD_ID}、${BUILD_NAME}、${BUILD_RESULT_FILE}、${BUILD_NUMBER_ID}</li>
                      <li>可以引用工作空间的环境变量 变量占位符 ${xxxx} xxxx 为变量名称</li>
                    </ul>
                  </template>
                  <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-input
                v-model="tempExtraData.releaseBeforeCommand"
                allow-clear
                :auto-size="{ minRows: 2, maxRows: 10 }"
                type="textarea"
                :rows="3"
                placeholder="发布前执行的命令(非阻塞命令),一般是关闭项目命令 ,支持变量替换：${BUILD_ID}、${BUILD_NAME}、${BUILD_RESULT_FILE}、${BUILD_NUMBER_ID}"
              />
            </a-form-item>
            <a-form-item v-if="temp.releaseMethod === 3 || temp.releaseMethod === 4" prop="releaseCommand">
              <!-- sshCommand LocalCommand -->
              <template slot="label">
                <a-tooltip>
                  发布后命令
                  <template slot="title">
                    发布后执行的命令(非阻塞命令),一般是启动项目命令 如：ps -aux | grep java
                    <ul>
                      <li>支持变量替换：${BUILD_ID}、${BUILD_NAME}、${BUILD_RESULT_FILE}、${BUILD_NUMBER_ID}</li>
                      <li>可以引用工作空间的环境变量 变量占位符 ${xxxx} xxxx 为变量名称</li>
                    </ul>
                  </template>
                  <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-input
                v-model="tempExtraData.releaseCommand"
                allow-clear
                :auto-size="{ minRows: 2, maxRows: 10 }"
                type="textarea"
                :rows="3"
                placeholder="发布后执行的命令(非阻塞命令),一般是启动项目命令 如：ps -aux | grep java,支持变量替换：${BUILD_ID}、${BUILD_NAME}、${BUILD_RESULT_FILE}、${BUILD_NUMBER_ID}"
              />
            </a-form-item>

            <a-form-item v-if="temp.releaseMethod === 2 || temp.releaseMethod === 3" prop="clearOld">
              <template slot="label">
                <a-tooltip>
                  清空发布
                  <template slot="title">
                    清空发布是指在上传新文件前,会将项目文件夹目录里面的所有文件先删除后再保存新文件
                  </template>
                  <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-row>
                <a-col :span="4">
                  <a-switch v-model="tempExtraData.clearOld" checked-children="是" un-checked-children="否" />
                </a-col>
                <div v-if="temp.releaseMethod === 2">
                  <a-col :span="4" style="text-align: right">
                    <a-tooltip>
                      差异发布：
                      <template slot="title">
                        差异发布是指对应构建产物和项目文件夹里面的文件是否存在差异,如果存在增量差异那么上传或者覆盖文件。
                        <ul>
                          <li>
                            开启差异发布并且开启清空发布时将自动删除项目目录下面有的文件但是构建产物目录下面没有的文件
                            【清空发布差异上传前会先执行删除差异文件再执行上传差异文件】
                          </li>
                          <li>开启差异发布但不开启清空发布时相当于只做增量和变动更新</li>
                        </ul>
                      </template>
                      <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                    </a-tooltip>
                  </a-col>
                  <a-col :span="4">
                    <a-switch v-model="tempExtraData.diffSync" checked-children="是" un-checked-children="否" />
                  </a-col>
                  <a-col :span="4" style="text-align: right">
                    <a-tooltip>
                      发布前停止：
                      <template slot="title">
                        发布前停止是指在发布文件到项目文件时先将项目关闭，再进行文件替换。避免 windows
                        环境下出现文件被占用的情况
                      </template>
                      <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                    </a-tooltip>
                  </a-col>
                  <a-col :span="4">
                    <a-switch
                      v-model="tempExtraData.projectUploadCloseFirst"
                      checked-children="是"
                      un-checked-children="否"
                    />
                  </a-col>
                </div>
              </a-row>
            </a-form-item>
            <!-- docker -->
            <template v-if="temp.releaseMethod === 5">
              <a-form-item prop="fromTag">
                <template #label>
                  <a-tooltip>
                    执行容器
                    <template slot="title">
                      使用哪个 docker 构建,填写 docker 标签（ 标签在 docker 编辑页面配置） 默认查询可用的第一个,如果 tag
                      查询出多个将依次构建</template
                    >
                    <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                  </a-tooltip>
                </template>
                <a-input v-model="tempExtraData.fromTag" placeholder="执行容器 标签" />
              </a-form-item>

              <a-tooltip
                title="需要在仓库里面 dockerfile,如果多文件夹查看可以指定二级目录如果 springboot-test-jar:springboot-test-jar/Dockerfile"
              >
                <a-form-item prop="dockerfile" label="Dockerfile">
                  <a-input v-model="tempExtraData.dockerfile" placeholder="文件夹路径 需要在仓库里面 dockerfile" />
                </a-form-item>
              </a-tooltip>
              <a-form-item prop="dockerTag" label="镜像 tag">
                <a-tooltip
                  title="容器标签,如：xxxx:latest 多个使用逗号隔开, 配置附加环境变量文件支持加载仓库目录下 .env 文件环境变量 如： xxxx:${VERSION}"
                >
                  <a-input v-model="tempExtraData.dockerTag" placeholder="容器标签,如：xxxx:latest 多个使用逗号隔开" />
                </a-tooltip>
              </a-form-item>
              <a-form-item prop="dockerBuildArgs" label="构建参数">
                <a-row>
                  <a-col :span="10">
                    <a-tooltip title="构建参数,如：key1=values1&keyvalue2 使用 URL 编码">
                      <a-input
                        v-model="tempExtraData.dockerBuildArgs"
                        placeholder="构建参数,如：key1=values1&keyvalue2"
                      />
                    </a-tooltip>
                  </a-col>
                  <a-col :span="4" style="text-align: right">镜像标签：</a-col>
                  <a-col :span="10">
                    <a-tooltip title="镜像标签,如：key1=values1&keyvalue2 使用 URL 编码">
                      <a-input
                        v-model="tempExtraData.dockerImagesLabels"
                        placeholder="镜像标签,如：key1=values1&keyvalue2"
                      />
                    </a-tooltip>
                  </a-col>
                </a-row>
              </a-form-item>
              <a-form-item prop="swarmId">
                <template slot="label">
                  <a-tooltip>
                    发布集群
                    <template slot="title"> 目前使用的 docker swarm 集群，需要先创建 swarm 集群才能选择 </template>
                    <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                  </a-tooltip>
                </template>
                <a-select
                  @change="selectSwarm()"
                  show-search
                  allowClear
                  v-model="tempExtraData.dockerSwarmId"
                  placeholder="请选择发布到哪个 docker 集群"
                >
                  <a-select-option value="">不发布到 docker 集群</a-select-option>
                  <a-select-option v-for="item1 in dockerSwarmList" :key="item1.id">{{ item1.name }}</a-select-option>
                  <a-icon slot="suffixIcon" type="reload" @click="loadDockerSwarmListAll" />
                </a-select>
              </a-form-item>
              <a-form-item prop="pushToRepository" :label="` `" :colon="false">
                <a-row>
                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        推送到仓库
                        <template slot="title"> 镜像构建成功后是否需要推送到远程仓库 </template>
                        <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                      </a-tooltip>

                      <a-switch
                        v-model="tempExtraData.pushToRepository"
                        checked-children="是"
                        un-checked-children="否"
                      />
                    </a-space>
                  </a-col>
                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        版本递增
                        <template slot="title">
                          开启 dockerTag 版本递增后将在每次构建时自动将版本号最后一位数字同步为构建序号ID,
                          如：当前构建为第 100 次构建 testtag:1.0 -> testtag:1.100,testtag:1.0.release ->
                          testtag:1.100.release。如果没有匹配到数字将忽略递增操作
                        </template>
                        <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                      </a-tooltip>

                      <a-switch
                        v-model="tempExtraData.dockerTagIncrement"
                        checked-children="是"
                        un-checked-children="否"
                      />
                    </a-space>
                  </a-col>
                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        no-cache
                        <template slot="title">构建镜像的过程不使用缓存 </template>
                        <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                      </a-tooltip>

                      <a-switch v-model="tempExtraData.dockerNoCache" checked-children="是" un-checked-children="否" />
                    </a-space>
                  </a-col>
                  <a-col :span="6" style="text-align: right">
                    <a-space>
                      <a-tooltip>
                        更新镜像
                        <template slot="title">构建镜像尝试去更新基础镜像的新版本 </template>
                        <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                      </a-tooltip>

                      <a-switch
                        v-model="tempExtraData.dockerBuildPull"
                        checked-children="是"
                        un-checked-children="否"
                      />
                    </a-space>
                  </a-col>
                </a-row>
              </a-form-item>
              <a-form-item prop="dockerSwarmServiceName" v-if="tempExtraData.dockerSwarmId">
                <template slot="label">
                  <a-tooltip>
                    集群服务
                    <template slot="title"> 需要选发布到集群中的对应的服务名，需要提前去集群中创建服务 </template>
                    <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                  </a-tooltip>
                </template>
                <a-select
                  allowClear
                  placeholder="请选择发布到集群的服务名"
                  v-model="tempExtraData.dockerSwarmServiceName"
                >
                  <a-select-option v-for="item2 in swarmServiceListOptions" :key="item2.spec.name">{{
                    item2.spec.name
                  }}</a-select-option>
                </a-select>
              </a-form-item>
            </template>
          </template>
        </a-collapse-panel>
        <a-collapse-panel key="2">
          <template slot="header">
            <a-form-item label="其他配置" style="margin-bottom: 0px"></a-form-item>
          </template>
          <a-form-item prop="cacheBuild">
            <template slot="label">
              <a-tooltip>
                缓存构建目录
                <template slot="title">
                  开启缓存构建目录将保留仓库文件,二次构建将 pull 代码,
                  不开启缓存目录每次构建都将重新拉取仓库代码(较大的项目不建议关闭缓存)
                  、特别说明如果缓存目录中缺失版本控制相关文件将自动删除后重新拉取代码</template
                >
                <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-row>
              <a-col :span="2">
                <a-tooltip
                  title="开启缓存构建目录将保留仓库文件,二次构建将 pull 代码, 不开启缓存目录每次构建都将重新拉取仓库代码(较大的项目不建议关闭缓存)"
                >
                  <a-switch v-model="tempExtraData.cacheBuild" checked-children="是" un-checked-children="否" />
                </a-tooltip>
              </a-col>
              <a-col :span="6" style="text-align: right">
                <a-space>
                  <a-tooltip>
                    保留产物：
                    <template slot="title"> 保留产物是指对在构建完成后是否保留构建产物相关文件，用于回滚 </template>

                    <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                  </a-tooltip>
                  <a-switch v-model="tempExtraData.saveBuildFile" checked-children="是" un-checked-children="否" />
                </a-space>
              </a-col>

              <a-col :span="6" style="text-align: right">
                <a-space>
                  <a-tooltip>
                    差异构建：
                    <template slot="title">
                      差异构建是指构建时候是否判断仓库代码有变动，如果没有变动则不执行构建
                    </template>

                    <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                  </a-tooltip>
                  <a-switch
                    v-model="tempExtraData.checkRepositoryDiff"
                    checked-children="是"
                    un-checked-children="否"
                  />
                </a-space>
              </a-col>
              <a-col :span="6" style="text-align: right">
                <a-space>
                  <a-tooltip>
                    严格执行：
                    <template slot="title">
                      严格执行脚本（构建命令、事件脚本、本地发布脚本、容器构建命令）执行返回状态码必须是
                      0、否则将构建状态标记为失败
                    </template>

                    <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                  </a-tooltip>
                  <a-switch v-model="tempExtraData.strictlyEnforce" checked-children="是" un-checked-children="否" />
                </a-space>
              </a-col>
            </a-row>
          </a-form-item>
          <a-form-item prop="webhook">
            <template slot="label">
              <a-tooltip>
                WebHooks
                <template slot="title">
                  <ul>
                    <li>构建过程请求对应的地址,开始构建,构建完成,开始发布,发布完成,构建异常,发布异常</li>
                    <li>传入参数有：buildId、buildName、type、statusMsg、triggerTime</li>
                    <li>type 的值有：startReady、pull、executeCommand、release、done、stop、success、error</li>
                    <li>异步请求不能保证有序性</li>
                  </ul>
                </template>
                <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input v-model="temp.webhook" placeholder="构建过程请求,非必填，GET请求" />
          </a-form-item>
          <a-form-item label="定时构建" prop="autoBuildCron">
            <a-auto-complete
              v-model="temp.autoBuildCron"
              placeholder="如果需要定时自动构建则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）"
              option-label-prop="value"
            >
              <template slot="dataSource">
                <a-select-opt-group v-for="group in cronDataSource" :key="group.title">
                  <span slot="label">
                    {{ group.title }}
                  </span>
                  <a-select-option v-for="opt in group.children" :key="opt.title" :value="opt.value">
                    {{ opt.title }} {{ opt.value }}
                  </a-select-option>
                </a-select-opt-group>
              </template>
            </a-auto-complete>
          </a-form-item>
          <a-form-item prop="noticeScriptId">
            <template slot="label">
              <a-tooltip>
                事件脚本
                <template slot="title">
                  <ul>
                    <li>构建过程执行对应的脚本,开始构建,构建完成,开始发布,发布完成,构建异常,发布异常</li>
                    <li>
                      传入环境变量有：buildId、buildName、type、statusMsg、triggerTime、buildNumberId、buildSourceFile
                    </li>
                    <li>执行脚本传入参数有：startReady、pull、executeCommand、release、done、stop、success</li>
                    <li>
                      <b
                        >注意：为了避免不必要的事件执行脚本，选择的脚本的备注中包含需要实现的事件参数关键词，如果需要执行
                        success 事件,那么选择的脚本的备注中需要包含 success 关键词</b
                      >
                    </li>
                  </ul>
                </template>
                <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input-search
              :value="`${tempExtraData ? tempExtraData.noticeScriptId || '请选择脚本' : '请选择脚本'}`"
              readOnly
              placeholder="请选择脚本"
              enter-button="选择脚本"
              @search="
                () => {
                  this.chooseScriptVisible = 1
                }
              "
            >
              <template slot="addonBefore" v-if="tempExtraData && this.tempExtraData.noticeScriptId">
                <span
                  @click="
                    () => {
                      this.tempExtraData = { ...this.tempExtraData, noticeScriptId: '' }
                    }
                  "
                >
                  重置选择
                </span>
              </template>
            </a-input-search>
            <!-- <a-select allowClear show-search option-filter-prop="children" placeholder="构建过程执行对应的脚本" v-model="tempExtraData.noticeScriptId">
              <a-select-option v-for="item2 in scriptList" :key="item2.id">{{ item2.name }}</a-select-option>
            </a-select> -->
          </a-form-item>
          <a-form-item prop="attachEnv">
            <template slot="label">
              <a-tooltip>
                附加环境变量
                <template slot="title">
                  <ul>
                    <li>附加环境变量是指读取仓库指定环境变量文件来添加到执行构建运行时</li>
                    <li>比如常见的 .env 文件</li>
                    <li>文件内容格式要求：env_name=xxxxx 不满足格式的行将自动忽略</li>
                  </ul>
                </template>
                <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input v-model="tempExtraData.attachEnv" placeholder="附加环境变量  .env 添加多个使用逗号分隔" />
          </a-form-item>
          <a-form-item prop="cacheBuild">
            <template slot="label">
              <a-tooltip>
                文件管理中心
                <template slot="title">
                  如果开启同步到文件管理中心，在构建发布流程将自动执行同步到文件管理中心的操作。</template
                >
                <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-row>
              <a-col :span="4">
                <a-switch
                  v-model="tempExtraData.syncFileStorage"
                  checked-children="同步"
                  un-checked-children="不同步"
                />
              </a-col>
              <a-col :span="7" style="text-align: right">
                <a-space>
                  <a-tooltip>
                    发布隐藏文件
                    <template slot="title"> 默认构建错误将自动忽略隐藏文件,开启此选项后可以正常发布隐藏文件 </template>

                    <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                  </a-tooltip>
                  <a-switch v-model="tempExtraData.releaseHideFile" checked-children="是" un-checked-children="否" />
                </a-space>
              </a-col>
            </a-row>
          </a-form-item>
          <a-form-item label="别名码" prop="aliasCode" help="如果产物同步到文件中心,当前值会共享">
            <a-input-search
              :maxLength="50"
              v-model="temp.aliasCode"
              placeholder="请输入别名码"
              @search="
                () => {
                  this.temp = { ...this.temp, aliasCode: randomStr(6) }
                }
              "
            >
              <template slot="enterButton">
                <a-button type="primary"> 随机生成 </a-button>
              </template>
            </a-input-search>
          </a-form-item>
          <a-form-item prop="excludeReleaseAnt">
            <template slot="label">
              <a-tooltip>
                排除发布
                <template slot="title">
                  <ul>
                    <li>使用 ANT 表达式来实现在过滤指定目录来实现发布排除指定目录</li>
                  </ul>
                </template>
                <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <a-input v-model="tempExtraData.excludeReleaseAnt" placeholder="排除发布 ANT 表达式,多个使用逗号分隔" />
          </a-form-item>
        </a-collapse-panel>
      </a-collapse>
    </a-form>
    <div>
      <div
        :style="{
          position: 'fixed',
          right: 0,
          bottom: 0,
          width: '60vw',
          borderTop: '1px solid #e9e9e9',
          padding: '10px 16px',
          background: '#fff',
          textAlign: 'right',
          zIndex: 10
        }"
      >
        <a-space>
          <a-button
            @click="
              () => {
                this.$emit('close')
              }
            "
          >
            取消
          </a-button>
          <a-tooltip
            v-if="temp.id"
            title="如果当前构建信息已经在其他页面更新过，需要点击刷新按钮来获取最新的信息，点击刷新后未保存的数据也将丢失"
          >
            <a-button @click="refresh"> 刷新</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleEditBuildOk(false)"> 保存 </a-button>
          <a-button type="primary" @click="handleEditBuildOk(true)"> 保存并构建 </a-button>
        </a-space>
      </div>
    </div>

    <!-- 选择仓库 -->
    <a-drawer
      destroyOnClose
      :title="`选择仓库`"
      placement="right"
      :visible="repositoryisible"
      width="85vw"
      :zIndex="1009"
      @close="
        () => {
          this.repositoryisible = false
        }
      "
    >
      <repository
        v-if="repositoryisible"
        :choose="true"
        @confirm="
          (repositoryId) => {
            this.temp = { ...this.temp, repositoryId: repositoryId, branchName: '', branchTagName: '' }
            this.repositoryisible = false
            changeRepositpry()
          }
        "
        @cancel="
          () => {
            this.repositoryisible = false
          }
        "
      ></repository>
    </a-drawer>
    <!-- 选择脚本 -->
    <a-drawer
      destroyOnClose
      :title="`选择脚本`"
      placement="right"
      :visible="chooseScriptVisible != 0"
      width="50vw"
      :zIndex="1009"
      @close="
        () => {
          this.chooseScriptVisible = 0
        }
      "
    >
      <scriptPage
        v-if="chooseScriptVisible"
        choose="radio"
        @confirm="
          (id) => {
            if (this.chooseScriptVisible === 1) {
              this.tempExtraData = { ...this.tempExtraData, noticeScriptId: id }
            } else if (this.chooseScriptVisible === 2) {
              this.temp = { ...this.temp, script: '$ref.script.' + id }
            }
            this.chooseScriptVisible = 0
          }
        "
        @cancel="
          () => {
            this.chooseScriptVisible = 0
          }
        "
      ></scriptPage>
    </a-drawer>

    <!-- 查看命令示例 -->
    <a-modal
      destroyOnClose
      width="50vw"
      v-model="viewScriptTemplVisible"
      title="构建命令示例"
      :footer="null"
      :maskClosable="false"
    >
      <a-collapse
        :activeKey="
          buildScipts.map((item, index) => {
            return index + ''
          })
        "
      >
        <a-collapse-panel v-for="(group, index) in buildScipts" :key="`${index}`" :header="group.title">
          <a-list size="small" bordered :data-source="group.children">
            <a-list-item slot="renderItem" slot-scope="opt">
              <a-space>
                {{ opt.title }}
                <a-icon
                  type="swap"
                  @click="
                    () => {
                      temp = { ...temp, script: opt.value }
                      viewScriptTemplVisible = false
                    }
                  "
                />
              </a-space>
            </a-list-item>
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
import { mapGetters } from 'vuex'
import { afterOptListSimple } from '@/api/dispatch'
export default {
  components: {
    CustomSelect,
    codeEditor,
    repository,
    scriptPage
  },
  data() {
    return {
      //   afterOptList,
      afterOptListSimple,
      releaseMethodMap,
      cronDataSource: CRON_DATA_SOURCE,
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
        releasePath: [{ required: true, message: '请填写发布目录', trigger: 'blur' }],
        repositoryId: [{ required: true, message: '请填选择构建的仓库', trigger: 'blur' }]
      },
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
        '# 给容器添加环境变量\n' +
        'env:\n' +
        '  NODE_OPTIONS: --max-old-space-size=900'
    }
  },
  computed: {
    ...mapGetters(['getInDocker']),
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
          disabled: parseInt(item) === 0 && this.getInDocker ? true : false,
          name: this.buildModeMap[item]
        }
      })
    }
  },
  created() {
    this.loadGroupList()
  },
  methods: {
    randomStr,
    refresh() {
      getBuildGet({
        id: this.temp.id
      }).then((res) => {
        if (res.data) {
          this.handleEdit(res.data)
        }
      })
    },
    // 添加
    handleAdd() {
      this.temp = {}
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
        saveBuildFile: true
      }
      this.$refs['editBuildForm']?.resetFields()
    },
    // 修改
    handleEdit(record) {
      this.$refs['editBuildForm']?.resetFields()
      this.temp = Object.assign({}, record)
      this.temp.buildMode = this.temp.buildMode || 0
      // 设置当前临时的 额外构建信息
      this.tempExtraData = JSON.parse(record.extraData) || {}
      if (typeof this.tempExtraData === 'string') {
        this.tempExtraData = JSON.parse(this.tempExtraData)
      }
      if (this.tempExtraData.cacheBuild === undefined) {
        this.tempExtraData.cacheBuild = true
      }
      if (this.tempExtraData.saveBuildFile === undefined) {
        this.tempExtraData.saveBuildFile = true
      }

      // 设置发布方式的数据
      if (this.tempExtraData.releaseMethodDataId) {
        if (record.releaseMethod === 1) {
          this.tempExtraData.releaseMethodDataId_1 = this.tempExtraData.releaseMethodDataId
        }
        if (record.releaseMethod === 2) {
          this.temp = {
            ...this.temp,
            releaseMethodDataIdList: this.tempExtraData.releaseMethodDataId.split(':')
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
              item.label = findArra.length ? findArra[0].name : '未知'
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
      getBranchList(params).then((res) => {
        if (res.code === 200) {
          this.branchList = res.data?.branch || []
          this.branchTagList = res.data?.tags || []
        }
      })
    },
    // 提交节点数据
    handleEditBuildOk(build) {
      // 检验表单
      this.$refs['editBuildForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        const tempExtraData = Object.assign({}, this.tempExtraData)
        // 设置参数
        if (this.temp.releaseMethod === 2) {
          if (this.temp.releaseMethodDataIdList.length < 2) {
            this.$notification.warn({
              message: '请选择节点项目,可能是节点中不存在任何项目,需要去节点中创建项目'
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
          extraData: JSON.stringify(tempExtraData)
        }
        // 提交数据
        editBuild(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg
            })
            //
            this.$emit('confirm', build, res.data)
          }
        })
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
      this.tempExtraData = { ...this.tempExtraData, dockerSwarmServiceName: undefined }
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
    }
  }
}
</script>
