// Chat Gusogst 暖色调主题
export const colors = {
  // 背景色 - 米黄色系
  bg: '#FFF8F0',           // 主背景（米白）
  bgSecondary: '#FFF0E0',  // 次背景（浅杏）
  bgTertiary: '#FFE8D0',   // 三级背景
  bgCard: '#FFF5EB',       // 卡片背景
  bgInput: '#FFF0E5',      // 输入框背景
  
  // 强调色 - 粉色系（从 Lovemo 借鉴）
  accent: '#FF9CBE',       // 主强调色（粉）
  accentLight: '#FFD6E7',  // 浅粉
  accentDark: '#E87A9E',   // 深粉
  
  // 文字色
  textPrimary: '#2D1B0E',  // 主文字（深棕）
  textSecondary: '#8B7355', // 次文字（暖灰）
  textTertiary: '#B8A080',  // 三级文字
  textInverse: '#FFFFFF',   // 反色文字
  
  // 聊天气泡
  bubbleUser: '#FF9CBE',    // 用户气泡（粉）
  bubbleUserText: '#FFFFFF',
  bubbleAI: '#FFFFFF',       // AI 气泡（白）
  bubbleAIText: '#2D1B0E',
  
  // 边框和分割线
  border: '#F0DCC8',
  divider: '#F5E6D3',
  
  // 功能色
  success: '#7BC67E',
  warning: '#F5C542',
  error: '#E85D5D',
  
  // 状态
  online: '#7BC67E',
  typing: '#FF9CBE',
};

type ColorKey = keyof typeof colors;
export const c = (key: ColorKey) => colors[key];
