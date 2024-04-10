<template>
  <div>
    <a-form :label-col="formLable.labelCol" :wrapper-col="formLable.wrapperCol">
      <a-form-item label="发布类型" name="subStageType" :validate-status="useData.subStageType ? '' : 'error'">
        <a-radio-group v-model:value="useData.subStageType" :disabled="!!useData.subStageType">
          <a-radio value="PUBLISH_PROJECT">项目</a-radio>
        </a-radio-group>
        <template v-if="!!useData.subStageType" #help>选择后不能切换</template>
        <template v-if="!useData.subStageType" #extra>
          <span class="ant-form-item-explain-error">请选择发布类型</span>
        </template>
      </a-form-item>
    </a-form>
    <widgetPublishProject
      v-if="useData.subStageType === 'PUBLISH_PROJECT'"
      v-model:data="useData"
      :form-lable="formLable"
    ></widgetPublishProject>
    <a-form :label-col="formLable.labelCol" :wrapper-col="formLable.wrapperCol">
      <a-form-item label="产物" name="artifacts">
        <a-list size="small" bordered :data-source="useData.artifacts">
          <template #renderItem="{ item, index }">
            <a-list-item>
              <a-card size="small" style="width: 100%">
                <template #title>产物{{ index + 1 }}</template>
                <template #extra>
                  <a-space>
                    <a-button type="primary" size="small" @click="item.path.push('')">新增目录</a-button>
                    <a-button
                      type="primary"
                      danger
                      :disabled="!useData.artifacts || useData.artifacts.length <= 1"
                      size="small"
                      @click="useData?.artifacts?.splice(index, 1)"
                    >
                      删除产物
                    </a-button>
                  </a-space>
                </template>
                <a-space direction="vertical" style="width: 100%">
                  <a-input
                    v-for="(path, indexPath) in item.path"
                    :key="indexPath"
                    v-model:value="item.path[indexPath]"
                    placeholder="产物目录或者文件,相对路径"
                  >
                    <template #addonBefore> 目录{{ indexPath + 1 }} </template>
                    <template #addonAfter>
                      <a-button
                        type="primary"
                        :disabled="!item.path || item.path.length <= 1"
                        danger
                        size="small"
                        @click="item.path.splice(indexPath, 1)"
                      >
                        <DeleteOutlined />
                      </a-button>
                    </template>
                  </a-input>
                  <a-form-item label="压缩格式" name="format" help="如果是目录情况压缩包的格式">
                    <a-radio-group v-model:value="item.format">
                      <a-radio value="ZIP">ZIP</a-radio>
                      <a-radio value="TAR_GZ">TAR_GZ</a-radio>
                    </a-radio-group>
                  </a-form-item>
                </a-space>
              </a-card>
            </a-list-item>
          </template>
          <template #header>
            <a-button type="primary" size="small" @click="addArtifacts">添加产物</a-button>
          </template>
        </a-list>
      </a-form-item>
    </a-form>
  </div>
</template>
<script setup lang="ts">
import widgetPublishProject from './publish-project.vue'
import { PublishBase, PublishProject } from './types'
const props = defineProps({
  data: {
    type: Object as PropType<PublishBase & PublishProject>,
    required: true
  },
  formLable: {
    type: Object,
    required: true
  }
})
const emit = defineEmits<{ (e: 'update:data', value: object): void }>()
const useData = ref(props.data)
watch(
  () => props.data,
  (val) => {
    useData.value = val
  },
  {
    deep: true,
    immediate: false
  }
)
watch(
  () => useData.value,
  (val) => {
    emit('update:data', val)
  },
  {
    immediate: false
  }
)

// 添加产物
const addArtifacts = () => {
  const artifacts = useData.value.artifacts || []
  artifacts.push({
    path: ['']
  })
  useData.value = { ...useData.value, artifacts: artifacts }
}
</script>
