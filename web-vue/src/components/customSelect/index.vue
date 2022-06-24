<template>
  <div @mousedown="setSelectOpen(true)">
    <Select
      :getPopupContainer="
        (triggerNode) => {
          return triggerNode.parentNode || document.body;
        }
      "
      v-model="selected"
      :style="selStyle"
      :open="selectOpen"
      @blur="setSelectOpen(false)"
      showSearch
      @focus="setSelectOpen(true)"
      @change="selectChange"
      :placeholder="selectPlaceholder"
    >
      <a-icon slot="suffixIcon" v-if="suffixIcon" :type="suffixIcon" @click="refreshSelect" />
      <template v-if="$slots.suffixIcon && !suffixIcon" slot="suffixIcon">
        <slot name="suffixIcon"></slot>
      </template>
      <div slot="dropdownRender" slot-scope="menu">
        <div style="padding: 8px 8px; cursor: pointer; display: flex" @mousedown="(e) => e.preventDefault()">
          <a-input-search
            enter-button="添加"
            v-model="selectInput"
            :maxLength="maxLength"
            @search="onSearch"
            @blur="visibleInput(false)"
            @focus="visibleInput(true)"
            @click="(e) => e.target.focus()"
            :placeholder="inputPlaceholder"
            size="small"
          >
            <a-tooltip slot="suffix" v-if="$slots.inputTips">
              <template slot="title">
                <slot name="inputTips"></slot>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </a-input-search>
        </div>
        <a-divider style="margin: 4px 0" />
        <v-nodes :vnodes="menu" />
      </div>
      <a-select-option v-if="selectPlaceholder" value="">{{ selectPlaceholder }}</a-select-option>
      <a-select-option v-for="item in optionList" :key="item">{{ item }} </a-select-option>
    </Select>
  </div>
</template>

<script>
import {Select} from "ant-design-vue";

export default {
  components: {
    Select,
    VNodes: {
      functional: true,
      render: (h, ctx) => ctx.props.vnodes,
    },
  },

  data() {
    return {
      selectInput: "",
      selectOpen: false,
      selectFocus: false,
      inputFocus: false,
      optionList: [],
      selected: "",
    };
  },
  props: {
    // 继承原组件所有props
    ...Select.props,
    data: {
      type: Array,
      default: () => [],
    },
    inputPlaceholder: {
      type: String,
      default: "请输入...",
    },
    selectPlaceholder: {
      type: String,
      default: "请选择",
    },
    selStyle: { type: String, default: "" },
    suffixIcon: {
      type: String,
      default: "reload",
    },
    maxLength: {
      type: Number,
      default: 200,
    },
  },
  watch: {
    value: {
      handler(v) {
        this.selected = v;
      },
      immediate: true,
    },
    data: {
      handler(v) {
        this.optionList = v;
      },
      deep: true,
      immediate: true,
    },
  },

  methods: {
    refreshSelect() {
      this.$emit("onRefreshSelect");
    },
    selectChange(v) {
      this.$emit("input", v);
      this.selectOpen = false;
      this.$emit("change", v);
    },
    onSearch(v) {
      if (!v) {
        return;
      }
      let index = this.optionList.indexOf(v);
      if (index === -1) {
        this.optionList = [...this.optionList, v];
      }
      this.selectInput = "";
      this.selected = v;
      //
      this.selectChange(v);
      this.$emit("addOption", this.optionList);
    },
    setSelectOpen(v) {
      this.selectFocus = v;
      if (this.inputFocus || this.selectFocus) {
        this.selectOpen = true;
        return;
      }
      this.selectOpen = false;
    },
    visibleInput(v) {
      this.inputFocus = v;
      if (this.inputFocus || this.selectFocus) {
        this.selectOpen = true;
        return;
      }
      this.selectOpen = false;
    },
  },
};
</script>
