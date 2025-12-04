import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  Card, 
  CardContent, 
  CardHeader, 
  CardTitle,
  Button,
  SectionHeading,
  Badge
} from '@mirel/ui';
import { 
  Building2, 
  Plus, 
  Users,
  Settings
} from 'lucide-react';
import { OrganizationTree } from '../components/OrganizationTree';
import { 
  getOrganizations, 
  getOrganizationTree, 
  getOrganizationUnitMembers 
} from '../api';
import type { OrganizationUnit } from '../types';

export function OrganizationManagementPage() {
  const [selectedOrgId, setSelectedOrgId] = useState<string | null>(null);
  const [selectedUnit, setSelectedUnit] = useState<OrganizationUnit | null>(null);

  // Fetch Organizations
  const { data: organizations } = useQuery({
    queryKey: ['organizations'],
    queryFn: getOrganizations,
  });

  // Set default organization
  React.useEffect(() => {
    if (organizations && organizations.length > 0 && !selectedOrgId) {
      setSelectedOrgId(organizations[0].organizationId);
    }
  }, [organizations, selectedOrgId]);

  // Fetch Tree
  const { data: treeData, isLoading: isTreeLoading } = useQuery({
    queryKey: ['organization-tree', selectedOrgId],
    queryFn: () => getOrganizationTree(selectedOrgId as string),
    enabled: !!selectedOrgId,
  });

  // Fetch Members
  const { data: members } = useQuery({
    queryKey: ['organization-members', selectedOrgId, selectedUnit?.unitId],
    queryFn: () => getOrganizationUnitMembers(selectedOrgId!, selectedUnit!.unitId),
    enabled: !!selectedOrgId && !!selectedUnit,
  });

  return (
    <div className="space-y-6 pb-16">
      <SectionHeading
        eyebrow={
          <span className="inline-flex items-center gap-2">
            <Building2 className="size-4" />
            システム管理
          </span>
        }
        title="組織管理"
        description="組織構造、部門、役職、およびメンバーの所属を管理します。"
      />

      <div className="grid grid-cols-12 gap-6 h-[calc(100vh-250px)] min-h-[600px]">
        {/* Left Sidebar: Organization Tree */}
        <div className="col-span-4 flex flex-col gap-4">
          <Card className="flex-1 flex flex-col overflow-hidden">
            <CardHeader className="py-4 px-4 border-b">
              <div className="flex items-center justify-between">
                <CardTitle className="text-base">組織階層</CardTitle>
                <Button size="sm" variant="ghost">
                  <Plus className="size-4" />
                </Button>
              </div>
            </CardHeader>
            <CardContent className="flex-1 overflow-y-auto p-2">
              {isTreeLoading ? (
                <div className="text-center py-4 text-muted-foreground">読み込み中...</div>
              ) : treeData ? (
                <OrganizationTree
                  units={treeData}
                  onSelect={setSelectedUnit}
                  selectedUnitId={selectedUnit?.unitId}
                />
              ) : (
                <div className="text-center py-4 text-muted-foreground">組織データがありません</div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Right Content: Unit Details */}
        <div className="col-span-8 flex flex-col gap-4">
          {selectedUnit ? (
            <Card className="flex-1 flex flex-col overflow-hidden">
              <CardHeader className="py-4 px-6 border-b">
                <div className="flex items-center justify-between">
                  <div className="space-y-1">
                    <CardTitle>{selectedUnit.name}</CardTitle>
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Badge variant="outline">{selectedUnit.unitType}</Badge>
                      <span>Code: {selectedUnit.code}</span>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm">
                      <Settings className="size-4 mr-2" />
                      設定
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="flex-1 overflow-y-auto p-6">
                <div className="space-y-6">
                  {/* Basic Info */}
                  <div>
                    <h3 className="text-sm font-medium mb-3">基本情報</h3>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <span className="text-muted-foreground block">有効期間</span>
                        <span>{selectedUnit.effectiveFrom || '-'} 〜 {selectedUnit.effectiveTo || '-'}</span>
                      </div>
                      <div>
                        <span className="text-muted-foreground block">ステータス</span>
                        <Badge variant={selectedUnit.isActive ? 'success' : 'neutral'}>
                          {selectedUnit.isActive ? '有効' : '無効'}
                        </Badge>
                      </div>
                    </div>
                  </div>

                  {/* Members */}
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <h3 className="text-sm font-medium flex items-center gap-2">
                        <Users className="size-4" />
                        所属メンバー
                      </h3>
                      <Button size="sm" variant="outline">
                        <Plus className="size-3 mr-1" />
                        メンバー追加
                      </Button>
                    </div>
                    
                    <div className="border rounded-md">
                      {members && members.length > 0 ? (
                        <table className="w-full text-sm">
                          <thead className="bg-muted/50">
                            <tr>
                              <th className="py-2 px-4 text-left font-medium">氏名</th>
                              <th className="py-2 px-4 text-left font-medium">役職</th>
                              <th className="py-2 px-4 text-left font-medium">区分</th>
                              <th className="py-2 px-4 text-left font-medium">権限</th>
                            </tr>
                          </thead>
                          <tbody>
                            {members.map(member => (
                              <tr key={member.id} className="border-t">
                                <td className="py-2 px-4">User ID: {member.userId}</td>
                                <td className="py-2 px-4">{member.jobTitle || '-'}</td>
                                <td className="py-2 px-4">
                                  <Badge variant="outline" className="text-xs">
                                    {member.positionType}
                                  </Badge>
                                </td>
                                <td className="py-2 px-4">
                                  {member.isManager && <Badge variant="info" className="text-xs mr-1">長</Badge>}
                                  {member.canApprove && <Badge variant="neutral" className="text-xs">承認</Badge>}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      ) : (
                        <div className="py-8 text-center text-muted-foreground">
                          メンバーがいません
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          ) : (
            <Card className="flex-1 flex items-center justify-center text-muted-foreground">
              <div className="text-center">
                <Building2 className="size-12 mx-auto mb-4 opacity-20" />
                <p>左側のツリーから組織を選択してください</p>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
