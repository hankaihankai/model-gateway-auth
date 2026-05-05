import { PageContainer, ProTable } from '@ant-design/pro-components';
import { Card, Tabs, Descriptions, Tag, Skeleton } from 'antd';
import { useParams, useRequest } from '@umijs/max';
import React from 'react';
import { getUserDetail, getUserTokenRecords } from '@/services/user';

const UserDetail: React.FC = () => {
  const { userId } = useParams<{ userId: string }>();
  const id = Number(userId);
  const { data: detail, loading: detailLoading } = useRequest(() => getUserDetail(id));

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
        <Descriptions bordered column={2}>
          <Descriptions.Item label="当前余额">{detail?.currentBalanceAmount ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="已用额度">{detail?.usedQuotaAmount ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="总额度">{detail?.totalQuotaAmount ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="剩余额度(原始)">{detail?.quota ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="已用额度(原始)">{detail?.usedQuota ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="总额度(原始)">{detail?.totalQuota ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="换算比例">{detail?.quotaPerUnit ?? '-'}</Descriptions.Item>
        </Descriptions>
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
            if (!res) {
              return { success: false, data: [], total: 0 };
            }
            return { success: true, data: res.items, total: res.total };
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
