<template>
  <div>
    <a-form :label-col="formLable.labelCol" :wrapper-col="formLable.wrapperCol">
      <a-form-item label="节点" name="nodeId" :validate-status="useData.nodeId ? '' : 'error'">
        <a-select v-model:value="useData.nodeId" placeholder="选择发布文件的节点" @change="changeNode">
          <a-select-option v-for="item in nodeList" :key="item.id">{{ item.name }}</a-select-option>
        </a-select>
        <template v-if="!useData.nodeId" #extra>
          <span class="ant-form-item-explain-error">请选择节点</span>
        </template>
      </a-form-item>
      <a-form-item label="项目" name="projectId" :validate-status="useData.projectId ? '' : 'error'">
        <a-select v-model:value="useData.projectId" placeholder="选择发布节点对应的项目">
          <a-select-option v-for="item in projectList" :key="item.projectId">{{ item.name }}</a-select-option>
        </a-select>
        <template v-if="!useData.nodeId" #extra>
          <span class="ant-form-item-explain-error">请选择项目</span>
        </template>
      </a-form-item>
      <a-form-item name="secondaryDirectory" label="二级目录">
        <a-input v-model:value="useData.secondaryDirectory" placeholder="不填写则发布至项目根路径" />
      </a-form-item>
      <a-form-item label="配置">
        <a-form layout="inline" autocomplete="off">
          <a-form-item label="自动重启">
            <a-switch
              v-model:checked="useData.afterOpt"
              checked-children="重启"
              un-checked-children="不重启"
              checked-value="Restart"
              un-checked-value="No"
            />
          </a-form-item>
          <a-form-item label="清空发布">
            <a-switch v-model:checked="useData.clearOld" checked-children="清空" un-checked-children="不清空" />
          </a-form-item>
          <a-form-item label="增量发布">
            <a-switch v-model:checked="useData.diffSync" checked-children="是" un-checked-children="否" />
          </a-form-item>
          <a-form-item label="先停止">
            <a-switch v-model:checked="useData.uploadCloseFirst" checked-children="是" un-checked-children="否" />
          </a-form-item>
          <a-form-item label="自动解压">
            <a-switch v-model:checked="useData.unCompression" checked-children="是" un-checked-children="否" />
          </a-form-item>
        </a-form>
      </a-form-item>
    </a-form>
  </div>
</template>
<script setup lang="ts">
import { getNodeListAll } from '@/api/node'
import { getProjectListByNodeId } from '@/api/node-project'
import { publishProject } from './types'

const props = defineProps({
  data: {
    type: Object as PropType<publishProject>,
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
  () => useData.value,
  (val) => {
    emit('update:data', val)
  },
  {
    immediate: true
  }
)

const nodeList = ref<Array<{ id: string; name: string }>>([])
const projectList = ref<Array<{ id: string; name: string; projectId: string }>>([])

onMounted(() => {
  getNodeListFn()
})

const getNodeListFn = () => {
  getNodeListAll({}).then((res) => {
    if (res.code === 200) {
      nodeList.value = res.data || []
    }
  })
}

const getProjectListFn = (nodeId: string) => {
  getProjectListByNodeId({ nodeId }).then((res) => {
    if (res.code === 200) {
      projectList.value = res.data || []
    }
  })
}

const changeNode = () => {
  projectList.value = []
  delete useData.value.projectId
  getProjectListFn(useData.value.nodeId)
}
</script>
