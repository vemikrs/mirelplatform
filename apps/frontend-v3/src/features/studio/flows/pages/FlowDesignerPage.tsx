import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { FlowDesignerContainer } from '../../components/FlowDesigner/FlowDesignerContainer';
import { useFlowDesignerStore } from '../../stores/useFlowDesignerStore';
import { StudioLayout } from '../../layouts';
import { StudioContextBar, ModeSwitcher } from '../../components';
import { StudioNavigation } from '../../components/StudioNavigation';
import { useToast } from '@mirel/ui';
import { Edit, Workflow, Eye } from 'lucide-react';

export const FlowDesignerPage: React.FC = () => {
    const { flowId } = useParams<{ flowId: string }>();
    const navigate = useNavigate();
    const { toast } = useToast();
    const { loadFlow, saveFlow } = useFlowDesignerStore();
    const [modelName, setModelName] = useState(flowId || 'Flow');

    useEffect(() => {
        if (flowId) {
            loadFlow(flowId);
            setModelName(flowId); // Fetch name logic if available
        }
    }, [flowId, loadFlow]);

    const handleSave = async () => {
        if (flowId) {
            try {
                await saveFlow(flowId, modelName);
                toast({
                    title: 'Success',
                    description: 'Flow saved successfully',
                    variant: 'success',
                });
            } catch (error) {
                toast({
                    title: 'Error',
                    description: 'Failed to save flow',
                    variant: 'destructive',
                });
            }
        }
    };

    const modes = [
        { id: 'edit', label: 'Form', icon: Edit },
        { id: 'flow', label: 'Flow', icon: Workflow },
        { id: 'preview', label: 'Preview', icon: Eye },
    ];

    return (
        <StudioLayout
            navigation={<StudioNavigation className="h-full border-r" />}
            hideContextBar={true}
        >
            <div className="flex flex-col h-full w-full">
                <StudioContextBar
                    title={modelName}
                    breadcrumbs={[
                        { label: 'Studio', href: '/apps/studio' },
                        { label: 'Flows', href: '/apps/studio/flows' },
                        { label: modelName },
                    ]}
                    actions={
                        <ModeSwitcher
                            modes={modes}
                            activeMode="flow"
                            onModeChange={(mode) => {
                                if (mode === 'edit') navigate(`/apps/studio/forms/${flowId}?mode=edit`);
                                if (mode === 'preview') navigate(`/apps/studio/forms/${flowId}?mode=preview`);
                            }}
                        />
                    }
                    onSave={handleSave}
                    showSave={true}
                />
                <FlowDesignerContainer />
            </div>
        </StudioLayout>
    );
};
