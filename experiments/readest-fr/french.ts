/**
 * French lemmatizer PoC for Readest dictionary lookup fallback.
 * Exact dictionary matches still win; these are fallback candidates only.
 */
const IRREGULAR: Record<string, string> = {
  "suis":"être","es":"être","est":"être","sommes":"être","êtes":"être","sont":"être",
  "étais":"être","était":"être","étions":"être","étiez":"être","étaient":"être","été":"être",
  "ai":"avoir","as":"avoir","a":"avoir","avons":"avoir","avez":"avoir","ont":"avoir",
  "vais":"aller","vas":"aller","va":"aller","allons":"aller","allez":"aller","vont":"aller",
  "allé":"aller","allée":"aller","allés":"aller","allées":"aller",
  "fais":"faire","fait":"faire","faisons":"faire","faites":"faire","font":"faire","faisaient":"faire",
  "prends":"prendre","prend":"prendre","prenons":"prendre","prenez":"prendre","prennent":"prendre",
  "pris":"prendre","prise":"prendre","prises":"prendre",
  "peux":"pouvoir","peut":"pouvoir","pouvons":"pouvoir","pouvez":"pouvoir","peuvent":"pouvoir","pouvait":"pouvoir",
  "écris":"écrire","écrit":"écrire","écrite":"écrire","écrits":"écrire","écrites":"écrire",
  "reçois":"recevoir","reçoit":"recevoir","reçu":"recevoir","reçue":"recevoir","reçus":"recevoir","reçues":"recevoir"
};

const CONTRACTIONS: Record<string, string[]> = {
  "au":["à","le"], "aux":["à","les"], "du":["de","le"], "des":["de","les"]
};

export const lemmatizeFrench = (word: string): string[] => {
  const lower = word.toLowerCase().normalize("NFC");
  if (!/^[a-zàâäæçéèêëîïôöœùûüÿ'’-]+$/iu.test(lower)) return [];

  const out: string[] = [];
  const push = (s: string) => {
    s = s.normalize("NFC");
    if (s && s !== lower && !out.includes(s)) out.push(s);
  };

  for (const c of CONTRACTIONS[lower] ?? []) push(c);
  if (IRREGULAR[lower]) push(IRREGULAR[lower]);

  let singular = lower;
  if (singular.endsWith("s") && singular.length > 3) {
    singular = singular.slice(0, -1);
    push(singular);
  }
  if (singular.endsWith("euse")) push(singular.slice(0, -4) + "eux");
  if (singular.endsWith("ive")) push(singular.slice(0, -3) + "if");
  if (singular.endsWith("e") && singular.length > 3) push(singular.slice(0, -1));

  for (const suffix of ["ées","ée","és","é"]) {
    if (lower.endsWith(suffix) && lower.length > suffix.length + 1) {
      push(lower.slice(0, -suffix.length) + "er");
    }
  }
  for (const suffix of ["ies","ie","is","i"]) {
    if (lower.endsWith(suffix) && lower.length > suffix.length + 1) {
      push(lower.slice(0, -suffix.length) + "ir");
    }
  }

  for (const suffix of ["aient","ions","iez","ais","ait","ons","ez","ent","es","e"]) {
    if (!lower.endsWith(suffix) || lower.length <= suffix.length + 1) continue;
    const stem = lower.slice(0, -suffix.length);
    if (stem.endsWith("ge")) push(stem.slice(0, -1) + "er");
    push(stem + "er");
    push(stem + "ir");
    push(stem + "re");
  }

  return out;
};
