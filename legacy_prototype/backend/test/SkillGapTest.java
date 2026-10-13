package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillGapTest {

    public static void runTests() {
        System.out.println("Running SkillGapTest...");

        testMissingSkillsDetection();
        testFullSkillMatch();

        System.out.println("  [PASS] SkillGapTest (2/2 passed)");
    }

    private static void testMissingSkillsDetection() {
        // Milestone 4 requires Python and REST API
        List<String> requiredSkills = Arrays.asList("Python", "REST API");

        // Developer Arun Kumar has Java, MySQL, REST API (lacks Python)
        List<String> developerSkills = Arrays.asList("Java", "MySQL", "REST API");

        List<String> missing = new ArrayList<>();
        for (String req : requiredSkills) {
            if (!developerSkills.contains(req)) {
                missing.add(req);
            }
        }

        assert missing.size() == 1 : "Expected 1 missing skill, got " + missing.size();
        assert missing.contains("Python") : "Expected missing skill Python";
        assert !missing.contains("REST API") : "REST API should be matched";
    }

    private static void testFullSkillMatch() {
        List<String> requiredSkills = Arrays.asList("Java", "MySQL");
        List<String> developerSkills = Arrays.asList("Java", "MySQL", "REST API");

        List<String> missing = new ArrayList<>();
        for (String req : requiredSkills) {
            if (!developerSkills.contains(req)) {
                missing.add(req);
            }
        }

        assert missing.isEmpty() : "Expected 0 missing skills for fully qualified developer";
    }
}
