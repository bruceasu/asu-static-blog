package me.asu.blog;

import java.nio.file.Path;
import java.util.Date;
import lombok.Data;

@Data
public class SrcFileInfo {

    Path     path;
    long     lastModified;
    String[] fileTags;
    String   title;
    Date     articleDate;
    String   description;
    // 转发
    String   author;
    // 转发来源
    String   authorWebsite;
    String   project;
    String   destUrl;
}