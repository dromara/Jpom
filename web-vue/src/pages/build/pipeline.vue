<template>
  <div>
    <a-spin tip="加载构建数据中" :spinning="loading">
      <a-card>
        <template #title>
          <a-row>
            <a-col :span="22">
              <a-steps
                v-model:current="stepsGroupCurrent"
                size="small"
                :items="stepsItems"
                @change="stepsChange"
              ></a-steps>
            </a-col>
          </a-row>
        </template>
        <template #extra>
          <a-button-group>
            <a-button type="primary" size="small" :disabled="stepsGroupCurrent < 1" danger @click="delStepGroups">
              删除流程组
            </a-button>
            <!-- 当前流程组未确认 或者 已经确认的流程组小于流程组头 -->
            <a-button
              type="primary"
              size="small"
              :disabled="
                (stepsGroupCurrent > 0 && !jsonConfig.stageGroups[stepsGroupCurrent - 1]) ||
                jsonConfig.stageGroups.length < stepsItems.length - 1
              "
              @click="addStepGroups"
              >添加流程组</a-button
            >
          </a-button-group>
        </template>

        <div v-show="stepsGroupCurrent === 0">
          <!-- 流水线基础信息 -->
          <a-form :model="formData" :label-col="formLable.labelCol" :wrapper-col="formLable.wrapperCol">
            <a-form-item label="名称" name="name" :validate-status="formData.name ? '' : 'error'">
              <a-input v-model:value="formData.name" :max-length="50" placeholder="名称" />
              <template v-if="!formData.name" #extra>
                <span class="ant-form-item-explain-error">请输入流水线名称</span>
              </template>
            </a-form-item>
            <a-form-item label="分组" name="group">
              <custom-select
                v-model:value="formData.group"
                :max-length="50"
                :data="groupList"
                input-placeholder="新增分组"
                select-placeholder="选择分组"
              >
              </custom-select>
            </a-form-item>

            <a-form-item
              label="源仓库"
              name="repository"
              :validate-status="repositoryList && repositoryList.length ? '' : 'error'"
            >
              <template v-if="!repositoryList || !repositoryList.length" #extra>
                <span class="ant-form-item-explain-error">请添加源仓库</span>
              </template>
              <a-form-item-rest>
                <a-space direction="vertical" style="width: 100%">
                  <a-collapse v-if="repositoryList && repositoryList.length" v-model:activeKey="repositoryActiveKeys">
                    <a-collapse-panel v-for="(item, index) in repositoryList" :key="item.id">
                      <template #header>
                        <a-row :wrap="false">
                          <a-col flex="auto">
                            仓库{{ index + 1 }} <a-tag>{{ item.id }}</a-tag>
                          </a-col>
                          <a-col flex="none">
                            <a-button type="primary" danger size="small" @click="delRepositoryList(index)"
                              >删除</a-button
                            >
                          </a-col>
                        </a-row>
                      </template>
                      <a-form :label-col="formLable.labelCol" :wrapper-col="formLable.wrapperCol">
                        <a-form-item
                          label="代码仓库"
                          name="repositoryId"
                          :validate-status="
                            jsonConfig.repositories[item.id] && jsonConfig.repositories[item.id].repositoryId
                              ? ''
                              : 'error'
                          "
                        >
                          <a-input-search
                            :value="`${tempRepository[item.id] && tempRepository[item.id].raw ? tempRepository[item.id].raw.name + '[' + tempRepository[item.id].raw.gitUrl + ']' : '请选择仓库'}`"
                            read-only
                            placeholder="请选择仓库"
                            enter-button="选择仓库"
                            @search="
                              () => {
                                repositoryActiveTag = item.id
                                repositoryisible[item.id] = true
                              }
                            "
                          />
                          <template
                            v-if="!jsonConfig.repositories[item.id] || !jsonConfig.repositories[item.id].repositoryId"
                            #extra
                          >
                            <span class="ant-form-item-explain-error">请选择代码仓库</span>
                          </template>
                        </a-form-item>
                        <a-form-item
                          label="使用分支"
                          name="branchName"
                          :validate-status="
                            (jsonConfig.repositories[item.id] && jsonConfig.repositories[item.id].branchName) ||
                            (jsonConfig.repositories[item.id] && jsonConfig.repositories[item.id].branchTagName)
                              ? ''
                              : 'error'
                          "
                        >
                          <template
                            v-if="
                              (!jsonConfig.repositories[item.id] || !jsonConfig.repositories[item.id].branchName) &&
                              (!jsonConfig.repositories[item.id] || !jsonConfig.repositories[item.id].branchTagName)
                            "
                            #extra
                          >
                            <span class="ant-form-item-explain-error">请选择仓库分支或者选择标签</span>
                          </template>
                          <custom-select
                            v-model:value="jsonConfig.repositories[item.id].branchName"
                            :disabled="jsonConfig.repositories[item.id].branchTagName ? true : false"
                            :data="tempRepository[item.id] && tempRepository[item.id].branchList"
                            :can-reload="true"
                            input-placeholder="自定义分支通配表达式"
                            select-placeholder="请选择构建对应的分支,必选"
                            @on-refresh-select="loadBranchList(item.id)"
                          >
                            <template #inputTips>
                              <div>
                                支持通配符(AntPathMatcher)
                                <ul>
                                  <li>? 匹配一个字符</li>
                                  <li>* 匹配零个或多个字符</li>
                                  <li>** 匹配路径中的零个或多个目录</li>
                                </ul>
                              </div>
                            </template>
                          </custom-select>
                        </a-form-item>
                        <a-form-item
                          label="标签(TAG)"
                          name="branchTagName"
                          :validate-status="
                            (jsonConfig.repositories[item.id] && jsonConfig.repositories[item.id].branchName) ||
                            (jsonConfig.repositories[item.id] && jsonConfig.repositories[item.id].branchTagName)
                              ? ''
                              : 'error'
                          "
                        >
                          <template
                            v-if="
                              (!jsonConfig.repositories[item.id] || !jsonConfig.repositories[item.id].branchName) &&
                              (!jsonConfig.repositories[item.id] || !jsonConfig.repositories[item.id].branchTagName)
                            "
                            #extra
                          >
                            <span class="ant-form-item-explain-error">请选择仓库分支或者选择标签</span>
                          </template>
                          <custom-select
                            v-model:value="jsonConfig.repositories[item.id].branchTagName"
                            :data="tempRepository[item.id] && tempRepository[item.id].branchTagList"
                            :can-reload="true"
                            input-placeholder="自定义标签通配表达式"
                            select-placeholder="选择构建的标签,不选为最新提交"
                            @on-refresh-select="loadBranchList(item.id)"
                          >
                            <template #inputTips>
                              <div>
                                支持通配符(AntPathMatcher)
                                <ul>
                                  <li>? 匹配一个字符</li>
                                  <li>* 匹配零个或多个字符</li>
                                  <li>** 匹配路径中的零个或多个目录</li>
                                </ul>
                              </div>
                            </template>
                          </custom-select>
                        </a-form-item>
                        <a-form-item
                          v-if="guideStore.getExtendPlugins.indexOf('system-git') > -1"
                          label="克隆深度"
                          name="cloneDepth"
                        >
                          <a-input-number
                            v-model:value="jsonConfig.repositories[item.id].cloneDepth"
                            style="width: 100%"
                            placeholder="自定义克隆深度，避免大仓库全部克隆"
                          />
                        </a-form-item>
                      </a-form>
                    </a-collapse-panel>
                  </a-collapse>

                  <a-button type="primary" size="small" @click="addRepositoryList">添加仓库</a-button>
                </a-space>
              </a-form-item-rest>
            </a-form-item>
          </a-form>
        </div>
        <!-- 后续自定义流程 -->
        <div
          v-for="(item, index) in Array.from(stepsItems).slice(1)"
          v-show="stepsGroupCurrent === index + 1"
          :key="index"
        >
          <a-form :label-col="formLable.labelCol" :wrapper-col="formLable.wrapperCol">
            <a-form-item v-if="!jsonConfig.stageGroups[index]" label="">
              <a-alert message="不能操作未确认的流程" banner />
            </a-form-item>
            <a-form-item
              v-if="jsonConfig.stageGroups[index] && jsonConfig.stageGroups[index].name"
              label="流程标记"
              name="stageGroupName"
            >
              <a-input :value="jsonConfig.stageGroups[index].name" placeholder="请输入流程标记" disabled />
            </a-form-item>
            <a-form-item label="流程名称" name="stageName">
              <a-input v-model:value="item.title" placeholder="请输入流程描述名称" disabled />
            </a-form-item>
            <a-form-item label="流程描述" name="stageDescription">
              <a-input-search
                v-model:value="item.description"
                placeholder="请输入流程描述名称"
                :enter-button="`${!!jsonConfig.stageGroups[index] ? '修改' : '确认'}`"
                @search="
                  (value) => {
                    enterSteps(value, index)
                  }
                "
              />
            </a-form-item>
            <a-form-item
              label="子流程"
              name="stages"
              :validate-status="
                jsonConfig.stageGroups[index] &&
                jsonConfig.stageGroups[index].stages &&
                jsonConfig.stageGroups[index].stages.length
                  ? ''
                  : 'error'
              "
            >
              <template
                v-if="
                  !jsonConfig.stageGroups[index] ||
                  !jsonConfig.stageGroups[index].stages ||
                  !jsonConfig.stageGroups[index].stages.length
                "
                #extra
              >
                <span class="ant-form-item-explain-error">请添加子流程</span>
              </template>
              <a-form-item-rest>
                <a-space direction="vertical" style="width: 100%">
                  <a-collapse
                    v-if="
                      jsonConfig.stageGroups[index] &&
                      jsonConfig.stageGroups[index].stages &&
                      jsonConfig.stageGroups[index].stages.length
                    "
                    v-model:activeKey="childStageActiveKeys[index]"
                  >
                    <a-collapse-panel
                      v-for="(childItem, childIndex) in jsonConfig.stageGroups[index].stages"
                      :key="childIndex"
                    >
                      <template #header>
                        <a-row :wrap="false">
                          <a-col flex="auto"> 子流程{{ childIndex + 1 }} </a-col>
                          <a-col flex="none">
                            <a-button type="primary" danger size="small" @click="delChildStage(index, childIndex)"
                              >删除</a-button
                            >
                          </a-col>
                        </a-row>
                      </template>
                      <a-form :label-col="formLable.labelCol" :wrapper-col="formLable.wrapperCol">
                        <a-form-item
                          label="流程类型"
                          name="stageType"
                          :validate-status="jsonConfig.stageGroups[index]?.stages[childIndex]?.stageType ? '' : 'error'"
                        >
                          <a-select
                            v-model:value="jsonConfig.stageGroups[index].stages[childIndex].stageType"
                            placeholder="选择流程类型"
                          >
                            <a-select-option value="EXEC">执行脚本</a-select-option>
                            <a-select-option value="PUBLISH">发布文件</a-select-option>
                          </a-select>
                          <template v-if="!jsonConfig.stageGroups[index]?.stages[childIndex]?.stageType" #extra>
                            <span class="ant-form-item-explain-error">请选择流程类型</span>
                          </template>
                        </a-form-item>
                        <!-- <a-form-item
                          label="流程描述"
                          name="description"
                          :validate-status="
                            (jsonConfig.repositories[item.id] && jsonConfig.repositories[item.id].branchName) ||
                            (jsonConfig.repositories[item.id] && jsonConfig.repositories[item.id].branchTagName)
                              ? ''
                              : 'error'
                          "
                        >
                          <template
                            v-if="
                              (!jsonConfig.repositories[item.id] || !jsonConfig.repositories[item.id].branchName) &&
                              (!jsonConfig.repositories[item.id] || !jsonConfig.repositories[item.id].branchTagName)
                            "
                            #extra
                          >
                            <span class="ant-form-item-explain-error">请选择仓库分支或者选择标签</span>
                          </template>
                        </a-form-item> -->
                      </a-form>
                    </a-collapse-panel>
                  </a-collapse>

                  <a-button
                    type="primary"
                    size="small"
                    :disabled="!jsonConfig.stageGroups[index]"
                    @click="addChildStage(index)"
                    >添加子流程</a-button
                  >
                </a-space>
              </a-form-item-rest>
            </a-form-item>
          </a-form>
          <!-- <template v-if="!!jsonConfig.stageGroups[index]"> 1233</template> -->
        </div>
      </a-card>
    </a-spin>
    <!-- 选择仓库 -->
    <a-drawer
      destroy-on-close
      :title="`选择仓库`"
      placement="right"
      :open="repositoryisible[repositoryActiveTag]"
      width="85vw"
      :z-index="1009"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          repositoryisible[repositoryActiveTag] = false
        }
      "
    >
      <repository
        v-if="repositoryisible[repositoryActiveTag]"
        ref="repositoryComponent"
        :choose="true"
        :choose-val="
          tempRepository[repositoryActiveTag] &&
          tempRepository[repositoryActiveTag].raw &&
          tempRepository[repositoryActiveTag].raw.id
        "
        @confirm="
          (repositoryId: any) => {
            changeRepositpry(repositoryActiveTag, repositoryId)
          }
        "
        @cancel="
          () => {
            repositoryisible[repositoryActiveTag] = false
          }
        "
      >
      </repository>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                repositoryisible[repositoryActiveTag] = false
              }
            "
          >
            取消
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                repositoryComponent.handerConfirm()
              }
            "
          >
            确认
          </a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>
