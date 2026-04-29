<#macro emailLayout>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body style="margin:0;padding:0;background:#eef4fb;font-family:'Manrope',-apple-system,system-ui,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;color:#162030;-webkit-font-smoothing:antialiased;line-height:1.5;">
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#eef4fb;padding:24px 12px;">
        <tr>
            <td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 18px 48px rgba(15,23,42,0.08);">
                    <!-- Slim header band: mark + wordmark + eyebrow -->
                    <tr>
                        <td style="background:linear-gradient(180deg,#f7fbff,#eef4fb);padding:24px 32px;border-bottom:1px solid #d8dee9;">
                            <table role="presentation" width="100%">
                                <tr>
                                    <td>
                                        <table role="presentation"><tr>
                                            <td style="vertical-align:middle;padding-right:8px;">
                                                <svg width="32" height="32" viewBox="0 0 40 40">
                                                    <circle cx="20" cy="20" r="19" fill="#3366CC"/>
                                                    <path d="M5 27 Q20 12 35 27" stroke="#99BBEE" stroke-width="1.6" fill="none" stroke-linecap="round"/>
                                                    <g fill="#fff">
                                                        <circle cx="13.5" cy="17" r="2"/><rect x="11.5" y="19.5" width="4" height="6.5" rx="1.5"/>
                                                        <circle cx="20" cy="14.5" r="2.2"/><rect x="17.8" y="17.3" width="4.4" height="7.2" rx="1.6"/>
                                                        <circle cx="26.5" cy="17" r="2"/><rect x="24.5" y="19.5" width="4" height="6.5" rx="1.5"/>
                                                    </g>
                                                </svg>
                                            </td>
                                            <td style="vertical-align:middle;font-weight:700;font-size:18px;letter-spacing:-0.01em;color:#162030;">
                                                <span style="color:#3366CC;">Travel</span>mate
                                            </td>
                                        </tr></table>
                                    </td>
                                    <td align="right" style="font-size:11px;color:#526071;letter-spacing:0.08em;text-transform:uppercase;font-weight:700;">
                                        ACCOUNT &middot; KONTO
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                        <td style="padding:32px 36px;">
                            <#nested>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="padding:0 36px 28px 36px;">
                            <table role="presentation" width="100%">
                                <tr>
                                    <td style="border-top:1px solid #d8dee9;padding-top:18px;font-size:11px;color:#8a98aa;line-height:1.6;">
                                        <strong style="color:#526071;">Travelmate</strong> &middot; Group trip planning for families &amp; friends
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
</#macro>
