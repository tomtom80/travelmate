<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-new','password-confirm'); section>
    <#if section = "header">
        ${msg("updatePasswordTitle")}
    <#elseif section = "form">
        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">
            <input type="text" id="username" name="username" value="${username}" autocomplete="username" readonly="readonly" style="display:none;"/>
            <input type="password" id="password" name="password" autocomplete="current-password" style="display:none;"/>

            <label for="password-new">${msg("passwordNew")}</label>
            <input tabindex="2" type="password" id="password-new" name="password-new" autofocus autocomplete="new-password"
                   aria-invalid="<#if messagesPerField.existsError('password','password-new')>true</#if>" />
            <#if messagesPerField.existsError('password-new')>
                <small class="kc-field-error">
                    ${kcSanitize(messagesPerField.get('password-new'))?no_esc}
                </small>
            </#if>

            <label for="password-confirm">${msg("passwordConfirm")}</label>
            <input tabindex="3" type="password" id="password-confirm" name="password-confirm" autocomplete="new-password"
                   aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>" />
            <#if messagesPerField.existsError('password-confirm')>
                <small class="kc-field-error">
                    ${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}
                </small>
            </#if>

            <#if isAppInitiatedAction??>
                <div class="kc-form-actions">
                    <button tabindex="4" type="submit">${msg("doSubmit")}</button>
                    <button tabindex="5" type="submit" name="cancel-aia" value="true" class="outline secondary">${msg("doCancel")}</button>
                </div>
            <#else>
                <button tabindex="4" type="submit">${msg("doSubmit")}</button>
            </#if>
        </form>
    </#if>
</@layout.registrationLayout>
