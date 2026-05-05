import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { App, Button, Tag, Modal, Form, Input, Switch } from 'antd';
import React, { useRef, useState } from 'react';
import { Link } from '@umijs/max';
import { listUsers, createUser, bindNewApi, updateUserStatus, getUserGatewayToken } from '@/services/user';

// ProColumns 类型中无 hideInSearch，使用 search: false 替代以避免类型错误
const col = (c: ProColumns<API.UserListItem> & { hideInSearch?: boolean }): ProColumns<API.UserListItem> => c;

const ROLE_VALUE_ENUM = {
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
  const { message } = App.useApp();
  const actionRef = useRef<any>(null);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createForm] = Form.useForm();
  const [testAiModalOpen, setTestAiModalOpen] = useState(false);
  const [testAiUserId, setTestAiUserId] = useState<number | null>(null);
  const [testAiForm] = Form.useForm();
  const [testAiResult, setTestAiResult] = useState<string>('');
  const [testAiLoading, setTestAiLoading] = useState(false);

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

  const handleTestAi = async (values: any) => {
    if (!testAiUserId) return;
    setTestAiLoading(true);
    try {
      const tokenRes = await getUserGatewayToken(testAiUserId);
      const token = (tokenRes as any) ?? (tokenRes as any)?.data ?? tokenRes;
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
      dataIndex: 'role',
      width: 100,
      valueEnum: ROLE_VALUE_ENUM,
      render: (_, record) => {
        const cfg = ROLE_VALUE_ENUM[record.role as keyof typeof ROLE_VALUE_ENUM];
        return <Tag color={cfg?.color}>{cfg?.text || record.role}</Tag>;
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
      width: 160,
      render: (_, record) => [
        <Link key="view" to={`/user-manage/detail/${record.userId}`}>
          查看
        </Link>,
        !record.newApiBound && (
          <a
            key="bind"
            onClick={async () => {
              try {
                await bindNewApi(record.userId);
                message.success('绑定成功');
                actionRef.current?.reload();
              } catch (error: any) {
                message.error(error?.message || '绑定失败');
              }
            }}
          >
            绑定
          </a>
        ),
        record.newApiBound && (
          <a
            key="testAi"
            onClick={() => {
              setTestAiUserId(record.userId);
              setTestAiModalOpen(true);
              setTestAiResult('');
              testAiForm.resetFields();
            }}
          >
            测试AI
          </a>
        ),
      ],
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
          <Button
            key="create"
            type="primary"
            onClick={() => setCreateModalOpen(true)}
          >
            添加用户
          </Button>,
        ]}
      />
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
            initialValue={JSON.stringify({
              model: 'gpt-4o-mini',
              messages: [{ role: 'user', content: 'hello' }],
            }, null, 2)}
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
