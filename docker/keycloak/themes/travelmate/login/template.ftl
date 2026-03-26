<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html data-theme="light"<#if locale??> lang="${locale.currentLanguageTag}"<#if realm.internationalizationEnabled> dir="${(locale.rtl)?then('rtl','ltr')}"</#if></#if>>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css">
    <link href="${url.resourcesPath}/css/travelmate.css" rel="stylesheet" />
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script type="importmap">
        {
            "imports": {
                "rfc4648": "${url.resourcesCommonPath}/vendor/rfc4648/rfc4648.js"
            }
        }
    </script>
    <script type="module">
        import { startSessionPolling } from "${url.resourcesPath}/js/authChecker.js";
        startSessionPolling("${url.ssoLoginInOtherTabsUrl?no_esc}");
    </script>
    <#if authenticationSession??>
        <script type="module">
            import { checkAuthSession } from "${url.resourcesPath}/js/authChecker.js";
            checkAuthSession("${authenticationSession.authSessionIdHash}");
        </script>
    </#if>
</head>
<body>
    <nav class="container nav-bar">
        <ul>
            <li>
                <a href="http://localhost:8080/" class="nav-brand">
                    <img src="${url.resourcesPath}/img/logo.svg" alt="Travelmate" class="nav-logo">
                </a>
            </li>
            <li class="nav-toggle">
                <button type="button" aria-label="Menu" aria-expanded="false"
                        onclick="var n=this.closest('nav');n.classList.toggle('nav-open');this.setAttribute('aria-expanded',n.classList.contains('nav-open'))">
                    <svg width="20" height="20" viewBox="0 0 24 24" stroke="currentColor" fill="none" stroke-width="2" stroke-linecap="round"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
                </button>
            </li>
        </ul>
        <ul class="nav-links">
            <li><a href="http://localhost:8080/iam/signup">${msg("doRegister")}</a></li>
            <#if locale?? && locale.supported?size gt 1>
                <li class="nav-lang">
                    <#list locale.supported as l>
                        <a href="${l.url}"<#if locale.currentLanguageTag == l.languageTag> class="current-locale"</#if>>${l.label}</a><#if l_has_next> / </#if>
                    </#list>
                </li>
            </#if>
            <li><a href="http://localhost:8080/oauth2/authorization/keycloak">${msg("doLogIn")}</a></li>
        </ul>
    </nav>

    <main class="container">
        <article class="kc-card">
            <#if !(auth?has_content && auth.showUsername() && !auth.showResetCredentials())>
                <header>
                    <img src="${url.resourcesPath}/img/logo-mark.svg" alt="" class="kc-card-logo">
                    <h2><#nested "header"></h2>
                </header>
            <#else>
                <header>
                    <#nested "show-username">
                    <div class="kc-attempted-username">
                        <span>${auth.attemptedUsername}</span>
                        <a href="${url.loginRestartFlowUrl}" aria-label="${msg("restartLoginTooltip")}">&larr; ${msg("restartLoginTooltip")}</a>
                    </div>
                </header>
            </#if>

            <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="kc-alert kc-alert-${message.type}" role="alert">
                    ${kcSanitize(message.summary)?no_esc}
                </div>
            </#if>

            <#nested "form">

            <#if auth?has_content && auth.showTryAnotherWayLink()>
                <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post">
                    <input type="hidden" name="tryAnotherWay" value="on"/>
                    <a href="#" onclick="document.forms['kc-select-try-another-way-form'].requestSubmit();return false;">${msg("doTryAnotherWay")}</a>
                </form>
            </#if>

            <#nested "socialProviders">

            <#if displayInfo>
                <div class="kc-info">
                    <#nested "info">
                </div>
            </#if>
        </article>
    </main>

    <footer class="container">
        <small>&copy; Travelmate</small>
    </footer>
</body>
</html>
</#macro>
