import { getTimelyGreeting, getRandomCare, dailyFortune } from './charms';
import { chatSync } from './llm';
import { smartScheduler } from './scheduler';
import { ModelProvider, Message } from '../types';

export class ProactiveService {
  private timer: ReturnType<typeof setInterval> | null = null;
  private onMessage: ((text: string) => void) | null = null;
  private provider: ModelProvider | null = null;

  start(provider: ModelProvider, onMessage: (text: string) => void) {
    this.stop();
    this.provider = provider;
    this.onMessage = onMessage;
    this.timer = setInterval(() => this.check(), 30 * 60 * 1000);
    setTimeout(() => this.check(), 5 * 60 * 1000);
  }

  stop() { if (this.timer) { clearInterval(this.timer); this.timer = null; } }

  async recordUserMessage(messages: Message[]) { await smartScheduler.recordUserMessage(messages); }

  private async check() {
    if (!this.provider || !this.onMessage) return;
    const decision = smartScheduler.shouldSendNow();
    if (!decision.send) return;
    const msg = await this.generateMessage();
    this.onMessage(msg);
    await smartScheduler.recordProactiveSent();
  }

  private async generateMessage(): Promise<string> {
    if (!this.provider) return getTimelyGreeting();
    try {
      const r = await chatSync(this.provider, [
        { role: 'system', content: '你是用户的AI伴侣。用简短温暖的一句话主动找用户聊天，不超过30字。不要用你好开头。要自然，像真的想你了发消息一样。' },
        { role: 'user', content: '现在是' + new Date().toLocaleTimeString('zh-CN',{hour:'2-digit',minute:'2-digit'}) + '，给我一句自然的问候' },
      ], { temperature: 0.95, maxTokens: 60 });
      return r.content || getTimelyGreeting();
    } catch { return getTimelyGreeting(); }
  }

  async trigger(provider: ModelProvider): Promise<string> { this.provider = provider; return this.generateMessage(); }
  getStats() { return smartScheduler.getStats(); }
}

export const proactiveService = new ProactiveService();
