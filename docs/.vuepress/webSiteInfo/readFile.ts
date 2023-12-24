import fs from 'fs'; // 文件模块
import path from 'path'; // 路径模块
import matter from 'gray-matter'; // FrontMatter解析器 https://github.com/jonschlinkert/gray-matter
import chalk from 'chalk' // 命令行打印美化
const log = console.log
const docsRoot = path.join(__dirname, '..', '..', '..', 'docs'); // docs文件路径

/**
 * 获取本站的文章数据
 * 获取所有的 md 文档，可以排除指定目录下的文档
 */
function readFileList(excludeFiles: Array<string> = [''], dir: string = docsRoot, filesList: Array<Object> = []) {
  const files = fs.readdirSync(dir);
  files.forEach((item, index) => {
    let filePath = path.join(dir, item);
    const stat = fs.statSync(filePath);
    if (!(excludeFiles instanceof Array)) {
      log(chalk.yellow(`error: 传入的参数不是一个数组。`))
    }
    excludeFiles.forEach((excludeFile) => {
      if (stat.isDirectory() && item !== '.vuepress' && item !== '@pages' && item !== excludeFile) {
        readFileList(excludeFiles, path.join(dir, item), filesList);  //递归读取文件
      } else {
        if (path.basename(dir) !== 'docs') { // 过滤 docs目录级下的文件

          const fileNameArr = path.basename(filePath).split('.')
          let name = null, type = null;
          if (fileNameArr.length === 2) { // 没有序号的文件
            name = fileNameArr[0]
            type = fileNameArr[1]
          } else if (fileNameArr.length === 3) { // 有序号的文件
            name = fileNameArr[1]
            type = fileNameArr[2]
          } else { // 超过两个‘.’的
            log(chalk.yellow(`warning: 该文件 "${filePath}" 没有按照约定命名，将忽略生成相应数据。`))
            return
          }
          if (type === 'md') { // 过滤非 md 文件
            filesList.push({
              name,
              filePath
            });
          }
        }
      }
    });
  });
  return filesList;
}
/**
 * 获取本站的文章总字数
 * 可以排除某个目录下的 md 文档字数
 */
function readTotalFileWords(excludeFiles = ['']) {
  const filesList = readFileList(excludeFiles);
  let wordCount = 0;
  filesList.forEach((item: any) => {
    const content = getContent(item.filePath);
    let len = counter(content);
    wordCount += len[0] + len[1];
  });
  if (wordCount < 1000) {
    return wordCount;
  }
  return Math.round(wordCount / 100) / 10 + 'k';
}
/**
 * 获取每一个文章的字数
 * 可以排除某个目录下的 md 文档字数
 */
function readEachFileWords(excludeFiles: Array<string> = [''], cn: number, en: number) {
  const filesListWords = [];
  const filesList = readFileList(excludeFiles);
  filesList.forEach((item: any) => {
    const content = getContent(item.filePath);
    let len = counter(content);
    // 计算预计的阅读时间
    let readingTime = readTime(len, cn, en);
    let wordsCount: any = 0;
    wordsCount = len[0] + len[1];
    if (wordsCount >= 1000) {
      wordsCount = Math.round(wordsCount / 100) / 10 + 'k';
    }
    // fileMatterObj => {content:'剔除frontmatter后的文件内容字符串', data:{<frontmatter对象>}, ...}
    const fileMatterObj = matter(content, {});
    const matterData = fileMatterObj.data;
    filesListWords.push({ ...item, wordsCount, readingTime, ...matterData });
  });
  return filesListWords;
}

/**
 * 计算预计的阅读时间
 */
function readTime(len: Array<number>, cn: number = 300, en: number = 160) {
  let readingTime = len[0] / cn + len[1] / en;
  if (readingTime > 60 && readingTime < 60 * 24) {   // 大于一个小时，小于一天
    let hour = Math.trunc(readingTime / 60);
    let minute = Math.trunc(readingTime - hour * 60);
    if (minute === 0) {
      return hour + 'h';
    }
    return hour + 'h' + minute + 'm';
  } else if (readingTime > 60 * 24) {      // 大于一天
    let day = Math.trunc(readingTime / (60 * 24));
    let hour = Math.trunc((readingTime - day * 24 * 60) / 60);
    if (hour === 0) {
      return day + 'd';
    }
    return day + 'd' + hour + 'h';
  }
  return readingTime < 1 ? '1' : Math.trunc(readingTime * 10) / 10 + 'm';   // 取一位小数
}

/**
 * 读取文件内容
 */
function getContent(filePath: string) {
  return fs.readFileSync(filePath, 'utf8');
}
/**
 * 获取文件内容的字数
 * cn：中文
 * en：一整句英文（没有空格隔开的英文为 1 个）
 */
function counter(content: string) {
  const cn = (content.match(/[\u4E00-\u9FA5]/g) || []).length;
  const en = (content.replace(/[\u4E00-\u9FA5]/g, '').match(/[a-zA-Z0-9_\u0392-\u03c9\u0400-\u04FF]+|[\u4E00-\u9FFF\u3400-\u4dbf\uf900-\ufaff\u3040-\u309f\uac00-\ud7af\u0400-\u04FF]+|[\u00E4\u00C4\u00E5\u00C5\u00F6\u00D6]+|\w+/g) || []).length;
  return [cn, en];
}

export {
  readFileList,
  readTotalFileWords,
  readEachFileWords,
}
