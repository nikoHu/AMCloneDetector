package com.hdu.manager;

import com.hdu.bean.Measure;

import java.util.*;

/**
 * Final improved IDPairGenerator (Type-3 recall friendly, token-enhanced)
 *
 * Key idea:
 * - "Full tokens" that KEEP mid-frequency signals:
 *   identifiers subtokens, call names, Class.method, short valuable strings, NUM/STR normalization.
 *
 * Strategy:
 * 1) Candidate stage: NO lineGap filter (delay to verification stage using Config.LineGapDis)
 * 2) Build inverted index over ENHANCED UNIQUE tokens (token -> methodIds)
 * 3) Query tokens in 3 channels (OR):
 *    - Rare-first: df <= dfCutoffThreshold, capped by MAX_RARE_QUERY_TOKENS
 *    - Mid-band sampling: sample tokens after cutoff (often Type-3 signal)
 *    - Tail backfill: last few high-df tokens with posting cap
 */
public class InvertedIndexMeasureIDPairGenerator extends IDPairGenerator {

    private final List<Measure> measureList;

    // ================== Tunables ==================
    // Tokens appearing in more than N * MAX_DF_RATIO methods are considered "too frequent" for rare-channel.
    private static final double MAX_DF_RATIO = 0.30;

    // Max number of rare tokens to query per method
    private static final int MAX_RARE_QUERY_TOKENS = 200;

    // Mid-frequency sampling (Type-3 often shares these)
    private static final int MID_BAND_TOKENS = 80;   // how many mid tokens to query
    private static final int MID_BAND_STRIDE = 2;    // 1 denser(slower), 2~4 typical

    // High-frequency tail backfill
    private static final int TAIL_BACKFILL_TOKENS = 20;

    // Posting list cap for tail tokens (scale-aware)
    private static final int TAIL_POSTING_MULTIPLIER = 10;

    // Absolute lower bound for df cutoff (small datasets)
    private static final int MIN_DF_CUTOFF = 50;

    // Tokenization / normalization
    private static final int MIN_SUBTOKEN_LEN = 2;
    private static final int MAX_KEEP_STR_LEN = 12;  // keep short strings like UTF-8, md5, etc.
    // ==============================================

    // df(token): number of methods containing token
    private final Map<String, Integer> df = new HashMap<>();
    // inverted index: token -> sorted methodIds containing token
    private final Map<String, int[]> posting = new HashMap<>();
    // per method: UNIQUE enhanced tokens sorted by df asc (rare -> frequent)
    private final List<String[]> sortedTokensPerMethod = new ArrayList<>();

    // streaming state
    private int curA = 0;
    private int curBPos = 0;
    private int[] curCandidates = null;

    // df cutoff threshold (absolute count)
    private final int dfCutoffThreshold;
    // tail posting cap (absolute length)
    private final int tailPostingCap;

