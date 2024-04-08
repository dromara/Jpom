<template>
  <div>
    <a-form :label-col="formLable.labelCol" :wrapper-col="formLable.wrapperCol">
      <a-form-item label="流程类型" name="stageType" :validate-status="useData.stageType ? '' : 'error'">
        <a-select v-model:value="useData.stageType" placeholder="选择流程类型">
          <a-select-option value="EXEC">执行脚本</a-select-option>
          <a-select-option value="PUBLISH">发布文件</a-select-option>
        </a-select>
        <template v-if="!useData.stageType" #extra>
          <span class="ant-form-item-explain-error">请选择流程类型</span>
        </template>
      </a-form-item>
      <a-form-item label="流程描述" name="description" :validate-status="useData.description ? '' : 'error'">
        <a-input v-model:value="useData.description" placeholder="请输入子流程描述" />
        <template v-if="!useData.description" #extra>
          <span class="ant-form-item-explain-error">请输入子流程描述</span>
        </template>
      </a-form-item>
      <a-form-item label="仓库标记" name="repoTag" :validate-status="useData.repoTag ? '' : 'error'">
        <a-select v-model:value="useData.repoTag" placeholder="请选择仓库标记">
          <a-select-option v-if="!repositoryList || !repositoryList.length" value="">请先添加源仓库</a-select-option>
          <a-select-option v-for="item in repositoryList" :key="item.id">{{ item.id }}</a-select-option>
        </a-select>
        <template v-if="!useData.repoTag" #extra>
          <span class="ant-form-item-explain-error">请选择仓库标记</span>
        </template>
      </a-form-item>
    </a-form>
    <!-- 脚本执行 -->
    <widgetStageExec
      v-if="useData.stageType === 'EXEC'"
      v-model:data="useData"
      :form-lable="formLable"
    ></widgetStageExec>
  </div>
</template>
<script setup lang="ts">
import widgetStageExec from './stages-exec.vue'
const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  formLable: {
    type: Object,
    required: true
  },
  repositoryList: {
    type: Array<any>,
    required: true
  }
})
const emit = defineEmits<{ (e: 'update:data', value: object): void }>()

const useData = ref(props.data)

watch(
  () => useData.value,
  (val) => {
    emit('update:data', val)
  },
  {
    immediate: true
  }
)
</script>
