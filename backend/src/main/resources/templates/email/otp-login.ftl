<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ログイン認証コード</title>
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
        .otp-code {
            background-color: #fff;
            border: 2px solid #0066cc;
            border-radius: 8px;
            padding: 20px;
            text-align: center;
            margin: 30px 0;
        }
        .code {
            font-size: 36px;
            font-weight: bold;
            letter-spacing: 8px;
            color: #0066cc;
            font-family: 'Courier New', monospace;
        }
        .expiry {
            color: #666;
            font-size: 14px;
            margin-top: 10px;
        }
        .warning {
            background-color: #fff3cd;
            border-left: 4px solid #ffc107;
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
        
        <h2>ログイン認証コード</h2>
        
        <p>こんにちは、</p>
        <p>以下の認証コードを入力してログインを完了してください。</p>
        
        <div class="otp-code">
            <div class="code">${otpCode}</div>
            <div class="expiry">有効期限: ${expirationMinutes}分</div>
        </div>
        
        <div class="warning">
            <strong>⚠️ セキュリティのお願い</strong>
            <ul style="margin: 10px 0 0 0; padding-left: 20px;">
                <li>このコードは誰にも教えないでください</li>
                <li>心当たりがない場合は無視してください</li>
                <li>リクエストしていない場合は、パスワードを変更してください</li>
            </ul>
        </div>
        
        <p>このメールに心当たりがない場合は、無視していただいて構いません。</p>
        
        <div class="footer">
            <p>このメールはシステムから自動送信されています。返信はできません。</p>
            <p>&copy; 2025 mirelplatform. All rights reserved.</p>
        </div>
    </div>
</body>
</html>
