import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { App, Button, Tag, Modal, Form, Input, Switch } from 'antd';
import React, { useRef, useState } from 'react';
import { Link } from '@umijs/max';
import { listUsers, createUser, bindNewApi } from '@/services/user';

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
        const cfg = STATUS_VALUE_ENUM[record.status];
        return <Tag color={cfg?.color}>{cfg?.text}</Tag>;
      },
    },
    {
      title: '操作',
      valueType: 'option',
      width: 120,
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
    </PageContainer>
  );
};

export default UserList;
