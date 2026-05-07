import { List, Modal, Form, Input, App } from 'antd';
import { history } from '@umijs/max';
import React, { useState } from 'react';
import { updateCurrentUserPassword } from '@/services/user';

const TOKEN_KEY = 'access_token';
const USER_KEY = 'user_info';
const PERMISSION_CONTEXT_KEY = 'permission_context';

type SecurityItem = {
  title: string;
  description: React.ReactNode;
  actions: React.ReactNode[];
};

const SecurityView: React.FC = () => {
  const { message } = App.useApp();
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [passwordForm] = Form.useForm<API.UserPasswordUpdateRequest & { confirmPassword: string }>();

  const handlePasswordSave = async (values: API.UserPasswordUpdateRequest & { confirmPassword: string }) => {
    await updateCurrentUserPassword({
      oldPassword: values.oldPassword,
      newPassword: values.newPassword,
    });
    message.success('密码已修改，请重新登录');
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(PERMISSION_CONTEXT_KEY);
    history.replace('/user/login');
  };

  const data: SecurityItem[] = [
    {
      title: '账户密码',
      description: '用于登录 Model Gateway Auth 的账号密码',
      actions: [<a key="modify-password" onClick={() => setPasswordModalOpen(true)}>修改</a>],
    },
  ];

  return (
    <>
      <List<SecurityItem>
        itemLayout="horizontal"
        dataSource={data}
        renderItem={(item) => (
          <List.Item actions={item.actions}>
            <List.Item.Meta title={item.title} description={item.description} />
          </List.Item>
        )}
      />
      <Modal
        title="修改密码"
        open={passwordModalOpen}
        onOk={() => passwordForm.submit()}
        onCancel={() => {
          setPasswordModalOpen(false);
          passwordForm.resetFields();
        }}
        destroyOnClose
      >
        <Form form={passwordForm} layout="vertical" onFinish={handlePasswordSave}>
          <Form.Item name="oldPassword" label="旧密码" rules={[{ required: true, message: '请输入旧密码' }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 6, message: '新密码长度不能少于6位' },
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认新密码"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请确认新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('两次输入的新密码不一致'));
                },
              }),
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default SecurityView;