<script setup lang="ts">
import { getRepositoryInfo } from '@/api/repository'
import { getBranchList } from '@/api/build-info'
import repository from '@/pages/repository/list.vue'
import { randomStr } from '@/utils/const'
import { useGuideStore } from '@/stores/guide'

const guideStore = useGuideStore()
const loading = ref(false)

const repositoryComponent = ref()
const formLable = ref({
  labelCol: { span: 2 },
  wrapperCol: { span: 20 }
})
const jsonConfig = ref<any>({
  repositories: {},
  stageGroups: []
})
////////////////////////////////////////////////////////////////////////////////

const childStageActiveKeys = ref<Array<Array<number>>>([])

// 添加子流程
const addChildStage = (index: number) => {
  const stages = (jsonConfig.value.stageGroups[index] && jsonConfig.value.stageGroups[index].stages) || []
  stages.push({})
  jsonConfig.value.stageGroups[index] = {
    ...jsonConfig.value.stageGroups[index],
    stages: stages
  }
  childStageActiveKeys.value[index].push(stages.length - 1)
}

// 删除子流程

const delChildStage = (index: number, index2: number) => {
  jsonConfig.value.stageGroups[index] &&
    jsonConfig.value.stageGroups[index].stages &&
    jsonConfig.value.stageGroups[index].stages.splice(index2, 1)
  //
  childStageActiveKeys.value[index] = childStageActiveKeys.value[index].filter((item) => item != index2)
  // console.log(jsonConfig.value.stageGroups[index])
}

