<template>
  <div>
    <a-form ref="ruleForm" :model="pruneForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }" :rules="rules">
      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-alert :message="$t('i18n_bc2c23b5d2')" banner />
      </a-form-item>
      <a-form-item
        :label="$t('i18n_629f3211ca')"
        :help="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].help"
      >
        <a-select v-model:value="pruneForm.pruneType" :placeholder="$t('i18n_ad209825b5')">
          <a-select-option v-for="(item, key) in pruneTypes" :key="key">
            {{ item.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        v-if="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].filters.includes('dangling')"
        :label="$t('i18n_81e4018e9d')"
      >
        <a-switch
          v-model:checked="pruneForm.dangling"
          :checked-children="$t('i18n_a09375d96c')"
          :un-checked-children="$t('i18n_643f39d45f')"
        />
      </a-form-item>
      <a-form-item
        v-if="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].filters.includes('until')"
        :label="$t('i18n_44d13f7017')"
      >
        <template #help
          ><a-tag color="#f50"> {{ $t('i18n_1775ff0f26') }},{{ $t('i18n_143d8d3de5') }}</a-tag>
        </template>
        <a-tooltip :title="$t('i18n_a37c573d7b')">
          <a-input v-model:value="pruneForm.until" :placeholder="`${$t('i18n_f8f20c1d1e')}`" />
        </a-tooltip>
      </a-form-item>

      <a-form-item
        v-if="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].filters.includes('labels')"
        :label="$t('i18n_3d61e4aaf1')"
      >
        <a-tooltip :title="$t('i18n_1c10461124')">
          <a-input v-model:value="pruneForm.labels" :placeholder="$t('i18n_9a436e2a53')" />
        </a-tooltip>
      </a-form-item>
      <a-form-item :wrapper-col="{ span: 14, offset: 4 }"
        >{{ $t('i18n_4d49b2a15f') }}{{ pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].command }}
        prune xxxxx
      </a-form-item>
      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" :loading="loading" @click="onPruneSubmit">
          {{ $t('i18n_38cf16f220') }}
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script>
import { dockerPrune } from '@/api/docker-api'
import { renderSize } from '@/utils/const'
export default {
  props: {
    id: {
      type: String,
      default: ''
    },
    urlPrefix: {
      type: String,
      default: ''
    },
    machineDockerId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      temp: {},
      pruneForm: {
        pruneType: 'IMAGES',
        dangling: true
      },
      pruneTypes: {
        IMAGES: {
          value: 'IMAGES',
          name: this.$t('i18n_3477228591'),
          command: 'image',
          help: this.$t('i18n_b96b07e2bb'),
          filters: ['until', 'dangling']
        },
        CONTAINERS: {
          value: 'CONTAINERS',
          name: this.$t('i18n_22c799040a'),
          command: 'container',
          filters: ['until', 'labels']
        },
        NETWORKS: {
          value: 'NETWORKS',
          name: this.$t('i18n_7ddbe15c84'),
          command: 'network',
          filters: ['until']
        },
        VOLUMES: {
          value: 'VOLUMES',
          name: this.$t('i18n_7088e18ac9'),
          command: 'volume',
          filters: ['labels']
        },
        BUILD: {
          value: 'BUILD',
          name: this.$t('i18n_fcba60e773'),
          command: 'builder',
          filters: ['until']
        }
      },
      rules: {},
      loading: false
    }
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId
    }
  },
  mounted() {
    // this.loadData();
    // console.log(Comparator);
  },
  methods: {
    renderSize,

    onPruneSubmit() {
      this.$refs['ruleForm'].validate().then(() => {
        //
        const that = this
        $confirm({
          title: this.$t('i18n_c4535759ee'),
          zIndex: 1009,
          content: this.$t('i18n_8e8bcfbb4f'),
          okText: this.$t('i18n_e83a256e4f'),
          cancelText: this.$t('i18n_625fb26b4b'),
          async onOk() {
            return await new Promise((resolve, reject) => {
              // 组装参数
              const params = {
                id: that.reqDataId,
                pruneType: that.pruneForm.pruneType
              }

              that.pruneTypes[params.pruneType] &&
                that.pruneTypes[params.pruneType].filters.forEach((element) => {
                  params[element] = that.pruneForm[element]
                })
              that.loading = true
              dockerPrune(that.urlPrefix, params)
                .then((res) => {
                  if (res.code === 200) {
                    $notification.success({
                      message: res.msg
                    })
                  }
                  resolve()
                })
                .catch(reject)
                .finally(() => {
                  that.loading = false
                })
            })
          }
        })
      })
    }
  }
}
</script>
