-- Add model_type column to sch_dic_model_header
ALTER TABLE sch_dic_model_header ADD COLUMN model_type VARCHAR(50) DEFAULT 'transaction';
