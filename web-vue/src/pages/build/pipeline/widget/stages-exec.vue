<template>
  <div>
    <a-form :label-col="formLable.labelCol" :wrapper-col="formLable.wrapperCol">
      <a-form-item name="commands">
        <template #label>
          <a-tooltip> 执行命令 </a-tooltip>
        </template>
        <template #help> </template>

        <a-form-item-rest>
          <code-editor v-model:content="useData.commands" height="40vh" :show-tool="true" :options="{ mode: 'shell' }">
            <template #tool_before> <a-tag>必填</a-tag></template>
          </code-editor>
        </a-form-item-rest>
      </a-form-item>
      <a-form-item name="env">
        <template #label>
          <a-tooltip> 环境变量 </a-tooltip>
        </template>
        <template #help> </template>

        <a-space direction="vertical" style="width: 100%">
          <a-space direction="vertical" style="width: 100%">
            <template #split>
              <a-divider type="horizontal" style="margin: 0" />
            </template>
            <a-row v-for="(item, index) in envList" :key="index">
              <a-col :span="22">
                <a-space direction="vertical" style="width: 100%">
                  <a-input
                    :value="item.key"
                    :addon-before="`变量${index + 1}key`"
                    placeholder="变量key"
                    @change="(e) => envInputChange(index, 'key', e.target.value as string)" />
                  <a-input
                    :value="item.value"
                    :addon-before="`变量${index + 1}值`"
                    placeholder="变量值"
                    @change="(e) => envInputChange(index, 'value', e.target.value as string)"
                /></a-space>
              </a-col>
              <a-col :span="2">
                <a-row type="flex" justify="center" align="middle">
                  <a-col>
                    <MinusCircleOutlined style="color: #ff0000" @click="() => envList.splice(index, 1)" />
                  </a-col>
                </a-row>
              </a-col>
            </a-row>

            <a-button type="primary" size="small" @click="() => envList.push({})">新增参数</a-button>
          </a-space>
        </a-space>
      </a-form-item>
      <a-form-item label="超时时间" name="timeout">
        <a-input-number v-model:value="useData.timeout" style="width: 100%" placeholder="执行脚本超时时间" />
      </a-form-item>
    </a-form>
  </div>
</template>
<script setup lang="ts">
import { StagesExec } from './types'
const props = defineProps({
  data: {
    type: Object as PropType<StagesExec>,
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
    // console.log('修改数据', val)
    emit('update:data', val)
  },
  {
    deep: true,
    immediate: false
  }
)
type EnvType = { key?: string; value?: string }
// 暂存变量列表
const envList = ref<Array<EnvType>>([])

watch(
  () => props.data,
  (val) => {
    useData.value = val
    const propEnv = props.data.env
    const putArray = []
    for (let key in propEnv) {
      const find = envList.value
        .map((item) => {
          if (item.key === key) {
            item.value = propEnv[key]
          }
          return item
        })
        .filter((item) => item.key === key).length
      if (!find) {
        putArray.push({
          key: key,
          value: propEnv[key]
        })
      }
    }
    // console.log(putArray)
    envList.value.push(...putArray)

    // putArray.forEach((item) => envList.value.push(item))
  },
  {
    deep: true,
    immediate: true
  }
)

const envInputChange = (index: number, key: keyof EnvType, value: string) => {
  // if (isKey) {
  envList.value[index][key] = value
  // } else {
  // envList.value[index].value = value
  // }
}

// 监听环境变量变化
watch(
  () => envList.value.map((item: any, index) => index + '=' + item.key + ':' + item.value),
  () => {
    const env: any = {}
    envList.value.forEach((element) => {
      if (element.key) {
        env[element.key] = element.value
      }
    })
    useData.value = { ...useData.value, env: env }
    //console.log(env, val)
    //emit('update:data', useData.value)
  },
  {
    immediate: false
  }
)
</script>
