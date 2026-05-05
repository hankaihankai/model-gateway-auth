import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Card, Tabs, Descriptions, Tag, Skeleton, Button, Modal, Form, InputNumber, Radio } from 'antd';
import { useParams, useRequest } from '@umijs/max';
import React, { useState } from 'react';
import { getUserDetail, getUserTokenRecords, updateUserAmount } from '@/services/user';

const UserDetail: React.FC = () => {
  const { userId } = useParams<{ userId: string }>();
  const id = Number(userId);
  const { data: detail, loading: detailLoading } = useRequest(() => getUserDetail(id));
  const [amountModalOpen, setAmountModalOpen] = useState(false);
  const [amountForm] = Form.useForm();

  const handleAmount = async (values: any) => {
    await updateUserAmount(id, {
      mode: values.mode,
      amount: values.amount,
    });
    setAmountModalOpen(false);
    amountForm.resetFields();
    window.location.reload();
  };

  const recordColumns = [
    { title: 'ID', dataIndex: 'id', width: 80 },
    { title: '模型', dataIndex: 'modelName', width: 140 },
    { title: '时间', dataIndex: 'createdAt', width: 160 },
    { title: 'Token 消耗', dataIndex: 'tokenUsed', width: 100 },
    { title: '请求次数', dataIndex: 'count', width: 100 },
    { title: '额度消耗', dataIndex: 'quota', width: 100 },
  ];

  const statusText = (s?: number) => {
    if (s === 0) return '启用';
    if (s === 1) return '禁用';
    if (s === 2) return '处理中';
    if (s === 3) return '异常';
    return '未知';
  };

  const statusColor = (s?: number) => {
    if (s === 0) return 'green';
    if (s === 1) return 'default';
    if (s === 2) return 'blue';
    if (s === 3) return 'red';
    return 'default';
  };

  const items = [
    {
      key: 'info',
      label: '用户信息',
      children: detailLoading ? (
        <Skeleton active paragraph={{ rows: 5 }} />
      ) : (
        <Descriptions bordered column={2}>
          <Descriptions.Item label="用户ID">{detail?.userId}</Descriptions.Item>
          <Descriptions.Item label="用户名">{detail?.username}</Descriptions.Item>
          <Descriptions.Item label="昵称">{detail?.nickname}</Descriptions.Item>
          <Descriptions.Item label="手机号">{detail?.phone}</Descriptions.Item>
          <Descriptions.Item label="邮箱">{detail?.email}</Descriptions.Item>
          <Descriptions.Item label="角色">{detail?.role}</Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={statusColor(detail?.status)}>{statusText(detail?.status)}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="newapi 用户ID">{detail?.newApiUserId ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="newapi 用户名">{detail?.newApiUserName ?? '-'}</Descriptions.Item>
        </Descriptions>
      ),
    },
    {
      key: 'quota',
      label: '额度概览',
      children: detailLoading ? (
        <Skeleton active paragraph={{ rows: 4 }} />
      ) : (
        <>
          <Descriptions bordered column={2}>
            <Descriptions.Item label="当前余额">
              <span>{detail?.currentBalanceAmount ?? '-'}</span>
              <Button type="link" size="small" onClick={() => setAmountModalOpen(true)}>充值</Button>
            </Descriptions.Item>
            <Descriptions.Item label="已用额度">{detail?.usedQuotaAmount ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="总额度">{detail?.totalQuotaAmount ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="剩余额度(原始)">{detail?.quota ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="已用额度(原始)">{detail?.usedQuota ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="总额度(原始)">{detail?.totalQuota ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="换算比例">{detail?.quotaPerUnit ?? '-'}</Descriptions.Item>
          </Descriptions>
          <Modal
          title="充值"
          open={amountModalOpen}
          onOk={() => amountForm.submit()}
          onCancel={() => {
            setAmountModalOpen(false);
            amountForm.resetFields();
          }}
          destroyOnClose
        >
          <Form form={amountForm} onFinish={handleAmount} layout="vertical">
            <Form.Item name="mode" label="操作类型" initialValue="add" rules={[{ required: true }]}>
              <Radio.Group>
                <Radio value="add">增加</Radio>
                <Radio value="subtract">减少</Radio>
                <Radio value="override">覆盖</Radio>
              </Radio.Group>
            </Form.Item>
            <Form.Item name="amount" label="金额" rules={[{ required: true, message: '请输入金额' }]}>
              <InputNumber style={{ width: '100%' }} precision={2} min={0} placeholder="请输入金额" />
            </Form.Item>
          </Form>
        </Modal>
        </>
      ),
    },
    {
      key: 'records',
      label: 'AI 调用记录',
      children: (
        <ProTable
          columns={recordColumns}
          rowKey="id"
          search={false}
          request={async (params) => {
            const res = await getUserTokenRecords(id, {
              pageNo: params.current,
              pageSize: params.pageSize,
            });
            // 兼容 Umi request 自动解包 data 的情况
            const data = (res as any)?.items ?? (res as any)?.data?.items ?? [];
            const total = (res as any)?.total ?? (res as any)?.data?.total ?? 0;
            return { success: true, data, total };
          }}
          pagination={{ defaultPageSize: 10 }}
        />
      ),
    },
  ];

  return (
    <PageContainer title={`用户详情 #${id}`}>
      <Card>
        <Tabs items={items} />
      </Card>
    </PageContainer>
  );
};

export default UserDetail;
