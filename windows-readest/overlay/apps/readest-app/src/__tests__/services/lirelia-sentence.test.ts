import { describe, expect, it } from 'vitest';
import { sentenceAtRange } from '@/services/lirelia/sentence';

describe('Lirelia sentence context', () => {
  it('extracts the French sentence across inline markup', () => {
    const block = document.createElement('p');
    block.innerHTML = 'Bonjour ! Il <em>disait</em> toujours la vérité. Ensuite, il partait.';
    const range = document.createRange();
    range.selectNodeContents(block.querySelector('em')!);
    expect(sentenceAtRange(range)).toBe('Il disait toujours la vérité.');
  });
  it('uses the actual offset when the English word repeats', () => {
    const block = document.createElement('p');
    block.textContent = 'A word appears here. The same word appears again!';
    const range = document.createRange();
    const start = block.textContent.lastIndexOf('word');
    range.setStart(block.firstChild!, start);
    range.setEnd(block.firstChild!, start + 4);
    expect(sentenceAtRange(range, 'en')).toBe('The same word appears again!');
  });
});
