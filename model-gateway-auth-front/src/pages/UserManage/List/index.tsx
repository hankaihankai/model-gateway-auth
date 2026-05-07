import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Access, useAccess } from '@umijs/max';
import { App, Button, Tag, Modal, Form, Input, Switch, Select, Space, Dropdown } from 'antd';
import React, { useRef, useState } from 'react';
import { Link } from '@umijs/max';
import { MoreOutlined } from '@ant-design/icons';
import {
  listUsers,
  createUser,
  bindNewApi,
  updateUserStatus,
  getUserModels,
  getUserRoles,
  updateUserRoles,
  resetUserPassword,
} from '@/services/user';
import { listRoles } from '@/services/rbac';

// ProColumns 类型中无 hideInSearch，使用 search: false 替代以避免类型错误
const col = (c: ProColumns<API.UserListItem> & { hideInSearch?: boolean }): ProColumns<API.UserListItem> => c;

const ROLE_VALUE_ENUM = {
  SUPER_ADMIN: { text: 'SUPER_ADMIN', color: 'purple' },
  ADMIN: { text: 'ADMIN', color: 'red' },
  USER: { text: 'USER', color: 'blue' },
};

const STATUS_VALUE_ENUM = {
  0: { text: '启用', color: 'green' },
  1: { text: '禁用', color: 'default' },
  2: { text: '处理中', color: 'blue' },
  3: { text: '异常', color: 'red' },
};

