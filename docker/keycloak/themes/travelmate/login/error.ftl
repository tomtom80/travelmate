<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${kcSanitize(msg("errorTitle"))?no_esc}
    <#elseif section = "form">
        <div class="kc-alert kc-alert-error" role="alert">
            ${kcSanitize(message.summary)?no_esc}
        </div>
        <#if skipLink??>
        <#else>
            <#if client?? && client.baseUrl?has_content>
                <p><a id="backToApplication" href="${client.baseUrl}" role="button">${kcSanitize(msg("backToApplication"))?no_esc}</a></p>
            </#if>
        </#if>
    </#if>
</@layout.registrationLayout>
