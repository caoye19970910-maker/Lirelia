import { describe, expect, it } from 'vitest';
import { lemmatizeFrench } from '@/services/dictionaries/lemmatize/french';

describe('lemmatizeFrench', () => {
  it.each([
    ['bénéficiait', 'bénéficier'],
    ['meilleures', 'meilleur'],
    ['pris', 'prendre'],
    ['étaient', 'être'],
    ['mangeaient', 'manger'],
    ['allées', 'aller'],
    ['faisaient', 'faire'],
    ['pouvait', 'pouvoir'],
    ['écrites', 'écrire'],
    ['reçues', 'recevoir'],
  ])('%s includes %s', (form, lemma) => {
    expect(lemmatizeFrench(form)).toContain(lemma);
  });

  it('does not return the input itself', () => {
    expect(lemmatizeFrench('manger')).not.toContain('manger');
  });
});
