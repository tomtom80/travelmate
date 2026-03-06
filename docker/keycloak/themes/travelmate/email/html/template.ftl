<#macro emailLayout>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body style="margin:0;padding:0;background-color:#f8fafc;font-family:system-ui,-apple-system,'Segoe UI',Roboto,sans-serif;color:#1e293b;">
    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f8fafc;">
        <tr>
            <td align="center" style="padding:2rem 1rem;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:0.5rem;border:1px solid #e2e8f0;">
                    <!-- Header -->
                    <tr>
                        <td style="padding:1.5rem 2rem 1rem;text-align:center;border-bottom:1px solid #e2e8f0;">
                            <span style="font-size:1.25rem;font-weight:700;color:#1095c1;text-decoration:none;">Travelmate</span>
                        </td>
                    </tr>
                    <!-- Content -->
                    <tr>
                        <td style="padding:1.5rem 2rem;">
                            <#nested>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="padding:1rem 2rem;text-align:center;border-top:1px solid #e2e8f0;color:#94a3b8;font-size:0.8rem;">
                            Travelmate &mdash; Gemeinsam Reisen planen
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
</#macro>
