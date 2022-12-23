<template></template>
<script>
import "element-ui/lib/theme-chalk/index.css";
import {Notification, MessageBox} from "element-ui";

export default {
  name: "Notice",
  data() {
    return {
      notifyPromise: Promise.resolve(),
      //使用messageId作为弹窗的key，用来获取弹窗的实例，以对对应弹窗进行操作
      notifications: {},
      destroying: true
    };
  },
  props: {
    initTop: {
      type: Number,
      default: 55,
    },
    data: {
      type: Array,
      default: [
        {
          id: null,
          title: "最新公告",
          content: "演示示例",
          isHtmlContent: false,
        },
      ],
    },
  },
  mounted() {
    if (window.innerWidth < 720) {
      return
    }
    this.destroying = false;
    this.data
      .filter(item => item.id != null && !this.isIgnoreNotice(item.id))
      .filter(item => !(window["isShowedNotify_" + item.id] || false))
      .forEach((element) => {
        this.notifyPromise = this.notifyPromise.then(() => {
          const notify = Notification({
            title: element.title,
            message: element.content,
            position: element.position || 'top-right',
            dangerouslyUseHTMLString: element.isHtmlContent || false,
            duration: 0,
            offset: this.initTop,
            onClose: () => {
              if (this.destroying) {
                return
              }
              this.isShowAgain(element.id);
            },
          });
          window["isShowedNotify_" + element.id] = true;
          //将messageId和通知实例放入字典中
          this.notifications[element.id] = notify;
        });
      });
  },
  beforeDestroy() {
    this.destroying = true;
    for (let key in this.notifications) {
      this.notifications[key].close();
      delete this.notifications[key];
      delete window["isShowedNotify_" + key];
    }
  },
  methods: {
    saveIgnoreNotice(id) {
      const json = JSON.parse(localStorage.getItem("ignoreNotice") || "[]");
      json.push(id);
      localStorage.setItem("ignoreNotice", JSON.stringify(json));
    },
    isIgnoreNotice(id) {
      const json = JSON.parse(localStorage.getItem("ignoreNotice") || "[]");
      return json.indexOf(id) >= 0;
    },
    isShowAgain(id) {
      MessageBox.confirm("是否下次不再提示该公告？", "忽略公告", {
        confirmButtonText: "不再提示",
        cancelButtonText: "否",
        confirmButtonClass: "confirmButtonClass",
        cancelButtonClass: "cancelButtonClass",
        type: "info",
      }).then(() => {
        this.saveIgnoreNotice(id);
      });
    },
  },
};
</script>

<style>
.el-notification {
  background-color: var(--bodyBg);
  border-color: var(--borderColor);
}

.el-notification__title {
  border-bottom: none;
  color: var(--textColor);
}

.el-notification__content {
  color: var(--textColor);
}

.el-message-box {
  background-color: var(--bodyBg);
  border-color: var(--borderColor);
}

.el-message-box__title {
  color: var(--textColor);
}

.el-message-box__content {
  color: var(--textColor);
}

.confirmButtonClass {
  color: #FFF;
  background-color: #4274F4;
  border-color: #4274F4;
}

.confirmButtonClass:hover, .confirmButtonClass:focus {
  color: #FFF;
  background-color: #4274F4;
  border-color: #4274F4;
}

.cancelButtonClass {
  color: var(--textColor);
  background-color: var(--bodyBg);
  border-color: var(--borderColor);
}

.cancelButtonClass:hover, .cancelButtonClass:focus {
  color: var(--textColor);
  background-color: var(--bodyBg);
  border-color: var(--borderColor);
}
</style>
