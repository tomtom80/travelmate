## Component: Inline Action Feedback
**Page**: All pages — any HTMX form submission that does not navigate away
**PicoCSS Base**: `<p role="alert">` inside the HTMX swap target, or OOB swap into a dedicated `#feedback` region

---

### Design Decision: Inline Fragment, Not Toast

The architect proposes a toast notification system using `HX-Trigger: showToast` headers and JavaScript to render an auto-dismissing `<div id="toast-container">`. This should be **rejected for the primary feedback mechanism** and adopted only as a secondary, non-critical channel.

**Why toast is the wrong primary pattern here:**

1. **Errors must not auto-dismiss.** The core complaint is "errors silently fail." An auto-dismissing toast that shows an error message for 3–5 seconds and then vanishes does not solve silent failure — it replaces it with ephemeral failure. A user who looked away or is on a slow device will see nothing. Errors must persist until the user acknowledges them or a new action supersedes them.

2. **The feedback belongs next to the action.** In this app, HTMX always replaces a named region (`#invitations`, `#members`, `#companions`). The natural place for feedback is *within* that region — the server can return a fragment that starts with a success or error notice, followed by the updated list. This is server-rendered, requires zero JavaScript, degrades gracefully, and is always in focus after the swap.

3. **Toast requires JavaScript to function at all.** The app's philosophy is progressive enhancement. An inline fragment returned in the HTMX response works even if the JS toast library fails to load or the `HX-Trigger` header is lost.

4. **Accessibility.** A `role="alert"` inserted into the DOM after an HTMX swap is announced by screen readers automatically because it enters the live region. A toast injected into a floating `#toast-container` requires the container to already be in the DOM with `aria-live="polite"` and relies on correct JS timing. The inline approach is more reliable.

**Where toast IS appropriate:**

- Non-critical, transient success confirmations that complement an already-visible state change (e.g., "Einladung gesendet" after the invitation list has visibly updated). In this case toast is additive, not the only signal.
- Globally-scoped notifications that have no natural DOM anchor (e.g., a background RabbitMQ event arrives and the user should know — not a current use case).

---

### Inline Feedback Pattern (Recommended)

The HTMX response for any mutation returns a fragment structured as:

```html
<!-- Server returns this as the full replacement for #invitations (or any target) -->
<div th:fragment="invitationList">

  <!-- 1. Feedback notice — present only when there is something to say -->
  <p th:if="${successMessage}" role="alert" class="success-notice"
     th:text="${successMessage}"></p>
  <p th:if="${errorMessage}" role="alert" class="error-notice"
     th:text="${errorMessage}"></p>

  <!-- 2. Updated content follows -->
  <table th:if="${!invitations.isEmpty()}">
    ...
  </table>
  <p th:if="${invitations.isEmpty()}" th:text="#{invitation.empty}"></p>

</div>
```

The feedback notice sits at the top of the refreshed region, immediately visible because HTMX scrolls to the target by default. It is part of the page's normal flow — no floating layers, no JS timers, no accessibility hacks.

**CSS additions required (in `static/css/style.css`):**

```css
.success-notice {
    color: var(--pico-ins-color, #2d6a2d);
    background: #f0fdf4;
    border: 1px solid #bbf7d0;
    padding: 0.75rem 1rem;
    border-radius: var(--pico-border-radius);
    margin-bottom: 1rem;
}

/* .error class already exists — alias it */
.error-notice {
    /* same as .error */
    color: #991b1b;
    background: var(--tm-danger-bg);
    border: 1px solid var(--tm-danger-border);
    padding: 0.75rem 1rem;
    border-radius: var(--pico-border-radius);
    margin-bottom: 1rem;
}
```

---

### Global Error Handler (keep the architect's proposal, modify the target)

The `GlobalExceptionHandler` returning error fragments for HTMX requests is correct. The modification needed: instead of returning a fragment that targets `#toast-container`, the handler should return a fragment that replaces the HTMX request's own `hx-target`. This requires reading the `HX-Target` request header in the handler:

```
If HX-Target header present → return error fragment for that target
If no HX-Target (regular request) → return full error page
```

This means the error appears exactly where the user was looking, not in a floating overlay.

---

### States

| State | Visual | Where rendered |
|-------|--------|---------------|
| Success | Green bordered notice, `role="alert"`, persists until next action | Top of HTMX swap target |
| Error (validation) | Red bordered notice, `role="alert"`, persists | Top of HTMX swap target |
| Error (server fault) | Same red notice with generic message | Top of HTMX swap target (via GlobalExceptionHandler) |
| Loading | `aria-busy="true"` on the submit button; button text stays readable | Submit button during HTMX request |

### Loading State Specification

The reported bug "participant save button grows larger after submit" is caused by the browser reflowing the button while the HTMX request is in flight. The fix: add `hx-indicator` pointing to a spinner element, and set `aria-busy` on the button itself.

```html
<button type="submit"
        th:text="#{common.save}"
        style="white-space: nowrap; min-width: 6rem;"
        hx-indicator="closest form">Speichern</button>
```

The `white-space: nowrap` and `min-width` prevent the label from wrapping. The real fix is moving the stay-period editing out of the table cell entirely (see `stay-period-row.md`).

---

### Accessibility

- `role="alert"` on feedback notices ensures screen reader announcement on DOM insertion
- Do not use `aria-live="assertive"` on success messages — `role="alert"` (which implies `aria-live="assertive"`) is correct for errors; for success, consider `role="status"` (implies `aria-live="polite"`) so screen readers are not interrupted mid-sentence
- Feedback notices must not rely on colour alone — always include an icon glyph or text prefix ("Erfolg:", "Fehler:") for colour-blind users
