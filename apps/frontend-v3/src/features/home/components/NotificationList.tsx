import { Card, CardContent, CardHeader, CardTitle, Button, Badge } from '@mirel/ui';
import { Bell, Info, AlertTriangle, CheckCircle, AlertCircle, Circle, MailOpen, Mail } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getAnnouncements, markAsRead, markAsUnread } from '@/lib/api/announcement';
import { format } from 'date-fns';

const severityConfig = {
  INFO: { icon: Info, color: 'text-blue-500', bg: 'bg-blue-50 dark:bg-blue-900/20', border: 'border-blue-200 dark:border-blue-800' },
  RELEASE: { icon: CheckCircle, color: 'text-green-500', bg: 'bg-green-50 dark:bg-green-900/20', border: 'border-green-200 dark:border-green-800' },
  MAINTENANCE: { icon: AlertTriangle, color: 'text-amber-500', bg: 'bg-amber-50 dark:bg-amber-900/20', border: 'border-amber-200 dark:border-amber-800' },
  ALERT: { icon: AlertCircle, color: 'text-red-500', bg: 'bg-red-50 dark:bg-red-900/20', border: 'border-red-200 dark:border-red-800' },
  TERMS: { icon: Info, color: 'text-purple-500', bg: 'bg-purple-50 dark:bg-purple-900/20', border: 'border-purple-200 dark:border-purple-800' },
  EVENT: { icon: Circle, color: 'text-pink-500', bg: 'bg-pink-50 dark:bg-pink-900/20', border: 'border-pink-200 dark:border-pink-800' },
};

interface NotificationListProps {
  variant?: 'widget' | 'popover';
  onItemClick?: () => void;
}

export function NotificationList({ variant = 'widget', onItemClick }: NotificationListProps) {
  const queryClient = useQueryClient();
  
  const { data, isLoading } = useQuery({
    queryKey: ['announcements'],
    queryFn: getAnnouncements,
  });

  const markReadMutation = useMutation({
    mutationFn: markAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['announcements'] });
      queryClient.invalidateQueries({ queryKey: ['unread-count'] });
    },
  });

  const markUnreadMutation = useMutation({
    mutationFn: markAsUnread,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['announcements'] });
      queryClient.invalidateQueries({ queryKey: ['unread-count'] });
    },
  });

  const handleToggleRead = (e: React.MouseEvent, id: string, isRead: boolean) => {
    e.stopPropagation();
    if (isRead) {
      markUnreadMutation.mutate(id);
    } else {
      markReadMutation.mutate(id);
    }
  };

  if (isLoading) {
    return (
      <div className="p-4 space-y-4">
        <div className="h-20 bg-muted/20 rounded-lg animate-pulse" />
        <div className="h-20 bg-muted/20 rounded-lg animate-pulse" />
      </div>
    );
  }

  const announcements = data?.items || [];
  const readIds = new Set(data?.readIds || []);

  const content = (
    <div className={`space-y-3 ${variant === 'popover' ? 'max-h-[400px]' : 'max-h-[500px]'} overflow-y-auto pr-2 custom-scrollbar`}>
      {announcements.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground text-sm">
          お知らせはありません
        </div>
      ) : (
        announcements.map((notification) => {
          const config = severityConfig[notification.category] || severityConfig.INFO;
          const Icon = config.icon;
          const isRead = readIds.has(notification.announcementId);
          
          return (
            <div 
              key={notification.announcementId} 
              onClick={onItemClick}
              className={`group flex gap-3 p-3 rounded-lg transition-all border cursor-default
                ${isRead 
                  ? 'bg-transparent border-transparent opacity-70 hover:opacity-100 hover:bg-surface-subtle' 
                  : 'bg-surface border-outline/10 shadow-sm hover:shadow-md hover:border-outline/20'}
              `}
            >
              <div className={`p-2 rounded-full h-fit shrink-0 ${config.bg} ${config.color}`}>
                <Icon className="size-4" />
              </div>
              <div className="flex-1 space-y-1 min-w-0">
                <div className="flex items-start justify-between gap-2">
                  <h4 className={`text-sm font-medium leading-tight ${!isRead ? 'text-foreground' : 'text-muted-foreground'}`}>
                    {notification.title}
                  </h4>
                  {!isRead && (
                    <Badge variant="destructive" className="shrink-0 text-[10px] px-1.5 py-0 h-5">
                      NEW
                    </Badge>
                  )}
                </div>
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <span>{format(new Date(notification.publishAt), 'yyyy-MM-dd')}</span>
                </div>
                <p className="text-sm text-muted-foreground leading-relaxed line-clamp-2">
                  {notification.summary || notification.content}
                </p>
                <div className="pt-2 flex justify-end opacity-0 group-hover:opacity-100 transition-opacity">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs gap-1.5"
                    onClick={(e) => handleToggleRead(e, notification.announcementId, isRead)}
                  >
                    {isRead ? (
                      <>
                        <Mail className="size-3.5" />
                        未読にする
                      </>
                    ) : (
                      <>
                        <MailOpen className="size-3.5" />
                        既読にする
                      </>
                    )}
                  </Button>
                </div>
              </div>
            </div>
          );
        })
      )}
    </div>
  );

  if (variant === 'widget') {
    return (
      <Card className="h-full bg-card/50 backdrop-blur-sm border-outline/15 shadow-sm">
        <CardHeader className="pb-3">
          <CardTitle className="text-lg flex items-center gap-2">
            <Bell className="size-5 text-primary" />
            お知らせ
          </CardTitle>
        </CardHeader>
        <CardContent>
          {content}
        </CardContent>
      </Card>
    );
  }

  return content;
}

