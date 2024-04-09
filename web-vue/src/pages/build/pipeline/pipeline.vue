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
          流程组:
          <a-button-group>
            <a-button type="primary" size="small" :disabled="stepsGroupCurrent < 1" danger @click="delStepGroups">
              删除
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
            >
              添加
            </a-button>
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
                    <a-collapse-panel v-for="(item, index) in repositoryList" :key="item.id" force-render>
                      <template #header>
                        <a-row :wrap="false">
                          <a-col flex="auto">
                            仓库{{ index + 1 }} <a-tag>{{ item.id }}</a-tag>
                          </a-col>
                          <a-col flex="none">
                            <a-button type="primary" danger size="small" @click.stop="delRepositoryList(index)">
                              删除
                            </a-button>
                          </a-col>
                        </a-row>
                      </template>

                      <widgetRepository
                        :key="item.id"
                        v-model:data="jsonConfig.repositories[item.id]"
                        v-model:loading="loading"
                        :form-lable="formLable"
                        @change="(data) => repositoryChange(data, item.id)"
                      />
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
                @change="(e) => enterSteps(e.target.value as string, index)"
                @search="(value) => enterSteps(value, index)"
              />
            </a-form-item>
            <a-form-item
              label="子流程"
              name="stages"
              :validate-status="jsonConfig.stageGroups[index]?.stages?.length ? '' : 'error'"
            >
              <template v-if="!jsonConfig.stageGroups[index]?.stages?.length" #extra>
                <span class="ant-form-item-explain-error">请添加子流程</span>
              </template>
              <a-form-item-rest>
                <a-space direction="vertical" style="width: 100%">
                  <a-collapse
                    v-if="jsonConfig.stageGroups[index]?.stages?.length"
                    v-model:activeKey="childStageActiveKeys[index]"
                  >
                    <a-collapse-panel
                      v-for="(childItem, childIndex) in jsonConfig.stageGroups[index]?.stages"
                      :key="childIndex"
                      force-render
                    >
                      <template #header>
                        <a-row :wrap="false">
                          <a-col flex="auto"> 子流程{{ childIndex + 1 }} </a-col>
                          <a-col flex="none">
                            <a-button type="primary" danger size="small" @click.stop="delChildStage(index, childIndex)"
                              >删除</a-button
                            >
                          </a-col>
                        </a-row>
                      </template>
                      <widgetStageBase
                        v-model:data="jsonConfig.stageGroups[index].stages[childIndex]"
                        :form-lable="formLable"
                        :repository-list="repositoryList"
                      ></widgetStageBase>
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
    <a-modal
      v-model:open="viewYamlOpen"
      destroy-on-close
      width="60vw"
      title="流水线配置源码视图"
      :mask-closable="false"
      ok-text="保存数据"
      @ok="handleYaml2Json"
    >
      <code-editor v-model:content="yamlConfig" height="60vh" :show-tool="true" :options="{ mode: 'yml' }">
        <template #tool_before> <a-tag>1</a-tag></template>
      </code-editor>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import widgetRepository from './widget/repository.vue'
import widgetStageBase from './widget/stages-base.vue'
import { randomStr } from '@/utils/const'
import { JsonConfigType, PublishBase, StagesConfig, StagesExec, PublishProject } from './widget/types'
import { editBuildPipeline, getBuildPipelineItem } from '@/api/build/pipeline'

const props = defineProps({
  id: {
    type: String,
    required: false,
    default: ''
  }
})
const formLable = ref({
  labelCol: { span: 2 },
  wrapperCol: { span: 22 }
})

const emit = defineEmits(['close'])

const jsonConfig = ref<JsonConfigType>({
  version: '1.0.0',
  repositories: {},
  stageGroups: []
})
const formData = ref<any>({})

const loading = ref(!!props.id)

const loadPipelineData = () => {
  loading.value = true
  getBuildPipelineItem({
    id: props.id
  })
    .then((res) => {
      if (res.code === 200) {
        formData.value = { ...formData.value, name: res.data.name, group: res.data.group }
        //
        jsonConfig.value = JSON.parse(res.data.jsonConfig)
        jsonConfig.value.stageGroups
          .map((item, index) => {
            return {
              // '流程组' + stepsItems.value.length,
              title: '流程组' + (index + 1),
              description: item.description
            } as StepsItem
          })
          .forEach((item) => {
            stepsItems.value.push(item)
          })

        console.log(jsonConfig.value)
      }
    })
    .finally(() => {
      loading.value = false
    })
}
if (props.id) {
  // 获取数据
  loadPipelineData()
} else {
  jsonConfig.value = {
    repositories: {},
    stageGroups: []
  }
}

////////////////////////////////////////////////////////////////////////////////

