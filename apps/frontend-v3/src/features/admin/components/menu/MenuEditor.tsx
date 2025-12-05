import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { MenuDto } from '@/lib/api/menu';
import { IconPicker } from './IconPicker';
import { useEffect } from 'react';

const formSchema = z.object({
  id: z.string().min(1, 'IDは必須です'), // ID is editable only on create typically, but here we might just auto-generate or let custom ID
  label: z.string().min(1, 'ラベルは必須です'),
  path: z.string().optional(),
  icon: z.string().optional(),
  parentId: z.string().optional(),
  sortOrder: z.coerce.number().optional(),
  requiredPermission: z.string().optional(),
  description: z.string().optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface MenuEditorProps {
  menu?: MenuDto | null; // null means create new (if we want that mode here, or we pass a blank object)
  onSave: (values: MenuDto) => Promise<void>;
  onCancel: () => void;
  isCreating?: boolean;
}

export function MenuEditor({ menu, onSave, onCancel, isCreating = false }: MenuEditorProps) {
  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
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
      children: menu?.children, // Preserve children if any
    });
  };

  return (
    <div className="p-4 border rounded-lg bg-card">
      <div className="mb-4">
        <h3 className="text-lg font-medium">
          {isCreating ? '新規メニュー作成' : 'メニュー編集'}
        </h3>
      </div>
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          <FormField
            control={form.control}
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

          <FormField
            control={form.control}
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
             <FormField
                control={form.control}
                name="icon"
                render={({ field }) => (
                <FormItem>
                    <FormLabel>アイコン</FormLabel>
                    <FormControl>
                    <IconPicker value={field.value} onChange={field.onChange} />
                    </FormControl>
                    <FormMessage />
                </FormItem>
                )}
            />
            <FormField
                control={form.control}
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

          <FormField
            control={form.control}
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

          <FormField
            control={form.control}
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
          
          <FormField
            control={form.control}
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

          <FormField
            control={form.control}
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
