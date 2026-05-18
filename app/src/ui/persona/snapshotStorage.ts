/** 角色编辑快照存储 - 崩溃恢复用 */

export interface EditSnapshot {
  prompt: string;
  sliders: { temperature: number; topP: number; maxTokens: number };
  overrideGlobal: boolean;
  autoMode: string;
  timestamp: number;
}

const KEY = (personaId: string) => `persona_edit_snapshot_${personaId}`;

export function saveSnapshot(personaId: string, data: Omit<EditSnapshot, 'timestamp'>): void {
  try {
    const snapshot: EditSnapshot = { ...data, timestamp: Date.now() };
    localStorage.setItem(KEY(personaId), JSON.stringify(snapshot));
  } catch {}
}

export function loadSnapshot(personaId: string): EditSnapshot | null {
  try {
    const raw = localStorage.getItem(KEY(personaId));
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!parsed.prompt && !parsed.sliders) return null;
    return parsed as EditSnapshot;
  } catch { return null; }
}

export function clearSnapshot(personaId: string): void {
  try { localStorage.removeItem(KEY(personaId)); } catch {}
}