const UserList: React.FC = () => {
  const access = useAccess();
  const { message } = App.useApp();
  const actionRef = useRef<any>(null);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createForm] = Form.useForm();
  const [roleModalOpen, setRoleModalOpen] = useState(false);
  const [roleUser, setRoleUser] = useState<API.UserListItem | null>(null);
  const [roleOptions, setRoleOptions] = useState<{ label: string; value: number }[]>([]);
  const [roleForm] = Form.useForm();
  const [testAiModalOpen, setTestAiModalOpen] = useState(false);
  const [testAiUserId, setTestAiUserId] = useState<number | null>(null);
  const [testAiForm] = Form.useForm();
  const [testAiResult, setTestAiResult] = useState<string>('');
  const [testAiLoading, setTestAiLoading] = useState(false);
  const [resetPasswordModalOpen, setResetPasswordModalOpen] = useState(false);
  const [resetPasswordUser, setResetPasswordUser] = useState<API.UserListItem | null>(null);
  const [resetPasswordForm] = Form.useForm<{ newPassword: string; confirmPassword: string }>();

  const handleCreate = async (values: any) => {
    await createUser({
      ...values,
      bindNewApi: values.bindNewApi ?? true,
    });
    message.success('创建成功');
    setCreateModalOpen(false);
    createForm.resetFields();
    actionRef.current?.reload();
  };

  const openRoleModal = async (record: API.UserListItem) => {
    const [rolesRes, userRolesRes] = await Promise.all([listRoles(), getUserRoles(record.userId)]);
    const roles = (rolesRes as any)?.data ?? [];
    setRoleOptions(roles.map((item: API.SysRole) => ({
      label: `${item.roleName} (${item.roleCode})`,
      value: item.roleId,
    })));
    roleForm.setFieldsValue({ roleIds: (userRolesRes as any)?.data ?? [] });
    setRoleUser(record);
    setRoleModalOpen(true);
  };

  const handleRoleSave = async (values: { roleIds?: number[] }) => {
    if (!roleUser) return;
    await updateUserRoles(roleUser.userId, values.roleIds ?? []);
    message.success('角色已更新');
    setRoleModalOpen(false);
    actionRef.current?.reload();
  };

  const openTestAiModal = async (record: API.UserListItem) => {
    setTestAiUserId(record.userId);
    setTestAiModalOpen(true);
    setTestAiResult('');
    testAiForm.resetFields();
    try {
      const res = await getUserModels(record.userId);
      const models = Array.isArray(res) ? res : ((res as any)?.data ?? []);
      const model = Array.isArray(models) && models.length > 0 ? models[0] : 'gpt-4o-mini';
      testAiForm.setFieldsValue({
        body: JSON.stringify({
          model,
          messages: [{ role: 'user', content: 'hello' }],
        }, null, 2),
      });
    } catch {
      testAiForm.setFieldsValue({
        body: JSON.stringify({
          model: 'gpt-4o-mini',
          messages: [{ role: 'user', content: 'hello' }],
        }, null, 2),
      });
    }
  };

  const openResetPasswordModal = (record: API.UserListItem) => {
    setResetPasswordUser(record);
    setResetPasswordModalOpen(true);
    resetPasswordForm.resetFields();
  };

  const handleResetPassword = async (values: { newPassword: string; confirmPassword: string }) => {
    if (!resetPasswordUser) return;
    await resetUserPassword(resetPasswordUser.userId, { newPassword: values.newPassword });
    message.success('密码已重置');
    setResetPasswordModalOpen(false);
    setResetPasswordUser(null);
    resetPasswordForm.resetFields();
  };

  const handleTestAi = async (values: any) => {
    if (!testAiUserId) return;
    setTestAiLoading(true);
    try {
      const token = localStorage.getItem('access_token') || '';
      if (!token) {
        throw new Error('登录Token不存在，请重新登录');
      }
      const body = JSON.parse(values.body);
      const response = await fetch('/v1/chat/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(body),
      });
      const res = await response.json();
      if (!response.ok) {
        throw new Error(res?.message || res?.error?.message || `HTTP ${response.status}`);
      }
      setTestAiResult(JSON.stringify(res, null, 2));
      message.success('调用成功');
    } catch (error: any) {
      message.error(error?.message || '调用失败');
    } finally {
      setTestAiLoading(false);
    }
  };

  const columns: ProColumns<API.UserListItem>[] = [
    col({
      title: '用户ID',
      dataIndex: 'userId',
      width: 80,
      hideInSearch: true,
    }),
    {
      title: '用户名',
      dataIndex: 'username',
      width: 140,
    },
    col({
      title: '昵称',
      dataIndex: 'nickname',
      width: 140,
      hideInSearch: true,
    }),
    col({
      title: '手机号',
      dataIndex: 'phone',
      width: 140,
      hideInSearch: true,
    }),
    col({
      title: '邮箱',
      dataIndex: 'email',
      width: 200,
      hideInSearch: true,
    }),
    {
      title: '角色',
      dataIndex: 'roles',
      width: 100,
      valueEnum: ROLE_VALUE_ENUM,
      render: (_, record) => {
        return (record.roles ?? []).map((role) => {
          const cfg = ROLE_VALUE_ENUM[role as keyof typeof ROLE_VALUE_ENUM];
          return <Tag key={role} color={cfg?.color}>{cfg?.text || role}</Tag>;
        });
      },
    },
    col({
      title: 'newapi 绑定',
      dataIndex: 'newApiBound',
      width: 120,
      hideInSearch: true,
      render: (_, record) => (
        <Tag color={record.newApiBound ? 'green' : 'default'}>
          {record.newApiBound ? '已绑' : '未绑'}
        </Tag>
      ),
    }),
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      valueEnum: STATUS_VALUE_ENUM,
      render: (_, record) => {
        if (record.status === 0 || record.status === 1) {
          return (
            <Switch
              checked={record.status === 0}
              checkedChildren="启用"
              unCheckedChildren="禁用"
              disabled={!access.canWriteUser}
              onChange={async (checked) => {
                try {
                  await updateUserStatus(record.userId, checked ? 0 : 1);
                  message.success('状态更新成功');
                  actionRef.current?.reload();
                } catch (error: any) {
                  message.error(error?.message || '状态更新失败');
                }
              }}
            />
          );
        }
        const cfg = STATUS_VALUE_ENUM[record.status];
        return <Tag color={cfg?.color}>{cfg?.text}</Tag>;
      },
    },
    {
      title: '操作',
      valueType: 'option',
      width: 240,
      render: (_, record) => (
        <Space>
          <Link to={`/user-manage/detail/${record.userId}`}>查看</Link>
          <Dropdown
            trigger={['click']}
            menu={{
              items: [
                access.canWriteUserRole ? { key: 'role', label: '角色' } : null,
                access.canWriteUser && !record.newApiBound ? { key: 'bind', label: '绑定 new-api' } : null,
                access.canWriteUser ? { key: 'resetPassword', label: '重置密码' } : null,
                record.newApiBound ? { key: 'testAi', label: '测试AI' } : null,
              ].filter(Boolean) as any[],
              onClick: async ({ key }) => {
                if (key === 'role') {
                  await openRoleModal(record);
                  return;
                }
                if (key === 'bind') {
                  try {
                    await bindNewApi(record.userId);
                    message.success('绑定成功');
                    actionRef.current?.reload();
                  } catch (error: any) {
                    message.error(error?.message || '绑定失败');
                  }
                  return;
                }
                if (key === 'resetPassword') {
                  openResetPasswordModal(record);
                  return;
                }
                if (key === 'testAi') {
                  await openTestAiModal(record);
                }
              },
            }}
          >
            <Button type="link" size="small" icon={<MoreOutlined />}>
              更多
            </Button>
          </Dropdown>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer>
      <ProTable<API.UserListItem, API.UserListQuery>
        actionRef={actionRef}
        rowKey="userId"
        columns={columns}
        request={async (params) => {
          const { current, pageSize, ...rest } = params;
          const res = await listUsers({
            pageNo: current,
            pageSize,
            username: rest.username,
            role: rest.role as API.UserListQuery['role'],
            status: rest.status !== undefined ? (Number(rest.status) as 0 | 1 | 2 | 3) : undefined,
          });
          // Umi request 在配置了 errorConfig 时会自动提取 res.data，此处兼容两种格式
          const data = (res as any)?.list ?? (res as any)?.data?.list ?? [];
          const total = (res as any)?.total ?? (res as any)?.data?.total ?? 0;
          return { success: true, data, total };
        }}
        pagination={{ defaultPageSize: 10, pageSizeOptions: ['10', '20', '50'] }}
        search={{ labelWidth: 'auto' }}
        toolBarRender={() => [
          <Access key="create" accessible={access.canWriteUser}>
            <Button type="primary" onClick={() => setCreateModalOpen(true)}>添加用户</Button>
          </Access>,
        ]}
      />
      <Modal
        title={`分配角色${roleUser ? ` - ${roleUser.username}` : ''}`}
        open={roleModalOpen}
        onOk={() => roleForm.submit()}
        onCancel={() => setRoleModalOpen(false)}
        destroyOnClose
      >
        <Form form={roleForm} layout="vertical" onFinish={handleRoleSave}>
          <Form.Item name="roleIds" label="角色">
            <Select mode="multiple" options={roleOptions} optionFilterProp="label" />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title={`重置密码${resetPasswordUser ? ` - ${resetPasswordUser.username}` : ''}`}
        open={resetPasswordModalOpen}
        onOk={() => resetPasswordForm.submit()}
        onCancel={() => {
          setResetPasswordModalOpen(false);
          setResetPasswordUser(null);
          resetPasswordForm.resetFields();
        }}
        destroyOnClose
      >
        <Form form={resetPasswordForm} onFinish={handleResetPassword} layout="vertical">
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
      <Modal
        title="添加用户"
        open={createModalOpen}
        onOk={() => createForm.submit()}
        onCancel={() => {
          setCreateModalOpen(false);
          createForm.resetFields();
        }}
        destroyOnClose
      >
        <Form form={createForm} onFinish={handleCreate} layout="vertical">
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="nickname" label="昵称">
            <Input />
          </Form.Item>
          <Form.Item name="phone" label="手机号">
            <Input />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input />
          </Form.Item>
          <Form.Item
            name="bindNewApi"
            label="同步创建 new-api 账号"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title={`测试AI调用 (用户 #${testAiUserId})`}
        open={testAiModalOpen}
        onOk={() => testAiForm.submit()}
        onCancel={() => {
          setTestAiModalOpen(false);
          setTestAiResult('');
          testAiForm.resetFields();
          setTestAiUserId(null);
        }}
        destroyOnClose
        confirmLoading={testAiLoading}
      >
        <Form form={testAiForm} onFinish={handleTestAi} layout="vertical">
          <Form.Item
            name="body"
            label="请求体 (OpenAI 格式)"
            rules={[
              { required: true, message: '请输入请求体' },
              {
                validator: (_, value) => {
                  if (!value) return Promise.resolve();
                  try {
                    JSON.parse(value);
                    return Promise.resolve();
                  } catch {
                    return Promise.reject(new Error('请求体必须是合法的 JSON'));
                  }
                },
              },
            ]}
          >
            <Input.TextArea rows={8} placeholder="请输入 OpenAI 格式的请求体 JSON" />
          </Form.Item>
        </Form>
        {testAiResult && (
          <div style={{ marginTop: 16 }}>
            <p style={{ fontWeight: 'bold', marginBottom: 8 }}>响应结果：</p>
            <pre
              style={{
                background: '#f6f8fa',
                padding: 12,
                borderRadius: 4,
                maxHeight: 300,
                overflow: 'auto',
                fontSize: 12,
              }}
            >
              {testAiResult}
            </pre>
          </div>
        )}
      </Modal>
    </PageContainer>
  );
};

export default UserList;
