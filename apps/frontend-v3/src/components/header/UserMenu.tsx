import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@mirel/ui';
import { User, Settings, LogOut, ChevronDown } from 'lucide-react';

/**
 * ユーザーメニューコンポーネント
 */
export function UserMenu() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);

  const handleLogout = async () => {
    try {
      await logout();
      navigate('/login');
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  if (!user) {
    return null;
  }

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-3 py-2 rounded-md hover:bg-gray-100"
      >
        <div className="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center text-white font-medium">
          {user.displayName?.charAt(0).toUpperCase() || 'U'}
        </div>
        <span className="text-sm font-medium">{user.displayName || user.email}</span>
        <ChevronDown className="w-4 h-4" />
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-56 bg-white rounded-md shadow-lg border z-50">
          <div className="px-4 py-3 border-b">
            <p className="text-sm font-medium">{user.displayName}</p>
            <p className="text-xs text-gray-500">{user.email}</p>
          </div>
          
          <div className="py-1">
            <button
              onClick={() => {
                setIsOpen(false);
                navigate('/settings/profile');
              }}
              className="w-full flex items-center gap-2 px-4 py-2 text-sm hover:bg-gray-100"
            >
              <User className="w-4 h-4" />
              プロフィール設定
            </button>
            
            <button
              onClick={() => {
                setIsOpen(false);
                navigate('/settings/security');
              }}
              className="w-full flex items-center gap-2 px-4 py-2 text-sm hover:bg-gray-100"
            >
              <Settings className="w-4 h-4" />
              セキュリティ設定
            </button>
          </div>

          <div className="py-1 border-t">
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50"
            >
              <LogOut className="w-4 h-4" />
              ログアウト
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
