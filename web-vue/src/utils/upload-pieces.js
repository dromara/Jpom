import sha1 from "js-sha1";
import { concurrentExecution } from "@/utils/const";
import { generateShardingId } from "@/api/common";
import Vue from "vue";

const uploadFileSliceSize = window.uploadFileSliceSize === "<uploadFileSliceSize>" ? 1 : window.uploadFileSliceSize;
const uploadFileConcurrent = window.uploadFileConcurrent === "<uploadFileConcurrent>" ? 1 : window.uploadFileConcurrent;
/**
 * 文件分片上传
 * @params file {File} 文件
 * @params pieceSize {Number} 分片大小 默认3MB
 * @params concurrent {Number} 并发数量 默认2
 * @params process {Function} 进度回调函数
 * @params success {Function} 成功回调函数
 * @params error {Function} 失败回调函数
 */
export const uploadPieces = ({ file, uploadCallback, success, process, error }) => {
  // 如果文件传入为空直接 return 返回
  if (!file || file.length < 1) {
    return error("文件不能为空");
  }
  if (!window.FileReader) {
    return error("您的浏览器版本太低，不支持该功能");
  }
  let fileSh1 = ""; //
  let sliceId = "";
  const chunkSize = uploadFileSliceSize * 1024 * 1024; // 1MB一片
  const chunkCount = Math.ceil(file.size / chunkSize); // 总片数
  const chunkList = []; // 分片列表
  const uploaded = []; // 已经上传的
  let total = 0;

  /***
   * 获取md5
   **/
  const readFileSh1 = () => {
    //
    Vue.prototype.$setLoading({
      spinning: true,
      tip: "解析文件,准备上传中",
    });
    const reader = new FileReader();
    const sha1Hash = sha1.create();
    let start = 0;
    total = file.size;
    // 默认 2M 解析缓存
    const batch = 1024 * 1024 * 2;
    const asyncUpdate = function () {
      if (start < total) {
        Vue.prototype.$setLoading({
          spinning: true,
          tip: "解析文件,准备上传中 " + ((start / total) * 100).toFixed(2) + "%",
        });
        let end = Math.min(start + batch, total);
        reader.readAsArrayBuffer(file.slice(start, end));
        start = end;
      } else {
        // 解析结束
        fileSh1 = sha1Hash.hex();
        for (let i = 0; i < chunkCount; i++) {
          chunkList.push(Number(i));
        }
        Vue.prototype.$setLoading("closeAll");
        //生成分片 id
        generateShardingId().then((res) => {
          if (res.code === 200) {
            sliceId = res.data;
            concurrentUpload();
          }
        });
      }
    };
    reader.onload = function (event) {
      try {
        sha1Hash.update(event.target.result);
        asyncUpdate();
      } catch (e) {
        Vue.prototype.$setLoading("closeAll");
        this.$notification.error({
          message: "解析文件失败：" + e,
        });
        error("解析文件失败：" + e);
      }
    };
    asyncUpdate();
  };
  /***
   * 获取每一个分片的详情
   **/
  const getChunkInfo = (file, currentChunk, chunkSize) => {
    let start = currentChunk * chunkSize;
    let end = Math.min(file.size, start + chunkSize);
    let chunk = file.slice(start, end);
    return {
      start,
      end,
      chunk,
    };
  };
  /***
   * 并发上传
   **/
  const concurrentUpload = () => {
    const startTime = new Date().getTime();
    concurrentExecution(chunkList, uploadFileConcurrent, (curItem) => {
      return new Promise((resolve, reject) => {
        const { chunk, end } = getChunkInfo(file, curItem, chunkSize);
        const chunkInfo = {
          chunk,
          currentChunk: curItem,
          chunkCount,
        };

        // 构建上传文件的formData
        const uploadData = createUploadData(chunkInfo);

        uploadCallback(uploadData)
          .then(() => {
            uploaded.push(chunkInfo.currentChunk + 1);
            const sd = parseInt((uploaded.length / chunkInfo.chunkCount) * 100);
            // console.log(chunk);
            process(sd, end, total, new Date().getTime() - startTime);
            //
            /***
             * 创建文件上传参数
             **/
            const createUploadData2 = {
              nowSlice: chunkInfo.currentChunk + 1,
              totalSlice: chunkCount,
              sliceId: sliceId,
              fileSumSha1: fileSh1,
            };
            resolve(createUploadData2);
          })
          .catch(() => {
            reject();
          });
      });
    }).then((uploadData) => {
      success(uploadData, file.name);
      //   console.log("finish", res);
    });
  };

  /***
   * 创建文件上传参数
   **/
  const createUploadData = (chunkInfo) => {
    let fetchForm = new FormData();

    fetchForm.append("nowSlice", chunkInfo.currentChunk + 1);
    fetchForm.append("totalSlice", chunkCount);
    fetchForm.append("sliceId", sliceId);
    const chunkfile = new File([chunkInfo.chunk], file.name);
    fetchForm.append("file", chunkfile); // fetchForm.append('file', chunkInfo.chunk)
    fetchForm.append("fileSumSha1", fileSh1);

    return fetchForm;
  };

  readFileSh1(); // 开始执行代码
};
