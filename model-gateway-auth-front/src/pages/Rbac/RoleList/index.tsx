import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Access, useAccess } from '@umijs/max';
import { App, Button, Form, Input, InputNumber, Modal, Select, Space, Tag } from 'antd';
import React, { useRef, useState } from 'react';
import {
  createRole,
  deleteRole,
  getRoleGrant,
  listApiPermissions,
  listMenus,
  listRoles,
  updateRole,
  updateRoleGrant,
} from '@/services/rbac';

const STATUS_VALUE_ENUM = {
  0: { text: '启用', color: 'green' },
  1: { text: '禁用', color: 'default' },
};

const RoleList: React.FC = () => {
  const access = useAccess();
  const { message, modal } = App.useApp();
  const actionRef = useRef<any>(null);
  const [form] = Form.useForm();
  const [grantForm] = Form.useForm();
  const [editingRole, setEditingRole] = useState<API.SysRole | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [grantOpen, setGrantOpen] = useState(false);
  const [grantRole, setGrantRole] = useState<API.SysRole | null>(null);
  const [menuOptions, setMenuOptions] = useState<{ label: string; value: number }[]>([]);
  const [apiOptions, setApiOptions] = useState<{ label: string; value: number }[]>([]);

  const openCreate = () => {
    setEditingRole(null);
    form.resetFields();
    form.setFieldsValue({ status: 0, sort: 0 });
    setModalOpen(true);
  };

  const openEdit = (record: API.SysRole) => {
    setEditingRole(record);
    form.setFieldsValue(record);
    setModalOpen(true);
  };

  const handleSave = async (values: API.RoleSaveRequest) => {
    if (editingRole) {
      await updateRole(editingRole.roleId, values);
      message.success('更新成功');
    } else {
      await createRole(values);
      message.success('创建成功');
    }
    setModalOpen(false);
    actionRef.current?.reload();
  };

  const openGrant = async (record: API.SysRole) => {
    setGrantRole(record);
    const [grantRes, menusRes, apiRes] = await Promise.all([
      getRoleGrant(record.roleId),
      listMenus(),
      listApiPermissions(),
    ]);
    const menus = (menusRes as any)?.data ?? [];
    const apis = (apiRes as any)?.data ?? [];
    setMenuOptions(menus.map((item: API.SysMenu) => ({
      label: `${item.menuName} (${item.permissionCode || item.menuType})`,
      value: item.menuId,
    })));
    setApiOptions(apis.map((item: API.SysApiPermission) => ({
      label: `${item.permissionName} (${item.permissionCode})`,
      value: item.apiPermissionId,
    })));
    grantForm.setFieldsValue((grantRes as any)?.data ?? { menuIds: [], apiPermissionIds: [] });
    setGrantOpen(true);
  };

  const handleGrant = async (values: { menuIds?: number[]; apiPermissionIds?: number[] }) => {
    if (!grantRole) return;
    await updateRoleGrant(grantRole.roleId, {
      menuIds: values.menuIds ?? [],
      apiPermissionIds: values.apiPermissionIds ?? [],
    });
    message.success('授权已更新');
    setGrantOpen(false);
  };

  const columns: ProColumns<API.SysRole>[] = [
    { title: '角色ID', dataIndex: 'roleId', width: 90, search: false },
    { title: '角色编码', dataIndex: 'roleCode', width: 160 },
    { title: '角色名称', dataIndex: 'roleName', width: 160 },
    { title: '说明', dataIndex: 'description', search: false },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      valueEnum: STATUS_VALUE_ENUM,
      render: (_, record) => {
        const cfg = STATUS_VALUE_ENUM[record.status as keyof typeof STATUS_VALUE_ENUM];
        return <Tag color={cfg?.color}>{cfg?.text}</Tag>;
      },
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
      width: 220,
      render: (_, record) => (
        <Access accessible={access.canWriteRole}>
          <Space>
            <a onClick={() => openGrant(record)}>授权</a>
            <a onClick={() => openEdit(record)}>编辑</a>
            {!record.builtin && (
              <a
                onClick={() => {
                  modal.confirm({
                    title: '删除角色',
                    content: `确认删除 ${record.roleName}？`,
                    onOk: async () => {
                      await deleteRole(record.roleId);
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
      <ProTable<API.SysRole>
        actionRef={actionRef}
        rowKey="roleId"
        columns={columns}
        request={async () => {
          const res = await listRoles();
          return { success: true, data: (res as any)?.data ?? [] };
        }}
        pagination={false}
        search={false}
        toolBarRender={() => [
          <Access key="create" accessible={access.canWriteRole}>
            <Button type="primary" onClick={openCreate}>新增角色</Button>
          </Access>,
        ]}
      />
      <Modal
        title={editingRole ? '编辑角色' : '新增角色'}
        open={modalOpen}
        onOk={() => form.submit()}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleSave}>
          <Form.Item name="roleCode" label="角色编码" rules={[{ required: !editingRole, message: '请输入角色编码' }]}>
            <Input disabled={!!editingRole} />
          </Form.Item>
          <Form.Item name="roleName" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
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
      <Modal
        title={`角色授权${grantRole ? ` - ${grantRole.roleName}` : ''}`}
        open={grantOpen}
        onOk={() => grantForm.submit()}
        onCancel={() => setGrantOpen(false)}
        destroyOnClose
        width={720}
      >
        <Form form={grantForm} layout="vertical" onFinish={handleGrant}>
          <Form.Item name="menuIds" label="菜单/按钮权限">
            <Select mode="multiple" options={menuOptions} optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="apiPermissionIds" label="API权限">
            <Select mode="multiple" options={apiOptions} optionFilterProp="label" />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default RoleList;
