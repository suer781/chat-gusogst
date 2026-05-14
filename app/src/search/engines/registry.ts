// ============================================
// 搜索引擎注册表 — 所有预置引擎的元数据
// ============================================

/** 引擎分类 */
export type EngineCategory = 'general' | 'vertical' | 'cloud' | 'builtin_api' | 'custom_api'

/** 引擎元数据 */
export interface EngineMeta {
  id: string
  name: string
  category: EngineCategory
  searchUrl: string        // %s 会被替换为 URL 编码的搜索词
  needsKey: boolean
  supportsTimeRange: boolean
  supportsLanguage: boolean
  description: string
  excludeDomains?: string[]
  headers?: Record<string, string>
}

// ─────────────────────────────────────────
// 通用及传统网页搜索引擎
// ─────────────────────────────────────────

const GENERAL_ENGINES: EngineMeta[] = [
  {
    id: 'baidu',
    name: '百度搜索',
    category: 'general',
    searchUrl: 'https://www.baidu.com/s?wd=%s',
    needsKey: false,
    supportsTimeRange: true,
    supportsLanguage: true,
    description: '国内市场占有率最高的搜索引擎',
    excludeDomains: ['baidu.com', 'baidustatic.com', 'bdimg.com'],
  },
  {
    id: 'bing',
    name: '必应 Bing',
    category: 'general',
    searchUrl: 'https://www.bing.com/search?q=%s',
    needsKey: false,
    supportsTimeRange: true,
    supportsLanguage: true,
    description: '微软旗下，搜索结果质量较高',
    excludeDomains: ['bing.com', 'microsoft.com', 'bing.net'],
  },
  {
    id: 'so360',
    name: '360 搜索',
    category: 'general',
    searchUrl: 'https://www.so.com/s?q=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '国内主流搜索引擎之一',
    excludeDomains: ['360.cn', 'so.com', 'qhimg.com', 'qhres.com'],
  },
  {
    id: 'sogou',
    name: '搜狗搜索',
    category: 'general',
    searchUrl: 'https://www.sogou.com/web?query=%s',
    needsKey: false,
    supportsTimeRange: true,
    supportsLanguage: true,
    description: '搜狐旗下，尤其适合中文搜索',
    excludeDomains: ['sogou.com', 'sogoucdn.com'],
  },
  {
    id: 'shenma',
    name: '神马搜索',
    category: 'general',
    searchUrl: 'https://m.sm.cn/s?q=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: 'UC和阿里巴巴联合推出的移动端搜索引擎',
    excludeDomains: ['sm.cn', 'uc.cn', 'alicdn.com'],
  },
  {
    id: 'toutiao',
    name: '头条搜索',
    category: 'general',
    searchUrl: 'https://m.toutiao.com/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '字节跳动旗下，聚合了头条系的资讯内容',
    excludeDomains: ['toutiao.com', 'bytedance.com', 'snssdk.com', 'byteimg.com'],
  },
  {
    id: 'chinaso',
    name: '中国搜索',
    category: 'general',
    searchUrl: 'http://www.chinaso.com/search?q=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '由国内多家权威媒体联合推出的搜索引擎',
    excludeDomains: ['chinaso.com'],
  },
  {
    id: 'quark',
    name: '夸克搜索',
    category: 'general',
    searchUrl: 'https://quark.sm.cn/s?q=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '阿里巴巴旗下的智能搜索，也可通过网页访问',
    excludeDomains: ['sm.cn', 'quark.cn', 'uc.cn'],
    headers: {
      'User-Agent': 'Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36',
    },
  },
  {
    id: 'duckduckgo',
    name: 'DuckDuckGo',
    category: 'general',
    searchUrl: 'https://html.duckduckgo.com/html/?q=%s',
    needsKey: false,
    supportsTimeRange: true,
    supportsLanguage: true,
    description: '注重隐私的搜索引擎，不追踪用户',
    excludeDomains: ['duckduckgo.com'],
  },
  {
    id: 'brave',
    name: 'Brave Search',
    category: 'general',
    searchUrl: 'https://search.brave.com/search?q=%s',
    needsKey: false,
    supportsTimeRange: true,
    supportsLanguage: true,
    description: '独立搜索引擎，注重隐私和去中心化',
    excludeDomains: ['brave.com'],
  },
]

