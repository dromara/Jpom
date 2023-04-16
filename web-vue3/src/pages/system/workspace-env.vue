<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="envVarList"
      size="middle"
      :loading="envVarLoading"
      :columns="envVarColumns"
      :pagination="envVarPagination"
      @change="changeListeEnvVar"
      bordered
      :rowKey="(record, index) => index"
    >
      <template #title>
        <a-space>
          <a-input
            v-model="envVarListQuery['%name%']"
            placeholder="名称"
            @pressEnter="loadDataEnvVar"
            allowClear
            class="search-input-item"
          />
          <a-input
            v-model="envVarListQuery['%value%']"
            placeholder="值"
            @pressEnter="loadDataEnvVar"
            allowClear
            class="search-input-item"
          />
          <a-input
            v-model="envVarListQuery['%description%']"
            placeholder="描述"
            @pressEnter="loadDataEnvVar"
            allowClear
            class="search-input-item"
          />
          <a-button type="primary" @click="loadDataEnvVar">搜索</a-button>
          <a-button type="primary" @click="addEnvVar">新增</a-button>
        </a-space>
      </template>
      <a-tooltip
        slot="value"
        slot-scope="text, item"
        placement="topLeft"
        :title="item.privacy === 1 ? '隐私字段' : text"
      >
        <a-icon v-if="item.privacy === 1" type="eye-invisible" />
        <span v-else>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="description" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="workspaceId" slot-scope="text">
        <span>{{ text === 'GLOBAL' ? '全局' : '当前工作空间' }}</span>
      </template>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEnvEdit(record)">编辑</a-button>
          <a-button size="small" type="danger" @click="handleEnvDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>

    <!-- 环境变量编辑区 -->
    <a-modal v-model="editEnvVisible" title="编辑环境变量" width="50vw" @ok="handleEnvEditOk" :maskClosable="false">
      <a-form ref="editEnvForm" :rules="rulesEnv" :model="envTemp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="名称" prop="name">
          <a-input v-model="envTemp.name" :maxLength="50" placeholder="变量名称" />
        </a-form-item>
        <a-form-item label="值" :prop="`${envTemp.privacy === 1 ? '' : 'value'}`">
          <a-input v-model="envTemp.value" type="textarea" :rows="5" placeholder="变量值" />
        </a-form-item>
        <a-form-item label="描述" prop="description">
          <a-input v-model="envTemp.description" :maxLength="200" type="textarea" :rows="5" placeholder="变量描述" />
        </a-form-item>
        <a-form-item prop="privacy">
          <template slot="label">
            隐私变量
            <a-tooltip v-show="!envTemp.id">
              <template slot="title">
                隐私变量是指一些密码字段或者关键密钥等重要信息，隐私字段只能修改不能查看（编辑弹窗中无法看到对应值）。
                隐私字段一旦创建后将不能切换为非隐私字段
              </template>
              <question-circle-filled />
            </a-tooltip>
          </template>
          <a-switch
            :checked="envTemp.privacy === 1"
            @change="
              (checked) => {
                envTemp = { ...envTemp, privacy: checked ? 1 : 0 }
              }
            "
            :disabled="envTemp.id !== undefined"
            checked-children="隐私"
            un-checked-children="非隐私"
          />
        </a-form-item>
        <a-form-item>
          <template slot="label">
            分发节点
            <a-tooltip v-show="!envTemp.id">
              <template slot="title"> 分发节点是指将变量同步到对应节点，在节点脚本中也可以使用当前变量</template>
              <question-circle-filled />
            </a-tooltip>
          </template>
          <a-select
            show-search
            option-filter-prop="children"
            placeholder="请选择分发到的节点"
            mode="multiple"
            v-model="envTemp.chooseNode"
          >
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script>
import { deleteWorkspaceEnv, editWorkspaceEnv, getWorkspaceEnvList } from '@/api/workspace'
import { getNodeListByWorkspace } from '@/api/node'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  props: {
    workspaceId: {
      type: String,
      default: ''
    },
    global: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      envVarLoading: false,
      envVarList: [],
      nodeList: [],
      envVarListQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editEnvVisible: false,
      envTemp: {},
      envVarColumns: [
        { title: '名称', dataIndex: 'name', ellipsis: true, scopedSlots: { customRender: 'name' } },
        { title: '值', dataIndex: 'value', ellipsis: true, scopedSlots: { customRender: 'value' } },

        { title: '描述', dataIndex: 'description', ellipsis: true, scopedSlots: { customRender: 'description' } },
        {
          title: '修改人',
          dataIndex: 'modifyUser',
          ellipsis: true,
          scopedSlots: { customRender: 'modifyUser' },
          width: 120
        },
        {
          title: '作用域',
          dataIndex: 'workspaceId',
          ellipsis: true,
          scopedSlots: { customRender: 'workspaceId' },
          width: '120px'
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          customRender: (text) => {
            return parseTime(text)
          },
          sorter: true,
          width: '180px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          align: 'center',
          scopedSlots: { customRender: 'operation' },
          width: 120
        }
      ],
      // 表单校验规则
      rulesEnv: {
        name: [{ required: true, message: '请输入变量名称', trigger: 'blur' }],
        description: [{ required: true, message: '请输入变量描述', trigger: 'blur' }],
        value: [{ required: true, message: '请输入变量值', trigger: 'blur' }]
      }
    }
  },
  computed: {
    envVarPagination() {
      return COMPUTED_PAGINATION(this.envVarListQuery)
    }
  },
  mounted() {
    this.loadDataEnvVar()
  },
  methods: {
    loadDataEnvVar(pointerEvent) {
      this.envVarLoading = true

      this.envVarListQuery.workspaceId = this.workspaceId + (this.global ? ',GLOBAL' : '')
      this.envVarListQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.envVarListQuery.page
      getWorkspaceEnvList(this.envVarListQuery).then((res) => {
        if (res.code === 200) {
          this.envVarList = res.data.result
          this.envVarListQuery.total = res.data.total
        }
        this.envVarLoading = false
      })
    },
    addEnvVar() {
      this.envTemp = {
        workspaceId: this.workspaceId
      }

      this.editEnvVisible = true
      this.$refs['editEnvForm'] && this.$refs['editEnvForm'].resetFields()
      this.getAllNodeList(this.envTemp.workspaceId)
    },
    handleEnvEdit(record) {
      this.envTemp = Object.assign({}, record)
      this.envTemp.workspaceId = this.workspaceId
      this.envTemp = { ...this.envTemp, chooseNode: record.nodeIds ? record.nodeIds.split(',') : [] }
      this.editEnvVisible = true
      this.getAllNodeList(this.envTemp.workspaceId)
    },
    handleEnvEditOk() {
      this.$refs['editEnvForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        this.envTemp.nodeIds = this.envTemp?.chooseNode?.join(',')
        editWorkspaceEnv(this.envTemp).then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })
            this.$refs['editEnvForm'].resetFields()
            this.editEnvVisible = false
            this.loadDataEnvVar()
          }
        })
      })
    },
    //
    handleEnvDelete(record) {
      $confirm({
        title: '系统提示',
        content: '真的删除当前变量吗？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteWorkspaceEnv({
            id: record.id,
            workspaceId: this.workspaceId
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadDataEnvVar()
            }
          })
        }
      })
    },
    // 获取所有节点
    getAllNodeList(workspaceId) {
      getNodeListByWorkspace({
        workspaceId: workspaceId
      }).then((res) => {
        this.nodeList = res.data || []
      })
    },
    changeListeEnvVar(pagination, filters, sorter) {
      this.envVarListQuery = CHANGE_PAGE(this.envVarListQuery, { pagination, sorter })

      this.loadDataEnvVar()
    }
  }
}
</script>
