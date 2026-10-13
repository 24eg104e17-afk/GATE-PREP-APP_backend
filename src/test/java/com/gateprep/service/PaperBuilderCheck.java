package com.gateprep.service;

import com.gateprep.service.PaperBuilder.Blueprint;
import com.gateprep.service.PaperBuilder.Candidate;
import com.gateprep.service.PaperBuilder.Result;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Self-checking run of the paper builder (no test framework needed):
 *   javac -d out src/main/java/com/gateprep/service/PaperBuilder.java src/test/java/com/gateprep/service/PaperBuilderCheck.java
 *   java -cp out com.gateprep.service.PaperBuilderCheck
 */
public class PaperBuilderCheck {
    static int failures = 0;
    static final String[] CORE = {"Engineering Mathematics", "Networks, Signals and Systems", "Electronic Devices",
            "Analog Circuits", "Digital Circuits", "Control Systems", "Communications", "Electromagnetics"};

    static void check(boolean ok, String what) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + what);
        if (!ok) failures++;
    }

    static long count(List<Candidate> l, String section, int marks) {
        return l.stream().filter(c -> c.section().equals(section) && c.marks() == marks).count();
    }

    static int marks(List<Candidate> l) {
        return l.stream().mapToInt(Candidate::marks).sum();
    }

    static List<Candidate> bigBank() {
        List<Candidate> p = new ArrayList<>();
        long id = 1;
        for (int m = 1; m <= 2; m++) for (int i = 0; i < 8; i++) p.add(new Candidate(id++, PaperBuilder.GA_SUBJECT, "GA", m));
        for (String s : CORE) for (int m = 1; m <= 2; m++) for (int i = 0; i < 10; i++) p.add(new Candidate(id++, s, "CORE", m));
        return p;
    }

    /** Same size as the 45-question bank in the prototype: GA 5+5, core 14 one-mark and 21 two-mark. */
    static List<Candidate> smallBank() {
        List<Candidate> p = new ArrayList<>();
        long id = 1;
        for (int m = 1; m <= 2; m++) for (int i = 0; i < 5; i++) p.add(new Candidate(id++, PaperBuilder.GA_SUBJECT, "GA", m));
        int[] ones = {3, 3, 2, 2, 1, 1, 1, 1};   // 14 one-mark questions across the 8 core subjects
        int[] twos = {2, 4, 3, 3, 3, 2, 2, 2};   // 21 two-mark questions
        for (int s = 0; s < CORE.length; s++) {
            for (int i = 0; i < ones[s]; i++) p.add(new Candidate(id++, CORE[s], "CORE", 1));
            for (int i = 0; i < twos[s]; i++) p.add(new Candidate(id++, CORE[s], "CORE", 2));
        }
        return p;
    }

    static int mathMarks(List<Candidate> l) {
        return l.stream().filter(c -> c.subject().equals(PaperBuilder.MATH_SUBJECT)).mapToInt(Candidate::marks).sum();
    }

    public static void main(String[] args) {
        System.out.println("--- Full GATE pattern from a large bank ---");
        List<Candidate> big = bigBank();
        int bigSize = big.size();
        Result r = PaperBuilder.build(big, Blueprint.gate(), new Random(1));
        List<Candidate> pk = r.picked();
        check(pk.size() == 65, "65 questions (got " + pk.size() + ")");
        check(marks(pk) == 100, "100 marks (got " + marks(pk) + ")");
        check(r.fullyBuilt(), "fully built, nothing missing");
        check(count(pk, "GA", 1) == 5 && count(pk, "GA", 2) == 5, "General Aptitude is 5 x 1-mark + 5 x 2-mark");
        check(count(pk, "CORE", 1) == 25 && count(pk, "CORE", 2) == 30, "Core is 25 x 1-mark + 30 x 2-mark");
        check(mathMarks(pk) == 13, "Engineering Mathematics is 13 marks (got " + mathMarks(pk) + ")");
        Set<Long> ids = pk.stream().map(Candidate::id).collect(Collectors.toSet());
        check(ids.size() == pk.size(), "no question is used twice");
        boolean order = true;
        for (int i = 0; i < 65; i++) {
            Candidate c = pk.get(i);
            String want = i < 5 ? "GA1" : i < 10 ? "GA2" : i < 35 ? "C1" : "C2";
            String got = (c.section().equals("GA") ? "GA" : "C") + c.marks();
            if (!want.equals(got)) order = false;
        }
        check(order, "paper order: GA 1-mark, GA 2-mark, core 1-mark, core 2-mark");
        long subjects = pk.stream().filter(c -> c.section().equals("CORE")).map(Candidate::subject).distinct().count();
        check(subjects == 8, "core questions cover all 8 subjects (got " + subjects + ")");
        check(big.size() == bigSize, "the input pool is not changed");
        List<Long> a = PaperBuilder.build(bigBank(), Blueprint.gate(), new Random(7)).picked().stream().map(Candidate::id).toList();
        List<Long> b = PaperBuilder.build(bigBank(), Blueprint.gate(), new Random(7)).picked().stream().map(Candidate::id).toList();
        List<Long> c2 = PaperBuilder.build(bigBank(), Blueprint.gate(), new Random(8)).picked().stream().map(Candidate::id).toList();
        check(a.equals(b), "same seed gives the same paper");
        check(!a.equals(c2), "different seed gives a different paper");

        System.out.println("--- Small bank (45 questions, like the prototype) ---");
        List<Candidate> small = smallBank();
        Result s = PaperBuilder.build(small, Blueprint.gate(), new Random(3));
        check(small.size() == 45, "bank has 45 questions");
        check(s.picked().size() == 45, "uses all 45 available (got " + s.picked().size() + ")");
        check(!s.fullyBuilt(), "reported as not fully built");
        check(Integer.valueOf(11).equals(s.shortfall().get("Core 1-mark")), "Core 1-mark short by 11 (got " + s.shortfall().get("Core 1-mark") + ")");
        check(Integer.valueOf(9).equals(s.shortfall().get("Core 2-mark")), "Core 2-mark short by 9 (got " + s.shortfall().get("Core 2-mark") + ")");
        check(!s.shortfall().containsKey("General Aptitude 1-mark") && !s.shortfall().containsKey("General Aptitude 2-mark"), "General Aptitude is complete");
        check(s.picked().stream().map(Candidate::id).distinct().count() == 45, "no question used twice");
        System.out.println("      shortfall report: " + s.shortfall());

        System.out.println("--- Custom blueprints ---");
        Result cu = PaperBuilder.build(bigBank(), new Blueprint(2, 1, 4, 3, 0), new Random(5));
        check(cu.picked().size() == 10 && marks(cu.picked()) == 14, "custom 2+1+4+3 gives 10 questions and 14 marks (got " + cu.picked().size() + " and " + marks(cu.picked()) + ")");
        check(cu.fullyBuilt(), "custom paper fully built");
        List<Candidate> noMath = bigBank().stream().filter(x -> !x.subject().equals(PaperBuilder.MATH_SUBJECT)).collect(Collectors.toList());
        Result nm = PaperBuilder.build(noMath, Blueprint.gate(), new Random(2));
        check(Integer.valueOf(13).equals(nm.shortfall().get("Engineering Mathematics marks")), "no Engineering Mathematics in bank is reported as 13 marks short");
        check(nm.picked().size() == 65, "core is still filled from other subjects (got " + nm.picked().size() + ")");
        List<Candidate> noGa = bigBank().stream().filter(x -> !x.section().equals("GA")).collect(Collectors.toList());
        Result ng = PaperBuilder.build(noGa, Blueprint.gate(), new Random(2));
        check(Integer.valueOf(5).equals(ng.shortfall().get("General Aptitude 1-mark")) && ng.picked().size() == 55, "missing General Aptitude is reported (got " + ng.picked().size() + " questions)");
        Result mo = PaperBuilder.build(bigBank(), new Blueprint(0, 0, 6, 0, 0), new Random(4));
        check(mo.picked().size() == 6 && mo.fullyBuilt(), "when math is not pinned, Engineering Mathematics is treated like any subject");

        System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED");
        if (failures > 0) System.exit(1);
    }
}
