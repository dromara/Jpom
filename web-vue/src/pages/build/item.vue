<template>
  <div>
    <!-- 编辑区 -->
    <CustomDrawer
      destroy-on-close
      :open="true"
      :body-style="{
        padding: '0'
      }"
      :header-style="{
        padding: '0 10px'
      }"
      width="70vw"
      :mask-closable="false"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          $emit('close')
        }
      "
    >
      <template #title>
        <template v-if="id">
          <a-menu v-model:selectedKeys="menuKey" mode="horizontal" class="menu" @click="menuClick">
            <a-menu-item key="info"> <InfoOutlined /> {{ $t('i18n_224aef211c') }} </a-menu-item>
            <a-menu-item key="edit"> <EditOutlined /> {{ $t('i18n_e54c5ecb54') }} </a-menu-item>
            <a-menu-item key="trigger"> <ApiOutlined /> {{ $t('i18n_4696724ed3') }} </a-menu-item>
            <a-menu-item key="environment"><UnorderedListOutlined />{{ $t('i18n_3867e350eb') }}</a-menu-item>
          </a-menu>
        </template>
        <template v-else>
          <a-menu v-model:selectedKeys="menuKey" mode="horizontal" class="menu" @click="menuClick">
            <a-menu-item key="edit"> <EditOutlined /> {{ $t('i18n_44a6891817') }} </a-menu-item>
            <a-menu-item key="environment"><UnorderedListOutlined />{{ $t('i18n_3867e350eb') }}</a-menu-item>
          </a-menu>
        </template>
      </template>

      <div class="layout-content">
        <detailsPage v-if="id" v-show="menuKey.includes('info')" :id="id" />
        <editBuildPage
          v-show="menuKey.includes('edit')"
          :id="id"
          ref="editBuild"
          v-model:editSteps="stepsCurrent"
          :data="data"
          @confirm="
            (build, buildId, buildEnvParameter) => {
              $emit('build', build, buildId, buildEnvParameter)
            }
          "
          @change-edit-steps="
            (current) => {
              stepsCurrent = current
            }
          "
          @change-build-mode="
            (buildMode1) => {
              buildMode = buildMode1
              getEnvironmentList()
            }
          "
        ></editBuildPage>
        <triggerPage v-if="id" v-show="menuKey.includes('trigger')" :id="id" />

        <div v-show="menuKey.includes('environment')">
          <a-list size="small" bordered :data-source="Object.keys(environment)">
            <template #renderItem="{ item }">
              <a-list-item style="display: block">
                <a-row :wrap="true">
                  <a-col :span="12" :flex="12" class="text-overflow-hidden">
                    <a-tooltip placement="topLeft" :title="item">
                      {{ item }}
                    </a-tooltip>
                  </a-col>

                  <a-col :span="12" :flex="12" class="text-overflow-hidden">
                    <a-tooltip
                      placement="topLeft"
                      :title="environment[item].privacy ? $t('i18n_b12d003367') : environment[item].value"
                    >
                      <EyeInvisibleOutlined v-if="environment[item].privacy" />
                      <CodeOutlined v-if="environment[item].system" />
                      {{ environment[item].value }}
                    </a-tooltip>
                  </a-col>
                </a-row>
              </a-list-item>
            </template>
            <template #header>
              <b>{{ $t('i18n_c0ad27a701') }}</b>
              <a-alert :message="$t('i18n_f11569cfa9')" type="warning" />
            </template>
            <!-- <template #footer>
        <div>Footer</div>
      </template> -->
          </a-list>
        </div>
      </div>
      <!-- <template> </template> -->

      <template v-if="menuKey.includes('edit')" #footer>
        <a-space>
          <a-button
            @click="
              () => {
                $emit('close')
              }
            "
          >
            {{ $t('i18n_625fb26b4b') }}
          </a-button>
          <a-tooltip v-if="id" :title="$t('i18n_18c7e2556e')">
            <a-button @click="$refs.editBuild.refresh()"> {{ $t('i18n_694fc5efa9') }}</a-button>
          </a-tooltip>
          <a-divider type="vertical" />
          <a-button
            type="primary"
            :disabled="stepsCurrent === 0"
            @click="
              () => {
                stepsCurrent = stepsCurrent - 1
              }
            "
            >{{ $t('i18n_eeb6908870') }}</a-button
          >
          <a-button
            type="primary"
            :disabled="stepsCurrent === 4"
            @click="
              () => {
                stepsCurrent = stepsCurrent + 1
              }
            "
            >{{ $t('i18n_38ce27d846') }}</a-button
          >
          <a-divider type="vertical" />

          <a-button type="primary" @click="$refs.editBuild.handleEditBuildOk(false)">
            {{ $t('i18n_be5fbbe34c') }}
          </a-button>
          <a-button type="primary" @click="$refs.editBuild.handleEditBuildOk(true)">
            {{ $t('i18n_a577822cdd') }}
          </a-button>
        </a-space>
      </template>
    </CustomDrawer>
  </div>
</template>
<script>
import detailsPage from './details.vue'
import editBuildPage from './edit.vue'
import triggerPage from './trigger.vue'
import { getBuildEnvironment } from '@/api/build-info'
export default {
  components: {
    detailsPage,
    triggerPage,
    editBuildPage
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    visibleType: {
      type: Number,
      default: 0
    },
    data: {
      type: Object,
      default: null
    },
    editSteps: {
      type: Number,
      default: 0
    }
  },
  emits: ['close', 'build'],
  data() {
    return {
      menuKey: ['info'],
      stepsCurrent: this.editSteps,
      environment: {},
      buildMode: null
    }
  },
  created() {
    const array = ['info', 'edit', 'trigger']
    if (this.id) {
      this.menuKey = [array[this.visibleType - 1]]
    } else {
      this.menuKey = [array[1]]
    }
    this.getEnvironmentList()
  },
  methods: {
    menuClick(item) {
      this.menuKey = item.key
    },
    onClose() {
      this.$emit('close')
    },
    // 获取可用环境变量
    getEnvironmentList() {
      // 构建基础信息
      getBuildEnvironment({
        id: this.id,
        buildMode: this.buildMode
      }).then((res) => {
        if (res.data) {
          this.environment = res.data || {}
        }
      })
    }
  }
}
</script>
<style scoped>
.menu {
  border-bottom: 0;
}

.layout-content {
  padding: 0;
  margin: 15px;
}
</style>
