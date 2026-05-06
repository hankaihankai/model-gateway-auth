import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Access, useAccess } from '@umijs/max';
import {
  App,
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Tag,
} from 'antd';
import React, { useMemo, useRef, useState } from 'react';
import {
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
import { createMenu, deleteMenu, listMenus, updateMenu } from '@/services/rbac';

const MENU_TYPE_OPTIONS = [
  { label: '目录', value: 'DIR' },
  { label: '菜单', value: 'MENU' },
  { label: '按钮', value: 'BUTTON' },
];

const COMPONENT_OPTIONS = [
  { label: '用户列表', value: 'UserManageList' },
  { label: '角色管理', value: 'RoleManageList' },
  { label: '菜单管理', value: 'MenuManageList' },
  { label: 'API权限', value: 'ApiPermissionManageList' },
];

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

const MenuList: React.FC = () => {
  const access = useAccess();
  const { message, modal } = App.useApp();
  const actionRef = useRef<any>(null);
  const [form] = Form.useForm();
  const [menus, setMenus] = useState<API.SysMenu[]>([]);
  const [editingMenu, setEditingMenu] = useState<API.SysMenu | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const iconValue = Form.useWatch('icon', form);

  const parentOptions = useMemo(
    () => [
      { label: '根节点', value: 0 },
      ...menus
        .filter((item) => item.menuType !== 'BUTTON')
        .map((item) => ({ label: `${item.menuName} (#${item.menuId})`, value: item.menuId })),
    ],
    [menus],
  );

  const openCreate = () => {
    setEditingMenu(null);
    form.resetFields();
    form.setFieldsValue({ parentId: 0, menuType: 'MENU', visible: true, status: 0, sort: 0 });
    setModalOpen(true);
  };

  const openEdit = (record: API.SysMenu) => {
    setEditingMenu(record);
    form.setFieldsValue(record);
    setModalOpen(true);
  };

  const handleSave = async (values: API.MenuSaveRequest) => {
    if (editingMenu) {
      await updateMenu(editingMenu.menuId, values);
      message.success('更新成功');
    } else {
      await createMenu(values);
      message.success('创建成功');
    }
    setModalOpen(false);
    actionRef.current?.reload();
  };

  const columns: ProColumns<API.SysMenu>[] = [
    { title: '菜单ID', dataIndex: 'menuId', width: 90, search: false },
    { title: '父级ID', dataIndex: 'parentId', width: 90, search: false },
    {
      title: '类型',
      dataIndex: 'menuType',
      width: 100,
      valueEnum: {
        DIR: { text: '目录' },
        MENU: { text: '菜单' },
        BUTTON: { text: '按钮' },
      },
    },
    { title: '名称', dataIndex: 'menuName', width: 160 },
    { title: '路径', dataIndex: 'path', width: 200, search: false },
    { title: '组件Key', dataIndex: 'componentKey', width: 180, search: false },
    { title: '权限编码', dataIndex: 'permissionCode', width: 180 },
    {
      title: '图标',
      dataIndex: 'icon',
      width: 140,
      search: false,
      render: (_, record) => (
        <Space>
          {ICON_MAP[record.icon || '']}
          {record.icon}
        </Space>
      ),
    },
    {
      title: '可见',
      dataIndex: 'visible',
      width: 80,
      search: false,
      render: (_, record) => <Tag color={record.visible ? 'green' : 'default'}>{record.visible ? '是' : '否'}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      search: false,
      render: (_, record) => <Tag color={record.status === 0 ? 'green' : 'default'}>{record.status === 0 ? '启用' : '禁用'}</Tag>,
    },
    { title: '排序', dataIndex: 'sort', width: 90, search: false },
    {
      title: '操作',
      valueType: 'option',
      width: 160,
      render: (_, record) => (
        <Access accessible={access.canWriteMenu}>
          <Space>
            <a onClick={() => openEdit(record)}>编辑</a>
            {!record.builtin && (
              <a
                onClick={() => {
                  modal.confirm({
                    title: '删除菜单',
                    content: `确认删除 ${record.menuName}？`,
                    onOk: async () => {
                      await deleteMenu(record.menuId);
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
      <ProTable<API.SysMenu>
        actionRef={actionRef}
        rowKey="menuId"
        columns={columns}
        request={async () => {
          const res = await listMenus();
          const data = (res as any)?.data ?? [];
          setMenus(data);
          return { success: true, data };
        }}
        pagination={false}
        search={false}
        toolBarRender={() => [
          <Access key="create" accessible={access.canWriteMenu}>
            <Button type="primary" onClick={openCreate}>新增菜单</Button>
          </Access>,
        ]}
      />
      <Modal
        title={editingMenu ? '编辑菜单' : '新增菜单'}
        open={modalOpen}
        onOk={() => form.submit()}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleSave}>
          <Form.Item name="parentId" label="父级菜单">
            <Select options={parentOptions} />
          </Form.Item>
          <Form.Item name="menuType" label="菜单类型" rules={[{ required: true, message: '请选择菜单类型' }]}>
            <Select options={MENU_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item name="menuName" label="菜单名称" rules={[{ required: true, message: '请输入菜单名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="path" label="路由路径">
            <Input />
          </Form.Item>
          <Form.Item name="componentKey" label="组件Key">
            <Select allowClear options={COMPONENT_OPTIONS} />
          </Form.Item>
          <Form.Item name="permissionCode" label="权限编码">
            <Input />
          </Form.Item>
          <Form.Item name="icon" label="图标">
            <Input suffix={ICON_MAP[iconValue || '']} />
          </Form.Item>
          <Form.Item name="visible" label="菜单可见" valuePropName="checked">
            <Switch checkedChildren="是" unCheckedChildren="否" />
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

export default MenuList;
