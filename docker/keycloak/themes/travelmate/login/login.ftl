<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=true; section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
        <#if realm.password>
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                <#if !usernameHidden??>
                    <label for="username">
                        <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
                    </label>
                    <input tabindex="2" id="username" name="username" value="${(login.username!'')}" type="text" autofocus autocomplete="username"
                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                           <#if messagesPerField.existsError('username','password')>aria-describedby="input-error"</#if>
                    />
                    <#if messagesPerField.existsError('username','password')>
                        <small class="kc-field-error" id="input-error">
                            ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                        </small>
                    </#if>
                </#if>

                <label for="password">${msg("password")}</label>
                <div class="kc-password-wrapper">
                    <input tabindex="3" id="password" name="password" type="password" autocomplete="current-password"
                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                    />
                </div>
                <#if usernameHidden?? && messagesPerField.existsError('username','password')>
                    <small class="kc-field-error">
                        ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                    </small>
                </#if>

                <div class="kc-form-options">
                    <#if realm.rememberMe && !usernameHidden??>
                        <label class="kc-remember-me">
                            <input tabindex="5" id="rememberMe" name="rememberMe" type="checkbox" <#if login.rememberMe??>checked</#if>>
                            ${msg("rememberMe")}
                        </label>
                    </#if>
                    <#if realm.resetPasswordAllowed>
                        <a tabindex="6" href="${url.loginResetCredentialsUrl}" class="kc-forgot-password">${msg("doForgotPassword")}</a>
                    </#if>
                </div>

                <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                <button tabindex="7" name="login" id="kc-login" type="submit">${msg("doLogIn")}</button>
            </form>
        </#if>
    <#elseif section = "info">
        <hr>
        <p class="kc-registration-link">
            ${msg("noAccount")} <a tabindex="8" href="http://localhost:8080/iam/signup">${msg("doRegister")}</a>
        </p>
    <#elseif section = "socialProviders">
        <#if realm.password && social?? && social.providers?has_content>
            <hr>
            <h3>${msg("identity-provider-login-label")}</h3>
            <div class="grid">
                <#list social.providers as p>
                    <a id="social-${p.alias}" href="${p.loginUrl}" role="button" class="outline secondary">${p.displayName!}</a>
                </#list>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
