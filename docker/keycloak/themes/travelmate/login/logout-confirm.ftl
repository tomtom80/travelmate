<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("logoutConfirmTitle")}
    <#elseif section = "form">
        <p>${msg("logoutConfirmHeader")}</p>

        <form id="kc-logout-confirm" action="${url.logoutConfirmAction}" method="POST">
            <input type="hidden" name="session_code" value="${logoutConfirm.code}">
            <button name="confirmLogout" id="kc-logout" type="submit">${msg("doLogout")}</button>
        </form>

        <#if !logoutConfirm.skipLink && (client.baseUrl)?has_content>
            <p style="text-align:center;margin-top:1rem;">
                <a href="${client.baseUrl}">${kcSanitize(msg("backToApplication"))?no_esc}</a>
            </p>
        </#if>

        <script>document.getElementById('kc-logout-confirm').submit();</script>
    </#if>
</@layout.registrationLayout>
