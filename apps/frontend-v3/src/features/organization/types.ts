export type UnitType = 
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

export interface Organization {
  organizationId: string;
  name: string;
  code: string;
  description?: string;
  fiscalYearStart?: number;
  isActive: boolean;
}

export interface OrganizationUnit {
  unitId: string;
  organizationId: string;
  parentUnitId?: string;
  name: string;
  code: string;
  unitType: UnitType;
  level: number;
  sortOrder: number;
  isActive: boolean;
  effectiveFrom?: string;
  effectiveTo?: string;
  children?: OrganizationUnit[];
}

export interface UserOrganization {
  id: string;
  userId: string;
  unitId: string;
  positionType: PositionType;
  jobTitle?: string;
  jobGrade?: number;
  isManager: boolean;
  canApprove: boolean;
  startDate?: string;
  endDate?: string;
  unitName?: string;
  unitCode?: string;
}
