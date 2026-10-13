package test;

import service.SkillTaxonomyService;

public class SkillTaxonomyTest {

    public static void runTests() {
        System.out.println("Running SkillTaxonomyTest...");
        SkillTaxonomyService service = new SkillTaxonomyService();
        int passed = 0;
        int total = 4;

        // Test 1: React synonyms resolve to canonical "React"
        String r1 = service.resolveCanonicalSkill("react.js");
        String r2 = service.resolveCanonicalSkill("reactjs");
        String r3 = service.resolveCanonicalSkill("React Native");
        if (!"React".equals(r1) || !"React".equals(r2) || !"React".equals(r3)) {
            throw new AssertionError("React synonym resolution failed: " + r1 + ", " + r2 + ", " + r3);
        }
        passed++;

        // Test 2: Kubernetes & Docker synonyms
        String k8s = service.resolveCanonicalSkill("k8s");
        String docker = service.resolveCanonicalSkill("docker compose");
        if (!"Kubernetes".equals(k8s) || !"Docker".equals(docker)) {
            throw new AssertionError("Containerization synonym resolution failed: " + k8s + ", " + docker);
        }
        passed++;

        // Test 3: Known Taxonomy detection
        if (!service.isKnownTaxonomySkill("AWS") || !service.isKnownTaxonomySkill("Java")) {
            throw new AssertionError("Known taxonomy check failed for core skills");
        }
        passed++;

        // Test 4: Unknown skill preservation & capitalization
        String unknown = service.resolveCanonicalSkill("quantum computing algorithm");
        if (!"Quantum Computing Algorithm".equals(unknown)) {
            throw new AssertionError("Unknown skill formatting failed: " + unknown);
        }
        passed++;

        System.out.println("  [PASS] SkillTaxonomyTest (" + passed + "/" + total + " passed)");
    }
}
