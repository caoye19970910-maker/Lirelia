'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { LuGraduationCap, LuX, LuVolume2 } from 'react-icons/lu';
import { isTauriAppPlatform } from '@/services/environment';
import { lireliaCall } from '@/services/lirelia/client';
import type {
  LireliaSelection,
  LireliaSettings,
  QueryResult,
  ReadingLanguage,
  ReadingSource,
  StudyCard,
  StudySummary,
} from '@/services/lirelia/types';
import { useReaderStore } from '@/store/readerStore';
import { useAppRouter } from '@/hooks/useAppRouter';
import { navigateToReader } from '@/utils/nav';
import BoxedList from '@/components/settings/primitives/BoxedList';
import SettingsRow from '@/components/settings/primitives/SettingsRow';

type Tab = 'query' | 'vocabulary' | 'review' | 'settings';
type LanguageRequest = {
  bookId: string;
  title: string;
  resolve: (continueOpening: boolean) => void;
};
const errorMessage = (error: unknown) =>
  error instanceof Error ? error.message : '操作未完成，请重试。';

export default function LireliaPanel() {
  const router = useAppRouter();
  const [open, setOpen] = useState(false);
  const [tab, setTab] = useState<Tab>('query');
  const [selection, setSelection] = useState<LireliaSelection>({ text: '' });
  const [language, setLanguage] = useState<ReadingLanguage>('fr');
  const [studyLanguage, setStudyLanguage] = useState<ReadingLanguage>('fr');
  const [languageRequest, setLanguageRequest] = useState<LanguageRequest | null>(null);
  const [result, setResult] = useState<QueryResult | null>(null);
  const [analysis, setAnalysis] = useState('');
  const [settings, setSettings] = useState<LireliaSettings | null>(null);
  const [cards, setCards] = useState<StudyCard[]>([]);
  const [summary, setSummary] = useState<StudySummary | null>(null);
  const [flipped, setFlipped] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [width, setWidth] = useState(440);
  const [provider, setProvider] = useState<'gemini' | 'deepl' | 'deepgram'>('gemini');
  const [keyValue, setKeyValue] = useState('');
  const [playing, setPlaying] = useState(false);
  const generation = useRef(0);
  const audioGeneration = useRef(0);
  const audio = useRef<HTMLAudioElement | null>(null);
  const audioUrl = useRef<string | null>(null);
  const panel = useRef<HTMLElement>(null);
  const previousFocus = useRef<HTMLElement | null>(null);
  const prefs = useRef<LireliaSettings | null>(null);
  const reviewContext = useRef({ open, tab, language: studyLanguage });
  reviewContext.current = { open, tab, language: studyLanguage };

  const stopAudio = useCallback(() => {
    audioGeneration.current++;
    audio.current?.pause();
    audio.current = null;
    if (audioUrl.current) URL.revokeObjectURL(audioUrl.current);
    audioUrl.current = null;
    setPlaying(false);
  }, []);

  const speak = useCallback(
    async (text: string, kind: 'word' | 'sentence', lang: ReadingLanguage) => {
      stopAudio();
      const ticket = audioGeneration.current;
      setPlaying(true);
      try {
        const response = await lireliaCall<{ data: string; mime: string }>('speech', {
          text,
          kind,
          language: lang,
        });
        if (ticket !== audioGeneration.current) return;
        const data = Uint8Array.from(atob(response.data), (char) => char.charCodeAt(0));
        const url = URL.createObjectURL(new Blob([data], { type: response.mime }));
        audioUrl.current = url;
        const player = new Audio(url);
        audio.current = player;
        player.playbackRate = prefs.current?.playbackRate ?? 1;
        player.onended = () => {
          if (ticket === audioGeneration.current) stopAudio();
        };
        player.onerror = () => {
          if (ticket === audioGeneration.current) {
            stopAudio();
            setMessage('音频播放失败，请检查声音输出后重试。');
          }
        };
        await player.play();
      } catch (error) {
        if (ticket === audioGeneration.current) {
          stopAudio();
          setMessage(errorMessage(error));
        }
      }
    },
    [stopAudio],
  );

  const loadSettings = useCallback(async () => {
    const value = await lireliaCall<LireliaSettings>('settings');
    prefs.current = value;
    setSettings(value);
    return value;
  }, []);

  const lookup = useCallback(
    async (input: LireliaSelection) => {
      const ticket = ++generation.current;
      stopAudio();
      setSelection(input);
      setResult(null);
      setAnalysis('');
      setMessage('');
      setBusy(true);
      setOpen(true);
      setTab('query');
      try {
        const currentSettings = await loadSettings();
        const lang = input.source
          ? ((await lireliaCall<ReadingLanguage | null>('book_language', {
              bookId: input.source.bookId,
            })) ?? 'fr')
          : language;
        const value = await lireliaCall<QueryResult>('query', { text: input.text, language: lang });
        if (ticket !== generation.current) return;
        setLanguage(lang);
        setResult(value);
        if (value.kind === 'word' && currentSettings.autoSpeak)
          void speak(input.text, 'word', lang);
      } catch (error) {
        if (ticket === generation.current) setMessage(errorMessage(error));
      } finally {
        if (ticket === generation.current) setBusy(false);
      }
    },
    [language, loadSettings, speak, stopAudio],
  );
  const lookupRef = useRef(lookup);
  lookupRef.current = lookup;

  const close = useCallback(() => {
    generation.current++;
    stopAudio();
    setBusy(false);
    setOpen(false);
    previousFocus.current?.focus();
  }, [stopAudio]);

  useEffect(() => {
    const savedWidth = Number(localStorage.getItem('lirelia-panel-width'));
    if (savedWidth >= 320 && savedWidth <= 800) setWidth(savedWidth);
    const onOpen = (event: Event) => {
      previousFocus.current = document.activeElement as HTMLElement;
      const input = (event as CustomEvent<LireliaSelection | undefined>).detail;
      if (input?.text) void lookupRef.current(input);
      else {
        setOpen(true);
        void loadSettings().catch((error: unknown) => setMessage(errorMessage(error)));
      }
    };
    const onLanguage = (event: Event) =>
      setLanguageRequest((event as CustomEvent<LanguageRequest>).detail);
    window.addEventListener('lirelia-open', onOpen);
    window.addEventListener('lirelia-language', onLanguage);
    return () => {
      window.removeEventListener('lirelia-open', onOpen);
      window.removeEventListener('lirelia-language', onLanguage);
      generation.current++;
      stopAudio();
    };
  }, [loadSettings, stopAudio]);

  useEffect(() => {
    if (!open) return;
    panel.current?.focus();
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !languageRequest) {
        event.preventDefault();
        close();
      }
    };
    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [open, close, languageRequest]);

  const refreshCards = useCallback(
    async (currentTab: Tab, lang: ReadingLanguage) => {
      const [values, counts] = await Promise.all([
        lireliaCall<StudyCard[]>(currentTab === 'review' ? 'review_queue' : 'bookmarks', {
          language: lang,
        }),
        lireliaCall<StudySummary>('summary', { language: lang }),
      ]);
      const current = reviewContext.current;
      if (!current.open || current.tab !== currentTab || current.language !== lang) return;
      setCards(values);
      setSummary(counts);
      setFlipped(false);
      const first = values[0];
      if (currentTab === 'review' && first && prefs.current?.autoSpeak)
        void speak(first.text, 'word', first.language);
    },
    [speak],
  );

  useEffect(() => {
    if (!open || (tab !== 'review' && tab !== 'vocabulary')) return;
    let active = true;
    stopAudio();
    setCards([]);
    setFlipped(false);
    void (async () => {
      try {
        const [values, counts] = await Promise.all([
          lireliaCall<StudyCard[]>(tab === 'review' ? 'review_queue' : 'bookmarks', {
            language: studyLanguage,
          }),
          lireliaCall<StudySummary>('summary', { language: studyLanguage }),
        ]);
        if (!active) return;
        setCards(values);
        setSummary(counts);
        const first = values[0];
        if (tab === 'review' && first && prefs.current?.autoSpeak)
          void speak(first.text, 'word', first.language);
      } catch (error) {
        if (active) setMessage(errorMessage(error));
      }
    })();
    return () => {
      active = false;
      stopAudio();
    };
  }, [open, tab, studyLanguage, speak, stopAudio]);

  async function action(work: () => Promise<void>) {
    setBusy(true);
    setMessage('');
    try {
      await work();
    } catch (error) {
      setMessage(errorMessage(error));
    } finally {
      setBusy(false);
    }
  }

  async function saveSettings(changes: Partial<LireliaSettings>) {
    const value = await lireliaCall<LireliaSettings>('save_settings', changes);
    setSettings(value);
    prefs.current = value;
  }

  async function returnToSource(source: ReadingSource) {
    if (!source.cfi) return;
    const view = useReaderStore.getState().getView(source.bookKey ?? null);
    if (view) await view.goTo(source.cfi);
    else
      navigateToReader(
        router,
        [source.bookId],
        new URLSearchParams({ cfi: source.cfi }).toString(),
      );
    close();
  }

  async function selectLanguage(lang: ReadingLanguage) {
    if (!languageRequest) return;
    await lireliaCall('set_book_language', { bookId: languageRequest.bookId, language: lang });
    languageRequest.resolve(true);
    setLanguageRequest(null);
  }

  if (!isTauriAppPlatform()) return null;
  const currentCard = cards[0];
  const sourceView = (source?: ReadingSource) =>
    source && (
      <div className='border-base-200 space-y-2 border-t pt-3 text-sm'>
        <p className='text-base-content/65'>{source.title}</p>
        {source.context && (
          <blockquote className='border-base-300 border-s-2 ps-3'>{source.context}</blockquote>
        )}
        {source.cfi && (
          <button
            className='btn btn-sm eink-bordered'
            onClick={() => void action(() => returnToSource(source))}
          >
            返回原文
          </button>
        )}
      </div>
    );

  return (
    <>
      {!open && (
        <button
          className='btn btn-sm eink-bordered fixed bottom-5 right-5 z-40 gap-2 rounded-full'
          aria-label='打开 Lirelia 学习面板'
          onClick={() => {
            previousFocus.current = document.activeElement as HTMLElement;
            setOpen(true);
            void loadSettings().catch((error: unknown) => setMessage(errorMessage(error)));
          }}
        >
          <LuGraduationCap /> Lirelia
        </button>
      )}
      {open && (
        <aside
          ref={panel}
          role='region'
          aria-label='Lirelia 学习面板'
          tabIndex={-1}
          className='bg-base-100 text-base-content border-base-300 eink-bordered fixed bottom-4 right-4 top-12 z-[60] flex max-w-[calc(100vw-2rem)] flex-col rounded-xl border outline-none'
          style={{ width }}
        >
          <header className='border-base-200 flex items-center justify-between border-b px-4 py-3'>
            <span className='flex items-center gap-2 font-semibold'>
              <LuGraduationCap /> Lirelia
            </span>
            <button
              className='btn btn-circle btn-ghost btn-sm'
              aria-label='关闭 Lirelia'
              onClick={close}
            >
              <LuX />
            </button>
          </header>
          <nav className='border-base-200 flex border-b p-2' aria-label='学习功能'>
            {(
              [
                ['query', '查词翻译'],
                ['vocabulary', '生词本'],
                ['review', '今日复习'],
                ['settings', '设置'],
              ] as const
            ).map(([id, label]) => (
              <button
                key={id}
                className={`btn btn-sm flex-1 ${tab === id ? 'btn-contrast' : 'btn-ghost'}`}
                aria-pressed={tab === id}
                onClick={() => {
                  generation.current++;
                  stopAudio();
                  setBusy(false);
                  setMessage('');
                  setTab(id);
                }}
              >
                {label}
              </button>
            ))}
          </nav>
          <main className='min-h-0 flex-1 space-y-4 overflow-y-auto p-4'>
            {message && (
              <p role='status' className='bg-base-200 eink-bordered rounded-lg border p-3 text-sm'>
                {message}
              </p>
            )}
            {tab === 'query' && (
              <>
                <p className='text-base-content/65 text-sm'>
                  {selection.source?.title ?? '手动查询'} · {language === 'fr' ? '法语' : '英语'}
                </p>
                <textarea
                  aria-label='查询内容'
                  className='textarea eink-bordered min-h-20 w-full'
                  value={selection.text}
                  onChange={(event) => {
                    generation.current++;
                    stopAudio();
                    setBusy(false);
                    setResult(null);
                    setAnalysis('');
                    setSelection({ text: event.target.value });
                  }}
                />
                <button
                  className='btn btn-contrast btn-sm'
                  disabled={busy || !selection.text.trim()}
                  onClick={() => void lookup(selection)}
                >
                  {busy ? '正在查询…' : '查询'}
                </button>
                {result && (
                  <>
                    <div className='flex flex-wrap gap-2'>
                      <button
                        className='btn btn-sm eink-bordered'
                        disabled={playing}
                        onClick={() =>
                          void speak(
                            selection.text,
                            result.kind === 'word' ? 'word' : 'sentence',
                            language,
                          )
                        }
                      >
                        <LuVolume2 />
                        {playing ? '正在播放…' : '朗读'}
                      </button>
                      {playing && (
                        <button className='btn btn-sm eink-bordered' onClick={stopAudio}>
                          停止
                        </button>
                      )}
                      {result.lemma && (
                        <button
                          className='btn btn-sm eink-bordered'
                          onClick={() => void speak(result.lemma!, 'word', language)}
                        >
                          <LuVolume2 />
                          原形：{result.lemma}
                        </button>
                      )}
                      <button
                        className='btn btn-contrast btn-sm'
                        disabled={busy}
                        onClick={() =>
                          void action(async () => {
                            await lireliaCall('save', {
                              text: selection.text,
                              language,
                              kind: result.kind,
                              result,
                              source: selection.source,
                            });
                            setMessage(
                              result.kind === 'word'
                                ? '已收藏，已自动加入复习队列。'
                                : '已收藏句子。',
                            );
                          })
                        }
                      >
                        收藏
                      </button>
                      {result.kind === 'sentence' && (
                        <button
                          className='btn btn-sm eink-bordered'
                          disabled={busy}
                          onClick={() => {
                            const ticket = generation.current;
                            void action(async () => {
                              const value = await lireliaCall<QueryResult>('query', {
                                text: selection.text,
                                language,
                                kind: 'analysis',
                              });
                              if (ticket === generation.current) setAnalysis(value.answer);
                            });
                          }}
                        >
                          详细解析
                        </button>
                      )}
                    </div>
                    <article className='select-text whitespace-pre-wrap break-words leading-relaxed'>
                      {result.answer}
                    </article>
                    {analysis && (
                      <section className='border-base-200 space-y-2 border-t pt-4'>
                        <h3 className='font-semibold'>详细解析</h3>
                        <p className='select-text whitespace-pre-wrap leading-relaxed'>
                          {analysis}
                        </p>
                      </section>
                    )}
                    {sourceView(selection.source)}
                  </>
                )}
              </>
            )}
            {(tab === 'review' || tab === 'vocabulary') && (
              <>
                <label className='flex items-center gap-3 text-sm'>
                  学习语言
                  <select
                    aria-label='复习语言'
                    className='select select-sm eink-bordered'
                    value={studyLanguage}
                    onChange={(event) => setStudyLanguage(event.target.value as ReadingLanguage)}
                  >
                    <option value='fr'>法语</option>
                    <option value='en'>英语</option>
                  </select>
                </label>
                {summary && (
                  <p className='text-base-content/65 text-sm'>
                    共 {summary.total ?? 0} 词 · 到期 {summary.due_words ?? 0} · 新词{' '}
                    {summary.new_words ?? 0} · 今日已复习 {summary.reviewed_today ?? 0}
                  </p>
                )}
                {tab === 'review' &&
                  (currentCard ? (
                    <>
                      <div className='border-base-200 eink-bordered space-y-4 rounded-xl border p-5'>
                        <h2 className='break-words text-center text-3xl font-semibold'>
                          {currentCard.text}
                        </h2>
                        <button
                          className='btn btn-sm eink-bordered'
                          onClick={() => void speak(currentCard.text, 'word', currentCard.language)}
                        >
                          <LuVolume2 />
                          再听一次
                        </button>
                        {!flipped ? (
                          <button
                            className='btn btn-contrast w-full'
                            onClick={() => setFlipped(true)}
                          >
                            查看释义
                          </button>
                        ) : (
                          <>
                            <article className='select-text whitespace-pre-wrap break-words leading-relaxed'>
                              {currentCard.result.answer}
                            </article>
                            {sourceView(currentCard.source)}
                            {currentCard.source?.context && (
                              <button
                                className='btn btn-sm eink-bordered'
                                onClick={() =>
                                  void speak(
                                    currentCard.source!.context!,
                                    'sentence',
                                    currentCard.language,
                                  )
                                }
                              >
                                朗读原句
                              </button>
                            )}
                          </>
                        )}
                      </div>
                      {flipped && (
                        <div className='grid grid-cols-4 gap-2'>
                          {['忘记', '困难', '记得', '轻松'].map((label, index) => (
                            <button
                              key={label}
                              className='btn btn-sm eink-bordered'
                              disabled={busy}
                              onClick={() =>
                                void action(async () => {
                                  stopAudio();
                                  await lireliaCall('rate', {
                                    text: currentCard.text,
                                    language: currentCard.language,
                                    rating: index + 1,
                                  });
                                  await refreshCards('review', studyLanguage);
                                })
                              }
                            >
                              {label}
                            </button>
                          ))}
                        </div>
                      )}
                      <button
                        className='btn btn-ghost btn-sm'
                        disabled={busy}
                        onClick={() =>
                          void action(async () => {
                            stopAudio();
                            await lireliaCall('status', {
                              text: currentCard.text,
                              language: currentCard.language,
                              status: 'paused',
                            });
                            await refreshCards('review', studyLanguage);
                          })
                        }
                      >
                        暂停这张卡片
                      </button>
                    </>
                  ) : (
                    <p className='py-8 text-center'>
                      当前没有待复习单词。新收藏的单词会自动进入计划。
                    </p>
                  ))}
                {tab === 'vocabulary' &&
                  (cards.length ? (
                    cards.map((card) => (
                      <details
                        key={`${card.language}:${card.text}`}
                        className='border-base-200 eink-bordered rounded-lg border p-3'
                      >
                        <summary className='cursor-pointer font-semibold'>{card.text}</summary>
                        <article className='my-3 select-text whitespace-pre-wrap break-words'>
                          {card.result.answer}
                        </article>
                        {sourceView(card.source)}
                        <div className='mt-3 flex gap-2'>
                          <button
                            className='btn btn-sm eink-bordered'
                            onClick={() =>
                              void speak(
                                card.text,
                                card.kind === 'sentence' ? 'sentence' : 'word',
                                card.language,
                              )
                            }
                          >
                            朗读
                          </button>
                          {card.kind === 'word' && (
                            <button
                              className='btn btn-sm eink-bordered'
                              onClick={() =>
                                void action(async () => {
                                  await lireliaCall('status', {
                                    text: card.text,
                                    language: card.language,
                                    status: 'active',
                                  });
                                  setMessage('已恢复复习；原有进度保留。');
                                })
                              }
                            >
                              恢复复习
                            </button>
                          )}
                        </div>
                      </details>
                    ))
                  ) : (
                    <p>暂无收藏。在正文划词后点击 Lirelia，即可查询和收藏。</p>
                  ))}
              </>
            )}
            {tab === 'settings' && settings && (
              <>
                <BoxedList title='查询与复习'>
                  <SettingsRow label='Gemini 模型'>
                    <select
                      aria-label='Gemini 模型'
                      className='select select-sm eink-bordered max-w-52'
                      value={settings.geminiModel}
                      onChange={(event) =>
                        void action(() => saveSettings({ geminiModel: event.target.value }))
                      }
                    >
                      {settings.models.map((model) => (
                        <option key={model}>{model}</option>
                      ))}
                    </select>
                  </SettingsRow>
                  <SettingsRow label='每日新词'>
                    <input
                      aria-label='每日新词'
                      type='number'
                      min={1}
                      max={200}
                      className='input input-sm eink-bordered w-20'
                      value={settings.dailyNew}
                      onChange={(event) => {
                        const value = Number(event.target.value);
                        if (value >= 1 && value <= 200)
                          void action(() => saveSettings({ dailyNew: value }));
                      }}
                    />
                  </SettingsRow>
                  <SettingsRow label='单词自动发音' asLabel>
                    <input
                      className='toggle toggle-sm'
                      type='checkbox'
                      checked={settings.autoSpeak}
                      onChange={(event) =>
                        void action(() => saveSettings({ autoSpeak: event.target.checked }))
                      }
                    />
                  </SettingsRow>
                  <SettingsRow label='朗读语速'>
                    <select
                      aria-label='朗读语速'
                      className='select select-sm eink-bordered'
                      value={settings.playbackRate}
                      onChange={(event) =>
                        void action(() =>
                          saveSettings({ playbackRate: Number(event.target.value) }),
                        )
                      }
                    >
                      {[0.5, 0.75, 1, 1.25, 1.5, 2].map((rate) => (
                        <option key={rate} value={rate}>
                          {rate}×
                        </option>
                      ))}
                    </select>
                  </SettingsRow>
                </BoxedList>
                <p className='text-base-content/65 text-sm'>
                  复习调度：{settings.scheduler}。单词沿用 gTTS；句子使用
                  Deepgram。书籍语言可在书架右键菜单中修改。
                </p>
                <BoxedList
                  title='API 密钥'
                  description='已自动读取此电脑现有的 Lecture Mini 凭据；留空不会覆盖。'
                >
                  <SettingsRow label='服务商'>
                    <select
                      aria-label='密钥服务商'
                      className='select select-sm eink-bordered'
                      value={provider}
                      onChange={(event) => {
                        setProvider(event.target.value as typeof provider);
                        setKeyValue('');
                      }}
                    >
                      <option value='gemini'>Gemini</option>
                      <option value='deepl'>DeepL</option>
                      <option value='deepgram'>Deepgram</option>
                    </select>
                  </SettingsRow>
                  {provider !== 'deepgram' && (
                    <SettingsRow label='使用的密钥槽位'>
                      <select
                        aria-label='密钥槽位'
                        className='select select-sm eink-bordered'
                        value={settings[provider === 'gemini' ? 'geminiSlot' : 'deeplSlot']}
                        onChange={(event) =>
                          void action(() =>
                            saveSettings({
                              [provider === 'gemini' ? 'geminiSlot' : 'deeplSlot']: Number(
                                event.target.value,
                              ),
                            }),
                          )
                        }
                      >
                        {Array.from({ length: 10 }, (_, index) => (
                          <option key={index} value={index + 1}>
                            {index + 1}
                          </option>
                        ))}
                      </select>
                    </SettingsRow>
                  )}
                  <SettingsRow label={settings.credentials[provider] ? '已配置' : '尚未配置'}>
                    <input
                      aria-label='API Key'
                      autoComplete='off'
                      type='password'
                      className='input input-sm eink-bordered max-w-48'
                      value={keyValue}
                      placeholder='输入新密钥'
                      onChange={(event) => setKeyValue(event.target.value)}
                    />
                  </SettingsRow>
                </BoxedList>
                <button
                  className='btn btn-contrast btn-sm'
                  disabled={busy || !keyValue.trim()}
                  onClick={() =>
                    void action(async () => {
                      await lireliaCall('save_key', {
                        provider,
                        value: keyValue,
                        slot: provider === 'gemini' ? settings.geminiSlot : settings.deeplSlot,
                      });
                      setKeyValue('');
                      await loadSettings();
                      setMessage('密钥已保存至系统凭据管理器。');
                    })
                  }
                >
                  保存密钥
                </button>
                <BoxedList title='迁移与界面'>
                  <SettingsRow label='导入 Lecture Mini'>
                    <button
                      className='btn btn-sm eink-bordered'
                      disabled={busy}
                      onClick={() =>
                        void action(async () => {
                          const { open: pickFile } = await import('@tauri-apps/plugin-dialog');
                          const path = await pickFile({
                            multiple: false,
                            filters: [
                              {
                                name: 'Lecture Mini 数据库',
                                extensions: ['sqlite3', 'sqlite', 'db'],
                              },
                            ],
                          });
                          if (!path) return;
                          const value = await lireliaCall<{ message: string }>(
                            'import_lecturemini',
                            { path },
                          );
                          setMessage(value.message);
                        })
                      }
                    >
                      选择数据库
                    </button>
                  </SettingsRow>
                  <SettingsRow label='面板宽度'>
                    <input
                      aria-label='面板宽度'
                      type='range'
                      min={320}
                      max={800}
                      value={width}
                      onChange={(event) => {
                        const value = Number(event.target.value);
                        setWidth(value);
                        localStorage.setItem('lirelia-panel-width', String(value));
                      }}
                    />
                  </SettingsRow>
                </BoxedList>
              </>
            )}
          </main>
        </aside>
      )}
      {languageRequest && (
        <div className='bg-base-content/30 fixed inset-0 z-[100] flex items-center justify-center p-6'>
          <section
            role='dialog'
            aria-modal='true'
            aria-labelledby='lirelia-language-title'
            className='bg-base-100 eink-bordered w-full max-w-md space-y-4 rounded-xl border p-6'
          >
            <h2 id='lirelia-language-title' className='text-lg font-semibold'>
              选择这本书的阅读语言
            </h2>
            <p>{languageRequest.title}</p>
            <p className='text-sm'>查词、翻译和发音将使用该语言，之后可从书架右键菜单修改。</p>
            <div className='flex gap-3'>
              <button
                className='btn btn-contrast flex-1'
                disabled={busy}
                onClick={() => void action(() => selectLanguage('fr'))}
              >
                法语
              </button>
              <button
                className='btn btn-contrast flex-1'
                disabled={busy}
                onClick={() => void action(() => selectLanguage('en'))}
              >
                英语
              </button>
            </div>
            <button
              className='btn btn-ghost btn-sm'
              onClick={() => {
                languageRequest.resolve(false);
                setLanguageRequest(null);
              }}
            >
              取消
            </button>
            {message && <p role='status'>{message}</p>}
          </section>
        </div>
      )}
    </>
  );
}
