# QA Engineer Agent Memory

## Test Statistics
- Total tests: 265 (15 common, 161 IAM, 89 Trips)
- E2E tests: 6 test classes in travelmate-e2e (SignUpIT, AuthenticationFlowIT, LandingPageIT, NavigationIT, DashboardMemberIT, TripLifecycleIT)

## Test Patterns
- Domain: plain JUnit 5 + AssertJ
- Application: Mockito (@ExtendWith(MockitoExtension.class))
- Persistence: @SpringBootTest + @ActiveProfiles("test") + H2
- Controller: @SpringBootTest + @AutoConfigureMockMvc + @MockitoBean
- E2E: Playwright Java API extending E2ETestBase

## Known E2E Test Base Issues (2026-03-13)
- signUpAndLogin() masks Bug #1 (verification page detour) — it has a conditional workaround at lines 100-113
- getVerificationLinkFromMailpit() returns null on failure instead of throwing — mail delivery failures are silent
- No E2E test clicks the delete-tenant button and asserts outcome
- No E2E test asserts visible user feedback (success/error messages) after HTMX actions

## Gap Analysis (2026-03-13)
- Full analysis at: docs/test-cases/test-strategy-gap-analysis.md
- 8 bugs found manually, none by 265 automated tests
- Root cause: tests cover building blocks in isolation, not user-visible behavior at integration boundaries
- Missing: E2E for email verification flow, password reset flow, tenant delete action, external invite feedback, mail delivery assertion, error feedback assertions
- BDD scenarios written in docs/test-cases/bdd/
