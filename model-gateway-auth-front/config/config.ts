import { defineConfig } from '@umijs/max';
import defaultSettings from './defaultSettings';
import proxy from './proxy';
import routes from './routes';

const { REACT_APP_ENV = 'dev' } = process.env;

export default defineConfig({
  hash: true,
  routes,
  proxy: (proxy as any)[REACT_APP_ENV],
  fastRefresh: true,
  layout: { ...defaultSettings, locale: false },
  locale: {
    default: 'zh-CN',
    antd: true,
    baseNavigator: false,
    title: false,
  },
  request: {},
  access: {},
  initialState: {},
  model: {},
  npmClient: 'pnpm',
  antd: {},
  define: { 'process.env.REACT_APP_ENV': REACT_APP_ENV },
  publicPath: '/',
});
