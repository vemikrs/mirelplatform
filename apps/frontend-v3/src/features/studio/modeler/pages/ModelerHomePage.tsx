import React from 'react';
import { Link } from 'react-router-dom';
import { ModelerLayout } from '../components/layout/ModelerLayout';

export const ModelerHomePage: React.FC = () => {
  return (
    <ModelerLayout>
      <div className="p-6 space-y-6">
        <h1 className="text-2xl font-bold text-foreground">スキーマ管理アプリケーション</h1>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Link
            to="/apps/studio/modeler/models"
            className="block p-6 bg-card border border-border rounded-lg shadow-sm hover:bg-accent hover:text-accent-foreground transition-colors"
          >
            <h2 className="text-xl font-semibold mb-2 text-card-foreground">モデル定義</h2>
            <p className="text-muted-foreground">データモデルとフィールドを定義します。</p>
          </Link>
          <Link
            to="/apps/studio/modeler/records"
            className="block p-6 bg-card border border-border rounded-lg shadow-sm hover:bg-accent hover:text-accent-foreground transition-colors"
          >
            <h2 className="text-xl font-semibold mb-2 text-card-foreground">データ管理</h2>
            <p className="text-muted-foreground">定義されたモデルのレコードを管理します。</p>
          </Link>
          <Link
            to="/apps/studio/modeler/codes"
            className="block p-6 bg-card border border-border rounded-lg shadow-sm hover:bg-accent hover:text-accent-foreground transition-colors"
          >
            <h2 className="text-xl font-semibold mb-2 text-card-foreground">コードマスタ</h2>
            <p className="text-muted-foreground">選択肢のコード値を管理します。</p>
          </Link>
        </div>
      </div>
    </ModelerLayout>
  );
};
