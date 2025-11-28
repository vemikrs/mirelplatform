import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  Badge, 
  Button, 
  Card, 
  CardContent, 
  CardHeader, 
  CardTitle,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Input,
  Label,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Textarea,
  Switch,
  SectionHeading,
} from '@mirel/ui';
import { 
  Plus, 
  Search, 
  Edit2, 
  Trash2, 
  Flag, 
  RefreshCw,
  AlertTriangle,
  CheckCircle,
  Clock,
  Beaker,
  Archive,
} from 'lucide-react';
import {
  getFeatures,
  createFeature,
  updateFeature,
  deleteFeature,
  checkFeatureKeyExists,
  getStatusLabel,
  type FeatureFlag,
  type FeatureStatus,
  type LicenseTier,
  type CreateFeatureFlagRequest,
  type UpdateFeatureFlagRequest,
} from '@/lib/api/features';

// Status badge configuration
const statusConfig: Record<FeatureStatus, { icon: React.ReactNode; color: string }> = {
  STABLE: { icon: <CheckCircle className="size-3.5" />, color: 'green' },
  BETA: { icon: <Beaker className="size-3.5" />, color: 'yellow' },
  ALPHA: { icon: <AlertTriangle className="size-3.5" />, color: 'orange' },
  PLANNING: { icon: <Clock className="size-3.5" />, color: 'gray' },
  DEPRECATED: { icon: <Archive className="size-3.5" />, color: 'red' },
};

