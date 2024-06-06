<template>
  <div>
    <a-form ref="ruleForm" :model="pruneForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }" :rules="rules">
      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-alert :message="$t('pages.docker.prune.17b5ea77')" banner />
      </a-form-item>
      <a-form-item
        :label="$t('pages.docker.prune.4fec20d2')"
        :help="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].help"
      >
        <a-select v-model:value="pruneForm.pruneType" :placeholder="$t('pages.docker.prune.a197e176')">
          <a-select-option v-for="(item, key) in pruneTypes" :key="key">
            {{ item.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        v-if="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].filters.includes('dangling')"
        :label="$t('pages.docker.prune.a669feea')"
      >
        <a-switch
          v-model:checked="pruneForm.dangling"
          :checked-children="$t('pages.docker.prune.d361adb6')"
          :un-checked-children="$t('pages.docker.prune.8788f12a')"
        />
      </a-form-item>
      <a-form-item
        v-if="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].filters.includes('until')"
        :label="$t('pages.docker.prune.937a97fd')"
      >
        <template #help
          ><a-tag color="#f50"> {{ $t('pages.docker.prune.cb38bb6a') }},{{ $t('pages.docker.prune.a3e34a37') }}</a-tag>
        </template>
        <a-tooltip :title="$t('pages.docker.prune.7ea3f135')">
          <a-input v-model:value="pruneForm.until" :placeholder="`${$t('pages.docker.prune.dbf26257')}`" />
        </a-tooltip>
      </a-form-item>

      <a-form-item
        v-if="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].filters.includes('labels')"
        :label="$t('pages.docker.prune.8dd61567')"
      >
        <a-tooltip :title="$t('pages.docker.prune.b6d13d99')">
          <a-input v-model:value="pruneForm.labels" :placeholder="$t('pages.docker.prune.d3cece44')" />
        </a-tooltip>
      </a-form-item>
      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        自动执行：docker
        {{ pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].command }}
        prune xxxxx
      </a-form-item>
      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" :loading="loading" @click="onPruneSubmit">
          {{ $t('pages.docker.prune.e8e9db25') }}
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
          name: this.$t('pages.docker.prune.4fbd9851'),
          command: 'image',
          help: this.$t('pages.docker.prune.2ac80e6a'),
          filters: ['until', 'dangling']
        },
        CONTAINERS: {
          value: 'CONTAINERS',
          name: this.$t('pages.docker.prune.e59a28c9'),
          command: 'container',
          filters: ['until', 'labels']
        },
        NETWORKS: {
          value: 'NETWORKS',
          name: this.$t('pages.docker.prune.7beffdd'),
          command: 'network',
          filters: ['until']
        },
        VOLUMES: {
          value: 'VOLUMES',
          name: this.$t('pages.docker.prune.5ea86045'),
          command: 'volume',
          filters: ['labels']
        },
        BUILD: {
          value: 'BUILD',
          name: this.$t('pages.docker.prune.37206ed5'),
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
    $tl(key, ...args) {
      return this.$t(`pages.docker.prune.${key}`, ...args)
    },
    renderSize,

    onPruneSubmit() {
      this.$refs['ruleForm'].validate().then(() => {
        //
        const that = this
        $confirm({
          title: this.$t('pages.docker.prune.b22d55a0'),
          zIndex: 1009,
          content: this.$t('pages.docker.prune.faa2b295'),
          okText: this.$t('pages.docker.prune.e5a2dcc3'),
          cancelText: this.$t('pages.docker.prune.b12468e9'),
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