const childStageActiveKeys = ref<Array<Array<number>>>([])

// 添加子流程
const addChildStage = (index: number) => {
  console.log(jsonConfig.value.stageGroups[index] && jsonConfig.value.stageGroups[index].stages)
  const stages = (jsonConfig.value.stageGroups[index] && jsonConfig.value.stageGroups[index].stages) || []
  stages.push({
    description: '子流程' + (stages.length + 1)
  })
  jsonConfig.value.stageGroups[index] = {
    ...jsonConfig.value.stageGroups[index],
    stages: stages
  }
  childStageActiveKeys.value[index].push(stages.length - 1)
  console.log(stages)
}

// 删除子流程

const delChildStage = (index: number, index2: number) => {
  $confirm({
    title: '系统提示',
    zIndex: 1009,
    content: '确定要删除此子流程' + (index2 + 1) + '吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      jsonConfig.value?.stageGroups[index]?.stages?.splice(index2, 1)
      //
      childStageActiveKeys.value[index] = childStageActiveKeys.value[index].filter((item: number) => item != index2)
      // console.log(jsonConfig.value.stageGroups[index])
    }
  })
}

////////////////////////////////////////////////////////////////////////////////

const repositoryNameCache = ref<Record<string, string>>({})

const repositoryActiveKeys = ref<Array<string>>([])

const repositoryList = computed(() => {
  if (!jsonConfig.value || !jsonConfig.value.repositories) {
    return []
  }
  const list: Array<{ id: string; sort: number; name: string }> = []
  for (let key in jsonConfig.value.repositories) {
    list.push({
      id: key,
      sort: jsonConfig.value.repositories[key].sort || 0,
      name: repositoryNameCache.value[key]
    })
  }
  list.sort((a, b) => a.sort - b.sort)
  return list.map((item, index) => {
    return { ...item, sort: index }
  })
})
// 添加仓库
const addRepositoryList = () => {
  let repositoryTag
  do {
    repositoryTag = randomStr(4)
  } while (jsonConfig.value.repositories[repositoryTag])

  jsonConfig.value.repositories[repositoryTag] = {
    // 找到当前排序值的最大并且 +1 ，保证新增的仓库排序处于最后
    sort: repositoryList.value.length ? Math.max(...repositoryList.value.map((item) => item.sort)) + 1 : 0
  }
  //
  repositoryActiveKeys.value.push(repositoryTag)
}

// 删除仓库
const delRepositoryList = (index: number) => {
  $confirm({
    title: '系统提示',
    zIndex: 1009,
    content: '确定要删除此源仓库吗？删除源仓库后关闭的子流程将无法正常执行',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      const repository = repositoryList.value[index]
      delete jsonConfig.value.repositories[repository.id]

      // 修改排序值
      repositoryList.value.forEach((item) => {
        jsonConfig.value.repositories[item.id] = {
          ...jsonConfig.value.repositories[item.id],
          sort: item.sort
        }
      })
    }
  })
}

const repositoryChange = (data: any, id: string) => {
  repositoryNameCache.value = { ...repositoryNameCache.value, [id]: data.name }
}

const groupList = ref([])

const stepsGroupCurrent = ref<number>(0)
type StepsItem = {
  title: string
  description?: string
}

