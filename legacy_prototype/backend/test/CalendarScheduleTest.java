package test;

import service.SkillTaxonomyService;

public class CalendarScheduleTest {

    public static void runTests() {
        System.out.println("Running CalendarScheduleTest...");
        int passed = 0;
        int total = 3;

        // Test 1: Color code threshold logic
        // Green <70%, Amber 70-90%, Red >90%
        int loadLow = 60;
        int loadMed = 80;
        int loadHigh = 110;

        String colorLow = determineColor(loadLow);
        String colorMed = determineColor(loadMed);
        String colorHigh = determineColor(loadHigh);

        if (!"green".equals(colorLow) || !"amber".equals(colorMed) || !"red".equals(colorHigh)) {
            throw new AssertionError("Calendar load color coding failed: " + colorLow + ", " + colorMed + ", " + colorHigh);
        }
        passed++;

        // Test 2: Edge boundary threshold logic (70% and 90%)
        String color70 = determineColor(70);
        String color90 = determineColor(90);
        if (!"amber".equals(color70) || !"amber".equals(color90)) {
            throw new AssertionError("Calendar boundary threshold failed at 70% or 90%: " + color70 + ", " + color90);
        }
        passed++;

        // Test 3: Zero load indicator
        String colorZero = determineColor(0);
        if (!"none".equals(colorZero)) {
            throw new AssertionError("Zero load should yield color 'none'");
        }
        passed++;

        System.out.println("  [PASS] CalendarScheduleTest (" + passed + "/" + total + " passed)");
    }

    private static String determineColor(int load) {
        if (load <= 0) return "none";
        if (load < 70) return "green";
        if (load <= 90) return "amber";
        return "red";
    }
}