export function AdminFeaturesPage() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = React.useState('');
  const [statusFilter, setStatusFilter] = React.useState<FeatureStatus | 'ALL'>('ALL');
  const [inDevFilter, setInDevFilter] = React.useState<boolean | undefined>(undefined);
  const [page, setPage] = React.useState(0);
  const [selectedFeature, setSelectedFeature] = React.useState<FeatureFlag | null>(null);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = React.useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = React.useState(false);
  const [featureToDelete, setFeatureToDelete] = React.useState<FeatureFlag | null>(null);

  // Fetch features
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['admin-features', page, searchTerm, statusFilter, inDevFilter],
    queryFn: () => getFeatures({
      page,
      size: 20,
      q: searchTerm || undefined,
      status: statusFilter === 'ALL' ? undefined : statusFilter,
      inDevelopment: inDevFilter,
    }),
  });

  // Create mutation
  const createMutation = useMutation({
    mutationFn: createFeature,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-features'] });
      setIsCreateDialogOpen(false);
    },
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateFeatureFlagRequest }) =>
      updateFeature(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-features'] });
      setSelectedFeature(null);
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: deleteFeature,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-features'] });
      setIsDeleteDialogOpen(false);
      setFeatureToDelete(null);
    },
  });

  const handleDeleteClick = (feature: FeatureFlag) => {
    setFeatureToDelete(feature);
    setIsDeleteDialogOpen(true);
  };

  const handleConfirmDelete = () => {
    if (featureToDelete) {
      deleteMutation.mutate(featureToDelete.id);
    }
  };

  return (
    <div className="space-y-6 pb-16">
      <SectionHeading
        eyebrow={
          <span className="inline-flex items-center gap-2">
            <Flag className="size-4" />
            システム管理
          </span>
        }
        title="フィーチャーフラグ管理"
        description="機能の有効化/無効化、段階的ロールアウト、ライセンス連携を管理します。"
        actions={
          <Button 
            onClick={() => setIsCreateDialogOpen(true)}
            className="gap-2"
          >
            <Plus className="size-4" />
            新規作成
          </Button>
        }
      />

      {/* Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-wrap gap-4">
            <div className="flex-1 min-w-[200px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
                <Input
                  placeholder="キーワードで検索..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-9"
                />
              </div>
            </div>
            <Select 
              value={statusFilter} 
              onValueChange={(v) => setStatusFilter(v as FeatureStatus | 'ALL')}
            >
              <SelectTrigger className="w-[160px]">
                <SelectValue placeholder="ステータス" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">すべて</SelectItem>
                <SelectItem value="STABLE">安定版</SelectItem>
                <SelectItem value="BETA">ベータ版</SelectItem>
                <SelectItem value="ALPHA">アルファ版</SelectItem>
                <SelectItem value="PLANNING">計画中</SelectItem>
                <SelectItem value="DEPRECATED">非推奨</SelectItem>
              </SelectContent>
            </Select>
            <Select 
              value={inDevFilter === undefined ? 'ALL' : inDevFilter.toString()}
              onValueChange={(v) => setInDevFilter(v === 'ALL' ? undefined : v === 'true')}
            >
              <SelectTrigger className="w-[160px]">
                <SelectValue placeholder="開発状態" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">すべて</SelectItem>
                <SelectItem value="true">開発中のみ</SelectItem>
                <SelectItem value="false">リリース済み</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" onClick={() => refetch()} className="gap-2">
              <RefreshCw className="size-4" />
              更新
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Feature List */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            <span>フィーチャーフラグ一覧</span>
            {data && (
              <span className="text-sm font-normal text-muted-foreground">
                {data.totalElements} 件
              </span>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : error ? (
            <div className="flex items-center justify-center py-8 text-destructive">
              エラーが発生しました
            </div>
          ) : data?.features.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
              <Flag className="size-12 mb-4 opacity-50" />
              <p>フィーチャーフラグがありません</p>
            </div>
          ) : (
            <div className="space-y-3">
              {data?.features.map((feature) => (
                <FeatureRow
                  key={feature.id}
                  feature={feature}
                  onEdit={() => setSelectedFeature(feature)}
                  onDelete={() => handleDeleteClick(feature)}
                />
              ))}
            </div>
          )}

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-6">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                前へ
              </Button>
              <span className="text-sm text-muted-foreground px-4">
                {page + 1} / {data.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= data.totalPages - 1}
                onClick={() => setPage(page + 1)}
              >
                次へ
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Create Dialog */}
      <FeatureFormDialog
        open={isCreateDialogOpen}
        onOpenChange={setIsCreateDialogOpen}
        onSubmit={(data) => createMutation.mutate(data)}
        isLoading={createMutation.isPending}
        title="新規フィーチャーフラグ作成"
      />

      {/* Edit Dialog */}
      {selectedFeature && (
        <FeatureFormDialog
          open={!!selectedFeature}
          onOpenChange={(open) => !open && setSelectedFeature(null)}
          onSubmit={(data) => updateMutation.mutate({ id: selectedFeature.id, data })}
          isLoading={updateMutation.isPending}
          title="フィーチャーフラグ編集"
          defaultValues={selectedFeature}
          isEdit
        />
      )}

      {/* Delete Confirmation Dialog */}
      <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>フィーチャーフラグの削除</DialogTitle>
            <DialogDescription>
              「{featureToDelete?.featureName}」を削除しますか？この操作は取り消せません。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsDeleteDialogOpen(false)}
            >
              キャンセル
            </Button>
            <Button
              variant="destructive"
              onClick={handleConfirmDelete}
              disabled={deleteMutation.isPending}
            >
              {deleteMutation.isPending ? '削除中...' : '削除する'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// Feature Row Component
interface FeatureRowProps {
  feature: FeatureFlag;
  onEdit: () => void;
  onDelete: () => void;
}

function FeatureRow({ feature, onEdit, onDelete }: FeatureRowProps) {
  const status = statusConfig[feature.status];
  
  return (
    <div 
      className="flex items-center gap-4 p-4 rounded-lg border bg-card hover:bg-accent/50 transition-colors"
    >
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-1">
          <span className="font-medium truncate">{feature.featureName}</span>
          <Badge variant={status.color as 'green' | 'yellow' | 'neutral'} className="gap-1">
            {status.icon}
            {getStatusLabel(feature.status)}
          </Badge>
          {feature.inDevelopment && (
            <Badge variant="warning" className="gap-1">
              <Clock className="size-3" />
              開発中
            </Badge>
          )}
        </div>
        <div className="flex items-center gap-4 text-sm text-muted-foreground">
          <code className="text-xs bg-muted px-1.5 py-0.5 rounded">
            {feature.featureKey}
          </code>
          <span>{feature.applicationId}</span>
          {feature.requiredLicenseTier && (
            <Badge variant="outline" className="text-xs">
              {feature.requiredLicenseTier}
            </Badge>
          )}
        </div>
        {feature.description && (
          <p className="text-sm text-muted-foreground mt-1 truncate">
            {feature.description}
          </p>
        )}
      </div>
      <div className="flex items-center gap-2">
        <Button variant="ghost" size="icon" onClick={onEdit}>
          <Edit2 className="size-4" />
        </Button>
        <Button variant="ghost" size="icon" onClick={onDelete}>
          <Trash2 className="size-4 text-destructive" />
        </Button>
      </div>
    </div>
  );
}

// Feature Form Dialog Component
interface FeatureFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (data: CreateFeatureFlagRequest | UpdateFeatureFlagRequest) => void;
  isLoading: boolean;
  title: string;
  defaultValues?: FeatureFlag;
  isEdit?: boolean;
}

