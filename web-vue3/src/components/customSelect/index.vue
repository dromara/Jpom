<template>
  <div>
    <Select
      v-model:value="selected"
      :style="selStyle"
      :disabled="disabled"
      showSearch
      @change="selectChange"
      :placeholder="selectPlaceholder"
    >
      <template #dropdownRender="{ menuNode: menu }">
        <v-nodes :vnodes="menu" />
        <a-divider />
        <a-space>
          <a-input ref="inputRef" :maxLength="maxLength" v-model:value="selectInput" :placeholder="inputPlaceholder" />
          <a-button type="text" @click="addInput(selectInput)">
            <template #icon>
              <plus-outlined />
            </template>
            添加
          </a-button>
        </a-space>
      </template>
      <a-select-option v-if="selectPlaceholder" value="">{{ selectPlaceholder }}</a-select-option>
      <a-select-option v-for="item in optionList" :key="item">{{ item }} </a-select-option>
    </Select>
  </div>
</template>

<script>
import { Select } from 'ant-design-vue'

export default {
  components: {
    Select,
    VNodes: {
      props: {
        vnodes: {
          type: Object,
          required: true
        }
      },
      render() {
        return this.vnodes
      }
    }
  },

  data() {
    return {
      selectInput: '',

      optionList: [],
      selected: ''
    }
  },
  props: {
    // 继承原组件所有props
    ...Select.props,
    data: {
      type: Array,
      default: () => []
    },
    inputPlaceholder: {
      type: String,
      default: '请输入...'
    },
    selectPlaceholder: {
      type: String,
      default: '请选择'
    },
    selStyle: { type: String, default: '' },

    maxLength: {
      type: Number,
      default: 200
    }
  },
  watch: {
    value: {
      handler(v) {
        this.selected = v
      },
      immediate: true
    },
    data: {
      handler(v) {
        this.optionList = v
      },
      deep: true,
      immediate: true
    }
  },

  methods: {
    selectChange(v) {
      this.$emit('update:value', v)
    },
    addInput(v) {
      if (!v) {
        return
      }
      let index = this.optionList.indexOf(v)
      if (index === -1) {
        this.optionList = [...this.optionList, v]
      }
      this.selectInput = ''
      this.selected = v
      //
      this.selectChange(v)
    }
  }
}
</script>
