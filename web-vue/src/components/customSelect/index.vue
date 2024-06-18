<template>
  <div>
    <a-select
      v-model:value="selected"
      :style="selStyle"
      :disabled="disabled"
      show-search
      :placeholder="selectPlaceholder"
      @change="selectChange"
    >
      <template v-if="canReload" #suffixIcon> <ReloadOutlined @click="refreshSelect" /></template>
      <template #dropdownRender="{ menuNode: menu }">
        <v-nodes :vnodes="menu" />
        <a-divider />
        <a-space>
          <a-input ref="inputRef" v-model:value="selectInput" :max-length="maxLength" :placeholder="inputPlaceholder" />
          <a-button type="text" @click="addInput(selectInput)">
            <template #icon> <plus-outlined /> </template>{{ $t('i18n_66ab5e9f24') }}</a-button
          >
        </a-space>
      </template>
      <a-select-option v-if="selectPlaceholder" value="">{{ selectPlaceholder }}</a-select-option>
      <a-select-option v-for="item in optionList" :key="item">{{ item }} </a-select-option>
    </a-select>
  </div>
</template>
<script>
import { Select } from 'ant-design-vue'
import { t } from '@/i18n/index'
export default {
  components: {
    ASelect: Select,
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
  props: {
    // 继承原组件所有props
    ...Select.props,
    data: {
      type: Array,
      default: () => []
    },
    inputPlaceholder: {
      type: String,
      default: function () {
        return t('i18n_101a86bc84')
      }
    },
    selectPlaceholder: {
      type: String,
      default: function () {
        return t('i18n_708c9d6d2a')
      }
    },
    selStyle: { type: String, default: '' },

    maxLength: {
      type: Number,
      default: 200
    },
    canReload: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:value', 'onRefreshSelect', 'change', 'addOption'],

  data() {
    return {
      selectInput: '',

      optionList: [],
      selected: ''
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
      this.$emit('change', v)
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
      this.$emit('addOption', this.optionList)
    },
    refreshSelect() {
      this.$emit('onRefreshSelect')
    }
  }
}
</script>
