import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Access, useAccess } from '@umijs/max';
import { App, Button, Form, Input, InputNumber, Modal, Select, Space, Tag } from 'antd';
import React, { useRef, useState } from 'react';
import {
  createApiPermission,
  deleteApiPermission,
  listApiPermissions,
  updateApiPermission,
} from '@/services/rbac';

const METHOD_OPTIONS = ['GET', 'POST', 'PUT', 'DELETE', '*'].map((value) => ({ label: value, value }));

const ApiPermissionList: React.FC = () => {
  const access = useAccess();
  const { message, modal } = App.useApp();
  const actionRef = useRef<any>(null);
  const [form] = Form.useForm();
  const [editingPermission, setEditingPermission] = useState<API.SysApiPermission | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const openCreate = () => {
    setEditingPermission(null);
    form.resetFields();
    form.setFieldsValue({ method: 'GET', status: 0, sort: 0 });
    setModalOpen(true);
  };

  const openEdit = (record: API.SysApiPermission) => {
    setEditingPermission(record);
    form.setFieldsValue(record);
    setModalOpen(true);
  };

  const handleSave = async (values: API.ApiPermissionSaveRequest) => {
    if (editingPermission) {
      await updateApiPermission(editingPermission.apiPermissionId, values);
      message.success('更新成功');
    } else {
      await createApiPermission(values);
      message.success('创建成功');
    }
    setModalOpen(false);
    actionRef.current?.reload();
  };

  const columns: ProColumns<API.SysApiPermission>[] = [
    { title: 'ID', dataIndex: 'apiPermissionId', width: 80, search: false },
    { title: '权限编码', dataIndex: 'permissionCode', width: 180 },
    { title: '权限名称', dataIndex: 'permissionName', width: 160 },
    { title: '方法', dataIndex: 'method', width: 90 },
    { title: '路径', dataIndex: 'pathPattern', width: 240, search: false },
    { title: '说明', dataIndex: 'description', search: false },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      search: false,
      render: (_, record) => <Tag color={record.status === 0 ? 'green' : 'default'}>{record.status === 0 ? '启用' : '禁用'}</Tag>,
    },
    {
      title: '内置',
      dataIndex: 'builtin',
      width: 90,
      search: false,
      render: (_, record) => <Tag color={record.builtin ? 'blue' : 'default'}>{record.builtin ? '是' : '否'}</Tag>,
    },
    { title: '排序', dataIndex: 'sort', width: 90, search: false },
    {
      title: '操作',
      valueType: 'option',
      width: 160,
      render: (_, record) => (
        <Access accessible={access.canWriteApiPermission}>
          <Space>
            <a onClick={() => openEdit(record)}>编辑</a>
            {!record.builtin && (
              <a
                onClick={() => {
                  modal.confirm({
                    title: '删除API权限',
                    content: `确认删除 ${record.permissionName}？`,
                    onOk: async () => {
                      await deleteApiPermission(record.apiPermissionId);
                      message.success('删除成功');
                      actionRef.current?.reload();
                    },
                  });
                }}
              >
                删除
              </a>
            )}
          </Space>
        </Access>
      ),
    },
  ];

  return (
    <PageContainer>
      <ProTable<API.SysApiPermission>
        actionRef={actionRef}
        rowKey="apiPermissionId"
        columns={columns}
        request={async () => {
          const res = await listApiPermissions();
          return { success: true, data: (res as any)?.data ?? [] };
        }}
        pagination={false}
        search={false}
        toolBarRender={() => [
          <Access key="create" accessible={access.canWriteApiPermission}>
            <Button type="primary" onClick={openCreate}>新增API权限</Button>
          </Access>,
        ]}
      />
      <Modal
        title={editingPermission ? '编辑API权限' : '新增API权限'}
        open={modalOpen}
        onOk={() => form.submit()}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleSave}>
          <Form.Item name="permissionCode" label="权限编码" rules={[{ required: !editingPermission, message: '请输入权限编码' }]}>
            <Input disabled={!!editingPermission} />
          </Form.Item>
          <Form.Item name="permissionName" label="权限名称" rules={[{ required: true, message: '请输入权限名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="method" label="HTTP方法" rules={[{ required: true, message: '请选择HTTP方法' }]}>
            <Select options={METHOD_OPTIONS} />
          </Form.Item>
          <Form.Item name="pathPattern" label="路径表达式" rules={[{ required: true, message: '请输入路径表达式' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ label: '启用', value: 0 }, { label: '禁用', value: 1 }]} />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber style={{ width: '100%' }} min={0} precision={0} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default ApiPermissionList;
