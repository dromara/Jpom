<!--

    Copyright (c) 2019 Of Him Code Technology Studio
    Jpom is licensed under Mulan PSL v2.
    You can use this software according to the terms and conditions of the Mulan PSL v2.
    You may obtain a copy of Mulan PSL v2 at:
    			http://license.coscl.org.cn/MulanPSL2
    THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
    See the Mulan PSL v2 for more details.

-->
<template>
  <div class="pipeline-editor">
    <a-spin :spinning="loading">
      <adaptive-form :label-width="112" :label-width-tight="96" :tight-width="560" :inline-min-width="360">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('i18n_pipeline_name')">
              <a-input v-model:value="form.name" data-testid="pipeline-name" :max-length="80" :placeholder="$t('i18n_pipeline_name_placeholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('i18n_829abe5a8d')">
              <!-- 分组与在线构建 / 项目 / 仓库 / SSH 同一套交互：虚拟逻辑分组 + 定制下拉框（可选已有、可直接新增） -->
              <custom-select
                v-model:value="form.groupName"
                data-testid="pipeline-group"
                :max-length="50"
                :data="groupList"
                :input-placeholder="$t('i18n_bd0362bed3')"
                :select-placeholder="$t('i18n_3e8c9c54ee')"
              >
                <template #suffix>
                  <a-tooltip>
                    <template #title>
                      <div>{{ $t('i18n_bd7c7abc8c') }}</div>
                    </template>
                    <QuestionCircleOutlined />
                  </a-tooltip>
                </template>
              </custom-select>
            </a-form-item>
          </a-col>
        </a-row>
      </adaptive-form>

      <!-- 步骤指示器：把「仓库源 / 事件通知 / 阶段编排」分开，避免一屏堆到底（参照在线构建编辑页） -->
      <a-steps
        :current="stepsCurrent"
        size="small"
        class="editor-steps"
        :items="stepsItems"
        data-testid="pipeline-steps"
        @change="handleStepChange"
      />

      <!-- 步骤 1：仓库源 -->
      <div v-show="stepsCurrent === 0" data-testid="pipeline-step-source">
      <a-card :title="$t('i18n_pipeline_source_title')" size="small" class="block">
        <template #extra>
          <a-button type="primary" data-testid="pipeline-add-source" @click="addSource">
            <PlusOutlined />
            {{ $t('i18n_pipeline_source_add') }}
          </a-button>
        </template>
        <div class="card-hint">{{ $t('i18n_pipeline_source_tip') }}</div>
        <div v-if="!sources.length" class="empty-hint">{{ $t('i18n_pipeline_source_empty') }}</div>
        <div v-for="(source, index) in sources" :key="source.key" class="source-card">
          <div class="source-head">
            <b>{{ $t('i18n_pipeline_source_index', { index: index + 1 }) }}</b>
            <a-button size="small" type="primary" danger @click="removeSource(index)">
              <DeleteOutlined />
              {{ $t('i18n_2f4aaddde3') }}
            </a-button>
          </div>
          <a-form layout="vertical" class="source-form">
            <a-row :gutter="12">
              <a-col :xs="24" :sm="24" :md="6">
                <a-form-item :label="$t('i18n_pipeline_source_id')">
                  <a-input v-model:value="source.id" :placeholder="$t('i18n_pipeline_source_id_placeholder')" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="24" :md="12">
                <a-form-item :label="$t('i18n_pipeline_build_repo')">
                  <a-select
                    v-model:value="source.repoId"
                    style="width: 100%"
                    show-search
                    option-filter-prop="label"
                    :options="repoOptions"
                    :loading="repoLoading"
                    :placeholder="$t('i18n_pipeline_repo_placeholder')"
                    :data-testid="'pipeline-source-repo-' + index"
                    @change="() => onRepoChange(source)"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="24" :md="6">
                <a-form-item :label="$t('i18n_pipeline_source_mount')">
                  <a-input :value="sourceMount(source)" readonly />
                  <div class="field-tip">{{ $t('i18n_pipeline_source_mount_locked') }}</div>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="12">
              <a-col :xs="24" :sm="24" :md="4">
                <a-form-item :label="$t('i18n_pipeline_ref_type')">
                  <a-select v-model:value="source.refType" :options="refTypeOptions" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="24" :md="14">
                <a-form-item :label="source.refType === 'tag' ? $t('i18n_pipeline_ref_tag') : $t('i18n_pipeline_ref_branch')">
                  <a-select
                    v-model:value="source.refName"
                    style="width: 100%"
                    show-search
                    allow-clear
                    :disabled="!source.repoId"
                    :options="refOptions(source)"
                    :loading="!!branchLoading[source.repoId]"
                    :placeholder="source.repoId ? $t('i18n_pipeline_ref_placeholder') : $t('i18n_pipeline_ref_need_repo')"
                    :not-found-content="branchLoading[source.repoId] ? $t('i18n_pipeline_branch_loading') : undefined"
                  >
                    <template #suffixIcon>
                      <ReloadOutlined @click="source.repoId && loadBranches(source.repoId, true)" />
                    </template>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="24" :md="6">
                <a-form-item :label="$t('i18n_0253279fb8')">
                  <a-input-number
                    v-model:value="source.cloneDepth"
                    style="width: 100%"
                    :min="0"
                    :placeholder="$t('i18n_pipeline_clone_depth_placeholder')"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </div>
      </a-card>

      </div>

      <!-- 步骤 2：阶段编排 + 配置 -->
      <div v-show="stepsCurrent === 1" class="step-pane step-pane-stages" data-testid="pipeline-step-stages">
      <div ref="stageLayout" class="stage-layout block">
        <a-card
          v-show="!canvasCollapsed"
          :title="$t('i18n_pipeline_stage_title')"
          size="small"
          class="stage-canvas-card"
        >
            <template #extra>
              <a-space>
                <a-dropdown trigger="click">
                  <a-button type="primary" data-testid="pipeline-add-stage">
                    <PlusOutlined />
                    {{ $t('i18n_pipeline_stage_add') }}
                  </a-button>
                  <template #overlay>
                    <a-menu @click="({ key }) => addStage(key)">
                      <a-menu-item key="BUILD">{{ $t('i18n_pipeline_stage_type_build') }}</a-menu-item>
                      <a-menu-item key="COMMAND">{{ $t('i18n_pipeline_stage_type_command') }}</a-menu-item>
                      <a-menu-item key="RELEASE">{{ $t('i18n_pipeline_stage_type_release') }}</a-menu-item>
                      <a-menu-item key="NOTIFY">{{ $t('i18n_pipeline_stage_type_notify') }}</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
                <a-tooltip :title="configCollapsed ? $t('i18n_pipeline_stage_expand') : $t('i18n_pipeline_stage_collapse')">
                  <a-button data-testid="pipeline-config-toggle" @click="configCollapsed = !configCollapsed">
                    <MenuUnfoldOutlined v-if="configCollapsed" />
                    <MenuFoldOutlined v-else />
                  </a-button>
                </a-tooltip>
              </a-space>
            </template>
            <div class="canvas-wrap">
              <PipelineCanvas
                v-if="stages.length"
                :stages="stages"
                :selected="selectedIndex"
                @select="(index) => (selectedIndex = index)"
                @reorder="onReorder"
              />
              <a-empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" :description="$t('i18n_pipeline_stage_empty')">
                <a-button type="primary" @click="addStage('BUILD')">{{ $t('i18n_pipeline_stage_add') }}</a-button>
              </a-empty>
            </div>
            <div class="canvas-tip">{{ $t('i18n_pipeline_canvas_tip') }}</div>
          </a-card>
        <!-- 可拖拽分割线：拖拽改宽、双击复位；两侧按钮分别折叠/展开「阶段编排」「阶段配置」 -->
        <div
          class="stage-splitter"
          :title="$t('i18n_pipeline_config_splitter_tip')"
          data-testid="pipeline-config-splitter"
          @mousedown.prevent="startResizeConfig"
          @dblclick="resetConfigWidth"
        >
          <button
            type="button"
            class="stage-splitter-btn"
            :title="canvasCollapsed ? $t('i18n_pipeline_canvas_expand') : $t('i18n_pipeline_canvas_collapse')"
            data-testid="pipeline-toggle-canvas"
            @mousedown.stop.prevent
            @click.stop.prevent="toggleCanvas"
          >
            <MenuUnfoldOutlined v-if="canvasCollapsed" />
            <MenuFoldOutlined v-else />
          </button>
          <span class="stage-splitter-bar"></span>
          <button
            type="button"
            class="stage-splitter-btn"
            :title="configCollapsed ? $t('i18n_pipeline_stage_expand') : $t('i18n_pipeline_stage_collapse')"
            data-testid="pipeline-toggle-config"
            @mousedown.stop.prevent
            @click.stop.prevent="toggleConfig"
          >
            <MenuUnfoldOutlined v-if="configCollapsed" />
            <MenuFoldOutlined v-else />
          </button>
        </div>
        <a-card
          v-show="!configCollapsed"
          :title="$t('i18n_pipeline_stage_config')"
          size="small"
          class="stage-config-card"
          :style="configCardStyle"
        >
            <template #extra>
              <a-space v-if="current && current.id">
                <a-button :disabled="selectedIndex <= 0" @click="moveStage(-1)">{{ $t('i18n_315eacd193') }}</a-button>
                <a-button :disabled="selectedIndex >= stages.length - 1" @click="moveStage(1)">{{ $t('i18n_17acd250da') }}</a-button>
                <a-popconfirm :title="$t('i18n_pipeline_stage_remove_confirm')" @confirm="removeStage(selectedIndex)">
                  <a-button danger>{{ $t('i18n_2f4aaddde3') }}</a-button>
                </a-popconfirm>
              </a-space>
            </template>
            <a-empty v-if="!current || !current.id" :image="Empty.PRESENTED_IMAGE_SIMPLE" :description="$t('i18n_pipeline_stage_select_tip')" />
            <adaptive-form
              v-else
              class="stage-form"
              :inline-min-width="420"
              :tight-width="620"
              :label-width="112"
              :label-width-tight="88"
            >
              <a-row :gutter="8">
                <a-col :span="24">
                  <a-form-item :label="$t('i18n_pipeline_stage_name')">
                    <a-input
                      v-model:value="current.name"
                      data-testid="pipeline-stage-name"
                      :max-length="50"
                      :placeholder="$t('i18n_pipeline_stage_name_placeholder')"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="24">
                  <a-form-item :label="$t('i18n_pipeline_stage_key')">
                    <a-input v-model:value="current.id" :max-length="40" :placeholder="$t('i18n_pipeline_stage_key_placeholder')" />
                  </a-form-item>
                </a-col>
                <a-col :span="24">
                  <a-form-item :label="$t('i18n_pipeline_stage_enabled')">
                    <a-switch v-model:checked="current.enabled" />
                  </a-form-item>
                </a-col>
                <a-col :span="24">
                  <a-form-item :label="$t('i18n_pipeline_stage_fail_fast')">
                    <a-radio-group v-model:value="current.failFast">
                      <a-radio :value="true">{{ $t('i18n_pipeline_stage_fail_fast_on') }}</a-radio>
                      <a-radio :value="false">{{ $t('i18n_pipeline_stage_fail_fast_off') }}</a-radio>
                    </a-radio-group>
                  </a-form-item>
                </a-col>
              </a-row>

              <a-form-item :label="$t('i18n_pipeline_env_vars')">
                <a-space wrap>
                  <a-popover trigger="click" placement="rightTop" :title="$t('i18n_pipeline_env_vars')">
                    <template #content>
                      <a-table
                        size="small"
                        bordered
                        :pagination="false"
                        row-key="name"
                        :columns="envVarColumns"
                        :data-source="envVariableList"
                        :scroll="{ x: 520 }"
                        class="env-var-table"
                      />
                    </template>
                    <a-button type="link" size="small">
                      <InfoCircleOutlined />
                      {{ $t('i18n_pipeline_env_vars_view') }}
                    </a-button>
                  </a-popover>
                  <span class="field-tip">{{ $t('i18n_pipeline_env_vars_tip') }}</span>
                </a-space>
              </a-form-item>

              <!-- 构建 / 命令 -->
              <template v-if="current.type === 'BUILD' || current.type === 'COMMAND'">
                <a-divider orientation="left">{{ $t('i18n_17a74824de') }}</a-divider>
                <a-form-item :label="$t('i18n_pipeline_executor')">
                  <a-radio-group v-model:value="current.executor">
                    <a-radio value="local">{{ $t('i18n_pipeline_executor_local') }}</a-radio>
                    <a-radio value="container">{{ $t('i18n_pipeline_executor_container') }}</a-radio>
                    <a-radio value="node">{{ $t('i18n_pipeline_executor_node') }}</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item v-if="current.executor === 'container'" :label="$t('i18n_3477228591')">
                  <a-auto-complete
                    v-model:value="current.image"
                    :options="dockerTagOptions"
                    :placeholder="$t('i18n_pipeline_image_placeholder')"
                    style="width: 100%"
                  />
                </a-form-item>
                <template v-if="current.executor === 'node'">
                  <a-form-item :label="$t('i18n_pipeline_build_node')">
                    <a-select
                      v-model:value="current.nodeId"
                      show-search
                      option-filter-prop="label"
                      :options="nodeOptions"
                      :placeholder="$t('i18n_pipeline_build_node')"
                    />
                  </a-form-item>
                  <a-form-item :label="$t('i18n_pipeline_build_repo')">
                    <a-select
                      v-model:value="current.repoId"
                      style="width: 100%"
                      show-search
                      option-filter-prop="label"
                      :options="repoOptions"
                      :placeholder="$t('i18n_pipeline_repo_placeholder')"
                      @change="() => onRepoChange(current)"
                    />
                  </a-form-item>
                  <a-form-item :label="$t('i18n_pipeline_ref')">
                    <a-row :gutter="8">
                      <a-col :span="9">
                        <a-select v-model:value="current.refType" style="width: 100%" :options="refTypeOptions" />
                      </a-col>
                      <a-col :span="15">
                        <a-select
                          v-model:value="current.refName"
                          style="width: 100%"
                          show-search
                          allow-clear
                          :disabled="!current.repoId"
                          :options="refOptions(current)"
                          :loading="!!branchLoading[current.repoId]"
                          :placeholder="current.repoId ? $t('i18n_pipeline_ref_placeholder') : $t('i18n_pipeline_ref_need_repo')"
                        />
                      </a-col>
                    </a-row>
                  </a-form-item>
                  <a-form-item :label="$t('i18n_c972010694')">
                    <a-input v-model:value="current.resultDirFile" :placeholder="$t('i18n_pipeline_result_dir_placeholder')" />
                  </a-form-item>
                </template>
                <a-form-item v-else :label="$t('i18n_pipeline_work_dir')">
                  <a-select
                    v-model:value="current.cwd"
                    style="width: 100%"
                    allow-clear
                    show-search
                    :options="workDirOptions"
                    :placeholder="$t('i18n_pipeline_work_dir_placeholder')"
                  />
                  <div class="field-tip">{{ $t('i18n_pipeline_work_dir_tip') }}</div>
                </a-form-item>
                <a-form-item :label="$t('i18n_c2ee58c247')">
                  <code-editor
                    v-model:content="current.run"
                    :show-tool="false"
                    :options="{ mode: 'shell' }"
                    height="clamp(180px, 34vh, 420px)"
                    :placeholder="$t('i18n_pipeline_command_placeholder')"
                  />
                </a-form-item>
                <a-form-item v-if="current.type === 'BUILD'" :label="$t('i18n_c972010694')">
                  <a-select
                    v-model:value="current.outputs"
                    mode="tags"
                    :open="false"
                    :placeholder="$t('i18n_pipeline_outputs_placeholder')"
                    style="width: 100%"
                  />
                </a-form-item>
                <a-form-item :label="$t('i18n_3867e350eb')">
                  <a-textarea
                    v-model:value="current.env"
                    :auto-size="{ minRows: 3, maxRows: 8 }"
                    :placeholder="$t('i18n_pipeline_env_placeholder')"
                  />
                </a-form-item>
              </template>

              <!-- 发布 -->
              <template v-if="current.type === 'RELEASE'">
                <a-divider orientation="left">{{ $t('i18n_f98994f7ec') }}</a-divider>
                <a-form-item :label="$t('i18n_pipeline_release_from')">
                  <a-select
                    v-model:value="current.from"
                    allow-clear
                    :options="artifactStageOptions"
                    :placeholder="$t('i18n_pipeline_release_from_tip')"
                  />
                </a-form-item>
                <a-form-item :label="$t('i18n_pipeline_release_strategy')">
                  <a-radio-group v-model:value="current.parallel">
                    <a-radio :value="false">{{ $t('i18n_pipeline_release_seq') }}</a-radio>
                    <a-radio :value="true">{{ $t('i18n_pipeline_release_parallel_label') }}</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item :label="$t('i18n_pipeline_release_strict')">
                  <a-switch v-model:checked="current.strictlyEnforce" />
                </a-form-item>
                <a-form-item :label="$t('i18n_pipeline_release_exclude')">
                  <a-input
                    v-model:value="current.excludeReleaseAnt"
                    :placeholder="$t('i18n_pipeline_release_exclude_tip')"
                  />
                </a-form-item>
                <a-form-item :label="$t('i18n_pipeline_release_targets')">
                  <div class="target-list">
                    <div v-for="(target, ti) in current.targets" :key="ti" class="target-card">
                      <div class="target-head">
                        <a-select
                          v-model:value="target.method"
                          style="width: 160px"
                          :options="methodOptions"
                          :data-testid="'pipeline-target-method-' + ti"
                          @change="() => onTargetMethodChange(target)"
                        />
                        <a-button type="text" danger @click="removeTarget(ti)">
                          <DeleteOutlined />
                        </a-button>
                      </div>
                      <div class="target-form">
                        <!-- 节点分发 -->
                        <template v-if="target.method === 1">
                          <a-form-item :label="$t('i18n_pipeline_target_dispatch')">
                            <a-select
                              v-model:value="target.targetId"
                              show-search
                              option-filter-prop="label"
                              :options="dispatchOptions"
                              :placeholder="$t('i18n_pipeline_target_dispatch')"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_target_secondary_dir')">
                            <a-input v-model:value="target.secondaryDirectory" :placeholder="$t('i18n_pipeline_secondary_dir_placeholder')" />
                          </a-form-item>
                        </template>
                        <!-- 项目 -->
                        <template v-if="target.method === 2">
                          <a-form-item :label="$t('i18n_3bf3c0a8d6')">
                            <a-select
                              v-model:value="target.nodeId"
                              show-search
                              option-filter-prop="label"
                              :options="nodeOptions"
                              :placeholder="$t('i18n_pipeline_build_node')"
                              @change="() => loadProjects(target)"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_31ecc0e65b')">
                            <a-select
                              v-model:value="target.projectId"
                              show-search
                              option-filter-prop="label"
                              :options="projectOptions(target)"
                              :placeholder="$t('i18n_31ecc0e65b')"
                              :data-testid="'pipeline-target-project-' + ti"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_target_project_after_opt')">
                            <a-select
                              v-model:value="target.afterOpt"
                              :options="afterOptOptions"
                              :placeholder="$t('i18n_pipeline_after_opt_placeholder')"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_target_secondary_dir')">
                            <a-input v-model:value="target.secondaryDirectory" :placeholder="$t('i18n_pipeline_secondary_dir_placeholder')" />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_target_clear_old')">
                            <a-switch v-model:checked="target.clearOld" />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_target_diff_sync')">
                            <a-switch v-model:checked="target.diffSync" :disabled="!target.clearOld" />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_target_close_first')">
                            <a-switch v-model:checked="target.closeFirst" />
                          </a-form-item>
                        </template>
                        <!-- SSH -->
                        <template v-if="target.method === 3">
                          <a-form-item :label="$t('i18n_pipeline_target_ssh')">
                            <a-select
                              v-model:value="target.ids"
                              mode="multiple"
                              show-search
                              option-filter-prop="label"
                              :options="sshOptions"
                              :placeholder="$t('i18n_pipeline_target_ssh')"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_dbb2df00cf')">
                            <a-auto-complete
                              v-model:value="target.releasePath"
                              :options="sshDirOptions(target)"
                              :placeholder="$t('i18n_pipeline_target_path_placeholder')"
                              style="width: 100%"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_e44f59f2d9')">
                            <code-editor
                              v-model:content="target.beforeCommand"
                              :show-tool="false"
                              :options="{ mode: 'shell' }"
                              height="clamp(120px, 22vh, 260px)"
                              :placeholder="$t('i18n_pipeline_before_command_placeholder')"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_923f8d2688')">
                            <code-editor
                              v-model:content="target.afterCommand"
                              :show-tool="false"
                              :options="{ mode: 'shell' }"
                              height="clamp(120px, 22vh, 260px)"
                              :placeholder="$t('i18n_pipeline_after_command_placeholder')"
                            />
                          </a-form-item>
                        </template>
                        <!-- 本地命令 -->
                        <template v-if="target.method === 4">
                          <a-form-item :label="$t('i18n_923f8d2688')">
                            <code-editor
                              v-model:content="target.afterCommand"
                              :show-tool="false"
                              :options="{ mode: 'shell' }"
                              height="clamp(120px, 22vh, 260px)"
                              :placeholder="$t('i18n_pipeline_after_command_placeholder')"
                            />
                          </a-form-item>
                        </template>
                        <!-- 容器镜像 -->
                        <template v-if="target.method === 5">
                          <a-form-item :label="$t('i18n_pipeline_docker_from_tag')">
                            <a-input v-model:value="target.docker.fromTag" placeholder="openjdk:8" />
                          </a-form-item>
                          <a-form-item label="Dockerfile">
                            <a-input v-model:value="target.docker.dockerfile" placeholder="Dockerfile" />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_docker_tag')">
                            <a-input v-model:value="target.docker.dockerTag" placeholder="app:latest" />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_docker_build_args')">
                            <a-input v-model:value="target.docker.dockerBuildArgs" placeholder="KEY=VALUE" />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_docker_labels')">
                            <a-input v-model:value="target.docker.dockerImagesLabels" :placeholder="$t('i18n_pipeline_docker_labels_placeholder')" />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_docker_network')">
                            <a-auto-complete
                              v-model:value="target.docker.dockerImagesNetworkMode"
                              :options="networkOptions"
                              :placeholder="$t('i18n_pipeline_docker_network_placeholder')"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_docker_swarm')">
                            <a-select
                              v-model:value="target.docker.dockerSwarmId"
                              allow-clear
                              show-search
                              option-filter-prop="label"
                              :options="swarmOptions"
                              :placeholder="$t('i18n_pipeline_docker_swarm_placeholder')"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_docker_service')">
                            <a-input v-model:value="target.docker.dockerSwarmServiceName" :placeholder="$t('i18n_pipeline_docker_service_placeholder')" />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_docker_push')">
                            <a-switch v-model:checked="target.docker.pushToRepository" />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_docker_push_after_delete')">
                            <a-switch
                              v-model:checked="target.docker.pushToRepositoryAfterDelete"
                              :disabled="!target.docker.pushToRepository"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_docker_pull')">
                            <a-switch v-model:checked="target.docker.dockerBuildPull" />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_docker_no_cache')">
                            <a-switch v-model:checked="target.docker.dockerNoCache" />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_docker_tag_increment')">
                            <a-switch v-model:checked="target.docker.dockerTagIncrement" />
                          </a-form-item>
                        </template>
                        <!-- FTP -->
                        <template v-if="target.method === 6">
                          <a-form-item :label="$t('i18n_pipeline_target_ftp')">
                            <a-select
                              v-model:value="target.ids"
                              mode="multiple"
                              show-search
                              option-filter-prop="label"
                              :options="ftpOptions"
                              :placeholder="$t('i18n_pipeline_target_ftp')"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_dbb2df00cf')">
                            <a-auto-complete
                              v-model:value="target.releaseFtpPath"
                              :options="ftpDirOptions(target)"
                              :placeholder="$t('i18n_pipeline_target_path_placeholder')"
                              style="width: 100%"
                            />
                          </a-form-item>
                        </template>
                        <!-- SSH 项目 -->
                        <template v-if="target.method === 7">
                          <a-form-item :label="$t('i18n_8215da30b4')">
                            <a-select
                              v-model:value="target.ids"
                              mode="multiple"
                              show-search
                              option-filter-prop="label"
                              :options="sshProjectOptions"
                              :placeholder="$t('i18n_8215da30b4')"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_923f8d2688')">
                            <code-editor
                              v-model:content="target.afterCommand"
                              :show-tool="false"
                              :options="{ mode: 'shell' }"
                              height="clamp(120px, 22vh, 260px)"
                              :placeholder="$t('i18n_pipeline_after_command_placeholder')"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_target_clear_old')">
                            <a-switch v-model:checked="target.clearOld" />
                          </a-form-item>
                        </template>
                        <!-- 文件中心 -->
                        <template v-if="target.method === 8">
                          <a-form-item :label="$t('i18n_pipeline_target_keep_day')">
                            <a-input-number
                              v-model:value="target.fileStorageKeepDay"
                              style="width: 100%"
                              :min="1"
                              :precision="0"
                              allow-clear
                              :placeholder="$t('i18n_pipeline_target_keep_day_placeholder')"
                              :data-testid="'pipeline-target-keep-day-' + ti"
                            />
                          </a-form-item>
                          <a-form-item :label="$t('i18n_pipeline_target_alias_code')">
                            <a-input
                              v-model:value="target.aliasCode"
                              :max-length="50"
                              :placeholder="$t('i18n_pipeline_target_alias_code_placeholder')"
                              :data-testid="'pipeline-target-alias-code-' + ti"
                            />
                          </a-form-item>
                          <div class="field-tip">{{ $t('i18n_pipeline_target_file_storage_tip') }}</div>
                        </template>
                      </div>
                    </div>
                    <a-button type="dashed" block data-testid="pipeline-add-target" @click="addTarget">
                      <PlusOutlined />
                      {{ $t('i18n_pipeline_target_add') }}
                    </a-button>
                  </div>
                </a-form-item>
              </template>

              <!-- 通知 -->
              <template v-if="current.type === 'NOTIFY'">
                <a-divider orientation="left">{{ $t('i18n_pipeline_stage_type_notify') }}</a-divider>
                <a-form-item :label="$t('i18n_pipeline_notify_scripts')">
                  <a-select
                    v-model:value="current.scripts"
                    mode="multiple"
                    show-search
                    option-filter-prop="label"
                    :options="scriptOptions"
                    :placeholder="$t('i18n_pipeline_notify_scripts')"
                    :data-testid="'pipeline-notify-scripts'"
                  />
                </a-form-item>
                <a-form-item :label="$t('i18n_pipeline_notify_event')">
                  <a-select
                    v-model:value="current.event"
                    :options="eventOptions"
                    :placeholder="$t('i18n_pipeline_notify_event_placeholder')"
                  />
                </a-form-item>
              </template>

              <a-alert
                v-if="!stageValid(current)"
                type="warning"
                show-icon
                class="stage-warning"
                :message="stageInvalidReason(current)"
              />
            </adaptive-form>
        </a-card>
      </div>
      </div>
      <!-- 步骤 3：事件通知（Hooks） -->
      <div v-show="stepsCurrent === 2" data-testid="pipeline-step-hooks">
      <a-card :title="$t('i18n_pipeline_hooks_title')" size="small" class="block hooks-card">
        <div class="card-hint">{{ $t('i18n_pipeline_hooks_tip') }}</div>

        <a-divider orientation="left">{{ $t('i18n_pipeline_hook_scripts') }}</a-divider>
        <a-alert
          v-if="!serverScriptLoading && !serverScripts.length"
          type="info"
          show-icon
          class="hook-alert"
          :message="$t('i18n_pipeline_hook_script_empty')"
        />
        <div v-if="!hooks.scripts.length" class="empty-hint">{{ $t('i18n_pipeline_hooks_empty') }}</div>
        <div
          v-for="(hook, hi) in hooks.scripts"
          :key="hook.key"
          class="hook-card"
          :class="{ 'hook-card-invalid': !scriptHookValid(hook) }"
        >
          <div class="hook-head">
            <b>{{ $t('i18n_pipeline_hook_script_index', { index: hi + 1 }) }}</b>
            <a-space>
              <span class="hook-switch-label">{{ $t('i18n_pipeline_hook_enabled') }}</span>
              <a-switch
                v-model:checked="hook.enabled"
                size="small"
                :data-testid="'pipeline-hook-script-enabled-' + hi"
              />
              <a-button size="small" type="text" danger @click="removeHookScript(hi)">
                <DeleteOutlined />
                {{ $t('i18n_2f4aaddde3') }}
              </a-button>
            </a-space>
          </div>
          <a-form layout="vertical" class="hook-form">
            <a-row :gutter="12">
              <a-col :xs="24" :sm="12">
                <a-form-item :label="$t('i18n_pipeline_hook_events')">
                  <a-select
                    v-model:value="hook.events"
                    mode="multiple"
                    allow-clear
                    :max-tag-count="2"
                    :options="hookEventOptions"
                    :placeholder="$t('i18n_pipeline_hook_events_placeholder')"
                    :data-testid="'pipeline-hook-script-events-' + hi"
                    @change="(value) => onHookEventsChange(hook, value)"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12">
                <a-form-item :label="$t('i18n_pipeline_hook_script')">
                  <a-select
                    v-model:value="hook.scriptId"
                    show-search
                    allow-clear
                    option-filter-prop="label"
                    :options="serverScriptOptions"
                    :placeholder="$t('i18n_pipeline_hook_script_placeholder')"
                    :not-found-content="$t('i18n_pipeline_hook_script_empty')"
                    :data-testid="'pipeline-hook-script-id-' + hi"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <div v-if="!scriptHookValid(hook)" class="hook-error">{{ scriptHookReason(hook) }}</div>
          </a-form>
        </div>
        <a-button
          type="dashed"
          block
          data-testid="pipeline-add-hook-script"
          :disabled="!serverScripts.length"
          @click="addHookScript"
        >
          <PlusOutlined />
          {{ $t('i18n_pipeline_hook_script_add') }}
        </a-button>
        <div v-if="!serverScripts.length" class="field-tip">{{ $t('i18n_pipeline_hook_script_disabled_tip') }}</div>

        <a-divider orientation="left">{{ $t('i18n_pipeline_hook_webhook') }}</a-divider>
        <div
          v-for="(hook, hi) in hooks.webhooks"
          :key="hook.key"
          class="hook-card"
          :class="{ 'hook-card-invalid': !webhookHookValid(hook) }"
        >
          <div class="hook-head">
            <b>{{ $t('i18n_pipeline_hook_webhook_index', { index: hi + 1 }) }}</b>
            <a-space>
              <span class="hook-switch-label">{{ $t('i18n_pipeline_hook_enabled') }}</span>
              <a-switch
                v-model:checked="hook.enabled"
                size="small"
                :data-testid="'pipeline-hook-webhook-enabled-' + hi"
              />
              <a-button size="small" type="text" danger @click="removeHookWebhook(hi)">
                <DeleteOutlined />
                {{ $t('i18n_2f4aaddde3') }}
              </a-button>
            </a-space>
          </div>
          <a-form layout="vertical" class="hook-form">
            <a-row :gutter="12">
              <a-col :xs="24" :sm="10">
                <a-form-item :label="$t('i18n_pipeline_hook_url')">
                  <a-input
                    v-model:value="hook.url"
                    :max-length="500"
                    :placeholder="$t('i18n_pipeline_hook_url_placeholder')"
                    :data-testid="'pipeline-hook-webhook-url-' + hi"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="8">
                <a-form-item :label="$t('i18n_pipeline_hook_events')">
                  <a-select
                    v-model:value="hook.events"
                    mode="multiple"
                    allow-clear
                    :max-tag-count="2"
                    :options="hookEventOptions"
                    :placeholder="$t('i18n_pipeline_hook_events_placeholder')"
                    :data-testid="'pipeline-hook-webhook-events-' + hi"
                    @change="(value) => onHookEventsChange(hook, value)"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="12" :sm="3">
                <a-form-item :label="$t('i18n_pipeline_hook_method')">
                  <a-select v-model:value="hook.method" :options="hookMethodOptions" />
                </a-form-item>
              </a-col>
              <a-col :xs="12" :sm="3">
                <a-form-item :label="$t('i18n_pipeline_hook_body_type')">
                  <a-select
                    v-model:value="hook.bodyType"
                    :options="hookBodyTypeOptions"
                    :disabled="hook.method === 'GET'"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item :label="$t('i18n_pipeline_hook_headers')">
              <a-textarea
                v-model:value="hook.headers"
                :auto-size="{ minRows: 2, maxRows: 6 }"
                :placeholder="$t('i18n_pipeline_hook_headers_placeholder')"
              />
            </a-form-item>
            <div v-if="!webhookHookValid(hook)" class="hook-error">{{ webhookHookReason(hook) }}</div>
          </a-form>
        </div>
        <a-button type="dashed" block data-testid="pipeline-add-hook-webhook" @click="addHookWebhook">
          <PlusOutlined />
          {{ $t('i18n_pipeline_hook_webhook_add') }}
        </a-button>
      </a-card>

      </div>

    </a-spin>

  </div>
