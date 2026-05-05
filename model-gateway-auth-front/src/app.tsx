import { LogoutOutlined } from '@ant-design/icons';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import type { RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import { history } from '@umijs/max';
import { App, Avatar, Dropdown, message } from 'antd';
import React from 'react';
import defaultSettings from '../config/defaultSettings';
import { logout as logoutApi } from '@/services/auth';

const TOKEN_KEY = 'access_token';
const USER_KEY = 'user_info';
const LOGIN_PATH = '/user/login';

/**
 * 启动时拉初始登录态：从 localStorage 恢复 currentUser。
 */
export async function getInitialState(): Promise<{
  currentUser?: API.UserInfo;
  settings?: Partial<LayoutSettings>;
}> {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!token) {
    return { settings: defaultSettings as Partial<LayoutSettings> };
  }
  try {
    const userInfo = JSON.parse(localStorage.getItem(USER_KEY) || 'null');
    return {
      currentUser: userInfo ?? undefined,
      settings: defaultSettings as Partial<LayoutSettings>,
    };
  } catch {
    return { settings: defaultSettings as Partial<LayoutSettings> };
  }
}

/**
 * Pro Layout 运行时配置。
 */
export const layout: RunTimeLayoutConfig = ({ initialState, setInitialState }) => {
  const handleLogout = async () => {
    try {
      await logoutApi();
    } catch {
      // 忽略，照样清登录态
    }
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setInitialState((s) => ({ ...s, currentUser: undefined }));
    history.replace(`${LOGIN_PATH}?redirect=${encodeURIComponent(history.location.pathname)}`);
  };

  return {
    ...(initialState?.settings ?? {}),
    title: 'Model Gateway Auth',
    locale: false,
    avatarProps: {
      title: initialState?.currentUser?.nickname || initialState?.currentUser?.username || '未登录',
      render: (_props, dom) => {
        if (!initialState?.currentUser) return dom;
        return (
          <Dropdown
            menu={{
              items: [
                { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: handleLogout },
              ],
            }}
          >
            {dom}
          </Dropdown>
        );
      },
    },
    menuHeaderRender: undefined,
    footerRender: () => null,
    onPageChange: () => {
      const { location } = history;
      const loggedIn = !!initialState?.currentUser;
      if (!loggedIn && location.pathname !== LOGIN_PATH) {
        history.replace(`${LOGIN_PATH}?redirect=${encodeURIComponent(location.pathname)}`);
      }
    },
    childrenRender: (children) => <App>{children}</App>,
  };
};

/**
 * 全局 request 配置：统一鉴权头、统一错误提示、401 自动跳登录。
 */
export const request: RequestConfig = {
  timeout: 15000,
  errorConfig: {
    errorThrower(res: any) {
      if (res?.code !== 200) {
        const error: any = new Error(res?.message || '请求失败');
        error.name = 'BizError';
        error.info = res;
        throw error;
      }
    },
    errorHandler(error: any, opts: any) {
      if (opts?.skipErrorHandler) throw error;
      if (error?.name === 'BizError') {
        message.error(error.info?.message || '请求失败');
      } else if (error?.response) {
        message.error(`HTTP ${error.response.status}`);
      } else {
        message.error('网络异常，请稍后重试');
      }
      throw error;
    },
  },
  requestInterceptors: [
    (config: any) => {
      const token = localStorage.getItem(TOKEN_KEY);
      if (token) {
        config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` };
      }
      return config;
    },
  ],
  responseInterceptors: [
    [
      (response: any) => response,
      (error: any) => {
        if (error?.response?.status === 401) {
          localStorage.removeItem(TOKEN_KEY);
          localStorage.removeItem(USER_KEY);
          history.replace(`${LOGIN_PATH}?redirect=${encodeURIComponent(history.location.pathname)}`);
        }
        throw error;
      },
    ],
  ],
};