const stepsItems = ref<StepsItem[]>([
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
  !childStageActiveKeys.value[index] && (childStageActiveKeys.value[index] = [])
  // console.log(jsonConfig.value)
}

// 同步描述
const syncStepsDescription = () => {
  jsonConfig.value.stageGroups.forEach((item, index) => {
    const realIndex = index + 1
    stepsItems.value[realIndex] = { ...stepsItems.value[realIndex], description: item.description }
  })
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
  $confirm({
    title: '系统提示',
    zIndex: 1009,
    content: '确定要删除此流程组' + stepsGroupCurrent.value + '吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
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
  })
}
// 流程组切换
const stepsChange = (current: number) => {
  console.log('切换流程组', current)
}

////////////////////////////////////////////////////////////

const checkSteps = (stage: StagesConfig, groupIndex: number, stageIndex: number) => {
  //console.log(stage)
  if (!stage || !stage.stageType) {
    $notification.warn({
      message: `流程组${groupIndex + 1}中的子流程${stageIndex + 1}请选择流程类型`
    })
    return false
  }
  if (!stage.repoTag) {
    $notification.warn({
      message: `流程组${groupIndex + 1}中的子流程${stageIndex + 1}请选择源仓库标记`
    })
    return false
  }
  if (stage.stageType === 'EXEC') {
    if (!(stage as StagesExec).commands) {
      $notification.warn({
        message: `流程组${groupIndex + 1}中的子流程${stageIndex + 1}请输入执行命令`
      })
      return false
    }
    //return false
  } else if (stage.stageType === 'PUBLISH') {
    if (!(stage as PublishBase).publishType) {
      $notification.warn({
        message: `流程组${groupIndex + 1}中的子流程${stageIndex + 1}请选择发布方式`
      })
      return false
    }
    const artifacts = (stage as PublishBase)?.artifacts || []
    for (let i = 0; i < artifacts.length; i++) {
      const item = artifacts[i]
      if (!item.path?.filter((item) => !!item).length) {
        $notification.warn({
          message: `流程组${groupIndex + 1}中的子流程${stageIndex + 1}产物${i + 1}请配置产物目录或者文件`
        })
        return false
      }
      // if (
      //  ?.filter((item) => {
      //     return item.path?.filter((item) => !!item).length
      //   }).length
      // ) {

      // }
    }
    if ((stage as PublishBase).publishType === 'PROJECT') {
      // 项目发布
      if (!(stage as PublishProject).nodeId) {
        $notification.warn({
          message: `流程组${groupIndex + 1}中的子流程${stageIndex + 1}请选择节点`
        })
        return false
      }
      if (!(stage as PublishProject).projectId) {
        $notification.warn({
          message: `流程组${groupIndex + 1}中的子流程${stageIndex + 1}请选择项目`
        })
        return false
      }
      if (!(stage as PublishProject).artifacts?.length) {
        $notification.warn({
          message: `流程组${groupIndex + 1}中的子流程${stageIndex + 1}请添加产物`
        })
        return false
      }
    }
  }
  return true
}

const handleEditSave = () => {
  const formDataTemp = formData.value
  if (!formDataTemp.name) {
    $notification.warn({
      message: '请输入流水线名称'
    })
    stepsGroupCurrent.value = 0
    return false
  }
  const jsonConfigTemp = jsonConfig.value

  jsonConfigTemp.version = jsonConfigTemp.version || '1.0.0'

  if (!repositoryList.value.length) {
    $notification.warn({
      message: '请添加源仓库'
    })
    stepsGroupCurrent.value = 0
    return false
  }

  for (let key in jsonConfigTemp.repositories) {
    if (!jsonConfigTemp.repositories[key].repositoryId) {
      $notification.warn({
        message: '请选择源仓库：' + key
      })
      stepsGroupCurrent.value = 0
      repositoryActiveKeys.value = [key]
      return false
    }
    if (!jsonConfigTemp.repositories[key].branchName && !jsonConfigTemp.repositories[key].branchTagName) {
      $notification.warn({
        message: '请选择源仓库分支或者标签：' + key
      })
      repositoryActiveKeys.value = [key]
      stepsGroupCurrent.value = 0
      return false
    }
  }
  if (!jsonConfigTemp.stageGroups.length) {
    $notification.warn({
      message: '请添加流水线阶段或者存在未确认流程组'
    })

    return false
  }
  for (let i = 0; i < jsonConfigTemp.stageGroups.length; i++) {
    const stageGroups = jsonConfigTemp.stageGroups[i]
    if (!stageGroups.stages.length) {
      $notification.warn({
        message: '流程组' + (i + 1) + '没有子流程'
      })
      stepsGroupCurrent.value = i + 1
      return false
    }
    // 检查子流程
    for (let j = 0; j < stageGroups.stages.length; j++) {
      const stage = stageGroups.stages[j]
      if (!checkSteps(stage, i, j)) {
        stepsGroupCurrent.value = i + 1
        return false
      }
    }
  }
  if (jsonConfigTemp.stageGroups.length != stepsItems.value.length - 1) {
    $notification.warn({
      message: '存在未确认的流程请检查'
    })
    return false
  }
  // console.log(jsonConfigTemp, formDataTemp)
  loading.value = true
  editBuildPipeline({
    ...formDataTemp,
    jsonConfig: jsonConfigTemp,
    id: props.id
  })
    .then((res) => {
      if (res.code === 200) {
        $notification.success({
          message: res.msg
        })
        emit('close')
      }
    })
    .finally(() => {
      loading.value = false
    })
}

////////////////

import yaml from 'js-yaml'

const viewYamlOpen = ref(false)
const yamlConfig = ref('')
const handleViewYaml = () => {
  yamlConfig.value = yaml.dump(jsonConfig.value)
  viewYamlOpen.value = true
}

const handleYaml2Json = () => {
  jsonConfig.value = { ...yaml.load(yamlConfig.value) }
  // loading.value = true
  // nextTick(() => {
  //   yaml.load(yamlConfig.value)
  //   loading.value = false
  // })
  viewYamlOpen.value = false
  syncStepsDescription()
}

//----------
defineExpose({
  handleEditSave,
  handleViewYaml
})
</script>
