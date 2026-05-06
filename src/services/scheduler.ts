import AsyncStorage from '@react-native-async-storage/async-storage';
import { Message } from '../types';

const ACTIVITY_KEY = 'cgs_activity_log';
const SCHEDULE_STATE_KEY = 'cgs_schedule_state';

// ── 数据结构 ──────────────────────────────────

/** 每小时活动计数 */
interface HourlyActivity {
  hour: number;   // 0-23
  count: number;  // 该小时发消息次数
  avgReplyMs: number; // 平均回复间隔(ms)
}

/** 活动日志条目 */
interface ActivityRecord {
  timestamp: number;
  isReply: boolean;      // true=回复AI, false=主动发消息
  replyDelayMs?: number; // 如果是回复，从收到AI消息到用户回复的延迟
}

/** 调度状态 */
interface ScheduleState {
  lastProactiveTime: number;
  lastUserActiveTime: number;
  totalProactiveSent: number;
  totalUserReplies: number;   // 用户回复了几次主动消息
  hitRate: number;            // 历史命中率(用户回复/发送)
}

/** 空闲窗口 */
export interface IdleWindow {
  startHour: number;  // 开始小时(含)
  endHour: number;    // 结束小时(含)
  score: number;      // 推荐分数 0-1
  reason: string;     // 推荐理由
}

// ── 核心调度器 ──────────────────────────────────

export class SmartScheduler {
  private activityLog: ActivityRecord[] = [];
  private state: ScheduleState = {
    lastProactiveTime: 0,
    lastUserActiveTime: 0,
    totalProactiveSent: 0,
    totalUserReplies: 0,
    hitRate: 0,
  };

  async init() {
    try {
      const [log, state] = await Promise.all([
        AsyncStorage.getItem(ACTIVITY_KEY),
        AsyncStorage.getItem(SCHEDULE_STATE_KEY),
      ]);
      if (log) this.activityLog = JSON.parse(log);
      if (state) this.state = JSON.parse(state);
    } catch {}
  }

  private async save() {
    // 只保留最近 7 天的记录
    const cutoff = Date.now() - 7 * 24 * 60 * 60 * 1000;
    this.activityLog = this.activityLog.filter(r => r.timestamp > cutoff);
    await Promise.all([
      AsyncStorage.setItem(ACTIVITY_KEY, JSON.stringify(this.activityLog)),
      AsyncStorage.setItem(SCHEDULE_STATE_KEY, JSON.stringify(this.state)),
    ]);
  }

  // ── 记录用户行为 ──────────────────────────

  /** 记录用户发了一条消息 */
  async recordUserMessage(messages: Message[]) {
    const now = Date.now();
    this.state.lastUserActiveTime = now;

    // 检查是否是回复（上一条是AI消息）
    const lastTwo = messages.slice(-2);
    let isReply = false;
    let replyDelay = 0;
    if (lastTwo.length === 2 && lastTwo[0].role === 'assistant' && lastTwo[1].role === 'user') {
      isReply = true;
      replyDelay = now - lastTwo[0].timestamp;
    }

    this.activityLog.push({
      timestamp: now,
      isReply,
      replyDelayMs: isReply ? replyDelay : undefined,
    });

    await this.save();
  }

  /** 记录发了一条主动消息 */
  async recordProactiveSent() {
    this.state.lastProactiveTime = Date.now();
    this.state.totalProactiveSent++;
    await this.save();
  }

  /** 记录用户回复了主动消息 */
  async recordProactiveReply() {
    this.state.totalUserReplies++;
    this.state.hitRate = this.state.totalUserReplies / Math.max(1, this.state.totalProactiveSent);
    await this.save();
  }

  // ── 分析引擎 ──────────────────────────

  /** 分析每小时活动分布 */
  analyzeHourlyActivity(): HourlyActivity[] {
    const hourly: { count: number; totalReplyMs: number; replyCount: number }[] =
      Array.from({ length: 24 }, () => ({ count: 0, totalReplyMs: 0, replyCount: 0 }));

    for (const r of this.activityLog) {
      const hour = new Date(r.timestamp).getHours();
      hourly[hour].count++;
      if (r.isReply && r.replyDelayMs) {
        hourly[hour].totalReplyMs += r.replyDelayMs;
        hourly[hour].replyCount++;
      }
    }

    return hourly.map((h, i) => ({
      hour: i,
      count: h.count,
      avgReplyMs: h.replyCount > 0 ? h.totalReplyMs / h.replyCount : 0,
    }));
  }

  /** 计算用户活跃时间段 */
  getActiveHours(): number[] {
    const hourly = this.analyzeHourlyActivity();
    const maxCount = Math.max(...hourly.map(h => h.count), 1);
    // 活跃度超过最大值 30% 的小时算活跃
    return hourly
      .filter(h => h.count > maxCount * 0.3)
      .map(h => h.hour);
  }

