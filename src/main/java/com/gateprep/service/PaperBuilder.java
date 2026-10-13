package com.gateprep.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Builds a question paper in the GATE pattern from a pool of candidate questions.
 * Plain Java with no framework types, so the logic can be tested on its own.
 *
 * Rules: every question is used at most once. Questions are taken round-robin across subjects so the paper
 * covers as many subjects as possible. When mathMarks is above zero, Engineering Mathematics contributes as close
 * to that many marks as the bank allows, without going over. If the bank cannot fill the blueprint, the builder
 * uses what exists and reports what is missing instead of failing.
 */
public final class PaperBuilder {
    public static final String GA_SUBJECT = "General Aptitude";
    public static final String MATH_SUBJECT = "Engineering Mathematics";

    /** section is "GA" (General Aptitude) or "CORE" (subject questions). */
    public record Candidate(long id, String subject, String section, int marks) {
    }

    public record Blueprint(int ga1, int ga2, int core1, int core2, int mathMarks) {
        /** The real GATE pattern: 10 GA (5 x 1-mark, 5 x 2-mark) and 55 core (25 x 1-mark, 30 x 2-mark), 13 marks of Engineering Mathematics. */
        public static Blueprint gate() {
            return new Blueprint(5, 5, 25, 30, 13);
        }

        public int totalQuestions() {
            return ga1 + ga2 + core1 + core2;
        }

        public int totalMarks() {
            return ga1 + 2 * ga2 + core1 + 2 * core2;
        }
    }

    /** picked is in paper order. shortfall maps a label to the number of questions (or marks) the bank could not supply. */
    public record Result(List<Candidate> picked, Map<String, Integer> shortfall) {
        public boolean fullyBuilt() {
            return shortfall.isEmpty();
        }
    }

    private PaperBuilder() {
    }

    public static Result build(List<Candidate> pool, Blueprint bp, Random rnd) {
        Map<String, Integer> shortfall = new LinkedHashMap<>();

        List<Candidate> ga1 = spread(filter(pool, "GA", 1, null, false), bp.ga1(), rnd);
        List<Candidate> ga2 = spread(filter(pool, "GA", 2, null, false), bp.ga2(), rnd);
        shortage(shortfall, "General Aptitude 1-mark", bp.ga1(), ga1.size());
        shortage(shortfall, "General Aptitude 2-mark", bp.ga2(), ga2.size());

        boolean pinMath = bp.mathMarks() > 0;
        List<Candidate> core1 = new ArrayList<>();
        List<Candidate> core2 = new ArrayList<>();
        if (pinMath) {
            List<Candidate> math1 = filter(pool, "CORE", 1, MATH_SUBJECT, true);
            List<Candidate> math2 = filter(pool, "CORE", 2, MATH_SUBJECT, true);
            int[] mix = mathMix(bp.mathMarks(), math1.size(), math2.size(), bp.core1(), bp.core2());
            core1.addAll(spread(math1, mix[0], rnd));
            core2.addAll(spread(math2, mix[1], rnd));
            int got = mix[0] + 2 * mix[1];
            if (got < bp.mathMarks()) shortfall.put("Engineering Mathematics marks", bp.mathMarks() - got);
        }
        // Other core questions. When Engineering Mathematics is pinned to a target, it is kept out of this pool so the target is not exceeded.
        core1.addAll(spread(filter(pool, "CORE", 1, MATH_SUBJECT, !pinMath), bp.core1() - core1.size(), rnd));
        core2.addAll(spread(filter(pool, "CORE", 2, MATH_SUBJECT, !pinMath), bp.core2() - core2.size(), rnd));
        shortage(shortfall, "Core 1-mark", bp.core1(), core1.size());
        shortage(shortfall, "Core 2-mark", bp.core2(), core2.size());

        // Paper order: GA 1-mark, GA 2-mark, core 1-mark, core 2-mark, with subjects mixed inside each group.
        Collections.shuffle(ga1, rnd);
        Collections.shuffle(ga2, rnd);
        Collections.shuffle(core1, rnd);
        Collections.shuffle(core2, rnd);
        List<Candidate> picked = new ArrayList<>(ga1);
        picked.addAll(ga2);
        picked.addAll(core1);
        picked.addAll(core2);
        return new Result(picked, shortfall);
    }

    /** Chooses how many 1-mark (x) and 2-mark (y) questions give the most marks without going over the target. A tie prefers y close to 4. */
    static int[] mathMix(int target, int avail1, int avail2, int quota1, int quota2) {
        int max1 = Math.min(avail1, quota1);
        int max2 = Math.min(avail2, quota2);
        int bestX = 0, bestY = 0, bestMarks = -1;
        for (int y = 0; y <= max2; y++) {
            for (int x = 0; x <= max1; x++) {
                int m = x + 2 * y;
                if (m > target) continue;
                if (m > bestMarks || (m == bestMarks && Math.abs(y - 4) < Math.abs(bestY - 4))) {
                    bestMarks = m;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        return new int[]{bestX, bestY};
    }

    /** Candidates of a section and mark value. If subject is given, keeps only that subject (onlySubject = true) or drops it (false). */
    private static List<Candidate> filter(List<Candidate> pool, String section, int marks, String subject, boolean onlySubject) {
        List<Candidate> out = new ArrayList<>();
        for (Candidate c : pool) {
            if (!c.section().equals(section) || c.marks() != marks) continue;
            if (subject != null && c.subject().equals(subject) != onlySubject) continue;
            out.add(c);
        }
        return out;
    }

    /** Takes up to n candidates, rotating through the subjects in random order. */
    static List<Candidate> spread(List<Candidate> from, int n, Random rnd) {
        List<Candidate> shuffled = new ArrayList<>(from);
        Collections.shuffle(shuffled, rnd);
        Map<String, Deque<Candidate>> bySubject = new LinkedHashMap<>();
        for (Candidate c : shuffled) bySubject.computeIfAbsent(c.subject(), k -> new ArrayDeque<>()).add(c);
        List<String> order = new ArrayList<>(bySubject.keySet());
        Collections.shuffle(order, rnd);
        List<Candidate> out = new ArrayList<>();
        while (out.size() < n) {
            boolean took = false;
            for (String s : order) {
                if (out.size() >= n) break;
                Candidate c = bySubject.get(s).poll();
                if (c != null) {
                    out.add(c);
                    took = true;
                }
            }
            if (!took) break;
        }
        return out;
    }

    private static void shortage(Map<String, Integer> map, String label, int wanted, int got) {
        if (got < wanted) map.put(label, wanted - got);
    }
}
