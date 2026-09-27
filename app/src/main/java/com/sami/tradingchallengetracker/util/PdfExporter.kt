package com.sami.tradingchallengetracker.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.sami.tradingchallengetracker.data.ChallengeEntity
import com.sami.tradingchallengetracker.data.TradeEntity
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun generateHtmlReport(
        challenge: ChallengeEntity,
        trades: List<TradeEntity>
    ): String {
        var totalProfits = 0L
        var totalLosses = 0L
        var netResult = 0L

        trades.forEach { t ->
            if (t.resultCents > 0) totalProfits += t.resultCents
            else if (t.resultCents < 0) totalLosses += Math.abs(t.resultCents)
            netResult += t.resultCents
        }

        val progress = Money.calculateProgress(challenge.currentBalanceCents, challenge.targetBalanceCents)
        val reportDate = Money.formatDateTime(System.currentTimeMillis())

        val tradeRows = if (trades.isEmpty()) {
            """<tr><td colspan="5" style="text-align:center; padding: 16px; color: #64748b;">لا توجد صفقات مسجلة حتى الآن</td></tr>"""
        } else {
            trades.joinToString("\n") { t ->
                val isWin = t.type.name == "WIN"
                val color = if (isWin) "#10b981" else "#ef4444"
                """
                <tr style="border-bottom: 1px solid #e2e8f0;">
                    <td style="padding: 8px; text-align: center; font-weight: bold;">${t.tradeNumber}</td>
                    <td style="padding: 8px; font-weight: bold; color: $color;">${Money.format(t.resultCents, true)}</td>
                    <td style="padding: 8px; color: #475569;">${Money.format(t.oldBalanceCents)}</td>
                    <td style="padding: 8px; font-weight: bold;">${Money.format(t.newBalanceCents)}</td>
                    <td style="padding: 8px; font-size: 11px; color: #64748b;">${Money.formatDateTime(t.timestamp)}</td>
                </tr>
                """.trimIndent()
            }
        }

        return """
        <!DOCTYPE html>
        <html dir="rtl" lang="ar">
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Cairo", sans-serif; background: #ffffff; color: #0f172a; margin: 20px; line-height: 1.5; }
                .header { text-align: center; border-bottom: 2px solid #0f172a; padding-bottom: 12px; margin-bottom: 20px; }
                .title { font-size: 22px; font-weight: 800; margin: 0; }
                .subtitle { font-size: 11px; color: #64748b; text-transform: uppercase; margin-bottom: 4px; letter-spacing: 1px; }
                .section { margin-bottom: 20px; }
                .section-title { font-size: 12px; font-weight: 700; color: #64748b; text-transform: uppercase; margin-bottom: 8px; }
                .info-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; font-size: 13px; }
                .info-row { display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px solid #f1f5f9; }
                .info-row:last-child { border-bottom: none; }
                .stats-grid { display: flex; gap: 8px; }
                .stat-card { flex: 1; padding: 10px; border-radius: 8px; text-align: center; font-size: 12px; }
                .stat-profit { background: #ecfdf5; border: 1px solid #a7f3d0; color: #047857; }
                .stat-loss { background: #fff1f2; border: 1px solid #fecdd3; color: #be123c; }
                .stat-net { background: #fffbeb; border: 1px solid #fde68a; color: #b45309; }
                table { width: 100%; border-collapse: collapse; font-size: 12px; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; }
                th { background: #f1f5f9; padding: 8px; text-align: right; border-bottom: 1px solid #e2e8f0; }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="subtitle">Sami • Trading Journey Tracker</div>
                <h1 class="title">تقرير رحلة التداول</h1>
            </div>

            <div class="section">
                <div class="section-title">معلومات المتداول</div>
                <div class="info-box">
                    <div class="info-row"><span>اسم المتداول:</span><b>${challenge.userName.ifEmpty { "غير محدد" }}</b></div>
                    <div class="info-row"><span>رأس المال الابتدائي:</span><b>${Money.format(challenge.initialCapitalCents)}</b></div>
                    <div class="info-row"><span>الرصيد الحالي:</span><b>${Money.format(challenge.currentBalanceCents)}</b></div>
                    <div class="info-row"><span>الهدف النهائي:</span><b>${Money.format(challenge.targetBalanceCents)}</b></div>
                    <div class="info-row"><span>نسبة التقدم:</span><b style="color: #047857;">${String.format(Locale.US, "%.2f", progress)}%</b></div>
                    <div class="info-row"><span>عدد الصفقات:</span><b>${trades.size}</b></div>
                    <div class="info-row"><span>تاريخ إنشاء التقرير:</span><span>$reportDate</span></div>
                </div>
            </div>

            <div class="section">
                <div class="section-title">ملخص الأداء</div>
                <div class="stats-grid">
                    <div class="stat-card stat-profit">
                        <div>إجمالي الأرباح</div>
                        <div style="font-weight: 800; font-size: 14px; margin-top: 4px;">${Money.format(totalProfits, true)}</div>
                    </div>
                    <div class="stat-card stat-loss">
                        <div>إجمالي الخسائر</div>
                        <div style="font-weight: 800; font-size: 14px; margin-top: 4px;">${if (totalLosses > 0) "-${Money.format(totalLosses)}" else "$0.00"}</div>
                    </div>
                    <div class="stat-card stat-net">
                        <div>صافي النتائج</div>
                        <div style="font-weight: 800; font-size: 14px; margin-top: 4px;">${Money.format(netResult, true)}</div>
                    </div>
                </div>
            </div>

            <div class="section">
                <div class="section-title">سجل الصفقات</div>
                <table>
                    <thead>
                        <tr>
                            <th style="text-align: center;">#</th>
                            <th>النتيجة</th>
                            <th>الرصيد السابق</th>
                            <th>الرصيد الجديد</th>
                            <th>التاريخ</th>
                        </tr>
                    </thead>
                    <tbody>
                        $tradeRows
                    </tbody>
                </table>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    fun shareReport(context: Context, challenge: ChallengeEntity, trades: List<TradeEntity>) {
        val htmlContent = generateHtmlReport(challenge, trades)
        val file = File(context.cacheDir, "trading_challenge_report.html")
        FileOutputStream(file).use { out ->
            out.write(htmlContent.toByteArray(Charsets.UTF_8))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "تقرير رحلة التداول")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "مشاركة تقرير رحلة التداول"))
    }
}
