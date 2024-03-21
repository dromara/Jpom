<template>
  <div>
    <a-row type="flex" justify="center">
      <a-col :span="18">
        <a-card title="我的工作空间" :bordered="true">
          <Container drag-handle-selector=".move" orientation="vertical" @drop="onDrop">
            <Draggable v-for="(element, index) in myWorkspaceList" :key="index">
              <a-row class="item-row">
                <a-col :span="18">
                  <template v-if="element.edit">
                    <a-input-search
                      v-model:value="element.name"
                      placeholder="请输入工作空间备注,留空使用默认的名称"
                      enter-button="确定"
                      @search="editOk(element)"
                    />
                  </template>
                  <template v-else>
                    <a-tooltip :title="`原始名：${element.originalName}`">
                      {{ element.name || element.originalName }}
                    </a-tooltip>
                  </template>
                </a-col>
                <a-col :span="2"></a-col>
                <a-col :span="4">
                  <a-space>
                    <a-button :disabled="element.edit" type="primary" size="small" @click="edit(element)">
                      <template #icon><EditOutlined /></template>
                    </a-button>
                    <a-tooltip placement="left" :title="`长按可以拖动排序`" class="move">
                      <MenuOutlined />
                    </a-tooltip>
                  </a-space>
                </a-col>
              </a-row>
            </Draggable>
          </Container>
          <a-col style="margin-top: 10px">
            <a-space>
              <a-button type="primary" @click="save"> 保存 </a-button>
              <a-button type="primary" @click="resetDefaultName"> 恢复默认名称 </a-button>
            </a-space>
          </a-col>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>
<script>
import { myWorkspace, saveWorkspace } from '@/api/user/user'
import { dropApplyDrag } from '@/utils/const'
import { Container, Draggable } from 'vue3-smooth-dnd'
export default {
  components: {
    Container,
    Draggable
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
    onDrop(dropResult) {
      this.myWorkspaceList = dropApplyDrag(this.myWorkspaceList, dropResult).map((item, index) => {
        return { ...item, sort: index }
      })
    },
    resetDefaultName() {
      this.myWorkspaceList = this.myWorkspaceList.map((item) => {
        return { ...item, name: '' }
      })
    },
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
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.8);
}
</style>
