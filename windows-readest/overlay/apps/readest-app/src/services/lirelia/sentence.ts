// Preserve DOM offsets across inline emphasis and links before segmenting.
export function sentenceAtRange(range: Range, language = 'fr'): string {
  const element =
    range.startContainer.nodeType === 1
      ? (range.startContainer as Element)
      : range.startContainer.parentElement;
  const block =
    element?.closest('p,li,blockquote,td,th,h1,h2,h3,h4,h5,h6,pre,figcaption') ?? element;
  if (!block) return range.toString().trim();
  const prefix = range.cloneRange();
  prefix.selectNodeContents(block);
  prefix.setEnd(range.startContainer, range.startOffset);
  const offset = prefix.toString().length;
  const text = block.textContent ?? '';
  for (const part of new Intl.Segmenter(language, { granularity: 'sentence' }).segment(text)) {
    if (offset >= part.index && offset < part.index + part.segment.length)
      return part.segment.replace(/\s+/g, ' ').trim();
  }
  return range.toString().trim();
}
