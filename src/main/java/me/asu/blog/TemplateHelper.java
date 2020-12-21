package me.asu.blog;

import asu.fastm.FastEx;
import asu.fastm.FastmConfig;
import net.fastm.Parser;

public class TemplateHelper
{
    static{
        try {
            FastmConfig.setTemplateDir("templates");
            Parser.setParserContext("templates");
            FastmConfig.loadFastmConfigByFilePath("templates/fastm.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String parse(String templateName, Object value) throws Exception
    {
        return FastEx.parse(templateName, value);
    }
}
