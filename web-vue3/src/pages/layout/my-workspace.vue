<template>
  <div>
    <a-row type="flex" justify="center">
      <a-col :span="18">
        <a-card title="我的工作空间" :bordered="true">
          <draggable
            v-model="myWorkspaceList"
            :group="`sort`"
            @end="sortFieldEnd()"
            handle=".move"
            chosenClass="box-shadow"
          >
            <a-row v-for="item in myWorkspaceList" :key="item.id" class="item-row">
              <a-col :span="18">
                <template v-if="item.edit">
                  <a-input-search
                    placeholder="请输入工作空间备注,留空使用默认的名称"
                    enter-button="确定"
                    v-model="item.name"
                    @search="editOk(item)"
                  />
                </template>
                <template v-else>
                  <a-tooltip :title="`原始名：${item.originalName}`">
                    {{ item.name || item.originalName }}
                  </a-tooltip>
                </template>
              </a-col>
              <a-col :span="2"></a-col>
              <a-col :span="4">
                <a-space>
                  <a-button :disabled="item.edit" type="primary" icon="edit" size="small" @click="edit(item)">
                  </a-button>
                  <a-tooltip placement="left" :title="`长按可以拖动排序`" class="move">
                    <a-icon type="menu" />
                  </a-tooltip>
                </a-space>
              </a-col>
            </a-row>
          </draggable>
          <a-col style="margin-top: 10px">
            <a-space>
              <a-button type="primary" @click="save"> 保存 </a-button>
              <a-button
                type="primary"
                @click="
                  () => {
                    myWorkspaceList = myWorkspaceList.map((item) => {
                      return { ...item, name: '' }
                    })
                  }
                "
              >
                恢复默认名称
              </a-button>
            </a-space>
          </a-col>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>
<script>
import { myWorkspace, saveWorkspace } from '@/api/user/user'
import draggable from 'vuedraggable'

export default {
  components: {
    draggable
  },
  data() {
    return {
      myWorkspaceList: [],
      loading: true
    }
  },
  created() {
    this.init()
  },
  methods: {
    init() {
      myWorkspace().then((res) => {
        if (res.code == 200 && res.data) {
          this.myWorkspaceList = res.data
        }
        this.loading = false
      })
    },
    edit(editItem) {
      this.myWorkspaceList = this.myWorkspaceList.map((item) => {
        if (item.id === editItem.id) {
          item.edit = true
        }
        return item
      })
    },
    sortFieldEnd() {
      this.myWorkspaceList = this.myWorkspaceList.map((item, index) => {
        return { ...item, sort: index }
      })
    },
    // 编辑 ok
    editOk(editItem) {
      this.myWorkspaceList = this.myWorkspaceList.map((item) => {
        if (item.id === editItem.id) {
          item.edit = false
        }
        return item
      })
    },
    // 保存
    save() {
      saveWorkspace(this.myWorkspaceList).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
        }
      })
    }
  }
}
</script>
<style scoped>
.box-shadow {
  box-shadow: 0 0 10px 5px rgba(223, 222, 222, 0.5);
  border-radius: 5px;
}

.item-row {
  padding: 10px;
  margin: 5px;
  border: 1px solid #e8e8e8;
  border-radius: 2px;
}
</style>
