import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Button, Input, Textarea } from '@mirel/ui';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from './ShadcnForm';
import type { MenuDto } from '@/lib/api/menu';
import { IconPicker } from './IconPicker';
import { useEffect } from 'react';

const formSchema = z.object({
  id: z.string().min(1, 'IDは必須です'), // ID is editable only on create typically, but here we might just auto-generate or let custom ID
  label: z.string().min(1, 'ラベルは必須です'),
  path: z.string().optional(),
  icon: z.string().optional(),
  parentId: z.string().optional(),
  sortOrder: z.coerce.number().optional().default(10),
  requiredPermission: z.string().optional(),
  description: z.string().optional(),
});

type FormValues = {
    id: string;
    label: string;
    path?: string;
    icon?: string;
    parentId?: string;
    sortOrder?: number;
    requiredPermission?: string;
    description?: string;
};

interface MenuEditorProps {
  menu?: MenuDto | null; // null means create new (if we want that mode here, or we pass a blank object)
  onSave: (values: MenuDto) => Promise<void>;
  onCancel: () => void;
  isCreating?: boolean;
}

export function MenuEditor({ menu, onSave, onCancel, isCreating = false }: MenuEditorProps) {
  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema) as any,
    defaultValues: {
      id: '',
      label: '',
      path: '',
      icon: '',
      parentId: '',
      sortOrder: 10,
      requiredPermission: '',
      description: '',
    },
  });

  useEffect(() => {
    if (menu) {
      form.reset({
        id: menu.id,
        label: menu.label,
        path: menu.path || '',
        icon: menu.icon || '',
        parentId: menu.parentId || '',
        sortOrder: menu.sortOrder || 10,
        requiredPermission: menu.requiredPermission || '',
        description: menu.description || '',
      });
    } else {
        form.reset({
            id: '',
            label: '',
            path: '',
            icon: '',
            parentId: '',
            sortOrder: 10,
            requiredPermission: '',
            description: '',
        });
    }
  }, [menu, form]);

  const onSubmit = async (values: FormValues) => {
    await onSave({
      ...values,
      path: values.path || '',
      children: menu?.children,
    } as MenuDto);
  };

  return (
    <div className="p-4 border rounded-lg bg-card">
      <div className="mb-4">
        <h3 className="text-lg font-medium">
          {isCreating ? '新規メニュー作成' : 'メニュー編集'}
        </h3>
      </div>
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit as any)} className="space-y-4">
          <FormField<FormValues>
            control={form.control as any}
            name="id"
            render={({ field }) => (
              <FormItem>
                <FormLabel>ID</FormLabel>
                <FormControl>
                  <Input {...field} disabled={!isCreating} placeholder="menu-id" />
                </FormControl>
                <FormDescription>一意のIDを入力してください。</FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField<FormValues>
            control={form.control as any}
            name="label"
            render={({ field }) => (
              <FormItem>
                <FormLabel>ラベル</FormLabel>
                <FormControl>
                  <Input {...field} placeholder="メニュー表示名" />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <div className="grid grid-cols-2 gap-4">
             <FormField<FormValues>
                control={form.control as any}
                name="icon"
                render={({ field }) => (
                <FormItem>
                    <FormLabel>アイコン</FormLabel>
                    <FormControl>
                    <IconPicker value={field.value as string | undefined} onChange={field.onChange} />
                    </FormControl>
                    <FormMessage />
                </FormItem>
                )}
            />
            <FormField<FormValues>
                control={form.control as any}
                name="sortOrder"
                render={({ field }) => (
                <FormItem>
                    <FormLabel>並び順</FormLabel>
                    <FormControl>
                    <Input type="number" {...field} />
                    </FormControl>
                    <FormMessage />
                </FormItem>
                )}
            />
          </div>

          <FormField<FormValues>
            control={form.control as any}
            name="path"
            render={({ field }) => (
              <FormItem>
                <FormLabel>パス</FormLabel>
                <FormControl>
                  <Input {...field} placeholder="/example/path" />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField<FormValues>
            control={form.control as any}
            name="requiredPermission"
            render={({ field }) => (
              <FormItem>
                <FormLabel>必要な権限 (例: ADMIN|TENANT_ADMIN)</FormLabel>
                <FormControl>
                  <Input {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          
          <FormField<FormValues>
            control={form.control as any}
            name="parentId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>親メニューID (任意)</FormLabel>
                <FormControl>
                    {/* Ideally this would be a tree selector, but text is fine for simplified v1 */}
                  <Input {...field} placeholder="parent-id" />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField<FormValues>
            control={form.control as any}
            name="description"
            render={({ field }) => (
              <FormItem>
                <FormLabel>説明</FormLabel>
                <FormControl>
                  <Textarea {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="ghost" onClick={onCancel}>
              キャンセル
            </Button>
            <Button type="submit">保存</Button>
          </div>
        </form>
      </Form>
    </div>
  );
}
