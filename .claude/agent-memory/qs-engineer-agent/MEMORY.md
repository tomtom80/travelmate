# QA Engineer Agent Memory

## Test Statistics
- Total tests: 185 (12 common, 113 IAM, 59 Trips, 1 Expense)
- E2E tests: 7 test classes in travelmate-e2e

## E2E Test Classes
- SignUpIT, AuthenticationFlowIT, LandingPageIT, NavigationIT
- DashboardMemberIT, TripLifecycleIT

## Test Patterns
- Domain: plain JUnit 5 + AssertJ
- Application: Mockito (@ExtendWith(MockitoExtension.class))
- Persistence: @SpringBootTest + @ActiveProfiles("test") + H2
- Controller: @SpringBootTest + @AutoConfigureMockMvc + @MockitoBean
- E2E: Playwright Java API extending E2ETestBase
