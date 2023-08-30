<template>
  <codemirror v-model="value" placeholder="Code goes here..." :style="{ height: '400px' }" :autofocus="true"
    :indent-with-tab="true" :tab-size="2" :extensions="extensions" @ready="handleReady" @change="onChange" />
</template>


<script lang="ts" setup>
import { Codemirror } from 'vue-codemirror'
import { javascript } from '@codemirror/lang-javascript'

const props = defineProps({
  code: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:code',])

const value = ref('')

const extensions = [javascript()]


watch(() =>
  props.code,
  () => {
    value.value = props.code
  })

// Codemirror EditorView instance ref
const view = shallowRef()
const handleReady = (payload) => {
  view.value = payload.view
}

const onChange = () => {
  emit('update:code', value.value)
}

// // Status is available at all times via Codemirror EditorView
// const getCodemirrorStates = () => {
//   const state = view.value.state
//   const ranges = state.selection.ranges
//   const selected = ranges.reduce((r, range) => r + range.to - range.from, 0)
//   const cursor = ranges[0].anchor
//   const length = state.doc.length
//   const lines = state.doc.lines
//   // more state info ...
//   // return ...
// }
</script>
