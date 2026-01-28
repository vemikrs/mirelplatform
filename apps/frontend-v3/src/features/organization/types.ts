// ============================================================
// 組織種別
// ============================================================

export type OrganizationType = 
  | 'COMPANY'
  | 'DIVISION'
  | 'DEPARTMENT'
  | 'SECTION'
  | 'TEAM'
  | 'PROJECT'
  | 'VIRTUAL';

export type PositionType = 
  | 'PRIMARY'
  | 'SECONDARY'
  | 'TEMPORARY';

// ============================================================
// 統合組織エンティティ
// ============================================================

export interface Organization {
  id: string;
  tenantId: string;
  parentId?: string;
  name: string;
  displayName?: string;
  code?: string;
  type: OrganizationType;
  path?: string;
  level: number;
  sortOrder?: number;
  isActive: boolean;
  startDate?: string;
  endDate?: string;
  periodCode?: string;
  children?: Organization[];
  companySettings?: CompanySettings;
  organizationSettings?: OrganizationSettings;
}

// ============================================================
// 設定エンティティ
// ============================================================

export interface CompanySettings {
  id: string;
  organizationId: string;
  periodCode?: string;
  fiscalYearStart?: number;
  currencyCode?: string;
  timezone?: string;
  locale?: string;
}

export interface OrganizationSettings {
  id: string;
  organizationId: string;
  periodCode?: string;
  allowFlexibleSchedule?: boolean;
  requireApproval?: boolean;
  maxMemberCount?: number;
  extendedSettings?: Record<string, unknown>;
}

// ============================================================
// ユーザー所属
// ============================================================

export interface UserOrganization {
  id: string;
  userId: string;
  organizationId: string; // 旧: unitId
  positionType: PositionType;
  role?: string; // 旧: isManager
  jobTitle?: string;
  jobGrade?: number;
  canApprove: boolean;
  startDate?: string;
  endDate?: string;
  organizationName?: string; // 旧: unitName
  organizationCode?: string; // 旧: unitCode
}