////////////////////////////////////////////////////////////////////////////////

const repositoryActiveKeys = ref<Array<string>>([])
const repositoryActiveTag = ref<string>('')

const repositoryisible = ref<{
  [key: string]: boolean
}>({})

const tempRepository = ref<{
  [key: string]: {
    raw: any
    branchList: Array<string>
    branchTagList: Array<string>
  }
}>({})

const repositoryList = ref<
  Array<{
    id: string
  }>
>([])

// 添加仓库
const addRepositoryList = () => {
  let repositoryTag
  do {
    repositoryTag = randomStr(4)
  } while (jsonConfig.value.repositories[repositoryTag])

  repositoryList.value.push({
    id: repositoryTag
  })
  jsonConfig.value.repositories[repositoryTag] = {
    sort: repositoryList.value.length
  }
  //
  repositoryActiveKeys.value.push(repositoryTag)
}

// 删除仓库
const delRepositoryList = (index: number) => {
  const repository = repositoryList.value[index]
  repositoryList.value.splice(index, 1)
  delete jsonConfig.value.repositories[repository.id]
  delete tempRepository.value[repository.id]
}

const groupList = ref([])
const formData = ref<any>({})
const stepsGroupCurrent = ref<number>(0)
const stepsItems = ref([
  {
    title: '基础信息',
    description: '配置流水线信息和仓库'
  }
])

