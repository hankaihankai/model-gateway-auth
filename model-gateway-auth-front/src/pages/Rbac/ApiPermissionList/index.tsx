import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Access, useAccess } from '@umijs/max';
import {
  App,
  Badge,
  Button,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Tag,
  Tooltip,
} from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import React, { useEffect, useMemo, useState } from 'react';
import {
  createApiPermission,
  createApp,
  deleteApiPermission,
  deleteApp,
  listApps,
  listApiPermissions,
  updateApiPermission,
  updateApp,
} from '@/services/rbac';

const METHOD_OPTIONS = ['GET', 'POST', 'PUT', 'DELETE', '*'].map((value) => ({ label: value, value }));

const STATUS_OPTIONS = [
  { label: '启用', value: 0 },
  { label: '禁用', value: 1 },
];

const ApiPermissionList: React.FC = () => {
  const access = useAccess();
  const { message, modal } = App.useApp();
  const [permissionForm] = Form.useForm();
  const [appForm] = Form.useForm();
  const [apps, setApps] = useState<API.SysApp[]>([]);
  const [permissions, setPermissions] = useState<API.SysApiPermission[]>([]);
  const [selectedAppId, setSelectedAppId] = useState<number>();
  const [editingPermission, setEditingPermission] = useState<API.SysApiPermission | null>(null);
  const [editingApp, setEditingApp] = useState<API.SysApp | null>(null);
  const [permissionModalOpen, setPermissionModalOpen] = useState(false);
  const [appModalOpen, setAppModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const selectedApp = useMemo(
    () => apps.find((item) => item.appId === selectedAppId),
    [apps, selectedAppId],
  );

  const filteredPermissions = useMemo(
    () => permissions.filter((item) => item.appId === selectedAppId),
    [permissions, selectedAppId],
  );

  const loadData = async (nextSelectedAppId?: number) => {
    setLoading(true);
    try {
      const [appsRes, permissionsRes] = await Promise.all([listApps(), listApiPermissions()]);
      const nextApps = (appsRes as any)?.data ?? [];
      const nextPermissions = (permissionsRes as any)?.data ?? [];
      const preferredAppId = nextSelectedAppId ?? selectedAppId;
      const existsSelected = nextApps.some((item: API.SysApp) => item.appId === preferredAppId);
      setApps(nextApps);
      setPermissions(nextPermissions);
      setSelectedAppId(existsSelected ? preferredAppId : nextApps[0]?.appId);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const openCreatePermission = () => {
    if (!selectedAppId) {
      message.warning('请先选择应用');
      return;
    }
    setEditingPermission(null);
    permissionForm.resetFields();
    permissionForm.setFieldsValue({ method: 'GET', status: 0, sort: 0 });
    setPermissionModalOpen(true);
  };

  const openEditPermission = (record: API.SysApiPermission) => {
    setEditingPermission(record);
    permissionForm.setFieldsValue(record);
    setPermissionModalOpen(true);
  };

  const handleSavePermission = async (values: API.ApiPermissionSaveRequest) => {
    if (editingPermission) {
      await updateApiPermission(editingPermission.apiPermissionId, {
        ...values,
        appId: editingPermission.appId,
      });
      message.success('更新成功');
      setPermissionModalOpen(false);
      await loadData(editingPermission.appId);
      return;
    }
    await createApiPermission({
      ...values,
      appId: selectedAppId!,
    });
    message.success('创建成功');
    setPermissionModalOpen(false);
    await loadData(selectedAppId);
  };

  const openCreateApp = () => {
    setEditingApp(null);
    appForm.resetFields();
    appForm.setFieldsValue({ status: 0, sort: 0 });
    setAppModalOpen(true);
  };

  const openEditApp = (record: API.SysApp) => {
    setEditingApp(record);
    appForm.setFieldsValue(record);
    setAppModalOpen(true);
  };

  const handleSaveApp = async (values: API.AppSaveRequest) => {
    if (editingApp) {
      await updateApp(editingApp.appId, values);
      message.success('更新成功');
      setAppModalOpen(false);
      await loadData(editingApp.appId);
      return;
    }
    const res = await createApp(values);
    const createdApp = (res as any)?.data;
    message.success('创建成功');
    setAppModalOpen(false);
    await loadData(createdApp?.appId);
  };

  const confirmDeleteApp = (record: API.SysApp) => {
    modal.confirm({
      title: '删除应用',
      content: `确认删除 ${record.appName}？`,
      onOk: async () => {
        await deleteApp(record.appId);
        message.success('删除成功');
        await loadData();
      },
    });
  };

  const columns: ProColumns<API.SysApiPermission>[] = [
    { title: 'ID', dataIndex: 'apiPermissionId', width: 80, search: false },
    { title: '权限编码', dataIndex: 'permissionCode', width: 190 },
    { title: '权限名称', dataIndex: 'permissionName', width: 160 },
    { title: '方法', dataIndex: 'method', width: 90 },
    { title: '路径', dataIndex: 'pathPattern', width: 260, search: false },
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
            <a onClick={() => openEditPermission(record)}>编辑</a>
            {!record.builtin && (
              <a
                onClick={() => {
                  modal.confirm({
                    title: '删除API权限',
                    content: `确认删除 ${record.permissionName}？`,
                    onOk: async () => {
                      await deleteApiPermission(record.apiPermissionId);
                      message.success('删除成功');
                      await loadData(record.appId);
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
      <div style={{ display: 'grid', gridTemplateColumns: '280px minmax(0, 1fr)', gap: 16, alignItems: 'start' }}>
        <section style={{ border: '1px solid #f0f0f0', borderRadius: 6, background: '#fff' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: 16, borderBottom: '1px solid #f0f0f0' }}>
            <strong>应用</strong>
            <Access accessible={access.canWriteApp || access.canWriteApiPermission}>
              <Button type="primary" size="small" icon={<PlusOutlined />} onClick={openCreateApp}>
                新增
              </Button>
            </Access>
          </div>
          <div style={{ maxHeight: 'calc(100vh - 240px)', overflowY: 'auto', padding: 8 }}>
            {apps.length === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无应用" />
            ) : (
              apps.map((item) => {
                const selected = item.appId === selectedAppId;
                const permissionCount = item.permissionCount ?? 0;
                const deleteDisabled = permissionCount > 0;
                const deleteTip = permissionCount > 0 ? '应用下存在API权限，不能删除' : '删除应用';
                return (
                  <div
                    key={item.appId}
                    onClick={() => setSelectedAppId(item.appId)}
                    style={{
                      cursor: 'pointer',
                      borderRadius: 6,
                      border: selected ? '1px solid #1677ff' : '1px solid transparent',
                      background: selected ? '#e6f4ff' : '#fff',
                      padding: 10,
                      marginBottom: 8,
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                      <Space size={6} wrap>
                        <strong>{item.appName}</strong>
                        <Badge count={permissionCount} showZero size="small" />
                      </Space>
                      <Access accessible={access.canWriteApp || access.canWriteApiPermission}>
                        <Space size={2} onClick={(event) => event.stopPropagation()}>
                          <Tooltip title="编辑应用">
                            <Button
                              type="text"
                              size="small"
                              icon={<EditOutlined />}
                              onClick={() => openEditApp(item)}
                            />
                          </Tooltip>
                          <Tooltip title={deleteTip}>
                            <Button
                              type="text"
                              danger
                              size="small"
                              disabled={deleteDisabled}
                              icon={<DeleteOutlined />}
                              onClick={() => confirmDeleteApp(item)}
                            />
                          </Tooltip>
                        </Space>
                      </Access>
                    </div>
                    <div style={{ marginTop: 6, color: '#666', fontSize: 12 }}>{item.appCode}</div>
                    <div style={{ marginTop: 8 }}>
                      <Tag color={item.status === 0 ? 'green' : 'default'}>{item.status === 0 ? '启用' : '禁用'}</Tag>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </section>
        <section style={{ minWidth: 0 }}>
          <ProTable<API.SysApiPermission>
            rowKey="apiPermissionId"
            columns={columns}
            dataSource={filteredPermissions}
            loading={loading}
            pagination={false}
            search={false}
            headerTitle={selectedApp ? `${selectedApp.appName} API权限` : 'API权限'}
            toolBarRender={() => [
              <Access key="create" accessible={access.canWriteApiPermission}>
                <Button type="primary" icon={<PlusOutlined />} onClick={openCreatePermission} disabled={!selectedApp}>
                  新增API权限
                </Button>
              </Access>,
            ]}
          />
        </section>
      </div>
      <Modal
        title={editingPermission ? '编辑API权限' : '新增API权限'}
        open={permissionModalOpen}
        onOk={() => permissionForm.submit()}
        onCancel={() => setPermissionModalOpen(false)}
        destroyOnClose
      >
        <Form form={permissionForm} layout="vertical" onFinish={handleSavePermission}>
          <Form.Item label="应用">
            <Input value={editingPermission ? apps.find((item) => item.appId === editingPermission.appId)?.appName : selectedApp?.appName} disabled />
          </Form.Item>
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
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber style={{ width: '100%' }} min={0} precision={0} />
          </Form.Item>
        </Form>
      </Modal>
      <Modal
        title={editingApp ? '编辑应用' : '新增应用'}
        open={appModalOpen}
        onOk={() => appForm.submit()}
        onCancel={() => setAppModalOpen(false)}
        destroyOnClose
      >
        <Form form={appForm} layout="vertical" onFinish={handleSaveApp}>
          <Form.Item name="appCode" label="应用编码" rules={[{ required: !editingApp, message: '请输入应用编码' }]}>
            <Input disabled={!!editingApp} />
          </Form.Item>
          <Form.Item name="appName" label="应用名称" rules={[{ required: true, message: '请输入应用名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={STATUS_OPTIONS} />
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
