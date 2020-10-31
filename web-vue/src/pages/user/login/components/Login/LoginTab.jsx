import React, { useEffect } from 'react';
import { Tabs } from 'antd';
import LoginContext from './LoginContext';
const { TabPane } = Tabs;

const generateId = (() => {
  let i = 0;
  return (prefix = '') => {
    i += 1;
    return `${prefix}${i}`;
  };
})();

const LoginTab = (props) => {
  useEffect(() => {
    const uniqueId = generateId('login-tab-');
    const { tabUtil } = props;

    if (tabUtil) {
      tabUtil.addTab(uniqueId);
    }
  }, []);
  const { children } = props;
  return <TabPane {...props}>{props.active && children}</TabPane>;
};

const WrapContext = (props) => (
  <LoginContext.Consumer>
    {(value) => <LoginTab tabUtil={value.tabUtil} {...props} />}
  </LoginContext.Consumer>
); // 标志位 用来判断是不是自定义组件

WrapContext.typeName = 'LoginTab';
export default WrapContext;
