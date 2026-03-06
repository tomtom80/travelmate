<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>
    <#if section = "header">
        ${msg("emailForgotTitle")}
    <#elseif section = "form">
        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">
            <label for="username">
                <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
            </label>
            <input type="text" id="username" name="username" autofocus value="${(auth.attemptedUsername!'')}"
                   aria-invalid="<#if messagesPerField.existsError('username')>true</#if>" />
            <#if messagesPerField.existsError('username')>
                <small class="kc-field-error">
                    ${kcSanitize(messagesPerField.get('username'))?no_esc}
                </small>
            </#if>

            <button type="submit">${msg("doSubmit")}</button>
            <p style="text-align:center;margin-top:0.75rem;">
                <a href="${url.loginUrl}">${msg("backToLogin")}</a>
            </p>
        </form>
    <#elseif section = "info">
        <p class="kc-info-text">
            <#if realm.duplicateEmailsAllowed>
                ${msg("emailInstructionUsername")}
            <#else>
                ${msg("emailInstruction")}
            </#if>
        </p>
    </#if>
</@layout.registrationLayout>