// 创建大流程
const enterSteps = (value: string, index: number) => {
  const realIndex = index + 1

  stepsItems.value[realIndex] = { ...stepsItems.value[realIndex], description: value }
  if (!jsonConfig.value.stageGroups[index] || !jsonConfig.value.stageGroups[index].name) {
    let id: string
    do {
      id = randomStr(4).toLowerCase()
      jsonConfig.value.stageGroups[index] = {
        name: 'default_steps_' + id,
        stages: []
      }
      break
    } while (jsonConfig.value.stageGroups.filter((item: any) => item.name === id).length)
  }
  jsonConfig.value.stageGroups[index] = {
    ...jsonConfig.value.stageGroups[index],
    description: value
  }
  childStageActiveKeys.value[index] = []
  // console.log(jsonConfig.value)
}

// 添加流程组
const addStepGroups = () => {
  stepsItems.value.push({
    title: '流程组' + stepsItems.value.length,
    description: '流水线阶段流程' + stepsItems.value.length
  })
  stepsGroupCurrent.value = stepsItems.value.length - 1
}

// 删除流程组
const delStepGroups = () => {
  if (stepsGroupCurrent.value < 1) {
    return
  }
  stepsItems.value.splice(stepsGroupCurrent.value, 1)
  jsonConfig.value.stageGroups.splice(stepsGroupCurrent.value, 1)
  stepsItems.value = stepsItems.value.map((item, index) => {
    if (index === 0) {
      return item
    }
    return { ...item, title: '流程组' + index }
  })
  stepsGroupCurrent.value = stepsGroupCurrent.value - 1
}
// 流程组切换
const stepsChange = (current: number) => {
  console.log(123, current)
}