function FeatureFormDialog({
  open,
  onOpenChange,
  onSubmit,
  isLoading,
  title,
  defaultValues,
  isEdit = false,
}: FeatureFormDialogProps) {
  const [formData, setFormData] = React.useState<CreateFeatureFlagRequest>({
    featureKey: defaultValues?.featureKey ?? '',
    featureName: defaultValues?.featureName ?? '',
    description: defaultValues?.description ?? '',
    applicationId: defaultValues?.applicationId ?? 'mirelplatform',
    status: defaultValues?.status ?? 'STABLE',
    inDevelopment: defaultValues?.inDevelopment ?? false,
    requiredLicenseTier: defaultValues?.requiredLicenseTier,
    enabledByDefault: defaultValues?.enabledByDefault ?? true,
    rolloutPercentage: defaultValues?.rolloutPercentage ?? 100,
    metadata: defaultValues?.metadata ?? '',
  });
  const [keyError, setKeyError] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (defaultValues) {
      setFormData({
        featureKey: defaultValues.featureKey,
        featureName: defaultValues.featureName,
        description: defaultValues.description ?? '',
        applicationId: defaultValues.applicationId,
        status: defaultValues.status,
        inDevelopment: defaultValues.inDevelopment,
        requiredLicenseTier: defaultValues.requiredLicenseTier,
        enabledByDefault: defaultValues.enabledByDefault,
        rolloutPercentage: defaultValues.rolloutPercentage,
        metadata: defaultValues.metadata ?? '',
      });
    }
  }, [defaultValues]);

  const handleKeyChange = async (key: string) => {
    setFormData({ ...formData, featureKey: key });
    if (!isEdit && key.length > 2) {
      const exists = await checkFeatureKeyExists(key);
      setKeyError(exists ? 'このキーは既に使用されています' : null);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (keyError) return;
    onSubmit(formData);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
          </DialogHeader>
          
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="featureKey">機能キー *</Label>
                <Input
                  id="featureKey"
                  value={formData.featureKey}
                  onChange={(e) => handleKeyChange(e.target.value)}
                  placeholder="app.feature_name"
                  disabled={isEdit}
                  required
                />
                {keyError && (
                  <p className="text-sm text-destructive">{keyError}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="featureName">機能名 *</Label>
                <Input
                  id="featureName"
                  value={formData.featureName}
                  onChange={(e) => setFormData({ ...formData, featureName: e.target.value })}
                  placeholder="新機能"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">説明</Label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="この機能の説明..."
                rows={2}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="applicationId">アプリケーションID *</Label>
                <Select
                  value={formData.applicationId}
                  onValueChange={(v) => setFormData({ ...formData, applicationId: v })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="mirelplatform">mirelplatform</SelectItem>
                    <SelectItem value="promarker">promarker</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="status">ステータス</Label>
                <Select
                  value={formData.status}
                  onValueChange={(v) => setFormData({ ...formData, status: v as FeatureStatus })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="STABLE">安定版</SelectItem>
                    <SelectItem value="BETA">ベータ版</SelectItem>
                    <SelectItem value="ALPHA">アルファ版</SelectItem>
                    <SelectItem value="PLANNING">計画中</SelectItem>
                    <SelectItem value="DEPRECATED">非推奨</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="requiredLicenseTier">必要ライセンス</Label>
                <Select
                  value={formData.requiredLicenseTier ?? 'NONE'}
                  onValueChange={(v) => setFormData({ 
                    ...formData, 
                    requiredLicenseTier: v === 'NONE' ? undefined : v as LicenseTier 
                  })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="NONE">なし</SelectItem>
                    <SelectItem value="FREE">FREE</SelectItem>
                    <SelectItem value="TRIAL">TRIAL</SelectItem>
                    <SelectItem value="PRO">PRO</SelectItem>
                    <SelectItem value="MAX">MAX</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="rolloutPercentage">ロールアウト比率</Label>
                <Input
                  id="rolloutPercentage"
                  type="number"
                  min={0}
                  max={100}
                  value={formData.rolloutPercentage}
                  onChange={(e) => setFormData({ 
                    ...formData, 
                    rolloutPercentage: parseInt(e.target.value) || 0 
                  })}
                />
              </div>
            </div>

            <div className="flex items-center gap-8">
              <div className="flex items-center gap-2">
                <Switch
                  id="enabledByDefault"
                  checked={formData.enabledByDefault}
                  onCheckedChange={(v) => setFormData({ ...formData, enabledByDefault: v })}
                />
                <Label htmlFor="enabledByDefault">デフォルトで有効</Label>
              </div>
              <div className="flex items-center gap-2">
                <Switch
                  id="inDevelopment"
                  checked={formData.inDevelopment}
                  onCheckedChange={(v) => setFormData({ ...formData, inDevelopment: v })}
                />
                <Label htmlFor="inDevelopment">開発中</Label>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="metadata">メタデータ (JSON)</Label>
              <Textarea
                id="metadata"
                value={formData.metadata}
                onChange={(e) => setFormData({ ...formData, metadata: e.target.value })}
                placeholder='{"key": "value"}'
                rows={3}
                className="font-mono text-sm"
              />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              キャンセル
            </Button>
            <Button type="submit" disabled={isLoading || !!keyError}>
              {isLoading ? '保存中...' : isEdit ? '更新する' : '作成する'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
