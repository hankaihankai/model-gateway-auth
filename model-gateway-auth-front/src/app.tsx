import {
  LogoutOutlined,
  UserOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  KeyOutlined,
  MenuOutlined,
  ApiOutlined,
  SettingOutlined,
  DashboardOutlined,
  FileTextOutlined,
  BarChartOutlined,
} from '@ant-design/icons';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import type { RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import { history } from '@umijs/max';
import { App, Dropdown, message } from 'antd';
import React from 'react';
import defaultSettings from '../config/defaultSettings';
import { current as currentApi, logout as logoutApi } from '@/services/auth';

const TOKEN_KEY = 'access_token';
const USER_KEY = 'user_info';
const PERMISSION_CONTEXT_KEY = 'permission_context';
const LOGIN_PATH = '/user/login';
const MENU_COMPONENT_WHITELIST = new Set([
  'UserManageList',
  'RoleManageList',
  'MenuManageList',
  'ApiPermissionManageList',
]);

const ICON_MAP: Record<string, React.ReactNode> = {
  UserOutlined: <UserOutlined />,
  SafetyCertificateOutlined: <SafetyCertificateOutlined />,
  TeamOutlined: <TeamOutlined />,
  KeyOutlined: <KeyOutlined />,
  MenuOutlined: <MenuOutlined />,
  ApiOutlined: <ApiOutlined />,
  SettingOutlined: <SettingOutlined />,
  DashboardOutlined: <DashboardOutlined />,
  FileTextOutlined: <FileTextOutlined />,
  BarChartOutlined: <BarChartOutlined />,
};

function getIcon(iconName?: string): React.ReactNode {
  if (!iconName) return undefined;
  return ICON_MAP[iconName] || undefined;
}

/**
 * 判断本地是否已经有登录凭据。
 */
function hasLoginCredential(): boolean {
  return !!localStorage.getItem(TOKEN_KEY);
}

/**
 * 从本地缓存读取权限上下文。
 */
function getStoredPermissionContext(): API.PermissionContext | undefined {
  try {
    return JSON.parse(localStorage.getItem(PERMISSION_CONTEXT_KEY) || 'null') ?? undefined;
  } catch {
    return undefined;
  }
}

/**
 * 将后端菜单树转换为 ProLayout 菜单。
 */
function buildLayoutMenus(menus: API.MenuTreeItem[] = []): any[] {
  return menus
    .filter((item) => {
      if (!item.visible || item.status !== 0 || item.menuType === 'BUTTON') return false;
      if (item.menuType === 'MENU' && item.componentKey && !MENU_COMPONENT_WHITELIST.has(item.componentKey)) return false;
      return true;
    })
    .map((item) => ({
      path: item.path,
      name: item.menuName,
      icon: getIcon(item.icon),
      routes: buildLayoutMenus(item.children ?? []),
    }));
}

/**
 * 启动时拉初始登录态：从 localStorage 恢复 currentUser。
 */
export async function getInitialState(): Promise<{
  currentUser?: API.UserInfo;
  permissionContext?: API.PermissionContext;
  settings?: Partial<LayoutSettings>;
}> {
  const token = localStorage.getItem(TOKEN_KEY);
  if (!token) {
    return { settings: defaultSettings as Partial<LayoutSettings> };
  }
  try {
    const res = await currentApi();
    if (res.code === 200 && res.data) {
      const { userInfo, permissionContext } = res.data;
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo));
      localStorage.setItem(PERMISSION_CONTEXT_KEY, JSON.stringify(permissionContext));
      return {
        currentUser: userInfo,
        permissionContext,
        settings: defaultSettings as Partial<LayoutSettings>,
      };
    }
    const userInfo = JSON.parse(localStorage.getItem(USER_KEY) || 'null') as API.UserInfo | null;
    const permissionContext = getStoredPermissionContext();
    return {
      currentUser: userInfo ?? undefined,
      permissionContext,
      settings: defaultSettings as Partial<LayoutSettings>,
    };
  } catch {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(PERMISSION_CONTEXT_KEY);
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
    localStorage.removeItem(PERMISSION_CONTEXT_KEY);
    setInitialState((s) => ({ ...s, currentUser: undefined, permissionContext: undefined }));
    history.replace(`${LOGIN_PATH}?redirect=${encodeURIComponent(history.location.pathname)}`);
  };

  return {
    ...(initialState?.settings ?? {}),
    title: 'Model Gateway Auth',
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
    menu: {
      params: {
        userId: initialState?.currentUser?.userId,
        permissionContext: initialState?.permissionContext ?? getStoredPermissionContext(),
      },
      request: async () => buildLayoutMenus((initialState?.permissionContext ?? getStoredPermissionContext())?.menus ?? []),
    },
    onPageChange: () => {
      const { location } = history;
      const loggedIn = !!initialState?.currentUser || hasLoginCredential();
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
          localStorage.removeItem(PERMISSION_CONTEXT_KEY);
          history.replace(`${LOGIN_PATH}?redirect=${encodeURIComponent(history.location.pathname)}`);
        }
        throw error;
      },
    ],
  ],
};
