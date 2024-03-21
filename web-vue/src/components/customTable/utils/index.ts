///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

/**
 * 判断两个数组包含元素是否一致，but不区分顺序
 * @param arr1
 * @param arr2
 */
export const compareArrays = (arr1: string[], arr2: string[]) => {
  if (arr1.length !== arr2.length) {
    return false
  }
  const matches = []
  for (let i = 0; i < arr1.length; i++) {
    let found = false
    for (let j = 0; j < arr2.length; j++) {
      if (arr1[i] === arr2[j]) {
        matches.push(j)
        found = true
        break
      }
    }
    if (!found) {
      return false
    }
  }
  return true
}
