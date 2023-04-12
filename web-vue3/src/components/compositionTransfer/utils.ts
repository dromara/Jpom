/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/**
 * 深拷贝
 * @param data
 */
export function cloneDeep(data: any) {
  return JSON.parse(JSON.stringify(data))
}

/**
 * 树转数组
 * @param tree
 * @param hasChildren
 */
export function treeToList(tree: any[], hasChildren = false) {
  let queen: any[] = []
  const out = []
  queen = queen.concat(JSON.parse(JSON.stringify(tree)))
  while (queen.length) {
    const first = queen.shift()
    if (first?.children) {
      queen = queen.concat(first.children)
      if (!hasChildren) delete first.children
    }
    out.push(first)
  }
  return out
}

/**
 * 数组转树
 * @param list
 * @param tree
 * @param parentId
 * @param key
 */
export function listToTree(list: any, tree: any, parentId = 0, key = 'parentId') {
  list.forEach((item: any) => {
    if (item[key] === parentId) {
      const child = {
        ...item,
        children: []
      }
      listToTree(list, child.children, item.key, key)
      if (!child.children?.length) delete child.children
      tree.push(child)
    }
  })
  return tree
}

/**
 * 获取树节点 key 列表
 * @param treeData
 */
export function getTreeKeys(treeData: any) {
  const list = treeToList(treeData)
  return list.map((item) => item.key)
}

// /**
//  * 循环遍历出最深层子节点，存放在一个数组中
//  * @param deepList
//  * @param treeData
//  */
// export function getDeepList(deepList, treeData) {
//   treeData?.forEach((item) => {
//     if (item?.children?.length) {
//       getDeepList(deepList, item.children);
//     } else {
//       deepList.push(item.key);
//     }
//   });
//   return deepList;
// }

// /**
//  * 将后台返回的含有父节点的数组和第一步骤遍历的数组做比较,如果有相同值，将相同值取出来，push到一个新数组中
//  * @param uniqueArr
//  * @param arr
//  */
// export function uniqueTree(uniqueArr, arr) {
//   const uniqueChild = [];
//   for (const i in arr) {
//     for (const k in uniqueArr) {
//       if (uniqueArr[k] === arr[i]) {
//         uniqueChild.push(uniqueArr[k]);
//       }
//     }
//   }
//   return uniqueChild;
// }

/**
 * 是否选中
 * @param selectedKeys
 * @param eventKey
 */
export function isChecked(selectedKeys: any[], eventKey: any) {
  return selectedKeys.indexOf(eventKey) !== -1
}

/**
 * 处理左侧树数据
 * @param data
 * @param targetKeys
 * @param direction
 */
export function handleLeftTreeData(data: any, targetKeys: any, direction = 'right') {
  data.forEach((item: any) => {
    if (direction === 'right') {
      item.disabled = targetKeys.includes(item.key)
    } else if (direction === 'left') {
      if (item.disabled && targetKeys.includes(item.key)) item.disabled = false
    }
    if (item.children) handleLeftTreeData(item.children, targetKeys, direction)
  })
  return data
}

/**
 * 处理右侧树数据
 * @param data
 * @param targetKeys
 * @param direction
 */
export function handleRightTreeData(data: any, targetKeys: any, direction = 'right') {
  const list = treeToList(data)
  const arr: any[] = []
  const tree: any[] = []
  list.forEach((item) => {
    if (direction === 'right') {
      if (targetKeys.includes(item.key)) {
        const content = { ...item }
        if (content.children) delete content.children
        arr.push({ ...content })
      }
    } else if (direction === 'left') {
      if (!targetKeys.includes(item.key)) {
        const content = { ...item }
        if (content.children) delete content.children
        arr.push({ ...content })
      }
    }
  })
  listToTree(arr, tree, 0)
  return tree
}

/**
 * 树数据展平
 * @param list
 * @param dataSource
 */
export function flatten(list: any, dataSource: any) {
  list.forEach((item: any) => {
    dataSource.push(item)
    if (item.children) flatten(item.children, dataSource)
  })
  return dataSource
}
