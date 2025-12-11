<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>アカウント作成完了 - パスワード設定のご案内</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans JP', sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .container {
            background-color: #f9f9f9;
            border-radius: 8px;
            padding: 30px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .header {
            text-align: center;
            margin-bottom: 30px;
        }
        .logo {
            font-size: 24px;
            font-weight: bold;
            color: #0066cc;
        }
        .info-box {
            background-color: #fff;
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 20px;
            margin: 20px 0;
        }
        .info-row {
            margin: 10px 0;
        }
        .info-label {
            font-weight: bold;
            color: #666;
            display: inline-block;
            width: 120px;
        }
        .info-value {
            color: #333;
        }
        .cta-button {
            text-align: center;
            margin: 30px 0;
        }
        .cta-button a {
            background-color: #0066cc;
            color: #ffffff;
            padding: 15px 40px;
            text-decoration: none;
            border-radius: 6px;
            font-weight: bold;
            font-size: 16px;
            display: inline-block;
        }
        .warning {
            background-color: #fff3cd;
            border-left: 4px solid #ffc107;
            padding: 15px;
            margin: 20px 0;
        }
        .expiry-info {
            background-color: #e7f3ff;
            border-left: 4px solid #0066cc;
            padding: 15px;
            margin: 20px 0;
        }
        .footer {
            text-align: center;
            margin-top: 30px;
            color: #666;
            font-size: 12px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="logo">mirelplatform</div>
        </div>
        
        <h2>アカウントが作成されました</h2>
        
        <p>こんにちは <strong>${displayName}</strong> 様、</p>
        <p>管理者によってアカウントが作成されました。以下の情報をご確認ください。</p>
        
        <div class="info-box">
            <div class="info-row">
                <span class="info-label">ユーザー名:</span>
                <span class="info-value"><strong>${username}</strong></span>
            </div>
            <div class="info-row">
                <span class="info-label">メールアドレス:</span>
                <span class="info-value">${email}</span>
            </div>
        </div>
        
        <p>アカウントを有効化するには、以下のボタンをクリックしてパスワードを設定してください。</p>
        
        <div class="cta-button">
            <a href="${setupLink}">パスワードを設定する</a>
        </div>
        
        <div class="expiry-info">
            <strong>📅 有効期限について</strong>
            <p style="margin: 10px 0 0 0;">このリンクは <strong>72時間</strong> 有効です。期限が切れた場合は、管理者に再発行を依頼してください。</p>
        </div>
        
        <div class="warning">
            <strong>⚠️ セキュリティのお願い</strong>
            <ul style="margin: 10px 0 0 0; padding-left: 20px;">
                <li>このリンクは一度のみ使用できます</li>
                <li>第三者と共有しないでください</li>
                <li>強力なパスワードを設定してください（8文字以上推奨）</li>
            </ul>
        </div>
        
        <p>このメールに心当たりがない場合は、管理者までお問い合わせください。</p>
        
        <div class="footer">
            <p>このメールはシステムから自動送信されています。返信はできません。</p>
            <p>&copy; 2025 mirelplatform. All rights reserved.</p>
        </div>
    </div>
</body>
</html>
