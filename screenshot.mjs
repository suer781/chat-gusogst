import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  page.on('pageerror', err => console.log('[PAGE ERROR]', err.message));
  
  await page.emulateMedia({ colorScheme: 'dark' });
  await page.setViewportSize({ width: 390, height: 844 });
  
  await page.goto('http://localhost:5174/', { waitUntil: 'networkidle', timeout: 30000 });
  await page.waitForTimeout(2500);
  
  // 检查是否有 React 错误边界
  const errors = await page.evaluate(() => {
    const bodyText = document.body.innerText?.substring(0, 300) || '';
    return { bodyText };
  });
  console.log('页面文本:', errors.bodyText);
  
  // 布局数据
  const layout = await page.evaluate(() => {
    const el = (sel) => {
      const e = document.querySelector(sel);
      if (!e) return null;
      const r = e.getBoundingClientRect();
      return { top: Math.round(r.top), left: Math.round(r.left), width: Math.round(r.width), height: Math.round(r.height) };
    };
    return {
      viewport: [window.innerWidth, window.innerHeight],
      appRoot: el('.app-root'),
      appHeader: el('.app-header'),
      appContent: el('.app-content'),
      appNav: el('.app-nav'),
      chatRoot: el('.app-content > div'), // ChatView root
    };
  });
  console.log('布局:', JSON.stringify(layout, null, 2));
  
  await page.screenshot({ path: '/workspace/ui-dark.png' });
  
  // 切亮色
  await page.evaluate(() => document.documentElement.setAttribute('data-theme', 'light'));
  await page.waitForTimeout(800);
  await page.screenshot({ path: '/workspace/ui-light.png' });
  
  console.log('截图完成');
  await browser.close();
})().catch(err => { console.error('FATAL:', err); process.exit(1); });