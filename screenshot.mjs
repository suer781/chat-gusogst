import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  // 先强制暗色模式
  await page.emulateMedia({ colorScheme: 'dark' });
  await page.setViewportSize({ width: 390, height: 844 });
  
  await page.goto('http://localhost:5174/', { waitUntil: 'networkidle', timeout: 30000 });
  await page.waitForTimeout(2000);
  
  // 确认暗色
  let theme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
  console.log('当前主题:', theme);
  
  await page.screenshot({ path: '/workspace/ui-dark.png', fullPage: false });
  console.log('暗色截图完成:', (await page.evaluate(() => document.querySelector('.app-root')?.getBoundingClientRect())));

  // 切换到亮色
  await page.evaluate(() => document.documentElement.setAttribute('data-theme', 'light'));
  await page.waitForTimeout(1000);
  theme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
  console.log('切换后主题:', theme);
  
  await page.screenshot({ path: '/workspace/ui-light.png', fullPage: false });
  console.log('亮色截图完成');
  
  // 查看页面元素的实际布局数值
  const layout = await page.evaluate(() => {
    const el = (sel) => {
      const e = document.querySelector(sel);
      if (!e) return null;
      const r = e.getBoundingClientRect();
      return { top: r.top, left: r.left, width: r.width, height: r.height };
    };
    return {
      appRoot: el('.app-root'),
      appHeader: el('.app-header'),
      appContent: el('.app-content'),
      appNav: el('.app-nav'),
      viewport: { w: window.innerWidth, h: window.innerHeight },
    };
  });
  console.log('布局数据:', JSON.stringify(layout, null, 2));
  
  await browser.close();
})().catch(err => { console.error('FATAL:', err); process.exit(1); });