// 选择仓库
const changeRepositpry = (repositoryTag: string, repositoryId: string) => {
  jsonConfig.value.repositories[repositoryTag] = {
    ...jsonConfig.value.repositories[repositoryTag],
    repositoryId: repositoryId,
    branchName: '',
    branchTagName: ''
  }
  repositoryisible.value[repositoryTag] = false
  loading.value = true
  //const repositoryId = jsonConfig.value.repositories[repositoryTag].repositoryId
  getRepositoryInfo({
    id: repositoryId
  })
    .then((res) => {
      if (res.code === 200) {
        tempRepository.value = {
          ...tempRepository.value,
          [repositoryTag]: { ...tempRepository.value[repositoryTag], raw: res.data }
        }

        // 刷新分支
        loadBranchList(repositoryTag)
      }
    })
    .finally(() => {
      loading.value = false
    })
}
// 获取仓库分支
const loadBranchList = (repositoryTag: string) => {
  const repositoryId = jsonConfig.value.repositories[repositoryTag].repositoryId
  tempRepository.value = {
    ...tempRepository.value,
    [repositoryTag]: { ...tempRepository.value[repositoryTag], branchList: [], branchTagList: [] }
  }

  const params = {
    repositoryId: repositoryId
  }
  loading.value = true
  getBranchList(params)
    .then((res) => {
      if (res.code === 200) {
        tempRepository.value = {
          ...tempRepository.value,
          [repositoryTag]: {
            ...tempRepository.value[repositoryTag],
            branchList: res.data?.branch || [],
            branchTagList: res.data?.tags || []
          }
        }
      }
    })
    .finally(() => {
      loading.value = false
    })
}
</script>
