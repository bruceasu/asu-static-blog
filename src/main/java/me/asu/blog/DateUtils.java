package me.asu.blog;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils
{
    /**
     * @param inputDate 要解析的字符串
     * @return 解析出来的日期，如果没有匹配的返回null
     */
    public static Date parseDate(String inputDate)
    {
        //可能出现的时间格式
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'",
                             "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm",
                             "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy年MM月dd日", "yyyy-MM-dd",
                             "yyyy/MM/dd", "yyyyMMdd"};
        SimpleDateFormat df = new SimpleDateFormat();
        for (String pattern : patterns) {
            df.applyPattern(pattern);
            //设置解析日期格式是否严格解析日期
            df.setLenient(false);
            ParsePosition pos = new ParsePosition(0);
            Date date = df.parse(inputDate, pos);
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    public static String formatDate(Date d)
    {
        if (d == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(d);
    }

}
