import React from 'react';
import { User, Settings as SettingsIcon, Rocket, Plus, Target, CheckCircle2, TrendingUp, Layers, Wallet, Flag } from 'lucide-react';
import { Challenge, Trade } from '../types';
import { formatMoney, calculateProgress } from '../utils/money';

interface DashboardScreenProps {
  challenge: Challenge;
  trades?: Trade[];
  latestTrade?: Trade | null;
  notificationMessage?: string | null;
  onOpenSettings: () => void;
  onOpenAddTrade: () => void;
  onStartChallengeClick: () => void;
  onNewChallengeClick: () => void;
}

export const DashboardScreen: React.FC<DashboardScreenProps> = ({
  challenge,
  trades,
  latestTrade,
  notificationMessage,
  onOpenSettings,
  onOpenAddTrade,
  onStartChallengeClick,
  onNewChallengeClick,
}) => {
  const progressPercent = calculateProgress(
    challenge.currentBalanceCents,
    challenge.targetBalanceCents
  );

  const isTargetReached = challenge.currentBalanceCents >= challenge.targetBalanceCents;
  const completedTradesCount = trades
    ? trades.filter((t) => t.type === 'WIN').length
    : challenge.tradeCount;
  const remainingTrades = Math.max(0, 150 - completedTradesCount);

  return (
    <div className="space-y-4 pb-20 text-right animate-in fade-in duration-200">
      {/* Top Header */}
      <div className="flex items-center justify-between pt-1">
        <div className="flex items-center gap-3">
          <div className="relative">
            <img
              src="/app-icon.png"
              alt="Sami Logo"
              className="w-11 h-11 rounded-2xl object-cover border border-amber-500/40 shadow-md shadow-amber-500/10"
              referrerPolicy="no-referrer"
            />
            <span className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-emerald-500 rounded-full border-2 border-[#0b0f17]" />
          </div>
          <div>
            <span className="text-[11px] text-slate-400 font-medium block">تطبيق Sami</span>
            <h2 className="text-base font-bold text-slate-100 tracking-tight flex items-center gap-1.5">
              <span>{challenge.userName ? challenge.userName : 'المتداول'}</span>
            </h2>
          </div>
        </div>

        <button
          onClick={onOpenSettings}
          className="w-10 h-10 rounded-full bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700/60 flex items-center justify-center text-slate-300 hover:text-amber-400 transition-colors"
          title="الإعدادات"
        >
          <SettingsIcon className="w-5 h-5" />
        </button>
      </div>

      {/* Success Notification Alert */}
      {notificationMessage && (
        <div className="flex items-center gap-2 p-3 bg-emerald-500/15 border border-emerald-500/30 rounded-xl text-emerald-400 text-xs font-bold animate-in slide-in-from-top duration-300">
          <CheckCircle2 className="w-4 h-4 shrink-0" />
          <span>{notificationMessage}</span>
        </div>
      )}

      {/* Target Reached Banner */}
      {isTargetReached && (
        <div className="flex items-center gap-2 p-3.5 bg-gradient-to-r from-amber-500/20 via-emerald-500/20 to-amber-500/20 border border-amber-500/40 rounded-2xl text-amber-300 text-xs font-black shadow-lg">
          <Target className="w-5 h-5 text-amber-400 shrink-0 animate-bounce" />
          <div className="flex-1">
            <div className="text-sm">🎯 تم الوصول إلى الهدف النهائي!</div>
            <div className="text-[11px] text-slate-300 font-medium mt-0.5">
              تهانينا! يمكنك الاستمرار في التداول حتى المحطة الـ150.
            </div>
          </div>
        </div>
      )}

      {/* Main Balance Hero Card */}
      <div className="relative overflow-hidden bg-gradient-to-b from-[#141b2b]/85 to-[#0e1422]/85 backdrop-blur-md border border-slate-800/90 rounded-3xl p-5 shadow-2xl">
        {/* Glow accent */}
        <div className="absolute top-0 right-0 w-36 h-36 bg-amber-500/5 rounded-full blur-3xl pointer-events-none" />

        <div className="flex items-center justify-between mb-2">
          <span className="text-xs text-slate-400 font-medium">الرصيد الحالي</span>
          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-800/80 text-amber-400 border border-amber-500/20">
            البدء: {formatMoney(challenge.initialCapitalCents)}
          </span>
        </div>

        {/* Large Balance Number */}
        <div className="text-3xl sm:text-4xl font-black text-slate-50 font-['JetBrains_Mono',monospace] tracking-tight mb-4">
          {formatMoney(challenge.currentBalanceCents)}
        </div>

        {/* Target Progress Bar */}
        <div className="space-y-1.5 pt-2 border-t border-slate-800/80">
          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-400 font-medium">
              الهدف:{' '}
              <span className="font-['JetBrains_Mono',monospace] font-bold text-slate-200">
                {formatMoney(challenge.targetBalanceCents)}
              </span>
            </span>
            <span className="font-['JetBrains_Mono',monospace] font-bold text-amber-400">
              {progressPercent.toFixed(2)}%
            </span>
          </div>

          <div className="w-full h-2 bg-slate-800/80 rounded-full overflow-hidden p-0.5">
            <div
              className="h-full bg-gradient-to-r from-amber-500 to-emerald-400 rounded-full transition-all duration-500 ease-out"
              style={{ width: `${progressPercent}%` }}
            />
          </div>

          <div className="flex justify-between text-[10px] text-slate-400 font-['JetBrains_Mono',monospace] pt-0.5">
            <span>{formatMoney(challenge.initialCapitalCents)}</span>
            <span>{formatMoney(challenge.targetBalanceCents)}</span>
          </div>
        </div>
      </div>

      {/* 4 Stats Cards Grid (2x2) */}
      <div className="grid grid-cols-2 gap-3">
        {/* Card 1: Initial Capital */}
        <div className="bg-[#121824]/85 backdrop-blur-sm border border-slate-800/80 rounded-2xl p-3.5 shadow-md">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-[11px] text-slate-400 font-medium">رأس المال الابتدائي</span>
            <Wallet className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-base sm:text-lg font-black text-slate-100 font-['JetBrains_Mono',monospace]">
            {formatMoney(challenge.initialCapitalCents)}
          </div>
          <span className="text-[10px] text-slate-400 block mt-1">نقطة الانطلاق</span>
        </div>

        {/* Card 2: Trade Count */}
        <div className="bg-[#121824]/85 backdrop-blur-sm border border-slate-800/80 rounded-2xl p-3.5 shadow-md">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-[11px] text-slate-400 font-medium">عدد الصفقات</span>
            <TrendingUp className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-base sm:text-lg font-black text-slate-100 font-['JetBrains_Mono',monospace]">
            {completedTradesCount} / 150
          </div>
          <span className="text-[10px] text-slate-400 block mt-1">
            متبقي {remainingTrades}
          </span>
        </div>

        {/* Card 3: Current Balance */}
        <div className="bg-[#121824]/85 backdrop-blur-sm border border-slate-800/80 rounded-2xl p-3.5 shadow-md">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-[11px] text-slate-400 font-medium">الرصيد الحالي</span>
            <Layers className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-base sm:text-lg font-black text-slate-100 font-['JetBrains_Mono',monospace]">
            {formatMoney(challenge.currentBalanceCents)}
          </div>
          <span className="text-[10px] text-slate-400 block mt-1">
            {challenge.tradeCount === 0 ? 'قبل البدء' : 'رصيد الحساب'}
          </span>
        </div>

        {/* Card 4: Final Target */}
        <div className="bg-[#121824]/85 backdrop-blur-sm border border-slate-800/80 rounded-2xl p-3.5 shadow-md">
          <div className="flex items-center justify-between mb-1.5">
            <span className="text-[11px] text-slate-400 font-medium">الهدف النهائي</span>
            <Flag className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-base sm:text-lg font-black text-slate-100 font-['JetBrains_Mono',monospace]">
            {formatMoney(challenge.targetBalanceCents)}
          </div>
          <span className="text-[10px] text-slate-400 block mt-1">خطة النهاية</span>
        </div>
      </div>

      {/* Action CTA Buttons */}
      {!challenge.challengeStarted ? (
        <div className="bg-[#121824]/85 backdrop-blur-sm border border-amber-500/30 rounded-2xl p-4 text-center space-y-3">
          <div className="text-xs text-amber-300 font-bold">
            الرحلة غير مفعلة حالياً. اضغط على الزر أدناه لبدء رحلة الـ 150 صفقة!
          </div>
          <button
            onClick={onStartChallengeClick}
            className="w-full py-4 rounded-xl font-black text-slate-950 bg-amber-400 hover:bg-amber-300 active:scale-[0.98] transition-all shadow-xl shadow-amber-500/20 flex items-center justify-center gap-2 text-sm"
          >
            <Rocket className="w-5 h-5 transform rotate-45" />
            <span>بدء الرحلة برأس مال {formatMoney(challenge.initialCapitalCents)}</span>
          </button>
        </div>
      ) : (
        <div className="space-y-2.5 pt-1">
          {completedTradesCount < 150 ? (
            <button
              onClick={onOpenAddTrade}
              className="w-full py-4 rounded-2xl font-black text-slate-950 bg-amber-400 hover:bg-amber-300 active:scale-[0.98] transition-all shadow-xl shadow-amber-500/20 flex items-center justify-center gap-2 text-base"
            >
              <Plus className="w-5 h-5 stroke-[2.5]" />
              <span>إضافة صفقة جديدة (الصفقة #{challenge.tradeCount + 1})</span>
            </button>
          ) : (
            <div className="p-4 bg-emerald-500/15 border border-emerald-500/30 rounded-2xl text-center text-emerald-300 font-bold text-xs">
              🎉 تم إكمال جميع محطات الرحلة الـ 150 بنجاح!
            </div>
          )}

          <button
            onClick={onNewChallengeClick}
            className="w-full py-3 rounded-xl bg-slate-900/90 hover:bg-slate-800 text-slate-300 hover:text-amber-400 border border-slate-800 text-xs font-bold transition-all flex items-center justify-center gap-2"
          >
            <Rocket className="w-4 h-4 text-amber-400" />
            <span>ابدأ رحلة جديدة (سوف يتم تجديد 150 محطة تداول)</span>
          </button>
        </div>
      )}
    </div>
  );
};