// ─────────────────────────────────────────
// 垂直领域与 AI 搜索
// ─────────────────────────────────────────

const VERTICAL_ENGINES: EngineMeta[] = [
  {
    id: 'baidu_dev',
    name: '百度开发者搜索',
    category: 'vertical',
    searchUrl: 'https://kaifa.baidu.com/search?wd=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '专注技术及编程领域，来源博客园、CSDN、知乎等',
    excludeDomains: ['baidu.com'],
  },
  {
    id: 'bocha',
    name: '博查 AI 搜索',
    category: 'vertical',
    searchUrl: 'https://bochaai.com/search?q=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '面向AI的搜索引擎，为开发者提供Search API',
    excludeDomains: ['bochaai.com'],
  },
  {
    id: 'weixin',
    name: '微信搜一搜',
    category: 'vertical',
    searchUrl: 'https://weixin.sogou.com/weixin?type=2&query=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '通过搜狗引擎，独家检索微信公众号及文章内容',
    excludeDomains: ['sogou.com', 'weixin.qq.com'],
  },
  {
    id: 'baidu_academic',
    name: '百度学术',
    category: 'vertical',
    searchUrl: 'https://xueshu.baidu.com/s?wd=%s',
    needsKey: false,
    supportsTimeRange: true,
    supportsLanguage: true,
    description: '检索学术论文、期刊、专利等专业文献资料',
    excludeDomains: ['baidu.com'],
  },
  {
    id: 'zhihu',
    name: '知乎搜索',
    category: 'vertical',
    searchUrl: 'https://www.zhihu.com/search?type=content&q=%s',
    needsKey: false,
    supportsTimeRange: true,
    supportsLanguage: true,
    description: '聚焦高质量的问答内容和专栏文章',
    excludeDomains: ['zhihu.com', 'zhimg.com'],
  },
  {
    id: 'smzdm',
    name: '什么值得买',
    category: 'vertical',
    searchUrl: 'https://search.smzdm.com/?c=home&s=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '搜索全网商品优惠、评测和购物攻略',
    excludeDomains: ['smzdm.com'],
  },
  {
    id: '58',
    name: '58同城',
    category: 'vertical',
    searchUrl: 'https://cn.58.com/sou/?key=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '本地生活服务：招聘、租房、二手物品等',
    excludeDomains: ['58.com', '58cdn.com'],
  },
]

// ─────────────────────────────────────────
// 网盘资源搜索引擎
// ─────────────────────────────────────────

