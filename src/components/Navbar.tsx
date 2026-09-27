import React from 'react';
import { Home, BookOpen, MapPin, MessageCircle, Settings as SettingsIcon } from 'lucide-react';
import { ActiveTab } from '../types';

interface NavbarProps {
  activeTab: ActiveTab;
  onTabChange: (tab: ActiveTab) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ activeTab, onTabChange }) => {
  const tabs: { id: ActiveTab; label: string; icon: React.ReactNode }[] = [
    {
      id: 'dashboard',
      label: 'الرئيسية',
      icon: <Home className="w-5 h-5" />,
    },
    {
      id: 'chat',
      label: 'الدردشة 3000$',
      icon: <MessageCircle className="w-5 h-5" />,
    },
    {
      id: 'history',
      label: 'سجل الصفقات',
      icon: <BookOpen className="w-5 h-5" />,
    },
    {
      id: 'roadmap',
      label: 'المحطات',
      icon: <MapPin className="w-5 h-5" />,
    },
    {
      id: 'settings',
      label: 'الإعدادات',
      icon: <SettingsIcon className="w-5 h-5" />,
    },
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-[#0d131f]/95 backdrop-blur-md border-t border-slate-800/80 px-2 py-1 max-w-md mx-auto shadow-2xl">
      <div className="flex items-center justify-around h-15">
        {tabs.map((tab) => {
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
              className={`relative flex flex-col items-center justify-center flex-1 py-1 transition-all duration-200 select-none ${
                isActive
                  ? 'text-amber-400 font-bold'
                  : 'text-slate-400 hover:text-slate-200 font-medium'
              }`}
            >
              <div className="relative">
                {tab.icon}
                {isActive && (
                  <span className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-6 h-0.5 bg-amber-400 rounded-full" />
                )}
              </div>
              <span className="text-[11px] mt-1 tracking-tight">{tab.label}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
