import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { LoginForm, ProFormCheckbox, ProFormText } from '@ant-design/pro-components';
import { Helmet, history, useModel } from '@umijs/max';
import { message } from 'antd';
import React from 'react';
import defaultSettings from '../../../../config/defaultSettings';
import { login as loginApi } from '@/services/auth';

const TOKEN_KEY = 'access_token';
const USER_KEY = 'user_info';

const Login: React.FC = () => {
  const { setInitialState } = useModel('@@initialState');

  const handleSubmit = async (values: { username: string; password: string }) => {
    try {
      const res = await loginApi({ username: values.username, password: values.password });
      if (res.code !== 200 || !res.data) {
        message.error(res.message || '登录失败');
        return;
      }
      const { accessToken, userInfo, permissionContext } = res.data;
      const currentUser = {
        ...userInfo,
        roles: permissionContext?.roles ?? userInfo.roles,
        permissions: permissionContext?.permissions ?? userInfo.permissions,
      };
      localStorage.setItem(TOKEN_KEY, accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(currentUser));
      await setInitialState((s) => ({ ...(s ?? {}), currentUser, permissionContext }));
      message.success('登录成功');
      const params = new URLSearchParams(history.location.search);
      const redirect = params.get('redirect') || permissionContext?.menus?.[0]?.path || '/user-manage/list';
      history.push(redirect);
    } catch (error: any) {
      message.error(error?.info?.message || error?.message || '登录失败');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', background: '#f0f2f5' }}>
      <Helmet>
        <title>登录 - Model Gateway Auth</title>
      </Helmet>
      <div style={{ flex: '1', padding: '32px 0', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ minWidth: 360 }}>
          <LoginForm
            logo={defaultSettings.logo}
            title="Model Gateway Auth"
            subTitle="后台管理系统"
            initialValues={{ autoLogin: true }}
            onFinish={async (values) => {
              await handleSubmit(values as { username: string; password: string });
            }}
          >
            <ProFormText
              name="username"
              fieldProps={{ size: 'large', prefix: <UserOutlined /> }}
              placeholder="请输入用户名"
              rules={[{ required: true, message: '请输入用户名' }]}
            />
            <ProFormText.Password
              name="password"
              fieldProps={{ size: 'large', prefix: <LockOutlined /> }}
              placeholder="请输入密码"
              rules={[{ required: true, message: '请输入密码' }]}
            />
            <div style={{ marginBottom: 24 }}>
              <ProFormCheckbox noStyle name="autoLogin">自动登录</ProFormCheckbox>
            </div>
          </LoginForm>
        </div>
      </div>
    </div>
  );
};

export default Login;