const CLOUD_ENGINES: EngineMeta[] = [
  {
    id: 'panclub',
    name: '网盘俱乐部',
    category: 'cloud',
    searchUrl: 'https://pan.club/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '网盘资源聚合搜索',
    excludeDomains: ['pan.club'],
  },
  {
    id: 'pandashi',
    name: '盘大师',
    category: 'cloud',
    searchUrl: 'https://www.pandashi8.com/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '网盘资源搜索',
    excludeDomains: ['pandashi8.com'],
  },
  {
    id: 'pansou',
    name: 'Pansou',
    category: 'cloud',
    searchUrl: 'https://so.252035.xyz/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '网盘资源搜索引擎',
    excludeDomains: ['252035.xyz'],
  },
  {
    id: 'haisou',
    name: '海搜',
    category: 'cloud',
    searchUrl: 'https://haisou.cc/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '网盘资源搜索',
    excludeDomains: ['haisou.cc'],
  },
  {
    id: 'xuebapan',
    name: '学霸盘',
    category: 'cloud',
    searchUrl: 'https://www.xuebapan.com/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '学习资源网盘搜索',
    excludeDomains: ['xuebapan.com'],
  },
  {
    id: 'xiongdipan',
    name: '兄弟盘',
    category: 'cloud',
    searchUrl: 'https://xiongdipan.com/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '网盘资源搜索',
    excludeDomains: ['xiongdipan.com'],
  },
  {
    id: 'duanjuso',
    name: '短剧搜',
    category: 'cloud',
    searchUrl: 'https://www.duanjuso.cc/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '短剧资源搜索',
    excludeDomains: ['duanjuso.cc'],
  },
  {
    id: 'yunso',
    name: '小云搜索',
    category: 'cloud',
    searchUrl: 'https://www.yunso.net/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '网盘资源聚合搜索',
    excludeDomains: ['yunso.net'],
  },
  {
    id: 'qupansou',
    name: '趣盘搜',
    category: 'cloud',
    searchUrl: 'https://pan.funletu.com/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '网盘资源搜索',
    excludeDomains: ['funletu.com'],
  },
  {
    id: 'pikasou',
    name: '皮卡搜索',
    category: 'cloud',
    searchUrl: 'https://www.pikasoo.top/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '网盘资源搜索',
    excludeDomains: ['pikasoo.top'],
  },
  {
    id: 'xiaobaipan',
    name: '小白盘',
    category: 'cloud',
    searchUrl: 'https://www.xiaobaipan.com/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '网盘资源搜索',
    excludeDomains: ['xiaobaipan.com'],
  },
  {
    id: 'liangyiniao',
    name: '两仪鸟搜索',
    category: 'cloud',
    searchUrl: 'https://www.liangyiniao.com/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '网盘资源搜索',
    excludeDomains: ['liangyiniao.com'],
  },
  {
    id: 'yunpanem',
    name: '云盘恶魔',
    category: 'cloud',
    searchUrl: 'https://www.yunpanem.com/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '网盘资源搜索',
    excludeDomains: ['yunpanem.com'],
  },
  {
    id: 'woaisoupan',
    name: '我爱搜盘',
    category: 'cloud',
    searchUrl: 'https://www.woaisoupan.com/search?keyword=%s',
    needsKey: false,
    supportsTimeRange: false,
    supportsLanguage: true,
    description: '网盘资源搜索',
    excludeDomains: ['woaisoupan.com'],
  },
]

// ─────────────────────────────────────────
// 汇总导出（不包含 custom_api，那些由用户动态添加）
// ─────────────────────────────────────────

export const ALL_ENGINES: EngineMeta[] = [
  ...GENERAL_ENGINES,
  ...VERTICAL_ENGINES,
  ...CLOUD_ENGINES,
]

/** 按 id 快速查找 */
export const ENGINE_MAP = new Map<string, EngineMeta>()
for (const e of ALL_ENGINES) ENGINE_MAP.set(e.id, e)

/** 按分类获取 */
export function getEnginesByCategory(category: EngineCategory): EngineMeta[] {
  return ALL_ENGINES.filter(e => e.category === category)
}

/** 人设默认引擎链（不设置时走这个） */
export const DEFAULT_ENGINE_CHAIN = ['duckduckgo', 'baidu', 'bing', 'brave']

/** 预设引擎组合（人设可直接选用） */
export const PRESETS: Record<string, { name: string; engines: string[] }> = {
  general:   { name: '通用搜索', engines: ['duckduckgo', 'baidu', 'bing', 'brave'] },
  china:     { name: '国内优先', engines: ['baidu', 'bing', 'sogou', 'so360'] },
  tech:      { name: '技术向', engines: ['baidu_dev', 'duckduckgo', 'zhihu'] },
  academic:  { name: '学术向', engines: ['baidu_academic', 'duckduckgo'] },
  shopping:  { name: '购物向', engines: ['smzdm', 'baidu', 'bing'] },
  cloud:     { name: '网盘资源', engines: ['pansou', 'panclub', 'xiongdipan', 'pikasou'] },
  privacy:   { name: '隐私优先', engines: ['duckduckgo', 'brave'] },
}
