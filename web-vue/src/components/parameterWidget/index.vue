<template>
  <div>
    <a-tag v-for="(data, key) in parameterMap" closable @close="delteParameters(key)" @click="handleEdit(key)">
      <a-tooltip :title="parameterMap[key]">{{ key }}</a-tooltip>
    </a-tag>
    <a-tag
      :style="{
        borderStyle: 'dashed'
      }"
      @click="handleAdd"
    >
      <PlusOutlined />{{ $t('components.parameterWidget.index.42cbc280') }}</a-tag
    >

    <!-- 编辑区 -->
    <a-modal
      v-model:open="editVisible"
      :z-index="1009"
      destroy-on-close
      :title="$t('components.parameterWidget.index.77ecbd27')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :model="temp" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="key" name="key">
          <a-input
            v-model:value="temp.key"
            :disabled="!!temp.oldKey"
            :placeholder="$t('components.parameterWidget.index.1bb65976')"
          />
        </a-form-item>
        <a-form-item label="value" name="value">
          <a-input v-model:value="temp.value" :placeholder="$t('components.parameterWidget.index.8485924b')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import Qs from 'qs'
const props = withDefaults(
  defineProps<{
    value?: String
  }>(),
  {
    value: ''
  }
)
const emit = defineEmits<{ (e: 'update:value', value: object): void }>()
const useData = ref(props.value)

const parameterMap = ref<Record<String, any>>({})

// 删除
const delteParameters = (key: number) => {
  delete parameterMap.value[key]
  useData.value = Qs.stringify(parameterMap.value)
}

watch(
  () => useData.value,
  (val) => {
    emit('update:value', val)
  },
  {
    immediate: false
  }
)
watch(
  () => props.value,
  (val) => {
    useData.value = val
    parameterMap.value = Qs.parse(props.value)
  },
  {
    immediate: true
  }
)

// 监听变量变化
watch(
  () => parameterMap.value,
  () => {
    useData.value = Qs.stringify(parameterMap.value)
  },
  {
    deep: true,
    immediate: false
  }
)

const rules = ref({
  key: [{ required: true, message: this.$t('components.parameterWidget.index.1bb65976'), trigger: 'blur' }]
})

const editVisible = ref(false)
const editForm = ref()
const temp = ref({
  key: '',
  value: ''
})

const handleAdd = () => {
  editVisible.value = true
  temp.value = { key: '', value: '' }
}

const handleEdit = (key) => {
  editVisible.value = true
  temp.value = { key: key, value: parameterMap.value[key], oldKey: key }
}

const handleEditOk = () => {
  editForm.value.validate().then(() => {
    editVisible.value = false
    parameterMap.value[temp.value.key] = temp.value.value
    if (temp.value.key !== temp.value.oldKey) {
      delete parameterMap.value[temp.value.oldKey]
    }
  })
}
</script>