    // small stoplist to remove ultra-high-df noise
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            // keywords
            "if","for","while","switch","case","return","new","try","catch","throw","throws",
            "public","private","protected","static","final","class","interface","enum",
            "void","int","float","double","long","short","byte","char","boolean",
            "null","true","false","this","super","break","continue","default",
            "import","package","extends","implements","synchronized","volatile","transient",
            // ultra-common generic names (optional)
            "get","set" // 如果你觉得 get/set 太有价值，可删掉这两项
    ));

    public InvertedIndexMeasureIDPairGenerator(List<Measure> measureList, float unusedLineGapDis) {
        super(measureList.size());
        this.measureList = measureList;

        int th = (int) Math.ceil(measureList.size() * MAX_DF_RATIO);
        if (th < MIN_DF_CUTOFF) th = MIN_DF_CUTOFF;
        this.dfCutoffThreshold = th;
        this.tailPostingCap = th * TAIL_POSTING_MULTIPLIER;

        buildDf();                 // uses enhanced tokens
        buildSortedTokens();       // uses enhanced tokens
        buildPostingAllTokens();   // uses enhanced tokens
    }

    @Override
    public List<String> generate(int length) {
        List<String> out = new ArrayList<>(Math.min(length, 1024));
        if (size < 2) return out;

        while (out.size() < length) {
            if (curCandidates == null || curBPos >= curCandidates.length) {
                if (curA >= size - 1) break;
                curCandidates = buildCandidatesForA(curA);
                curBPos = 0;
                curA++;
                continue;
            }

            int a = curA - 1;
            int b = curCandidates[curBPos++];
            out.add(a + "," + b);
        }
        return out;
    }

    /** Step 1: build global df(token) using ENHANCED UNIQUE tokens */
    private void buildDf() {
        for (Measure m : measureList) {
            List<String> enhanced = enhanceTokens(asList(m.getCode()));
            HashSet<String> seen = new HashSet<>(enhanced.size() * 2);

            for (String tok : enhanced) {
                if (tok == null || tok.isEmpty()) continue;
                if (seen.add(tok)) {
                    df.put(tok, df.getOrDefault(tok, 0) + 1);
                }
            }
        }
    }

    /** Step 2: per method, get UNIQUE enhanced tokens and sort by rarity (df asc) */
    private void buildSortedTokens() {
        for (Measure m : measureList) {
            List<String> enhanced = enhanceTokens(asList(m.getCode()));
            HashSet<String> uniq = new HashSet<>(enhanced);

            String[] toks = uniq.toArray(new String[0]);
            Arrays.sort(toks, (a, b) -> {
                int da = df.getOrDefault(a, Integer.MAX_VALUE);
                int db = df.getOrDefault(b, Integer.MAX_VALUE);
                if (da != db) return Integer.compare(da, db);
                return a.compareTo(b);
            });
            sortedTokensPerMethod.add(toks);
        }
    }

    /** Step 3: build posting lists using ALL UNIQUE enhanced tokens */
    private void buildPostingAllTokens() {
        Map<String, IntList> tmp = new HashMap<>();
        for (int i = 0; i < measureList.size(); i++) {
            String[] toks = sortedTokensPerMethod.get(i);
            for (String tok : toks) {
                tmp.computeIfAbsent(tok, z -> new IntList()).add(i);
            }
        }
        for (Map.Entry<String, IntList> e : tmp.entrySet()) {
            int[] arr = e.getValue().toArray();
            Arrays.sort(arr);
            posting.put(e.getKey(), arr);
        }
    }

    /**
     * Build candidates for a specific A:
     * - Rare-channel: query low-df tokens until dfCutoffThreshold (and cap token count)
     * - Mid-band: query sampled tokens after cutoff (often Type-3 key)
     * - Tail backfill: query last few high-df tokens with posting cap
     *
     * NOTE: No lineGap filter here. Do it in verification stage.
     */
    private int[] buildCandidatesForA(int a) {
        String[] toks = sortedTokensPerMethod.get(a);
        if (toks.length == 0) return new int[0];

        // Use boolean[] to avoid HashSet<Integer> boxing overhead
        boolean[] mark = new boolean[size];
        int marked = 0;

        // 1) Rare-channel
        int queried = 0;
        int cutoffPos = toks.length; // first index where df > cutoff

        for (int i = 0; i < toks.length; i++) {
            String tok = toks[i];
            int tokenDf = df.getOrDefault(tok, Integer.MAX_VALUE);
            if (tokenDf > dfCutoffThreshold) {
                cutoffPos = i;
                break;
            }

            int[] plist = posting.get(tok);
            if (plist != null) {
                marked += addAllBGreaterThanA(mark, a, plist);
            }

            if (++queried >= MAX_RARE_QUERY_TOKENS) {
                cutoffPos = Math.min(cutoffPos, i + 1);
                break;
            }
        }

        // 2) Mid-band sampling (Type-3 signal often lies here)
        int midAdded = 0;
        for (int i = cutoffPos; i < toks.length && midAdded < MID_BAND_TOKENS; i += MID_BAND_STRIDE) {
            String tok = toks[i];
            int[] plist = posting.get(tok);
            if (plist == null) continue;

            // allow longer posting lists, but still cap
            if (plist.length > tailPostingCap * 20) continue;

            marked += addAllBGreaterThanA(mark, a, plist);
            midAdded++;
        }

        // 3) Tail backfill (high-df, capped)
        int tail = Math.min(TAIL_BACKFILL_TOKENS, toks.length);
        for (int k = toks.length - 1; k >= 0 && k >= toks.length - tail; k--) {
            String tok = toks[k];
            int[] plist = posting.get(tok);
            if (plist == null) continue;

            if (plist.length > tailPostingCap) continue;
            marked += addAllBGreaterThanA(mark, a, plist);
        }

        if (marked == 0) return new int[0];

        int[] res = new int[marked];
        int idx = 0;
        for (int b = a + 1; b < size; b++) {
            if (mark[b]) res[idx++] = b;
        }
        return res;
    }

    /**
     * Add all b > a from posting list into mark[] (binary-search start for speed).
     * Return how many NEW bs were marked.
     */
    private static int addAllBGreaterThanA(boolean[] mark, int a, int[] plist) {
        int start = Arrays.binarySearch(plist, a + 1);
        if (start < 0) start = -start - 1;

        int added = 0;
        for (int i = start; i < plist.length; i++) {
            int b = plist[i];
            if (!mark[b]) {
                mark[b] = true;
                added++;
            }
        }
        return added;
    }

    // =========================
    // Token enhancement section
    // =========================

    /** Adapt Measure.getCode() to List<String> regardless of underlying type. */
    @SuppressWarnings("unchecked")
    private static List<String> asList(Object code) {
        if (code == null) return Collections.emptyList();
        if (code instanceof List) return (List<String>) code;
        if (code instanceof String[]) return Arrays.asList((String[]) code);
        // fallback
        return Collections.singletonList(code.toString());
    }

    /**
     * Enhance raw tokens to be Type-3 friendly:
     * - lowercasing
     * - drop pure punctuation
     * - normalize number -> "num"
     * - normalize string literal -> "str" + optionally "str_xxx" for short valuable strings
     * - keep Class.method as: class, method, class.method
     * - keep call token like "foo(" -> "foo"
     * - split identifiers (snake/kebab + digit transitions)
     */
    private static List<String> enhanceTokens(List<String> raw) {
        HashSet<String> out = new HashSet<>(raw.size() * 2);

        for (String t : raw) {
            if (t == null) continue;
            t = t.trim();
            if (t.isEmpty()) continue;

            String s = t.toLowerCase(Locale.ROOT);

            // drop pure punctuation tokens
            if (s.matches("^[\\p{Punct}]+$")) continue;

            // string literal heuristic
            if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
                out.add("str");
                String inner = s.substring(1, s.length() - 1);
                // keep short, clean strings (UTF-8, md5, tag, etc.)
                if (inner.length() > 0 && inner.length() <= MAX_KEEP_STR_LEN && inner.matches("[a-z0-9\\-_.]+")) {
                    out.add("str_" + inner.replace('-', '_').replace('.', '_'));
                }
                continue;
            }

            // number literal heuristic
            if (s.matches("^[+-]?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$")) {
                out.add("num");
                continue;
            }

            // normalize call token like "foo(" or "bar()"
            if (s.endsWith("(")) s = s.substring(0, s.length() - 1);
            if (s.endsWith("()")) s = s.substring(0, s.length() - 2);

            // keep full token (if not stop/noise)
            addIfUseful(out, s);

            // handle Class.method (also captures package.class.method partially; only first dot is enough)
            int dot = s.indexOf('.');
            if (dot > 0 && dot < s.length() - 1) {
                String left = s.substring(0, dot);
                String right = s.substring(dot + 1);
                addIfUseful(out, left);
                addIfUseful(out, right);
                if (left.length() >= MIN_SUBTOKEN_LEN && right.length() >= MIN_SUBTOKEN_LEN) {
                    addIfUseful(out, left + "." + right);
                }
            }

            // split identifiers: snake/kebab + digit transitions (cheap & robust)
            for (String sub : splitIdentifier(s)) {
                addIfUseful(out, sub);
            }
        }

        return new ArrayList<>(out);
    }

    private static void addIfUseful(Set<String> out, String tok) {
        if (tok == null) return;
        tok = tok.trim();
        if (tok.length() < MIN_SUBTOKEN_LEN) return;
        if (STOP.contains(tok)) return;
        out.add(tok);
    }

    private static List<String> splitIdentifier(String s) {
        ArrayList<String> out = new ArrayList<>();
        // split snake/kebab first
        String[] parts = s.split("[_\\-]+");
        for (String p : parts) {
            if (p == null) continue;
            p = p.trim();
            if (p.isEmpty()) continue;

            // split letter<->digit transitions
            String[] p2 = p.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
            for (String x : p2) {
                if (x != null && !x.isEmpty()) out.add(x);
            }
        }
        return out;
    }

    // =========================
    // tiny int list helper
    // =========================
    private static class IntList {
        private int[] a = new int[8];
        private int n = 0;

        void add(int v) {
            if (n == a.length) a = Arrays.copyOf(a, a.length * 2);
            a[n++] = v;
        }

        int[] toArray() {
            return Arrays.copyOf(a, n);
        }
    }
}