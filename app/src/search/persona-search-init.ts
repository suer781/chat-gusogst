/**
 * 人设搜索引擎智能初始化
 * 
 * 核心流程：
 * 1. extractTags — 标签提取（内容领域 + 语气风格）
 * 2. extractScenarios — 场景识别
 * 3. recommendEngines — 引擎推荐（基于标签 + 引擎特征库）
 * 4. inferSampling — 参数判断（基于语气风格 + 领域修正）
 * 5. autoOptimizeWithLLM — LLM 深度分析（可选，提取结构化人设字段）
 */

import type {
  PersonaSearchConfig,
  PersonaSamplingConfig,
  PersonaStructuredProfile,
} from '../shared/types'

// ============================================
// 类型定义
// ============================================

export interface PersonaAnalysis {
  tags: string[]
  scenarios: string[]
  recommendedEngines: string[]
  engineReasons: Record<string, string>
  concurrency: number
  enableSearch: boolean
  enableTimeRange: boolean
  sampling: PersonaSamplingConfig
  /** 结构化人设档案（LLM 分析时填充） */
  structured?: PersonaStructuredProfile
}

interface EngineProfile {
  id: string
  strengths: string[]
  weaknesses?: string[]
  languages: 'zh' | 'en' | 'both'
  qualityScore: number
  speedScore: number
}

// ============================================
// 引擎特征库（30 个引擎全覆盖）
// ============================================