  /**
   * 核心算法：推算最佳主动聊天时间
   *
   * 逻辑：
   * 1. 从历史数据找出用户活跃小时
   * 2. 活跃小时的前后 1-2 小时 = 可能空闲但在线
   * 3. 排除深夜(0-6点)和明显工作时间(9-12, 14-18 如果活跃度低)
   * 4. 根据回复延迟和活跃度打分
   * 5. 返回排序后的推荐窗口
   */
  findBestWindows(): IdleWindow[] {
    const hourly = this.analyzeHourlyActivity();
    const activeHours = new Set(this.getActiveHours());
    const now = new Date();
    const currentHour = now.getHours();

    if (activeHours.size === 0) {
      // 没有足够数据，返回默认窗口
      return [
        { startHour: 8, endHour: 9, score: 0.5, reason: '默认早间' },
        { startHour: 12, endHour: 13, score: 0.6, reason: '默认午间' },
        { startHour: 19, endHour: 21, score: 0.7, reason: '默认晚间' },
      ];
    }

    const windows: IdleWindow[] = [];

    for (let h = 7; h < 23; h++) {
      // 跳过当前小时之前的时间（今天已经过了）
      if (h < currentHour) continue;

      let score = 0;
      const reasons: string[] = [];

      // 因素1: 这个小时是否活跃？活跃说明在线
      if (hourly[h].count > 0) {
        score += 0.3;
        reasons.push('常在线');
      }

      // 因素2: 前一小时是否活跃？说明刚忙完可能空闲
      const prevH = (h + 23) % 24;
      if (hourly[prevH].count > 0 && hourly[h].count === 0) {
        score += 0.25;
        reasons.push('刚忙完');
      }

      // 因素3: 回复速度快的时段更推荐
      if (hourly[h].avgReplyMs > 0 && hourly[h].avgReplyMs < 5 * 60 * 1000) {
        score += 0.2;
        reasons.push('回复快');
      }

      // 因素4: 午休和晚间加权
      if (h >= 12 && h <= 13) { score += 0.15; reasons.push('午休'); }
      if (h >= 19 && h <= 22) { score += 0.2; reasons.push('晚间'); }

      // 因素5: 周末全天加权
      const day = now.getDay();
      if (day === 0 || day === 6) {
        score += 0.1;
        reasons.push('周末');
      }

      // 惩罚: 距离上次主动消息太近
      const hoursSinceLast = (Date.now() - this.state.lastProactiveTime) / (60 * 60 * 1000);
      if (hoursSinceLast < 3) {
        score *= 0.3;
        reasons.push('刚发过');
      }

      // 惩罚: 深夜
      if (h >= 0 && h < 7) {
        score *= 0.1;
        reasons.push('深夜');
      }

      if (score > 0.2) {
        windows.push({
          startHour: h,
          endHour: Math.min(h + 1, 23),
          score: Math.min(score, 1),
          reason: reasons.join('+'),
        });
      }
    }

    // 按分数排序
    windows.sort((a, b) => b.score - a.score);
    return windows.slice(0, 5);
  }

  /** 判断现在是否适合发主动消息 */
  shouldSendNow(): { send: boolean; reason: string } {
    const hour = new Date().getHours();
    const windows = this.findBestWindows();

    // 深夜不发
    if (hour >= 0 && hour < 7) return { send: false, reason: '深夜时段' };

    // 距离上次不到 2 小时不发
    const hoursSince = (Date.now() - this.state.lastProactiveTime) / (60 * 60 * 1000);
    if (hoursSince < 2) return { send: false, reason: '距离上次太近(' + Math.round(hoursSince) + 'h)' };

    // 检查当前小时是否在推荐窗口中
    const currentWindow = windows.find(w => hour >= w.startHour && hour <= w.endHour);
    if (currentWindow && currentWindow.score > 0.3) {
      return { send: true, reason: currentWindow.reason + ' (score=' + currentWindow.score.toFixed(2) + ')' };
    }

    // 用户超过 8 小时没活跃，发一条关心
    const hoursSinceActive = (Date.now() - this.state.lastUserActiveTime) / (60 * 60 * 1000);
    if (hoursSinceActive > 8) {
      return { send: true, reason: '用户' + Math.round(hoursSinceActive) + '小时没活跃了' };
    }

    return { send: false, reason: '当前不在推荐窗口' };
  }

  /** 获取调度统计（用于调试/展示） */
  getStats() {
    return {
      totalRecords: this.activityLog.length,
      activeHours: this.getActiveHours(),
      bestWindows: this.findBestWindows().slice(0, 3),
      hitRate: Math.round(this.state.hitRate * 100) + '%',
      totalSent: this.state.totalProactiveSent,
      totalReplies: this.state.totalUserReplies,
      lastProactive: this.state.lastProactiveTime ? new Date(this.state.lastProactiveTime).toLocaleString() : '从未',
      lastUserActive: this.state.lastUserActiveTime ? new Date(this.state.lastUserActiveTime).toLocaleString() : '从未',
    };
  }
}

export const smartScheduler = new SmartScheduler();
