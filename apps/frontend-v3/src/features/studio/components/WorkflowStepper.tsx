import React from 'react';
import { cn } from '@mirel/ui';

interface Step {
  id: string;
  label: string;
  description: string;
}

const steps: Step[] = [
  { id: 'model', label: '1. Define Model', description: 'Create entities & relations' },
  { id: 'form', label: '2. Design Form', description: 'Build UI & layout' },
  { id: 'flow', label: '3. Create Flow', description: 'Add business logic' },
  { id: 'release', label: '4. Release', description: 'Publish to runtime' },
];

export const WorkflowStepper: React.FC<{ className?: string }> = ({ className }) => {
  return (
    <div className={cn("w-full py-6", className)}>
      <div className="relative flex items-center justify-between w-full max-w-4xl mx-auto">
        {/* Connecting Line */}
        <div className="absolute top-4 left-0 w-full h-0.5 bg-border -z-10" />

        {steps.map((step, index) => (
          <div key={step.id} className="flex flex-col items-center bg-background px-4">
            <div className={`
              w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm mb-2
              ${index === 0 ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground border border-border'}
            `}>
              {index + 1}
            </div>
            <div className="text-center">
              <div className="font-medium text-sm">{step.label.split('. ')[1]}</div>
              <div className="text-xs text-muted-foreground hidden sm:block">{step.description}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
