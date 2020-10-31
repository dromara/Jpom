import { Tabs, Form } from 'antd';
import React, { useState } from 'react';
import useMergeValue from 'use-merge-value';
import classNames from 'classnames';
import LoginContext from './LoginContext';
import LoginItem from './LoginItem';
import LoginSubmit from './LoginSubmit';
import LoginTab from './LoginTab';
import styles from './index.less';

const Login = (props) => {
  const { className } = props;
  const [tabs, setTabs] = useState([]);
  const [active, setActive] = useState({});
  const [type, setType] = useMergeValue('', {
    value: props.activeKey,
    onChange: props.onTabChange,
  });
  const TabChildren = [];
  const otherChildren = [];
  React.Children.forEach(props.children, (child) => {
    if (!child) {
      return;
    }

    if (child.type.typeName === 'LoginTab') {
      TabChildren.push(child);
    } else {
      otherChildren.push(child);
    }
  });
  return (
    <LoginContext.Provider
      value={{
        tabUtil: {
          addTab: (id) => {
            setTabs([...tabs, id]);
          },
          removeTab: (id) => {
            setTabs(tabs.filter((currentId) => currentId !== id));
          },
        },
        updateActive: (activeItem) => {
          if (!active) return;

          if (active[type]) {
            active[type].push(activeItem);
          } else {
            active[type] = [activeItem];
          }

          setActive(active);
        },
      }}
    >
      <div className={classNames(className, styles.login)}>
        <Form
          form={props.from}
          onFinish={(values) => {
            if (props.onSubmit) {
              props.onSubmit(values);
            }
          }}
        >
          {tabs.length ? (
            <React.Fragment>
              <Tabs
                destroyInactiveTabPane
                animated={false}
                className={styles.tabs}
                activeKey={type}
                onChange={(activeKey) => {
                  setType(activeKey);
                }}
              >
                {TabChildren}
              </Tabs>
              {otherChildren}
            </React.Fragment>
          ) : (
            props.children
          )}
        </Form>
      </div>
    </LoginContext.Provider>
  );
};

Login.Tab = LoginTab;
Login.Submit = LoginSubmit;
Login.UserName = LoginItem.UserName;
Login.Password = LoginItem.Password;
Login.Mobile = LoginItem.Mobile;
Login.Captcha = LoginItem.Captcha;
export default Login;
