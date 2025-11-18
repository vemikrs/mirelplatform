/**
 * ステンシル一覧ページ
 */
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { listStencils } from '../api/stencil-editor-api';
import type { StencilListItem, StencilCategory } from '../types';
import { Button, Card, CardHeader, CardTitle, CardContent, Badge } from '@mirel/ui';
import { Edit, Eye, FolderOpen, FileText, Calendar, User } from 'lucide-react';

export const StencilListPage: React.FC = () => {
  const navigate = useNavigate();
  const [categories, setCategories] = useState<StencilCategory[]>([]);
  const [stencils, setStencils] = useState<StencilListItem[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadStencils();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedCategory]);

  const loadStencils = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await listStencils(selectedCategory || undefined);
      setCategories(response.categories);
      setStencils(response.stencils);
    } catch (err) {
      console.error('ステンシル一覧取得エラー:', err);
      setError('ステンシル一覧の取得に失敗しました');
    } finally {
      setLoading(false);
    }
  };

  const handleView = (stencilId: string, serial: string) => {
    // stencilIdは既に/で始まっている（例: /springboot/service）
    navigate(`/promarker/editor${stencilId}/${serial}?mode=view`);
  };

  const handleEdit = (stencilId: string, serial: string) => {
    // stencilIdは既に/で始まっている（例: /springboot/service）
    navigate(`/promarker/editor${stencilId}/${serial}?mode=edit`);
  };

  const filteredStencils = selectedCategory
    ? stencils.filter(s => s.categoryId === selectedCategory)
    : stencils;

  return (
    <div className="container mx-auto p-6 space-y-6">
      {/* ヘッダー */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold">ステンシル管理</h1>
          <p className="text-muted-foreground mt-2">
            ステンシルの閲覧・編集・バージョン管理を行います
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => navigate('/promarker')}>
            ProMarkerへ戻る
          </Button>
        </div>
      </div>

      {/* カテゴリフィルター */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FolderOpen className="size-5" />
            カテゴリフィルター
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-2">
            <Button
              variant={selectedCategory === '' ? 'default' : 'outline'}
              onClick={() => setSelectedCategory('')}
              size="sm"
            >
              すべて ({stencils.length})
            </Button>
            {categories.map(cat => (
              <Button
                key={cat.id}
                variant={selectedCategory === cat.id ? 'default' : 'outline'}
                onClick={() => setSelectedCategory(cat.id)}
                size="sm"
              >
                {cat.name} ({cat.stencilCount})
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* ステンシル一覧 */}
      {loading ? (
        <div className="text-center py-8 text-muted-foreground">
          読み込み中...
        </div>
      ) : error ? (
        <div className="text-center py-8 text-red-600">
          {error}
        </div>
      ) : filteredStencils.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          ステンシルがありません
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filteredStencils.map(stencil => (
            <Card key={stencil.id} className="hover:shadow-lg transition-shadow">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <CardTitle className="flex items-center gap-2 text-lg">
                      <FileText className="size-5" />
                      {stencil.name}
                    </CardTitle>
                    <div className="mt-2 flex items-center gap-2 text-sm text-muted-foreground">
                      <Badge variant="secondary">{stencil.categoryName}</Badge>
                      <Badge variant="outline">v{stencil.latestSerial}</Badge>
                    </div>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {/* 説明 */}
                {stencil.description && (
                  <p className="text-sm text-muted-foreground line-clamp-2">
                    {stencil.description}
                  </p>
                )}

                {/* メタ情報 */}
                <div className="space-y-1 text-xs text-muted-foreground">
                  <div className="flex items-center gap-2">
                    <Calendar className="size-3" />
                    <span>更新: {new Date(stencil.lastUpdate).toLocaleString('ja-JP')}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <User className="size-3" />
                    <span>更新者: {stencil.lastUpdateUser}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <FileText className="size-3" />
                    <span>バージョン数: {stencil.versionCount}</span>
                  </div>
                </div>

                {/* アクションボタン */}
                <div className="flex gap-2 pt-2 border-t">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleView(stencil.id, stencil.latestSerial)}
                    className="flex-1"
                  >
                    <Eye className="mr-1 size-4" />
                    参照
                  </Button>
                  <Button
                    variant="default"
                    size="sm"
                    onClick={() => handleEdit(stencil.id, stencil.latestSerial)}
                    className="flex-1"
                  >
                    <Edit className="mr-1 size-4" />
                    編集
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};
