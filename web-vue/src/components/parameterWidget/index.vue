<template>
  <div>
    <a-tag
      v-for="(data, key) in parameterMap"
      :key="key"
      closable
      @close="delteParameters(key)"
      @click="handleEdit(key)"
    >
      <a-tooltip :title="parameterMap[key]">{{ key }}</a-tooltip>
    </a-tag>
    <a-tag
      :style="{
        borderStyle: 'dashed'
      }"
      @click="handleAdd"
    >
      <PlusOutlined />{{ $t('i18n_7e1b283c57') }}</a-tag
    >

    <!-- 编辑区 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :title="$t('i18n_71a2c432b0')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :model="temp" :rules="rules" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="key" name="key">
          <a-input v-model:value="temp.key" :disabled="!!temp.oldKey" :placeholder="$t('i18n_c0d19bbfb3')" />
        </a-form-item>
        <a-form-item label="value" name="value">
          <a-input v-model:value="temp.value" :placeholder="$t('i18n_24384dab27')" />
        </a-form-item>
      </a-form>
    </CustomModal>
  </div>
</template>
<script lang="ts" setup>
import Qs from 'qs'
import { useI18n } from 'vue-i18n'
import type { Rule } from 'ant-design-vue/es/form'
const props = withDefaults(
  defineProps<{
    value?: string
  }>(),
  {
    value: ''
  }
)
const emit = defineEmits<{ (e: 'update:value', value: object): void }>()
const useData = ref(props.value)
const { t: $t } = useI18n()
const parameterMap = ref<Record<string, any>>({})

// 删除
const delteParameters = (key: string) => {
  delete parameterMap.value[key]
  useData.value = Qs.stringify(parameterMap.value)
}

watch(
  () => useData.value,
  (val) => {
    emit('update:value', val as any)
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

const rules = ref<Record<string, Rule[]>>({
  key: [{ required: true, message: $t('i18n_c0d19bbfb3') as string, trigger: 'blur' }]
})

const editVisible = ref(false)
const editForm = ref()
const temp = ref<{
  key: string
  value: string
  oldKey?: string
}>({
  key: '',
  value: ''
})

const handleAdd = () => {
  editVisible.value = true
  temp.value = { key: '', value: '' }
}

const handleEdit = (key: string) => {
  editVisible.value = true
  temp.value = { key: key, value: parameterMap.value[key], oldKey: key }
}

const handleEditOk = () => {
  editForm.value.validate().then(() => {
    editVisible.value = false
    parameterMap.value[temp.value.key] = temp.value.value
    if (temp.value.key !== temp.value.oldKey && temp.value.oldKey) {
      delete parameterMap.value[temp.value.oldKey]
    }
  })
}
</script>
