<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>パスワードリセット</title>
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
        .button-container {
            text-align: center;
            margin: 30px 0;
        }
        .reset-button {
            background-color: #dc3545;
            color: #ffffff;
            padding: 15px 30px;
            text-decoration: none;
            border-radius: 6px;
            font-weight: bold;
            display: inline-block;
            font-size: 16px;
        }
        .reset-button:hover {
            background-color: #c82333;
        }
        .expiry {
            color: #666;
            font-size: 14px;
            margin-top: 20px;
            text-align: center;
        }
        .warning {
            background-color: #f8d7da;
            border-left: 4px solid #dc3545;
            padding: 15px;
            margin: 20px 0;
        }
        .footer {
            text-align: center;
            margin-top: 30px;
            color: #666;
            font-size: 12px;
            border-top: 1px solid #ddd;
            padding-top: 20px;
        }
        .alternative-link {
            background-color: #fff;
            border: 1px solid #ddd;
            border-radius: 4px;
            padding: 15px;
            margin: 20px 0;
            word-break: break-all;
            font-size: 12px;
            color: #666;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="logo">mirelplatform</div>
        </div>
        
        <h2>パスワードリセットのリクエスト</h2>
        
        <p>こんにちは、</p>
        <p>パスワードリセットのリクエストを受け付けました。以下のボタンをクリックして、新しいパスワードを設定してください。</p>
        
        <div class="button-container">
            <a href="${resetLink}" class="reset-button">パスワードをリセット</a>
        </div>
        
        <div class="expiry">
            このリンクは <strong>${expirationHours}時間</strong> 有効です
        </div>
        
        <div class="warning">
            <strong>⚠️ 重要なセキュリティ通知</strong>
            <ul style="margin: 10px 0 0 0; padding-left: 20px;">
                <li><strong>パスワードリセットをリクエストしていない場合は、このメールを無視してください</strong></li>
                <li>このリンクは誰にも共有しないでください</li>
                <li>不審なアクティビティを検知した場合は、すぐにサポートにご連絡ください</li>
            </ul>
        </div>
        
        <p>このリクエストに心当たりがない場合は、第三者による不正アクセスの可能性があります。すぐにパスワードを変更し、サポートチームにご連絡ください。</p>
        
        <div class="alternative-link">
            <p style="margin: 0 0 10px 0;"><strong>ボタンが機能しない場合</strong></p>
            <p style="margin: 0;">以下のURLをコピーしてブラウザに貼り付けてください：</p>
            <p style="margin: 10px 0 0 0;">${resetLink}</p>
        </div>
        
        <div class="footer">
            <p>このメールはシステムから自動送信されています。返信はできません。</p>
            <p>&copy; 2025 mirelplatform. All rights reserved.</p>
        </div>
    </div>
</body>
</html>