</template>

<script>
import { message, Empty } from 'ant-design-vue'
import { getPipelineGet, savePipeline, getPipelineGroupAll, pipelineHookEventCodes } from '@/api/pipeline'
import { getRepositoryList } from '@/api/repository'
import { getBuildNodeList, getBranchList, releaseMethodMap } from '@/api/build-info'
import { getSshListAll } from '@/api/ssh'
import { getSshProjectList } from '@/api/ssh-project'
import { getDishPatchListAll, afterOptList } from '@/api/dispatch'
import { getFtpListAll } from '@/api/ftp'
import { dockerAllTag } from '@/api/docker-api'
import { dockerSwarmListAll } from '@/api/docker-swarm'
import axios from '@/api/config'
import PipelineCanvas from './PipelineCanvas.vue'
import AdaptiveForm from '@/components/adaptiveForm/index.vue'

let seed = 0
const nextKey = () => ++seed

const createDocker = () => ({
  fromTag: '',
  dockerfile: 'Dockerfile',
  dockerTag: '',
  dockerBuildArgs: '',
  dockerImagesLabels: '',
  dockerImagesNetworkMode: 'bridge',
  dockerSwarmId: undefined,
  dockerSwarmServiceName: '',
  pushToRepository: false,
  pushToRepositoryAfterDelete: false,
  dockerBuildPull: false,
  dockerNoCache: false,
  dockerTagIncrement: false
})

