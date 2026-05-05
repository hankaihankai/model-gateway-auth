import type { ProLayoutProps } from '@ant-design/pro-components';

const Settings: ProLayoutProps & { pwa?: boolean; logo?: string } = {
  navTheme: 'light',
  colorPrimary: '#1677ff',
  layout: 'mix',
  contentWidth: 'Fluid',
  fixedHeader: false,
  fixSiderbar: true,
  pwa: false,
  logo: '/logo.svg',
  iconfontUrl: '',
  title: 'Model Gateway Auth',
  token: {},
};

export default Settings;
