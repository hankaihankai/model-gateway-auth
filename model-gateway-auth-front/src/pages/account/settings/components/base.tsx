import { ProForm, ProFormText } from '@ant-design/pro-components';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useModel } from '@umijs/max';
import { App, Skeleton } from 'antd';
import React from 'react';
import { updateCurrentUserProfile } from '@/services/user';
import { queryCurrent } from '../service';
import useStyles from './index.style';

const BaseView: React.FC = () => {
  const { styles } = useStyles();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const { setInitialState } = useModel('@@initialState');

  const { data: currentUser, isLoading: loading } = useQuery({
    queryKey: ['current-user-profile'],
    queryFn: queryCurrent,
  });

  const handleFinish = async (values: API.UserProfileUpdateRequest) => {
    await updateCurrentUserProfile(values);
    const nextUser = await queryCurrent();
    queryClient.setQueryData(['current-user-profile'], nextUser);
    const storedUser = JSON.parse(localStorage.getItem('user_info') || 'null') as API.UserInfo | null;
    const nextUserInfo = storedUser
      ? { ...storedUser, nickname: nextUser.nickname }
      : {
          userId: nextUser.userId,
          username: nextUser.username,
          nickname: nextUser.nickname,
          roles: nextUser.roles,
          permissions: [],
        };
    localStorage.setItem('user_info', JSON.stringify(nextUserInfo));
    setInitialState((state) => ({
      ...state,
      currentUser: state?.currentUser ? { ...state.currentUser, nickname: nextUser.nickname } : nextUserInfo,
    }));
    message.success('基本信息已更新');
  };

  return (
    <div className={styles.baseView}>
      {loading ? (
        <Skeleton active paragraph={{ rows: 4 }} />
      ) : (
        <div className={styles.left}>
          <ProForm<API.UserProfileUpdateRequest>
            layout="vertical"
            onFinish={handleFinish}
            submitter={{
              searchConfig: {
                submitText: '更新基本信息',
              },
              render: (_, dom) => dom[1],
            }}
            initialValues={{
              username: currentUser?.username,
              nickname: currentUser?.nickname,
              phone: currentUser?.phone,
              email: currentUser?.email,
            }}
            requiredMark={false}
          >
            <ProFormText width="md" name="username" label="用户名" disabled />
            <ProFormText
              width="md"
              name="nickname"
              label="昵称"
              rules={[{ required: true, message: '请输入昵称' }]}
            />
            <ProFormText
              width="md"
              name="phone"
              label="手机号"
              rules={[{ required: true, message: '请输入手机号' }]}
            />
            <ProFormText
              width="md"
              name="email"
              label="邮箱"
              rules={[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '请输入正确的邮箱' },
              ]}
            />
          </ProForm>
        </div>
      )}
    </div>
  );
};

export default BaseView;
