/** * 这是新版本的构建列表页面，主要是分离了部分数据到【仓库管理】，以及数据会存储到数据库 */
<template>
  <div class="full-content">
    <!-- <div ref="filter" class="filter"></div> -->
    <!-- 表格 -->
    <a-table size="middle" :columns="columns" :data-source="list" bordered rowKey="id" :pagination="pagination" @change="changePage">
      <template slot="title">
        <a-space>
          <a-input allowClear class="search-input-item" @pressEnter="loadData" v-model="listQuery['%name%']" placeholder="构建名称" />
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            allowClear
            option-filter-prop="children"
            v-model="listQuery.status"
            placeholder="状态"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            v-model="listQuery.releaseMethod"
            allowClear
            placeholder="发布方式"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in releaseMethodMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            v-model="listQuery.group"
            allowClear
            placeholder="分组"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-input allowClear class="search-input-item" @pressEnter="loadData" v-model="listQuery['%resultDirFile%']" placeholder="产物目录" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text, record" placement="topLeft" @click="handleEdit(record)" :title="`名称：${text} 点击可以编辑`">
        <!-- <a-icon type="edit" theme="twoTone" /> -->
        <a-button type="link" style="padding: 0px" size="small">{{ text }}</a-button>
      </a-tooltip>
      <a-tooltip slot="branchName" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <!-- <a-tooltip slot="resultDirFile" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip> -->
      <a-tooltip
        slot="buildMode"
        slot-scope="text, record"
        @click="record.status === 1 || record.status === 4 ? handleStopBuild(record) : handleConfirmStartBuild(record)"
        placement="topLeft"
        :title="text === 1 ? '容器构建' : '本地构建'"
      >
        <a-icon v-if="text === 1" type="cloud" />
        <a-icon v-else type="code" />
      </a-tooltip>
      <a-tooltip slot="releaseMethod" slot-scope="text, record">
        <template slot="title">
          <ul>
            <li>发布方式：{{ releaseMethodMap[text] }}</li>
            <li>产物目录：{{ record.resultDirFile }}</li>
            <li>构建命令：{{ record.script }}</li>
          </ul>
        </template>
        <span>{{ releaseMethodMap[text] }}</span>
      </a-tooltip>
      <template slot="status" slot-scope="text">
        <span>{{ statusMap[text] || "未知" }}</span>
      </template>
      <a-tooltip slot="buildId" slot-scope="text, record" placement="topLeft" :title="text + ' ( 点击查看日志 ) '">
        <span v-if="record.buildId <= 0"></span>
        <a-tag v-else color="#108ee9" @click="handleBuildLog(record)">#{{ text }}</a-tag>
      </a-tooltip>
      <a-tooltip slot="modifyUser" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="danger" v-if="record.status === 1 || record.status === 4" @click="handleStopBuild(record)">停止 </a-button>
          <a-button size="small" type="primary" v-else @click="handleConfirmStartBuild(record)">构建</a-button>
          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
              更多
              <a-icon type="down" />
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="primary" @click="handleTrigger(record)">触发器</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="primary" @click="copyItem(record)">复制</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
              </a-menu-item>

              <a-menu-item>
                <a-tooltip
                  placement="leftBottom"
                  title="清除代码(仓库目录)为删除服务器中存储仓库目录里面的所有东西,删除后下次构建将重新拉起仓库里面的文件,一般用于解决服务器中文件和远程仓库中文件有冲突时候使用。执行时间取决于源码目录大小和文件数量如超时请耐心等待，或稍后重试"
                >
                  <a-button size="small" type="danger" :disabled="!record.sourceDirExist" @click="handleClear(record)">清除代码 </a-button>
                </a-tooltip>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editBuildVisible" title="编辑构建" @ok="handleEditBuildOk" width="60vw" :maskClosable="false">
      <a-form-model ref="editBuildForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item label="名称" prop="name">
          <a-row>
            <a-col :span="10">
              <a-input v-model="temp.name" :maxLength="50" placeholder="名称" />
            </a-col>
            <a-col :span="4" style="text-align: right">分组名称：</a-col>
            <a-col :span="10">
              <custom-select suffixIcon="" :maxLength="50" v-model="temp.group" :data="groupList" inputPlaceholder="添加分组" selectPlaceholder=""> </custom-select>
            </a-col>
          </a-row>
        </a-form-model-item>

        <a-collapse :activeKey="['0', '1', '2']">
          <a-collapse-panel key="0">
            <template slot="header">
              <a-form-model-item prop="buildMode" style="margin-bottom: 0px">
                <template slot="label">
                  构建方式
                  <a-tooltip v-show="!temp.id">
                    <template slot="title">
                      <ul>
                        <li>本地构建是指直接在服务端中的服务器执行构建命令</li>
                        <li>容器构建是指使用 docker 容器执行构建,这样可以达到和宿主机环境隔离不用安装依赖环境</li>
                        <li>使用容器构建，docker 容器所在的宿主机需要由公网,因为需要远程下载环境依赖的 sdk 和镜像</li>
                      </ul>
                    </template>
                    <a-icon type="question-circle" theme="filled" />
                  </a-tooltip>
                </template>
                <a-radio-group v-model="temp.buildMode" name="buildMode">
                  <a-radio v-for="(val, key) in buildModeMap" :key="key" :value="parseInt(key)">{{ val }}</a-radio>
                </a-radio-group>
              </a-form-model-item>
            </template>
            <div v-if="temp.buildMode === undefined" style="text-align: center">请选择构建方式</div>

            <a-form-model-item v-if="temp.buildMode !== undefined" label="构建源仓库" prop="repositoryId">
              <a-select
                :getPopupContainer="
                  (triggerNode) => {
                    return triggerNode.parentNode || document.body;
                  }
                "
                show-search
                option-filter-prop="children"
                v-model="temp.repositoryId"
                @change="changeRepositpry"
                placeholder="请选择仓库"
              >
                <a-select-option v-for="item in repositoryList" :key="item.id" :value="item.id">{{ item.name }}[{{ item.gitUrl }}]</a-select-option>
              </a-select>
            </a-form-model-item>

            <a-form-model-item v-if="temp.buildMode !== undefined && tempRepository.repoType === 0" label="分支" prop="branchName">
              <a-row>
                <a-col :span="10">
                  <custom-select v-model="temp.branchName" :data="branchList" @onRefreshSelect="loadBranchList" inputPlaceholder="自定义分支通配表达式" selectPlaceholder="请选择构建对应的分支,必选">
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
            </a-form-model-item>

            <a-form-model-item v-if="temp.buildMode === 0" label="构建命令" prop="script">
              <a-popover title="命令示例">
                <template slot="content">
                  <p
                    @click="
                      () => {
                        this.viewScriptTemplVisible = true;
                      }
                    "
                  >
                    <a-button type="link"> 点击查看 <a-icon type="fullscreen" /> </a-button>
                  </p>
                </template>
                <a-input
                  v-model="temp.script"
                  type="textarea"
                  :auto-size="{ minRows: 2, maxRows: 6 }"
                  allow-clear
                  placeholder="构建执行的命令(非阻塞命令)，如：mvn clean package、npm run build。支持变量：${BUILD_ID}、${BUILD_NAME}、${BUILD_SOURCE_FILE}、${BUILD_NUMBER_ID}、仓库目录下 .env、工作空间变量"
                />
              </a-popover>
            </a-form-model-item>
            <a-form-model-item v-if="temp.buildMode === 1" prop="script">
              <template slot="label">
                DSL 内容
                <a-tooltip v-show="temp.type !== 'edit'">
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
                        <li>java sdk 镜像使用：https://mirrors.tuna.tsinghua.edu.cn/ 支持版本有：8, 9, 10, 11, 12, 13, 14, 15, 16, 17</li>
                        <li>maven sdk 镜像使用：https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/</li>
                        <li>node sdk 镜像使用：https://registry.npmmirror.com/-/binary/node</li>
                        <li>(存在兼容问题,实际使用中需要提前测试) python3 sdk 镜像使用：https://repo.huaweicloud.com/python/${PYTHON3_VERSION}/Python-${PYTHON3_VERSION}.tar.xz</li>
                        <li>(存在兼容问题,实际使用中需要提前测试) go sdk 镜像使用：https://studygolang.com/dl/golang/go${GO_VERSION}.linux-${ARCH}.tar.gz</li>
                      </ol>
                    </div>
                  </template>
                  <a-icon type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-tabs>
                <a-tab-pane key="1" tab="DSL 配置">
                  <div style="height: 40vh; overflow-y: scroll">
                    <code-editor v-model="temp.script" :options="{ mode: 'yaml', tabSize: 2, theme: 'abcdef' }"></code-editor>
                  </div>
                </a-tab-pane>
                <a-tab-pane key="2" tab="配置示例">
                  <div style="height: 40vh; overflow-y: scroll">
                    <code-editor v-model="dslDefault" :options="{ mode: 'yaml', tabSize: 2, theme: 'abcdef', readOnly: true }"></code-editor>
                  </div>
                </a-tab-pane>
              </a-tabs>
            </a-form-model-item>
            <a-form-model-item v-if="temp.buildMode !== undefined" prop="resultDirFile" class="jpom-target-dir">
              <template slot="label">
                产物目录
                <a-tooltip v-show="!temp.id">
                  <template slot="title">
                    <div>可以理解为项目打包的目录。 如 Jpom 项目执行（构建命令） <b>mvn clean package</b> 构建命令，构建产物相对路径为：<b>modules/server/target/server-2.4.2-release</b></div>
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
                  <a-icon type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-input :maxLength="200" v-model="temp.resultDirFile" placeholder="构建产物目录,相对仓库的路径,如 java 项目的 target/xxx.jar vue 项目的 dist" />
            </a-form-model-item>
          </a-collapse-panel>
          <a-collapse-panel key="1">
            <template slot="header">
              <a-form-model-item prop="releaseMethod" style="margin-bottom: 0px">
                <template slot="label">
                  发布操作
                  <a-tooltip v-show="!temp.id">
                    <template slot="title">
                      <ul>
                        <li>发布操作是指,执行完构建命令后将构建产物目录中的文件用不同的方式发布(上传)到对应的地方</li>
                        <li>节点分发是指,一个项目部署在多个节点中使用节点分发一步完成多个节点中的项目发布操作</li>
                        <li>项目是指,节点中的某一个项目,需要提前在节点中创建项目</li>
                        <li>SSH 是指,通过 SSH 命令的方式对产物进行发布或者执行多条命令来实现发布(需要到 SSH 中提前去添加)</li>
                        <li>本地命令是指,在服务端本地执行多条命令来实现发布</li>
                        <li>SSH、本地命令发布都执行变量替换,系统预留变量有：#{BUILD_ID}、#{BUILD_NAME}、#{BUILD_RESULT_FILE}、#{BUILD_NUMBER_ID}</li>
                        <li>可以引用工作空间的环境变量 变量占位符 #{xxxx} xxxx 为变量名称</li>
                      </ul>
                    </template>
                    <a-icon type="question-circle" theme="filled" />
                  </a-tooltip>
                </template>
                <a-radio-group v-model="temp.releaseMethod" name="releaseMethod">
                  <a-radio v-for="(val, key) in releaseMethodMap" :key="key" :value="parseInt(key)">{{ val }}</a-radio>
                </a-radio-group>
              </a-form-model-item>
            </template>
            <div v-if="!temp.releaseMethod" style="text-align: center">请选择发布方式</div>
            <template v-else>
              <template v-if="temp.releaseMethod === 0"> 不发布：只执行构建流程并且保存构建历史,不执行发布流程</template>
              <!-- 节点分发 -->
              <a-form-model-item v-if="temp.releaseMethod === 1" label="分发项目" prop="releaseMethodDataId">
                <a-select
                  :getPopupContainer="
                    (triggerNode) => {
                      return triggerNode.parentNode || document.body;
                    }
                  "
                  show-search
                  allowClear
                  v-model="tempExtraData.releaseMethodDataId_1"
                  placeholder="请选择分发项目"
                >
                  <a-select-option v-for="dispatch in dispatchList" :key="dispatch.id">{{ dispatch.name }} </a-select-option>
                </a-select>
              </a-form-model-item>
              <!-- 项目 -->
              <a-form-model-item v-if="temp.releaseMethod === 2" label="发布项目" prop="releaseMethodDataIdList">
                <a-cascader
                  :getPopupContainer="
                    (triggerNode) => {
                      return triggerNode.parentNode || document.body;
                    }
                  "
                  v-model="temp.releaseMethodDataIdList"
                  :options="cascaderList"
                  placeholder="请选择节点项目"
                />
              </a-form-model-item>
              <a-form-model-item v-if="temp.releaseMethod === 2" label="发布后操作" prop="afterOpt">
                <a-select
                  :getPopupContainer="
                    (triggerNode) => {
                      return triggerNode.parentNode || document.body;
                    }
                  "
                  show-search
                  allowClear
                  v-model="tempExtraData.afterOpt"
                  placeholder="请选择发布后操作"
                >
                  <a-select-option v-for="opt in afterOptListSimple" :key="opt.value">{{ opt.title }}</a-select-option>
                </a-select>
              </a-form-model-item>
              <!-- SSH -->
              <template v-if="temp.releaseMethod === 3">
                <a-form-model-item prop="releaseMethodDataId">
                  <template slot="label">
                    发布的SSH
                    <a-tooltip v-show="!temp.id">
                      <template slot="title"> 如果 ssh 没有配置授权目录是不能选择的哟 </template>
                      <a-icon type="question-circle" theme="filled" />
                    </a-tooltip>
                  </template>
                  <a-select
                    :getPopupContainer="
                      (triggerNode) => {
                        return triggerNode.parentNode || document.body;
                      }
                    "
                    mode="multiple"
                    v-model="tempExtraData.releaseMethodDataId_3"
                    placeholder="请选择SSH"
                  >
                    <a-select-option v-for="ssh in sshList" :disabled="!ssh.fileDirs" :key="ssh.id">{{ ssh.name }}</a-select-option>
                  </a-select>
                </a-form-model-item>
                <a-form-model-item label="发布目录" prop="releaseMethodDataId">
                  <a-input-group compact>
                    <a-tooltip title="如果多选 ssh 下面目录只显示选项中的第一项，但是授权目录需要保证每项都配置对应目录">
                      <a-select
                        :getPopupContainer="
                          (triggerNode) => {
                            return triggerNode.parentNode || document.body;
                          }
                        "
                        show-search
                        allowClear
                        style="width: 30%"
                        v-model="tempExtraData.releaseSshDir"
                        placeholder="请选择SSH"
                      >
                        <a-select-option v-for="item in selectSshDirs" :key="item">{{ item }}</a-select-option>
                      </a-select>
                    </a-tooltip>
                    <a-input style="width: 70%" v-model="tempExtraData.releasePath2" placeholder="发布目录,构建产物上传到对应目录" />
                  </a-input-group>
                </a-form-model-item>
              </template>
              <a-form-model-item v-if="temp.releaseMethod === 3 || temp.releaseMethod === 4" prop="releaseCommand">
                <!-- sshCommand LocalCommand -->
                <template slot="label">
                  发布命令
                  <a-tooltip v-show="!temp.id">
                    <template slot="title">
                      发布执行的命令(非阻塞命令),一般是启动项目命令 如：ps -aux | grep java
                      <ul>
                        <li>支持变量替换：#{BUILD_ID}、#{BUILD_NAME}、#{BUILD_RESULT_FILE}、#{BUILD_NUMBER_ID}</li>
                        <li>可以引用工作空间的环境变量 变量占位符 #{xxxx} xxxx 为变量名称</li>
                      </ul>
                    </template>
                    <a-icon type="question-circle" theme="filled" />
                  </a-tooltip>
                </template>
                <a-input
                  v-model="tempExtraData.releaseCommand"
                  allow-clear
                  :auto-size="{ minRows: 2, maxRows: 10 }"
                  type="textarea"
                  :rows="3"
                  placeholder="发布执行的命令(非阻塞命令),一般是启动项目命令 如：ps -aux | grep java,支持变量替换：#{BUILD_ID}、#{BUILD_NAME}、#{BUILD_RESULT_FILE}、#{BUILD_NUMBER_ID}"
                />
              </a-form-model-item>

              <a-form-model-item v-if="temp.releaseMethod === 2 || temp.releaseMethod === 3" prop="clearOld">
                <template slot="label">
                  清空发布
                  <a-tooltip v-show="!temp.id">
                    <template slot="title"> 清空发布是指在上传新文件前,会将项目文件夹目录里面的所有文件先删除后再保存新文件 </template>
                    <a-icon type="question-circle" theme="filled" />
                  </a-tooltip>
                </template>
                <a-row>
                  <a-col :span="4">
                    <a-switch v-model="tempExtraData.clearOld" checked-children="是" un-checked-children="否" />
                  </a-col>
                  <div v-if="temp.releaseMethod === 2">
                    <a-col :span="4" style="text-align: right">
                      <a-tooltip v-if="!temp.id">
                        <template slot="title">
                          差异发布是指对应构建产物和项目文件夹里面的文件是否存在差异,如果存在增量差异那么上传或者覆盖文件。
                          <ul>
                            <li>开启差异发布并且开启清空发布时将自动删除项目目录下面有的文件但是构建产物目录下面没有的文件 【清空发布差异上传前会先执行删除差异文件再执行上传差异文件】</li>
                            <li>开启差异发布但不开启清空发布时相当于只做增量和变动更新</li>
                          </ul>
                        </template>
                        <a-icon type="question-circle" theme="filled" />
                      </a-tooltip>
                      差异发布：
                    </a-col>
                    <a-col :span="10">
                      <a-switch v-model="tempExtraData.diffSync" checked-children="是" un-checked-children="否" />
                    </a-col>
                  </div>
                </a-row>
              </a-form-model-item>
              <!-- docker -->
              <template v-if="temp.releaseMethod === 5">
                <a-tooltip title="使用哪个 docker 构建,填写 docker 标签 默认查询可用的第一个,如果 tag 查询出多个将依次构建">
                  <a-form-model-item prop="fromTag" label="执行容器">
                    <a-input v-model="tempExtraData.fromTag" placeholder="执行容器 标签" />
                  </a-form-model-item>
                </a-tooltip>

                <a-tooltip title="需要在仓库里面 dockerfile,如果多文件夹查看可以指定二级目录如果 springboot-test-jar:springboot-test-jar/Dockerfile">
                  <a-form-model-item prop="dockerfile" label="Dockerfile">
                    <a-input v-model="tempExtraData.dockerfile" placeholder="文件夹路径 需要在仓库里面 dockerfile" />
                  </a-form-model-item>
                </a-tooltip>
                <a-form-model-item prop="dockerTag" label="镜像 tag">
                  <a-tooltip title="容器标签,如：xxxx:latest 多个使用逗号隔开, 支持加载仓库目录下 .env 文件环境变量 如： xxxx:${VERSION}">
                    <a-input v-model="tempExtraData.dockerTag" placeholder="容器标签,如：xxxx:latest 多个使用逗号隔开" />
                  </a-tooltip>
                </a-form-model-item>
                <a-form-model-item prop="swarmId">
                  <template slot="label">
                    发布集群
                    <a-tooltip v-show="!temp.id">
                      <template slot="title"> 目前使用的 docker swarm 集群，需要先创建 swarm 集群才能选择 </template>
                      <a-icon type="question-circle" theme="filled" />
                    </a-tooltip>
                  </template>
                  <a-select
                    :getPopupContainer="
                      (triggerNode) => {
                        return triggerNode.parentNode || document.body;
                      }
                    "
                    @change="selectSwarm()"
                    show-search
                    allowClear
                    v-model="tempExtraData.dockerSwarmId"
                    placeholder="请选择发布到哪个 docker 集群"
                  >
                    <a-select-option value="">不发布到 docker 集群</a-select-option>
                    <a-select-option v-for="item1 in dockerSwarmList" :key="item1.id">{{ item1.name }}</a-select-option>
                  </a-select>
                </a-form-model-item>
                <a-form-model-item prop="dockerSwarmServiceName" v-if="tempExtraData.dockerSwarmId">
                  <template slot="label">
                    集群服务
                    <a-tooltip v-show="!temp.id">
                      <template slot="title"> 需要选发布到集群中的对应的服务名，需要提前去集群中创建服务 </template>
                      <a-icon type="question-circle" theme="filled" />
                    </a-tooltip>
                  </template>
                  <a-select
                    :getPopupContainer="
                      (triggerNode) => {
                        return triggerNode.parentNode || document.body;
                      }
                    "
                    allowClear
                    placeholder="请选择发布到集群的服务名"
                    v-model="tempExtraData.dockerSwarmServiceName"
                  >
                    <a-select-option v-for="item2 in swarmServiceListOptions" :key="item2.spec.name">{{ item2.spec.name }}</a-select-option>
                  </a-select>
                </a-form-model-item>
              </template>
            </template>
          </a-collapse-panel>
          <a-collapse-panel key="2">
            <template slot="header">
              <a-form-model-item label="其他配置" style="margin-bottom: 0px"></a-form-model-item>
            </template>
            <a-form-model-item prop="cacheBuild">
              <template slot="label">
                缓存构建目录
                <a-tooltip v-show="!temp.id">
                  <template slot="title">
                    开启缓存构建目录将保留仓库文件,二次构建将 pull 代码, 不开启缓存目录每次构建都将重新拉取仓库代码(较大的项目不建议关闭缓存)
                    、特别说明如果缓存目录中缺失版本控制相关文件将自动删除后重新拉取代码</template
                  >
                  <a-icon type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-row>
                <a-col :span="4">
                  <a-tooltip title="开启缓存构建目录将保留仓库文件,二次构建将 pull 代码, 不开启缓存目录每次构建都将重新拉取仓库代码(较大的项目不建议关闭缓存)">
                    <a-switch v-model="tempExtraData.cacheBuild" checked-children="是" un-checked-children="否" />
                  </a-tooltip>
                </a-col>
                <a-col :span="4" style="text-align: right">
                  <a-tooltip>
                    <template slot="title"> 保留产物是指对在构建完成后是否保留构建产物相关文件，用于回滚 </template>

                    <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                    保留产物：
                  </a-tooltip>
                </a-col>
                <a-col :span="4">
                  <a-switch v-model="tempExtraData.saveBuildFile" checked-children="是" un-checked-children="否" />
                </a-col>
                <a-col :span="4" style="text-align: right">
                  <a-tooltip>
                    <template slot="title"> 差异构建是指构建时候是否判断仓库代码有变动，如果没有变动则不执行构建 </template>

                    <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
                    差异构建：
                  </a-tooltip>
                </a-col>
                <a-col :span="4">
                  <a-switch v-model="tempExtraData.checkRepositoryDiff" checked-children="是" un-checked-children="否" />
                </a-col>
              </a-row>
            </a-form-model-item>
            <a-form-model-item prop="webhook">
              <template slot="label">
                WebHooks
                <a-tooltip v-show="!temp.id">
                  <template slot="title">
                    <ul>
                      <li>构建过程请求对应的地址,开始构建,构建完成,开始发布,发布完成,构建异常,发布异常</li>
                      <li>传人参数有：buildId、buildName、type、error、triggerTime</li>
                      <li>type 的值有：startReady、pull、executeCommand、release、done、stop、success</li>
                      <li>异步请求不能保证有序性</li>
                    </ul>
                  </template>
                  <a-icon type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-input v-model="temp.webhook" placeholder="构建过程请求,非必填，GET请求" />
            </a-form-model-item>
            <a-form-model-item label="自动构建" prop="autoBuildCron">
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
                    <a-select-option v-for="opt in group.children" :key="opt.title" :value="opt.value"> {{ opt.title }} {{ opt.value }} </a-select-option>
                  </a-select-opt-group>
                </template>
              </a-auto-complete>
            </a-form-model-item>
            <a-form-model-item prop="noticeScriptId">
              <template slot="label">
                事件脚本
                <a-tooltip v-show="!temp.id">
                  <template slot="title">
                    <ul>
                      <li>构建过程执行对应的脚本,开始构建,构建完成,开始发布,发布完成,构建异常,发布异常</li>
                      <li>传人环境变量有：buildId、buildName、type、error、triggerTime、buildNumberId、buildSourceFile</li>
                      <li>执行脚本传入参数有：startReady、pull、executeCommand、release、done、stop、success</li>
                      <li><b>注意：为了避免不必要的事件执行脚本，选择的脚本的备注中包含需要实现的事件参数关键词，如果需要执行 success 事件,那么选择的脚本的备注中需要包含 success 关键词</b></li>
                    </ul>
                  </template>
                  <a-icon type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-select
                :getPopupContainer="
                  (triggerNode) => {
                    return triggerNode.parentNode || document.body;
                  }
                "
                allowClear
                show-search
                option-filter-prop="children"
                placeholder="构建过程执行对应的脚本"
                v-model="tempExtraData.noticeScriptId"
              >
                <a-select-option v-for="item2 in scriptList" :key="item2.id">{{ item2.name }}</a-select-option>
              </a-select>
            </a-form-model-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form-model>
    </a-modal>
    <!-- 触发器 -->
    <a-modal v-model="triggerVisible" title="触发器" width="50%" :footer="null" :maskClosable="false">
      <a-form-model ref="editTriggerForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template slot="tabBarExtraContent">
            <a-tooltip title="重置触发器 token 信息,重置后之前的触发器 token 将失效">
              <a-button type="primary" size="small" @click="resetTrigger">重置</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" tab="执行构建">
            <a-space style="display: block" direction="vertical" align="baseline">
              <a-alert message="温馨提示" type="warning">
                <template slot="description">
                  <ul>
                    <li>单个触发器地址中：第一个随机字符串为构建ID，第二个随机字符串为 token</li>
                    <li>重置为重新生成触发地址,重置成功后之前的触发器地址将失效,构建触发器绑定到生成触发器到操作人上,如果将对应的账号删除触发器将失效</li>
                    <li>批量构建参数 BODY json： [ { "id":"1", "token":"a", "delay":"0" } ]</li>
                    <li>批量构建参数还支持指定参数,delay（延迟执行构建,单位秒） branchName（分支名）、branchTagName（标签）、script（构建脚本）、resultDirFile（构建产物）、webhook（通知 webhook）</li>
                    <li>
                      批量构建全部参数举例 BODY json： [ { "id":"1", "token":"a", "delay":"0","branchName":"test","branchTagName":"1.*","script":"mvn clean
                      package","resultDirFile":"/target/","webhook":"http://test.com/webhook" } ]
                    </li>
                    <li>批量构建传人其他参数将同步执行修改</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.triggerBuildUrl"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' });
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' });
                  }
                "
                type="info"
                :message="`单个触发器地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>GET</a-tag> <span>{{ temp.triggerBuildUrl }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.batchTriggerBuildUrl"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' });
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' });
                  }
                "
                type="info"
                :message="`批量触发器地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>POST</a-tag> <span>{{ temp.batchTriggerBuildUrl }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
          <a-tab-pane key="2" tab="查看当前状态">
            <a-space style="display: block" direction="vertical" align="baseline">
              <a-alert message="温馨提示" type="warning">
                <template slot="description">
                  <ul>
                    <li>批量构建参数 BODY json： [ { "id":"1", "token":"a" } ]</li>
                    <li>参数中的 id 、token 和触发构建一致</li>
                    <li>
                      <a-tag>No(0, "未构建")</a-tag>, <a-tag>Ing(1, "构建中")</a-tag>, <a-tag>Success(2, "构建结束")</a-tag>, <a-tag>Error(3, "构建失败")</a-tag>, <a-tag>PubIng(4, "发布中")</a-tag>,
                      <a-tag>PubSuccess(5, "发布成功")</a-tag>, <a-tag>PubError(6, "发布失败")</a-tag>, <a-tag>Cancel(7, "取消构建")</a-tag>,
                    </li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.batchBuildStatusUrl2"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' });
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' });
                  }
                "
                type="info"
                :message="`获取单个构建状态地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>GET</a-tag> <span>{{ temp.batchBuildStatusUrl2 }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.batchBuildStatusUrl"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' });
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' });
                  }
                "
                type="info"
                :message="`批量获取构建状态地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>POST</a-tag> <span>{{ temp.batchBuildStatusUrl }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
        </a-tabs>
      </a-form-model>
    </a-modal>
    <!-- 构建日志 -->
    <a-modal width="80vw" v-model="buildLogVisible" title="构建日志" :footer="null" :maskClosable="false" @cancel="closeBuildLogModel">
      <build-log v-if="buildLogVisible" :temp="temp" />
    </a-modal>
    <!-- 构建确认 -->
    <a-modal width="40vw" v-model="buildConfirmVisible" title="构建确认弹窗" @ok="handleStartBuild" :maskClosable="false">
      <a-form-model :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item label="名称" prop="name">
          <a-input readOnly disabled v-model="temp.name" />
        </a-form-model-item>
        <a-form-model-item label="分支" prop="branchName">
          <custom-select
            v-model="temp.branchName"
            :data="branchList"
            @onRefreshSelect="loadBranchListById(temp.repositoryId)"
            inputPlaceholder="自定义分支通配表达式"
            selectPlaceholder="请选择构建对应的分支"
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
        </a-form-model-item>
        <a-form-model-item v-if="(branchTagList && branchTagList.length) || (temp.branchTagName && temp.branchTagName.length)" label="标签(TAG)" prop="branchTagName">
          <custom-select
            v-model="temp.branchTagName"
            :data="branchTagList"
            @onRefreshSelect="loadBranchListById(temp.repositoryId)"
            inputPlaceholder="自定义标签通配表达式"
            selectPlaceholder="选择构建的标签,不选为最新提交"
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
        </a-form-model-item>
        <a-form-model-item prop="resultDirFile" label="产物目录">
          <a-input v-model="temp.resultDirFile" placeholder="不填写则不更新" />
        </a-form-model-item>
        <a-form-model-item label="构建备注" prop="buildRemark">
          <a-textarea v-model="temp.buildRemark" :maxLength="240" placeholder="请输入构建备注,长度小于 240" :auto-size="{ minRows: 3, maxRows: 5 }" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 查看命令示例 -->
    <a-modal width="50vw" v-model="viewScriptTemplVisible" title="构建命令示例" :footer="null" :maskClosable="false">
      <a-collapse
        :activeKey="
          buildScipts.map((item, index) => {
            return index + '';
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
                      temp.script = opt.value;
                      viewScriptTemplVisible = false;
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
import CustomSelect from "@/components/customSelect";
import BuildLog from "./log";
import {getRepositoryListAll} from "@/api/repository";
import {
  buildModeMap,
  clearBuid,
  deleteBuild,
  editBuild,
  getBranchList,
  getBuildGroupAll,
  getBuildList,
  getTriggerUrl,
  releaseMethodMap,
  resetTrigger,
  startBuild,
  statusMap,
  stopBuild,
} from "@/api/build-info";
import {afterOptList, afterOptListSimple, getDishPatchListAll} from "@/api/dispatch";
import {getNodeListAll, getProjectListAll} from "@/api/node";
import {getSshListAll} from "@/api/ssh";
import {itemGroupBy, parseTime} from "@/utils/time";
import codeEditor from "@/components/codeEditor";
import {CHANGE_PAGE, COMPUTED_PAGINATION, CRON_DATA_SOURCE, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";
import Vue from "vue";
import {dockerSwarmListAll, dockerSwarmServicesList} from "@/api/docker-swarm";
import {getScriptListAll} from "@/api/server-script";

export default {
  components: {
    BuildLog,
    CustomSelect,
    codeEditor,
  },
  data() {
    return {
      releaseMethodMap,
      buildModeMap,
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      cronDataSource: CRON_DATA_SOURCE,
      // 动态列表参数
      groupList: [],
      list: [],
      statusMap: statusMap,
      repositoryList: [],
      tempVue: null,
      // 当前仓库信息
      tempRepository: {},
      // 当前构建信息的 extraData 属性
      tempExtraData: {},
      viewScriptTemplVisible: false,
      buildScipts: [
        {
          title: "Java 项目(示例参考，具体还需要根据项目实际情况来决定)",
          children: [
            {
              title: "不执行，也不编译测试用例 mvn clean package -Dmaven.test.skip=true",
              value: "mvn clean package -Dmaven.test.skip=true",
            },
            {
              title: "打包生产环境包 mvn clean package -Dmaven.test.skip=true -Pprod",
              value: "mvn clean package -Dmaven.test.skip=true -Pprod",
            },
            {
              title: "打包测试环境包 mvn clean package -Dmaven.test.skip=true -Ptest",
              value: "mvn clean package -Dmaven.test.skip=true -Ptest",
            },
            {
              title: "不执行，但是编译测试用例 mvn clean package -DskipTests",
              value: "mvn clean package -DskipTests",
            },
            {
              title: "mvn clean package",
              value: "mvn clean package",
            },
            {
              title: "指定 pom 文件打包 mvn -f xxx/pom.xml clean package",
              value: "mvn -f xxx/pom.xml clean package",
            },
            {
              title: "指定 settings 文件打包 mvn -s xxx/settings.xml clean package",
              value: "mvn -s xxx/settings.xml clean package",
            },
          ],
        },
        {
          title: "vue 项目(示例参考，具体还需要根据项目实际情况来决定)",
          children: [
            {
              title: "需要先安装依赖 npm i && npm run build",
              value: "npm i && npm run build",
            },
            {
              title: "打包正式环境 npm i && npm run build:prod",
              value: "npm i && npm run build:prod",
            },
            {
              title: "打包预发布环境 npm i && npm run build:stage",
              value: "npm i && npm run build:stage",
            },
            {
              title: "需要先安装依赖 yarn && yarn run build",
              value: "yarn && yarn run build",
            },
            {
              title: "指定目录打包 yarn && yarn --cwd xxx build",
              value: "yarn && yarn --cwd xxx build",
            },
          ],
        },
      ],
      branchList: [],
      branchTagList: [],
      dispatchList: [],
      cascaderList: [],
      sshList: [],
      dockerSwarmList: [],
      //集群下 服务下拉数据
      swarmServiceListOptions: [],
      scriptList: [],
      temp: {},
      // 页面控制变量
      editBuildVisible: false,
      triggerVisible: false,
      buildLogVisible: false,
      afterOptList,
      afterOptListSimple,
      buildConfirmVisible: false,
      columns: [
        { title: "名称", dataIndex: "name", sorter: true, ellipsis: true, scopedSlots: { customRender: "name" } },

        {
          title: "分支",
          dataIndex: "branchName",
          ellipsis: true,
          scopedSlots: { customRender: "branchName" },
        },
        { title: "方式", dataIndex: "buildMode", align: "center", width: 80, ellipsis: true, scopedSlots: { customRender: "buildMode" } },
        { title: "状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        {
          title: "构建 ID",
          dataIndex: "buildId",
          width: 90,
          ellipsis: true,
          align: "center",
          scopedSlots: { customRender: "buildId" },
        },
        {
          title: "修改人",
          dataIndex: "modifyUser",
          width: 130,
          ellipsis: true,
          sorter: true,
          scopedSlots: { customRender: "modifyUser" },
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          customRender: (text) => {
            if (!text) {
              return "";
            }
            return parseTime(text);
          },
          width: 170,
        },
        {
          title: "其他信息",
          dataIndex: "releaseMethod",
          width: 100,
          ellipsis: true,
          scopedSlots: { customRender: "releaseMethod" },
        },
        // {
        //   title: "产物目录",
        //   dataIndex: "resultDirFile",
        //   ellipsis: true,
        //   width: 100,
        //   scopedSlots: { customRender: "resultDirFile" },
        // },
        // { title: "构建命令", width: 100, dataIndex: "script", ellipsis: true, scopedSlots: { customRender: "script" } },
        {
          title: "操作",
          dataIndex: "operation",
          width: 130,
          scopedSlots: { customRender: "operation" },
          align: "center",
          // fixed: "right",
        },
      ],
      rules: {
        name: [{ required: true, message: "请填写构建名称", trigger: "blur" }],
        buildMode: [{ required: true, message: "请选择构建方式", trigger: "blur" }],
        releaseMethod: [{ required: true, message: "请选择发布操作", trigger: "blur" }],
        branchName: [{ required: true, message: "请选择分支", trigger: "blur" }],
        script: [{ required: true, message: "请填写构建命令", trigger: "blur" }],
        resultDirFile: [{ required: true, message: "请填写产物目录", trigger: "blur" }],
        releasePath: [{ required: true, message: "请填写发布目录", trigger: "blur" }],
        repositoryId: [{ required: true, message: "请填选择构建的仓库", trigger: "blur" }],
      },
      dslDefault:
        "# 基础镜像 目前仅支持 ubuntu-latest\n" +
        "runsOn: ubuntu-latest\n" +
        "# 使用哪个 docker 构建,填写 docker 标签 默认查询可用的第一个,如果 tag 查询出多个也选择第一个结果\n" +
        "fromTag: xxx\n" +
        "# version 需要在对应镜像源中存在\n" +
        "# java 镜像源 https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/\n" +
        "# maven 镜像源 https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/\n" +
        "# node 镜像源 https://registry.npmmirror.com/-/binary/node/\n" +
        "steps:\n" +
        "  - uses: java\n" +
        "    version: 8\n" +
        "  - uses: maven\n" +
        "    version: 3.8.5\n" +
        "  - uses: node\n" +
        "    version: 16.3.0\n" +
        "#  - uses: go\n" +
        "#    version: 1.17.6\n" +
        "#  - uses: python3\n" +
        "#    version: 3.6.6\n" +
        "# 将容器中的文件缓存到 docker 卷中\n" +
        "  - uses: cache\n" +
        "    path: /root/.m2\n" +
        "  - run: npm config set registry https://registry.npmmirror.com\n" +
        "# 内置变量 ${JPOM_WORKING_DIR} ${JPOM_BUILD_ID}\n" +
        "  - run: cd  ${JPOM_WORKING_DIR}/web-vue && npm i && npm run build\n" +
        "  - run: cd ${JPOM_WORKING_DIR} && mvn package -s script/settings.xml\n" +
        "# 宿主机目录和容器目录挂载 /host:/container:ro\n" +
        "# binds:\n" +
        "#  - /Users/user/.m2/settings.xml:/root/.m2/\n" +
        "# 宿主机文件上传到容器 /host:/container:true\n" +
        "# dirChildrenOnly = true will create /var/data/titi and /var/data/tata dirChildrenOnly = false will create /var/data/root/titi and /var/data/root/tata\n" +
        "# copy:\n" +
        "#  - /Users/user/.m2/settings.xml:/root/.m2/:false\n" +
        "# 给容器添加环境变量\n" +
        "env:\n" +
        "  NODE_OPTIONS: --max-old-space-size=900",
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
    selectSshDirs() {
      if (!this.sshList || this.sshList.length <= 0) {
        return [];
      }
      const findArray = this.sshList.filter((item) => {
        if (Array.isArray(this.tempExtraData.releaseMethodDataId_3)) {
          return item.id === this.tempExtraData.releaseMethodDataId_3[0];
        }
        return item.id === this.tempExtraData.releaseMethodDataId_3;
      });
      if (findArray.length) {
        const fileDirs = findArray[0].fileDirs;
        if (!fileDirs) {
          return [];
        }
        return JSON.parse(fileDirs).map((item) => {
          return (item + "/").replace(new RegExp("//", "gm"), "/");
        });
      }
      return [];
    },
  },
  watch: {},
  created() {
    this.loadData();
    this.loadGroupList();
  },
  methods: {
    // 页面引导
    loadDockerSwarmListAll() {
      dockerSwarmListAll().then((res) => {
        this.dockerSwarmList = res.data;
      });
    },
    // 分组数据
    loadGroupList() {
      getBuildGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data;
        }
      });
    },
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getBuildList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 加载仓库列表
    loadRepositoryList(fn) {
      getRepositoryListAll().then((res) => {
        if (res.code === 200) {
          this.repositoryList = res.data;
          fn && fn();
        }
      });
    },
    // 加载脚本列表
    loadScriptListList() {
      getScriptListAll().then((res) => {
        if (res.code === 200) {
          this.scriptList = res.data;
        }
      });
    },
    // 加载节点分发列表
    loadDispatchList() {
      this.dispatchList = [];
      getDishPatchListAll().then((res) => {
        if (res.code === 200) {
          this.dispatchList = res.data;
        }
      });
    },
    // 加载节点项目列表
    loadNodeProjectList() {
      this.cascaderList = [];
      getNodeListAll().then((res0) => {
        if (res0.code !== 200) {
          return;
        }
        getProjectListAll().then((res) => {
          if (res.code === 200) {
            let temp = itemGroupBy(res.data, "nodeId", "value", "children");

            this.cascaderList = temp.map((item) => {
              let findArra = res0.data.filter((res0Item) => {
                return res0Item.id === item.value;
              });
              item.label = findArra.length ? findArra[0].name : "未知";
              item.children = item.children.map((item2) => {
                return {
                  label: item2.name,
                  value: item2.projectId,
                };
              });
              return item;
            });
          }
        });
      });
    },
    // 加载 SSH 列表
    loadSshList() {
      return new Promise((resolve) => {
        this.sshList = [];
        getSshListAll().then((res) => {
          if (res.code === 200) {
            this.sshList = res.data;
            resolve();
          }
        });
      });
    },
    // 筛选
    handleFilter() {
      this.loadData();
      // this.loadRepositoryList();
    },
    // 选择仓库
    changeRepositpry(value) {
      this.repositoryList.forEach((element) => {
        if (element.id === value) {
          this.tempRepository = element;
          this.temp.branchName = "";
          this.temp.branchTagName = "";
          // 刷新分支
          this.loadBranchList();
        }
      });
    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.branchList = [];
      this.loadRepositoryList();
      this.loadDispatchList();
      this.loadNodeProjectList();
      this.loadSshList();
      this.loadDockerSwarmListAll();
      this.loadScriptListList();
      this.editBuildVisible = true;
      this.tempExtraData = {
        cacheBuild: true,
        saveBuildFile: true,
      };
      this.$refs["editBuildForm"]?.resetFields();
    },
    // 复制
    copyItem(record) {
      const temp = Object.assign({}, record);
      delete temp.id;
      temp.name = temp.name + "副本";
      this.handleEdit(temp);
    },
    // 修改
    handleEdit(record) {
      this.$refs["editBuildForm"]?.resetFields();
      this.temp = Object.assign({}, record);
      this.temp.buildMode = this.temp.buildMode || 0;
      // 设置当前临时的 额外构建信息
      this.tempExtraData = JSON.parse(record.extraData) || {};
      if (typeof this.tempExtraData === "string") {
        this.tempExtraData = JSON.parse(this.tempExtraData);
      }
      if (this.tempExtraData.cacheBuild === undefined) {
        this.tempExtraData.cacheBuild = true;
      }
      if (this.tempExtraData.saveBuildFile === undefined) {
        this.tempExtraData.saveBuildFile = true;
      }

      // 设置发布方式的数据
      if (this.tempExtraData.releaseMethodDataId) {
        if (record.releaseMethod === 1) {
          this.tempExtraData.releaseMethodDataId_1 = this.tempExtraData.releaseMethodDataId;
        }
        if (record.releaseMethod === 2) {
          this.temp = {
            ...this.temp,
            releaseMethodDataIdList: this.tempExtraData.releaseMethodDataId.split(":"),
          };
        }
        if (record.releaseMethod === 3) {
          this.tempExtraData.releaseMethodDataId_3 = this.tempExtraData.releaseMethodDataId.split(",");
        }
      }
      this.tempExtraData = { ...this.tempExtraData };
      this.loadRepositoryList(() => {
        // 从仓库列表里匹配对应的仓库信息

        this.tempRepository = this.repositoryList.filter((element) => this.temp.repositoryId === element.id)[0];
        this.editBuildVisible = true;
        this.loadBranchList();
      });

      this.loadDispatchList();
      this.loadDockerSwarmListAll();
      this.loadNodeProjectList();
      this.loadScriptListList();
      this.loadSshList().then(() => {
        if (this.tempExtraData.releaseMethodDataId_3) {
          //
          const findDirs = this.selectSshDirs
            .filter((item) => {
              return this.tempExtraData.releasePath && this.tempExtraData.releasePath.indexOf(item) > -1;
            })
            .sort((item1, item2) => {
              return item2.length - item1.length;
            });
          const releaseSshDir = findDirs[0] || "";
          this.tempExtraData = { ...this.tempExtraData, releaseSshDir: releaseSshDir, releasePath2: (this.tempExtraData.releasePath || "").slice(releaseSshDir.length) };
        }
      });
    },
    // 获取仓库分支
    loadBranchList() {
      if (this.tempRepository.repoType !== 0) {
        return;
      }
      this.loadBranchListById(this.tempRepository?.id);
    },
    loadBranchListById(id) {
      this.branchList = [];
      this.branchTagList = [];
      const params = {
        repositoryId: id,
      };
      getBranchList(params).then((res) => {
        if (res.code === 200) {
          this.branchList = res.data[0];
          this.branchTagList = res.data[1];
        }
      });
    },
    // 提交节点数据
    handleEditBuildOk() {
      // 检验表单
      this.$refs["editBuildForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const tempExtraData = Object.assign({}, this.tempExtraData);
        // 设置参数
        if (this.temp.releaseMethod === 2) {
          if (this.temp.releaseMethodDataIdList.length < 2) {
            this.$notification.warn({
              message: "请选择节点项目,可能是节点中不存在任何项目,需要去节点中创建项目",
            });
            return false;
          }
          tempExtraData.releaseMethodDataId_2_node = this.temp.releaseMethodDataIdList[0];
          tempExtraData.releaseMethodDataId_2_project = this.temp.releaseMethodDataIdList[1];
        } else if (this.temp.releaseMethod === 3) {
          //  (this. tempExtraData.releasePath || '').slice(releaseSshDir.length);
          tempExtraData.releasePath = ((tempExtraData.releaseSshDir || "") + "/" + (tempExtraData.releasePath2 || "")).replace(new RegExp("//", "gm"), "/");
          tempExtraData.releaseMethodDataId_3 = (tempExtraData.releaseMethodDataId_3 || []).join(",");
        }

        this.temp = {
          ...this.temp,
          extraData: JSON.stringify(tempExtraData),
        };
        // 提交数据
        editBuild(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            //this.$refs["editBuildForm"].resetFields();
            this.editBuildVisible = false;
            this.handleFilter();
            this.loadGroupList();
          }
        });
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除构建信息么？删除也将同步删除所有的构建历史记录信息",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteBuild(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 触发器
    handleTrigger(record) {
      this.temp = Object.assign({}, record);
      this.tempVue = Vue;
      getTriggerUrl(record.id).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res);
          this.triggerVisible = true;
        }
      });
    },
    // 重置触发器
    resetTrigger() {
      resetTrigger(this.temp.id).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.fillTriggerResult(res);
        }
      });
    },
    fillTriggerResult(res) {
      this.temp.triggerBuildUrl = `${location.protocol}//${location.host}${res.data.triggerBuildUrl}`;
      this.temp.batchTriggerBuildUrl = `${location.protocol}//${location.host}${res.data.batchTriggerBuildUrl}`;
      this.temp.batchBuildStatusUrl = `${location.protocol}//${location.host}${res.data.batchBuildStatusUrl}`;
      // this.temp.id = res.data.id;
      // this.temp.token = res.data.token;
      this.temp.batchBuildStatusUrl2 = `${this.temp.batchBuildStatusUrl}?id=${res.data.id}&token=${res.data.token}`;
      this.temp = { ...this.temp };
    },
    // 清除构建
    handleClear(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要清除构建信息么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          clearBuid(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 开始构建
    handleConfirmStartBuild(record) {
      this.temp = Object.assign({}, record);
      this.buildConfirmVisible = true;
      this.branchList = [];
      this.branchTagList = [];
      // this.$confirm({
      //   title: "系统提示",
      //   content: "确定要开始构建 【名称：" + record.name + "】 【分支：" + record.branchName + "】 吗？",
      //   okText: "确认",
      //   cancelText: "取消",
      //   onOk: () => {

      // });
    },
    handleStartBuild() {
      this.buildConfirmVisible = false;
      startBuild({
        id: this.temp.id,
        buildRemark: this.temp.buildRemark,
        resultDirFile: this.temp.resultDirFile,
        branchTagName: this.temp.branchTagName,
        branchName: this.temp.branchName,
      }).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.handleFilter();
          // 自动打开构建日志
          this.handleBuildLog({
            id: this.temp.id,
            buildId: res.data,
          });
        }
      });
    },
    // 停止构建
    handleStopBuild(record) {
      this.$confirm({
        title: "系统提示",
        content: "确定要取消构建 【名称：" + record.name + "】 吗？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          this.temp = Object.assign({}, record);
          stopBuild(this.temp.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.handleFilter();
            }
          });
        },
      });
    },
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.id,
        buildId: record.buildId,
      };
      this.buildLogVisible = true;
    },
    // 关闭日志对话框
    closeBuildLogModel() {
      this.handleFilter();
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
    // 选择发布集群时 渲染服务名称 数据
    selectSwarm() {
      this.swarmServiceListOptions = [];
      if (this.tempExtraData.dockerSwarmId) {
        // 选中时才处理
        dockerSwarmServicesList({
          id: this.tempExtraData.dockerSwarmId,
        }).then((res) => {
          if (res.code === 200) {
            this.swarmServiceListOptions = res.data;
          }
        });
      } else {
        this.swarmServiceListOptions = [];
      }
    },
  },
};
</script>
<style scoped></style>
