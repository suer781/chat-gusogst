import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  // 暗色模式 - iPhone 14 尺寸
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('http://localhost:5174/', { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);
  await page.screenshot({ path: '/workspace/ui-dark.png', fullPage: false });
  console.log('暗色截图完成: ui-dark.png');
  
  // 切换到亮色模式
  await page.evaluate(() => {
    document.documentElement.setAttribute('data-theme', 'light');
  });
  await page.waitForTimeout(1000);
  await page.screenshot({ path: '/workspace/ui-light.png', fullPage: false });
  console.log('亮色截图完成: ui-light.png');
  
  // 切到设置页
  await page.click('button.nav-btn:last-child');
  await page.waitForTimeout(1000);
  await page.screenshot({ path: '/workspace/ui-settings.png', fullPage: false });
  console.log('设置页截图完成: ui-settings.png');
  
  await browser.close();
  console.log('全部完成');
})().catch(err => { console.error(err); process.exit(1); });