const createTarget = () => ({
  method: 3,
  targetId: undefined,
  nodeId: undefined,
  projectId: undefined,
  ids: [],
  afterOpt: 0,
  secondaryDirectory: '',
  releasePath: '',
  releaseFtpPath: '',
  beforeCommand: '',
  afterCommand: '',
  clearOld: false,
  diffSync: false,
  closeFirst: false,
  // 文件中心（method 8）：保留天数留空即长期保留，别名码留空由服务端生成
  fileStorageKeepDay: undefined,
  aliasCode: '',
  docker: createDocker()
})

// 事件脚本行（type=script）：选择事件 + 一个服务端脚本
const createHookScript = () => ({
  key: nextKey(),
  events: [],
  scriptId: undefined,
  enabled: true
})

// Webhook 行（type=webhook）：地址 + 请求方式 + 请求体格式 + 事件 + 自定义请求头
const createHookWebhook = () => ({
  key: nextKey(),
  url: '',
  method: 'POST',
  bodyType: 'json',
  events: [],
  headers: '',
  enabled: true
})

export default {
  name: 'PipelineEdit',
  components: { PipelineCanvas, AdaptiveForm },
  props: {
    id: { type: String, default: '' },
    // 当前步骤（与父级抽屉 footer 的「上一步 / 下一步」双向同步）
    editSteps: { type: Number, default: 0 }
  },
  emits: ['close', 'saved', 'update:editSteps'],
  data() {
    return {
      Empty,
      loading: false,
      saving: false,
      configCollapsed: false,
      stepsCurrent: this.editSteps || 0,
      // 右侧「阶段配置」宽度（百分比）：可拖拽分割线调整并记住；窄屏可缩到 26%，双击分割线复位
      configWidth: this.readConfigWidth(),
      // 左右互斥折叠：canvasCollapsed 隐藏画布把宽度全给配置，configCollapsed 反之
      canvasCollapsed: false,
      stepsItems: [
        { title: this.$t('i18n_pipeline_source_title') },
        // 阶段编排放中间、事件通知放最后：事件通知是「配置完流程之后」的收尾动作
        { title: this.$t('i18n_pipeline_stage_title') },
        { title: this.$t('i18n_pipeline_hooks_title') }
      ],
      form: { id: '', name: '', groupName: '' },
      // 分组候选项（虚拟逻辑分组：后端按当前工作空间去重返回，不是独立管理的分组数据）
      groupList: [],
      sources: [],
      stages: [],
      selectedIndex: -1,
      repos: [],
      repoLoading: false,
      nodes: [],
      sshList: [],
      ftpList: [],
      dispatchList: [],
      sshProjects: [],
      scripts: [],
      dockerTags: [],
      swarmList: [],
      branchCache: {},
      branchLoading: {},
      projectMap: {},
      // 事件通知（Hooks）：按类型分组维护，序列化时合并为一个数组
      hooks: { scripts: [], webhooks: [] },
      serverScripts: [],
      serverScriptLoading: false
    }
  },
  computed: {
    configCardStyle() {
      // 画布收起时，配置面板接管整行宽度（不能只写 width:auto，否则只按内容撑开）
      if (this.canvasCollapsed) {
        return { flex: '1 1 auto', width: 'auto' }
      }
      return { flex: '0 0 auto', width: this.configWidth + '%' }
    },
    current() {
      return this.stages[this.selectedIndex] || {}
    },
    repoOptions() {
      return this.repos.map((repo) => ({
        value: repo.id,
        label: repo.name + (repo.gitUrl ? '（' + repo.gitUrl + '）' : '')
      }))
    },
    nodeOptions() {
      return this.nodes.map((node) => ({
        value: node.id,
        label: node.name + (node.online ? '' : '（' + this.$t('i18n_pipeline_node_offline') + '）')
      }))
    },
    refTypeOptions() {
      return [
        { value: 'branch', label: this.$t('i18n_pipeline_ref_branch') },
        { value: 'tag', label: this.$t('i18n_pipeline_ref_tag') }
      ]
    },
    methodOptions() {
      const list = []
      Object.keys(releaseMethodMap).forEach((key) => {
        const value = Number(key)
        // 0 = 不发布，流水线里没有意义（发布阶段至少一个目标）
        if (value === 0) return
        list.push({ value, label: releaseMethodMap[key] })
      })
      return list
    },
    afterOptOptions() {
      return afterOptList.map((item) => ({ value: item.value, label: item.title }))
    },
    dispatchOptions() {
      return this.dispatchList.map((item) => ({ value: item.id, label: item.name }))
    },
    sshOptions() {
      return this.sshList.map((item) => ({ value: item.id, label: item.name, disabled: !item.fileDirs }))
    },
    ftpOptions() {
      return this.ftpList.map((item) => ({ value: item.id, label: item.name, disabled: !item.fileDirs }))
    },
    sshProjectOptions() {
      return this.sshProjects.map((item) => ({ value: item.id, label: item.name }))
    },
    scriptOptions() {
      return this.scripts.map((item) => ({
        value: item.id,
        label: item.tag + (item.description ? ' - ' + item.description : '')
      }))
    },
    eventOptions() {
      return [
        { value: 'run_finished', label: this.$t('i18n_pipeline_notify_event_finish') },
        { value: 'stage_failed', label: this.$t('i18n_pipeline_notify_event_fail') }
      ]
    },
    serverScriptOptions() {
      return this.serverScripts.map((item) => ({
        value: item.id,
        label: item.name + (item.description ? '（' + item.description + '）' : '')
      }))
    },
    hookEventOptions() {
      return pipelineHookEventCodes.map((code) => ({ value: code, label: this.$t('i18n_pipeline_hook_event_' + code) }))
    },
    hookMethodOptions() {
      return [
        { value: 'POST', label: 'POST' },
        { value: 'GET', label: 'GET' }
      ]
    },
    hookBodyTypeOptions() {
      return [
        { value: 'json', label: 'JSON' },
        { value: 'form', label: 'form' }
      ]
    },
    dockerTagOptions() {
      return this.dockerTags.map((tag) => ({ value: tag }))
    },
    networkOptions() {
      return [
        { value: 'bridge' },
        { value: 'host' },
        { value: 'none' },
        { value: 'container:<name|id>' }
      ]
    },
    swarmOptions() {
      return this.swarmList.map((item) => ({ value: item.id, label: item.name }))
    },
    workDirOptions() {
      const options = [{ value: '', label: this.$t('i18n_pipeline_work_dir_root') }]
      this.sources
        .filter((source) => source.id)
        .forEach((source) => {
          const dir = this.sourceMount(source)
          // 目录名与源标识一致时不再重复展示（旧配置的自定义目录才带上源标识）
          options.push({ value: dir, label: dir === source.id ? dir : dir + ' — ' + source.id })
        })
      // 旧配置的 cwd 不在源目录里：保留为一个显式选项，避免静默改成根目录
      const currentCwd = this.current && this.current.cwd
      if (currentCwd && !options.some((item) => item.value === currentCwd)) {
        options.push({ value: currentCwd, label: currentCwd + '（' + this.$t('i18n_pipeline_work_dir_custom') + '）' })
      }
      return options
    },
    envVariableList() {
      const list = [
        { name: 'PIPELINE_ID', desc: this.$t('i18n_pipeline_var_id') },
        { name: 'PIPELINE_RUN', desc: this.$t('i18n_pipeline_var_run') },
        { name: 'PIPELINE_STAGE_ID', desc: this.$t('i18n_pipeline_var_stage') },
        { name: 'PIPELINE_WORKSPACE', desc: this.$t('i18n_pipeline_var_workspace') }
      ]
      this.sources
        .filter((source) => source.id)
        .forEach((source) => {
          const prefix = 'PIPELINE_SOURCE_' + source.id.toUpperCase()
          list.push({ name: prefix + '_DIR', desc: this.$t('i18n_pipeline_var_source_dir', { id: source.id }) })
          list.push({ name: prefix + '_COMMIT', desc: this.$t('i18n_pipeline_var_source_commit', { id: source.id }) })
        })
      this.stages
        .filter((stage) => stage.id && (stage.type === 'BUILD' || stage.type === 'COMMAND'))
        .forEach((stage) => {
          list.push({
            name: 'PIPELINE_STAGE_' + stage.id.toUpperCase() + '_ARTIFACT',
            desc: this.$t('i18n_pipeline_var_stage_artifact', { name: stage.name || stage.id })
          })
        })
      return list
    },
    envVarColumns() {
      return [
        { title: this.$t('i18n_pipeline_env_var_name'), dataIndex: 'name', key: 'name', width: 320 },
        { title: this.$t('i18n_pipeline_env_var_desc'), dataIndex: 'desc', key: 'desc' }
      ]
    },
    artifactStageOptions() {
      return this.stages
        .filter((stage) => (stage.type === 'BUILD' || stage.type === 'COMMAND') && stage.id)
        .map((stage) => ({ value: stage.id, label: stage.name + '（' + stage.id + '）' }))
    }
  },
  mounted() {
    this.loadRefs()
    this.loadGroupList()
    if (this.id) {
      this.loadDetail()
    }
  },
  watch: {
    // 父级（抽屉 footer 的「上一步 / 下一步」）改动时必须同步到子组件，
    // 否则按钮只改父级计数、内容仍停在原步骤（独立验收 D-1）
    editSteps: {
      handler(v) {
        const total = this.stepsItems.length
        const next = typeof v === 'number' ? Math.max(0, Math.min(total - 1, v)) : 0
        if (next !== this.stepsCurrent) {
          this.stepsCurrent = next
        }
      },
      immediate: true
    }
  },
  methods: {
    readConfigWidth() {
      const v = Number(localStorage.getItem('pipeline-stage-config-width'))
      return v >= 26 && v <= 62 ? v : 42
    },
    toggleCanvas() {
      this.canvasCollapsed = !this.canvasCollapsed
      // 两侧不能同时收起：收起画布时确保配置可见
      if (this.canvasCollapsed) {
        this.configCollapsed = false
      }
    },
    toggleConfig() {
      this.configCollapsed = !this.configCollapsed
      if (this.configCollapsed) {
        this.canvasCollapsed = false
      }
    },
    resetConfigWidth() {
      this.configWidth = 42
      localStorage.setItem('pipeline-stage-config-width', '42')
    },
    /** 拖拽分割线调整「阶段配置」宽度：26%~62%，松手后记住 */
    startResizeConfig(event) {
      const layout = this.$refs.stageLayout
      if (!layout) {
        return
      }
      const rect = layout.getBoundingClientRect()
      const onMove = (ev) => {
        const percent = ((rect.right - ev.clientX) / rect.width) * 100
        this.configWidth = Number(Math.min(62, Math.max(26, percent)).toFixed(2))
      }
      const onUp = () => {
        document.removeEventListener('mousemove', onMove)
        document.removeEventListener('mouseup', onUp)
        document.body.style.userSelect = ''
        localStorage.setItem('pipeline-stage-config-width', String(this.configWidth))
      }
      document.body.style.userSelect = 'none'
      document.addEventListener('mousemove', onMove)
      document.addEventListener('mouseup', onUp)
      event.preventDefault()
    },
    /**
     * 事件选择互斥：「全部事件」与具体事件不能同时存在
     * <p>
     * AntD 的 change 只给最终数组，但数组顺序就是选择顺序：最后一项是 all 说明刚选了「全部事件」，
     * 其余情况说明是在已选 all 后又点了具体事件——两种都收敛成一个语义明确的取值。
     * </p>
     */
    onHookEventsChange(hook, value) {
      const list = Array.isArray(value) ? value.slice() : []
      if (list.length <= 1 || list.indexOf('all') === -1) {
        return
      }
      hook.events = list[list.length - 1] === 'all' ? ['all'] : list.filter((code) => code !== 'all')
    },
    handleStepChange(current) {
      this.stepsCurrent = current
      this.$emit('update:editSteps', current)
    },
    setStep(step) {
      const next = Math.max(0, Math.min(this.stepsItems.length - 1, step))
      this.stepsCurrent = next
      this.$emit('update:editSteps', next)
    },
    loadDetail() {
      this.loading = true
      getPipelineGet({ id: this.id })
        .then((res) => {
          if (res.code === 200) {
            const data = res.data || {}
            this.form = { id: data.id, name: data.name, groupName: data.groupName }
            this.sources = this.parseSources(data.sourcesJson)
            this.stages = this.parseStages(data.stagesJson)
            this.hooks = this.parseHooks(data.hooksJson)
            this.selectedIndex = this.stages.length ? 0 : -1
            // 节点/项目、分支等联动数据：回显时补齐
            this.sources.forEach((source) => {
              if (source.repoId) this.loadBranches(source.repoId)
            })
            this.stages.forEach((stage) => {
              if (stage.repoId) this.loadBranches(stage.repoId)
              if (stage.type === 'RELEASE') {
                ;(stage.targets || []).forEach((target) => {
                  if (target.method === 2 && target.nodeId) this.loadProjects(target)
                })
              }
            })
          } else {
            message.warning(res.msg)
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    // 分组候选项：与在线构建编辑页一致，失败不阻断编辑（下拉仍可手工输入新分组）
    loadGroupList() {
      getPipelineGroupAll().then((res) => {
        if (res.code === 200) {
          this.groupList = res.data || []
        }
      })
    },
    loadRefs() {
      this.repoLoading = true
      getRepositoryList({ page: 1, limit: 100 })
        .then((res) => {
          if (res.code === 200) {
            this.repos = (res.data && res.data.result) || []
          }
        })
        .finally(() => {
          this.repoLoading = false
        })
      getBuildNodeList().then((res) => {
        if (res.code === 200) this.nodes = res.data || []
      })
      getSshListAll().then((res) => {
        if (res.code === 200) this.sshList = res.data || []
      })
      getFtpListAll().then((res) => {
        if (res.code === 200) this.ftpList = res.data || []
      })
      getDishPatchListAll().then((res) => {
        if (res.code === 200) this.dispatchList = res.data || []
      })
      getSshProjectList({ page: 1, limit: 100 }).then((res) => {
        if (res.code === 200) this.sshProjects = (res.data && res.data.result) || []
      })
      axios({ url: '/system/assets/script-library/list-data', method: 'post', data: {} }).then((res) => {
        if (res.code === 200) this.scripts = (res.data && res.data.result) || []
      })
      // 事件通知里的「服务端脚本」来自服务端脚本列表（/script/list-all 无独立 api 导出，直调）
      this.serverScriptLoading = true
      axios({ url: '/script/list-all', method: 'get' })
        .then((res) => {
          if (res.code === 200) this.serverScripts = res.data || []
        })
        .finally(() => {
          this.serverScriptLoading = false
        })
      dockerAllTag().then((res) => {
        if (res.code === 200) this.dockerTags = res.data || []
      })
      dockerSwarmListAll().then((res) => {
        if (res.code === 200) this.swarmList = res.data || []
      })
    },
    loadBranches(repoId, force) {
      if (!repoId) return
      const repo = this.repos.find((item) => item.id === repoId)
      // SVN 仓库没有分支/标签接口，按在线构建同口径只对 GIT（repoType=0）拉取
      if (repo && repo.repoType !== 0) return
      if (!force && this.branchCache[repoId]) return
      this.branchLoading = { ...this.branchLoading, [repoId]: true }
      getBranchList({ repositoryId: repoId })
        .then((res) => {
          if (res.code === 200) {
            this.branchCache = {
              ...this.branchCache,
              [repoId]: {
                branch: (res.data && res.data.branch) || [],
                tags: (res.data && res.data.tags) || []
              }
            }
          }
        })
        .finally(() => {
          this.branchLoading = { ...this.branchLoading, [repoId]: false }
        })
    },
    refOptions(ctx) {
      if (!ctx || !ctx.repoId) return []
      const cache = this.branchCache[ctx.repoId]
      if (!cache) return []
      const list = ctx.refType === 'tag' ? cache.tags : cache.branch
      return list.map((item) => ({ value: item, label: item }))
    },
    composeRef(ctx) {
      if (!ctx || !ctx.refType || !ctx.refName) return ''
      return ctx.refType + ':' + ctx.refName
    },
    parseRef(ctx, ref) {
      const text = ref || ''
      if (text.indexOf('tag:') === 0) {
        ctx.refType = 'tag'
        ctx.refName = text.slice(4)
      } else if (text.indexOf('branch:') === 0) {
        ctx.refType = 'branch'
        ctx.refName = text.slice(7)
      } else {
        ctx.refType = 'branch'
        ctx.refName = undefined
      }
    },
    sourceMount(source) {
      if (!source) return ''
      // 挂载目录 = 源标识（旧配置带自定义 mount 时保持原值，避免路径漂移）
      return source.mount || source.id || ''
    },
    onRepoChange(ctx) {
      if (!ctx) return
      ctx.refName = undefined
      if (ctx.repoId) this.loadBranches(ctx.repoId)
    },
    loadProjects(target) {
      if (!target.nodeId) return
      this.post('/node/project_list', { nodeId: target.nodeId, page: 1, limit: 100 }).then((res) => {
        if (res.code === 200) {
          this.projectMap = { ...this.projectMap, [target.nodeId]: (res.data && res.data.result) || [] }
        }
      })
    },
    projectOptions(target) {
      const list = this.projectMap[target.nodeId] || []
      return list.map((item) => ({ value: item.id, label: item.name }))
    },
    post(url, data) {
      return axios({ url, method: 'post', data })
    },
    parseFileDirs(asset) {
      if (!asset || !asset.fileDirs) return []
      try {
        const list = JSON.parse(asset.fileDirs)
        return (list || []).map((dir) => (dir + '/').replace(new RegExp('//', 'gm'), '/'))
      } catch (e) {
        return []
      }
    },
    sshDirOptions(target) {
      const options = []
      ;(target.ids || []).forEach((id) => {
        const asset = this.sshList.find((item) => item.id === id)
        this.parseFileDirs(asset).forEach((dir) => options.push({ value: dir }))
      })
      return options
    },
    ftpDirOptions(target) {
      const options = []
      ;(target.ids || []).forEach((id) => {
        const asset = this.ftpList.find((item) => item.id === id)
        this.parseFileDirs(asset).forEach((dir) => options.push({ value: dir }))
      })
      return options
    },
    parseHooks(json) {
      let list = []
      try {
        list = JSON.parse(json || '[]') || []
      } catch (e) {
        list = []
      }
      const result = { scripts: [], webhooks: [] }
      list.forEach((raw) => {
        if (!raw) return
        if (raw.type === 'webhook') {
          result.webhooks.push({
            key: nextKey(),
            url: raw.url || '',
            // 只认契约里的两种取值，历史/异常值回落到默认
            method: raw.method === 'GET' ? 'GET' : 'POST',
            bodyType: raw.bodyType === 'form' ? 'form' : 'json',
            events: raw.events || [],
            headers: raw.headers || '',
            enabled: raw.enabled !== false
          })
        } else if (raw.type === 'script') {
          result.scripts.push({
            key: nextKey(),
            events: raw.events || [],
            scriptId: raw.scriptId,
            enabled: raw.enabled !== false
          })
        }
      })
      return result
    },
    addHookScript() {
      this.hooks.scripts.push(createHookScript())
    },
    removeHookScript(index) {
      this.hooks.scripts.splice(index, 1)
    },
    addHookWebhook() {
      this.hooks.webhooks.push(createHookWebhook())
    },
    removeHookWebhook(index) {
      this.hooks.webhooks.splice(index, 1)
    },
    isValidHookUrl(url) {
      return /^https?:\/\/[^\s]+$/i.test((url || '').trim())
    },
    scriptHookValid(hook) {
      return !!(hook && hook.events && hook.events.length && hook.scriptId)
    },
    scriptHookReason(hook) {
      const reasons = []
      if (!hook || !hook.events || !hook.events.length) reasons.push(this.$t('i18n_pipeline_hook_invalid_events'))
      if (!hook || !hook.scriptId) reasons.push(this.$t('i18n_pipeline_hook_invalid_script'))
      return reasons.join('；')
    },
    webhookHookValid(hook) {
      return !!(hook && hook.events && hook.events.length && this.isValidHookUrl(hook.url))
    },
    webhookHookReason(hook) {
      const reasons = []
      if (!hook || !hook.events || !hook.events.length) reasons.push(this.$t('i18n_pipeline_hook_invalid_events'))
      if (!hook || !this.isValidHookUrl(hook.url)) reasons.push(this.$t('i18n_pipeline_hook_invalid_url'))
      return reasons.join('；')
    },
    /**
     * 保存前的 hooks 校验：返回第一条不合法行的描述（空字符串表示全部通过）
     * 前端只做交互提示，服务端仍会再校验一次
     */
    firstHookError() {
      for (let index = 0; index < this.hooks.scripts.length; index++) {
        const hook = this.hooks.scripts[index]
        if (!this.scriptHookValid(hook)) {
          return (
            this.$t('i18n_pipeline_hook_script_row_invalid', { index: index + 1 }) + '：' + this.scriptHookReason(hook)
          )
        }
      }
      for (let index = 0; index < this.hooks.webhooks.length; index++) {
        const hook = this.hooks.webhooks[index]
        if (!this.webhookHookValid(hook)) {
          return (
            this.$t('i18n_pipeline_hook_webhook_row_invalid', { index: index + 1 }) +
            '：' +
            this.webhookHookReason(hook)
          )
        }
      }
      return ''
    },
    serializeHooks() {
      const list = []
      this.hooks.scripts.forEach((hook) => {
        list.push({
          type: 'script',
          enabled: hook.enabled !== false,
          events: hook.events || [],
          scriptId: hook.scriptId
        })
      })
      this.hooks.webhooks.forEach((hook) => {
        list.push({
          type: 'webhook',
          enabled: hook.enabled !== false,
          events: hook.events || [],
          url: (hook.url || '').trim(),
          method: hook.method === 'GET' ? 'GET' : 'POST',
          bodyType: hook.bodyType === 'form' ? 'form' : 'json',
          headers: (hook.headers || '').trim()
        })
      })
      return list
    },
    parseSources(json) {
      let list = []
      try {
        list = JSON.parse(json || '[]') || []
      } catch (e) {
        list = []
      }
      return list.map((item) => {
        const source = {
          key: nextKey(),
          id: item.id,
          repoId: item.repoId,
          // 旧配置的 mount 原样保留（只读展示）；新源为空表示跟随源标识
          mount: item.mount || undefined,
          cloneDepth: item.cloneDepth,
          refType: 'branch',
          refName: undefined
        }
        this.parseRef(source, item.ref)
        return source
      })
    },
    parseTarget(raw) {
      const target = createTarget()
      target.method = raw.method
      const extra = raw.extra || {}
      const id = raw.id || ''
      if (target.method === 1) {
        target.targetId = id
      } else if (target.method === 2) {
        const parts = String(id).split(':')
        target.nodeId = parts[0]
        target.projectId = parts[1]
      } else if (target.method === 3 || target.method === 6 || target.method === 7) {
        target.ids = id ? String(id).split(',') : []
      } else if (target.method === 8) {
        // 文件中心：目标 id 为空，配置都在 extra 里
        target.fileStorageKeepDay =
          extra.fileStorageKeepDay === undefined || extra.fileStorageKeepDay === null || extra.fileStorageKeepDay === ''
            ? undefined
            : Number(extra.fileStorageKeepDay)
        target.aliasCode = extra.aliasCode || ''
      }
      target.afterOpt = extra.afterOpt === undefined ? 0 : extra.afterOpt
      target.secondaryDirectory = extra.projectSecondaryDirectory || ''
      target.releasePath = extra.releasePath || ''
      target.releaseFtpPath = extra.releaseFtpPath || ''
      target.beforeCommand = extra.releaseBeforeCommand || ''
      target.afterCommand = extra.releaseCommand || ''
      target.clearOld = !!extra.clearOld
      target.diffSync = !!extra.diffSync
      target.closeFirst = !!extra.projectUploadCloseFirst
      target.docker = {
        fromTag: extra.fromTag || '',
        dockerfile: extra.dockerfile || '',
        dockerTag: extra.dockerTag || '',
        dockerBuildArgs: extra.dockerBuildArgs || '',
        dockerImagesLabels: extra.dockerImagesLabels || '',
        dockerImagesNetworkMode: extra.dockerImagesNetworkMode || 'bridge',
        dockerSwarmId: extra.dockerSwarmId,
        dockerSwarmServiceName: extra.dockerSwarmServiceName || '',
        pushToRepository: !!extra.pushToRepository,
        pushToRepositoryAfterDelete: !!extra.pushToRepositoryAfterDelete,
        dockerBuildPull: !!extra.dockerBuildPull,
        dockerNoCache: !!extra.dockerNoCache,
        dockerTagIncrement: !!extra.dockerTagIncrement
      }
      return target
    },
    parseStages(json) {
      let list = []
      try {
        list = JSON.parse(json || '[]') || []
      } catch (e) {
        list = []
      }
      return list.map((raw) => {
        const config = raw.config || {}
        const stage = {
          key: nextKey(),
          id: raw.id,
          name: raw.name,
          type: raw.type,
          enabled: raw.enabled !== false,
          failFast: raw.failFast !== false,
          executor: config.executor || 'local',
          cwd: config.cwd || '',
          run: config.run || '',
          outputs: config.outputs || [],
          resultDirFile: config.resultDirFile || '/',
          image: config.image || '',
          nodeId: config.nodeId,
          repoId: config.repoId,
          env: config.env || '',
          refType: 'branch',
          refName: undefined,
          from: config.from,
          parallel: config.parallel === true,
          strictlyEnforce: config.strictlyEnforce === true,
          excludeReleaseAnt: config.excludeReleaseAnt || '',
          targets: (config.targets || []).map((target) => this.parseTarget(target)),
          scripts: config.scripts || [],
          event: config.event || 'run_finished'
        }
        this.parseRef(stage, config.ref)
        return stage
      })
    },
    typeLabel(type) {
      const map = {
        BUILD: this.$t('i18n_pipeline_stage_type_build'),
        COMMAND: this.$t('i18n_pipeline_stage_type_command'),
        RELEASE: this.$t('i18n_pipeline_stage_type_release'),
        NOTIFY: this.$t('i18n_pipeline_stage_type_notify')
      }
      return map[type] || type
    },
    nextStageId() {
      let index = this.stages.length + 1
      let candidate = 'stage' + index
      const used = this.stages.map((stage) => stage.id)
      while (used.indexOf(candidate) > -1) {
        index++
        candidate = 'stage' + index
      }
      return candidate
    },
    addSource() {
      this.sources.push({
        key: nextKey(),
        id: 'source' + (this.sources.length + 1),
        repoId: undefined,
        mount: undefined,
        cloneDepth: undefined,
        refType: 'branch',
        refName: undefined
      })
    },
    removeSource(index) {
      this.sources.splice(index, 1)
    },
    addStage(type) {
      const stage = {
        key: nextKey(),
        id: this.nextStageId(),
        name: this.typeLabel(type),
        type,
        enabled: true,
        failFast: true,
        executor: 'local',
        cwd: '',
        run: '',
        outputs: type === 'BUILD' ? ['**/*'] : [],
        resultDirFile: '/',
        image: '',
        nodeId: undefined,
        repoId: undefined,
        env: '',
        refType: 'branch',
        refName: undefined,
        from: undefined,
        parallel: false,
        strictlyEnforce: false,
        excludeReleaseAnt: '',
        targets: [],
        scripts: [],
        event: 'run_finished'
      }
      if (type === 'RELEASE') stage.targets = [createTarget()]
      this.stages.push(stage)
      this.selectedIndex = this.stages.length - 1
      if (this.configCollapsed) this.configCollapsed = false
    },
    removeStage(index) {
      this.stages.splice(index, 1)
      if (this.selectedIndex >= this.stages.length) this.selectedIndex = this.stages.length - 1
    },
    moveStage(delta) {
      const from = this.selectedIndex
      const to = from + delta
      if (from < 0 || to < 0 || to >= this.stages.length) return
      const item = this.stages.splice(from, 1)[0]
      this.stages.splice(to, 0, item)
      this.selectedIndex = to
    },
    onReorder(payload) {
      const from = payload.from
      const to = payload.to
      if (from === to || from < 0 || to < 0 || from >= this.stages.length) return
      const item = this.stages.splice(from, 1)[0]
      this.stages.splice(to, 0, item)
      this.selectedIndex = to
    },
    addTarget() {
      if (!this.current.targets) this.current.targets = []
      this.current.targets.push(createTarget())
    },
    removeTarget(index) {
      this.current.targets.splice(index, 1)
    },
    onTargetMethodChange(target) {
      target.targetId = undefined
      target.projectId = undefined
      target.ids = []
      // 切换发布方式时清掉文件中心专属配置，避免残留值被带到新方式
      target.fileStorageKeepDay = undefined
      target.aliasCode = ''
    },
    stageValid(stage) {
      if (!stage || !stage.id || !stage.name) return false
      if (stage.type === 'BUILD' || stage.type === 'COMMAND') {
        if (!stage.run) return false
        if (stage.executor === 'node' && (!stage.nodeId || !stage.repoId)) return false
        if (stage.executor === 'container' && !stage.image) return false
      }
      if (stage.type === 'RELEASE') {
        if (!stage.targets || !stage.targets.length) return false
        return stage.targets.every((target) => this.targetValid(target))
      }
      if (stage.type === 'NOTIFY' && (!stage.scripts || !stage.scripts.length)) return false
      return true
    },
    targetValid(target) {
      if (!target) return false
      if (target.method === 1) return !!target.targetId
      if (target.method === 2) return !!(target.nodeId && target.projectId)
      if (target.method === 3 || target.method === 6 || target.method === 7) return !!(target.ids && target.ids.length)
      // 文件中心：保留天数与别名码都可空，始终可用
      if (target.method === 8) return true
      return true
    },
    stageInvalidReason(stage) {
      if (!stage.id || !stage.name) return this.$t('i18n_pipeline_stage_invalid_basic')
      if ((stage.type === 'BUILD' || stage.type === 'COMMAND') && !stage.run) return this.$t('i18n_pipeline_stage_invalid_command')
      if (stage.type === 'BUILD' && stage.executor === 'node' && (!stage.nodeId || !stage.repoId)) {
        return this.$t('i18n_pipeline_stage_invalid_node')
      }
      if (stage.type === 'BUILD' && stage.executor === 'container' && !stage.image) {
        return this.$t('i18n_pipeline_stage_invalid_image')
      }
      if (stage.type === 'RELEASE' && (!stage.targets || !stage.targets.length)) return this.$t('i18n_pipeline_stage_invalid_target')
      if (stage.type === 'RELEASE' && stage.targets && !stage.targets.every((target) => this.targetValid(target))) {
        return this.$t('i18n_pipeline_stage_invalid_target_detail')
      }
      if (stage.type === 'NOTIFY' && (!stage.scripts || !stage.scripts.length)) return this.$t('i18n_pipeline_stage_invalid_notify')
      return ''
    },
    serializeTarget(target, stage) {
      const result = { method: target.method, extra: {} }
      const extra = result.extra
      // 阶段级发布开关：与在线构建同名字段，逐目标下发给发布实现
      if (stage && stage.strictlyEnforce) extra.strictlyEnforce = true
      if (stage && stage.excludeReleaseAnt) extra.excludeReleaseAnt = stage.excludeReleaseAnt
      if (target.method === 1) {
        result.id = target.targetId
        extra.projectSecondaryDirectory = target.secondaryDirectory
      } else if (target.method === 2) {
        result.id = (target.nodeId || '') + ':' + (target.projectId || '')
        extra.afterOpt = target.afterOpt
        extra.projectSecondaryDirectory = target.secondaryDirectory
        extra.clearOld = target.clearOld
        extra.diffSync = target.diffSync
        extra.projectUploadCloseFirst = target.closeFirst
      } else if (target.method === 3) {
        result.id = (target.ids || []).join(',')
        extra.releasePath = target.releasePath
        extra.releaseBeforeCommand = target.beforeCommand
        extra.releaseCommand = target.afterCommand
      } else if (target.method === 4) {
        result.id = ''
        extra.releaseCommand = target.afterCommand
      } else if (target.method === 5) {
        result.id = ''
        Object.keys(target.docker || {}).forEach((key) => {
          extra[key] = target.docker[key]
        })
      } else if (target.method === 6) {
        result.id = (target.ids || []).join(',')
        extra.releaseFtpPath = target.releaseFtpPath
      } else if (target.method === 7) {
        result.id = (target.ids || []).join(',')
        extra.releaseCommand = target.afterCommand
        extra.clearOld = target.clearOld
      } else if (target.method === 8) {
        // 文件中心：id 固定为空串，保留天数留空表示长期保留（不下发该键）
        result.id = ''
        if (
          target.fileStorageKeepDay !== undefined &&
          target.fileStorageKeepDay !== null &&
          target.fileStorageKeepDay !== ''
        ) {
          extra.fileStorageKeepDay = Math.floor(Number(target.fileStorageKeepDay))
        }
        extra.aliasCode = target.aliasCode || ''
      }
      return result
    },
    buildStageConfig(stage) {
      if (stage.type === 'BUILD' || stage.type === 'COMMAND') {
        const config = { executor: stage.executor, cwd: stage.cwd, run: stage.run, env: stage.env }
        if (stage.executor === 'container') {
          config.image = stage.image
        }
        if (stage.executor === 'node') {
          config.nodeId = stage.nodeId
          config.repoId = stage.repoId
          config.ref = this.composeRef(stage)
          config.resultDirFile = stage.resultDirFile || '/'
        }
        if (stage.type === 'BUILD') {
          config.outputs = stage.outputs || []
        }
        return config
      }
      if (stage.type === 'RELEASE') {
        return {
          from: stage.from,
          parallel: stage.parallel,
          failFast: stage.failFast,
          strictlyEnforce: stage.strictlyEnforce,
          excludeReleaseAnt: stage.excludeReleaseAnt,
          targets: (stage.targets || []).map((target) => this.serializeTarget(target, stage))
        }
      }
      if (stage.type === 'NOTIFY') {
        return { scripts: stage.scripts, event: stage.event }
      }
      return {}
    },
    handleSave() {
      if (!this.form.name) {
        this.setStep(0)
        message.warning(this.$t('i18n_pipeline_name_required'))
        return
      }
      if (!this.stages.length) {
        this.setStep(1)
        message.warning(this.$t('i18n_pipeline_stage_required'))
        return
      }
      const invalid = this.stages.find((stage) => !this.stageValid(stage))
      if (invalid) {
        this.setStep(1)
        message.error(this.$t('i18n_pipeline_stage_invalid') + '：' + (invalid.name || invalid.id) + ' - ' + this.stageInvalidReason(invalid))
        this.selectedIndex = this.stages.indexOf(invalid)
        this.configCollapsed = false
        return
      }
      const ids = this.stages.map((stage) => stage.id)
      const duplicated = ids.filter((id, index) => ids.indexOf(id) !== index)
      if (duplicated.length) {
        this.setStep(1)
        message.error(this.$t('i18n_pipeline_stage_key_duplicate') + '：' + duplicated.join(','))
        return
      }
      // 事件通知与阶段校验分开：行内标红之外，保存前再指明具体第几行
      const hookError = this.firstHookError()
      if (hookError) {
        this.setStep(2)
        message.error(hookError)
        return
      }
      const sourcesJson = JSON.stringify(
        this.sources
          .filter((source) => source.id && source.repoId)
          .map((source) => ({
            id: source.id,
            repoId: source.repoId,
            ref: this.composeRef(source),
            // 新源挂载目录跟随源标识；旧配置保留原自定义 mount
            mount: source.mount || source.id,
            cloneDepth: source.cloneDepth
          }))
      )
      const stagesJson = JSON.stringify(
        this.stages.map((stage) => ({
          id: stage.id,
          name: stage.name,
          type: stage.type,
          enabled: stage.enabled,
          failFast: stage.failFast,
          config: this.buildStageConfig(stage)
        }))
      )
      const hooksJson = JSON.stringify(this.serializeHooks())
      this.saving = true
      this.$emit('saving-change', true)
      savePipeline({
        id: this.form.id,
        name: this.form.name,
        group: this.form.groupName,
        sourcesJson,
        stagesJson,
        hooksJson
      })
        .then((res) => {
          if (res.code === 200) {
            message.success(this.$t('i18n_pipeline_save_success'))
            this.$emit('saved')
            this.$emit('close')
          } else {
            message.error(res.msg)
          }
        })
        .finally(() => {
          this.saving = false
          this.$emit('saving-change', false)
        })
    }
  }
}
</script>

<style scoped>
.pipeline-editor {
  padding-bottom: 56px;
}
.block {
  margin-top: 8px;
}
.block-tip {
  margin-bottom: 8px;
}
.empty-hint {
  padding: 12px 0;
  color: var(--jp-color-text-tertiary);
  font-size: 13px;
}
.field-tip {
  color: var(--jp-color-text-tertiary);
  font-size: 12px;
  line-height: 18px;
  margin-top: 2px;
}
.env-var-table {
  max-width: 560px;
}
.card-hint {
  color: var(--jp-color-text-tertiary);
  font-size: 12px;
  margin: -4px 0 8px;
}
.source-card {
  border: 1px solid var(--jp-color-border-secondary);
  border-radius: 8px;
  padding: 10px 12px 2px;
  margin-bottom: 8px;
  background: var(--jp-color-fill-alter);
}
.source-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.source-form :deep(.ant-form-item) {
  margin-bottom: 10px;
}
.source-form :deep(.ant-form-item-label) {
  padding-bottom: 2px;
}
.source-row {
  margin-bottom: 8px;
}
.hooks-card :deep(.ant-divider) {
  margin: 8px 0;
}
.hook-alert {
  margin-bottom: 8px;
}
.hook-card {
  border: 1px solid var(--jp-color-border-secondary);
  border-radius: 8px;
  padding: 10px 12px 2px;
  margin-bottom: 8px;
  background: var(--jp-color-fill-alter);
}
/* 未选事件 / 地址非法：整行标红，配合下方行内原因 */
.hook-card-invalid {
  border-color: var(--jp-color-error-border);
  background: var(--jp-color-error-bg);
}
.hook-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.hook-switch-label {
  color: var(--jp-color-text-tertiary);
  font-size: 12px;
}
.hook-form :deep(.ant-form-item) {
  margin-bottom: 10px;
}
.hook-form :deep(.ant-form-item-label) {
  padding-bottom: 2px;
}
.hook-error {
  color: var(--jp-color-error-text);
  font-size: 12px;
  line-height: 18px;
  padding-bottom: 8px;
}
.canvas-wrap {
  /* 高度由卡片 flex 分配（见 .stage-canvas-card），不再按视口猜数字 */
  flex: 1 1 auto;
  min-height: 240px;
}
.canvas-tip {
  color: var(--jp-color-text-tertiary);
  font-size: 12px;
  margin-top: 6px;
}
.target-list {
  width: 100%;
}
.target-card {
  border: 1px solid var(--jp-color-border-secondary);
  border-radius: 8px;
  padding: 8px;
  margin-bottom: 8px;
  background: var(--jp-color-fill-alter);
}
.target-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}
.target-form :deep(.ant-form-item) {
  margin-bottom: 8px;
}
.stage-form :deep(.ant-form-item) {
  margin-bottom: 10px;
}
.stage-warning {
  margin-top: 8px;
}
.pipeline-editor {
  /* 高度交给抽屉：editor 占满 body，各步骤内用 flex 分配，不再用 100vh-420px 这类魔法数字 */
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding-bottom: 0;
}
.pipeline-editor :deep(.ant-spin-nested-loading),
.pipeline-editor :deep(.ant-spin-container) {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.step-pane-stages {
  /* 阶段编排这一步要吃掉剩余高度，画布与配置卡片才能等高铺满 */
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.stage-layout {
  display: flex;
  align-items: stretch;
  flex: 1 1 auto;
  min-height: 0;
}
.stage-canvas-card,
.stage-config-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}
.stage-canvas-card {
  flex: 1 1 auto;
}
.stage-config-card {
  flex: 0 0 auto;
}
.stage-canvas-card :deep(.ant-card-body),
.stage-config-card :deep(.ant-card-body) {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}
/* 画布卡片 body 必须是 flex 列，否则 .canvas-wrap 的 flex:1 无处可依 → .vue-flow 高度 0（节点看不见） */
.stage-canvas-card :deep(.ant-card-body) {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
/* 可拖拽分割线：视觉上是一条浅色竖条，hover 变主色，光标为 col-resize */
.stage-splitter {
  flex: 0 0 18px;
  margin-left: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: col-resize;
}
.stage-splitter-btn {
  border: none;
  background: transparent;
  color: var(--jp-color-text-tertiary);
  cursor: pointer;
  padding: 0;
  line-height: 1;
  font-size: 12px;
}
.stage-splitter-btn:hover {
  color: var(--jp-color-primary);
}
.stage-splitter-bar {
  width: 4px;
  height: 46px;
  border-radius: 2px;
  background: var(--jp-color-border);
  transition: background 0.2s;
}
.stage-splitter:hover .stage-splitter-bar {
  background: var(--jp-color-primary);
}
.editor-steps {
  /* 步骤条与下方区块留出呼吸感，并随主题色 */
  margin: 4px 0 12px;
  padding: 10px 12px;
  border: 1px solid var(--jp-color-border-secondary);
  border-radius: var(--jp-radius-lg);
  background: var(--jp-color-fill-alter);
}
</style>
