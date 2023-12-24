<template>
  <div class="fantasy">
    <canvas id="cvs" class="hidden" width="1980" height="1080"></canvas>
    <canvas id="screenImage" class="hidden" width="234" height="357"></canvas>
    <canvas id="rili" class="hidden" width="600" height="600"></canvas>
    <canvas id="display"></canvas>
  </div>
</template>

<script>
export default {
  mounted() {
    // 只有一个 fantasy 元素，防止重复加载多个图片
    if (document.getElementsByClassName("fantasy").length == 1) {
      // 如果使用 IndexBigImg.vue，则去掉该组件提供的时间罩
      this.clearBannerColor();
      // 如果是在首页注册该组件，则挂载到正确的位置
      if (this.$attrs.index) {
        this.mountedElement();
      }
      this.init();
    }
  },
  methods: { 
    init() {
      var cvs = document.getElementById("cvs");
      if (!cvs) {
        return;
      }
      var ctx = cvs.getContext("2d");

      var display = document.getElementById("display");
      var displayCtx = display.getContext("2d");

      var screenImage = document.getElementById("screenImage");
      var screenImageCtx = screenImage.getContext("2d");

      var rili = document.getElementById("rili");
      var riliCtx = rili.getContext("2d");

      var songInfo = {};
      var AllTimeBak = 0;
      var NowBak = 0;
      var DrawWarning = false;
      var EnMonth = false;

      // 出处：https://blog.csdn.net/u012601195/article/details/47860617
      function drawRili() {
        riliCtx.clearRect(0, 0, 600, 600);
        var date = new Date();
        var year = date.getYear();
        var mouth = date.getMonth();
        var today = date.getDate();
        var week = date.getDay();

        var cardSize = 40;

        var array_three = [4, 6, 9, 11];
        var array_threeone = [1, 3, 5, 7, 8, 10, 12];
        var array_week = ["SUN", "MON", "TUES", "WED", "THUR", "FRI", "SAT"];

        var firstDraw; //1号绘制位置
        var wIdx = (today - 1) % 7;

        if (week >= wIdx) {
          firstDraw = week - wIdx;
        } else {
          firstDraw = week - wIdx + 7;
        }

        var dayIndex = 1;
        var countDay = 30;

        if (array_three.indexOf(mouth + 1) > -1) {
          countDay = 30;
        } else if (array_threeone.indexOf(mouth + 1) > -1) {
          countDay = 31;
        } else {
          if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0)
            countDay = 29;
          else countDay = 28;
        }

        var row = 6;
        // if (7 - firstDraw + 7 * 4 < countDay) { //确定表格行数，防止日期绘制不全
        //     row = 7;
        //}

        function drawTodaybg(i, j) {
          riliCtx.save();

          riliCtx.beginPath();
          riliCtx.strokeStyle = "#900";
          riliCtx.arc(
            45 + i * cardSize * 1.7 + cardSize / 1.18,
            50 + j * cardSize + cardSize / 2,
            cardSize / 2 - 10,
            -Math.PI,
            Math.PI * 1
          );
          riliCtx.stroke();
          riliCtx.closePath();

          riliCtx.beginPath();
          riliCtx.arc(
            45 + i * cardSize * 1.7 + cardSize / 1.18,
            50 + j * cardSize + cardSize / 2,
            cardSize / 2 - 9,
            -Math.PI,
            Math.PI * 0.9
          );
          riliCtx.stroke();
          riliCtx.closePath();

          riliCtx.beginPath();
          riliCtx.arc(
            45 + i * cardSize * 1.7 + cardSize / 1.18,
            50 + j * cardSize + cardSize / 2,
            cardSize / 2 - 8,
            -Math.PI,
            Math.PI * 0.8
          );
          riliCtx.stroke();
          riliCtx.closePath();

          riliCtx.beginPath();
          riliCtx.arc(
            45 + i * cardSize * 1.7 + cardSize / 1.18,
            50 + j * cardSize + cardSize / 2,
            cardSize / 2 - 7,
            -Math.PI,
            Math.PI * 0.7
          );
          riliCtx.stroke();
          riliCtx.closePath();

          riliCtx.beginPath();
          riliCtx.arc(
            45 + i * cardSize * 1.7 + cardSize / 1.18,
            50 + j * cardSize + cardSize / 2,
            cardSize / 2 - 6,
            -Math.PI,
            Math.PI * 0.6
          );
          riliCtx.stroke();
          riliCtx.closePath();

          riliCtx.restore();
        }

        var isNum = /^\d+(\d+)?$/;

        function drawDate(txt, i, j) {
          riliCtx.textAlign = "center";
          riliCtx.fillStyle = "rgb(69,68,84)";
          riliCtx.font = cardSize / 1.5 + "px Impact";
          var yOffest = 3;

          if ((j == 0 || j == 6) && isNum.test(txt)) {
            riliCtx.fillStyle = "#900";
          }

          riliCtx.fillText(
            txt.toString(),
            45 + j * cardSize * 1.7 + cardSize / 1.18,
            50 + i * cardSize + (cardSize / 3) * 2 + yOffest
          );

          if (txt == today) {
            drawTodaybg(j, i);
          }
        }

        riliCtx.fillStyle = "rgb(69,68,84)";
        riliCtx.font = "900 26pt SimHei";
        riliCtx.textAlign = "center";
        var monthCN = [
          "一",
          "二",
          "三",
          "四",
          "五",
          "六",
          "七",
          "八",
          "九",
          "十",
          "十一",
          "十二",
        ];
        var monthEN = [
          " January",
          "February",
          "  March",
          "  April",
          "   May",
          "  June",
          "  July",
          " August",
          "September",
          " October",
          "November",
          " December",
        ];

        if (EnMonth) {
          riliCtx.scale(1.1, 1);
          riliCtx.fillText(monthEN[mouth], 245, 32);
          riliCtx.resetTransform();
        } else {
          riliCtx.scale(1.1, 1);
          riliCtx.fillText(monthCN[mouth] + "月", 260, 32);
          riliCtx.resetTransform();

          riliCtx.font = "20pt SimHei";
          riliCtx.textAlign = "end";
          riliCtx.fillText(today + "日", 520, 38);
        }

        for (var i = 0; i < row; i++) {
          for (var j = 0; j < 7; j++) {
            riliCtx.strokeRect(
              45 + j * cardSize * 1.7,
              50 + i * cardSize,
              cardSize * 1.7,
              cardSize
            );
          }
        }

        dayIndex = 1;

        for (var i = 0; i < row; i++) {
          //开始绘制日期数
          for (var j = 0; j < 7; j++) {
            if (i == 0) {
              //表格第一行绘制星期
              drawDate(array_week[j], i, j);
              continue;
            }

            if (i == 1 && j < firstDraw) {
              //确定1号绘制位置
              continue;
            }

            if (dayIndex > countDay) {
              //只绘制月份的天数
              break;
            }

            drawDate(dayIndex++, i, j);
          }
        }
      }

      var riliInterval = setInterval(drawRili, 3600000);
      drawRili();

      // Canvas奇妙的剪切蒙版实现
      var screenMask = new Image();
      screenMask.src = "/fantasy/Screenmask.png";

      var screen = new Image();
      screen.src = "/fantasy/screen.png";

      var iv = setInterval(() => {
        if (screen.complete && screenMask.complete) {
          // 可以切换图片的位置，也可以换成自己的图片
          screenImageCtx.drawImage(screen, -300, -50, 1280, 720);
          screenImageCtx.globalCompositeOperation = "destination-atop";
          screenImageCtx.drawImage(screenMask, 0, 0);
          screenImageCtx.globalCompositeOperation = "source-over";
          clearInterval(iv);
        }
      }, 14);

      // 奇妙的屏幕大小自适应
      window.onresize = function () {
        display.width = window.innerWidth;
        if (window.innerWidth / window.innerHeight > 1.8333333333333) {
          display.height = (window.innerWidth / 1980) * 1080;
          // window.scrollTo(0, (window.innerHeight - 123) / 16);
        } else {
          display.height = window.innerHeight;
        }
      };

      window.onresize();

      // 加载图片
      var bg = new Image();
      bg.src = "/fantasy/bg.png";

      var mask = new Image();
      mask.src = "/fantasy/mask.png";

      var light = new Image();
      light.src = "/fantasy/light.png";

      var caidai = new Image();
      caidai.src = "/fantasy/caidai.png";

      var two = new Image();
      two.src = "/fantasy/22.png";

      var screenLight = new Image();
      screenLight.src = "/fantasy/screenLight.png";

      var phoneLight = new Image();
      phoneLight.src = "/fantasy/phoneLight.png";

      var phoneText = JSON.parse(
        '[{"time":0,"text":"凌晨啦!"},{"time":6,"text":"早上好!"},{"time":8,"text":"上午好!"},{"time":11,"text":"你吃了吗"},{"time":13,"text":"下午好鸭!"},{"time":16,"text":"傍晚咯!"},{"time":19,"text":"晚安!"}]'
      );

      var noRili = false;
      var updateSongInfoHandler = -1;

      var data = new Array(128);
      var animData = new Array(128);
      var SoundPlaying = false;

      // 奇妙的初始化
      for (var i = 0; i < 128; i++) {
        data[i] = animData[i] = 0;
      }

      // 奇妙的Normalize
      var peakValue = 1;
      if (window.wallpaperRegisterAudioListener) {
        window.wallpaperRegisterAudioListener(function (audioData) {
          var max = 0;

          for (var i = 0; i < 128; i++) {
            if (audioData[i] > max) max = audioData[i];
          }

          peakValue = peakValue * 0.99 + max * 0.01;

          for (i = 0; i < 64; i++) {
            data[63 - i] = audioData[i] / peakValue;
          }

          for (i = 0; i < 64; i++) {
            data[127 - i] = audioData[127 - i];
          }
        });
      } else {
        var iva;
        let audio = document.getElementsByClassName("aplayer-button")[0];
        if (audio) {
          audio.onclick = () => {
            let play = document.getElementsByClassName("aplayer-play")[0];
            if (play) {
              iva = setInterval(() => {
                for (i = 0; i < 64; i++) {
                  peakValue = peakValue * 0.99 + 1 * 0.01;
                  data[63 - i] =
                    ((Math.random() * 0.4) / peakValue) * Math.random();
                }
                for (i = 0; i < 64; i++) {
                  data[127 - i] = Math.random() * 0.2 * Math.random();
                }
                // for (var i = 0; i < 128; i++) {
                //     data[i] = Math.random();
                // }
              }, 130);
            } else {
              clearInterval(iva);
              for (var i = 0; i < 128; i++) {
                data[i] = animData[i] = 0;
              }
            }
          };
        }
      }

      // ....
      function min(a, b) {
        return a > b ? b : a;
      }

      function max(a, b) {
        return a > b ? a : b;
      }

      // 奇妙的颜色变化
      var targetColor = { r: 80, g: 120, b: 169 };
      var currentColor = { r: 80, g: 120, b: 169 };
      var lightColor = { r: 0, g: 34, b: 77, a: 0 };

      function colorToRgb(color) {
        return (
          "rgb(" +
          color.r.toString() +
          "," +
          color.g.toString() +
          "," +
          color.b.toString() +
          ")"
        );
      }

      function colorToRgba(colorWithA) {
        return (
          "rgba(" +
          colorWithA.r.toString() +
          "," +
          colorWithA.g.toString() +
          "," +
          colorWithA.b.toString() +
          "," +
          colorWithA.a.toString() +
          ")"
        );
      }

      var night = false;
      var debug = false;

      // Canvas的奇妙冒险!
      function render() {
        for (var i = 0; i < 128; i++) {
          animData[i] += (data[i] - animData[i]) * 0.3;
          animData[i] = min(animData[i], 1);
        }

        currentColor.r += (targetColor.r - currentColor.r) * 0.01;
        currentColor.r = min(currentColor.r, 255);
        currentColor.r = max(currentColor.r, 0);

        currentColor.g += (targetColor.g - currentColor.g) * 0.01;
        currentColor.g = min(currentColor.g, 255);
        currentColor.g = max(currentColor.g, 0);

        currentColor.b += (targetColor.b - currentColor.b) * 0.01;
        currentColor.b = min(currentColor.b, 255);
        currentColor.b = max(currentColor.b, 0);

        ctx.clearRect(0, 0, 1980, 1080);
        ctx.drawImage(bg, 0, 0);
        ctx.drawImage(mask, 954, 99);

        ctx.fillStyle = "#97adbb"; // 时间的颜色
        ctx.font = "32pt Impact";

        ctx.transform(1, 2.05 * (Math.PI / 180), 0, 1, 0, 0);

        var time = new Date();
        ctx.fillText(
          (time.getHours() < 10 ? "0" : "") +
            time.getHours().toString() +
            ":" +
            (time.getMinutes() < 10 ? "0" : "") +
            time.getMinutes() +
            ":" +
            (time.getSeconds() < 10 ? "0" : "") +
            time.getSeconds().toString(),
          725,
          318
        );
        ctx.resetTransform();

        // 日历本
        ctx.transform(0.9645, 0, 0 * (Math.PI / 180), 0.96, 967, 100);
        ctx.rotate(6 * (Math.PI / 180));

        if (!noRili) {
          ctx.drawImage(rili, 0, 0);

          ctx.resetTransform();

          ctx.transform(0.9645, 0, 9 * (Math.PI / 180), 1, 825, 160);
          ctx.rotate(7 * (Math.PI / 180));
        }

        targetColor = { r: 80, g: 120, b: 169 };

        if (night) {
          targetColor = { r: 255, g: 75, b: 80 };
        }

        if (!noRili) {
          ctx.fillStyle = "rgba(0,0,0,0.5)";
          ctx.fillRect(-10, 320, 650, 2);
        }

        ctx.fillStyle = colorToRgb(currentColor);

        if (!noRili) {
          for (var i = 32; i < 95; i++)
            ctx.fillRect(
              10 * (i - 32),
              20 + (300 - 300 * animData[i]),
              4,
              300 * animData[i]
            );
        } else
          for (var i = 32; i < 95; i++)
            ctx.fillRect(
              40 + 7.5 * (i - 32),
              30 + (300 - 300 * animData[i]),
              4,
              300 * animData[i]
            );

        ctx.resetTransform();

        ctx.globalCompositeOperation = "overlay";
        ctx.drawImage(light, 971, 197);
        ctx.globalCompositeOperation = "source-over";

        ctx.drawImage(caidai, 949, 25);
        ctx.drawImage(two, 1319, 345);

        // 夜间光照
        if (night && lightColor.a < 0.7) {
          lightColor.a += 0.005;
          lightColor.a = min(lightColor.a, 0.7);
        } else if (!night) {
          lightColor.a -= 0.005;
          lightColor.a = max(lightColor.a, 0.0);
        }

        if (lightColor.a > 0) {
          ctx.globalCompositeOperation = "hard-light";
          ctx.fillStyle = colorToRgba(lightColor);
          ctx.fillRect(0, 0, 1980, 1080);
          ctx.globalCompositeOperation = "source-over";

          ctx.globalAlpha = lightColor.a / 0.7;
          ctx.drawImage(phoneLight, 860, 437);
          ctx.globalAlpha = 1;
        }

        // 屏幕
        ctx.drawImage(screenImage, 0, 0);
        if (lightColor.a > 0) {
          ctx.globalAlpha = lightColor.a / 0.7;
          ctx.drawImage(screenLight, 0, 0);
          ctx.globalAlpha = 1;
        }

        night = true;
        var greeting = "凌晨啦!";

        phoneText.forEach((v) => {
          if (time.getHours() >= v.time) {
            greeting = v.text;
          }
        });

        if (time.getHours() >= 6 && time.getHours() <= 18) {
          night = false;
        }

        night = debug ? !night : night;

        // 手机
        ctx.fillStyle = "#000";
        ctx.font = "31.02pt SimHei";

        ctx.transform(
          1.0911,
          -35 * (Math.PI / 180),
          0,
          0.5868,
          1132.94,
          564.07
        );
        ctx.rotate(56.5 * (Math.PI / 180));
        ctx.textAlign = "center";
        ctx.fillStyle = "#fff";
        ctx.fillText(greeting, 135, 100);
        ctx.textAlign = "start";
        ctx.resetTransform();

        displayCtx.drawImage(cvs, 0, 0, display.width, display.height);
        window.requestAnimationFrame(render);
      }

      window.requestAnimationFrame(render);
    },
    // 针对首页挂载元素
    mountedElement() {
      let fantasy = document.getElementsByClassName("fantasy")[0];
      let banner = document.getElementsByClassName("banner")[0];
      // 去掉黑色栅格背景
      banner.style.background = "";
      fantasy && banner && banner.appendChild(fantasy);
    },
    clearBannerColor() {
      let bannerColor = document.getElementsByClassName("banner-color")[0];
      if (bannerColor) {
        bannerColor.parentNode.removeChild(bannerColor);
      }
    },
  },
};
</script>

<style>
.fantasy {
  position: fixed;
  top: 0;
  height: 100vh;
  width: 100%;
  z-index: -9;
}
.hidden {
  display: none;
}
#display {
  margin: auto;
}
/* 图片大小 */
.vdoing-index-class .home-wrapper .banner {
  margin-top: 0 !important;
  height: 100vh;
  background-attachment: fixed !important;
}
/* 图片中间的签名和标题位置 */
.banner-conent {
  margin-top: 23vh !important;
}
</style>
