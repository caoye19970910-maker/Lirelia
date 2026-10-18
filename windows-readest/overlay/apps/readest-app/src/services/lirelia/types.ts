export type ReadingLanguage = 'fr' | 'en';
export type QueryKind = 'word' | 'sentence' | 'analysis';
export interface ReadingSource {
  bookId: string;
  bookKey?: string;
  title: string;
  cfi?: string;
  context?: string;
}
export interface LireliaSelection {
  text: string;
  source?: ReadingSource;
}
export interface QueryResult {
  answer: string;
  text?: string;
  query?: string;
  kind?: QueryKind;
  language?: ReadingLanguage;
  lemma?: string;
  source?: string;
}
export interface StudyCard {
  text: string;
  language: ReadingLanguage;
  kind?: string;
  result: QueryResult;
  source?: ReadingSource;
  status?: string;
  due?: string;
}
export interface LireliaSettings {
  dailyNew: number;
  autoSpeak: boolean;
  geminiModel: string;
  geminiSlot: number;
  deeplSlot: number;
  voiceFr: string;
  voiceEn: string;
  playbackRate: number;
  models: string[];
  scheduler: string;
  credentials: { gemini: boolean; deepl: boolean; deepgram: boolean };
}
export interface StudySummary {
  total: number;
  new_words: number;
  due_words: number;
  reviewed_today: number;
}
