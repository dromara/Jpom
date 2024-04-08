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
          <a-row v-for="(item, index) in envList" :key="index">
            <a-col :span="22">
              <a-space direction="vertical" style="width: 100%">
                <a-input v-model:value="item.key" :addon-before="`变量${index + 1}key`" placeholder="变量key" />
                <a-input v-model:value="item.value" :addon-before="`变量${index + 1}值`" placeholder="变量值"
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
          <a-divider style="margin: 5px 0" />
        </a-space>

        <a-button type="primary" size="small" @click="() => envList.push({})">新增参数</a-button>
      </a-form-item>
      <a-form-item label="超时时间" name="timeout">
        <a-input-number v-model:value="useData.timeout" style="width: 100%" placeholder="执行脚本超时时间" />
      </a-form-item>
    </a-form>
  </div>
</template>
<script setup lang="ts">
const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  formLable: {
    type: Object,
    required: true
  }
})

const emit = defineEmits<{ (e: 'update:data', value: object): void }>()

const useData = ref<any>(props.data)

watch(
  () => useData.value,
  (val) => {
    emit('update:data', val)
  },
  {
    immediate: true
  }
)

const envList = ref<Array<{ key?: string; value?: string }>>([])

onMounted(() => {
  const list: Array<{ key: string; value: string }> = []
  for (let key in useData.env) {
    list.push({
      key: key,
      value: useData.env[key]
    })
  }
  envList.value = list
})
// 监听环境变量变化
watch(
  () => envList.value.map((item) => item.key + item.value),
  () => {
    const env: any = {}
    envList.value.forEach((element) => {
      if (element.key) {
        env[element.key] = element.value
      }
    })
    useData.value = { ...useData.value, env: env }
    // console.log(env)
    //emit('update:data', useData.value)
  },
  {
    immediate: true
  }
)

// const envList = computed(() => {
//   if (!useData || !useData.env) {
//     return []
//   }
// })
</script>
