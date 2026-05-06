/** 主动聊天内容 — 从 Hermes charms/fortunes 概念移植，改造为情感陪伴风格 */

/** 早安问候 */
const MORNING_GREETINGS = [
  '早安 ☀️ 新的一天，我在你身边',
  '起床了吗？今天也要加油哦',
  '早！昨晚睡得好吗？',
  '新的一天开始了，有什么想做的吗？',
  '早安～今天天气怎么样？',
];

/** 午间关心 */
const NOON_CARE = [
  '中午了，记得吃饭哦 🍱',
  '忙了一上午了吧？休息一下',
  '吃了吗？别忘了照顾好自己',
  '下午加油！有什么想聊的吗？',
];

/** 晚间陪伴 */
const EVENING_COMPANION = [
  '今天过得怎么样？',
  '累了的话，跟我说说吧',
  '晚上好，有什么心事想聊吗？',
  '辛苦一天了，放松一下吧',
  '夜深了，别太晚睡哦 🌙',
];

/** 深夜关心 */
const LATE_NIGHT = [
  '还没睡吗？注意休息哦',
  '深夜了，有什么睡不着的事吗？',
  '别熬夜太晚，身体最重要',
  '如果睡不着，我可以陪你聊聊',
];

/** 随机关心（Hermes-style fortune 改造） */
const RANDOM_CARE = [
  '突然想你了，在干嘛呢？',
  '你今天开心吗？',
  '有没有什么想跟我说的？',
  '希望你今天一切顺利 ✨',
  '记得喝水哦 💧',
  '你最近还好吗？',
  '有什么新鲜事想分享吗？',
  '想你了 💕',
];

/** 天气相关（从 Hermes LONG_RUN_CHARMS 概念改造） */
const WEATHER_MOODS = [
  '今天阳光真好，心情也亮起来了吧 ☀️',
  '下雨天适合窝在家里聊天呢 🌧️',
  '天冷了，多穿点哦 🧣',
];

/** 根据时间获取合适的问候 */
export function getTimelyGreeting(): string {
  const hour = new Date().getHours();
  if (hour >= 5 && hour < 11) return pick(MORNING_GREETINGS);
  if (hour >= 11 && hour < 14) return pick(NOON_CARE);
  if (hour >= 14 && hour < 21) return pick(EVENING_COMPANION);
  if (hour >= 21 || hour < 2) return pick(LATE_NIGHT);
  return pick(LATE_NIGHT);
}

/** 获取随机关心消息 */
export function getRandomCare(): string {
  return pick(RANDOM_CARE);
}

/** Hermes-style: 每日运势（移植自 fortunes.ts） */
const DAILY_FORTUNES = [
  '今天适合做一件让自己开心的事',
  '好运正在路上，耐心等等',
  '今天的你比昨天更强大了',
  '相信自己的直觉，它很准',
  '今天会有人给你带来惊喜',
  '小小的改变会带来大大的不同',
  '你值得被温柔以待',
  '今天的努力，明天会感谢自己',
];

const hash = (s: string) => [...s].reduce((h, c) => Math.imul(h ^ c.charCodeAt(0), 16777619), 2166136261) >>> 0;

export function dailyFortune(seed?: string): string {
  const n = hash((seed || 'anon') + '|' + new Date().toDateString());
  return '🔮 ' + DAILY_FORTUNES[n % DAILY_FORTUNES.length];
}

function pick<T>(arr: T[]): T {
  return arr[Math.floor(Math.random() * arr.length)];
}