const ENGINE_PROFILES: EngineProfile[] = [
  // ── 通用搜索引擎 ──
  {
    id: 'duckduckgo',
    strengths: ['隐私', '国际', '技术', '英文内容', '开源', '学术', '编程'],
    weaknesses: ['中文内容'],
    languages: 'both',
    qualityScore: 8,
    speedScore: 7,
  },
  {
    id: 'bing',
    strengths: ['通用', '技术', '国际', '英文内容', '学术', '编程', '微软'],
    languages: 'both',
    qualityScore: 7,
    speedScore: 8,
  },
  {
    id: 'baidu',
    strengths: ['中文', '通用', '国内', '百科', '生活', '贴吧', '经验'],
    weaknesses: ['英文内容', '隐私'],
    languages: 'zh',
    qualityScore: 6,
    speedScore: 8,
  },
  {
    id: 'brave',
    strengths: ['隐私', '技术', '独立', '开源', '去中心化'],
    weaknesses: ['中文内容'],
    languages: 'en',
    qualityScore: 7,
    speedScore: 7,
  },
  {
    id: 'so360',
    strengths: ['国内', '安全', '中文', '生活', '实用'],
    weaknesses: ['英文内容', '隐私'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 7,
  },
  {
    id: 'sogou',
    strengths: ['中文', '微信', '国内', '生活', '输入法'],
    weaknesses: ['英文内容'],
    languages: 'zh',
    qualityScore: 6,
    speedScore: 7,
  },
  {
    id: 'shenma',
    strengths: ['移动端', '中文', '小说', '影视', '娱乐'],
    weaknesses: ['技术', '学术'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 8,
  },
  {
    id: 'toutiao',
    strengths: ['新闻', '资讯', '热点', '中文', '时事', '短视频'],
    weaknesses: ['学术', '技术'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 8,
  },
  {
    id: 'chinaso',
    strengths: ['权威', '官方', '中文', '政务', '政策'],
    weaknesses: ['国际'],
    languages: 'zh',
    qualityScore: 6,
    speedScore: 6,
  },
  {
    id: 'quark',
    strengths: ['移动端', '中文', '智能', '生活', '学习', 'AI'],
    languages: 'zh',
    qualityScore: 6,
    speedScore: 8,
  },

  // ── 垂直领域与 AI 搜索 ──
  {
    id: 'baidu_dev',
    strengths: ['技术', '编程', '开发', '代码', 'GitHub', 'CSDN', '博客', '教程'],
    weaknesses: ['非技术'],
    languages: 'zh',
    qualityScore: 8,
    speedScore: 7,
  },
  {
    id: 'zhihu',
    strengths: ['问答', '深度', '经验', '知乎', '观点', '讨论', '分析', '评测'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 8,
    speedScore: 6,
  },
  {
    id: 'weixin',
    strengths: ['微信', '公众号', '深度', '行业', '自媒体', '原创'],
    weaknesses: ['英文', '实时性'],
    languages: 'zh',
    qualityScore: 7,
    speedScore: 5,
  },
  {
    id: 'baidu_academic',
    strengths: ['学术', '论文', '科研', '专业', '期刊', '文献', '引用'],
    weaknesses: ['通俗内容'],
    languages: 'both',
    qualityScore: 8,
    speedScore: 5,
  },
  {
    id: 'smzdm',
    strengths: ['购物', '优惠', '评测', '消费', '比价', '折扣', '好价'],
    weaknesses: ['非购物'],
    languages: 'zh',
    qualityScore: 7,
    speedScore: 7,
  },
  {
    id: '58',
    strengths: ['本地', '生活服务', '招聘', '租房', '二手', '搬家', '家政'],
    weaknesses: ['技术', '学术'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 7,
  },
  {
    id: 'bocha',
    strengths: ['AI搜索', '中文', '综合', '实时', '问答'],
    languages: 'zh',
    qualityScore: 7,
    speedScore: 7,
  },

  // ── 网盘资源搜索引擎 ──
  {
    id: 'pansou',
    strengths: ['网盘', '资源', '下载', '电影', '软件', '电子书', '课程'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 6,
    speedScore: 7,
  },
  {
    id: 'panclub',
    strengths: ['网盘', '资源', '下载', '分享', '社区'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 6,
    speedScore: 7,
  },
  {
    id: 'xiongdipan',
    strengths: ['网盘', '资源', '影视', '动漫'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 7,
  },
  {
    id: 'haisou',
    strengths: ['网盘', '资源', '搜索', '聚合'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 7,
  },
  {
    id: 'xuebapan',
    strengths: ['网盘', '学习', '教育', '课程', '教材', '考试'],
    weaknesses: ['娱乐'],
    languages: 'zh',
    qualityScore: 6,
    speedScore: 6,
  },
  {
    id: 'duanjuso',
    strengths: ['短剧', '影视', '视频', '网剧'],
    weaknesses: ['非影视'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 7,
  },
  {
    id: 'yunso',
    strengths: ['网盘', '资源', '聚合', '综合'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 7,
  },
  {
    id: 'qupansou',
    strengths: ['网盘', '资源', '搜索', '娱乐'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 7,
  },
  {
    id: 'pikasou',
    strengths: ['网盘', '资源', '搜索', '影视'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 7,
  },
  {
    id: 'xiaobaipan',
    strengths: ['网盘', '资源', '简洁', '快速'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 7,
  },
  {
    id: 'liangyiniao',
    strengths: ['网盘', '资源', '搜索'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 6,
  },
  {
    id: 'yunpanem',
    strengths: ['网盘', '资源', '搜索', '聚合'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 6,
  },
  {
    id: 'woaisoupan',
    strengths: ['网盘', '资源', '搜索', '综合'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 6,
  },
  {
    id: 'pandashi',
    strengths: ['网盘', '资源', '搜索', '整理'],
    weaknesses: ['实时性'],
    languages: 'zh',
    qualityScore: 5,
    speedScore: 6,
  },
]

const ENGINE_PROFILE_MAP = new Map<string, EngineProfile>(
  ENGINE_PROFILES.map(e => [e.id, e])
)

// ============================================
// 标签提取
// ============================================

/** 内容领域关键词 → 标签 */
const DOMAIN_KEYWORDS: Record<string, string[]> = {
  '技术': ['技术', '编程', '开发', '代码', '程序员', 'debug', 'bug', '算法', 'API', '框架', 'SDK', '前端', '后端', '全栈', '运维', 'DevOps'],
  'AI': ['AI', '人工智能', '机器学习', '深度学习', 'GPT', 'LLM', '大模型', '神经网络', 'NLP', 'ChatGPT', 'Claude', 'Gemini'],
  '学术': ['学术', '论文', '科研', '研究', '文献', '期刊', '引用', '综述', '实验', '假设', '方法论'],
  '购物': ['购物', '优惠', '折扣', '比价', '评测', '推荐', '好物', '划算', '性价比', '种草', '拔草'],
  '美食': ['美食', '菜谱', '烹饪', '做饭', '餐厅', '好吃', '食谱', '食材', '口味'],
  '旅行': ['旅行', '旅游', '攻略', '景点', '酒店', '机票', '行程', '签证', '出境', '自由行'],
  '影视': ['电影', '电视剧', '动漫', '综艺', '追剧', '好看', '推荐', '剧情', '演员', '导演', '短剧'],
  '游戏': ['游戏', '攻略', '手游', '端游', 'steam', 'switch', 'PS5', 'Xbox', '电竞', '氪金'],
  '金融': ['股票', '基金', '投资', '理财', '行情', 'K线', '涨跌', '收益', '风险', 'A股', '港股', '美股'],
  '新闻': ['新闻', '热搜', '头条', '资讯', '热点', '时事', '突发', '快讯'],
  '学习': ['学习', '教程', '课程', '考试', '复习', '知识', '笔记', '教材', '网课', '自学'],
  '编程': ['Python', 'Java', 'JavaScript', 'TypeScript', 'Go', 'Rust', 'C++', 'React', 'Vue', 'Node', 'Docker', 'K8s', 'Git'],
  '写作': ['写作', '文案', '文章', '创作', '小说', '故事', '剧本', '散文', '诗歌'],
  '翻译': ['翻译', 'translate', '英译中', '中译英', '日语', '韩语', '英语'],
  '情感': ['情感', '恋爱', '感情', '对象', '暗恋', '表白', '分手', '挽回', '暧昧', '约会', '恋人'],
  '健康': ['健康', '运动', '健身', '减肥', '饮食', '睡眠', '心理', '焦虑', '抑郁', '养生'],
  '音乐': ['音乐', '歌曲', '歌单', '演唱会', '乐器', '钢琴', '吉他', '乐队'],
  '摄影': ['摄影', '拍照', '相机', '修图', '调色', '构图', '镜头'],
  '职场': ['职场', '面试', '简历', '跳槽', '升职', '加薪', '工作', '同事', '领导'],
  '宠物': ['宠物', '猫', '狗', '养猫', '养狗', '铲屎', '萌宠'],
  '育儿': ['育儿', '宝宝', '孩子', '带娃', '辅食', '早教', '幼儿园'],
  '法律': ['法律', '合同', '纠纷', '维权', '诉讼', '律师', '法规'],
  '网盘资源': ['网盘', '资源', '下载', '百度云', '阿里云盘', '夸克网盘', '链接', '提取码'],
  '软件': ['软件', '工具', 'APP', '插件', '脚本', '效率', '自动化'],
  '问答': ['问答', '解答', '为什么', '怎么回事', '怎么解决', '求助'],
  '中文': ['中文', '国内', '中国', '国内网站', '中文内容'],
  '英文': ['英文', '英语', '国外', '海外', 'international'],
  '生活': ['生活', '日常', '居家', '收纳', '打扫', '装修', '租房', '买房'],
  '汽车': ['汽车', '车', '驾照', '保养', '保险', '新能源', '电车', '特斯拉', '小米汽车'],
  '数码': ['手机', '电脑', '平板', '耳机', '相机', '数码', '测评', '开箱', '小米', '华为', '苹果'],
}

/** 语气/性格风格关键词 → 标签 */
const TONE_KEYWORDS: Record<string, string[]> = {
  '温柔语气': ['温柔', '体贴', '轻声细语', '关心', '温暖', '柔和', '安抚', '宝贝', '亲爱的', '心疼'],
  '傲娇语气': ['傲娇', '才不是', '哼', '别误会', '不是因为', '讨厌', '谁要', '勉强', '笨蛋'],
  '元气语气': ['活泼', '开朗', '正能量', '元气', '兴奋', '冲鸭', '加油', '超棒', '厉害', '！', '哈哈'],
  '冷酷语气': ['冷酷', '高冷', '不屑', '无所谓', '随你', '无聊', '幼稚', '呵'],
  '理性语气': ['理性', '逻辑', '分析', '客观', '数据', '事实', '推理', '论证', '严谨'],
  '幽默语气': ['幽默', '搞笑', '段子', '沙雕', '逗比', '笑死', '梗', '调侃', '皮一下'],
  '文艺语气': ['文艺', '诗意', '浪漫', '星辰', '月光', '微风', '岁月', '温柔以待', '故事'],
  '深夜语气': ['深夜', '夜晚', '安静', '失眠', '心事', '低语', '枕边', '夜色', '晚安'],
  '御姐语气': ['御姐', '女王', '成熟', '霸气', '姐姐', '主导', '掌控', '自信'],
  '软萌语气': ['软萌', '可爱', '嘤嘤嘤', '呜呜', '人家', '嘛', '鸭', '呀', '嘻嘻'],
  '病娇语气': ['病娇', '占有', '只属于', '不准离开', '永远在一起', '监视', '嫉妒', '疯狂'],
}

/** 提取标签 */
export function extractTags(prompt: string): string[] {
  const tags: string[] = []
  const lowerPrompt = prompt.toLowerCase()

  // 提取内容领域标签
  for (const [tag, keywords] of Object.entries(DOMAIN_KEYWORDS)) {
    if (keywords.some(kw => lowerPrompt.includes(kw.toLowerCase()))) {
      tags.push(tag)
    }
  }

  // 提取语气风格标签
  for (const [tag, keywords] of Object.entries(TONE_KEYWORDS)) {
    if (keywords.some(kw => lowerPrompt.includes(kw.toLowerCase()))) {
      tags.push(tag)
    }
  }

  // 默认标签
  if (tags.length === 0) {
    tags.push('通用', '中文')
  }

  return [...new Set(tags)]
}

// ============================================
// 场景识别
// ============================================

const SCENARIO_PATTERNS: Record<string, string[]> = {
  '信息检索': ['查', '搜索', '找', '什么是', '介绍一下', '了解', '资料'],
  '实时资讯': ['最新', '今天', '新闻', '热搜', '最近', '刚刚', '快讯'],
  '产品评测': ['评测', '对比', '哪个好', '值得买', '推荐', '性价比', '测评'],
  '学习教程': ['教程', '学习', '怎么用', '入门', '进阶', '课程', '知识'],
  '问题解决': ['怎么办', '解决', '报错', '问题', '故障', '修复', '排查'],
  '创意灵感': ['创意', '灵感', '设计', '文案', '写一个', '帮我写', '创作'],
  '日常陪伴': ['聊聊', '陪我', '无聊', '心情', '今天', '感觉', '想你'],
  '情绪安慰': ['难过', '伤心', '焦虑', '压力', '烦', '不开心', '安慰'],
  '深度探讨': ['深度', '讨论', '观点', '看法', '分析', '思考', '哲学'],
  '资源下载': ['下载', '资源', '网盘', '链接', '种子', '电影资源'],
}

export function extractScenarios(prompt: string): string[] {
  const scenarios: string[] = []

  for (const [scenario, keywords] of Object.entries(SCENARIO_PATTERNS)) {
    if (keywords.some(kw => prompt.includes(kw))) {
      scenarios.push(scenario)
    }
  }

  if (scenarios.length === 0) {
    scenarios.push('信息检索', '日常陪伴')
  }

  return scenarios
}

// ============================================
// 引擎推荐
// ============================================

export function recommendEngines(
  tags: string[],
  scenarios: string[],
  maxEngines = 6,
): { engines: string[]; reasons: Record<string, string> } {
  const scores: { id: string; score: number; reason: string }[] = []

  for (const profile of ENGINE_PROFILES) {
    let score = 0
    const matchedStrengths: string[] = []

    // 标签匹配优势
    for (const tag of tags) {
      if (profile.strengths.some(s => s.includes(tag) || tag.includes(s))) {
        score += 3
        matchedStrengths.push(tag)
      }
    }

    // 场景匹配
    for (const scenario of scenarios) {
      if (profile.strengths.some(s => scenario.includes(s) || s.includes(scenario))) {
        score += 2
      }
    }

    // 加权：质量分 + 速度分
    score += profile.qualityScore * 0.5 + profile.speedScore * 0.3

    // 语言匹配：中文标签多 → 偏好中文引擎
    const zhTags = tags.filter(t => ['中文', '国内', '技术', '学术', '购物', '美食', '影视', '游戏', '金融', '学习', '生活', '职场', '育儿', '法律', '网盘资源', '软件'].includes(t))
    if (zhTags.length > 2 && profile.languages === 'zh') score += 2
    if (zhTags.length <= 1 && profile.languages === 'en') score += 1

    // 弱势惩罚
    if (profile.weaknesses) {
      for (const tag of tags) {
        if (profile.weaknesses.some(w => w.includes(tag) || tag.includes(w))) {
          score -= 2
        }
      }
    }

    if (score > 0) {
      const reason = matchedStrengths.length > 0
        ? `擅长${matchedStrengths.join('、')}相关内容`
        : `综合质量${profile.qualityScore}分，速度${profile.speedScore}分`

      scores.push({ id: profile.id, score, reason })
    }
  }

  // 按分数排序，取 top N
  scores.sort((a, b) => b.score - a.score)
  const selected = scores.slice(0, maxEngines)

  const engines = selected.map(s => s.id)
  const reasons: Record<string, string> = {}
  for (const s of selected) {
    reasons[s.id] = s.reason
  }

  return { engines, reasons }
}

// ============================================
// 参数判断
// ============================================

/** 语气风格 → 采样参数基线 */
const TONE_SAMPLING: Record<string, Partial<PersonaSamplingConfig>> = {
  '温柔语气': { temperature: 0.55, topP: 0.85, presencePenalty: 0.4, frequencyPenalty: 0.2, maxTokens: 600 },
  '傲娇语气': { temperature: 0.85, topP: 0.92, presencePenalty: 0.5, frequencyPenalty: 0.4, maxTokens: 500 },
  '元气语气': { temperature: 0.9, topP: 0.95, presencePenalty: 0.6, frequencyPenalty: 0.3, maxTokens: 700 },
  '冷酷语气': { temperature: 0.3, topP: 0.75, presencePenalty: 0.1, frequencyPenalty: 0.1, maxTokens: 400 },
  '理性语气': { temperature: 0.25, topP: 0.7, presencePenalty: 0.1, frequencyPenalty: 0.1, maxTokens: 800 },
  '幽默语气': { temperature: 1.0, topP: 0.95, presencePenalty: 0.7, frequencyPenalty: 0.3, maxTokens: 600 },
  '文艺语气': { temperature: 0.8, topP: 0.9, presencePenalty: 0.5, frequencyPenalty: 0.2, maxTokens: 800 },
  '深夜语气': { temperature: 0.6, topP: 0.85, presencePenalty: 0.4, frequencyPenalty: 0.2, maxTokens: 700 },
  '御姐语气': { temperature: 0.5, topP: 0.8, presencePenalty: 0.3, frequencyPenalty: 0.2, maxTokens: 500 },
  '软萌语气': { temperature: 0.75, topP: 0.9, presencePenalty: 0.5, frequencyPenalty: 0.3, maxTokens: 500 },
  '病娇语气': { temperature: 0.95, topP: 0.93, presencePenalty: 0.6, frequencyPenalty: 0.4, maxTokens: 600 },
}

/** 领域修正系数 */
const DOMAIN_MODIFIERS: Record<string, Partial<PersonaSamplingConfig>> = {
  '技术': { temperature: -0.1, maxTokens: 200 },
  '学术': { temperature: -0.15, maxTokens: 300 },
  '编程': { temperature: -0.1, maxTokens: 200 },
  '金融': { temperature: -0.1, maxTokens: 200 },
  '法律': { temperature: -0.1, maxTokens: 200 },
  '写作': { temperature: 0.1, presencePenalty: 0.1, maxTokens: 200 },
  '翻译': { temperature: -0.05, maxTokens: 100 },
  '情感': { temperature: 0.05, presencePenalty: 0.05 },
  '创意灵感': { temperature: 0.15, presencePenalty: 0.15 },
}

export function inferSampling(tags: string[]): PersonaSamplingConfig {
  // 默认基线
  let config: PersonaSamplingConfig = {
    temperature: 0.7,
    topP: 0.9,
    presencePenalty: 0.3,
    frequencyPenalty: 0.2,
    maxTokens: 600,
  }

  // 找到匹配的语气风格，应用基线
  for (const tag of tags) {
    if (TONE_SAMPLING[tag]) {
      config = { ...config, ...TONE_SAMPLING[tag] }
      break // 只取第一个匹配的语气
    }
  }

  // 领域修正叠加
  for (const tag of tags) {
    if (DOMAIN_MODIFIERS[tag]) {
      const mod = DOMAIN_MODIFIERS[tag]
      if (mod.temperature) config.temperature = Math.max(0.1, Math.min(1.2, config.temperature + mod.temperature))
      if (mod.topP) config.topP = Math.max(0.5, Math.min(1.0, config.topP + mod.topP))
      if (mod.presencePenalty) config.presencePenalty = Math.max(0, Math.min(2.0, config.presencePenalty + mod.presencePenalty))
      if (mod.frequencyPenalty) config.frequencyPenalty = Math.max(0, Math.min(2.0, config.frequencyPenalty + mod.frequencyPenalty))
      if (mod.maxTokens) config.maxTokens = Math.max(200, Math.min(2000, config.maxTokens + mod.maxTokens))
    }
  }

  // 精度修正
  config.temperature = Math.round(config.temperature * 100) / 100
  config.topP = Math.round(config.topP * 100) / 100
  config.presencePenalty = Math.round(config.presencePenalty * 100) / 100
  config.frequencyPenalty = Math.round(config.frequencyPenalty * 100) / 100

  return config
}

// ============================================
// 完整分析入口
// ============================================

/**
 * 分析人设系统提示词，生成完整分析结果
 * 纯关键词匹配，无需 LLM，适用于初始化和快速分析
 */
export function analyzePersona(prompt: string): PersonaAnalysis {
  const tags = extractTags(prompt)
  const scenarios = extractScenarios(prompt)
  const { engines, reasons } = recommendEngines(tags, scenarios)
  const sampling = inferSampling(tags)

  // 是否需要搜索：有信息检索/实时资讯/产品评测等场景时开启
  const searchScenarios = ['信息检索', '实时资讯', '产品评测', '资源下载', '问题解决']
  const enableSearch = scenarios.some(s => searchScenarios.includes(s))

  // 是否需要时间范围：有实时资讯场景时开启
  const enableTimeRange = scenarios.includes('实时资讯')

  // 并发数：标签多说明需求多样，可以多开
  const concurrency = Math.min(4, Math.max(2, Math.ceil(tags.length / 3)))

  return {
    tags,
    scenarios,
    recommendedEngines: engines,
    engineReasons: reasons,
    concurrency,
    enableSearch,
    enableTimeRange,
    sampling,
  }
}

// ============================================
// 分析结果 → 搜索配置
// ============================================

export function analysisToSearchConfig(analysis: PersonaAnalysis): PersonaSearchConfig {
  const engineWeights: Record<string, number> = {}
  analysis.recommendedEngines.forEach((id, i) => {
    // 权重递减：第一个最高
    engineWeights[id] = Math.max(0.3, 1 - i * 0.15)
  })

  return {
    engines: analysis.recommendedEngines,
    engineWeights,
    concurrency: analysis.concurrency,
    enableTimeRange: analysis.enableTimeRange,
    enableSearch: analysis.enableSearch,
  }
}

// ============================================
// LLM 深度分析（自动优化核心）
// ============================================

/**
 * 构建 LLM 分析提示词
 * 让 LLM 理解系统提示词，提取结构化人设字段
 */
export function buildLLMAnalysisPrompt(systemPrompt: string): string {
  return `你是一个 AI 人设分析专家。请分析以下系统提示词，提取结构化的人设信息。

## 系统提示词
"""
${systemPrompt}
"""

## 请提取以下信息（JSON 格式输出）

\`\`\`json
{
  "tone": "语气风格（如：温柔、傲娇、元气、冷酷、理性、幽默、文艺、深夜、御姐、软萌、病娇，或自定义描述）",
  "personality": ["性格特征1", "性格特征2", "性格特征3"],
  "verbalHabits": ["口头禅1", "口头禅2", "口头禅3"],
  "speakingStyle": "说话风格（如：简短直接、长句抒情、emoji丰富、口语化、文雅、俏皮）",
  "relationship": "关系设定（如：恋人、朋友、老师、助手、妹妹、姐姐）",
  "scenario": "主要场景（如：日常陪伴、深夜谈心、学习辅导、情绪安慰、技术讨论）",
  "nickname": "AI 对用户的称呼（如：亲爱的、宝贝、主人、大佬、同学）",
  "userNickname": "用户对 AI 的称呼（如：老婆、小助手、老师）",
  "restrictions": ["禁忌1", "禁忌2"],
  "suggestedTags": ["标签1", "标签2", "标签3"],
  "toneDetail": "语气的详细描述（一两句话，描述说话的具体感觉）"
}
\`\`\`

注意：
- 如果提示词中没有明确提到某个字段，根据整体语气和风格合理推断
- verbalHabits 提取最能代表该人设说话特点的表达（如果没有明显的口头禅，可以留空数组）
- restrictions 提取提示词中的限制/禁忌（如"不要用括号描述动作"）
- 只输出 JSON，不要其他内容`
}

/**
 * 解析 LLM 分析响应
 */
export function parseLLMAnalysisResponse(response: string): PersonaStructuredProfile | null {
  try {
    // 提取 JSON 块
    const jsonMatch = response.match(/```json\s*([\s\S]*?)```/) || response.match(/\{[\s\S]*\}/)
    if (!jsonMatch) return null

    const jsonStr = jsonMatch[1] || jsonMatch[0]
    const data = JSON.parse(jsonStr)

    return {
      tone: data.tone || '',
      personality: Array.isArray(data.personality) ? data.personality : [],
      verbalHabits: Array.isArray(data.verbalHabits) ? data.verbalHabits : [],
      speakingStyle: data.speakingStyle || '',
      relationship: data.relationship || '',
      scenario: data.scenario || '',
      nickname: data.nickname || '',
      userNickname: data.userNickname || '',
      restrictions: Array.isArray(data.restrictions) ? data.restrictions : [],
    }
  } catch {
    return null
  }
}

/**
 * 合并 LLM 分析结果到现有分析
 * LLM 结果用于填充结构化字段，关键词分析结果用于引擎推荐和参数
 */
export function mergeAnalysis(
  keywordAnalysis: PersonaAnalysis,
  llmProfile: PersonaStructuredProfile,
): PersonaAnalysis {
  // 用 LLM 提取的标签补充关键词标签
  const mergedTags = [...new Set([
    ...keywordAnalysis.tags,
    ...(llmProfile.tone ? [llmProfile.tone + '语气'] : []),
    ...llmProfile.personality,
  ])]

  // 用合并后的标签重新推荐引擎和参数
  const { engines, reasons } = recommendEngines(mergedTags, keywordAnalysis.scenarios)
  const sampling = inferSampling(mergedTags)

  return {
    ...keywordAnalysis,
    tags: mergedTags,
    recommendedEngines: engines,
    engineReasons: reasons,
    sampling,
    structured: llmProfile,
  }
}

// ============================================
// UI 辅助函数
// ============================================

const TAG_EMOJI: Record<string, string> = {
  '技术': '💻', 'AI': '🤖', '学术': '📚', '购物': '🛒', '美食': '🍜',
  '旅行': '✈️', '影视': '🎬', '游戏': '🎮', '金融': '📈', '新闻': '📰',
  '学习': '📖', '编程': '⌨️', '写作': '✍️', '翻译': '🌐', '情感': '💕',
  '健康': '💪', '音乐': '🎵', '摄影': '📷', '职场': '💼', '宠物': '🐱',
  '育儿': '👶', '法律': '⚖️', '网盘资源': '📁', '软件': '🔧', '问答': '❓',
  '生活': '🏠', '汽车': '🚗', '数码': '📱',
  '温柔语气': '💕', '傲娇语气': '😤', '元气语气': '✨', '冷酷语气': '❄️',
  '理性语气': '🧠', '幽默语气': '😂', '文艺语气': '🌸', '深夜语气': '🌙',
  '御姐语气': '👑', '软萌语气': '🐰', '病娇语气': '🖤',
}

export function getTagEmoji(tag: string): string {
  return TAG_EMOJI[tag] || '🏷️'
}

/** 采样配置 → 中文描述 */
export function describeSampling(config: PersonaSamplingConfig): string {
  const { temperature } = config
  if (temperature <= 0.3) return '精确克制 · 严谨一致'
  if (temperature <= 0.5) return '稳定可靠 · 细致周到'
  if (temperature <= 0.65) return '均衡自然 · 温暖亲切'
  if (temperature <= 0.8) return '灵活多变 · 话题丰富'
  if (temperature <= 0.95) return '活泼跳跃 · 创意十足'
  return '天马行空 · 最大创意'
}

/** 结构化档案 → 可读摘要 */
export function describeStructured(profile: PersonaStructuredProfile): string {
  const parts: string[] = []
  if (profile.tone) parts.push(`语气：${profile.tone}`)
  if (profile.personality.length) parts.push(`性格：${profile.personality.join('、')}`)
  if (profile.verbalHabits.length) parts.push(`口头禅：${profile.verbalHabits.join('、')}`)
  if (profile.speakingStyle) parts.push(`风格：${profile.speakingStyle}`)
  if (profile.relationship) parts.push(`关系：${profile.relationship}`)
  if (profile.nickname) parts.push(`称呼：${profile.nickname}`)
  return parts.join(' · ')
}
