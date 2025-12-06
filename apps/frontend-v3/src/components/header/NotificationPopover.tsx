import { Bell } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { 
  Button, 
  Popover, 
  PopoverContent, 
  PopoverTrigger,
  Tooltip,
  TooltipTrigger,
  TooltipContent
} from '@mirel/ui';
import { getUnreadCount } from '@/lib/api/announcement';
import { NotificationList } from '@/features/home/components/NotificationList';

interface NotificationPopoverProps {
  /** コンパクト表示（サイドバー折りたたみ時） */
  isCompact?: boolean;
}

/**
 * 通知ポップオーバーコンポーネント
 */
export function NotificationPopover({ isCompact = false }: NotificationPopoverProps) {
  const { data } = useQuery({
    queryKey: ['unread-count'],
    queryFn: getUnreadCount,
    refetchInterval: 60000, // 1 minute
  });

  const count = data?.count || 0;

  const triggerButton = (
    <Button 
      variant="ghost" 
      size="icon"
      aria-label="通知" 
      className="relative size-8"
    >
      <Bell className="size-4" />
      {count > 0 && (
        <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center">
          {count > 99 ? '99+' : count}
        </span>
      )}
    </Button>
  );

  if (isCompact) {
    return (
      <Popover>
        <Tooltip>
          <TooltipTrigger asChild>
            <PopoverTrigger asChild>
              {triggerButton}
            </PopoverTrigger>
          </TooltipTrigger>
          <TooltipContent side="right">
            通知 {count > 0 && `(${count})`}
          </TooltipContent>
        </Tooltip>
        <PopoverContent className="w-[380px] p-0" side="right" align="end">
          <div className="p-4 border-b border-border">
            <h4 className="font-semibold leading-none">お知らせ</h4>
          </div>
          <div className="p-2">
            <NotificationList variant="popover" />
          </div>
        </PopoverContent>
      </Popover>
    );
  }

  return (
    <Popover>
      <PopoverTrigger asChild>
        {triggerButton}
      </PopoverTrigger>
      <PopoverContent className="w-[380px] p-0" side="right" align="end">
        <div className="p-4 border-b border-border">
          <h4 className="font-semibold leading-none">お知らせ</h4>
        </div>
        <div className="p-2">
          <NotificationList variant="popover" />
        </div>
      </PopoverContent>
    </Popover>
  );
}
