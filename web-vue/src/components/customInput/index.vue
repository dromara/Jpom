<template>
  <div>
    <template v-if="inputData.indexOf(refTag) === -1 && type == 'password'">
      <a-input-password
        v-model:value="inputData"
        :placeholder="placeholder"
        :disabled="!!selectData"
        @change="inputChange"
      >
        <template #addonBefore>
          <a-tooltip>
            <template #title
              >{{ $t('i18n_e6551a2295') }}
              <ul v-if="!envList.length">
                {{
                  $t('i18n_7afb02ed93')
                }}
              </ul>
            </template>
            <a-select
              v-model:value="selectData"
              :placeholder="$t('i18n_3a1052ccfc')"
              style="width: 120px"
              @change="selectChange"
            >
              <a-select-option value="">{{ $t('i18n_e76e6a13dd') }}</a-select-option>
              <a-select-option v-for="item in envList" :key="item.id" :value="item.name"
                >{{ item.name }}
              </a-select-option>
            </a-select>
          </a-tooltip>
        </template>
      </a-input-password>
    </template>
    <template v-else>
      <a-input v-model:value="inputData" :placeholder="placeholder" :disabled="!!selectData" @change="inputChange">
        <template #addonBefore>
          <a-tooltip>
            <template #title
              >{{ $t('i18n_e6551a2295') }}
              <ul v-if="!envList.length">
                {{
                  $t('i18n_7afb02ed93')
                }}
              </ul>
            </template>
            <a-select
              v-model:value="selectData"
              :placeholder="$t('i18n_3a1052ccfc')"
              style="width: 120px"
              @change="selectChange"
            >
              <a-select-option value="">{{ $t('i18n_3a1052ccfc') }}</a-select-option>
              <a-select-option v-for="item in envList" :key="item.id" :value="item.name"
                >{{ item.name }}
              </a-select-option>
            </a-select>
          </a-tooltip>
        </template>
      </a-input>
    </template>
  </div>
</template>
<script>
import { t } from '@/i18n/index'
export default {
  components: {},
  props: {
    input: {
      type: String,
      default: ''
    },
    envList: {
      type: Array,
      default: () => []
    },
    placeholder: {
      type: String,
      default: function () {
        return t('i18n_101a86bc84')
      }
    },
    type: {
      type: String,
      default: 'password'
    }
  },
  emits: ['change'],
  data() {
    return {
      inputData: '',
      refTag: '$ref.wEnv.',
      selectData: null
    }
  },
  watch: {
    input: {
      deep: true,

      handler(v) {
        this.inputData = v
        if (v.indexOf(this.refTag) == -1) {
          this.selectData = null
        } else {
          // this.selectData = v.replace(this.refTag)
        }
      },

      immediate: true
    }
  },
  methods: {
    selectChange(v) {
      this.selectData = v
      const newV = v ? this.refTag + v : ''
      this.$emit('change', newV)
    },
    inputChange() {
      this.$emit('change', this.inputData)
    }
  }
}
</script>
