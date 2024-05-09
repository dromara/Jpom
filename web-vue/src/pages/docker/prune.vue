<template>
  <div>
    <a-form ref="ruleForm" :model="pruneForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }" :rules="rules">
      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-alert :message="$tl('p.trimOperation')" banner />
      </a-form-item>
      <a-form-item
        :label="$tl('p.trimType')"
        :help="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].help"
      >
        <a-select v-model:value="pruneForm.pruneType" :placeholder="$tl('p.selectTrimType')">
          <a-select-option v-for="(item, key) in pruneTypes" :key="key">
            {{ item.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item
        v-if="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].filters.includes('dangling')"
        :label="$tl('p.floatingType')"
      >
        <a-switch
          v-model:checked="pruneForm.dangling"
          :checked-children="$tl('p.floating')"
          :un-checked-children="$tl('p.nonFloating')"
        />
      </a-form-item>
      <a-form-item
        v-if="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].filters.includes('until')"
        :label="$tl('p.limitedTime')"
      >
        <template #help
          ><a-tag color="#f50"> {{ $tl('p.suggestedTimeRange') }},{{ $tl('p.otherwiseDeleteAllData') }}</a-tag>
        </template>
        <a-tooltip :title="$tl('p.timeFormat')">
          <a-input v-model:value="pruneForm.until" :placeholder="`${$tl('p.trimBeforeTimestamp')}`" />
        </a-tooltip>
      </a-form-item>

      <a-form-item
        v-if="pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].filters.includes('labels')"
        :label="$tl('p.specifiedLabel')"
      >
        <a-tooltip :title="$tl('p.labelExample')">
          <a-input v-model:value="pruneForm.labels" :placeholder="$tl('p.trimObjectsWithLabel')" />
        </a-tooltip>
      </a-form-item>
      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        自动执行：docker
        {{ pruneTypes[pruneForm.pruneType] && pruneTypes[pruneForm.pruneType].command }}
        prune xxxxx
      </a-form-item>
      <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
        <a-button type="primary" :loading="loading" @click="onPruneSubmit"> {{ $tl('p.confirm') }} </a-button>
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
          name: this.$tl('p.image'),
          command: 'image',
          help: this.$tl('p.trimUnusedAndUnmarkedImages'),
          filters: ['until', 'dangling']
        },
        CONTAINERS: {
          value: 'CONTAINERS',
          name: this.$tl('p.container'),
          command: 'container',
          filters: ['until', 'labels']
        },
        NETWORKS: {
          value: 'NETWORKS',
          name: this.$tl('p.network'),
          command: 'network',
          filters: ['until']
        },
        VOLUMES: {
          value: 'VOLUMES',
          name: this.$tl('p.volume'),
          command: 'volume',
          filters: ['labels']
        },
        BUILD: {
          value: 'BUILD',
          name: this.$tl('p.build'),
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
          title: this.$tl('p.systemPrompt'),
          zIndex: 1009,
          content: this.$tl('p.confirmTrimInfo'),
          okText: this.$tl('p.confirmAction'),
          cancelText: this.$tl('p.cancel'),
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
