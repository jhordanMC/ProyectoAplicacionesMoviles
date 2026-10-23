package com.example.saasvilcas.feature.dashboard.domain;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Business dates in Lima, represented at UTC midnight to avoid device timezone drift. */
public final class DashboardFilter {
    public final int period;
    public final long fromDay, toDay, chartToDay;
    public DashboardFilter(int period, long fromDay, long toDay, long chartToDay) {
        if(period<0 || period>4 || fromDay>toDay || chartToDay<toDay)
            throw new IllegalArgumentException("Revisa el rango de fechas.");
        this.period=period; this.fromDay=fromDay; this.toDay=toDay; this.chartToDay=chartToDay;
    }
    public static long day(int year,int month,int day) {
        Calendar date=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        date.clear(); date.setLenient(false); date.set(year,month,day); return date.getTimeInMillis();
    }
    public static long today(long instant) {
        Calendar lima=Calendar.getInstance(TimeZone.getTimeZone("America/Lima")); lima.setTimeInMillis(instant);
        return day(lima.get(Calendar.YEAR),lima.get(Calendar.MONTH),lima.get(Calendar.DAY_OF_MONTH));
    }
    public static DashboardFilter forPeriod(int period,long today,long customFrom,long customTo) {
        if(period==4) return new DashboardFilter(period,customFrom,customTo,customTo);
        Calendar from=Calendar.getInstance(TimeZone.getTimeZone("UTC")); from.setTimeInMillis(today);
        Calendar end=(Calendar)from.clone();
        if(period==1) {
            int elapsed=(from.get(Calendar.DAY_OF_WEEK)+5)%7;
            from.add(Calendar.DAY_OF_MONTH,-elapsed); end.setTimeInMillis(from.getTimeInMillis()); end.add(Calendar.DAY_OF_MONTH,6);
        } else if(period==2) {
            from.set(Calendar.DAY_OF_MONTH,1); end.set(Calendar.DAY_OF_MONTH,end.getActualMaximum(Calendar.DAY_OF_MONTH));
        } else if(period==3) {
            from.set(Calendar.DAY_OF_YEAR,1); end.set(Calendar.MONTH,Calendar.DECEMBER); end.set(Calendar.DAY_OF_MONTH,31);
        }
        return new DashboardFilter(period,from.getTimeInMillis(),today,end.getTimeInMillis());
    }
    public static String iso(long day) {
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd",Locale.US); format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(day));
    }
    public String query() { return "desde="+iso(fromDay)+"&hasta="+iso(toDay); }
    public String chartQuery() { return "desde="+iso(fromDay)+"&hasta="+iso(chartToDay); }
}
