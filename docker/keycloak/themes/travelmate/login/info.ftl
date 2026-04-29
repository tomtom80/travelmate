<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeader??>
            ${kcSanitize(msg("${messageHeader}"))?no_esc}
        <#else>
            ${message.summary}
        </#if>
    <#elseif section = "form">
        <p>${message.summary}<#if requiredActions??><#list requiredActions>: <strong><#items as reqActionItem>${kcSanitize(msg("requiredAction.${reqActionItem}"))?no_esc}<#sep>, </#items></strong></#list><#else></#if></p>
        <#if skipLink??>
        <#else>
            <#if pageRedirectUri?has_content>
                <script>window.location.href = "${pageRedirectUri?no_esc}";</script>
                <p><a href="${pageRedirectUri}" role="button">${kcSanitize(msg("backToApplication"))?no_esc}</a></p>
            <#elseif actionUri?has_content>
                <p><a href="${actionUri}" role="button">${kcSanitize(msg("proceedWithAction"))?no_esc}</a></p>
            <#elseif (client.baseUrl)?has_content>
                <script>window.location.href = "${client.baseUrl?no_esc}";</script>
                <p><a href="${client.baseUrl}" role="button">${kcSanitize(msg("backToApplication"))?no_esc}</a></p>
            <#else>
                <#assign sectionClientBaseUrl = "" />
                <#if client?? && client.baseUrl?has_content>
                    <#assign sectionClientBaseUrl = client.baseUrl />
                </#if>
                <#assign sectionRealmAppBaseUrl = "" />
                <#if realm.attributes?? && realm.attributes["appBaseUrl"]?? && realm.attributes["appBaseUrl"]?has_content>
                    <#assign sectionRealmAppBaseUrl = realm.attributes["appBaseUrl"] />
                </#if>
                <#assign sectionAppBaseUrl = (sectionClientBaseUrl?has_content)?then(sectionClientBaseUrl?replace("/iam/?$", "", "r"), sectionRealmAppBaseUrl) />
                <#assign sectionAppLoginUrl = sectionAppBaseUrl + "/oauth2/authorization/keycloak" />
                <p><a href="${sectionAppLoginUrl}" role="button">${kcSanitize(msg("doLogIn"))?no_esc}</a></p>
            </#if>
        </#if>
    </#if>
</@layout.registrationLayout>
