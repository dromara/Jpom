<template>
  <canvas id="bubble"></canvas>
</template>

<script>
  let bubbleStyle = 'rgba(0, 0, 0, .5)';
  let waterNum = 200;
export default {
  mounted(){
    setTimeout(() => {
      this.mounteElement();
      this.canvasBubble();
    }, 200);

  },
  methods: {
    // 气泡效果
    canvasBubble() {
      let canvas = document.getElementById("canvas");
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
      canvas.style.position = "fixed";
      let ctx = canvas.getContext("2d");
      let [width, height] = [canvas.width, canvas.height];
      let num = waterNum; // 数量
      let numArr = [];
      for (let i = 0; i < num; i++) {
        let obj = can();
        numArr.push(obj);
      }

      function run() {
        window.requestAnimationFrame(run);
        ctx.fillStyle = bubbleStyle;
        ctx.rect(0, 0, canvas.width, canvas.height);
        ctx.fill();
        numArr.forEach(({ x, y, r, s }, index) => {
          y -= s;
          if (y <= -r - 10 || Math.random() > 0.995) {
            // 使用随机数限制气泡升上
            numArr[index] = can();
          } else {
            numArr[index] = { x, y, r, s };
          }
          article(x, y, r);
        });
      }

      run();

      // 参数设置
      function can() {
        let x = Math.random() * width;
        let r = Math.random() * 5 + 5;
        let y = height + 2 * r + Math.random() * 10;
        let s = Math.random() * 1 + 1;
        return {
          x, //气泡起始水平位置
          y, //气泡起始高度
          r, //气泡半径
          s, //气泡升上速度
        };
      }

      //绘制气泡
      function article(x, y, r) {
        ctx.beginPath();
        var canvasGradient = ctx.createRadialGradient(
          x + r / 3,
          y + r / 3,
          r * 0.2,
          x + r / 3,
          y + r / 3,
          r * 0.8
        );
        canvasGradient.addColorStop(0, "#333");
        canvasGradient.addColorStop(0.6, "#ccc");
        canvasGradient.addColorStop(0.8, "#efefef");
        canvasGradient.addColorStop(1, "#fff");
        ctx.fillStyle = canvasGradient;
        ctx.globalAlpha = 0.5;
        ctx.arc(x, y, r, 0, 2 * Math.PI);
        ctx.fill();
        ctx.closePath();
      }
    },
    mounteElement(){
     let theme = document.getElementsByClassName("theme-container")[0];
     let bubble = document.getElementById("bubble");
     theme.append(bubble);
    }
  },
};
</script>

<style>
/* 气泡效果的画布元素 */
#bubble {
  position: fixed;
  top: 0;
}
</style>