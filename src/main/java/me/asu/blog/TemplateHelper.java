package me.asu.blog;

import asu.fastm.FastEx;
import asu.fastm.FastmConfig;

public class TemplateHelper
{
    public static void init(String templatesDir) {
        try {
            FastmConfig.setTemplateDir(templatesDir);
            // FastmConfig.setTemplateDir 会调用  Parser.setParserContext
//            net.fastm.Parser.setParserContext("src/main/templates");
            FastmConfig.loadFastmConfigByFilePath(templatesDir + "/fastm.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String parse(String templateName, Object value) throws Exception
    {
        return FastEx.parse(templateName, value);
    }
}
